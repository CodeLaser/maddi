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
package io.codelaser.maddi.cst.api.info;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.codelaser.maddi.cst.api.type.ParameterizedType;

/**
 * The computation behind {@link TypeInfo#inheritedImplementations()}: for a class, the methods of its superclasses
 * that take the place of its interfaces' abstract and default methods.
 * <p>
 * The match is the Java Language Specification's (§8.4.2, subsignature), as far as compiled code needs it: both
 * signatures are taken as members of the class, that is with the type parameters of the interface and of the
 * superclass replaced by the class's type arguments for them, and then compared by erasure. Two methods of one
 * class cannot differ in their type arguments only (they would have the same erasure), so the erasures of the
 * signatures as members decide. A type parameter that is left (the class's own, a method's, a raw supertype's) is its
 * leftmost bound, {@code Object} without one.
 */
final class InheritedImplementations {

    private InheritedImplementations() {
    }

    /** A supertype, with its type parameters in the terms of the class the computation is for. */
    private record Seen(TypeInfo typeInfo, Map<TypeParameter, ParameterizedType> arguments) {
    }

    static Map<MethodInfo, MethodInfo> of(TypeInfo cls) {
        if (cls.isInterface() || cls.interfacesImplemented().isEmpty()) return Map.of();
        List<Seen> superclasses = superclasses(cls);
        if (superclasses.isEmpty()) return Map.of();
        Set<MethodInfo> overriddenByOwn = new HashSet<>();
        cls.methods().forEach(own -> overriddenByOwn.addAll(own.overrides()));
        Map<MethodInfo, MethodInfo> found = new LinkedHashMap<>();
        for (Seen iface : interfaces(cls)) {
            for (MethodInfo m : iface.typeInfo().methods()) {
                // an abstract method and a default one alike: the superclass's method wins over the default
                // (JLS §8.4.8, a class's method is preferred), and runs in its place
                // the class's own method that overrides it is the implementation
                if (!(m.isAbstract() || m.isDefault()) || overriddenByOwn.contains(m)) continue;
                MethodInfo x = implementation(m, iface, superclasses);
                if (x != null) found.put(m, x);
            }
        }
        return found.isEmpty() ? Map.of() : Map.copyOf(found);
    }

    /** The nearest superclass's method with {@code m}'s signature; one that overrides {@code m} is no inherited one. */
    private static MethodInfo implementation(MethodInfo m, Seen iface, List<Seen> superclasses) {
        List<String> signature = null;
        for (Seen superclass : superclasses) {
            for (MethodInfo x : superclass.typeInfo().methods()) {
                if (x.isStatic() || !x.name().equals(m.name()) || x.parameters().size() != m.parameters().size()) {
                    continue;
                }
                if (signature == null) signature = erasedSignature(m, iface.arguments());
                if (!signature.equals(erasedSignature(x, superclass.arguments()))) continue;
                // the superclass implements the interface itself: Java's overrides() links the two
                return x.overrides().contains(m) ? null : x;
            }
        }
        return null;
    }

    /** The superclasses, nearest first, without {@code java.lang.Object}. */
    private static List<Seen> superclasses(TypeInfo cls) {
        List<Seen> superclasses = new ArrayList<>();
        Set<TypeInfo> visited = new HashSet<>();
        Map<TypeParameter, ParameterizedType> arguments = Map.of();
        for (ParameterizedType parent = cls.parentClass(); parent != null; ) {
            TypeInfo typeInfo = parent.typeInfo();
            if (typeInfo == null || typeInfo.isJavaLangObject() || !visited.add(typeInfo)) break;
            arguments = arguments(parent, arguments);
            superclasses.add(new Seen(typeInfo, arguments));
            parent = typeInfo.parentClass();
        }
        return superclasses;
    }

    /** The interfaces the class declares, and the interfaces they extend. Not the superclasses': those are theirs. */
    private static List<Seen> interfaces(TypeInfo cls) {
        List<Seen> interfaces = new ArrayList<>();
        Set<TypeInfo> visited = new HashSet<>();
        Deque<Seen> todo = new ArrayDeque<>();
        todo.add(new Seen(cls, Map.of()));
        while (!todo.isEmpty()) {
            Seen from = todo.poll();
            for (ParameterizedType i : from.typeInfo().interfacesImplemented()) {
                if (i.typeInfo() == null || !visited.add(i.typeInfo())) continue;
                Seen seen = new Seen(i.typeInfo(), arguments(i, from.arguments()));
                interfaces.add(seen);
                todo.add(seen);
            }
        }
        return interfaces;
    }

    /**
     * The type arguments of {@code supertype}, written in a subtype in that subtype's type parameters, in the terms
     * of the class: {@code inSubtype} are the subtype's. Empty for a raw supertype.
     */
    private static Map<TypeParameter, ParameterizedType> arguments(ParameterizedType supertype,
                                                                   Map<TypeParameter, ParameterizedType> inSubtype) {
        List<TypeParameter> formal = supertype.typeInfo().typeParameters();
        List<ParameterizedType> actual = supertype.parameters();
        if (formal.isEmpty() || actual.size() != formal.size()) return Map.of();
        Map<TypeParameter, ParameterizedType> map = new HashMap<>();
        for (int i = 0; i < formal.size(); i++) {
            ParameterizedType a = actual.get(i);
            ParameterizedType known = a.typeParameter() == null ? null : inSubtype.get(a.typeParameter());
            map.put(formal.get(i), known == null ? a : a.arrays() == 0 ? known
                    : known.copyWithArrays(known.arrays() + a.arrays()));
        }
        return map;
    }

    private static List<String> erasedSignature(MethodInfo method, Map<TypeParameter, ParameterizedType> arguments) {
        List<String> erased = new ArrayList<>(method.parameters().size());
        for (ParameterInfo p : method.parameters()) erased.add(erasure(p.parameterizedType(), arguments));
        return erased;
    }

    /** The erasure of a type as the class sees it: its type's name, and its array dimensions. */
    private static String erasure(ParameterizedType pt, Map<TypeParameter, ParameterizedType> arguments) {
        int arrays = 0;
        ParameterizedType current = pt;
        // a type parameter is the class's argument for it, else its leftmost bound; the bound may be one in turn
        for (int guard = 0; current != null && guard < 32; guard++) {
            arrays += current.arrays();
            if (current.typeInfo() != null) return current.typeInfo().fullyQualifiedName() + "[]".repeat(arrays);
            TypeParameter tp = current.typeParameter();
            if (tp == null) break;
            ParameterizedType argument = arguments.get(tp);
            if (argument != null && argument.typeParameter() != tp) {
                current = argument;
            } else {
                current = tp.typeBounds().isEmpty() ? null : tp.typeBounds().getFirst();
            }
        }
        return "java.lang.Object" + "[]".repeat(arrays);
    }
}
