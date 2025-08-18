package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.util.OneVariable;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

public interface MethodCall extends Expression, OneVariable {
    MethodInfo methodInfo();

    Expression object();

    List<Expression> parameterExpressions();

    String modificationTimes();

    boolean objectIsImplicit();

    ParameterizedType concreteReturnType();

    List<ParameterizedType> typeArguments();

    MethodCall withObject(Expression object);

    // make a copy, with different parameters
    MethodCall withParameterExpressions(List<Expression> parameterExpressions);

    interface Builder extends Element.Builder<Builder> {
        MethodCall build();

        @Fluent
        Builder setObject(Expression object);

        @Fluent
        Builder setMethodInfo(MethodInfo methodInfo);

        @Fluent
        Builder setModificationTimes(String modificationTimes);

        @Fluent
        Builder setParameterExpressions(List<Expression> expressions);

        @Fluent
        Builder setTypeArguments(List<ParameterizedType> typeArguments);

        @Fluent
        Builder setObjectIsImplicit(boolean objectIsImplicit);

        @Fluent
        Builder setConcreteReturnType(ParameterizedType returnType);

    }

    String NAME = "methodCall";

    @Override
    default String name() {
        return NAME;
    }
}
