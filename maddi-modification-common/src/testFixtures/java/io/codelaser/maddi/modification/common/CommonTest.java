package io.codelaser.maddi.modification.common;

import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.support.SetOnce;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.integration.JavaInspectorFactory;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
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
        // Two source sets since the 0.9.1 split: sourceSetOf() resolves the ARTIFACT containing the
        // class. The annotations now live in maddi-annotation while Either/SetOnce stay in
        // maddi-support, so naming only Immutable here silently drops maddi-support from the test
        // classpath -- which is how OrgE2immuSupport stopped resolving.
        SourceSet maddiAnnotation = SourceSetImpl.sourceSetOf(Immutable.class);
        SourceSet maddiSupport = SourceSetImpl.sourceSetOf(SetOnce.class, maddiAnnotation);
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
                return List.of(maddiAnnotation, maddiSupport, slf4jApi, logbackClassic, junitPlatform, jupiter,
                        opentest4j, annotations);
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
        // Two source sets since the 0.9.1 split: sourceSetOf() resolves the ARTIFACT containing the
        // class. The annotations now live in maddi-annotation while Either/SetOnce stay in
        // maddi-support, so naming only Immutable here silently drops maddi-support from the test
        // classpath -- which is how OrgE2immuSupport stopped resolving.
        SourceSet maddiAnnotation = SourceSetImpl.sourceSetOf(Immutable.class);
        SourceSet maddiSupport = SourceSetImpl.sourceSetOf(SetOnce.class, maddiAnnotation);
        SourceSet slf4jApi = SourceSetImpl.sourceSetOf(org.slf4j.Logger.class);
        SourceSet logbackClassic = SourceSetImpl.sourceSetOf(Logger.class);
        SourceSet junitPlatform = SourceSetImpl.sourceSetOf(JUnitException.class);
        SourceSet jupiter = SourceSetImpl.sourceSetOf(Test.class, junitPlatform);
        SourceSet opentest4j = SourceSetImpl.sourceSetOf(AssertionFailedError.class);
        SourceSet annotations = SourceSetImpl.sourceSetOf(NotNull.class);

        JavaInspector javaInspector = new io.codelaser.maddi.inspection.openjdk.JavaInspectorImpl();
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
        javaInspector.preload("io.codelaser.maddi.annotation.");

        List<SourceSet> classPathParts = new ArrayList<>(List.of(javaBase,
                maddiAnnotation, maddiSupport, slf4jApi, logbackClassic, jupiter, junitPlatform, opentest4j, annotations));
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
