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
 * Lookup methods (in the read-only {@link InfoMapView} super-interface) throw if the requested object is not
 * registered, except where explicitly documented as returning {@code null}. A completed map can be handed out as an
 * {@link InfoMapView} without exposing the mutators below.
 */
public interface InfoMap extends InfoMapView {

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
}
