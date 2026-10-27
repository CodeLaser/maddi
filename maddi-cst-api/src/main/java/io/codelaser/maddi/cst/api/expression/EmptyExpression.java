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
 * A placeholder for an absent expression, so that "no expression" can be represented without {@code null}
 * (for example a missing {@code switch} guard, the value of a {@code return;}, or a {@code default}
 * label). Always reports {@link #isEmpty()}; the several predicates and {@link #msg()} distinguish why it
 * is empty.
 */
public interface EmptyExpression extends Expression {

    /**
     * @return {@code true} when this stands for the {@code default} case label.
     */
    boolean isDefaultExpression();

    /**
     * @return {@code true} when this stands for the (absent) value of a {@code void} return.
     */
    boolean isNoReturnValue();

    /**
     * @return {@code true} when this stands for a genuinely absent expression.
     */
    boolean isNoExpression();

    @Override
    default boolean isEmpty() {
        return true;
    }

    /**
     * @return a short message describing which kind of emptiness this is.
     */
    String msg();

    String NAME = "emptyExpression";

    @Override
    default String name() {
        return NAME;
    }
}
