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

package org.e2immu.analyzer.modification.linkedvariables.staticvalues;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
import org.e2immu.analyzer.modification.linkedvariables.lv.StaticValuesImpl;
import org.e2immu.analyzer.modification.prepwork.variable.StaticValues;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestStaticValuesMerge extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                void method(int[] array) {
                    int y = array[0];
                    if(array[1]>10) {
                        y += 9;
                    }
                    System.out.println(array[0] +" ? "+y);
                    array[0] = y;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 1);
        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().get(0));
        VariableInfo vi0y = vd0.variableInfo("y");
        assertEquals("E=array[0]", vi0y.staticValues().toString());

        VariableData vd100 = VariableDataImpl.of(method.methodBody().statements().get(1).block().statements().get(0));
        VariableInfo vi100y = vd100.variableInfo("y");
        assertTrue(vi100y.staticValues().multipleExpressions());

        VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
        VariableInfo vi1y = vd1.variableInfo("y");
        assertTrue(vi1y.staticValues().multipleExpressions());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class X {
                void method(int[] array) {
                    int y = array[0];
                    if(array[1]>10) {
                        y += 9 * array[3] - array[4];
                    }
                    System.out.println(array[0] +" ? "+y);
                    array[0] = y;
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        MethodInfo method = X.findUniqueMethod("method", 1);
        VariableData vd0 = VariableDataImpl.of(method.methodBody().statements().get(0));
        VariableInfo vi0y = vd0.variableInfo("y");
        assertEquals("E=array[0]", vi0y.staticValues().toString());

        VariableData vd100 = VariableDataImpl.of(method.methodBody().statements().get(1).block().statements().get(0));
        VariableInfo vi100y = vd100.variableInfo("y");
        assertTrue(vi100y.staticValues().multipleExpressions());

        VariableData vd1 = VariableDataImpl.of(method.methodBody().statements().get(1));
        VariableInfo vi1y = vd1.variableInfo("y");
        assertTrue(vi1y.staticValues().multipleExpressions());
    }

}
