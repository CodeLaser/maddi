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
import io.codelaser.maddi.cst.api.expression.Expression;

/**
 * The enhanced {@code for (var item : iterable) body} loop. The loop variable is declared by
 * {@link #initializer()}, the iterated expression is {@link Statement#expression()}, and the body is the
 * primary {@link Statement#block()}.
 */
public interface ForEachStatement extends LoopStatement {
    /**
     * @return the declaration of the loop variable (the {@code var item} part).
     */
    LocalVariableCreation initializer();

    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder setInitializer(LocalVariableCreation localVariableCreation);

        @Fluent
        Builder setExpression(Expression expression);

        @Fluent
        Builder setBlock(Block block);

        ForEachStatement build();
    }

    String NAME = "forEach";

    @Override
    default String name() {
        return NAME;
    }
}
