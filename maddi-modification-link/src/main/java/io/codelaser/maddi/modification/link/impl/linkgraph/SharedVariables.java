package io.codelaser.maddi.modification.link.impl.linkgraph;

import io.codelaser.maddi.modification.link.impl.Gate;
import io.codelaser.maddi.modification.link.impl.LinkNatureImpl;
import io.codelaser.maddi.modification.link.impl.localvar.IntermediateVariable;
import io.codelaser.maddi.modification.link.impl.localvar.SharedVariable;
import io.codelaser.maddi.modification.link.impl.translate.VariableTranslationMap;
import io.codelaser.maddi.modification.prepwork.Util;
import io.codelaser.maddi.modification.prepwork.variable.Link;
import io.codelaser.maddi.modification.prepwork.variable.impl.LinksImpl;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// variant on EquivalenceGroup, to be used for equivalent SharedVariable objects
public class SharedVariables {
    private final Runtime runtime;
    // LinkedHashMaps: derivedFaceKeyed/faceKeyed take the FIRST match while iterating — insertion order
    // (statement-deterministic) must decide, not hash order (TestModificationBasics m∩copy flickered)
    private Map<String, SharedVariable> sharedVariablesByName = new LinkedHashMap<>();
    private Map<Variable, SharedVariable> memberToGroup = new LinkedHashMap<>();
    private VariableTranslationMap variableTranslationMap;

    public SharedVariables(Runtime runtime) {
        this.runtime = runtime;
        variableTranslationMap = new VariableTranslationMap(runtime);
    }

    /*
     The groups as they stand at one point of the method, detached from this object: a branch of an if/else (or
     a switch case, a loop body) is linked from a copy of the state before the statement, so that one branch's
     reassignment cannot evict what a sibling branch recorded. The reps are shared by every copy (a rep is a
     name: a graph vertex, equal by name); each copy owns its own Data, which restore() points the reps at.
     */
    public record State(Map<String, SharedVariable> sharedVariablesByName,
                        Map<Variable, SharedVariable> memberToGroup,
                        VariableTranslationMap variableTranslationMap,
                        Map<Variable, java.util.Set<Variable>> membersByPrefix,
                        Map<SharedVariable, SharedVariable.Data> data,
                        Map<Variable, java.util.Set<SharedVariable>> detachedIn,
                        Map<Variable, java.util.Set<SharedVariable>> evictedFrom) {
        public State copy() {
            Map<Variable, java.util.Set<SharedVariable>> detached = new HashMap<>();
            detachedIn.forEach((k, v) -> detached.put(k, new java.util.LinkedHashSet<>(v)));
            Map<Variable, java.util.Set<SharedVariable>> evicted = new HashMap<>();
            evictedFrom.forEach((k, v) -> evicted.put(k, new java.util.LinkedHashSet<>(v)));
            Map<Variable, java.util.Set<Variable>> prefixes = new HashMap<>();
            membersByPrefix.forEach((k, v) -> prefixes.put(k, new java.util.LinkedHashSet<>(v)));
            Map<SharedVariable, SharedVariable.Data> dataCopy = new LinkedHashMap<>();
            data.forEach((sv, d) -> dataCopy.put(sv, d.copy()));
            return new State(new LinkedHashMap<>(sharedVariablesByName), new LinkedHashMap<>(memberToGroup),
                    variableTranslationMap.copy(), prefixes, dataCopy, detached, evicted);
        }
    }

    /** An independent copy of the current groups. */
    public State snapshot() {
        Map<SharedVariable, SharedVariable.Data> data = new LinkedHashMap<>();
        sharedVariablesByName.values().forEach(sv -> data.put(sv, sv.data()));
        return new State(sharedVariablesByName, memberToGroup, variableTranslationMap, membersByPrefix, data,
                detachedIn, evictedFrom).copy();
    }

    /** The current state itself, not a copy: this object must not be used again before the next restore. */
    public State detach() {
        Map<SharedVariable, SharedVariable.Data> data = new LinkedHashMap<>();
        sharedVariablesByName.values().forEach(sv -> data.put(sv, sv.data()));
        return new State(sharedVariablesByName, memberToGroup, variableTranslationMap, membersByPrefix, data,
                detachedIn, evictedFrom);
    }

    /** Install a state; it is owned by this object from now on (pass a copy to keep using it). */
    public void restore(State state) {
        assert lastMergedAway == null;
        sharedVariablesByName = state.sharedVariablesByName();
        memberToGroup = state.memberToGroup();
        variableTranslationMap = state.variableTranslationMap();
        membersByPrefix = state.membersByPrefix();
        detachedIn = state.detachedIn();
        evictedFrom = state.evictedFrom();
        state.data().forEach(SharedVariable::setData);
    }

    /*
     DETACHED records: a member evicted by a join (planJoin) is no member of any group any more -- it holds no
     identity with the group -- but the assignments it received in an alternative stay in that group's records,
     so that its own extraction still reconstructs them ('0:p ← 0:p.impliedBy' after 'p = p.impliedBy' in a loop
     body; assignmentEdgeStream). Indexed per variable: its next reassignment (remove) drops them.
     */
    private Map<Variable, java.util.Set<SharedVariable>> detachedIn = new HashMap<>();

    /*
     EVICTED members, recipients and sources alike, with the groups they were members of in some alternative of a
     join: they may still hold that group's object, so a modification of one reaches the others
     (detachedAliases). Unlike detachedIn, nothing is extracted from it. Dropped at the variable's next assignment.
     */
    private Map<Variable, java.util.Set<SharedVariable>> evictedFrom = new HashMap<>();

    public Collection<Variable> allShared(Variable variable) {
        SharedVariable sv = memberToGroup.get(variable);
        if(sv == null) return List.of(variable);
        return sv.variables();
    }

