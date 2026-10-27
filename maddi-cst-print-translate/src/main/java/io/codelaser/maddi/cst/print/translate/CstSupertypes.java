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

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.type.ParameterizedTypeImpl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A type as seen through one of its supertypes, for a Java-to-X printer: the supertype as the type extends or
 * implements it, and what the supertype's type parameters are in the type. Pure functions of the CST.
 * <p>
 * The search is the printers' own rather than {@link ParameterizedType#concreteSuperType}: breadth first, with a
 * visited set, so a cyclic or very deep hierarchy on a partial class path neither overflows nor needs a catch, and with
 * plain substitution of the type's own type arguments. The two agree except for {@code java.lang.Object}, which
 * {@code concreteSuperType} does not return as a superclass (maddi#157).
 */
public final class CstSupertypes {

    private CstSupertypes() {
    }

    /** {@code ancestor} as {@code type} extends or implements it, in {@code type}'s type parameters; null when it does not. */
    public static ParameterizedType asSeen(TypeInfo type, TypeInfo ancestor) {
        return firstAsSeen(type, ancestor::equals);
    }

    /**
     * {@code from} as its supertype {@code ancestor}, in {@code from}'s type arguments: {@code from} itself when it is
     * of that type; null when {@code from} is raw, or has no such supertype.
     */
    public static ParameterizedType asSeen(ParameterizedType from, TypeInfo ancestor) {
        TypeInfo type = from.typeInfo();
        if (type.equals(ancestor)) return from;
        if (from.parameters().size() != type.typeParameters().size()) return null;
        ParameterizedType seen = asSeen(type, ancestor);
        return seen == null ? null : substitute(seen, typeArguments(from));
    }

    /** The first supertype of {@code type}, breadth first, that is an {@code ancestor}, in {@code type}'s type parameters. */
    public static ParameterizedType firstAsSeen(TypeInfo type, Predicate<TypeInfo> ancestor) {
        Deque<ParameterizedType> todo = new ArrayDeque<>();
        Set<TypeInfo> seen = new HashSet<>();
        if (type.parentClass() != null) todo.add(type.parentClass());
        todo.addAll(type.interfacesImplemented());
        while (!todo.isEmpty()) {
            ParameterizedType pt = todo.poll();
            TypeInfo t = pt.typeInfo();
            if (t == null || !seen.add(t)) continue;
            if (ancestor.test(t)) return pt;
            Map<TypeParameter, ParameterizedType> map = typeArguments(pt);
            if (t.parentClass() != null) todo.add(substitute(t.parentClass(), map));
            t.interfacesImplemented().forEach(i -> todo.add(substitute(i, map)));
        }
        return null;
    }

    /** The type parameters of {@code pt}'s type, mapped to its arguments (as many as it has: none when raw). */
    public static Map<TypeParameter, ParameterizedType> typeArguments(ParameterizedType pt) {
        Map<TypeParameter, ParameterizedType> map = new HashMap<>();
        TypeInfo t = pt.typeInfo();
        for (int k = 0; k < Math.min(t.typeParameters().size(), pt.parameters().size()); k++) {
            map.put(t.typeParameters().get(k), pt.parameters().get(k));
        }
        return map;
    }

    /**
     * {@code pt} with the type parameters of {@code map} replaced by their arguments, nested ones included. A type
     * argument written with a wildcard keeps it: {@code Map<Class<? extends B>, B>} with {@code Number} for {@code B} is
     * {@code Map<Class<? extends Number>, Number>}, where it used to lose it, {@code Map<Class<Number>, Number>}
     * (maddi#158). Combined with a wildcard argument as {@link ParameterizedTypeImpl#nestedArgument} does, as
     * {@code concreteSuperType} does.
     */
    public static ParameterizedType substitute(ParameterizedType pt, Map<TypeParameter, ParameterizedType> map) {
        return substitute(pt, map, true);
    }

    /**
     * As {@link #substitute(ParameterizedType, Map)}, but a type argument written with a wildcard on a substituted type
     * parameter becomes the argument without it: {@code List<? extends T>} with {@code String} for {@code T} is
     * {@code List<String>}. For a printer that prints such a declaration without its wildcard (the C# printer's
     * {@code List<? extends T>} is {@code List<T>}), so that a substituted signature, a hoisted constructor's or an
     * override's, prints as the declaration it must match does: with the wildcard kept, {@code List<? extends String>}
     * is C#'s {@code IReadOnlyList<string>}, which {@code List<string>} is not.
     */
    public static ParameterizedType substituteDroppingWildcards(ParameterizedType pt,
                                                                Map<TypeParameter, ParameterizedType> map) {
        return substitute(pt, map, false);
    }

    private static ParameterizedType substitute(ParameterizedType pt, Map<TypeParameter, ParameterizedType> map,
                                                boolean keepWildcards) {
        if (map.isEmpty()) return pt;
        if (pt.isTypeParameter() && map.containsKey(pt.typeParameter())) {
            ParameterizedType mapped = map.get(pt.typeParameter());
            if (pt.arrays() > 0) return mapped.copyWithArrays(mapped.arrays() + pt.arrays());
            return pt.wildcard() == null || !keepWildcards ? mapped : ParameterizedTypeImpl.nestedArgument(pt, mapped);
        }
        if (pt.parameters().isEmpty()) return pt;
        return pt.withParameters(pt.parameters().stream().map(p -> substitute(p, map, keepWildcards)).toList());
    }

    /**
     * Each {@code ? extends X} of {@code pt}, itself and nested, as its bound {@code X}: what a value of the type gives
     * out is an {@code X}. A diamond's inferred {@code ? extends Mapped_}, {@code new CountDistinctUniCollector<>(mapper)}
     * of a {@code Function<? super A, ? extends Mapped_>}.
     */
    public static ParameterizedType extendsAsBound(ParameterizedType pt) {
        ParameterizedType bound = pt.wildcard() != null && pt.wildcard().isExtends() ? pt.withWildcard(null) : pt;
        if (bound.parameters().isEmpty()) return bound;
        return bound.withParameters(bound.parameters().stream().map(CstSupertypes::extendsAsBound).toList());
    }

    /**
     * The one abstract method of a functional interface, also when no lambda targets it: of an interface that extends
     * none, its only abstract instance method; of one that extends others, the CST's, when it is functional.
     */
    public static MethodInfo singleAbstractMethod(TypeInfo typeInfo) {
        if (!typeInfo.interfacesImplemented().isEmpty()) {
            return typeInfo.isFunctionalInterface() ? typeInfo.singleAbstractMethod() : null;
        }
        List<MethodInfo> abstractMethods = typeInfo.methods().stream()
                .filter(m -> !m.isSynthetic() && m.isAbstract() && !m.isStatic()).toList();
        return abstractMethods.size() == 1 ? abstractMethods.getFirst() : null;
    }
}
