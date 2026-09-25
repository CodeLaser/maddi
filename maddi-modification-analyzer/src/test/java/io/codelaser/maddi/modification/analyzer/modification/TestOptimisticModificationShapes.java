/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.modification.analyzer.modification;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The OPTIMISTIC shapes of the Eclipse Collections engine work list (maddi-aapi-archive
 * .../libs/eclipsecollections/ENGINE-WORKLIST.md, O1-O4): each is a method that modifies its receiver and is
 * computed {@code @NotModified}, reduced from the EC source it was found in. Each test asserts the verdict a CLIENT
 * sees, under the production configuration (modification via reachability on: every fixture that leaves it off,
 * which is the default, never saw O1).
 * <p>
 * O1 was ShadowModificationPass's receiver-projection cache, keyed by expression only while a lambda body is walked
 * twice (as its own method and inside its enclosing method): the enclosing method lost its edge and the MODREACH
 * cutover wrote {@code list.forEach(x -> this.items.add(x))} non-modifying.
 */
public class TestOptimisticModificationShapes extends CommonTest {

    private void analyze(TypeInfo typeInfo) {
        List<Info> ao = prepWork(typeInfo);
        new IteratingAnalyzerImpl(javaInspector, new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(10)
                .setModificationViaReachability(true)
                .build()).analyze(ao);
    }

    private static MethodInfo method(TypeInfo typeInfo, String name) {
        return typeInfo.methods().stream().filter(m -> m.name().equals(name)).findFirst().orElseThrow();
    }

    /*
     O2. EC's MutableByteList.sortThis(ByteComparator) is a default method that only throws; ByteArrayList overrides
     it and sorts. A call through the interface reaches the override, so the interface method, and every caller
     through it, modifies.
     */
    @Language("java")
    private static final String O2 = """
            package a.b;
            class O2 {
                interface Sortable {
                    default void sortThis() { throw new UnsupportedOperationException("not supported"); }
                }
                static class Counter implements Sortable {
                    private int sorts;
                    @Override public void sortThis() { sorts++; }
                    int sorts() { return sorts; }
                }
                static void sortIt(Sortable s) { s.sortThis(); }
            }
            """;

    @DisplayName("O2: a throwing default method overridden by a modifying implementation modifies")
    @Test
    public void o2() {
        TypeInfo X = javaInspector.parse("a.b.O2", O2);
        analyze(X);
        MethodInfo sortIt = method(X, "sortIt");
        assertTrue(!sortIt.parameters().getFirst().isUnmodified(),
                "the argument of sortIt(s) reaches Counter.sortThis(), which modifies it");
    }

    /*
     O2, option D: a default method with a REAL, non-modifying body overridden by a modifying method keeps its own
     verdict (no dispatch union, by design); the guard reports it instead.
     */
    @Language("java")
    private static final String O2_D = """
            package a.b;
            class O2d {
                interface Closeable2 {
                    default void close() { }
                }
                static class Counter implements Closeable2 {
                    private int closes;
                    @Override public void close() { closes++; }
                }
                static void closeIt(Closeable2 c) { c.close(); }
            }
            """;

    @DisplayName("O2-D: a real default body keeps its verdict; the weakening override is reported")
    @Test
    public void o2d() {
        TypeInfo X = javaInspector.parse("a.b.O2d", O2_D);
        List<Info> ao = prepWork(X);
        var iterating = new IteratingAnalyzerImpl(javaInspector, new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(10)
                .setModificationViaReachability(true)
                .build());
        iterating.analyze(ao);
        MethodInfo close = method(X.findSubType("Closeable2"), "close");
        assertTrue(close.isNonModifying(), "the default's own body decides its verdict");
        assertTrue(iterating.messages().stream().anyMatch(m -> m.message().contains("O2d.Counter.close")
                        && io.codelaser.maddi.modification.analyzer.impl.GuardAnalyzerImpl.OVERRIDE_WEAKENS_COMPUTED
                        .equals(m.category())),
                "the override that modifies is reported: " + iterating.messages());
    }

    /*
     O1. EC's <P>ObjectHashMap.putAll: map.forEachKeyValue((k, v) -> Outer.this.put(k, v)).
     */
    @Language("java")
    private static final String O1 = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.function.Consumer;
            class O1 {
                interface Source { void each(Consumer<String> consumer); }
                private final List<String> items = new ArrayList<>();
                void add(String s) { items.add(s); }
                void addAll(Source source) { source.each(s -> O1.this.add(s)); }
                int size() { return items.size(); }
            }
            """;

    @DisplayName("O1: a lambda calling a modifying method on the captured outer instance modifies the receiver")
    @Test
    public void o1() {
        TypeInfo X = javaInspector.parse("a.b.O1", O1);
        analyze(X);
        assertTrue(method(X, "add").isModifying(), "add modifies items");
        assertTrue(method(X, "addAll").isModifying(), "addAll adds through the lambda");
    }

    /*
     O1, with an implementation of Source that calls its consumer (EC's forEachKeyValue has many).
     */
    @Language("java")
    private static final String O1_IMPL = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.function.Consumer;
            class O1b {
                interface Source { void each(Consumer<String> consumer); }
                static class ListSource implements Source {
                    private final List<String> list = new ArrayList<>();
                    @Override public void each(Consumer<String> consumer) { for (String s : list) consumer.accept(s); }
                }
                private final List<String> items = new ArrayList<>();
                void add(String s) { items.add(s); }
                void addAll(Source source) { source.each(s -> O1b.this.add(s)); }
            }
            """;

