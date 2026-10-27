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

package io.codelaser.maddi.cst.api.type;

import io.codelaser.maddi.cst.api.output.element.Keyword;

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
