package org.e2immu.analyzer.modification.link.impl.localvar;

import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.variable.LocalVariableImpl;

import java.util.LinkedHashSet;
import java.util.Set;

public class SharedVariable extends LocalVariableImpl implements LinkVariable {
    public static final String PREFIX = "$__sv_";

    private final Variable referenceVariable;
    private final Set<Variable> variables = new LinkedHashSet<>();

    public SharedVariable(String name, Variable referenceVariable, Runtime runtime) {
        super(PREFIX + name, referenceVariable.parameterizedType(),
                runtime.newVariableExpression(referenceVariable));
        this.referenceVariable = referenceVariable;
        variables.add(referenceVariable);
    }

    public boolean add(Variable variable) {
        return variables.add(variable);
    }

    @Override
    public boolean acceptForLinkedVariables() {
        return false;
    }

    public Variable referenceVariable() {
        return referenceVariable;
    }

    public Set<Variable> variables() {
        return variables;
    }

    public void removeAll(Set<Variable> variables) {
        this.variables.removeAll(variables);
    }
}
