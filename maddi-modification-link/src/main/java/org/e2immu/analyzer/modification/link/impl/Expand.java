package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;
import static org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer.isVirtualModificationField;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.UNMODIFIED_VARIABLE;

public record Expand(Runtime runtime) {
    private static final Logger LOGGER = LoggerFactory.getLogger(Expand.class);

    // different equality from Variable: container virtual fields
    record V(Variable v) {
        @Override
        public boolean equals(Object object) {
            if (!(object instanceof V(Variable v1)) || v1 == null) return false;
            if (v == v1) return true;
            String fqn1 = newFqn(v);
            String fqn2 = newFqn(v1);
            return fqn1.equals(fqn2);
        }

        @Override
        public @NotNull String toString() {
            return Util.simpleName(v);
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
        assert graph.size() == graph.keySet().stream().map(v -> v.v.toString()).distinct().count();
    }

    private static void mergeEdge(Map<V, Map<V, LinkNature>> graph,
                                  V primary,
                                  V from,
                                  LinkNature linkNature,
                                  V to) {
        mergeEdge(graph, from, linkNature, to);
        if (!from.equals(primary) && !(primary.v instanceof This)
            //   && !from.v.simpleName().startsWith("§")
            && from.v instanceof FieldReference && primary.v.equals(fieldScope(from.v))) {
            mergeEdge(graph, primary, CONTAINS_AS_FIELD, from);
            mergeEdge(graph, from, IS_FIELD_OF, primary);
        }
    }

    private static Variable fieldScope(Variable v) {
        if (v instanceof FieldReference fr) {
            if (fr.scopeVariable() instanceof This) return v;
            if (fr.scopeVariable() != null) return fieldScope(fr.scopeVariable());
        }
        return v;
    }

    private static V correctForThis(V primary, V v) {
        if (primary.v instanceof This) {
            return new V(org.e2immu.analyzer.modification.prepwork.Util.primary(v.v));
        }
        return primary;
    }

    private void addToGraph(Variable lFrom, LinkNature linkNature, Variable lTo,
                            V primaryIn,
                            Map<V, Map<V, LinkNature>> graph,
                            Map<V, Set<V>> primaryToSub,
                            Map<V, V> subToPrimary) {
        V vFrom = new V(lFrom);
        V vTo = new V(lTo);
        V primary = correctForThis(primaryIn, vFrom);
        mergeEdge(graph, primary, vFrom, linkNature, vTo);

        // other direction
        V toPrimary = correctForThis(subToPrimary.getOrDefault(vTo, vTo), vTo);
        mergeEdge(graph, toPrimary, vTo, linkNature.reverse(), vFrom);

        // add extra: if a ← b, and we know a.§xs exists, then a.§xs ← b.§xs
        //            if rv ← b, and we know b.§m exists, then rv.§m ≡ b.§m
        if (linkNature == IS_IDENTICAL_TO || linkNature == IS_ASSIGNED_FROM || linkNature == IS_ASSIGNED_TO) {
            Set<V> subsOfFrom = primaryToSub.get(vFrom);
            if (subsOfFrom != null) {
                subsOfFrom.forEach(s ->
                        mergeEdge(graph, s, linkNature, makeComparableSub(vFrom, s, vTo)));
            }
            Set<V> subsOfTo = primaryToSub.get(vTo);
            if (subsOfTo != null) {
                subsOfTo.forEach(s -> {
                    LinkNature ln;
                    if (s.v instanceof FieldReference fr && isVirtualModificationField(fr.fieldInfo())) {
                        ln = IS_IDENTICAL_TO;
                    } else {
                        ln = linkNature;
                    }
                    mergeEdge(graph, makeComparableSub(vTo, s, vFrom), ln, s);
                });
            }
        }
    }

