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
                if (!Util.isPartOf(primary, m)) continue;
                for (Variable t : reachable(m, fwd)) {
                    builder.add(new LinksImpl.LinkImpl(m, LinkNatureImpl.IS_ASSIGNED_FROM, t));
                }
                for (Variable t : reachable(m, bwd)) {
                    builder.add(new LinksImpl.LinkImpl(m, LinkNatureImpl.IS_ASSIGNED_TO, t));
                }
            }
        }
        return builder.build();
    }

    // variables reachable from 'start' along the adjacency map (excluding 'start'). Only recurse THROUGH a node
    // that is a pass-through intermediate ($__rv) — those are filtered out downstream and would otherwise break
    // a chain (makeFromGet.t ← $__rv ← box.t). Never chain through real variables or array elements: doing so
    // links mismatched dimensions (crashes on grid[0][0]/varargs).
    private static java.util.Set<Variable> reachable(Variable start, java.util.Map<Variable, java.util.List<Variable>> adj) {
        java.util.Set<Variable> seen = new java.util.LinkedHashSet<>();
        java.util.Deque<Variable> stack = new java.util.ArrayDeque<>(adj.getOrDefault(start, java.util.List.of()));
        while (!stack.isEmpty()) {
            Variable v = stack.pop();
            if (seen.add(v) && Util.lvPrimaryOrNull(v) instanceof IntermediateVariable) {
                stack.addAll(adj.getOrDefault(v, java.util.List.of()));
            }
        }
        return seen;
    }

    public boolean isKnown(Variable from) {
        return memberToGroup.containsKey(from);
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
