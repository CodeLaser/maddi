package org.e2immu.language.java.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

import java.util.*;
import java.util.stream.Stream;

public class InfoByFqn {
    private final Map<String, TypeInfo> singleTypeByFqn = new HashMap<>();
    private final Map<String, List<TypeInfo>> multiTypeByFqn = new HashMap<>();
    private final Map<String, MethodInfo> methodByDescriptor = new HashMap<>();
    private final Map<String, FieldInfo> fieldByDescriptor = new HashMap<>();

    void put(FieldInfo fieldInfo) {
        fieldByDescriptor.put(fieldInfo.descriptor(), fieldInfo);
    }

    void put(MethodInfo methodInfo) {
        methodByDescriptor.put(methodInfo.descriptor(), methodInfo);
    }

    // works for anonymous types
    void put(String fqn, TypeInfo typeInfo, SourceSet sourceSetOfCurrentTask) {
        TypeInfo prev = singleTypeByFqn.put(fqn, typeInfo);
        List<TypeInfo> list;
        // NOTE that the sourceSet can be null, when the type was not properly loaded
        if (sourceSetOfCurrentTask.equals(typeInfo.compilationUnit().sourceSet())) {
            if (prev != null) {
                if (!prev.compilationUnit().sourceSet().equals(typeInfo.compilationUnit().sourceSet())) {
                    singleTypeByFqn.remove(fqn);
                    multiTypeByFqn.put(fqn, List.of(prev, typeInfo));
                } else {
                    throw new UnsupportedOperationException("Duplicating type " + typeInfo);
                }
            } else if ((list = multiTypeByFqn.get(fqn)) != null) {
                multiTypeByFqn.put(fqn, Stream.concat(list.stream(), Stream.of(typeInfo)).toList());
            } // else: not a problem, first time
        } else if (prev != null) {
            throw new UnsupportedOperationException("Duplicating type " + typeInfo);
        }
    }

    void merge(InfoByFqn other) {

    }

    TypeInfo getType(String fullyQualifiedName, SourceSet sourceSetOfCurrentTask) {
        TypeInfo ti = singleTypeByFqn.get(fullyQualifiedName);
        if (ti != null) return ti;
        List<TypeInfo> multi = multiTypeByFqn.get(fullyQualifiedName);
        if (multi == null) return null;
        assert multi.size() >= 2;
        return multi.stream()
                .map(m -> distance(m, sourceSetOfCurrentTask, m.compilationUnit().sourceSet()))
                .sorted()
                .findFirst()
                .map(ClassSymbolScanner.TypeDistance::typeInfo)
                .orElseThrow();
    }

    MethodInfo getMethod(String methodDescriptor) {
        return methodByDescriptor.get(methodDescriptor);
    }

    FieldInfo getField(String fieldDescriptor) {
        return fieldByDescriptor.get(fieldDescriptor);
    }

    public Collection<TypeInfo> typesLoaded() {
        return Stream.concat(singleTypeByFqn.values().stream(),
                        multiTypeByFqn.values().stream().flatMap(Collection::stream))
                .toList();
    }

    private ClassSymbolScanner.TypeDistance distance(TypeInfo typeInfo,
                                                     SourceSet sourceSetOfCurrentTask,
                                                     SourceSet sourceSet) {
        int distance;
        if (sourceSetOfCurrentTask.equals(sourceSet)) {
            distance = -2; // top
        } else if (sourceSet.partOfJdk()) {
            distance = 1000; // bottom
        } else {
            distance = computeDistance(sourceSetOfCurrentTask, sourceSet);
        }
        return new ClassSymbolScanner.TypeDistance(typeInfo, distance);
    }

    private record SourceSetDistance(SourceSet sourceSet, int distance) {
    }

    private static int computeDistance(SourceSet sourceSetOfCurrentTask, SourceSet target) {
        // Queue holds pairs of [Current SourceSet, Current Distance]
        Queue<SourceSetDistance> queue = new LinkedList<>();
        // Set to track visited nodes and prevent infinite cycles
        Set<SourceSet> visited = new HashSet<>();

        // Initialize the search
        queue.add(new SourceSetDistance(sourceSetOfCurrentTask, 0));
        visited.add(sourceSetOfCurrentTask);

        while (!queue.isEmpty()) {
            SourceSetDistance currentPair = queue.poll();
            SourceSet sourceSet = currentPair.sourceSet;
            int currentDistance = currentPair.distance;
            // Explore all immediate dependencies
            for (SourceSet dependency : sourceSet.dependencies()) {
                if (dependency.equals(target)) {
                    return currentDistance + 1;
                }
                if (visited.add(dependency)) {
                    queue.add(new SourceSetDistance(dependency, currentDistance + 1));
                }
            }
        }
        // Target was never reached
        return Integer.MAX_VALUE;
    }

}
