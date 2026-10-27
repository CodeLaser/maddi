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

package io.codelaser.maddi.cst.api.analysis;

import io.codelaser.maddi.annotation.Independent;

import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;

import java.util.function.Supplier;
import java.util.stream.Stream;

public interface PropertyValueMap {

    boolean isEmpty();

    PropertyValueMap rewire(@Independent(hc = true) InfoMapView infoMap);

    /**
     * A filtered carry: keep only the properties matching {@code filter}, re-pointing their values through the
     * {@code infoMap}. {@link #rewire(InfoMap)} is this with {@code Property::carryOnRewire}. The fingerprint-gated
     * skip passes the analyzer-output predicate (verdicts + link summaries, excluding prepwork-internal detail like
     * {@code VARIABLE_DATA}, which is recomputed anyway) — see {@code docs/design/analysis-rewiring.md}. Requires each kept
     * property's value to implement {@code rewire}.
     */
    PropertyValueMap rewire(@Independent(hc = true) InfoMapView infoMap, java.util.function.Predicate<Property> filter);

    /**
     * Remove every property matching {@code filter}. The early-cutoff skip's clear-before-recompute: a type carried
     * optimistically but found dirty by the worklist must have its carried (possibly-stale) analyzer output cleared,
     * or re-analysis hits the monotonic-overwrite guard when it lowers a value. See {@code docs/design/analysis-rewiring.md}.
     */
    void removeIf(java.util.function.Predicate<Property> filter);

    record PropertyValue(Property property, Value value) {

    }

    <V extends Value> V getOrDefault(Property property, V defaultValue);

    <V extends Value> V getOrNull(Property property, Class<? extends V> clazz);

    <V extends Value> V getOrCreate(Property property, Supplier<V> createDefaultValue);

    boolean haveAnalyzedValueFor(Property property);

    default boolean haveAnalyzedValueFor(Property property, Runnable runWhenNoValue) {
        boolean b = haveAnalyzedValueFor(property);
        if (!b) runWhenNoValue.run();
        return b;
    }

    Stream<PropertyValue> propertyValueStream();

    <V extends Value> void set(Property property, V value);

    <V extends Value> boolean setAllowControlledOverwrite(Property property, V value);

    /**
     * Unconditional replacement, bypassing the {@link Value#overwriteAllowed(Value)} direction policy. For
     * direction-aware refinement in the iterating analyzer (evidence-accumulating properties whose legal
     * refinement runs OPPOSITE to the value's default policy); use sparingly.
     *
     * @return true when the stored value changed
     */
    <V extends Value> boolean overwrite(Property property, V value);

    void setAll(PropertyValueMap analysis);
}
