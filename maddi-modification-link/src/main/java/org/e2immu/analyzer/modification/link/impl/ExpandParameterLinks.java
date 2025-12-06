package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;

public record ExpandParameterLinks(Runtime runtime) {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExpandParameterLinks.class);

    public List<Links> go(MethodInfo methodInfo, VariableData vd) {

        Map<Variable, Map<Variable, LinkNature>> graph = makeBiDirectionalGraph(vd);
        LOGGER.debug("Reverse graph: {}", graph);

        List<Links> linksPerParameter = new ArrayList<>(methodInfo.parameters().size());
        for (ParameterInfo pi : methodInfo.parameters()) {
            Set<Variable> fromSet = Stream.concat(Stream.of(pi), graph.keySet().stream()
                    .filter(v -> LinksImpl.primary(v).equals(pi))).collect(Collectors.toUnmodifiableSet());
            Links.Builder piBuilder = new LinksImpl.Builder(pi);
            for (Variable from : fromSet) {
                if (graph.containsKey(from)) {
                    Map<Variable, LinkNature> all = ExpandReturnValueLinks.bestPath(graph, from);
                    for (Map.Entry<Variable, LinkNature> entry : all.entrySet()) {
                        Variable to = entry.getKey();
                        if (entry.getValue() != LinkNature.NONE
                            && ExpandReturnValueLinks.containsNoLocalVariable(to)
                            && !LinksImpl.primary(to).equals(pi)) {
                            piBuilder.add(from, entry.getValue(), to);
                        }
                    }
                } // otherwise, the best path algorithm makes no sense
            }
            linksPerParameter.add(piBuilder.build());
        }

        return linksPerParameter;
    }

    private static Map<Variable, Map<Variable, LinkNature>> makeBiDirectionalGraph(VariableData vd) {
        Map<Variable, Map<Variable, LinkNature>> graph = new HashMap<>();
        vd.variableInfoStream().forEach(vi -> {
            Links vLinks = vi.analysis().getOrNull(LINKS, LinksImpl.class);
            if (vLinks != null) {
                vLinks.links().forEach(l -> {
                    ExpandReturnValueLinks.mergeEdge(graph, l.from(), l.linkNature(), l.to());
                    ExpandReturnValueLinks.mergeEdge(graph, l.to(), l.linkNature().reverse(), l.from());
                });
            }
        });
        return graph;
    }
}
