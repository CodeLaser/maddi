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

import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.SwitchExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.SwitchStatementNewStyle;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A new-style switch statement and a switch expression are elements like any other: a predicate visitor is asked
 * about them, and can prune them. Both used to visit their selector and entries without testing themselves, so
 * `e instanceof SwitchExpression` never matched, and a visitor could not stop at a switch.
 */
public class TestSwitchIsVisited extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              void statement(int i) {
                switch (i) {
                  case 0 -> System.out.println("zero");
                  default -> System.out.println("other");
                }
              }
              int expression(int i) {
                return switch (i) { case 0 -> "a".length(); default -> 2; };
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo statement = typeInfo.findUniqueMethod("statement", 1);
        MethodInfo expression = typeInfo.findUniqueMethod("expression", 1);

        int[] seen = new int[2];
        statement.methodBody().visit(e -> {
            if (e instanceof SwitchStatementNewStyle) seen[0]++;
            return true;
        });
        expression.methodBody().visit(e -> {
            if (e instanceof SwitchExpression) seen[1]++;
            return true;
        });
        assertEquals(1, seen[0]);
        assertEquals(1, seen[1]);

        // pruning at the switch keeps its contents out of the visit
        int[] calls = new int[2];
        statement.methodBody().visit(e -> {
            if (e instanceof MethodCall) calls[0]++;
            return !(e instanceof SwitchStatementNewStyle);
        });
        expression.methodBody().visit(e -> {
            if (e instanceof MethodCall) calls[1]++;
            return !(e instanceof SwitchExpression);
        });
        assertEquals(0, calls[0]);
        assertEquals(0, calls[1]);
    }
}
