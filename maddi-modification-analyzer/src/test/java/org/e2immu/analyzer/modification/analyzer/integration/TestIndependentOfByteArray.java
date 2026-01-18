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

package org.e2immu.analyzer.modification.analyzer.integration;


import org.e2immu.analyzer.modification.analyzer.CommonTest;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.analyzer.modification.prepwork.variable.VariableInfo;
import org.e2immu.analyzer.modification.prepwork.variable.impl.VariableDataImpl;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.IfElseStatement;
import org.e2immu.language.cst.api.statement.Statement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.EMPTY;
import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.INDEPENDENT_PARAMETER;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestIndependentOfByteArray extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            import java.io.EOFException;
            import java.io.IOException;
            import java.io.RandomAccessFile;
            
            public class B {
              public void readFully(byte[] b, int off, int len) throws IOException {
                int n = 0;
                do {
                  int count = read(b, off + n, len - n);
                  if (count < 0) throw new EOFException();
                  n += count;
                } while (n < len);
              }
            
              RandomAccessFile rf;
              byte[] arrayIn;
              int arrayInPtr;
              byte back;
              boolean isBack = false;
            
              public int read(byte[] b, int off, int len) throws IOException {
                if (len == 0) return 0;
                int n = 0;
                if (isBack) {
                  isBack = false;
                  if (len == 1) {
                    b[off] = back;
                    return 1;
                  } else {
                    n = 1;
                    b[off++] = back;
                    --len;
                  }
                }
                if (arrayIn == null) {
                  return rf.read(b, off, len) + n;
                } else {
                  if (arrayInPtr >= arrayIn.length) return -1;
                  if (arrayInPtr + len > arrayIn.length) len = arrayIn.length - arrayInPtr;
                  System.arraycopy(arrayIn, arrayInPtr, b, off, len);
                  arrayInPtr += len;
                  return len + n;
                }
              }
            }
            """;

    @DisplayName("byte array independent?")
    @Test
    public void test1() {
        TypeInfo B = javaInspector.parse(INPUT1);
        List<Info> ao = prepWork(B);
        analyzer.go(ao);

        MethodInfo readFully = B.findUniqueMethod("readFully", 3);
        assertEquals("""
                [0:b*.§3←this*.arrayIn.§2,0:b*∋this*.back, -, -] --> -\
                """, readFully.analysis().getOrDefault(METHOD_LINKS, EMPTY).toString());

        MethodInfo read = B.findUniqueMethod("read", 3);
        assertEquals("""
                [0:b*[1:off]←this*.back,\
                0:b*[off++]←this*.back,\
                0:b*∋this*.back,\
                0:b*.§3←this*.arrayIn.§2, 1:off←$_ce7, -] --> -\
                """, read.analysis().getOrDefault(METHOD_LINKS, EMPTY).toString());

        ParameterInfo b = read.parameters().getFirst();
        Statement s3 = read.methodBody().statements().get(3);
        {
            Statement s312 = ((IfElseStatement) s3).elseBlock().statements().get(2);
            VariableData vd312 = VariableDataImpl.of(s312);
            VariableInfo viB = vd312.variableInfo(b);
            assertEquals("""
                    0:b[1:off]←this.back,0:b[off++]←this.back,0:b.§3←this.arrayIn.§2,0:b∋this.back\
                    """, viB.linkedVariables().toString());
        }
        {
            VariableData vd3 = VariableDataImpl.of(s3);
            VariableInfo viB = vd3.variableInfo(b);
            assertEquals("""
                    0:b[1:off]←this.back,0:b[off++]←this.back,0:b∋this.back,0:b.§3←this.arrayIn.§2\
                    """, viB.linkedVariables().toString());
        }
        assertTrue(b.analysis().getOrDefault(INDEPENDENT_PARAMETER, DEPENDENT).isIndependent());
    }
}
