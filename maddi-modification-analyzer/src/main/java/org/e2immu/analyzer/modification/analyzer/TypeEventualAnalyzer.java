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

import org.e2immu.language.cst.api.info.TypeInfo;

/**
 * Phase 4.3: eventual immutability by <em>propagation</em> (road to immutability §060; plan in
 * {@code docs/eventual-immutability.md}).
 * <p>
 * A type that holds a field of eventually immutable type — overwhelmingly one of the {@code org.e2immu.support}
 * classes — is itself eventually immutable, and its methods inherit the mark: a method calling a {@code @Mark}
 * method on such a field is itself a {@code @Mark}, one calling an {@code @Only(before=)} method can itself only
 * run before the mark, and so on. The same holds for a call on {@code this} to an inherited marked method.
 * <p>
 * This needs no preconditions: the callee's own contract says which side of the transition it belongs to. It is
 * the reason the precondition machinery of the previous analyzer generation does not have to be revived.
 */
public interface TypeEventualAnalyzer {

    void go(TypeInfo typeInfo, boolean activateCycleBreaking);
}
