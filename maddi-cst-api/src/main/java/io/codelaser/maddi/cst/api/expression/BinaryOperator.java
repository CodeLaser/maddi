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
import org.e2immu.language.cst.api.type.ParameterizedType;

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
