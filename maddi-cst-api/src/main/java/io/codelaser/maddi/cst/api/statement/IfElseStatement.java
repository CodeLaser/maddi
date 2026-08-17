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
import org.e2immu.language.cst.api.expression.Expression;

/**
 * The {@code if (condition) ... else ...} statement. The condition is {@link Statement#expression()}, the
 * {@code if} branch is the primary {@link Statement#block()}, and the {@code else} branch is
 * {@link #elseBlock()} (which may be empty when there is no {@code else}).
 */
public interface IfElseStatement extends Statement {

    /**
     * @return the {@code else} branch; an empty block when the statement has no {@code else}.
     */
    Block elseBlock();

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setExpression(Expression expression);

        @Fluent
        Builder setIfBlock(Block ifBlock);

        @Fluent
        Builder setElseBlock(Block ifBlock);

        IfElseStatement build();
    }

    String NAME = "ifElse";

    @Override
    default String name() {
        return NAME;
    }
}
