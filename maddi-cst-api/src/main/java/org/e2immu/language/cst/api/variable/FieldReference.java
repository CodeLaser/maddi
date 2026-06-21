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
import org.e2immu.language.cst.api.info.FieldInfo;

/**
 * A variable representing a field access of the form {@code scope.fieldName} or a bare
 * {@code fieldName} that implicitly refers to {@code this.fieldName} or {@code SomeType.fieldName}.
 * <p>
 * The scope may be any expression — a variable reference, a method call, or even another
 * {@code FieldReference} — which allows chains such as {@code a.b.c} to be represented as
 * nested {@code FieldReference} instances.
 */
public interface FieldReference extends Variable {

    /** Returns the {@link FieldInfo} metadata for the field being accessed. */
    FieldInfo fieldInfo();

    /**
     * Returns the scope expression qualifying this field access (the left-hand side of the dot),
     * or {@code null} when the scope is implicitly the current instance ({@code this}).
     */
    Expression scope();

    /**
     * Returns the scope as a {@link Variable} when {@link #scope()} is a
     * {@link org.e2immu.language.cst.api.expression.VariableExpression}, or {@code null} otherwise.
     */
    Variable scopeVariable();

    /**
     * Returns {@code true} if the scope resolves transitively to {@code this} —
     * including chains like {@code this.someField.anotherField}.
     */
    boolean scopeIsRecursivelyThis();

    /**
     * Returns {@code true} when no explicit {@code .} qualifier appears in the source;
     * the field is written as a bare name.
     */
    boolean isDefaultScope();

    /** Returns {@code true} if the immediate scope is the {@code this} pseudo-variable. */
    boolean scopeIsThis();

    @Override
    default FieldReference fieldReferenceScope() {
        return this;
    }

    @Override
    default Variable fieldReferenceBase() {
        Variable scopeVariable = scopeVariable();
        if (scopeVariable instanceof FieldReference fr) return fr.fieldReferenceBase();
        return scopeVariable;
    }
}
