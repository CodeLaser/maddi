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
