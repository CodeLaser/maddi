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

import org.e2immu.language.cst.api.info.TypeInfo;

/**
 * The pseudo-variable representing the {@code this} or {@code super} reference.
 * <p>
 * Every non-static method implicitly has a {@code This} variable bound to the enclosing type.
 * Inner classes can carry a qualified {@code this} (e.g. {@code Outer.this}), which is
 * expressed by setting {@link #explicitlyWriteType()} to the outer type.
 * When the reference is {@code super} rather than {@code this}, {@link #writeSuper()} returns
 * {@code true}.
 */
public interface This extends Variable {

    /** Returns the type this pseudo-variable refers to. */
    TypeInfo typeInfo();

    /**
     * Returns the type that should be written explicitly in front of {@code .this} for a
     * qualified reference (e.g. {@code Outer} in {@code Outer.this}),
     * or {@code null} when no explicit qualification is needed.
     */
    TypeInfo explicitlyWriteType();

    /**
     * Returns {@code true} if this pseudo-variable should be printed as {@code super}
     * rather than {@code this}.
     */
    boolean writeSuper();
}
