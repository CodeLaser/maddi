package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.LinkedVariables;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.util.internal.graph.G;
import org.e2immu.util.internal.graph.ImmutableGraph;
import org.e2immu.util.internal.graph.V;

import java.util.Map;
import java.util.stream.Stream;

public class Expand {

    public static Links completion(Map<Variable, Links> linkedVariables, Variable variable) {
        return linkedVariables.getOrDefault(variable, LinksImpl.EMPTY);
    }

    public static Links connect(LocalVariable lv, Links links) {
        return links; // FIXME more!
    }

    public static Links expandReturnValue(ReturnVariable returnVariable, Links links, LinkedVariables extra, VariableData vd) {
        Links.Builder rvBuilder = new LinksImpl.Builder(returnVariable);
        rvBuilder.add(LinkNature.IS_IDENTICAL_TO, links.primary());
        G<Variable> graph = makeGraph(links, extra);
        Map<V<Variable>, Long> all = graph.edges(new V<>(links.primary()));
        if (all != null) {
            for (Map.Entry<V<Variable>, Long> entry : all.entrySet()) {
                Variable to = entry.getKey().t();
                if (entry.getValue() >= 0L && !containsLocal(to)) {
                    rvBuilder.add(LinkNature.values()[(int) (long) entry.getValue()], to);
                }
            }
        }
        return rvBuilder.build();
    }

    private static G<Variable> makeGraph(Links links, LinkedVariables extra) {
        G.Builder<Variable> builder = new ImmutableGraph.Builder<>(LinkNature::combineLongs);
        Stream<Link> stream = Stream.concat(links.links().stream(),
                extra.map().values().stream().flatMap(l -> l.links().stream()));
        stream.forEach(link -> builder.mergeEdge(link.from(), link.to(), link.linkNature().longValue()));

        // transitive closure

        return builder.build();
    }

    private static boolean containsLocal(Variable variable) {
        return variable.variableStreamDescend().anyMatch(v -> v instanceof LocalVariable);
    }
}
