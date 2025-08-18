package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.statement.SwitchEntry;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.Collection;
import java.util.List;

public interface SwitchExpression extends Expression {

    Expression selector();

    List<SwitchEntry> entries();

    SwitchExpression withSelector(Expression newSelector);

    interface Builder extends Element.Builder<Builder> {

        @Fluent
        Builder setParameterizedType(ParameterizedType parameterizedType);

        @Fluent
        Builder setSelector(Expression selector);

        @Fluent
        Builder addSwitchEntries(Collection<SwitchEntry> switchEntries);

        SwitchExpression build();
    }

    String NAME = "switchExpression";

    @Override
    default String name() {
        return NAME;
    }
}
