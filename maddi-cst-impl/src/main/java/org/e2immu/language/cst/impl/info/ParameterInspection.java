package org.e2immu.language.cst.impl.info;

public interface ParameterInspection extends Inspection {
    boolean isFinal();

    boolean isVarArgs();

}
