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

package io.codelaser.maddi.parser.java.util;

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

    // A unicode escape (JLS 3.3) is a lexer-level construct, so it normally never reaches this class.
    // It does when it survives into a text block's raw token, which is how 30 questdb compilation units
    // ended up throwing UnsupportedOperationException here.
    @Test
    public void testUnicodeEscapeInTextBlock() {
        assertEquals("aAb", EscapeSequence.translateEscapeInTextBlock("a\\u0041b"));
    }

    @Test
    public void testUnicodeEscapeRepeatedU() {
        assertEquals("aAb", EscapeSequence.translateEscapeInTextBlock("a\\uuu0041b"));
    }

    // an escaped backslash is consumed first, so the following u is ordinary text, not an escape
    @Test
    public void testEscapedBackslashBeforeU() {
        assertEquals("a\\u0041b", EscapeSequence.translateEscapeInTextBlock("a\\\\u0041b"));
    }

    // questdb's shape: a surrogate pair inside expected query output
    @Test
    public void testUnicodeEscapeSurrogatePair() {
        assertEquals("ab\uDB47\uDD9Ccd",
                EscapeSequence.translateEscapeInTextBlock("ab\\uDB47\\uDD9Ccd"));
    }

    // the other caller: ParseExpression hands a character literal's source after the backslash
    @Test
    public void testUnicodeEscapeInCharacterLiteral() {
        assertEquals('A', EscapeSequence.escapeSequence("u0041'"));
    }
}
