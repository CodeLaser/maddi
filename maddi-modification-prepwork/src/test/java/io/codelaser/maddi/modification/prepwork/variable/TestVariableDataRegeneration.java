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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 0 (GO/NO-GO) for the VariableData flatten-snapshot design
 * ({@code maddi-modification-analyzer/DESIGN-vardata-flatten.md}): the regeneration leg rests on being
 * able to drop a single method's per-statement VariableData mid-run and rebuild it from the body via
 * prepwork. This proves exactly that — clear the VARIABLE_DATA (write-once, cleared with removeIf so a
 * re-set is legal), re-run {@link PrepAnalyzer#doMethod}, and assert the rebuilt per-statement data is
 * identical. If prepwork could not be re-invoked per method, the design's Phase 3 would need rework.
 */
public class TestVariableDataRegeneration extends CommonTest {

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

    @DisplayName("drop a method's VariableData and regenerate it from the body")
    @Test
    public void regenerate() {
        TypeInfo X = javaInspector.parse(ABX, INPUT);
        MethodInfo method = X.findUniqueMethod("method", 1);
        new PrepAnalyzer(runtime).doMethod(method);

        List<Statement> statements = method.methodBody().statements();
        assertEquals(4, statements.size(), "int a; int b; int c; return c;");

        // capture the accumulative per-statement picture: a={p,a}, b={p,a,b}, c={p,a,b,c}, return={p,a,b,c}
        String methodBefore = VariableDataImpl.of(method).knownVariableNamesToString();
        List<String> before = statements.stream()
                .map(s -> VariableDataImpl.of(s).knownVariableNamesToString()).toList();
        assertTrue(before.get(0).contains("a") && before.get(2).contains("c"), () -> before.toString());

        // DROP: removeIf clears the write-once guard so a later set() is legal again
        method.analysis().removeIf(p -> p == VariableDataImpl.VARIABLE_DATA);
        statements.forEach(s -> s.analysis().removeIf(p -> p == VariableDataImpl.VARIABLE_DATA));
        assertNull(VariableDataImpl.of(method), "method VD cleared");
        statements.forEach(s -> assertNull(VariableDataImpl.of(s), "statement VD cleared"));

        // REGENERATE from the body
        new PrepAnalyzer(runtime).doMethod(method);

        assertEquals(methodBefore, VariableDataImpl.of(method).knownVariableNamesToString(),
                "method-level VD identical after regeneration");
        List<String> after = statements.stream()
                .map(s -> VariableDataImpl.of(s).knownVariableNamesToString()).toList();
        assertEquals(before, after, "per-statement VD identical after regeneration");
    }
}
