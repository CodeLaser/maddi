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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ProjectConfiguratorTest {

    /* a store of another tool's own, in memory */
    static final class MemoryRecords implements ProjectRecords {
        final Map<String, ProjectRecord> records = new LinkedHashMap<>();
        final Path work;
        int saves;

        MemoryRecords(Path work) {
            this.work = work;
        }

        @Override
        public Optional<ProjectRecord> load(String name) {
            return Optional.ofNullable(records.get(name));
        }

        @Override
        public List<ProjectRecord> loadAll(List<String> problems) {
            return new ArrayList<>(records.values());
        }

        @Override
        public void save(ProjectRecord record) {
            records.put(record.name(), record);
            saves++;
        }

        @Override
        public Path workDirectory(String name) {
            return work.resolve(name);
        }
    }

    @Test
    public void captured(@TempDir Path dir) throws Exception {
        Path root = CompileLogCaptureTest.project(dir);
        MemoryRecords store = new MemoryRecords(dir.resolve("work"));
        ProjectConfigurator configurator = new ProjectConfigurator(store);
        CaptureSpec spec = CaptureSpec.compileLog(root,
                CompileLogCaptureTest.printing("echo '" + CompileLogCaptureTest.javacLine(root) + "'"), null, null,
                List.of("build.txt"));
        ProjectRecord record = configurator.configure("p", new ProjectConfigurator.FromCapture(spec),
                CaptureListener.NONE);
        assertEquals(ConfiguredBy.COMPILE_LOG, record.configuredBy());
        assertEquals(store.workDirectory("p").resolve("inputConfiguration.json"), record.inputConfiguration());
        assertEquals(spec, record.regenerate().capture());

        ProjectConfigurator.Opened unchanged = configurator.open("p", false, CaptureListener.NONE);
        assertEquals("unchanged", unchanged.action());
        assertEquals(BuildFileCheck.State.FRESH, unchanged.after().state());

        Files.writeString(root.resolve("build.txt"), "version 2\n");
        ProjectConfigurator.Opened regenerated = configurator.open("p", false, CaptureListener.NONE);
        assertEquals("regenerated, as the build changed: build.txt (changed)", regenerated.action());
        assertEquals(BuildFileCheck.State.STALE, regenerated.before().state());
        assertEquals(BuildFileCheck.State.FRESH, regenerated.after().state(), "checked again, not carried over");
        assertTrue(regenerated.record().configuredAt().isAfter(record.configuredAt())
                   || regenerated.record().configuredAt().equals(record.configuredAt()));
        assertEquals("unchanged", configurator.open("p", false, CaptureListener.NONE).action());

        ProjectConfigurator.Opened refreshed = configurator.refresh("p", List.of(Path.of("build.txt")),
                CaptureListener.NONE);
        assertEquals("regenerated, as the build changed: build.txt (changed)", refreshed.action(),
                "the caller said what it touched: no hashing first");
        assertEquals(BuildFileCheck.State.FRESH, refreshed.after().state());
    }

    @Test
    public void directories(@TempDir Path dir) throws Exception {
        Path root = CompileLogCaptureTest.project(dir);
        Files.writeString(root.resolve("pom.xml"), "<project/>\n");
        MemoryRecords store = new MemoryRecords(dir.resolve("work"));
        ProjectConfigurator configurator = new ProjectConfigurator(store);
        Directories directories = new Directories(root, List.of("src/main/java"), List.of(), List.of(), List.of(),
                null, 21, null, List.of("java.base"), null);
        ProjectRecord record = configurator.configure("d", new ProjectConfigurator.FromDirectories(directories),
                CaptureListener.NONE);
        assertNull(record.regenerate(), "nothing can make it again");
        assertEquals(List.of(root.resolve("src/main/java")),
                BuildFileCheck.sourceDirectoriesOf(record.inputConfiguration()));
        assertEquals("unchanged", configurator.open("d", false, CaptureListener.NONE).action());

        Files.writeString(root.resolve("pom.xml"), "<project><!-- changed --></project>\n");
        ProjectConfigurator.StaleException e = assertThrows(ProjectConfigurator.StaleException.class,
                () -> configurator.open("d", false, CaptureListener.NONE));
        assertEquals(List.of("pom.xml (changed)"), e.changed());
        assertEquals("stale, allowed", configurator.open("d", true, CaptureListener.NONE).action());
    }

    @Test
    public void fromAFileWithACommand(@TempDir Path dir) throws Exception {
        Path root = CompileLogCaptureTest.project(dir);
        // a configuration a build tool wrote, and a "build tool" that writes the next one: a copy
        Path made = dir.resolve("made");
        CompileLogCapture.capture(CaptureSpec.compileLog(root,
                        CompileLogCaptureTest.printing("echo '" + CompileLogCaptureTest.javacLine(root) + "'"), null,
                        null, List.of("build.txt")), made, List.of(), CaptureListener.NONE);
        Path file = Files.copy(made.resolve("inputConfiguration.json"), root.resolve("inputConfiguration.json"));
        MemoryRecords store = new MemoryRecords(dir.resolve("work"));
        ProjectConfigurator configurator = new ProjectConfigurator(store);
        Regenerate copy = Regenerate.command(List.of("cp", made.resolve("inputConfiguration.json").toString(),
                file.toString()), root);
        ProjectRecord record = configurator.configure("f", new ProjectConfigurator.FromFile(file, copy),
                CaptureListener.NONE);
        assertEquals(ConfiguredBy.FILE, record.configuredBy());

        Files.writeString(root.resolve("build.txt"), "version 2\n");
        // the copy still records the old hash: regenerated, but still stale afterwards, and said so
        ProjectConfigurator.Opened opened = configurator.open("f", false, CaptureListener.NONE);
        assertEquals("regenerated, as the build changed: build.txt (changed)", opened.action());
        assertEquals(BuildFileCheck.State.STALE, opened.after().state());

        Regenerate failing = Regenerate.command(List.of("sh", "-c", "echo broken; exit 2"), root);
        configurator.configure("g", new ProjectConfigurator.FromFile(file, failing), CaptureListener.NONE);
        CaptureException e = assertThrows(CaptureException.class,
                () -> configurator.open("g", false, CaptureListener.NONE));
        assertEquals(2, e.exitCode());
        assertEquals("broken", e.buildLogTail().getLast());
    }

    @Test
    public void goneConfiguration(@TempDir Path dir) throws Exception {
        BuildFileCheck.Freshness gone = BuildFileCheck.check(dir.resolve("build/inputConfiguration.json"));
        assertEquals(BuildFileCheck.State.STALE, gone.state(), "a clean deleted it: make it again");
    }

    @Test
    public void defaultRegenerate(@TempDir Path dir) {
        var gradle = new io.codelaser.maddi.inspection.api.resource.BuildFiles(
                io.codelaser.maddi.inspection.api.resource.BuildFiles.GRADLE_PLUGIN, dir.toString(), List.of(),
                Map.of(), "now");
        Regenerate r = Regenerate.defaultFor(gradle, dir.resolve("a/build/inputConfiguration.json"));
        assertEquals(List.of("gradle", "-q", "-p", dir.resolve("a").toString(), Regenerate.GRADLE_TASK), r.command());
        assertNull(Regenerate.defaultFor(gradle, dir.resolve("a/x.json")), "not in a build directory");
        assertEquals(ConfiguredBy.MAVEN_CAPTURE, ConfiguredBy.ofKey("maven-capture"));
        assertEquals(List.of("gradle", "-Dorg.gradle.java.home=/jdk/21"), CaptureSpec.gradle(dir, Path.of("/jdk/21")).command());
        assertEquals(List.of("gradle"), CaptureSpec.gradle(dir, null).command());
        assertEquals(List.of("env", "JAVA_HOME=/jdk/21", "mvn", "-B", "-X", "clean", "compile", "test-compile"),
                CaptureSpec.maven(dir, Path.of("/jdk/21")).command());
        assertEquals("the build changed", ProjectConfigurator.because(List.of()));
        assertEquals("the build changed: a (changed), b (gone), c (changed) and 1 more",
                ProjectConfigurator.because(List.of("a (changed)", "b (gone)", "c (changed)", "d (changed)")));
        assertThrows(IllegalArgumentException.class, () -> ConfiguredBy.ofKey("bazel"));
    }
}
