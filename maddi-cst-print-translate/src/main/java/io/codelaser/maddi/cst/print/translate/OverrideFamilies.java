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
package io.codelaser.maddi.cst.print.translate;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

/**
 * The override families of a program's methods: the methods that override one another, transitively, as one set.
 * A family is linked by Java's {@link MethodInfo#overrides()}, and by a superclass's method that implements a
 * subclass's interface method ({@link #inheritedImplementations}: {@code class C extends B implements I}, where
 * {@code B.d(List)} implements {@code I.d(List)} without overriding it), which {@code overrides()} does not link. A
 * family reaches the methods outside the types given that one of them overrides.
 * <p>
 * A Java-to-X printer decides a family's members together, where the target wants an override's signature exactly
 * the overridden member's: a read-only collection parameter, a property. Each printer decides for itself which
 * families count, by what it translates and what a lambda implements.
 */
public final class OverrideFamilies {

    private final Map<MethodInfo, List<MethodInfo>> byMethod;

    private OverrideFamilies(Map<MethodInfo, List<MethodInfo>> byMethod) {
        this.byMethod = byMethod;
    }

    /** The families of the methods of {@code types}: every type to look in, the nested, local and anonymous ones too. */
    public static OverrideFamilies of(Collection<TypeInfo> types) {
        // union-find: each method to one of its family, up to the family's root
        Map<MethodInfo, MethodInfo> parent = new HashMap<>();
        Set<MethodInfo> members = new HashSet<>();
        for (TypeInfo t : types) {
            for (MethodInfo m : t.methods()) {
                if (m.isStatic()) continue;
                for (MethodInfo o : m.overrides()) {
                    if (o == m) continue;
                    members.add(m);
                    members.add(o);
                    parent.put(root(parent, m), root(parent, o));
                }
            }
            inheritedImplementations(t).forEach((m, x) -> {
                members.add(m);
                members.add(x);
                parent.put(root(parent, x), root(parent, m));
            });
        }
        Map<MethodInfo, List<MethodInfo>> byRoot = new HashMap<>();
        for (MethodInfo m : members) byRoot.computeIfAbsent(root(parent, m), k -> new ArrayList<>()).add(m);
        Map<MethodInfo, List<MethodInfo>> byMethod = new HashMap<>();
        for (List<MethodInfo> family : byRoot.values()) {
            List<MethodInfo> members2 = List.copyOf(family);
            members2.forEach(m -> byMethod.put(m, members2));
        }
        return new OverrideFamilies(byMethod);
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

    /** The family of {@code m}: {@code m} alone when nothing overrides it and it overrides nothing. */
    public List<MethodInfo> family(MethodInfo m) {
        List<MethodInfo> family = byMethod.get(m);
        return family == null ? List.of(m) : family;
    }

    /** The families of two or more. */
    public List<List<MethodInfo>> families() {
        Set<List<MethodInfo>> distinct = new HashSet<>();
        List<List<MethodInfo>> families = new ArrayList<>();
        for (List<MethodInfo> family : byMethod.values()) {
            if (distinct.add(family)) families.add(family);
        }
        return families;
    }

    /**
     * Per interface method of {@code cls}, the superclass's method that implements it without overriding it in Java's
     * {@code overrides()}: a superclass's method that implements a subclass's interface. Empty for an interface, and
     * for a class that implements none.
     */
    public static Map<MethodInfo, MethodInfo> inheritedImplementations(TypeInfo cls) {
        if (cls.isInterface() || cls.interfacesImplemented().isEmpty()) return Map.of();
        List<MethodInfo> inherited = CstQueries.superclasses(cls).stream().flatMap(t -> t.methods().stream()).toList();
        if (inherited.isEmpty()) return Map.of();
        Set<TypeInfo> interfaces = new HashSet<>();
        Deque<TypeInfo> todo = new ArrayDeque<>();
        cls.interfacesImplemented().forEach(i -> { if (i.typeInfo() != null) todo.add(i.typeInfo()); });
        while (!todo.isEmpty()) {
            TypeInfo iface = todo.poll();
            if (!interfaces.add(iface)) continue;
            iface.interfacesImplemented().forEach(i -> { if (i.typeInfo() != null) todo.add(i.typeInfo()); });
        }
        Map<MethodInfo, MethodInfo> found = new HashMap<>();
        for (TypeInfo iface : interfaces) {
            for (MethodInfo m : iface.methods()) {
                if (m.isStatic() || !m.isAbstract()) continue;
                inherited.stream().filter(x -> inheritedImplementation(x, m)).findFirst()
                        .ifPresent(x -> found.put(m, x));
            }
        }
        return found;
    }

    /**
     * Class method {@code x} has interface method {@code m}'s signature, without overriding it in Java's
     * {@code overrides()}: a superclass's method that implements a subclass's interface. A type parameter matches any
     * type.
     */
    public static boolean inheritedImplementation(MethodInfo x, MethodInfo m) {
        if (x == m || x.typeInfo().isInterface() || x.isStatic() || x.isConstructor() || !x.name().equals(m.name())
            || x.parameters().size() != m.parameters().size() || x.overrides().contains(m)) {
            return false;
        }
        for (int i = 0; i < x.parameters().size(); i++) {
            ParameterizedType a = x.parameters().get(i).parameterizedType();
            ParameterizedType b = m.parameters().get(i).parameterizedType();
            if (a.arrays() != b.arrays()) return false;
            if (a.isTypeParameter() || b.isTypeParameter()) continue;
            if (a.typeInfo() == null || !a.typeInfo().equals(b.typeInfo())) return false;
        }
        return true;
    }
}
