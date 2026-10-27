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

import java.util.Map;
import java.util.Objects;

/**
 * How a target language spells Java's string, char, double and float literals. The escapes of {@code \\}, newline,
 * carriage return, tab, backspace and the quote are the same in Java, Kotlin and C#; a language adds its own
 * ({@code extraEscapes}: Kotlin's {@code $}, which would start a template; C#'s {@code \f}, {@code \0}). Other control
 * characters, and surrogates, are written as a 4-digit unicode escape.
 *
 * @param doubleType       the receiver of the double constants ({@code Double}, {@code double})
 * @param floatType        the receiver of the float constants
 * @param nan              the name of the not-a-number constant
 * @param positiveInfinity the name of positive infinity
 * @param negativeInfinity the name of negative infinity
 * @param extraEscapes     the language's own escapes, by character
 */
public record Literals(String doubleType, String floatType, String nan, String positiveInfinity,
                       String negativeInfinity, Map<Character, String> extraEscapes) {

    public Literals {
        Objects.requireNonNull(extraEscapes);
    }

    public String stringLiteral(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) sb.append(escape(c, '"'));
        return sb.append('"').toString();
    }

    public String charLiteral(char c) {
        return "'" + escape(c, '\'') + "'";
    }

    private String escape(char c, char quote) {
        return switch (c) {
            case '\\' -> "\\\\";
            case '\n' -> "\\n";
            case '\r' -> "\\r";
            case '\t' -> "\\t";
            case '\b' -> "\\b";
            default -> {
                String extra = extraEscapes.get(c);
                if (extra != null) yield extra;
                if (c == quote) yield "\\" + c;
                if (c < 0x20 || (c >= 0x7f && c < 0xa0) || Character.isSurrogate(c)) {
                    yield String.format("\\u%04x", (int) c);
                }
                yield String.valueOf(c);
            }
        };
    }

    /** Java's {@code Double.toString} has a {@code .} or an exponent for every finite value, as both targets need. */
    public String doubleLiteral(double d) {
        if (Double.isNaN(d)) return doubleType + "." + nan;
        if (Double.isInfinite(d)) return doubleType + "." + (d > 0 ? positiveInfinity : negativeInfinity);
        return Double.toString(d);
    }

    public String floatLiteral(float f) {
        if (Float.isNaN(f)) return floatType + "." + nan;
        if (Float.isInfinite(f)) return floatType + "." + (f > 0 ? positiveInfinity : negativeInfinity);
        return Float.toString(f) + "f";
    }
}
