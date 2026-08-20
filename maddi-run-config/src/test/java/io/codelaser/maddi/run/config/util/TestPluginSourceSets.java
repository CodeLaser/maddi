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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

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
                StandardCharsets.UTF_8, false, null, 0, List.of());

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
                StandardCharsets.UTF_8, false, null, 0, List.of());

        assertNotNull(set);
        assertEquals(notCompiledYet.toUri(), set.uri(),
                "recording the source directory instead would make javac recompile these sources implicitly");
    }

    /** No class output to offer at all (a sibling publishing only its sources): the source directory it is. */
    @Test
    public void noClassOutputFallsBackToTheSourceDirectory(@TempDir Path dir) throws IOException {
        Path src = Files.createDirectories(dir.resolve("src"));

        SourceSet set = PluginSourceSets.sourceSet("p/main", null, List.of(src), null, null, false, null, 0, List.of());

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
                null, 0, List.of());
        assertNotNull(set);
        assertEquals(List.of(real), set.sourceDirectories());

        assertNull(PluginSourceSets.sourceSet("p/main", ":p", List.of(absent), null, null, false, null, 0, List.of()),
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

        SourceSet set = PluginSourceSets.sourceSet("m/main", ":m", List.of(modular), null, null, false, null, 0, List.of());
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
        SourceSet set = PluginSourceSets.sourceSet("p/main", ":p", List.of(src), null, null, false, null, 17, List.of());
        assertNotNull(set);
        assertEquals(17, set.sourceRelease());
        // 0 means "the build said nothing", which is not the same as "release 0"
        SourceSet silent = PluginSourceSets.sourceSet("p/test", ":p", List.of(src), null, null, true, null, 0, List.of());
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

    /**
     * ⛔⛔ {@code AUTOMATIC} AND {@code NONE} ARE NOT THE SAME ANSWER, AND ONLY ONE OF THEM WORKS. Gradle routes a
     * jar to the module path when it carries a descriptor OR declares {@code Automatic-Module-Name}, and leaves
     * every other jar on the class path -- which a named module cannot read. A {@code requires} on a
     * {@code NONE} artifact therefore fails however correct the descriptor is, and that is the whole of the
     * OpenSearch JPMS failure: the measurement was right and the build refused to provide what it named.
     */
    @Test
    public void moduleKindSeparatesTheThreeWaysAJarCanPresentItself(@TempDir Path dir) throws IOException {
        File explicit = jar(dir.resolve("explicit.jar"), null, "module-info.class");
        File multiRelease = jar(dir.resolve("mr.jar"), null, "META-INF/versions/9/module-info.class");
        File automatic = jar(dir.resolve("automatic.jar"), "Automatic-Module-Name: com.acme.lib\n", "com/acme/A.class");
        File osgiOnly = jar(dir.resolve("osgi.jar"), "Bundle-SymbolicName: org.jsr-305\n", "javax/annotation/A.class");
        File noManifest = jar(dir.resolve("bare.jar"), null, "p/A.class");

        assertEquals(PluginSourceSets.ModuleKind.EXPLICIT, PluginSourceSets.moduleKind(explicit));
        assertEquals(PluginSourceSets.ModuleKind.EXPLICIT, PluginSourceSets.moduleKind(multiRelease));
        assertEquals(PluginSourceSets.ModuleKind.AUTOMATIC, PluginSourceSets.moduleKind(automatic));
        // ⛔ THE jsr305 SHAPE. JPMS ignores the OSGi header, so this is NONE -- not AUTOMATIC.
        assertEquals(PluginSourceSets.ModuleKind.NONE, PluginSourceSets.moduleKind(osgiOnly));
        assertEquals(PluginSourceSets.ModuleKind.NONE, PluginSourceSets.moduleKind(noManifest));
    }

    /**
     * ⚠ A directory is a sibling project's compile output. {@code Automatic-Module-Name} is a property of a jar
     * MANIFEST, and no build tool derives one from a class-output directory, so a directory is EXPLICIT or it is
     * NONE -- never AUTOMATIC.
     */
    @Test
    public void aDirectoryIsExplicitOrNothing(@TempDir Path dir) throws IOException {
        Path modular = Files.createDirectories(dir.resolve("modular"));
        Files.write(modular.resolve("module-info.class"), new byte[]{(byte) 0xCA, (byte) 0xFE});
        Path plain = Files.createDirectories(dir.resolve("plain"));

        assertEquals(PluginSourceSets.ModuleKind.EXPLICIT, PluginSourceSets.moduleKind(modular.toFile()));
        assertEquals(PluginSourceSets.ModuleKind.NONE, PluginSourceSets.moduleKind(plain.toFile()));
    }

    /** ⚠ CONTROL: something that is not a jar at all is NONE, and says so rather than throwing. */
    @Test
    public void whatCannotBeReadIsNotAModule(@TempDir Path dir) throws IOException {
        Path notAJar = dir.resolve("notes.txt");
        Files.writeString(notAJar, "this is not a jar");
        assertEquals(PluginSourceSets.ModuleKind.NONE, PluginSourceSets.moduleKind(notAJar.toFile()));
        assertEquals(PluginSourceSets.ModuleKind.NONE, PluginSourceSets.moduleKind(dir.resolve("absent.jar").toFile()));
    }

    /**
     * ⚠ THE BEHAVIOUR THAT MUST NOT HAVE MOVED. {@code isModularArtifact} feeds {@code SourceSet.isModule()} and
     * through it {@code JavaInspectorImpl}'s choice between javac's class path and module path. Widening it to
     * include AUTOMATIC is a change to which artifacts land where, judged by a corpus run and not by this file;
     * until then it stays EXPLICIT alone, and this test is what says so.
     */
    @Test
    public void isModularArtifactStillMeansExplicitAlone(@TempDir Path dir) throws IOException {
        File explicit = jar(dir.resolve("explicit.jar"), null, "module-info.class");
        File automatic = jar(dir.resolve("automatic.jar"), "Automatic-Module-Name: com.acme.lib\n", "com/acme/A.class");
        File osgiOnly = jar(dir.resolve("osgi.jar"), "Bundle-SymbolicName: org.jsr-305\n", "javax/annotation/A.class");

        assertTrue(PluginSourceSets.isModularArtifact(explicit));
        assertFalse(PluginSourceSets.isModularArtifact(automatic));
        assertFalse(PluginSourceSets.isModularArtifact(osgiOnly));
    }

    /**
     * A jar with the given manifest body (headers only, {@code Manifest-Version} is added here) and the given
     * entries, each empty. Only the presence of an entry and the manifest headers are ever read.
     */
    private static File jar(Path path, String manifestBody, String... entries) throws IOException {
        Manifest manifest = null;
        if (manifestBody != null) {
            manifest = new Manifest(new ByteArrayInputStream(
                    ("Manifest-Version: 1.0\n" + manifestBody + "\n").getBytes(StandardCharsets.UTF_8)));
        }
        try (OutputStream out = Files.newOutputStream(path);
             JarOutputStream jar = manifest == null ? new JarOutputStream(out) : new JarOutputStream(out, manifest)) {
            for (String entry : entries) {
                jar.putNextEntry(new JarEntry(entry));
                jar.closeEntry();
            }
        }
        return path.toFile();
    }

    /**
     * ⛔⛔ AN INCUBATOR MODULE IS NOT IN THE {@code java.se} CLOSURE AND WIDENING {@code jmods} DOES NOT REACH
     * IT: it must be in the ROOT SET of the compilation that uses it.
     *
     * <p>⚠ MEASURED on trino (2026-08-19): {@code core/trino-main} passes
     * {@code --add-modules=jdk.incubator.vector}, {@code --compile-log} records it on 6 of trino's 209 source
     * sets because it reads javac's own line, and NEITHER build plugin set it at all — so the same module parsed
     * with one error the log route does not have: "package jdk.incubator.vector is not visible".
     */
    @Test
    public void addModulesIsReadInBothSpellings() {
        assertEquals(List.of("jdk.incubator.vector"),
                PluginSourceSets.addModulesFrom(List.of("-g", "--add-modules=jdk.incubator.vector")),
                "the = form, which is how trino writes it into a property");
        assertEquals(List.of("jdk.incubator.vector"),
                PluginSourceSets.addModulesFrom(List.of("--add-modules", "jdk.incubator.vector")),
                "the two-argument form, which is how options.compilerArgs is usually written");
        assertEquals(List.of("jdk.incubator.vector", "jdk.unsupported"),
                PluginSourceSets.addModulesFrom(List.of("--add-modules=jdk.incubator.vector,jdk.unsupported")),
                "comma-separated, as javac accepts");
        assertEquals(List.of("a", "b"),
                PluginSourceSets.addModulesFrom(List.of("--add-modules=a", "--add-modules", "b", "--add-modules=a")),
                "accumulated across occurrences, in order, without duplicates");
    }

    /**
     * ⚠ {@code ALL-MODULE-PATH} and {@code ALL-DEFAULT} are javac's own pseudo-names, not modules. Passing one
     * on would send the parse looking for a jmod that does not exist, which is a worse answer than dropping it.
     */
    @Test
    public void addModulesDropsJavacsPseudoNames() {
        assertEquals(List.of("jdk.incubator.vector"),
                PluginSourceSets.addModulesFrom(List.of("--add-modules=ALL-MODULE-PATH,jdk.incubator.vector")));
        assertEquals(List.of(), PluginSourceSets.addModulesFrom(List.of("--add-modules=ALL-DEFAULT")));
    }

    /** ⚠ CONTROL: a build that says nothing yields nothing, not a spurious module. */
    @Test
    public void addModulesIsEmptyWhenNothingSaysSo() {
        assertEquals(List.of(), PluginSourceSets.addModulesFrom(List.of("-g", "-parameters")));
        assertEquals(List.of(), PluginSourceSets.addModulesFrom(null));
        assertEquals(List.of(), PluginSourceSets.addModulesFrom(List.of("--add-modules")),
                "a trailing flag with no value names nothing");
    }
}
