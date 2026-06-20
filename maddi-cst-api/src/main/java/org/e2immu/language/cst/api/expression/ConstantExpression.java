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

import org.e2immu.language.cst.api.info.InfoMap;

/**
 * A compile-time constant expression wrapping a value of type {@code T}. Implemented by the literal
 * expressions: {@code BooleanConstant}, the numeric constants (which also implement {@link Numeric}),
 * {@code CharConstant}, {@link StringConstant}, and {@code ClassExpression} ({@code X.class}).
 *
 * @param <T> the type of the wrapped constant value
 */
public interface ConstantExpression<T> extends Expression {

    @Override
    default boolean isConstant() {
        return true;
    }

    /**
     * @return the wrapped constant value.
     */
    T constant();

    /**
     * Constants are self-contained, so rewiring returns the same instance.
     */
    @Override
    default Expression rewire(InfoMap infoMap) {
        return this;
    }
}
