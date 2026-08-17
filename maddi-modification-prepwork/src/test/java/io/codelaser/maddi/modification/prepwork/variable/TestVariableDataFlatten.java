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

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 of the flatten-snapshot design ({@code DESIGN-vardata-flatten.md}): the flatten primitive
 * itself, in isolation (no analyzer wiring). A method's last-statement VariableData holds a container
 * per in-scope variable, each chained back to the previous statement's container. Flattening must
 * preserve every value-bearing query while collapsing that chain link, so the intermediate containers
 * become collectible once the intermediate statements' VARIABLE_DATA is dropped.
 */
public class TestVariableDataFlatten extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                static int method(int p) {
                    int a = p + 1;
                    int b = a * 2;
                    int c = b - a;
                    return c;
                }
            }
            """;

    @DisplayName("flatten preserves best()/definition and breaks the previous-statement chain")
    @Test
    public void flatten() {
        TypeInfo X = javaInspector.parse(ABX, INPUT);
        MethodInfo method = X.findUniqueMethod("method", 1);
        new PrepAnalyzer(runtime).doMethod(method);

        Statement last = method.methodBody().statements().getLast(); // return c;
        VariableDataImpl vd = (VariableDataImpl) VariableDataImpl.of(last);
        assertNotNull(vd);
        // p (param), a, b (in scope, untouched here => previous-only VICs) and c (read => has evaluation)
        assertTrue(vd.knownVariableNames().size() >= 4, () -> vd.knownVariableNamesToString());

        VariableDataImpl flat = vd.flattened();
        assertNotSame(vd, flat);
        assertEquals(vd.knownVariableNamesToString(), flat.knownVariableNamesToString(),
                "same variables in the same order");

        vd.variableInfoContainerStream().forEach(orig -> {
            String fqn = orig.variable().fullyQualifiedName();
            VariableInfoContainer f = flat.variableInfoContainerOrNull(fqn);
            assertNotNull(f, () -> "missing " + fqn);
            // value-bearing queries preserved — same VariableInfo object, so links/assignments/reads too
            assertSame(orig.best(), f.best(), () -> "best() preserved for " + fqn);
            assertSame(orig.getPreviousOrInitial(), f.getPreviousOrInitial(),
                    () -> "getPreviousOrInitial() value preserved for " + fqn);
            assertEquals(orig.indexOfDefinition(), f.indexOfDefinition(), () -> "definition for " + fqn);
            assertEquals(orig.hasEvaluation(), f.hasEvaluation(), () -> "hasEvaluation for " + fqn);
            assertEquals(orig.hasMerge(), f.hasMerge(), () -> "hasMerge for " + fqn);
            // structural change: the chain link is gone
            assertFalse(f.isPrevious(), () -> "chain link broken for " + fqn);
            assertTrue(f.isInitial(), () -> "flattened container reads as back-reference-free for " + fqn);
        });

        // idempotent: flattening a flattened VD changes nothing structural
        VariableDataImpl again = flat.flattened();
        again.variableInfoContainerStream().forEach(f -> assertFalse(f.isPrevious()));
    }
}
