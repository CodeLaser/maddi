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
