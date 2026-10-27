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

package io.codelaser.maddi.java.openjdk.statement;

import io.codelaser.maddi.cst.api.element.Comment;
import io.codelaser.maddi.cst.api.element.SingleLineComment;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.statement.TryStatement;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestTryCatch extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              static void main(String[] args) {
                try {
                   System.out.println(args[0]);
                } catch(Exception e) {
                   System.out.println("exception "+e);
                } finally {
                   System.out.println("bye");
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().getFirst() instanceof TryStatement tryStatement) {
            assertTrue(tryStatement.resources().isEmpty());
            if (tryStatement.block().statements().getFirst() instanceof ExpressionAsStatement eas) {
                if (eas.expression() instanceof MethodCall mc) {
                    assertEquals("args[0]", mc.parameterExpressions().getFirst().toString());
                    if (mc.parameterExpressions().getFirst() instanceof VariableExpression ve
                        && ve.variable() instanceof DependentVariable dv) {
                        assertEquals("args", dv.arrayVariable().simpleName());
                    } else fail();
                }
            } else fail();
            assertEquals(1, tryStatement.catchClauses().size());
            assertEquals(1, tryStatement.catchClauses().getFirst().block().size());
            assertEquals(1, tryStatement.finallyBlock().size());

            assertEquals("""
                    try{System.out.println(args[0]);}catch(Exception e){System.out.println("exception "+e);}finally{System.out.println("bye");}\
                    """, tryStatement.toString());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.io.IOException;
            import java.io.Writer;
            class X {
                public void method(String in, Writer writer) {
                    try { //
                        writer.append("input: ").append(in);
                    } catch (IOException | AssertionError e) {
                        System.out.println("Caught io or runtime exception!");
                        throw new RuntimeException(e);
                    } finally {
                        System.out.println("this was method1");
                    }
                }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = scan("a.b.X", INPUT2);

        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 2);
        TryStatement ts = (TryStatement) methodInfo.methodBody().statements().getFirst();
        Statement s0 = ts.block().statements().getFirst();
        assertEquals(1, s0.comments().size());
        Comment c = s0.comments().getFirst();
        if (c instanceof SingleLineComment slc) {
            assertEquals("", slc.comment());
        } else fail();
        TryStatement.CatchClause cc = ts.catchClauses().getFirst();
        assertEquals("e", cc.catchVariable().simpleName());
        assertEquals("Type Throwable", cc.catchVariable().parameterizedType().toString());
        assertEquals(2, cc.exceptionTypes().size());
    }
}
