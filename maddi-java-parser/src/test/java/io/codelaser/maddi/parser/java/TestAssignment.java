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
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExpressionAsStatement;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAssignment extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public int method(int j) {
                int i;
                i = 3;
                i += 2;
                i *= j;
                i ++;
                ++ i;
                return i;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("method", 1);
        Assignment a1 = assignment(methodInfo, 1);
        assertEquals("i=3", a1.toString());
        Assignment a2 = assignment(methodInfo, 2);
        assertEquals("i+=2", a2.toString());
        Assignment a3 = assignment(methodInfo, 3);
        assertEquals("i*=j", a3.toString());
        Assignment a4 = assignment(methodInfo, 4);
        assertEquals("i++", a4.toString());
        Assignment a5 = assignment(methodInfo, 5);
        assertEquals("++i", a5.toString());
    }

    private Assignment assignment(MethodInfo methodInfo, int statementIndex) {
        if (methodInfo.methodBody().statements().get(statementIndex) instanceof ExpressionAsStatement eas
            && eas.expression() instanceof Assignment a) return a;
        throw new UnsupportedOperationException();
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            class C {
              public int method(int j) {
                int i = i = j;
                return i;
              }
            }
            """;

    @Test
    public void test2() {
        parse(INPUT2);
    }
}
