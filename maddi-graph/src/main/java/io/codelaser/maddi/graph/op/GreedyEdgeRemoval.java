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

package io.codelaser.maddi.graph.op;

import io.codelaser.maddi.graph.EdgeIterator;
import io.codelaser.maddi.graph.EdgePrinter;
import io.codelaser.maddi.graph.G;
import io.codelaser.maddi.graph.V;
import io.codelaser.maddi.graph.util.TimedLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

public class GreedyEdgeRemoval<T> implements BreakCycles.ActionComputer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GreedyEdgeRemoval.class);

    private final EdgePrinter<T> edgePrinter;
    private final EdgeIterator<T> edgeIterator;
    private final TimedLogger timedLogger;

    public GreedyEdgeRemoval() {
        this(Object::toString, g -> g.edgeIterator(Long::compareTo, null), null);
    }

    public GreedyEdgeRemoval(EdgePrinter<T> edgePrinter, EdgeIterator<T> edgeIterator, TimedLogger timedLogger) {
        this.edgePrinter = edgePrinter;
        this.edgeIterator = edgeIterator;
        this.timedLogger = timedLogger;
    }

    @Override
    public BreakCycles.Action<T> compute(G<T> inputGraph, Cycle<T> cycle) {
        G<T> g = inputGraph.subGraph(cycle.vertices());

        int bestQuality = cycle.size();
        assert bestQuality > 0;
        G<T> bestSubGraph = null;
        Map<V<T>, Map<V<T>, Long>> bestEdgesToRemove = null;
        int count = 0;
        Iterator<Map<V<T>, Map<V<T>, Long>>> iterator = edgeIterator.iterator(g);
        while (iterator.hasNext() && bestQuality > 0) {
            Map<V<T>, Map<V<T>, Long>> edgesToRemove = iterator.next();
            G<T> withoutEdges = g.withFewerEdgesMap(edgesToRemove);
            int quality = Linearize.qualityBasedOnTotalCluster(withoutEdges);
            if (quality < bestQuality) {
                bestSubGraph = withoutEdges;
                bestQuality = quality;
                bestEdgesToRemove = edgesToRemove;
            }
            ++count;
            if (timedLogger != null) {
                timedLogger.info("Edge removal, done {}, best {}", count, bestQuality);
            }
        }
        LOGGER.info("Best choice for greedy edge removal is {}, quality now {}",
                edgePrinter.print(bestEdgesToRemove), bestQuality);
        if (bestQuality < cycle.size()) {
            G<T> finalGraph = bestSubGraph.subGraph(cycle.vertices());
            BreakCycles.EdgeRemoval<T> info = new BreakCycles.EdgeRemoval<>(bestEdgesToRemove);
            return new BreakCycles.Action<T>() {
                @Override
                public G<T> apply() {
                    return finalGraph;
                }

                @Override
                public BreakCycles.ActionInfo info() {
                    return info;
                }
            };
        }
        LOGGER.info("No edge found that improves quality; keeping cycle of size {}", cycle.size());
        return null; // must be a group, we cannot break the cycle
    }
}
