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

    @Test
    public void stableUntilABuildFileChanges(@TempDir Path tmp) throws IOException {
        Path root = gradleTree(tmp);
        BuildFiles first = BuildFilesCapture.capture(BuildFiles.GRADLE_PLUGIN, root, BuildFiles.GRADLE_GLOBS, List.of());
        assertEquals(List.of("a/build.gradle.kts", "a/src/main/java/module-info.java", "settings.gradle.kts"),
                List.copyOf(first.sha256().keySet()), "build output and tool state are not searched");
        BuildFiles second = BuildFilesCapture.capture(BuildFiles.GRADLE_PLUGIN, root, BuildFiles.GRADLE_GLOBS,
                List.of());
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
    public void moduleInfoIsAlwaysWatched(@TempDir Path tmp) throws IOException {
        Path root = gradleTree(tmp);
        for (BuildFiles buildFiles : List.of(BuildFilesCapture.none(root),
                BuildFilesCapture.capture(BuildFiles.COMPILE_LOG, root, List.of(), List.of()),
                BuildFilesCapture.capture(BuildFiles.MAVEN_PLUGIN, root, BuildFiles.MAVEN_GLOBS, List.of()))) {
            assertTrue(buildFiles.globs().contains(BuildFiles.MODULE_INFO), buildFiles.toString());
            assertTrue(buildFiles.sha256().containsKey("a/src/main/java/module-info.java"), buildFiles.toString());
        }
        BuildFiles none = BuildFilesCapture.none(root);
        assertEquals(BuildFiles.NONE, none.declaredBy());
        assertEquals(List.of(BuildFiles.MODULE_INFO), none.globs());
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
