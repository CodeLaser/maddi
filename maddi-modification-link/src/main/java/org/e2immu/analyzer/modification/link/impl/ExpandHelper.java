package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExpandHelper {

    static void mergeEdge(Map<Variable, Map<Variable, LinkNature>> graph,
                          Variable from,
                          LinkNature linkNature,
                          Variable to) {
        Map<Variable, LinkNature> edges = graph.computeIfAbsent(from, _ -> new HashMap<>());
        edges.merge(to, linkNature, LinkNature::combine);
    }

    static void mergeEdge(Map<Variable, Map<Variable, LinkNature>> graph,
                          Variable primary,
                          Variable from,
                          LinkNature linkNature,
                          Variable to) {
        mergeEdge(graph, from, linkNature, to);
        if (!from.equals(primary) && !(primary instanceof This)) {
            mergeEdge(graph, primary, LinkNature.HAS_FIELD, from);
            mergeEdge(graph, from, LinkNature.IS_FIELD_OF, primary);
        }
    }

    static boolean containsNoLocalVariable(Variable variable) {
        assert variable.variableStreamDescend().noneMatch(v -> v instanceof ReturnVariable) : """
                Return variables should not occur here: the result of LinkMethodCall should never contain them.
                """;
        return variable.variableStreamDescend().noneMatch(v -> v instanceof LocalVariable);
    }

    static Links.Builder followGraph(Map<Variable, Map<Variable, LinkNature>> graph,
                                     Variable primary,
                                     TranslationMap translationMap,
                                     boolean allowLocalVariables) {
        // start from the parameter and any of its fields, as present in the graph.
        Set<Variable> fromSet = graph.keySet().stream()
                .filter(v -> Util.isPartOf(primary, v))
                .collect(Collectors.toUnmodifiableSet());
        Variable tPrimary = translationMap == null ? primary : translationMap.translateVariableRecursively(primary);
        Links.Builder builder = new LinksImpl.Builder(tPrimary);
        for (Variable from : fromSet) {
            Map<Variable, LinkNature> all = bestPath(graph, from);
            Variable tFrom = translationMap == null ? from : translationMap.translateVariableRecursively(from);
            for (Map.Entry<Variable, LinkNature> entry : all.entrySet()) {
                Variable to = entry.getKey();
                if (entry.getValue().valid()
                    && (allowLocalVariables || containsNoLocalVariable(to))
                    // remove internal references (field inside primary to primary or other field in primary)
                    && !Util.isPartOf(primary, to)) {
                    builder.add(tFrom, entry.getValue(), to);
                }
            }
        }
        return builder;
    }

    static Map<Variable, LinkNature> bestPath(Map<Variable, Map<Variable, LinkNature>> graph, Variable start) {
        Map<Variable, Set<LinkNature>> res =
                FixpointPropagationAlgorithm.computePathLabels(s -> graph.getOrDefault(s, Map.of()),
                        graph.keySet(), start, LinkNature.EMPTY, LinkNature::combine);
        return res.entrySet().stream()
                .filter(e -> !start.equals(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        e -> e.getValue().stream().reduce(LinkNature.EMPTY, LinkNature::best)));
    }
}
