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
 * CodeLaser/maddi#49: {@link FormattingOptions#bracesOnOwnLine()}. An Allman-style project (the brace of a body on
 * a line of its own) had nothing to map its style to: moved code always came out with the brace at the end of the
 * line, and {@code alwaysBreakPriorityBlocks} breaks after the brace, not before it.
 */
public class TestBracePlacement extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X
            {
                int m(int a)
                {
                    try
                    {
                        a = a / 2;
                    }
                    catch (ArithmeticException e)
                    {
                        a = 0;
                    }
                    finally
                    {
                        a++;
                    }
                    do
                    {
                        a--;
                    }
                    while (a > 10);
                    if (a > 0)
                    {
                        return a;
                    }
                    else
                    {
                        int[] values = { 1, 2 };
                        return values[0];
                    }
                }
                void empty() { }
            }
            """;

    private String print(TypeInfo typeInfo, boolean bracesOnOwnLine) {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4)
                .setBracesOnOwnLine(bracesOnOwnLine).build();
        return new Formatter2Impl(runtime, options).write(typeInfo.print(runtime.qualificationQualifyFromPrimaryType()));
    }

    @DisplayName("with bracesOnOwnLine, a body's brace starts a line, and so do else, catch, finally and while")
    @Test
    public void allman() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        assertEquals("""
                class X
                {
                    int m(int a)
                    {
                        try
                        {
                            a = a / 2;
                        }
                        catch (ArithmeticException e)
                        {
                            a = 0;
                        }
                        finally
                        {
                            a++;
                        }

                        do
                        {
                            a--;
                        }
                        while (a > 10);

                        if (a > 0)
                        {
                            return a;
                        }
                        else
                        {
                            int[] values = { 1, 2 };
                            return values[0];
                        }
                    }
                    void empty() { }
                }
                """, print(typeInfo, true));
    }

    @DisplayName("control: by default the brace ends the line")
    @Test
    public void endOfLine() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        String printed = print(typeInfo, false);
        assertEquals(true, printed.startsWith("class X {\n    int m(int a) {\n        try { a = a / 2; } catch"), printed);
    }
}
