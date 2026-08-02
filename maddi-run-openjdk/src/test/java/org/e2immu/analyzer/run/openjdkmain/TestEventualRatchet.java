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

import ch.qos.logback.classic.Level;
import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.impl.EventualCluster;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.parser.Summary;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.openjdk.JavaInspectorImpl;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The eventual-immutability ratchet: the composed dogfood run (maddi analysing its own CST) must keep
 * certifying exactly the types listed in {@code dogfood/expected-eventual-survivors.txt}.
 * <p>
 * This exists because every regression in the 2026-07/08 certification arc was found by dogfood
 * archaeology days after the commit that caused it — {@code ProvidesImpl.addImplementationResolved}
 * turned a resolve-once field into a mutable {@code ArrayList} and sank the whole {@code Element}
 * hierarchy for a week without a single red test. See {@code docs/eventual-design-improvements.md} §1.
 * <p>
 * The run is built here rather than through {@link RunAnalyzer} so that both gates are set
 * programmatically: {@code MODREACH} and {@code EVENTUALCLUSTER} are environment opt-outs on the CLI
 * path, and a developer with either exported to {@code 0} would otherwise measure a different engine
 * and read the difference as a regression.
 */
@Tag("slow")
public class TestEventualRatchet {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestEventualRatchet.class);

    private static final Path DOGFOOD = Path.of("../dogfood");
    private static final Path INPUT_CONFIGURATION = DOGFOOD.resolve("cst-impl/build/inputConfiguration.json");
    private static final Path EXPECTED = DOGFOOD.resolve("expected-eventual-survivors.txt");
    private static final Path WOBBLE = DOGFOOD.resolve("eventual-survivor-wobble.txt");
    private static final Path ACTUAL = Path.of("build/eventual-survivors-actual.txt");

    /**
     * Without these three the annotated-API results are not loaded, {@code List.copyOf} has no
     * {@code immutableMethod}, and the survivor count reads far too low — the trap documented in
     * {@code dogfood/README.md}. A missing directory is a hard failure, not a warning: the whole point
     * of the ratchet is that it cannot pass vacuously.
     */
    private static final String AAPI = "../maddi-aapi-archive/src/main/resources/org/e2immu/analyzer/aapi/archive/analyzedPackageFiles/";
    private static final List<String> PRELOAD = List.of(AAPI + "jdk", AAPI + "libs/test", AAPI + "libs/log");

    @BeforeAll
    public static void beforeAll() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    }

    @Test
    public void test() throws IOException {
        if (!Files.isRegularFile(INPUT_CONFIGURATION)) {
            fail("The dogfood input configuration " + INPUT_CONFIGURATION.toAbsolutePath() + " does not exist."
                 + " Generate it with `cd dogfood && ../gradlew --refresh-dependencies"
                 + " :cst-impl:e2immu-write-input-configuration` (see dogfood/README.md). This test must not"
                 + " skip: a ratchet that silently passes when its input is missing defends nothing.");
        }
        for (String dir : PRELOAD) {
            if (!Files.isDirectory(Path.of(dir))) {
                fail("Missing analysed-package directory " + dir + "; without preloading, every eventual"
                     + " verdict reads absent (dogfood/README.md).");
            }
        }
        logCoverage();

        Set<String> survivors = runDogfoodAndCollectSurvivors();

        Files.createDirectories(ACTUAL.getParent());
        Files.write(ACTUAL, survivors);
        LOGGER.info("Wrote {} surviving type(s) to {}", survivors.size(), ACTUAL.toAbsolutePath());

        Set<String> wobble = readNames(WOBBLE);
        Set<String> expected = readNames(EXPECTED);
        Set<String> overlap = new TreeSet<>(expected);
        overlap.retainAll(wobble);
        assertTrue(overlap.isEmpty(), "A type cannot be both a pinned survivor and a known wobble: " + overlap);

        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(survivors);
        Set<String> added = new TreeSet<>(survivors);
        added.removeAll(expected);
        added.removeAll(wobble);

        if (!missing.isEmpty() || !added.isEmpty()) {
            StringBuilder sb = new StringBuilder("The eventual survivor set changed.\n");
            if (!missing.isEmpty()) {
                sb.append("LOST (").append(missing.size()).append(") — a commit has cost these types their")
                        .append(" eventual verdict. This is the regression the ratchet exists to catch; diagnose")
                        .append(" with the recipe in docs/eventual-info-hierarchy.md §\"The drift round\"")
                        .append(" (re-run with EC_RETRACT_DEBUG=1 and rank the ECRETRACT broken-lists):\n");
                missing.forEach(t -> sb.append("    ").append(t).append('\n'));
            }
            if (!added.isEmpty()) {
                sb.append("NEW (").append(added.size()).append(") — progress. If these are stable across two runs,")
                        .append(" update the baseline: cp ").append(ACTUAL).append(' ').append(EXPECTED)
                        .append(" (keeping the header comment). If they flip run-to-run, they belong in ")
                        .append(WOBBLE).append(":\n");
                added.forEach(t -> sb.append("    ").append(t).append('\n'));
            }
            fail(sb.toString());
        }
        LOGGER.info("Eventual ratchet holds: {} surviving type(s)", expected.size());
    }

    /**
     * What this ratchet does and does not cover, reported on every run so it cannot be mistaken.
     * <p>
     * cst-api, cst-analysis and cst-impl are analysed as SOURCE — those are what the ratchet defends.
     * maddi-support and maddi-util arrive as JARS at a version the dogfood build pins (so that reading
     * {@code @Mark}/{@code @Only} out of byte code is exercised), and the pin does not follow the project
     * version. A change to either module therefore does not reach this run at all, and would read as
     * "no change" rather than as an improvement or a regression. To bring them in: bump the versions in
     * {@code dogfood/{cst-api,cst-analysis,cst-impl}/build.gradle.kts}, re-publish the plugin, and
     * regenerate the input configuration per {@code dogfood/README.md} — then re-derive this baseline,
     * because the jars moving forward will move verdicts with them.
     */
    private static void logCoverage() throws IOException {
        java.util.regex.Matcher versionMatcher = java.util.regex.Pattern.compile("(?m)^version=(.+)$")
                .matcher(Files.readString(Path.of("../gradle.properties")));
        String version = versionMatcher.find() ? versionMatcher.group(1).trim() : "?";
        Set<String> jars = new TreeSet<>();
        java.util.regex.Matcher jarMatcher = java.util.regex.Pattern
                .compile("maddi-(?:support|util)-[0-9][^/\"]*\\.jar").matcher(Files.readString(INPUT_CONFIGURATION));
        while (jarMatcher.find()) jars.add(jarMatcher.group());
        LOGGER.info("Ratchet scope: cst-api/cst-analysis/cst-impl as source; {} as jars (project version {}),"
                    + " whose contents this run cannot see changes to", jars, version);
    }

    /**
     * Prep + modification over the dogfood input, mirroring {@link RunAnalyzer}'s pipeline, and the
     * surviving {@code EVENTUALLY_IMMUTABLE_TYPE} verdicts read straight off the analysis — not parsed
     * back out of an {@code FPDUMP}, whose format is a diagnostic and free to change.
     */
    private Set<String> runDogfoodAndCollectSurvivors() throws IOException {
        boolean eventualClusterWasEnabled = EventualCluster.ENABLED;
        EventualCluster.ENABLED = true;
        try {
            InputConfiguration inputConfiguration = JsonStreaming.objectMapper()
                    .readValue(INPUT_CONFIGURATION.toFile(), InputConfigurationImpl.class);
            JavaInspector javaInspector = new JavaInspectorImpl(true, false);
            javaInspector.initialize(inputConfiguration);
            javaInspector.preload("java.base::java.util");

            JavaInspector.ParseOptions parseOptions = new JavaInspector.ParseOptions.Builder()
                    .setDetailedSources(true)
                    .setFailFast(false)
                    .setParallel(true)
                    .setLombok(inputConfiguration.containsLombok())
                    .setIgnoreModule(true)
                    .build();
            Summary summary = javaInspector.parse(parseOptions);
            assertFalse(summary.haveErrors(), "The dogfood sources must parse cleanly");
            ParseResult parseResult = summary.parseResult();

            // after the parse: loading earlier resolves 0 hint types (RunAnalyzer carries the same note)
            SourceSet mainSources = javaInspector.mainSources() != null ? javaInspector.mainSources()
                    : inputConfiguration.sourceSets().stream().findAny().orElseThrow();
            new LoadAnalysisResults(javaInspector.runtime(), mainSources).go(PRELOAD);

            Predicate<TypeInfo> externalsToAccept = _ -> false;
            PrepAnalyzer prepAnalyzer = new PrepAnalyzer(javaInspector.runtime(),
                    new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
            ComputeCallGraph ccg = prepAnalyzer.doPrimaryTypesReturnComputeCallGraph(
                    Set.copyOf(parseResult.primaryTypes()),
                    parseResult.sourceSetToModuleInfoMap().values(),
                    externalsToAccept, parseOptions.parallel());

            List<Info> order = new ComputeAnalysisOrder().go(ccg.graph(), parseOptions.parallel());
            IteratingAnalyzer.Configuration modConfig = new IteratingAnalyzerImpl.ConfigurationBuilder()
                    .setMaxIterations(30)
                    .setStopWhenCycleDetectedAndNoImprovements(true)
                    .setModificationViaReachability(true)
                    .setFaultTolerant(true)
                    .build();
            // the run reports an ANALYSER_ERROR exit on the CLI (cycle protection trips on a few printer
            // methods, dogfood/README.md); the verdicts are still computed, so we do not assert on messages
            new IteratingAnalyzerImpl(javaInspector, modConfig).analyze(order, ccg.graph());

            Set<String> survivors = new TreeSet<>();
            for (Info info : order) {
                if (!(info instanceof TypeInfo typeInfo)) continue;
                Value.EventuallyImmutable ev = info.analysis().getOrNull(PropertyImpl.EVENTUALLY_IMMUTABLE_TYPE,
                        ValueImpl.EventuallyImmutableImpl.class);
                if (ev != null && !ev.isDefault()) {
                    survivors.add(typeInfo.fullyQualifiedName());
                }
            }
            return survivors;
        } finally {
            EventualCluster.ENABLED = eventualClusterWasEnabled;
        }
    }

    private static Set<String> readNames(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            fail("Missing " + path.toAbsolutePath() + "; it is checked in next to dogfood/README.md");
        }
        Set<String> names = new TreeSet<>();
        List<String> unsorted = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            names.add(trimmed);
            unsorted.add(trimmed);
        }
        if (!List.copyOf(names).equals(unsorted)) {
            fail(path + " must be sorted with no duplicates, so that a diff of two versions reads as"
                 + " gained/lost types rather than as a reordering");
        }
        return names;
    }
}
