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
