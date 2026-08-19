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

package io.codelaser.maddi.run.config.util;

import io.codelaser.maddi.cst.api.element.SourceSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The construction both build plugins share. The assertions here are the ones that used to hold in neither
 * plugin, or in only one of them.
 */
public class TestPluginSourceSets {

    /**
     * ⛔ THE DEFECT THIS CLASS EXISTS FOR. Both plugins pointed {@code uri} at the first SOURCE directory; it is
     * what javac resolves a dependent source set's references against, so it has to be the class output.
     */
    @Test
    public void uriIsTheClassOutput(@TempDir Path dir) throws IOException {
        Path src = Files.createDirectories(dir.resolve("src/main/java"));
        Path classes = Files.createDirectories(dir.resolve("build/classes/java/main"));

        SourceSet set = PluginSourceSets.sourceSet("p/main", ":p", List.of(src), classes,
                StandardCharsets.UTF_8, false, null, 0);

        assertNotNull(set);
        assertEquals(classes.toUri(), set.uri(), "the uri must be the class output, not the source directory");
        assertEquals(List.of(src), set.sourceDirectories(), "the sources stay in sourceDirectories");
        assertEquals(":p", set.buildUnit());
        assertFalse(set.test());
    }

    /**
     * ⛔ THE CLASS OUTPUT IS A DECLARATION, NOT AN OBSERVATION. Both plugins compute this configuration before the
     * compile tasks they depend on have run, so the directory is routinely absent at this moment and must still be
     * recorded. Probing it here cost a configuration-cache hit (see {@link PluginSourceSets} and
     * {@code TestAnalyzerPluginFunctional#configurationCacheCompatible}); whether it exists is parse-time's
     * question, and {@code JavaInspectorImpl#validateOneDependency} asks it there.
     */
    @Test
    public void anAbsentClassOutputIsStillTheClassOutput(@TempDir Path dir) throws IOException {
        Path src = Files.createDirectories(dir.resolve("src/main/java"));
        Path notCompiledYet = dir.resolve("build/classes/java/main");
        assertFalse(Files.exists(notCompiledYet));

        SourceSet set = PluginSourceSets.sourceSet("p/main", ":p", List.of(src), notCompiledYet,
                StandardCharsets.UTF_8, false, null, 0);

        assertNotNull(set);
        assertEquals(notCompiledYet.toUri(), set.uri(),
                "recording the source directory instead would make javac recompile these sources implicitly");
    }

    /** No class output to offer at all (a sibling publishing only its sources): the source directory it is. */
    @Test
    public void noClassOutputFallsBackToTheSourceDirectory(@TempDir Path dir) throws IOException {
        Path src = Files.createDirectories(dir.resolve("src"));

        SourceSet set = PluginSourceSets.sourceSet("p/main", null, List.of(src), null, null, false, null, 0);

        assertNotNull(set);
        assertEquals(src.toUri(), set.uri());
    }

    /**
     * A declared-but-absent source directory is dropped, and a set with nothing left is not a set. Maven's compile
     * source roots are declared rather than present (guava's root pom declares a {@code test/} that most modules
     * do not have), and a Gradle source set can name a directory that was never created.
     */
    @Test
    public void absentSourceDirectoriesAreDropped(@TempDir Path dir) throws IOException {
        Path real = Files.createDirectories(dir.resolve("src/main/java"));
        Path absent = dir.resolve("src/main/generated");

        SourceSet set = PluginSourceSets.sourceSet("p/main", ":p", List.of(real, absent), null, null, false,
                null, 0);
        assertNotNull(set);
        assertEquals(List.of(real), set.sourceDirectories());

        assertNull(PluginSourceSets.sourceSet("p/main", ":p", List.of(absent), null, null, false, null, 0),
                "a source set over no existing directory must not be created at all");
    }

    /** ⚠ The Maven plugin never asked this question; it is the shared construction that gives it the answer. */
    @Test
    public void modularSourceIsFlagged(@TempDir Path dir) throws IOException {
        Path plain = Files.createDirectories(dir.resolve("plain"));
        Path modular = Files.createDirectories(dir.resolve("modular"));
        Files.writeString(modular.resolve("module-info.java"), "module m {}");

        assertFalse(PluginSourceSets.isModularSource(List.of(plain)));
        assertTrue(PluginSourceSets.isModularSource(List.of(plain, modular)));

        SourceSet set = PluginSourceSets.sourceSet("m/main", ":m", List.of(modular), null, null, false, null, 0);
        assertNotNull(set);
        assertTrue(set.isModule());
    }


    /**
     * ⛔ THE PARSE OTHERWISE RUNS ON WHATEVER JDK MADDI IS, NOT THE ONE THE CORPUS TARGETS. Measured on pulsar:
     * the corpus states release 17, the plugin recorded nothing, and {@code Thread.suspend()} -- gone from
     * JDK 26 -- stopped resolving in {@code ZooKeeperUtil}.
     */
    @Test
    public void sourceReleaseIsRecordedPerSet(@TempDir Path dir) throws IOException {
        Path src = Files.createDirectories(dir.resolve("src"));
        SourceSet set = PluginSourceSets.sourceSet("p/main", ":p", List.of(src), null, null, false, null, 17);
        assertNotNull(set);
        assertEquals(17, set.sourceRelease());
        // 0 means "the build said nothing", which is not the same as "release 0"
        SourceSet silent = PluginSourceSets.sourceSet("p/test", ":p", List.of(src), null, null, true, null, 0);
        assertNotNull(silent);
        assertEquals(0, silent.sourceRelease());
    }

    /** Both build tools spell the level in more than one way, and neither guarantees one is set at all. */
    @Test
    public void releaseStringsAreParsedInEverySpelling() {
        assertEquals(21, PluginSourceSets.parseRelease("21"));
        assertEquals(8, PluginSourceSets.parseRelease("1.8"));
        assertEquals(8, PluginSourceSets.parseRelease("8"));
        assertEquals(25, PluginSourceSets.parseRelease(" 25 "));
        // "says nothing" and "cannot be a version" both mean 0, never a wrong number
        assertEquals(0, PluginSourceSets.parseRelease(null));
        assertEquals(0, PluginSourceSets.parseRelease(""));
        assertEquals(0, PluginSourceSets.parseRelease("VERSION_21"));
        assertEquals(0, PluginSourceSets.parseRelease("0"));
    }
}
