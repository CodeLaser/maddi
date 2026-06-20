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

package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.List;
import java.util.stream.Stream;

/**
 * A statement in the common syntax tree (CST).
 *
 * <p>Every statement is an {@link Element} (so it has a {@link org.e2immu.language.cst.api.element.Source
 * source}, comments and annotations) and exposes its nested blocks through a uniform block model. See the
 * {@code org.e2immu.language.cst.api.statement} package documentation for the conventions shared by all
 * statements (the {@code NAME}/{@link #name()} tag, the block model, builders, {@code withX(...)} copies,
 * and the {@link #rewire}/{@link #translate} lifecycles).
 *
 * <p>Concrete kinds either implement this interface directly or one of the two grouping interfaces
 * {@link LoopStatement} and {@link BreakOrContinueStatement}.
 */
public interface Statement extends Element {

    /**
     * @return {@code true} if this statement contains at least one nested {@link Block}; equivalent to
     * {@link #subBlockStream()} being non-empty.
     */
    boolean hasSubBlocks();

    /**
     * @return {@code true} if this statement was inserted by the analyzer rather than written in source.
     * Defaults to {@code false}.
     */
    default boolean isSynthetic() {
        return false;
    }

    /**
     * @return the label attached to this statement (the {@code L:} in {@code L: while(...)}), or
     * {@code null} when unlabelled. This is the statement's own label; for the target of a jump see
     * {@link BreakOrContinueStatement#goToLabel()}.
     */
    String label();

    /**
     * @return the <em>primary</em> nested block of this statement (for example a loop body, the
     * {@code try} block, or the {@code if} branch), or {@code null} when the statement has no primary
     * block. Additional blocks are returned by {@link #otherBlocksStream()}.
     */
    default Block block() {
        return null;
    }

    /**
     * @return a short, stable identifier for the kind of statement (for example {@code "while"} or
     * {@code "ifElse"}); concrete statements return their {@code NAME} constant.
     */
    String name();

    /**
     * @return every nested block <em>other</em> than {@link #block()}, in source order (for example an
     * {@code else} branch, or {@code catch}/{@code finally} blocks). Empty by default.
     */
    @NotNull
    default Stream<Block> otherBlocksStream() {
        return Stream.of();
    }

    /**
     * @return the expression most closely associated with this statement, or {@code null} when there is
     * none. When a statement carries several expressions this is the first/principal one (for example a
     * loop or {@code switch} selector, an {@code if} condition, or a {@code return} value).
     */
    default Expression expression() {
        return null;
    }

    /**
     * @return all nested blocks, namely {@link #block()} (when present) followed by
     * {@link #otherBlocksStream()}, <em>including empty blocks</em>.
     */
    @NotNull
    default Stream<Block> subBlockStream() {
        return Stream.concat(Stream.ofNullable(block()), otherBlocksStream());
    }

    /**
     * @return {@link #subBlockStream()} as an {@link Iterable}, for use in for-each loops.
     */
    default Iterable<Block> subBlocks() {
        return () -> subBlockStream().iterator();
    }

    /**
     * Return an immutable copy of this statement with its nested blocks replaced. The supplied list must
     * match this statement's block structure (the elements of {@link #subBlockStream()}), in order.
     *
     * @param tSubBlocks the replacement blocks
     * @return a new statement; this instance is unchanged
     */
    Statement withBlocks(List<Block> tSubBlocks);

    /**
     * Return an immutable copy of this statement with a different {@link Source}; this instance is
     * unchanged. Every concrete statement supports this; some narrow the return type covariantly.
     *
     * @param source the replacement source
     * @return a new statement with the given source
     */
    Statement withSource(Source source);

    /**
     * Fluent builder shared by all statement builders. {@code build()} is narrowed covariantly by each
     * concrete statement to return that statement's type.
     *
     * @param <B> the concrete builder type, for fluent chaining
     */
    interface Builder<B extends Builder<?>> extends Element.Builder<B> {
        @Fluent
        B setLabel(String label);

        Statement build();
    }

    /**
     * Source-to-source rewrite of this statement under the given map. A single statement may expand to
     * zero, one, or several statements, hence the list return type.
     *
     * @param translationMap the substitutions to apply
     * @return the translated statements, in order
     */
    List<Statement> translate(TranslationMap translationMap);

    /**
     * @return {@code true} if control never falls through past this statement (for example {@code throw}
     * and {@code return}). Populated by analysis; defaults to {@code false}.
     */
    default boolean alwaysEscapes() {
        return false;
    }

    /**
     * Clone this statement into a new {@code Info} graph, relinking all references through the supplied
     * map. Structure is preserved one-to-one (contrast with {@link #translate}).
     *
     * @param infoMap maps old {@code Info} objects to their replacements
     * @return the rewired statement
     */
    Statement rewire(InfoMap infoMap);

    /**
     * Helper for implementations: rewire this statement's annotations through the supplied map.
     */
    default List<AnnotationExpression> rewireAnnotations(InfoMap infoMap) {
        return annotations().stream().map(ae -> (AnnotationExpression) ae.rewire(infoMap)).toList();
    }
}
