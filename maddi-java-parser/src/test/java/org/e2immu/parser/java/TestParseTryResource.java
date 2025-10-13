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

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseTryResource extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              class A implements AutoCloseable {
                  void close() { }
              }
              public static void method(String[] args, A b) {
                try(A a = new A(); b) {
                   System.out.println(a);
                } catch(Exception e) {
                   System.out.println("exception"+args[0]);
                } finally {
                   System.out.println("bye");
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("method", 2);
        if (main.methodBody().statements().get(0) instanceof TryStatement tryStatement) {
            assertEquals(2, tryStatement.resources().size());
            assertEquals("0+0@7:9-7:21", tryStatement.resources().get(0).source().toString());
            assertEquals("0+1@7:24-7:24", tryStatement.resources().get(1).source().toString());
            if (tryStatement.block().statements().get(0) instanceof ExpressionAsStatement eas) {
                if (eas.expression() instanceof MethodCall mc) {
                    assertEquals("a", mc.parameterExpressions().get(0).toString());
                }
                assertEquals("0.0.0", eas.source().index());
            } else fail();
            assertEquals(1, tryStatement.catchClauses().size());
            assertEquals(1, tryStatement.catchClauses().get(0).block().size());
            assertEquals(1, tryStatement.finallyBlock().size());

            assertEquals("""
                    try(A a=new A();b){System.out.println(a);}catch(Exception e){System.out.println("exception"+args[0]);}finally{System.out.println("bye");}\
                    """, tryStatement.toString());
        } else fail();
    }


}
