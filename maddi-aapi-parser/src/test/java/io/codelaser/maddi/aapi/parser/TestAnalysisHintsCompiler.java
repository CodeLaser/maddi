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

package io.codelaser.maddi.aapi.parser;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Fails when the committed analysis-result files in {@code maddi-aapi-archive} are no longer what the hints
 * compile to — i.e. when someone edited a hand-written shadow under {@code maddi-aapi-archive/src/main/java},
 * or changed the compiler, without regenerating.
 * <p>
 * It compiles into a temporary directory and compares; it never writes into the working tree. Until 2026-09-20
 * this test simply called {@code CompileAnalysisHints.compileAll()}, which writes straight into
 * {@code src/main/resources}: every {@code gradle test} rewrote 33 tracked files, and whenever the output
 * genuinely changed it left the repository dirty for whoever ran next to discover, commit by accident, or trip
 * over. Regenerating is a build action — {@code gradle :maddi-aapi-parser:compileAnalysisHints} — and this is
 * the test that makes forgetting it visible.
 * <p>
 * It checks the jars too, which is not redundant: {@code compileAll()} and {@code packageJars()} are separate
 * steps and only {@code main} runs both, so the committed {@code openjdk.jar}/{@code libs.jar} can drift from the
 * committed JSON beside them. That has happened and cost real verdicts — see the ⛔ note in
 * {@link CompileAnalysisHints#packageJars()}, where a regeneration silently added a stale
 * {@code libs/support} entry and {@code maddi-ide-daemon} started reading obsolete contracts for
 * {@code io.codelaser.maddi.support.*}. The jars are what the daemon and the runners actually load; the JSON is
 * only what we review.
 * <p>
 * Jars are compared entry by entry rather than byte by byte: the entry set and each entry's content are the
 * properties that matter to a reader, and they do not depend on the deflate level of the JDK that packed them.
 * <p>
 * Costs one compile of all four libraries, ~2.5 s.
 */
public class TestAnalysisHintsCompiler extends CommonTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestAnalysisHintsCompiler.class);

    private static final String REGENERATE = "gradle :maddi-aapi-parser:compileAnalysisHints";

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("io.codelaser.maddi.aapi")).setLevel(Level.DEBUG);
    }

    @Test
    public void test(@TempDir Path tempDirectory) throws IOException {
        CompileAnalysisHints.compileAll(tempDirectory);
        CompileAnalysisHints.packageJars(tempDirectory);

        List<String> complaints = new ArrayList<>();
        for (String library : CompileAnalysisHints.LIBRARIES) {
            compareJson(CompileAnalysisHints.RESULTS_BASE_DIR.resolve(library), tempDirectory.resolve(library),
                    library, complaints);
        }
        // the two archives packageJars writes, packed here from the freshly compiled JSON
        for (String jar : List.of("openjdk.jar", "libs.jar")) {
            compareJar(CompileAnalysisHints.RESULTS_BASE_DIR.resolve(jar), tempDirectory.resolve(jar), complaints);
        }
        if (!complaints.isEmpty()) {
            fail("""
                    The committed analysis results in maddi-aapi-archive are stale:
                      %s
                    Regenerate them (this writes into the working tree; commit the diff):
                      %s
                    """.formatted(String.join("\n  ", complaints), REGENERATE));
        }
        LOGGER.info("All {} libraries and both archive jars are up to date", CompileAnalysisHints.LIBRARIES.size());
    }

    private void compareJson(Path committedDirectory, Path regeneratedDirectory, String library,
                             List<String> complaints) throws IOException {
        TreeSet<String> committed = jsonNames(committedDirectory);
        TreeSet<String> regenerated = jsonNames(regeneratedDirectory);
        for (String name : regenerated) {
            if (!committed.contains(name)) complaints.add(library + ": not committed: " + name);
        }
        for (String name : committed) {
            if (!regenerated.contains(name)) complaints.add(library + ": no longer generated: " + name);
        }
        for (String name : regenerated) {
            if (!committed.contains(name)) continue;
            byte[] a = Files.readAllBytes(committedDirectory.resolve(name));
            byte[] b = Files.readAllBytes(regeneratedDirectory.resolve(name));
            if (!Arrays.equals(a, b)) {
                complaints.add(library + ": out of date: " + name
                               + " (committed " + a.length + " bytes, regenerated " + b.length + " bytes)");
            }
        }
    }

    /**
     * Entry set + entry content. A missing or stale entry here is what the loaders see, whatever the JSON on
     * disk says.
     */
    private void compareJar(Path committedJar, Path regeneratedJar, List<String> complaints) throws IOException {
        String name = committedJar.getFileName().toString();
        if (!Files.isRegularFile(committedJar)) {
            complaints.add("missing archive: " + name);
            return;
        }
        Map<String, byte[]> committed = jsonEntries(committedJar);
        Map<String, byte[]> regenerated = jsonEntries(regeneratedJar);
        for (String entry : regenerated.keySet()) {
            if (!committed.containsKey(entry)) complaints.add(name + ": entry missing from the jar: " + entry);
        }
        for (String entry : committed.keySet()) {
            if (!regenerated.containsKey(entry)) complaints.add(name + ": stale entry in the jar: " + entry);
        }
        for (Map.Entry<String, byte[]> entry : regenerated.entrySet()) {
            byte[] c = committed.get(entry.getKey());
            if (c != null && !Arrays.equals(c, entry.getValue())) {
                complaints.add(name + ": entry out of date: " + entry.getKey()
                               + " (jar " + c.length + " bytes, regenerated " + entry.getValue().length + " bytes)");
            }
        }
    }

    private static TreeSet<String> jsonNames(Path directory) throws IOException {
        TreeSet<String> names = new TreeSet<>();
        if (!Files.isDirectory(directory)) return names;
        try (var jsons = Files.newDirectoryStream(directory, "*.json")) {
            for (Path json : jsons) names.add(json.getFileName().toString());
        }
        return names;
    }

    private static Map<String, byte[]> jsonEntries(Path jarFile) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        if (!Files.isRegularFile(jarFile)) return entries;
        try (JarFile jar = new JarFile(jarFile.toFile())) {
            for (JarEntry entry : jar.stream().filter(e -> e.getName().endsWith(".json")).toList()) {
                try (var in = jar.getInputStream(entry)) {
                    entries.put(entry.getName(), in.readAllBytes());
                }
            }
        }
        return entries;
    }
}
