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
