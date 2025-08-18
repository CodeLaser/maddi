package org.e2immu.analyzer.modification.prepwork.variable.impl;

import org.e2immu.analyzer.modification.prepwork.variable.VariableNature;

public class NormalVariableNature implements VariableNature {
    public static NormalVariableNature INSTANCE = new NormalVariableNature();

    @Override
    public boolean isNormal() {
        return true;
    }
}
