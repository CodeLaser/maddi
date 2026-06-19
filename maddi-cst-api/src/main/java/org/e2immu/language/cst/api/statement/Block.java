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
import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.info.InfoMap;

import java.util.List;

/**
 * A brace-delimited sequence of statements, {@code { ... }}.
 *
 * <p>A {@code Block} is itself a {@link Statement}, which lets it appear wherever a statement is expected
 * and lets statements nest blocks uniformly (see {@link Statement#block()}). Its own
 * {@link Statement#block()} is {@code null}; its contents are {@link #statements()}.
 */
public interface Block extends Statement {

    /**
     * @return the last statement of the block, or {@code null} when the block is empty.
     */
    default Statement lastStatement() {
        int size = size();
        return size == 0 ? null : statements().get(size - 1);
    }

    /**
     * @return the number of statements directly in this block.
     */
    default int size() {
        return statements().size();
    }

    /**
     * @return the statements of this block, in source order.
     */
    List<Statement> statements();

    /**
     * @return comments that trail the last statement, before the closing brace.
     */
    List<Comment> trailingComments();

    /**
     * Builder for a {@link Block}. Statements are appended in call order; the {@code (int index, ...)}
     * overloads insert at the given position in the statement list instead.
     */
    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder addTrailingComments(List<Comment> trailingComments);

        Block build();

        @Fluent
        Builder addStatements(List<Statement> statements);

        @Fluent
        Builder addStatement(Statement statement);

        /** Insert statements starting at {@code index} in the current statement list. */
        @Fluent
        Builder addStatements(int index, List<Statement> statements);

        /** Insert a single statement at {@code index} in the current statement list. */
        @Fluent
        Builder addStatement(int index, Statement statement);

        /** @return the statements added so far. */
        List<Statement> statements();
    }

    /**
     * @return {@code true} when the block contains no statements.
     */
    default boolean isEmpty() {
        return statements().isEmpty();
    }

    /**
     * Return an immutable copy of this block with the given statement removed. The search descends
     * recursively into nested blocks.
     *
     * @param toRemove the statement to remove
     * @return a new block; this instance is unchanged
     */
    Block remove(Statement toRemove);

    String NAME = "block";

    @Override
    default String name() {
        return NAME;
    }

    /**
     * Look up a (possibly deeply nested) statement by its structured index.
     *
     * @param index the statement index (for example {@code "0.0.1"})
     * @return the statement at that index, or {@code null} when absent
     */
    Statement findStatementByIndex(String index);

    @Override
    Block rewire(InfoMap infoMap);
}