    public SharedVariable isAssignedFrom(Variable from, Variable to, String statementIndex) {
        joinedBySpelling = false;
        SharedVariable sv1 = groupOfWithSiblingSpelling(from);
        boolean joined1 = joinedBySpelling;
        joinedBySpelling = false;
        SharedVariable sv2 = groupOfWithSiblingSpelling(to);
        boolean joined2 = joinedBySpelling;
        if (sv1 == null && sv2 == null) {
            SharedVariable sv = create(from, to);
            sv.addAssignment(from, to, statementIndex);
            return sv;
        }
        if (sv1 == sv2) {
            if (joined1 || joined2) {
                // one side entered the group as a sibling spelling of a member ('b.stringSet' joining
                // {$__rv6.stringSet, 0:in} because b ≡ $__rv6): the assignment hop must still be recorded —
                // reachable() walks these records, and without it the chain to the group's true sources
                // (0:in) breaks at the joined spelling. Returning the group also re-keys both sides' graph
                // vertices onto the rep (the sv==null path asserts there are none).
                sv1.addAssignment(from, to, statementIndex);
                return sv1;
            }
            return null; // already in the same group
        }
        if (sv1 == null) {
            add(sv2, from);
            sv2.addAssignment(from, to, statementIndex);
            return sv2;
        }
        if (sv2 == null) {
            add(sv1, to);
            sv1.addAssignment(from, to, statementIndex);
            return sv1;
        }
        // merge 2 groups: sv2 folds into sv1; the caller must re-key sv2's graph vertices onto sv1
        // (Graph.mergeEdgeBi does, via lastMergedAway)
        merge(sv1, sv2, from, to, statementIndex);
        return sv1;
    }

    /*
    One runtime slot must be ONE group (gate NOSIBFACE). In a collapsed builder chain
    ({$__c0, $__rv2, $__rv6, b} whole-object-grouped), the setter calls group the slot under one spelling
    ($__sv_j: {$__c0.j, $__rv2.j}, carrying '← $_ce1') while a later build()/read spells the same slot through
    another sibling ('b.j'). A plain memberToGroup lookup misses, and a PARALLEL, disconnected group forms
    ({b.j, r.i}) — the slot's knowledge never reaches the reader (the builder-chain ce-constants loss;
    same disease as the ThrowingFunction rep-vs-face disconnection). Resolution: when a field face has no
    direct group, try the same field spelled through each whole-object group sibling of its scope; on a hit,
    the face JOINS that group (one slot, one group) and the hit is returned.
     */
    // set by groupOfWithSiblingSpelling when the lookup joined the variable into a group via a sibling
    // spelling (consumed per-lookup by isAssignedFrom, cf. the lastMergedAway pattern)
    private boolean joinedBySpelling;

    private SharedVariable groupOfWithSiblingSpelling(Variable v) {
        SharedVariable direct = memberToGroup.get(v);
        if (direct != null || Gate.isSet("NOSIBFACE")) return direct;
        if (v instanceof FieldReference fr && fr.scopeVariable() != null) {
            SharedVariable scopeGroup = memberToGroup.get(fr.scopeVariable());
            if (scopeGroup != null) {
                for (Variable sib : scopeGroup.variables()) {
                    if (sib.equals(fr.scopeVariable())) continue;
                    VariableTranslationMap vtm = new VariableTranslationMap(runtime);
                    vtm.put(fr.scopeVariable(), sib);
                    Variable spelled = vtm.translateVariableRecursively(v);
                    SharedVariable g = memberToGroup.get(spelled);
                    if (g != null) {
                        add(g, v);
                        joinedBySpelling = true;
                        return g;
                    }
                }
            }
        }
        return null;
    }

    // the group representative that was discarded by the most recent isAssignedFrom-triggered merge; the caller
    // (Graph.mergeEdgeBi) consumes it to re-key that rep's graph vertices onto the surviving group's rep
    private SharedVariable lastMergedAway;

    public SharedVariable consumeLastMergedAway() {
        SharedVariable sv = lastMergedAway;
        lastMergedAway = null;
        return sv;
    }

    // true when 'from' is being reassigned (it already recipient-participates in an assignment at a different
    // statement), as opposed to a multi-valued assignment where 'from' is assigned several values in one statement.
    public boolean isReassignment(Variable from, String statementIndex) {
        SharedVariable sv = memberToGroup.get(from);
        return sv != null && sv.recipientAtOtherStatement(from, statementIndex);
    }

    // true when 'from' participated in its group only as a SOURCE ('method ← from' at an earlier statement) and
    // is now being assigned at a different statement: the group's past knowledge stays valid for the OLD value,
    // but the new value must not join the group as an alias (a re-assigned parameter must not make
    // 'method ← 0:amb' collapse into an identity pool with the parameter's original value; @Identity verdicts).
    public boolean isSourceAtOtherStatement(Variable from, String statementIndex) {
        SharedVariable sv = memberToGroup.get(from);
        return sv != null && sv.assignments().stream()
                .anyMatch(a -> a.to().equals(from) && !a.statementIndex().equals(statementIndex));
    }

