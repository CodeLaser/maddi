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

package org.e2immu.util.internal.graph.op;

import org.e2immu.util.internal.graph.EdgeIterator;
import org.e2immu.util.internal.graph.EdgePrinter;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.V;
import org.e2immu.util.internal.graph.util.TimedLogger;
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
