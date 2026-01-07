package org.e2immu.analyzer.modification.link.impl.localvar;

import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.analyzer.modification.link.impl.Result;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;

public class FunctionalInterfaceVariable extends MarkerVariable implements LocalVariable, LinkVariable {
    public static final String LABEL = "fi";

    private final Result result;

    public FunctionalInterfaceVariable(Runtime runtime, int index,
                                       ParameterizedType parameterizedType,
                                       Result result) {
        super(LABEL + index, parameterizedType, runtime.newEmptyExpression());
        this.result = result;
    }

    public Result result() {
        return result;
    }
}
