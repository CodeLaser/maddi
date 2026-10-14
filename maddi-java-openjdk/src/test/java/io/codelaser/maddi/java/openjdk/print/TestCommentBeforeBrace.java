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
 * CodeLaser/maddi#49: a line comment between an {@code if}'s condition and its brace stays there. It moved to the
 * other side of the brace, and later to a line of its own between the condition and the brace.
 */
public class TestCommentBeforeBrace extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                Object read(String datum) {
                    if (datum.isEmpty()) // nothing to read
                    {
                        return null;
                    }
                    if (datum.isBlank())
                    // only blanks
                    {
                        return "";
                    }
                    return datum;
                }
            }
            """;

    private String print(TypeInfo typeInfo, boolean bracesOnOwnLine) {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4)
                .setAlwaysBreakPriorityBlocks(true).setBracesOnOwnLine(bracesOnOwnLine).build();
        return new Formatter2Impl(runtime, options).write(typeInfo.findUniqueMethod("read", 1)
                .print(runtime.qualificationQualifyFromPrimaryType()));
    }

    @DisplayName("the comment after the condition, on its line, stays there, in front of the brace")
    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        assertEquals("""
                Object read(String datum) {
                    if (datum.isEmpty()) // nothing to read
                    {
                        return null;
                    }
                    if (datum.isBlank())
                    // only blanks
                    {
                        return "";
                    }
                    return datum;
                }
                """, print(typeInfo, false).replace("\n\n", "\n"));
        assertEquals("""
                Object read(String datum)
                {
                    if (datum.isEmpty()) // nothing to read
                    {
                        return null;
                    }
                    if (datum.isBlank())
                    // only blanks
                    {
                        return "";
                    }
                    return datum;
                }
                """, print(typeInfo, true).replace("\n\n", "\n"));
    }
}
