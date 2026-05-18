package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.e2immu.language.inspection.openjdk.JavaInspectorImpl.JAR_WITH_PATH_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled
public class TestJavaInspector2 {

    private JavaInspector javaInspector;
    private Runtime runtime;

    @BeforeEach
    public void test() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addClassPath("../maddi-support/build/classes/java/main")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/junit/jupiter/api")
                .addClassPath(JAR_WITH_PATH_PREFIX + "org/slf4j/event")
                .build();
        for (String pkg : new String[]{"org.e2immu.support", "org.slf4j..", "org.junit.jupiter.api.."}) {
            javaInspector.preload(pkg);
            // meaning: all classes is these packages and their subpackages should be loaded entirely
        }

        javaInspector.initialize(inputConfiguration);
        javaInspector.parse(new JavaInspectorImpl.ParseOptionsBuilder()
                .setFailFast(true)
                .setLombok(false)
                .setDetailedSources(true).build());
        javaInspector.javaBase().computePriorityDependencies();
        runtime = javaInspector.runtime();
    }

    @Language("java")
    public static final String INPUT1 = """
            package a.b;
            import org.e2immu.annotation.ImmutableContainer;
            import org.slf4j.Logger;
            import org.slf4j.LoggerFactory;
            
            @ImmutableContainer
            record C(int k) {
                private static final Logger LOGGER = LoggerFactory.getLogger(C.class);
            
                int kSquared() {
                    LOGGER.info("Returning k*k = {}", k*k);
                    return k*k;
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo C = javaInspector.parse(INPUT1);
        assertEquals("@ImmutableContainer", C.annotations().getFirst().toString());
        runtime.getFullyQualified("", true);
    }
}
