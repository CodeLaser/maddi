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

import java.util.List;

/**
 * Boolean conjunction of two or more operands ({@code a && b && ...}). Modelled as a flat list rather
 * than a nested {@link BinaryOperator} so the analyzer can normalise and simplify boolean formulae.
 */
public interface And extends Expression {
    /**
     * @return the conjuncts, in canonical order.
     */
    List<Expression> expressions();

    interface Builder extends Element.Builder<Builder> {
        @Fluent
        Builder addExpressions(List<Expression> expressions);

        @Fluent
        Builder addExpression(Expression expression);

        And build();
    }

    String NAME = "and";

    @Override
    default String name() {
        return NAME;
    }
}
