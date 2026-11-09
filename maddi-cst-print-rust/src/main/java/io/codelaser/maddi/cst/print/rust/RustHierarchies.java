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

package io.codelaser.maddi.cst.print.rust;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

/**
 * Closed class hierarchies as one Rust type (rust-printing.md, "A closed hierarchy is an enum"). A source class whose
 * subclasses are all source classes, named and at the top level of a file or nested, is the root of a closed
 * hierarchy: every object of a member class is an object of the root's struct, which holds the root's fields and a
 * {@code kind}, the enum of the member classes, whose variant carries the fields each class adds along its chain
 * below the root ({@code ExprentKind::RecordVar(VarExprentData, RecordVarExprentData)}).
 * <p>
 * A member type is printed as the root's struct; a cast between members is the object itself, {@code instanceof} a
 * test of the kind. The methods of all members are methods of the root's struct: a method no other member overrides
 * keeps its name, one that is overridden is a family, with a dispatching method of the family's name that matches
 * on the kind and calls the implementation of the variant's class, each implementation with its own verdict.
 * <p>
 * The hierarchies of the program being translated are known while it is printed ({@link #current()}), as type and
 * method names ({@link RustNames}) depend on them wherever they are printed.
 */
final class RustHierarchies {

    static final RustHierarchies NONE = new RustHierarchies(Map.of(), Map.of());

    private static final ThreadLocal<RustHierarchies> CURRENT = ThreadLocal.withInitial(() -> NONE);

    /** The hierarchies of the translation in progress on this thread; {@link #NONE} outside one. */
    static RustHierarchies current() {
        return CURRENT.get();
    }

    /** Makes {@code hierarchies} the current ones; returns the ones to {@link #restore} afterwards. */
    static RustHierarchies enter(RustHierarchies hierarchies) {
        RustHierarchies outer = CURRENT.get();
        CURRENT.set(hierarchies);
        return outer;
    }

    static void restore(RustHierarchies outer) {
        CURRENT.set(outer);
    }

    /** Runs {@code action} with {@code hierarchies} as the current ones. */
    static <T> T with(RustHierarchies hierarchies, Supplier<T> action) {
        RustHierarchies outer = CURRENT.get();
        CURRENT.set(hierarchies);
        try {
            return action.get();
        } finally {
            CURRENT.set(outer);
        }
    }

    /** One closed hierarchy: its root, and its members, the root first, every class before its subclasses. */
    record Hierarchy(TypeInfo root, List<TypeInfo> members, Map<MethodInfo, Family> families) {
    }

    /**
     * Methods related by overriding, inside the hierarchy or through a method outside it that they all override
     * ({@code toString}, an interface's). {@code representative} gives the dispatching method its signature.
     */
    record Family(String name, List<MethodInfo> methods, MethodInfo representative) {
        boolean dispatched() {
            return methods.size() > 1;
        }
    }

    private final Map<TypeInfo, Hierarchy> byMember;

    /*
     The source structs the program tests or casts a value of a source interface to ('obj instanceof Statement st',
     '(SupertypeTarget) targetInfo'), per interface: the trait gets a conversion to each (downcastName), None by
     default, Some(self) in the struct's impl. Any cannot downcast a struct with a lifetime; these can.
     */
    private final Map<TypeInfo, Set<TypeInfo>> downcasts;

    private RustHierarchies(Map<TypeInfo, Hierarchy> byMember, Map<TypeInfo, Set<TypeInfo>> downcasts) {
        this.byMember = byMember;
        this.downcasts = downcasts;
    }

