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
 * The first <b>Kotlin</b> corpus test: the JVM slice of <a href="https://github.com/coil-kt/coil">coil</a>'s
 * {@code coil-core}. The Java corpus tests ({@code TestGuava}, {@code TestFernflower}, … in
 * {@code maddi-run-openjdk}) resolve their checkout the same way, via {@link TestOssCorpus}.
 *
 * <h2>Why a slice, and why this slice</h2>
 * Coil is <b>Kotlin Multiplatform</b>, not Kotlin/JVM: 445 {@code .kt} files, zero {@code .java}, targeting
 * JVM, Android, JS, wasmJs and native/Apple. Two things follow.
 * <ol>
 *   <li><b>One target only.</b> {@code KotlinProjectScan} builds its K2 session on
 *   {@code JvmPlatforms.defaultJvmPlatform}, so Android (which additionally needs {@code android.jar}), JS,
 *   wasmJs and native are out of reach. Leaving several targets in would also give one FQN several
 *   {@code actual} declarations — {@code TestExpectActual} only proves the {@code expect} is dropped when a
 *   single {@code actual} remains.</li>
 *   <li><b>The hierarchy is flattened.</b> The JVM target's six main source sets ({@code commonMain},
 *   {@code nonAndroidMain}, {@code nonJsCommonMain}, {@code nonAppleMain}, {@code jvmCommonMain},
 *   {@code jvmMain}) become ONE maddi {@link SourceSet} with six source directories — which is what a
 *   {@code compileKotlinJvm} invocation would itself yield, since the hierarchy source sets have no compile
 *   of their own. All six are required: {@code commonMain} carries {@code expect} declarations whose
 *   {@code actual}s live in the others. The same shape as {@code TestKotlinStdlibParse}, which flattens the
 *   stdlib's {@code commonMain} + {@code jvmMain}.</li>
 * </ol>
 *
 * <h2>Where the input configuration comes from</h2>
 * Neither documented route produces one for coil. The Gradle plugin keys on {@code org.jetbrains.kotlin.jvm},
 * the java plugin's {@code SourceSet} container and the {@code compileKotlin} task, none of which a
 * multiplatform build has; and {@code --compile-log} needs the build to run, which needs an Android SDK for
 * AGP. So {@code test-oss/coil/inputConfiguration.json} is assembled directly (recipe:
 * {@code corpus/}'s {@code task config:coil}). It lists only the six library jars — the JDK is not in
 * it, because both {@link KotlinInspector} and {@code MixedProjectInspector} take {@code jdkHome} from the
 * running JVM's {@code java.home}.
 */
@Tag("slow")
public class TestCoilJvmSlice {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCoilJvmSlice.class);

    private static final String CORPUS = "coil";

    /**
     * A floor rather than an exact count, so that a coil version bump does not make this brittle — the same
     * contract {@code TestKotlinStdlibParse} uses. The slice holds 101 {@code .kt} files; Kotlin puts several
     * top-level declarations in one file and synthesizes file facades, so the type count is not the file count.
     */
    private static final int PRIMARY_TYPE_FLOOR = 80;

    private static Path config() {
        Path config = TestOssCorpus.config(CORPUS);
        Assumptions.assumeTrue(Files.exists(config),
                () -> "requires the coil corpus checkout with its input configuration at "
                      + config.toAbsolutePath().normalize()
                      + "; generate it with `task corpus:config:coil` at the repo root");
        return config;
    }

    private static InputConfigurationImpl read(Path config) throws IOException {
        return JsonStreaming.objectMapper().readValue(config.toFile(), InputConfigurationImpl.class);
    }

    /**
     * The pure-Kotlin path: {@link KotlinInspector#parseFromConfiguration}. This is the one that says whether
     * K2 handles idiomatic multiplatform Kotlin at corpus scale — coroutines, {@code expect}/{@code actual}
     * across a source-set hierarchy, extension functions, {@code object}/companion, sealed hierarchies. It is
     * deliberately separate from the CLI test below so a failure names which layer broke.
     */
    @Test
    public void parsesViaTheKotlinInspector() throws IOException {
        Path config = config();
        KotlinInspector inspector = new KotlinInspector(new RuntimeImpl());
        inspector.initialize(read(config));

        Map<SourceSet, List<TypeInfo>> bySourceSet = inspector.parseFromConfiguration();
        int primaryTypes = bySourceSet.values().stream().mapToInt(List::size).sum();
        // print the scale: a corpus test that silently analyzed nothing must not read as a pass
        LOGGER.info("coil-core JVM slice: {} primary type(s) over {} source set(s)",
                primaryTypes, bySourceSet.size());
        assertTrue(primaryTypes >= PRIMARY_TYPE_FLOOR,
                "expected at least " + PRIMARY_TYPE_FLOOR + " primary types from coil-core's JVM slice, got "
                + primaryTypes);
    }

    /**
     * The full mixed parse — {@code MixedProjectInspector}, which is what the shipping CLI runs before any
     * analysis. It exercises more than {@link #parsesViaTheKotlinInspector}: the openjdk inspector owns the
     * shared core, so every library type the Kotlin front end touches is loaded from <b>bytecode</b> through
     * it rather than from K2's own view. No Java stub is generated — coil has no {@code .java} at all, and a
     * stub exists only so javac can resolve a Kotlin type for Java source.
     */
    @Test
    public void parsesViaTheMixedProjectInspector() throws IOException {
        Path config = config();
        MixedProjectInspector.Result result = new MixedProjectInspector().parse(read(config));
        LOGGER.info("coil-core JVM slice, mixed parse: {} Kotlin + {} Java type(s)",
                result.getKotlinTypes().size(), result.getJavaTypes().size());
        assertTrue(result.getKotlinTypes().size() >= PRIMARY_TYPE_FLOOR,
                "expected at least " + PRIMARY_TYPE_FLOOR + " Kotlin types, got " + result.getKotlinTypes().size());
        assertEquals(List.of(), result.getJavaTypes(), "coil has no Java sources");
    }

    /**
     * The shipping path's runner: the mixed parse above <i>plus</i> the prep analysis (call graph + analysis
     * order), which is what the {@code maddi-kotlin} CLI drives.
     *
     * <p>Prep does not run clean on coil, and the isolated elements are the point of the assertion rather than
     * a reason to skip: {@code coil3.util.getCompletedOrNull} is
     * {@code return try { getCompleted() } catch (_: Throwable) { null }} — <b>{@code try} as an
     * expression</b>, which {@code maddi-cst-api/kotlin-cst-assessment.md} already lists as open ("rare;
     * desugar to a helper or accept a small new node if it actually shows up"). No CST node yields a value
     * from a {@code try}, so the statement is built without a {@code Source} and {@code MethodAnalyzer} NPEs
     * on {@code statement.source().index()}. Prep isolates it and continues; the count is asserted small, so
     * this cannot quietly become a run that skips most of the corpus.
     */
    @Test
    public void runsPrepViaTheMixedRunner() throws IOException {
        Path config = config();
        RunMixedPrepAnalyzer.Summary summary = new RunMixedPrepAnalyzer().go(read(config));
        LOGGER.info("coil prep: {} primary type(s), analysis order {}, {} isolated",
                summary.primaryTypes(), summary.analysisOrderSize(), summary.prepErrors());
        assertTrue(summary.analysisOrderSize() > 500,
                "expected a substantial analysis order, got " + summary.analysisOrderSize());
        assertTrue(summary.prepErrors() < 10,
                "prep isolated " + summary.prepErrors() + " elements; that is no longer a tail");
    }
}
