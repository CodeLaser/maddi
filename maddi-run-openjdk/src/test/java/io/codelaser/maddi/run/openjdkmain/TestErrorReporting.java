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

package io.codelaser.maddi.run.openjdkmain;

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
