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

package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.CommonTest;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestVariableDataArrays extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X {
                public static int[][] method(int[] pixels, int w, int h, int size) {
                    int[][] npixels = new int[w][h];
                    for (int i = 0; i < npixels.length; i++) {
                        for (int j = 0; j < npixels[i].length; j++) {
                            npixels[i][j] = pixels[i + j * size];
                        }
                    }
                    int pixel[][] = new int[h][w];
                    for (int i = 0; i < h; i++) {
                        for (int j = 0; j < w; j++) {
                            pixel[i][j] = npixels[j][i];
                        }
                    }
                    return pixel;
                }
            }
            """;

    @DisplayName("arrays, 1")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        PrepAnalyzer analyzer = new PrepAnalyzer(javaInspector.runtime());
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 4);
        Statement s100 = method.methodBody().statements().get(1).block().statements().get(0);
        VariableData vd100 = VariableDataImpl.of(s100);
        // the `7-40` refers to the index of pixels[i+j*size]
        assertEquals("""
                a.b.X.method(int[],int,int,int):0:pixels, \
                a.b.X.method(int[],int,int,int):1:w, \
                a.b.X.method(int[],int,int,int):2:h, \
                a.b.X.method(int[],int,int,int):3:size, \
                i, j, npixels, npixels[i]\
                """, vd100.knownVariableNamesToString());

        Statement s10000 = s100.block().statements().get(0);
        VariableData vd10000 = VariableDataImpl.of(s10000);
        assertEquals("""
                a.b.X.method(int[],int,int,int):0:pixels, a.b.X.method(int[],int,int,int):0:pixels[`7-40`], \
                a.b.X.method(int[],int,int,int):1:w, \
                a.b.X.method(int[],int,int,int):2:h, \
                a.b.X.method(int[],int,int,int):3:size, \
                i, j, npixels, npixels[i], npixels[i][j]\
                """, vd10000.knownVariableNamesToString());
    }

    @Language("java")
    private static final String INPUT2 = """
            //empty
            class X {
                public static boolean method(byte abyte0[][], byte byte0, byte byte1) {
                    int i = 0;
                    if (abyte0[i][0] == byte0 && abyte0[i][1] == byte1) return true;
                    int j = abyte0.length - 1;
                    if (abyte0[j][0] == byte0 && abyte0[j][1] == byte1) return true;
                    do {
                        int k = (i + j) / 2;
                        if (abyte0[k][0] == byte0 && abyte0[k][1] == byte1) return true;
                        if (byte0 < abyte0[k][0] || byte0 == abyte0[k][0] && byte1 < abyte0[k][1]) j = k; else i = k;
                    } while (i != j && i + 1 != j);
                    return false;
                }
            }
            """;

    @DisplayName("arrays, 2")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        PrepAnalyzer analyzer = new PrepAnalyzer(javaInspector.runtime());
        analyzer.doPrimaryType(X);

        MethodInfo method = X.findUniqueMethod("method", 3);
        Statement s401 = method.methodBody().statements().get(4).block().statements().get(1);
        VariableData vd401 = VariableDataImpl.of(s401);
        // the `7-40` refers to the index of pixels[i+j*size]
        String expected = """
                X.method(byte[][],byte,byte), \
                X.method(byte[][],byte,byte):0:abyte0, \
                X.method(byte[][],byte,byte):0:abyte0[i], \
                X.method(byte[][],byte,byte):0:abyte0[i][0], \
                X.method(byte[][],byte,byte):0:abyte0[i][1], \
                X.method(byte[][],byte,byte):0:abyte0[j], \
                X.method(byte[][],byte,byte):0:abyte0[j][0], \
                X.method(byte[][],byte,byte):0:abyte0[j][1], \
                X.method(byte[][],byte,byte):0:abyte0[k], \
                X.method(byte[][],byte,byte):0:abyte0[k][0], \
                X.method(byte[][],byte,byte):0:abyte0[k][1], \
                X.method(byte[][],byte,byte):1:byte0, \
                X.method(byte[][],byte,byte):2:byte1, i, j, k\
                """;
        assertEquals(expected, vd401.knownVariableNamesToString());
        VariableInfo viAbyte0k = vd401.variableInfo("X.method(byte[][],byte,byte):0:abyte0[k]");
        assertEquals("D:4.0.0, A:[]", viAbyte0k.assignments().toString()); // is the value of 'k'

        Statement s402 = method.methodBody().statements().get(4).block().statements().get(2);
        VariableData vd402 = VariableDataImpl.of(s402);
        assertEquals(expected, vd402.knownVariableNamesToString());
    }
}
