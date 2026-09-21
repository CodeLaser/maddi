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

package io.codelaser.maddi.inspection.resource;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <b>Kotlin source files handed to a run that can only read Java.</b> The Java front end walks a source
 * directory with {@code filter(p -> p.endsWith(".java"))}, so a {@code .kt} file beside them is not an error,
 * not a warning, and not analyzed — the run reports success over a tree it has only partly read.
 *
 * <h2>⭐ Why this refuses rather than warns</h2>
 * The analyzer's output is a set of verdicts about types, and a missing type does not look different from a
 * type with nothing to say about it. A partial parse therefore produces a <em>plausible</em> result, which is
 * the worst kind: nothing downstream — not the build, not jdeps, not a test suite — can tell it from a
 * complete one. Kotlin has a front end in this repository, reachable through the {@code maddi-kotlin}
 * distribution; what the Java entry points lack is not the ability to read Kotlin but a way to say so.
 *
 * <h2>⛔ The test is the file extension, never the source set's name</h2>
 * Keying on "does this project apply the Kotlin plugin" misses the case that motivated this: a {@code .kt}
 * file under {@code src/main/java}, which every build tool compiles and no source-set name mentions. It also
 * mis-fires in the other direction — a Kotlin source set that happens to be empty is not a reason to stop.
 *
 * <h2>⚠ {@link #directoriesScanned} exists so that "clean" can be told from "looked nowhere"</h2>
 * A detector whose input resolution silently produced no directories reports exactly what a Java-only project
 * reports. That is the shape of a check that proves its own converse, so the count of directories actually
 * walked is part of the result and a test asserts it is not zero.
 *
 * @param fileCount        how many Kotlin files were found, across every scanned source set
 * @param directoriesScanned how many existing directories were actually walked
 * @param sourceSetNames   the names of the source sets holding them, in configuration order
 * @param samples          at most {@link #MAX_SAMPLES} of the files, for the message
 */
public record DetectKotlinSources(int fileCount, int directoriesScanned, List<String> sourceSetNames,
                                  List<Path> samples) {
    private static final Logger LOGGER = LoggerFactory.getLogger(DetectKotlinSources.class);

    /** Enough to recognise the project; the whole list belongs in a build log, not in a refusal. */
    public static final int MAX_SAMPLES = 5;

    /** {@code .kts} is included: a build script is not analyzed either, and its presence is worth saying. */
    public static final Set<String> KOTLIN_EXTENSIONS = Set.of(".kt", ".kts");

    /** The option that downgrades the refusal to a warning, spelled as the CLI writes it. */
    public static final String SKIP_OPTION = "--skip-kotlin-sources";

    public boolean found() {
        return fileCount > 0;
    }

    /**
     * Walks the source directories of every source set that is {@link SourceSet#parsedFromSource()}, resolving
     * relative directories against the configuration's working directory exactly as the front end does — a
     * detector reading different paths from the parser would be measuring another tree.
     */
    public static DetectKotlinSources in(InputConfiguration inputConfiguration) {
        if (inputConfiguration == null) return new DetectKotlinSources(0, 0, List.of(), List.of());
        Path workingDirectory = inputConfiguration.workingDirectory();
        int count = 0;
        int scanned = 0;
        Set<String> setNames = new LinkedHashSet<>();
        List<Path> samples = new ArrayList<>();
        for (SourceSet sourceSet : inputConfiguration.sourceSets()) {
            if (!sourceSet.parsedFromSource()) continue;
            for (Path directory : sourceSet.sourceDirectories()) {
                Path resolved = workingDirectory == null || directory.isAbsolute()
                        ? directory : workingDirectory.resolve(directory);
                if (!Files.isDirectory(resolved)) continue; // as the front end does: absent means empty
                ++scanned;
                try (Stream<Path> walk = Files.walk(resolved)) {
                    List<Path> kotlin = walk.filter(DetectKotlinSources::isKotlinFile).sorted().toList();
                    if (!kotlin.isEmpty()) {
                        count += kotlin.size();
                        setNames.add(sourceSet.name());
                        for (Path path : kotlin) {
                            if (samples.size() >= MAX_SAMPLES) break;
                            samples.add(path);
                        }
                    }
                } catch (IOException ioException) {
                    // an unreadable source directory is the build's problem, not this check's; say so and go on
                    LOGGER.warn("Cannot scan {} for Kotlin sources: {}", resolved, ioException.getMessage());
                }
            }
        }
        return new DetectKotlinSources(count, scanned, List.copyOf(setNames), List.copyOf(samples));
    }

    /**
     * The whole decision, in the one place both runners can share it: scan, and either refuse (returning true,
     * the caller stops with {@code Main.EXIT_KOTLIN_SOURCES}) or log the warning the opt-out earns.
     * <p>
     * ⚠ Two copies of this in two runners is how {@code PluginOptions}' header describes a drift nobody can
     * catch: the same project would refuse from one entry point and warn from the other.
     *
     * @param skipKotlinSources the caller's configuration said to analyze the Java alone
     * @param skipOption        how the caller's surface spells that option, for the message
     * @return true when the caller must stop without analyzing
     */
    public static boolean refuse(InputConfiguration inputConfiguration, boolean skipKotlinSources,
                                 String skipOption, Logger logger) {
        DetectKotlinSources detected = DetectKotlinSources.in(inputConfiguration);
        if (!detected.found()) return false;
        if (skipKotlinSources) {
            logger.warn("Skipping {} Kotlin source file(s) in source set(s) {}: the analysis is INCOMPLETE.",
                    detected.fileCount(), detected.sourceSetNames());
            return false;
        }
        logger.error("{}", detected.message(skipOption));
        return true;
    }

    private static boolean isKotlinFile(Path path) {
        String name = path.getFileName().toString();
        return KOTLIN_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    /**
     * The refusal, naming what was found and the two ways forward. {@code skipOption} is how the caller's own
     * surface spells the opt-out ({@link #SKIP_OPTION} for the CLI, {@code skipKotlinSources} for the plugins),
     * because a message naming a flag the reader cannot type is worse than no message.
     */
    public String message(String skipOption) {
        StringBuilder sb = new StringBuilder();
        sb.append(fileCount).append(" Kotlin source file(s) in source set(s) ").append(sourceSetNames)
                .append(" would be SKIPPED by this analyzer, which reads Java only, and the run would report")
                .append(" success over a partly-read tree.");
        for (Path sample : samples) sb.append("\n    ").append(sample);
        if (fileCount > samples.size()) sb.append("\n    ... and ").append(fileCount - samples.size()).append(" more");
        sb.append("\nAnalyze Kotlin (and mixed Java+Kotlin) with the maddi-kotlin distribution instead.")
                .append(" To analyze the Java sources alone and accept the incomplete result, set ")
                .append(skipOption).append(".");
        return sb.toString();
    }
}
