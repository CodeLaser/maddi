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

import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;

/**
 * An expression in the common syntax tree.
 *
 * <p>Every expression is an {@link Element} (carrying a {@link Source source}, comments and annotations)
 * and additionally has a static {@link #parameterizedType() type}. Expressions are {@link Comparable},
 * which the analyzer uses to keep symbolic values in a canonical form. See the
 * {@code org.e2immu.language.cst.api.expression} package documentation for the shared families
 * (constants, operators, wrappers) and conventions.
 */
public interface Expression extends Comparable<Expression>, Element {

    /**
     * @return a short kind tag (concrete expressions return their {@code NAME} constant); {@code "?"}
     * when unset.
     */
    default String name() {
        return "?";
    }

    /**
     * @return the static type of this expression.
     */
    ParameterizedType parameterizedType();

    /**
     * @return the operator precedence used when printing, so sub-expressions are parenthesised correctly.
     */
    Precedence precedence();

    /**
     * @return a rank used to order expressions of <em>different</em> kinds in the canonical form
     * ("external" comparison). Ties between same-kind expressions are broken by
     * {@link #internalCompareTo(Expression)}.
     */
    int order();

    /**
     * @return the comparison between this and {@code expression}, which are of the <em>same</em> kind
     * ("internal" comparison); see {@link #order()}.
     */
    int internalCompareTo(Expression expression);

    // convenience methods

    default boolean isBoolValueTrue() {
        return false;
    }

    default boolean isBoolValueFalse() {
        return false;
    }

    default boolean isBooleanConstant() {
        return false;
    }

    default boolean isNullConstant() {
        return false;
    }

    default boolean isEmpty() {
        return false;
    }

    default boolean isConstant() {
        return false;
    }

    default boolean isNegatedOrNumericNegative() {
        return false;
    }

    /**
     * @return the numeric value as a {@link Double} when this expression is a numeric constant, otherwise
     * {@code null}.
     */
    default Double numericValue() {
        return null;
    }

    default boolean isNumeric() {
        return parameterizedType().isNumeric();
    }

    /**
     * @return the condition when this expression is an inline conditional ({@code c ? a : b}), otherwise
     * {@code null}.
     */
    default Expression conditionOfInlineConditional() {
        return null;
    }

    /**
     * Source-to-source rewrite under the given map; returns a single expression (contrast with
     * {@link org.e2immu.language.cst.api.statement.Statement#translate}, which may expand to several).
     */
    Expression translate(TranslationMap translationMap);

    /**
     * Clone this expression into a new {@code Info} graph, relinking references through the map.
     */
    Expression rewire(InfoMap infoMap);

    /**
     * @return an immutable copy of this expression with a different {@link Source}; this instance is
     * unchanged.
     */
    Expression withSource(Source source);
}
