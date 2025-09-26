package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.inspection.api.parser.TypeMap;

import java.util.*;
import java.util.stream.Stream;

public class TypeMapImpl implements TypeMap {
    private final TreeMap<String, Object> map = new TreeMap<>();

    @Override
    public synchronized void put(TypeInfo typeInfo) {
        String fqn = typeInfo.fullyQualifiedName();
        Object prev = map.put(fqn, typeInfo);
        if (prev instanceof TypeInfo prevTi) {
            SourceSet prevSet = prevTi.compilationUnit().sourceSet();
            SourceSet set = typeInfo.compilationUnit().sourceSet();
            if (!prevSet.equals(set)) {
                Map<SourceSet, TypeInfo> newMap = new HashMap<>();
                newMap.put(prevSet, prevTi);
                newMap.put(set, typeInfo);
                map.put(fqn, newMap);
            }
        } else if (prev instanceof Map<?, ?> map1) {
            //noinspection ALL
            Map<SourceSet, TypeInfo> localMap = (Map<SourceSet, TypeInfo>) map1;
            localMap.put(typeInfo.compilationUnit().sourceSet(), typeInfo);
            map.put(fqn, map1);
        } else {
            assert prev == null;
        }
    }

    public synchronized void putAll(Map<String, TypeInfo> map) {
        map.values().forEach(this::put);
    }

    @Override
    public synchronized void invalidate(TypeInfo typeInfo) {
        String fqn = typeInfo.fullyQualifiedName();
        Object prev = map.remove(fqn);
        if (prev instanceof Map<?, ?> map1) {
            //noinspection ALL
            Map<SourceSet, TypeInfo> localMap = (Map<SourceSet, TypeInfo>) map1;
            localMap.remove(typeInfo.compilationUnit().sourceSet());
            if (localMap.size() == 1) {
                map.put(fqn, localMap.values().stream().findFirst().orElseThrow());
            } else if (!localMap.isEmpty()) {
                map.put(fqn, localMap);
            }
        }
    }

    @Override
    public synchronized TypeInfo get(String fullyQualifiedName, SourceSet sourceSetOfRequest) {
        Object o = map.get(fullyQualifiedName);
        if (o instanceof TypeInfo ti) {
            // there is only one; because the code compiles, there can be no doubt
            return ti;
        }
        if (o instanceof Map<?, ?> map1) {
            //noinspection ALL
            Map<SourceSet, TypeInfo> map = (Map<SourceSet, TypeInfo>) map1;
            return sourceSetOfRequest.recursiveDependenciesSameExternal().stream()
                    .map(map::get)
                    .filter(Objects::nonNull).findFirst().orElse(null);
        }
        if (o != null) throw new UnsupportedOperationException();
        return null;
    }

    @Override
    public synchronized List<TypeInfo> primaryTypesInPackage(String packageName) {
        List<TypeInfo> result = new LinkedList<>();
        Map.Entry<String, Object> lower = map.ceilingEntry(packageName);
        while (lower != null && lower.getKey().startsWith(packageName)) {
            for (TypeInfo typeInfo : typesInObject(lower.getValue())) {
                if (typeInfo.isPrimaryType() && packageName.equals(typeInfo.packageName())) {
                    result.add(typeInfo);
                }
            }
            lower = map.higherEntry(lower.getKey());
        }
        return result;
    }

    private Iterable<TypeInfo> typesInObject(Object value) {
        if (value instanceof TypeInfo ti) return List.of(ti);
        if (value instanceof Map<?, ?> map1) {
            //noinspection ALL
            Map<SourceSet, TypeInfo> map = (Map<SourceSet, TypeInfo>) map1;
            return map.values();
        }
        throw new UnsupportedOperationException();
    }

    public TreeMap<String, Object> getMap() {
        return map;
    }

    @Override
    public synchronized Stream<TypeInfo> typeStream() {
        return map.values().stream().flatMap(object -> {
            if (object instanceof TypeInfo ti) return Stream.of(ti);
            if (object instanceof Map<?, ?> map1) {
                //noinspection ALL
                Map<SourceSet, TypeInfo> map = (Map<SourceSet, TypeInfo>) map1;
                return map.values().stream();
            }
            throw new UnsupportedOperationException();
        });
    }
}
