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

package io.codelaser.maddi.cst.api.variable;

import io.codelaser.maddi.cst.api.expression.Expression;

/**
 * A variable representing an array-element access such as {@code a[i]}.
 * <p>
 * The variable is modelled as a pair of an array variable and an index expression.
 * Nested accesses (e.g. {@code a[i][j]}) are represented as a {@code DependentVariable}
 * whose {@link #arrayVariable()} is itself a {@code DependentVariable}.
 */
public interface DependentVariable extends Variable {

    /**
     * Returns the variable that holds the array being indexed, or {@code null} when the array expression is
     * not a variable at all — {@code map.get(k)[0]} has an array <em>expression</em> and no array
     * <em>variable</em>. Use {@link #arrayExpression()}, which is always present, when you need something
     * unconditional.
     * <p>
     * ⚠ This nullability was undocumented while {@link #indexVariable()}'s was documented, and callers took
     * the silence as a guarantee: {@code DependentVariableImpl} carries a comment recording two sites that
     * had to be fixed for it, and a third (the refactor DSL's {@code AstToMap}) took down a whole corpus
     * query with a message-less {@code NullPointerException} from a pattern switch. Stated here so the next
     * caller does not have to read the implementation to find out.
     */
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