    /*
     Reconstruct the intra-group assignment relations that touch 'primary'. The group stores each 'from ← to'
     once (the collapse's whole point); here we hand back, keyed on the member that is part of 'primary', the
     directed link (reversed when 'primary' owns the to-side, so a parameter reads as '→' and a field as '←').
     Mirrors Graph.virtualModificationEdgeStream for the ≡ groups.
     */
    public Stream<Link> assignmentEdgeStream(Variable primary,
                                             java.util.function.BiPredicate<Variable, Variable> mediatedHop) {
        Stream.Builder<Link> builder = Stream.builder();
        // The primary's faces: the primary plus its whole-object shared-group siblings. A collapsed 'return p'
        // groups the return with 'p', so a field assignment keyed on the sibling ('p.f ← 0:x') belongs to the
        // return, rehomed onto the return's face ('create2.f ← 0:x').
        java.util.Collection<Variable> primaryFaces = allShared(primary);
        for (SharedVariable sv : new java.util.LinkedHashSet<>(memberToGroup.values())) {
            // The group's recorded 'a ← b' assignments form a directed chain; an intermediate ($__rv) merged in
            // the middle (makeFromGet.t ← $__rv ← box.t) breaks a direct pair once it is filtered out downstream.
            // So follow the chain transitively: emit 'm ← t' for every t reachable from m along ←, and its reverse.
            java.util.Map<Variable, java.util.List<Variable>> fwd = new java.util.HashMap<>();
            java.util.Map<Variable, java.util.List<Variable>> bwd = new java.util.HashMap<>();
            // unmediated-only adjacency for the chain-taint rule (task #39): a chain target is emitted
            // MEDIATED only when NO unmediated path reaches it — an unmediated path is genuine declared-type
            // coupling, and coupling (the consumer's conservative side) must win over provenance
            java.util.Map<Variable, java.util.List<Variable>> fwdU = new java.util.HashMap<>();
            java.util.Map<Variable, java.util.List<Variable>> bwdU = new java.util.HashMap<>();
            for (SharedVariable.Assignment a : sv.assignments()) {
                fwd.computeIfAbsent(a.from(), k -> new java.util.ArrayList<>()).add(a.to());
                bwd.computeIfAbsent(a.to(), k -> new java.util.ArrayList<>()).add(a.from());
                if (!mediatedHop.test(a.from(), a.to())) {
                    fwdU.computeIfAbsent(a.from(), k -> new java.util.ArrayList<>()).add(a.to());
                    bwdU.computeIfAbsent(a.to(), k -> new java.util.ArrayList<>()).add(a.from());
                }
            }
            java.util.Collection<Variable> emitters = sv.variables();
            java.util.Set<Variable> detachedHere = detachedFroms(sv, primary);
            if (!detachedHere.isEmpty()) {
                emitters = new java.util.LinkedHashSet<>(sv.variables());
                emitters.addAll(detachedHere);
            }
            for (Variable m : emitters) {
                // emitM is 'm' keyed onto the primary: 'm' itself when part of the primary, or the sibling-rehomed
                // form ('p.f' -> 'create2.f') when 'm' is a proper field/element of a sibling face.
                Variable emitM = faceKeyed(m, primary, primaryFaces);
                boolean derivedFace = false;
                if (emitM == null) {
                    emitM = derivedFaceKeyed(m, primary);
                    derivedFace = emitM != null;
                }
                if (emitM == null) continue;
                // Chain through method-internal locals only when reconstructing the return value (the whole
                // return, or one of its faces — 'justJ.j'): there a chain of assignments
                // (return ← ttt ← tt ← 0:t; justJ.j ← b.j ← 0:jp) must collapse to the summary form
                // ('method ← 0:t', 'justJ.j ← 0:jp'). For field/parameter endpoints the field-precise link
                // already arrives via the always-chained pass-through intermediates ($__rv), and bridging locals
                // there would add a spurious primary-level shortcut (makeFromGet ≈ 0:box). For a plain local 'm'
                // (per-statement view) we also stay shallow, preserving the collapse's dedup ('ttt ← tt', not
                // the transitive 'ttt ← 0:t').
                boolean deep = Util.primary(emitM)
                        instanceof io.codelaser.maddi.modification.prepwork.variable.ReturnVariable
                        // parameters are summary endpoints too: '0:in → v → this.f' must reach the
                        // field for the parameter's summary (var-transparency). Gate NOPDEEP.
                        || !Gate.isSet("NOPDEEP")
                           && Util.primary(emitM) instanceof io.codelaser.maddi.cst.api.info.ParameterInfo;
                java.util.Set<Variable> fwdReachU = reachable(m, fwdU, deep);
                for (Variable t : reachable(m, fwd, deep)) {
                    if (!emitM.equals(t)) {
                        boolean mediatedT = !fwdReachU.contains(t);
                        builder.add(new LinksImpl.LinkImpl(emitM, LinkNatureImpl.IS_ASSIGNED_FROM, t, mediatedT));
                        // alias a to-side ELEMENT face onto its base's assignment sources: 'm ← r[0]' with the
                        // for-each row 'r ← g[0]' equally holds as 'm ← 0:g[0][0]' — the local face (r) dies at
                        // summary time, the parameter face survives. Sources only: a recipient sibling of the
                        // base may be reassigned later and must not be aliased.
                        if (t instanceof io.codelaser.maddi.cst.api.variable.DependentVariable dvT
                            && dvT.arrayVariable() != null) {
                            for (Variable src : assignmentSources(dvT.arrayVariable())) {
                                VariableTranslationMap aliasTm = new VariableTranslationMap(runtime);
                                aliasTm.put(dvT.arrayVariable(), src);
                                Variable tAlt = aliasTm.translateVariableRecursively(t);
                                if (!tAlt.equals(t) && !emitM.equals(tAlt)) {
                                    builder.add(new LinksImpl.LinkImpl(emitM, LinkNatureImpl.IS_ASSIGNED_FROM, tAlt,
                                            mediatedT));
                                }
                            }
                        }
                        // containment companions, ONLY for a derived slot face (a real slot vertex gets
                        // 'slot ∈ container' from the graph and 'container ∋ value' from sub-propagation;
                        // a derived face — td.variables[0] ← someSet, rehomed across the collapsed builder
                        // chain — bypasses both): td.variables[0] ∈ td.variables, td.variables ∋ someSet.
                        if (derivedFace
                            && emitM instanceof io.codelaser.maddi.cst.api.variable.DependentVariable dv
                            && dv.arrayVariable() != null && !Util.virtual(t)) {
                            builder.add(new LinksImpl.LinkImpl(dv, LinkNatureImpl.IS_ELEMENT_OF, dv.arrayVariable()));
                            builder.add(new LinksImpl.LinkImpl(dv.arrayVariable(), LinkNatureImpl.CONTAINS_AS_MEMBER, t));
                        }
                    }
                }
                java.util.Set<Variable> bwdReachU = reachable(m, bwdU, deep);
                for (Variable t : reachable(m, bwd, deep)) {
                    if (!emitM.equals(t)) {
                        boolean mediatedT = !bwdReachU.contains(t);
                        builder.add(new LinksImpl.LinkImpl(emitM, LinkNatureImpl.IS_ASSIGNED_TO, t, mediatedT));
                        // RETURN-SPELLING SIBLINGS (gate NORVSP): when the recipient is a RETURN's FIELD FACE
                        // ('x → writeReturn.t' for a fluent mutator), the same slot's non-return spellings hold
                        // the same fact ('x → 0:box.t' — slot group {writeReturn.t, box.t, x}). The return
                        // spelling alone is useless on a parameter's summary face: filteredPi strips return-links
                        // from params mentioned in the return value; the old engine printed the box spelling.
                        // FIELD FACES only — for the WHOLE return, group siblings are multi-source could-be
                        // aliases (switch arms yielding list1/list2/list3: no flow between co-sources).
                        if (!Gate.isSet("NORVSP")
                            && t instanceof FieldReference
                            && Util.primary(t) instanceof io.codelaser.maddi.modification.prepwork.variable.ReturnVariable) {
                            for (Variable sib : allShared(t)) {
                                if (!sib.equals(t) && !sib.equals(m) && !emitM.equals(sib)
                                    && !(Util.primary(sib) instanceof io.codelaser.maddi.modification.prepwork.variable.ReturnVariable)
                                    && !(Util.firstRealVariable(sib) instanceof IntermediateVariable)
                                    && sib instanceof FieldReference) {
                                    builder.add(new LinksImpl.LinkImpl(emitM, LinkNatureImpl.IS_ASSIGNED_TO, sib,
                                            mediatedT));
                                }
                            }
                        }
                    }
                }
                if (deep) {
                    // SIBLING RECIPIENTS, one hop (gate NOSIBR): the summary endpoint and another face both
                    // received the same source's value ('method ← y' and 'this.ys[1] ← y' ⟹
                    // 'method → this.ys[1]': the returned value was also stored in the slot). Dying faces
                    // among the siblings are filtered downstream as usual.
                    if (!Gate.isSet("NOSIBR")) {
                        java.util.Set<Variable> sources = reachable(m, fwd, deep);
                        for (Variable src : sources) {
                            for (Variable sib : bwd.getOrDefault(src, java.util.List.of())) {
                                if (!sib.equals(m) && !emitM.equals(sib) && !sources.contains(sib)
                                    // two index-SPELLINGS of the same slot (0:b[off++] / 0:b[1:off]) are not
                                    // sibling recipients — the pair carries no information and its direction
                                    // is arbitrary (flipped between class-run and full-suite contexts)
                                    && !(sib instanceof io.codelaser.maddi.cst.api.variable.DependentVariable dvS
                                         && m instanceof io.codelaser.maddi.cst.api.variable.DependentVariable dvM
                                         && dvS.arrayVariable() != null
                                         && dvS.arrayVariable().equals(dvM.arrayVariable()))) {
                                    builder.add(new LinksImpl.LinkImpl(emitM, LinkNatureImpl.IS_ASSIGNED_TO, sib));
                                }
                            }
                        }
                    }
                } else if (!Gate.isSet("NOSIBEQ")) {
                    // CO-RECIPIENT IDENTITY (gate NOSIBEQ), per-statement views: two members DIRECTLY assigned
                    // the same source's value hold the same object — 'ii = (II)o' + 'ii2 = (II)o' ⟹ 'ii2 ≡ ii'
                    // (the §m companion follows in the fold). Direct records only (no chaining: a transitive
                    // source does not guarantee identity); reassignment of the source evicts it from the group,
                    // so both records still see the same value. Same-base DependentVariable pairs excluded
                    // (spelling aliases, cf. the sibling-recipient rule above).
                    for (Variable src : fwd.getOrDefault(m, java.util.List.of())) {
                        for (Variable sib : bwd.getOrDefault(src, java.util.List.of())) {
                            if (!sib.equals(m) && !emitM.equals(sib)
                                && !(sib instanceof io.codelaser.maddi.cst.api.variable.DependentVariable dvS
                                     && m instanceof io.codelaser.maddi.cst.api.variable.DependentVariable dvM
                                     && dvS.arrayVariable() != null
                                     && dvS.arrayVariable().equals(dvM.arrayVariable()))) {
                                builder.add(new LinksImpl.LinkImpl(emitM,
                                        LinkNatureImpl.makeIdenticalTo(null), sib));
                            }
                        }
                    }
                }
            }
        }
        return builder.build();
    }

