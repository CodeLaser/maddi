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
import java.util.ArrayList;
import java.util.List;

public class CommonTest {

    /**
     * Lean default: only java.base is on the classpath (plus the non-JDK test dependencies). Since analysis hints
     * are now gated on classpath module presence (see PrepWorkCodec / LoadAnalysisResults), the AAPI archive no
     * longer force-loads java.desktop (swing/awt) or java.net.http, which is ~40% of the JDK types otherwise loaded
     * per test. Tests that genuinely parse code referencing those modules must opt in explicitly via
     * {@link #javaInspectorFactory(String...)} (e.g. {@code javaInspectorFactory("java.desktop")}).
     */
    public static @NonNull JavaInspectorFactory javaInspectorFactory() {
        return javaInspectorFactory(new String[0]);
    }

    /** Heavy opt-in: {@link #javaInspectorFactory()} plus the given extra JDK modules on the classpath. */
    public static @NonNull JavaInspectorFactory javaInspectorFactory(String... extraJdkModules) {
        SourceSet maddiSupport = SourceSetImpl.sourceSetOf(Immutable.class);
        SourceSet slf4jApi = SourceSetImpl.sourceSetOf(org.slf4j.Logger.class);
        SourceSet logbackClassic = SourceSetImpl.sourceSetOf(Logger.class);
        SourceSet junitPlatform = SourceSetImpl.sourceSetOf(JUnitException.class);
        SourceSet jupiter = SourceSetImpl.sourceSetOf(Test.class, junitPlatform);
        SourceSet opentest4j = SourceSetImpl.sourceSetOf(AssertionFailedError.class);
        SourceSet annotations = SourceSetImpl.sourceSetOf(NotNull.class);
        List<String> jdkModules = List.of(extraJdkModules);

        return new JavaInspectorFactory() {
            @Override
            public List<SourceSet> dependencies() {
                return List.of(maddiSupport, slf4jApi, logbackClassic, junitPlatform, jupiter, opentest4j, annotations);
            }

            @Override
            public JavaInspector withSources(SourceSet sourceSet) throws IOException {
                return javaInspectorWithExtras(sourceSet, List.of(), jdkModules);
            }
        };
    }

    /**
     * Builds an openjdk {@link JavaInspector} with the lean test classpath (java.base + the non-JDK test
     * dependencies), plus optional extra source sets (each registered in the input configuration) and optional
     * extra JDK modules (e.g. {@code "java.desktop"}, {@code "java.net.http"}, {@code "java.sql"}). Used by
     * clone-bench style tests that parse many external files, each in its own per-directory source set, and by
     * tests that reference JDK modules beyond java.base.
     */
    public static @NonNull JavaInspector javaInspectorWithExtras(SourceSet primarySourceSet,
                                                                 List<SourceSet> extraSourceSets,
                                                                 List<String> extraJdkModules) throws IOException {
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet maddiSupport = SourceSetImpl.sourceSetOf(Immutable.class);
        SourceSet slf4jApi = SourceSetImpl.sourceSetOf(org.slf4j.Logger.class);
        SourceSet logbackClassic = SourceSetImpl.sourceSetOf(Logger.class);
        SourceSet junitPlatform = SourceSetImpl.sourceSetOf(JUnitException.class);
        SourceSet jupiter = SourceSetImpl.sourceSetOf(Test.class, junitPlatform);
        SourceSet opentest4j = SourceSetImpl.sourceSetOf(AssertionFailedError.class);
        SourceSet annotations = SourceSetImpl.sourceSetOf(NotNull.class);

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
        // java.desktop (awt/swing) and java.net.http are deliberately NOT preloaded here; they are only loaded when
        // explicitly added via extraJdkModules, and their analysis hints are then loaded on demand.
        if (extraJdkModules.contains("java.desktop")) {
            javaInspector.preload("java.desktop::java.awt");
            javaInspector.preload("java.desktop::javax.swing.");
        }
        if (extraJdkModules.contains("java.net.http")) {
            javaInspector.preload("java.net.http::java.net.http");
        }
        javaInspector.preload("org.slf4j");
        javaInspector.preload("org.junit.jupiter.api.");
        javaInspector.preload("org.e2immu.annotation.");

        List<SourceSet> classPathParts = new ArrayList<>(List.of(javaBase,
                maddiSupport, slf4jApi, logbackClassic, jupiter, junitPlatform, opentest4j, annotations));
        for (String jdkModule : extraJdkModules) {
            classPathParts.add(SourceSetImpl.jdkModule(jdkModule));
        }
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                .addSourceSets(primarySourceSet)
                .addClassPathParts(classPathParts.toArray(new SourceSet[0]));
        for (SourceSet extra : extraSourceSets) {
            builder.addSourceSets(extra);
        }
        InputConfiguration inputConfiguration = builder.build();
        javaInspector.initialize(inputConfiguration);
        return javaInspector;
    }
}
