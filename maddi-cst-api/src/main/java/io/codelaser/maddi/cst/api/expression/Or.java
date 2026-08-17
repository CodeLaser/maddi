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
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;

/**
 * Boolean disjunction of two or more operands ({@code a || b || ...}). Like {@link And}, a flat list so
 * boolean formulae can be normalised and simplified.
 */
public interface Or extends Expression {
    /**
     * @return the disjuncts, in canonical order.
     */
    List<Expression> expressions();

    interface Builder extends Element.Builder<Builder> {
        @Fluent
        Builder addExpressions(List<Expression> expressions);

        @Fluent
        Builder addExpression(Expression expression);

        Or build();
    }

    String NAME = "or";

    @Override
    default String name() {
        return NAME;
    }
}
