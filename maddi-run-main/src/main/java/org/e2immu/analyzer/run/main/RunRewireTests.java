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

package org.e2immu.analyzer.run.main;

import org.e2immu.analyzer.modification.prepwork.callgraph.ComputeCallGraph;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.e2immu.language.inspection.api.parser.ParseResult;
import org.e2immu.language.inspection.api.resource.InputConfiguration;
import org.e2immu.language.inspection.integration.JavaInspectorImpl;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.ImmutableGraph;
import org.e2immu.util.internal.graph.V;
import org.e2immu.util.internal.graph.op.Common;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.api.integration.JavaInspector.InvalidationState.*;

public class RunRewireTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunRewireTests.class);
    private static final int ITERATIONS = 200;
    public static final long SEED = 15;

    private final InputConfiguration inputConfiguration;
    private final JavaInspector javaInspector;
    private final ParseResult parseResult;
    private final G<Info> initialGraph;

    public RunRewireTests(InputConfiguration inputConfiguration,
                          JavaInspector javaInspector,
                          ParseResult parseResult,
                          G<Info> graph) {
        this.inputConfiguration = inputConfiguration;
        this.javaInspector = javaInspector;
        this.parseResult = parseResult;
        this.initialGraph = graph;
    }

    public void go() {
        Random random = new Random(SEED);
        List<TypeInfo> list = parseResult.primaryTypes().stream().sorted(Comparator.comparing(TypeInfo::fullyQualifiedName)).toList();
        G<Info> infoGraph = initialGraph;
        for (int i = 0; i < ITERATIONS; ++i) {
            LOGGER.info("Compute primary type use graph, iteration {}", i);
            TypeInfo typeToRewire = list.get(random.nextInt(list.size()));
            Path path = Path.of(typeToRewire.compilationUnit().uri().getSchemeSpecificPart());
            Path absolutePath = path.toAbsolutePath();
            LOGGER.info("Modifying {}", absolutePath);
            boolean stop = false;
            try {
                String content = Files.readString(path);
                try {
                    String updatedContent = "// some comment\n\n" + content;
                    // this will trigger a change of fingerprint
                    stop |= write(path, updatedContent);
                    if (!stop) {
                        infoGraph = reloadSources(typeToRewire, infoGraph);
                    }
                } finally {
                    LOGGER.info("Restoring {}", absolutePath);
                    stop |= write(path, content);
                }
            } catch (IOException ioe) {
                LOGGER.error("Could not read {}", absolutePath, ioe);
                stop = true;
            }
            if (stop) return;
        }
    }

    private G<Info> reloadSources(TypeInfo pt, G<Info> infoGraph) throws IOException {
        LOGGER.info("Calling javaInspector.reloadSources");
        JavaInspector.ReloadResult rr = javaInspector.reloadSources(inputConfiguration, Map.of());
        assert rr.sourceHasChanged().contains(pt);
        G<TypeInfo> primaryTypeUseGraph = primaryTypeUseGraph(infoGraph);
        Set<TypeInfo> dependentPrimaryTypes = dependent(primaryTypeUseGraph, rr.sourceHasChanged(), rr.sourceHasChanged());
        assert Collections.disjoint(dependentPrimaryTypes, rr.sourceHasChanged());
        LOGGER.info("{} has {} dependent types", pt, dependentPrimaryTypes.size());
        JavaInspector.Invalidated invalidated = ti -> {
            if (rr.sourceHasChanged().contains(ti)) return INVALID;
            if (dependentPrimaryTypes.contains(ti)) return REWIRE;
            return UNCHANGED;
        };
        JavaInspector.ParseOptions parseOptions = new JavaInspectorImpl.ParseOptionsBuilder()
                .setLombok(inputConfiguration.containsLombok())
                .setDetailedSources(true)
                .setFailFast(true)
                .setInvalidated(invalidated)
                .setParallel(true)
                .build();
        LOGGER.info("Reparse");
        ParseResult parseResult1 = javaInspector.parse(parseOptions).parseResult();
        LOGGER.info("Recompute call graph");
        ComputeCallGraph ccg = new ComputeCallGraph(javaInspector.runtime(), parseResult1,
                _ -> false);
        return ccg.go().graph();
    }

    private static G<TypeInfo> primaryTypeUseGraph(G<Info> infoGraph) {
        G.Builder<TypeInfo> typeUseBuilder = new ImmutableGraph.Builder<>(Long::sum);
        infoGraph.edgeStream().forEach(e -> {
            TypeInfo ptFrom = e.from().t().typeInfo().primaryType();
            TypeInfo ptTo = e.to().t().typeInfo().primaryType();
            if (ptFrom != ptTo) typeUseBuilder.mergeEdge(ptTo, ptFrom, 1L);
        });
        return typeUseBuilder.build();
    }

    private static Set<TypeInfo> dependent(G<TypeInfo> primaryTypeUseGraph, Set<TypeInfo> pts, Set<TypeInfo> changed) {
        List<V<TypeInfo>> list = pts.stream().map(V::new).toList();
        return Common.follow(primaryTypeUseGraph, list, false)
                .stream().map(V::t)
                .filter(t -> !changed.contains(t))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static boolean write(Path path, String content) {
        try {
            Files.writeString(path, content);
            return false;
        } catch (IOException ioe) {
            LOGGER.error("Could not write {}", path.toAbsolutePath(), ioe);
            return true;
        }
    }
}
