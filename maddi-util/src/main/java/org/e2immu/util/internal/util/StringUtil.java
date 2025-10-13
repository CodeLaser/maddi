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

package org.e2immu.util.internal.util;

import org.e2immu.annotation.NotNull;

public class StringUtil {

    private StringUtil() {
        throw new UnsupportedOperationException();
    }

    public static void indent(StringBuilder sb, int num) {
        sb.append(" ".repeat(Math.max(0, num)));
    }

    public static final String AWFULLY_LONG_METHOD = "?? awfully long method";

    /*
        n <= 10  >> 0..9
        n <=100  >> 00..99
        n <=1000 >> 000..999
         */
    @NotNull
    public static String pad(int i, int n) {
        String s = Integer.toString(i);
        if (n <= 10) return s;
        if (n <= 100) {
            if (i < 10) return "0" + s;
            return s;
        }
        if (n <= 1_000) {
            if (i < 10) return "00" + s;
            if (i < 100) return "0" + s;
            return s;
        }
        if (n <= 10_000) {
            if (i < 10) return "000" + s;
            if (i < 100) return "00" + s;
            if (i < 1_000) return "0" + s;
            return s;
        }
        if (n <= 100_000) {
            if (i < 10) return "0000" + s;
            if (i < 100) return "000" + s;
            if (i < 1_000) return "00" + s;
            if (i < 10_000) return "0" + s;
            return s;
        }
        if (n <= 1_000_000) {
            if (i < 10) return "00000" + s;
            if (i < 100) return "0000" + s;
            if (i < 1_000) return "000" + s;
            if (i < 10_000) return "00" + s;
            if (i < 100_000) return "0" + s;
            return s;
        }
        throw new UnsupportedOperationException(AWFULLY_LONG_METHOD);
    }

    public static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() * 2);
        sb.append('"');
        for (char c : s.toCharArray()) {
            if ('\n' == c) {
                sb.append("\\n");
            } else if ('\r' == c) {
                sb.append("\\r");
            } else if ('\t' == c) {
                sb.append("\\t");
            } else if ('\b' == c) {
                sb.append("\\b");
            } else if ('"' == c || '\\' == c) {
                sb.append("\\").append(c);
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static String capitalize(String name) {
        assert name != null && !name.isEmpty();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public static String replaceLastDigitsBy(String s, String by) {
        StringBuilder sb = new StringBuilder(s);
        while (!sb.isEmpty() && Character.isDigit(sb.charAt(sb.length() - 1))) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb + by;
    }

    public static String replaceSlashDollar(String in) {
        StringBuilder sb = new StringBuilder(in.length());
        char[] charArray = in.toCharArray();
        for (char c : charArray) {
            if ((c == '$' || c == '/') && !sb.isEmpty() && sb.charAt(sb.length() - 1) != '.') {
                sb.append('.');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
