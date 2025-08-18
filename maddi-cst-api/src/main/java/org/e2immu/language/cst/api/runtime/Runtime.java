package org.e2immu.language.cst.api.runtime;

import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.info.ComputeMethodOverrides;
import org.e2immu.language.cst.api.variable.Variable;

public interface Runtime extends Predefined, Factory, Eval, Types {

    LanguageConfiguration configuration();

    ComputeMethodOverrides computeMethodOverrides();

    Variable getterVariable(MethodCall methodCall);

    /* return the field rather than the getter*/
    Variable setterVariable(MethodCall methodCall);

}
