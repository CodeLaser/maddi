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

package io.codelaser.maddi.cst.api.expression;

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.RecordPattern;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.LocalVariable;

/**
 * An {@code expression instanceof TestType} test, including pattern-matching forms
 * ({@code x instanceof String s} or a record pattern). The result type is {@code boolean}.
 */
public interface InstanceOf extends Expression {
    /**
     * @return the expression being tested.
     */
    Expression expression();

    /**
     * @return the binding pattern ({@code s} in {@code instanceof String s}, or a record pattern), or
     * {@code null} for a plain type test.
     */
    RecordPattern patternVariable();

    /**
     * @return the type tested against.
     */
    ParameterizedType testType();

    interface Builder extends Element.Builder<Builder> {
        @Fluent
        Builder setExpression(Expression expression);

        @Fluent
        Builder setPatternVariable(RecordPattern patternVariable);

        @Fluent
        Builder setTestType(ParameterizedType testType);

        InstanceOf build();
    }

    String NAME = "instanceOf";

    @Override
    default String name() {
        return NAME;
    }
}
