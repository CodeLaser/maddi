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

package org.e2immu.language.cst.api.analysis;

public interface Property {
    Class<? extends Value> classOfValue();

    Value defaultValue();

    String key();

    /**
     * Whether this property's value survives a rewire (see {@code rewiring.md}).
     * <p>
     * A rewired type is one whose own source did not change, but which reaches a type that did — so anything
     * <em>derived across types</em> (links, immutability, independence) was computed against source that no longer
     * exists, and must be recomputed rather than carried: for those, dropping is the correct answer, not merely the
     * safe one. Hence the default of {@code false}.
     * <p>
     * Two kinds do want carrying. Data <em>intrinsic</em> to the type's own body — prepwork's, chiefly
     * {@code VARIABLE_DATA} — is still valid, because the body did not change, and recomputing it is most of the
     * cost a reload exists to avoid. Data written at <em>parse</em> time is stronger still: a rewired type is never
     * re-parsed, so if it is not carried it is simply lost.
     * <p>
     * Saying true here is a claim that the value is one of those two, and that its {@link Value#rewire} maps every
     * Info and Variable reference it holds.
     */
    default boolean carryOnRewire() {
        return false;
    }
}
