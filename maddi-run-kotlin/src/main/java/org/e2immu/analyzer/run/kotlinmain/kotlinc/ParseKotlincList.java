package org.e2immu.analyzer.run.kotlinmain.kotlinc;

import org.e2immu.analyzer.run.config.compile.CompileListToInputConfiguration;
import org.e2immu.analyzer.run.config.compile.CompileInvocation;
import org.e2immu.analyzer.run.config.compile.CompileListToSourceSets;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Reads a build log containing {@code kotlinc} command lines and reconstructs an {@link InputConfiguration}.
 * The Kotlin analogue of {@code ParseJavacList}: same log-reading (plain / {@code .gz} / json-list), same
 * jmod-closure wrapping, but Kotlin markers and the {@link Kotlinc} tokenizer, over the shared
 * {@link CompileListToSourceSets} engine.
 */
public class ParseKotlincList {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseKotlincList.class);

    public InputConfiguration parse(Path kotlincLogFile) throws IOException {
        return parse(kotlincLogFile, List.of());
    }

    public InputConfiguration parse(Path kotlincLogFile, List<String> extraJmods) throws IOException {
        return inputConfiguration(kotlincLines(kotlincLogFile), extraJmods);
    }

    /**
     * ⚠ THE ASSEMBLY IS SHARED WITH THE JAVA FRONT-END. This method used to be a verbatim copy of
     * {@code ParseJavacList}'s, so anything added to one silently did not hold for the other; both now call
     * {@link CompileListToInputConfiguration}, which is also where the TYPE_USE annotation closure lives.
     */
    public InputConfiguration inputConfiguration(List<? extends CompileInvocation> kotlincList, List<String> extraJmods) {
        return inputConfiguration(kotlincList, extraJmods, null);
    }

    /** @param buildRoot the directory the build was run from; it decides the source-set names. */
    public InputConfiguration inputConfiguration(List<? extends CompileInvocation> kotlincList,
                                                 List<String> extraJmods, String buildRoot) {
        CompileListToSourceSets.Result result = new CompileListToSourceSets(buildRoot).compute(kotlincList);
        return CompileListToInputConfiguration.build(result, extraJmods);
    }

    public List<Kotlinc> kotlincLines(Path kotlincLogFile) throws IOException {
        List<String> lines;
        if (kotlincLogFile.getFileName().toString().endsWith(".gz")) {
            try (BufferedInputStream bis = new BufferedInputStream(new GZIPInputStream(new FileInputStream(kotlincLogFile.toFile())))) {
                byte[] bytes = bis.readAllBytes();
                lines = Arrays.stream(new String(bytes).split("\\n")).toList();
                LOGGER.info("Read {} bytes from {}, split into {} lines", bytes.length, kotlincLogFile, lines.size());
            }
        } else {
            String content = Files.readString(kotlincLogFile);
            if (content.startsWith("- ")) {
                // json-list rather than log lines: each entry starts with "- "
                List<String> entries = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                for (String line : content.split("\n")) {
                    if (line.startsWith("- ")) {
                        if (!sb.isEmpty()) {
                            entries.add(sb.toString());
                            sb = new StringBuilder();
                        }
                        sb.append(line.substring(2));
                    } else {
                        sb.append(" ").append(line);
                    }
                }
                if (!sb.isEmpty()) entries.add(sb.toString());
                return entries.stream().map(Kotlinc::parse).filter(Objects::nonNull).toList();
            }
            lines = Arrays.stream(content.split("\n")).filter(s -> !s.isBlank()).toList();
            LOGGER.info("Read {} lines from {}", lines.size(), kotlincLogFile);
        }
        return parseLines(lines);
    }

    // Gradle (kotlin-gradle-plugin, --debug): one line "… v: Kotlin compiler args: <args>"
    public static final String GRADLE_PATTERN = ".*Kotlin compiler args:\\s*(.+)";
    private static final Pattern GRADLE = Pattern.compile(GRADLE_PATTERN);
    // raw CLI: "kotlinc <args>"
    private static final Pattern KOTLINC = Pattern.compile("kotlinc(?:-jvm)?\\s+(.+)");

    // Maven (kotlin-maven-plugin, -X): a multi-line block of labelled DEBUG lines, one per compile execution.
    private static final Pattern MVN_SOURCES = Pattern.compile(".*Compiling Kotlin sources from \\[(.*)]");
    private static final Pattern MVN_CLASSPATH = Pattern.compile(".*\\[DEBUG] Classpath:\\s*(.*)");
    private static final Pattern MVN_CLASSES_DIR = Pattern.compile(".*Classes directory is (.*)");
    private static final Pattern MVN_MODULE = Pattern.compile(".*Module name is (.*)");

    /**
     * Single pass over the log. A line is either a Gradle/CLI single-line invocation, or part of a Maven block
     * (opened by "Compiling Kotlin sources from […]" and flushed on its "Module name is …" line).
     */
    List<Kotlinc> parseLines(List<String> lines) {
        List<Kotlinc> result = new ArrayList<>();
        MavenBlock block = null;
        for (String line : lines) {
            Kotlinc single = convertSingleLine(line);
            if (single != null) {
                result.add(single);
                block = null;
                continue;
            }
            Matcher src = MVN_SOURCES.matcher(line);
            if (src.matches()) {
                block = new MavenBlock();
                block.sourceDirs = splitCommaList(src.group(1));
                continue;
            }
            if (block != null) {
                Matcher cp = MVN_CLASSPATH.matcher(line);
                Matcher cd = MVN_CLASSES_DIR.matcher(line);
                Matcher mn = MVN_MODULE.matcher(line);
                if (cp.matches()) {
                    block.classpath = splitPath(cp.group(1));
                } else if (cd.matches()) {
                    block.destination = cd.group(1).trim();
                } else if (mn.matches()) {
                    block.moduleName = mn.group(1).trim();
                    Kotlinc k = block.build(); // module-name is the last line of the block
                    if (k != null) result.add(k);
                    block = null;
                }
            }
        }
        return result;
    }

    private static final class MavenBlock {
        List<String> sourceDirs = List.of();
        List<String> classpath = List.of();
        String destination;
        String moduleName;

        Kotlinc build() {
            return destination == null ? null : Kotlinc.mavenBlock(destination, sourceDirs, classpath, moduleName);
        }
    }

    /** Parse a single Gradle/CLI line into a {@link Kotlinc}, or {@code null} (no Maven multi-line handling). */
    public Kotlinc kotlincSingleLine(String line) {
        return convertSingleLine(line);
    }

    private Kotlinc convertSingleLine(String line) {
        Matcher g = GRADLE.matcher(line);
        if (g.matches()) return Kotlinc.parse(g.group(1));
        Matcher k = KOTLINC.matcher(line);
        if (k.matches()) return Kotlinc.parse(k.group(1));
        return null;
    }

    private static List<String> splitCommaList(String inside) {
        if (inside.isBlank()) return List.of();
        return Arrays.stream(inside.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static List<String> splitPath(String path) {
        if (path.isBlank()) return List.of();
        return Arrays.stream(path.split(java.io.File.pathSeparator)).filter(s -> !s.isBlank()).toList();
    }
}
