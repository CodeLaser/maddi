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
