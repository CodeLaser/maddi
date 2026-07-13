package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.link.impl.graph.Fact;
import org.e2immu.analyzer.modification.link.impl.linkgraph.FollowGraph;
import org.e2immu.analyzer.modification.link.impl.linkgraph.RedundantLinks;
import org.e2immu.analyzer.modification.link.impl.localvar.IntermediateVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.SharedVariable;
import org.e2immu.analyzer.modification.link.impl.translate.VariableTranslationMap;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.LinkNatureImpl.*;
import static org.e2immu.analyzer.modification.prepwork.variable.impl.VariableInfoImpl.UNMODIFIED_VARIABLE;

class WriteLinksAndModification {
    private final Runtime runtime;
    private final VirtualFieldComputer virtualFieldComputer;
    private final FollowGraph followGraph;

    WriteLinksAndModification(JavaInspector javaInspector,
                              VirtualFieldComputer virtualFieldComputer,
                              FollowGraph followGraph) {
        this.runtime = javaInspector.runtime();
        this.virtualFieldComputer = virtualFieldComputer;
        this.followGraph = followGraph;
    }

    record WriteResult(Map<Variable, Links> newLinks, Set<Variable> modifiedOutsideVariableData, int newLinksSize) {
    }

    @NotNull WriteResult go(Statement statement,
                            boolean lastStatement,
                            VariableData vd,
                            Set<Variable> previouslyModified,
                            Map<Variable, Set<MethodInfo>> modifiedDuringEvaluation) {
        Set<Variable> unmarkedModifications = new HashSet<>(modifiedDuringEvaluation.keySet());
        Map<Variable, Links.Builder> newLinkedVariables = new HashMap<>();
        List<Link> toRemove = new ArrayList<>();
        if (System.getenv("SVDUMP") != null) {
            System.out.println("SVDUMP stmt " + statement.source().index() + "\n"
                               + followGraph.graph().printShared(Object::toString));
            System.out.println("SVDUMP-MOD stmt " + statement.source().index() + " "
                               + modifiedDuringEvaluation.keySet());
        }

        Map<Variable, Set<MethodInfo>> expandedModifiedDuringEvaluation = new HashMap<>();
        for (Map.Entry<Variable, Set<MethodInfo>> entry : modifiedDuringEvaluation.entrySet()) {
            for (Variable v : followGraph.graph().allShared(entry.getKey())) {
                expandedModifiedDuringEvaluation.put(v, entry.getValue());
            }
            // a modified key that never existed as a graph vertex (ldIn.variables[1], marked through a
            // functional-interface call) still denotes the same runtime slot as its source-chain group faces
            // ({matrix, 0:ld.variables[1]}); expand through the derived-face composition as well
            for (Variable v : followGraph.graph().derivedShared(entry.getKey())) {
                expandedModifiedDuringEvaluation.put(v, entry.getValue());
            }
        }

        RedundantLinks redundantLinks = new RedundantLinks();
        for (VariableInfo vi : vd.variableInfoIterable(Stage.EVALUATION)) {
            toRemove.addAll(doVariableReturnRecompute(statement, lastStatement, vi, unmarkedModifications,
                    previouslyModified, expandedModifiedDuringEvaluation, newLinkedVariables, redundantLinks));
        }
        /*
         toRemove now contains links that should change from ⊆, ⊇ to ~, see e.g. TestConstructor,1; TestDependent,1

         The complication is that we must update the graph, and some of the results, but not all, because some results
         are "before the modification", such as ∈ in a removeFirst() operation on a list (TestDependent,1),
         and the ⊆ to ~ is an "after the modification" operation.

         The solution here is to selectively remove the correct edges from the graph, and to rely
         on the 'best path' to solve the problem. TestDependent,2 shows the way. TestList2,2 shows a variant
         of the problem, with assignment order.
         */
        if (!toRemove.isEmpty()) {
            Set<Variable> affected = new HashSet<>();
            for (Link link : toRemove) {
                Set<Variable> set = followGraph.graph()
                        .replaceReturnAffected(link.from(), link.to(), link.linkNature(), SHARES_ELEMENTS);
                affected.add(link.from());
                affected.add(link.to());
                updateNewLinks(newLinkedVariables, link);
            }
            // assert !affected.isEmpty();
            followGraph.graph().recompute(affected, statement.source().index(), this::acceptRemoval);
        }
        Map<Variable, Links> builtNewLinkedVariables = new HashMap<>();
        int sum = newLinkedVariables.entrySet().stream().mapToInt(e -> {
            Links links = e.getValue().sort().build();
            builtNewLinkedVariables.put(e.getKey(), links);
            return links.size();
        }).sum();
        return new WriteResult(builtNewLinkedVariables, unmarkedModifications, sum);
    }

