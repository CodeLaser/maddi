package org.e2immu.analyzer.modification.link.impl.linkgraph;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.localvar.IntermediateVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.SharedVariable;
import org.e2immu.analyzer.modification.link.impl.translate.VariableTranslationMap;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.Link;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;

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
    private final Map<String, SharedVariable> sharedVariablesByName = new LinkedHashMap<>();
    private final Map<Variable, SharedVariable> memberToGroup = new LinkedHashMap<>();
    private final VariableTranslationMap variableTranslationMap;

    public SharedVariables(Runtime runtime) {
        this.runtime = runtime;
        variableTranslationMap = new VariableTranslationMap(runtime);
    }

    public Collection<Variable> allShared(Variable variable) {
        SharedVariable sv = memberToGroup.get(variable);
        if(sv == null) return List.of(variable);
        return sv.variables();
    }

    public SharedVariable isAssignedFrom(Variable from, Variable to, String statementIndex) {
        SharedVariable sv1 = memberToGroup.get(from);
        SharedVariable sv2 = memberToGroup.get(to);
        if (sv1 == null && sv2 == null) {
            SharedVariable sv = create(from, to);
            sv.addAssignment(from, to, statementIndex);
            return sv;
        }
        if (sv1 == sv2) {
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
    public Stream<Link> assignmentEdgeStream(Variable primary) {
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
            for (SharedVariable.Assignment a : sv.assignments()) {
                fwd.computeIfAbsent(a.from(), k -> new java.util.ArrayList<>()).add(a.to());
                bwd.computeIfAbsent(a.to(), k -> new java.util.ArrayList<>()).add(a.from());
            }
            for (Variable m : sv.variables()) {
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
                        instanceof org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable
                        // parameters are summary endpoints too: '0:in → v → this.f' must reach the
                        // field for the parameter's summary (var-transparency). Gate NOPDEEP.
                        || System.getenv("NOPDEEP") == null
                           && Util.primary(emitM) instanceof org.e2immu.language.cst.api.info.ParameterInfo;
                for (Variable t : reachable(m, fwd, deep)) {
                    if (!emitM.equals(t)) {
                        builder.add(new LinksImpl.LinkImpl(emitM, LinkNatureImpl.IS_ASSIGNED_FROM, t));
                        // containment companions, ONLY for a derived slot face (a real slot vertex gets
                        // 'slot ∈ container' from the graph and 'container ∋ value' from sub-propagation;
                        // a derived face — td.variables[0] ← someSet, rehomed across the collapsed builder
                        // chain — bypasses both): td.variables[0] ∈ td.variables, td.variables ∋ someSet.
                        if (derivedFace
                            && emitM instanceof org.e2immu.language.cst.api.variable.DependentVariable dv
                            && dv.arrayVariable() != null && !Util.virtual(t)) {
                            builder.add(new LinksImpl.LinkImpl(dv, LinkNatureImpl.IS_ELEMENT_OF, dv.arrayVariable()));
                            builder.add(new LinksImpl.LinkImpl(dv.arrayVariable(), LinkNatureImpl.CONTAINS_AS_MEMBER, t));
                        }
                    }
                }
                for (Variable t : reachable(m, bwd, deep)) {
                    if (!emitM.equals(t)) builder.add(new LinksImpl.LinkImpl(emitM, LinkNatureImpl.IS_ASSIGNED_TO, t));
                }
            }
        }
        return builder.build();
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
        if (System.getenv("NODF") != null) return null;
        for (Variable pf : memberToGroup.keySet()) {
            if (pf.equals(primary) || !Util.isPartOf(primary, pf)) continue;
            for (Variable s : assignmentSources(pf)) {
                Variable root = Util.primary(s);
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
        if (System.getenv("NODF") != null) return java.util.Set.of();
        if (memberToGroup.containsKey(key)) return java.util.Set.of(); // allShared covers group members
        java.util.Set<Variable> result = new java.util.LinkedHashSet<>();
        for (Variable pf : memberToGroup.keySet()) {
            if (pf.equals(key) || !Util.isPartOf(pf, key)) continue;
            for (Variable s : assignmentSources(pf)) {
                Variable root = Util.primary(s);
                if (root.equals(s)) continue;
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
        if (v instanceof org.e2immu.language.cst.api.variable.LocalVariable) return true;
        // the field face of a DYING LOCAL ('b.j' of builder 'b') does not survive into the summary either:
        // bridge it (justJ.j ← b.j ← 0:jp → justJ.j ← 0:jp). Plain FieldReference only (never a
        // DependentVariable — dimensions), and only on a real local (not a synthetic LinkVariable).
        if (v instanceof org.e2immu.language.cst.api.variable.FieldReference fr) {
            Variable pr = Util.primary(fr);
            return pr instanceof org.e2immu.language.cst.api.variable.LocalVariable
                   && !(pr instanceof org.e2immu.analyzer.modification.link.impl.LinkVariable);
        }
        return false;
    }

    public boolean isKnown(Variable from) {
        return memberToGroup.containsKey(from);
    }

    // group members (of ANY group) that are proper fields/elements of 'owner': for the fluent setter, 'this.i'
    // is a member of the {this.i, 0:i} group and therefore invisible under the {return, this} group's rep — but
    // it is a field of the face 'this' and must be discoverable for the field-level mirror (setI.i ← this.i).
    public Stream<Variable> memberFieldsOf(Variable owner) {
        return memberToGroup.keySet().stream()
                .filter(m -> !m.equals(owner) && Util.isPartOf(owner, m));
    }

    // the whole-object group members that 'variable' was (transitively) assigned FROM: for 'return zs' with group
    // {return, zs}, assignmentSources(return) = {zs}. Knowledge attached to a source (its §m equivalences, its
    // field-precise links) legitimately transfers to the recipient; the reverse direction does not (a pure source
    // must not inherit the recipient's links, see isPureAssignmentSource).
    public java.util.Set<Variable> assignmentSources(Variable variable) {
        SharedVariable sv = memberToGroup.get(variable);
        if (sv == null) return java.util.Set.of();
        java.util.Map<Variable, java.util.List<Variable>> fwd = new java.util.HashMap<>();
        for (SharedVariable.Assignment a : sv.assignments()) {
            fwd.computeIfAbsent(a.from(), k -> new java.util.ArrayList<>()).add(a.to());
        }
        boolean deep = variable instanceof org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
        java.util.Set<Variable> result = new java.util.LinkedHashSet<>(reachable(variable, fwd, deep));
        result.retainAll(sv.variables());
        return result;
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

    public void remove(Variable variable) {
        if (memberToGroup.remove(variable) != null) {
            sharedVariablesByName.values().forEach(g -> g.remove(variable));
            boolean removed = variableTranslationMap.remove(variable);
            assert removed;
        }
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
        while (sharedVariablesByName.containsKey(name(s, i))) {
            ++i;
        }
        return name(s, i);
    }

    private static String name(String s, int i) {
        return s + (i > 0 ? "" + i : "");
    }

    private void add(SharedVariable sharedVariable, Variable variable) {
        sharedVariable.add(variable);
        memberToGroup.put(variable, sharedVariable);
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
        sv1.assignments().addAll(sv2.assignments());
        sv1.addAssignment(from, to, statementIndex);
        sharedVariablesByName.remove(sv2.fullyQualifiedName());
        lastMergedAway = sv2;
    }

}
