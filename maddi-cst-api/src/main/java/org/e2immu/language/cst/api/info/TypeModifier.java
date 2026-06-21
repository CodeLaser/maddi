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

package org.e2immu.language.cst.api.info;

/**
 * A modifier that can appear on a type declaration:
 * {@code public}, {@code private}, {@code protected}, {@code abstract},
 * {@code final}, {@code static}, {@code sealed}, or {@code non-sealed}.
 */
public interface TypeModifier extends Modifier {
    /** Returns {@code true} if this is the {@code public} modifier. */
    boolean isPublic();

    /** Returns {@code true} if this is the {@code private} modifier. */
    boolean isPrivate();

    /** Returns {@code true} if this is the {@code protected} modifier. */
    boolean isProtected();

    /** Returns {@code true} if this is the {@code abstract} modifier. */
    boolean isAbstract();

    /** Returns {@code true} if this is the {@code final} modifier. */
    boolean isFinal();

    /** Returns {@code true} if this is the {@code static} modifier (nested types only). */
    boolean isStatic();

    /** Returns {@code true} if this is the {@code sealed} modifier. */
    boolean isSealed();

    /** Returns {@code true} if this is the {@code non-sealed} modifier. */
    boolean isNonSealed();

    /** Returns {@code true} if this modifier controls visibility ({@code public}, {@code protected}, or {@code private}). */
    default boolean isAccessModifier() {
        return isPrivate() || isProtected() || isPublic();
    }
}
