package org.e2immu.language.cst.api.statement;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.RecordPattern;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.translate.TranslationMap;

import java.util.Collection;
import java.util.List;

public interface SwitchEntry extends Comparable<SwitchEntry>, Element {
    // EmptyExpression for 'default', NullConstant for 'null'

    List<Expression> conditions();
    // or null, when absent (Java 21)

    RecordPattern patternVariable();

    Block statementAsBlock();

    // EmptyExpression when absent (Java 21)
    Expression whenExpression();

    Statement statement();

    SwitchEntry translate(TranslationMap translationMap);

    SwitchEntry withStatement(Statement statement);

    interface Builder extends Element.Builder<Builder> {
        @Fluent
        Builder addConditions(Collection<Expression> expressions);

        @Fluent
        Builder setStatement(Statement statement);

        @Fluent
        Builder setPatternVariable(RecordPattern patternVariable);

        @Fluent
        Builder setWhenExpression(Expression whenExpression);

        SwitchEntry build();
    }
}
