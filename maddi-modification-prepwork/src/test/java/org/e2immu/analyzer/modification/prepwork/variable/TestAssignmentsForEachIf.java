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
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAssignmentsForEachIf extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.util.List;
            import java.io.IOException;

            public class X {
                private static final String SEEN_THEM_ALL = "seen them all";

                protected String method(String[] array) throws IOException {
                    StringBuilder sb = new StringBuilder();
                    for (String s : array) {
                        if (s.length() < 2) {
                            sb.append(s);
                        } else if(s.length() > 20) {
                            throw new IOException("too long");
                        } else {
                            return sb.toString();
                        }
                    }
                    return SEEN_THEM_ALL;
                }
            }
            """;

    @DisplayName("debugging a merge issue")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        ParameterInfo array = method.parameters().get(0);

        Statement s0 = method.methodBody().statements().get(0);
        VariableData vd0 = VariableDataImpl.of(s0);
        VariableInfo sbVi0 = vd0.variableInfo("sb");
        assertEquals("D:0, A:[0]", sbVi0.assignments().toString());
        assertFalse(vd0.isKnown(array.fullyQualifiedName()));

        Statement s1 = method.methodBody().statements().get(1);
        IfElseStatement s100 = (IfElseStatement) s1.block().statements().get(0);
        Statement s10000 = s100.block().statements().get(0);

        VariableData vd10000 = VariableDataImpl.of(s10000);
        VariableInfo sbVi10000 = vd10000.variableInfo("sb");
        assertEquals("D:0, A:[0]", sbVi10000.assignments().toString());
        assertEquals("1.0.0.0.0", sbVi10000.reads().toString());
        VariableInfo arrayVi10000 = vd10000.variableInfo(array.fullyQualifiedName());
        assertEquals("D:-, A:[]", arrayVi10000.assignments().toString());
        assertEquals("1-E", arrayVi10000.reads().toString());

        IfElseStatement s10010 = (IfElseStatement) s100.elseBlock().statements().get(0);
        Statement s1001010 = s10010.elseBlock().statements().get(0);

        VariableData vd1001010 = VariableDataImpl.of(s1001010);
        VariableInfo sbVi1001010 = vd1001010.variableInfo("sb");
        assertEquals("D:0, A:[0]", sbVi1001010.assignments().toString());
        assertEquals("1.0.0.1.0.1.0", sbVi1001010.reads().toString());
        VariableInfo arrayVi1001010 = vd1001010.variableInfo(array.fullyQualifiedName());
        assertEquals("D:-, A:[]", arrayVi1001010.assignments().toString());
        assertEquals("1-E", arrayVi1001010.reads().toString());

        // first merge
        VariableData vd10010 = VariableDataImpl.of(s10010);
        VariableInfo sbVi10010 = vd10010.variableInfo("sb");
        assertEquals("1.0.0.1.0.1.0", sbVi10010.reads().toString());
        assertEquals("D:0, A:[0]", sbVi10010.assignments().toString());
        VariableInfo arrayVi10010 = vd10010.variableInfo(array.fullyQualifiedName());
        assertEquals("D:-, A:[]", arrayVi10010.assignments().toString());
        assertEquals("1-E", arrayVi10010.reads().toString());

        // second merge
        VariableData vd100 = VariableDataImpl.of(s100);
        VariableInfo sbVi100 = vd100.variableInfo("sb");
        assertEquals("1.0.0.0.0, 1.0.0.1.0.1.0", sbVi100.reads().toString());
        assertEquals("D:0, A:[0]", sbVi100.assignments().toString());
        VariableInfo arrayVi100 = vd100.variableInfo(array.fullyQualifiedName());
        assertEquals("D:-, A:[]", arrayVi100.assignments().toString());
        assertEquals("1-E", arrayVi100.reads().toString());

        // third merge
        VariableData vd1 = VariableDataImpl.of(s1);
        VariableInfo sbVi1 = vd1.variableInfo("sb");
        assertEquals("D:0, A:[0]", sbVi1.assignments().toString());
        VariableInfo arrayVi1 = vd1.variableInfo(array.fullyQualifiedName());
        assertEquals("D:-, A:[]", arrayVi1.assignments().toString());

        // last statement
        Statement s2 = method.methodBody().statements().get(1);
        VariableData vd2 = VariableDataImpl.of(s2);
        VariableInfo sbVi2 = vd2.variableInfo("sb");
        assertEquals("D:0, A:[0]", sbVi2.assignments().toString());
        VariableInfo arrayVi2 = vd2.variableInfo(array.fullyQualifiedName());
        assertEquals("D:-, A:[]", arrayVi2.assignments().toString());

        VariableData vdMethod = VariableDataImpl.of(method);
        assertNotNull(vdMethod);

        VariableInfo rvVi = vdMethod.variableInfo(method.fullyQualifiedName());
        assertEquals("D:-, A:[1.0.0.1.0.0.0, 1.0.0.1.0.1.0, 1.0.0.1.0=M, 2]", rvVi.assignments().toString());
        assertTrue(rvVi.hasBeenDefined("2=M"));
        assertTrue(rvVi.hasBeenDefined("3.0.0"));
    }

}
