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

package io.codelaser.maddi.run.openjdkmain;

import io.codelaser.maddi.run.config.util.JsonStreaming;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Exercises the three CLI options that obtain an input configuration from a build/javac log:
 * {@code --compile-log}, {@code --extra-jmod} and {@code --write-input-configuration} (the last one being
 * terminal: write + exit, no analysis -- so these tests never run the analyzer, only the input-config plumbing).
 */

/**
 * The one test of {@code TestCompileLogCli} (maddi-run-openjdk) that runs an analysis step, moved here in split
 * stage 3: a base module cannot run the analysis, even in a test. The rest of that class tests the compile-log
 * route itself, which is parse-only, and stays with it.
 */
public class TestCompileLogCliPrep {
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
}
