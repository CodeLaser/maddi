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

import java.util.List;

/**
 * A sequence of expressions evaluated in order, the value being that of the last. Java has no comma
 * operator in general expression position (only in {@code for} clauses); this node is used by the
 * analyzer to represent such sequencing.
 */
public interface CommaExpression extends Expression {

    /**
     * @return the sub-expressions, in evaluation order; the last one supplies the value.
     */
    List<Expression> expressions();

    interface Builder extends Expression.Builder<Builder> {
        @Fluent
        Builder addExpression(Expression expression);

        @Fluent
        Builder addExpressions(List<Expression> expressions);

        CommaExpression build();
    }

    String NAME = "comma";

    @Override
    default String name() {
        return NAME;
    }
}
