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

package org.e2immu.parser.java;

import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.Summary;
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
                        Exception: org.e2immu.language.inspection.api.parser.Summary.ParseException
                        In: input
                        In: a.b.C.method1(String[])
                        Message: In: input
                        In: a.b.C.method1(String[])
                        Message: Unknown identifier 'arguments'\
                        """,
                s.parseExceptions().getFirst().getMessage());
    }
}