    /** The closed hierarchies among {@code types} and their nested types. */
    static RustHierarchies of(Collection<TypeInfo> types) {
        List<TypeInfo> all = types.stream().flatMap(TypeInfo::recursiveSubTypeStreamIncludingLocal).toList();
        Map<TypeInfo, List<TypeInfo>> children = new HashMap<>();
        for (TypeInfo t : all) {
            TypeInfo parent = sourceParentClass(t);
            if (parent != null) children.computeIfAbsent(parent, p -> new ArrayList<>()).add(t);
        }
        Map<TypeInfo, Hierarchy> byMember = new HashMap<>();
        for (TypeInfo root : children.keySet()) {
            if (sourceParentClass(root) != null || !eligible(root)) continue;
            List<TypeInfo> members = new ArrayList<>();
            if (!collect(root, children, members)) continue;
            // one module: what names a member's module names the root's (RustNames.module of its package)
            if (members.stream().anyMatch(m -> !m.packageName().equals(root.packageName()))) continue;
            Hierarchy h = new Hierarchy(root, List.copyOf(members), families(root, members));
            members.forEach(m -> byMember.put(m, h));
        }
        RustHierarchies partial = new RustHierarchies(byMember, Map.of());
        return new RustHierarchies(byMember, partial.downcasts(all));
    }

    private Map<TypeInfo, Set<TypeInfo>> downcasts(List<TypeInfo> all) {
        Map<TypeInfo, Set<TypeInfo>> out = new HashMap<>();
        for (TypeInfo t : all) {
            t.constructorAndMethodStream().filter(m -> m.methodBody() != null).forEach(m -> m.methodBody().visit(e -> {
                ParameterizedType from = null;
                ParameterizedType to = null;
                if (e instanceof io.codelaser.maddi.cst.api.expression.InstanceOf io) {
                    from = io.expression().parameterizedType();
                    to = io.testType();
                } else if (e instanceof io.codelaser.maddi.cst.api.expression.Cast c) {
                    from = c.expression().parameterizedType();
                    to = c.parameterizedType();
                }
                TypeInfo i = from == null ? null : from.typeInfo();
                TypeInfo c = to == null ? null : to.typeInfo();
                if (i != null && c != null && from.arrays() == 0 && to.arrays() == 0 && i.isInterface()
                    && !i.compilationUnit().externalLibrary() && !c.isInterface()
                    && !c.compilationUnit().externalLibrary() && c.typeParameters().isEmpty()
                    && !c.typeNature().isEnum() && !c.typeNature().isRecord()) {
                    TypeInfo root = root(c);
                    out.computeIfAbsent(i, k -> new java.util.TreeSet<>(
                            java.util.Comparator.comparing(TypeInfo::fullyQualifiedName))).add(root != null ? root : c);
                }
                return true;
            }));
        }
        return out;
    }

    /** The structs a value of interface {@code i} is tested or cast to: the trait's conversions. */
    Set<TypeInfo> downcasts(TypeInfo i) {
        return downcasts.getOrDefault(i, Set.of());
    }

    /** {@code as_statement}: the conversion of an interface's value to a struct, {@code Option<&Statement>}. */
    static String downcastName(TypeInfo struct) {
        return "as_" + RustNames.snake(RustNames.ownType(struct));
    }

    /** The struct a cast or {@code instanceof} of interface {@code i} to {@code target} converts to; else null. */
    TypeInfo downcastStruct(TypeInfo i, TypeInfo target) {
        if (i == null || target == null) return null;
        TypeInfo root = root(target);
        TypeInfo struct = root != null ? root : target;
        return downcasts(i).contains(struct) ? struct : null;
    }

    private static TypeInfo sourceParentClass(TypeInfo t) {
        ParameterizedType parent = t.parentClass();
        if (parent == null || parent.typeInfo() == null) return null;
        TypeInfo p = parent.typeInfo();
        return p.isJavaLangObject() || p.compilationUnit().externalLibrary() || p.isInterface() ? null : p;
    }

    private static boolean eligible(TypeInfo t) {
        return !t.isInterface() && !t.typeNature().isEnum() && !t.typeNature().isRecord()
               && !t.typeNature().isAnnotation() && !t.isAnonymous() && t.enclosingMethod() == null
               && t.typeParameters().isEmpty() && !t.isSynthetic();
    }

    private static boolean collect(TypeInfo t, Map<TypeInfo, List<TypeInfo>> children, List<TypeInfo> into) {
        if (!eligible(t)) return false;
        into.add(t);
        for (TypeInfo c : children.getOrDefault(t, List.of())) {
            if (!collect(c, children, into)) return false;
        }
        return true;
    }

