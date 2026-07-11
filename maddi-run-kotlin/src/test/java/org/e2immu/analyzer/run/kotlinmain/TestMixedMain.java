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

package org.e2immu.analyzer.run.kotlinmain;

import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Drives the {@link Main} CLI through {@code --input-configuration} on a tiny mixed Java+Kotlin project written to
 * disk: it must derive an {@link InputConfiguration}, run the prep-only mixed analysis and exit cleanly.
 */
public class TestMixedMain {

    @Test
    public void runsMixedPrepFromAnInputConfiguration(@TempDir Path tmp) throws Exception {
        Path kDir = tmp.resolve("src/main/kotlin");
        Path jDir = tmp.resolve("src/main/java");
        Files.createDirectories(kDir.resolve("a"));
        Files.createDirectories(jDir.resolve("b"));
        Files.writeString(kDir.resolve("a/Foo.kt"), "package a\nclass Foo(val id: Int)\n");
        Files.writeString(jDir.resolve("b/UseFoo.java"),
                "package b;\npublic class UseFoo {\n    public a.Foo foo;\n}\n");

        SourceSet kotlinSet = new SourceSetImpl.Builder().setName("kotlin/main")
                .setSourceDirectories(List.of(kDir)).setUri(kDir.toUri()).build();
        SourceSet javaSet = new SourceSetImpl.Builder().setName("java/main")
                .setSourceDirectories(List.of(jDir)).setUri(jDir.toUri())
                .setDependencies(List.of(kotlinSet)).build();
        InputConfiguration config = new InputConfigurationImpl.Builder()
                .addSourceSets(kotlinSet).addSourceSets(javaSet).build();

        File configFile = tmp.resolve("input-configuration.json").toFile();
        JsonStreaming.objectMapper().writerFor(InputConfigurationImpl.class).writeValue(configFile, config);

        int exit = Main.execute(new String[]{Main.INPUT_CONFIGURATION, configFile.getAbsolutePath()});

        assertEquals(Main.EXIT_OK, exit);
    }
}
