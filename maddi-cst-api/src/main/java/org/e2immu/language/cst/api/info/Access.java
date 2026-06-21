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
 * Represents the effective visibility of an {@link Info} element.
 * <p>
 * Levels are ordered from most to least restrictive: for Java,
 * {@code private == 0}, {@code package == 1}, {@code protected == 2}, {@code public == 3}.
 * Comparisons should use {@link #level()} or the convenience methods {@link #ge} / {@link #le}.
 */
public interface Access {

    /**
     * Returns a numeric accessibility level, with {@code 0} being the most restrictive.
     * For Java: {@code private=0}, {@code package=1}, {@code protected=2}, {@code public=3}.
     */
    int level();

    /**
     * Returns the most restrictive of {@code this} and {@code other}.
     * Used to propagate the access of an enclosing type onto its members.
     */
    Access combine(Access other);

    /** Returns the least restrictive (most accessible) of {@code this} and {@code other}. */
    Access max(Access other);

    /** Returns {@code true} if this access level is at least as permissive as {@code other}. */
    default boolean ge(Access other) {
        return level() >= other.level();
    }

    /** Returns {@code true} if this access level is at most as permissive as {@code other}. */
    default boolean le(Access other) {
        return level() <= other.level();
    }

    /** Returns {@code true} if this element has {@code public} visibility. */
    boolean isPublic();

    /** Returns {@code true} if this element has {@code private} visibility. */
    boolean isPrivate();

    /** Returns {@code true} if this element has {@code protected} visibility. */
    boolean isProtected();

    /** Returns {@code true} if this element has package-private (default) visibility. */
    boolean isPackage();
}
