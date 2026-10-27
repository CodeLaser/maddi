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

package io.codelaser.maddi.java.openjdk.print;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.FormattingOptions;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CodeLaser/maddi#137: a lambda body written as a block keeps its braces. {@code () -> { a(); }} printed as
 * {@code () -> a()}, and {@code x -> { return x; }} as {@code x -> x}.
 */
public class TestLambdaBlockBody extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class X {
                void method() {
                    Runnable block = () -> { a(); };
                    Runnable expression = () -> a();
                    Function<String, String> returns = s -> { return s; };
                    Function<String, String> identity = s -> s;
                }
                void a() { }
            }
            """;

    @DisplayName("a block body keeps its braces, an expression body stays an expression")
    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        MethodInfo method = typeInfo.findUniqueMethod("method", 0);
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4)
                .build();
        assertEquals("""
                void method() {
                    Runnable block = () -> { a(); };
                    Runnable expression = () -> a();
                    Function<String, String> returns = s -> { return s; };
                    Function<String, String> identity = s -> s;
                }
                """, new Formatter2Impl(runtime, options).write(method.print(runtime.qualificationQualifyFromPrimaryType())));
    }
}
