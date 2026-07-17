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

package org.e2immu.language.cst.impl.analysis;

import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.InfoMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyValueMapImpl.class);
    private final Map<Property, Value> map = new HashMap<>();

    /**
     * Carries only the properties that declare {@link Property#carryOnRewire()}; the rest are dropped, which for
     * anything derived across types is the correct answer rather than a lossy one (see {@code rewiring.md}).
     * Applies to expressions as much as to Info: a stale value is no more welcome inside a rewired MethodCall than
     * on a rewired method.
     */
    @Override
    public synchronized PropertyValueMap rewire(InfoMap infoMap) {
        PropertyValueMapImpl rewiredMap = new PropertyValueMapImpl();
        map.forEach((key, value) -> {
            if (key.carryOnRewire()) rewiredMap.set(key, value.rewire(infoMap));
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
    public synchronized boolean isEmpty() {
        return map.isEmpty();
    }
}