    /*
     The method families of a hierarchy. Two methods are one family when one overrides the other, or both override
     the same method outside the hierarchy. A family's name is its top method's (the one closest to the root); a name
     two families share is told apart by the class of the top method, the root's family keeping the plain one.
     */
    private static Map<MethodInfo, Family> families(TypeInfo root, List<TypeInfo> members) {
        Set<TypeInfo> memberSet = new HashSet<>(members);
        Map<MethodInfo, MethodInfo> parent = new HashMap<>();
        List<MethodInfo> methods = new ArrayList<>();
        Map<MethodInfo, MethodInfo> byOutside = new HashMap<>();
        for (TypeInfo t : members) {
            for (MethodInfo m : t.methods()) {
                if (m.isSynthetic() || m.isStaticInitializer() || m.isInstanceInitializer()) continue;
                methods.add(m);
                parent.put(m, m);
            }
        }
        for (MethodInfo m : methods) {
            if (m.isStatic()) continue;
            for (MethodInfo o : m.overrides()) {
                if (memberSet.contains(o.typeInfo())) {
                    if (parent.containsKey(o)) union(parent, m, o);
                } else {
                    MethodInfo first = byOutside.putIfAbsent(o, m);
                    if (first != null) union(parent, m, first);
                }
            }
        }
        Map<MethodInfo, List<MethodInfo>> groups = new LinkedHashMap<>();
        for (MethodInfo m : methods) groups.computeIfAbsent(find(parent, m), k -> new ArrayList<>()).add(m);
        Map<String, List<List<MethodInfo>>> byName = new HashMap<>();
        Map<List<MethodInfo>, MethodInfo> tops = new HashMap<>();
        for (List<MethodInfo> group : groups.values()) {
            MethodInfo top = group.stream().min((a, b) -> depth(a.typeInfo(), root) - depth(b.typeInfo(), root))
                    .orElseThrow();
            tops.put(group, top);
            byName.computeIfAbsent(RustNames.ownMethod(top), n -> new ArrayList<>()).add(group);
        }
        Map<MethodInfo, Family> out = new HashMap<>();
        for (Map.Entry<String, List<List<MethodInfo>>> e : byName.entrySet()) {
            for (List<MethodInfo> group : e.getValue()) {
                MethodInfo top = tops.get(group);
                boolean plain = e.getValue().size() == 1 || top.typeInfo() == root;
                String name = plain ? e.getKey() : e.getKey() + "_" + RustNames.snake(RustNames.ownType(top.typeInfo()));
                MethodInfo representative = top;
                boolean siblings = group.stream().filter(m -> m != top)
                        .anyMatch(m -> depth(m.typeInfo(), root) == depth(top.typeInfo(), root));
                if (siblings) {
                    // implementations without a common member method: the outside method they override
                    representative = top.overrides().stream().filter(o -> !memberSet.contains(o.typeInfo()))
                            .findFirst().orElse(top);
                }
                Family f = new Family(name, List.copyOf(group), representative);
                group.forEach(m -> out.put(m, f));
            }
        }
        return out;
    }

    private static MethodInfo find(Map<MethodInfo, MethodInfo> parent, MethodInfo m) {
        MethodInfo p = parent.get(m);
        if (p == m) return m;
        MethodInfo r = find(parent, p);
        parent.put(m, r);
        return r;
    }

    private static void union(Map<MethodInfo, MethodInfo> parent, MethodInfo a, MethodInfo b) {
        MethodInfo ra = find(parent, a);
        MethodInfo rb = find(parent, b);
        if (ra != rb) parent.put(ra, rb);
    }

    static int depth(TypeInfo t, TypeInfo root) {
        int d = 0;
        for (TypeInfo c = t; c != null && c != root; c = sourceParentClass(c)) d++;
        return d;
    }

    // ---- queries

    Hierarchy hierarchy(TypeInfo t) {
        return t == null ? null : byMember.get(t);
    }

