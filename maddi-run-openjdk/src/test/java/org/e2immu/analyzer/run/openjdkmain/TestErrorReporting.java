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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Guards the error-reporting fix: a source file that fails to parse must be surfaced (openjdk diagnostics now flow
 * into the {@code Summary}) and the process must exit with the parser error code — <em>not</em> the previous
 * silent exit 0.
 */
public class TestErrorReporting {

    @Test
    public void parseErrorExitsWithParserCodeNotSilentZero(@TempDir Path tmp) throws Exception {
        Path src = tmp.resolve("src");
        Files.createDirectories(src.resolve("x"));
        Files.writeString(src.resolve("x/Broken.java"), """
                package x;
                public class Broken {
                    @@@ this is not valid java @@@
                }
                """);

        int exit = Main.execute(new String[]{
                "--" + Main.SOURCE, src.toString(),
                "--" + Main.JMOD, "java.base",
                "--" + Main.ANALYSIS_STEPS, Main.AS_PREP});

        assertNotEquals(Main.EXIT_OK, exit, "a parse error must not report success");
        assertEquals(Main.EXIT_PARSER_ERROR, exit, "a parse error must exit with the parser error code");
    }
}
