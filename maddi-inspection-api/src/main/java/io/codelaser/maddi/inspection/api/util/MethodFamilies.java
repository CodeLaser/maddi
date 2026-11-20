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
package io.codelaser.maddi.inspection.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.parser.ParseResult;

/**
 * The override families of a program: the methods that override one another, transitively, as one set. A change to
 * one member's signature (its name, a parameter, its {@code throws}) is a change to all of them, or to none.
 * <p>
 * {@link MethodInfo#overrides()} is a fact of one method: what it overrides, in its supertypes. A family is a fact of
 * the program: who overrides a method depends on the types looked in, as {@link ParseResult#descendants} does for a
 * type. So this is an index, built once for a set of types, and built again when they change. Two kinds of edges
 * make a family:
 * <ul>
 *     <li>a method and each method of its {@code overrides()}, which is transitive upwards;</li>
 *     <li>an interface's method and the superclass's method that takes its place in a class
 *     ({@link TypeInfo#inheritedImplementations()}: {@code class C extends B implements I}, {@code B.d()} for
 *     {@code I.d()}), which no {@code overrides()} has.</li>
 * </ul>
 * A family reaches the methods outside the types given that one of them overrides (a JDK interface's): see
 * {@link #reachesOutside}. The types given must be all the types there are: a class in a method body that implements
 * the interface overrides too, and is in no {@link TypeInfo#subTypes()}. {@link #of(ParseResult)} and
 * {@link #allTypes} look there.
 * <p>
 * Not here: a lambda or a method reference implements a functional interface's method without a method of its own,
 * and belongs to no family; whoever changes such a method looks for them itself.
 * <p>
 * Every set is in one order, by declaring type, then name, then signature, whatever the member asked for.
 */
public final class MethodFamilies {

    /** By declaring type, then name, then the full signature: the same order on every run. */
    private static final Comparator<MethodInfo> ORDER =
            Comparator.<MethodInfo, String>comparing(m -> m.typeInfo().fullyQualifiedName())
                    .thenComparing(MethodInfo::name)
                    .thenComparing(MethodInfo::fullyQualifiedName);

    private final Map<MethodInfo, Set<MethodInfo>> familyOf;
    private final Map<MethodInfo, Set<MethodInfo>> overridersOf;

    private MethodFamilies(Map<MethodInfo, Set<MethodInfo>> familyOf, Map<MethodInfo, Set<MethodInfo>> overridersOf) {
        this.familyOf = familyOf;
        this.overridersOf = overridersOf;
    }

    /** The families of the program's source types, the nested, local and anonymous ones too. */
    public static MethodFamilies of(ParseResult parseResult) {
        return of(allTypes(parseResult.primaryTypes()));
    }

    /**
     * The families of the methods of {@code types}: exactly these types are looked in, so the caller includes the
     * nested, local and anonymous ones ({@link #allTypes}).
     */
    public static MethodFamilies of(Collection<TypeInfo> types) {
        // union-find: each method to one of its family, up to the family's root
        Map<MethodInfo, MethodInfo> parent = new HashMap<>();
        Set<MethodInfo> members = new LinkedHashSet<>();
        Map<MethodInfo, Set<MethodInfo>> down = new HashMap<>();
        for (TypeInfo t : types) {
            for (MethodInfo m : t.methods()) {
                if (m.isStatic()) continue;
                for (MethodInfo o : m.overrides()) {
                    if (o == m) continue;
                    members.add(m);
                    members.add(o);
                    parent.put(root(parent, m), root(parent, o));
                    down.computeIfAbsent(o, k -> new LinkedHashSet<>()).add(m);
                }
            }
            t.inheritedImplementations().forEach((m, x) -> {
                members.add(m);
                members.add(x);
                parent.put(root(parent, x), root(parent, m));
                down.computeIfAbsent(m, k -> new LinkedHashSet<>()).add(x);
            });
        }
        Map<MethodInfo, List<MethodInfo>> byRoot = new HashMap<>();
        for (MethodInfo m : members) byRoot.computeIfAbsent(root(parent, m), k -> new ArrayList<>()).add(m);
        Map<MethodInfo, Set<MethodInfo>> familyOf = new HashMap<>();
        for (List<MethodInfo> family : byRoot.values()) {
            Set<MethodInfo> ordered = ordered(family);
            ordered.forEach(m -> familyOf.put(m, ordered));
        }
        Map<MethodInfo, Set<MethodInfo>> overridersOf = new HashMap<>();
        down.forEach((m, overriders) -> overridersOf.put(m, ordered(overriders)));
        return new MethodFamilies(familyOf, overridersOf);
    }

    /**
     * The types and every type declared in them: the member types, and the local and anonymous classes of their
     * code, at any depth ({@link TypeInfo#recursiveSubTypeStreamIncludingLocal()}).
     */
    public static List<TypeInfo> allTypes(Collection<TypeInfo> primaryTypes) {
        return primaryTypes.stream().flatMap(TypeInfo::recursiveSubTypeStreamIncludingLocal).distinct().toList();
    }

    private static Set<MethodInfo> ordered(Collection<MethodInfo> methods) {
        List<MethodInfo> sorted = new ArrayList<>(methods);
        sorted.sort(ORDER);
        return Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
    }

    private static MethodInfo root(Map<MethodInfo, MethodInfo> parent, MethodInfo m) {
        MethodInfo root = m;
        for (MethodInfo p = parent.get(root); p != null && p != root; p = parent.get(root)) root = p;
        // the path to the root, shortened for the next time
        for (MethodInfo x = m; x != root; ) {
            MethodInfo next = parent.get(x);
            parent.put(x, root);
            x = next;
        }
        return root;
    }

    /**
     * The family of {@code m}, {@code m} included: {@code m} alone when nothing overrides it and it overrides
     * nothing, as a constructor, a static or a private method.
     */
    public Set<MethodInfo> family(MethodInfo m) {
        Set<MethodInfo> family = familyOf.get(m);
        return family == null ? Set.of(m) : family;
    }

    /** The families of two or more, each once. */
    public List<Set<MethodInfo>> families() {
        Set<Set<MethodInfo>> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Set<MethodInfo>> families = new ArrayList<>();
        // by its first member, as the members are: the same list on every run
        familyOf.values().stream().filter(seen::add)
                .sorted(Comparator.comparing(family -> family.iterator().next(), ORDER)).forEach(families::add);
        return List.copyOf(families);
    }

    /**
     * The methods of the types looked in that a call of {@code m} can run instead of {@code m}: those that override
     * it, at any depth, and the superclass's methods that take its place in a class. Not {@code m} itself. This is
     * the downward half of {@link #family}: the family also has what {@code m} overrides, and from there the other
     * implementations, which a call of {@code m} does not reach.
     */
    public Set<MethodInfo> overriders(MethodInfo m) {
        return overridersOf.getOrDefault(m, Set.of());
    }

    /**
     * A member of {@code m}'s family is declared in a type that is not {@code inside}: with "the program's own
     * source types" for {@code inside}, the family has a declaration that cannot be edited, as a class's
     * {@code close()} that implements {@code java.io.Closeable}'s.
     */
    public boolean reachesOutside(MethodInfo m, Predicate<TypeInfo> inside) {
        return family(m).stream().anyMatch(member -> !inside.test(member.typeInfo()));
    }
}
