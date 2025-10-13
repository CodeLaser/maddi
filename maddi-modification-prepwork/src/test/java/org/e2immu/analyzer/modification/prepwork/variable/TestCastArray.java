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
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.Cast;
import org.e2immu.language.cst.api.expression.EnclosedExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestCastArray extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            import java.lang.reflect.Array;
            
            public class X {
            
                public static void put(Object array, Object element, int index) {
                    try {
                        if (array instanceof Object[]) {
                            try {
                                ((Object[]) array)[index] = element;
                            } catch (ArrayStoreException e) {
                                throw new IllegalArgumentException();
                            }
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new ArrayIndexOutOfBoundsException(index + " into " + Array.getLength(array));
                    }
                }
            }
            """;

    @DisplayName("variable should exist")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT);
        PrepAnalyzer analyzer = new PrepAnalyzer(runtime);
        analyzer.doPrimaryType(X);
        MethodInfo put = X.findUniqueMethod("put", 3);
        ParameterInfo p0 = put.parameters().get(0);
        Statement s000 = put.methodBody().statements().get(0).block().statements().get(0);
        Statement s00000 = s000.block().statements().get(0);
        Statement s000000 = s00000.block().statements().get(0);
        if (s000000 instanceof ExpressionAsStatement eas) {
            if (eas.expression() instanceof Assignment a) {
                if (a.variableTarget() instanceof DependentVariable dv) {
                    assertTrue(dv.arrayExpression() instanceof EnclosedExpression ee && ee.expression() instanceof Cast);
                    assertSame(p0, dv.arrayVariable());
                } else fail();
            } else fail();
        } else fail();

        VariableData vd000000 = VariableDataImpl.of(s000000);
        VariableInfo vi000000av = vd000000.variableInfo(p0);
        assertEquals("D:-, A:[]", vi000000av.assignments().toString());
        assertEquals("0.0.0-E, 0.0.0.0.0.0.0", vi000000av.reads().toString());
        assertEquals("""
                X.put(Object,Object,int):0:array, X.put(Object,Object,int):0:array[X.put(Object,Object,int):2:index], \
                X.put(Object,Object,int):1:element, X.put(Object,Object,int):2:index\
                """, vd000000.knownVariableNamesToString());
    }
}