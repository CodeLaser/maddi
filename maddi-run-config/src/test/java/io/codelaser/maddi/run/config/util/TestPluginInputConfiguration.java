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

    // ------------------------------------------------------------------ fixture

    private static SourceSet sourceSet(String name) {
        return new SourceSetImpl.Builder().setName(name).setUri(URI.create("file:/tmp/" + name + "/")).build();
    }

    private static SourceSet library(String name) {
        return new SourceSetImpl.Builder().setName(name).setUri(URI.create("file:/tmp/" + name))
                .setLibrary(true).setExternalLibrary(true).build();
    }
}
