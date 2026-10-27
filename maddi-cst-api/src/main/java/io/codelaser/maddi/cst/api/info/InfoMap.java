/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.api.info;

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
