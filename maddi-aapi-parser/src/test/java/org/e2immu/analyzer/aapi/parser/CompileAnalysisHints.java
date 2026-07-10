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

package org.e2immu.analyzer.aapi.parser;

import ch.qos.logback.classic.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.e2immu.analyzer.modification.common.CommonTest.javaInspectorFactory;

/**
 * Compiles the hand-written analysis hints in {@code maddi-aapi-archive/src/main/java} into the analysis-result
 * (.json) files under {@code .../analyzedPackageFiles/<library>}. This is the same work as
 * {@link TestAnalysisHintsCompiler}, but runnable as a build task ({@code gradle :maddi-aapi-parser:compileAnalysisHints})
 * rather than as a test. Paths are relative to the {@code maddi-aapi-parser} module directory (the task sets that
 * as the working directory).
 */
public class CompileAnalysisHints {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompileAnalysisHints.class);

    static final String HINTS_PATH = "../maddi-aapi-archive/src/main/java";
    static final String RESULTS_BASE =
            "../maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/";
    static final List<String> LIBRARIES = List.of("jdk", "libs/test", "libs/log");
    // fixed entry timestamp (2020-01-01T00:00:00Z) so a regenerated jar only differs when its content does
    private static final long FIXED_ENTRY_TIME = 1_577_836_800_000L;

    public static void main(String[] args) throws IOException {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.aapi")).setLevel(Level.INFO);
        compileAll();
        packageJars();
    }

    /** Compile every configured library; reused by {@link TestAnalysisHintsCompiler}. */
    public static void compileAll() throws IOException {
        AnalysisHintsCompiler compiler = new AnalysisHintsCompiler(javaInspectorFactory());
        for (String library : LIBRARIES) {
            compile(compiler, library);
        }
    }

    private static void compile(AnalysisHintsCompiler compiler, String library) throws IOException {
        AnalysisHints analysisHints = new AnalysisHints.Builder()
                .setLibraryName(library)
                .setAnalysisResultsDir(Path.of(RESULTS_BASE + library))
                .setHintsPath(Path.of(HINTS_PATH))
                .setPackagePrefix("org.e2immu.analyzer.aapi.archive." + library.replace("/", "."))
                .build();
        LOGGER.info("Compiling analysis hints for library '{}'", library);
        compiler.go(analysisHints);
    }

    /**
     * Package the generated result files into the two archive jars (the former copyToJars.sh):
     * {@code openjdk.jar} holds {@code jdk/*.json} at its root, {@code libs.jar} holds {@code libs/<lib>/*.json}
     * keeping the {@code <lib>/} directory. They are loaded from the classpath as {@code resource:.../*.jar}.
     */
    static void packageJars() throws IOException {
        Path base = Path.of(RESULTS_BASE);
        Path jdk = base.resolve("jdk");
        try (var stream = Files.list(jdk)) {
            writeJar(base.resolve("openjdk.jar"), jdk, stream.filter(CompileAnalysisHints::isJson).sorted().toList());
        }
        Path libs = base.resolve("libs");
        try (var stream = Files.walk(libs)) {
            writeJar(base.resolve("libs.jar"), libs, stream.filter(CompileAnalysisHints::isJson).sorted().toList());
        }
    }

    private static boolean isJson(Path p) {
        return Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json");
    }

    private static void writeJar(Path jarFile, Path base, List<Path> jsonFiles) throws IOException {
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile))) {
            // a minimal, fixed-time manifest first (as `jar cf` writes one), then the sorted .json entries
            putEntry(jos, "META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            for (Path file : jsonFiles) {
                String name = base.relativize(file).toString().replace(File.separatorChar, '/');
                putEntry(jos, name, Files.readAllBytes(file));
            }
        }
        LOGGER.info("Packaged {} ({} entries)", jarFile, jsonFiles.size());
    }

    private static void putEntry(JarOutputStream jos, String name, byte[] content) throws IOException {
        JarEntry entry = new JarEntry(name);
        entry.setTime(FIXED_ENTRY_TIME);
        jos.putNextEntry(entry);
        jos.write(content);
        jos.closeEntry();
    }
}
