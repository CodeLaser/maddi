package org.e2immu.language.cst.api.statement;

public interface EmptyStatement extends Statement {

    interface Builder extends Statement.Builder<Builder> {

        EmptyStatement build();
    }

    String NAME = "empty";

    @Override
    default String name() {
        return NAME;
    }
}