    // FIXME should be replaced by real code that goes as far as §m equivalence: anything @Dependent
    //  should be removed
    private boolean acceptRemoval(Fact<Variable, LinkNature> fact) {
        return fact.label() == IS_SUBSET_OF || fact.label() == IS_SUPERSET_OF;
    }

    private void updateNewLinks(Map<Variable, Links.Builder> newLinkedVariables, Link toUpdate) {
        Variable primary = Util.primary(toUpdate.from());
        Links.Builder builder = newLinkedVariables.computeIfAbsent(primary, _ -> new LinksImpl.Builder(primary));
        builder.replace(toUpdate, SHARES_ELEMENTS);
    }

    /*
    It is possible that 2 real variables are represented by one shared variable.
    It is equally possible that one real variable refers is composed of multiple shared variables.

    On top of that, we add the relevant modification links kept in VirtualModificationIdenticals.
    */
    private List<Link> doVariableReturnRecompute(Statement statement,
                                                 boolean lastStatement,
                                                 VariableInfo vi,
                                                 Set<Variable> unmarkedModifications,
                                                 Set<Variable> previouslyModified,
                                                 Map<Variable, Set<MethodInfo>> modifiedInThisEvaluation,
                                                 Map<Variable, Links.Builder> newLinkedVariables,
                                                 RedundantLinks redundantLinks) {
        Variable variable = vi.variable();
        unmarkedModifications.remove(variable);

        Links.Builder builder2;
        // step one, multiple real variables map onto a single shared variable
        Variable toFollow = followGraph.graph().translateForward(variable);
        Links.Builder builder1 = followGraph.followGraph(virtualFieldComputer, toFollow);
        if (toFollow != variable) {
            builder2 = new LinksImpl.Builder(variable);
            // A pure assignment source (a value that flows INTO the collapsed variable, e.g. 'alternative' in
            // 'x ← alternative') must not inherit the rep's incoming edges: 'rep ← optional.§x' belongs to the
            // recipient x (and whatever x flows into), not to the source. Attributing it would produce the spurious
            // 'alternative ← optional.§x'. So drop the rep's incoming-assignment edges when rehoming onto a source.
            boolean pureSource = followGraph.graph().isPureAssignmentSource(variable);
            builder1.linkSet().forEach(link -> {
                if (pureSource && link.linkNature().isAssignedFrom() && link.from().equals(toFollow)) return;
                VariableTranslationMap vtm = new VariableTranslationMap(runtime);
                vtm.put(toFollow, variable);
                Link translated = link.translateFrom(vtm);
                builder2.add(translated.from(), translated.linkNature(), translated.to());
            });
        } else {
            builder2 = builder1;
        }

        Links.Builder builder = new LinksImpl.Builder(builder2.primary());
        // components of the variable can also be part of the shared variables...
        for (Link link : builder2.linkSet()) {
            // Directionality: on an assignment edge the recipient side (the 'from' of ←, the 'to' of →) must expand
            // only to recipient members of the rep, not to a pure source. 'optional.§x → rep' ({x, alternative})
            // means optional.§x flowed into x, not into the source 'alternative'; expanding to 'alternative' would
            // produce the spurious 'optional.§x → alternative'.
            boolean fromRecipient = link.linkNature().isAssignedFrom();
            boolean toRecipient = link.linkNature() == IS_ASSIGNED_TO;
            expandShared(link.from(), fromRecipient).forEach(from -> expandShared(link.to(), toRecipient)
                    .forEach(to -> {
                        // Expanding a shared-variable rep can surface two bad links that the rep previously masked:
                        // (1) an internal self-field link ('choose ≻ $__sv_s1' → 'choose ≻ choose.s1'), which
                        //     FollowGraph's internal-reference filter would have dropped;
                        // (2) an INVALID containment: a 'field ← param' collapse groups the field with the param, so
                        //     'wrap ≻ $__sv_v' expands to both 'wrap ≻ wrap.v' (valid, internal) and 'wrap ≻ 0:y'
                        //     (invalid — 0:y is the value source, not a field of wrap). A ≻/≺ asserts the field is
                        //     part of the container; drop the expansion when that does not hold.
                        if (!isInternalSelfFieldLink(from, to)
                            && !isInvalidFieldContainment(from, link.linkNature(), to)) {
                            builder.add(from, link.linkNature(), to);
                        }
                    }));
        }
        // reconstruct intra-group shared-variable assignment edges (the collapse stores 'field ← param' once;
        // e.g. a record constructor's this.s1 ← 0:s1). Unlike the §m fold below we do not gate on
        // containsPrimaryOf: the summary of a both-fresh group has no external edge yet to anchor to.
        followGraph.graph().sharedAssignmentEdgeStream(variable)
                // rank-desc, FollowGraph's convention: when the stream carries both directions of a pair
                // ('s.r.j → s.k' and 's.k ← s.r.j'), the higher-ranked → is processed first and wins the dedup
                .sorted((l1, l2) -> l2.linkNature().rank() - l1.linkNature().rank())
                .filter(link -> !builder.contains(link.from(), link.linkNature(), link.to())
                                // reverse-dedup, mirroring FollowGraph's block: if 's.r.j → s.k' is already in the
                                // builder, do not also add the reconstructed 's.k ← s.r.j'
                                && !builder.contains(link.to(), link.linkNature().reverse(), link.from()))
                .forEach(link -> {
                    builder.add(link.from(), link.linkNature(), link.to());
                    // a real assignment graph edge gets its §m modification-equivalence generated in FollowGraph;
                    // a reconstructed intra-group edge bypasses it, so add it here too (this.list ← 0:l yields
                    // 0:l.§m ≡ this.list.§m). Same guards as FollowGraph: skip return values and virtual fields.
                    Variable f = link.from(), t = link.to();
                    if (!(Util.primary(f) instanceof ReturnVariable) && !(Util.primary(t) instanceof ReturnVariable)
                        && !Util.virtual(f) && !Util.virtual(t) && virtualFieldComputer != null) {
                        VirtualFieldComputer.M2 m2 = virtualFieldComputer.addModificationFieldEquivalence(f, t);
                        LinkNature id = LinkNatureImpl.makeIdenticalTo(null);
                        if (m2 != null && !builder.contains(m2.m1(), id, m2.m2())) {
                            builder.add(m2.m1(), id, m2.m2());
                        }
                    }
                });
        // finally, modification edges
        followGraph.graph().virtualModificationEdgeStream(variable)
                .filter(link -> builder.containsPrimaryOf(link.to()))
                .filter(link -> !builder.contains(link.from(), link.linkNature(), link.to()))
                .forEach(link -> builder.add(link.from(), link.linkNature(), link.to()));

        dedupReversePairs(builder);
        List<Link> toRemove = new ArrayList<>();
        if (variable instanceof ReturnVariable rv) {
            // A coarse scope-up link (copy ≈ 0:pair) is redundant once the finer link (copy.f ← 0:pair.f) exists.
            // FollowGraph suppresses such redundancy within its own builder, but the finer field links can arrive
            // from a different builder (the shared-variable reconstruct), which FollowGraph's block never sees. So
            // re-apply the redundancy suppression across the fully-assembled return builder.
            suppressRedundantScopeUps(builder);
            // return variables will always be complete
            handleReturnVariable(rv, builder);
        } else {
            // cross-variable transitive-redundancy suppression, ported from the pre-sv engine: keep the nearest
            // hop, drop the origin ('stream1.§xs⊆stream.§xs' stays, the transitive 'stream1.§xs⊆0:in.§xs' goes,
            // because 'stream.§xs⊆0:in.§xs' was already emitted for an earlier variable of this statement).
            // Returns stay complete (handled above); in the last statement, parameters stay complete too — the
            // method summary reads them there.
            if (System.getenv("NORL") == null
                && (!lastStatement || !(variable instanceof org.e2immu.language.cst.api.info.ParameterInfo))) {
                redundantLinks.redundantLinks(builder);
            }
            boolean unmodified =
                    variable.isIgnoreModifications()
                    ||
                    !previouslyModified.contains(variable)
                    && (assignedInThisStatement(statement, vi)
                        || !modifiedInThisEvaluation.containsKey(variable)
                           // all the §m links
                           && notLinkedToModifiedVirtualModification(variable, toFollow, modifiedInThisEvaluation)
                           // and other links such as ≺ IS_FIELD_OF
                           && notLinkedToModified(builder, modifiedInThisEvaluation));
            builder.removeIf(WriteLinksAndModification::notInLinkedVariables);

            if (variable instanceof This) {
                // only keep direct links for "this", the others are replicated in its fields
                builder.removeIf(l -> !(l.from() instanceof This));
            }
            Value.Bool newValue = ValueImpl.BoolImpl.from(unmodified);
            vi.analysis().setAllowControlledOverwrite(UNMODIFIED_VARIABLE, newValue);

            if (!unmodified) {
                // ⊆, ⊇ become ~ after a modification
                builder.linkSet().forEach(link -> {
                    if (link.linkNature() == IS_SUBSET_OF || link.linkNature() == IS_SUPERSET_OF) {
                        toRemove.add(link);
                        toRemove.add(new LinksImpl.LinkImpl(link.to(), link.linkNature().reverse(), link.from()));
                    }
                });
            }
        }
        if (newLinkedVariables.put(variable, builder) != null) {
            throw new UnsupportedOperationException("Each real variable must be a primary");
        }
        return toRemove;
    }

