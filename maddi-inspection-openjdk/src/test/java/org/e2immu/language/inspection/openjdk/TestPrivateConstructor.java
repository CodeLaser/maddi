/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 */
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A private constructor of a compiled classpath type must be loaded. Static-utility classes (e.g. org.slf4j.MDC,
 * LoggerFactory) have exactly one private no-arg constructor; if the on-demand member loading skips it (because it is
 * private) then {@code typeInfo.constructors()} is empty, which breaks analyzed-package decode (a {@code C0}
 * constructor reference cannot be resolved). Regular JAR classfiles carry private members, so javac exposes them and
 * we must not filter them out for constructors. (JDK platform types are a separate case: their symbols come from the
 * stripped {@code ct.sym}, which has no private members at all.)
 */
public class TestPrivateConstructor {

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

        for (String fqn : new String[]{"org.slf4j.MDC", "org.slf4j.LoggerFactory", "org.slf4j.helpers.Util"}) {
            TypeInfo ti = javaInspector.compiledTypesManager().getOrLoad(fqn, null);
            assertEquals(1, ti.constructors().size(), fqn + " should have its private constructor loaded");
            MethodInfo constructor = ti.constructors().getFirst();
            assertTrue(constructor.isConstructor());
            assertTrue(constructor.parameters().isEmpty(), "no-arg constructor");
        }
    }
}
