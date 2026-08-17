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
