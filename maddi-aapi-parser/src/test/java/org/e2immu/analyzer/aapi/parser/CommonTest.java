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

package org.e2immu.analyzer.aapi.parser;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.e2immu.analyzer.modification.common.defaults.ShallowAnalyzer;
import org.e2immu.annotation.Immutable;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.integration.JavaInspectorFactory;
import org.e2immu.language.inspection.api.resource.CompiledTypesManager;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.e2immu.util.internal.graph.G;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.ImmutableImpl.MUTABLE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.DEPENDENT;
import static org.junit.jupiter.api.Assertions.*;

public class CommonTest {
    private static CompiledTypesManager compiledTypesManager;

    static List<TypeInfo> allTypes;
    static List<TypeInfo> sorted;
    static G<TypeInfo> graph;
    private static Runtime runtime;
    private static JavaInspector javaInspector;

    public static CompiledTypesManager compiledTypesManager() {
        return compiledTypesManager;
    }

    public static Runtime runtime() {
        return runtime;
    }

    public static SourceSet mainSources() {
        return javaInspector.mainSources();
    }

    @BeforeAll
    public static void beforeAll() throws IOException, URISyntaxException {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((Logger) LoggerFactory.getLogger("org.e2immu.analyzer.aapi")).setLevel(Level.DEBUG);

        AnalysisHintsParser analysisHintsParser = createAnalysisHintsParser();
        AnalysisHints test = new AnalysisHints.Builder()
                .setLibraryName("test")
                .setAnalysisResultsDir(Path.of("build/"))
                .setHintsPath(Path.of("../maddi-aapi-archive/src/main/java"))
                .setPackagePrefix("org.e2immu.analyzer.aapi.archive")
                .build();

        javaInspector = analysisHintsParser.go(test);
        runtime = javaInspector.runtime();
        compiledTypesManager = javaInspector.compiledTypesManager();

        TypeInfo httpRequest = compiledTypesManager.get("java.net.http.HttpRequest", null);
        assertNotNull(httpRequest);

        ShallowAnalyzer shallowAnalyzer = new ShallowAnalyzer(runtime, analysisHintsParser, true);
        ShallowAnalyzer.Result sr = shallowAnalyzer.go(analysisHintsParser.types());

        sorted = sr.sorted();
        graph = sr.typeGraph();
        allTypes = sr.allTypes();
    }

    private static @NonNull AnalysisHintsParser createAnalysisHintsParser() throws URISyntaxException {
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet maddiSupport = SourceSetImpl.sourceSetOf(Immutable.class);
        SourceSet slf4jApi = SourceSetImpl.sourceSetOf(org.slf4j.Logger.class);
        SourceSet logbackClassic = SourceSetImpl.sourceSetOf(Logger.class);
        SourceSet junitPlatform = SourceSetImpl.sourceSetOf(JUnitException.class);
        SourceSet jupiter = SourceSetImpl.sourceSetOf(Test.class, junitPlatform);

        JavaInspectorFactory javaInspectorFactory = new JavaInspectorFactory() {
            @Override
            public List<SourceSet> dependencies() {
                return List.of(maddiSupport, slf4jApi, logbackClassic, junitPlatform, jupiter);
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
                InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                        .addSourceSets(sourceSet)
                        .addClassPathParts(javaBase, maddiSupport, slf4jApi, logbackClassic, jupiter, junitPlatform)
                        .build();
                javaInspector.initialize(inputConfiguration);
                return javaInspector;
            }
        };
        return new AnalysisHintsParser(javaInspectorFactory);
    }

    protected void testImmutableContainer(TypeInfo typeInfo, boolean hcImmutable) {
        Value.Immutable immutable = typeInfo.analysis().getOrDefault(IMMUTABLE_TYPE, MUTABLE);
        Value.Immutable expectImmutable = hcImmutable
                ? ValueImpl.ImmutableImpl.IMMUTABLE_HC : ValueImpl.ImmutableImpl.IMMUTABLE;
        assertSame(expectImmutable, immutable);

        Value.Independent independent = typeInfo.analysis().getOrDefault(INDEPENDENT_TYPE, DEPENDENT);
        assertSame(ValueImpl.IndependentImpl.INDEPENDENT, independent);

        boolean container = typeInfo.analysis().getOrDefault(CONTAINER_TYPE, ValueImpl.BoolImpl.FALSE).isTrue();
        assertTrue(container);
    }
}