    private static boolean notInLinkedVariables(Link l) {
        return Util.lvPrimaryOrNull(l.to()) instanceof IntermediateVariable
               || l.to() instanceof MarkerVariable mv && mv.isConstant()
                  && !(l.linkNature().equals(IS_ASSIGNED_FROM) || l.linkNature().equals(CONTAINS_AS_MEMBER))
               || l.from() instanceof MarkerVariable mvf && mvf.isConstant()
                  && !(l.linkNature().equals(IS_ASSIGNED_TO) || l.linkNature().equals(IS_ELEMENT_OF));
    }

    // remove any coarse scope-up link that a finer link in the same builder makes redundant (mirrors the
    // redundantFromUp/redundantToUp/redundantUp suppression in FollowGraph, but applied across builders): drop
    // 'coarse' when some 'fine' link has coarse.from/​to in its from/to scope and coarse's nature among fine's
    // scope-up natures (e.g. 'copy ≈ 0:pair' given 'copy.f ← 0:pair.f').
    /*
     Links enter the assembled builder from several paths (FollowGraph, the shared-variable reconstruct, the §m
     folds, rep expansion); FollowGraph's reverse-block only sees its own emissions. Remove exact duplicates and,
     for a pair present in BOTH directions ('b.variables[0] ∈ b.variables' and 'b.variables ∋ b.variables[0]'),
     keep the link keyed on the DEEPER from-side — FollowGraph's parts-first fromList convention ('∈' keyed on
     the element wins over '∋' keyed on the container; 's.r.j → s.k' wins over 's.k ← s.r.j'). Survivors keep
     their insertion order (output order is asserted by the tests).
     */
    private void dedupReversePairs(Links.Builder builder) {
        List<Link> links = new ArrayList<>(builder.linkSet());
        // removals tracked BY INDEX: Link is a record with value equality, so equal duplicates would otherwise
        // all be swept by one removal (that bug dropped both ∈ copies while keeping the ∋)
        boolean[] removed = new boolean[links.size()];
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < links.size(); i++) {
            Link link = links.get(i);
            String key = link.from() + "|" + link.linkNature() + "|" + link.to();
            if (!seen.add(key)) removed[i] = true; // exact duplicate: keep the first
        }
        for (int i = 0; i < links.size(); i++) {
            if (removed[i]) continue;
            Link a = links.get(i);
            for (int j = i + 1; j < links.size(); j++) {
                if (removed[j]) continue;
                Link b = links.get(j);
                if (a.from().equals(b.to()) && a.to().equals(b.from())
                    && a.linkNature().reverse().equals(b.linkNature())) {
                    if (depth(a.from()) >= depth(b.from())) removed[j] = true;
                    else removed[i] = true;
                }
            }
        }
        boolean any = false;
        for (boolean r : removed) any |= r;
        if (any) {
            builder.removeIf(_ -> true);
            for (int i = 0; i < links.size(); i++) {
                if (!removed[i]) {
                    Link link = links.get(i);
                    builder.add(link.from(), link.linkNature(), link.to());
                }
            }
        }
    }

    // structural depth of a variable's access chain: b=0, b.variables=1, b.variables[0]=2
    private static int depth(Variable v) {
        if (v instanceof FieldReference fr && fr.scopeVariable() != null) return 1 + depth(fr.scopeVariable());
        if (v instanceof org.e2immu.language.cst.api.variable.DependentVariable dv && dv.arrayVariable() != null) {
            return 1 + depth(dv.arrayVariable());
        }
        return 0;
    }

    private void suppressRedundantScopeUps(Links.Builder builder) {
        List<Link> links = new ArrayList<>(builder.linkSet());
        Set<Link> toRemove = new HashSet<>();
        for (Link coarse : links) {
            for (Link fine : links) {
                if (fine.equals(coarse)) continue;
                boolean fromUp = Util.scopeVariables(fine.from()).contains(coarse.from())
                                 && fine.to().equals(coarse.to())
                                 && fine.linkNature().redundantFromUp().contains(coarse.linkNature());
                boolean toUp = fine.from().equals(coarse.from())
                               && Util.scopeVariables(fine.to()).contains(coarse.to())
                               && fine.linkNature().redundantToUp().contains(coarse.linkNature());
                boolean bothUp = Util.scopeVariables(fine.from()).contains(coarse.from())
                                 && Util.scopeVariables(fine.to()).contains(coarse.to())
                                 && fine.linkNature().redundantUp().contains(coarse.linkNature());
                if (fromUp || toUp || bothUp) {
                    toRemove.add(coarse);
                    break;
                }
            }
        }
        builder.removeIf(toRemove::contains);
    }

    private void handleReturnVariable(ReturnVariable rv, Links.Builder builder) {
        // replace all intermediates by a marker; don't worry about duplicate makers for now
        // don't bother with modifications; not relevant.
        boolean needMarker = false;
        List<Link> newLinks = new ArrayList<>();
        for (Link link : builder) {
            if (link.linkNature().isIdenticalToOrAssignedFromTo()
                && link.to() instanceof IntermediateVariable iv && iv.isNewObject()) {
                needMarker = true;
            } else if (LinkVariable.acceptForLinkedVariables(link.from())
                       && LinkVariable.acceptForLinkedVariables(link.to())) {
                newLinks.add(link);
            }
        }
        if (needMarker) {
            Variable marker = MarkerVariable.someValue(runtime, rv.methodInfo().returnType());
            newLinks.addFirst(new LinksImpl.LinkImpl(rv, IS_ASSIGNED_FROM, marker));
        }
        builder.replaceAll(newLinks);
    }

    // mirrors the internal-reference filter in FollowGraph.followGraph: a link between two variables sharing the
    // same primary, where one is (part of) the primary itself, is an internal self-reference and not emitted.
    private static boolean isInternalSelfFieldLink(Variable from, Variable to) {
        Variable primaryFrom = Util.primary(from);
        Variable primaryTo = Util.primary(to);
        if (primaryFrom == null || !primaryFrom.equals(primaryTo)) return false;
        Variable firstRealFrom = Util.firstRealVariable(from);
        Variable firstRealTo = Util.firstRealVariable(to);
        boolean crossField = !firstRealFrom.equals(primaryFrom)
                             && !firstRealTo.equals(primaryTo)
                             && !firstRealFrom.equals(firstRealTo);
        return !crossField;
    }

    // A field-containment link asserts the field is part of the container: 'A ≻ B' (CONTAINS_AS_FIELD) requires B
    // to be part of A; 'A ≺ B' (IS_FIELD_OF) requires A to be part of B. After rep-expansion a 'field ← param'
    // collapse can produce a link to the param side ('wrap ≻ 0:y'), which violates this — drop it.
    private static boolean isInvalidFieldContainment(Variable from, LinkNature linkNature, Variable to) {
        // Only a REAL field-side can make the containment invalid ('wrap ≻ 0:y' — 0:y is a value source, not a
        // part of wrap). A VIRTUAL field-side is content: with the owner≻own-content spine the closure derives
        // legitimate cross-variable content containment ('entry.§xy.§x ≺ 0:optional' — entry's content lives in
        // optional's object graph), which is expected output.
        if (linkNature == CONTAINS_AS_FIELD) return !Util.virtual(to) && !Util.isPartOf(from, to);
        if (linkNature == IS_FIELD_OF) return !Util.virtual(from) && !Util.isPartOf(to, from);
        return false;
    }

    // iterateOverShared, but when 'recipientSide' the rep expands only to recipient members (drops pure sources),
    // so an incoming assignment edge is not attributed to a value that merely flows into the collapsed variable.
    private Stream<Variable> expandShared(Variable variable, boolean recipientSide) {
        if (!recipientSide) return iterateOverShared(variable);
        return iterateOverShared(variable).filter(m -> !followGraph.graph().isPureAssignmentSource(m));
    }

    private Stream<Variable> iterateOverShared(Variable variable) {
        if (variable instanceof SharedVariable sv) {
            return sv.variables().stream();
        }
        if (variable instanceof FieldReference fr && fr.scopeVariable() != null) {
            return iterateOverShared(fr.scopeVariable())
                    .map(scope -> scope == fr.scopeVariable()
                            ? fr
                            : runtime.newFieldReference(fr.fieldInfo(),
                            runtime.newVariableExpression(scope), fr.parameterizedType()));
        }
        return Stream.of(variable);
    }

    private static boolean assignedInThisStatement(Statement statement, VariableInfo vi) {
        String index = statement.source().index();
        return vi.assignments().contains(index) && !vi.reads().indices().contains(index);
    }

    private boolean notLinkedToModifiedVirtualModification(Variable variable,
                                                           Variable toFollow,
                                                           Map<Variable, Set<MethodInfo>> modifiedVariablesAndTheirCause) {
        // Respect the ☷ pass semantics per group (mirrors the ≡-branch of notLinkedToModified): a pass-marked
        // §m-equivalence ('identical except via remove()', Iterable.iterator()) propagates a member's modification
        // only when the modifying method is in the pass set. E.g. iterating a collection modifies the ITERATOR
        // (next()), which must not mark the collection modified; remove() would.
        if (System.getenv("NOPASSFIX") != null) {
            return followGraph.graph().eqVariables(variable).noneMatch(modifiedVariablesAndTheirCause::containsKey);
        }
        return followGraph.graph().eqGroups(variable).noneMatch(group ->
                group.members().stream().map(Util::firstRealVariable).anyMatch(m -> {
                    Set<MethodInfo> causes = modifiedVariablesAndTheirCause.get(m);
                    if (causes == null) return false;
                    Set<MethodInfo> pass = group.linkNature().pass();
                    return pass.isEmpty() || !Collections.disjoint(pass, causes);
                }));
    }

    private boolean notLinkedToModified(Links.Builder builder,
                                        Map<Variable, Set<MethodInfo>> modifiedVariablesAndTheirCause) {
        for (Link link : builder) {
            Variable toReal = Util.firstRealVariable(link.to());
            Set<MethodInfo> causesOfModification = modifiedVariablesAndTheirCause.get(toReal);
            if (causesOfModification != null) {
                LinkNature ln = link.linkNature();
                if (ln.isIdenticalTo()
                    && link.to() instanceof FieldReference fr
                    && Util.isVirtualModificationField(fr.fieldInfo())
                    && (ln.pass().isEmpty() || !Collections.disjoint(ln.pass(), causesOfModification))) {
                    // x.§m ≡ y.§m
                    // pass = see Iterable, whose iterator() method is @Independent(hc = true, except = "remove")

                    // because we're processing the variables in order, adding to the map here provides the completion
                    modifiedVariablesAndTheirCause.put(builder.primary(), causesOfModification);
                    return false;
                }
                if (ln == CONTAINS_AS_FIELD
                    || ln == SHARES_FIELDS // see impl/TestInstanceOf,2
                    || ln == CONTAINS_AS_MEMBER) {
                    return false;
                }
                // the following rule is only valid for variables of non-abstract types (those that have no §m)
                // in particular, it is NOT valid for arrays and unbound type parameters
                if (ln == IS_ASSIGNED_TO) {
                    ParameterizedType pt = toReal.parameterizedType();
                    if (!Util.needsVirtual(pt)) {
                        Value.Immutable immutable = new AnalysisHelper().typeImmutable(pt);
                        if (!immutable.isAtLeastImmutableHC()) return false;
                    }
                }
            }
        }
        return true;
    }
}
