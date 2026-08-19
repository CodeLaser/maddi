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
import io.codelaser.maddi.graph.V;
import io.codelaser.maddi.graph.op.Linearize;
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Walking a dependency graph out into an {@link InputConfiguration}: the last step of both build plugins, and
 * the same twenty lines in each.
 */
public class PluginInputConfiguration {

    /**
     * Emit every source set and class path part in dependency order.
     *
     * <p>⛔ THE COPIES HAD DIVERGED, AND ONLY ONE CARRIED THE FIX. The Maven plugin ended with a pass that forces
     * every requested JDK module onto the class path whatever the graph says; the Gradle plugin had no
     * equivalent, and got away with it only because <em>its</em> {@code ComputeDependencies} happens to wire every
     * source set to every jmod. Two remedies for one problem, in two files, neither naming the other -- and the
     * one that depends on a detail of a different class is the one that breaks silently when that class changes.
     * Stated once here, it protects both.
     *
     * @param graph        source-set name -> the names it depends on
     * @param allByName    every source set and class path part the graph can name
     * @param javaModules  the requested JDK modules, which must reach the class path whether or not the graph
     *                     reaches them: a non-modular corpus sees the whole platform, and the graph typically
     *                     only gets as far as {@code java.base}
     */
    public static void emit(InputConfiguration.Builder builder,
                            G<String> graph,
                            Map<String, SourceSet> allByName,
                            List<SourceSet> javaModules,
                            Consumer<String> log) {
        List<String> linearization = Linearize.linearize(graph).asList(String::compareToIgnoreCase);
        log.accept("Linearization:\n  " + String.join("\n  ", linearization) + "\n");
        Set<String> emitted = new HashSet<>();
        List<SourceSet> emittedSourceSets = new ArrayList<>();
        List<SourceSet> emittedClassPathParts = new ArrayList<>();
        for (String name : linearization) {
            Map<V<String>, Long> edges = graph.edges(new V<>(name));
            List<SourceSet> dependencies = new ArrayList<>();
            if (edges != null) {
                for (V<String> edge : edges.keySet()) {
                    SourceSet dependency = allByName.get(edge.t());
                    // ⛔ THE `filter(Objects::nonNull)` THIS REPLACES WAS THE SAME SILENCE ONE LEVEL EARLIER.
                    // An edge the graph carries and `allByName` cannot name is a set of packages that will not
                    // resolve, and dropping it here means `ConfigurationChecks.checkEveryDependencyResolves`
                    // below is asked about a graph the drop has already made consistent. The neighbouring
                    // `sourceSet == null` branch had said so since it was written; this one had not.
                    if (dependency == null) log.accept("Don't know dependency " + edge.t() + " of " + name);
                    else dependencies.add(dependency);
                }
                // ⚠ SORTED, BECAUSE A SERIALIZED CONFIGURATION THAT SHUFFLES DEFEATS THE ONE VERIFICATION THIS
                // CAMPAIGN LEANS ON. `graph.edges()` hands back a map whose iteration order is an artefact of
                // insertion and capacity, not of the graph, so an unrelated edit reorders a dependency list and a
                // byte-comparison across a refactor -- the strongest evidence available that a change is
                // behaviour-preserving -- reports a difference that means nothing. Observed exactly that here on
                // 2026-08-19: `java.se`'s 11 dependencies, same set, different order, across a change that
                // touched neither.
                // ⚠ The same guard, for the same reason, is already stated in CompileListToInputConfiguration
                // over the jmod parts; the plugin path never had it.
                dependencies.sort(java.util.Comparator.comparing(SourceSet::name));
            }
            SourceSet sourceSet = allByName.get(name);
            if (sourceSet == null) {
                log.accept("Don't know source set " + name);
            } else {
                SourceSet withDependencies = sourceSet.withDependencies(dependencies);
                if (withDependencies.externalLibrary()) emittedClassPathParts.add(withDependencies);
                else emittedSourceSets.add(withDependencies);
                emitted.add(name);
            }
        }
        for (SourceSet javaModule : javaModules) {
            if (emitted.add(javaModule.name())) emittedClassPathParts.add(javaModule);
        }
        // ⛔⛔⛔ THE TWO REPAIRS `--compile-log` MAKES DO NOT BELONG HERE, AND THIS IS WHERE SOMEBODY WILL TRY
        // TO PUT THEM. Both were wired in on 2026-08-19 and both were taken straight back out.
        //
        // THE MEASUREMENT, `TestAnalyzerPluginFunctional#configurationCacheCompatible`, three runs:
        //     checks only ............................ PASSES
        //     + TypeUseAnnotationClosure ............. FAILS
        //     + AnnotationProcessorOutput ............ FAILS
        //   "configuration cache cannot be reused because the file system entry 'build/classes/java/main'
        //    has been created."
        //
        // ⛔ THE CACHE IS THE SYMPTOM; THE CAUSE IS *WHEN* THIS RUNS. `AnalyzerPlugin` computes the whole
        // configuration inside a `project.provider(...)` resolved at CONFIGURATION/STORE time -- it says so in
        // as many words -- and both repairs read the file system. Gradle records a configuration-time read as an
        // input to the entry, so the moment compilation creates the directory the entry is discarded.
        //
        // ⛔⛔ AND EVEN WITH THE CACHE OFF THEY WOULD ANSWER NOTHING, WHICH IS THE REAL POINT. Both ask about a
        // source set's COMPILED DESTINATION, and a plugin computes this configuration BEFORE the compile tasks
        // it depends on have run (the same sentence `PluginSourceSets.classPathUri` already carries). There are
        // no classes there to read.
        //   ⚠ On Maven they LOOKED like they worked -- langchain4j-core and activemq-broker really did yield
        //   generated-class libraries. That is because those corpora are built with `mvn install` first, so the
        //   mojo was reading the PREVIOUS build's `target/classes`. A repair whose input is last time's output
        //   is not a repair, it is a stale read that happens to be right.
        //
        // ⭐ WHAT IS DIFFERENT ABOUT `--compile-log`: it parses a log written AFTER the compilation, so the
        // destination it reads is the output of the very compile it is describing. That guarantee is the whole
        // reason the repairs work there, and no build plugin has it at configuration time.
        //
        // ▶ IF THIS IS EVER WORTH DOING, IT BELONGS AT EXECUTION TIME -- inside the task action or the forked
        // worker, after the compile tasks it depends on -- not here. That is a design change, not a call site.
        List<SourceSet> sourceSets = emittedSourceSets;

        // ⛔⛔ THE CHECKS RUN BEFORE THE BUILDER SEES ANYTHING. They are pure -- no file system, no side
        // effects -- which is exactly what the repairs above are not, and why these survive here and those did
        // not. They were private to `--compile-log` until 2026-08-19, so the two producers that had
        // never met a corpus were also the two that verified nothing.
        // The jmods are already in `emittedClassPathParts` here, so there is nothing extra to declare known.
        ConfigurationChecks.check(sourceSets, emittedClassPathParts, Set.of());
        sourceSets.forEach(builder::addSourceSets);
        emittedClassPathParts.forEach(builder::addClassPathParts);
    }
}
