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
