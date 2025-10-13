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

package org.e2immu.analyzer.modification.prepwork.callgraph;

import org.e2immu.language.cst.api.element.ModuleInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.V;
import org.e2immu.util.internal.graph.op.Linearize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

}
