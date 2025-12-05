package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;

public record ExpandParameterLinks(Runtime runtime) {
    public List<Links> go(MethodInfo methodInfo, VariableData vd) {

        Map<Variable, Map<Variable, LinkNature>> graph = makeReverseGraph(vd);

        List<Links> linksPerParameter = new ArrayList<>(methodInfo.parameters().size());
        for (ParameterInfo pi : methodInfo.parameters()) {
            List<Variable> fromList = Stream.concat(Stream.of(pi), graph.keySet().stream()
                    .filter(v -> LinksImpl.primary(v).equals(pi))).toList();
            Links.Builder piBuilder = new LinksImpl.Builder(pi);
            for (Variable from : fromList) {
                if (graph.containsKey(from)) {
                    Map<Variable, LinkNature> all = ExpandReturnValueLinks.bestPath(graph, from);
                    for (Map.Entry<Variable, LinkNature> entry : all.entrySet()) {
                        Variable to = entry.getKey();
                        if (entry.getValue() != LinkNature.NONE && ExpandReturnValueLinks.containsNoLocalVariable(to)) {
                            piBuilder.add(from, entry.getValue(), to);
                        }
                    }
                } // otherwise, the best path algorithm makes no sense
            }
            linksPerParameter.add(piBuilder.build());
        }

        return linksPerParameter;
    }

    private static Map<Variable, Map<Variable, LinkNature>> makeReverseGraph(VariableData vd) {
        Map<Variable, Map<Variable, LinkNature>> graph = new HashMap<>();
        vd.variableInfoStream().forEach(vi -> {
            Links vLinks = vi.analysis().getOrNull(LINKS, LinksImpl.class);
            if (vLinks != null) {
                vLinks.links().forEach(l -> ExpandReturnValueLinks.mergeEdge(graph, l.to(),
                        l.linkNature().reverse(), l.from()));
            }
        });
        return graph;
    }
}
