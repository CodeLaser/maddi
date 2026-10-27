/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.api.statement;

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.MethodInfo;

import java.util.List;

/**
 * An explicit constructor invocation as the first statement of a constructor body: {@code this(...)} or
 * {@code super(...)}. {@link #isSuper()} distinguishes the two, {@link #methodInfo()} is the invoked
 * constructor, and {@link #parameterExpressions()} are its arguments.
 */
public interface ExplicitConstructorInvocation extends Statement {

    /**
     * @return {@code true} for {@code super(...)}, {@code false} for {@code this(...)}.
     */
    boolean isSuper();

    /**
     * @return the constructor being invoked.
     */
    MethodInfo methodInfo();

    /**
     * @return the argument expressions passed to the constructor, in order.
     */
    List<Expression> parameterExpressions();

    /**
     * @return an immutable copy of this statement with a different {@link Source}; this instance is
     * unchanged.
     */
    ExplicitConstructorInvocation withSource(Source newSource);

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setIsSuper(boolean isSuper);

        @Fluent
        Builder setSynthetic(boolean synthetic);

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
