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

package io.codelaser.maddi.cst.api.info;

/**
 * A modifier that can appear on a field declaration:
 * {@code public}, {@code private}, {@code protected},
 * {@code static}, {@code final}, {@code volatile}, or {@code transient}.
 */
public interface FieldModifier extends Modifier {

    /** Returns {@code true} if this modifier controls visibility ({@code public}, {@code protected}, {@code private}, or {@code internal}). */
    default boolean isAccessModifier() {
        return isPublic() || isPrivate() || isProtected() || isInternal();
    }

    /** Returns {@code true} if this is the {@code internal} modifier (module visibility, e.g. Kotlin {@code internal}). */
    boolean isInternal();

    /** Returns {@code true} if this is the {@code static} modifier. */
    boolean isStatic();

    /** Returns {@code true} if this is the {@code final} modifier. */
    boolean isFinal();

    /** Returns {@code true} if this is the {@code volatile} modifier. */
    boolean isVolatile();

    /** Returns {@code true} if this is the {@code transient} modifier (excluded from serialisation). */
    boolean isTransient();

    /** Returns {@code true} if this is the {@code public} modifier. */
    boolean isPublic();

    /** Returns {@code true} if this is the {@code private} modifier. */
    boolean isPrivate();

    /** Returns {@code true} if this is the {@code protected} modifier. */
    boolean isProtected();
}
