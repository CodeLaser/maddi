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

package org.e2immu.language.inspection.openjdk;

import lombok.Data;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.e2immu.language.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
import static org.e2immu.language.inspection.resource.SourceSetImpl.sourceSetOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the <em>production</em> openjdk parse path ({@link JavaInspectorImpl}) with Lombok. Unlike the
 * {@code maddi-java-openjdk} lombok tests -- which build their own javac task with the processor hard-wired -- this
 * drives the real inspector, so it pins down that {@code createTask} enables the real Lombok annotation processor
 * (instead of {@code -proc:none}) exactly when {@code ParseOptions.lombok()} is set, and leaves it disabled otherwise.
 */
public class TestLombok {

    private JavaInspector javaInspector;
    private SourceSet sourceSet;

    @BeforeEach
    public void before() throws IOException {
        javaInspector = new JavaInspectorImpl();
        SourceSet javaBase = SourceSetImpl.javaBase();
        // the lombok-*.jar (resolved from lombok.Data's code source) goes on the parsed classpath, so javac can both
        // resolve the @Data/@Getter annotations and load the annotation processor from it
        SourceSet lombok = sourceSetOf(Data.class, javaBase);
        sourceSet = new SourceSetImpl.Builder().setName(TEST_PROTOCOL + "1").setUri(URI.create("file:/"))
                .setDependencies(List.of(javaBase, lombok)).build();
        InputConfiguration inputConfiguration = new InputConfigurationImpl.Builder()
                .addSourceSets(sourceSet)
                .addClassPath("jmod:java.base")
                .addClassPathParts(lombok)
                .build();
        javaInspector.initialize(inputConfiguration);
    }

    // non-final fields: compiles with or without the Lombok processor, so the flag -- not a compile error -- is what
    // decides whether the getters exist
    @Language("java")
    private static final String INPUT = """
            package org.e2immu.test;
            import lombok.Getter;
            @Getter
            public class X {
                private String s;
                private int t;
            }
            """;

    private TypeInfo parseWithLombok(boolean lombok) {
        JavaInspector.ParseOptions options = new JavaInspector.ParseOptions.Builder()
                .setFailFast(true).setDetailedSources(true).setLombok(lombok).build();
        ParseResult parseResult = javaInspector.parseMultiSourceSet(
                Map.of(sourceSet, Map.of("org.e2immu.test.X", INPUT)), options).parseResult();
        return parseResult.findType("org.e2immu.test.X");
    }

    @Test
    public void lombokEnabledGeneratesGetters() {
        TypeInfo x = parseWithLombok(true);
        MethodInfo getS = x.findUniqueMethod("getS", 0);
        MethodInfo getT = x.findUniqueMethod("getT", 0);
        // real Lombok stamps its generated members with @lombok.Generated -- proves these came from the processor
        assertTrue(getS.annotations().stream().anyMatch(a -> "Generated".equals(a.typeInfo().simpleName())),
                "expected Lombok @Generated on the generated getter getS");
        assertTrue(getT.annotations().stream().anyMatch(a -> "Generated".equals(a.typeInfo().simpleName())),
                "expected Lombok @Generated on the generated getter getT");
    }

    @Test
    public void lombokDisabledGeneratesNothing() {
        TypeInfo x = parseWithLombok(false);
        // -proc:none: @Getter is inert, so no getters are synthesized
        assertThrows(NoSuchElementException.class, () -> x.findUniqueMethod("getS", 0));
        assertThrows(NoSuchElementException.class, () -> x.findUniqueMethod("getT", 0));
    }
}
