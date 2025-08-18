package org.e2immu.parser.java.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestEscapeSequence {

    @Test
    public void test() {
        char c = '\15';
        assertEquals(13, c);
        assertEquals("a b", EscapeSequence.translateEscapeInTextBlock("a\40b"));
        assertEquals("ab!", EscapeSequence.translateEscapeInTextBlock("ab\41"));
        assertEquals("\"ab", EscapeSequence.translateEscapeInTextBlock("\42ab"));
    }
}
