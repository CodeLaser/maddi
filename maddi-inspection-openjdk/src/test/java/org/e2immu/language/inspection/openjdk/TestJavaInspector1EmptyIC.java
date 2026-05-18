package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJavaInspector1EmptyIC {

    private JavaInspector javaInspector;
    private Runtime runtime;

    @BeforeEach
    public void test() throws IOException {
        javaInspector = new JavaInspectorImpl();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .build().withDefaultModules();
        javaInspector.initialize(inputConfiguration);
        runtime = javaInspector.runtime();
    }

    @Language("java")
    private static final String INPUT1 = """
            package a.b;
            class X1 {
                interface I { }
                record RI(String s) implements I { }
                void method(I i) {
                    if(i instanceof RI(String t)) {
                        System.out.println(t);
                    }
                }
            }
            """;

    @Test
    public void test1() {
        TypeInfo X1 = javaInspector.parse(Map.of(JavaInspectorImpl.TEST_PROTOCOL_PREFIX + "a.b.X1", INPUT1),
                JavaInspectorImpl.DETAILED_SOURCES).parseResult().firstType();
        assertEquals("a.b.X1", X1.fullyQualifiedName());
    }
}
