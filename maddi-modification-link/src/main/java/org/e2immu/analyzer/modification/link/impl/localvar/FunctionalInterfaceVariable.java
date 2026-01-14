package org.e2immu.analyzer.modification.link.impl.localvar;

import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.analyzer.modification.link.impl.Result;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Set;

public class FunctionalInterfaceVariable extends MarkerVariable implements LocalVariable, LinkVariable {
    public static final String LABEL = "fi";

    private final Result result;
    private final Set<Variable> modified;
    public FunctionalInterfaceVariable(Runtime runtime, int index,
                                       ParameterizedType parameterizedType,
                                       Result result,
                                       Set<Variable> modified) {
        super(LABEL + index, parameterizedType, runtime.newEmptyExpression());
        this.result = result;
        this.modified = modified;
    }

    public Set<Variable> modified() {
        return modified;
    }

    public Result result() {
        return result;
    }
}
