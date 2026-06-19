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
 * An expression used as a statement, for example {@code foo();} or {@code i++;}. The wrapped expression
 * is {@link Statement#expression()}.
 */
public interface ExpressionAsStatement extends Statement {

    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder setExpression(Expression expression);
        ExpressionAsStatement build();

    }
    String NAME = "expressionAsStatement";

    @Override
    default String name() {
        return NAME;
    }

    /**
     * @return an immutable copy of this statement with a different {@link Source}; this instance is
     * unchanged.
     */
    ExpressionAsStatement withSource(Source newSource);
}
