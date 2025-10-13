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

package org.e2immu.parser.java.util;

public class EscapeSequence {
    static char escapeSequence(char c2) {
        return switch (c2) {
            case '0' -> '\0';
            case 'b' -> '\b';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'n' -> '\n';
            case 'f' -> '\f';
            case 's' -> ' '; // does not seem to be official?
            case '\'' -> '\'';
            case '\\' -> '\\';
            case '"' -> '"';
            default -> {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static char escapeSequence(String c2) {
        char c = c2.charAt(0);
        if (c >= '0' && c <= '7') {
            StringBuilder sb = new StringBuilder();
            octal(c2.toCharArray(), sb, c, 0);
            return sb.charAt(0);
        }
        return escapeSequence(c);
    }

    // replace everything except for \<line terminator>
    public static String translateEscapeInTextBlock(String in) {
        StringBuilder sb = new StringBuilder(in.length());
        int i = 0;
        char[] chars = in.toCharArray();
        while (i < chars.length) {
            char c = chars[i];
            if (c == '\\' && i + 1 < chars.length) {
                char c2 = chars[i + 1];
                if (c2 != '\n') {
                    if (c2 >= '0' && c2 <= '7') {
                        i = octal(chars, sb, c2, i);
                    } else {
                        sb.append(escapeSequence(c2));
                        ++i;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
            ++i;
        }
        return sb.toString();
    }

    private static int octal(char[] chars, StringBuilder sb, char first, int start) {
        int i = start + 1;
        StringBuilder newSb = new StringBuilder();
        newSb.append(first);
        while (i < chars.length && chars[i] >= '0' && chars[i] <= '7') {
            newSb.append(chars[i]);
            ++i;
        }
        int value = Integer.parseInt(newSb.toString(), 8);
        sb.append((char) value);
        return i;
    }
}
