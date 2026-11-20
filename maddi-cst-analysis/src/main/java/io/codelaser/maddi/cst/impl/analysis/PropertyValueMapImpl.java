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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.PropertyValueMap;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.InfoMapView;

/*
Does not use SetOnceMap<> because of controlled overwrites.

All access is synchronized on 'this': analyzers write properties of OTHER elements (a method call writes
UNMODIFIED_PARAMETER on the callee's parameters), so under intra-iteration parallelism two threads can hit
the same element's map. External compound check-then-act sequences (TolerantWrite) synchronize on the map
object as well — same monitor.
 */
public class PropertyValueMapImpl implements PropertyValueMap, AnalysisAccessRecorder.Owner {
    /*
    Null until the first write. Most maps are never written: on closed-core, preparation left 57M of them, 51M empty;
    an empty HashMap is 40 bytes each, about 2 GB of heap. Every access holds the monitor, so creating it is safe.
     */
    private Map<Property, Value> map;

    private Map<Property, Value> read() {
        return map == null ? Collections.emptyMap() : map;
    }

    private Map<Property, Value> write() {
        if (map == null) map = new HashMap<>();
        return map;
    }

    /**
     * Carries only the properties that declare {@link Property#carryOnRewire()}; the rest are dropped, which for
     * anything derived across types is the correct answer rather than a lossy one (see {@code docs/design/rewiring.md}).
     * Applies to expressions as much as to Info: a stale value is no more welcome inside a rewired MethodCall than
     * on a rewired method.
     * <p>
     * The intrinsic tier only when the rewire asks for it ({@link InfoMapView#carryIntrinsic()}, a partial re-parse).
     */
    @Override
    public synchronized PropertyValueMap rewire(InfoMapView infoMap) {
        boolean intrinsic = infoMap.carryIntrinsic();
        return rewire(infoMap, p -> p.carryOnRewire()
                                    && (intrinsic || p.analysisTier() != Property.AnalysisTier.INTRINSIC));
    }

    // filtered carry (docs/design/analysis-rewiring.md): the fingerprint-gated skip passes the analyzer-output predicate
    @Override
    public synchronized PropertyValueMap rewire(InfoMapView infoMap, Predicate<Property> filter) {
        PropertyValueMapImpl rewiredMap = new PropertyValueMapImpl();
        read().forEach((key, value) -> {
            if (filter.test(key)) rewiredMap.set(key, value.rewire(infoMap));
        });
        return rewiredMap;
    }

    @Override
    public synchronized Stream<PropertyValue> propertyValueStream() {
        // snapshot under the lock; the returned stream is safe to consume without it
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.readAll();
        return read().entrySet().stream().map(e -> new PropertyValue(e.getKey(), e.getValue())).toList().stream();
    }

    @Override
    public synchronized boolean haveAnalyzedValueFor(Property property) {
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.read(this, property, read().get(property));
        return read().containsKey(property);
    }

    /** the current value, without recording the access (AnalysisAccessRecorder's validation) */
    @Override
    public synchronized Value peek(Object property) {
        return read().get(property);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <V extends Value> V getOrDefault(Property property, V defaultValue) {
        assert defaultValue != null;
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.read(this, property, read().get(property));
        return (V) read().getOrDefault(property, defaultValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <V extends Value> V getOrNull(Property property, Class<? extends V> clazz) {
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.read(this, property, read().get(property));
        return (V) read().get(property);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <V extends Value> V getOrCreate(Property property, Supplier<V> computeValue) {
        V v = (V) read().get(property);
        if (v != null) {
            if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.read(this, property, v);
            return v;
        }
        V vv = computeValue.get();
        if (vv != null) {
            if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.write(this, property);
            write().put(property, vv);
        }
        return vv;
    }

    @Override
    public synchronized void set(Property property, Value value) {
        assert value != null : "Not allowed to write null";

        assert property.classOfValue().isAssignableFrom(value.getClass());
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.write(this, property);
        if (write().put(property, value) != null) {
            throw new IllegalArgumentException("Trying to overwrite a value for property " + property);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <V extends Value> boolean setAllowControlledOverwrite(Property property, V value) {
        assert value != null : "Not allowed to write null";
        V current = (V) read().get(property);
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.read(this, property, current);
        if (current == null) {
            if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.write(this, property);
            write().put(property, value);
            return true;
        }
        if (!current.equals(value)) {
            if (current.overwriteAllowed(value)) {
                if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.write(this, property);
                write().put(property, value);
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
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.write(this, property);
        Value prev = write().put(property, value);
        return !value.equals(prev);
    }

    @Override
    public void setAll(PropertyValueMap analysis) {
        analysis.propertyValueStream().forEach(pv -> set(pv.property(), pv.value()));
    }

    @Override
    public synchronized void removeIf(Predicate<Property> filter) {
        if (map == null) return;
        if (AnalysisAccessRecorder.active()) {
            map.keySet().removeIf(p -> {
                boolean remove = filter.test(p);
                if (remove) AnalysisAccessRecorder.write(this, p);
                return remove;
            });
            return;
        }
        map.keySet().removeIf(filter);
    }

    @Override
    public synchronized boolean isEmpty() {
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.readAll();
        return read().isEmpty();
    }
}
