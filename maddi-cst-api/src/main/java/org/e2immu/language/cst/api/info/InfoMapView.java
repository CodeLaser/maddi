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

import java.util.Set;

/**
 * The read-only lookup facet of a completed rewire (see {@link InfoMap}, which extends it). Every {@code rewire(...)}
 * method takes an {@code InfoMapView}, so a completed {@code InfoMap} can be handed out — e.g. from the reload — to
 * re-point references without also exposing the {@link InfoMap#put} / {@link InfoMap#rewireAll} mutators. Its lookups
 * are pure and stable after {@link InfoMap#rewireAll()}. See {@code docs/analysis-rewiring.md}.
 */
public interface InfoMapView {

    /**
     * <em>Every</em> type this map rewired, as opposed to only the primary types {@link InfoMap#rewireAll()} returns:
     * their subtypes, and the types phase 3 rewires on demand — anonymous classes ({@code ConstructorCall}), local
     * classes ({@code LocalTypeDeclaration}) and lambdas.
     * <p>
     * Those last three are not among a type's {@code subTypes()} ("the types directly enclosed in this type's body"),
     * so a caller that re-registers a rewired type by walking it misses them and keeps handing out the objects the
     * rewire replaced. This map is the only thing that knows what it built; ask it rather than re-derive it.
     * <p>
     * Call after {@link InfoMap#rewireAll()}: the on-demand types come into existence during phase 3.
     */
    Set<TypeInfo> rewiredTypes();

    /**
     * Returns the rewired copy of {@code typeInfo}.
     * Does not recurse through enclosing types; throws if not registered.
     */
    TypeInfo typeInfo(TypeInfo typeInfo);

    /**
     * Returns the rewired copy of {@code typeInfo}, trying the full enclosing-type chain
     * if a direct mapping is absent. For use inside phase-3 expression rewiring only.
     */
    TypeInfo typeInfoRecurseAllPhases(TypeInfo typeInfo);

    /**
     * Returns the rewired copy of {@code typeInfo}, or {@code null} if not registered.
     * Used during phase 0 to check whether a type has already been registered.
     */
    TypeInfo typeInfoNullIfAbsent(TypeInfo typeInfo);

    /**
     * Returns the rewired copy of {@code methodInfo}.
     * Throws if not registered.
     */
    MethodInfo methodInfo(MethodInfo methodInfo);

    /**
     * Returns the rewired copy of {@code fieldInfo}.
     * Throws if not registered.
     */
    FieldInfo fieldInfo(FieldInfo fieldInfo);

    /**
     * Returns the rewired copy of {@code parameterInfo}.
     * Throws if not registered.
     */
    ParameterInfo parameterInfo(ParameterInfo parameterInfo);
}
