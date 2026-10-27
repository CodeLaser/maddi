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

import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ForEachStatement;
import io.codelaser.maddi.java.openjdk.CommonTest;
import io.codelaser.maddi.java.openjdk.CompilationProblems;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestForEach extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            // some comment
            class C {
              static void main(String[] args) {
                for(String s: args) {
                  System.out.println(s);
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.C", INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().getFirst() instanceof ForEachStatement forEach) {
            assertTrue(forEach.initializer().hasSingleDeclaration());
            assertEquals("5-9:5-16", forEach.initializer().source().compact2());
            assertEquals("s", forEach.initializer().localVariable().simpleName());
            assertTrue(forEach.initializer().localVariable().assignmentExpression().isEmpty());
            if (forEach.expression() instanceof VariableExpression ve) {
                assertEquals("args", ve.variable().simpleName());
            } else fail();
        } else fail();
    }

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
    public void test2() {
        assertThrows(CompilationProblems.class, () -> scan("a.b.C", INPUT_2));
    }
}
