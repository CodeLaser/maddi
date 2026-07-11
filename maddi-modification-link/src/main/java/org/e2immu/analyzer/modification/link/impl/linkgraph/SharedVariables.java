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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// variant on EquivalenceGroup, to be used for equivalent SharedVariable objects
public class SharedVariables {
    private final Runtime runtime;
    private final Map<String, SharedVariable> sharedVariablesByName = new HashMap<>();
    private final Map<Variable, SharedVariable> memberToGroup = new HashMap<>();
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

    public SharedVariable isAssignedFrom(Variable from, Variable to) {
        SharedVariable sv1 = memberToGroup.get(from);
        SharedVariable sv2 = memberToGroup.get(to);
        if (sv1 == null && sv2 == null) {
            SharedVariable sv = create(from, to);
            sv.addAssignment(from, to);
            return sv;
        }
        if (sv1 == sv2) {
            return null; // already in the same group
        }
        if (sv1 == null) {
            add(sv2, from);
            sv2.addAssignment(from, to);
            return sv2;
        }
        if (sv2 == null) {
            add(sv1, to);
            sv1.addAssignment(from, to);
            return sv1;
        }
        // merge 2 groups
        merge(sv1, sv2, from, to);
        return sv1;
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
                if (emitM == null) continue;
                // Chain through method-internal locals only when reconstructing the whole return value: there a
                // chain of whole-object assignments (return ← ttt ← tt ← 0:t) must collapse to 'method ← 0:t'.
                // For field/parameter endpoints the field-precise link already arrives via the always-chained
                // pass-through intermediates ($__rv), and bridging locals there would add a spurious primary-level
                // shortcut (makeFromGet ≈ 0:box). For a plain local 'm' (per-statement view) we also stay shallow,
                // preserving the collapse's dedup ('ttt ← tt', not the transitive 'ttt ← 0:t').
                boolean deep = emitM instanceof org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
                for (Variable t : reachable(m, fwd, deep)) {
                    if (!emitM.equals(t)) builder.add(new LinksImpl.LinkImpl(emitM, LinkNatureImpl.IS_ASSIGNED_FROM, t));
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

    // a node we may transitively chain through: a pass-through intermediate ($__rv) always, or (only when 'deep',
    // i.e. reconstructing a summary endpoint) a bare scalar local variable — never an array element / field /
    // parameter, so the assignment dimension is preserved.
    private static boolean canChainThrough(Variable v, boolean deep) {
        if (Util.lvPrimaryOrNull(v) instanceof IntermediateVariable) return true;
        return deep && v instanceof org.e2immu.language.cst.api.variable.LocalVariable;
    }

    public boolean isKnown(Variable from) {
        return memberToGroup.containsKey(from);
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

    private void merge(SharedVariable sv1, SharedVariable sv2, Variable from, Variable to) {
        throw new UnsupportedOperationException("NYI");
    }

}
