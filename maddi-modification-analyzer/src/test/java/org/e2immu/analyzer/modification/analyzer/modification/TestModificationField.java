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
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.util.List;
import java.util.stream.Collectors;


import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.EMPTY_PART_OF_CONSTRUCTION;
import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION;
import static org.junit.jupiter.api.Assertions.*;

public class TestModificationField extends CommonTest {

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.io.BufferedReader;
            import java.io.IOException;
            public class Function63498_file16492 {
                protected BufferedReader buffRead;
                public String fastForward(String strSearch, boolean blnRegexMatch) throws IOException {
                    boolean blnContinue = false;
                    String strLine;
                    do {
                        strLine = buffRead.readLine();
                        if(strLine != null) {
                            if(blnRegexMatch) {
                                blnContinue = !strLine.matches(strSearch);
                            } else { blnContinue = !strLine.contains(strSearch); }
                        }
                    } while(blnContinue && strLine != null);
                    return strLine;
                }
            }
            """;

    @DisplayName("simple field modification")
    @Test
    public void test1() {
        TypeInfo X = javaInspector.parse(INPUT1);
        List<Info> analysisOrder = prepWork(X);
        analyzer.go(analysisOrder);

        TypeInfo bufferedReader = javaInspector.compiledTypesManager().get(BufferedReader.class);
        assertTrue(bufferedReader.analysis().getOrDefault(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.MUTABLE).isMutable());

        MethodInfo brReadLine = bufferedReader.findUniqueMethod("readLine", 0);
        assertTrue(brReadLine.isModifying());

        MethodInfo fastForward = X.findUniqueMethod("fastForward", 2);
        FieldInfo buffRead = X.getFieldByName("buffRead", true);

        Statement s200 = fastForward.methodBody().statements().get(2).block().statements().getFirst();
        VariableData vd200 = VariableDataImpl.of(s200);
        VariableInfo vi200BuffRead = vd200.variableInfo(runtime.newFieldReference(buffRead));
        assertTrue(vi200BuffRead.isModified());

        assertTrue(fastForward.isModifying());
        assertTrue(buffRead.isModified());
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            import java.io.BufferedInputStream;
            import java.io.IOException;
            
            public class X {
              protected void readNetscapeExt() {
                do {
                  readBlock();
                  if (block[0] == 1) {
                    int b1 = block[1] & 0xff;
                    int b2 = block[2] & 0xff;
                    loopCount = (b2 << 8) | b1;
                  }
                } while ((blockSize > 0) && !err());
              }
            
              /** File read status: No errors. */
              public static final int STATUS_OK = 0;
            
              /** File read status: Error decoding file (may be partially decoded) */
              public static final int STATUS_FORMAT_ERROR = 1;
            
              private BufferedInputStream in;
              private int status;
              private int loopCount = 1;
              private byte[] block = new byte[256];
              private int blockSize = 0;
            
              /** Returns true if an error was encountered during reading/decoding */
              protected boolean err() {
                return status != STATUS_OK;
              }
            
              /** Reads a single byte from the input stream. */
              protected int read() {
                int curByte = 0;
                try {
                  curByte = in.read();
                } catch (IOException e) {
                  status = STATUS_FORMAT_ERROR;
                }
                return curByte;
              }
            
              /**
               * Reads next variable length block from input.
               *
               * @return number of bytes stored in "buffer"
               */
              protected int readBlock() {
                blockSize = read();
                int n = 0;
                if (blockSize > 0) {
                  try {
                    int count;
                    while (n < blockSize) {
                      count = in.read(block, n, blockSize - n);
                      if (count == -1) {
                        break;
                      }
                      n += count;
                    }
                  } catch (IOException _) {
                  }
                  if (n < blockSize) {
                    status = STATUS_FORMAT_ERROR;
                  }
                }
                return n;
              }
            }
            """;

