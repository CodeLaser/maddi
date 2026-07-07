package org.e2immu.analyzer.modification.link.impl.localvar;

import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.variable.LocalVariableImpl;

import java.util.LinkedHashSet;
import java.util.Set;

public class SharedVariable extends LocalVariableImpl implements LinkVariable {
    public static final String PREFIX = "$__sv_";

    private final Set<Variable> variables = new LinkedHashSet<>();

    public SharedVariable(String name, ParameterizedType parameterizedType, Runtime runtime) {
        super(name, parameterizedType, runtime.newEmptyExpression());
    }

    public boolean add(Variable variable) {
        return variables.add(variable);
    }

    @Override
    public boolean acceptForLinkedVariables() {
        throw new UnsupportedOperationException();
    }

    public Set<Variable> variables() {
        return variables;
    }

    public void removeAll(Set<Variable> variables) {
        this.variables.removeAll(variables);
    }

    public void remove(Variable variable) {
        variables.remove(variable);
    }
}
