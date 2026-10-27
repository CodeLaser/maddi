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

import io.codelaser.maddi.annotation.Independent;

import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.cst.api.element.DetailedSources;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.util.OneVariable;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

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
 * Variables implement {@link Comparable} so that {@link io.codelaser.maddi.cst.api.expression.Expression}
 * collections can be sorted canonically by {@link #fullyQualifiedName()}.
 */
public interface Variable extends Comparable<Variable>, Element, OneVariable {

    /** Returns the fully-qualified name used to sort and uniquely identify this variable. */
    @NotNull
    String fullyQualifiedName();

    /**
     * Returns {@code true} if the analyzer should not track modifications through this variable.
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
    Variable rewire(@Independent(hc = true) InfoMapView infoMap);

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