    @DisplayName("field assignment vs modification")
    @Test
    public void test2() {
        TypeInfo X = javaInspector.parse(INPUT2);
        List<Info> analysisOrder = prepAnalyzer.doPrimaryType(X);

        String simpleOrder = analysisOrder.stream().map(i -> i.info() + ":" + i.simpleName()).sorted().collect(Collectors.joining(", "));
        assertEquals("""
                field:STATUS_FORMAT_ERROR, field:STATUS_OK, field:block, field:blockSize, field:in, field:loopCount, \
                field:status, method:<init>, method:err, method:read, method:readBlock, method:readNetscapeExt, \
                type:X""", simpleOrder);

        analyzer.go(analysisOrder);

        FieldInfo loopCount = X.getFieldByName("loopCount", true);
        FieldInfo blockSize = X.getFieldByName("blockSize", true);
        FieldInfo status = X.getFieldByName("status", true);
        FieldReference loopCountFr = runtime.newFieldReference(loopCount);
        FieldReference blockSizeFr = runtime.newFieldReference(blockSize);
        FieldReference statusFr = runtime.newFieldReference(status);

        MethodInfo read = X.findUniqueMethod("read", 0);
        {
            VariableData vdLast = VariableDataImpl.of(read.methodBody().lastStatement());
            VariableInfo viLastStatus = vdLast.variableInfo(statusFr);
            assertEquals("D:-, A:[1.1.0]", viLastStatus.assignments().toString());
        }

        ValueImpl.VariableBooleanMapImpl mcm = read.analysis()
                .getOrNull(PropertyImpl.MODIFIED_COMPONENTS_METHOD, ValueImpl.VariableBooleanMapImpl.class);
        assertEquals("this.in=true, this.status=true", mcm.toString());

        MethodInfo readBlock = X.findUniqueMethod("readBlock", 0);
        {
            Statement tryStmt = readBlock.methodBody().statements().get(2).block().statements().getFirst();
            Statement whileStmt = tryStmt.block().statements().get(1);
            Statement ifInWhile = whileStmt.block().statements().get(1);

            VariableData vd2000101 = VariableDataImpl.of(ifInWhile);
            VariableInfo blockSize2000101 = vd2000101.variableInfo(blockSizeFr);
            assertFalse(blockSize2000101.isModified());

            VariableData vd20001 = VariableDataImpl.of(whileStmt);
            VariableInfo blockSize20001 = vd20001.variableInfo(blockSizeFr);
            assertFalse(blockSize20001.isModified());

            VariableData vd200 = VariableDataImpl.of(tryStmt);
            VariableInfo blockSize200 = vd200.variableInfo(blockSizeFr);
            assertFalse(blockSize200.isModified());

            VariableData vd2 = VariableDataImpl.of(readBlock.methodBody().statements().get(2));
            VariableInfo blockSize0 = vd2.variableInfo(blockSizeFr);
            assertFalse(blockSize0.isModified());

            VariableData vdLast = VariableDataImpl.of(readBlock.methodBody().statements().getLast());
            VariableInfo blockSizeLast = vdLast.variableInfo(blockSizeFr);
            assertFalse(blockSizeLast.isModified());
        }
        MethodInfo readNetscapeExt = X.findUniqueMethod("readNetscapeExt", 0);
        {
            Statement s000 = readNetscapeExt.methodBody().statements().getFirst().block().statements().getFirst();
            VariableData vd000 = VariableDataImpl.of(s000);
            VariableInfo viBlockSize = vd000.variableInfo(blockSizeFr);
            assertFalse(viBlockSize.isModified()); // it is assigned, not modified!!

            VariableData vdLast = VariableDataImpl.of(readNetscapeExt.methodBody().statements().getLast());
            VariableInfo blockSizeLast = vdLast.variableInfo(blockSizeFr);
            assertFalse(blockSizeLast.isModified());
        }
        Statement s00102 = readNetscapeExt.methodBody().statements().getFirst().block().statements().get(1).block().statements().get(2);
        VariableData vds00102 = VariableDataImpl.of(s00102);
        VariableInfo viLoopCount = vds00102.variableInfo(loopCountFr);
        assertEquals("D:-, A:[0.0.1.0.2]", viLoopCount.assignments().toString());

        {
            Statement last = readNetscapeExt.methodBody().lastStatement();
            VariableInfo viLastLoopCount = VariableDataImpl.of(last).variableInfo(loopCountFr);
            assertEquals(viLoopCount.assignments(), viLastLoopCount.assignments());
            assertFalse(viLastLoopCount.isModified());

            VariableInfo viLastBlockSize = VariableDataImpl.of(last).variableInfo(blockSizeFr);
            assertFalse(viLastBlockSize.isModified());
        }
        Value.SetOfInfo poc = X.analysis().getOrDefault(PART_OF_CONSTRUCTION, EMPTY_PART_OF_CONSTRUCTION);
        assertFalse(poc.infoSet().contains(readNetscapeExt));

        assertFalse(loopCount.isFinal());
        assertFalse(loopCount.isPropertyFinal());
        assertFalse(blockSize.isPropertyFinal());

        assertFalse(loopCount.isModified());
        assertFalse(blockSize.isModified());
    }
}