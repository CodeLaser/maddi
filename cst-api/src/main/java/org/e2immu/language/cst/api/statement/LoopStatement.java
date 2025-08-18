package org.e2immu.language.cst.api.statement;

public interface LoopStatement extends Statement {

    @Override
    default boolean hasSubBlocks() {
        return true;
    }
}
