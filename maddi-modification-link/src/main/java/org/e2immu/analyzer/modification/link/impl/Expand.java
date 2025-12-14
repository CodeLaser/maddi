package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.LinkedVariables;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.LinksImpl.LINKS;

public record Expand(Runtime runtime) {
    private static final Logger LOGGER = LoggerFactory.getLogger(Expand.class);

    private static void mergeEdge(Map<Variable, Map<Variable, LinkNature>> graph,
                                  Variable from,
                                  LinkNature linkNature,
                                  Variable to) {
        Map<Variable, LinkNature> edges = graph.computeIfAbsent(from, _ -> new HashMap<>());
        edges.merge(to, linkNature, LinkNature::combine);
    }

    private static void mergeEdge(Map<Variable, Map<Variable, LinkNature>> graph,
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

    private void addToGraph(Link l,
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
                        mergeEdge(graph, s, LinkNature.IS_IDENTICAL_TO, makeComparableSub(l.from(), s, l.to())));
            }
            Set<Variable> subsOfTo = primaryToSub.get(l.to());
            if (subsOfTo != null) {
                subsOfTo.forEach(s ->
                        mergeEdge(graph, makeComparableSub(l.to(), s, l.from()), LinkNature.IS_IDENTICAL_TO, s));
            }
        }
    }

    private Variable makeComparableSub(Variable base, Variable sub, Variable target) {
        if (sub instanceof FieldReference fr && base.equals(fr.scopeVariable())) {
            return runtime.newFieldReference(fr.fieldInfo(), runtime.newVariableExpression(target), fr.fieldInfo().type());
        }
        throw new UnsupportedOperationException("More complex subbing, to be implemented");
    }

    private record GraphData(Map<Variable, Map<Variable, LinkNature>> graph,
                             Set<Variable> primaries,
                             Map<Variable, Variable> subToPrimary) {
    }

    private GraphData makeGraph(Map<Variable, Links> linkedVariables, boolean bidirectional) {
        Map<Variable, Set<Variable>> subs = new HashMap<>();
        Map<Variable, Variable> subToPrimary = new HashMap<>();
        linkedVariables.entrySet().stream()
                .filter(e -> !(e.getKey() instanceof This))
                .map(Map.Entry::getValue)
                .forEach(links -> links.linkList().forEach(l -> {
                    if (!(l.to() instanceof This) && !l.from().equals(links.primary())) {
                        subs.computeIfAbsent(links.primary(), _ -> new HashSet<>()).add(l.from());
                        subToPrimary.put(l.from(), links.primary());
                    }
                }));
        Map<Variable, Map<Variable, LinkNature>> graph = new HashMap<>();
        linkedVariables.entrySet().stream()
                .filter(e -> !(e.getKey() instanceof This))
                .map(Map.Entry::getValue)
                .forEach(links -> links.linkList()
                        .stream().filter(l -> !(l.to() instanceof This))
                        .forEach(l -> addToGraph(l, links.primary(), graph, bidirectional, subs, subToPrimary)));
        return new GraphData(graph, subs.keySet(), subToPrimary);
    }

    private static boolean containsNoLocalVariable(Variable variable) {
        assert variable.variableStreamDescend().noneMatch(v -> v instanceof ReturnVariable) : """
                Return variables should not occur here: the result of LinkMethodCall should never contain them.
                """;
        return variable.variableStreamDescend().noneMatch(v -> v instanceof LocalVariable);
    }

    private record NatureTo(LinkNature linkNature, Variable to) {
    }

    private record FromNature(Variable from, LinkNature linkNature) {
    }

    private static Links.Builder followGraph(GraphData gd,
                                             Variable primary,
                                             TranslationMap translationMap,
                                             boolean allowLocalVariables) {
        // first do the fields of the primary
        Set<Variable> fromSetExcludingPrimary = gd.graph.keySet().stream()
                .filter(v -> Util.isPartOf(primary, v) && !v.equals(primary))
                .collect(Collectors.toUnmodifiableSet());
        Variable tPrimary = translationMap == null ? primary : translationMap.translateVariableRecursively(primary);
        Links.Builder builder = new LinksImpl.Builder(tPrimary);
        // keep a set to avoid X->Y.ys and X.xs->Y.ys; so add the latter first
        Set<NatureTo> natureTos = new HashSet<>();
        // keep a set for each "to" variable to avoid X->Y.z and X.ys->Y.z
        Map<Variable, Set<FromNature>> toPrimaryToFromNatures = new HashMap<>();
        // for this second map/set to be effective, the links should be processed in "order",
        // with the primary of "to" coming after "to" itself
        for (Variable from : fromSetExcludingPrimary) {
            Map<Variable, LinkNature> all = bestPath(gd.graph, from);
            Variable tFrom = translationMap == null ? from : translationMap.translateVariableRecursively(from);
            for (Map.Entry<Variable, LinkNature> entry : all.entrySet()) {
                Variable to = entry.getKey();
                if (!gd.primaries.contains(to)) {
                    acceptAndAddLink(primary, allowLocalVariables, entry, to,
                            gd.subToPrimary.getOrDefault(to, to),
                            toPrimaryToFromNatures, tFrom, builder,
                            natureTos);
                }
            }
            for (Map.Entry<Variable, LinkNature> entry : all.entrySet()) {
                Variable to = entry.getKey();
                if (gd.primaries.contains(to)) {
                    acceptAndAddLink(primary, allowLocalVariables, entry, to, to, toPrimaryToFromNatures, tFrom, builder,
                            natureTos);
                }
            }
        }
        // then the primary itself
        if (gd.graph.containsKey(primary)) {
            Map<Variable, LinkNature> allFromPrimary = bestPath(gd.graph, primary);
            for (Map.Entry<Variable, LinkNature> entry : allFromPrimary.entrySet()) {
                Variable to = entry.getKey();
                if (!gd.primaries.contains(to)) {
                    acceptAndAddPrimaryLink(allowLocalVariables, entry, to,
                            gd.subToPrimary.getOrDefault(to, to),
                            natureTos, toPrimaryToFromNatures,
                            builder, tPrimary);
                }
            }
            for (Map.Entry<Variable, LinkNature> entry : allFromPrimary.entrySet()) {
                Variable to = entry.getKey();
                if (gd.primaries.contains(to)) {
                    acceptAndAddPrimaryLink(allowLocalVariables, entry, to, to, natureTos, toPrimaryToFromNatures,
                            builder, tPrimary);
                }
            }
        }
        return builder;
    }

    private static void acceptAndAddPrimaryLink(boolean allowLocalVariables,
                                                Map.Entry<Variable, LinkNature> entry,
                                                Variable to,
                                                Variable toPrimary,
                                                Set<NatureTo> natureTos,
                                                Map<Variable, Set<FromNature>> toPrimaryToFromNatures,
                                                Links.Builder builder,
                                                Variable tPrimary) {
        LinkNature linkNature = entry.getValue();
        if (acceptLink(tPrimary, allowLocalVariables, entry, to)
            // remove links that already exist for some sub in exactly the same way
            && !natureTos.contains(new NatureTo(linkNature, to))
            && toPrimaryToFromNatures.computeIfAbsent(toPrimary, _ -> new HashSet<>())
                    .add(new FromNature(tPrimary, linkNature))) {
            builder.add(tPrimary, linkNature, to);
        }
    }

    private static void acceptAndAddLink(Variable primary,
                                         boolean allowLocalVariables,
                                         Map.Entry<Variable, LinkNature> entry,
                                         Variable to,
                                         Variable toPrimary,
                                         Map<Variable, Set<FromNature>> toPrimaryToFromNatures,
                                         Variable tFrom,
                                         Links.Builder builder,
                                         Set<NatureTo> natureTos) {
        LinkNature linkNature = entry.getValue();
        if (acceptLink(primary, allowLocalVariables, entry, to)
            && toPrimaryToFromNatures.computeIfAbsent(toPrimary, _ -> new HashSet<>())
                    .add(new FromNature(tFrom, linkNature))) {
            builder.add(tFrom, linkNature, to);
            natureTos.add(new NatureTo(linkNature, to));
        }
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

    private static Map<Variable, LinkNature> bestPath(Map<Variable, Map<Variable, LinkNature>> graph, Variable start) {
        Map<Variable, Set<LinkNature>> res =
                FixpointPropagationAlgorithm.computePathLabels(s -> graph.getOrDefault(s, Map.of()),
                        graph.keySet(), start, LinkNature.EMPTY, LinkNature::combine);
        return res.entrySet().stream()
                .filter(e -> !start.equals(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        e -> e.getValue().stream().reduce(LinkNature.EMPTY, LinkNature::best)));
    }

    /*
     Write the links in terms of variables that we have, removing the temporarily created ones.
     Ensure that we use the variables and not their virtual fields.
     Ensure that v1 == v2 also means that v1.ts == v2.ts, v1.$m == v2.$m, so that these connections can be made.
     Filtering out ? links is done in followGraph.
     */
    public Map<Variable, Links> local(Map<Variable, Links> lvIn, VariableData previousVd, VariableData vd) {
        // copy everything into lv
        Map<Variable, Links> linkedVariables;
        if (previousVd == null) {
            linkedVariables = lvIn;
        } else {
            linkedVariables = new HashMap<>(lvIn);
            previousVd.variableInfoStream().forEach(vi -> {
                Links vLinks = vi.analysis().getOrNull(LINKS, LinksImpl.class);
                if (vLinks != null) {
                    linkedVariables.merge(vLinks.primary(), vLinks, Links::merge);
                }
            });
        }
        GraphData gd = makeGraph(linkedVariables, true);
        LOGGER.debug("Bi-directional graph: {}", gd.graph);
        Map<Variable, Links> newLinkedVariables = new HashMap<>();
        vd.variableInfoStream().forEach(vi -> {
            Links.Builder piBuilder = followGraph(gd, vi.variable(), null, true);
            piBuilder.removeIf(Link::toIntermediateVariable);
            newLinkedVariables.put(vi.variable(), piBuilder.build());
        });
        return newLinkedVariables;
    }


    public List<Links> parameters(MethodInfo methodInfo, VariableData vd) {
        if (vd == null) return List.of();

        Map<Variable, Links> linkedVariables = new HashMap<>();
        vd.variableInfoStream().forEach(vi -> {
            Links vLinks = vi.analysis().getOrNull(LINKS, LinksImpl.class);
            if (vLinks != null) {
                linkedVariables.merge(vLinks.primary(), vLinks, Links::merge);
            }
        });

        /*
        why a bidirectional graph for the parameters, and only a directional one for the return value?
        the flow is obviously linear parm -> rv, so if we want rv in function of parameters, only one
        direction is relevant. But if we want the parameters in function of the fields, we may have
        to see the reverse of param -> field. See e.g. TestList,4 (set)
         */
        GraphData gd = makeGraph(linkedVariables, true);
        LOGGER.debug("Bi-directional graph: {}", gd.graph);

        List<Links> linksPerParameter = new ArrayList<>(methodInfo.parameters().size());
        for (ParameterInfo pi : methodInfo.parameters()) {
            Links.Builder piBuilder = followGraph(gd, pi, null, false);
            linksPerParameter.add(piBuilder.build());
        }

        return linksPerParameter;
    }

    /*
    Prepares the links of the return value for the outside world:
    - find as many links to fields and parameters
    - remove (intermediate) links to local variables
    */
    public Links returnValue(ReturnVariable returnVariable, Links links, LinkedVariables extra, VariableData vd) {
        Variable primary = links.primary();
        if (primary == null) return LinksImpl.EMPTY;
        TranslationMap tm = runtime.newTranslationMapBuilder().put(primary, returnVariable).build();

        Map<Variable, Links> linkedVariables = new HashMap<>(extra.map());
        linkedVariables.merge(links.primary(), links, Links::merge);
        vd.variableInfoStream().forEach(vi -> {
            Links vLinks = vi.analysis().getOrNull(LINKS, LinksImpl.class);
            if (vLinks != null) {
                linkedVariables.merge(vLinks.primary(), vLinks, Links::merge);
            }
        });

        GraphData gd = makeGraph(linkedVariables, false);
        LOGGER.debug("Return graph: {}", gd.graph);

        Links.Builder rvBuilder = followGraph(gd, primary, tm, false);

        if (containsNoLocalVariable(primary)) {
            rvBuilder.add(LinkNature.IS_IDENTICAL_TO, primary);
        }
        return rvBuilder.build();
    }

}
