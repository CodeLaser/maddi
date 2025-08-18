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
 */
public class PropertyValueMapImpl implements PropertyValueMap {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyValueMapImpl.class);
    private final Map<Property, Value> map = new HashMap<>();

    @Override
    public PropertyValueMap rewire(InfoMap infoMap) {
        PropertyValueMapImpl rewiredMap = new PropertyValueMapImpl();
        map.forEach((key, value) -> rewiredMap.set(key, value.rewire(infoMap)));
        return rewiredMap;
    }

    @Override
    public Stream<PropertyValue> propertyValueStream() {
        return map.entrySet().stream().map(e -> new PropertyValue(e.getKey(), e.getValue()));
    }

    @Override
    public boolean haveAnalyzedValueFor(Property property) {
        return map.containsKey(property);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V extends Value> V getOrDefault(Property property, V defaultValue) {
        assert defaultValue != null;
        return (V) map.getOrDefault(property, defaultValue);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V extends Value> V getOrNull(Property property, Class<? extends V> clazz) {
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
    public void set(Property property, Value value) {
        assert value != null : "Not allowed to write null";

        assert property.classOfValue().isAssignableFrom(value.getClass());
        if (map.put(property, value) != null) {
            throw new IllegalArgumentException("Trying to overwrite a value for property " + property);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V extends Value> boolean setAllowControlledOverwrite(Property property, V value) {
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
            // FIXME-DEMO this should be an exception thrown
            LOGGER.warn("Trying to overwrite {} with {} for property {}", current, value, property);
        }
        return false;
    }

    @Override
    public void setAll(PropertyValueMap analysis) {
        analysis.propertyValueStream().forEach(pv -> set(pv.property(), pv.value()));
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }
}
