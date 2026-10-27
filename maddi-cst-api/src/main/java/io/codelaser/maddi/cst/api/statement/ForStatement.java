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
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Expression;

import java.util.List;

/**
 * The classic {@code for (init; condition; update) body} loop. The condition is
 * {@link Statement#expression()} and the body is the primary {@link Statement#block()}; the init and
 * update clauses are {@link #initializers()} and {@link #updaters()}.
 */
public interface ForStatement extends LoopStatement {
    /**
     * @return the init clause elements. Each is either a {@link LocalVariableCreation} (when the loop
     * declares a variable) or an {@link Expression} (an expression statement); hence the common
     * {@link Element} supertype.
     */
    List<Element> initializers();

    /**
     * @return the update clause expressions, evaluated after each iteration.
     */
    List<Expression> updaters();

    /**
     * Return an immutable copy of this loop with its init clause replaced.
     *
     * @param elements the replacement initializers (see {@link #initializers()})
     * @return a new statement; this instance is unchanged
     */
    ForStatement withInitializers(List<Element> elements);

    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder addInitializer(Element initializer);

        @Fluent
        Builder setExpression(Expression expression);

        @Fluent
        Builder addUpdater(Expression expression);

        @Fluent
        Builder setBlock(Block block);

        ForStatement build();
    }

    String NAME = "for";

    @Override
    default String name() {
        return NAME;
    }
}
