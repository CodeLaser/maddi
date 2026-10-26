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
package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.variable.FieldReference;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The code that is translated together: the primary types whose Kotlin is compiled as one, main and test sources. A
 * member it holds may take Kotlin's read-only collection type when each use of it in these types allows one, whatever
 * its access ({@link KotlinReadOnly}); Java code compiled against the Kotlin classes sees {@code java.util.List} either
 * way. {@link #FILE}, the default, is the printer on its own, which sees one compilation unit: then only a private
 * field or a closed method's result can be read-only.
 */
public final class KotlinWorld {

    /** Only the compilation unit being printed. */
    public static final KotlinWorld FILE = new KotlinWorld(List.of());

    private final Set<TypeInfo> types;
    // per primary type, the fields and methods its code mentions, computed when first asked
    private final Map<TypeInfo, Set<Object>> mentioned = new ConcurrentHashMap<>();

    // the read-only verdicts on fields and methods (KotlinReadOnly), decided once for the world
    private final Map<Object, Boolean> readOnly = new ConcurrentHashMap<>();

    private KotlinWorld(Collection<TypeInfo> primaryTypes) {
        Set<TypeInfo> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(primaryTypes);
        this.types = Collections.unmodifiableSet(set);
    }

    /** The primary types translated and compiled together. */
    public static KotlinWorld of(Collection<TypeInfo> primaryTypes) {
        return primaryTypes.isEmpty() ? FILE : new KotlinWorld(primaryTypes);
    }

    public boolean isFile() {
        return types.isEmpty();
    }

    /** The primary type is translated with the others: every use of its members is in sight. */
    public boolean contains(TypeInfo primaryType) {
        return types.contains(primaryType);
    }

    /** The primary types whose code mentions the field or method: reads, calls, method references. */
    public List<TypeInfo> mentioning(Object member) {
        return types.stream().filter(t -> mentioned.computeIfAbsent(t, KotlinWorld::mentions).contains(member))
                .toList();
    }

    // per method, whether a method of the world overrides it, computed when first asked
    private final Map<Object, Boolean> overridden = new ConcurrentHashMap<>();

    /**
     * A method of the code translated together overrides {@code method} ({@link #FILE}: of its compilation unit), so
     * a call of it may run another body.
     */
    public boolean overridden(io.codelaser.maddi.cst.api.info.MethodInfo method) {
        if (isFile()) return overriddenIn(method, method.typeInfo().compilationUnit().types());
        return overridden.computeIfAbsent(method, m -> overriddenIn(method, types));
    }

    private static boolean overriddenIn(io.codelaser.maddi.cst.api.info.MethodInfo method,
                                        Collection<TypeInfo> primaryTypes) {
        return primaryTypes.stream().flatMap(t -> java.util.stream.Stream.concat(java.util.stream.Stream.of(t),
                        t.recursiveSubTypeStream()))
                .flatMap(TypeInfo::constructorAndMethodStream)
                .anyMatch(m -> m != method && m.overrides().contains(method));
    }

    // per interface, whether an enum of the world implements it, computed when first asked
    private final Map<TypeInfo, Boolean> implementedByEnum = new ConcurrentHashMap<>();

    /** An enum of the code translated together ({@link #FILE}: of its compilation unit) implements the interface. */
    public boolean implementedByEnum(TypeInfo anInterface) {
        if (isFile()) return implementedByEnumIn(anInterface, anInterface.compilationUnit().types());
        return implementedByEnum.computeIfAbsent(anInterface, i -> implementedByEnumIn(i, types));
    }

    private static boolean implementedByEnumIn(TypeInfo anInterface, Collection<TypeInfo> primaryTypes) {
        return primaryTypes.stream().flatMap(TypeInfo::recursiveSubTypeStream)
                .anyMatch(t -> t.typeNature().isEnum() && t.recursiveSuperTypeStream().anyMatch(anInterface::equals));
    }

    // the override families of the world's methods, built when first asked (maddi-mod#25)
    private volatile Map<io.codelaser.maddi.cst.api.info.MethodInfo, List<io.codelaser.maddi.cst.api.info.MethodInfo>> families;

    /**
     * The override family of a method of the world: the methods that override it or that it overrides, transitively,
     * all in the world's types (member, anonymous and local ones). Null when the family reaches outside the world (a
     * JDK interface's method, a type not translated with it), or when a lambda or method reference of the world
     * implements one of its members: Kotlin types such an implementation's parameters from the interface, and its
     * body is not checked here. {@link #FILE} has no families.
     */
    public List<io.codelaser.maddi.cst.api.info.MethodInfo> family(io.codelaser.maddi.cst.api.info.MethodInfo method) {
        if (isFile()) return null;
        if (families == null) {
            synchronized (this) {
                if (families == null) families = computeFamilies();
            }
        }
        return families.get(method);
    }

    private Map<io.codelaser.maddi.cst.api.info.MethodInfo, List<io.codelaser.maddi.cst.api.info.MethodInfo>> computeFamilies() {
        Map<io.codelaser.maddi.cst.api.info.MethodInfo, io.codelaser.maddi.cst.api.info.MethodInfo> parent = new IdentityHashMap<>();
        Set<io.codelaser.maddi.cst.api.info.MethodInfo> implementedByFunction = Collections.newSetFromMap(new IdentityHashMap<>());
        List<io.codelaser.maddi.cst.api.info.MethodInfo> methods = types.stream()
                .flatMap(TypeInfo::recursiveSubTypeStreamIncludingLocal)
                .flatMap(TypeInfo::constructorAndMethodStream)
                .filter(m -> !m.isConstructor() && !m.isStatic()).toList();
        for (io.codelaser.maddi.cst.api.info.MethodInfo m : methods) {
            parent.putIfAbsent(m, m);
            for (io.codelaser.maddi.cst.api.info.MethodInfo o : m.overrides()) {
                parent.putIfAbsent(o, o);
                union(parent, m, o);
            }
        }
        types.forEach(t -> KotlinReadOnly.forEachBody(t, (method, body) -> body.visit((Element e) -> {
            TypeInfo functional = e instanceof io.codelaser.maddi.cst.api.expression.Lambda l
                    ? l.concreteFunctionalType().typeInfo()
                    : e instanceof MethodReference mr ? mr.parameterizedType().typeInfo() : null;
            if (functional != null && functional.singleAbstractMethod() != null) {
                implementedByFunction.add(functional.singleAbstractMethod());
            }
            return true;
        })));
        Map<io.codelaser.maddi.cst.api.info.MethodInfo, List<io.codelaser.maddi.cst.api.info.MethodInfo>> byRoot = new IdentityHashMap<>();
        for (io.codelaser.maddi.cst.api.info.MethodInfo m : parent.keySet()) {
            byRoot.computeIfAbsent(find(parent, m), _ -> new java.util.ArrayList<>()).add(m);
        }
        Map<io.codelaser.maddi.cst.api.info.MethodInfo, List<io.codelaser.maddi.cst.api.info.MethodInfo>> result = new IdentityHashMap<>();
        for (List<io.codelaser.maddi.cst.api.info.MethodInfo> family : byRoot.values()) {
            boolean complete = family.stream().allMatch(m -> types.contains(m.typeInfo().primaryType())
                                                              && !implementedByFunction.contains(m));
            List<io.codelaser.maddi.cst.api.info.MethodInfo> members = complete ? List.copyOf(family) : null;
            for (io.codelaser.maddi.cst.api.info.MethodInfo m : family) {
                if (members != null) result.put(m, members);
            }
        }
        return result;
    }

    private static io.codelaser.maddi.cst.api.info.MethodInfo find(
            Map<io.codelaser.maddi.cst.api.info.MethodInfo, io.codelaser.maddi.cst.api.info.MethodInfo> parent,
            io.codelaser.maddi.cst.api.info.MethodInfo m) {
        io.codelaser.maddi.cst.api.info.MethodInfo root = m;
        while (parent.get(root) != root) root = parent.get(root);
        while (parent.get(m) != root) {
            io.codelaser.maddi.cst.api.info.MethodInfo next = parent.get(m);
            parent.put(m, root);
            m = next;
        }
        return root;
    }

    private static void union(Map<io.codelaser.maddi.cst.api.info.MethodInfo, io.codelaser.maddi.cst.api.info.MethodInfo> parent,
                              io.codelaser.maddi.cst.api.info.MethodInfo a, io.codelaser.maddi.cst.api.info.MethodInfo b) {
        io.codelaser.maddi.cst.api.info.MethodInfo ra = find(parent, a), rb = find(parent, b);
        if (ra != rb) parent.put(ra, rb);
    }

    /** A read-only verdict remembered; null when not decided, or for {@link #FILE}, which remembers nothing. */
    Boolean readOnly(Object member) {
        return readOnly.get(member);
    }

    void readOnly(Object member, boolean verdict) {
        if (!isFile()) readOnly.put(member, verdict);
    }

    private static Set<Object> mentions(TypeInfo primaryType) {
        Set<Object> members = Collections.newSetFromMap(new IdentityHashMap<>());
        KotlinReadOnly.forEachBody(primaryType, (method, body) -> body.visit((Element e) -> {
            switch (e) {
                case VariableExpression ve when ve.variable() instanceof FieldReference fr -> members.add(fr.fieldInfo());
                case MethodCall mc when mc.methodInfo() != null -> members.add(mc.methodInfo());
                case MethodReference mr when mr.methodInfo() != null -> members.add(mr.methodInfo());
                default -> {
                }
            }
            return true;
        }));
        return Collections.unmodifiableSet(members);
    }
}
