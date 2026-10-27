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


import io.codelaser.maddi.cst.api.runtime.Runtime;

/**
 * Addition, {@code lhs + rhs} (numeric). In the analyzer's canonical form a sum is normalised so its
 * left-hand side holds the constant/numeric part; the helpers below expose that split.
 */
public interface Sum extends BinaryOperator {

    /**
     * @return the numeric (constant) part of the left-hand side, or {@code null} when there is none.
     */
    Double numericPartOfLhs();

    /**
     * @return the left-hand side with its numeric part removed (the symbolic remainder).
     */
    Expression nonNumericPartOfLhs(Runtime runtime);

    /**
     * @return the expression {@code this == 0}, useful for turning a sum into a (in)equality condition.
     */
    Expression isZero(Runtime runtime);

    String NAME = "sum";

    @Override
    default String name() {
        return NAME;
    }
}
