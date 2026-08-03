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
 * {@code maddi-testoss}'s {@code task config:coil}). It lists only the six library jars — the JDK is not in
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
                      + "; generate it with `task config:coil` in test-oss/maddi-testoss");
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
     * analysis. It exercises considerably more than {@link #parsesViaTheKotlinInspector}: every Kotlin type is
     * emitted as a Java stub and <b>compiled by javac</b>, so this fails on any type maddi cannot express in
     * Java. Coil having no {@code .java} at all, it is also the first exercise of the mixed driver with an
     * empty Java half.
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
     * The shipping path: the {@code maddi-kotlin} CLI, which is how Kotlin support is released. It runs
     * {@code RunMixedPrepAnalyzer} — the mixed parse above <i>plus</i> the prep analysis (call graph +
     * analysis order).
     *
     * <h2>Why this is disabled</h2>
     * The parse half now succeeds end to end; what fails is <b>prep</b>, and on a construct maddi's own
     * {@code maddi-cst-api/kotlin-cst-assessment.md} already lists as open — <b>{@code try} as an
     * expression</b>. Coil's {@code coil3.util.getCompletedOrNull} is
     * {@code return try { getCompleted() } catch (_: Throwable) { null }}; no CST node yields a value from a
     * {@code try}, the statement is built without a {@code Source}, and {@code MethodAnalyzer} dereferences
     * {@code statement.source().index()} — NPE after 148 types processed. The assessment calls it "rare;
     * desugar to a helper or accept a small new node if it actually shows up". It has shown up, and choosing
     * between those two is a design decision, not a bug fix.
     * <p>
     * Everything that blocked this before prep <i>was</i> fixed: in {@code JavaStubGenerator}, file-facade
     * naming, Java keywords, interface field initializers, annotation nature, erased generic supertypes and
     * empty-body interface defaults; in {@code MixedProjectInspector}, nested-type and classpath handling; and
     * in the front end, {@code actual typealias} expansion and Kotlin interface delegation. That took the stub
     * compile from 24 errors to zero. Kotlin's primitive array classes are translated in the stub generator
     * rather than the CST — see {@code ExpectActualTypealiasTest.primitiveArrayClassesAreJvmPrimitiveArrays}
     * for why. Re-enable when {@code try}-as-an-expression is handled.
     */
    @Disabled("prep NPEs on `try` as an expression, an open item in kotlin-cst-assessment.md; see the javadoc")
    @Test
    public void runsPrepViaTheMixedCli() {
        Path config = config();
        assertEquals(Main.EXIT_OK, Main.execute(new String[]{Main.INPUT_CONFIGURATION, config.toString()}));
    }
}
