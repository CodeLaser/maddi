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

public interface Block extends Statement {

    default Statement lastStatement() {
        int size = size();
        return size == 0 ? null : statements().get(size - 1);
    }

    default int size() {
        return statements().size();
    }

    List<Statement> statements();

    List<Comment> trailingComments();

    interface Builder extends Statement.Builder<Builder> {

        @Fluent
        Builder addTrailingComments(List<Comment> trailingComments);

        Block build();

        @Fluent
        Builder addStatements(List<Statement> statements);

        @Fluent
        Builder addStatement(Statement statement);

        @Fluent
        Builder addStatements(int index, List<Statement> statements);

        @Fluent
        Builder addStatement(int index, Statement statement);

        List<Statement> statements();
    }

    default boolean isEmpty() {
        return statements().isEmpty();
    }

    /*
    Remove statement from statements list in block.
    This method descends into Block statements!
     */
    Block remove(Statement toRemove);

    String NAME = "block";

    @Override
    default String name() {
        return NAME;
    }

    Statement findStatementByIndex(String index);

    Block rewire(InfoMap infoMap);
}
