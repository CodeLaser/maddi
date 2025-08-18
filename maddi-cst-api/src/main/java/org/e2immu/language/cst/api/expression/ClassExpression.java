package org.e2immu.language.cst.api.expression;


import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.type.ParameterizedType;

public interface ClassExpression extends ConstantExpression<ParameterizedType> {

    // String.class -> String
    ParameterizedType type();

    String NAME = "classLiteral";

    @Override
    default String name() {
        return NAME;
    }

    interface Builder extends Element.Builder<Builder> {
        @Fluent
        Builder setParameterizedType(ParameterizedType type);

        @Fluent
        Builder setClassType(ParameterizedType classType);

        ClassExpression build();
    }
}
