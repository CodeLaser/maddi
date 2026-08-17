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

package io.codelaser.maddi.java.openjdk.statement;

import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.ArrayLength;
import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.IntConstant;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestLocalVariable extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public int length(String[] args) {
                int a = args.length;
                return a+1;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        assertEquals("C", typeInfo.simpleName());
        MethodInfo methodInfo = typeInfo.methods().getFirst();
        assertEquals("length", methodInfo.name());
        assertEquals("a.b.C.length(String[])", methodInfo.fullyQualifiedName());

        Block block = methodInfo.methodBody();
        assertEquals(2, block.size());
        if (block.statements().get(0) instanceof LocalVariableCreation lvc) {
            LocalVariable lv = lvc.localVariable();
            assertTrue(lvc.hasSingleDeclaration());
            assertTrue(lvc.otherLocalVariables().isEmpty());
            assertEquals("a", lv.fullyQualifiedName());
            if (lv.assignmentExpression() instanceof ArrayLength al) {
                if (al.scope() instanceof VariableExpression ve) {
                    assertEquals("args", ve.variable().simpleName());
                    assertEquals("Type String[]", ve.variable().parameterizedType().toString());
                } else fail();
            } else fail();
            assertEquals("args.length", lv.assignmentExpression().toString());
        } else fail();
        if (block.statements().get(1) instanceof ReturnStatement rs) {
            if (rs.expression() instanceof BinaryOperator plus) {
                assertSame(runtime.plusOperatorInt(), plus.operator());
                if (plus.lhs() instanceof VariableExpression ve) {
                    assertEquals("a", ve.variable().simpleName());
                } else fail();
                if (plus.rhs() instanceof IntConstant i) {
                    assertEquals(1, i.constant());
                } else fail();
            } else fail("Have " + rs.expression().getClass());
            assertEquals("return a+1;", rs.toString());
        } else fail();
        assertEquals(1, methodInfo.parameters().size());
        ParameterInfo pi = methodInfo.parameters().getFirst();
        assertEquals("args", pi.name());
        ParameterizedType pt = pi.parameterizedType();
        assertEquals(1, pt.arrays());
    }


    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class C {
              public int length(String[] args) {
                int a = args.length, b = 3;
                return a+1;
              }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = scan("a.b.C", INPUT2);
        assertEquals("C", typeInfo.simpleName());
        MethodInfo methodInfo = typeInfo.methods().getFirst();
        assertEquals("length", methodInfo.name());

        Block block = methodInfo.methodBody();
        assertEquals(2, block.size());
        if (block.statements().getFirst() instanceof LocalVariableCreation lvc) {
            LocalVariable lv = lvc.localVariable();
            assertFalse(lvc.hasSingleDeclaration());
            assertFalse(lvc.otherLocalVariables().isEmpty());
            assertEquals("a", lv.fullyQualifiedName());
            Source aName = lvc.source().detailedSources().detail(lv.simpleName());
            assertEquals("4-9:4-9", aName.compact2());
            // the '=' and the surrounding commas are recorded per variable, nested in the variable's name source
            assertEquals("4-11:4-11", aName.detailedSources().detail(DetailedSources.SUCCEEDING_EQUALS).compact2());
            assertNull(aName.detailedSources().detail(DetailedSources.PRECEDING_COMMA));
            assertEquals("4-24:4-24", aName.detailedSources().detail(DetailedSources.SUCCEEDING_COMMA).compact2());

            if (lv.assignmentExpression() instanceof ArrayLength al) {
                if (al.scope() instanceof VariableExpression ve) {
                    assertEquals("args", ve.variable().simpleName());
                    assertEquals("Type String[]", ve.variable().parameterizedType().toString());
                } else fail();
            } else fail();
            assertEquals("args.length", lv.assignmentExpression().toString());

            LocalVariable lv2 = lvc.otherLocalVariables().getFirst();
            assertEquals("b", lv2.simpleName());
            Source bName = lvc.source().detailedSources().detail(lv2.simpleName());
            assertEquals("4-26:4-26", bName.compact2());
            assertEquals("4-28:4-28", bName.detailedSources().detail(DetailedSources.SUCCEEDING_EQUALS).compact2());
            assertEquals("4-24:4-24", bName.detailedSources().detail(DetailedSources.PRECEDING_COMMA).compact2());
            assertNull(bName.detailedSources().detail(DetailedSources.SUCCEEDING_COMMA));
        } else fail();
    }
}
