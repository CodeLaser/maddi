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

package io.codelaser.maddi.inspection.api.parser;

import io.codelaser.maddi.cst.api.type.NamedType;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record TypeParameterMap(Map<NamedType, ParameterizedType> map) {

    public TypeParameterMap {
        assert map != null;
    }

    public static final TypeParameterMap EMPTY = new TypeParameterMap();

    private TypeParameterMap() {
        this(Map.of());
    }

    public TypeParameterMap merge(TypeParameterMap other) {
        if (other.isEmpty()) return this;
        if (isEmpty()) return other;
        Map<NamedType, ParameterizedType> newMap = new HashMap<>(map);
        newMap.putAll(other.map);
        if (newMap.size() > 1 && containsCycles(newMap)) {
            return this;
        }
        return new TypeParameterMap(Map.copyOf(newMap));
    }

    private boolean containsCycles(Map<NamedType, ParameterizedType> newMap) {
        for (NamedType start : newMap.keySet()) {
            if (containsCycles(start, newMap)) return true;
        }
        return false;
    }

    private boolean containsCycles(NamedType start, Map<NamedType, ParameterizedType> newMap) {
        Set<NamedType> visited = new HashSet<>();
        NamedType s = start;
        while (s != null) {
            if (!visited.add(s)) {
                return true;
            }
            ParameterizedType t = newMap.get(s);
            if (t != null && t.isTypeParameter()) {
                s = t.typeParameter();
            } else break;
        }
        return false;
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }
}
