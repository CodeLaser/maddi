package org.e2immu.analyzer.modification.linkedvariables.modification;

import org.e2immu.analyzer.modification.linkedvariables.CommonTest;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.List;
import java.util.stream.Collectors;


import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.EMPTY_PART_OF_CONSTRUCTION;
import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION;
import static org.junit.jupiter.api.Assertions.*;

public class TestModificationField extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestModificationField.class);

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
                    int count = 0;
                    while (n < blockSize) {
                      count = in.read(block, n, blockSize - n);
                      if (count == -1) {
                        break;
                      }
                      n += count;
                    }
                  } catch (IOException e) {
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

        MethodInfo readNetscapeExt = X.findUniqueMethod("readNetscapeExt", 0);
        {
            Statement s000 = readNetscapeExt.methodBody().statements().get(0).block().statements().get(0);
            VariableData vd000 = VariableDataImpl.of(s000);
            VariableInfo viBlockSize = vd000.variableInfo(blockSizeFr);
            assertFalse(viBlockSize.isModified()); // it is assigned, not modified!!
        }
        Statement s00102 = readNetscapeExt.methodBody().statements().get(0).block().statements().get(1).block().statements().get(2);
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