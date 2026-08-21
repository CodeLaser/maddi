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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
            Set<String> edgeNames = sourceSetClosure(graph, name, allByName);
            List<SourceSet> dependencies = new ArrayList<>();
            if (!edgeNames.isEmpty()) {
                for (String edgeName : edgeNames) {
                    SourceSet dependency = allByName.get(edgeName);
                    // ⛔ THE `filter(Objects::nonNull)` THIS REPLACES WAS THE SAME SILENCE ONE LEVEL EARLIER.
                    // An edge the graph carries and `allByName` cannot name is a set of packages that will not
                    // resolve, and dropping it here means `ConfigurationChecks.checkEveryDependencyResolves`
                    // below is asked about a graph the drop has already made consistent. The neighbouring
                    // `sourceSet == null` branch had said so since it was written; this one had not.
                    if (dependency == null) log.accept("Don't know dependency " + edgeName + " of " + name);
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

    /**
     * A source set's direct edges, <b>plus every source set reachable from them</b>.
     *
     * <h2>⛔⛔ {@code dependencies()} IS A CLASS PATH, AND AN ADJACENCY LIST IS NOT ONE</h2>
     * {@link SourceSet#dependencies()} promises <em>"the source sets that must be resolved before this one can be
     * inspected or compiled"</em>, ordered so that <em>"earlier entries take priority over later ones"</em> — that
     * is javac's class path, and a class path is a CLOSURE. This method used to hand back the graph's adjacency
     * list, so a source set reachable only through another source set was on no path at all, and
     * {@code JavaInspectorImpl} — which builds javac's class path from this list, because <em>"javac knows nothing
     * of the CST and resolves every reference into them from CLASS FILES"</em> — never saw it.
     *
     * <h2>⛔⛔ WHY A CLOSURE IS RIGHT HERE AND WOULD BE WRONG IN {@code --compile-log}</h2>
     * A graph edge from this producer means <em>"Gradle project A depends on project B"</em>, and Gradle's real
     * compile class path for A contains B <b>plus B's own api closure</b>. The adjacency list therefore
     * <em>understates</em> what javac was given. MEASURED on OpenSearch (2026-08-21): 2 of 21 source sets not
     * closed, 10 missing edges — {@code framework/main} 7 direct and 9 missing, including
     * {@code opensearch-core/main}; {@code server/main} 10 direct and 1 missing. The failure was three layers
     * downstream: 86 unresolved symbols in one file, javac degrading {@code ShardId.id} into a TYPE reference and
     * inventing a {@code ClassSymbol} with no class file, then maddi's scanner registering that "member type" on
     * an owner another source set had already committed — 12 errors, 11 compilation units, 4 type names, and none
     * of them naming the class path. With the closure: <b>198 dropped compilation units → 7, and a parse that
     * produces a model at all.</b>
     *
     * <p>⛔⛔ <b>AND THE ARGUMENT IS NOT "THE OTHER PRODUCER IS CLOSED", THOUGH ONE CORPUS SAID SO.</b>
     * OpenSearch through {@code --compile-log} is closed 20 of 20, and I nearly wrote that down as the reference.
     * A census over every registered configuration says otherwise: <b>es-phase3 139 of 348 source sets not
     * closed, pulsar 40 of 90, timefold 3 of 65</b> — all {@code --compile-log}. That is <b>not</b> a defect
     * there: that producer records the {@code -classpath} javac was actually handed, which is ground truth, and
     * a set javac compiled without Y on its path genuinely does not need Y. ⇒ Do not "repair" it there.
     *
     * <p>⚠ The closure OVER-approximates for the same reason the adjacency list under-approximated: Gradle's
     * {@code implementation} dependencies are not transitive on a consumer's compile class path. That trade is
     * already made one file away, in {@code ComputeDependencies} — "a wider class path costs a type resolving
     * that a stricter build would have rejected, which is not what this configuration is judged on".
     *
     * <h2>⚠ ONLY THE SOURCE-SET HALF IS CLOSED HERE, AND THAT IS NOT A SHORTCUT</h2>
     * The external half already is: both {@code ComputeDependencies} wire <b>every</b> source set to every
     * external library and every jmod, so a library reachable transitively is on the list directly. Closing over
     * externals as well would add nothing and would inflate every configuration by the size of its class path.
     * The measured gap is source-set edges, and this closes exactly it.
     */
    static Set<String> sourceSetClosure(G<String> graph, String name, Map<String, SourceSet> allByName) {
        Map<V<String>, Long> direct = graph.edges(new V<>(name));
        if (direct == null) return Set.of();
        Set<String> result = new LinkedHashSet<>();
        Deque<String> todo = new ArrayDeque<>();
        for (V<String> edge : direct.keySet()) {
            if (result.add(edge.t())) todo.add(edge.t());
        }
        while (!todo.isEmpty()) {
            String next = todo.poll();
            SourceSet set = allByName.get(next);
            // ⚠ Do not walk THROUGH an external library or a jmod: its own edges are the jmod graph, and following
            // them turns every source set's list into the whole platform. An unknown name is not walked either --
            // it is reported by the caller, which is the one place that has been told to report it.
            if (set == null || set.externalLibrary()) continue;
            Map<V<String>, Long> edges = graph.edges(new V<>(next));
            if (edges == null) continue;
            for (V<String> edge : edges.keySet()) {
                SourceSet target = allByName.get(edge.t());
                // ⛔⛔ ONLY A SOURCE SET IS ADDED HERE, AND THE FIRST VERSION OF THIS METHOD ADDED LIBRARIES TOO
                // -- which quietly undid a deliberate scoping decision one file away. `ComputeDependencies`
                // gives a SIBLING every library the analysed project has, test-scope included, because "a
                // sibling's class path is not the consumer's" and denying it its own dependencies costs real
                // compilation units; the analysed project's own main sets get only the libraries that reach it
                // outside test scope. Walking through a sibling and collecting what IT has hands those
                // test-scope libraries straight back to the consumer's main class path.
                // MEASURED, OpenSearch, the analysed project's own main set: 55 dependencies -> 110, every one
                // of the 55 additions an external library and not one of them a source set.
                if (target == null || target.externalLibrary()) continue;
                // ⛔ A CYCLE AMONG SOURCE SETS IS NOT IMPOSSIBLE, and `result` is what stops this walking one.
                // Linearize tolerates them; this must too, and it must not add the set to its own class path.
                if (!edge.t().equals(name) && result.add(edge.t())) todo.add(edge.t());
            }
        }
        return result;
    }
}
