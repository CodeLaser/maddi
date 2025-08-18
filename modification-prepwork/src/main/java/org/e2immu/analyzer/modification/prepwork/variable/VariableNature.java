package org.e2immu.analyzer.modification.prepwork.variable;

public interface VariableNature {

    default boolean isNormal() {
        return false;
    }

}
