package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.RecordPattern;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;

public interface InstanceOf extends Expression {
    Expression expression();

    RecordPattern patternVariable();

    ParameterizedType testType();

    interface Builder extends Element.Builder<Builder> {
        @Fluent
        Builder setExpression(Expression expression);

        @Fluent
        Builder setPatternVariable(RecordPattern patternVariable);

        @Fluent
        Builder setTestType(ParameterizedType testType);

        InstanceOf build();
    }

    String NAME = "instanceOf";

    @Override
    default String name() {
        return NAME;
    }
}
