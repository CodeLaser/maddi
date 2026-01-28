package org.e2immu.analyzer.modification.link.impl.localvar;

import org.e2immu.analyzer.modification.link.impl.LinkVariable;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.impl.variable.LocalVariableImpl;

public class IntermediateVariable extends LocalVariableImpl implements LocalVariable, LinkVariable {
    public static final String PREFIX = "$__";

    public static final String LABEL_RETURN_VALUE = "rv";
    public static final String LABEL_NEW_OBJECT = "c";
    public static final String LABEL_INLINE_CONDITION = "ic";

    private static final String RV_PREFIX = PREFIX + LABEL_RETURN_VALUE;
    private static final String NEW_PREFIX = PREFIX + LABEL_NEW_OBJECT;

    IntermediateVariable(String name, ParameterizedType parameterizedType, Expression assignmentExpression) {
        super(PREFIX + name, parameterizedType, assignmentExpression);
    }

    public boolean isNewObject() {
        return simpleName().startsWith(NEW_PREFIX);
    }

    public boolean isReturnVariable() {
        return simpleName().startsWith(RV_PREFIX);
    }

    public static IntermediateVariable returnValue(Runtime runtime, int index, ParameterizedType parameterizedType) {
        return new IntermediateVariable(LABEL_RETURN_VALUE + index, parameterizedType, runtime.newEmptyExpression());
    }

    public static IntermediateVariable parameterValue(int index, ParameterizedType parameterizedType, Expression assignmentExpression) {
        return new IntermediateVariable(LABEL_RETURN_VALUE + index, parameterizedType, assignmentExpression);
    }

    public static IntermediateVariable newObject(Runtime runtime, int index, ParameterizedType parameterizedType) {
        return new IntermediateVariable(LABEL_NEW_OBJECT + index, parameterizedType, runtime.newEmptyExpression());
    }

    // also used for switch expressions
    public static IntermediateVariable inlineCondition(Runtime runtime, int index, ParameterizedType parameterizedType) {
        return new IntermediateVariable(LABEL_INLINE_CONDITION + index, parameterizedType, runtime.newEmptyExpression());
    }

    @Override
    public boolean acceptForLinkedVariables() {
        return false;
    }
}
