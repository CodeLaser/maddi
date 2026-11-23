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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import io.codelaser.maddi.inspection.api.parser.Summary;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;

/**
 * trino's {@code core/trino-spi} declares {@code requires jdk.incubator.vector;} in its {@code module-info.java}
 * (trino 43ce7d91552, line 20) and four classes use the Vector API ({@code ByteArrayBlockEncoding} and the Int,
 * Long and Short ones). Its javac line has no {@code --add-modules}: javac compiles the set as a named module, and
 * a named module's {@code requires} puts the module in the graph by itself. The configuration therefore carries no
 * {@code addModules} for the set, and the parse dropped the four files with "package jdk.incubator.vector is not
 * visible" -- so everything only they call would be reported dead.
 * <p>
 * The source set itself says what javac needs: a {@code requires} of a JDK module that is not resolved by default
 * (an incubator module) must reach the parse as {@code --add-modules}, just as one the build passes explicitly.
 */
public class TestRequiresIncubatorModule {

    private static final String MODULE_INFO = """
            module spi {
                requires jdk.incubator.vector;
                exports spi;
            }
            """;
    private static final String ENCODING = """
            package spi;
            import jdk.incubator.vector.ByteVector;
            import jdk.incubator.vector.VectorSpecies;
            public class Encoding {
                private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_PREFERRED;
                public static int lanes() { return SPECIES.length(); }
            }
            """;

    /**
     * The release the build states, below the one the engine runs on: javac then compiles with {@code --release N}
     * against the JDK's stored API data, and there an incubator module is not resolved for the unnamed module unless
     * it is added explicitly. On trino the configuration states 25 for trino-spi.
     */
    private static final int OLDER_RELEASE = Runtime.version().feature() - 1;

    @DisplayName("a module that requires jdk.incubator.vector parses its users, ignoring modules")
    @Test
    public void ignoringModules(@TempDir Path dir) throws IOException {
        assertParses(dir, true, 0);
    }

    @DisplayName("a module that requires jdk.incubator.vector parses its users, as a module")
    @Test
    public void asModule(@TempDir Path dir) throws IOException {
        assertParses(dir, false, 0);
    }

    @DisplayName("a module that requires jdk.incubator.vector parses its users, ignoring modules, older release")
    @Test
    public void ignoringModulesOlderRelease(@TempDir Path dir) throws IOException {
        assertParses(dir, true, OLDER_RELEASE);
    }

    @DisplayName("a module that requires jdk.incubator.vector parses its users, as a module, older release")
    @Test
    public void asModuleOlderRelease(@TempDir Path dir) throws IOException {
        assertParses(dir, false, OLDER_RELEASE);
    }

    /**
     * A required module that the release the set compiles against does not have: jdk.incubator.vector arrived in 16,
     * so release 15 has no such module. javac refuses {@code --add-modules} of a module it cannot find ("module not
     * found") and rejects the whole task, so the module is added only when the platform javac compiles against has
     * it -- the running JDK has it, release 15 does not. A class that does not use the module parses either way.
     */
    @DisplayName("a required module the set's release does not have is not added, the rest of the set parses")
    @Test
    public void requiredModuleMissingFromRelease(@TempDir Path dir) throws IOException {
        Path src = dir.resolve("src");
        Files.createDirectories(src.resolve("spi"));
        Files.writeString(src.resolve("module-info.java"), MODULE_INFO);
        Files.writeString(src.resolve("spi/Plain.java"), "package spi;\npublic class Plain { }\n");
        SourceSet spi = new SourceSetImpl.Builder().setName("spi/main").setSourceDirectories(List.of(src))
                .setUri(src.toUri()).setModule(true).setSourceRelease(15).build();
        Summary summary = parse(spi, true);

        List<String> problems = Stream.concat(summary.parseExceptions().stream(), summary.parseWarnings().stream())
                .map(e -> e.uri() + ": " + e.getMessage()).toList();
        assertEquals(List.of(), problems, "the parse reports these problems; it should report none");
        assertEquals(List.of("spi.Plain"), summary.types().stream().map(TypeInfo::fullyQualifiedName).sorted().toList(),
                "the classes the parse produced; spi.Plain is the only class in the source set");
    }

