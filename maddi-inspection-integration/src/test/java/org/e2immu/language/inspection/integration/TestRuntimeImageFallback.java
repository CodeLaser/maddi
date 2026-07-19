package org.e2immu.language.inspection.integration;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ASM/in-house front-end reads JDK library classes from {@code .jmod} files. Some JDK distributions ship the
 * runtime image ({@code lib/modules}) but no {@code jmods/} directory (e.g. Eclipse Temurin), which used to make
 * this path fail outright. maddi now falls back to the runtime image via the {@code jrt} filesystem.
 * <p>
 * This test only means something on such a JDK, so it skips when the running JDK has a {@code jmods/} directory
 * (there the normal {@code .jmod} path is used and covered by {@link TestPreloadJavaBase}).
 */
public class TestRuntimeImageFallback {

    @Test
    public void loadsJdkTypesWithoutJmods() throws IOException {
        Path jmods = Path.of(System.getProperty("java.home"), "jmods");
        Assumptions.assumeFalse(Files.isDirectory(jmods),
                "only relevant on a JDK without a jmods/ directory; this JDK has " + jmods);

        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addClassPath("jmod:java.base")
                .build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(inputConfiguration); // indexes java.base from the runtime image (no .jmod on disk)

        TypeInfo string = javaInspector.compiledTypesManager().getOrLoad(String.class);
        assertNotNull(string, "java.lang.String must load from the runtime image");
        assertTrue(string.hasBeenInspected());
        // a real member proves actual bytecode was read, not a stub
        assertNotNull(string.findUniqueMethod("length", 0));

        TypeInfo list = javaInspector.compiledTypesManager().getOrLoad(java.util.List.class);
        assertNotNull(list);
        assertTrue(list.hasBeenInspected());
    }
}
