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

import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.TryStatement;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseTryResource extends CommonTestParse {

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
