package org.e2immu.analyzer.modification.prepwork.variable.impl;

import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.PropertyValueMapImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VariableInfoImpl implements VariableInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(VariableInfoImpl.class);

    public static final Property UNMODIFIED_VARIABLE = new PropertyImpl("unmodifiedVariable");
    public static final Property MODIFIED_FI_COMPONENTS_VARIABLE =
            new PropertyImpl("modifiedFunctionalInterfaceComponentsVariable",
                    ValueImpl.VariableBooleanMapImpl.EMPTY);

    public static final Property DOWNCAST_VARIABLE = new PropertyImpl("downcastVariable", ValueImpl.SetOfTypeInfoImpl.EMPTY);

    private LinkedVariables linkedVariables;
    private StaticValues staticValues;

    private final PropertyValueMap analysis = new PropertyValueMapImpl();

    private final Variable variable;
    private final Assignments assignments;
    private final Reads reads;
    private final VariableData variableInClosure;

    public VariableInfoImpl(Variable variable, Assignments assignments, Reads reads, VariableData variableInClosure) {
        this.variable = variable;
        this.assignments = assignments;
        this.reads = reads;
        this.variableInClosure = variableInClosure;
    }

    public void initializeLinkedVariables(LinkedVariables initialValue) {
        if (this.linkedVariables == null) {
            this.linkedVariables = initialValue;
        }
    }

    public boolean setLinkedVariables(LinkedVariables linkedVariables) {
        assert linkedVariables != null;
        assert !linkedVariables.contains(variable) : "Self references are not allowed";
        if (this.linkedVariables.isNotYetSet() || this.linkedVariables.isDelayed()) {
            this.linkedVariables = linkedVariables;
            return true;
        }
        if (this.linkedVariables.equals(linkedVariables)) {
            return false;
        }
        if (this.linkedVariables.overwriteAllowed(linkedVariables)) {
            this.linkedVariables = linkedVariables;
            return true;
        }
        // FIXME-DEMO this should be an exception thrown
        LOGGER.warn("Variable {}: new linked variables are not better than old: {}, new {}",
                variable, this.linkedVariables, linkedVariables);
        return false;
    }

    public boolean staticValuesIsSet() {
        return staticValues != null && !staticValues.isDefault();
    }

    public void staticValuesSet(StaticValues staticValues) {
        if (this.staticValues == null || this.staticValues.overwriteAllowed(staticValues)) {
            this.staticValues = staticValues;
        }
    }

    @Override
    public Variable variable() {
        return variable;
    }

    @Override
    public LinkedVariables linkedVariables() {
        return linkedVariables;
    }

    @Override
    public StaticValues staticValues() {
        return staticValues;
    }

    @Override
    public PropertyValueMap analysis() {
        return analysis;
    }

    @Override
    public Assignments assignments() {
        return assignments;
    }

    @Override
    public Reads reads() {
        return reads;
    }

    @Override
    public boolean hasBeenDefined(String index) {
        if (variable instanceof LocalVariable || variable instanceof ReturnVariable) {
            return assignments.hasAValueAt(index);
        }
        return true;
    }

    @Override
    public boolean isUnmodified() {
        return analysis.getOrDefault(UNMODIFIED_VARIABLE, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    @Override
    public VariableData variableInfoInClosure() {
        return variableInClosure;
    }

    @Override
    public String toString() {
        return "VI[" + variable + "]";
    }
}
