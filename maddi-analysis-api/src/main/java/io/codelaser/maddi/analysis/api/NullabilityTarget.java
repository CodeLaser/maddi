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

package io.codelaser.maddi.analysis.api;

/**
 * What the nullability facts are for: the language a translation prints, whose type system decides how "no
 * verdict" must be read. Each target maps onto a policy of the nullability pass (maddi-mod).
 */
public enum NullabilityTarget {
    /** Kotlin source has no platform type: what the analysis does not call nullable is non-null. */
    KOTLIN,
    /** C# with nullable reference types: the facts as for Kotlin; the C# printer adds the stored verdicts. */
    CSHARP,
}
