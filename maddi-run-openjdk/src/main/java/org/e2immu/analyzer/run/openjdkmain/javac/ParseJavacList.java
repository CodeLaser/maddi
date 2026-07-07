package org.e2immu.analyzer.run.openjdkmain.javac;

import org.e2immu.analyzer.run.config.util.JavaModules;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
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

    public InputConfiguration inputConfiguration(List<Javac> javacList, List<String> extraJmods) throws IOException {
        JavacListToSourceSets.Result result = new JavacListToSourceSets().compute(javacList);
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();
        Set<String> closure = new HashSet<>(JavaModules.jmodDependencyClosure("java.se"));
        if (extraJmods != null) {
            extraJmods.forEach(jm -> {
                closure.add(jm);
                closure.addAll(JavaModules.jmodDependencyClosure(jm));
            });
        }
        closure.forEach(jmod -> builder.addClassPathParts(
                new SourceSetImpl.Builder().setName(jmod)
                        .setSourceDirectories(List.of())
                        .setUri(URI.create("jmod:" + jmod))
                        .setLibrary(true)
                        .setExternalLibrary(true)
                        .setPartOfJdk(true)
                        .setModule(true)
                        .build()));
        for (JavacListToSourceSets.JSourceSet js : result.jSourceSets()) {
            builder.addSourceSets(js.sourceSet());
        }
        for (SourceSet sourceSet : result.jars()) {
            builder.addClassPathParts(sourceSet);
        }
        return builder.build();
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
        return lines.stream().map(this::convertToJavac)
                .filter(Objects::nonNull)
                .toList();
    }

    public static final String GRADLE_PATTERN = ".+Compiler arguments: (.+)";
    public static final String MAVEN_PATTERN = "\\[DEBUG] (-d (.+))";

    private static final Pattern JAVAC = Pattern.compile("javac (.+)");
    private static final Pattern GRADLE = Pattern.compile(GRADLE_PATTERN);
    private static final Pattern MVN = Pattern.compile(MAVEN_PATTERN);

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
