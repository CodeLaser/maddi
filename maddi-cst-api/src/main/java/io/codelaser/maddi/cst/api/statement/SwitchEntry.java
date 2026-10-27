/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.api.statement;

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.RecordPattern;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.translate.TranslationMap;

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
     * @return the patterns after the first one of a label that carries several (JEP 456: {@code case A _, B _ ->}),
     * in source order; empty otherwise. {@link #patternVariable()} is the first. Java allows several patterns in one
     * label only when none of them binds a variable, so these never introduce one.
     */
    default List<RecordPattern> otherPatterns() {
        return List.of();
    }

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

        /** A pattern after the first one of the label, see {@link SwitchEntry#otherPatterns()}. */
        @Fluent
        Builder addOtherPattern(RecordPattern pattern);

        @Fluent
        Builder setWhenExpression(Expression whenExpression);

        SwitchEntry build();
    }
}
