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

import io.codelaser.maddi.cst.api.info.MethodInfo;

/**
 * A prefix unary operation {@code op expression}. The operator is identified by a {@link MethodInfo}.
 * Implemented by {@code Negation} ({@code -x}/{@code !x}) and {@code BitwiseNegation} ({@code ~x}); both
 * also implement {@link ExpressionWrapper} since they wrap a single operand.
 */
public interface UnaryOperator extends Expression {
    /**
     * @return the single operand.
     */
    Expression expression();

    /**
     * @return the operator, represented as a {@link MethodInfo}.
     */
    MethodInfo operator();

    Precedence precedence();

    String NAME = "unaryOperator";

    @Override
    default String name() {
        return NAME;
    }
}
