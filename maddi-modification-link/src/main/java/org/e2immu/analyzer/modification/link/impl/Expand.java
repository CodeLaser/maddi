package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.LinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
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

    // different equality from Variable: container virtual fields
    private record V(Variable v) {
        @Override
        public boolean equals(Object object) {
            if (!(object instanceof V(Variable v1)) || v1 == null) return false;
            if (v == v1) return true;
            String fqn1 = newFqn(v);
            String fqn2 = newFqn(v1);
            return fqn1.equals(fqn2);
        }

        @Override
        public int hashCode() {
            return newFqn(v).hashCode();
        }

        private static String newFqn(Variable v) {
            if (v instanceof FieldReference fr) {
                TypeInfo typeInfo = fr.fieldInfo().type().typeInfo();
                String name;
                if (typeInfo != null && typeInfo.typeNature() == VirtualFieldComputer.VIRTUAL_FIELD
                    || fr.fieldInfo().name().contains("$")) { // $m, $s (where $ replaces ts)
                    name = "VF:" + fr.fieldInfo().name(); // NOTE: for now there is ambiguity in the 's'
                } else {
                    name = fr.fieldInfo().fullyQualifiedName();
                }
                String scope;
                if (fr.isStatic() || fr.scopeIsThis()) {
                    return name;
                }
                if (fr.scopeVariable() != null) {
                    scope = newFqn(fr.scopeVariable());
                } else {
                    // take from the existing
                    scope = fr.fullyQualifiedName().substring(0, fr.fullyQualifiedName().lastIndexOf('.'));
                }
                return name + "#" + scope;
            }
            return v.fullyQualifiedName();
        }
    }

    private static void mergeEdge(Map<V, Map<V, LinkNature>> graph,
                                  V from,
                                  LinkNature linkNature,
                                  V to) {
        Map<V, LinkNature> edges = graph.computeIfAbsent(from, _ -> new HashMap<>());
        edges.merge(to, linkNature, LinkNature::combine);
        assert graph.size() == graph.keySet().stream().map(v -> v.v.toString()).count();
    }

    private static void mergeEdge(Map<V, Map<V, LinkNature>> graph,
                                  V primary,
                                  V from,
                                  LinkNature linkNature,
                                  V to) {
        mergeEdge(graph, from, linkNature, to);
        if (!from.equals(primary) && !(primary.v instanceof This)) {
            mergeEdge(graph, primary, LinkNature.HAS_FIELD, from);
            mergeEdge(graph, from, LinkNature.IS_FIELD_OF, primary);
        }
    }

    private static V correctForThis(V primary, V v) {
        if (primary.v instanceof This) {
            return new V(org.e2immu.analyzer.modification.prepwork.Util.primary(v.v));
        }
        return primary;
    }

    private void addToGraph(Link l,
                            V primaryIn,
                            Map<V, Map<V, LinkNature>> graph,
                            boolean bidirectional,
                            Map<V, Set<V>> primaryToSub,
                            Map<V, V> subToPrimary) {
        V vFrom = new V(l.from());
        V vTo = new V(l.to());
        V primary = correctForThis(primaryIn, vFrom);
        mergeEdge(graph, primary, vFrom, l.linkNature(), vTo);
        if (bidirectional) {
            V toPrimary = correctForThis(subToPrimary.getOrDefault(vTo, vTo), vTo);
            mergeEdge(graph, toPrimary, vTo, l.linkNature().reverse(), vFrom);
        }
        if (l.linkNature() == LinkNature.IS_IDENTICAL_TO) {
            Set<V> subsOfFrom = primaryToSub.get(vFrom);
            if (subsOfFrom != null) {
                subsOfFrom.forEach(s ->
                        mergeEdge(graph, s, LinkNature.IS_IDENTICAL_TO, makeComparableSub(vFrom, s, vTo)));
            }
            Set<V> subsOfTo = primaryToSub.get(vTo);
            if (subsOfTo != null) {
                subsOfTo.forEach(s ->
                        mergeEdge(graph, makeComparableSub(vTo, s, vFrom), LinkNature.IS_IDENTICAL_TO, s));
            }
        }
    }

    private V makeComparableSub(V base, V sub, V target) {
        if (sub.v instanceof FieldReference fr && base.v.equals(fr.scopeVariable())) {
            return new V(runtime.newFieldReference(fr.fieldInfo(), runtime.newVariableExpression(target.v), fr.fieldInfo().type()));
        }
        throw new UnsupportedOperationException("More complex subbing, to be implemented");
    }

    private record GraphData(Map<V, Map<V, LinkNature>> graph,
                             Set<V> primaries,
                             Map<V, V> subToPrimary) {
    }

    private GraphData makeGraph(Map<Variable, Links> linkedVariables, boolean bidirectional) {
        Map<V, Set<V>> subs = new HashMap<>();
        Map<V, V> subToPrimary = new HashMap<>();
        linkedVariables.entrySet().stream()
                // .filter(e -> !(e.getKey() instanceof This))
                .map(Map.Entry::getValue)
                .forEach(links -> links.linkSet().forEach(l -> {
                    V primary = new V(links.primary());
                    Set<V> subsOfPrimary = subs.computeIfAbsent(primary, _ -> new HashSet<>());
                    V vFrom = new V(l.from());
                    if (!vFrom.equals(primary)) {
                        subsOfPrimary.add(vFrom);
                        subToPrimary.put(vFrom, primary);
                    }
                    V toPrimary = new V(org.e2immu.analyzer.modification.prepwork.Util.primary(l.to()));
                    Set<V> subsOfToPrimary = subs.computeIfAbsent(toPrimary, _ -> new HashSet<>());
                    V vTo = new V(l.to());
                    if (!vTo.equals(toPrimary)) {
                        subsOfToPrimary.add(vTo);
                        subToPrimary.put(vTo, toPrimary);
                    }
                }));
        Map<V, Map<V, LinkNature>> graph = new HashMap<>();
        linkedVariables.entrySet().stream()
                //   .filter(e -> !(e.getKey() instanceof This))
                .map(Map.Entry::getValue)
                .forEach(links -> links.linkSet()
                        .stream().filter(l -> !(l.to() instanceof This))
                        .forEach(l -> addToGraph(l, new V(links.primary()), graph, bidirectional, subs, subToPrimary)));
        return new GraphData(graph, subs.keySet(), subToPrimary);
    }

    private static boolean containsNoLocalVariable(Variable variable) {
        assert variable.variableStreamDescend().noneMatch(v -> v instanceof ReturnVariable) : """
                Return variables should not occur here: the result of LinkMethodCall should never contain them.
                """;
        return variable.variableStreamDescend().noneMatch(v -> v instanceof LocalVariable);
    }


    private static Links.Builder followGraph(GraphData gd,
                                             Variable primary,
                                             TranslationMap translationMap,
                                             boolean allowLocalVariables) {
        // first do the fields of the primary
        Set<V> fromSetExcludingPrimary = gd.graph.keySet().stream()
                .filter(v -> org.e2immu.analyzer.modification.prepwork.Util.isPartOf(primary, v.v) && !v.v.equals(primary))
                .collect(Collectors.toUnmodifiableSet());
        V tPrimary = new V(translationMap == null ? primary : translationMap.translateVariableRecursively(primary));
        Links.Builder builder = new LinksImpl.Builder(tPrimary.v);
        // keep a set to avoid X->Y.ys and X.xs->Y.ys; so add the latter first
        Set<V> natureTos = new HashSet<>();
        // keep a set for each "to" variable to avoid X->Y.z and X.ys->Y.z
        Map<V, Set<V>> toPrimaryToFromNatures = new HashMap<>();
        // for this second map/set to be effective, the links should be processed in "order",
        // with the primary of "to" coming after "to" itself
        V vPrimary = new V(primary);

        for (V from : fromSetExcludingPrimary) {
            Map<V, LinkNature> all = bestPath(gd.graph, from);
            V tFrom = new V(translationMap == null ? from.v : translationMap.translateVariableRecursively(from.v));
            for (Map.Entry<V, LinkNature> entry : all.entrySet()) {
                V to = entry.getKey();
                if (!gd.primaries.contains(to)) {
                    acceptAndAddLink(vPrimary, allowLocalVariables, entry, to,
                            gd.subToPrimary.getOrDefault(to, to),
                            toPrimaryToFromNatures, true, tFrom, builder, natureTos);
                }
            }
            for (Map.Entry<V, LinkNature> entry : all.entrySet()) {
                V to = entry.getKey();
                if (gd.primaries.contains(to)) {
                    acceptAndAddLink(vPrimary, allowLocalVariables, entry, to, to, toPrimaryToFromNatures,
                            false, tFrom, builder, natureTos);
                }
            }
        }
        // then the primary itself
        if (gd.graph.containsKey(vPrimary)) {
            Map<V, LinkNature> allFromPrimary = bestPath(gd.graph, vPrimary);
            for (Map.Entry<V, LinkNature> entry : allFromPrimary.entrySet()) {
                V to = entry.getKey();
                if (!gd.primaries.contains(to)) {
                    acceptAndAddPrimaryLink(allowLocalVariables, entry, to,
                            gd.subToPrimary.getOrDefault(to, to),
                            natureTos, toPrimaryToFromNatures, true,
                            builder, tPrimary);
                }
            }
            for (Map.Entry<V, LinkNature> entry : allFromPrimary.entrySet()) {
                V to = entry.getKey();
                if (gd.primaries.contains(to)) {
                    acceptAndAddPrimaryLink(allowLocalVariables, entry, to, to, natureTos, toPrimaryToFromNatures,
                            false, builder, tPrimary);
                }
            }
        }
        return builder;
    }

    private static void acceptAndAddPrimaryLink(boolean allowLocalVariables,
                                                Map.Entry<V, LinkNature> entry,
                                                V to,
                                                V toPrimary,
                                                Set<V> natureTos,
                                                Map<V, Set<V>> toPrimaryToFromNatures,
                                                boolean addToToPrimary,
                                                Links.Builder builder,
                                                V tPrimary) {
        LinkNature linkNature = entry.getValue();
        if (acceptLink(tPrimary, allowLocalVariables, entry, to)
            // remove links that already exist for some sub in exactly the same way
            && !natureTos.contains(to)
            && (toPrimaryToFromNatures.computeIfAbsent(toPrimary, _ -> new HashSet<>()).add(tPrimary) || addToToPrimary)) {
            builder.add(tPrimary.v, linkNature, to.v);
        }
    }

    private static void acceptAndAddLink(V primary,
                                         boolean allowLocalVariables,
                                         Map.Entry<V, LinkNature> entry,
                                         V to,
                                         V toPrimary,
                                         Map<V, Set<V>> toPrimaryToFromNatures,
                                         boolean addToToPrimary,
                                         V tFrom,
                                         Links.Builder builder,
                                         Set<V> natureTos) {
        LinkNature linkNature = entry.getValue();
        if (acceptLink(primary, allowLocalVariables, entry, to)
            && (toPrimaryToFromNatures.computeIfAbsent(toPrimary, _ -> new HashSet<>()).add(tFrom) || addToToPrimary)) {
            builder.add(tFrom.v, linkNature, to.v);
            natureTos.add(to);
        }
    }

    private static boolean acceptLink(V primary,
                                      boolean allowLocalVariables,
                                      Map.Entry<V, LinkNature> entry,
                                      V to) {
        return entry.getValue().valid()
               && (allowLocalVariables || containsNoLocalVariable(to.v))
               // remove internal references (field inside primary to primary or other field in primary)
               && !Util.isPartOf(primary.v, to.v);
    }

    private static Map<V, LinkNature> bestPath(Map<V, Map<V, LinkNature>> graph, V start) {
        Map<V, Set<LinkNature>> res =
                FixpointPropagationAlgorithm.computePathLabels(s -> graph.getOrDefault(s, Map.of()),
                        graph.keySet(), start, LinkNature.EMPTY, LinkNature::combine);
        V startPrimary = new V(org.e2immu.analyzer.modification.prepwork.Util.primary(start.v));
        return res.entrySet().stream()
                .filter(e -> !start.equals(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        e -> {
                            boolean samePrimary = startPrimary.equals(new V(org.e2immu.analyzer.modification.prepwork.Util.primary(e.getKey().v)));
                            return e.getValue().stream().reduce(LinkNature.EMPTY,
                                    (ln1, ln2) -> ln1.best(ln2, samePrimary));
                        }));
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
        LOGGER.debug("Bi-directional graph for local: {}", gd.graph);
        Map<Variable, Links> newLinkedVariables = new HashMap<>();
        vd.variableInfoStream().forEach(vi -> {
            Links.Builder piBuilder = followGraph(gd, vi.variable(), null, true);
            piBuilder.removeIf(Link::toIntermediateVariable);
            if (newLinkedVariables.put(vi.variable(), piBuilder.build()) != null) {
                throw new UnsupportedOperationException("Each real variable must be a primary");
            }
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
        LOGGER.debug("Bi-directional graph for parameters: {}", gd.graph);

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
