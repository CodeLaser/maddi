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
