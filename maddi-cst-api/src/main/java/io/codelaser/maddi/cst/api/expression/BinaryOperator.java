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

/**
 * An infix binary operation {@code lhs op rhs}. The operator itself is identified by a
 * {@link MethodInfo} (so operators are modelled like methods on their operand types). Implemented by
 * {@code Sum}, {@code Product}, {@code Divide}, {@code Remainder}, {@code Equals} and
 * {@code StringConcat}; note that boolean {@code &&}/{@code ||} are modelled separately as {@code And}
 * and {@code Or}.
 */
public interface BinaryOperator extends Expression {

    /**
     * @return the left-hand operand.
     */
    Expression lhs();

    /**
     * @return the right-hand operand.
     */
    Expression rhs();

    /**
     * @return the operator, represented as a {@link MethodInfo}.
     */
    MethodInfo operator();

    Precedence precedence();

    interface Builder extends Expression.Builder<Builder> {
        @Fluent
        Builder setLhs(Expression lhs);

        @Fluent
        Builder setRhs(Expression rhs);

        @Fluent
        Builder setOperator(MethodInfo operator);

        @Fluent
        Builder setPrecedence(Precedence precedence);

        @Fluent
        Builder setParameterizedType(ParameterizedType parameterizedType);

        BinaryOperator build();
    }

    String NAME = "binaryOperator";

    @Override
    default String name() {
        return NAME;
    }
}
