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

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.SingleLineComment;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.inspection.openjdk.CommonTest;
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
        TypeInfo typeInfo = scan("a.b.C", INPUT2);

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
