/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.modification.link.io;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.language.inspection.integration.ToolChain;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.support.SetOnce;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.e2immu.language.inspection.resource.SourceSetImpl.sourceSetOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonTest {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CommonTest.class);

    protected JavaInspector javaInspector;
    protected Runtime runtime;
    protected PrepAnalyzer prepAnalyzer;
    protected final boolean loadAnalyzedPackageFiles;

    public CommonTest(boolean loadAnalyzedPackageFiles) {
        this.loadAnalyzedPackageFiles = loadAnalyzedPackageFiles;
    }

    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("graph-algorithm")).setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        String impl = System.getProperty("maddi_parser", "maddi");
        LOGGER.info("Parsing with {}", impl);
        List<String> directories;
        if ("maddi".equalsIgnoreCase(impl)) {
            javaInspector = new JavaInspectorImpl();
            InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder()
                    .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                    .addClassPath(JavaInspectorImpl.E2IMMU_SUPPORT)
                    .addClassPath(ToolChain.CLASSPATH_JUNIT)
                    .addClassPath(ToolChain.CLASSPATH_SLF4J_LOGBACK);
            builder.addSources("none");
            InputConfiguration inputConfiguration = builder.build();
            javaInspector.initialize(inputConfiguration);
            javaInspector.preload("java.util");
            directories = List.of(ToolChain.currentJdkAnalyzedPackages(), ToolChain.commonLibsAnalyzedPackages());
        } else {
            SourceSet javaBase = SourceSetImpl.javaBase();
            SourceSet orgSlf4j = sourceSetOf(org.slf4j.Logger.class, javaBase);
            SourceSet annotations = sourceSetOf(NotNull.class, javaBase);
            SourceSet maddiSupport = sourceSetOf(SetOnce.class, javaBase);
            SourceSet junitJupiter = sourceSetOf(Assertions.class, javaBase);

            SourceSet sources = new SourceSetImpl.Builder().setName(TEST_PROTOCOL).setUri(URI.create("file:/"))
                    .setDependencies(List.of(javaBase, orgSlf4j, annotations, maddiSupport, junitJupiter))
                    .build();

            InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                    .addSourceSets(sources)
                    .addClassPath("jmod:java.base")
                    .addClassPathParts(maddiSupport, orgSlf4j, annotations, junitJupiter)
                    .build();
            javaInspector = new org.e2immu.language.inspection.openjdk.JavaInspectorImpl();
            javaInspector.initialize(inputConfiguration);
            javaInspector.preload("java.base::java.util");
            javaInspector.preload("java.desktop::java.awt");
            directories = List.of("resource:/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/jdk/openjdk-26.0.1.jar",
                    "resource:/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/libs.jar");
            javaInspector.parse("a.b.X", "class X { }"); // kickstarts the system
        }
        new LoadAnalysisResults(javaInspector.mainSources()).go(javaInspector, directories);

        javaInspector.parse(JavaInspectorImpl.FAIL_FAST);
        runtime = javaInspector.runtime();
        prepAnalyzer = new PrepAnalyzer(runtime);
    }

    protected void prepWork(TypeInfo typeInfo) {
        List<TypeInfo> typesLoaded = javaInspector.compiledTypesManager().typesLoaded(true);
        assertTrue(typesLoaded.stream().anyMatch(ti -> "java.util.ArrayList".equals(ti.fullyQualifiedName())));

        prepAnalyzer.doPrimaryType(typeInfo);
    }
}
