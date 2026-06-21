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

package org.e2immu.language.cst.api.type;

import org.e2immu.language.cst.api.output.element.Keyword;

/**
 * Classifies the kind of a type declaration: {@code class}, {@code interface},
 * {@code enum}, {@code record}, or {@code @interface} (annotation).
 * <p>
 * Implementations are typically a closed enum. The default methods all return {@code false}
 * so that new natures can be added without breaking existing implementations.
 */
public interface TypeNature {

    /** Returns {@code true} if this nature is {@code class}. */
    default boolean isClass() {
        return false;
    }

    /** Returns {@code true} if this nature is {@code enum}. */
    default boolean isEnum() {
        return false;
    }

    /** Returns {@code true} if this nature is {@code interface}. */
    default boolean isInterface() {
        return false;
    }

    /** Returns {@code true} if this nature is {@code record}. */
    default boolean isRecord() {
        return false;
    }

    /**
     * Returns {@code true} if types of this nature are implicitly static (i.e. not inner classes).
     * True for all natures except non-static nested {@code class}.
     */
    default boolean isStatic() {
        return false;
    }

    /** Returns {@code true} if this nature is {@code @interface} (annotation type). */
    default boolean isAnnotation() {
        return false;
    }

    /**
     * Returns {@code true} if this type is a stub — created during parsing because both
     * source and bytecode are unavailable. Inspection data is filled in as far as possible
     * so that parsing can continue without stopping.
     */
    default boolean isStub() {
        return false;
    }

    /** Returns {@code true} if this type represents a {@code package-info.java} compilation unit. */
    default boolean isPackageInfo() {
        return false;
    }

    /** Returns the {@link Keyword} token used to print the type-nature keyword in source form. */
    Keyword keyword();
}
