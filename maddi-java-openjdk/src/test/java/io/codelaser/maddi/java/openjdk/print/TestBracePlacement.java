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
