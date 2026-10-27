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

package io.codelaser.maddi.cst.api.expression;

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.util.OneVariable;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.List;

/**
 * A method invocation, {@code object.method(args)}. Operators are also modelled as method calls (see
 * {@link BinaryOperator}). It is a {@link io.codelaser.maddi.cst.api.expression.util.OneVariable} because
 * the call may stand for a single variable (a getter), which the analyzer exploits.
 */
public interface MethodCall extends Expression, OneVariable {
    /**
     * @return the invoked method.
     */
    MethodInfo methodInfo();

    /**
     * @return the receiver expression ({@code object} in {@code object.method(...)}); see also
     * {@link #objectIsImplicit()}.
     */
    Expression object();

    /**
     * @return the argument expressions, in order.
     */
    List<Expression> parameterExpressions();

    /**
     * @return analysis-time modification-time markers used to distinguish calls that observe different
     * states of mutable objects.
     */
    String modificationTimes();

    /**
     * @return {@code true} when the receiver was not written in source (an implicit {@code this} or the
     * enclosing type), so it should not be printed.
     */
    boolean objectIsImplicit();

    /**
     * @return the return type as instantiated at this call site (after type-argument inference).
     */
    ParameterizedType concreteReturnType();

    /**
     * @return the explicit type arguments ({@code obj.<String>method()}), empty when none.
     */
    List<ParameterizedType> typeArguments();

    /**
     * @return an immutable copy with a different receiver (and explicit implicitness flag).
     */
    MethodCall withObject(Expression object, boolean objectIsImplicit);

    /**
     * @return an immutable copy with a different receiver.
     */
    MethodCall withObject(Expression object);

    /**
     * @return an immutable copy with different argument expressions.
     */
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
