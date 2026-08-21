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
import io.codelaser.maddi.inspection.api.resource.InputConfiguration;
import io.codelaser.maddi.inspection.resource.InputConfigurationImpl;
import io.codelaser.maddi.inspection.resource.SourceSetImpl;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The last step both build plugins share: walking a dependency graph out into an {@link InputConfiguration}.
 *
 * <p>⚠ <b>THIS IS THE ONLY PLACE EITHER PLUGIN VERIFIES ANYTHING.</b> Until 2026-08-19 the configuration checks
 * were private methods of {@code CompileListToInputConfiguration}, so the two producers that had never been
 * pointed at a corpus were also the two that checked nothing — see {@link ConfigurationChecks}.
 */
public class TestPluginInputConfiguration {

    /**
     * ⛔⛔ AN EDGE THAT NAMES NOTHING WAS DROPPED BY A {@code filter(Objects::nonNull)}, IN SILENCE.
     *
     * <p>It is the same statement as the {@code sourceSet == null} branch beside it, which has always logged —
     * and it matters more, because a dropped dependency is a set of packages that will not resolve in a source
     * set that is otherwise perfectly emitted. It also made {@link ConfigurationChecks} unable to see anything:
     * the drop leaves behind exactly the consistent graph the check is looking for.
     */
    @Test
    public void anEdgeNamingNothingIsReported() {
        Map<String, SourceSet> allByName = new LinkedHashMap<>();
        allByName.put("app/main", sourceSet("app/main"));
        allByName.put("lib.jar", library("lib.jar"));

        G.Builder<String> graph = new ImmutableGraph.Builder<>(Long::sum);
        graph.add("app/main", List.of("lib.jar", "ghost.jar"));

        List<String> log = new ArrayList<>();
        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();
        PluginInputConfiguration.emit(builder, graph.build(), allByName, List.of(), log::add);
        InputConfiguration configuration = builder.build();

        assertTrue(log.stream().anyMatch(l -> l.contains("Don't know dependency ghost.jar of app/main")),
                "the drop must be named, not silent: " + log);
        assertEquals(List.of("lib.jar"),
                configuration.sourceSets().getFirst().dependencies().stream().map(SourceSet::name).toList(),
                "and what is left is the part that does resolve");
    }

