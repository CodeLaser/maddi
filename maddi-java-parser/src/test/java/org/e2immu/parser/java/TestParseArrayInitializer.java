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

import org.e2immu.language.cst.api.expression.ArrayInitializer;
import org.e2immu.language.cst.api.expression.BinaryOperator;
import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.AssertStatement;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseArrayInitializer extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public void test(String s) {
                int[] a = {1, 2, 3};
                Object[] o = {1, s, "abc"};
                boolean[] b = new boolean[]{true, false, false, true};
                String[][] bs = new String[][] {{"a"}, {}};
                String[][] bs2 = new String[2][1];
                String[] bs3 = new String[]{};
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("test", 1);

        Block block = methodInfo.methodBody();
        assertEquals(6, block.size());
        if (block.statements().get(0) instanceof LocalVariableCreation lvc &&
            lvc.localVariable().assignmentExpression() instanceof ArrayInitializer ai) {
            assertTrue(ai.parameterizedType().copyWithOneFewerArrays().isInt());
            assertEquals("int[] a={1,2,3};", lvc.toString());
        } else fail();
        if (block.statements().get(1) instanceof LocalVariableCreation lvc &&
            lvc.localVariable().assignmentExpression() instanceof ArrayInitializer ai) {
            assertTrue(ai.parameterizedType().copyWithOneFewerArrays().isJavaLangObject());
            assertEquals("Object[] o={1,s,\"abc\"};", lvc.toString());
        } else fail();
        if (block.statements().get(2) instanceof LocalVariableCreation lvc &&
            lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            assertEquals(runtime.booleanParameterizedType(), cc.parameterizedType().copyWithOneFewerArrays());
            assertEquals("new boolean[]{true,false,false,true}", cc.toString());
            assertEquals(1, cc.constructor().parameters().size());
        } else fail();
        if (block.statements().get(3) instanceof LocalVariableCreation lvc &&
            lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            assertNotNull(cc.arrayInitializer());
            assertEquals(2, cc.parameterizedType().arrays());
            assertEquals(runtime.stringParameterizedType(), cc.parameterizedType().copyWithoutArrays());
            assertEquals("new String[][]{{\"a\"},{}}", cc.toString());
            assertEquals(2, cc.parameterExpressions().size());
            assertTrue(cc.parameterExpressions().get(0).isEmpty());
            assertTrue(cc.parameterExpressions().get(1).isEmpty());
        } else fail();
        if (block.statements().get(4) instanceof LocalVariableCreation lvc &&
            lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            assertNull(cc.arrayInitializer());
            assertEquals(2, cc.parameterizedType().arrays());
            assertEquals(runtime.stringParameterizedType(), cc.parameterizedType().copyWithoutArrays());
            assertEquals("new String[2][1]", cc.toString());
        } else fail();
        if (block.statements().get(5) instanceof LocalVariableCreation lvc &&
            lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            assertEquals("{}", cc.arrayInitializer().toString());
            assertEquals(1, cc.parameterizedType().arrays());
            assertEquals(runtime.stringParameterizedType(), cc.parameterizedType().copyWithoutArrays());
            assertEquals("new String[]{}", cc.toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
                package org.e2immu.analyser.resolver.testexample;


            public class ArrayInitializer_1 {

                interface R {
                    String get(int i);
                }

                String[] method1(R rst) {
                    return new String[]{rst.get(1), rst.get(2), rst.get(3)};
                }

                Object[] method2(long supplierId, String invoiceNumber, long invoiceType) {
                    Object[] params = {Long.valueOf(supplierId), invoiceNumber, Long.valueOf(invoiceType)};
                    return params;
                }
            }
            """;

    @Test
    public void test2() {
        parse(INPUT2);
    }

    @Language("java")
    private static final String INPUT3 = """
            package org.e2immu.analyser.resolver.testexample;

            public class ArrayInitializer_0 {
                boolean[][] patterns = {{true, true, true, true, true}, {true, true, true, false, true},
                        {true, true, true, true, false}, {true, true, true, false, false}};
            }""";

    @Test
    public void test3() {
        parse(INPUT3);
    }


    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            public class X {
                interface Writer {
                    void write(byte[] bytes);
                    void write(char[] chars);
                    void write(int[] chars);
                }
                void method(Writer writer) {
                    writer.write(new byte[] { 'a', 'b', 'c'});
                    writer.write(new char[] { 'a', 'b', 'c'});
                    writer.write(new int[] { 'a', 'b', 'c'});
                }
            }
            """;

    @Test
    public void test4() {
        TypeInfo typeInfo = parse(INPUT4);
        MethodInfo method = typeInfo.findUniqueMethod("method", 1);
        ExpressionAsStatement eas0 = (ExpressionAsStatement) method.methodBody().statements().get(0);
        assertEquals("writer.write(new byte[]{'a','b','c'});", eas0.toString());
        ExpressionAsStatement eas1 = (ExpressionAsStatement) method.methodBody().statements().get(1);
        assertEquals("writer.write(new char[]{'a','b','c'});", eas1.toString());
        ExpressionAsStatement eas2 = (ExpressionAsStatement) method.methodBody().statements().get(2);
        assertEquals("writer.write(new int[]{'a','b','c'});", eas2.toString());
    }


}
