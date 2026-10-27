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

import io.codelaser.maddi.cst.api.type.Diamond;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

/**
 * A bare type used in expression position — the type-name qualifier of a static member access, e.g. the
 * {@code Integer} in {@code Integer.MAX_VALUE} or {@code Math} in {@code Math.max(...)}. Contrast with
 * {@link ClassExpression} ({@code X.class}).
 */
public interface TypeExpression extends Expression {
    /**
     * @return the referenced type.
     */
    ParameterizedType parameterizedType();

    String NAME = "typeExpression";

    @Override
    default String name() {
        return NAME;
    }


    interface Builder extends Expression.Builder<Builder> {
        Builder setParameterizedType(ParameterizedType parameterizedType);

        Builder setDiamond(Diamond diamond);

        TypeExpression build();
    }
}
