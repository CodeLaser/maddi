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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives the {@code --compile-log} CLI path (the third way to obtain an input configuration) end to end on a
 * captured Maven build log, and verifies {@code --write-input-configuration} produces a round-trippable JSON.
 */
public class TestCompileLogCli {

    @Test
    public void deriveInputConfigurationFromCompileLog(@TempDir Path tempDir) throws Exception {
        File out = tempDir.resolve("timefold-input.json").toFile();

        int exit = Main.execute(new String[]{
                "--" + Main.COMPILE_LOG, "src/test/resources/javac/mvnTimefold-solver.txt.gz",
                "--" + Main.EXTRA_JMOD, "java.sql",
                "--" + Main.WRITE_INPUT_CONFIGURATION, out.getAbsolutePath()
        });

        assertEquals(Main.EXIT_OK, exit);
        assertTrue(out.isFile(), "expected the derived input configuration to be written");

        ObjectMapper objectMapper = JsonStreaming.objectMapper();
        InputConfiguration inputConfiguration = objectMapper.readValue(out, InputConfigurationImpl.class);
        assertFalse(inputConfiguration.sourceSets().isEmpty(), "expected non-empty source sets");
        assertFalse(inputConfiguration.classPathParts().isEmpty(), "expected non-empty classpath parts");
    }
}
