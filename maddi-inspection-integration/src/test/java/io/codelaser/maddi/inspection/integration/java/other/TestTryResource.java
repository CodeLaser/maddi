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

package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.ExpressionAsStatement;
import org.e2immu.language.cst.api.statement.LocalVariableCreation;
import org.e2immu.language.cst.api.statement.TryStatement;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestTryResource extends CommonTest {

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import java.io.IOException;
            import java.io.OutputStreamWriter;
            public class X {
              public static void main(String[] args) {
                OutputStreamWriter out = null;
                try (out;
                    final OutputStreamWriter out2 = null) {
                  out.write(1);
                } catch (IOException ex) {
                  System.err.println(ex);
                }
              }
            }
            """;


    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("main", 1);
        TryStatement ts = (TryStatement) methodInfo.methodBody().statements().get(1);
        assertEquals(2, ts.resources().size());
        Element r0 = ts.resources().get(0);
        if (r0 instanceof ExpressionAsStatement eas && eas.expression() instanceof VariableExpression ve) {
            assertEquals("out", ve.variable().simpleName());
        } else fail();
        Element r1 = ts.resources().get(1);
        if (r1 instanceof LocalVariableCreation lvc) {
            assertEquals("out2", lvc.localVariable().simpleName());
            assertTrue(lvc.modifiers().contains(javaInspector.runtime().localVariableModifierFinal()));
        } else fail();
    }

    @Language("java")
    public static final String INPUT2 = """
            package a.b;
            public class Example2 {
                void test() {
                    try (final var a = lock()) {
                    } catch (Exception e) {
                    }
                }
            
                AutoCloseable lock() {
                    return null;
                }
            }
            """;


    @Test
    public void test2() {
        TypeInfo typeInfo = javaInspector.parse(INPUT2);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("test", 0);
        TryStatement ts = (TryStatement) methodInfo.methodBody().statements().get(0);
        assertEquals(1, ts.resources().size());
        Element r0 = ts.resources().get(0);
        if (r0 instanceof LocalVariableCreation lvc) {
            assertEquals("a", lvc.localVariable().simpleName());
            assertTrue(lvc.isVar());
            assertTrue(lvc.isFinal());
        } else fail();
    }
}
