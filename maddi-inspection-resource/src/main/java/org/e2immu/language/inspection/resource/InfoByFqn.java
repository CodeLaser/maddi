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

package org.e2immu.language.inspection.resource;

import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

/**
 * The shared type registry: fully-qualified name -> {@link TypeInfo}, with multi-source-set resolution
 * (when the same FQN exists in several source sets, {@link #getType} returns the one nearest in the
 * source-set dependency graph). It enforces the single-instance invariant per (FQN, source set)
 * (see the {@code Info} javadoc).
 * <p>
 * Promoted out of the openjdk parser so that every language front-end (the javac-based Java parser and
 * the K2-based Kotlin parser) can thread the same instance and thereby share one {@code TypeInfo} per
 * type across languages.
 */
public class InfoByFqn {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfoByFqn.class);

    private final Map<String, TypeInfo> singleTypeByFqn = new HashMap<>();
    private final Map<String, List<TypeInfo>> multiTypeByFqn = new HashMap<>();

    private final Set<TypeInfo> loadedForThisSourceSet = new HashSet<>();

    public void removeAllSources() {
        singleTypeByFqn.values().removeIf(InfoByFqn::isSourceType);
        multiTypeByFqn.values().forEach(list -> list.removeIf(InfoByFqn::isSourceType));
        multiTypeByFqn.values().removeIf(List::isEmpty);
    }

    private static boolean isSourceType(TypeInfo ti) {
        return ti.compilationUnit().sourceSet() != null && !ti.compilationUnit().sourceSet().externalLibrary();
    }

    public void startOfNewSourceSet() {
        loadedForThisSourceSet.clear();
    }

    public void put(String fqn, TypeInfo typeInfo, SourceSet sourceSetOfCurrentTask) {
        loadedForThisSourceSet.add(typeInfo);
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
            }
        } else if (prev != null) {
            LOGGER.info("Overwriting type {}, {} -> {}", typeInfo,
                    prev.compilationUnit().sourceSet(), typeInfo.compilationUnit().sourceSet());
            assert !prev.compilationUnit().sourceSet().equals(typeInfo.compilationUnit().sourceSet());
            // all sub-types must be removed as well:
            // TODO would a TreeMap be more efficient here? We'd have to balance
            String prefix = typeInfo.fullyQualifiedName() + ".";
            singleTypeByFqn.keySet().removeIf(key -> key.startsWith(prefix));
            multiTypeByFqn.keySet().removeIf(key -> key.startsWith(prefix));
        }
    }

    public TypeInfo getType(String fullyQualifiedName, SourceSet sourceSetOfCurrentTask) {
        TypeInfo ti = singleTypeByFqn.get(fullyQualifiedName);
        if (ti != null) return ti;
        List<TypeInfo> multi = multiTypeByFqn.get(fullyQualifiedName);
        if (multi == null) return null;
        assert multi.size() >= 2;
        return multi.stream()
                .map(m -> distance(m, sourceSetOfCurrentTask, m.compilationUnit().sourceSet()))
                .sorted()
                .findFirst()
                .map(TypeDistance::typeInfo)
                .orElseThrow();
    }

    public Collection<TypeInfo> typesLoadedForThisSourceSet() {
        return loadedForThisSourceSet;
    }

    public record TypeDistance(TypeInfo typeInfo, int distance) implements Comparable<TypeDistance> {
        @Override
        public int compareTo(TypeDistance o) {
            int c = Integer.compare(distance, o.distance);
            if (c != 0) return c;
            return typeInfo.compilationUnit().sourceSet().name()
                    .compareTo(o.typeInfo.compilationUnit().sourceSet().name());
        }
    }

    private TypeDistance distance(TypeInfo typeInfo,
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
        return new TypeDistance(typeInfo, distance);
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
