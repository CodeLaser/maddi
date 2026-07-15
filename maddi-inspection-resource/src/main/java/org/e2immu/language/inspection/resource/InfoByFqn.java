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
import java.util.function.Predicate;
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

    // The class scanner's "type setup block" (access, type parameters, parent class, interfaces, annotations) must
    // run exactly once per TypeInfo, EVER -- not once per scanner. Each parse() builds a fresh ClassSymbolScanner,
    // but TypeInfo instances (and their still-open builders) are shared through this registry: a type left LAZILY-
    // loaded-but-uncommitted by one scanner would otherwise have its whole setup re-run by the next scanner (a later
    // parse's on-demand bytecode load), appending its interfaces/annotations a second time and tripping the "Extending
    // multiple identical interfaces" assertion in commit(). Persisting the guard here, alongside the shared TypeInfo
    // instances it keys on, makes the double-run impossible across scanner instances. Identity-keyed, like the scanner's
    // other guards. Source entries are cleared with the source TypeInfos in removeAllSources() (they are re-created with
    // fresh identities on re-parse); library/JDK entries persist for the inspector's lifetime, as those types do.
    private final Set<TypeInfo> classScannerSetupDone = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Returns true the first time this type's class-scanner setup runs; false on every later attempt (skip it). */
    public boolean markClassScannerSetupDone(TypeInfo typeInfo) {
        return classScannerSetupDone.add(typeInfo);
    }

    public void removeAllSources() {
        singleTypeByFqn.values().removeIf(InfoByFqn::isSourceType);
        multiTypeByFqn.values().forEach(list -> list.removeIf(InfoByFqn::isSourceType));
        multiTypeByFqn.values().removeIf(List::isEmpty);
        classScannerSetupDone.removeIf(InfoByFqn::isSourceType);
    }

    /**
     * Forget one source primary type and everything registered under it, so that a re-parse of its compilation unit
     * builds fresh {@code TypeInfo} objects rather than finding (and reusing) the stale ones. The counterpart of
     * {@link #removeAllSources()} for a single type; see {@code JavaInspector.InvalidationState#INVALID}.
     * <p>
     * "Under it" is decided by {@link #belongsTo}. The {@code classScannerSetupDone} guard is dropped along with the
     * type, exactly as {@code removeAllSources} does — the entries are identity-keyed on objects nobody will use again.
     */
    public void removeType(TypeInfo primaryType) {
        Predicate<TypeInfo> belongsToIt = belongsTo(primaryType);
        singleTypeByFqn.values().removeIf(belongsToIt);
        for (String fqn : List.copyOf(multiTypeByFqn.keySet())) {
            // rebuild: the lists in this map are immutable (List.of/Stream.toList), so removeIf would throw
            List<TypeInfo> remaining = multiTypeByFqn.get(fqn).stream().filter(ti -> !belongsToIt.test(ti)).toList();
            if (remaining.size() == multiTypeByFqn.get(fqn).size()) continue; // nothing of ours in there
            if (remaining.isEmpty()) multiTypeByFqn.remove(fqn);
            else if (remaining.size() == 1) {
                multiTypeByFqn.remove(fqn);
                singleTypeByFqn.put(fqn, remaining.getFirst());
            } else multiTypeByFqn.put(fqn, remaining);
        }
        classScannerSetupDone.removeIf(belongsToIt);
        loadedForThisSourceSet.removeIf(belongsToIt);
    }

    /**
     * Does this type belong to the given primary type? Asked of the type itself via {@link TypeInfo#primaryType()},
     * which walks up through enclosing types <em>and enclosing methods</em> — so it also claims the anonymous types a
     * compilation unit registers ({@code a.b.C.$0}), which {@code recursiveSubTypeStream()} does not list and which,
     * left behind, make the re-scan of a source set throw "Duplicating type".
     * <p>
     * No separate source-set test: {@code TypeInfo} equality is fqn + source set, so comparing primary types already
     * spares a same-named type in another source set.
     */
    private static Predicate<TypeInfo> belongsTo(TypeInfo primaryType) {
        return typeInfo -> primaryType.equals(typeInfo.primaryType());
    }

    /**
     * Point this registry at the rewired copy of one type: same FQN, same source set, new object (see {@code
     * InfoMap}'s "BASIC RULE OF REWIRING"). Registers it even when it was absent, so a type that did not resolve
     * before still does.
     * <p>
     * One type, not a type and its subtypes: the caller passes every type the rewire produced
     * ({@code InfoMap.rewiredTypes()}), which is the only complete list — anonymous classes, local classes and
     * lambdas are rewired too, and none of them is among a type's {@code subTypes()}.
     */
    public void replaceType(TypeInfo typeInfo) {
        String fqn = typeInfo.fullyQualifiedName();
        List<TypeInfo> multi = multiTypeByFqn.get(fqn);
        if (multi == null) {
            singleTypeByFqn.put(fqn, typeInfo);
        } else {
            SourceSet sourceSet = typeInfo.compilationUnit().sourceSet();
            multiTypeByFqn.put(fqn, multi.stream().map(m -> sameSourceSet(m, sourceSet) ? typeInfo : m).toList());
        }
    }

    private static boolean sameSourceSet(TypeInfo typeInfo, SourceSet sourceSet) {
        SourceSet ss = typeInfo.compilationUnit().sourceSet();
        return ss == null ? sourceSet == null : ss.equals(sourceSet);
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
