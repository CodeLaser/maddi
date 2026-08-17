package io.codelaser.maddi.modification.link.impl.localvar;

import io.codelaser.maddi.modification.link.impl.LinkVariable;
import io.codelaser.maddi.modification.link.impl.Result;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;

public class FunctionalInterfaceVariable extends MarkerVariable implements LocalVariable, LinkVariable {
    public static final String LABEL = "fi";

    // use links, extra, modified
    private final Result result;

    public FunctionalInterfaceVariable(Runtime runtime,
                                       int index,
                                       ParameterizedType parameterizedType,
                                       Result result) {
        super(PREFIX + LABEL + index, parameterizedType, runtime.newEmptyExpression());
        this.result = result;
    }

    // for streaming/codec
    public FunctionalInterfaceVariable(String name,
                                       ParameterizedType parameterizedType,
                                       Runtime runtime,
                                       Result result) {
        super(name, parameterizedType, runtime.newEmptyExpression());
        this.result = result;
    }

    public Result result() {
        return result;
    }
}
