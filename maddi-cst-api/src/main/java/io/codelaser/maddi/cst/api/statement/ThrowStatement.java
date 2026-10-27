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

package io.codelaser.maddi.cst.api.statement;

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Expression;

/**
 * The {@code throw expression;} statement; the thrown value is {@link Statement#expression()}. Always
 * escapes (see {@link Statement#alwaysEscapes()}).
 */
public interface ThrowStatement extends Statement {
    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder setExpression(Expression expression);

        ThrowStatement build();

    }

    String NAME = "throw";

    @Override
    default String name() {
        return NAME;
    }

    /**
     * @return an immutable copy of this statement with a different {@link Source}; this instance is
     * unchanged.
     */
    ThrowStatement withSource(Source newSource);
}
