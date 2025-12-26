package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
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

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.IS_IDENTICAL_TO;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.UNMODIFIED_VARIABLE;

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
        assert graph.size() == graph.keySet().stream().map(v -> v.v.toString()).count();
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
            mergeEdge(graph, primary, LinkNatureImpl.CONTAINS_AS_FIELD, from);
            mergeEdge(graph, from, LinkNatureImpl.IS_FIELD_OF, primary);
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
        if (l.linkNature() == IS_IDENTICAL_TO) {
            Set<V> subsOfFrom = primaryToSub.get(vFrom);
            if (subsOfFrom != null) {
                subsOfFrom.forEach(s ->
                        mergeEdge(graph, s, IS_IDENTICAL_TO, makeComparableSub(vFrom, s, vTo)));
            }
            Set<V> subsOfTo = primaryToSub.get(vTo);
            if (subsOfTo != null) {
                subsOfTo.forEach(s ->
                        mergeEdge(graph, makeComparableSub(vTo, s, vFrom), IS_IDENTICAL_TO, s));
            }
        }
    }

    private V makeComparableSub(V base, V sub, V target) {
        if (sub.v instanceof FieldReference fr && base.v.equals(fr.scopeVariable())) {
            VariableExpression tve = runtime.newVariableExpression(target.v);
            return new V(runtime.newFieldReference(fr.fieldInfo(), tve, fr.fieldInfo().type()));
        }
        TranslationMap tm = runtime.newTranslationMapBuilder().put(base.v, target.v).build();
        Variable newSub = tm.translateVariableRecursively(sub.v);
        return new V(newSub);
    }

    private record GraphData(Map<V, Map<V, LinkNature>> graph,
                             Set<V> primaries,
                             Map<V, V> subToPrimary) {
    }

    private GraphData makeGraph(Map<Variable, Links> linkedVariables, boolean bidirectional) {
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
                        .forEach(l -> addToGraph(l, new V(links.primary()), graph, bidirectional, subs, subToPrimary)));
        return new GraphData(graph, subs.keySet(), subToPrimary);
    }

    private static boolean containsNoLocalVariable(Variable variable) {
        assert variable.variableStreamDescend().noneMatch(v -> v instanceof ReturnVariable) : """
                Return variables should not occur here: the result of LinkMethodCall should never contain them.
                """;
        return variable.variableStreamDescend().noneMatch(v -> v instanceof LocalVariable);
    }

    private record PC(Variable from, LinkNature linkNature, Variable to) {
        @Override
        public @NotNull String toString() {
            return Util.simpleName(from) + " " + linkNature + " " + Util.simpleName(to);
        }
    }

    // sorting is needed to consistently take the same direction for tests
    private static Links.Builder followGraph(GraphData gd,
                                             Variable primary,
                                             TranslationMap translationMap,
                                             boolean allowLocalVariables) {
        V tPrimary = new V(translationMap == null ? primary : translationMap.translateVariableRecursively(primary));
        Links.Builder builder = new LinksImpl.Builder(tPrimary.v);
        var fromList = gd.graph.keySet().stream()
                .filter(v -> org.e2immu.analyzer.modification.prepwork.Util.isPartOf(primary, v.v))
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
            V tFrom = new V(translationMap == null ? from.v : translationMap.translateVariableRecursively(from.v));
            Variable tFromV = tFrom.v;
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
            Variable primaryFrom = Util.primary(tFromV);
            LOGGER.debug("Entries of {}: {}", from, entries);

            for (Map.Entry<V, LinkNature> entry : entries) {
                LinkNature linkNature = entry.getValue();
                Variable toV = entry.getKey().v;
                Variable primaryTo = Util.primary(toV);
                if (linkNature.valid()
                    && (allowLocalVariables || containsNoLocalVariable(toV))
                    // remove internal references (field inside primary to primary or other field in primary)
                    && !primaryTo.equals(primaryFrom)
                    && block.add(new PC(tFromV, linkNature, toV))) {
                    builder.add(tFromV, linkNature, toV);
                    // don't add if the reverse is already present in this builder
                    block.add(new PC(toV, linkNature.reverse(), tFromV));
                    // when adding p.sub < q.sub, don't add p < q.sub, p.sub < q
                    Set<Variable> scopeFrom = Util.scopeVariables(tFromV);
                    for (LinkNature lnUp : linkNature.redundantFromUp()) {
                        for (Variable sv : scopeFrom) {
                            block.add(new PC(sv, lnUp, toV));
                        }
                    }
                    Set<Variable> scopeTo = Util.scopeVariables(toV);
                    for (LinkNature lnUp : linkNature.redundantToUp()) {
                        for (Variable sv : scopeTo) {
                            block.add(new PC(tFromV, lnUp, sv));
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

    private boolean notLinkedToModified(Links.Builder piBuilder, Set<Variable> modifiedVariables) {
        for (Link link : piBuilder) {
            Variable toPrimary = Util.primary(link.to());
            if (modifiedVariables.contains(toPrimary)) {
                if (link.linkNature() == IS_IDENTICAL_TO
                    || link.linkNature() == LinkNatureImpl.CONTAINS_AS_MEMBER) {
                    return false;
                }
                if (link.linkNature() == LinkNatureImpl.SHARES_ELEMENTS) {
                    // FIXME we need to look at the immutability of the variable's type
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
                                      VariableData vd) {
        // copy everything into lv
        Map<Variable, Links> linkedVariables;
        Set<Variable> modifiedVariables = new HashSet<>(modifiedDuringEvaluation);
        if (previousVd == null) {
            linkedVariables = lvIn;
        } else {
            linkedVariables = new HashMap<>(lvIn);
            previousVd.variableInfoStream(stageOfPrevious)
                    .filter(vi -> !(vi.variable() instanceof This))
                    .forEach(vi -> {
                        Links vLinks = vi.linkedVariables();
                        if (vLinks != null) {
                            linkedVariables.merge(vLinks.primary(), vLinks, Links::merge);
                        }
                        Value.Bool unmodified = vi.analysis().getOrNull(UNMODIFIED_VARIABLE, ValueImpl.BoolImpl.class);
                        boolean explicitlyModified = unmodified != null && unmodified.isFalse();
                        if (explicitlyModified) modifiedVariables.add(vi.variable());
                    });
        }
        assert linkedVariables.entrySet().stream().noneMatch(e ->
                e.getKey() instanceof This || e.getValue().primary() == null || e.getValue().primary() instanceof This)
                : "Not linking null or 'this'";

        GraphData gd = makeGraph(linkedVariables, true);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Bi-directional graph for local:\n{}", printGraph(gd.graph));
        }
        Map<Variable, Links> newLinkedVariables = new HashMap<>();
        vd.variableInfoStream(Stage.EVALUATION)
                .filter(vi -> !(vi.variable() instanceof This))
                .forEach(vi -> {
                    Links.Builder builder = followGraph(gd, vi.variable(), null, true);
                    boolean unmodified = !modifiedVariables.contains(vi.variable())
                                         && notLinkedToModified(builder, modifiedVariables);
                    if (!vi.analysis().haveAnalyzedValueFor(UNMODIFIED_VARIABLE)) {
                        Value.Bool newValue = ValueImpl.BoolImpl.from(unmodified);
                        vi.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE, newValue);
                    }
                    if (!unmodified) {
                        builder.replaceSubsetSuperset(vi.variable());
                    }
                    builder.removeIf(Link::toIntermediateVariable);
                    Links newLinks = builder.build();
                    if (newLinkedVariables.put(vi.variable(), newLinks) != null) {
                        throw new UnsupportedOperationException("Each real variable must be a primary");
                    }

                });
        return newLinkedVariables;
    }

    public List<Links> parameters(MethodInfo methodInfo, VariableData vd) {
        if (vd == null) return List.of();

        Map<Variable, Links> linkedVariables = new HashMap<>();
        vd.variableInfoStream()
                .filter(vi -> !(vi.variable() instanceof This))
                .forEach(vi -> {
                    Links vLinks = vi.linkedVariables();
                    if (vLinks != null && vLinks.primary() != null) {
                        assert !(vLinks.primary() instanceof This);
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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Bi-directional graph for parameters:\n{}", printGraph(gd.graph));
        }
        List<Links> linksPerParameter = new ArrayList<>(methodInfo.parameters().size());
        for (ParameterInfo pi : methodInfo.parameters()) {
            Links.Builder builder = followGraph(gd, pi, null, false);
            linksPerParameter.add(builder.build());
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
        vd.variableInfoStream()
                .filter(vi -> !(vi.variable() instanceof This))
                .forEach(vi -> {
                    Links vLinks = vi.linkedVariables();
                    if (vLinks != null) {
                        linkedVariables.merge(vLinks.primary(), vLinks, Links::merge);
                    }
                });

        GraphData gd = makeGraph(linkedVariables, false);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Return graph, primary {}:\n{}", primary, printGraph(gd.graph));
        }
        Links.Builder builder = followGraph(gd, primary, tm, false);

        if (containsNoLocalVariable(primary)) {
            builder.prepend(IS_IDENTICAL_TO, primary);
        }
        return builder.build();
    }

}
