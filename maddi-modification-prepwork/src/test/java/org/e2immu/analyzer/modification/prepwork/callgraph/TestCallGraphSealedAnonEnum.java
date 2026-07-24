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

package org.e2immu.analyzer.modification.prepwork.callgraph;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.util.internal.graph.G;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The call graph must record three forward type references it used to drop, each of which made periphery
 * analysis ({@code cyclicTypeComponents}/{@code dependentTypes}) mis-classify a type as "above the SCC"
 * when the giant actually depends on it (found carving Elasticsearch):
 * <ol>
 *   <li>a sealed type's {@code permits} → its subclass;</li>
 *   <li>a {@code new Y()} passed as an argument to an anonymous-class constructor {@code new Z(new Y()){}};</li>
 *   <li>a {@code Y::new} constructor method-reference in an enum constant that has a body.</li>
 * </ol>
 */
public class TestCallGraphSealedAnonEnum extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Supplier;
            class X {
                sealed interface Shape permits Circle {}
                record Circle() implements Shape {}

                static class Payload {}
                static class Wrapper { Wrapper(Payload p) {} }
                // new Payload() is an argument to an anonymous-class constructor
                static final Wrapper W = new Wrapper(new Payload()) { };

                interface Maker { Object make(); }
                static class Prod implements Maker { public Object make() { return null; } }
                enum E {
                    A(Prod::new) { };            // enum constant with a body + Prod::new argument
                    E(Supplier<Prod> s) {}
                }
            }
            """;

    @DisplayName("permits, anon-ctor argument, and enum-constant ::new all produce edges")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse(ABX, INPUT);
        ComputeCallGraph ccg = new ComputeCallGraph(runtime, X);
        G<Info> graph = ccg.go().graph();
        String printed = ComputeCallGraph.print(graph);
        System.out.println(printed);

        // 1. sealed permits: Shape -> Circle
        assertTrue(printed.contains("a.b.X.Shape->H->a.b.X.Circle"),
                "missing sealed-permits edge Shape->Circle");
        // 2. new Payload() as an argument to the anonymous-class constructor: reaches Payload
        assertTrue(printed.contains("a.b.X.Payload"),
                "missing edge into Payload (arg of anonymous-class constructor)");
        assertTrue(printed.matches("(?s).*->R->a\\.b\\.X\\.Payload.*"),
                "no REFERENCES edge into Payload");
        // 3. Prod::new inside an enum constant with a body: reaches Prod
        assertTrue(printed.matches("(?s).*->[A-Z]*->a\\.b\\.X\\.Prod.*"),
                "missing edge into Prod (Prod::new in enum constant with body)");
    }
}
