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
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

/**
 * A variable declared inside a method body, initialiser block, for-loop, catch clause,
 * or similar local scope.
 * <p>
 * This interface covers named locals, pattern variables (from {@code instanceof} or
 * {@code switch}), and Java 22 unnamed variables ({@code _}).  Parameters are
 * represented by {@link io.codelaser.maddi.cst.api.info.ParameterInfo} rather than by
 * this interface.
 * <p>
 * A {@code LocalVariable} may carry the value it was initialised with via
 * {@link #assignmentExpression()}; this is set during inspection from the
 * {@link io.codelaser.maddi.cst.api.statement.LocalVariableCreation} statement and
 * may be {@code null} when the value is not known or not relevant.
 */
public interface LocalVariable extends Variable {

    /**
     * Returns {@code true} if this is a Java 22 unnamed variable ({@code _}),
     * which may not be read after its declaration.
     */
    boolean isUnnamed();

    /**
     * Returns the initialisation expression recorded at creation time, {@code EmptyExpression} when absent,
     * or {@code null} if not yet resolved.
     */
    Expression assignmentExpression();

    /**
     * The identity of the declaration: the same object for the declaration and every read of it, through every
     * copy ({@link #withType}, {@link #withName}, {@link #withAssignmentExpression}, translation, rewiring); a
     * different one for another declaration of the same name. {@code equals} compares names only, so two
     * same-named locals of sibling scopes are equal; key on this when the declaration matters, e.g. "is the local
     * this expression reads assigned anywhere in the method?" (CodeLaser/maddi#127). Compare it with
     * {@code equals} or {@code ==}; its hash code is not stable across runs, so do not order by it.
     * By default the variable itself: an implementation that makes no copies needs nothing more.
     */
    default Object declarationId() {
        return this;
    }

    /** Returns a translated copy of this local variable as described by {@code translationMap}. */
    LocalVariable translate(TranslationMap translationMap);

    /** Returns an immutable copy of this local variable with a different declared type. */
    LocalVariable withType(ParameterizedType type);

    /**
     * @param name the new name
     * @return an immutable copy of this local variable with the given name
     */
    LocalVariable withName(String name);

    /**
     * @param expression the new assignment expression
     * @return an immutable copy of this local variable with the given initialisation expression
     */
    LocalVariable withAssignmentExpression(Expression expression);
}
