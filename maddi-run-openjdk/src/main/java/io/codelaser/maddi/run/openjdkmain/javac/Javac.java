package org.e2immu.analyzer.run.openjdkmain.javac;

import org.e2immu.analyzer.run.config.compile.CompileInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public record Javac(int sourceRelease,
                    int targetRelease,
                    int release,
                    String destination, // -d
                    String generatedHeadersDestination, // -h
                    String generatedSourceFilesDestination, // -s
                    List<String> classpath,
                    List<String> modulePath,
                    List<String> sourcePath, // sources
                    List<String> sourceFiles,
                    List<String> processorPath,
                    String annotationProcessing,
                    String encoding,
                    List<String> addModules) implements CompileInvocation {
    public static class Builder {
        int sourceRelease;
        int targetRelease;
        int release;
        String destination; // -d
        String generatedHeadersDestination; // -h
        String generatedSourceFilesDestination; // -s
        List<String> classpath;
        List<String> modulePath; // dirs or jars in the build/libs/ of the project
        List<String> sourcePath; // sources
        List<String> sourceFiles = new LinkedList<>();
        List<String> processorPath;
        String annotationProcessing;

        String encoding;
        List<String> addModules = List.of();

        public Javac build() {
            return new Javac(sourceRelease, targetRelease, release,
                    destination, generatedHeadersDestination, generatedSourceFilesDestination,
                    classpath, modulePath, sourcePath, List.copyOf(sourceFiles),
                    processorPath, annotationProcessing,
                    encoding, addModules);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Javac.class);

    private static int parseJavaVersion(String version) {
        try {
            // Handle decimal versions like "1.8", "1.7"
            if (version.startsWith("1.")) {
                String[] parts = version.split("\\.");
                if (parts.length >= 2) {
                    return Integer.parseInt(parts[1]); // "1.8" -> 8, "1.7" -> 7
                }
            }
            // Handle direct integer versions like "11", "17", "21"
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Java version format: '" + version + "'. Expected formats: '1.8', '11', '17', etc.", e);
        }
    }

    public static Javac parse(String line) {
        String[] split = line.split("\\s+");
        int i = 0;
        Builder builder = new Builder();

        while (i < split.length) {
            String option = split[i];
            if (option.endsWith(".java")) {
                builder.sourceFiles.add(option);
            } else if (option.startsWith("-X") || option.startsWith("-J")) {
                LOGGER.debug("Ignoring -X,-J {}", option);
            } else if (isLongOptionWithEquals(option)) {
                // ⛔ A GNU-STYLE `--option=value` USED TO BE DROPPED IN SILENCE, and it is not hypothetical: Gradle
                // writes `--module-path=<path>` on every modular compile it logs. Today that is harmless only BY
                // LUCK — Gradle happens to emit the space form on the same line first, so the value is set anyway —
                // and worse, the `default` branch below consumed the NEXT token as if this option took one, which
                // swallowed the `--module-version=...` that followed. javac accepts both spellings for every long
                // option, so a build tool that emits only the `=` form would have produced a source set with an
                // EMPTY module path and no complaint. Split on the first '=' and route it through the same table.
                int eq = option.indexOf('=');
                applyValuedOption(builder, option.substring(0, eq), option.substring(eq + 1));
            } else if (VALUED_OPTIONS.contains(option) && i < split.length - 1) {
                applyValuedOption(builder, option, split[i + 1]);
                ++i;
            } else {
                // ⛔ ONLY A KNOWN OPTION CONSUMES THE TOKEN AFTER IT. This used to be inverted: every option not on
                // a hard-coded parameter-less list was ASSUMED to take a value, so an unlisted flag ate its
                // successor — and on a javac line the successor is very often the first SOURCE FILE. A dropped
                // source file is invisible: the source set still parses, with one type fewer.
                // ⚠ MEASURED BEFORE CHANGING IT, and it is prevention rather than repair: over 48 real Gradle
                // javac lines from the Elasticsearch corpus this swallowed **0** source files, because an -X option
                // happens to sit last on every one of them. The fixture found it; the corpus says it does not fire
                // here. *A list of parameter-less options records what somebody remembered; the valued table is the
                // one that has to be right, so it is the only one kept.*
                // An unknown option's VALUE, now examined on its own, is a path or a module spec and falls through
                // here harmlessly — see the --patch-module and -Akey=value controls in TestJavacGradleLine.
                LOGGER.debug("Ignoring {}", option);
            }
            ++i;
        }
        return builder.build();
    }

    private static final Set<String> VALUED_OPTIONS = Set.of("-d", "-h", "-s",
            "--encoding", "-encoding",
            "--source", "-source", "--target", "-target", "--release", "-release",
            "--module-path", "-p",
            "-classpath", "-cp", "--class-path",
            "-sourcepath", "--source-path",
            // ⚠ VALUED: `--add-modules jdk.incubator.vector`. Without it here the module name is read as a
            // source FILE, so the flag is lost AND a bogus compilation unit is added.
            "--add-modules");

    /**
     * A long option carrying its value inline. Restricted to {@code --} so that a value containing an {@code '='}
     * — {@code --patch-module java.base=some.jar}, or a {@code -Akey=value} annotation-processor option — is not
     * mistaken for one, and so a short option's separate value is left to the caller's lookahead.
     */
    private static boolean isLongOptionWithEquals(String option) {
        return option.startsWith("--") && option.indexOf('=') > 2;
    }

    private static void applyValuedOption(Builder builder, String option, String value) {
        switch (option) {
            case "-d" -> builder.destination = value;
            case "-h" -> builder.generatedHeadersDestination = value;
            case "-s" -> builder.generatedSourceFilesDestination = value;
            case "--encoding", "-encoding" -> builder.encoding = value;
            case "--source", "-source" -> builder.sourceRelease = parseJavaVersion(value);
            case "--target", "-target" -> builder.targetRelease = parseJavaVersion(value);
            case "--release", "-release" -> builder.release = parseJavaVersion(value);
            case "--module-path", "-p" -> builder.modulePath = splitPath(value);
            case "-classpath", "-cp", "--class-path" -> builder.classpath = splitPath(value);
            case "-sourcepath", "--source-path" -> builder.sourcePath = splitPath(value);
            // comma-separated, per javac's own grammar; ALL/ALL-MODULE-PATH and friends are carried through
            // unchanged -- this records what the build said, it does not interpret it
            case "--add-modules" -> builder.addModules = Arrays.stream(value.split(","))
                    .map(String::strip).filter(m -> !m.isBlank()).toList();
            default -> LOGGER.debug("Ignoring parameter option {}", option);
        }
    }

    // FileSystem.getSeparator()
    private static List<String> splitPath(String path) {
        if ("\"\"".equals(path)) return List.of();
        return Arrays.stream(path.split(File.pathSeparator)).filter(s -> !s.isBlank()).toList();
    }
}
