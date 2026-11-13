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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

/**
 * Java's shared objects: the source classes whose objects are held as {@code crate::jrt::Shared<T>}, a reference
 * with Java's semantics (a clone shares the object; {@code ==} and hashing are identity). A class is shared when the
 * verdicts say Java shares its objects: some method or constructor keeps one (a dependent parameter, or one
 * independent with hidden content: moved in), and a field or a collection in a field holds them. So is a class whose
 * shape is a graph with back-links, holding its own kind both in a collection and in a plain field (linkedGraph): its
 * objects can be wired to each other between locals, where no verdict sees it. A closed hierarchy
 * (RustHierarchies) is shared as a whole, through its root, as its members print as the root.
 * <p>
 * A field the ownership pass calls owned is not a holder: a class held only in owned fields is not shared, its
 * objects moved into the one field that holds each.
 * <p>
 * Kept is read from the stored verdict itself, not from {@link RustFacts#passing}: a parameter without a verdict is
 * owned there too, and that is not evidence of sharing. Gate {@code NORUSTSHARED} switches the representation off.
 */
final class RustShared {

    static final boolean OFF = System.getenv("NORUSTSHARED") != null;

    private static final RustShared NONE = new RustShared(Set.of(), Map.of(), Set.of());
    private static final ThreadLocal<RustShared> CURRENT = ThreadLocal.withInitial(() -> NONE);

    private final Set<TypeInfo> shared;
    private final Map<TypeInfo, Set<TypeInfo>> cyclic;
    private final Set<TypeInfo> backLinks;

    private RustShared(Set<TypeInfo> shared, Map<TypeInfo, Set<TypeInfo>> cyclic, Set<TypeInfo> backLinks) {
        this.shared = shared;
        this.cyclic = cyclic;
        this.backLinks = backLinks;
    }

    /**
     * Whether a constructor of {@code ti} hands its {@code this} to an object that keeps it, as a back-link interface
     * (isBackLink): the object is built with {@code Shared::new_cyclic}, its constructors given a weak handle of it.
     */
    boolean isCyclic(TypeInfo ti) {
        return ti != null && cyclic.containsKey(struct(ti));
    }

    // a member of a closed hierarchy is its root's struct
    private static TypeInfo struct(TypeInfo ti) {
        TypeInfo root = RustHierarchies.current().root(ti);
        return root != null ? root : ti;
    }

    /**
     * The back-link interfaces a cyclic class hands out, its own or its superclass's: a subclass passes its weak
     * handle to the superclass's constructor, which hands it out as those (TestConsoleDecompiler extends
     * ConsoleDecompiler, whose 'new Fernflower(this, this, ...)' must reach the subclass's getBytecode).
     */
    Set<TypeInfo> handedOut(TypeInfo ti) {
        return ti == null ? Set.of() : cyclic.getOrDefault(struct(ti), Set.of());
    }

    /** Whether {@code ti} is a cyclic class with a cyclic subclass: its constructors take any object's handle. */
    boolean cyclicWithSubclasses(TypeInfo ti) {
        return isCyclic(ti) && cyclic.keySet().stream().anyMatch(t -> t != ti && t.superTypesExcludingJavaLangObject().contains(ti));
    }

    /** The cyclic superclass {@code ti} composes, whose constructor takes its handle; null when there is none. */
    TypeInfo cyclicSuperclass(TypeInfo ti) {
        if (ti == null || ti.parentClass() == null) return null;
        TypeInfo parent = ti.parentClass().typeInfo();
        return isCyclic(parent) ? parent : null;
    }

    /**
     * Whether {@code ti} is an interface held as a {@code crate::jrt::WeakShared<dyn I>}: a back-link to an object
     * that is not yet built when it is handed out (fernflower's Fernflower, which gives StructContext itself as its
     * IDecompiledData), and that its holder does not keep alive.
     */
    boolean isBackLink(TypeInfo ti) {
        return ti != null && backLinks.contains(ti);
    }

    static RustShared current() {
        return CURRENT.get();
    }

    static RustShared enter(RustShared value) {
        RustShared outer = CURRENT.get();
        CURRENT.set(value);
        return outer;
    }

    static void restore(RustShared outer) {
        CURRENT.set(outer);
    }

    /** Whether {@code ti}'s objects are held as {@code Shared<T>}: a member of a closed hierarchy through its root. */
    boolean isShared(TypeInfo ti) {
        if (ti == null || shared.isEmpty()) return false;
        TypeInfo root = RustHierarchies.current().root(ti);
        return shared.contains(root != null ? root : ti);
    }

