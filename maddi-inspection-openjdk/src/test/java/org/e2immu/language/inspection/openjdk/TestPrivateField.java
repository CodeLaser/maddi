/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 */
package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Private fields of a compiled classpath type must be loaded. Like private constructors (see
 * {@link TestPrivateConstructor}), a type's fields are part of its shape and are referenced in analyzed-package
 * files (@GetSet targets, linked variables); skipping private ones left {@code typeInfo.fields()} short of what the
 * source-side encoder saw, so decode hit "field index out of range" (a nested {@code ...Impl.Builder} with 0
 * fields). Compiler-synthetic fields ({@code this$0}) stay excluded. (JDK ct.sym types carry no private members.)
 */
public class TestPrivateField {

    @Test
    public void test() throws IOException {
        SourceSet slf4j = SourceSetImpl.sourceSetOf(Logger.class);
        JavaInspector javaInspector = new JavaInspectorImpl();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addClassPathParts(SourceSetImpl.javaBase(), slf4j)
                .addSourceSets(SourceSetImpl.testProtocolSourceSet())
                .build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.onlyPreload();

        // org.slf4j.helpers.BasicMarker: private String name; private final List<Marker> referenceList;
        TypeInfo basicMarker = javaInspector.compiledTypesManager().getOrLoad("org.slf4j.helpers.BasicMarker", null);
        Set<String> fields = basicMarker.fields().stream().map(f -> f.name()).collect(Collectors.toSet());
        assertTrue(fields.contains("name"), "private field 'name' must be loaded, have: " + fields);
        assertTrue(fields.contains("referenceList"), "private field 'referenceList' must be loaded, have: " + fields);
        assertTrue(fields.contains("serialVersionUID"));
        assertFalse(fields.stream().anyMatch(n -> n.startsWith("$") || n.equals("this$0")),
                "no compiler-synthetic fields: " + fields);
    }
}
