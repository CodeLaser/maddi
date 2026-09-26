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

package io.codelaser.maddi.cst.api.statement;

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Expression;

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

    /**
     * @return how many enclosing LAMBDAS this {@code return} leaves before it returns: {@code 0}, the only value
     * Java can express, returns from the innermost method or lambda body that contains it. A Kotlin lambda passed
     * to an {@code inline} function can return from an enclosing function: {@code xs.forEach { if (p(it)) return
     * it }} leaves the lambda AND returns from the enclosing function, {@code exitLevels() == 1}; nested
     * lambdas count one level each, and {@code return@outer} from an inner lambda returns from the outer one.
     * The method that is returned from is the {@code exitLevels()}-th {@link
     * io.codelaser.maddi.cst.api.info.TypeInfo#enclosingMethod()} of the lambda that contains this statement.
     * A count rather than a reference, so that it survives translation and rewiring of the enclosing method.
     */
    int exitLevels();

    /**
     * @return {@code true} when this statement returns from a method other than the one whose body contains it;
     * see {@link #exitLevels()}.
     */
    default boolean isNonLocal() {
        return exitLevels() > 0;
    }

    /**
     * @return the label of a Kotlin qualified return ({@code forEach} in {@code return@forEach}), or {@code null};
     * informative, for printing: {@link #exitLevels()} alone decides where the return goes. Distinct from
     * {@link Statement#label()}, which is the label attached <em>to</em> this statement.
     */
    String goToLabel();

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setExpression(Expression expression);

        @Fluent
        Builder setExitLevels(int exitLevels);

        @Fluent
        Builder setGoToLabel(String goToLabel);

        ReturnStatement build();
    }

    String NAME = "return";

    @Override
    default String name() {
        return NAME;
    }
}
