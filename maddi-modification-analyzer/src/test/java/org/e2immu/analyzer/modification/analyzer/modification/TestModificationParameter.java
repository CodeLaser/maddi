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

package org.e2immu.analyzer.modification.analyzer.modification;

import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestModificationParameter extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            public class Function63498_file16492 {
                private static int method(int[] dest, int offset, int[] x, int len, int y) {
                    long yl =(long)y & 4294967295L;
                    int carry = 0;
                    int j = 0;
            
                    do {
                        long prod =((long)x[j] & 4294967295L) * yl;
                        int prod_low =(int)prod;
                        int prod_high =(int)(prod >> 32);
                        prod_low += carry;
                        carry =((prod_low ^ -2147483648) <(carry ^ -2147483648) ? 1 : 0) + prod_high;
                        int x_j = dest[offset + j];
                        prod_low = x_j - prod_low;
                        if((prod_low ^ -2147483648) >(x_j ^ -2147483648)) { carry++; }
                        dest[offset + j] = prod_low;
                    } while((++j) < len);
            
                    return carry;
                }
            }
            """;

    @DisplayName("parameter is array type, modification")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        MethodInfo methodInfo = X.findUniqueMethod("method", 5);
        ParameterInfo dest = methodInfo.parameters().getFirst();

        Statement s308 = methodInfo.methodBody().statements().get(3).block().statements().get(8);
        assertInstanceOf(Assignment.class, s308.expression());
        VariableData vd308 = VariableDataImpl.of(s308);
        VariableInfo dest308 = vd308.variableInfo(dest);
        assertTrue(dest308.isModified());

        Statement s4 = methodInfo.methodBody().statements().get(4);
        VariableData vd4 = VariableDataImpl.of(s4);
        VariableInfo dest4 = vd4.variableInfo(dest);
        assertTrue(dest4.isModified());

        assertTrue(dest.isModified());
    }

    @Language("java")
    private static final String INPUT2 = """
            import java.io.IOException;
            import java.io.Reader;
            public class BundleJSONParser {
                private static String readFully(final Reader reader) throws IOException {
                    final char[] arr = new char[1024];
                    final StringBuilder sb = new StringBuilder();
            
                    try(reader) {
                        int numChars;
                        while((numChars = reader.read(arr, 0, arr.length)) > 0) { sb.append(arr, 0, numChars); }
                    }
            
                    return sb.toString();
                }
            }
            """;

    @DisplayName("parameter is reader, read inside expression, modification")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);
        MethodInfo methodInfo = X.findUniqueMethod("readFully", 1);
        ParameterInfo reader = methodInfo.parameters().getFirst();

        Statement s201 = methodInfo.methodBody().statements().get(2).block().statements().get(1);
        VariableInfo viReader201 = VariableDataImpl.of(s201).variableInfo(reader);
        assertTrue(viReader201.isModified());

        VariableData vd = VariableDataImpl.of(methodInfo.methodBody().lastStatement());
        VariableInfo viReader = vd.variableInfo(reader);
        assertEquals("2+0, 2.0.1-E, 2.0.1;E", viReader.reads().toString());
        assertTrue(viReader.isModified());

        assertTrue(reader.isModified());
    }
}
