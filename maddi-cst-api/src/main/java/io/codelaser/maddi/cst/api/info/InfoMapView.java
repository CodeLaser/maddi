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
import java.util.function.Supplier;

/**
 * The read-only lookup facet of a completed rewire (see {@link InfoMap}, which extends it). Every {@code rewire(...)}
 * method takes an {@code InfoMapView}, so a completed {@code InfoMap} can be handed out — e.g. from the reload — to
 * re-point references without also exposing the {@link InfoMap#put} / {@link InfoMap#rewireAll} mutators. Its lookups
 * are pure and stable after {@link InfoMap#rewireAll()}. See {@code docs/design/analysis-rewiring.md}.
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

    /**
     * The copy this rewire made of {@code original}, made once: the first call runs {@code rewire} and every later
     * call for the same object (by identity) returns that same copy.
     * <p>
     * ⛔ WHY IDENTITY MATTERS. {@link io.codelaser.maddi.cst.api.element.DetailedSources} is keyed by object identity:
     * the source of a field's TYPE is found under the field's {@code ParameterizedType} instance. A rewire that copies
     * the field's type and, separately, the field's source has to arrive at the same new instance twice, or every
     * such lookup misses on the rewired type, and the old keys keep the previous generation reachable (found by the
     * rewire driver on fernflower; docs/design/incremental-and-overlay.md).
     */
    <T> T rewired(T original, Supplier<T> rewire);

    /**
     * The copy this rewire made of {@code original} through {@link #rewired}, or {@code null} when it made none. A
     * lookup only: what carries values onto the copies after the rewire (a seeded analysis carrying the derived tier
     * of a {@code REWIRE} type's variable data) must not make a copy of its own.
     */
    default <T> T rewiredIfPresent(T original) {
        return null;
    }

    /**
     * Records {@code rewired} as the copy of {@code original}, for a copy made outside {@link #rewired} (a type's
     * type parameters are built by the rewire phases), or made under construction (a type parameter is entered
     * before its bounds are rewired, so that a self-referential bound terminates).
     */
    void registerRewired(Object original, Object rewired);

    /**
     * Whether this rewire carries the intrinsic tier ({@code Property.AnalysisTier.INTRINSIC}, prep's output and the
     * like) of the properties that opt into {@code carryOnRewire}. Only a partial re-parse does: it rewires types whose
     * bodies did not change, so what prep computed from them still holds, and prep skips them. Every other rewire
     * (a transform that builds new bodies from old statements, then rewires and preps the result) must not: the
     * carried values describe the old bodies, prep then writes them a second time, and rewiring them can already fail
     * on a type the transform changed. The other tiers carry as before.
     */
    default boolean carryIntrinsic() {
        return false;
    }
}
