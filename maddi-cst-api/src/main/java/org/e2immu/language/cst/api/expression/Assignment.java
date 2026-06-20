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
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.variable.Variable;

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
