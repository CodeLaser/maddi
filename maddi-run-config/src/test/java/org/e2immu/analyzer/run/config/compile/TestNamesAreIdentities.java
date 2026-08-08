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

package org.e2immu.analyzer.run.config.compile;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A {@code SourceSet}'s NAME is its identity — its own contract says so: <i>"source sets are identified by their
 * name() throughout the system, including in serialized dependency references"</i>, and {@code buildUnit()} is
 * deliberately kept out of {@code equals}. So a serialized {@code dependencies: ["core/main"]} has exactly one
 * reading, and two entries answering to one name make it a coin toss.
 * <p>
 * ⛔ THE SYMPTOM IS NOT A DUPLICATE-NAME ERROR, WHICH IS WHY THIS IS WORTH A CHECK RATHER THAN A CONVENTION. In
 * the Elasticsearch parse configuration a library once took a declared source set's name, and what came out was a
 * <b>phantom dependency cycle</b>: a source set that appeared to depend on itself. The diagnosis was the expensive
 * part, and nothing along the way mentioned a name.
 * <p>
 * ⚠ MEASURED BEFORE BEING WRITTEN, and it does not currently fire: today's Elasticsearch configuration has 348
 * source sets and 785 class-path parts, with <b>0</b> names shared between the two groups and 0 duplicates within
 * either. This is prevention with a denominator attached, not a repair — the check is one pass over ~1,100 strings.
 */
public class TestNamesAreIdentities {

    private static SourceSet sourceSet(String name, Path dir) {
        return new SourceSetImpl.Builder()
                .setName(name)
                .setSourceDirectories(List.of(dir))
                .setUri(dir.toUri())
                .setDependencies(List.of())
                .build();
    }

    private static SourceSet library(String name, String path) {
        return new SourceSetImpl.Builder()
                .setName(name)
                .setSourceDirectories(List.of())
                .setUri(URI.create("file:" + path))
                .setLibrary(true)
                .setExternalLibrary(true)
                .build();
    }

    private static CompileListToSourceSets.Result result(List<SourceSet> sourceSets, List<SourceSet> jars) {
        return new CompileListToSourceSets.Result(
                sourceSets.stream().map(ss -> new CompileListToSourceSets.JSourceSet(null, ss)).toList(), jars,
                null);
    }

    /** ⚠ CONTROL FIRST: an ordinary configuration must build, or the check below proves nothing. */
    @DisplayName("CONTROL: distinct names build without complaint")
    @Test
    public void distinctNamesAreFine() {
        InputConfiguration ic = CompileListToInputConfiguration.build(
                result(List.of(sourceSet("core/main", Path.of("/p/core/src/main/java")),
                                sourceSet("core/test", Path.of("/p/core/src/test/java"))),
                        List.of(library("guava-33.jar", "/m2/guava-33.jar"))),
                List.of());

        assertEquals(2, ic.sourceSets().size());
        assertTrue(ic.classPathParts().stream().anyMatch(p -> "guava-33.jar".equals(p.name())));
    }

    /** ⛔⛔ THE HEADLINE, and the shape the Elasticsearch config actually had: a LIBRARY wearing a source set's name. */
    @DisplayName("a library carrying a declared source set's name is refused")
    @Test
    public void libraryTakingASourceSetName() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                CompileListToInputConfiguration.build(
                        result(List.of(sourceSet("core/main", Path.of("/p/core/src/main/java"))),
                                List.of(library("core/main", "/somewhere/else/classes"))),
                        List.of()));

        assertTrue(e.getMessage().contains("core/main"), e.getMessage());
        // the message must say what the symptom will look like, because that is the part that cost the time
        assertTrue(e.getMessage().contains("dependency cycle"), e.getMessage());
    }

    /** Two distinct jars whose file names coincide: the same ambiguity from the other direction. */
    @DisplayName("two libraries with one name are refused")
    @Test
    public void twoLibrariesOneName() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                CompileListToInputConfiguration.build(
                        result(List.of(sourceSet("core/main", Path.of("/p/core/src/main/java"))),
                                List.of(library("annotations.jar", "/a/annotations.jar"),
                                        library("annotations.jar", "/b/annotations.jar"))),
                        List.of()));

        assertTrue(e.getMessage().contains("annotations.jar"), e.getMessage());
    }

    /**
     * ⚠ THE CONTROL THAT KEEPS THE CHECK HONEST ABOUT ITS SCOPE: the JDK modules are in the same namespace, so a
     * source set named after one is ambiguous too. Without this, the check could have been written over the source
     * sets and jars only and would have looked complete.
     */
    @DisplayName("CONTROL: a source set colliding with a JDK MODULE name is refused too")
    @Test
    public void sourceSetTakingAJmodName() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () ->
                CompileListToInputConfiguration.build(
                        result(List.of(sourceSet("java.base", Path.of("/p/core/src/main/java"))), List.of()),
                        List.of()));

        assertTrue(e.getMessage().contains("java.base"), e.getMessage());
    }
}
