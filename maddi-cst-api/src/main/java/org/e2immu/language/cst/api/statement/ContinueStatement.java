package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Source;

public interface ContinueStatement extends BreakOrContinueStatement {

    ContinueStatement withSource(Source newSource);

    interface Builder extends Statement.Builder<Builder> {
        @Fluent
        Builder setGoToLabel(String goToLabel);

        ContinueStatement build();
    }

    String NAME = "continue";

    @Override
    default String name() {
        return NAME;
    }
}
