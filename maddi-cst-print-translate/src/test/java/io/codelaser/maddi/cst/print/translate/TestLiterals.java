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
package io.codelaser.maddi.cst.print.translate;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLiterals {

    private static final Literals KOTLIN = new Literals("Double", "Float", "NaN", "POSITIVE_INFINITY",
            "NEGATIVE_INFINITY", Map.of('$', "\\$"));
    private static final Literals CSHARP = new Literals("double", "float", "NaN", "PositiveInfinity",
            "NegativeInfinity", Map.of('\f', "\\f", '\0', "\\0"));

    @Test
    public void strings() {
        String java = "a\"b\\c\nd$e\f\0" + (char) 0x7f;
        assertEquals("\"a\\\"b\\\\c\\nd\\$e\\u000c\\u0000\\u007f\"", KOTLIN.stringLiteral(java));
        assertEquals("\"a\\\"b\\\\c\\nd$e\\f\\0\\u007f\"", CSHARP.stringLiteral(java));
    }

    @Test
    public void chars() {
        assertEquals("'\\''", KOTLIN.charLiteral('\''));
        assertEquals("'\"'", KOTLIN.charLiteral('"'));
        assertEquals("'\\$'", KOTLIN.charLiteral('$'));
        assertEquals("'\\0'", CSHARP.charLiteral('\0'));
    }

    @Test
    public void floatingPoint() {
        assertEquals("1.0", KOTLIN.doubleLiteral(1));
        assertEquals("1.0E21", CSHARP.doubleLiteral(1e21));
        assertEquals("Double.NaN", KOTLIN.doubleLiteral(Double.NaN));
        assertEquals("double.NegativeInfinity", CSHARP.doubleLiteral(Double.NEGATIVE_INFINITY));
        assertEquals("Float.POSITIVE_INFINITY", KOTLIN.floatLiteral(Float.POSITIVE_INFINITY));
        assertEquals("2.5f", CSHARP.floatLiteral(2.5f));
    }
}
