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

/**
 * Controls whether variable traversal should recurse into the scope of a variable.
 * <p>
 * Used when collecting the set of variables referenced by an expression: {@link #isYes()}
 * means collect variables transitively from scopes (e.g. the scope of a {@link FieldReference}),
 * while {@link #isNo()} means collect only the top-level variable without descending.
 */
public interface DescendMode {
    /** Returns {@code true} if traversal should recurse into variable scopes. */
    boolean isYes();

    /** Returns {@code true} if traversal should stop at the top-level variable. */
    boolean isNo();
}
