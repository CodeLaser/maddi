/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.api.expression;

import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

/**
 * An expression in the common syntax tree.
 *
 * <p>Every expression is an {@link Element} (carrying a {@link Source source}, comments and annotations)
 * and additionally has a static {@link #parameterizedType() type}. Expressions are {@link Comparable},
 * which the analyzer uses to keep symbolic values in a canonical form. See the
 * {@code io.codelaser.maddi.cst.api.expression} package documentation for the shared families
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
     * {@link io.codelaser.maddi.cst.api.statement.Statement#translate}, which may expand to several).
     * The map is a read-only lookup: it receives the receiver as a KEY (hidden content), never as
     * mutable state to link -- the {@code @Independent(hc = true)} contract on the parameter.
     */
    Expression translate(@Independent(hc = true) TranslationMap translationMap);

    /**
     * Clone this expression into a new {@code Info} graph, relinking references through the map.
     */
    Expression rewire(@Independent(hc = true) InfoMapView infoMap);

    /**
     * @return an immutable copy of this expression with a different {@link Source}; this instance is
     * unchanged.
     */
    Expression withSource(Source source);
}