    private V makeComparableSub(V base, V sub, V target) {
        if (sub.v instanceof FieldReference fr && base.v.equals(fr.scopeVariable())) {
            VariableExpression tve = runtime.newVariableExpression(target.v);
            return new V(runtime.newFieldReference(fr.fieldInfo(), tve, fr.fieldInfo().type()));
        }
        TranslationMap tm = new VariableTranslationMap(runtime).put(base.v, target.v);
        Variable newSub = tm.translateVariableRecursively(sub.v);
        return new V(newSub);
    }

    private record GraphData(Map<V, Map<V, LinkNature>> graph,
                             Set<V> primaries,
                             Map<V, V> subToPrimary) {
    }

    private GraphData makeGraph(Map<Variable, Links> linkedVariables) {
        Map<V, Set<V>> subs = new HashMap<>();
        Map<V, V> subToPrimary = new HashMap<>();
        linkedVariables.values()
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
        linkedVariables.values()
                .forEach(links -> links
                        .stream().filter(l -> !(l.to() instanceof This))
                        .forEach(l -> addToGraph(l.from(), l.linkNature(), l.to(), new V(links.primary()),
                                graph, subs, subToPrimary)));
        List<PC> extra = new ExpandSlice().completeSliceInformation(graph);
        extra.forEach(pc -> addToGraph(pc.from, pc.linkNature, pc.to, new V(Util.primary(pc.from)), graph,
                subs, subToPrimary));
        return new GraphData(graph, subs.keySet(), subToPrimary);
    }

    record PC(Variable from, LinkNature linkNature, Variable to) {
        @Override
        public @NotNull String toString() {
            return Util.simpleName(from) + " " + linkNature + " " + Util.simpleName(to);
        }
    }

    // sorting is needed to consistently take the same direction for tests
    private static Links.Builder followGraph(GraphData gd, Variable primary) {
        V tPrimary = new V(primary);
        Links.Builder builder = new LinksImpl.Builder(tPrimary.v);
        var fromList = gd.graph.keySet().stream()
                .filter(v -> Util.isPartOf(primary, v.v))
                .sorted((v1, v2) -> {
                    if (Util.isPartOf(v1.v, v2.v)) return 1;
                    if (Util.isPartOf(v2.v, v1.v)) return -1;
                    return v1.v.fullyQualifiedName().compareTo(v2.v.fullyQualifiedName());
                })
                .toList();

        // stream.§$s⊆0:in.§$s
        Set<PC> block = new HashSet<>();

        for (V from : fromList) {
            Map<V, LinkNature> all = bestPath(gd.graph, from);
            List<Map.Entry<V, LinkNature>> entries = all.entrySet().stream()
                    .sorted((e1, e2) -> {
                        int c = e2.getValue().rank() - e1.getValue().rank();
                        if (c != 0) return c;
                        boolean p1 = Util.isPrimary(e1.getKey().v);
                        boolean p2 = Util.isPrimary(e2.getKey().v);
                        if (p1 && !p2) return 1;
                        if (p2 && !p1) return -1;
                        // subs first, best score first
                        return e1.getKey().v.fullyQualifiedName().compareTo(e2.getKey().v.fullyQualifiedName());
                    })
                    .toList();
            Variable primaryFrom = Util.primary(from.v);
            Variable firstRealFrom = Util.firstRealVariable(from.v);

            //LOGGER.debug("Entries of {}: {}", from, entries);

            for (Map.Entry<V, LinkNature> entry : entries) {
                LinkNature linkNature = entry.getValue();
                Variable toV = entry.getKey().v;
                Variable primaryTo = Util.primary(toV);
                Variable firstRealTo = Util.firstRealVariable(toV);
                // remove internal references (field inside primary to primary or other field in primary)
                // see TestStaticValues1,5 for an example where s.k ← s.r.i, which requires the 2nd clause
                if (linkNature.rank() >= 0
                    && (!primaryTo.equals(primaryFrom) ||
                        !firstRealFrom.equals(primaryFrom) &&
                        !firstRealTo.equals(primaryTo) &&
                        !firstRealFrom.equals(firstRealTo))
                    && block.add(new PC(from.v, linkNature, toV))) {
                    builder.add(from.v, linkNature, toV);
                    // don't add if the reverse is already present in this builder
                    block.add(new PC(toV, linkNature.reverse(), from.v));
                    // when adding p.sub < q.sub, don't add p < q.sub, p.sub < q
                    Set<Variable> scopeFrom = Util.scopeVariables(from.v);
                    for (LinkNature lnUp : linkNature.redundantFromUp()) {
                        for (Variable sv : scopeFrom) {
                            block.add(new PC(sv, lnUp, toV));
                        }
                    }
                    Set<Variable> scopeTo = Util.scopeVariables(toV);
                    for (LinkNature lnUp : linkNature.redundantToUp()) {
                        for (Variable sv : scopeTo) {
                            block.add(new PC(from.v, lnUp, sv));
                        }
                    }
                    for (LinkNature lnBoth : linkNature.redundantUp()) {
                        for (Variable fromUp : scopeFrom) {
                            for (Variable toUp : scopeTo) {
                                block.add(new PC(fromUp, lnBoth, toUp));
                            }
                        }
                    }
                }
            }
        }
        return builder;
    }

