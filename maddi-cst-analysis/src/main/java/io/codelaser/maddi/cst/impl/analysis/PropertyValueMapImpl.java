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

package io.codelaser.maddi.cst.impl.analysis;

import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.PropertyValueMap;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.InfoMap;
import io.codelaser.maddi.cst.api.info.InfoMapView;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/*
Does not use SetOnceMap<> because of controlled overwrites.

All access is synchronized on 'this': analyzers write properties of OTHER elements (a method call writes
UNMODIFIED_PARAMETER on the callee's parameters), so under intra-iteration parallelism two threads can hit
the same element's map. External compound check-then-act sequences (TolerantWrite) synchronize on the map
object as well — same monitor.
 */
public class PropertyValueMapImpl implements PropertyValueMap {
    private final Map<Property, Value> map = new HashMap<>();

    /**
     * Carries only the properties that declare {@link Property#carryOnRewire()}; the rest are dropped, which for
     * anything derived across types is the correct answer rather than a lossy one (see {@code docs/design/rewiring.md}).
     * Applies to expressions as much as to Info: a stale value is no more welcome inside a rewired MethodCall than
     * on a rewired method.
     */
    @Override
    public synchronized PropertyValueMap rewire(InfoMapView infoMap) {
        return rewire(infoMap, Property::carryOnRewire);
    }

    // filtered carry (docs/design/analysis-rewiring.md): the fingerprint-gated skip passes the analyzer-output predicate
    @Override
    public synchronized PropertyValueMap rewire(InfoMapView infoMap, java.util.function.Predicate<Property> filter) {
        PropertyValueMapImpl rewiredMap = new PropertyValueMapImpl();
        map.forEach((key, value) -> {
            if (filter.test(key)) rewiredMap.set(key, value.rewire(infoMap));
        });
        return rewiredMap;
    }

    @Override
    public synchronized Stream<PropertyValue> propertyValueStream() {
        // snapshot under the lock; the returned stream is safe to consume without it
        return map.entrySet().stream().map(e -> new PropertyValue(e.getKey(), e.getValue())).toList().stream();
    }

    @Override
    public synchronized boolean haveAnalyzedValueFor(Property property) {
        return map.containsKey(property);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <V extends Value> V getOrDefault(Property property, V defaultValue) {
        assert defaultValue != null;
        return (V) map.getOrDefault(property, defaultValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <V extends Value> V getOrNull(Property property, Class<? extends V> clazz) {
        return (V) map.get(property);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <V extends Value> V getOrCreate(Property property, Supplier<V> computeValue) {
        V v = (V) map.get(property);
        if (v != null) return v;
        V vv = computeValue.get();
        if (vv != null) {
            map.put(property, vv);
        }
        return vv;
    }

    @Override
    public synchronized void set(Property property, Value value) {
        assert value != null : "Not allowed to write null";

        assert property.classOfValue().isAssignableFrom(value.getClass());
        if (map.put(property, value) != null) {
            throw new IllegalArgumentException("Trying to overwrite a value for property " + property);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <V extends Value> boolean setAllowControlledOverwrite(Property property, V value) {
        assert value != null : "Not allowed to write null";
        V current = (V) map.get(property);
        if (current == null) {
            map.put(property, value);
            return true;
        }
        if (!current.equals(value)) {
            if (current.overwriteAllowed(value)) {
                map.put(property, value);
                return true;
            }
            throw new UnsupportedOperationException("Trying to overwrite " + current + " with "
                                                    + value + " for property " + property);
        }
        return false;
    }

    @Override
    public synchronized <V extends Value> boolean overwrite(Property property, V value) {
        assert value != null : "Not allowed to write null";
        assert property.classOfValue().isAssignableFrom(value.getClass());
        Value prev = map.put(property, value);
        return !value.equals(prev);
    }

    @Override
    public void setAll(PropertyValueMap analysis) {
        analysis.propertyValueStream().forEach(pv -> set(pv.property(), pv.value()));
    }

    @Override
    public synchronized void removeIf(java.util.function.Predicate<Property> filter) {
        map.keySet().removeIf(filter);
    }

    @Override
    public synchronized boolean isEmpty() {
        return map.isEmpty();
    }
}
