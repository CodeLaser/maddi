package io.codelaser.maddi.run.openjdkmain.javac;

import io.codelaser.maddi.run.config.compile.CompileListToInputConfiguration;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
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

public class ParseJavacList {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseJavacList.class);

    public InputConfiguration parse(Path javacLogFile) throws IOException {
        List<Javac> javacList = javacLines(javacLogFile);
        return inputConfiguration(javacList, List.of());
    }

    public InputConfiguration parse(Path javacLogFile, List<String> extraJmods) throws IOException {
        List<Javac> javacList = javacLines(javacLogFile);
        return inputConfiguration(javacList, extraJmods);
    }

    /**
     * ⚠ THE ASSEMBLY IS SHARED WITH THE KOTLIN FRONT-END ({@code ParseKotlincList}), which used to carry a
     * verbatim copy of it. Everything {@link CompileListToInputConfiguration} does — the jmod closure, and the
     * TYPE_USE annotation closure over each source set's classpath — therefore holds for both by construction.
     */
    public InputConfiguration inputConfiguration(List<Javac> javacList, List<String> extraJmods) throws IOException {
        return inputConfiguration(javacList, extraJmods, null);
    }

    /**
     * @param buildRoot the directory the build was run from; it decides the source-set names, so a caller that
     *                  knows it should pass it — see
     *                  {@link io.codelaser.maddi.run.config.compile.CompileListToSourceSets#CompileListToSourceSets(String)}.
     */
    public InputConfiguration inputConfiguration(List<Javac> javacList, List<String> extraJmods, String buildRoot) {
        return inputConfiguration(javacList, extraJmods, buildRoot, List.of());
    }

    /**
     * @param excludedSourceSets source sets to keep out of the parse; a compile task list cannot express this,
     *                           because Gradle compiles a requested task's dependencies whether you asked for
     *                           them or not. See {@code CompileListToInputConfiguration#exclude}.
     */
    public InputConfiguration inputConfiguration(List<Javac> javacList, List<String> extraJmods, String buildRoot,
                                                 List<String> excludedSourceSets) {
        JavacListToSourceSets.Result result = new JavacListToSourceSets(buildRoot).compute(javacList);
        return CompileListToInputConfiguration.build(result, extraJmods, excludedSourceSets);
    }

    public List<Javac> javacLines(Path javacLogFile) throws IOException {
        List<String> lines;
        if (javacLogFile.getFileName().toString().endsWith(".gz")) {
            try (BufferedInputStream bis = new BufferedInputStream(new GZIPInputStream(new FileInputStream(javacLogFile.toFile())))) {
                byte[] bytes = bis.readAllBytes();
                String bigString = new String(bytes);
                lines = Arrays.stream(bigString.split("\\n")).toList();
                LOGGER.info("Read {} bytes from {}, split into {} lines", bytes.length, javacLogFile, lines.size());
            }
        } else {
            String content = Files.readString(javacLogFile);
            if (content.startsWith("- ")) {
                // this is json rather than actual log lines
                lines = new ArrayList<>();
                StringBuilder sb = new StringBuilder();
                for (String line : content.split("\n")) {
                    if (line.startsWith("- ")) {
                        if (!sb.isEmpty()) {
                            lines.add(sb.toString());
                            sb = new StringBuilder();
                        }
                        sb.append(line.substring(2));
                    } else {
                        sb.append(" ").append(line);
                    }
                }
                if (!sb.isEmpty()) {
                    lines.add(sb.toString());
                }
                return lines.stream().map(Javac::parse)
                        .filter(Objects::nonNull)
                        .toList();
            } else {
                lines = Arrays.stream(content.split("\n")).filter(s -> !s.isBlank()).toList();
                LOGGER.info("Read {} lines from {}", lines.size(), javacLogFile);
            }
        }
        return foldAntBlocks(lines).stream().map(this::convertToJavac)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Ant's {@code <javac>} block, rewritten as the single {@code javac …} line the {@link #JAVAC} pattern
     * reads. Every other line passes through untouched, so a log that holds no Ant block is returned as it
     * came in and the Gradle and Maven routes are unaffected.
     *
     * <p>⛔ <b>THE FOURTH BUILD TOOL, AND THE ONLY ONE THAT DOES NOT FIT ON A LINE.</b> {@code ant -verbose}
     * prints
     * <pre>
     *     [javac] Compilation arguments:
     *     [javac] '-d'
     *     [javac] '/checkout/build/classes/main'
     *     [javac] Files to be compiled:
     *     [javac]     /checkout/src/java/org/apache/cassandra/db/Keyspace.java
     * </pre>
     * one quoted argument per line, then the sources. The three existing patterns are all single-line, so an
     * Ant corpus produced ZERO compile invocations and therefore an empty configuration. The Apache Cassandra
     * campaign (2026-09-18) worked around it with a converter script outside the product; this is that
     * converter, with its refusal.
     *
     * <p>⚠ <b>A FORMAT CHANGE ONLY: no argument is added, dropped or reordered.</b> Which is why an argument
     * containing whitespace REFUSES rather than being emitted: {@link Javac#parse} splits on whitespace, so
     * such an argument would silently become two, and one of the two would then be read as a source file or
     * consume the token after it. A whole source set built from a corrupted line is far harder to notice than
     * a refusal naming the argument.
     */
    static List<String> foldAntBlocks(List<String> lines) {
        if (lines.stream().noneMatch(l -> ANT_START.matcher(l).matches())) return lines;
        List<String> folded = new ArrayList<>();
        int i = 0;
        int blocks = 0;
        while (i < lines.size()) {
            if (!ANT_START.matcher(lines.get(i)).matches()) {
                folded.add(lines.get(i));
                ++i;
                continue;
            }
            List<String> parts = new ArrayList<>();
            ++i;
            Matcher m;
            while (i < lines.size() && (m = ANT_ARG.matcher(lines.get(i))).matches()) {
                parts.add(m.group(1));
                ++i;
            }
            // ⚠ Ant prints its own chatter between the arguments and the file list; skip it, but only while the
            // lines still belong to this task, so a block that never lists files cannot swallow the whole log
            while (i < lines.size() && lines.get(i).contains("[javac]")
                   && !ANT_FILES_HEADER.matcher(lines.get(i)).find()) {
                ++i;
            }
            ++i;    // the "Files to be compiled:" line itself
            while (i < lines.size() && (m = ANT_FILE.matcher(lines.get(i))).matches()) {
                parts.add(m.group(1));
                ++i;
            }
            // ⚠ An EMPTY argument is in the same class as one holding whitespace: the join-then-split loses it
            // entirely rather than turning it into two. Both are reported, neither is converted quietly.
            List<String> unsplittable = parts.stream()
                    .filter(p -> p.isEmpty() || p.chars().anyMatch(Character::isWhitespace))
                    .toList();
            if (!unsplittable.isEmpty()) {
                throw new IllegalStateException("Ant javac block holds argument(s) that cannot survive the"
                                                + " split into a javac command line (empty, or containing"
                                                + " whitespace): "
                                                + unsplittable.subList(0, Math.min(3, unsplittable.size())));
            }
            folded.add("javac " + String.join(" ", parts));
            ++blocks;
        }
        LOGGER.info("Folded {} Ant '[javac] Compilation arguments:' block(s) into javac command lines", blocks);
        return folded;
    }

    public static final String GRADLE_PATTERN = ".+Compiler arguments: (.+)";
    public static final String MAVEN_PATTERN = "\\[DEBUG] (-d (.+))";

    private static final Pattern JAVAC = Pattern.compile("javac (.+)");
    private static final Pattern GRADLE = Pattern.compile(GRADLE_PATTERN);
    private static final Pattern MVN = Pattern.compile(MAVEN_PATTERN);

    /** ant -verbose, one quoted argument per line; see {@link #foldAntBlocks} */
    private static final Pattern ANT_START = Pattern.compile("\\s*\\[javac] Compilation arguments:\\s*");
    private static final Pattern ANT_ARG = Pattern.compile("\\s*\\[javac] '(.*)'\\s*");
    private static final Pattern ANT_FILES_HEADER = Pattern.compile("\\[javac] Files to be compiled:");
    private static final Pattern ANT_FILE = Pattern.compile("\\s*\\[javac]\\s{4,}(\\S+\\.java)\\s*");

    private static final Pattern[] PATTERNS = new Pattern[]{JAVAC, GRADLE, MVN};

    private Javac convertToJavac(String line) {
        return javacLine(line);
    }

    /** Parse a single log line into a {@link Javac} (javac/Gradle/Maven markers), or {@code null}. */
    public Javac javacLine(String line) {
        for (Pattern pattern : PATTERNS) {
            Matcher m = pattern.matcher(line);
            if (m.matches()) {
                return Javac.parse(m.group(1));
            }
        }
        return null;
    }
}
