/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.analyzer.run.openjdkmain;

import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Exercises the three CLI options that obtain an input configuration from a build/javac log:
 * {@code --compile-log}, {@code --extra-jmod} and {@code --write-input-configuration} (the last one being
 * terminal: write + exit, no analysis -- so these tests never run the analyzer, only the input-config plumbing).
 */
public class TestCompileLogCli {

    private static final String COMPILE_LOG = "src/test/resources/javac/mvnTimefold-solver.txt.gz";
    // a module NOT part of the java.se closure that ParseJavacList always adds, so its presence in the derived
    // classpath proves --extra-jmod actually took effect (as opposed to, say, java.sql, which java.se pulls in)
    private static final String EXTRA_JMOD = "jdk.compiler";

    @Test
    public void deriveFromCompileLog(@TempDir Path tempDir) throws Exception {
        File out = tempDir.resolve("input.json").toFile();

        int exit = Main.execute(new String[]{
                "--" + Main.COMPILE_LOG, COMPILE_LOG,
                "--" + Main.EXTRA_JMOD, EXTRA_JMOD,
                "--" + Main.WRITE_INPUT_CONFIGURATION, out.getAbsolutePath()});

        assertEquals(Main.EXIT_OK, exit);
        assertTrue(out.isFile(), "expected the derived input configuration to be written");

        InputConfiguration ic = read(out);
        assertFalse(ic.sourceSets().isEmpty(), "expected non-empty source sets");
        assertTrue(hasClassPathPart(ic, EXTRA_JMOD),
                "expected the --extra-jmod " + EXTRA_JMOD + " in the derived classpath");
    }

    @Test
    public void inputConfigurationTakesPrecedenceOverCompileLog(@TempDir Path tempDir) throws Exception {
        // 1. derive a config that includes the extra jmod, and write it out
        File derived = tempDir.resolve("derived.json").toFile();
        assertEquals(Main.EXIT_OK, Main.execute(new String[]{
                "--" + Main.COMPILE_LOG, COMPILE_LOG,
                "--" + Main.EXTRA_JMOD, EXTRA_JMOD,
                "--" + Main.WRITE_INPUT_CONFIGURATION, derived.getAbsolutePath()}));
        assertTrue(hasClassPathPart(read(derived), EXTRA_JMOD));

        // 2. pass BOTH --input-configuration and --compile-log, the latter WITHOUT the extra jmod. If
        // --input-configuration wins (it must), the extra jmod is still present in what gets written back.
        File out = tempDir.resolve("out.json").toFile();
        assertEquals(Main.EXIT_OK, Main.execute(new String[]{
                "--" + Main.INPUT_CONFIGURATION, derived.getAbsolutePath(),
                "--" + Main.COMPILE_LOG, COMPILE_LOG,
                "--" + Main.WRITE_INPUT_CONFIGURATION, out.getAbsolutePath()}));

        assertTrue(hasClassPathPart(read(out), EXTRA_JMOD),
                "--input-configuration should take precedence over --compile-log");
    }

    /**
     * End-to-end on a real, on-disk project: this very repository. {@code maddi-cst-api} sits at the bottom of the
     * module hierarchy (single dependency: {@code maddi-support}), so it is fast to analyze. We synthesize a javac
     * invocation for its sources -- source directory from the repo, libraries from this test JVM's own runtime
     * classpath (minus cst-api's own compiled output, since we parse it from source) -- feed it through
     * {@code --compile-log}, and run the prep analysis. Proves the compile-log input method drives a genuine
     * analysis, not just input-configuration derivation.
     */
    @Test
    public void prepAnalyzeMaddiCstApiViaCompileLog(@TempDir Path tempDir) throws Exception {
        Path src = Path.of("..", "maddi-cst-api", "src", "main", "java");
        assumeTrue(Files.isDirectory(src), "maddi-cst-api sources not on disk");

        List<Path> javaFiles;
        try (var walk = Files.walk(src)) {
            javaFiles = walk.filter(p -> p.getFileName().toString().endsWith(".java")).sorted().toList();
        }
        assumeTrue(!javaFiles.isEmpty(), "no maddi-cst-api sources found");

        // libraries = this test JVM's runtime classpath minus cst-api's own compiled output (it is parsed from
        // source here, so it must not also appear as a compiled library on the classpath)
        String classpath = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                .filter(p -> !p.contains("maddi-cst-api"))
                .collect(Collectors.joining(File.pathSeparator));

        String javacLine = "javac -source 25"
                          + " -d " + tempDir.resolve("classes")
                          + " -sourcepath " + src
                          + " -classpath " + classpath + " "
                          + javaFiles.stream().map(Path::toString).collect(Collectors.joining(" "));
        Path log = tempDir.resolve("cstapi-javac.txt");
        Files.writeString(log, javacLine + System.lineSeparator());

        int exit = Main.execute(new String[]{
                "--" + Main.COMPILE_LOG, log.toString(),
                "--" + Main.ANALYSIS_STEPS, Main.AS_PREP,
                "--" + Main.ANALYSIS_RESULTS_DIR, tempDir.resolve("out").toString()});

        assertEquals(Main.EXIT_OK, exit, "prep analysis of maddi-cst-api via --compile-log should succeed");
    }

    private static boolean hasClassPathPart(InputConfiguration ic, String name) {
        return ic.classPathParts().stream().anyMatch(s -> name.equals(s.name()));
    }

    private static InputConfiguration read(File f) throws IOException {
        return JsonStreaming.objectMapper().readValue(f, InputConfigurationImpl.class);
    }
}
