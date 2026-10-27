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

/**
 * Mixin for expressions that carry a numeric value, implemented by the numeric constants
 * ({@code ByteConstant}, {@code ShortConstant}, {@code IntConstant}, {@code LongConstant},
 * {@code FloatConstant}, {@code DoubleConstant}). Provides the value in boxed ({@link #number()}) and
 * {@code double} ({@link #doubleValue()}) form and the two numeric inversions.
 */
public interface Numeric extends Expression {
    /**
     * @return the value as a boxed {@link Number} of the matching primitive type.
     */
    Number number();

    /**
     * @return the value widened to {@code double} (lossy for large {@code long} values).
     */
    double doubleValue();

    /**
     * @return the arithmetic negation ({@code -x}) of this value.
     */
    Expression negate();

    /**
     * @return the bitwise complement ({@code ~x}) of this value.
     */
    Expression bitwiseNegation();

    @Override
    default boolean isNegatedOrNumericNegative() {
        return doubleValue() < 0;
    }

    @Override
    default boolean isNumeric() {
        return true;
    }

    @Override
    default Double numericValue() {
        return doubleValue();
    }
}
