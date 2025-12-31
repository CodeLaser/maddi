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

package org.e2immu.parser.java;

import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.ArrayLength;
import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseLocalVariable extends CommonTestParse {

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
        TypeInfo typeInfo = parse(INPUT);
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
        TypeInfo typeInfo = parse(INPUT2, true);
        assertEquals("C", typeInfo.simpleName());
        MethodInfo methodInfo = typeInfo.methods().getFirst();
        assertEquals("length", methodInfo.name());

        Block block = methodInfo.methodBody();
        if (block.statements().getFirst() instanceof LocalVariableCreation lvc) {
            LocalVariable lv = lvc.localVariable();
            assertFalse(lvc.hasSingleDeclaration());
            assertFalse(lvc.otherLocalVariables().isEmpty());
            assertEquals("a", lv.fullyQualifiedName());
            if (lv.assignmentExpression() instanceof ArrayLength al) {
                if (al.scope() instanceof VariableExpression ve) {
                    assertEquals("args", ve.variable().simpleName());
                    assertEquals("Type String[]", ve.variable().parameterizedType().toString());
                } else fail();
            } else fail();
            assertEquals("args.length", lv.assignmentExpression().toString());

            LocalVariable lv2 = lvc.otherLocalVariables().getFirst();
            assertEquals("b", lv2.simpleName());

            List<Source> sources = lvc.source().detailedSources().details(DetailedSources.LOCAL_VARIABLE_ASSIGNMENT_OPERATORS);
            assertEquals(2, sources.size());
            assertEquals("@4:11-4:11", sources.getFirst().toString());
            assertEquals("@4:28-4:28", sources.getLast().toString());
        } else fail();
    }
}
