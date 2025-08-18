package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;

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
