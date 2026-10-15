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

package io.codelaser.maddi.cst.print.csharp;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The type hierarchy as C# needs it: a supertype as a type extends or implements it, in its own type parameters, and
 * what the type parameters of a generic supertype are in a subtype; the interface methods an abstract class leaves to
 * its subclasses, which C# makes it redeclare; the default methods C# implements explicitly.
 */
final class CSharpHierarchy {

    private CSharpHierarchy() {
    }

    /**
     * The method of an inherited interface that this default method of interface {@code iface} implements, when C#
     * needs it implemented explicitly: a C# interface's member never implements another's implicitly.
     */
    static MethodInfo interfaceMethodDefaulted(TypeInfo iface, MethodInfo m) {
        if (!iface.isInterface() || m.isStatic() || m.isAbstract() || m.methodBody() == null
            || !m.typeParameters().isEmpty() || m.isConstructor() || CSharpWildcards.mayCapture(m)) return null;
        return m.overrides().stream()
                .filter(o -> o != m && o.typeInfo() != iface && o.typeInfo().isInterface()
                             && CSharpNames.translated(o.typeInfo()) && o.isAbstract() && o.typeParameters().isEmpty())
                .filter(o -> implementedAs(iface, o.typeInfo()) != null)
                .findFirst().orElse(null);
    }

    /** {@code ancestor} as {@code type} extends or implements it, in {@code type}'s type parameters. */
    static ParameterizedType implementedAs(TypeInfo type, TypeInfo ancestor) {
        return firstAsSeen(type, ancestor::equals);
    }

    /** {@code Iterable<E>} as {@code t} implements it, directly or through its supertypes; null when it does not. */
    static ParameterizedType iterableAsSeen(TypeInfo t) {
        return firstAsSeen(t, at -> "java.lang.Iterable".equals(at.fullyQualifiedName()));
    }

