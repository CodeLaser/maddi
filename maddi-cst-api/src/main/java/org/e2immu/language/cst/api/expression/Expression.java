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

public interface Expression extends Comparable<Expression>, Element {

    default String name() {
        return "?";
    }

    ParameterizedType parameterizedType();

    Precedence precedence();

    // "external": helps to compare expressions of different types
    int order();

    // "internal" as: how do two expressions of the same type compare to each other?
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

    default Double numericValue() {
        return null;
    }

    default boolean isNumeric() {
        return parameterizedType().isNumeric();
    }

    default Expression conditionOfInlineConditional() {
        return null;
    }

    Expression translate(TranslationMap translationMap);

    Expression rewire(InfoMap infoMap);

    Expression withSource(Source source);
}
