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
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CodeLaser/maddi#49: a body written without braces prints without them. A braceless {@code if} came out as
 * {@code if (c) { throw ...; }}, and {@code else if} as {@code else { if ... }}, one level deeper.
 */
public class TestBracelessBodies extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                int forms(int n, String s) {
                    if (s.isEmpty())
                        throw new IllegalArgumentException(s);
                    for (int i = 0; i < n; i++)
                        n--;
                    while (n > 10)
                        n /= 2;
                    if (n == 1)
                        return 1;
                    else if (n == 2)
                        return 2;
                    else
                        return 3;
                }
                void danglingElse(boolean a, boolean b) {
                    if (a)
                        x();
                    else
                        y();
                    if (b)
                        x();
                }
                void x() { }
                void y() { }
            }
            """;

    private String print(MethodInfo methodInfo) {
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4)
                .setAlwaysBreakPriorityBlocks(true).build();
        return new Formatter2Impl(runtime, options).write(methodInfo.print(runtime.qualificationQualifyFromPrimaryType()));
    }

    @DisplayName("if, else, else if, for and while bodies without braces print without them")
    @Test
    public void asWritten() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        assertEquals("""
                int forms(int n, String s) {
                    if (s.isEmpty())
                        throw new IllegalArgumentException(s);

                    for (int i = 0; i < n; i++)
                        n--;

                    while (n > 10)
                        n /= 2;

                    if (n == 1)
                        return 1;
                    else if (n == 2)
                        return 2;
                    else
                        return 3;
                }
                """, print(typeInfo.findUniqueMethod("forms", 2)));
    }

    @DisplayName("a body that fits on the line keeps the space in front of it")
    @Test
    public void onOneLine() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4).build();
        String printed = new Formatter2Impl(runtime, options).write(typeInfo.findUniqueMethod("forms", 2)
                .print(runtime.qualificationQualifyFromPrimaryType()));
        assertEquals("""
                int forms(int n, String s) {
                    if (s.isEmpty()) throw new IllegalArgumentException(s);
                    for (int i = 0; i < n; i++) n--;
                    while (n > 10) n /= 2;
                    if (n == 1) return 1; else if (n == 2) return 2; else return 3;
                }
                """, printed);
    }

    @DisplayName("⛔ a then-part rewritten into an 'if' without 'else' gets braces: the 'else' must keep its 'if'")
    @Test
    public void danglingElse() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        MethodInfo danglingElse = typeInfo.findUniqueMethod("danglingElse", 2);
        Statement first = danglingElse.methodBody().statements().getFirst();
        Statement openIf = danglingElse.methodBody().statements().get(1);
        Statement callX = first.block().statements().getFirst();
        TranslationMap tm = runtime.newTranslationMapBuilder().put(callX, openIf).build();
        Statement rewritten = first.translate(tm).getFirst();

        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4)
                .setAlwaysBreakPriorityBlocks(true).build();
        assertEquals("""
                if (a) {
                    if (b)
                        x();
                } else
                    y();
                """, new Formatter2Impl(runtime, options)
                .write(rewritten.print(runtime.qualificationQualifyFromPrimaryType())));
    }
}
