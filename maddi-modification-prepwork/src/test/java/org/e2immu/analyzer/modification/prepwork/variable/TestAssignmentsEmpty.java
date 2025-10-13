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

public class TestAssignmentsEmpty extends CommonTest {


    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                public static void method(String[] strings) {
                    for(String s: strings) {
                        int n = s.length();
                        if(n > 0) { }
                    }
                }
            }
            """;


    @DisplayName("merge and empty block")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);

        assertEquals("a.b.X.method(String[]):0:strings, s", vdMethod.knownVariableNamesToString());

        Statement s0 = method.methodBody().statements().get(0);
        VariableData vd0 = VariableDataImpl.of(s0);
        assertEquals("a.b.X.method(String[]):0:strings, s", vd0.knownVariableNamesToString());

        Statement s001 = s0.block().statements().get(1);
        VariableData vd001 = VariableDataImpl.of(s001);
        assertEquals("a.b.X.method(String[]):0:strings, n, s", vd001.knownVariableNamesToString());
        VariableInfoContainer vicN = vd001.variableInfoContainerOrNull("n");
        assertTrue(vicN.hasMerge());
        assertTrue(vicN.hasEvaluation());
        assertSame(vicN.best(Stage.EVALUATION), vicN.best(Stage.MERGE));
    }

}
