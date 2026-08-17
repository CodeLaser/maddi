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

package io.codelaser.maddi.run.kotlinmain;

import io.codelaser.maddi.run.config.util.JsonStreaming;
import io.codelaser.maddi.run.openjdkmain.TestOssCorpus;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import io.codelaser.maddi.inspection.kotlin.KotlinInspector;
import io.codelaser.maddi.inspection.mixed.MixedProjectInspector;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The second Kotlin corpus, and the one that exercises what {@link TestCoilJvmSlice} cannot.
 *
 * <p>Where coil is Kotlin Multiplatform — forcing a hand-assembled configuration covering one flattened
 * source set — detekt is a plain multi-module Kotlin/JVM Gradle build. That means two firsts:
 * <ol>
 *   <li>its {@code inputConfiguration.json} comes from the <b>{@code --compile-log}</b> route, so
 *   {@code ParseKotlincList} + {@code CompileListToSourceSets} are exercised against a real build for the
 *   first time: 32 {@code kotlinc} invocations become <b>31 source sets</b> linked by output identity, with
 *   82 library jars and generated-source directories (buildConfig, kotlin-dsl accessors) picked up;</li>
 *   <li>it is a genuine <b>multi-source-set</b> parse — {@code detekt-core} alone depends on 18 others —
 *   whereas coil is one set, so cross-source-set resolution in dependency order is under test here.</li>
 * </ol>
 * Roughly 1,000 {@code .kt} files against coil's 101. detekt has no compiled Java at all (its nine
 * {@code .java} files are test <i>resources</i>).
 */
@Tag("slow")
public class TestDetektCorpus {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDetektCorpus.class);

    private static final String CORPUS = "detekt";

    /** A floor, not an exact count, so a detekt version bump does not make this brittle. */
    private static final int PRIMARY_TYPE_FLOOR = 1_000;
    private static final int SOURCE_SET_FLOOR = 25;

    private static Path config() {
        Path config = TestOssCorpus.config(CORPUS);
        Assumptions.assumeTrue(Files.exists(config),
                () -> "requires the detekt corpus checkout with its input configuration at "
                      + config.toAbsolutePath().normalize()
                      + "; generate it with `task corpus:config:detekt` at the repo root");
        return config;
    }

    private static InputConfigurationImpl read(Path config) throws IOException {
        return JsonStreaming.objectMapper().readValue(config.toFile(), InputConfigurationImpl.class);
    }

    /**
     * The pure-Kotlin path over the whole multi-module project. This is the one that says whether the front
     * end handles a real Kotlin codebase at scale, and it is deliberately independent of the Java-stub
     * machinery below.
     */
    @Test
    public void parsesViaTheKotlinInspector() throws IOException {
        Path config = config();
        KotlinInspector inspector = new KotlinInspector(new RuntimeImpl());
        inspector.initialize(read(config));

        Map<SourceSet, List<TypeInfo>> bySourceSet = inspector.parseFromConfiguration();
        int primaryTypes = bySourceSet.values().stream().mapToInt(List::size).sum();
        LOGGER.info("detekt: {} primary type(s) over {} source set(s)", primaryTypes, bySourceSet.size());
        assertTrue(bySourceSet.size() >= SOURCE_SET_FLOOR,
                "expected at least " + SOURCE_SET_FLOOR + " source sets, got " + bySourceSet.size());
        assertTrue(primaryTypes >= PRIMARY_TYPE_FLOOR,
                "expected at least " + PRIMARY_TYPE_FLOOR + " primary types, got " + primaryTypes);
    }

    /**
     * The mixed parse, which is what the shipping CLI runs. detekt has no Java source sets, so no Java stub is
     * generated or compiled — that step exists only so javac can resolve Kotlin types for Java source, and
     * there is none. It is still the stricter path: the openjdk inspector owns the shared core, and every
     * library type the Kotlin front end touches is loaded from bytecode through it.
     */
    @Test
    public void parsesViaTheMixedProjectInspector() throws IOException {
        Path config = config();
        MixedProjectInspector.Result result = new MixedProjectInspector().parse(read(config));
        LOGGER.info("detekt, mixed parse: {} Kotlin + {} Java type(s)",
                result.getKotlinTypes().size(), result.getJavaTypes().size());
        assertTrue(result.getKotlinTypes().size() >= PRIMARY_TYPE_FLOOR);
        assertEquals(List.of(), result.getJavaTypes(), "detekt has no compiled Java sources");
    }

    /**
     * The <b>annotated APIs for the JDK</b>. Without them every library type is an unknown, and the analysis
     * cannot conclude anything positive about a type built on one — which is exactly what happened the first
     * time this ran: {@code @FinalFields=1320, @Mutable=428} and not one immutable type. The Java corpus tests
     * pass the same directory ({@code TestFernflower} et al.).
     */
    private static final List<String> JDK_ANNOTATED_APIS = List.of(
            "../maddi-aapi-archive/src/main/resources/io/codelaser/maddi/aapi/archive/analyzedPackageFiles/jdk");

    /**
     * Prep <b>and</b> the iterating modification/immutability analysis, over the whole project — the first time
     * the modification analyzer has run on Kotlin. It converges in seven iterations over ~9,200 elements,
     * ending in certification.
     *
     * <p>The immutability assertion is the point. With the JDK annotated APIs loaded the verdicts are
     * {@code @Immutable=676, @Immutable(hc=true)=137, @FinalFields=517, @Mutable=418}; without them the first
     * two are <b>zero</b> and almost everything piles up in {@code @FinalFields}. A corpus run that quietly
     * lost the AAPI would still converge, still report thousands of verdicts, and still look like a success —
     * so the floor below is what makes that visible.
     *
     * <p>The isolated elements are prep failures, all one cause today
     * ({@code Trying to overwrite a value for property variableData}); their ceiling keeps the fault-tolerant
     * run honest, since one that skipped half the corpus would otherwise also look like a success.
     */
    @Test
    public void runsModificationAnalysis() throws IOException {
        Path config = config();
        RunMixedPrepAnalyzer.Summary summary =
                new RunMixedPrepAnalyzer().go(read(config), true, JDK_ANNOTATED_APIS);
        LOGGER.info("detekt modification: {} primary type(s), analysis order {}, {} isolated by prep,"
                    + " {} immutable type(s)", summary.primaryTypes(), summary.analysisOrderSize(),
                summary.prepErrors(), summary.immutableTypes());
        assertTrue(summary.primaryTypes() >= PRIMARY_TYPE_FLOOR,
                "expected at least " + PRIMARY_TYPE_FLOOR + " primary types, got " + summary.primaryTypes());
        assertTrue(summary.analysisOrderSize() > 5_000,
                "expected a substantial analysis order, got " + summary.analysisOrderSize());
        assertTrue(summary.prepErrors() < 50,
                "prep isolated " + summary.prepErrors() + " elements; that is no longer a tail");
        assertTrue(summary.immutableTypes() > 300,
                "expected the JDK annotated APIs to yield immutable types, got " + summary.immutableTypes()
                + "; zero means they were not loaded");
    }
}
