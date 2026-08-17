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

/**
 * Nullability of a {@link ParameterizedType}, as a first-class dimension of the type.
 * <p>
 * Languages that do not track nullability in their type system (Java) always produce
 * {@link #UNSPECIFIED}, which is the default everywhere; this keeps their behaviour unchanged.
 * Kotlin distinguishes {@code String} (non-null) from {@code String?} (nullable); the Kotlin
 * front-end records {@link #NULLABLE} for the latter. {@link #NONNULL} is reserved for an explicit
 * non-null guarantee (e.g. {@code @NotNull}); it is not yet emitted by default.
 */
public enum NullableState {
    UNSPECIFIED,
    NONNULL,
    NULLABLE,
}
