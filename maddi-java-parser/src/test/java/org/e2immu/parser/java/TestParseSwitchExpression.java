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

import org.e2immu.language.cst.api.expression.SwitchExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.ReturnStatement;
import org.e2immu.language.cst.api.statement.SwitchStatementNewStyle;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestParseSwitchExpression extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public static int method(String[] args) {
                return switch(args.length) {
                  case 0 -> -1;
                  case 1, 2 -> {
                    System.out.println("less than 3");
                    yield -2;
                  }
                  default ->
                  // noinspection ALL
                  {
                    System.out.println("all the rest");
                    yield 1;
                  }
                };
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("method", 1);
        if (main.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof SwitchExpression se) {
            assertEquals("""
                            switch(args.length){case 0->-1;case 1,2->{System.out.println("less than 3");yield -2;}default->// noinspection ALL
                            
                            {System.out.println("all the rest");yield 1;}}\
                            """,
                    se.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class SwitchExpression_0 {
                public String method(int i) {
                    return switch (i) {
                        case 0 -> "0";
                        case 1, 2 -> {
                            //noinspection ALL
                            yield "a";
                        }
                        case 3 -> {
                            {
                                yield "b";
                            }
                        }
                        default -> throw new RuntimeException();
                    };
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = parse(INPUT2);
        MethodInfo main = typeInfo.findUniqueMethod("method", 1);
        if (main.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof SwitchExpression se) {
            assertEquals("""
                            switch(i){case 0->"0";case 1,2->{//noinspection ALL
                            
                            yield "a";}case 3->{{yield "b";}}default->throw new RuntimeException();}\
                            """,
                    se.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT3 = """
            package a.b;
            public class SwitchExpression_1 {
            
                enum Property {P1, P2, P3, P4, P5, P6}
            
                String method(Property p, int i) {
                    if (i < 0) return "negative";
                    return switch (p) {
                        case P1 -> {
                            System.out.println(i);
                            if (i > 10) {
                                yield "y";
                            }
                            System.out.println("less than 10");
                            yield 10 + "?" + i;
                        }
                        case P2 -> {
                            System.out.println("less than 10");
                            yield 10 + "?" + i;
                        }
                        case P3 -> "hello";
                        default -> "x";
                    };
                }
            }
            """;

    @Test
    public void test3() {
        TypeInfo typeInfo = parse(INPUT3);
        MethodInfo main = typeInfo.findUniqueMethod("method", 2);
        if (main.methodBody().statements().get(1) instanceof ReturnStatement rs
            && rs.expression() instanceof SwitchExpression se) {
            assertEquals("""
                            switch(p){\
                            case P1->{System.out.println(i);if(i>10){yield "y";}System.out.println("less than 10");yield 10+"?"+i;}\
                            case P2->{System.out.println("less than 10");yield 10+"?"+i;}\
                            case P3->"hello";\
                            default->"x";}\
                            """,
                    se.print(runtime.qualificationQualifyFromPrimaryType()).toString());
        } else fail();
    }
}