    // the detached recipients of 'sv' (see detachedIn) that are part of 'primary'
    private java.util.Set<Variable> detachedFroms(SharedVariable sv, Variable primary) {
        java.util.Set<Variable> result = new java.util.LinkedHashSet<>();
        detachedIn.forEach((v, reps) -> {
            if (reps.contains(sv) && Util.isPartOf(primary, v)) result.add(v);
        });
        return result;
    }

    // variables reachable from 'start' along the adjacency map (excluding 'start'). Recurse THROUGH a node that is
    // a pass-through intermediate ($__rv) — filtered downstream, and would otherwise break a chain
    // (makeFromGet.t ← $__rv ← box.t) — OR a plain scalar local variable, to bridge an assignment chain through
    // method-internal locals (return ← ttt ← tt ← 0:t collapses to method ← 0:t; the locals are filtered
    // downstream). The dimension guard is 'canChainThrough': never recurse through an array element
    // (DependentVariable) or any field/parameter/this face, which would link mismatched dimensions
    // (crashes on grid[0][0]/varargs).
    private static java.util.Set<Variable> reachable(Variable start,
                                                     java.util.Map<Variable, java.util.List<Variable>> adj,
                                                     boolean deep) {
        java.util.Set<Variable> seen = new java.util.LinkedHashSet<>();
        java.util.Deque<Variable> stack = new java.util.ArrayDeque<>(adj.getOrDefault(start, java.util.List.of()));
        while (!stack.isEmpty()) {
            Variable v = stack.pop();
            if (seen.add(v) && canChainThrough(v, deep)) {
                stack.addAll(adj.getOrDefault(v, java.util.List.of()));
            }
        }
        return seen;
    }

