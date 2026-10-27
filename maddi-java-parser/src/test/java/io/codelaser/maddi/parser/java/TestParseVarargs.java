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

import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.WhileStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseVarargs extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package org.e2immu.analyser.resolver.testexample;

            public class Varargs_0 {

                private static void method(int p1, String... args) {
                    System.out.println(p1 + " " + args.length);
                }

                public void test(String s) {
                    method(0);
                    method(1);
                    method(2, "");
                    method(3, "a");
                    method(2, "a", "b", "c");
                    method(4, new String[]{"a", "b"});
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo method = typeInfo.findUniqueMethod("method", 2);
        assertEquals("Type String[]", method.parameters().get(1).parameterizedType().toString());
        assertTrue(method.parameters().get(1).isVarArgs());
        MethodInfo test = typeInfo.findUniqueMethod("test", 1);
        for (int i = 0; i < 6; i++) {
            if (test.methodBody().statements().get(i) instanceof ExpressionAsStatement eas
                && eas.expression() instanceof MethodCall mc) {
                assertSame(method, mc.methodInfo());
            }
        }
    }
}
