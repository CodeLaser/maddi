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

package io.codelaser.maddi.java.openjdk.method;

import io.codelaser.maddi.java.openjdk.CommonTest;
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