    // 'm' keyed onto the primary: 'm' itself when it is part of the primary; the sibling-rehomed form when 'm' is a
    // proper field/element of a shared-group sibling of the primary (create2 ≡ p ⇒ 'p.f' -> 'create2.f'); null when
    // 'm' belongs to neither. The whole sibling ('p' itself, m.equals(face)) is skipped: its whole-object edge is
    // already emitted from the primary's own member, and rehoming it would produce a self-link.
    private Variable faceKeyed(Variable m, Variable primary, java.util.Collection<Variable> faces) {
        if (Util.isPartOf(primary, m)) return m;
        for (Variable face : faces) {
            if (!face.equals(primary) && !m.equals(face) && Util.isPartOf(face, m)) {
                VariableTranslationMap vtm = new VariableTranslationMap(runtime);
                vtm.put(face, primary);
                return vtm.translateVariableRecursively(m);
            }
        }
        return null;
    }

    // Fallback for members invisible to faceKeyed: a collapsed construction chain (builder pattern). The primary's
    // FIELD 'pf' (ldIn.variables) was assigned from a source face 's' ($__rv137.variables) whose scope root
    // ($__rv137, the build() result) is whole-object-grouped with the chain intermediates ($__c122 .. $__rv135).
    // A member recorded on a sibling's counterpart of 's' ($__rv124.variables[1], grouped with 'matrix' by the
    // fluent set(1, matrix)) denotes the same slot as pf's element: rehome it onto pf (ldIn.variables[1]), so the
    // slot links (ldIn.variables[1] ← matrix) survive the collapse. Only the SOURCE direction transfers
    // (pf ← s; assignmentSources), mirroring the memberFieldsOf projection: source knowledge flows to the
    // recipient, never the reverse.
    private Variable derivedFaceKeyed(Variable m, Variable primary) {
        if (Gate.isSet("NODF")) return null;
        /*
         The candidates are the members that are PARTS of the primary, and membersByPrefix answers that
         directly. Scanning the whole key set with isPartOf here was 40% of the analysis of a transformed
         loop body (asprof-style sampling, 2026-09-15): the transform packs thirty locals into
         `ld.variables[i]` slots, every slot is a group member, and this ran for every member of every
         group, for every variable, at every sub-block merge -- the quadratic the design notes name.
         Same order as the key set: both are first-membership order (LinkedHashMap put on an existing key
         keeps its place, so does the LinkedHashSet in the index).
        */
        for (Variable pf : membersRootedAt(primary)) {
            if (pf.equals(primary)) continue;
            for (Variable s : assignmentSources(pf)) {
                Variable root = Util.primary(s);
                // an array access on an EXPRESSION base has no primary variable (clone-bench shapes) — nothing
                // to rehome the slot onto
                if (root == null) continue;
                java.util.List<Variable> sourceFaces;
                if (root.equals(s)) {
                    // whole-object source ('withException.exit ← $__c_a'): its own faces
                    // ($__c_a.exception ← 0:e) rehome directly onto pf (withException.exit.exception).
                    // (faceKeyed's sibling faces only cover this when the PRIMARY itself is in the
                    // whole-object group; here only its field pf is.)
                    sourceFaces = java.util.List.of(s);
                } else {
                    sourceFaces = new java.util.ArrayList<>();
                    for (Variable sibling : allShared(root)) {
                        VariableTranslationMap toSibling = new VariableTranslationMap(runtime);
                        toSibling.put(root, sibling);
                        sourceFaces.add(toSibling.translateVariableRecursively(s));
                    }
                }
                for (Variable sFace : sourceFaces) {
                    // m may BE the face itself (fluent chain 'new Builder().setJ(jp).setK(kp)':
                    // m = $__rv9.j, the setJ face, sibling of b's source) — the emit loop's
                    // !emitM.equals(t) guard prevents self-links
                    if (Util.isPartOf(sFace, m)) {
                        VariableTranslationMap vtm = new VariableTranslationMap(runtime);
                        vtm.put(sFace, pf);
                        return vtm.translateVariableRecursively(m);
                    }
                }
            }
        }
        return null;
    }

    // The inverse of derivedFaceKeyed, for modification expansion: 'key' (ldIn.variables[1], modified through a
    // functional-interface call) is not itself a group member — the DV on the recipient never existed as a graph
    // vertex. Its base pf (ldIn.variables) IS a member; rehome the key onto the source-chain sibling faces
    // ($__rv124.variables[1]) and return THEIR groups' members ({matrix, 0:ld.variables[1], ...}): they denote the
    // same runtime slot, so a modification of the key is a modification of each of them.
    public java.util.Set<Variable> derivedShared(Variable key) {
        if (Gate.isSet("NODF")) return java.util.Set.of();
        if (memberToGroup.containsKey(key)) return java.util.Set.of(); // allShared covers group members
        java.util.Set<Variable> result = new java.util.LinkedHashSet<>();
        for (Variable pf : memberToGroup.keySet()) {
            if (pf.equals(key) || !Util.isPartOf(pf, key)) continue;
            for (Variable s : assignmentSources(pf)) {
                Variable root = Util.primary(s);
                // null: expression-based array access ('arr()[i]' has no array VARIABLE) — no root to
                // rehome onto, so it cannot contribute sibling faces (closed-core: NPE in a loop body)
                if (root == null || root.equals(s)) continue;
                for (Variable sibling : allShared(root)) {
                    VariableTranslationMap toSibling = new VariableTranslationMap(runtime);
                    toSibling.put(root, sibling);
                    Variable sFace = toSibling.translateVariableRecursively(s);
                    VariableTranslationMap vtm = new VariableTranslationMap(runtime);
                    vtm.put(pf, sFace);
                    Variable candidate = vtm.translateVariableRecursively(key);
                    SharedVariable sv = memberToGroup.get(candidate);
                    if (sv != null) result.addAll(sv.variables());
                }
            }
        }
        return result;
    }

