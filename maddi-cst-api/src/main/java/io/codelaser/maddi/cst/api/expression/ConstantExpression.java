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

import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;

/**
 * A compile-time constant expression wrapping a value of type {@code T}. Implemented by the literal
 * expressions: {@code BooleanConstant}, the numeric constants (which also implement {@link Numeric}),
 * {@code CharConstant}, {@link StringConstant}, and {@code ClassExpression} ({@code X.class}).
 *
 * @param <T> the type of the wrapped constant value
 */
public interface ConstantExpression<T> extends Expression {

    @Override
    default boolean isConstant() {
        return true;
    }

    /**
     * @return the wrapped constant value.
     */
    T constant();

    /**
     * How a literal was written in the source, when that is not how its value prints by default: {@code 0xFFFF},
     * {@code 50_000}, {@code 0b1010}, a char written as a unicode escape, {@code 1e3}. Null otherwise, and for any
     * constant not read from a literal. The Java printer writes it, so that re-rendered code keeps its literals as
     * written (CodeLaser/maddi#49); a translator prints the value, since the spelling is Java's.
     */
    default String sourceSpelling() {
        return null;
    }

    /**
     * @return this constant, written as {@code sourceSpelling}; this instance when the constant does not keep a
     * spelling. The caller guarantees that the spelling denotes the constant's value.
     */
    default ConstantExpression<T> withSourceSpelling(String sourceSpelling) {
        return this;
    }

    /**
     * Constants are self-contained, so rewiring returns the same instance.
     */
    @Override
    default Expression rewire(InfoMapView infoMap) {
        return this;
    }
}
