package org.e2immu.analyzer.run.kotlinmain.kotlinc;

import org.e2immu.analyzer.run.config.compile.CompileInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * One parsed {@code kotlinc} command line — the Kotlin analogue of {@code Javac}. A {@link CompileInvocation}
 * so it feeds the shared {@code CompileListToSourceSets} engine.
 *
 * <p>kotlinc differs from javac: sources are bare {@code .kt}/{@code .java} args (no {@code -sourcepath}), the
 * module name is explicit ({@code -module-name}), the JVM level is {@code -jvm-target}, and a test compile
 * carries {@code -Xfriend-paths=<main-output>} (its link to the main source set). {@code modulePath()} and
 * {@code sourcePath()} are always empty (Kotlin/JVM has neither).
 */
public record Kotlinc(int jvmTarget,
                      String apiVersion,
                      String languageVersion,
                      String destination, // -d (a dir, or a .jar)
                      String moduleName, // -module-name
                      String jdkHome, // -jdk-home
                      List<String> classpath,
                      List<String> sourcePath, // source directories (Maven gives these directly; Gradle: empty)
                      List<String> sourceFiles,
                      List<String> friendPaths,
                      String encoding) implements CompileInvocation {

    @Override
    public List<String> modulePath() {
        return List.of();
    }

    /**
     * Build a {@link Kotlinc} from the labelled DEBUG lines the kotlin-maven-plugin emits (which give source
     * <i>directories</i> and put the main output on the test classpath rather than using {@code -Xfriend-paths}).
     */
    public static Kotlinc mavenBlock(String destination, List<String> sourceDirs, List<String> classpath, String moduleName) {
        return new Kotlinc(0, null, null, destination, moduleName, null, classpath, List.copyOf(sourceDirs),
                List.of(), List.of(), null);
    }

    public static class Builder {
        int jvmTarget;
        String apiVersion;
        String languageVersion;
        String destination;
        String moduleName;
        String jdkHome;
        List<String> classpath = List.of();
        final List<String> sourceFiles = new LinkedList<>();
        List<String> friendPaths = List.of();
        String encoding;

        public Kotlinc build() {
            return new Kotlinc(jvmTarget, apiVersion, languageVersion, destination, moduleName, jdkHome,
                    classpath, List.of(), List.copyOf(sourceFiles), friendPaths, encoding);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Kotlinc.class);

    private static int parseJvmTarget(String version) {
        try {
            if (version.startsWith("1.")) { // "1.8" -> 8
                String[] parts = version.split("\\.");
                if (parts.length >= 2) return Integer.parseInt(parts[1]);
            }
            return Integer.parseInt(version); // "8", "17", "21"
        } catch (NumberFormatException e) {
            LOGGER.debug("Cannot parse -jvm-target '{}'", version);
            return 0;
        }
    }

    // options that consume the following token as their value (so it is not misread as a source file)
    private static final java.util.Set<String> VALUE_OPTIONS = java.util.Set.of(
            "-d", "-classpath", "-cp", "-module-name", "-jvm-target", "-api-version", "-language-version",
            "-jdk-home", "-kotlin-home", "-script-templates", "-jvm-default", "-expression", "-e", "-P");

    public static Kotlinc parse(String line) {
        List<String> tokens = expandArgFiles(Arrays.stream(line.trim().split("\\s+")).filter(s -> !s.isBlank()).toList());
        Builder builder = new Builder();
        int i = 0;
        while (i < tokens.size()) {
            String option = tokens.get(i);
            if (option.endsWith(".kt") || option.endsWith(".java")) {
                builder.sourceFiles.add(option);
            } else if (option.startsWith("-Xfriend-paths=")) {
                builder.friendPaths = splitPath(option.substring("-Xfriend-paths=".length()));
            } else if (option.equals("-Xfriend-paths")) {
                if (i + 1 < tokens.size()) builder.friendPaths = splitPath(tokens.get(++i));
            } else if (option.startsWith("-X")) {
                LOGGER.debug("Ignoring {}", option); // -Xplugin=…, -Xjsr305=…, param-less -X…: single token
            } else if (VALUE_OPTIONS.contains(option) && i + 1 < tokens.size()) {
                String next = tokens.get(++i); // consume the value
                switch (option) {
                    case "-d" -> builder.destination = next;
                    case "-classpath", "-cp" -> builder.classpath = splitPath(next);
                    case "-module-name" -> builder.moduleName = next;
                    case "-jvm-target" -> builder.jvmTarget = parseJvmTarget(next);
                    case "-api-version" -> builder.apiVersion = next;
                    case "-language-version" -> builder.languageVersion = next;
                    case "-jdk-home" -> builder.jdkHome = next;
                    default -> LOGGER.debug("Ignoring option {} with value", option); // -P, -kotlin-home, …
                }
            } else {
                LOGGER.debug("Ignoring {}", option); // param-less (-no-stdlib, -verbose, …) or unknown: no value
            }
            ++i;
        }
        return builder.destination == null ? null : builder.build();
    }

    /** Replace any {@code @argfile} token with the whitespace-split tokens of that file (one level). */
    private static List<String> expandArgFiles(List<String> tokens) {
        boolean any = tokens.stream().anyMatch(t -> t.startsWith("@"));
        if (!any) return tokens;
        List<String> out = new ArrayList<>();
        for (String t : tokens) {
            if (t.startsWith("@")) {
                Path file = Path.of(t.substring(1));
                try {
                    String content = Files.readString(file);
                    Arrays.stream(content.split("\\s+")).filter(s -> !s.isBlank()).forEach(out::add);
                } catch (IOException e) {
                    LOGGER.warn("Cannot read argfile {}", file);
                }
            } else {
                out.add(t);
            }
        }
        return out;
    }

    private static List<String> splitPath(String path) {
        if ("\"\"".equals(path) || path.isBlank()) return List.of();
        return Arrays.stream(path.split(File.pathSeparator)).filter(s -> !s.isBlank()).toList();
    }
}
