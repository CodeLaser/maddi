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

package io.codelaser.maddi.modification.analyzer.method;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.modification.analyzer.CommonTest;
import io.codelaser.maddi.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Option C of the O4 work (2026-09-26): a union over implementations that is only held down by implementations
 * delegating back into the union itself. The CST's Element.variables(d): ReturnStatementImpl returns
 * 'expression.variables(d)', leaves and containers build a fresh Stream. With the DEPENDENT default of the
 * undecided abstract method, every delegating implementation read @Dependent, and the union confirmed it.
 */
public class TestDelegatingUnion extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.stream.Stream;
            class X {
                interface Node { Stream<String> names(); }
                static final class Leaf implements Node {
                    private final String name;
                    Leaf(String name) { this.name = name; }
                    @Override public Stream<String> names() { return Stream.of(name); }
                }
                static final class Wrap implements Node {
                    private final Node inner;
                    Wrap(Node inner) { this.inner = inner; }
                    @Override public Stream<String> names() { return inner.names(); }
                }
                static final class Guarded implements Node {
                    private final Node inner;
                    private final boolean skip;
                    Guarded(Node inner, boolean skip) { this.inner = inner; this.skip = skip; }
                    @Override public Stream<String> names() {
                        if (skip) return Stream.of();
                        return inner.names();
                    }
                }
                static final class Maybe implements Node {
                    private final Node inner;
                    Maybe(Node inner) { this.inner = inner; }
                    @Override public Stream<String> names() { return inner == null ? Stream.of() : inner.names(); }
                }
                static final class Pair implements Node {
                    private final Node left;
                    private final Node right;
                    Pair(Node left, Node right) { this.left = left; this.right = right; }
                    @Override public Stream<String> names() { return Stream.concat(left.names(), right.names()); }
                }
            }
            """;

    private static String independent(MethodInfo methodInfo) {
        return String.valueOf(methodInfo.analysis().getOrNull(PropertyImpl.INDEPENDENT_METHOD,
                ValueImpl.IndependentImpl.class));
    }

    @DisplayName("delegating implementations do not hold the union down")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse("a.b.X", INPUT);
        List<Info> ao = prepWork(X);
        new IteratingAnalyzerImpl(javaInspector, new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(10).build()).analyze(ao);
        String all = List.of("Node", "Leaf", "Wrap", "Guarded", "Maybe", "Pair").stream()
                .map(t -> t + "=" + independent(X.findSubType(t).findUniqueMethod("names", 0)))
                .reduce((a, b) -> a + " " + b).orElseThrow();
        System.out.println("### " + all);
        assertEquals("Node=@Independent Leaf=@Independent Wrap=@Independent Guarded=@Independent Maybe=@Independent"
                     + " Pair=@Independent",
                all);
    }

    @Language("java")
    private static final String VETO = """
            package a.b;
            import java.util.ArrayList;
            import java.util.List;
            class Y {
                interface Node { List<String> names(); }
                static final class Holder implements Node {
                    private final List<String> names = new ArrayList<>();
                    @Override public List<String> names() { return names; }
                }
                static final class Wrap implements Node {
                    private final Node inner;
                    Wrap(Node inner) { this.inner = inner; }
                    @Override public List<String> names() { return inner.names(); }
                }
            }
            """;

    /*
     Fails at HEAD febe5d66d as well (found 2026-09-26): Wrap returns its own field's list and is computed @Independent
     while the union is correctly @Dependent -- the implementation is decided before the union and cannot come down.
     An optimistic (unsound) independence, pre-existing; kept as the reproducer.
     */
    @org.junit.jupiter.api.Disabled("pre-existing: a delegating implementation decided @Independent before its @Dependent union")
    @DisplayName("veto: one implementation exposing its own mutable field keeps the union and the delegator dependent")
    @Test
    public void veto() {
        TypeInfo Y = javaInspector.parse("a.b.Y", VETO);
        List<Info> ao = prepWork(Y);
        new IteratingAnalyzerImpl(javaInspector, new IteratingAnalyzerImpl.ConfigurationBuilder()
                .setMaxIterations(10).build()).analyze(ao);
        String all = List.of("Node", "Holder", "Wrap").stream()
                .map(t -> t + "=" + independent(Y.findSubType(t).findUniqueMethod("names", 0)))
                .reduce((a, b) -> a + " " + b).orElseThrow();
        assertEquals("Node=@Dependent Holder=@Dependent Wrap=@Dependent", all);
    }
}
