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
