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
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.Set;

/*
Phase 4.1

Given the modification and independence state of the fields of a type, compute its @Immutable properties.
This analyzer computes @Immutable across all subtypes, because the fields are visible across the subtypes,
hence their modification and independence status is shared.

It is possible to have to wait for other type's @Immutable status, because of extensions and non-private fields.
 */
public interface TypeImmutableAnalyzer {

    void go(TypeInfo primaryType, boolean activateCycleBreaking);

    /**
     * What may be discounted when computing the level a type reaches <em>after the mark</em> (road to
     * immutability §060). Everything listed here modifies only on the before-side of the state transition, so
     * once the mark has been passed it can no longer change.
     *
     * @param fields  fields of eventually immutable type that the type's own {@code @Mark} methods commit
     * @param methods the type's {@code @Mark} and {@code @Only(before=)} methods. Only the abstract ones
     *                actually matter -- rule 1 catches a concrete modifying method through the field it
     *                modifies -- but an interface has nothing <em>but</em> abstract methods, which is exactly
     *                the case this exists for.
     */
    record AfterMark(Set<FieldInfo> fields, Set<MethodInfo> methods) {
        public static final AfterMark NONE = new AfterMark(Set.of(), Set.of());

        public boolean isNone() {
            return fields.isEmpty() && methods.isEmpty();
        }
    }

    /**
     * The level the type reaches once everything in {@code afterMark} can no longer change. Null when undecided.
     * Used by the eventual analyzer (phase 4.3).
     */
    Value.Immutable immutableAfterMark(TypeInfo typeInfo, AfterMark afterMark, boolean activateCycleBreaking);
}
