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

/**
 * A placeholder for an absent expression, so that "no expression" can be represented without {@code null}
 * (for example a missing {@code switch} guard, the value of a {@code return;}, or a {@code default}
 * label). Always reports {@link #isEmpty()}; the several predicates and {@link #msg()} distinguish why it
 * is empty.
 */
public interface EmptyExpression extends Expression {

    /**
     * @return {@code true} when this stands for the {@code default} case label.
     */
    boolean isDefaultExpression();

    /**
     * @return {@code true} when this stands for the (absent) value of a {@code void} return.
     */
    boolean isNoReturnValue();

    /**
     * @return {@code true} when this stands for a genuinely absent expression.
     */
    boolean isNoExpression();

    @Override
    default boolean isEmpty() {
        return true;
    }

    /**
     * @return a short message describing which kind of emptiness this is.
     */
    String msg();

    String NAME = "emptyExpression";

    @Override
    default String name() {
        return NAME;
    }
}
