package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.expression.Expression;

import java.util.Collection;
import java.util.List;

public interface SwitchStatementNewStyle extends Statement {

    // selector == expression()
    // block = null
    // otherBlocks = each of the individual blocks?

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