    @DisplayName("O1b: as O1, with an implementation of Source that calls the consumer")
    @Test
    public void o1b() {
        TypeInfo X = javaInspector.parse("a.b.O1b", O1_IMPL);
        analyze(X);
        assertTrue(method(X, "addAll").isModifying(), "addAll adds through the lambda");
    }

    /*
     O1, through the JDK: List.forEach, whose behaviour comes from the JDK analysis hints.
     */
    @Language("java")
    private static final String O1_JDK = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class O1c {
                private final List<String> items = new ArrayList<>();
                void add(String s) { items.add(s); }
                void addAll(List<String> source) { source.forEach(s -> O1c.this.add(s)); }
                void addAllRef(List<String> source) { source.forEach(this::add); }
                void addAllThis(List<String> source) { source.forEach(s -> this.add(s)); }
                void addAllField(List<String> source) { source.forEach(s -> items.add(s)); }
            }
            """;

    @DisplayName("O1c: as O1, through java.util.List.forEach")
    @Test
    public void o1c() {
        TypeInfo X = javaInspector.parse("a.b.O1c", O1_JDK);
        analyze(X);
        String verdicts = java.util.stream.Stream.of("add", "addAll", "addAllRef", "addAllThis", "addAllField")
                .map(m -> m + "=" + method(X, m).isModifying()).collect(java.util.stream.Collectors.joining(" "));
        System.out.println("### O1c " + verdicts);
        assertTrue(method(X, "addAllField").isModifying(), "lambda modifying a field: " + verdicts);
        assertTrue(method(X, "addAllThis").isModifying(), "lambda calling this.add: " + verdicts);
        assertTrue(method(X, "addAllRef").isModifying(), "this::add: " + verdicts);
        assertTrue(method(X, "addAll").isModifying(), "lambda calling O1c.this.add (EC's shape): " + verdicts);
    }

    /*
     O3 (GREEN: does not reproduce the EC finding in this reduction -- EC's remove binds to its own interfaces).
     EC's AbstractUnifiedSet.removeAllIterable: for (Object each : iterable) changed |= this.remove(each); in an
     abstract class that inherits remove from java.util.Set.
     */
    @Language("java")
    private static final String O3 = """
            package a.b;
            import java.util.AbstractSet;
            abstract class O3<T> extends AbstractSet<T> {
                public boolean removeAllIterable(Iterable<?> iterable) {
                    boolean changed = false;
                    for (Object each : iterable) { changed |= this.remove(each); }
                    return changed;
                }
            }
            """;

    @DisplayName("O3: this.remove(x) in a for-each over an argument modifies the receiver")
    @Test
    public void o3() {
        TypeInfo X = javaInspector.parse("a.b.O3", O3);
        analyze(X);
        assertTrue(method(X, "removeAllIterable").isModifying(), "removeAllIterable calls this.remove");
    }

    /*
     O4. EC's MutableBooleanCollection.removeIf: a default method removing through an iterator obtained from this.
     */
    @Language("java")
    private static final String O4 = """
            package a.b;
            import java.util.Iterator;
            import java.util.function.Predicate;
            interface O4<T> extends Iterable<T> {
                default boolean removeIf2(Predicate<? super T> predicate) {
                    boolean changed = false;
                    Iterator<T> iterator = this.iterator();
                    while (iterator.hasNext()) {
                        if (predicate.test(iterator.next())) { iterator.remove(); changed = true; }
                    }
                    return changed;
                }
            }
            """;

    @DisplayName("O4: removing through an iterator obtained from this modifies the receiver")
    @Disabled("RED, awaiting a decision: the JDK hint makes java.lang.Iterable @ImmutableContainer(hc = true), so it "
              + "has no §m modification component and iterator()'s except = \"remove\" link (it.§m ☷ this.§m) is never "
              + "created; over java.util.Collection the same code is @Modified. Not yet reduced from EC's own shape "
              + "(MutableBooleanCollection.booleanIterator(), an EC interface), which may be a different mechanism.")
    @Test
    public void o4() {
        TypeInfo X = javaInspector.parse("a.b.O4", O4);
        analyze(X);
        assertTrue(method(X, "removeIf2").isModifying(), "iterator.remove() modifies the collection it came from");
    }
}