    /**
     * ⛔ THE KEY A PRODUCER FILES A SOURCE SET UNDER MUST BE ITS OWN {@code name()}.
     *
     * <p>Both plugins hold that invariant today, and nothing enforced it. If it breaks, the entry is emitted
     * under a name the graph never used, and two entries can then answer to one name — which surfaces as a
     * <b>phantom dependency cycle</b> rather than as anything mentioning a name. That diagnosis is what cost a
     * day on Elasticsearch, and it is now one thrown message instead.
     */
    @Test
    public void aNameThatMeansTwoThingsIsRefused() {
        Map<String, SourceSet> allByName = new LinkedHashMap<>();
        allByName.put("app/main", sourceSet("app/main"));
        // filed under one key, named as another -- so it is emitted as a second "app/main"
        allByName.put("other/main", sourceSet("app/main"));

        G.Builder<String> graph = new ImmutableGraph.Builder<>(Long::sum);
        graph.add("app/main", List.of("other/main"));

        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> PluginInputConfiguration.emit(builder, graph.build(), allByName, List.of(), s -> {
                }));
        assertTrue(thrown.getMessage().contains("app/main"), thrown.getMessage());
    }

    /** The ordinary walk: source sets and libraries separated, dependencies attached, jmods appended. */
    @Test
    public void emitsSourceSetsLibrariesAndJmods() {
        Map<String, SourceSet> allByName = new LinkedHashMap<>();
        allByName.put("app/main", sourceSet("app/main"));
        allByName.put("lib.jar", library("lib.jar"));

        G.Builder<String> graph = new ImmutableGraph.Builder<>(Long::sum);
        graph.add("app/main", List.of("lib.jar"));

        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();
        PluginInputConfiguration.emit(builder, graph.build(), allByName, List.of(JavaModules.jmodSourceSet("java.base")),
                s -> {
                });
        InputConfiguration configuration = builder.build();

        assertEquals(List.of("app/main"), configuration.sourceSets().stream().map(SourceSet::name).toList());
        assertEquals(List.of("lib.jar", "java.base"),
                configuration.classPathParts().stream().map(SourceSet::name).toList());
    }


    /**
     * ⛔⛔ <b>A SOURCE SET REACHABLE ONLY THROUGH ANOTHER SOURCE SET WAS ON NO CLASS PATH AT ALL.</b>
     * {@code dependencies()} is javac's class path — {@code JavaInspectorImpl} builds it from this list, because
     * "javac knows nothing of the CST and resolves every reference into them from CLASS FILES" — and a class path
     * is a CLOSURE, not an adjacency list.
     *
     * <p>THE SHAPE IS OPENSEARCH'S, MEASURED 2026-08-21: {@code framework/main} lists {@code server/main} and not
     * {@code opensearch-core/main}, which {@code server/main} lists. 86 symbols in one file were unresolvable,
     * javac degraded {@code ShardId.id} into a TYPE reference, and maddi's scanner reported an
     * inspection-ordering error three layers downstream of the real cause.
     *
     * <p>⚠ <b>THE ARGUMENT IS NOT "THE OTHER PRODUCER IS CLOSED".</b> The same corpus through
     * {@code --compile-log} is closed 20 of 20, and a census over every registered configuration then said
     * es-phase3 is closed for 209 of 348, pulsar for 50 of 90 — same producer. That is ground truth there, not a
     * gap: it records the {@code -classpath} javac was handed. What makes a closure right HERE is that a graph
     * edge means "project A depends on project B", and Gradle's compile class path for A carries B's api closure
     * with it.
     */
    @Test
    public void aSourceSetReachableOnlyThroughAnotherIsOnTheClassPath() {
        Map<String, SourceSet> allByName = new LinkedHashMap<>();
        allByName.put("framework/main", sourceSet("framework/main"));
        allByName.put("server/main", sourceSet("server/main"));
        allByName.put("core/main", sourceSet("core/main"));

        G.Builder<String> graph = new ImmutableGraph.Builder<>(Long::sum);
        graph.add("framework/main", List.of("server/main"));
        graph.add("server/main", List.of("core/main"));

        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();
        PluginInputConfiguration.emit(builder, graph.build(), allByName, List.of(), s -> {
        });
        InputConfiguration configuration = builder.build();

        Map<String, List<String>> byName = new LinkedHashMap<>();
        configuration.sourceSets().forEach(set ->
                byName.put(set.name(), set.dependencies().stream().map(SourceSet::name).sorted().toList()));
        assertEquals(List.of("core/main", "server/main"), byName.get("framework/main"),
                "core is reachable only through server, and javac still needs its class files");
        assertEquals(List.of("core/main"), byName.get("server/main"));
    }

    /**
     * ⛔⛔ <b>THE CLOSURE CARRIES SOURCE SETS AND NOT LIBRARIES, AND THE FIRST VERSION CARRIED BOTH.</b>
     * {@code ComputeDependencies} gives a SIBLING every library the analysed project has, test scope included,
     * on purpose — "a sibling's class path is not the consumer's", and denying it its own dependencies costs
     * real compilation units. The analysed project's own main sets get only what reaches them outside test
     * scope. Walking through a sibling and collecting what IT has hands those test-scope libraries straight
     * back to the consumer, which is that decision undone from one file away.
     *
     * <p>MEASURED on OpenSearch's analysed main set: <b>55 dependencies became 110</b>, every one of the 55
     * additions an external library and not one of them the source set the repair was for.
     *
     * <p>⚠ Nothing is lost by stopping: the external half of the class path is closed by construction, because
     * both {@code ComputeDependencies} wire every source set to every external library and every jmod.
     */
    @Test
    public void theClosureCarriesSourceSetsAndNotLibraries() {
        Map<String, SourceSet> allByName = new LinkedHashMap<>();
        allByName.put("app/main", sourceSet("app/main"));
        allByName.put("sibling/main", sourceSet("sibling/main"));
        allByName.put("lib.jar", library("lib.jar"));
        allByName.put("siblings-own.jar", library("siblings-own.jar"));

        G.Builder<String> graph = new ImmutableGraph.Builder<>(Long::sum);
        graph.add("app/main", List.of("lib.jar", "sibling/main"));
        graph.add("sibling/main", List.of("siblings-own.jar"));

        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();
        PluginInputConfiguration.emit(builder, graph.build(), allByName, List.of(), s -> {
        });

        Map<String, List<String>> byName = new LinkedHashMap<>();
        builder.build().sourceSets().forEach(set ->
                byName.put(set.name(), set.dependencies().stream().map(SourceSet::name).sorted().toList()));
        assertEquals(List.of("lib.jar", "sibling/main"), byName.get("app/main"),
                "the sibling's OWN library must not arrive on the consumer's class path");
    }

    /** ⛔ And a cycle among source sets terminates, and never puts a set on its own class path. */
    @Test
    public void aCycleAmongSourceSetsTerminates() {
        Map<String, SourceSet> allByName = new LinkedHashMap<>();
        allByName.put("a/main", sourceSet("a/main"));
        allByName.put("b/main", sourceSet("b/main"));

        G.Builder<String> graph = new ImmutableGraph.Builder<>(Long::sum);
        graph.add("a/main", List.of("b/main"));
        graph.add("b/main", List.of("a/main"));

        InputConfigurationImpl.Builder builder = new InputConfigurationImpl.Builder();
        PluginInputConfiguration.emit(builder, graph.build(), allByName, List.of(), s -> {
        });
        InputConfiguration configuration = builder.build();
        configuration.sourceSets().forEach(set -> assertFalse(
                set.dependencies().stream().map(SourceSet::name).toList().contains(set.name()),
                set.name() + " must not be on its own class path"));
    }

    // ------------------------------------------------------------------ fixture

    private static SourceSet sourceSet(String name) {
        return new SourceSetImpl.Builder().setName(name).setUri(URI.create("file:/tmp/" + name + "/")).build();
    }

    private static SourceSet library(String name) {
        return new SourceSetImpl.Builder().setName(name).setUri(URI.create("file:/tmp/" + name))
                .setLibrary(true).setExternalLibrary(true).build();
    }
}
