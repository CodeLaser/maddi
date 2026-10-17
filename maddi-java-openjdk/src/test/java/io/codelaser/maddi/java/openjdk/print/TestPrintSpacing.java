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
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.impl.info.ImportComputerImpl;
import io.codelaser.maddi.cst.print.FormattingOptionsImpl;
import io.codelaser.maddi.cst.print.formatter2.Formatter2Impl;
import io.codelaser.maddi.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CodeLaser/maddi#49, spacing: code the printer re-renders must not gain spaces the source did not have. Each line
 * below is printed as written, in nice and in compact mode; the compact one only loses the optional spaces.
 */
public class TestPrintSpacing extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.ArrayList;
            import java.util.Collections;
            import java.util.List;
            class X {
                static <T> int m(Object o, String datum, int a, int b, List<T>... lists) {
                    if (!datum.isEmpty()) {
                        return 0;
                    }
                    List<T> list = (List<T>) o;
                    List<T>[] arr = null;
                    double[][] grid = null;
                    List<T> copy = new ArrayList<T>(list);
                    List<T> none = Collections.<T>emptyList();
                    int c = a, d = b;
                    int m1 = a - -b, m2 = a + +b, m6 = ~a, m7 = (int) -a;
                    return lists.length;
                }
            }
            """;

    private static final List<String> LINES = List.of(
            "static <T> int m(Object o, String datum, int a, int b, List<T>... lists) {",
            "if (!datum.isEmpty()) { return 0; }",
            "List<T> list = (List<T>) o;",
            "List<T>[] arr = null;",
            "double[][] grid = null;",
            "List<T> copy = new ArrayList<T>(list);",
            "List<T> none = Collections.<T>emptyList();",
            "int c = a, d = b;",
            "int m1 = a - -b, m2 = a + +b, m6 = ~a, m7 = (int) -a;");

    @DisplayName("no space after '(' before a prefix operator, after a cast's or a call's type arguments, before"
                 + " an array suffix or varargs, or before the ';' of several declarators")
    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        OutputBuilder ob = runtime.newCompilationUnitPrinter(typeInfo.compilationUnit(), true)
                .print(new ImportComputerImpl(), runtime.qualificationQualifyFromPrimaryType());

        String nice = new Formatter2Impl(runtime, new FormattingOptionsImpl.Builder().setLengthOfLine(120)
                .setSpacesInTab(4).build()).write(ob);
        List<String> printed = nice.lines().map(String::strip).toList();
        for (String line : LINES) {
            assertTrue(printed.contains(line), "expected the line\n" + line + "\nin\n" + nice);
        }

        FormattingOptions compactOptions = new FormattingOptionsImpl.Builder().setLengthOfLine(400).setCompact(true)
                .build();
        String compact = new Formatter2Impl(runtime, compactOptions).write(ob);
        // the signs stay apart where touching would make another token: 'a--b' decrements
        assertTrue(compact.contains("a- -b") && compact.contains("a+ +b"), compact);
        assertEquals(-1, compact.indexOf("( !"), compact);
    }
}
