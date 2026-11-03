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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Configuring a project, and opening it again later: whatever way it was configured, its build files are checked,
 * and when they changed, its configuration is made again before it is used.
 */
public final class ProjectConfigurator {

    /** Where a command's regeneration writes its output, in the project's work directory. */
    public static final String REGENERATE_LOG = "regenerate.log";

    public sealed interface Way permits FromFile, FromDirectories, FromCapture {
    }

    /** @param regenerate null: {@link Regenerate#defaultFor} the file's producer, if it knows one */
    public record FromFile(Path inputConfiguration, Regenerate regenerate) implements Way {
    }

    public record FromDirectories(Directories directories) implements Way {
    }

    public record FromCapture(CaptureSpec spec) implements Way {
    }

    /**
     * @param before the freshness found when opening; for a {@link #refresh}, the files the caller said it touched
     * @param after  after a regeneration, checked again (FRESH, or UNCHECKED without globs); else {@code before}
     * @param action "unchanged", "stale, allowed", "regenerated, as ... changed", "made, for the first time"
     */
    public record Opened(ProjectRecord record, BuildFileCheck.Freshness before, BuildFileCheck.Freshness after,
                         String action) {
    }

    /** A stale configuration that nothing can make again, opened without {@code allowStale}. */
    public static final class StaleException extends IOException {
        private final List<String> changed;

        public StaleException(String name, List<String> changed) {
            super("the configuration of " + name + " is stale, as " + because(changed) + ", and there is no way"
                  + " to make it again: configure it again, or allow a stale configuration");
            this.changed = List.copyOf(changed);
        }

        public List<String> changed() {
            return changed;
        }
    }

    private final ProjectRecords store;

    public ProjectConfigurator(ProjectRecords store) {
        this.store = store;
    }

    public ProjectRecords store() {
        return store;
    }

    /** Configure {@code name} anew, replacing what was stored under it. */
    public ProjectRecord configure(String name, Way way, CaptureListener listener)
            throws IOException, InterruptedException {
        Instant now = Instant.now();
        ProjectRecord record = switch (way) {
            case FromFile f -> {
                Path file = f.inputConfiguration().toAbsolutePath().normalize();
                if (!Files.isRegularFile(file)) throw new IOException("no configuration at " + file);
                Regenerate regenerate = f.regenerate() != null ? f.regenerate()
                        : Regenerate.defaultFor(BuildFileCheck.buildFilesOf(file), file);
                yield new ProjectRecord(name, ConfiguredBy.FILE, file, regenerate, now);
            }
            case FromDirectories d -> {
                Path output = store.workDirectory(name).resolve(CompileLogCapture.INPUT_CONFIGURATION);
                DirectoriesConfiguration.write(d.directories(), output);
                yield new ProjectRecord(name, ConfiguredBy.DIRECTORIES, output, null, now);
            }
            case FromCapture c -> {
                CaptureResult result = CompileLogCapture.capture(c.spec(), store.workDirectory(name), List.of(),
                        listener);
                yield new ProjectRecord(name, c.spec().kind(), result.inputConfiguration(),
                        Regenerate.capture(c.spec()), now);
            }
        };
        store.save(record);
        return record;
    }

    /**
     * Open a stored project: check its build files and, when one changed, make its configuration again. Without a
     * way to make it again, a stale configuration is refused ({@link StaleException}) unless {@code allowStale}.
     */
    public Opened open(String name, boolean allowStale, CaptureListener listener)
            throws IOException, InterruptedException {
        ProjectRecord record = load(name);
        BuildFileCheck.Freshness before = BuildFileCheck.check(record.inputConfiguration());
        if (before.state() != BuildFileCheck.State.STALE) return new Opened(record, before, before, "unchanged");
        if (record.regenerate() == null) {
            if (allowStale) return new Opened(record, before, before, "stale, allowed");
            throw new StaleException(name, before.changed());
        }
        return regenerate(record, before, listener);
    }

    /**
     * Make a project's configuration again after a write to its build: with {@code touched} known, without hashing
     * first; without, as {@link #open} does, but never allowing a stale configuration.
     *
     * @param touched the build files the write changed; null or empty when the caller does not know
     */
    public Opened refresh(String name, List<Path> touched, CaptureListener listener)
            throws IOException, InterruptedException {
        if (touched == null || touched.isEmpty()) return open(name, false, listener);
        ProjectRecord record = load(name);
        BuildFileCheck.Freshness before = new BuildFileCheck.Freshness(BuildFileCheck.State.STALE,
                touched.stream().map(p -> p + " (changed)").toList());
        if (record.regenerate() == null) throw new StaleException(name, before.changed());
        return regenerate(record, before, listener);
    }

    private ProjectRecord load(String name) throws IOException {
        return store.load(name).orElseThrow(() -> new IOException("no project " + name + " is configured"));
    }

    private Opened regenerate(ProjectRecord record, BuildFileCheck.Freshness before, CaptureListener listener)
            throws IOException, InterruptedException {
        Regenerate regenerate = record.regenerate();
        listener.progress("regenerating the configuration: " + regenerate.describe());
        Path workDirectory = store.workDirectory(record.name());
        Path file = record.inputConfiguration();
        if (regenerate.isCapture()) {
            file = CompileLogCapture.capture(regenerate.capture(), workDirectory, List.of(), listener)
                    .inputConfiguration();
        } else {
            runCommand(regenerate, workDirectory.resolve(REGENERATE_LOG), listener);
            if (!Files.isRegularFile(file)) {
                throw new IOException("regenerating the configuration (" + regenerate.describe() + ") wrote no " + file);
            }
        }
        ProjectRecord fresh = record.withInputConfiguration(file, Instant.now());
        store.save(fresh);
        BuildFileCheck.Freshness after = BuildFileCheck.check(file);
        return new Opened(fresh, before, after, "regenerated, as " + because(before.changed()));
    }

    private static void runCommand(Regenerate regenerate, Path log, CaptureListener listener)
            throws IOException, InterruptedException {
        Files.createDirectories(log.toAbsolutePath().getParent());
        Process process = new ProcessBuilder(regenerate.command())
                .directory(regenerate.directory() == null ? null : regenerate.directory().toFile())
                .redirectErrorStream(true).start();
        listener.process(process);
        try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8);
             BufferedWriter out = Files.newBufferedWriter(log)) {
            out.write("$ " + regenerate.describe() + "\n");
            for (String line; (line = reader.readLine()) != null; ) {
                if (CompileLogCapture.chatter(line)) continue;
                out.write(line);
                out.write('\n');
            }
        } catch (IOException | RuntimeException e) {
            process.destroyForcibly();
            throw e;
        } finally {
            listener.process(null);
        }
        int exit = process.waitFor();
        if (exit != 0) {
            throw new CaptureException("regenerating the configuration failed (exit " + exit + ", "
                                       + regenerate.describe() + ")", exit, log);
        }
    }

    /* each entry says how it changed already: "settings.gradle (changed)", "pom.xml (gone)" */
    static String because(List<String> changed) {
        if (changed.isEmpty()) return "the build changed";
        return "the build changed: " + (changed.size() <= 3 ? String.join(", ", changed)
                : String.join(", ", changed.subList(0, 3)) + " and " + (changed.size() - 3) + " more");
    }
}
