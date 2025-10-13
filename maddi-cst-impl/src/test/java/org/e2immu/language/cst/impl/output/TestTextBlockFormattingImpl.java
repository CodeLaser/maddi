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

package org.e2immu.language.cst.impl.output;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTextBlockFormattingImpl {

    String s1 = """
            abc
              def
            ghi""";

    @Test
    public void test1() {
        assertEquals("abc\n  def\nghi", s1);
    }

    String s2 = """
            abc
              def
            ghi\
            """;

    @Test
    public void test2() {
        assertEquals(s1, s2);
    }

    String s3 = """
            abc
              def
          ghi""";

    @Test
    public void test3() {
        assertEquals("  abc\n    def\nghi", s3);
    }

    String s4 = """
            abc
       def
          ghi""";

    @Test
    public void test4() {
        assertEquals("     abc\ndef\n   ghi", s4);
    }


    String s5 = """
        abc

        def
        
        ghi
        """;

    @DisplayName("blank lines become empty")
    @Test
    public void test5() {
        assertEquals("abc\n\ndef\n\nghi\n", s5);
    }
}
