package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestPreload {

    // used to capture a bug
    // name: subscribe, num params: 1, paramsCsv: java.util.concurrent.Flow.Subscriber
    @Test
    public void test() throws IOException {
        SourceSet slf4j = SourceSetImpl.sourceSetOf(Logger.class);
        JavaInspector javaInspector = new org.e2immu.language.inspection.openjdk.JavaInspectorImpl();
        javaInspector.preload("java.base::java.util.");
        javaInspector.preload("org.slf4j");
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addClassPathParts(SourceSetImpl.javaBase(), slf4j)
                .addSourceSets(SourceSetImpl.testProtocolSourceSet())
                .build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.onlyPreload();

        TypeInfo logger = javaInspector.compiledTypesManager().get(Logger.class);
        assertSame(slf4j, logger.compilationUnit().sourceSet());
        assertTrue(slf4j.externalLibrary());


        TypeInfo number = javaInspector.compiledTypesManager()
                .getOrLoad("java.lang.Number", null);
        // sorted: javac's member iteration order (Elements.getAllMembers) is unspecified and not stable across
        // runs for sibling methods (byteValue/shortValue occasionally flip), so do not assert the load order
        assertEquals("byteValue, doubleValue, floatValue, intValue, longValue, shortValue",
                number.methods().stream()
                        .map(MethodInfo::name)
                        .sorted()
                        .collect(Collectors.joining(", ")));

    }
}
