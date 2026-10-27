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
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.ParseResult;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A source set's package restriction must hold when its source directory is written RELATIVE to the configuration's
 * working directory.
 * <p>
 * The inspector resolves such a directory against {@code workingDirectory} when it walks it, but the package
 * inference behind the restriction resolved it against the process's current directory. No file then lay below it,
 * every package was "unknown", and every file was kept "despite the package restriction". Found on
 * TestElasticsearchServer: its checked-in slice is relative, and with {@code restrictToPackages} set it still
 * analysed all 4872 types of elasticsearch's server/main.
 */
public class TestRestrictToPackagesRelativeSourceDirectory {

    @TempDir
    private Path project;

    @DisplayName("restrictToPackages with a source directory relative to workingDirectory")
    @Test
    public void restrictionHolds() throws Exception {
        Path src = project.resolve("src/main/java");
        Files.createDirectories(src.resolve("a/kept/deeper"));
        Files.createDirectories(src.resolve("a/dropped"));
        Files.writeString(src.resolve("a/kept/K.java"),
                "package a.kept; public class K { a.dropped.D d; @a.dropped.Inject public K() { } }\n");
        Files.writeString(src.resolve("a/kept/deeper/L.java"), "package a.kept.deeper; public class L { }\n");
        Files.writeString(src.resolve("a/dropped/D.java"), "package a.dropped; public class D { }\n");
        // outside the restriction, so parsed ON DEMAND from the source path: it must still get its hierarchy
        // (an annotation type without java.lang.annotation.Annotation failed to commit; elasticsearch's guice.Inject)
        Files.writeString(src.resolve("a/dropped/Inject.java"),
                "package a.dropped; @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)"
                + " public @interface Inject { }\n");

        SourceSet main = new SourceSetImpl.Builder().setName("main")
                .setSourceDirectories(List.of(Path.of("src/main/java")))   // relative, as in a checked-in slice
                .setUri(src.toUri())
                .setRestrictToPackages(Set.of("a.kept."))
                .build();
        JavaInspector javaInspector = new JavaInspectorImpl(true, false);
        javaInspector.initialize(new InputConfigurationImpl.Builder()
                .setWorkingDirectory(project.toString())
                .addSourceSets(main)
                .addClassPath(InputConfigurationImpl.DEFAULT_MODULES)
                .build());
        ParseResult parseResult = javaInspector.parse(Map.of(), JavaInspectorImpl.DETAILED_SOURCES).parseResult();

        assertEquals(List.of("a.kept.K", "a.kept.deeper.L"), parseResult.primaryTypes().stream()
                .map(TypeInfo::fullyQualifiedName).sorted().toList());
        TypeInfo inject = parseResult.findType("a.kept.K").findConstructor(0).annotations().getFirst()
                .typeInfo();
        assertEquals("java.lang.annotation.Annotation",
                inject.interfacesImplemented().getFirst().typeInfo().fullyQualifiedName());
    }
}
