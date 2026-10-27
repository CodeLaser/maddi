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
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.variable.Variable;

/**
 * An assignment expression, {@code target = value}, including compound forms ({@code +=}, {@code *=}, …)
 * and the increment/decrement operators ({@code ++}, {@code --}). It is an expression because in Java an
 * assignment yields the assigned value.
 */
public interface Assignment extends Expression {
    /**
     * @return the assignment target, as a {@link VariableExpression}.
     */
    VariableExpression target();

    /**
     * @return the value being assigned.
     */
    Expression value();

    /**
     * @return the target variable (convenience for {@code target().variable()}).
     */
    Variable variableTarget();

    /**
     * @return the compound-assignment operator (the {@code +} of {@code +=}) as a {@link MethodInfo}, or
     * {@code null} for a plain {@code =}.
     */
    MethodInfo assignmentOperator();

    /**
     * @return for {@code ++}/{@code --}: {@code true} when prefix ({@code ++i}), {@code false} when
     * postfix ({@code i++}); {@code null} when this is not an increment/decrement.
     */
    Boolean prefixPrimitiveOperator();

    /**
     * @return {@code true} when the compound operator is {@code +=}.
     */
    boolean assignmentOperatorIsPlus();

    /**
     * @return the underlying binary operator used to compute the new value of a compound assignment.
     */
    MethodInfo binaryOperator();

    /**
     * @return an immutable copy with a different assigned value; this instance is unchanged.
     */
    Assignment withValue(Expression ve);

    interface Builder extends Element.Builder<Assignment.Builder> {
        @Fluent
        Builder setTarget(VariableExpression target);

        @Fluent
        Builder setValue(Expression value);

        @Fluent
        Builder setAssignmentOperator(MethodInfo assignmentOperator);

        @Fluent
        Builder setPrefixPrimitiveOperator(Boolean prefixPrimitiveOperator);

        @Fluent
        Builder setBinaryOperator(MethodInfo binaryOperator);

        @Fluent
        Builder setAssignmentOperatorIsPlus(boolean assignmentOperatorIsPlus);

        Assignment build();
    }

    String NAME = "assignment";

    @Override
    default String name() {
        return NAME;
    }
}
