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

import io.codelaser.maddi.cst.api.element.Element;

/**
 * The array {@code .length} access, {@code scope.length}. Modelled as its own node (rather than a field
 * access) because {@code length} is not a real field. The result type is {@code int}.
 */
public interface ArrayLength extends Expression {
    /**
     * @return the array expression whose length is read.
     */
    Expression scope();

    interface Builder extends Element.Builder<Builder> {
        Builder setExpression(Expression e);

        ArrayLength build();
    }

    String NAME = "arrayLength";

    @Override
    default String name() {
        return NAME;
    }
}
