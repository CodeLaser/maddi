package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.annotation.Modified;
import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.variable.Variable;

public interface VariableInfoContainer {
     /* note: local variables also have a variableNature. This one can override their value
        but only when this variable nature is VariableDefinedOutsideLoop. All the others have to agree exactly,
        and can be used interchangeably.
         */

    @NotNull
    Variable variable();

    @NotNull
    VariableNature variableNature();

    // default statement/modification time
    int IGNORE_STATEMENT_TIME = -1;

    // prefixes in assignment id
    // see TestLevelSuffixes to visually understand the order

    String NOT_YET_READ = "-";

    boolean hasEvaluation();

    boolean hasMerge();

    /*
    return true on progress
     */
    @Modified
    boolean setLinkedVariables(LinkedVariables linkedVariables, Stage level);

    boolean isPrevious();

    boolean has(Stage level);

    /**
     * General method for obtaining the "most relevant" <code>VariableInfo</code> object describing the state
     * of the variable after executing this statement.
     *
     * @return a VariableInfo object, always. There would not have been a <code>VariableInfoContainer</code> if there
     * was not at least one <code>VariableInfo</code> object.
     */
    @NotNull
    VariableInfo best();

    /*
     * like current, but then with a limit
     */
    @NotNull
    VariableInfo best(Stage level);

    /**
     * Returns either the current() of the previous VIC, or the initial value if this is the first statement
     * where this variable occurs.
     */
    @NotNull
    VariableInfo getPreviousOrInitial();

    /**
     * @return if the variable was created in this statement
     */
    boolean isInitial();

    boolean isRecursivelyInitial();

    @NotNull
    VariableInfo getRecursiveInitialOrNull();

    VariableInfo bestCurrentlyComputed();

    String indexOfDefinition();
}
