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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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

    public static void main(String[] args) throws IOException {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.aapi")).setLevel(Level.INFO);
        compileAll();
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
}
