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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A type named ONLY by a switch type pattern is a compile dependency like any other, in both switch forms.
 * The arrow form was recorded; the OLD-STYLE (colon) form was not, and the difference is invisible until
 * something asks "who still names this type".
 * <p>
 * Found carving Elasticsearch: {@code path.seamVerdicts} reported a lift set's seam as CLEAN, the types moved,
 * and {@code :server} then failed to compile —
 * <pre>
 *   SearchRequestAttributesExtractor.java:30: error: package …search.retriever does not exist
 *       switch (retrieverBuilder) {
 *           case KnnRetrieverBuilder knn:          // the only mention of the type, in a STAYING class
 * </pre>
 * Same family as {@link TestCallGraphSealedAnonEnum}: a forward type reference the graph dropped, so periphery
 * analysis mis-classified the type as unreferenced.
 */
public class TestCallGraphSwitchPattern extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                static class ColonOnly {}
                static class ArrowOnly {}
                static class InstanceOfOnly {}
                // the Elasticsearch shape: the tested type is a SUBTYPE of the class doing the testing
                static class Base {}
                static class Derived extends Base {}

                // the Elasticsearch shape: an old-style switch STATEMENT whose labels are type patterns
                static int colon(Object o) {
                    switch (o) {
                        case ColonOnly c:
                            return 1;
                        default:
                            return 0;
                    }
                }

                static int arrow(Object o) {
                    return switch (o) {
                        case ArrowOnly a -> 1;
                        default -> 0;
                    };
                }

                static boolean negated(Object o) {
                    return o != null && o instanceof InstanceOfOnly == false;
                }

                static class BaseWithTest extends Base {
                    // AggregatorBase:137 verbatim: a base class asking whether something is one of ITS OWN
                    // subtypes. A reference to a subtype is a compile dependency like any other.
                    boolean isDerived(Base b) { return b != null && b instanceof Derived == false; }
                }
            }
            """;

    @DisplayName("a type named only by a switch type pattern is reachable — colon form as well as arrow")
    @Test
    public void test() {
        TypeInfo X = javaInspector.parse(ABX, INPUT);
        ComputeCallGraph ccg = new ComputeCallGraph(runtime, X);
        G<Info> graph = ccg.go().graph();
        String printed = ComputeCallGraph.print(graph);
        System.out.println(printed);

        assertTrue(printed.matches("(?s).*->R->a\\.b\\.X\\.ArrowOnly.*"),
                "arrow-form switch pattern: no REFERENCES edge into ArrowOnly");
        assertTrue(printed.matches("(?s).*->R->a\\.b\\.X\\.InstanceOfOnly.*"),
                "instanceof (negated, inside &&): no REFERENCES edge into InstanceOfOnly");
        assertTrue(printed.matches("(?s).*->R->a\\.b\\.X\\.ColonOnly.*"),
                "OLD-STYLE switch pattern: no REFERENCES edge into ColonOnly");
        assertTrue(printed.matches("(?s).*->R->a\\.b\\.X\\.Derived.*"),
                "a supertype naming its own SUBTYPE is a dependency too: no REFERENCES edge into Derived");
    }
}
