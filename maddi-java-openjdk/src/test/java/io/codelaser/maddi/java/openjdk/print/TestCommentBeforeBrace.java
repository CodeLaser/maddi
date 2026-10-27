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
