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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

import static org.e2immu.analyzer.modification.common.CommonTest.javaInspectorFactory;

public class TestAnalysisHintsCompiler extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestAnalysisHintsCompiler.class);

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.aapi")).setLevel(Level.DEBUG);
    }

    @Test
    public void test() throws IOException {
        // the archive covers java.desktop (swing/awt) and java.net.http, which the lean default omits
        AnalysisHintsCompiler compiler = new AnalysisHintsCompiler(javaInspectorFactory("java.desktop", "java.net.http"));
        go(compiler, "jdk");
        go(compiler, "libs/test");
        go(compiler, "libs/log");
    }

    private void go(AnalysisHintsCompiler compiler, String library) throws IOException {
        AnalysisHints ah = new AnalysisHints.Builder()
                .setLibraryName(library)
                .setAnalysisResultsDir(Path.of("../maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/" + library))
                .setHintsPath(Path.of("../maddi-aapi-archive/src/main/java"))
                .setPackagePrefix("org.e2immu.analyzer.aapi.archive." + (library.replace("/", ".")))
                .build();
        compiler.go(ah);
    }
}
