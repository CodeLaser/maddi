package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.impl.localvar.FunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.expression.NullConstant;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.CONTAINS_AS_FIELD;
import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.IS_FIELD_OF;

public record LinkGraph(JavaInspector javaInspector, Runtime runtime, boolean checkDuplicateNames) {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkGraph.class);

    private static boolean mergeEdgeSingle(Map<Variable, Map<Variable, LinkNature>> graph,
                                           Variable from,
                                           LinkNature linkNature,
                                           Variable to) {
        if (from.equals(to)) return false; // safety measure, is technically possible
        Map<Variable, LinkNature> edges = graph.get(from);
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
            LinkNature combined = prev.combine(linkNature, null);
            if (combined != prev) {
                edges.put(to, combined);
            }
        }
        return change;
    }

    private static boolean mergeEdgeBi(Map<Variable, Map<Variable, LinkNature>> graph,
                                       Variable from,
                                       LinkNature linkNature,
                                       Variable to) {
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

    private Variable makeComparableSub(Variable base, Variable sub, Variable target) {
        if (sub instanceof FieldReference fr && base.equals(fr.scopeVariable())) {
            VariableExpression tve = runtime.newVariableExpression(target);
            FieldInfo newField = fr.fieldInfo().withOwner(VariableTranslationMap.owner(runtime,
                    target.parameterizedType()));
            return runtime.newFieldReference(newField, tve, newField.type());
        }
        TranslationMap tm = new VariableTranslationMap(runtime).put(base, target);
        return tm.translateVariableRecursively(sub);
    }

    private boolean addField(Map<Variable, Map<Variable, LinkNature>> graph, Variable from, Variable primary) {
        if (!from.equals(primary) && !(primary instanceof This)
            && from instanceof FieldReference && primary.equals(fieldScope(from))) {
            boolean change = mergeEdgeSingle(graph, primary, CONTAINS_AS_FIELD, from);
            change |= mergeEdgeSingle(graph, from, IS_FIELD_OF, primary);
            return change;
        }
        return false;
    }

    private boolean simpleAddToGraph(Map<Variable, Map<Variable, LinkNature>> graph,
                                     Variable lFrom, LinkNature linkNature, Variable lTo) {

        boolean change = mergeEdgeSingle(graph, lFrom, linkNature, lTo);
        Variable primary = Util.primary(lFrom);
        change |= addField(graph, lFrom, primary);

        // other direction
        change |= mergeEdgeSingle(graph, lTo, linkNature.reverse(), lFrom);
        Variable toPrimary = Util.primary(lTo);
        change |= addField(graph, lTo, toPrimary);
        return change;
    }

    private record Add(Variable from, LinkNature ln, Variable to) {
    }

    Map<Variable, Map<Variable, LinkNature>> makeGraph(Map<Variable, Links> linkedVariables,
                                                       Set<Variable> modifiedInThisEvaluation) {
        Map<Variable, Map<Variable, LinkNature>> graph = new HashMap<>();
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
               graph.size() == graph.keySet().stream().map(v -> stringForDuplicate(v)).distinct().count();
        return graph;
    }

    // see TestModificationParameter, a return variable with the same name as a local variable
    private static String stringForDuplicate(Variable v) {
        if (v instanceof ReturnVariable) return "rv " + v;
        return v.toString();
    }

    private boolean doOneMakeGraphCycle(Map<Variable, Map<Variable, LinkNature>> graph, Set<Variable> modifiedInThisEvaluation) {
        Map<Variable, Set<Variable>> subs = computeSubs(graph, modifiedInThisEvaluation);
        List<Add> newLinks = new ArrayList<>();
        for (Map.Entry<Variable, Map<Variable, LinkNature>> entry : graph.entrySet()) {
            Variable vFrom = entry.getKey();
            for (Map.Entry<Variable, LinkNature> entry2 : entry.getValue().entrySet()) {
                Variable vTo = entry2.getKey();
                LinkNature linkNature = entry2.getValue();
                if (linkNature.isIdenticalToOrAssignedFromTo()) {
                    Set<Variable> subsOfFrom = subs.get(vFrom);
                    if (subsOfFrom != null && vTo.equals(Util.firstRealVariable(vTo)) && isNotNullConstant(vTo)) {
                        for (Variable s : subsOfFrom) {
                            LinkNature ln;
                            if (s instanceof FieldReference fr && Util.isVirtualModificationField(fr.fieldInfo())) {
                                ln = LinkNatureImpl.makeIdenticalTo(null);
                            } else {
                                ln = linkNature;
                            }
                            if (ensureArraysWhenSubIsIndex(vFrom, s, vTo)) {
                                Variable sub = makeComparableSub(vFrom, s, vTo);
                                assert !sub.equals(s);
                                newLinks.add(new Add(s, ln, sub));
                            }
                        }
                    }
                    Set<Variable> subsOfTo = subs.get(vTo);
                    if (subsOfTo != null && vFrom.equals(Util.firstRealVariable(vFrom)) && isNotNullConstant(vFrom)) {
                        for (Variable s : subsOfTo) {
                            LinkNature ln;
                            if (s instanceof FieldReference fr && Util.isVirtualModificationField(fr.fieldInfo())) {
                                ln = LinkNatureImpl.makeIdenticalTo(null);
                            } else {
                                ln = linkNature;
                            }
                            if (ensureArraysWhenSubIsIndex(vTo, s, vFrom)) {
                                Variable sub = makeComparableSub(vTo, s, vFrom);
                                assert !sub.equals(s);
                                newLinks.add(new Add(sub, ln, s));
                            }
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

    // see TestVarious,9; TestVarious2,5
    // must ensure that there are sufficient array capabilities on the target side when the sub is indexing
    private static boolean ensureArraysWhenSubIsIndex(Variable from, Variable sub, Variable target) {
        if (sub.equals(target)) return false;
        if (from instanceof FunctionalInterfaceVariable || target instanceof FunctionalInterfaceVariable) return false;
        if (sub instanceof DependentVariable dv && Util.scopeVariables(dv).contains(from)
            && (!(dv.indexExpression() instanceof IntConstant ic) || ic.constant() >= 0)) {
            return target.parameterizedType().arrays() == from.parameterizedType().arrays();
        }
        Set<FieldInfo> subFields = Util.fieldsOf(sub).collect(Collectors.toUnmodifiableSet());
        Set<FieldInfo> targetFields = Util.fieldsOf(target).collect(Collectors.toUnmodifiableSet());
        return Collections.disjoint(subFields, targetFields);
    }

    private static boolean isNotNullConstant(Variable v) {
        return !(v instanceof MarkerVariable mv) || !mv.isConstant() || !(mv.assignmentExpression() instanceof NullConstant);
    }

    private @NotNull Map<Variable, Set<Variable>> computeSubs(Map<Variable, Map<Variable, LinkNature>> graph,
                                                              Set<Variable> modifiedInThisEvaluation) {
        Map<Variable, Set<Variable>> subs = new HashMap<>();
        for (Map.Entry<Variable, Map<Variable, LinkNature>> entry : graph.entrySet()) {
            Variable from = entry.getKey();
            Set<Variable> scopeVariablesFrom = Util.scopeVariables(from);
            for (Variable scopeVariableFrom : scopeVariablesFrom) {
                subs.computeIfAbsent(scopeVariableFrom, _ -> new HashSet<>()).add(from);
            }
            for (Map.Entry<Variable, LinkNature> entry2 : entry.getValue().entrySet()) {
                Variable vTo = entry2.getKey();
                Set<Variable> scopeVariablesTo = Util.scopeVariables(vTo);
                for (Variable scopeVariableTo : scopeVariablesTo) {
                    subs.computeIfAbsent(scopeVariableTo, _ -> new HashSet<>()).add(vTo);
                }
            }
            if (modifiedInThisEvaluation.contains(from)
                && Util.firstRealVariable(from).equals(from)
                && Util.hasVirtualFields(from)) {
                // FIXME we should add the current type!
                Value.Immutable immutable = new AnalysisHelper().typeImmutable(from.parameterizedType());
                if (immutable.isMutable()) {
                    // add the mutation field
                    FieldInfo vf = new VirtualFieldComputer(javaInspector)
                            .newMField(VariableTranslationMap.owner(runtime, from.parameterizedType()));
                    FieldReference mutationFr = runtime().newFieldReference(vf, runtime.newVariableExpression(from),
                            vf.type());
                    subs.computeIfAbsent(from, _ -> new HashSet<>()).add(mutationFr);
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
    static Links.Builder followGraph(VirtualFieldComputer virtualFieldComputer,
                                     Map<Variable, Map<Variable, LinkNature>> graph, Variable primary,
                                     Set<MethodInfo> causesOfModification) {
        Links.Builder builder = new LinksImpl.Builder(primary);
        var fromList = graph.keySet().stream()
                .filter(v -> Util.isPartOf(primary, v))
                .sorted((v1, v2) -> {
                    if (Util.isPartOf(v1, v2)) return 1;
                    if (Util.isPartOf(v2, v1)) return -1;
                    return v1.fullyQualifiedName().compareTo(v2.fullyQualifiedName());
                })
                .toList();

        // stream.§$s⊆0:in.§$s
        Set<PC> block = new HashSet<>();

        for (Variable from : fromList) {
            Map<Variable, LinkNature> all = bestPath(graph, from, causesOfModification);
            List<Map.Entry<Variable, LinkNature>> entries = all.entrySet().stream()
                    .sorted((e1, e2) -> {
                        int c = e2.getValue().rank() - e1.getValue().rank();
                        if (c != 0) return c;
                        boolean p1 = Util.isPrimary(e1.getKey());
                        boolean p2 = Util.isPrimary(e2.getKey());
                        if (p1 && !p2) return 1;
                        if (p2 && !p1) return -1;
                        // subs first, best score first
                        return e1.getKey().fullyQualifiedName().compareTo(e2.getKey().fullyQualifiedName());
                    })
                    .toList();
            Variable primaryFrom = Util.primary(from);
            if (primaryFrom == null) continue; //array expression not a variable
            Variable firstRealFrom = Util.firstRealVariable(from);

            //LOGGER.debug("Entries of {}: {}", from, entries);

            for (Map.Entry<Variable, LinkNature> entry : entries) {
                LinkNature linkNature = entry.getValue();
                Variable to = entry.getKey();
                Variable primaryTo = Util.primary(to);
                if (primaryTo == null) continue;//array expression not a variable
                Variable firstRealTo = Util.firstRealVariable(to);
                // remove internal references (field inside primary to primary or other field in primary)
                // see TestStaticValues1,5 for an example where s.k ← s.r.i, which requires the 2nd clause
                if (linkNature.rank() >= 0
                    && (!primaryTo.equals(primaryFrom) ||
                        !firstRealFrom.equals(primaryFrom) &&
                        !firstRealTo.equals(primaryTo) &&
                        !firstRealFrom.equals(firstRealTo))
                    && LinksImpl.LinkImpl.noRelationBetweenMAndOtherVirtualFields(from, to)
                    && block.add(new PC(from, linkNature, to))) {
                    builder.add(from, linkNature, to);
                    if (linkNature.isIdenticalToOrAssignedFromTo()
                        && !(primaryTo instanceof ReturnVariable) && !(primaryFrom instanceof ReturnVariable)
                        && !Util.virtual(from)
                        && !Util.virtual(to)
                        && virtualFieldComputer != null) {
                        VirtualFieldComputer.M2 m2 = virtualFieldComputer.addModificationFieldEquivalence(from, to);
                        LinkNature id = LinkNatureImpl.makeIdenticalTo(null);
                        if (m2 != null && !builder.contains(m2.m1(), id, m2.m2())) {
                            builder.add(m2.m1(), id, m2.m2());
                        }
                    }
                    // don't add if the reverse is already present in this builder
                    block.add(new PC(to, linkNature.reverse(), from));
                    // when adding p.sub < q.sub, don't add p < q.sub, p.sub < q
                    Set<Variable> scopeFrom = Util.scopeVariables(from);
                    for (LinkNature lnUp : linkNature.redundantFromUp()) {
                        for (Variable sv : scopeFrom) {
                            block.add(new PC(sv, lnUp, to));
                        }
                    }
                    Set<Variable> scopeTo = Util.scopeVariables(to);
                    for (LinkNature lnUp : linkNature.redundantToUp()) {
                        for (Variable sv : scopeTo) {
                            block.add(new PC(from, lnUp, sv));
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

    static Map<Variable, LinkNature> bestPath(Map<Variable, Map<Variable, LinkNature>> graph,
                                              Variable start,
                                              Set<MethodInfo> causesOfModification) {
        Map<Variable, Set<LinkNature>> res =
                FixpointPropagationAlgorithm.computePathLabels(s -> graph.getOrDefault(s, Map.of()),
                        graph.keySet(), start, LinkNatureImpl.EMPTY,
                        (ln1, ln2) -> ln1.combine(ln2, causesOfModification));
        return res.entrySet().stream()
                .filter(e -> !start.equals(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        e -> e.getValue().stream().reduce(LinkNatureImpl.EMPTY, LinkNature::best)));
    }

    //-------------------------------------------------------------------------------------------------

    static String printGraph(Map<Variable, Map<Variable, LinkNature>> graph) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Variable, Map<Variable, LinkNature>> e : graph.entrySet()) {
            for (Map.Entry<Variable, LinkNature> e2 : e.getValue().entrySet()) {
                sb.append(Util.simpleName(e.getKey())).append(" ").append(e2.getValue()).append(" ")
                        .append(Util.simpleName(e2.getKey())).append("\n");
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
    Map<Variable, Map<Variable, LinkNature>> compute(Map<Variable, Links> lvIn,
                                       VariableData previousVd,
                                       Stage stageOfPrevious,
                                       VariableData vd,
                                       TranslationMap replaceConstants,
                                       Map<Variable, Set<MethodInfo>> modifiedInThisEvaluation) {
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
                                    .removeIfTo(v -> {
                                        Variable primary = Util.primary(v);
                                        // primary == null check: TestVarious,10
                                        return primary == null || !vd.isKnown(primary.fullyQualifiedName())
                                                                  && !LinkVariable.acceptForLinkedVariables(v);
                                    });
                            linkedVariables.merge(vLinks.primary(), translated, Links::merge);
                        }
                    });
        }

        Map<Variable, Map<Variable, LinkNature>> graph = makeGraph(linkedVariables, modifiedInThisEvaluation.keySet());
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
        Map<Variable, Map<Variable, LinkNature>> graph = makeGraph(linkedVariables, Set.of());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Indirection graph, primary {}:\n{}", links.primary(), printGraph(graph));
        }
        Links.Builder builder = followGraph(null, graph, links.primary(), Set.of());
        builder.removeIf(l -> l.to().variableStreamDescend().anyMatch(vv -> vv instanceof ParameterInfo));
        return builder.build();
    }

}
