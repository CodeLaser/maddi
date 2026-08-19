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

package io.codelaser.maddi.run.config.util;

import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.ImmutableGraph;

import java.util.*;
import java.util.function.Consumer;

/**
 * The dependency graph over a MAVEN module's source sets and libraries.
 *
 * <p>⚠ <b>IT HAS A TWIN, AND THEY ARE DELIBERATELY NOT MERGED:</b>
 * {@code io.codelaser.maddi.gradleplugin.inputconfig.ComputeDependencies}. They share three rules and state them
 * separately -- jmod edges, "every non-JDK set depends on all jmods", and test -> main -- but they are not two
 * copies of one algorithm. This one walks {@code SourceSet.dependencies()}, which the Maven side computes per
 * set; the Gradle one builds edges from a tree of {@code Result}s and knows about sibling projects reached
 * through a variant, runtime-only scoping, and source-project edges, none of which exist here.
 *
 * <p>⛔ SO CHECK BOTH WHEN CHANGING EITHER. What they already disagree on: this one has no notion of
 * {@code runtimeOnly} at all, so a runtime-only library reaches every Maven source set, and only the Gradle twin
 * keeps it off a compile class path.
 *
 * <p>Merging them would mean putting the Gradle model on the Maven path, and there is no Maven corpus that would
 * measure the result: {@code maddi-mvnplugin} has no tests, and langchain4j's checked-in configuration predates
 * build units. Until one exists, two honest implementations beat one unmeasured one.
 */
public class ComputeDependencies {
   private final Consumer<String> log;

    public ComputeDependencies(Consumer<String> log) {
        this.log = log;
    }

    public record SourceSetDependencies(String mainSourceSetName, Map<String, SourceSet> sourceSetsByName) {
    }

    public G<String> go(SourceSetDependencies result) {
        G.Builder<String> builder = new ImmutableGraph.Builder<>(Long::sum);

        // jmods are common
        Set<String> jmods = new HashSet<>();
        for (SourceSet sourceSet : result.sourceSetsByName().values()) {
            if (sourceSet.partOfJdk()) {
                String jmod = sourceSet.name();
                Set<String> dependencies = JavaModules.jmodDependency(jmod);
                log.accept("Adding JMOD " + jmod + " -> " + dependencies);
                builder.add(jmod, dependencies);
                jmods.add(jmod);
            }
        }

        HashSet<String> seen = new HashSet<>();

        log.accept(" -- now recursing for source sets");
        List<String> mainSourceSets = new ArrayList<>();
        List<String> testSourceSets = new ArrayList<>();
        for (SourceSet sourceSet : result.sourceSetsByName().values()) {
            String name = sourceSet.name();

            recursionForSourceSets(builder, sourceSet, seen, jmods, 1);
            if (!sourceSet.externalLibrary()) {
                if (sourceSet.test()) {
                    testSourceSets.add(name);
                } else {
                    mainSourceSets.add(name);
                }
            }
        }
        for (String testName : testSourceSets) {
            log.accept("ADDING SRC MAIN->TEST " + testName + " -> " + mainSourceSets);
            builder.add(testName, mainSourceSets);
        }

        return builder.build();
    }

    private void recursionForSourceSets(G.Builder<String> builder, SourceSet sourceSet,
                                        Set<String> seen, Set<String> jmods, int indent) {
        if (!seen.add(sourceSet.name())) return;
        log.accept("@@".repeat(indent) + " enter recursion for " + sourceSet.name());

        String name = sourceSet.name();
        if (!sourceSet.partOfJdk()) {
            builder.add(name, jmods);
        }
        if (sourceSet.dependencies() != null) {
            for (SourceSet dep : sourceSet.dependencies()) {
                builder.add(name, List.of(dep.name()));
                recursionForSourceSets(builder, dep, seen, jmods, indent + 1);
            }
        }
    }
}
