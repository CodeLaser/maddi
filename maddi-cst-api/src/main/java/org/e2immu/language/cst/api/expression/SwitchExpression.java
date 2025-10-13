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
