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

package io.codelaser.maddi.inspection.integration.java.other;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.statement.TryStatement;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
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
