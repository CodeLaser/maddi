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

package org.e2immu.analyzer.modification.analyzer;

import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.TypeInfo;

/*
Phase 4.1

Given the modification and independence state of the fields of a type, compute its @Immutable properties.
This analyzer computes @Immutable across all subtypes, because the fields are visible across the subtypes,
hence their modification and independence status is shared.

It is possible to have to wait for other type's @Immutable status, because of extensions and non-private fields.
 */
public interface TypeIndependentAnalyzer {

    void go(TypeInfo primaryType, boolean activateCycleBreaking);

    /**
     * The independence the type reaches once everything in {@code afterMark} can no longer change (road to
     * immutability §060), the counterpart of {@link TypeImmutableAnalyzer#immutableAfterMark}. Null when undecided.
     * <p>
     * Independence is a separate axis from immutability, and §060 does not define an "eventual independence".
     * It nevertheless has a well-defined after-the-mark reading: a dependent accessor that can only be called
     * <em>before</em> the mark ({@code TypeInfo.builder()} asserts {@code inspection.isVariable()}) cannot leak
     * anything once the mark has been passed. Without this, every eventually immutable type stops at
     * {@code FINAL_FIELDS} on the dependence cap in {@code computeImmutableType}, before the relaxation it was
     * given an {@code AfterMark} for is ever consulted.
     */
    Value.Independent independentAfterMark(TypeInfo typeInfo, TypeImmutableAnalyzer.AfterMark afterMark,
                                           boolean activateCycleBreaking);
}
