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

import java.util.Collection;
import java.util.List;

/**
 * The arrow-form {@code switch (selector) { case ... -> ...; }} statement (Java 14+), modelled as a list
 * of {@link SwitchEntry entries}. The selector is {@link Statement#expression()}; this statement has no
 * primary {@link Statement#block()} (each entry carries its own block, surfaced through
 * {@link Statement#otherBlocksStream()}).
 *
 * <p>Contrast with {@link SwitchStatementOldStyle}, which models the colon form with fall-through using
 * a single block and a list of {@code SwitchLabel}s.
 */
public interface SwitchStatementNewStyle extends Statement {

    // selector == expression()
    // block = null
    // otherBlocks = each of the individual blocks?

    /**
     * @return the entries (the {@code case ... -> ...} arms), in source order.
     */
    List<SwitchEntry> entries();

    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder addSwitchEntry(SwitchEntry switchEntry);

        @Fluent
        Builder setSelector(Expression selector);

        @Fluent
        Builder addSwitchEntries(Collection<SwitchEntry> switchEntries);

        SwitchStatementNewStyle build();
    }

    String NAME = "switchNewStyle";

    @Override
    default String name() {
        return NAME;
    }

}
