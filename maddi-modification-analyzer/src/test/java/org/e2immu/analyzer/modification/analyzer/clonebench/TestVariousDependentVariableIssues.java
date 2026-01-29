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
import org.e2immu.language.cst.api.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;


public class TestVariousDependentVariableIssues extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            import java.math.*;
            
            public class X {
              int[] encode(byte[] b, int count) {
                int j, i;
                int bLen = count;
                byte[] bp = b;
                _padding = bLen % 8;
                if (_padding != 0) {
                  _padding = 8 - (bLen % 8);
                  bp = new byte[bLen + _padding];
                  System.arraycopy(b, 0, bp, 0, bLen);
                  bLen = bp.length;
                }
                int intCount = bLen / 4;
                int[] r = new int[2];
                int[] out = new int[intCount];
                for (i = 0, j = 0; j < bLen; j += 8, i += 2) {
                  r[0] =
                      (bp[j] << 24)
                          | (((bp[j + 1]) & 0xff) << 16)
                          | (((bp[j + 2]) & 0xff) << 8)
                          | ((bp[j + 3]) & 0xff);
                  r[1] =
                      (bp[j + 4] << 24)
                          | (((bp[j + 5]) & 0xff) << 16)
                          | (((bp[j + 6]) & 0xff) << 8)
                          | ((bp[j + 7]) & 0xff);
                  encipher(r);
                  out[i] = r[0];
                  out[i + 1] = r[1];
                }
                return out;
              }
            
              private int[] _key;
              private int _padding;
            
              public int[] encipher(int[] v) {
                int y = v[0];
                int z = v[1];
                int sum = 0;
                int delta = 0x9E3779B9;
                int a = _key[0];
                int b = _key[1];
                int c = _key[2];
                int d = _key[3];
                int n = 32;
                while (n-- > 0) {
                  sum += delta;
                  y += (z << 4) + a ^ z + sum ^ (z >> 5) + b;
                  z += (y << 4) + c ^ y + sum ^ (y >> 5) + d;
                }
                v[0] = y;
                v[1] = z;
                return v;
              }
            }
            """;

    @DisplayName("various dependent variable issues")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);
    }
}
