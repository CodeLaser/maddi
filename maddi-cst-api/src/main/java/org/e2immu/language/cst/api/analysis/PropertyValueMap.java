package org.e2immu.language.cst.api.analysis;

import org.e2immu.language.cst.api.info.InfoMap;

import java.util.function.Supplier;
import java.util.stream.Stream;

public interface PropertyValueMap {

    boolean isEmpty();

    PropertyValueMap rewire(InfoMap infoMap);

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

    void setAll(PropertyValueMap analysis);
}