    /**
     * The test source set of a module has no module-info.java of its own. Maven compiles it INTO the main module
     * ({@code --patch-module}), so the main module's {@code requires} apply to the tests as well, and the build's
     * javac line for the tests has no {@code --add-modules} either. The test set inherits the required modules of
     * the source set of its own build unit that has the descriptor.
     */
    @DisplayName("a test set without module-info uses the modules its build unit's main module requires")
    @Test
    public void testSetOfTheModule(@TempDir Path dir) throws IOException {
        Path main = dir.resolve("spi/src/main/java");
        Path test = dir.resolve("spi/src/test/java");
        Files.createDirectories(main.resolve("spi"));
        Files.createDirectories(test.resolve("spi"));
        Files.writeString(main.resolve("module-info.java"), MODULE_INFO);
        Files.writeString(main.resolve("spi/Plain.java"), "package spi;\npublic class Plain { }\n");
        Files.writeString(test.resolve("spi/EncodingTest.java"), ENCODING.replace("class Encoding", "class EncodingTest"));
        String buildUnit = dir.resolve("spi").toString();
        SourceSet mainSet = new SourceSetImpl.Builder().setName("spi/main").setBuildUnit(buildUnit)
                .setSourceDirectories(List.of(main)).setUri(main.toUri()).setModule(true)
                .setSourceRelease(OLDER_RELEASE).build();
        SourceSet testSet = new SourceSetImpl.Builder().setName("spi/test").setBuildUnit(buildUnit).setTest(true)
                .setSourceDirectories(List.of(test)).setUri(test.toUri()).setDependencies(List.of(mainSet))
                .setSourceRelease(OLDER_RELEASE).build();
        InputConfiguration ic = new InputConfigurationImpl.Builder().addSourceSets(mainSet, testSet)
                .addClassPath("jmod:java.base").build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(ic);
        Summary summary = javaInspector.parse(Map.of(), new JavaInspector.ParseOptions.Builder()
                .setFailFast(false).setIgnoreModule(true).build());

        List<String> problems = Stream.concat(summary.parseExceptions().stream(), summary.parseWarnings().stream())
                .map(e -> e.uri() + ": " + e.getMessage()).toList();
        assertEquals(List.of(), problems, "the parse reports these problems; it should report none");
        assertEquals(List.of("spi.EncodingTest", "spi.Plain"),
                summary.types().stream().map(TypeInfo::fullyQualifiedName)
                        .filter(n -> !n.endsWith(".package-info")).sorted().toList(),
                "the classes the parse produced: spi.Plain in main, spi.EncodingTest in test");
    }

    @DisplayName("the modules of a release come from javac's release data: jdk.incubator.vector from 16 on")
    @Test
    public void modulesOfRelease() {
        Set<String> release15 = JavaInspectorImpl.modulesOfRelease(15);
        Set<String> release17 = JavaInspectorImpl.modulesOfRelease(17);
        assertTrue(release15.contains("java.base") && release17.contains("java.base"),
                "java.base is a module of every release with modules");
        assertFalse(release15.contains("jdk.incubator.vector"), "release 15 has no jdk.incubator.vector");
        assertTrue(release17.contains("jdk.incubator.vector"), "release 17 has jdk.incubator.vector");
    }

    @DisplayName("the modules of a JRE come from its runtime image")
    @Test
    public void modulesOfJre() {
        Set<String> modules = JavaInspectorImpl.modulesOfJre(Path.of(System.getProperty("java.home")));
        assertEquals(ModuleFinder.ofSystem().findAll().stream().map(r -> r.descriptor().name())
                        .collect(Collectors.toSet()), modules,
                "left: the running JDK's modules as ModuleFinder lists them; right: as read from its image");
    }

    private static Summary parse(SourceSet sourceSet, boolean ignoreModule) throws IOException {
        InputConfiguration ic = new InputConfigurationImpl.Builder().addSourceSets(sourceSet)
                .addClassPath("jmod:java.base").build();
        JavaInspector javaInspector = new JavaInspectorImpl();
        javaInspector.initialize(ic);
        return javaInspector.parse(Map.of(), new JavaInspector.ParseOptions.Builder()
                .setFailFast(false).setIgnoreModule(ignoreModule).build());
    }

    private static void assertParses(Path dir, boolean ignoreModule, int sourceRelease) throws IOException {
        Path src = dir.resolve("src");
        Files.createDirectories(src.resolve("spi"));
        Files.writeString(src.resolve("module-info.java"), MODULE_INFO);
        Files.writeString(src.resolve("spi/Encoding.java"), ENCODING);
        // as trino's configuration has it: a module, and no addModules
        SourceSet spi = new SourceSetImpl.Builder().setName("core/trino-spi/main").setSourceDirectories(List.of(src))
                .setUri(src.toUri()).setModule(true).setSourceRelease(sourceRelease).build();
        Summary summary = parse(spi, ignoreModule);

        List<String> problems = Stream.concat(summary.parseExceptions().stream(), summary.parseWarnings().stream())
                .map(e -> e.uri() + ": " + e.getMessage()).toList();
        assertEquals(List.of(), problems, "the parse reports these problems; it should report none");
        // a module's parse also gives the package-info type the engine makes for the exported package
        assertEquals(List.of("spi.Encoding"),
                summary.types().stream().map(TypeInfo::fullyQualifiedName)
                        .filter(n -> !n.endsWith(".package-info")).sorted().toList(),
                "the classes the parse produced; spi.Encoding is the only class in the source set");
    }
}
