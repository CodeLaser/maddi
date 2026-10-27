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
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.List;

/**
 * A method reference, {@code scope::method} (including constructor references {@code Type::new}). Its
 * type is the functional interface it targets; the concrete parameter/return types record how that
 * interface's type parameters are instantiated here.
 */
public interface MethodReference extends Expression {
    /**
     * @return the qualifier before {@code ::} (a type or an instance expression).
     */
    Expression scope();

    /**
     * @return the referenced method (or constructor).
     */
    MethodInfo methodInfo();

    /**
     * @return the parameter types of the targeted functional-interface method, as instantiated here.
     */
    List<ParameterizedType> concreteParameterTypes();

    /**
     * @return the return type of the targeted functional-interface method, as instantiated here.
     */
    ParameterizedType concreteReturnType();

    interface Builder extends Expression.Builder<Builder> {
        @Fluent
        Builder setScope(Expression expression);

        @Fluent
        Builder setMethod(MethodInfo method);

        @Fluent
        Builder setConcreteFunctionalType(ParameterizedType parameterizedType);

        Builder setConcreteParameterTypes(List<ParameterizedType> concreteParameterTypes);

        Builder setConcreteReturnType(ParameterizedType concreteReturnType);

        MethodReference build();
    }

    String NAME = "methodReference";

    @Override
    default String name() {
        return NAME;
    }
}
