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

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseUnaryOperator extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            // some comment
            class C {
              int bitwise(int i) {
                return ~i;
              }
              int bitwise2(int i) {
                return ~~i;
              }
              byte bitwiseConstant() {
                return ~10;
              }
              int minus(int j) {
                return -j;
              }
              int minusConstant() {
                return -10;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);

        MethodInfo methodInfo = typeInfo.findUniqueMethod("bitwise", 1);
        Expression e1 = rv(methodInfo);
        if (e1 instanceof UnaryOperator un) {
            assertEquals("~i", un.toString());
            Expression eval = runtime.unaryOperator(un);
            assertInstanceOf(BitwiseNegation.class, eval);
        } else fail();

        MethodInfo methodInfo2 = typeInfo.findUniqueMethod("bitwise2", 1);
        Expression e2 = rv(methodInfo2);
        if (e2 instanceof UnaryOperator un1 && un1.expression() instanceof UnaryOperator un) {
            assertEquals("~~i", un1.toString());
            Expression eval = runtime.unaryOperator(un1);
            assertEquals("i", eval.toString());
        } else fail();

        MethodInfo methodInfo3 = typeInfo.findUniqueMethod("minus", 1);
        Expression e3 = rv(methodInfo3);
        if (e3 instanceof UnaryOperator un) {
            assertEquals("-j", un.toString());
            Expression eval = runtime.unaryOperator(un);
            assertInstanceOf(Negation.class, eval);
        } else fail();

        MethodInfo methodInfo4 = typeInfo.findUniqueMethod("bitwiseConstant", 0);
        Expression e4 = rv(methodInfo4);
        if (e4 instanceof UnaryOperator un) {
            assertEquals("~10", un.toString());
            Expression eval = runtime.unaryOperator(un);
            assertInstanceOf(BitwiseNegation.class, eval);
        } else fail();

        MethodInfo methodInfo5 = typeInfo.findUniqueMethod("minusConstant", 0);
        Expression e5 = rv(methodInfo5);
        if (e5 instanceof IntConstant ic) {
            assertEquals("-10", ic.toString());
        } else fail("We don't expect a unary operator here");
    }

    private static Expression rv(MethodInfo methodInfo) {
        if (methodInfo.methodBody().statements().getFirst() instanceof ReturnStatement rs) {
            return rs.expression();
        }
        throw new RuntimeException();
    }
}
