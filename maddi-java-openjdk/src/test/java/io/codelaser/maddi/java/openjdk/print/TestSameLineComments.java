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
 * CodeLaser/maddi#137: a comment written after a statement, on its line, is printed there. Since #128 it belongs
 * to that statement, and {@code a(); // why} printed as {@code // why} above {@code a();}.
 */
public class TestSameLineComments extends CommonTest {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class X {
                int count(int n) {
                    int k = 0; // start
                    // own line
                    k++;
                    while (k < n) k++; // loop
                    if (k > 10) {
                        k = 10; // capped
                    }
                    return k; // done
                }
            }
            """;

    @DisplayName("a '//' comment after a statement stays after it")
    @Test
    public void test() {
        TypeInfo typeInfo = scan("a.b.X", INPUT);
        MethodInfo method = typeInfo.findUniqueMethod("count", 1);
        FormattingOptions options = new FormattingOptionsImpl.Builder().setLengthOfLine(120).setSpacesInTab(4)
                .build();
        // a body ending in a '//' comment cannot be inlined (the comment runs to the end of the line), so the while
        // is split, and as a split statement gets the formatter's blank lines around it
        assertEquals("""
                int count(int n) {
                    int k = 0; // start
                    // own line
                    k++;

                    while (k < n)
                        k++; // loop

                    if (k > 10) {
                        k = 10; // capped
                    }
                    return k; // done
                }
                """, new Formatter2Impl(runtime, options).write(method.print(runtime.qualificationQualifyFromPrimaryType())));
    }
}
