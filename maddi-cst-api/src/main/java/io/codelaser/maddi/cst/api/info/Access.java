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

    /** Returns {@code true} if this element has {@code internal} (module) visibility, e.g. Kotlin {@code internal}. */
    boolean isInternal();
}