    boolean isMember(TypeInfo t) {
        return hierarchy(t) != null;
    }

    /** A member below the root: its state is in a variant of the kind. */
    boolean isSubMember(TypeInfo t) {
        Hierarchy h = hierarchy(t);
        return h != null && h.root() != t;
    }

    TypeInfo root(TypeInfo t) {
        Hierarchy h = hierarchy(t);
        return h == null ? null : h.root();
    }

    /** The member's ancestors below the root and itself, from the top: the data its variant carries. */
    List<TypeInfo> chain(TypeInfo member) {
        TypeInfo root = root(member);
        List<TypeInfo> chain = new ArrayList<>();
        for (TypeInfo c = member; c != null && c != root; c = sourceParentClass(c)) chain.addFirst(c);
        return chain;
    }

    /** The members that are {@code t} or below it: the variants an {@code instanceof t} accepts. */
    List<TypeInfo> descendantsOrSelf(TypeInfo t) {
        Hierarchy h = hierarchy(t);
        if (h == null) return List.of();
        return h.members().stream().filter(m -> isSubtypeOrSelf(m, t)).toList();
    }

    static boolean isSubtypeOrSelf(TypeInfo t, TypeInfo of) {
        for (TypeInfo c = t; c != null; c = sourceParentClass(c)) if (c == of) return true;
        return false;
    }

    /** The kind enum of the hierarchy: {@code ExprentKind}. */
    String kind(TypeInfo member) {
        return RustNames.ownType(root(member)) + "Kind";
    }

    /** The variant of a member: its own name. */
    static String variant(TypeInfo member) {
        return RustNames.ownType(member);
    }

    /** The struct of the fields a member below the root adds: {@code AssignmentExprentData}. */
    static String data(TypeInfo member) {
        return RustNames.ownType(member) + "Data";
    }

    /** The kind's method reaching a member's data, {@code _mut} for the exclusive one: {@code assignment_exprent}. */
    static String accessor(TypeInfo member) {
        return RustNames.snake(RustNames.ownType(member));
    }

    /** The {@code Kind::Variant(..)} pattern of {@code member}. */
    String pattern(TypeInfo member) {
        return kind(member) + "::" + variant(member) + (isSubMember(member) ? "(..)" : "");
    }

    /** {@code matches!(o.kind, A(..) | B(..))}: whether the object {@code o} is of {@code t}. */
    String test(String object, TypeInfo t) {
        StringBuilder sb = new StringBuilder("matches!(").append(object).append(".kind, ");
        List<TypeInfo> accepted = descendantsOrSelf(t);
        for (int i = 0; i < accepted.size(); i++) {
            if (i > 0) sb.append(" | ");
            sb.append(pattern(accepted.get(i)));
        }
        return sb.append(")").toString();
    }

    Family family(MethodInfo m) {
        Hierarchy h = hierarchy(m.typeInfo());
        return h == null ? null : h.families().get(m);
    }

    /** The name a call of {@code m} uses: its family's, the dispatching method when the family has several. */
    String callName(MethodInfo m) {
        if (!isMember(m.typeInfo())) return null;
        if (m.isConstructor()) return constructorName(m);
        Family f = family(m);
        return f == null ? null : f.name();
    }

    /** The name of {@code m}'s own implementation: {@code super.m()} calls it, the dispatching method too. */
    String implementationName(MethodInfo m) {
        Family f = family(m);
        if (f == null) return null;
        return f.dispatched() ? f.name() + "_" + RustNames.snake(RustNames.ownType(m.typeInfo())) : f.name();
    }

    /** All constructors of a hierarchy are associated functions of the root: {@code new_assignment_exprent}. */
    static String constructorName(MethodInfo c) {
        String own = RustNames.ownMethod(c);
        return own.replaceFirst("^new", "new_" + RustNames.snake(RustNames.ownType(c.typeInfo())));
    }

    /** The default constructor of a member without one. */
    static String defaultConstructorName(TypeInfo member) {
        return "new_" + RustNames.snake(RustNames.ownType(member));
    }
}
