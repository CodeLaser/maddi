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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    Null until the first write, then a packed array of alternating keys and values (at most ARRAY_MAX entries),
    then a HashMap. Most maps are never written: on closed-core, preparation left 57M of them, 51M empty. In the
    modification phase most written maps hold 2-3 properties: a HashMap with its 16-slot table and nodes was ~185
    bytes each, 48M of them ~9 GB; the array is 32-48 bytes. One field, not two: a second reference would add 8
    bytes to each of the 57M maps.
    The array iterates in insertion order. The HashMap's order came from PropertyImpl's identity hash, so it
    already differed between runs: nothing can depend on it. Every access holds the monitor.
     */
    private static final int ARRAY_MAX = 8;
    private Object data;

    private static int arraySize(Object[] kv) {
        int n = 0;
        while (2 * n < kv.length && kv[2 * n] != null) n++;
        return n;
    }

    private Value get(Object property) {
        if (data == null) return null;
        if (data instanceof Object[] kv) {
            for (int i = 0; i < kv.length && kv[i] != null; i += 2) {
                if (kv[i].equals(property)) return (Value) kv[i + 1];
            }
            return null;
        }
        return asMap().get(property);
    }

    @SuppressWarnings("unchecked")
    private Map<Property, Value> asMap() {
        return (Map<Property, Value>) data;
    }

    /** put, returning the previous value or null */
    private Value put(Property property, Value value) {
        if (data == null) {
            data = new Object[]{property, value, null, null};
            return null;
        }
        if (data instanceof Object[] kv) {
            int n = arraySize(kv);
            for (int i = 0; i < 2 * n; i += 2) {
                if (kv[i].equals(property)) {
                    Value prev = (Value) kv[i + 1];
                    kv[i + 1] = value;
                    return prev;
                }
            }
            if (n == ARRAY_MAX) {
                Map<Property, Value> map = new HashMap<>();
                forEach(map::put);
                map.put(property, value);
                data = map;
                return null;
            }
            Object[] target = kv;
            if (2 * n == kv.length) {
                target = new Object[kv.length + 4];
                System.arraycopy(kv, 0, target, 0, kv.length);
                data = target;
            }
            target[2 * n] = property;
            target[2 * n + 1] = value;
            return null;
        }
        return asMap().put(property, value);
    }

    private void forEach(java.util.function.BiConsumer<Property, Value> consumer) {
        if (data == null) return;
        if (data instanceof Object[] kv) {
            for (int i = 0; i < kv.length && kv[i] != null; i += 2) consumer.accept((Property) kv[i], (Value) kv[i + 1]);
        } else {
            asMap().forEach(consumer);
        }
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
        forEach((key, value) -> {
            if (filter.test(key)) rewiredMap.set(key, value.rewire(infoMap));
        });
        return rewiredMap;
    }

    @Override
    public synchronized Stream<PropertyValue> propertyValueStream() {
        // snapshot under the lock; the returned stream is safe to consume without it
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.readAll();
        List<PropertyValue> list = new ArrayList<>();
        forEach((key, value) -> list.add(new PropertyValue(key, value)));
        return list.stream();
    }

    @Override
    public synchronized boolean haveAnalyzedValueFor(Property property) {
        Value v = get(property);
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.read(this, property, v);
        return v != null;
    }

    /** the current value, without recording the access (AnalysisAccessRecorder's validation) */
    @Override
    public synchronized Value peek(Object property) {
        return get(property);
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <V extends Value> V getOrDefault(Property property, V defaultValue) {
        assert defaultValue != null;
        Value v = get(property);
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.read(this, property, v);
        return v == null ? defaultValue : (V) v;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <V extends Value> V getOrNull(Property property, Class<? extends V> clazz) {
        Value v = get(property);
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.read(this, property, v);
        return (V) v;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <V extends Value> V getOrCreate(Property property, Supplier<V> computeValue) {
        V v = (V) get(property);
        if (v != null) {
            if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.read(this, property, v);
            return v;
        }
        V vv = computeValue.get();
        if (vv != null) {
            if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.write(this, property);
            put(property, vv);
        }
        return vv;
    }

    @Override
    public synchronized void set(Property property, Value value) {
        assert value != null : "Not allowed to write null";

        assert property.classOfValue().isAssignableFrom(value.getClass());
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.write(this, property);
        if (put(property, value) != null) {
            throw new IllegalArgumentException("Trying to overwrite a value for property " + property);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <V extends Value> boolean setAllowControlledOverwrite(Property property, V value) {
        assert value != null : "Not allowed to write null";
        V current = (V) get(property);
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.read(this, property, current);
        if (current == null) {
            if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.write(this, property);
            put(property, value);
            return true;
        }
        if (!current.equals(value)) {
            if (current.overwriteAllowed(value)) {
                if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.write(this, property);
                put(property, value);
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
        Value prev = put(property, value);
        return !value.equals(prev);
    }

    @Override
    public void setAll(PropertyValueMap analysis) {
        analysis.propertyValueStream().forEach(pv -> set(pv.property(), pv.value()));
    }

    @Override
    public synchronized void removeIf(Predicate<Property> filter) {
        if (data == null) return;
        Predicate<Property> f = AnalysisAccessRecorder.active() ? p -> {
            boolean remove = filter.test(p);
            if (remove) AnalysisAccessRecorder.write(this, p);
            return remove;
        } : filter;
        if (data instanceof Object[] kv) {
            // compacting keeps the survivors in insertion order
            int w = 0;
            for (int r = 0; r < kv.length && kv[r] != null; r += 2) {
                if (!f.test((Property) kv[r])) {
                    kv[w] = kv[r];
                    kv[w + 1] = kv[r + 1];
                    w += 2;
                }
            }
            for (int i = w; i < kv.length; i++) kv[i] = null;
            if (w == 0) data = null;
        } else {
            asMap().keySet().removeIf(f);
        }
    }

    @Override
    public synchronized boolean isEmpty() {
        if (AnalysisAccessRecorder.active()) AnalysisAccessRecorder.readAll();
        return data == null || data instanceof Map<?, ?> m && m.isEmpty();
    }
}
