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
