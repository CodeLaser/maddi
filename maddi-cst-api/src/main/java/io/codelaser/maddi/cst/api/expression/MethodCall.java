/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.util.OneVariable;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

/**
 * A method invocation, {@code object.method(args)}. Operators are also modelled as method calls (see
 * {@link BinaryOperator}). It is a {@link org.e2immu.language.cst.api.expression.util.OneVariable} because
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
