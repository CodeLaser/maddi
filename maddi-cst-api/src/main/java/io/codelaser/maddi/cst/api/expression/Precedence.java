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
 * Operator precedence, used by {@link Expression#precedence()} to decide when a sub-expression must be
 * parenthesised while printing. A higher {@link #value()} binds more tightly.
 */
public interface Precedence {
    /**
     * @return the precedence level; higher binds more tightly.
     */
    int value();

    /**
     * @return {@code true} when this precedence binds more tightly than {@code other}.
     */
    default boolean greaterThan(Precedence other) {
        return value() > other.value();
    }
}
