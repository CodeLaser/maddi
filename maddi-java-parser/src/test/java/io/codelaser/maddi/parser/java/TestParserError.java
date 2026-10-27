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

package io.codelaser.maddi.parser.java;

import io.codelaser.maddi.inspection.api.parser.Context;
import io.codelaser.maddi.inspection.api.parser.Summary;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParserError extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public static void method1(String[] args) {
                System.out.println(arguments.length);
              }
              public void method2(int i) {
                 // nothing wrong here
              }
            }
            """;

    @Test
    public void test() {
        assertThrows(Summary.FailFastException.class, () -> parse(INPUT));

        Context c = parseReturnContext(INPUT);
        Summary s = c.summary();
        assertTrue(s.haveErrors());
        assertEquals("""
                        Exception: io.codelaser.maddi.inspection.api.parser.Summary.ParseException
                        In: input:3-3
                        In: a.b.C.method1(String[])
                        Message: In: input:3-3
                        In: a.b.C.method1(String[])
                        Message: Unknown identifier 'arguments'\
                        """,
                s.parseExceptions().getFirst().getMessage());
    }
}
