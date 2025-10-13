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

package org.e2immu.analyzer.modification.prepwork;

import java.util.regex.Pattern;

public class StatementIndex {

    // pre-digits+dot: +,-.
    // post-digits: :;<=>?@ and at the end |~

    public static final char PLUS = '+';
    public static final char DASH = '-';
    public static final char DOT = '.';
    public static final char COLON = ':';
    public static final char SEMICOLON = ';';
    public static final char EQUALS = '=';
    public static final char END = '~';

    public static final String INIT = PLUS + "I"; // name of the stage +I
    public static final String EVAL_INIT = PLUS + "E"; // initializer in for/forEach +E
    public static final String EVAL = DASH + "E";  // normal expression; condition/iterable for loops -E
    public static final String EVAL_UPDATE = COLON + "E"; // still part of the loop, but after the statements :E
    public static final String EVAL_AFTER_UPDATE = SEMICOLON + "E"; // repeat of condition in for
    public static final String MERGE = EQUALS + "M"; // at the end =M

    public static final Pattern STAGE_PATTERN = Pattern.compile("(.*)(\\+I|[+-=]E|=M)");

    private StatementIndex() {
    }


    public static final String DOT_ZERO = DOT + "0";
    public static final String BEFORE_METHOD = "" + DASH;
    public static final String ENCLOSING_METHOD = "" + PLUS;
    public static final String END_OF_METHOD = "" + END;


    public static boolean seenBy(String s, String index) {
        int dot = s.lastIndexOf(StatementIndex.DOT);
        if (dot < 0) {
            return s.compareTo(index) < 0;
        }
        String sDot = s.substring(0, dot);
        return index.startsWith(sDot) && s.compareTo(index) < 0;
    }
}
