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
 * Bidirectional registry used during the <em>rewiring</em> protocol, which deep-copies a set
 * of {@link TypeInfo} objects while remapping all internal cross-references to their copies.
 * <p>
 * Rewiring is driven by {@link TypeInfo#rewirePhase0} through {@link TypeInfo#rewirePhase3}
 * (see those methods for the phase breakdown). An {@code InfoMap} is populated incrementally:
 * each phase registers the newly created shells or rewired members before the next phase
 * starts looking them up.
 * <p>
 * Lookup methods throw if the requested object is not registered, except where explicitly
 * documented as returning {@code null}.
 */
public interface InfoMap {

    /**
     * Registers a new (rewired) copy of {@code typeInfo} with the same fully qualified name.
     * May be called at most once per original {@code TypeInfo} object.
     */
    void put(TypeInfo typeInfo);

    /** Registers a mapping from an original method to its rewired copy. */
    void put(MethodInfo original, MethodInfo rewired);

    /**
     * Registers a new (rewired) copy of {@code fieldInfo} with the same owner and name.
     * The rewired copy must already be registered via its owning type before this is called.
     */
    void put(FieldInfo fieldInfo);

    /** Registers a mapping from an original parameter to its rewired copy. */
    void put(ParameterInfo original, ParameterInfo rewired);

    /**
     * Drives the full rewiring of all registered types (phases 1–3) and returns the
     * set of rewired primary types.
     */
    Set<TypeInfo> rewireAll();

    /**
     * <em>Every</em> type this map rewired, as opposed to only the primary types {@link #rewireAll()} returns: their
     * subtypes, and the types phase 3 rewires on demand — anonymous classes ({@code ConstructorCall}), local classes
     * ({@code LocalTypeDeclaration}) and lambdas.
     * <p>
     * Those last three are not among a type's {@code subTypes()} ("the types directly enclosed in this type's body"),
     * so a caller that re-registers a rewired type by walking it misses them and keeps handing out the objects the
     * rewire replaced. This map is the only thing that knows what it built; ask it rather than re-derive it.
     * <p>
     * Call after {@link #rewireAll()}: the on-demand types come into existence during phase 3.
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
