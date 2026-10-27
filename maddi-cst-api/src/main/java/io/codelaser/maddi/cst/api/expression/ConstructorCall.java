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
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.Diamond;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.List;

/**
 * An object or array creation: {@code new Type(args)}, {@code new Type[n]}/{@code new Type[]{...}}, an
 * anonymous class instantiation {@code new Type(){ ... }}, or a qualified {@code outer.new Inner(...)}.
 * The created type is the overall {@link Expression#parameterizedType()}.
 */
public interface ConstructorCall extends Expression {
    /**
     * @return the invoked constructor, or {@code null} for an array creation.
     */
    MethodInfo constructor();

    /**
     * @return the qualifying instance for an inner-class creation ({@code outer.new Inner()}), otherwise
     * {@code null}.
     */
    Expression object();

    /**
     * @return the constructor arguments, in order.
     */
    List<Expression> parameterExpressions();

    /**
     * @return explicit type arguments, empty when none.
     */
    List<ParameterizedType> typeArguments();

    /**
     * @return the anonymous class body for {@code new Type(){ ... }}, otherwise {@code null}.
     */
    TypeInfo anonymousClass();

    /**
     * @return the brace initialiser for an array creation ({@code new int[]{1,2}}), otherwise
     * {@code null}.
     */
    ArrayInitializer arrayInitializer();

    /**
     * @return an immutable copy with a different anonymous-class body.
     */
    ConstructorCall withAnonymousClass(TypeInfo newAnonymous);

    /**
     * @return an immutable copy with different constructor arguments.
     */
    ConstructorCall withParameterExpressions(List<Expression> newParameterExpressions);

    /**
     * @return how the generic type arguments are written: explicit, diamond {@code <>}, or absent.
     */
    Diamond diamond();

    interface Builder extends Expression.Builder<Builder> {

        ConstructorCall build();

        @Fluent
        Builder setObject(Expression object);

        @Fluent
        Builder setDiamond(Diamond diamond);

        @Fluent
        Builder setConstructor(MethodInfo methodInfo);

        @Fluent
        Builder setAnonymousClass(TypeInfo anonymousClass);

        @Fluent
        Builder setArrayInitializer(ArrayInitializer arrayInitializer);

        @Fluent
        Builder setParameterExpressions(List<Expression> expressions);

        @Fluent
        Builder setConcreteReturnType(ParameterizedType returnType);

        @Fluent
        Builder setTypeArguments(List<ParameterizedType> typeArguments);
    }

    String NAME = "constructorCall";

    @Override
    default String name() {
        return NAME;
    }
}
