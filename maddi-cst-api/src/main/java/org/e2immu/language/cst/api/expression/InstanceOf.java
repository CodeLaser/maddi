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

package org.e2immu.language.cst.api.expression;

import org.e2immu.annotation.Fluent;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.element.RecordPattern;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.LocalVariable;

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
