package org.e2immu.analyzer.modification.common;

import org.e2immu.annotation.Immutable;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.integration.JavaInspectorFactory;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class CommonTest {

    public static @NonNull JavaInspectorFactory javaInspectorFactory() {
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet javaDesktop = SourceSetImpl.jdkModule("java.desktop");
        SourceSet javaNetHttp = SourceSetImpl.jdkModule("java.net.http");
        SourceSet maddiSupport = SourceSetImpl.sourceSetOf(Immutable.class);
        SourceSet slf4jApi = SourceSetImpl.sourceSetOf(org.slf4j.Logger.class);
        SourceSet logbackClassic = SourceSetImpl.sourceSetOf(Logger.class);
        SourceSet junitPlatform = SourceSetImpl.sourceSetOf(JUnitException.class);
        SourceSet jupiter = SourceSetImpl.sourceSetOf(Test.class, junitPlatform);
        SourceSet opentest4j = SourceSetImpl.sourceSetOf(AssertionFailedError.class);
        SourceSet annotations = SourceSetImpl.sourceSetOf(NotNull.class);

        return new JavaInspectorFactory() {
            @Override
            public List<SourceSet> dependencies() {
                return List.of(maddiSupport, slf4jApi, logbackClassic, junitPlatform, jupiter, opentest4j, annotations);
            }

            @Override
            public JavaInspector withSources(SourceSet sourceSet) throws IOException {
                JavaInspector javaInspector = new org.e2immu.language.inspection.openjdk.JavaInspectorImpl();
                javaInspector.preload("java.base::java.util.");
                javaInspector.preload("java.base::java.net");
                javaInspector.preload("java.base::java.io");
                javaInspector.preload("java.base::java.nio.");
                javaInspector.preload("java.base::java.time.");
                javaInspector.preload("java.base::java.security");
                javaInspector.preload("java.base::java.lang.annotation");
                javaInspector.preload("java.base::java.lang.reflect");
                javaInspector.preload("java.base::java.lang.constant");
                javaInspector.preload("java.desktop::java.awt");
                javaInspector.preload("java.desktop::javax.swing.");
                javaInspector.preload("java.net.http::java.net.http");
                javaInspector.preload("org.slf4j");
                javaInspector.preload("org.junit.jupiter.api.");
                javaInspector.preload("org.e2immu.annotation.");
                InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                        .addSourceSets(sourceSet)
                        .addClassPathParts(javaBase, javaDesktop, javaNetHttp,
                                maddiSupport, slf4jApi, logbackClassic, jupiter, junitPlatform, opentest4j,
                                annotations)
                        .build();
                javaInspector.initialize(inputConfiguration);
                return javaInspector;
            }
        };
    }
}