    boolean isShared(ParameterizedType type) {
        return type != null && type.arrays() == 0 && !type.isTypeParameter() && isShared(type.typeInfo());
    }

    /** The shared classes among {@code types} and their nested types; call with the hierarchies entered. */
    static RustShared of(List<TypeInfo> types, RustFacts facts) {
        if (OFF) return NONE;
        Set<TypeInfo> kept = new HashSet<>();
        Set<TypeInfo> held = new HashSet<>();
        Set<TypeInfo> linkedGraph = new HashSet<>();
        for (TypeInfo t : types) visit(t, facts, kept, held, linkedGraph);
        Set<TypeInfo> shared = new HashSet<>();
        for (TypeInfo t : kept) {
            if (held.contains(t) && !view(t, facts)) shared.add(t);
        }
        shared.addAll(linkedGraph);
        Map<TypeInfo, Set<TypeInfo>> cyclic = new HashMap<>();
        Set<TypeInfo> backLinks = new HashSet<>();
        Map<TypeInfo, Set<TypeInfo>> handedOut = new HashMap<>();
        Map<TypeInfo, Set<TypeInfo>> implementations = new HashMap<>();
        for (TypeInfo t : types) selfHandOuts(t, facts, handedOut, implementations);
        handedOut.forEach((iface, classes) -> {
            // one class hands it out, and every implementation in the sources is that class or a subclass of it, so
            // that every value of the interface is such an object's handle
            if (classes.size() != 1) return;
            TypeInfo c = classes.iterator().next();
            Set<TypeInfo> impls = implementations.getOrDefault(iface, Set.of());
            if (!impls.stream().allMatch(t -> t == c || t.superTypesExcludingJavaLangObject().contains(c))) return;
            // a subclass composes its superclass, and its constructor passes its handle on; or it is a member of the
            // closed hierarchy c is the root of, the same struct
            if (impls.stream().anyMatch(t -> t != c && candidate(t, facts) != t && candidate(t, facts) != c)) return;
            backLinks.add(iface);
            for (TypeInfo t : impls) cyclic.computeIfAbsent(struct(t), k -> new HashSet<>()).add(iface);
            cyclic.computeIfAbsent(c, k -> new HashSet<>()).add(iface);
            shared.add(c);
            shared.addAll(impls);
        });
        Map<TypeInfo, Set<TypeInfo>> frozen = new HashMap<>();
        cyclic.forEach((k, v) -> frozen.put(k, Set.copyOf(v)));
        return shared.isEmpty() ? NONE : new RustShared(Set.copyOf(shared), Map.copyOf(frozen), Set.copyOf(backLinks));
    }

    /*
     Java's 'new StructContext(saver, this, loader)' in Fernflower's constructor: a bare 'this' handed, as a source
     interface, to a parameter that keeps it. Collects, per such interface, the classes doing that, and per source
     interface its implementations.
     */
    private static void selfHandOuts(TypeInfo type, RustFacts facts, Map<TypeInfo, Set<TypeInfo>> handedOut,
                                     Map<TypeInfo, Set<TypeInfo>> implementations) {
        if (!type.isInterface()) {
            for (TypeInfo st : type.superTypesExcludingJavaLangObject()) {
                if (st.isInterface() && !st.compilationUnit().externalLibrary()) {
                    implementations.computeIfAbsent(st, k -> new HashSet<>()).add(type);
                }
            }
        }
        if (candidate(type, facts) == type) {
            for (MethodInfo c : type.constructors()) {
                if (c.methodBody() == null) continue;
                c.methodBody().visit(e -> {
                    List<Expression> arguments;
                    MethodInfo callee;
                    if (e instanceof ConstructorCall cc && cc.constructor() != null) {
                        arguments = cc.parameterExpressions();
                        callee = cc.constructor();
                    } else if (e instanceof MethodCall mc) {
                        arguments = mc.parameterExpressions();
                        callee = mc.methodInfo();
                    } else {
                        return true;
                    }
                    for (int i = 0; i < arguments.size() && !callee.parameters().isEmpty(); i++) {
                        if (!(arguments.get(i) instanceof VariableExpression ve && ve.variable() instanceof This th
                              && th.explicitlyWriteType() == null && !th.writeSuper())) {
                            continue;
                        }
                        ParameterInfo p = callee.parameters().get(Math.min(i, callee.parameters().size() - 1));
                        ParameterizedType pt = p.parameterizedType();
                        TypeInfo iface = pt.arrays() == 0 && !pt.isTypeParameter() ? pt.typeInfo() : null;
                        if (iface != null && iface.isInterface() && !iface.compilationUnit().externalLibrary()
                            && (keptVerdict(p) || stored(p, 4))) {
                            handedOut.computeIfAbsent(iface, k -> new HashSet<>()).add(type);
                        }
                    }
                    return true;
                });
            }
        }
        type.subTypes().forEach(sub -> selfHandOuts(sub, facts, handedOut, implementations));
    }

