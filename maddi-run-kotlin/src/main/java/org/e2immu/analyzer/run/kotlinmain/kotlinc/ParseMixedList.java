package org.e2immu.analyzer.run.kotlinmain.kotlinc;

import org.e2immu.analyzer.run.config.compile.CompileInvocation;
import org.e2immu.analyzer.run.openjdkmain.javac.Javac;
import org.e2immu.analyzer.run.openjdkmain.javac.ParseJavacList;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Phase 5 — one build log, both compilers. Reads a single log that contains {@code javac} <i>and</i>
 * {@code kotlinc} invocations (a mixed Java+Kotlin project), and reconstructs one {@link InputConfiguration}
 * over the shared {@code CompileListToSourceSets} engine — so a Java source set and a Kotlin source set link by
 * output identity in one pass (e.g. a Java module whose classpath contains a Kotlin module's
 * {@code build/classes/kotlin/main}, or vice-versa).
 *
 * <p>A single pass over the log, in **log order** (which real build tools emit in dependency order — a
 * dependency's compile line precedes its dependent's, so classpath output-identity links resolve). Each line is
 * tried as a javac line, then as a Kotlin Gradle/CLI line. NB: Kotlin's Maven multi-line blocks are not
 * interleaved here (mixed-Maven is an edge case; a pure-Kotlin Maven log should use {@link ParseKotlincList}).
 */
public class ParseMixedList {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseMixedList.class);

    public InputConfiguration parse(Path logFile) throws IOException {
        return parse(logFile, List.of());
    }

    public InputConfiguration parse(Path logFile, List<String> extraJmods) throws IOException {
        List<CompileInvocation> ordered = invocations(logFile);
        return new ParseKotlincList().inputConfiguration(ordered, extraJmods);
    }

    /** The javac + kotlinc invocations of the log, in log order. */
    public List<CompileInvocation> invocations(Path logFile) throws IOException {
        ParseJavacList javacParser = new ParseJavacList();
        ParseKotlincList kotlincParser = new ParseKotlincList();
        List<CompileInvocation> ordered = new ArrayList<>();
        int javacCount = 0, kotlincCount = 0;
        for (String line : readLines(logFile)) {
            Javac j = javacParser.javacLine(line);
            if (j != null) {
                ordered.add(j);
                javacCount++;
                continue;
            }
            Kotlinc k = kotlincParser.kotlincSingleLine(line);
            if (k != null) {
                ordered.add(k);
                kotlincCount++;
            }
        }
        LOGGER.info("Mixed log {}: {} javac + {} kotlinc invocations", logFile, javacCount, kotlincCount);
        return ordered;
    }

    private static List<String> readLines(Path logFile) throws IOException {
        if (logFile.getFileName().toString().endsWith(".gz")) {
            try (BufferedInputStream bis = new BufferedInputStream(new GZIPInputStream(new FileInputStream(logFile.toFile())))) {
                return Arrays.stream(new String(bis.readAllBytes()).split("\\n")).toList();
            }
        }
        return Arrays.stream(Files.readString(logFile).split("\n")).filter(s -> !s.isBlank()).toList();
    }
}
