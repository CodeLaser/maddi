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
import io.codelaser.maddi.cst.api.info.FieldInfo;

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
     * {@link io.codelaser.maddi.cst.api.expression.VariableExpression}, or {@code null} otherwise.
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
