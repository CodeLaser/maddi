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

/**
 * One arm of an arrow-form switch ({@link SwitchStatementNewStyle}): the labels on the left of the
 * {@code ->} together with the code on its right. A {@code SwitchEntry} is an {@link Element}, not a
 * {@link Statement}. Entries are {@link Comparable} so they can be ordered (for example {@code default}
 * last).
 */
public interface SwitchEntry extends Comparable<SwitchEntry>, Element {

    /**
     * @return the case labels of this arm. A single {@code EmptyExpression} represents {@code default};
     * a {@code NullConstant} represents the {@code null} label.
     */
    List<Expression> conditions();

    /**
     * @return the record/type pattern of a pattern-matching label (Java 21), or {@code null} when the
     * arm has no pattern.
     */
    RecordPattern patternVariable();

    /**
     * @return the arm's code as a block (wrapping {@link #statement()} when it is a single statement).
     */
    Block statementAsBlock();

    /**
     * @return the {@code when} guard expression (Java 21), or an {@code EmptyExpression} when absent.
     */
    Expression whenExpression();

    /**
     * @return the arm's code (an expression-, throw-, or block statement).
     */
    Statement statement();

    SwitchEntry translate(TranslationMap translationMap);

    /**
     * @return an immutable copy of this entry with its {@link #statement()} replaced; this instance is
     * unchanged.
     */
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