    /** The first supertype of {@code type}, breadth first, that is an {@code ancestor}, in {@code type}'s type parameters. */
    private static ParameterizedType firstAsSeen(TypeInfo type, Predicate<TypeInfo> ancestor) {
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

    /** The type parameters of {@code pt}'s type, mapped to its arguments. */
    static Map<TypeParameter, ParameterizedType> typeArguments(ParameterizedType pt) {
        Map<TypeParameter, ParameterizedType> map = new HashMap<>();
        TypeInfo t = pt.typeInfo();
        for (int k = 0; k < Math.min(t.typeParameters().size(), pt.parameters().size()); k++) {
            map.put(t.typeParameters().get(k), pt.parameters().get(k));
        }
        return map;
    }

    static ParameterizedType substitute(ParameterizedType pt, Map<TypeParameter, ParameterizedType> map) {
        if (map.isEmpty()) return pt;
        if (pt.isTypeParameter() && map.containsKey(pt.typeParameter())) {
            ParameterizedType mapped = map.get(pt.typeParameter());
            return pt.arrays() == 0 ? mapped : mapped.copyWithArrays(mapped.arrays() + pt.arrays());
        }
        if (pt.parameters().isEmpty()) return pt;
        return pt.withParameters(pt.parameters().stream().map(p -> substitute(p, map)).toList());
    }

    /**
     * An abstract class of the program that is iterable, and that neither it nor a superclass gives an iterator(): it
     * declares {@code Iterator()} abstract, which its interface's GetEnumerator calls. Once, by the first such class.
     */
    static boolean needsAbstractIterator(TypeInfo t) {
        if (t.isInterface() || !t.isAbstract() || !CSharpNames.translated(t) || iterableAsSeen(t) == null) return false;
        for (TypeInfo at : CstQueries.classChain(t)) {
            if (at.methods().stream().anyMatch(m -> !m.isStatic() && "iterator".equals(m.name()) && m.parameters().isEmpty())) {
                return false;
            }
        }
        TypeInfo parent = CstQueries.superclass(t);
        return parent == null || !needsAbstractIterator(parent);
    }


    /**
     * A class that reaches two interfaces of the program, neither extending the other, that extend Iterable: each has
     * default GetEnumerators, and C# has no most specific one (CS8705). The class declares its own, unless a
     * superclass of the program does.
     */
    static boolean ambiguousEnumerable(TypeInfo cls) {
        if (cls.isInterface() || !CSharpNames.translated(cls)) return false;
        TypeInfo parent = CstQueries.superclass(cls);
        if (parent != null && CSharpNames.translated(parent) && declaresEnumerator(parent)) return false;
        Set<TypeInfo> iterables = new LinkedHashSet<>();
        Deque<TypeInfo> todo = new ArrayDeque<>();
        Set<TypeInfo> seen = new HashSet<>();
        for (TypeInfo at : CstQueries.classChain(cls)) {
            at.interfacesImplemented().forEach(i -> { if (i.typeInfo() != null) todo.add(i.typeInfo()); });
        }
        while (!todo.isEmpty()) {
            TypeInfo i = todo.poll();
            if (!seen.add(i) || !CSharpNames.translated(i)) continue;
            // the interfaces with default GetEnumerators: those that extend Iterable themselves (CSharpTypePrinter)
            if (i.isInterface() && i.interfacesImplemented().stream().anyMatch(s -> s.typeInfo() != null
                    && "java.lang.Iterable".equals(s.typeInfo().fullyQualifiedName()))) {
                iterables.add(i);
            }
            i.interfacesImplemented().forEach(s -> { if (s.typeInfo() != null) todo.add(s.typeInfo()); });
        }
        // the most specific ones: C# takes the default of an interface over the one it extends
        List<TypeInfo> mostSpecific = iterables.stream()
                .filter(i -> iterables.stream().noneMatch(j -> j != i && extendsInterface(j, i))).toList();
        return mostSpecific.size() > 1;
    }

    /** A class of the program that declares GetEnumerator: it implements Iterable itself, or resolves an ambiguity. */
    private static boolean declaresEnumerator(TypeInfo cls) {
        for (TypeInfo at : CstQueries.classChain(cls)) {
            if (!CSharpNames.translated(at)) break;
            if (at.interfacesImplemented().stream().anyMatch(i -> i.typeInfo() != null
                    && "java.lang.Iterable".equals(i.typeInfo().fullyQualifiedName()))) {
                return true;
            }
            if (ambiguousEnumerable(at)) return true;
        }
        return false;
    }

    private static boolean extendsInterface(TypeInfo sub, TypeInfo sup) {
        Deque<TypeInfo> todo = new ArrayDeque<>();
        Set<TypeInfo> seen = new HashSet<>();
        sub.interfacesImplemented().forEach(i -> { if (i.typeInfo() != null) todo.add(i.typeInfo()); });
        while (!todo.isEmpty()) {
            TypeInfo t = todo.poll();
            if (t.equals(sup)) return true;
            if (!seen.add(t)) continue;
            t.interfacesImplemented().forEach(i -> { if (i.typeInfo() != null) todo.add(i.typeInfo()); });
        }
        return false;
    }

    /** An interface's method an abstract class leaves to its subclasses, and the interface's type arguments. */
    record LeftAbstract(MethodInfo method, Map<TypeParameter, ParameterizedType> typeArguments) {}

    /** The interface methods abstract class {@code cls} redeclares abstract: no class or default implements them. */
    static List<LeftAbstract> leftAbstract(TypeInfo cls) {
        if (cls.isInterface() || !cls.isAbstract() || !CSharpNames.translated(cls)) return List.of();
        Map<TypeInfo, List<LeftAbstract>> cache = CSharpContext.memo().leftAbstract;
        List<LeftAbstract> known = cache.get(cls);
        if (known == null) {
            known = computeLeftAbstract(cls);
            cache.put(cls, known);
        }
        return known;
    }

    private static List<LeftAbstract> computeLeftAbstract(TypeInfo cls) {
        List<TypeInfo> chain = CstQueries.classChain(cls);
        List<MethodInfo> implementations = new ArrayList<>();
        chain.forEach(t -> implementations.addAll(t.methods()));
        Map<TypeInfo, ParameterizedType> interfaces = new LinkedHashMap<>();
        Deque<ParameterizedType> todo = new ArrayDeque<>();
        for (TypeInfo t : chain) {
            ParameterizedType asSeen = t == cls ? null : implementedAs(cls, t);
            Map<TypeParameter, ParameterizedType> map = asSeen == null ? Map.of() : typeArguments(asSeen);
            t.interfacesImplemented().forEach(i -> todo.add(substitute(i, map)));
        }
        while (!todo.isEmpty()) {
            ParameterizedType i = todo.poll();
            TypeInfo iface = i.typeInfo();
            if (iface == null || interfaces.containsKey(iface)) continue;
            interfaces.put(iface, i);
            Map<TypeParameter, ParameterizedType> map = typeArguments(i);
            iface.interfacesImplemented().forEach(sup -> todo.add(substitute(sup, map)));
        }
        interfaces.keySet().forEach(iface -> implementations.addAll(iface.methods().stream()
                .filter(m -> !m.isAbstract() && !m.isStatic()).toList()));
        // an abstract superclass of the program declares what it leaves abstract itself: a second declaration would hide
        // it (CS0533), and the subclasses' overrides would not implement it (CS0534)
        // (any of them: one in between that declares nothing does not make a third one's declaration go away)
        Set<MethodInfo> declaredAbove = new HashSet<>();
        CstQueries.superclasses(cls).forEach(t -> leftAbstract(t).forEach(l -> declaredAbove.add(l.method())));
        List<LeftAbstract> left = new ArrayList<>();
        interfaces.forEach((iface, i) -> {
            if (!CSharpNames.translated(iface)) return;
            for (MethodInfo m : iface.methods()) {
                if (declaredAbove.contains(m)) continue;
                if (m.isStatic() || !m.isAbstract() || !m.typeParameters().isEmpty() || m.isSynthetic()
                    || CSharpWildcards.mayCapture(m)) continue;
                if (CSharpBcl.objectOverridable(m)) continue;
                if (implementations.stream().anyMatch(x -> x != m && (x.overrides().contains(m)
                                                                       || inheritedImplementation(x, m)))) continue;
                left.add(new LeftAbstract(m, typeArguments(i)));
            }
        });
        return List.copyOf(left);
    }

    /**
     * The methods of {@code cls}'s superclasses that implement an interface method of {@code cls} without Java's
     * overrides() saying so: {@code class B { public boolean d() {…} }}, {@code class C extends B implements I}, with
     * I's {@code d()}. C#'s public {@code B.D()} implements {@code I.D()} for C as Java's does; it is no property.
     */
    static Set<MethodInfo> inheritedImplementations(TypeInfo cls) {
        if (cls.isInterface() || cls.interfacesImplemented().isEmpty()) return Set.of();
        List<MethodInfo> inherited = CstQueries.superclasses(cls).stream().flatMap(t -> t.methods().stream()).toList();
        if (inherited.isEmpty()) return Set.of();
        Set<TypeInfo> interfaces = new HashSet<>();
        Deque<TypeInfo> todo = new ArrayDeque<>();
        cls.interfacesImplemented().forEach(i -> { if (i.typeInfo() != null) todo.add(i.typeInfo()); });
        while (!todo.isEmpty()) {
            TypeInfo iface = todo.poll();
            if (!interfaces.add(iface)) continue;
            iface.interfacesImplemented().forEach(i -> { if (i.typeInfo() != null) todo.add(i.typeInfo()); });
        }
        Set<MethodInfo> found = new HashSet<>();
        for (TypeInfo iface : interfaces) {
            for (MethodInfo m : iface.methods()) {
                if (m.isStatic() || !m.isAbstract()) continue;
                inherited.stream().filter(x -> inheritedImplementation(x, m)).findFirst().ifPresent(found::add);
            }
        }
        return found;
    }

    /**
     * Class method {@code x} has interface method {@code m}'s signature, without overriding it in Java's
     * overrides(): a superclass's method that implements a subclass's interface. A type parameter matches any type.
     */
    private static boolean inheritedImplementation(MethodInfo x, MethodInfo m) {
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
