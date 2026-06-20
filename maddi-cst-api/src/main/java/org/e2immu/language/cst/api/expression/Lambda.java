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
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.output.Qualification;
import org.e2immu.language.cst.api.statement.Block;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

/**
 * A lambda expression, {@code (params) -> body}. It is represented through a synthetic
 * {@link MethodInfo}: the lambda's parameters and body are that method's parameters and body, and the
 * lambda's type is the functional interface it implements. The many default methods are conveniences over
 * that synthetic method.
 */
public interface Lambda extends Expression {
    /**
     * @return the synthetic method holding the lambda's parameters and body.
     */
    MethodInfo methodInfo();

    /**
     * @return the lambda body as a {@link Block} (single-expression bodies are wrapped in a block).
     */
    default Block methodBody() {
        return methodInfo().methodBody();
    }

    /**
     * @return the lambda's parameters.
     */
    default List<ParameterInfo> parameters() {
        return methodInfo().parameters();
    }

    /**
     * @return the functional interface type, e.g. {@code Function} in {@code Function<A,B>}.
     */
    default TypeInfo abstractFunctionalTypeInfo() {
        return concreteFunctionalType().typeInfo();
    }

    /**
     * @return the functional interface as instantiated here, e.g. {@code Function<A,B>}.
     */
    default ParameterizedType concreteFunctionalType() {
        return methodInfo().typeInfo().interfacesImplemented().get(0);
    }

    default ParameterizedType parameterizedType() {
        return implementation();
    }

    /**
     * @return the synthetic implementing type (the anonymous class that the synthetic method belongs to).
     */
    default ParameterizedType implementation() {
        return methodInfo().typeInfo().asSimpleParameterizedType();
    }

    /**
     * @return the lambda's return type (the body's result type).
     */
    default ParameterizedType concreteReturnType() {
        return methodInfo().returnType();
    }

    /**
     * @return per-parameter print styles (see {@link OutputVariant}); controls how each parameter is
     * rendered: implicit, explicitly typed, or {@code var}.
     */
    List<OutputVariant> outputVariants();

    /**
     * @return the body expression when the lambda is of the {@code x -> expr} single-expression form,
     * otherwise {@code null}.
     */
    Expression singleExpression();

    /**
     * @return an immutable copy backed by a different synthetic method (and hence body).
     */
    Lambda withMethodInfoAndMethodBody(MethodInfo methodInfo);

    /**
     * How one lambda parameter is rendered on output: nothing, an explicit type, or {@code var}.
     */
    interface OutputVariant {
        boolean isEmpty();

        boolean isTyped();

        boolean isVar();

        OutputBuilder print(ParameterInfo parameterInfo, Qualification qualification);
    }

    interface Builder extends Expression.Builder<Builder> {

        @Fluent
        Builder setMethodInfo(MethodInfo methodInfo);

        @Fluent
        Builder setOutputVariants(List<OutputVariant> outputVariants);

        Lambda build();
    }

    String NAME = "lambda";

    @Override
    default String name() {
        return NAME;
    }
}
