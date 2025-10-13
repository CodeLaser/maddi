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
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestAssignmentsExpression extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                public static int method(int value, byte[] pieces_bytes, int p) {
                    int pos = p;
                    if(value == -1) {
                        pieces_bytes[pos++] = pieces_bytes[pos++] = pieces_bytes[pos++] = pieces_bytes[pos++] =(byte)255;
                    } else {
                        pieces_bytes[pos++] =(byte)(value >> 24);
                        pieces_bytes[pos++] =(byte)(value >> 16);
                        pieces_bytes[pos++] =(byte)(value >> 8);
                        pieces_bytes[pos++] =(byte)(value);
                    }
                    return pos;
                }
            }
            """;

    @DisplayName("multiple assignments in one statement")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo method = X.findUniqueMethod("method", 3);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        IfElseStatement if1 = (IfElseStatement) method.methodBody().statements().get(1);
        VariableData vd100 = VariableDataImpl.of(if1.block().statements().get(0));
        VariableInfo viPos100 = vd100.variableInfo("pos");
        assertEquals("D:0, A:[0, 1.0.0, 1.0.0+0, 1.0.0+1, 1.0.0+2]", viPos100.assignments().toString());
    }


    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            import java.io.*;
            class X {
                int method(int[][] ints) {
                    int i = 3;
                    ints[i][i] = ints[++i][++i];
                    return ints[i][i];
                }
            }
            """;


    @DisplayName("multiple assignments in one statement, 2")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        MethodInfo method = X.findUniqueMethod("method", 1);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doMethod(method);
        Statement s1 = method.methodBody().statements().get(1);
        VariableData vd1i = VariableDataImpl.of(s1);
        VariableInfo vi1i = vd1i.variableInfo("i");
        assertEquals("D:0, A:[0, 1, 1+0]", vi1i.assignments().toString());
    }

}
