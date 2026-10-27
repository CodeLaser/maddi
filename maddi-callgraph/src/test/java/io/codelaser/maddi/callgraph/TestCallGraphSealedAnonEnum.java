/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.callgraph;

import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.graph.G;
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
