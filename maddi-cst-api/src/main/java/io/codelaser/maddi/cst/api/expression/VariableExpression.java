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
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.variable.Variable;

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
