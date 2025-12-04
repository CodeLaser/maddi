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

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;

public class Expand {

    public static Links completion(Map<Variable, Links> linkedVariables, Variable variable) {
        return linkedVariables.getOrDefault(variable, LinksImpl.EMPTY);
    }

    public static Links connect(LocalVariable lv, Links links) {
        Links.Builder builder = new LinksImpl.Builder(lv);
        for (Link link : links) {
            builder.add(link.linkNature(), link.to());
        }
        return builder.build();
    }

    public static Links expandReturnValue(ReturnVariable returnVariable, Links links, LinkedVariables extra, VariableData vd) {
        Links.Builder rvBuilder = new LinksImpl.Builder(returnVariable);
        if (!containsLocal(links.primary())) {
            rvBuilder.add(LinkNature.IS_IDENTICAL_TO, links.primary());
        }
        G<Variable> graph = makeGraph(links, extra, vd);
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

    private static G<Variable> makeGraph(Links links, LinkedVariables extra, VariableData vd) {
        G.Builder<Variable> builder = new ImmutableGraph.Builder<>(LinkNature::combineLongs);
        Stream<Link> stream = Stream.concat(links.links().stream(),
                extra.map().values().stream().flatMap(l -> l.links().stream()));
        stream.forEach(link -> builder.mergeEdge(link.from(), link.to(), link.linkNature().longValue()));

        vd.variableInfoStream().forEach(vi -> {
            Links vLinks = vi.analysis().getOrNull(LINKS, LinksImpl.class);
            if (vLinks != null) {
                vLinks.links().forEach(l -> builder.mergeEdge(l.from(), l.to(), l.linkNature().longValue()));
            }
        });

        return builder.build();
    }

    private static boolean containsLocal(Variable variable) {
        return variable.variableStreamDescend().anyMatch(v -> v instanceof LocalVariable);
    }
}
