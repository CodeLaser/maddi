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

package org.e2immu.analyzer.modification.analyzer.modification;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The {@code Element.visit(Predicate)} shape of the dogfood (Quest E, 2026-08-03): a visitor method
 * hands its receiver to a functional parameter carrying {@code @IgnoreModifications} — the author
 * disclaims whatever the predicate does. The jdk aapi already promises {@code Predicate.test} itself
 * modifies neither its receiver nor its argument; the only modification evidence can come from a
 * CALL SITE whose lambda modifies the element it receives. The disclaimer exists precisely to keep
 * that evidence from reaching {@code visit}: without the cut, one modifying lambda anywhere makes
 * every visit implementation modifying, which sinks the whole owner family's immutability
 * (the api statement family's visit/reject/typesReferenced, docs/eventual-info-hierarchy.md).
 */
public class TestVisitPredicateDisclaimer extends CommonTest {

    private String nonModifying(TypeInfo typeInfo, String methodName, int params) {
        var v = typeInfo.findUniqueMethod(methodName, params).analysis()
                .getOrNull(PropertyImpl.NON_MODIFYING_METHOD, ValueImpl.BoolImpl.class);
        return v == null ? "null" : v.isTrue() ? "true" : "false";
    }

    @Language("java")
    private static final String INPUT = """
            import java.util.function.Predicate;
            import org.e2immu.annotation.rare.IgnoreModifications;
            public class X {
              interface Element {
                void visit(@IgnoreModifications Predicate<Element> predicate);
              }
              static class Leaf implements Element {
                int counter;
                @Override public void visit(Predicate<Element> predicate) {
                  predicate.test(this);
                }
              }
              static class Node implements Element {
                private final Element child;
                Node(Element child) { this.child = child; }
                @Override public void visit(Predicate<Element> predicate) {
                  if (predicate.test(this)) child.visit(predicate);
                }
              }
              static class User {
                static boolean run(Element e) {
                  // the modifying lambda: the disclaimer on the visit parameter says this is the
                  // caller's business, not a modification of the element BY visit
                  final boolean[] done = { false };
                  e.visit(el -> { if (el instanceof Leaf leaf) leaf.counter++; done[0] = true; return true; });
                  return done[0];
                }
              }
            }
            """;

    @DisplayName("the @IgnoreModifications disclaimer keeps a modifying lambda from reaching visit (MODREACH)")
    @Test
    public void testModReach() {
        TypeInfo X = javaInspector.parse("X", INPUT);
        var ao = prepWork(X);
        var iterating = new org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl(javaInspector,
                new org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl.ConfigurationBuilder()
                        .setMaxIterations(10)
                        .setModificationViaReachability(true) // implies trackObjectCreations
                        .build());
        iterating.analyze(ao);
        TypeInfo element = X.findSubType("Element");
        TypeInfo leaf = X.findSubType("Leaf");
        TypeInfo node = X.findSubType("Node");

        assertEquals("Element.visit=true Leaf.visit=true Node.visit=true",
                "Element.visit=" + nonModifying(element, "visit", 1)
                + " Leaf.visit=" + nonModifying(leaf, "visit", 1)
                + " Node.visit=" + nonModifying(node, "visit", 1));
    }
}
