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
