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

package org.e2immu.language.cst.api.variable;

import org.e2immu.language.cst.api.expression.Expression;

/**
 * A variable representing an array-element access such as {@code a[i]}.
 * <p>
 * The variable is modelled as a pair of an array variable and an index expression.
 * Nested accesses (e.g. {@code a[i][j]}) are represented as a {@code DependentVariable}
 * whose {@link #arrayVariable()} is itself a {@code DependentVariable}.
 */
public interface DependentVariable extends Variable {

    /** Returns the variable that holds the array being indexed. */
    Variable arrayVariable();

    /**
     * Returns the outermost non-{@code DependentVariable} in nested array accesses —
     * the variable that actually holds the array object.
     */
    default Variable arrayVariableBase() {
        Variable av = arrayVariable();
        while (av instanceof DependentVariable dv) {
            av = dv.arrayVariable();
        }
        return av;
    }

    /**
     * Returns the variable used as the index, or {@code null} if the index is a constant
     * expression that is not backed by a variable.
     */
    Variable indexVariable();

    /** Returns the expression that evaluates to the array being indexed. */
    Expression arrayExpression();

    /** Returns the expression that evaluates to the index. */
    Expression indexExpression();

    @Override
    default Variable fieldReferenceBase() {
        return arrayVariable().fieldReferenceBase();
    }

    @Override
    default FieldReference fieldReferenceScope() {
        return arrayVariable().fieldReferenceScope();
    }
}
