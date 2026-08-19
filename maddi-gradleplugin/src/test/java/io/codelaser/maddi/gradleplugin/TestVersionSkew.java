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

package io.codelaser.maddi.gradleplugin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One gate over everything on disk that NAMES a maddi version.
 * <p>
 * {@code gradle.properties} is the single source of truth, and the nine repos that consume maddi are wired by
 * {@code includeBuild} on relative paths, so they hold no version string at all. What is left is a short list of
 * DERIVED artefacts that embed the version in a filename or a coordinate, each reached through a pointer that does
 * not. Nothing compares them to the source of truth, so they rot silently and individually.
 * <p>
 * ⛔ <b>The failure mode is not a missing file, it is a WRONG ANSWER.</b> On 2026-08-18 the daemon distribution at
 * {@code maddi.daemon.install} still held 0.9.0 jars from a week earlier, with no {@code maddi-annotation} jar at
 * all, six days after the rename moved to 0.9.1. The pre-rename daemon has never heard of
 * {@code io.codelaser.maddi.annotation.Container}, so it analysed the fixture cleanly — every phase reported, zero
 * parse errors — and found nothing. The red that reached a human said "expected a contract-violation over the
 * wire", which is a sentence about the analyzer, and the analyzer was fine. A day went into that.
 * <p>
 * Each check below therefore states the version it EXPECTED and the version it FOUND, so the diagnosis is in the
 * failure message rather than three modules away.
 * <p>
 * ⚠ <b>{@code README.md} is deliberately NOT checked.</b> It names the latest coordinate a consumer can actually
 * resolve from Central, which legitimately lags the working version between releases — asserting equality there
 * would be wrong, and asserting nothing is a doc-drift risk this gate does not cover.
 */
public class TestVersionSkew {

    private static final String VERSION = required("maddi.projectVersion");
    private static final Path ROOT = Path.of(required("maddi.rootDir"));
    private static final Path DAEMON_INSTALL = Path.of(required("maddi.daemonInstall"));
    private static final Path PLUGIN_REPO = Path.of(required("e2immu.localPluginRepo"));

    private static String required(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("system property " + key + " must be set by the build; without it this"
                                            + " gate would pass by checking nothing");
        }
        return value;
    }

    @DisplayName("the version the build uses is the one gradle.properties declares")
    @Test
    public void theBuildAgreesWithTheSourceOfTruth() throws IOException {
        Path gradleProperties = ROOT.resolve("gradle.properties");
        assertTrue(Files.isRegularFile(gradleProperties), "expected the source of truth at " + gradleProperties);
        String declared = Files.readAllLines(gradleProperties).stream()
                .map(String::strip)
                .filter(l -> l.startsWith("version="))
                .map(l -> l.substring("version=".length()).strip())
                .findFirst().orElseThrow(() -> new AssertionError("no `version=` in " + gradleProperties));
        // Everything else here compares against the injected value, so if THAT has drifted from the file the whole
        // gate is measuring the wrong thing and reporting green.
        assertEquals(declared, VERSION, "the build is using a different version than " + gradleProperties
                                        + " declares; every other check in this class is then meaningless");
    }

    @DisplayName("every maddi jar in the daemon distribution is at the project version")
    @Test
    public void theDaemonDistributionCarriesTheProjectVersion() throws IOException {
        Path lib = DAEMON_INSTALL.resolve("lib");
        assertTrue(Files.isDirectory(lib), "expected the daemon distribution at " + lib
                                           + "; the test task must depend on :maddi-ide-daemon:installDist");
        List<String> ours;
        try (Stream<Path> jars = Files.list(lib)) {
            ours = jars.map(p -> p.getFileName().toString())
                    .filter(n -> n.startsWith("maddi-") && n.endsWith(".jar"))
                    .sorted().toList();
        }
        // Refuse the vacuous pass: an empty lib/ would satisfy "every jar matches" without checking anything.
        assertFalse(ours.isEmpty(), "no maddi-*.jar in " + lib + " at all — this gate checked nothing");

        String suffix = "-" + VERSION + ".jar";
        List<String> stale = ours.stream().filter(n -> !n.endsWith(suffix)).toList();
        assertTrue(stale.isEmpty(), "the daemon distribution is stale: expected every maddi jar at " + VERSION
                                    + ", found " + stale + " in " + lib
                                    + "\nA daemon built before a rename does not fail, it analyses cleanly and"
                                    + " reports nothing — see this class's javadoc.");
    }

    @DisplayName("the local plugin repository carries the project version, not only older ones")
    @Test
    public void theLocalPluginRepoPublishesTheProjectVersion() throws IOException {
        assertTrue(Files.isDirectory(PLUGIN_REPO), "expected the local plugin repository at " + PLUGIN_REPO
                + "; the test task must depend on publishAllPublicationsToLocalPluginRepoRepository");
        Path jar = PLUGIN_REPO.resolve("io/codelaser/maddi-gradleplugin/" + VERSION
                                       + "/maddi-gradleplugin-" + VERSION + ".jar");
        assertTrue(Files.isRegularFile(jar), "the local plugin repository has no artefact at the project version."
                + "\n  expected: " + jar
                + "\n  present:  " + publishedVersions()
                + "\nTestAnalyzerPluginShadedJarIsolation formats the project version into a generated build"
                + " script and resolves from here, so a skew fails there as an unresolvable plugin.");
    }

    /** What the repository actually holds, so the failure names the skew rather than only the absence. */
    private static List<String> publishedVersions() throws IOException {
        Path artifact = PLUGIN_REPO.resolve("io/codelaser/maddi-gradleplugin");
        if (!Files.isDirectory(artifact)) return List.of("<no io/codelaser/maddi-gradleplugin at all>");
        try (Stream<Path> versions = Files.list(artifact)) {
            return versions.filter(Files::isDirectory).map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    @DisplayName("the Maven export pom tracks the project version")
    @Test
    public void theMavenExportPomTracksTheProjectVersion() throws IOException {
        // Maven has no includeBuild, so this one MUST name a version -- which is exactly why it is the one that
        // goes stale unnoticed. It sat at 0.8.1-SNAPSHOT, two releases behind, while the rename runbook recorded
        // the item as done: the coordinates had been renamed, the version had not.
        Path pom = ROOT.resolve("testmvnplugin-export/pom.xml");
        assertTrue(Files.isRegularFile(pom), "expected the Maven export at " + pom);
        Matcher m = Pattern.compile("<maddi\\.version>([^<]*)</maddi\\.version>").matcher(Files.readString(pom));
        assertTrue(m.find(), "no <maddi.version> property in " + pom);
        assertEquals(VERSION, m.group(1).strip(), "the Maven export pom names a maddi version the project no"
                                                  + " longer builds: " + pom);
    }
}
