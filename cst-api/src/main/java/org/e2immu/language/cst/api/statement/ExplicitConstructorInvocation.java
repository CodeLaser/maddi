package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.MethodInfo;

import java.util.List;

public interface ExplicitConstructorInvocation extends Statement {

    boolean isSuper();

    MethodInfo methodInfo();

    List<Expression> parameterExpressions();

    ExplicitConstructorInvocation withSource(Source newSource);

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setIsSuper(boolean isSuper);

        @Fluent
        Builder setMethodInfo(MethodInfo methodInfo);

        @Fluent
        Builder setParameterExpressions(List<Expression> parameterExpressions);

        ExplicitConstructorInvocation build();
    }

    String NAME = "explicitConstructorInvocation";

    @Override
    default String name() {
        return NAME;
    }
}
