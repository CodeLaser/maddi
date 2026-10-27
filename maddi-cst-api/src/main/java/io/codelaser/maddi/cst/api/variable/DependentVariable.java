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