    // a node we may transitively chain through: a pass-through intermediate ($__rv) always, or (only when 'deep',
    // i.e. reconstructing a summary endpoint) a bare scalar local variable — never an array element (dimension
    // guard) / parameter / this, which are summary endpoints themselves.
    private static boolean canChainThrough(Variable v, boolean deep) {
        if (Util.lvPrimaryOrNull(v) instanceof IntermediateVariable) return true;
        if (!deep) return false;
        if (v instanceof io.codelaser.maddi.cst.api.variable.LocalVariable) return true;
        // a FOREIGN method's return face in the group (the SAM's 'get' rv when an anonymous class
        // captures a variable: m ← get ← 0:x) is a value pass-through, like a dying local; the
        // primary's own rv is never a chain node in its own extraction (it is the emitM)
        if (v instanceof io.codelaser.maddi.modification.prepwork.variable.ReturnVariable) return true;
        // the field face of a DYING LOCAL ('b.j' of builder 'b') does not survive into the summary either:
        // bridge it (justJ.j ← b.j ← 0:jp → justJ.j ← 0:jp). Plain FieldReference only (never a
        // DependentVariable — dimensions), and only on a real local (not a synthetic LinkVariable).
        if (v instanceof io.codelaser.maddi.cst.api.variable.FieldReference fr) {
            Variable pr = Util.primary(fr);
            return pr instanceof io.codelaser.maddi.cst.api.variable.LocalVariable
                   && !(pr instanceof io.codelaser.maddi.modification.link.impl.LinkVariable);
        }
        return false;
    }

    public boolean isKnown(Variable from) {
        return memberToGroup.containsKey(from);
    }

    /*
    Persistent prefix index over the group members: scope-chain variable -> members spelled through it.
    Rebuilding this per statement inside drainDirtyVariables was 46% of the drain's CPU on fernflower
    (asprof 2026-08-05); membership mutations are rare, per-statement drains are not. Maintained in
    add() (first-time membership only — a member's prefixes never change) and remove().
     */
    private Map<Variable, java.util.Set<Variable>> membersByPrefix = new HashMap<>();

    public java.util.Set<Variable> membersRootedAt(Variable prefix) {
        return membersByPrefix.getOrDefault(prefix, java.util.Set.of());
    }

    private void indexMember(Variable member) {
        membersByPrefix.computeIfAbsent(member, _ -> new java.util.LinkedHashSet<>()).add(member);
        for (Variable p : Util.scopeVariables(member)) {
            membersByPrefix.computeIfAbsent(p, _ -> new java.util.LinkedHashSet<>()).add(member);
        }
    }

    private void unindexMember(Variable member) {
        java.util.function.BiConsumer<Variable, Variable> rm = (p, m) -> {
            java.util.Set<Variable> set = membersByPrefix.get(p);
            if (set != null) {
                set.remove(m);
                if (set.isEmpty()) membersByPrefix.remove(p);
            }
        };
        rm.accept(member, member);
        for (Variable p : Util.scopeVariables(member)) rm.accept(p, member);
    }

    public SharedVariable groupOf(Variable variable) {
        return memberToGroup.get(variable);
    }

    // group members (of ANY group) that are proper fields/elements of 'owner': for the fluent setter, 'this.i'
    // is a member of the {this.i, 0:i} group and therefore invisible under the {return, this} group's rep — but
    // it is a field of the face 'this' and must be discoverable for the field-level mirror (setI.i ← this.i).
    public Stream<Variable> memberFieldsOf(Variable owner) {
        // membersRootedAt(owner) IS {m : isPartOf(owner, m)}, in first-membership order like the key set
        return membersRootedAt(owner).stream().filter(m -> !m.equals(owner));
    }

    // the whole-object group members that 'variable' was (transitively) assigned FROM: for 'return zs' with group
    // {return, zs}, assignmentSources(return) = {zs}. Knowledge attached to a source (its §m equivalences, its
    // field-precise links) legitimately transfers to the recipient; the reverse direction does not (a pure source
    // must not inherit the recipient's links, see isPureAssignmentSource).
    public java.util.Set<Variable> assignmentSources(Variable variable) {
        SharedVariable sv = memberToGroup.get(variable);
        if (sv == null) return java.util.Set.of();
        // memoized on the group: FollowGraph asks this once per sibling, per rep expansion, per graph vertex, and
        // that walk runs per method call in a statement -- so recomputing it is cubic in the length of a
        // straight-line method (task #22). The memo is dropped by every mutator of SharedVariable.
        return sv.assignmentSources(variable, v -> {
            boolean deep = v instanceof io.codelaser.maddi.modification.prepwork.variable.ReturnVariable;
            java.util.Set<Variable> result =
                    new java.util.LinkedHashSet<>(reachable(v, sv.forwardAssignments(), deep));
            result.retainAll(sv.variables());
            return result;
        });
    }

