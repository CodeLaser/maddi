/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 */
package org.e2immu.language.inspection.openjdk;

import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A nested type must be resolvable by its dotted fully-qualified name via getOrLoad. javac's getTypeElement resolves
 * the nested symbol, but it is loaded as part of its enclosing type, not on its own -- so getOrLoad must load the
 * top-level enclosing type and return the nested one. Analyzed-package decode relies on this: a linked-variable
 * references e.g. Try.TryData / Loop.LoopData (nested interfaces of transform-support classes).
 */
public class TestNestedTypeLoad {

    @Test
    public void test() throws IOException {
        JavaInspector javaInspector = new JavaInspectorImpl();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addClassPathParts(SourceSetImpl.javaBase())
                .addSourceSets(SourceSetImpl.testProtocolSourceSet())
                .build();
        javaInspector.initialize(inputConfiguration);
        javaInspector.onlyPreload();

        // a nested type, requested by its dotted FQN, without loading the enclosing type first
        TypeInfo entry = javaInspector.compiledTypesManager().getOrLoad("java.util.Map.Entry", null);
        assertNotNull(entry, "java.util.Map.Entry must resolve");
        assertEquals("java.util.Map.Entry", entry.fullyQualifiedName());

        // a doubly-nested type
        TypeInfo simpleEntry = javaInspector.compiledTypesManager().getOrLoad("java.util.AbstractMap.SimpleEntry", null);
        assertNotNull(simpleEntry, "java.util.AbstractMap.SimpleEntry must resolve");
        assertEquals("java.util.AbstractMap.SimpleEntry", simpleEntry.fullyQualifiedName());
    }
}
