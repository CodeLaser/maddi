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
import org.e2immu.annotation.Immutable;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.integration.JavaInspectorFactory;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnalysisHintsParser extends CommonTest {
    @BeforeAll
    public static void beforeAll() {
        ((Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @Test
    public void test() throws IOException, URISyntaxException {
        AnalysisHintsParser analysisHintsParser = createAnalysisHintsParser();
        AnalysisHints example = new AnalysisHints.Builder()
                .setLibraryName("example")
                .setAnalysisResultsDir(Path.of("build/"))
                .setHintsPath(Path.of("src/test/java"))
                .setPackagePrefix("org.e2immu.analyzer.aapi.parser.example")
                .build();
        JavaInspector javaInspector = analysisHintsParser.go(example);

        List<TypeInfo> types = analysisHintsParser.typesParsed();
        assertEquals(2, types.size());
        TypeInfo t1 = types.stream()
                .filter(ti -> "org.e2immu.analyzer.aapi.parser.example.popular.OrgSlf4J".equals(ti.fullyQualifiedName()))
                .findFirst().orElseThrow();
        String uri = t1.compilationUnitOrEnclosingType().getLeft().uri().toString();
        assertTrue(uri.endsWith("example/popular/OrgSlf4J.java"), "Have: " + uri);

        assertEquals(2, analysisHintsParser.getWarnings());

        Runtime runtime = javaInspector.runtime();

        // test String
        TypeInfo string = runtime.stringTypeInfo();
        TypeInfo charInfo = runtime.charTypeInfo();
        MethodInfo charConstructor = string.constructors().stream()
                .filter(c -> c.parameters().size() == 1 &&
                             c.parameters().getFirst().parameterizedType().typeInfo() == charInfo)
                .findFirst().orElseThrow();
        assertEquals("java.lang.String.<init>(char[])", charConstructor.fullyQualifiedName());
        // the annotations have not been copied, they're in a map!!
        assertEquals(0, charConstructor.annotations().size());
        List<AnnotationExpression> charInfoAnnots = analysisHintsParser.annotations(charConstructor);
        assertEquals(1, charInfoAnnots.size());
        assertEquals("Independent", charInfoAnnots.getFirst().typeInfo().simpleName());

        // test Iterable
        TypeInfo iterable = javaInspector.compiledTypesManager().get(Iterable.class);
        assertNotNull(iterable, "Cannot read Iterable from ctm");
        List<AnnotationExpression> iterableAnnots = analysisHintsParser.annotations(iterable);
        assertEquals("[@Container]", iterableAnnots.toString());

        // test Object
        List<AnnotationExpression> objectAnnots = analysisHintsParser.annotations(runtime.objectTypeInfo());
        assertEquals("[@ImmutableContainer(hc=true), @Independent]", objectAnnots.toString());
    }

    static @NonNull AnalysisHintsParser createAnalysisHintsParser() {
        SourceSet javaBase = SourceSetImpl.javaBase();
        SourceSet maddiSupport = SourceSetImpl.sourceSetOf(Immutable.class);
        SourceSet slf4jApi = SourceSetImpl.sourceSetOf(org.slf4j.Logger.class);
        JavaInspectorFactory javaInspectorFactory = new JavaInspectorFactory() {
            @Override
            public List<SourceSet> dependencies() {
                return List.of(maddiSupport, slf4jApi);
            }

            @Override
            public JavaInspector withSources(SourceSet sourceSet) throws IOException {
                JavaInspector javaInspector = new JavaInspectorImpl();
                javaInspector.preload("java.base::java.util");
                javaInspector.preload("org.slf4j");
                InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                        .addSourceSets(sourceSet)
                        .addClassPathParts(javaBase, maddiSupport, slf4jApi)
                        .build();
                javaInspector.initialize(inputConfiguration);
                return javaInspector;
            }
        };
        return new AnalysisHintsParser(javaInspectorFactory);
    }
}
