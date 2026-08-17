package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
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
        JavaInspector javaInspector = new io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl();
        javaInspector.preload("java.base::java.util.");
        javaInspector.preload("org.slf4j");
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addClassPathParts(SourceSetImpl.javaBase(), slf4j)
                .addSourceSets(SourceSetImpl.testProtocolSourceSet())
                .build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.onlyPreload();

        TypeInfo logger = javaInspector.compiledTypesManager().typeIfLoaded(Logger.class);
        assertSame(slf4j, logger.compilationUnit().sourceSet());
        assertTrue(slf4j.externalLibrary());


        TypeInfo number = javaInspector.compiledTypesManager()
                .type("java.lang.Number", null);
        // sorted: javac's member iteration order (Elements.getAllMembers) is unspecified and not stable across
        // runs for sibling methods (byteValue/shortValue occasionally flip), so do not assert the load order
        assertEquals("byteValue, doubleValue, floatValue, intValue, longValue, shortValue",
                number.methods().stream()
                        .map(MethodInfo::name)
                        .sorted()
                        .collect(Collectors.joining(", ")));

    }
}
