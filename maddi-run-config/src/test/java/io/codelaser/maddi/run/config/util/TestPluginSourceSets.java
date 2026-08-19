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
                StandardCharsets.UTF_8, false, null);

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
                StandardCharsets.UTF_8, false, null);

        assertNotNull(set);
        assertEquals(notCompiledYet.toUri(), set.uri(),
                "recording the source directory instead would make javac recompile these sources implicitly");
    }

    /** No class output to offer at all (a sibling publishing only its sources): the source directory it is. */
    @Test
    public void noClassOutputFallsBackToTheSourceDirectory(@TempDir Path dir) throws IOException {
        Path src = Files.createDirectories(dir.resolve("src"));

        SourceSet set = PluginSourceSets.sourceSet("p/main", null, List.of(src), null, null, false, null);

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
                null);
        assertNotNull(set);
        assertEquals(List.of(real), set.sourceDirectories());

        assertNull(PluginSourceSets.sourceSet("p/main", ":p", List.of(absent), null, null, false, null),
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

        SourceSet set = PluginSourceSets.sourceSet("m/main", ":m", List.of(modular), null, null, false, null);
        assertNotNull(set);
        assertTrue(set.isModule());
    }

}
