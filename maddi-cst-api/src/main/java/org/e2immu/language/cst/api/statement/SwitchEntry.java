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
