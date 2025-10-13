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

import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ForEachStatement;
import org.e2immu.language.inspection.api.parser.Summary;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseForEach extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            // some comment
            class C {
              public static void main(String[] args) {
                for(String s: args) {
                  System.out.println(s);
                }
              }
            }
            """;
    @Language("java")
    private static final String INPUT_2 = """
            package a.b;
            class C {
              public String someMethod(String[] args) {
                for(String s: args) {
                  System.out.println(s);
                }
                return s; // should fail!
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof ForEachStatement forEach) {
            assertTrue(forEach.initializer().hasSingleDeclaration());
            assertEquals("s", forEach.initializer().localVariable().simpleName());
            assertTrue(forEach.initializer().localVariable().assignmentExpression().isEmpty());
            if (forEach.expression() instanceof VariableExpression ve) {
                assertEquals("args", ve.variable().simpleName());
            } else fail();
        } else fail();
    }

    @Test
    public void test2() {
        assertThrows(Summary.FailFastException.class, () -> parse(INPUT_2));
        Summary s = parseReturnContext(INPUT_2).summary();
        assertTrue(s.haveErrors());
    }
}