    private static Map<V, LinkNature> bestPath(Map<V, Map<V, LinkNature>> graph, V start) {
        Map<V, Set<LinkNature>> res =
                FixpointPropagationAlgorithm.computePathLabels(s -> graph.getOrDefault(s, Map.of()),
                        graph.keySet(), start, LinkNatureImpl.EMPTY, LinkNature::combine);
        return res.entrySet().stream()
                .filter(e -> !start.equals(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        e -> e.getValue().stream().reduce(LinkNatureImpl.EMPTY, LinkNature::best)));
    }

    //-------------------------------------------------------------------------------------------------

    private boolean notLinkedToModified(Links.Builder builder, Set<Variable> modifiedVariables) {
        for (Link link : builder) {
            Variable toPrimary = Util.primary(link.to());
            if (modifiedVariables.contains(toPrimary)) {
                LinkNature ln = link.linkNature();
                if (ln == IS_IDENTICAL_TO
                    || ln == IS_ASSIGNED_FROM
                    || ln == IS_ASSIGNED_TO
                    || ln == CONTAINS_AS_MEMBER
                    || ln == CONTAINS_AS_FIELD
                    || ln == OBJECT_GRAPH_CONTAINS) {
                    return false;
                }
                if (ln == SHARES_ELEMENTS || ln == SHARES_FIELDS) {
                    // TODO do we need to look at the immutability of the variable's type?
                    return false;
                }
            }
        }
        return true;
    }

