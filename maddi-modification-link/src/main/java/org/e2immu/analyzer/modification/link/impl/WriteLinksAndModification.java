package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.common.util.TolerantWrite;
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
import org.e2immu.language.cst.api.variable.DependentVariable;
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
        Map<Link, Variable> flipOwner = new HashMap<>();
        for (VariableInfo vi : vd.variableInfoIterable(Stage.EVALUATION)) {
            List<Link> tr = doVariableReturnRecompute(statement, lastStatement, vi, unmarkedModifications,
                    previouslyModified, expandedModifiedDuringEvaluation, newLinkedVariables, redundantLinks);
            for (Link l : tr) flipOwner.putIfAbsent(l, vi.variable());
            toRemove.addAll(tr);
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
            // gate NOFLIPSAME: raw edges inserted at THIS statement are the post-state of the modifying call
            // itself and survive the ⊇→~ rewrite (see replaceReturnAffected); NOFLIPSAME=1 restores the
            // unconditional rewrite
            String skipStatementIndex = System.getenv("NOFLIPSAME") == null ? statement.source().index() : null;
            Set<Variable> affected = new HashSet<>();
            for (Link link : toRemove) {
                // gate NOFLIPOWN: a composite ⊆/⊇ entailed by intact raw edges is not invalidated by the
                // modification — only raw edges the MODIFIED variable itself owns are stale. Without this, the
                // iterator's modification (next()) flipped the raw 'entries.§kvs ⊆ this.map.§kvs' (both
                // unmodified) because the iterator's derived ⊆ descended to it (TestMap test2Reverse0).
                Variable owner = flipOwner.get(link);
                java.util.function.Predicate<Fact<Variable, LinkNature>> acceptRaw =
                        owner == null || System.getenv("NOFLIPOWN") != null
                                ? _ -> true
                                : fact -> ownsFact(owner, fact);
                Set<Variable> set = followGraph.graph()
                        .replaceReturnAffected(link.from(), link.to(), link.linkNature(), SHARES_ELEMENTS,
                                skipStatementIndex, acceptRaw);
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

    // the raw fact touches the flip owner: one endpoint's primary is the owner itself, or a shared-variable
    // rep whose members include a face of the owner
    private boolean ownsFact(Variable owner, Fact<Variable, LinkNature> fact) {
        return touches(owner, fact.source()) || touches(owner, fact.target());
    }

    private boolean touches(Variable owner, Variable v) {
        Variable p = Util.primary(v);
        if (owner.equals(p)) return true;
        if (p != null) {
            return followGraph.graph().expandRepToMembers(p)
                    .anyMatch(m -> owner.equals(Util.primary(m)));
        }
        return false;
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
                    // whole-return endpoints get no §m companion (FollowGraph's convention), but a return's
                    // FIELD FACE does: 'withException.exit.exception ← 0:e' carries
                    // 'withException.exit.exception.§m ≡ 0:e.§m' into the summary — the §m is what modification
                    // propagation consumes at the call site
                    if (!(f instanceof ReturnVariable) && !(t instanceof ReturnVariable)
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

        if (System.getenv("RVTRACE") != null && variable instanceof ReturnVariable) {
            System.out.println("RVTRACE b1=" + builder1.linkSet() + " b2=" + builder2.linkSet()
                               + " b=" + builder.linkSet());
        }
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
            returnSideModificationCompanions(rv, builder);
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
            // 'modified by THIS statement's evaluation' — directly, or through a link to something modified now.
            // For previouslyModified variables only the direct check runs: the notLinked* probes (and
            // notLinkedToModified's completion side effect) stay short-circuited exactly as before.
            boolean modifiedInEval =
                    !modifiedInThisEvaluation.isEmpty()
                    && !assignedInThisStatement(statement, vi)
                    && (modifiedInThisEvaluation.containsKey(variable)
                        || !previouslyModified.contains(variable)
                           // all the §m links
                           && (!notLinkedToModifiedVirtualModification(variable, toFollow, modifiedInThisEvaluation)
                               // and other links such as ≺ IS_FIELD_OF
                               || !notLinkedToModified(builder, modifiedInThisEvaluation)));
            boolean unmodified =
                    variable.isIgnoreModifications()
                    ||
                    !previouslyModified.contains(variable) && !modifiedInEval;
            builder.removeIf(WriteLinksAndModification::notInLinkedVariables);

            if (variable instanceof This) {
                // only keep direct links for "this", the others are replicated in its fields
                builder.removeIf(l -> !(l.from() instanceof This));
            }
            Value.Bool newValue = ValueImpl.BoolImpl.from(unmodified);
            TolerantWrite.setAllowControlledOverwrite(vi.analysis(), UNMODIFIED_VARIABLE, newValue, variable);

            // The ⊇→~ rewrite runs at the statement where the modification OCCURS. With the persistent graph the
            // rewrite survives into later statements by itself; re-flipping on previouslyModified (the old
            // engine's behavior, needed there because its per-statement graph rebuild resurrected ⊆/⊇) would
            // permanently destroy containment knowledge that this evaluation did not invalidate.
            // Gate NOFLIPSAME=1 restores the old re-flip.
            if (!unmodified && (modifiedInEval || System.getenv("NOFLIPSAME") != null)) {
                // ⊆, ⊇ become ~ after a modification
                builder.linkSet().forEach(link -> {
                    if (link.linkNature() == IS_SUBSET_OF || link.linkNature() == IS_SUPERSET_OF) {
                        if (System.getenv("FLIPTRACE") != null) {
                            System.out.println("FLIPTRACE owner=" + variable + " link=" + link
                                               + " builder=" + builder.linkSet());
                        }
                        toRemove.add(link);
                        toRemove.add(new LinksImpl.LinkImpl(link.to(), link.linkNature().reverse(), link.from()));
                    }
                });
            }
        }
        // §m-directional inheritance, consumption-aware (gate NOVMIDIR): added AFTER the modification
        // decision and the ⊇→~ rewrite collection, so these facts are OUTPUT-ONLY — emitted earlier they
        // leak into verdicts (see Graph.vmiDirectionalFacts and the catalogue's VMIFP lesson).
        // Default ON. The 'context divergence' that had kept this opt-in was a measurement artifact: the
        // ⊇→~ flip at TestStaticValuesRecord test4b:357 was the previouslyModified re-flip destroying
        // same-statement containment in the persistent graph (fixed above, gate NOFLIPSAME), present in
        // every run context — not a class-vs-full-suite difference.
        if (System.getenv("NOVMIDIR") == null) {
            for (Link l : followGraph.graph().vmiDirectionalFacts(variable)) {
                if (!builder.contains(l.from(), l.linkNature(), l.to())
                    && !builder.contains(l.to(), l.linkNature().reverse(), l.from())) {
                    builder.add(l.from(), l.linkNature(), l.to());
                }
            }
        }
        // Return-face §m rehoming (gate NORVEQ), output-only like the block above: an §m-≡ to a variable that
        // whole-object-shares with a RETURN ('l1.§m ≡ l3.§m', group {method, l3}) also holds against the
        // return's face ('l1.§m ≡ method.§m' — the view chain's §m reaches the summary endpoint). Inherently
        // statement-scoped: the return group only exists from the return statement on, and views are written
        // forward — the leak-to-earlier-views that killed the naive graph-side rehoming cannot occur here.
        if (System.getenv("NORVEQ") == null && !(variable instanceof ReturnVariable) && virtualFieldComputer != null) {
            List<Link> toAddRv = new ArrayList<>();
            for (Link l : builder.linkSet()) {
                if (l.linkNature().isIdenticalTo()
                    && l.from() instanceof FieldReference frF && Util.isVirtualModificationField(frF.fieldInfo())
                    && l.to() instanceof FieldReference frT && Util.isVirtualModificationField(frT.fieldInfo())) {
                    Variable realTo = Util.firstRealVariable(l.to());
                    for (Variable sib : followGraph.graph().allShared(realTo)) {
                        if (sib instanceof ReturnVariable rv) {
                            VirtualFieldComputer.M2 m2 = virtualFieldComputer
                                    .addModificationFieldEquivalence(Util.firstRealVariable(l.from()), rv);
                            if (m2 != null) toAddRv.add(new LinksImpl.LinkImpl(m2.m1(),
                                    LinkNatureImpl.makeIdenticalTo(null), m2.m2()));
                        }
                    }
                }
            }
            for (Link l : toAddRv) {
                boolean present = builder.linkSet().stream().anyMatch(x ->
                        x.from().equals(l.from()) && x.to().equals(l.to())
                        || x.from().equals(l.to()) && x.to().equals(l.from()));
                if (!present) builder.add(l.from(), l.linkNature(), l.to());
            }
        }
        if (System.getenv("BTRACE") != null && variable.toString().contains(System.getenv("BTRACE"))) {
            System.out.println("BTRACE stmt " + statement.source().index() + " var=" + variable
                               + " builder=" + builder.linkSet());
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
    // key by the Variable OBJECTS (fqn-based equality), not their printed form: the previous string key
    // concatenated the variables, and Variable.toString runs the full CST printer — 22.6% of the fernflower
    // run's CPU was inside dedupReversePairs building these keys (async-profiler round 1). The nature stays
    // keyed on its symbol so pass-variants compare exactly as the old string key did.
    private record LinkKey(Variable from, String nature, Variable to) {
    }

    private void dedupReversePairs(Links.Builder builder) {
        List<Link> links = new ArrayList<>(builder.linkSet());
        // removals tracked BY INDEX: Link is a record with value equality, so equal duplicates would otherwise
        // all be swept by one removal (that bug dropped both ∈ copies while keeping the ∋)
        boolean[] removed = new boolean[links.size()];
        Set<LinkKey> seen = new HashSet<>();
        LinkKey[] keys = new LinkKey[links.size()];
        for (int i = 0; i < links.size(); i++) {
            Link link = links.get(i);
            LinkKey key = new LinkKey(link.from(), link.linkNature().toString(), link.to());
            keys[i] = key;
            if (!seen.add(key)) removed[i] = true; // exact duplicate: keep the first
        }
        // reverse-pair detection via a key index: the reverse-pair predicate is exactly "key(j) == revKey(i)"
        // (pass 1 above already relies on the same keyed identity), so only true candidates are
        // visited, in the same ascending index order as the former all-pairs scan — which was O(n^2) in
        // Variable.equals and the analysis-wide hot spot after the RedundantLinks fix (fernflower reorderIf).
        Map<LinkKey, List<Integer>> byKey = new HashMap<>();
        for (int i = 0; i < links.size(); i++) {
            if (!removed[i]) byKey.computeIfAbsent(keys[i], _ -> new ArrayList<>()).add(i);
        }
        for (int i = 0; i < links.size(); i++) {
            if (removed[i]) continue;
            Link a = links.get(i);
            LinkKey revKey = new LinkKey(a.to(), a.linkNature().reverse().toString(), a.from());
            List<Integer> candidates = byKey.get(revKey);
            if (candidates == null) continue;
            for (int j : candidates) {
                if (j <= i || removed[j]) continue;
                Link b = links.get(j);
                if (depth(a.from()) >= depth(b.from())) removed[j] = true;
                else removed[i] = true;
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
        /*
        Semantically identical to the former all-pairs scan, which cost n^2 pairs x up to 4 scope-chain
        walks each — a single guava method sat here for minutes (first-contact stall at 'Done 7863').
        A variable's scope chain never contains the variable itself, so the former fine.equals(coarse)
        guard was vacuous and the removal set is order-independent: build, once per 'fine' link, the
        scope-up (from,to) pair -> suppressed-natures maps, then drop every 'coarse' link whose
        endpoints + nature hit one of them.
         */
        record VPair(Variable from, Variable to) {
        }
        Map<VPair, Set<org.e2immu.analyzer.modification.prepwork.variable.LinkNature>> fromUp = new HashMap<>();
        Map<VPair, Set<org.e2immu.analyzer.modification.prepwork.variable.LinkNature>> toUp = new HashMap<>();
        Map<VPair, Set<org.e2immu.analyzer.modification.prepwork.variable.LinkNature>> bothUp = new HashMap<>();
        for (Link fine : links) {
            Set<Variable> svFrom = Util.scopeVariables(fine.from());
            Set<Variable> svTo = Util.scopeVariables(fine.to());
            var rFrom = fine.linkNature().redundantFromUp();
            var rTo = fine.linkNature().redundantToUp();
            var rBoth = fine.linkNature().redundantUp();
            if (!rFrom.isEmpty()) {
                for (Variable sf : svFrom) {
                    fromUp.computeIfAbsent(new VPair(sf, fine.to()), _ -> new HashSet<>()).addAll(rFrom);
                }
            }
            if (!rTo.isEmpty()) {
                for (Variable st : svTo) {
                    toUp.computeIfAbsent(new VPair(fine.from(), st), _ -> new HashSet<>()).addAll(rTo);
                }
            }
            if (!rBoth.isEmpty()) {
                for (Variable sf : svFrom) {
                    for (Variable st : svTo) {
                        bothUp.computeIfAbsent(new VPair(sf, st), _ -> new HashSet<>()).addAll(rBoth);
                    }
                }
            }
        }
        Set<Link> toRemove = new HashSet<>();
        for (Link coarse : links) {
            VPair key = new VPair(coarse.from(), coarse.to());
            var f = fromUp.get(key);
            var t = toUp.get(key);
            var b = bothUp.get(key);
            if (f != null && f.contains(coarse.linkNature())
                || t != null && t.contains(coarse.linkNature())
                || b != null && b.contains(coarse.linkNature())) {
                toRemove.add(coarse);
            }
        }
        builder.removeIf(toRemove::contains);
    }

    private void handleReturnVariable(ReturnVariable rv, Links.Builder builder) {
        // replace all intermediates by a marker; don't worry about duplicate makers for now
        // don't bother with modifications; not relevant.
        // side-band fresh-object fact: 'return new URL(...)' whose reduced intermediate never entered the
        // graph (LinkGraph.reduceLinks) — the marker must still appear
        boolean needMarker = followGraph.graph().isFreshObjectReturn(rv);
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

    /*
    Whole-return §m companions from content-flow links (gate NORVM; the second half of the §m-directional
    family, see catalogue): the old engine's summaries carried directional §m links on the return that the
    reconstructed fold does not produce, because whole-return endpoints are excluded from the assignment-§m
    companions (FollowGraph's convention). Two shapes, both consumption-aware (the return builder never feeds
    this method's own modification verdicts — returns skip the unmodified branch entirely):
    1. 'method.§X ⊆ S.§Y' (the return's content comes from S's content) ⟹ 'method.§m ← S.§m': modifying S's
       content reaches the returned value. TestStaticValuesRecord test4c: 'method.§$s ⊆ 1:rr.§$s' gives
       'method.§m ← 1:rr.§m'.
    2. double-∩ 'method ∩ Y.§X' AND 'method.§X' ∩ Y': the returned value and Y may be the same object cluster
       (each sits in the other's content web) ⟹ 'method.§m ≡ Y.§m'. TestStaticValuesRecord test4b:
       'method ∩ 0:in.§$s' + 'method.§$s ∩ 0:in' give 'method.§m ≡ 0:in.§m'.
     */
    private void returnSideModificationCompanions(ReturnVariable rv, Links.Builder builder) {
        if (virtualFieldComputer == null || System.getenv("NORVM") != null) return;
        List<Link> links = new ArrayList<>(builder.linkSet());
        List<Link> toAdd = new ArrayList<>();
        for (Link link : links) {
            Variable from = link.from(), to = link.to();
            if (link.linkNature() == IS_SUBSET_OF
                && Util.virtual(from) && rv.equals(Util.firstRealVariable(from))
                && Util.virtual(to) && to instanceof FieldReference frTo
                && !Util.isVirtualModificationField(frTo.fieldInfo())
                && !(from instanceof FieldReference frFrom && Util.isVirtualModificationField(frFrom.fieldInfo()))) {
                Variable realTo = Util.firstRealVariable(to);
                if (!rv.equals(realTo) && LinkVariable.acceptForLinkedVariables(realTo)
                    && !(realTo instanceof MarkerVariable)) {
                    VirtualFieldComputer.M2 m2 = virtualFieldComputer.addModificationFieldEquivalence(rv, realTo);
                    if (m2 != null && validCompanionFace(m2.m1()) && validCompanionFace(m2.m2())) {
                        toAdd.add(new LinksImpl.LinkImpl(m2.m1(), IS_ASSIGNED_FROM, m2.m2()));
                    }
                }
            }
            if (link.linkNature() == OBJECT_GRAPH_OVERLAPS
                && rv.equals(from) && Util.virtual(to)) {
                Variable realTo = Util.firstRealVariable(to);
                if (!rv.equals(realTo) && LinkVariable.acceptForLinkedVariables(realTo)
                    && !(realTo instanceof MarkerVariable)
                    // the mirror direction: some content face of the return overlaps Y itself
                    && links.stream().anyMatch(l2 -> l2.linkNature() == OBJECT_GRAPH_OVERLAPS
                                                     && realTo.equals(l2.to())
                                                     && Util.virtual(l2.from())
                                                     && rv.equals(Util.firstRealVariable(l2.from())))) {
                    VirtualFieldComputer.M2 m2 = virtualFieldComputer.addModificationFieldEquivalence(rv, realTo);
                    LinkNature id = LinkNatureImpl.makeIdenticalTo(null);
                    if (m2 != null && validCompanionFace(m2.m1()) && validCompanionFace(m2.m2())) {
                        toAdd.add(new LinksImpl.LinkImpl(m2.m1(), id, m2.m2()));
                    }
                }
            }
        }
        for (Link l : toAdd) {
            // any existing §m link between the same pair subsumes the companion (an assignment's ≡ makes ← redundant)
            boolean present = builder.linkSet().stream().anyMatch(x ->
                    x.from().equals(l.from()) && x.to().equals(l.to())
                    || x.from().equals(l.to()) && x.to().equals(l.from()));
            if (!present) {
                builder.add(l.from(), l.linkNature(), l.to());
            }
        }
    }

    // mirrors LinkImpl's doNotStackMOnTopOfVirtualField invariant: on real-world deep-generic shapes (timefold
    // bavet lambdas), addModificationFieldEquivalence can produce a §m face whose scope is itself a virtual
    // field; such a companion is not representable as a Link -- skip it instead of tripping the constructor assert
    private static boolean validCompanionFace(Variable v) {
        return !(v instanceof FieldReference fr && Util.isVirtualModificationField(fr.fieldInfo())
                 && fr.scopeVariable() instanceof FieldReference fr2 && Util.virtual(fr2.fieldInfo()));
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
    // NOTE (≺-family cluster): a group-aware relaxation of this filter (allow 'A ≺ B' when a group sibling of A
    // is part of B) was tried and reverted: it admits the unwanted 'o ≺ 0:r' for the accessor-copy shape
    // (TestCast, group {o, 0:r.object}) while NOT producing the wanted 'set ≺ 0:i' for the record-pattern shape
    // (TestInstanceOf, group {set, o} — the cast local, not the field face). The old engine emitted containment
    // for pattern bindings but not accessor copies; that distinction must come from the binding site, not from
    // this filter.
    private boolean isInvalidFieldContainment(Variable from, LinkNature linkNature, Variable to) {
        // Only a REAL field-side can make the containment invalid ('wrap ≻ 0:y' — 0:y is a value source, not a
        // part of wrap). A VIRTUAL field-side is content: with the owner≻own-content spine the closure derives
        // legitimate cross-variable content containment ('entry.§xy.§x ≺ 0:optional' — entry's content lives in
        // optional's object graph), which is expected output. A RETURN VARIABLE from-side is likewise value-level
        // by nature (a return is never structurally a field of anything): 'm ≺ 0:r' from a record-pattern
        // binding ('if (r instanceof R(X xx)) return xx;') says the returned value is a component of r.
        // A marked PATTERN BINDING (side-band, Graph.markPatternBinding — set at the binding site, the only
        // place that can distinguish deconstruction components from accessor-copy expansions) is genuine
        // containment, including through the binding's cast aliases ('0:i ≻ set' for bound 'o', group {o, set}).
        if (linkNature == CONTAINS_AS_FIELD) return !Util.virtual(to) && !Util.isPartOf(from, to)
                                                    && !followGraph.graph().isPatternBindingOrAlias(from, to);
        if (linkNature == IS_FIELD_OF) return !Util.virtual(from)
                                              && !(Util.primary(from) instanceof ReturnVariable)
                                              && !Util.isPartOf(to, from)
                                              && !followGraph.graph().isPatternBindingOrAlias(to, from);
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
        if (variable instanceof DependentVariable dv && dv.arrayVariable() != null) {
            // a rep in the array base ('$__sv_map.§vks[-1]' — the [-1]-slice face of a collapsed group) leaked
            // as-is into printed links; rebuild the access on the expanded base, keeping the original element
            // type (mirrors Graph.expandRepToMembers' DV branch)
            return iterateOverShared(dv.arrayVariable())
                    .map(arr -> arr == dv.arrayVariable()
                            ? dv
                            : runtime.newDependentVariable(runtime.newVariableExpression(arr),
                            dv.indexExpression(), dv.parameterizedType()));
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
