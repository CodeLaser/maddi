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

package io.codelaser.maddi.project.config;

import io.codelaser.maddi.inspection.api.resource.BuildFiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * How to make a project's configuration again, when its build files changed: run {@code command} in
 * {@code directory}, which rewrites the project's {@code inputConfiguration.json} where it is; or capture the build
 * again ({@code capture}). Exactly one of the two.
 */
public record Regenerate(List<String> command, Path directory, CaptureSpec capture) {

    /** The Gradle plugin's task that writes {@code build/inputConfiguration.json}. */
    public static final String GRADLE_TASK = "maddi-write-input-configuration";
    /** The Maven plugin's goal that writes {@code target/inputConfiguration.json}. */
    public static final String MAVEN_GOAL = "maddi:write-input-configuration";

    public Regenerate {
        command = command == null ? List.of() : List.copyOf(command);
        if (command.isEmpty() == (capture == null)) {
            throw new IllegalArgumentException("a regeneration is a command or a capture, not both or neither");
        }
    }

    public static Regenerate command(List<String> command, Path directory) {
        return new Regenerate(command, directory, null);
    }

    public static Regenerate capture(CaptureSpec spec) {
        return new Regenerate(List.of(), spec.root(), spec);
    }

    public boolean isCapture() {
        return capture != null;
    }

    public String describe() {
        return capture != null ? capture.describe() : String.join(" ", command);
    }

    /**
     * How to write {@code inputConfiguration} again, from who wrote it: the Gradle plugin's task or the Maven plugin's
     * goal, run in the project whose build directory holds the file, with the build's wrapper when there is one.
     * Null for any other producer: then only a command given at configuration can.
     */
    public static Regenerate defaultFor(BuildFiles buildFiles, Path inputConfiguration) {
        if (buildFiles == null) return null;
        Path parent = inputConfiguration.toAbsolutePath().normalize().getParent();
        Path projectDir = parent == null ? null : parent.getParent();
        if (projectDir == null) return null;
        Path stop = buildFiles.root() == null ? projectDir : Path.of(buildFiles.root());
        return switch (buildFiles.declaredBy()) {
            case BuildFiles.GRADLE_PLUGIN -> !"build".equals(parent.getFileName().toString()) ? null
                    : command(List.of(wrapper(projectDir, stop, "gradlew", "gradle"), "-q", "-p",
                    projectDir.toString(), GRADLE_TASK), projectDir);
            case BuildFiles.MAVEN_PLUGIN -> !"target".equals(parent.getFileName().toString()) ? null
                    : command(List.of(wrapper(projectDir, stop, "mvnw", "mvn"), "-q", "-f",
                    projectDir.resolve("pom.xml").toString(), MAVEN_GOAL), projectDir);
            default -> null;
        };
    }

    /* the wrapper script nearest above the project, up to the build's root; else the tool on the PATH */
    private static String wrapper(Path from, Path root, String script, String tool) {
        Path stop = root.toAbsolutePath().normalize();
        for (Path dir = from; dir != null; dir = dir.getParent()) {
            if (Files.isExecutable(dir.resolve(script))) return dir.resolve(script).toString();
            if (dir.equals(stop)) break;
        }
        return tool;
    }
}
