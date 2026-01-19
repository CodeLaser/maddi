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

package org.e2immu.analyzer.modification.analyzer.clonebench;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;


public class TestCastCharToInt extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            class X {
                protected static short[][] method(String[] sa) {
                    StringBuilder sb = new StringBuilder(sa[0]);
                    for (int i = 1; i < sa.length; i++) sb.append(sa[i]);
                    int n = 0;
                    int size1 = (((int) sb.charAt(n)) << 16) | ((int) sb.charAt(n + 1));
                    n += 2;
                    short[][] result = new short[size1][];
                    for (int i = 0; i < size1; i++) {
                        int size2 = (((int) sb.charAt(n)) << 16) | ((int) sb.charAt(n + 1));
                        n += 2;
                        result[i] = new short[size2];
                        for (int j = 0; j < size2; j++) result[i][j] = (short) (sb.charAt(n++) - 2);
                    }
                    return result;
                }
            }
            """;

    @DisplayName("assign char from int")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(X);
        analyzer.go(ao);
        MethodInfo method = X.findUniqueMethod("method", 1);

    }
}
