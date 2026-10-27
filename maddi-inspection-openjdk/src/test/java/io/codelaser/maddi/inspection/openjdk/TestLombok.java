/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.openjdk;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static io.codelaser.maddi.inspection.api.integration.JavaInspector.TEST_PROTOCOL;
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
        // the lombok-*.jar goes on the parsed classpath (and only there, see LombokJar), so javac both resolves the
        // @Data/@Getter annotations and loads the annotation processor from it
        SourceSet lombok = LombokJar.sourceSet(javaBase);
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
            package io.codelaser.maddi.test;
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
                Map.of(sourceSet, Map.of("io.codelaser.maddi.test.X", INPUT)), options).parseResult();
        return parseResult.findType("io.codelaser.maddi.test.X");
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
