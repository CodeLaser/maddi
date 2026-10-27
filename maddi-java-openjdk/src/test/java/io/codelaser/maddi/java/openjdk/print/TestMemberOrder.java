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
 * CodeLaser/maddi#137: the members of a type print in the order they were written. Fields, nested types,
 * constructors and methods came out grouped by kind, so a printed type moved its members around.
 */
public class TestMemberOrder extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                int a, b;
                void m() { }
                int c;
                X() { }
                static class Y { }
                void n() { }
                enum E { P, Q; int v; void e() { } }
            }
            """;

    @DisplayName("members in source order, enum constants first")
    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4)
                .build();
        String printed = new Formatter2Impl(runtime, options).write(typeInfo.print(runtime.qualificationQualifyFromPrimaryType()));
        assertEquals("class X { int a; int b; void m() { } int c; X() { } static class Y { } void n() { } "
                     + "enum E { P, Q; int v; void e() { } } }\n", printed);
    }

    @Language("java")
    private static final String COMMENTS = """
            package a.b;
            class X {
                int a; // first
                // own line
                int b;
                void m() { } // method
                int c;
            }
            """;

    @DisplayName("a '//' comment after a member stays after it")
    @Test
    public void comments() {
        TypeInfo typeInfo = scan("a.b.X", COMMENTS);
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4)
                .build();
        String printed = new Formatter2Impl(runtime, options).write(typeInfo.print(runtime.qualificationQualifyFromPrimaryType()));
        assertEquals("""
                class X {
                    int a; // first
                    // own line
                    int b;
                    void m() { } // method
                    int c;
                }
                """, printed);
    }
}
