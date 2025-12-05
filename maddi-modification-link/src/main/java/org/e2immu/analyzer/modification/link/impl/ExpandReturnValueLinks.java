package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.LinkedVariables;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;

public class ExpandReturnValueLinks {

    /*
     Prepares the links of the return value for the outside world:
     - find as many links to fields and parameters
     - remove (intermediate) links to local variables
     */
    public static Links go(ReturnVariable returnVariable, Links links, LinkedVariables extra, VariableData vd) {
        if (links.primary() == null) return LinksImpl.EMPTY;
        Links.Builder rvBuilder = new LinksImpl.Builder(returnVariable);
        if (containsNoLocalVariable(links.primary())) {
            rvBuilder.add(LinkNature.IS_IDENTICAL_TO, links.primary());
        }
        Map<Variable, Map<Variable, LinkNature>> graph = makeGraph(links, extra, vd);
        Map<Variable, LinkNature> all = bestPath(graph, links.primary());//.edges(new V<>(links.primary()));
        if (all != null) {
            for (Map.Entry<Variable, LinkNature> entry : all.entrySet()) {
                Variable to = entry.getKey();
                if (entry.getValue() != LinkNature.NONE && containsNoLocalVariable(to)) {
                    rvBuilder.add(entry.getValue(), to);
                }
            }
        }
        return rvBuilder.build();
    }

    private static Map<Variable, LinkNature> bestPath(Map<Variable, Map<Variable, LinkNature>> graph, Variable start) {
        Map<Variable, Set<LinkNature>> res =
                FixpointPropagationAlgorithm.computePathLabels(s -> graph.getOrDefault(s, Map.of()),
                        graph.keySet(), start, LinkNature.EMPTY, LinkNature::combine);
        return res.entrySet().stream()
                .filter(e -> !start.equals(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        e -> e.getValue().stream().reduce(LinkNature.EMPTY, LinkNature::combine)));
    }

    private static Map<Variable, Map<Variable, LinkNature>> makeGraph(Links links,
                                                                      LinkedVariables extra,
                                                                      VariableData vd) {
        Map<Variable, Map<Variable, LinkNature>> graph = new HashMap<>();
        Stream<Link> stream = Stream.concat(links.links().stream(),
                extra.map().values().stream().flatMap(l -> l.links().stream()));
        stream.forEach(link -> mergeEdge(graph, link.from(), link.linkNature(), link.to()));

        vd.variableInfoStream().forEach(vi -> {
            Links vLinks = vi.analysis().getOrNull(LINKS, LinksImpl.class);
            if (vLinks != null) {
                vLinks.links().forEach(l -> mergeEdge(graph, l.from(), l.linkNature(), l.to()));
            }
        });

        return graph;
    }

    private static void mergeEdge(Map<Variable, Map<Variable, LinkNature>> graph,
                                  Variable from,
                                  LinkNature linkNature,
                                  Variable to) {
        Map<Variable, LinkNature> edges = graph.computeIfAbsent(from, _ -> new HashMap<>());
        edges.merge(to, linkNature, LinkNature::combine);
    }

    private static boolean containsNoLocalVariable(Variable variable) {
        assert variable.variableStreamDescend().noneMatch(v -> v instanceof ReturnVariable) : """
                Return variables should not occur here: the result of LinkMethodCall should never contain them.
                """;
        return variable.variableStreamDescend().noneMatch(v -> v instanceof LocalVariable);
    }
}
