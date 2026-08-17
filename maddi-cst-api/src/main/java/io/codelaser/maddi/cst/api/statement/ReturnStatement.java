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

package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;

/**
 * The {@code return;} or {@code return expression;} statement. The returned value, when present, is
 * {@link Statement#expression()}; a bare {@code return;} has no value, in which case {@link #expression()} is
 * {@code null} or an {@link Expression#isEmpty() empty} expression depending on the producer — so prefer
 * {@link #hasNoValue()} to test for it. Always escapes (see {@link Statement#alwaysEscapes()}).
 */
public interface ReturnStatement extends Statement {

    /**
     * @return {@code true} when this is a bare {@code return;} with no returned value, i.e. {@link #expression()}
     * is absent ({@code null}) or {@link Expression#isEmpty() empty}. The null-safe, standard way to distinguish
     * {@code return;} from {@code return expression;}.
     */
    default boolean hasNoValue() {
        Expression e = expression();
        return e == null || e.isEmpty();
    }

    /**
     * @return an immutable copy of this statement with a different {@link Source}; this instance is
     * unchanged.
     */
    ReturnStatement withSource(Source newSource);

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setExpression(Expression expression);

        ReturnStatement build();
    }

    String NAME = "return";

    @Override
    default String name() {
        return NAME;
    }
}
