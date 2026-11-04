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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.codelaser.maddi.inspection.api.resource.BuildFiles;
import io.codelaser.maddi.inspection.resource.BuildFilesCapture;

/**
 * Whether a configuration still matches its build: the build files recorded with it ({@link BuildFiles}, the
 * {@code buildFiles} key of {@code inputConfiguration.json}) hashed again and compared.
 */
public final class BuildFileCheck {
    private BuildFileCheck() {
    }

    public enum State {
        /** Every recorded build file hashes as it did. */
        FRESH,
        /** A build file changed, appeared or went: the configuration may no longer be what the build compiles. */
        STALE,
        /** Nothing was recorded to compare with. */
        UNCHECKED
    }

    /** @param changed the files that differ, relative to the capture's root, each with what happened to it */
    public record Freshness(State state, List<String> changed) {
        public Freshness {
            changed = List.copyOf(changed);
        }

        public static Freshness unchecked() {
            return new Freshness(State.UNCHECKED, List.of());
        }

        public Map<String, Object> describe() {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("buildFiles", state.name());
            if (!changed.isEmpty()) out.put("changedBuildFiles", changed.size() > 20
                    ? List.copyOf(changed.subList(0, 20)) + " and " + (changed.size() - 20) + " more" : changed);
            return out;
        }
    }

    /**
     * @param sourceDirectories the configuration's own source directories: a module-info that appeared at the top of
     *                          one since is a change no glob finds
     */
    public static Freshness check(BuildFiles recorded, Collection<Path> sourceDirectories) {
        if (recorded == null || recorded.root() == null
            || recorded.sha256().isEmpty() && recorded.globs().isEmpty()) {
            return Freshness.unchecked();
        }
        Path root = Path.of(recorded.root());
        // the recorded files by name as well as by glob: a file outside the root (an included build, '../x') is
        // found by no glob
        List<Path> named = recorded.sha256().keySet().stream().map(root::resolve).toList();
        BuildFiles now = BuildFilesCapture.capture(recorded.declaredBy(), root, recorded.globs(), named);
        List<String> changed = new ArrayList<>();
        for (var entry : recorded.sha256().entrySet()) {
            String hash = now.sha256().get(entry.getKey());
            if (hash == null) changed.add(entry.getKey() + " (gone)");
            else if (!hash.equals(entry.getValue())) changed.add(entry.getKey() + " (changed)");
        }
        for (String key : now.sha256().keySet()) {
            if (!recorded.sha256().containsKey(key)) changed.add(key + " (new)");
        }
        // a module-info is recorded where there was one; one that appeared since has no glob to be found by
        for (Path moduleInfo : moduleInfos(sourceDirectories)) {
            String key = root.relativize(moduleInfo).toString().replace(moduleInfo.getFileSystem().getSeparator(), "/");
            if (!recorded.sha256().containsKey(key) && !now.sha256().containsKey(key)) changed.add(key + " (new)");
        }
        return new Freshness(changed.isEmpty() ? State.FRESH : State.STALE, changed);
    }

    /**
     * The check of an {@code inputConfiguration.json}, from what it records. A file that is gone (a {@code clean}
     * deletes one in a build directory) is STALE: there is nothing to analyse until it is made again.
     */
    public static Freshness check(Path inputConfiguration) throws IOException {
        if (!Files.isRegularFile(inputConfiguration)) {
            return new Freshness(State.STALE, List.of(inputConfiguration + " (gone)"));
        }
        return check(buildFilesOf(inputConfiguration), sourceDirectoriesOf(inputConfiguration));
    }

    /**
     * What to record for a configuration that no build tool wrote: the Gradle and Maven files under its working
     * directory and its module-infos, so a change to the build is noticed even though it cannot be redone.
     */
    public static BuildFiles hashDefault(Path workingDirectory, Collection<Path> sourceDirectories) {
        List<String> globs = new ArrayList<>(BuildFiles.GRADLE_GLOBS);
        globs.addAll(BuildFiles.MAVEN_GLOBS);
        return BuildFilesCapture.capture(BuildFiles.NONE, workingDirectory, globs, moduleInfos(sourceDirectories));
    }

    /** The {@code buildFiles} a configuration records; null when it records none. */
    public static BuildFiles buildFilesOf(Path inputConfiguration) throws IOException {
        JsonNode node = MAPPER.readTree(inputConfiguration.toFile()).get("buildFiles");
        return node == null || node.isNull() ? null : MAPPER.treeToValue(node, BuildFiles.class);
    }

    /** A configuration's own source directories, absolute: those of its {@code sourceSets}. */
    public static List<Path> sourceDirectoriesOf(Path inputConfiguration) throws IOException {
        JsonNode root = MAPPER.readTree(inputConfiguration.toFile());
        JsonNode workingDirectory = root.get("workingDirectory");
        Path base = workingDirectory == null || workingDirectory.isNull()
                ? inputConfiguration.toAbsolutePath().getParent() : Path.of(workingDirectory.asText());
        List<Path> out = new ArrayList<>();
        for (JsonNode set : root.path("sourceSets")) {
            for (JsonNode dir : set.path("sourceDirectories")) out.add(base.resolve(dir.asText()));
        }
        return out;
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static List<Path> moduleInfos(Collection<Path> sourceDirectories) {
        List<Path> out = new ArrayList<>();
        for (Path dir : sourceDirectories) {
            Path moduleInfo = dir.resolve("module-info.java").toAbsolutePath().normalize();
            if (Files.isRegularFile(moduleInfo)) out.add(moduleInfo);
        }
        return out;
    }
}
