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

package org.e2immu.language.java.openjdk.method;

import org.e2immu.language.java.openjdk.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TestMethodCallB1 extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class C {
                static int SINGLE_QUOTE = 2;
                static int DOUBLE_QUOTE = 3;
                static int ERR = 4;
                static int SPACE = 5;
                static int WHITE = 6;
                static int OTHER = 7;
                static final int[][][] FSM = new int[4][5][6];
            	private static int category(int c) {
            		return c == '\\'' ? SINGLE_QUOTE : (c == '\\"' ? DOUBLE_QUOTE
            		    : (c == ' ' ? SPACE : (c == '\\t' || c == '\\n' || c == '\\r' ? WHITE : OTHER)));
            	}
                private void error(int code, int line) {
                    System.out.println("Error "+code+" at line "+line);
                }
                void method(int state, int[] c, int i) {
                    int[] next = FSM[state][category(c[i])];
                    if (next[0] == ERR) {
                        error(category(c[i]), 9);
                    }
                }
            }
            """;

    @DisplayName("ensure that scope 'this' problem is fixed")
    @Test
    public void test1() {
        scan("a.b.C", INPUT1);
    }

}
