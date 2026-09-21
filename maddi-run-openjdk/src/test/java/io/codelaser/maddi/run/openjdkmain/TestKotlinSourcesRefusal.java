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

import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.run.config.Configuration;
import io.codelaser.maddi.run.config.GeneralConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The refusal has to fire <b>in the runner</b>, which is the thing the Gradle and Maven plugins fork. A helper
 * that returns the right answer while nobody calls it would leave the silent skip exactly where it was; this
 * is the other half of {@code TestRefuseKotlinSources}, which covers both arms of the helper itself.
 * <p>
 * ⚠ Only the refusing arm is run here, deliberately: the opt-out arm continues into a real parse, and what a
 * parse of a one-file tree then does is not this test's subject.
 */
public class TestKotlinSourcesRefusal {

    @Test
    public void theRunnerStopsOnAKotlinFile(@TempDir Path root) throws IOException {
        Path sources = root.resolve("src/main/java/a/b");
        Files.createDirectories(sources);
        Files.writeString(sources.resolve("Widget.java"), "package a.b; public class Widget {}");
        Files.writeString(sources.resolve("Gadget.kt"), "package a.b\nclass Gadget");

        Configuration configuration = new Configuration.Builder()
                .setGeneralConfiguration(new GeneralConfiguration.Builder().build())
                .setInputConfiguration(new InputConfigurationImpl.Builder()
                        .addSources(root.resolve("src/main/java").toString())
                        .build())
                .build();
        RunAnalyzer runAnalyzer = new RunAnalyzer(configuration);
        runAnalyzer.run();
        assertEquals(Main.EXIT_KOTLIN_SOURCES, runAnalyzer.exitValue(),
                "the runner must refuse, not parse the Java and report success");
    }

    /** ⛔ Two {@code Main} classes carry their own copy of the exit codes; a drift would misreport the refusal. */
    @Test
    public void bothMainsAgreeOnTheExitCode() {
        assertEquals(6, Main.EXIT_KOTLIN_SOURCES);
        assertEquals("Kotlin source files present, which this analyzer cannot read",
                Main.exitMessage(Main.EXIT_KOTLIN_SOURCES));
    }
}
