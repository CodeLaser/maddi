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
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.IteratingAnalyzer;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A parameter that is modified ONLY because it is handed to a functional parameter of the same method -- vavr's
 * {@code ValueModule.toTraversable(value, empty, ofElement, ofAll)}, {@code ofAll.apply(value)} -- is modified at a
 * call site only when the function passed there modifies it. {@code Value.toQueue()} passes {@code Queue::ofAll},
 * which does not touch its {@code Iterable}; the unconditional reading marked every {@code toQueue()} receiver
 * modified, and through it {@code List.Cons.tail}.
 * <ul>
 * <li>{@code measure}: the callee; its {@code value} stays modified (it is, for some function).</li>
 * <li>{@code useSize}/{@code useSizeLambda}: a method reference / lambda that does not modify -- {@code in}
 *     unmodified.</li>
 * <li>{@code useClear}: a method reference that does modify -- {@code in} modified.</li>
 * <li>{@code forward}: the caller's own functional parameter, unknown here -- conservatively modified.</li>
 * <li>{@code useBoth}: the callee also modifies {@code value} unconditionally -- modified whatever is passed.</li>
 * </ul>
 * {@code Fn.apply} is contracted {@code @NotModified} with a {@code @Modified} parameter, the unit-scale stand-in
 * for the JDK hint {@code Function.apply(@Modified T)} on a {@code @NotModified} method.
 */
public class TestModificationThroughPassedFunction extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import io.codelaser.maddi.annotation.Modified;
            import io.codelaser.maddi.annotation.NotModified;
            import java.util.List;

            public class X {
                interface Fn<A, R> {
                    @NotModified
                    R apply(@Modified A a);
                }

                static int measure(List<String> value, Fn<List<String>, Integer> f) {
                    return f.apply(value);
                }

                static int both(List<String> value, Fn<List<String>, Integer> f) {
                    value.add("x");
                    return f.apply(value);
                }

                static int size(List<String> l) {
                    return l.size();
                }

                static int clearAndCount(List<String> l) {
                    int n = l.size();
                    l.clear();
                    return n;
                }

                static int useSize(List<String> in) {
                    return measure(in, X::size);
                }

                static int useSizeLambda(List<String> in) {
                    return measure(in, l -> l.size());
                }

                static int useClear(List<String> in) {
                    return measure(in, X::clearAndCount);
                }

                static int forward(List<String> in, Fn<List<String>, Integer> f) {
                    return measure(in, f);
                }

                static int useBoth(List<String> in) {
                    return both(in, X::size);
                }
            }
            """;

    @DisplayName("a parameter modified only through a passed function is modified only when that function modifies")
    @Test
    public void test() throws IOException {
        run(false);
    }

    @DisplayName("... and the same with the MODREACH cutover on, as every corpus run has it")
    @Test
    public void testModReach() throws IOException {
        run(true);
    }

    private void run(boolean modReach) throws IOException {
        AnalyzerBundle bundle = buildAnalyzerBundle();
        TypeInfo x = bundle.javaInspector().parse("a.b.X", INPUT);
        List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(x);
        IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10)
                        .setModificationViaReachability(modReach).build());
        analyzer.analyze(analysisOrder);

        StringBuilder sb = new StringBuilder("### modReach=" + modReach);
        for (String m : List.of("measure", "both", "size", "clearAndCount", "useSize", "useSizeLambda", "useClear",
                "forward", "useBoth")) {
            sb.append(' ').append(m).append('=').append(modified(x, m));
        }
        System.out.println(sb);
        assertEquals(Set.of("1:0"), through(x, "measure"), "value goes to argument 0 of parameter 1's SAM");
        assertEquals(Set.of(), through(x, "both"), "both also modifies value itself: nothing to specialise");

        assertTrue(modified(x, "measure"), "measure's value IS modified, for some function");
        assertFalse(modified(x, "size"));
        assertTrue(modified(x, "clearAndCount"));

        assertFalse(modified(x, "useSize"), "X::size does not modify in");
        assertFalse(modified(x, "useSizeLambda"), "l -> l.size() does not modify in");
        assertTrue(modified(x, "useClear"), "X::clearAndCount does modify in");
        assertTrue(modified(x, "forward"), "an unknown function: conservatively modified");
        assertTrue(modified(x, "useBoth"), "both modifies value itself, whatever the function");
    }

    /*
     * The vavr shape, with java.util.function.Function. Val's accessors are contracted @NotModified: in vavr they are
     * NOT, because io.vavr.Value.isEmpty()/get() take the minimum over their implementations, and
     * io.vavr.collection.Iterator and io.vavr.concurrent.Future implement them modifying -- so on vavr,
     * value.isEmpty() alone modifies value, whatever function is passed (measured 2026-09-24).
     */
    @Language("java")
    private static final String INPUT_JDK = """
            package a.b;
            import io.codelaser.maddi.annotation.NotModified;
            import java.util.ArrayList;
            import java.util.List;
            import java.util.function.Function;

            public class Y {
                interface Val<T> extends Iterable<T> {
                    @NotModified
                    boolean isEmpty();
                    @NotModified
                    boolean isSingleValued();
                    @NotModified
                    T get();
                }

                static <T, R> R toTraversable(Val<T> value, R empty, Function<T, R> ofElement,
                                              Function<Iterable<T>, R> ofAll) {
                    if (value.isEmpty()) {
                        return empty;
                    } else if (value.isSingleValued()) {
                        return ofElement.apply(value.get());
                    } else {
                        return ofAll.apply(value);
                    }
                }

                static <T> List<T> of(T t) {
                    return List.of(t);
                }

                static <T> List<T> ofAll(Iterable<T> it) {
                    List<T> list = new ArrayList<>();
                    for (T t : it) list.add(t);
                    return list;
                }

                static <T> List<T> toList(Val<T> in) {
                    return toTraversable(in, List.of(), Y::of, Y::ofAll);
                }
            }
            """;

    @DisplayName("the vavr shape: java.util.function.Function, a Value-like parameter")
    @Test
    public void testJdkFunction() throws IOException {
        for (boolean modReach : new boolean[]{false, true}) {
            AnalyzerBundle bundle = buildAnalyzerBundle();
            TypeInfo y = bundle.javaInspector().parse("a.b.Y", INPUT_JDK);
            List<Info> analysisOrder = bundle.prepAnalyzer().doPrimaryType(y);
            IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(bundle.javaInspector(),
                    new IteratingAnalyzerImpl.ConfigurationBuilder().setMaxIterations(10)
                            .setModificationViaReachability(modReach).build());
            analyzer.analyze(analysisOrder);
            assertEquals(Set.of("3:0"), through(y, "toTraversable"), "modReach=" + modReach);
            assertTrue(modified(y, "toTraversable"), "modReach=" + modReach);
            assertFalse(modified(y, "ofAll"), "modReach=" + modReach);
            assertFalse(modified(y, "toList"), "Y::ofAll does not modify in; modReach=" + modReach);
        }
    }

    private static Set<String> through(TypeInfo x, String method) {
        return x.methods().stream().filter(m -> m.name().equals(method)).findFirst().orElseThrow()
                .parameters().getFirst().analysis().getOrDefault(PropertyImpl.MODIFIED_THROUGH_PASSED_FUNCTION,
                        ValueImpl.SetOfStringsImpl.EMPTY_SET).set();
    }

    private static boolean modified(TypeInfo x, String method) {
        return x.methods().stream().filter(m -> m.name().equals(method)).findFirst().orElseThrow()
                .parameters().getFirst().isModified();
    }
}
