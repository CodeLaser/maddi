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

import org.e2immu.analyzer.modification.prepwork.PrepAnalyzer;
import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeAnalysisOrder;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.mixed.MixedProjectInspector;
import org.e2immu.util.internal.graph.G;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /** A small summary of a prep run: the number of Kotlin/Java/primary types and the analysis-order size. */
    public record Summary(int kotlinTypes, int javaTypes, int primaryTypes, int analysisOrderSize) {
    }

    public Summary go(InputConfiguration inputConfiguration) {
        MixedProjectInspector.Result parsed = new MixedProjectInspector().parse(inputConfiguration);
        Runtime runtime = parsed.getRuntime();

        Set<TypeInfo> primaryTypes = Stream.concat(parsed.getKotlinTypes().stream(), parsed.getJavaTypes().stream())
                .map(TypeInfo::primaryType)
                .collect(Collectors.toUnmodifiableSet());
        LOGGER.info("Mixed parse produced {} Kotlin and {} Java type(s), {} primary; running prep analyzer",
                parsed.getKotlinTypes().size(), parsed.getJavaTypes().size(), primaryTypes.size());

        PrepAnalyzer prepAnalyzer = new PrepAnalyzer(runtime);
        G<Info> callGraph = prepAnalyzer.doPrimaryTypesReturnGraph(primaryTypes);
        List<Info> order = new ComputeAnalysisOrder().go(callGraph);
        LOGGER.info("Prep analysis order has size {}", order.size());

        return new Summary(parsed.getKotlinTypes().size(), parsed.getJavaTypes().size(),
                primaryTypes.size(), order.size());
    }
}
