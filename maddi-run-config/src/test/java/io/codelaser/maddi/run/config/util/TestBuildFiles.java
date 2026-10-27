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

package io.codelaser.maddi.run.config.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.codelaser.maddi.inspection.api.resource.BuildFiles;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.BuildFilesCapture;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The build files an input configuration declares, hashed at capture time, and their JSON ({@link BuildFiles}). */
public class TestBuildFiles {

    private static Path write(Path root, String relative, String content) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }

    private static Path gradleTree(Path root) throws IOException {
        write(root, "settings.gradle.kts", "include(\"a\")");
        write(root, "a/build.gradle.kts", "plugins { java }");
        write(root, "a/src/main/java/module-info.java", "module a { }");
        write(root, "a/build/generated/build.gradle.kts", "output, not a build file");
        write(root, ".gradle/8.0/build.gradle.kts", "tool state");
        write(root, "README.md", "not a build file");
        return root;
    }

    // the configuration the tree's build files shaped: one source set, a/src/main/java
    private static InputConfiguration configuration(Path root) {
        return new InputConfigurationImpl.Builder().setWorkingDirectory(root.toString())
                .addSource("a/main", "a/src/main/java").build();
    }

    @Test
    public void stableUntilABuildFileChanges(@TempDir Path tmp) throws IOException {
        Path root = gradleTree(tmp);
        BuildFiles first = BuildFilesCapture.capture(BuildFiles.GRADLE_PLUGIN, root, BuildFiles.GRADLE_GLOBS, List.of(),
                configuration(root));
        assertEquals(List.of("a/build.gradle.kts", "a/src/main/java/module-info.java", "settings.gradle.kts"),
                List.copyOf(first.sha256().keySet()), "build output and tool state are not searched");
        BuildFiles second = BuildFilesCapture.capture(BuildFiles.GRADLE_PLUGIN, root, BuildFiles.GRADLE_GLOBS,
                List.of(), configuration(root));
        assertEquals(first.sha256(), second.sha256(), "an unchanged tree hashes the same");

        write(root, "a/build.gradle.kts", "plugins { java }\ndependencies { implementation(\"x:y:1\") }");
        BuildFiles third = BuildFilesCapture.capture(BuildFiles.GRADLE_PLUGIN, root, BuildFiles.GRADLE_GLOBS,
                List.of());
        assertNotEquals(first.sha256().get("a/build.gradle.kts"), third.sha256().get("a/build.gradle.kts"));
        assertEquals(first.sha256().get("settings.gradle.kts"), third.sha256().get("settings.gradle.kts"));
        assertEquals(root.toAbsolutePath().normalize().toString(), third.root());
        assertNotNull(java.time.Instant.parse(third.capturedAt()));
    }

    @Test
    public void moduleInfoOfTheSourceDirectoriesIsAlwaysWatched(@TempDir Path tmp) throws IOException {
        Path root = gradleTree(tmp);
        for (BuildFiles buildFiles : List.of(BuildFilesCapture.none(root, configuration(root)),
                BuildFilesCapture.capture(BuildFiles.COMPILE_LOG, root, List.of(), List.of(), configuration(root)),
                BuildFilesCapture.capture(BuildFiles.MAVEN_PLUGIN, root, BuildFiles.MAVEN_GLOBS, List.of(),
                        configuration(root)))) {
            assertTrue(buildFiles.globs().contains("a/src/main/java/module-info.java"), buildFiles.toString());
            assertTrue(buildFiles.sha256().containsKey("a/src/main/java/module-info.java"), buildFiles.toString());
        }
        BuildFiles none = BuildFilesCapture.none(root, configuration(root));
        assertEquals(BuildFiles.NONE, none.declaredBy());
        assertEquals(List.of("a/src/main/java/module-info.java"), none.globs());
    }

    /**
     * An audit's root is the common parent of the repositories it captures (~/git): a module-info, or a build file,
     * of another checkout under it is none of this configuration's business, and is not even searched.
     */
    @Test
    public void notTheWholeRoot(@TempDir Path tmp) throws IOException {
        gradleTree(tmp.resolve("maddi"));
        write(tmp, "test-oss/guava/build.gradle.kts", "another checkout");
        write(tmp, "test-oss/guava/src/main/java/module-info.java", "module com.google.common { }");
        InputConfiguration maddi = new InputConfigurationImpl.Builder().setWorkingDirectory(tmp.toString())
                .addSource("a/main", "maddi/a/src/main/java").build();
        BuildFiles buildFiles = BuildFilesCapture.capture(BuildFiles.COMPILE_LOG, tmp,
                List.of("maddi/settings.gradle.kts", "maddi/**/build.gradle.kts"), List.of(), maddi);
        assertEquals(List.of("maddi/a/build.gradle.kts", "maddi/a/src/main/java/module-info.java",
                "maddi/settings.gradle.kts"), List.copyOf(buildFiles.sha256().keySet()));
        assertEquals("maddi", BuildFilesCapture.literalPrefix("maddi/**/build.gradle.kts"));
        assertEquals("", BuildFilesCapture.literalPrefix("**/pom.xml"));
    }

    @Test
    public void namedFilesOutsideTheRoot(@TempDir Path tmp) throws IOException {
        Path root = gradleTree(tmp.resolve("project"));
        Path included = write(tmp, "build-logic/build.gradle.kts", "plugins { `kotlin-dsl` }");
        BuildFiles buildFiles = BuildFilesCapture.capture(BuildFiles.GRADLE_PLUGIN, root, BuildFiles.GRADLE_GLOBS,
                List.of(included));
        assertTrue(buildFiles.sha256().containsKey("../build-logic/build.gradle.kts"), buildFiles.toString());
    }

    @Test
    public void json(@TempDir Path tmp) throws IOException {
        ObjectMapper objectMapper = JsonStreaming.objectMapper();
        BuildFiles buildFiles = BuildFilesCapture.capture(BuildFiles.GRADLE_PLUGIN, gradleTree(tmp),
                BuildFiles.GRADLE_GLOBS, List.of());
        InputConfiguration withFiles = new InputConfigurationImpl(Path.of("."), List.of(), List.of(), null, 0)
                .withBuildFiles(buildFiles);
        String json = objectMapper.writeValueAsString(withFiles);
        assertTrue(json.contains("\"buildFiles\":{\"declaredBy\":\"gradle-plugin\""), json);
        InputConfiguration copy = objectMapper.readerFor(InputConfiguration.class).readValue(json);
        assertEquals(buildFiles, copy.buildFiles());

        // every configuration written before: no key, and it reads as before
        String old = objectMapper.writeValueAsString(new InputConfigurationImpl(Path.of("."), List.of(), List.of(),
                null, 0));
        assertFalse(old.contains("buildFiles"), old);
        InputConfiguration oldCopy = objectMapper.readerFor(InputConfiguration.class).readValue(old);
        assertNull(oldCopy.buildFiles());
        assertEquals(Path.of("."), oldCopy.workingDirectory());
    }
}
