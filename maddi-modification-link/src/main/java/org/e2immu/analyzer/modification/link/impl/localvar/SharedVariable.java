package org.e2immu.analyzer.modification.link.impl.localvar;

import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.variable.LocalVariableImpl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SharedVariable extends LocalVariableImpl implements LinkVariable {
    public static final String PREFIX = "$__sv_";

    // a directed assignment 'from ← to' (from IS_ASSIGNED_FROM to) that folded these two members into the group.
    // Kept so the group's intra-member relation can be reconstructed at summary extraction (the collapse only
    // stores it once; cf. VirtualModificationIdenticals.Group for the ≡ analogue).
    public record Assignment(Variable from, Variable to) {
    }

    private final Set<Variable> variables = new LinkedHashSet<>();
    private final List<Assignment> assignments = new ArrayList<>();

    public SharedVariable(String name, ParameterizedType parameterizedType, Runtime runtime) {
        super(name, parameterizedType, runtime.newEmptyExpression());
    }

    public boolean add(Variable variable) {
        return variables.add(variable);
    }

    public void addAssignment(Variable from, Variable to) {
        assignments.add(new Assignment(from, to));
    }

    public List<Assignment> assignments() {
        return assignments;
    }

    @Override
    public boolean acceptForLinkedVariables() {
        // a shared-variable rep is a synthetic group representative ($__sv_); it must never surface directly in
        // the output — its real members are expanded (iterateOverShared) and filtered individually. If a rep
        // reaches this filter unexpanded, drop it rather than emit the synthetic name (mirrors IntermediateVariable).
        return false;
    }

    public Set<Variable> variables() {
        return variables;
    }

    public void removeAll(Set<Variable> variables) {
        this.variables.removeAll(variables);
        assignments.removeIf(a -> variables.contains(a.from()) || variables.contains(a.to()));
    }

    public void remove(Variable variable) {
        variables.remove(variable);
        assignments.removeIf(a -> variable.equals(a.from()) || variable.equals(a.to()));
    }
}
