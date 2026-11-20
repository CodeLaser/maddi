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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.codelaser.maddi.run.config.javac.ParseJavacList;

public class CompileLogCaptureTest {

    /* a one-class project; returns its root */
    static Path project(Path dir) throws IOException {
        Path root = Files.createDirectories(dir.resolve("p"));
        Path x = Files.createDirectories(root.resolve("src/main/java/x"));
        Files.writeString(x.resolve("A.java"), "package x;\npublic class A {\n}\n");
        Files.writeString(root.resolve("build.txt"), "version 1\n");
        return root;
    }

    /* the javac line a build of that project would echo */
    static String javacLine(Path root) {
        return "javac -d " + root.resolve("build/classes/java/main") + " "
               + root.resolve("src/main/java/x/A.java");
    }

    static List<String> printing(String script) {
        return List.of("sh", "-c", script);
    }

    @Test
    public void argumentFiles(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("args"), "-d \"/out/classes\"\n'-g'  /src/A.java\n");
        assertEquals("-proc:none -d /out/classes -g /src/A.java",
                CompileLogCapture.expandArgumentFiles("-proc:none @args", dir));
        assertEquals("-d x", CompileLogCapture.expandArgumentFiles("-d x", dir), "no argument file: as it was");

        Files.writeString(dir.resolve("spaced"), "-d \"/my classes\"\n");
        IOException e = assertThrows(IOException.class, () -> CompileLogCapture.expandArgumentFiles("@spaced", dir));
        assertTrue(e.getMessage().contains("/my classes"), e.getMessage());
        assertThrows(IOException.class, () -> CompileLogCapture.expandArgumentFiles("@gone", dir));
    }

    @Test
    public void chatter() {
        assertTrue(CompileLogCapture.chatter("2026-11-02T10:00:00.000+0100 [DEBUG] [org.gradle] x"));
        assertTrue(CompileLogCapture.chatter("2026-11-02T10:00:00.000+0100 [INFO] [org.gradle] x"));
        assertFalse(CompileLogCapture.chatter("2026-11-02T10:00:00.000+0100 [LIFECYCLE] [org.gradle] BUILD SUCCESSFUL"));
        assertTrue(CompileLogCapture.chatter("[DEBUG] (f) basedir = /x"));
        assertFalse(CompileLogCapture.chatter("[INFO] BUILD FAILURE"), "Maven's INFO stays");
        assertFalse(CompileLogCapture.chatter("[INFO] Reactor Summary for p 1.0:"));
        assertFalse(CompileLogCapture.chatter("[ERROR] Failed to execute goal"));
    }

    @Test
    public void javacCommandLines() {
        var pattern = Pattern.compile(CaptureSpec.JAVAC_COMMAND_LINE);
        for (String line : List.of("javac -d out A.java", "/usr/bin/javac -d out A.java", "+ javac @args",
                "  C:\\jdk\\bin\\javac.exe -d out A.java")) {
            var m = pattern.matcher(line);
            assertTrue(m.matches(), line);
            assertTrue(m.group(1).startsWith("-d") || m.group(1).startsWith("@"), line);
        }
        assertFalse(pattern.matcher("    [javac] Compiling 3 source files").matches());
        assertFalse(pattern.matcher("using javac").matches());
    }

    @Test
    public void fromACommand(@TempDir Path dir) throws Exception {
        Path root = project(dir);
        Files.writeString(root.resolve("args"), root.resolve("src/main/java/x/A.java").toString());
        CaptureSpec spec = CaptureSpec.compileLog(root, printing(
                "echo '[DEBUG] noise'; echo '+ /usr/bin/javac -d " + root.resolve("build/classes/java/main")
                + " @args'; echo '[WARNING] a warning'"), null, null, List.of("build.txt"));
        List<String> progress = new ArrayList<>();
        List<Process> processes = new ArrayList<>();
        CaptureResult result = CompileLogCapture.capture(spec, dir.resolve("work"), List.of(), new CaptureListener() {
            @Override
            public void progress(String message) {
                progress.add(message);
            }

            @Override
            public void process(Process process) {
                processes.add(process);
            }
        });
        assertEquals(1, result.compilations());
        assertEquals(List.of("1 compilation(s) logged"), progress);
        assertNotNull(processes.getFirst());
        assertNull(processes.getLast(), "the process is let go when it ended");
        assertEquals(List.of("javac -d " + root.resolve("build/classes/java/main") + " "
                             + root.resolve("src/main/java/x/A.java")), Files.readAllLines(result.compileLog()),
                "the argument file expanded: the build deletes it");
        List<String> buildLog = Files.readAllLines(result.buildLog());
        assertTrue(buildLog.getFirst().startsWith("$ sh -c"), buildLog::toString);
        assertEquals(List.of("[WARNING] a warning"), buildLog.subList(1, buildLog.size()));
        assertEquals(1, result.sourceSets());
        assertEquals(List.of("build.txt"), List.copyOf(result.buildFiles().sha256().keySet()));
        assertEquals(BuildFileCheck.State.FRESH, BuildFileCheck.check(result.inputConfiguration()).state());

        Files.writeString(root.resolve("build.txt"), "version 2\n");
        BuildFileCheck.Freshness stale = BuildFileCheck.check(result.inputConfiguration());
        assertEquals(BuildFileCheck.State.STALE, stale.state());
        assertEquals(List.of("build.txt (changed)"), stale.changed());
    }

    @Test
    public void fromAnExistingLog(@TempDir Path dir) throws Exception {
        Path root = project(dir);
        Path log = dir.resolve("build-output.txt");
        Files.writeString(log, "some line\n" + javacLine(root) + "\nanother\n");
        CaptureResult result = CompileLogCapture.capture(CaptureSpec.compileLog(root, List.of(), log, null, List.of()),
                dir.resolve("work"), List.of(), CaptureListener.NONE);
        assertEquals(1, result.compilations());
        assertNull(result.buildLog(), "nothing ran");
        assertEquals(BuildFileCheck.State.UNCHECKED, BuildFileCheck.check(result.inputConfiguration()).state(),
                "no globs: nothing to compare with");
    }

    @Test
    public void aFailedBuild(@TempDir Path dir) throws Exception {
        Path root = project(dir);
        CaptureSpec spec = CaptureSpec.compileLog(root, printing("echo '[DEBUG] x'; echo 'error: it broke'; exit 3"),
                null, null, List.of());
        CaptureException e = assertThrows(CaptureException.class,
                () -> CompileLogCapture.capture(spec, dir.resolve("work"), List.of(), CaptureListener.NONE));
        assertEquals(3, e.exitCode());
        assertEquals("error: it broke", e.buildLogTail().getLast());
        assertTrue(e.getMessage().contains("exit 3") && e.getMessage().contains(e.buildLog().toString()),
                e.getMessage());

        CaptureSpec nothing = CaptureSpec.compileLog(root, printing("echo compiled nothing"), null, null, List.of());
        CaptureException none = assertThrows(CaptureException.class,
                () -> CompileLogCapture.capture(nothing, dir.resolve("work"), List.of(), CaptureListener.NONE));
        assertEquals(0, none.exitCode());
        assertTrue(none.getMessage().contains("no Java source set"), none.getMessage());
    }

    @Test
    public void specs(@TempDir Path dir) {
        CaptureSpec maven = CaptureSpec.maven(dir);
        assertEquals(List.of("mvn", "-B", "-X", "clean", "compile", "test-compile"), maven.command());
        assertEquals(ParseJavacList.MAVEN_PATTERN, maven.patternOrDefault());
        assertTrue(maven.buildFileGlobs().contains("**/pom.xml"));
        assertEquals(List.of("gradle"), CaptureSpec.gradle(dir).command());
        assertThrows(IllegalArgumentException.class,
                () -> CaptureSpec.compileLog(dir, List.of(), null, null, List.of()), "a command or a log");
    }
}
