/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.project.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.codelaser.maddi.inspection.api.resource.BuildFiles;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.BuildFilesCapture;
import io.codelaser.maddi.run.config.javac.Javac;
import io.codelaser.maddi.run.config.javac.ParseJavacList;
import io.codelaser.maddi.run.config.util.JsonStreaming;

/**
 * A build's javac invocations, taken from its output as it runs, made into an {@code inputConfiguration.json} with
 * its build files hashed. The output is streamed, never held: a Gradle {@code --debug} or Maven {@code -X} log of a
 * large build runs to hundreds of MB. Of it, {@code compile.log} keeps the javac lines, each as {@code javac <args>}
 * with its argument files expanded (the build deletes them); {@code build.log} keeps the command and what a failure
 * needs, without the debug chatter. Everything else is dropped.
 */
public final class CompileLogCapture {
    private CompileLogCapture() {
    }

    public static final String COMPILE_LOG = "compile.log";
    public static final String BUILD_LOG = "build.log";
    public static final String INPUT_CONFIGURATION = "inputConfiguration.json";
    private static final String GRADLE_INIT_SCRIPT = "compile-all.init.gradle.kts";

    /**
     * Run (or read) the build, and write {@link #COMPILE_LOG}, {@link #BUILD_LOG} and {@link #INPUT_CONFIGURATION}
     * into {@code workDirectory}.
     *
     * @param extraJmods JDK modules to add to those the invocations name
     * @throws CaptureException when the build fails, or no line holds a javac invocation
     */
    public static CaptureResult capture(CaptureSpec spec, Path workDirectory, List<String> extraJmods,
                                        CaptureListener listener) throws IOException, InterruptedException {
        Files.createDirectories(workDirectory);
        Path compileLog = workDirectory.resolve(COMPILE_LOG);
        Pattern pattern = Pattern.compile(spec.patternOrDefault());
        int compilations;
        Path buildLog;
        if (spec.command().isEmpty()) {
            buildLog = null;
            try (BufferedReader reader = Files.newBufferedReader(spec.existingLog(), StandardCharsets.UTF_8);
                 BufferedWriter compile = Files.newBufferedWriter(compileLog)) {
                compilations = filter(reader, pattern, spec.root(), compile, null, listener);
            }
            if (compilations == 0) {
                throw new CaptureException("no line of " + spec.existingLog() + " is a javac invocation (pattern "
                                           + pattern + ")", 0, null);
            }
        } else {
            buildLog = workDirectory.resolve(BUILD_LOG);
            List<String> command = spec.kind() == ConfiguredBy.GRADLE_CAPTURE
                    ? spec.gradleCommand(gradleInitScript(workDirectory)) : spec.command();
            Process process = new ProcessBuilder(command).directory(spec.root().toFile()).redirectErrorStream(true)
                    .start();
            listener.process(process);
            try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8);
                 BufferedWriter compile = Files.newBufferedWriter(compileLog);
                 BufferedWriter build = Files.newBufferedWriter(buildLog)) {
                build.write("$ " + String.join(" ", command) + "\n");
                compilations = filter(reader, pattern, spec.root(), compile, build, listener);
            } catch (IOException | RuntimeException e) {
                process.destroyForcibly();
                throw e;
            } finally {
                listener.process(null);
            }
            int exit = process.waitFor();
            if (exit != 0) throw new CaptureException("the build failed (exit " + exit + ")", exit, buildLog);
            if (compilations == 0) {
                throw new CaptureException("the build compiled no Java source set: no line of its output is a javac"
                                           + " invocation (pattern " + pattern + ")", 0, buildLog);
            }
        }
        Path output = workDirectory.resolve(INPUT_CONFIGURATION);
        InputConfiguration configuration = fromCompileLog(compileLog, spec.root(), spec.buildFileGlobs(), extraJmods,
                output);
        return new CaptureResult(compilations, compileLog, buildLog, output, configuration.buildFiles(),
                configuration.sourceSets().size());
    }

    /**
     * A compile log made into a configuration, with the build files under {@code root} that {@code globs} name
     * hashed now, and written to {@code output}. No globs: nothing to check freshness against but the module-infos.
     */
    public static InputConfiguration fromCompileLog(Path compileLog, Path root, List<String> globs,
                                                    List<String> extraJmods, Path output) throws IOException {
        ParseJavacList parser = new ParseJavacList();
        List<Javac> invocations = parser.javacLines(compileLog);
        InputConfiguration parsed = parser.inputConfiguration(invocations, extraJmods);
        if (parsed.sourceSets().isEmpty()) {
            throw new IOException("no javac invocation in " + compileLog + ": nothing was compiled, or the log holds no"
                                  + " compiler arguments");
        }
        BuildFiles buildFiles = globs.isEmpty() ? BuildFilesCapture.none(root, parsed)
                : BuildFilesCapture.capture(BuildFiles.COMPILE_LOG, root, globs, List.of(), parsed);
        InputConfiguration configuration = parsed.withBuildFiles(buildFiles);
        if (output.toAbsolutePath().getParent() != null) Files.createDirectories(output.toAbsolutePath().getParent());
        Path tmp = output.resolveSibling(output.getFileName() + ".tmp");
        JsonStreaming.objectMapper().writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), configuration);
        Files.move(tmp, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return configuration;
    }

    /**
     * Debug chatter, which {@code build.log} leaves out: Gradle's {@code [DEBUG]} and {@code [INFO]} levels, Maven's
     * {@code [DEBUG]}. Maven's {@code [INFO]} stays (it carries {@code BUILD FAILURE} and the reactor summary), as do
     * every build's warnings and errors.
     */
    static boolean chatter(String line) {
        return line.contains("[DEBUG]") || line.contains(" [INFO] ");
    }

    private static int filter(BufferedReader reader, Pattern pattern, Path root, Writer compile, Writer build,
                              CaptureListener listener) throws IOException {
        int compilations = 0;
        for (String line; (line = reader.readLine()) != null; ) {
            Matcher m = pattern.matcher(line);
            if (m.matches()) {
                compile.write("javac " + expandArgumentFiles(m.group(1), root));
                compile.write('\n');
                listener.progress(++compilations + " compilation(s) logged");
            } else if (build != null && !chatter(line)) {
                build.write(line);
                build.write('\n');
            }
        }
        return compilations;
    }

    /**
     * javac's arguments with each {@code @file} replaced by the arguments in it, relative to {@code root}. The
     * parser splits on whitespace, so an argument holding whitespace is refused rather than split in two.
     */
    static String expandArgumentFiles(String arguments, Path root) throws IOException {
        if (!arguments.contains("@")) return arguments;
        List<String> out = new ArrayList<>();
        for (String argument : arguments.trim().split("\\s+")) {
            if (argument.length() > 1 && argument.startsWith("@")) {
                Path file = root.resolve(argument.substring(1));
                if (!Files.isRegularFile(file)) {
                    throw new IOException("javac argument file " + file + " is gone: capture while the build runs");
                }
                out.addAll(argumentFile(file));
            } else {
                out.add(argument);
            }
        }
        return String.join(" ", out);
    }

    /* javac's @file syntax: arguments separated by whitespace, quoted with " or ' when they contain it */
    private static List<String> argumentFile(Path file) throws IOException {
        List<String> out = new ArrayList<>();
        String content = Files.readString(file);
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean inArgument = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
                else current.append(c);
            } else if (c == '"' || c == '\'') {
                quote = c;
                inArgument = true;
            } else if (Character.isWhitespace(c)) {
                if (inArgument) out.add(current.toString());
                current.setLength(0);
                inArgument = false;
            } else {
                current.append(c);
                inArgument = true;
            }
        }
        if (inArgument) out.add(current.toString());
        List<String> unsplittable = out.stream().filter(a -> a.isEmpty() || a.chars().anyMatch(Character::isWhitespace))
                .toList();
        if (!unsplittable.isEmpty()) {
            throw new IOException("javac argument file " + file + " holds argument(s) that cannot survive the split"
                                  + " into a javac command line (empty, or containing whitespace): "
                                  + unsplittable.subList(0, Math.min(3, unsplittable.size())));
        }
        return out;
    }

    private static Path gradleInitScript(Path workDirectory) throws IOException {
        Path initScript = workDirectory.resolve(GRADLE_INIT_SCRIPT);
        try (InputStream in = CompileLogCapture.class.getResourceAsStream(GRADLE_INIT_SCRIPT)) {
            if (in == null) throw new IOException(GRADLE_INIT_SCRIPT + " is missing from maddi-project-config's jar");
            Files.copy(in, initScript, StandardCopyOption.REPLACE_EXISTING);
        }
        return initScript;
    }
}
