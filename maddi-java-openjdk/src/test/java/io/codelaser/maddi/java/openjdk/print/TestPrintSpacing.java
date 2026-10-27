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
