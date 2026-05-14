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

package org.e2immu.language.inspection.openjdk.statement;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.inspection.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestTryResource extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              static class A implements AutoCloseable {
                  public void close() { }
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
        TypeInfo typeInfo = scan(Map.of("a.b.C", INPUT), List.of()).getFirst();
        MethodInfo main = typeInfo.findUniqueMethod("method", 2);
        if (main.methodBody().statements().getFirst() instanceof TryStatement tryStatement) {
            assertEquals(2, tryStatement.resources().size());
            // NOTE: we catch the ; in the 1st statement
            assertEquals("0+0@7:9-7:22", tryStatement.resources().getFirst().source().toString());
            assertEquals("0+1@7:24-7:24", tryStatement.resources().get(1).source().toString());
            if (tryStatement.block().statements().getFirst() instanceof ExpressionAsStatement eas) {
                if (eas.expression() instanceof MethodCall mc) {
                    assertEquals("a", mc.parameterExpressions().getFirst().toString());
                }
                assertEquals("0.0.0", eas.source().index());
            } else fail();
            assertEquals(1, tryStatement.catchClauses().size());
            assertEquals(1, tryStatement.catchClauses().getFirst().block().size());
            assertEquals(1, tryStatement.finallyBlock().size());

            assertEquals("""
                    try(A a=new A();b){System.out.println(a);}catch(Exception e){System.out.println("exception"+args[0]);}finally{System.out.println("bye");}\
                    """, tryStatement.toString());
        } else fail();
    }


}
