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

import org.e2immu.language.cst.api.runtime.Runtime;

/**
 * The canonical form for all numeric inequalities: {@code expression >= 0} (or {@code > 0}). Every
 * {@code <}, {@code <=}, {@code >}, {@code >=} comparison is normalised into this single shape so the
 * analyzer reasons about inequalities uniformly.
 */
public interface GreaterThanZero extends Expression {
    /**
     * @return the expression compared against zero.
     */
    Expression expression();

    /**
     * @return {@code true} for {@code >= 0} (allows equality), {@code false} for strict {@code > 0}.
     */
    boolean allowEquals();

    /**
     * Decompose into the form {@code x < b} / {@code x > b}: split {@link #expression()} into a variable
     * part {@code x} and a numeric bound {@code b}.
     *
     * @return the decomposition (see {@link XB})
     */
    XB extract(Runtime runtime);

    /**
     * Result of {@link #extract(Runtime)}: the inequality rewritten as {@code x < b} or {@code x > b}.
     */
    interface XB {
        /** @return the variable (non-constant) part. */
        Expression x();

        /** @return the numeric bound. */
        double b();

        /** @return {@code true} when the relation is {@code x < b}, {@code false} when {@code x > b}. */
        boolean lessThan();
    }

    String NAME = "greaterThanZero";

    @Override
    default String name() {
        return NAME;
    }
}
