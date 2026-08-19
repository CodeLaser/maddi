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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        for (String name : linearization) {
            Map<V<String>, Long> edges = graph.edges(new V<>(name));
            List<SourceSet> dependencies = edges == null ? List.of() : edges.keySet().stream()
                    .map(v -> allByName.get(v.t()))
                    .filter(Objects::nonNull)
                    .toList();
            SourceSet sourceSet = allByName.get(name);
            if (sourceSet == null) {
                log.accept("Don't know source set " + name);
            } else {
                SourceSet withDependencies = sourceSet.withDependencies(dependencies);
                if (withDependencies.externalLibrary()) builder.addClassPathParts(withDependencies);
                else builder.addSourceSets(withDependencies);
                emitted.add(name);
            }
        }
        for (SourceSet javaModule : javaModules) {
            if (emitted.add(javaModule.name())) builder.addClassPathParts(javaModule);
        }
    }
}
