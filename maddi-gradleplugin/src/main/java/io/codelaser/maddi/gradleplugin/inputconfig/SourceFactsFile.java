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

package io.codelaser.maddi.gradleplugin.inputconfig;

import io.codelaser.maddi.run.config.util.JsonStreaming;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The channel that carries a project's {@link SourceFacts} to a SIBLING that co-analyses its sources.
 *
 * <p>⛔⛔ <b>WHY A FILE WRITTEN DURING CONFIGURATION, AND NOT THE THREE OTHER SHAPES.</b> {@code
 * sourceRelease}, {@code addModules} and {@code warningFlags} live on a {@code JavaCompile} task, and a
 * sibling's task belongs to another project — reading it is the cross-project access the whole
 * source-variant mechanism exists to avoid. What was tried and rejected:
 * <ul>
 *   <li><b>An artifact a TASK produces</b> — written, then taken back out. {@code ComputeSourceSets} runs at
 *       CONFIGURATION time, so a file only a task can produce does not exist yet when the consumer reads it;
 *       the consumer would get exactly the empty lists it had before. (Recorded in {@code SourceFacts}.)</li>
 *   <li><b>A shared {@code BuildService} keyed by project path</b> — works, and costs a service whose
 *       population order across projects is a second thing to get right. Kept as the fallback if this one
 *       ever needs to survive a producer that is configured after its consumer.</li>
 *   <li><b>Reading the sibling's task directly</b> — the thing being avoided.</li>
 * </ul>
 *
 * <p>⭐ <b>THE ORDERING IS WHAT MAKES THIS WORK, AND IT IS NOT LUCK.</b> The file is written by the
 * PROVIDER behind the {@code maddiSourceElements} outgoing artifacts, so it is written exactly when a
 * consumer resolves that variant — and resolving a project dependency configures the producing project
 * first. The consumer therefore never sees a missing file; it sees either the facts or a producer without
 * this plugin, which is the same "no facts" case as before.
 *
 * <p>⚠ <b>BOTH SKEW DIRECTIONS ARE SAFE, and that was checked rather than assumed.</b> An OLD consumer
 * against a NEW producer ignores the file: {@code collectProjectSources} keeps only artifacts for which
 * {@code file.isDirectory()}. A NEW consumer against an OLD producer finds no such artifact and falls back
 * to {@code SourceFacts.NONE}, which is today's behaviour.
 */
public final class SourceFactsFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceFactsFile.class);

    /** ⚠ The consumer matches on this NAME, not on "is not a directory": a name says what it found. */
    public static final String FILE_NAME = "maddi-source-facts.json";

    private SourceFactsFile() {
    }

    /**
     * Writes the main source set's facts and returns the file, or {@code null} when there is nothing to say
     * (no Java plugin, or no main source set) — an absent artifact is the correct answer there, and an empty
     * one would be a claim.
     */
    public static File write(Project project) {
        JavaPluginExtension java = project.getExtensions().findByType(JavaPluginExtension.class);
        if (java == null) return null;
        org.gradle.api.tasks.SourceSet main = java.getSourceSets()
                .findByName(org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME);
        if (main == null) return null;
        SourceFacts facts = ComputeSourceSets.factsOf(project, main);
        Path file = project.getLayout().getBuildDirectory().get().getAsFile().toPath()
                .resolve("maddi").resolve(FILE_NAME);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, JsonStreaming.objectMapper().writeValueAsString(facts));
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
        LOGGER.debug("Wrote source facts of {} to {}: {}", project.getPath(), file, facts);
        return file.toFile();
    }

    /**
     * ⚠ A file that cannot be read is reported and treated as absent. It is a cache of another project's
     * build model, not a source of truth: failing the consumer's whole configuration over it would trade a
     * missing attribute for a broken build.
     */
    public static SourceFacts read(File file) {
        try {
            return JsonStreaming.objectMapper().readValue(file, SourceFacts.class);
        } catch (IOException e) {
            LOGGER.warn("Cannot read source facts from {}: {}", file, e.getMessage());
            return null;
        }
    }
}
