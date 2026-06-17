package org.e2immu.language.java.openjdk;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

public class InfoByFqn {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfoByFqn.class);

    private final Map<String, TypeInfo> singleTypeByFqn = new HashMap<>();
    private final Map<String, List<TypeInfo>> multiTypeByFqn = new HashMap<>();

    void put(String fqn, TypeInfo typeInfo, SourceSet sourceSetOfCurrentTask) {
        TypeInfo prev = singleTypeByFqn.put(fqn, typeInfo);
        List<TypeInfo> list;
        // NOTE that the sourceSet can be null, when the type was not properly loaded
        if (sourceSetOfCurrentTask.equals(typeInfo.compilationUnit().sourceSet())) {
            if (prev != null) {
                if (!prev.compilationUnit().sourceSet().equals(typeInfo.compilationUnit().sourceSet())) {
                    singleTypeByFqn.remove(fqn);
                    multiTypeByFqn.put(fqn, List.of(prev, typeInfo));
                    LOGGER.info("Create multi: {}", typeInfo.descriptor());
                } else {
                    throw new UnsupportedOperationException("Duplicating type " + typeInfo);
                }
            } else if ((list = multiTypeByFqn.get(fqn)) != null) {
                multiTypeByFqn.put(fqn, Stream.concat(list.stream(), Stream.of(typeInfo)).toList());
                LOGGER.info("Appended to multi: {}", typeInfo.descriptor());
                // } else {
                //   LOGGER.info("Put in single (==): {}", typeInfo.descriptor());
            }
        } else if (prev != null) {
            LOGGER.info("Overwriting type {}, {} -> {}", typeInfo,
                    prev.compilationUnit().sourceSet(), typeInfo.compilationUnit().sourceSet());
            assert !prev.compilationUnit().sourceSet().equals(typeInfo.compilationUnit().sourceSet());
            //} else {
            //  LOGGER.info("Put in single (!=): {}", typeInfo.descriptor());
        }
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
