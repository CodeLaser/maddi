package io.codelaser.maddi.run.kotlinmain.kotlinc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.run.config.compile.CompileInvocation;
import io.codelaser.maddi.run.config.javac.Javac;
import io.codelaser.maddi.run.config.javac.ParseJavacList;

/**
 * Phase 5 — one build log, both compilers. Reads a single log that contains {@code javac} <i>and</i>
 * {@code kotlinc} invocations (a mixed Java+Kotlin project), and reconstructs one {@link InputConfiguration}
 * over the shared {@code CompileListToSourceSets} engine — so a Java source set and a Kotlin source set link by
 * output identity in one pass (e.g. a Java module whose classpath contains a Kotlin module's
 * {@code build/classes/kotlin/main}, or vice-versa).
 *
 * <p>A single pass over the log, in **log order** (which real build tools emit in dependency order — a
 * dependency's compile line precedes its dependent's, so classpath output-identity links resolve). Each line is
 * tried as a javac line, then as a Kotlin line: a Gradle/CLI single line, or part of a kotlin-maven-plugin
 * multi-line block (which a mixed Maven build logs just before the same source set's javac line).
 */
public class ParseMixedList {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseMixedList.class);

    public InputConfiguration parse(Path logFile) throws IOException {
        return parse(logFile, List.of());
    }

    public InputConfiguration parse(Path logFile, List<String> extraJmods) throws IOException {
        List<CompileInvocation> ordered = SharedDestination.merge(invocations(logFile));
        return new ParseKotlincList().inputConfiguration(ordered, extraJmods);
    }

    /** The javac + kotlinc invocations of the log, in log order. */
    public List<CompileInvocation> invocations(Path logFile) throws IOException {
        ParseJavacList javacParser = new ParseJavacList();
        ParseKotlincList.LineReader kotlincReader = new ParseKotlincList().new LineReader();
        List<CompileInvocation> ordered = new ArrayList<>();
        int javacCount = 0, kotlincCount = 0;
        try (BufferedReader reader = open(logFile)) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.isBlank()) continue;
                Javac j = javacParser.javacLine(line);
                if (j != null) {
                    ordered.add(j);
                    javacCount++;
                    continue;
                }
                Kotlinc k = kotlincReader.accept(line);
                if (k != null) {
                    ordered.add(k);
                    kotlincCount++;
                }
            }
        }
        LOGGER.info("Mixed log {}: {} javac + {} kotlinc invocations", logFile, javacCount, kotlincCount);
        return ordered;
    }

    /**
     * The log, line by line. ⚠ Never read whole: quarkus's whole-reactor {@code mvn -X} log is 3 GB, past a Java
     * array's size ("Required array size too large", whatever the heap). Undecodable bytes become U+FFFD rather
     * than failing the read: a build log is not guaranteed UTF-8.
     */
    private static BufferedReader open(Path logFile) throws IOException {
        InputStream in = Files.newInputStream(logFile);
        if (logFile.getFileName().toString().endsWith(".gz")) in = new GZIPInputStream(in);
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8), 1 << 16);
    }
}
