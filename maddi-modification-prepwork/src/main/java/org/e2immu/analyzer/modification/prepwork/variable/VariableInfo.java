package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.analyzer.modification.prepwork.variable.impl.Assignments;
import org.e2immu.analyzer.modification.prepwork.variable.impl.Reads;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.variable.Variable;

public interface VariableInfo {

    // FIRST PASS DATA, assigned in the prep-work stage during construction

    // for later
    //Set<Integer> readAtStatementTimes();

    /*
    Information about assignments to this variable.
     */
    Assignments assignments();

    default boolean isVariableInClosure() {
        return variableInfoInClosure() != null;
    }

    VariableData variableInfoInClosure();

    Reads reads();

    /*
    is there a definite value for this variable at the end of this statement?

    when this is the return variable:
    return true when the method's flow cannot go beyond index.
    e.g. true when at index, there is a return or throws, or an if in which both blocks exit.
     */
    boolean hasBeenDefined(String index);

    Variable variable();

    // SECOND PASS DATA, assigned in the modification stage; eventually immutable

    LinkedVariables linkedVariables();

    StaticValues staticValues();

    PropertyValueMap analysis();

    // for later
    //int modificationTime();

    boolean isUnmodified();

    // for testing only
    default boolean isModified() {
        return !isUnmodified();
    }
}