    private static String printGraph(Map<V, Map<V, LinkNature>> graph) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<V, Map<V, LinkNature>> e : graph.entrySet()) {
            for (Map.Entry<V, LinkNature> e2 : e.getValue().entrySet()) {
                sb.append(Util.simpleName(e.getKey().v)).append(" ").append(e2.getValue()).append(" ")
                        .append(Util.simpleName(e2.getKey().v)).append("\n");
            }
        }
        return sb.toString();
    }

    //-------------------------------------------------------------------------------------------------

    /*
     Write the links in terms of variables that we have, removing the temporarily created ones.
     Ensure that we use the variables and not their virtual fields.
     Ensure that v1 == v2 also means that v1.ts == v2.ts, v1.$m == v2.$m, so that these connections can be made.
     Filtering out ? links is done in followGraph.
     */
    public Map<Variable, Links> local(Map<Variable, Links> lvIn,
                                      Set<Variable> modifiedDuringEvaluation,
                                      VariableData previousVd,
                                      Stage stageOfPrevious,
                                      VariableData vd,
                                      TranslationMap replaceConstants) {
        // copy everything into lv
        Map<Variable, Links> linkedVariables = new HashMap<>();
        lvIn.entrySet().stream()
                .filter(e -> !(e.getKey() instanceof This))
                .filter(e -> e.getValue().primary() != null)
                .forEach(e -> linkedVariables.put(e.getKey(), e.getValue().translate(replaceConstants)));
        Set<Variable> modifiedVariables = new HashSet<>(modifiedDuringEvaluation);
        if (previousVd != null) {
            previousVd.variableInfoStream(stageOfPrevious)
                    .filter(vi -> !(vi.variable() instanceof This))
                    .forEach(vi -> {
                        Links vLinks = vi.linkedVariables();
                        if (vLinks != null && vLinks.primary() != null) {
                            assert !(vLinks.primary() instanceof This);
                            linkedVariables.merge(vLinks.primary(), vLinks.translate(replaceConstants), Links::merge);
                        }
                        Value.Bool unmodified = vi.analysis().getOrNull(UNMODIFIED_VARIABLE, ValueImpl.BoolImpl.class);
                        boolean explicitlyModified = unmodified != null && unmodified.isFalse();
                        if (explicitlyModified) modifiedVariables.add(vi.variable());
                    });
        }
        assert linkedVariables.entrySet().stream().noneMatch(e ->
                e.getKey() instanceof This || e.getValue().primary() == null || e.getValue().primary() instanceof This)
                : "Not linking null or 'this'";

        GraphData gd = makeGraph(linkedVariables);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Bi-directional graph for local:\n{}", printGraph(gd.graph));
        }
        Map<Variable, Links> newLinkedVariables = new HashMap<>();
        vd.variableInfoStream(Stage.EVALUATION)
                .filter(vi -> !(vi.variable() instanceof This))
                .forEach(vi -> {
                    // FIXME turning ⊆ into ~ when modified is best done directly in followGraph(),
                    //  otherwise we cannot change ≤ into ∩ easily for derivative relations (if we must have them)
                    Links.Builder builder = followGraph(gd, vi.variable());
                    boolean unmodified = !modifiedVariables.contains(vi.variable())
                                         && notLinkedToModified(builder, modifiedVariables);
                    builder.removeIf(Link::toIsIntermediateVariable);
                    if (vi.variable() instanceof ReturnVariable) {
                        builder.removeIfFromTo(Expand::isLocalVariable);
                    }
                    if (!vi.analysis().haveAnalyzedValueFor(UNMODIFIED_VARIABLE)) {
                        Value.Bool newValue = ValueImpl.BoolImpl.from(unmodified);
                        vi.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE, newValue);
                    }
                    if (!unmodified) {
                        builder.replaceSubsetSuperset(vi.variable());
                    }
                    Links newLinks = builder.build();
                    if (newLinkedVariables.put(vi.variable(), newLinks) != null) {
                        throw new UnsupportedOperationException("Each real variable must be a primary");
                    }

                });
        return newLinkedVariables;
    }

    static boolean isLocalVariable(Variable v) {
        if (v instanceof AppliedFunctionalInterfaceVariable a) {
            return !a.containsNoLocalVariables();
        }
        return v instanceof LocalVariable;
    }

    // indirection in applied functional interface variable
    // TODO this is shaky code, dedicated to TestStaticBiFunction,6
    //  it may have relevance later
    public Links indirect(Variable primary, Link link, Links links2) {
        Variable v = links2.primary();
        Variable pi = Util.primary(link.to());
        TranslationMap tm2 = new VariableTranslationMap(runtime).put(v, pi);
        Links links2Tm = links2.translate(tm2);
        Map<Variable, Links> linkedVariables = new HashMap<>();
        Links links = new LinksImpl(primary, List.of(link));
        linkedVariables.put(links.primary(), links);
        linkedVariables.put(links2Tm.primary(), links2Tm);
        GraphData gd = makeGraph(linkedVariables);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Indirection graph, primary {}:\n{}", links.primary(), printGraph(gd.graph));
        }
        Links.Builder builder = followGraph(gd, links.primary());
        builder.removeIf(l -> l.to().variableStreamDescend().anyMatch(vv -> vv instanceof ParameterInfo));
        return builder.build();
    }

}
