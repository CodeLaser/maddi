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

import org.e2immu.analyzer.modification.analyzer.IteratingAnalyzer;
import org.e2immu.analyzer.modification.analyzer.impl.IteratingAnalyzerImpl;
import org.e2immu.analyzer.modification.common.AnalyzerException;
import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.analyzer.modification.prepwork.io.LoadAnalysisResults;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.mixed.MixedProjectInspector;
import org.e2immu.util.internal.graph.G;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Prep-only runner for a mixed Java+Kotlin project. Parses the input configuration with
 * {@link MixedProjectInspector} — the openjdk and K2 front-ends share one core (so a cross-language reference
 * resolves to a single {@link TypeInfo}), each type keeps its own source set, and the configuration's library
 * class-path parts are honoured — then runs the prep analysis (call graph + analysis order) over the combined
 * primary types.
 * <p>
 * It deliberately stops after prep: no modification analysis is run and no results are written (the modification
 * analysis has open issues on real code, handled elsewhere). It inherits {@link MixedProjectInspector}'s current
 * scope (Java↔Java across rebuilt source sets, and a project mixing both cross-language directions in one module,
 * are follow-ups).
 * <p>
 * The running JVM must be started with the openjdk {@code --add-exports jdk.compiler/com.sun.tools.javac.*=ALL-UNNAMED}.
 */
public class RunMixedPrepAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunMixedPrepAnalyzer.class);

    /**
     * A small summary of a run: the number of Kotlin/Java/primary types, the analysis-order size, and how many
     * elements prep had to isolate (0 when it ran clean), and how many primary types were concluded immutable
     * (0 when no modification analysis ran — and a telltale that the annotated APIs were not loaded when one
     * did).
     */
    public record Summary(int kotlinTypes, int javaTypes, int primaryTypes, int analysisOrderSize,
                          int prepErrors, int immutableTypes) {
    }

    public Summary go(InputConfiguration inputConfiguration) throws IOException {
        return go(inputConfiguration, false, List.of());
    }

    public Summary go(InputConfiguration inputConfiguration, boolean modification) throws IOException {
        return go(inputConfiguration, modification, List.of());
    }

    /**
     * @param modification            also run the iterating modification/immutability analysis over the prep
     *                                result. The analyzer takes a {@code JavaInspector}; a Kotlin-only project
     *                                has one anyway, since the mixed driver's openjdk inspector owns the
     *                                shared core.
     * @param analysisResultsDirs     pre-analyzed annotations for library types (the AAPI archive). Without
     *                                them every library type is an unknown, so nothing built on one can be
     *                                concluded immutable and the run reports only {@code @FinalFields} and
     *                                {@code @Mutable} — which is exactly what detekt did before this.
     */
    public Summary go(InputConfiguration inputConfiguration, boolean modification,
                      List<String> analysisResultsDirs) throws IOException {
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(inputConfiguration);
        Runtime runtime = parsed.getRuntime();

        Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream())
                .map(TypeInfo::primaryType)
                .collect(Collectors.toUnmodifiableSet());
        LOGGER.info("Mixed parse produced {} Kotlin and {} Java type(s), {} primary; running prep analyzer",
                parsed.getKotlinTypes().size(), parsed.getJavaTypes().size(), primaryTypes.size());

        // AFTER the parse, as in run-openjdk's RunAnalyzer: only by now is the compiled-types manager
        // populated, and loading earlier resolves none of the hint types. The source set of request is a
        // Kotlin one here — that is where the lookups originate, and it is what the distance-based resolution
        // in InfoByFqn measures from.
        if (!analysisResultsDirs.isEmpty()) {
            SourceSet sourceSetOfRequest = parsed.getKotlinBySourceSet().keySet().stream().findFirst()
                    .orElseGet(() -> inputConfiguration.sourceSets().stream().findAny().orElse(null));
            LOGGER.info("Loading analyzed analysis hints from {} (source set of request {})",
                    analysisResultsDirs, sourceSetOfRequest);
            new LoadAnalysisResults(runtime, sourceSetOfRequest).go(analysisResultsDirs);
        }

        // Fault-tolerant, as in run-openjdk's RunAnalyzer: one failing method must not deny analysis to a whole
        // corpus. The Kotlin front end has more rough edges than the Java one, so this matters more here, not
        // less — prep aborted detekt outright at 652 of 1,202 types before this.
        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime,
                new PrepAnalyzer.Options.Builder().setFaultTolerant(true).build());
        G<Info> callGraph = prepAnalyzer.doPrimaryTypesReturnGraph(primaryTypes);
        int prepErrors = report("Prep", prepAnalyzer.exceptions());
        List<Info> order = new ComputeAnalysisOrder().go(callGraph);
        LOGGER.info("Prep analysis order has size {}", order.size());

        int immutableTypes = 0;
        if (modification) {
            LOGGER.info("Starting modification analysis over {} element(s)", order.size());
            IteratingAnalyzer.Configuration configuration = new IteratingAnalyzerImpl.ConfigurationBuilder()
                    .setMaxIterations(30) // safety net; the loop exits on convergence/certification/plateau
                    .setStopWhenCycleDetectedAndNoImprovements(true)
                    .setFaultTolerant(true) // isolate a crash on one element rather than abort the run
                    .build();
            IteratingAnalyzer analyzer = new IteratingAnalyzerImpl(parsed.getJavaInspector(), configuration);
            analyzer.analyze(order, callGraph); // the graph enables worklist narrowing
            LOGGER.info("Modification analysis finished");
            immutableTypes = (int) primaryTypes.stream().filter(RunMixedPrepAnalyzer::isImmutable).count();
        }
        return new Summary(parsed.getKotlinTypes().size(), parsed.getJavaTypes().size(),
                primaryTypes.size(), order.size(), prepErrors, immutableTypes);
    }

    /**
     * Whether a type reached either immutable level. Reported because it is the single number that says the
     * annotated APIs were in play: with no library annotations the analysis cannot conclude immutability for
     * anything built on a library type, so this is exactly zero while everything else still looks healthy.
     */
    private static boolean isImmutable(TypeInfo typeInfo) {
        Value.Immutable immutable = typeInfo.analysis()
                .getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
        return immutable != null && immutable.isAtLeastImmutableHC();
    }

    /** Log what was isolated, so a run that "succeeded" cannot hide how much it skipped. */
    private static int report(String phase, List<AnalyzerException> exceptions) {
        if (exceptions.isEmpty()) return 0;
        LOGGER.error("{} produced {} error(s); the affected elements were skipped:", phase, exceptions.size());
        int i = 1;
        for (AnalyzerException ae : exceptions) {
            Info info = ae.getInfo();
            String at = info == null || info.source() == null ? "?" : info.source().compact2();
            Throwable cause = ae.getCause() == null ? ae : ae.getCause();
            LOGGER.error("  [{}] {} ({}): {}: {}", i++, info, at, cause.getClass().getName(), cause.getMessage());
        }
        return exceptions.size();
    }
}
