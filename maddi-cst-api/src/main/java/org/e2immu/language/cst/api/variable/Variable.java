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

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Element;
import org.e2immu.language.cst.api.expression.util.OneVariable;
import org.e2immu.language.cst.api.info.InfoMap;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Base interface for all variable kinds in the CST.
 * <p>
 * A variable is a named, typed storage location that can appear as a read or write target inside
 * expressions. Concrete sub-types are {@link LocalVariable} (method-body locals, but not parameters),
 * {@link FieldReference} (instance and static field accesses), {@link DependentVariable}
 * (array-element accesses), and {@link This} (the {@code this} / {@code super} pseudo-variable).
 * <p>
 * Variables implement {@link Comparable} so that {@link org.e2immu.language.cst.api.expression.Expression}
 * collections can be sorted canonically by {@link #fullyQualifiedName()}.
 */
public interface Variable extends Comparable<Variable>, Element, OneVariable {

    /** Returns the fully-qualified name used to sort and uniquely identify this variable. */
    @NotNull
    String fullyQualifiedName();

    /**
     * Returns {@code true} if the analyser should not track modifications through this variable.
     * Defaults to {@code false}; overridden for certain synthetic or ignored variables.
     */
    default boolean isIgnoreModifications() {
        return false;
    }

    /** Returns the simple (unqualified) name as it appears in source. */
    @NotNull
    String simpleName();

    /** Returns the declared or inferred type of this variable. */
    @NotNull
    ParameterizedType parameterizedType();

    @Override
    default int compareTo(Variable o) {
        return fullyQualifiedName().compareTo(o.fullyQualifiedName());
    }

    /** Returns {@code true} if this variable refers to a static field. */
    default boolean isStatic() {
        return false;
    }

    /**
     * Returns {@code true} if {@code variable} appears somewhere in the scope chain of this variable.
     * Used to detect recursive or circular scope references.
     */
    default boolean scopeIsRecursively(Variable variable) {
        return false;
    }

    /**
     * Returns the nearest enclosing {@link FieldReference} in this variable's scope chain,
     * or {@code null} if this variable has no field-reference scope.
     */
    default FieldReference fieldReferenceScope() {
        return null;
    }

    /**
     * Returns the innermost non-{@link FieldReference} variable at the root of the scope chain,
     * or {@code null} if there is no such base variable.
     */
    default Variable fieldReferenceBase() {
        return null;
    }

    /**
     * Returns a stream containing this variable and, for variables with a scope,
     * all variables reachable by descending into that scope.
     */
    default Stream<Variable> variableStreamDescendIntoScope() {
        return Stream.of(this);
    }

    /**
     * Returns a copy of this variable with all cross-references rewritten through {@code infoMap}.
     * Used during the rewiring phase when types and members are cloned.
     */
    Variable rewire(InfoMap infoMap);

    @Override
    default Stream<TypeReference> typesReferenced(Predicate<Element> predicate) {
        if (reject(predicate)) return Stream.of();
        return typesReferenced(predicate, null);
    }

    /** Returns a stream of all types referenced by this variable, filtered by {@code predicate}. */
    default Stream<TypeReference> typesReferenced(Predicate<Element> predicate, DetailedSources detailedSources) {
        return Stream.of();
    }
}
