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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.cst.api.element.RecordPattern;
import io.codelaser.maddi.cst.api.expression.SwitchExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.SwitchEntry;
import io.codelaser.maddi.cst.api.statement.SwitchStatementNewStyle;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Language("java")
    private static final String INPUT4 = """
            package a.b;
            class C {
              sealed interface S permits A, B, D {}
              record A() implements S {}
              record B() implements S {}
              record D() implements S {}
              public static int method(S s) {
                return switch(s) {
                  case A _, B _ -> 1;
                  case D _ -> 2;
                };
              }
            }
            """;

    // 'case A _, B _ ->' (JEP 456): every pattern is part of the entry, in source order. The parser set each one in
    // turn as THE pattern, so the entry kept B only: A was neither referenced nor walked, nor printed.
    @Test
    public void test4() {
        TypeInfo typeInfo = parse(INPUT4);
        MethodInfo main = typeInfo.findUniqueMethod("method", 1);
        if (main.methodBody().statements().getFirst() instanceof ReturnStatement rs
            && rs.expression() instanceof SwitchExpression se) {
            SwitchEntry entry = se.entries().getFirst();
            assertEquals("A", entry.patternVariable().parameterizedType().typeInfo().simpleName());
            assertEquals(List.of("B"), entry.otherPatterns().stream()
                    .map(p -> p.parameterizedType().typeInfo().simpleName()).toList());
            assertEquals(List.of("A", "B"), entry.typesReferenced(null).map(r -> r.typeInfo().simpleName())
                    .filter(n -> n.length() == 1).distinct().sorted().toList());
            List<String> visited = new ArrayList<>();
            se.visit(e -> {
                if (e instanceof RecordPattern rp && rp.localVariable() != null) {
                    visited.add(rp.localVariable().parameterizedType().typeInfo().simpleName());
                }
                return true;
            });
            assertEquals(List.of("A", "B", "D"), visited);
        } else fail();
    }
}
