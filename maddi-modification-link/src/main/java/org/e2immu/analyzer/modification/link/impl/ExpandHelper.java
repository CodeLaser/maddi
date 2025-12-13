package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.HashMap;
import java.util.HashSet;
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

    static void addToGraph(Runtime runtime,
                           Link l,
                           Variable primary,
                           Map<Variable, Map<Variable, LinkNature>> graph,
                           boolean bidirectional,
                           Map<Variable, Set<Variable>> primaryToSub,
                           Map<Variable, Variable> subToPrimary) {
        mergeEdge(graph, primary, l.from(), l.linkNature(), l.to());
        if (bidirectional) {
            Variable toPrimary = subToPrimary.getOrDefault(l.to(), l.to());
            mergeEdge(graph, toPrimary, l.to(), l.linkNature().reverse(), l.from());
        }
        if (l.linkNature() == LinkNature.IS_IDENTICAL_TO) {
            Set<Variable> subsOfFrom = primaryToSub.get(l.from());
            if (subsOfFrom != null) {
                subsOfFrom.forEach(s ->
                        mergeEdge(graph, s, LinkNature.IS_IDENTICAL_TO,
                                makeComparableSub(runtime, l.from(), s, l.to())));
            }
            Set<Variable> subsOfTo = primaryToSub.get(l.to());
            if (subsOfTo != null) {
                subsOfTo.forEach(s ->
                        mergeEdge(graph, makeComparableSub(runtime, l.to(), s, l.from()),
                                LinkNature.IS_IDENTICAL_TO, s));
            }
        }
    }

    static Variable makeComparableSub(Runtime runtime, Variable base, Variable sub, Variable target) {
        if (sub instanceof FieldReference fr && base.equals(fr.scopeVariable())) {
            return runtime.newFieldReference(fr.fieldInfo(), runtime.newVariableExpression(target), fr.fieldInfo().type());
        }
        throw new UnsupportedOperationException("More complex subbing, to be implemented");
    }

    static Map<Variable, Map<Variable, LinkNature>> makeGraph(Runtime runtime,
                                                              Map<Variable, Links> linkedVariables,
                                                              boolean bidirectional) {
        Map<Variable, Set<Variable>> subs = new HashMap<>();
        Map<Variable, Variable> subToPrimary = new HashMap<>();
        linkedVariables.values().forEach(links -> links.links().forEach(l -> {
            if (!l.from().equals(links.primary())) {
                subs.computeIfAbsent(links.primary(), _ -> new HashSet<>()).add(l.from());
                subToPrimary.put(l.from(), links.primary());
            }
        }));
        Map<Variable, Map<Variable, LinkNature>> graph = new HashMap<>();
        linkedVariables.values().forEach(links -> links.links().forEach(l ->
                addToGraph(runtime, l, links.primary(), graph, bidirectional, subs, subToPrimary)));
        return graph;
    }

    static boolean containsNoLocalVariable(Variable variable) {
        assert variable.variableStreamDescend().noneMatch(v -> v instanceof ReturnVariable) : """
                Return variables should not occur here: the result of LinkMethodCall should never contain them.
                """;
        return variable.variableStreamDescend().noneMatch(v -> v instanceof LocalVariable);
    }

    private record NatureTo(LinkNature linkNature, Variable to) {
    }

    static Links.Builder followGraph(Map<Variable, Map<Variable, LinkNature>> graph,
                                     Variable primary,
                                     TranslationMap translationMap,
                                     boolean allowLocalVariables) {
        // start from the parameter and any of its fields, as present in the graph.
        Set<Variable> fromSetExcludingPrimary = graph.keySet().stream()
                .filter(v -> Util.isPartOf(primary, v) && !v.equals(primary))
                .collect(Collectors.toUnmodifiableSet());
        Variable tPrimary = translationMap == null ? primary : translationMap.translateVariableRecursively(primary);
        Links.Builder builder = new LinksImpl.Builder(tPrimary);
        Set<NatureTo> natureTos = new HashSet<>();
        for (Variable from : fromSetExcludingPrimary) {
            Map<Variable, LinkNature> all = bestPath(graph, from);
            Variable tFrom = translationMap == null ? from : translationMap.translateVariableRecursively(from);
            for (Map.Entry<Variable, LinkNature> entry : all.entrySet()) {
                Variable to = entry.getKey();
                if (acceptLink(primary, allowLocalVariables, entry, to)) {
                    builder.add(tFrom, entry.getValue(), to);
                    natureTos.add(new NatureTo(entry.getValue(), to));
                }
            }
        }
        if (graph.containsKey(primary)) {
            Map<Variable, LinkNature> allFromPrimary = bestPath(graph, primary);
            for (Map.Entry<Variable, LinkNature> entry : allFromPrimary.entrySet()) {
                Variable to = entry.getKey();
                if (acceptLink(primary, allowLocalVariables, entry, to)
                    // remove links that already exist for some sub in exactly the same way
                    && !natureTos.contains(new NatureTo(entry.getValue(), to))) {
                    builder.add(tPrimary, entry.getValue(), to);
                }
            }
        }
        return builder;
    }

    private static boolean acceptLink(Variable primary,
                                      boolean allowLocalVariables,
                                      Map.Entry<Variable, LinkNature> entry,
                                      Variable to) {
        return entry.getValue().valid()
               && (allowLocalVariables || containsNoLocalVariable(to))
               // remove internal references (field inside primary to primary or other field in primary)
               && !Util.isPartOf(primary, to);
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