    private static void visit(TypeInfo type, RustFacts facts, Set<TypeInfo> kept, Set<TypeInfo> held,
                              Set<TypeInfo> linkedGraph) {
        for (FieldInfo f : type.fields()) {
            if (f.isStatic()) continue;
            // an owned field (maddi-mod docs/roadmap/ownership.md) holds the only reference to its object: it is not
            // a second holder; a collection's elements are not judged, so they are still held
            if (owned(f)) {
                heldInElements(f.type(), facts, held);
            } else {
                heldIn(f.type(), facts, held);
            }
        }
        if (linkedGraph(type, facts)) linkedGraph.add(candidate(type, facts));
        for (MethodInfo m : type.constructorsAndMethods()) {
            for (ParameterInfo p : m.parameters()) {
                TypeInfo t = candidate(p.parameterizedType(), facts);
                if (t != null && keptVerdict(p)) kept.add(t);
            }
        }
        type.subTypes().forEach(sub -> visit(sub, facts, kept, held, linkedGraph));
    }

    // a field's type, and the element types of the collections and maps it is (a List<Edge>, Map<K, Statement>), also
    // of a source class that is one (fernflower's VBStyleCollection<Instruction, Integer> extends ArrayList<E>)
    private static void heldIn(ParameterizedType type, RustFacts facts, Set<TypeInfo> held) {
        TypeInfo t = candidate(type, facts);
        if (t != null) held.add(t);
        if (type.typeInfo() != null && (type.typeInfo().compilationUnit().externalLibrary() || collection(type.typeInfo()))) {
            for (ParameterizedType p : type.parameters()) heldIn(p, facts, held);
        }
    }

    private static void heldInElements(ParameterizedType type, RustFacts facts, Set<TypeInfo> held) {
        if (type.typeInfo() != null && (type.typeInfo().compilationUnit().externalLibrary() || collection(type.typeInfo()))) {
            for (ParameterizedType p : type.parameters()) heldIn(p, facts, held);
        }
    }

    /**
     * The ownership pass's {@code OWNED_FIELD} (maddi-mod docs/roadmap/ownership.md, run with {@code OWNERSHIP}): the
     * field's object is reachable only through the object holding the field, so the field holds it as a plain value,
     * moved in. Gate {@code NORUSTOWNED} ignores the verdict.
     */
    static boolean owned(FieldInfo f) {
        return !OWNED_OFF && f.analysis().getOrDefault(PropertyImpl.OWNED_FIELD, ValueImpl.BoolImpl.FALSE).isTrue();
    }

    static final boolean OWNED_OFF = System.getenv("NORUSTOWNED") != null;

    // a source class extending a JDK collection
    private static boolean collection(TypeInfo ti) {
        for (TypeInfo t = ti; t != null && !t.isJavaLangObject();
             t = t.parentClass() == null ? null : t.parentClass().typeInfo()) {
            if (t.compilationUnit().externalLibrary()) {
                return RustTypes.mapped(t) != null && !"String".equals(RustTypes.mapped(t));
            }
        }
        return false;
    }

    /*
     A view (RustBorrows): a field every constructor assigns a dependent parameter of a type that is not shared itself
     (a List<String> the caller owns) is a borrow with the struct's lifetime, and that design stays: the view borrows,
     it is not shared (TestJavaToRustMapping row 7).
     */
    private static boolean view(TypeInfo t, RustFacts facts) {
        List<MethodInfo> constructors = t.constructors().stream().filter(c -> !c.isSynthetic()).toList();
        if (constructors.isEmpty()) return false;
        for (FieldInfo f : t.fields()) {
            if (f.isStatic() || f.isSynthetic() || !f.isFinal() || f.initializer() != null && !f.initializer().isEmpty()
                || RustTypes.isCopy(f.type()) || RustTypes.isString(f.type()) || f.type().isTypeParameter()
                || RustTypes.isFunctional(f.type()) || candidate(f.type(), facts) != null || owned(f)) {
                continue;
            }
            if (constructors.stream().allMatch(c -> {
                ParameterInfo pi = RustBorrows.assigned(c, f);
                return pi != null && RustBorrows.dependent(pi);
            })) {
                return true;
            }
        }
        return false;
    }

