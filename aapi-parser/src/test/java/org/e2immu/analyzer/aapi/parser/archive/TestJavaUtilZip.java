package org.e2immu.analyzer.aapi.parser.archive;

import org.e2immu.analyzer.aapi.parser.CommonTest;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.junit.jupiter.api.Test;

import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestJavaUtilZip extends CommonTest {

    @Test
    public void testZipOutputStreamClose() {
        TypeInfo typeInfo = compiledTypesManager().getOrLoad(ZipOutputStream.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("close", 0);

        // must be "false" when the class is not present in the AAPI, and true otherwise
        // if not present, then the shallow analyser is never run, so we don't even have a value
        assertTrue(methodInfo.isModifying());
    }

    @Test
    public void testZipOutputStreamPutNextEntry() {
        TypeInfo typeInfo = compiledTypesManager().getOrLoad(ZipOutputStream.class);
        MethodInfo methodInfo = typeInfo.findUniqueMethod("putNextEntry", 1);
        assertTrue(methodInfo.isModifying());
    }
}
