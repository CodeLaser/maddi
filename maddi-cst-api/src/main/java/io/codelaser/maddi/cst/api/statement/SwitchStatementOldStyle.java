/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.codelaser.maddi.cst.api.statement;

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.RecordPattern;
import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DescendMode;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The colon-form {@code switch (selector) { case X: ...; default: ...; }} statement with fall-through.
 * The selector is {@link Statement#expression()} and the whole body is a single primary
 * {@link Statement#block()}; the {@code case}/{@code default} labels are kept separately as
 * {@link #switchLabels()}, each pointing (via {@link SwitchLabel#startFromPosition()}) at the statement
 * in that block where its code begins.
 *
 * <p>Contrast with {@link SwitchStatementNewStyle}, the arrow form, which has no fall-through and models
 * each arm as a self-contained {@link SwitchEntry}.
 */
public interface SwitchStatementOldStyle extends Statement {

    // selector == expression()

    /**
     * A single {@code case}/{@code default} label, anchored at a position within the switch body block.
     */
    interface SwitchLabel {
        SwitchLabel rewire(InfoMapView infoMap);

        /**
         * @return the index, within the switch body {@link Statement#block()}, of the first statement
         * guarded by this label.
         */
        int startFromPosition();

        /**
         * @return the case value, or an {@code EmptyExpression} for {@code default}.
         */
        Expression literal();

        /**
         * @return the record/type pattern of a pattern label (Java 21), or {@code null} when absent.
         */
        RecordPattern patternVariable();

        Stream<TypeReference> typesReferenced(Predicate<Element> predicate);

        Stream<Variable> variables(DescendMode descendMode);

        /**
         * @return the {@code when} guard expression (Java 21), or an {@code EmptyExpression} when absent.
         */
        Expression whenExpression();

        OutputBuilder print(Qualification qualification);

        SwitchLabel translate(TranslationMap translationMap);

        /**
         * @return an immutable copy of this label anchored at a different position; this instance is
         * unchanged.
         */
        SwitchLabel withStartPosition(int newStartPosition);
    }

    /**
     * Variant of {@link Statement#withBlocks(List)} that also replaces the switch labels.
     *
     * @return a new statement; this instance is unchanged
     */
    Statement withBlocks(List<Block> tSubBlocks, List<SwitchLabel> switchLabels);

    /**
     * @return the {@code case}/{@code default} labels, in source order.
     */
    List<SwitchLabel> switchLabels();

    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder setSelector(Expression selector);

        @Fluent
        Builder setBlock(Block block);

        @Fluent
        Builder addSwitchLabels(Collection<SwitchLabel> switchLabels);

        SwitchStatementOldStyle build();
    }

    String NAME = "switchOldStyle";

    @Override
    default String name() {
        return NAME;
    }

    /**
     * @return a map from statement index (within the body block) to the labels anchored there; a helper
     * used by analysis and by {@code print()}.
     */
    // helper method, useful for analysis; used by print()
    Map<String, List<SwitchLabel>> switchLabelMap();
}
