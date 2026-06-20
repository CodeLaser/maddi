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

/**
 * A reference to a {@link Variable} used as an expression (reading a local, parameter, field, …). This is
 * the leaf that connects expressions to the variable model.
 *
 * <p>An optional {@link Suffix} carries analysis-time disambiguation that is rendered after the variable
 * name (for example which assignment or modification a read observes); it has no effect on plain source.
 */
public interface VariableExpression extends Expression {

    /**
     * @return the referenced variable.
     */
    Variable variable();

    /**
     * @return an immutable copy with the given {@link Suffix}; this instance is unchanged.
     */
    VariableExpression withSuffix(Suffix suffix);

    /**
     * @return an immutable copy referencing a different variable; this instance is unchanged.
     */
    VariableExpression withVariable(Variable variable);

    /**
     * Analysis-time annotation appended to a variable read when printed; see {@link VariableField} and
     * {@link ModifiedVariable}.
     */
    interface Suffix {
        OutputBuilder print();
    }

    /**
     * @return the suffix attached to this read, or {@code null} (the default) when none.
     */
    default Suffix suffix() {
        return null;
    }

    /**
     * Suffix identifying which version of a field a read observes: its {@link #statementTime()} and the
     * {@link #latestAssignment()} that produced the value.
     */
    interface VariableField extends Suffix {
        int statementTime();

        String latestAssignment();
    }

    /**
     * Suffix identifying the {@link #latestModification()} a read observes for a mutable variable.
     */
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
