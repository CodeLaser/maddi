package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

public interface ArrayInitializer extends Expression {
    List<Expression> expressions();

    String NAME = "arrayInitializer";

    @Override
    default String name() {
        return NAME;
    }

    interface Builder extends Element.Builder<Builder> {
        @Fluent
        Builder setExpressions(List<Expression> expressions);

        @Fluent
        Builder setCommonType(ParameterizedType commonType);

        ArrayInitializer build();
    }
}
