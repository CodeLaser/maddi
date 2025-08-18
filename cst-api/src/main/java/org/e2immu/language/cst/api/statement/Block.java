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
