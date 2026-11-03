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

import io.codelaser.maddi.inspection.api.resource.BuildFiles;
import io.codelaser.maddi.run.config.javac.ParseJavacList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A build to capture the javac invocations of: a log, a pattern, javac lines, then the configuration. Gradle, Maven
 * and any other build differ only in the command, the pattern and the build files. Everything in it is a string or a
 * path, so a store can write it down and read it back ({@link ConfiguredBy} plus these fields).
 *
 * @param kind             {@link ConfiguredBy#GRADLE_CAPTURE}, {@link ConfiguredBy#MAVEN_CAPTURE} or
 *                         {@link ConfiguredBy#COMPILE_LOG}
 * @param root             the directory the command runs in, and the root of the build-file globs
 * @param command          the build to run; empty to read {@code existingLog} instead (compile-log only). The Gradle
 *                         capture's own command is filled in when it runs ({@link #gradle}): it needs its init script
 * @param existingLog      a log a build already wrote; null when the command writes it
 * @param javacLinePattern which lines are javac invocations, group 1 being javac's arguments; null for the kind's own
 *                         ({@link #patternOrDefault})
 * @param buildFileGlobs   the files whose change makes the configuration stale, relative to {@code root}; empty: its
 *                         freshness cannot be checked, and is reported UNCHECKED
 */
public record CaptureSpec(ConfiguredBy kind, Path root, List<String> command, Path existingLog,
                          String javacLinePattern, List<String> buildFileGlobs) {

    /**
     * A javac command line, as a build or a shell trace echoes it: {@code javac <args>}, {@code /path/to/javac <args>},
     * {@code + javac @argfile}. The arguments may name argument files; they are expanded when captured.
     */
    public static final String JAVAC_COMMAND_LINE = "\\s*(?:[+$]\\s+)?(?:\\S*[/\\\\])?javac(?:\\.exe)?\\s+(.+)";

    public CaptureSpec {
        if (!kind.isCapture()) throw new IllegalArgumentException(kind + " is not a capture");
        command = List.copyOf(command);
        buildFileGlobs = List.copyOf(buildFileGlobs);
        if (command.isEmpty() && existingLog == null) {
            throw new IllegalArgumentException("a capture needs a command to run or a log to read");
        }
        if (kind != ConfiguredBy.COMPILE_LOG && existingLog != null) {
            throw new IllegalArgumentException("only a compile-log capture reads an existing log");
        }
    }

    /**
     * Gradle, every Java source set compiled ({@code maddiCompileAll}, from an init script) with {@code --debug}, so
     * that each compile task logs javac's arguments. The command is the build's wrapper, else {@code gradle}.
     */
    public static CaptureSpec gradle(Path root) {
        return gradle(root, null);
    }

    /**
     * Gradle, as {@link #gradle(Path)}, with the Gradle daemon on {@code javaHome} ({@code -Dorg.gradle.java.home}):
     * the default JDK may be newer than the build's Gradle supports, which then fails as soon as it must compile a
     * build script again (an edited {@code settings.gradle}: "Unsupported class file major version").
     *
     * @param javaHome null: Gradle's own choice (its {@code JAVA_HOME}, else the {@code java} on the PATH)
     */
    public static CaptureSpec gradle(Path root, Path javaHome) {
        List<String> command = new ArrayList<>(List.of(tool(root, "gradlew", "gradle")));
        if (javaHome != null) command.add("-Dorg.gradle.java.home=" + javaHome);
        return new CaptureSpec(ConfiguredBy.GRADLE_CAPTURE, root, command, null, null, BuildFiles.GRADLE_GLOBS);
    }

    /**
     * Maven, main and test sources compiled with {@code -X}, which logs javac's arguments. {@code clean} first: the
     * compiler plugin skips a module that is up to date, and a skipped module logs nothing.
     */
    public static CaptureSpec maven(Path root) {
        return maven(root, null);
    }

    /**
     * Maven, as {@link #maven(Path)}, run on {@code javaHome}: the command starts with {@code env JAVA_HOME=...}.
     *
     * @param javaHome null: Maven's own choice (its {@code JAVA_HOME}, else the {@code java} on the PATH)
     */
    public static CaptureSpec maven(Path root, Path javaHome) {
        List<String> command = new ArrayList<>();
        if (javaHome != null) command.addAll(List.of("env", "JAVA_HOME=" + javaHome));
        command.addAll(List.of(tool(root, "mvnw", "mvn"), "-B", "-X", "clean", "compile", "test-compile"));
        return new CaptureSpec(ConfiguredBy.MAVEN_CAPTURE, root, command, null, null, BuildFiles.MAVEN_GLOBS);
    }

    /**
     * Any other build.
     *
     * @param command     the command that writes the log on its output; empty to read {@code existingLog}
     * @param existingLog a log already written; null when {@code command} is given
     * @param pattern     null: {@link #JAVAC_COMMAND_LINE}
     * @param globs       empty: freshness UNCHECKED
     */
    public static CaptureSpec compileLog(Path root, List<String> command, Path existingLog, String pattern,
                                         List<String> globs) {
        return new CaptureSpec(ConfiguredBy.COMPILE_LOG, root, command == null ? List.of() : command, existingLog,
                pattern, globs == null ? List.of() : globs);
    }

    public String patternOrDefault() {
        if (javacLinePattern != null) return javacLinePattern;
        return switch (kind) {
            case GRADLE_CAPTURE -> ParseJavacList.GRADLE_PATTERN;
            case MAVEN_CAPTURE -> ParseJavacList.MAVEN_PATTERN;
            default -> JAVAC_COMMAND_LINE;
        };
    }

    /** What the capture runs, or reads; for a store's listing and an answer to the user. */
    public String describe() {
        String what = existingLog != null ? "the log " + existingLog : String.join(" ", command);
        return kind.key() + ": " + what + " in " + root;
    }

    /** A Gradle capture's full command: the build's own, then the init script and the flags that make it log. */
    List<String> gradleCommand(Path initScript) {
        List<String> out = new ArrayList<>(command);
        out.addAll(List.of("-I", initScript.toString(), "--debug", "--no-build-cache", "--no-configuration-cache",
                "--console=plain", "maddiCompileAll"));
        return out;
    }

    /** Whether a directory holds a build the Gradle capture can run: a settings or build script at its root. */
    public static boolean isGradleBuild(Path root) {
        return List.of("settings.gradle", "settings.gradle.kts", "build.gradle", "build.gradle.kts").stream()
                .anyMatch(f -> Files.isRegularFile(root.resolve(f)));
    }

    /** Whether a directory holds a build the Maven capture can run. */
    public static boolean isMavenBuild(Path root) {
        return Files.isRegularFile(root.resolve("pom.xml"));
    }

    /* the build's wrapper, else the tool on the PATH */
    static String tool(Path root, String wrapper, String tool) {
        Path script = root.resolve(wrapper);
        return Files.isExecutable(script) ? script.toString() : tool;
    }
}
