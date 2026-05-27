package org.e2immu.language.inspection.integration.java.other;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.integration.java.CommonTest;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestNative extends CommonTest {
    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            import java.io.*;
            public class NativeSample {
            
                static {
                    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                        throw new IllegalStateException("Test is not implemented for Windows");
                    }
                    try {
                        InputStream inputStream = NativeSample.class.getResourceAsStream("/net_bytebuddy_test_c_NativeSample.so");
                        if (inputStream == null)  {
                            throw new IllegalStateException("Cannot find .so file");
                        }
                        File file;
                        try {
                            file = File.createTempFile("native_sample", ".so");
                            file.deleteOnExit();
                            OutputStream outputStream = new FileOutputStream(file);
                            try {
                                byte[] buffer = new byte[1024];
                                int length;
                                while ((length = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, length);
                                }
                            } finally {
                                outputStream.close();
                            }
                        } finally {
                            inputStream.close();
                        }
                        System.load(file.getAbsolutePath());
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            
                public native int foo(int left, int right);
            }
            """;

    @Test
    public void test1() {
        TypeInfo typeInfo = javaInspector.parse(INPUT1);
        MethodInfo foo = typeInfo.findUniqueMethod("foo", 2);
        assertTrue(foo.methodModifiers().contains(runtime.methodModifierNative()));
    }

}
