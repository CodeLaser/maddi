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

package io.codelaser.maddi.gradleplugin.inputconfig;

import io.codelaser.maddi.run.config.util.JavaModules;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.ImmutableGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The dependency graph over a GRADLE project's source sets and libraries.
 *
 * <p>⚠ <b>IT HAS A TWIN:</b> {@code io.codelaser.maddi.run.config.util.ComputeDependencies}, which the Maven
 * plugin uses. The three rules they share -- jmod edges, "every non-JDK set depends on all jmods", and
 * test -> main -- are stated in both; sibling projects from the {@code e2immuSourceElements} variant and
 * source-project edges have no counterpart there. Runtime-only scoping does, but not here: Maven derives it from
 * the artifact's SCOPE, in {@code mvnplugin/ComputeSourceSets}, before the twin sees anything.
 * See that class for why they are not merged. <b>Check both when changing a shared rule.</b>
 */
public class ComputeDependencies {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeDependencies.class);

    public G<String> go(ComputeSourceSets.Result result) {
        G.Builder<String> builder = new ImmutableGraph.Builder<>(Long::sum);

        // jmods are common
        Set<String> jmods = new HashSet<>();
        for (SourceSet sourceSet : result.sourceSetsByName().values()) {
            if (sourceSet.partOfJdk()) {
                String jmod = sourceSet.name();
                Set<String> dependencies = JavaModules.jmodDependency(jmod);
                LOGGER.info("Adding JMOD {} -> {}", jmod, dependencies);
                builder.add(jmod, dependencies);
                jmods.add(jmod);
            }
        }
        Map<String, Boolean> jmodsAndExternalToMain = new HashMap<>();
        jmods.forEach(jmod -> jmodsAndExternalToMain.put(jmod, true));
        HashSet<String> seen = new HashSet<>();
        // Runtime-only for the ANALYSED project. Kept rather than discarded, because that is a fact about this
        // project and a sibling's sources may well be compiled against them -- see recursionForSourceSets.
        Set<String> runtimeOnlyLibraries = new LinkedHashSet<>();
        recursionForClassPathParts(builder, result, seen, jmods, jmodsAndExternalToMain, runtimeOnlyLibraries);

        LOGGER.info(" -- now recursing for source sets");
        recursionForSourceSets(builder, result, seen, jmodsAndExternalToMain, runtimeOnlyLibraries, false);

        // the edges AMONG source projects (cst-analysis/main -> cst-api/main): the recursion above only wired the
        // consuming project to each of its dependency projects, not those dependency projects to one another
        result.sourceProjectEdges().forEach((from, tos) -> {
            String fromMain = from + "/main";
            for (String to : tos) {
                LOGGER.info("Adding SRC->SRC (transitive) {} -> {}", fromMain, to + "/main");
                builder.add(fromMain, List.of(to + "/main"));
            }
        });
        return builder.build();
    }

    private void recursionForClassPathParts(G.Builder<String> builder, ComputeSourceSets.Result result,
                                            Set<String> seen, Set<String> jmods, Map<String, Boolean> jmodsAndExternalToMain,
                                            Set<String> runtimeOnlyLibraries) {

        // depth first
        for (ComputeSourceSets.Result sub : result.sourceSetDependencies()) {
            recursionForClassPathParts(builder, sub, seen, jmods, jmodsAndExternalToMain, runtimeOnlyLibraries);
        }

        // every external library is dependent on all the jmods
        for (SourceSet sourceSet : result.sourceSetsByName().values()) {
            String name = sourceSet.name();
            if (sourceSet.externalLibrary() && !sourceSet.partOfJdk() && seen.add(name)) {
                builder.add(name, jmods);
                if (!sourceSet.runtimeOnly()) {
                    jmodsAndExternalToMain.merge(name, !sourceSet.test(), Boolean::logicalOr);
                    LOGGER.info("Adding EXT {} in main? {} -> {}", name, jmodsAndExternalToMain.get(name), jmods);
                } else {
                    runtimeOnlyLibraries.add(name);
                    LOGGER.info("Not adding EXT {} in main? {}, runtime only", name, !sourceSet.test());
                }
            }
        }
    }

    /**
     * @param siblingProject this {@link ComputeSourceSets.Result} is a DEPENDENCY project's, reached through the
     *                       {@code e2immuSourceElements} variant, rather than the analysed project's own.
     */
    private List<String> recursionForSourceSets(G.Builder<String> builder, ComputeSourceSets.Result result,
                                                Set<String> seen, Map<String, Boolean> jmodsAndExternalToMain,
                                                Set<String> runtimeOnlyLibraries,
                                                boolean siblingProject) {
        if (!seen.add(result.mainSourceSetName())) return List.of();
        LOGGER.info("Enter recursion for {}, have {} dependencies",
                result.mainSourceSetName(), result.sourceSetDependencies().size());

        // depth first
        List<String> dependentSourceSets = new ArrayList<>();
        for (ComputeSourceSets.Result sub : result.sourceSetDependencies()) {
            dependentSourceSets.addAll(recursionForSourceSets(builder, sub, seen, jmodsAndExternalToMain,
                    runtimeOnlyLibraries, true));
        }

        List<String> mainSourceSets = new ArrayList<>();
        List<String> testSourceSets = new ArrayList<>();

        // every source set is dependent on all the external libraries, and the jmods
        for (SourceSet sourceSet : result.sourceSetsByName().values()) {
            if (!sourceSet.externalLibrary()) {
                String name = sourceSet.name();
                jmodsAndExternalToMain.forEach((je, isMain) -> {
                    // ⛔ A SIBLING'S CLASS PATH IS NOT THE CONSUMER'S, AND SCOPING IT AS IF IT WERE DENIES IT ITS
                    // OWN DEPENDENCIES. `isMain` says whether a library reaches the ANALYSED project outside test
                    // scope -- a fact about that project, which says nothing about a sibling whose sources we are
                    // about to parse. pulsar's `buildtools` is the case: its MAIN sources are TestNG listeners,
                    // and TestNG reaches managed-ledger only in test scope, so buildtools/main was refused the one
                    // library it is written against (32 "package org.testng does not exist" and the errors that
                    // cascade from them).
                    // The union of the consumer's libraries is an approximation -- the exact answer is the
                    // sibling's own resolved class path, which cannot be read without the cross-project
                    // resolution the variant mechanism exists to avoid. It is a SUPERSET in the ordinary case
                    // (the consumer depends on the sibling, so the sibling's compile dependencies are on the
                    // consumer's class path transitively), and a wider class path costs a type resolving that a
                    // stricter build would have rejected -- which is not what this configuration is judged on.
                    if (siblingProject || sourceSet.test() || isMain) {
                        LOGGER.info("Adding SRC->EXT/JMOD {} -> {}", name, je);
                        builder.add(name, List.of(je));
                    }
                });
                if (siblingProject && !runtimeOnlyLibraries.isEmpty()) {
                    // ...and for the same reason, a library that is runtime-only HERE may be a compile dependency
                    // THERE. pulsar: oxia-client-api and protobuf-java reach managed-ledger at runtime only, and
                    // pulsar-metadata's and pulsar-common's main sources are written against both.
                    LOGGER.info("Adding SRC->RUNTIME-ONLY {} -> {}", name, runtimeOnlyLibraries);
                    builder.add(name, runtimeOnlyLibraries);
                }
                LOGGER.info("Adding SRC->DEP {} -> {}", name, dependentSourceSets);
                builder.add(name, dependentSourceSets);

                if (sourceSet.test()) {
                    testSourceSets.add(name);
                } else {
                    mainSourceSets.add(name);
                }
            }
        }
        for (String testName : testSourceSets) {
            LOGGER.info("ADDING SRC MAIN->TEST {} -> {}", testName, mainSourceSets);
            builder.add(testName, mainSourceSets);
        }
        LOGGER.info("Ended recursion for {}", result.mainSourceSetName());
        return mainSourceSets;
    }

}
