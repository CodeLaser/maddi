package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;
import static org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer.isVirtualModificationField;

public record LinkGraph(JavaInspector javaInspector, Runtime runtime, boolean checkDuplicateNames) {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkGraph.class);

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

    private static boolean mergeEdgeSingle(Map<V, Map<V, LinkNature>> graph,
                                           V from,
                                           LinkNature linkNature,
                                           V to) {
        if (from.equals(to)) return false; // safety measure, is technically possible
        Map<V, LinkNature> edges = graph.get(from);
        boolean change = false;
        if (edges == null) {
            edges = new HashMap<>();
            graph.put(from, edges);
            change = true;
        }
        LinkNature prev = edges.put(to, linkNature);
        if (prev == null) {
            change = true;
        } else {
            LinkNature combined = prev.combine(linkNature);
            if (combined != prev) {
                edges.put(to, combined);
            }
        }
        return change;
    }

    private static boolean mergeEdgeBi(Map<V, Map<V, LinkNature>> graph,
                                       V from,
                                       LinkNature linkNature,
                                       V to) {
        boolean change = mergeEdgeSingle(graph, from, linkNature, to);
        LinkNature ln2 = linkNature.reverse();
        if (ln2 != linkNature) {
            change |= mergeEdgeSingle(graph, to, ln2, from);
        }
        return change;
    }

    private static Variable fieldScope(Variable v) {
        if (v instanceof FieldReference fr) {
            if (fr.scopeVariable() instanceof This) return v;
            if (fr.scopeVariable() != null) return fieldScope(fr.scopeVariable());
        }
        return v;
    }

    private V makeComparableSub(V base, V sub, V target) {
        if (sub.v instanceof FieldReference fr && base.v.equals(fr.scopeVariable())) {
            VariableExpression tve = runtime.newVariableExpression(target.v);
            FieldInfo newField = fr.fieldInfo().withOwner(Util.owner(target.v));
            return new V(runtime.newFieldReference(newField, tve, newField.type()));
        }
        TranslationMap tm = new VariableTranslationMap(runtime).put(base.v, target.v);
        Variable newSub = tm.translateVariableRecursively(sub.v);
        return new V(newSub);
    }

    private boolean addField(Map<V, Map<V, LinkNature>> graph, V from, V primary) {
        if (!from.equals(primary) && !(primary.v instanceof This)
            && from.v instanceof FieldReference && primary.v.equals(fieldScope(from.v))) {
            boolean change = mergeEdgeSingle(graph, primary, CONTAINS_AS_FIELD, from);
            change |= mergeEdgeSingle(graph, from, IS_FIELD_OF, primary);
            return change;
        }
        return false;
    }

    private boolean simpleAddToGraph(Map<V, Map<V, LinkNature>> graph,
                                     Variable lFrom, LinkNature linkNature, Variable lTo) {
        V vFrom = new V(lFrom);
        V vTo = new V(lTo);

        boolean change = mergeEdgeSingle(graph, vFrom, linkNature, vTo);
        V primary = new V(Util.primary(vFrom.v));
        change |= addField(graph, vFrom, primary);

        // other direction
        change |= mergeEdgeSingle(graph, vTo, linkNature.reverse(), vFrom);
        V toPrimary = new V(Util.primary(vTo.v));
        change |= addField(graph, vTo, toPrimary);
        return change;
    }

    private record Add(V from, LinkNature ln, V to) {
    }

    Map<V, Map<V, LinkNature>> makeGraph(Map<Variable, Links> linkedVariables,
                                         Set<Variable> modifiedInThisEvaluation) {
        Map<V, Map<V, LinkNature>> graph = new HashMap<>();
        linkedVariables.values().forEach(links -> links.forEach(
                l -> simpleAddToGraph(graph, l.from(), l.linkNature(), l.to())));
        boolean change = true;
        int cycleProtection = 0;
        while (change) {
            ++cycleProtection;
            if (cycleProtection > 10) {
                throw new UnsupportedOperationException("cycle protection");
            }
            change = doOneMakeGraphCycle(graph, modifiedInThisEvaluation);
        }
        assert !checkDuplicateNames ||
               graph.size() == graph.keySet().stream().map(v -> stringForDuplicate(v.v)).distinct().count();
        return graph;
    }

    // see TestModificationParameter, a return variable with the same name as a local variable
    private static String stringForDuplicate(Variable v) {
        if (v instanceof ReturnVariable) return "rv " + v;
        return v.toString();
    }

    private boolean doOneMakeGraphCycle(Map<V, Map<V, LinkNature>> graph, Set<Variable> modifiedInThisEvaluation) {
        Map<V, Set<V>> subs = computeSubs(graph, modifiedInThisEvaluation);
        List<Add> newLinks = new ArrayList<>();
        for (Map.Entry<V, Map<V, LinkNature>> entry : graph.entrySet()) {
            V vFrom = entry.getKey();
            for (Map.Entry<V, LinkNature> entry2 : entry.getValue().entrySet()) {
                V vTo = entry2.getKey();
                LinkNature linkNature = entry2.getValue();
                if (linkNature.isIdenticalTo()) {
                    Set<V> subsOfFrom = subs.get(vFrom);
                    if (subsOfFrom != null) {
                        for (V s : subsOfFrom) {
                            LinkNature ln;
                            if (s.v instanceof FieldReference fr && isVirtualModificationField(fr.fieldInfo())) {
                                ln = IS_IDENTICAL_TO;
                            } else {
                                ln = linkNature;
                            }
                            V sub = makeComparableSub(vFrom, s, vTo);
                            assert !sub.equals(s);
                            newLinks.add(new Add(s, ln, sub));
                        }
                    }
                    Set<V> subsOfTo = subs.get(vTo);
                    if (subsOfTo != null) {
                        for (V s : subsOfTo) {
                            LinkNature ln;
                            if (s.v instanceof FieldReference fr && isVirtualModificationField(fr.fieldInfo())) {
                                ln = IS_IDENTICAL_TO;
                            } else {
                                ln = linkNature;
                            }
                            V sub = makeComparableSub(vTo, s, vFrom);
                            assert !sub.equals(s);
                            newLinks.add(new Add(sub, ln, s));
                        }
                    }
                }
            }
        }
        boolean change = false;
        for (Add add : newLinks) {
            change |= mergeEdgeBi(graph, add.from, add.ln, add.to);
        }
        List<PC> extra = new ExpandSlice().completeSliceInformation(graph);
        for (PC pc : extra) {
            change |= simpleAddToGraph(graph, pc.from, pc.linkNature, pc.to);
        }
        return change;
    }

    private @NotNull Map<V, Set<V>> computeSubs(Map<V, Map<V, LinkNature>> graph,
                                                Set<Variable> modifiedInThisEvaluation) {
        Map<V, Set<V>> subs = new HashMap<>();
        for (Map.Entry<V, Map<V, LinkNature>> entry : graph.entrySet()) {
            V vFrom = entry.getKey();
            Set<Variable> scopeVariablesFrom = Util.scopeVariables(vFrom.v);
            for (Variable scopeVariableFrom : scopeVariablesFrom) {
                subs.computeIfAbsent(new V(scopeVariableFrom), _ -> new HashSet<>()).add(vFrom);
            }
            for (Map.Entry<V, LinkNature> entry2 : entry.getValue().entrySet()) {
                V vTo = entry2.getKey();
                Set<Variable> scopeVariablesTo = Util.scopeVariables(vTo.v);
                for (Variable scopeVariableTo : scopeVariablesTo) {
                    subs.computeIfAbsent(new V(scopeVariableTo), _ -> new HashSet<>()).add(vTo);
                }
            }
            if (modifiedInThisEvaluation.contains(vFrom.v)
                && Util.firstRealVariable(vFrom.v).equals(vFrom.v)
                && Util.hasVirtualFields(vFrom.v)) {
                // FIXME we should add the current type!
                Value.Immutable immutable = new AnalysisHelper().typeImmutable(vFrom.v.parameterizedType());
                if (immutable.isMutable()) {
                    // add the mutation field
                    FieldInfo vf = new VirtualFieldComputer(javaInspector)
                            .newMField(vFrom.v.parameterizedType().typeInfo());
                    FieldReference mutationFr = runtime().newFieldReference(vf, runtime.newVariableExpression(vFrom.v),
                            vf.type());
                    subs.computeIfAbsent(vFrom, _ -> new HashSet<>()).add(new V(mutationFr));
                }
            }
        }
        return subs;
    }

    record PC(Variable from, LinkNature linkNature, Variable to) {
        @Override
        public @NotNull String toString() {
            return Util.simpleName(from) + " " + linkNature + " " + Util.simpleName(to);
        }
    }

    // sorting is needed to consistently take the same direction for tests
    static Links.Builder followGraph(Map<V, Map<V, LinkNature>> graph, Variable primary) {
        V tPrimary = new V(primary);
        Links.Builder builder = new LinksImpl.Builder(tPrimary.v);
        var fromList = graph.keySet().stream()
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
            Map<V, LinkNature> all = bestPath(graph, from);
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

    static Map<V, LinkNature> bestPath(Map<V, Map<V, LinkNature>> graph, V start) {
        Map<V, Set<LinkNature>> res =
                FixpointPropagationAlgorithm.computePathLabels(s -> graph.getOrDefault(s, Map.of()),
                        graph.keySet(), start, LinkNatureImpl.EMPTY, LinkNature::combine);
        return res.entrySet().stream()
                .filter(e -> !start.equals(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        e -> e.getValue().stream().reduce(LinkNatureImpl.EMPTY, LinkNature::best)));
    }

    //-------------------------------------------------------------------------------------------------

    static String printGraph(Map<V, Map<V, LinkNature>> graph) {
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
    Map<V, Map<V, LinkNature>> compute(Map<Variable, Links> lvIn,
                                       VariableData previousVd,
                                       Stage stageOfPrevious,
                                       VariableData vd,
                                       TranslationMap replaceConstants,
                                       Set<Variable> modifiedInThisEvaluation) {
        // copy everything into lv
        Map<Variable, Links> linkedVariables = new HashMap<>();
        lvIn.entrySet().stream()
                .filter(e -> !(e.getKey() instanceof This))
                .filter(e -> e.getValue().primary() != null)
                .forEach(e -> linkedVariables.put(e.getKey(), e.getValue().translate(replaceConstants)));
        if (previousVd != null) {
            previousVd.variableInfoStream(stageOfPrevious)
                    .filter(vi -> !(vi.variable() instanceof This))
                    .filter(vi -> vd.isKnown(vi.variable().fullyQualifiedName()))
                    .forEach(vi -> {
                        Links vLinks = vi.linkedVariables();
                        if (vLinks != null && vLinks.primary() != null) {
                            assert !(vLinks.primary() instanceof This);
                            Links translated = vLinks.translate(replaceConstants)
                                    .removeIfTo(v -> !vd.isKnown(Util.primary(v).fullyQualifiedName())
                                                     && !LinkVariable.acceptForLinkedVariables(v));
                            linkedVariables.merge(vLinks.primary(), translated, Links::merge);
                        }
                    });
        }

        Map<V, Map<V, LinkNature>> graph = makeGraph(linkedVariables, modifiedInThisEvaluation);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Bi-directional graph for local:\n{}", printGraph(graph));
        }
        return graph;
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
        Map<V, Map<V, LinkNature>> graph = makeGraph(linkedVariables, Set.of());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Indirection graph, primary {}:\n{}", links.primary(), printGraph(graph));
        }
        Links.Builder builder = followGraph(graph, links.primary());
        builder.removeIf(l -> l.to().variableStreamDescend().anyMatch(vv -> vv instanceof ParameterInfo));
        return builder.build();
    }

}
