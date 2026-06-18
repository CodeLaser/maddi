package org.e2immu.analyzer.run.openjdkmain.javac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
                    String encoding) {
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

        public Javac build() {
            return new Javac(sourceRelease, targetRelease, release,
                    destination, generatedHeadersDestination, generatedSourceFilesDestination,
                    classpath, modulePath, sourcePath, List.copyOf(sourceFiles),
                    processorPath, annotationProcessing,
                    encoding);
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
            } else {
                switch (option) {
                    case "-g", "-g:lines", "-g:vars", "-g:source", "-g:none",
                         "-implicit:none", "-implicit:class",
                         "-nowarn",
                         "-proc:none", "-parameters",
                         "-deprecation", "--enable-preview" -> LOGGER.debug("Ignoring parameter-less {}", option);
                    default -> {
                        if (i < split.length - 1) {
                            String next = split[i + 1];
                            assert next != null;
                            switch (option) {
                                case "-d" -> builder.destination = next;
                                case "-h" -> builder.generatedHeadersDestination = next;
                                case "-s" -> builder.generatedSourceFilesDestination = next;
                                case "--encoding", "-encoding" -> builder.encoding = next;
                                case "--source", "-source" -> builder.sourceRelease = parseJavaVersion(next);
                                case "--target", "-target" -> builder.targetRelease = parseJavaVersion(next);
                                case "--release", "-release" -> builder.release = parseJavaVersion(next);
                                case "--module-path" -> builder.modulePath = splitPath(next);
                                case "-classpath" -> builder.classpath = splitPath(next);
                                case "-sourcepath" -> builder.sourcePath = splitPath(next);
                                default -> LOGGER.debug("Ignoring parameter option {}", option);
                            }
                            ++i;
                        }
                    }
                }
            }
            ++i;
        }
        return builder.build();
    }

    // FileSystem.getSeparator()
    private static List<String> splitPath(String path) {
        if ("\"\"".equals(path)) return List.of();
        return Arrays.stream(path.split(File.pathSeparator)).filter(s -> !s.isBlank()).toList();
    }
}
