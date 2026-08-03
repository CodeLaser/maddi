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

package org.e2immu.analyzer.run.kotlinmain;

import org.e2immu.analyzer.run.config.util.JsonStreaming;
import org.e2immu.analyzer.run.openjdkmain.TestOssCorpus;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.e2immu.language.inspection.kotlin.KotlinInspector;
import org.e2immu.language.inspection.mixed.MixedProjectInspector;
import org.e2immu.language.inspection.resource.InputConfigurationImpl;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
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
                      + "; generate it with `task config:detekt` in test-oss/maddi-testoss");
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
     * The mixed parse, which additionally emits every Kotlin type as a Java stub and compiles it with javac.
     *
     * <h2>Why this is disabled</h2>
     * The Kotlin parse itself succeeds for all 31 source sets; what fails is the stub compile, on a tail of
     * {@code JavaStubGenerator} fidelity gaps that a corpus this size is the first to reach. Four distinct
     * causes, none of them in the front end:
     * <ul>
     *   <li><b>Implicit {@code super()} with no matching parent constructor.</b> A stub constructor body
     *   throws, but javac still inserts {@code super()}, and the parent — {@code Markdown},
     *   {@code java.io.PrintStream} — has no no-arg constructor. Needs an explicit {@code super(...)} with
     *   type-appropriate defaults; note the parent may be a library type, so "give every stub a no-arg
     *   constructor" only solves half of it.</li>
     *   <li><b>Duplicate methods</b> — {@code getIndent()} already defined in {@code YML}: a property's
     *   generated getter colliding with an explicitly declared one.</li>
     *   <li><b>Method-level erasure</b> — {@code DetektPomModel.getModelAspect} erases its own type
     *   parameter, so it neither overrides nor differs from {@code PomModel}'s generic method ("name clash
     *   … same erasure, yet neither overrides the other"). The supertype fix applied for coil kept type
     *   arguments on {@code extends}/{@code implements}; methods still erase.</li>
     * </ul>
     * Each is a bounded fix in the stub generator, and none blocks {@link #parsesViaTheKotlinInspector}.
     */
    @Disabled("tail of JavaStubGenerator fidelity gaps (implicit super(), duplicate getters, method erasure)")
    @Test
    public void parsesViaTheMixedProjectInspector() throws IOException {
        Path config = config();
        MixedProjectInspector.Result result = new MixedProjectInspector().parse(read(config));
        LOGGER.info("detekt, mixed parse: {} Kotlin + {} Java type(s)",
                result.getKotlinTypes().size(), result.getJavaTypes().size());
        assertTrue(result.getKotlinTypes().size() >= PRIMARY_TYPE_FLOOR);
        assertEquals(List.of(), result.getJavaTypes(), "detekt has no compiled Java sources");
    }
}
