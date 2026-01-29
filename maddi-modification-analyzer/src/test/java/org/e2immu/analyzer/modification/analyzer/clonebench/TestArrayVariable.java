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

package org.e2immu.analyzer.modification.analyzer.clonebench;


import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class TestArrayVariable extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.lang.reflect.Array;
            public class C {
                public static void put(Object array, Object element, int index) {
                    if (array instanceof Object[]) {
                        ((Object[]) array)[index] = element;
                    } else {
                        Array.set(array, index, element);
                    }
                }
            }
            """;

    @DisplayName("array variable")
    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(C);
        analyzer.go(ao);
        MethodInfo put = C.findUniqueMethod("put", 3);
        ParameterInfo put0 = put.parameters().getFirst();

        Statement s000 = put.methodBody().statements().getFirst().block().statements().getFirst();
        VariableData vd000 = VariableDataImpl.of(s000);

        Variable av = ((DependentVariable) ((Assignment) s000.expression()).variableTarget()).arrayVariable();
        assertSame(put0, av);

        VariableInfo viPut0 = vd000.variableInfo(put0);
        assertEquals("0:array[2:index]←1:element,0:array∋1:element", viPut0.linkedVariables().toString());

        /*
        Even though Object is immutable, it is immutable HC, so it can be downcast.
        This downcast information is available.
        Because of the downcast, modifications may occur; they are registered.
        */
        assertTrue(put0.isModified());
        ValueImpl.VariableToTypeInfoSetImpl downcast = (ValueImpl.VariableToTypeInfoSetImpl) put0.analysis().
                getOrDefault(PropertyImpl.DOWNCAST_PARAMETER, ValueImpl.VariableToTypeInfoSetImpl.EMPTY);
        assertEquals("C.put(Object,Object,int):0:array->[java.lang.Object]", downcast.nice());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class X {
              public void method() {
                for (int i = 0; i < 8; i++) {
                  for (int id = 0; id < 8; id++) {
                    OldBoard[i][id][move] = TheBoard[i][id];
                  }
                }
              }
            
              int[][] TheBoard;
              int[][][] OldBoard;
              int move = 0;
            }
            """;

    @DisplayName("array variable")
    @Test
    public void test2() {
        TypeInfo C = javaInspector.parse(INPUT2);
        List<Info> ao = prepWork(C);
        analyzer.go(ao);
    }
}
