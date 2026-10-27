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
import io.codelaser.maddi.cst.api.statement.SwitchEntry;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.Collection;
import java.util.List;

/**
 * A switch expression, {@code switch (selector) { case ... -> value; ... }}, which yields a value. Like
 * the arrow-form switch statement it is modelled as a list of
 * {@link io.codelaser.maddi.cst.api.statement.SwitchEntry SwitchEntry} arms; the result type is the
 * overall {@link Expression#parameterizedType()}.
 */
public interface SwitchExpression extends Expression {

    /**
     * @return the value being switched on.
     */
    Expression selector();

    /**
     * @return the arms (each producing a value, via {@code -> value} or {@code yield}).
     */
    List<SwitchEntry> entries();

    /**
     * @return an immutable copy with a different selector; this instance is unchanged.
     */
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