    /*
     The type's shape, not a verdict (CodeLaser/maddi#202, Bart's option C): a class that holds its own kind both in a
     collection (its children) and in a plain field (a back-link to its parent) is a graph with back-links, which no
     single owner can hold, whatever the analysis says of its parameters. Fernflower's ClassesProcessor.ClassNode
     ('List<ClassNode> nested', 'ClassNode parent'): its objects are wired to each other between locals
     ('superNode.nested.add(nestedNode); nestedNode.parent = superNode'), where no parameter verdict sees the sharing.
     */
    private static boolean linkedGraph(TypeInfo type, RustFacts facts) {
        TypeInfo self = candidate(type, facts);
        if (self == null) return false;
        boolean backLink = false;
        boolean children = false;
        for (FieldInfo f : type.fields()) {
            if (f.isStatic()) continue;
            ParameterizedType ft = f.type();
            if (self.equals(candidate(ft, facts))) {
                backLink = true;
            } else if (ft.arrays() == 0 && ft.typeInfo() != null
                       && (ft.typeInfo().compilationUnit().externalLibrary() || collection(ft.typeInfo()))
                       && ft.parameters().stream().anyMatch(p -> self.equals(candidate(p, facts)))) {
                children = true;
            }
        }
        return backLink && children;
    }

    /*
     Whether Java keeps the parameter's object: its method assigns it to a field, or passes it on to a constructor
     (also this(...) or super(...)) that does, up to 'depth' calls deep. The independence verdict does not say so for
     an object without state the analysis sees: Fernflower's 'provider', an IBytecodeProvider that LazyLoader stores,
     is independent.
     */
    static boolean stored(ParameterInfo p, int depth) {
        MethodInfo m = p.methodInfo();
        if (depth == 0 || m == null || m.methodBody() == null) return false;
        boolean[] found = {false};
        m.methodBody().visit(e -> {
            if (found[0]) return false;
            if (e instanceof io.codelaser.maddi.cst.api.expression.Assignment a
                && a.target().variable() instanceof io.codelaser.maddi.cst.api.variable.FieldReference
                && a.value() instanceof VariableExpression ve && p.equals(ve.variable())) {
                found[0] = true;
            }
            List<Expression> arguments = null;
            MethodInfo callee = null;
            if (e instanceof ConstructorCall cc && cc.constructor() != null) {
                arguments = cc.parameterExpressions();
                callee = cc.constructor();
            } else if (e instanceof io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation eci
                       && eci.methodInfo() != null) {
                arguments = eci.parameterExpressions();
                callee = eci.methodInfo();
            }
            if (callee != null && !callee.typeInfo().compilationUnit().externalLibrary()) {
                for (int i = 0; i < arguments.size() && i < callee.parameters().size(); i++) {
                    if (arguments.get(i) instanceof VariableExpression ve && p.equals(ve.variable())
                        && stored(callee.parameters().get(i), depth - 1)) {
                        found[0] = true;
                    }
                }
            }
            return !found[0];
        });
        return found[0];
    }

    private static boolean keptVerdict(ParameterInfo p) {
        Value.Independent independent = p.analysis().getOrNull(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.class);
        return independent != null && !independent.isIndependent();
    }

    // a mutable source class, as its struct: a closed hierarchy's root
    private static TypeInfo candidate(ParameterizedType type, RustFacts facts) {
        if (type == null || type.arrays() > 0 || type.isTypeParameter()) return null;
        return candidate(type.typeInfo(), facts);
    }

    private static TypeInfo candidate(TypeInfo ti, RustFacts facts) {
        if (ti == null || ti.compilationUnit().externalLibrary() || ti.isInterface() || ti.typeNature().isEnum()
            || ti.typeNature().isRecord() || facts.immutable(ti) || facts.immutableHc(ti)) {
            return null;
        }
        TypeInfo root = RustHierarchies.current().root(ti);
        // a class with its own equals (PartialEq): Java's collections compare its objects by it, a Shared handle by
        // identity; it keeps its value semantics. A hierarchy's members' equals is not translated (rust-printing.md).
        if (root == null && ti.methods().stream().anyMatch(RustTypePrinter::isEquals)) return null;
        return root != null ? root : ti;
    }
}
