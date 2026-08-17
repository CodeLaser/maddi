package io.codelaser.maddi.modification.link.impl.localvar;

import io.codelaser.maddi.modification.link.impl.LinkVariable;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.impl.variable.LocalVariableImpl;

public class MarkerVariable extends LocalVariableImpl implements LocalVariable, LinkVariable {
    public static final String PREFIX = "$_";

    public static final String LABEL_CONSTANT = "ce";
    public static final String LABEL_SOME_VALUE = "v";

    private static final String SOME_VALUE_PREFIX = PREFIX + LABEL_SOME_VALUE;
    private static final String CONSTANT_PREFIX = PREFIX + LABEL_CONSTANT;

    // public for codec
    public MarkerVariable(String name, ParameterizedType parameterizedType, Expression assignmentExpression) {
        super(name, parameterizedType, assignmentExpression);
    }

    public static MarkerVariable constant(int index, ParameterizedType parameterizedType, Expression expression) {
        return new MarkerVariable(PREFIX + LABEL_CONSTANT + index, parameterizedType, expression);
    }

    public static MarkerVariable someValue(Runtime runtime, ParameterizedType parameterizedType) {
        return new MarkerVariable(PREFIX + LABEL_SOME_VALUE, parameterizedType, runtime.newEmptyExpression());
    }

    @Override
    public boolean acceptForLinkedVariables() {
        return true;
    }

    public boolean isSomeValue() {
        return simpleName().startsWith(SOME_VALUE_PREFIX);
    }

    public boolean isConstant() {
        return simpleName().startsWith(CONSTANT_PREFIX);
    }
}
