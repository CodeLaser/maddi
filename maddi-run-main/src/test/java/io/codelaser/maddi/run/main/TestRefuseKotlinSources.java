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

package io.codelaser.maddi.run.main;

import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.DetectKotlinSources;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.run.config.GeneralConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Java entry points must REFUSE a project that holds Kotlin rather than skipping the {@code .kt} files and
 * reporting success over a partly-read tree.
 * <p>
 * ⭐ The negative control is the test that matters. "No Kotlin found" is also what a detector that resolved no
 * directories at all returns, so {@link #javaOnlyTreeIsNotRefused()} asserts {@code directoriesScanned} as well
 * as the verdict: without it, breaking the path resolution would turn every assertion here green.
 */
public class TestRefuseKotlinSources {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestRefuseKotlinSources.class);

    private static InputConfiguration configurationOver(Path sourceDirectory) {
        return new InputConfigurationImpl.Builder()
                .addSources(sourceDirectory.toString())
                .build();
    }

    private static Path javaTree(Path root) throws IOException {
        Path packageDir = root.resolve("src/main/java/a/b");
        Files.createDirectories(packageDir);
        Files.writeString(packageDir.resolve("Widget.java"), "package a.b; public class Widget {}");
        return root.resolve("src/main/java");
    }

    @Test
    public void aKotlinFileBesideTheJavaIsFound(@TempDir Path root) throws IOException {
        Path sources = javaTree(root);
        // the case that motivated the check: a .kt under src/main/java, which no source-set NAME mentions
        Files.writeString(sources.resolve("a/b/Gadget.kt"), "package a.b\nclass Gadget");

        DetectKotlinSources detected = DetectKotlinSources.in(configurationOver(sources));
        assertTrue(detected.found());
        assertEquals(1, detected.fileCount());
        assertEquals(1, detected.directoriesScanned());
        assertEquals(1, detected.samples().size());
        assertTrue(detected.samples().getFirst().toString().endsWith("Gadget.kt"), "got " + detected.samples());

        String message = detected.message(DetectKotlinSources.SKIP_OPTION);
        assertTrue(message.contains("Gadget.kt"), message);
        assertTrue(message.contains(DetectKotlinSources.SKIP_OPTION), message);
    }

    @Test
    public void javaOnlyTreeIsNotRefused(@TempDir Path root) throws IOException {
        Path sources = javaTree(root);
        DetectKotlinSources detected = DetectKotlinSources.in(configurationOver(sources));
        assertFalse(detected.found());
        assertEquals(0, detected.fileCount());
        // ⭐ non-vacuous: the walk really did visit the directory, so "no Kotlin" is a reading and not a silence
        assertEquals(1, detected.directoriesScanned());
        assertFalse(DetectKotlinSources.refuse(configurationOver(sources), false,
                DetectKotlinSources.SKIP_OPTION, LOGGER));
    }

    @Test
    public void refuseStopsTheRunUnlessTheOptionIsGiven(@TempDir Path root) throws IOException {
        Path sources = javaTree(root);
        Files.writeString(sources.resolve("a/b/Gadget.kt"), "package a.b\nclass Gadget");

        assertTrue(DetectKotlinSources.refuse(configurationOver(sources), false,
                DetectKotlinSources.SKIP_OPTION, LOGGER));
        // with the opt-out the run continues -- and says the analysis is incomplete
        assertFalse(DetectKotlinSources.refuse(configurationOver(sources), true,
                DetectKotlinSources.SKIP_OPTION, LOGGER));
    }

    @Test
    public void aKotlinScriptCounts(@TempDir Path root) throws IOException {
        Path sources = javaTree(root);
        Files.writeString(sources.resolve("build.gradle.kts"), "// not analyzed either");
        assertTrue(DetectKotlinSources.in(configurationOver(sources)).found());
    }

    /**
     * ⛔ The plugins do not call the CLI: they hand {@link Main} a key/value map. A flag that travels on one
     * path and not the other is exactly the drift {@code PluginOptions}' header was written about, so the
     * map route is asserted separately from {@code --skip-kotlin-sources}.
     */
    @Test
    public void theOptionTravelsThroughThePluginMap() {
        Map<String, String> map = PluginOptions.generalConfigMap(false, null, new File("build/maddi"),
                true, null, null, false, false, true);
        GeneralConfiguration general = Main.generalConfiguration(map);
        assertTrue(general.skipKotlinSources());

        Map<String, String> off = PluginOptions.generalConfigMap(false, null, new File("build/maddi"),
                true, null, null, false, false, false);
        assertFalse(Main.generalConfiguration(off).skipKotlinSources());
    }

    /** The exit code is part of the contract with the build plugins, which report {@code exitMessage}. */
    @Test
    public void theExitCodeHasAMessage() {
        assertEquals(6, Main.EXIT_KOTLIN_SOURCES);
        assertTrue(Main.exitMessage(Main.EXIT_KOTLIN_SOURCES).contains("Kotlin"));
    }
}
