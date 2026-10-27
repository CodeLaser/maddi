/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.element.DetailedSources;
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
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

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

            // the '=' of each declarator is nested as SUCCEEDING_EQUALS in that variable's name source
            assertEquals("@4:11-4:11", lvc.source().detailedSources().detail(lv)
                    .detailedSources().detail(DetailedSources.SUCCEEDING_EQUALS).toString());
            assertEquals("@4:28-4:28", lvc.source().detailedSources().detail(lv2)
                    .detailedSources().detail(DetailedSources.SUCCEEDING_EQUALS).toString());
        } else fail();
    }
}
