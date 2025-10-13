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
import org.e2immu.language.cst.api.output.OutputBuilder;
import org.e2immu.language.cst.api.variable.Variable;

public interface VariableExpression extends Expression {

    Variable variable();

    VariableExpression withSuffix(Suffix suffix);

    VariableExpression withVariable(Variable variable);

    interface Suffix {
        OutputBuilder print();
    }

    // no suffix by default
    default Suffix suffix() {
        return null;
    }

    interface VariableField extends Suffix {
        int statementTime();

        String latestAssignment();
    }

    interface ModifiedVariable extends Suffix {
        String latestModification();
    }

    interface Builder extends Element.Builder<Builder> {
        @Fluent
        Builder setVariable(Variable variable);

        @Fluent
        Builder setSuffix(Suffix suffix);

        VariableExpression build();
    }

    String NAME = "variableExpression";

    @Override
    default String name() {
        return NAME;
    }
}
