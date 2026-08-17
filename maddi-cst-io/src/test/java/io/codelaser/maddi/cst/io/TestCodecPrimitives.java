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

package org.e2immu.language.cst.io;

import org.e2immu.language.cst.api.analysis.Codec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Robustness tests for the scalar layer of {@link CodecImpl}: the pure quote/unquote helpers and the
 * boolean / int / string encode-decode round-trips, including edge cases (empty, special characters, integer
 * boundaries). All round-trips go encode -> toString -> JSON parse ({@code makeD}) -> decode, mirroring the
 * real write/read path.
 */
public class TestCodecPrimitives extends CommonTest {

    // ---- pure helpers -------------------------------------------------------

    @DisplayName("quote escapes embedded double quotes")
    @Test
    public void testQuote() {
        assertEquals("\"abc\"", CodecImpl.quote("abc"));
        assertEquals("\"\"", CodecImpl.quote(""));
        assertEquals("\"a\\\"b\"", CodecImpl.quote("a\"b"));
    }

    @DisplayName("unquote inverts quote, including embedded quotes and backslashes")
    @Test
    public void testQuoteUnquoteRoundTrip() {
        for (String s : new String[]{"", "a", "abc", "with spaces", "comma,semi;colon",
                "a\"b", "\"leading", "trailing\"", "two\"\"quotes", "λ-unicode-✓", "1.0.3", "T-prefixed",
                "back\\slash", "ends-with\\", "\\", "\\\\", "mix\\\"ed", "C:\\path\\to\\file", "tab\\tnotreal"}) {
            assertEquals(s, CodecImpl.unquote(CodecImpl.quote(s)), "round-trip failed for [" + s + "]");
        }
    }

    @DisplayName("backslash strings encode to valid JSON and survive a full parse round-trip")
    @Test
    public void testStringWithBackslash() {
        for (String s : new String[]{"back\\slash", "ends-with\\", "\\", "\\\\", "C:\\path", "mix\\\"ed"}) {
            Codec.EncodedValue ev = codec.encodeString(context, s);
            // the encoded form must be parseable JSON (previously a trailing backslash threw on parse)
            assertEquals(s, codec.decodeString(context, makeD(ev.toString())), "round-trip failed for [" + s + "]");
        }
    }

    @DisplayName("potentiallyUnquote only unquotes when actually quoted")
    @Test
    public void testPotentiallyUnquote() {
        assertEquals("abc", CodecImpl.potentiallyUnquote("abc"));
        assertEquals("abc", CodecImpl.potentiallyUnquote("\"abc\""));
        assertEquals("", CodecImpl.potentiallyUnquote(""));
        // a leading quote signals "quoted"; the content is then unwrapped
        assertEquals("a\"b", CodecImpl.potentiallyUnquote("\"a\\\"b\""));
    }

    // ---- boolean ------------------------------------------------------------

    @DisplayName("boolean encode/decode round-trip")
    @Test
    public void testBoolean() {
        for (boolean b : new boolean[]{true, false}) {
            Codec.EncodedValue ev = codec.encodeBoolean(context, b);
            assertEquals(Boolean.toString(b), ev.toString());
            assertEquals(b, codec.decodeBoolean(context, makeD(ev.toString())));
        }
    }

    // ---- int ----------------------------------------------------------------

    @DisplayName("int encode/decode round-trip, including boundaries")
    @Test
    public void testInt() {
        for (int i : new int[]{0, 1, -1, 42, -5, 1000000, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            Codec.EncodedValue ev = codec.encodeInt(context, i);
            assertEquals(Integer.toString(i), ev.toString());
            assertEquals(i, codec.decodeInt(context, makeD(ev.toString())));
        }
    }

    @DisplayName("decodeInt tolerates a quoted number (as produced for map keys)")
    @Test
    public void testDecodeQuotedInt() {
        assertEquals(123, codec.decodeInt(context, makeD("\"123\"")));
        assertEquals(-7, codec.decodeInt(context, makeD("\"-7\"")));
    }

    // ---- string -------------------------------------------------------------

    @DisplayName("string encode/decode round-trip, including empty and special characters")
    @Test
    public void testString() {
        for (String s : new String[]{"", "a", "abc", "with spaces", "comma,semi;colon",
                "a\"b", "two\"\"quotes", "λ-unicode-✓", "T-looks-like-prefix", "[]{}"}) {
            Codec.EncodedValue ev = codec.encodeString(context, s);
            assertEquals(s, codec.decodeString(context, makeD(ev.toString())), "round-trip failed for [" + s + "]");
        }
    }

    // ---- isList -------------------------------------------------------------

    @DisplayName("isList distinguishes arrays from scalars")
    @Test
    public void testIsList() {
        assertTrue(codec.isList(makeD("[1,2,3]")));
        assertTrue(codec.isList(makeD("[]")));
        assertFalse(codec.isList(makeD("\"abc\"")));
        assertFalse(codec.isList(makeD("42")));
    }

    @DisplayName("isList rejects an encoder-side value (E), expecting a decoder-side value (D)")
    @Test
    public void testIsListWrongSide() {
        Codec.EncodedValue e = codec.encodeInt(context, 1); // an E, not a D
        assertThrows(UnsupportedOperationException.class, () -> codec.isList(e));
    }
}
