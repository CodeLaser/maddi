package org.e2immu.gradleplugin.inputconfig;

import org.e2immu.analyzer.run.config.util.JavaModules;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.util.internal.graph.G;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ComputeDependencies {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeDependencies.class);

    public G<String> go(ComputeSourceSets.Result result) {
        G.Builder<String> builder = new G.Builder<>(Long::sum);

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
        recursionForClassPathParts(builder, result, seen, jmods, jmodsAndExternalToMain);

        LOGGER.info(" -- now recursing for source sets");
        recursionForSourceSets(builder, result, seen, jmodsAndExternalToMain);
        return builder.build();
    }

    private void recursionForClassPathParts(G.Builder<String> builder, ComputeSourceSets.Result result,
                                            Set<String> seen, Set<String> jmods, Map<String, Boolean> jmodsAndExternalToMain) {

        // depth first
        for (ComputeSourceSets.Result sub : result.sourceSetDependencies()) {
            recursionForClassPathParts(builder, sub, seen, jmods, jmodsAndExternalToMain);
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
                    LOGGER.info("Not adding EXT {} in main? {}, runtime only", name, !sourceSet.test());
                }
            }
        }
    }

    private List<String> recursionForSourceSets(G.Builder<String> builder, ComputeSourceSets.Result result,
                                                Set<String> seen, Map<String, Boolean> jmodsAndExternalToMain) {
        if (!seen.add(result.mainSourceSetName())) return List.of();
        LOGGER.info("Enter recursion for {}, have {} dependencies",
                result.mainSourceSetName(), result.sourceSetDependencies().size());

        // depth first
        List<String> dependentSourceSets = new ArrayList<>();
        for (ComputeSourceSets.Result sub : result.sourceSetDependencies()) {
            dependentSourceSets.addAll(recursionForSourceSets(builder, sub, seen, jmodsAndExternalToMain));
        }

        List<String> mainSourceSets = new ArrayList<>();
        List<String> testSourceSets = new ArrayList<>();

        // every source set is dependent on all the external libraries, and the jmods
        for (SourceSet sourceSet : result.sourceSetsByName().values()) {
            if (!sourceSet.externalLibrary()) {
                String name = sourceSet.name();
                jmodsAndExternalToMain.forEach((je, isMain) -> {
                    if (sourceSet.test() || isMain) {
                        LOGGER.info("Adding SRC->EXT/JMOD {} -> {}", name, je);
                        builder.add(name, List.of(je));
                    }
                });
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
