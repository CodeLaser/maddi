/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.callgraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.codelaser.maddi.cst.api.element.ModuleInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.V;
import io.codelaser.maddi.graph.op.Linearize;

/*
given the call graph, compute the linearization, and all supporting information to deal with cycles.

TODO this one will become more complex, giving priority to methods with more modification within each call cycle.
 */
public class ComputeAnalysisOrder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeAnalysisOrder.class);

    public List<Info> go(G<Info> callGraph) {
        return go(callGraph, false);
    }

    public List<Info> go(G<Info> callGraph, boolean parallel) {
        Stream<V<Info>> stream = parallel ? callGraph.vertices().parallelStream() : callGraph.vertices().stream();
        Set<V<Info>> subSet = stream
                .filter(v -> !(v.t() instanceof ModuleInfo))
                .filter(v -> !v.t().typeInfo().compilationUnit().externalLibrary())
                .collect(Collectors.toUnmodifiableSet());
        LOGGER.info("Computed vertex subset");
        G<Info> subGraph = callGraph.subGraph(subSet, l -> l >= ComputeCallGraph.REFERENCES);
        LOGGER.info("Created subgraph, start linearization");
        Linearize.Result<Info> result = Linearize.linearize(subGraph, Linearize.LinearizationMode.ALL);
        return result.asList(Comparator.comparing(Info::fullyQualifiedName));
    }

    /**
     * Dependency WAVES for a strata-parallel first iteration: each wave's units are mutually edge-free in
     * the (filtered) call graph, so they can run concurrently; a unit is a single element, except for the
     * remaining call cycles, where a whole connected cycle group is one sequential unit (its members recurse
     * into each other, and must stay on one thread). Waves in dependency order: linearized strata (callees
     * first), then the cycle groups, then the periphery attached to cycles (its hierarchy levels are
     * dependency-ordered). Same subgraph and element set as {@link #go(G)}; FQN-sorted for determinism.
     */
    /*
     A cycle group larger than this is cut into chunks that run concurrently. One sequential unit was right for the
     cycles of a normal code base; closed-core has a cycle of some 10K of its 25K types, and its first iteration
     ran on one thread for hours while the others idled. The link computer's lock-free mode (lockComputeDisabled)
     already handles a member whose callee another thread has not linked yet: it computes it on demand and writes
     it TolerantWrite-guarded, and RecursionPrevention ends the recursion; the later passes, parallel anyway, reach
     the fixpoint. CYCLESPLIT=<n> sets the size; gate NOCYCLESPLIT keeps every cycle one unit.
     */
    static final int CYCLE_SPLIT = System.getenv("NOCYCLESPLIT") != null ? Integer.MAX_VALUE
            : System.getenv("CYCLESPLIT") == null ? 512 : Integer.parseInt(System.getenv("CYCLESPLIT"));

    /** A cycle group in chunks of about {@link #CYCLE_SPLIT} elements; a primary type's elements stay together. */
    static List<List<Info>> splitLargeCycle(List<Info> unit) {
        if (unit.size() <= CYCLE_SPLIT) return List.of(unit);
        List<List<Info>> chunks = chunks(unit, CYCLE_SPLIT, info -> info.typeInfo().primaryType());
        LOGGER.info("Cycle group of {} elements split into {} chunks for the parallel first iteration", unit.size(),
                chunks.size());
        return chunks;
    }

    /**
     * Consecutive chunks of at least {@code size} elements (the last one may be smaller), in the list's order; a new
     * chunk only starts where the group key changes, so elements with the same key in a row are never split.
     */
    static <T> List<List<T>> chunks(List<T> list, int size, Function<T, Object> groupKey) {
        List<List<T>> chunks = new ArrayList<>();
        List<T> current = new ArrayList<>();
        Object currentKey = null;
        for (T t : list) {
            Object key = groupKey.apply(t);
            if (current.size() >= size && !key.equals(currentKey)) {
                chunks.add(List.copyOf(current));
                current = new ArrayList<>();
            }
            current.add(t);
            currentKey = key;
        }
        if (!current.isEmpty()) chunks.add(List.copyOf(current));
        return chunks;
    }

    public static List<List<List<Info>>> waves(G<Info> callGraph) {
        Set<V<Info>> subSet = callGraph.vertices().stream()
                .filter(v -> !(v.t() instanceof ModuleInfo))
                .filter(v -> !v.t().typeInfo().compilationUnit().externalLibrary())
                .collect(Collectors.toUnmodifiableSet());
        G<Info> subGraph = callGraph.subGraph(subSet, l -> l >= ComputeCallGraph.REFERENCES);
        Linearize.Result<Info> result = Linearize.linearize(subGraph, Linearize.LinearizationMode.ALL);
        Comparator<Info> cmp = Comparator.comparing(Info::fullyQualifiedName);
        List<List<List<Info>>> waves = new ArrayList<>();
        for (Set<V<Info>> stratum : result.linearized().list()) {
            if (!stratum.isEmpty()) {
                waves.add(stratum.stream().map(V::t).sorted(cmp).map(i -> (List<Info>) List.of(i)).toList());
            }
        }
        List<List<Info>> cycleUnits = result.remainingCycles().cycles().stream()
                .map(c -> (List<Info>) c.vertices().stream().map(V::t).sorted(cmp).toList())
                .filter(u -> !u.isEmpty())
                .sorted(Comparator.comparing(u -> u.getFirst().fullyQualifiedName()))
                .flatMap(u -> splitLargeCycle(u).stream())
                .toList();
        if (!cycleUnits.isEmpty()) waves.add(cycleUnits);
        for (Set<V<Info>> level : result.attachedToCycles().list()) {
            if (!level.isEmpty()) {
                waves.add(level.stream().map(V::t).sorted(cmp).map(i -> (List<Info>) List.of(i)).toList());
            }
        }
        return waves;
    }

}
