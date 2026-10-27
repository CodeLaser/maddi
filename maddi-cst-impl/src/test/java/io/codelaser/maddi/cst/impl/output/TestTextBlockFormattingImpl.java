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

package io.codelaser.maddi.cst.impl.output;

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
