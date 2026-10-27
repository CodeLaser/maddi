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

package io.codelaser.maddi.inspection.integration.java.print;

import io.codelaser.maddi.cst.api.expression.TextBlock;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestTextBlockFormatting extends CommonTest {


    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            public class X {
                public void method() {
                    String s = \"""
                        abc\\
                        def
                        123
                        \""";
                }
            }
            """;
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        MethodInfo methodInfo = X.findUniqueMethod("method", 0);
        LocalVariableCreation lvc  = (LocalVariableCreation)  methodInfo.methodBody().statements().get(0);
        TextBlock tb = (TextBlock) lvc.localVariable().assignmentExpression();
        assertNotNull(tb.textBlockFormatting());
        String s = javaInspector.print2(X.compilationUnit());
        @Language("java")
        String expect = """
            package a.b;
            public class X {
                public void method() {
                    String s = \"""
                            abc\\
                            def
                            123
                            \""";
                }
            }
            """;
        assertEquals(expect, s);
    }

}
