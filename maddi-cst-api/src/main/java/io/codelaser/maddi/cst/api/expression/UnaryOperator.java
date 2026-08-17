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

import org.e2immu.language.cst.api.info.MethodInfo;

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