    // A member that only appears on the 'to' (upstream) side of its group's assignments is a PURE SOURCE: a value
    // that flows into the collapsed variable, not a recipient of it. When 'x ← alternative' collapses {x, alternative}
    // and x also has 'x ← optional.§x', the rep carries 'rep ← optional.§x' — an edge that belongs to the recipient
    // (x, and whatever x flows into, e.g. the return), NOT to the source 'alternative'. So a pure source must not
    // inherit the rep's incoming edges; its own extraction stays empty. Returns false for non-collapsed variables.
    public boolean isPureAssignmentSource(Variable variable) {
        SharedVariable sv = memberToGroup.get(variable);
        if (sv == null) return false;
        boolean appearsAsFrom = sv.assignments().stream().anyMatch(a -> a.from().equals(variable));
        boolean appearsAsTo = sv.assignments().stream().anyMatch(a -> a.to().equals(variable));
        return appearsAsTo && !appearsAsFrom;
    }

    public Variable translateForward(Variable variable) {
        return variableTranslationMap.translateVariableRecursively(variable);
    }

    public String print(Function<Variable, String> variablePrinter) {
        return sharedVariablesByName.entrySet().stream()
                .map(e -> e.getKey() + ": "
                          + e.getValue().variables().stream().sorted(Variable::compareTo)
                                  .map(variablePrinter)
                                  .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
    }

    // the reps of the groups 'variable' was evicted from by a join (empty when none): see detachedIn
    public java.util.Set<SharedVariable> detachedReps(Variable variable) {
        return detachedIn.getOrDefault(variable, java.util.Set.of());
    }

    /*
     The variables that hold the same object as 'variable' in SOME alternative of an earlier join, but are no
     member of its group any more (see detachedIn): the members detached from 'variable''s group, and when
     'variable' was itself detached, the members of the groups it was detached from. A modification of 'variable'
     may be a modification of each of them: 'if (pred == null) first = succ; ... succ.pred = pred;' modifies the
     object 'first' holds on the path through the then-block (guava's LinkedHashMultimap.MultimapIterationChain).
     */
    public java.util.Set<Variable> detachedAliases(Variable variable) {
        if (evictedFrom.isEmpty()) return java.util.Set.of();
        // the groups 'variable' belongs to, now or in some alternative
        java.util.Set<SharedVariable> groups = new java.util.HashSet<>(evictedFrom.getOrDefault(variable, java.util.Set.of()));
        SharedVariable sv = memberToGroup.get(variable);
        if (sv != null) groups.add(sv);
        if (groups.isEmpty()) return java.util.Set.of();
        java.util.Set<Variable> result = new java.util.LinkedHashSet<>();
        for (SharedVariable group : groups) {
            if (group != sv) result.addAll(group.variables());
        }
        // and every variable evicted from one of those groups ('first' and 'succ' both left {first, succ} when
        // the else-block put 'succ' in another group)
        evictedFrom.forEach((v, reps) -> {
            for (SharedVariable rep : reps) {
                if (groups.contains(rep)) {
                    result.add(v);
                    break;
                }
            }
        });
        result.remove(variable);
        return result;
    }

    // 'variable' is (re)assigned: the assignments it received in earlier alternatives no longer describe it
    public void dropDetached(Variable variable) {
        if (!evictedFrom.isEmpty()) evictedFrom.remove(variable);
        if (detachedIn.isEmpty()) return;
        java.util.Set<SharedVariable> detached = detachedIn.remove(variable);
        if (detached != null) {
            for (SharedVariable sv : detached) {
                if (sv.assignments().removeIf(a -> variable.equals(a.from()))) sv.invalidateMemos();
            }
        }
    }

    public void remove(Variable variable) {
        dropDetached(variable);
        if (memberToGroup.remove(variable) != null) {
            unindexMember(variable);
            sharedVariablesByName.values().forEach(g -> g.remove(variable));
            boolean removed = variableTranslationMap.remove(variable);
            assert removed;
        }
    }

    /*
     The JOIN of the groups the alternatives of a statement end with. A group is an identity claim: its members
     hold the same object. So a member keeps its group only when it is in that same group at the end of EVERY
     alternative (a pure source -- a member that was only ever assigned FROM -- also when it sits in one group
     wherever it is grouped at all: 'this.f = in' in one branch does not make 'in' anything else in another).
     Any other member is EVICTED from its groups: merging the groups instead ('m = c ? a : b' does that within
     one statement) claimed that the values of different alternatives are one object -- false identities that
     reach the callers through summaries (fernflower's SwitchHelper.JavacSwitchCandidate lost the constructor's
     own assignments to them). An evicted member keeps what each alternative knew through a plain graph edge to
     that alternative's rep: 'x ← rep' where it was assigned from the group, 'rep ← x' where it was a source (the
     shape mergeEdgeBi already uses for a source assigned at another statement). Pure: the caller adds each
     alternative's edges to THAT alternative's graph (composed with the other alternatives' facts, an edge would
     derive what no execution does) and then installs the joined groups (restore).
     */
    public record JoinEdge(Variable from, Variable to) { // from ← to
    }

    public record JoinPlan(List<List<JoinEdge>> edgesPerAlternative, State joined) {
    }

    public JoinPlan planJoin(List<State> alternatives) {
        int n = alternatives.size();
        Map<Variable, SharedVariable[]> repsOf = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            int ii = i;
            alternatives.get(i).memberToGroup().forEach((member, rep) ->
                    repsOf.computeIfAbsent(member, _ -> new SharedVariable[n])[ii] = rep);
        }
        List<List<JoinEdge>> edges = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) edges.add(new java.util.ArrayList<>());
        Map<Variable, SharedVariable> kept = new LinkedHashMap<>();
        java.util.Set<Variable> evicted = new java.util.HashSet<>();
        repsOf.forEach((member, reps) -> {
            java.util.Set<SharedVariable> distinct = new java.util.LinkedHashSet<>();
            boolean everywhere = true;
            boolean pureSource = true;
            for (int i = 0; i < n; i++) {
                if (reps[i] == null) {
                    everywhere = false;
                } else {
                    distinct.add(reps[i]);
                    if (isRecipient(alternatives.get(i), reps[i], member)) pureSource = false;
                }
            }
            if (distinct.size() == 1 && (everywhere || pureSource)) {
                kept.put(member, distinct.iterator().next());
            } else {
                evicted.add(member);
                for (int i = 0; i < n; i++) {
                    if (reps[i] == null) continue;
                    edges.get(i).add(isRecipient(alternatives.get(i), reps[i], member)
                            ? new JoinEdge(member, reps[i]) : new JoinEdge(reps[i], member));
                }
            }
        });
        // the joined groups: every rep of any alternative (an emptied rep stays known; its vertices still carry
        // what its members established), the kept members, and the records that mention no evicted member
        Map<String, SharedVariable> byName = new LinkedHashMap<>();
        Map<SharedVariable, SharedVariable.Data> data = new LinkedHashMap<>();
        Map<Variable, java.util.Set<SharedVariable>> detached = new HashMap<>();
        // detached records the alternatives already carried stay detached (their variable is no member)
        Map<Variable, java.util.Set<SharedVariable>> evictedFrom = new HashMap<>();
        for (State alternative : alternatives) {
            alternative.detachedIn().forEach((v, reps) -> {
                if (!kept.containsKey(v)) detached.computeIfAbsent(v, _ -> new java.util.LinkedHashSet<>()).addAll(reps);
            });
            alternative.evictedFrom().forEach((v, reps) ->
                    evictedFrom.computeIfAbsent(v, _ -> new java.util.LinkedHashSet<>()).addAll(reps));
        }
        repsOf.forEach((member, reps) -> {
            if (!evicted.contains(member)) return;
            for (SharedVariable rep : reps) {
                if (rep != null) evictedFrom.computeIfAbsent(member, _ -> new java.util.LinkedHashSet<>()).add(rep);
            }
        });
        for (State alternative : alternatives) {
            alternative.sharedVariablesByName().forEach((name, rep) -> {
                byName.putIfAbsent(name, rep);
                SharedVariable.Data joined = data.computeIfAbsent(rep, _ -> new SharedVariable.Data());
                SharedVariable.Data d = alternative.data().get(rep);
                if (d == null) return;
                for (Variable v : d.variables()) {
                    if (kept.get(v) == rep) joined.variables().add(v);
                }
                for (SharedVariable.Assignment a : d.assignments()) {
                    if (!joined.assignments().contains(a)) joined.assignments().add(a);
                    if (evicted.contains(a.from())) {
                        detached.computeIfAbsent(a.from(), _ -> new java.util.LinkedHashSet<>()).add(rep);
                    }
                }
            });
        }
        VariableTranslationMap vtm = new VariableTranslationMap(runtime);
        Map<Variable, java.util.Set<Variable>> prefixes = new HashMap<>();
        kept.forEach((member, rep) -> {
            vtm.put(member, rep);
            prefixes.computeIfAbsent(member, _ -> new java.util.LinkedHashSet<>()).add(member);
            for (Variable pv : Util.scopeVariables(member)) {
                prefixes.computeIfAbsent(pv, _ -> new java.util.LinkedHashSet<>()).add(member);
            }
        });
        return new JoinPlan(edges, new State(byName, new LinkedHashMap<>(kept), vtm, prefixes, data, detached,
                evictedFrom));
    }

    private static boolean isRecipient(State state, SharedVariable rep, Variable member) {
        SharedVariable.Data d = state.data().get(rep);
        return d != null && d.assignments().stream().anyMatch(a -> a.from().equals(member));
    }

    /*
     Names of reps created while an alternative was linked. Not part of a State: two alternatives start from the
     same state and would otherwise pick the same fresh name for two different groups, which a join (whose graph
     vertices are keyed by name) would then confuse.
     */
    private final java.util.Set<String> reservedNames = new java.util.HashSet<>();
    private int alternativeDepth;

    public void enterAlternatives() {
        alternativeDepth++;
    }

    public void leaveAlternatives() {
        alternativeDepth--;
    }

    private SharedVariable create(Variable referenceVariable, Variable firstAssignedTo) {
        String newName = makeName(SharedVariable.PREFIX + referenceVariable.simpleName());
        SharedVariable sv = new SharedVariable(newName, referenceVariable.parameterizedType(),
                runtime);
        sharedVariablesByName.put(sv.fullyQualifiedName(), sv);
        add(sv, referenceVariable);
        add(sv, firstAssignedTo);
        return sv;
    }

    private String makeName(String s) {
        int i = 0;
        while (sharedVariablesByName.containsKey(name(s, i)) || reservedNames.contains(name(s, i))) {
            ++i;
        }
        String name = name(s, i);
        if (alternativeDepth > 0) reservedNames.add(name);
        return name;
    }

    private static String name(String s, int i) {
        return s + (i > 0 ? "" + i : "");
    }

    private void add(SharedVariable sharedVariable, Variable variable) {
        sharedVariable.add(variable);
        if (memberToGroup.put(variable, sharedVariable) == null) {
            indexMember(variable); // first-time membership; a member's prefixes never change
        }
        variableTranslationMap.put(variable, sharedVariable);
    }

    // 'x = y; ...; x = z' style bridging of two existing groups: fold sv2 into sv1. Members, assignments and
    // member->group/translation entries move; the bridging assignment is recorded on the survivor. sv2's rep is
    // remembered in lastMergedAway so the caller can re-key its graph vertices onto sv1.
    private void merge(SharedVariable sv1, SharedVariable sv2, Variable from, Variable to, String statementIndex) {
        for (Variable v : sv2.variables()) {
            sv1.add(v);
            memberToGroup.put(v, sv1);
            variableTranslationMap.put(v, sv1);
        }
        sv1.addAssignments(sv2.assignments());
        sv1.addAssignment(from, to, statementIndex);
        sharedVariablesByName.remove(sv2.fullyQualifiedName());
        lastMergedAway = sv2;
    }

}
