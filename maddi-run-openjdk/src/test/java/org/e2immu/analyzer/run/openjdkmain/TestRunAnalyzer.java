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

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
public class TestRunAnalyzer {

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.e2immu.analyzer.shallow")).setLevel(Level.DEBUG);
    }

    @Test
    public void test() throws IOException {
        Path cstApiPath = Path.of("../maddi-cst-api/src/main/java").toRealPath();
        assertTrue(Files.isDirectory(cstApiPath));

        // located by glob rather than by version: a hard-coded "maddi-support-<version>.jar" breaks on
        // every release bump (see gradle.properties), and toRealPath() then fails with NoSuchFileException
        Path maddiSupportJar = findMaddiSupportJar();

        Main.main(new String[]{
                "--debug=classpath",
                "--jmod=java.base",
                "--classpath=" + maddiSupportJar,
                "--source=" + cstApiPath,
                "--analysis-steps=prep",
        });
    }

    private static Path findMaddiSupportJar() throws IOException {
        Path libs = Path.of("../maddi-support/build/libs").toRealPath();
        try (Stream<Path> stream = Files.list(libs)) {
            return stream
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.startsWith("maddi-support-") && n.endsWith(".jar")
                               && !n.endsWith("-sources.jar") && !n.endsWith("-javadoc.jar");
                    })
                    .findFirst()
                    .orElseThrow(() -> new IOException("no maddi-support jar in " + libs
                                                       + "; run :maddi-support:jar first"));
        }
    }
}
