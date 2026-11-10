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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.analysis.PropertyImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;

/**
 * Java's shared objects: the source classes whose objects are held as {@code crate::jrt::Shared<T>}, a reference
 * with Java's semantics (a clone shares the object; {@code ==} and hashing are identity). A class is shared when the
 * verdicts say Java shares its objects: some method or constructor keeps one (a dependent parameter, or one
 * independent with hidden content: moved in), and a field or a collection in a field holds them. A closed hierarchy
 * (RustHierarchies) is shared as a whole, through its root, as its members print as the root.
 * <p>
 * Kept is read from the stored verdict itself, not from {@link RustFacts#passing}: a parameter without a verdict is
 * owned there too, and that is not evidence of sharing. Gate {@code NORUSTSHARED} switches the representation off.
 */
final class RustShared {

    static final boolean OFF = System.getenv("NORUSTSHARED") != null;

    private static final RustShared NONE = new RustShared(Set.of());
    private static final ThreadLocal<RustShared> CURRENT = ThreadLocal.withInitial(() -> NONE);

    private final Set<TypeInfo> shared;

    private RustShared(Set<TypeInfo> shared) {
        this.shared = shared;
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
        for (TypeInfo t : types) visit(t, facts, kept, held);
        Set<TypeInfo> shared = new HashSet<>();
        for (TypeInfo t : kept) {
            if (held.contains(t) && !view(t, facts)) shared.add(t);
        }
        return shared.isEmpty() ? NONE : new RustShared(Set.copyOf(shared));
    }

    private static void visit(TypeInfo type, RustFacts facts, Set<TypeInfo> kept, Set<TypeInfo> held) {
        for (FieldInfo f : type.fields()) {
            if (!f.isStatic()) heldIn(f.type(), facts, held);
        }
        for (MethodInfo m : type.constructorsAndMethods()) {
            for (ParameterInfo p : m.parameters()) {
                TypeInfo t = candidate(p.parameterizedType(), facts);
                if (t != null && keptVerdict(p)) kept.add(t);
            }
        }
        type.subTypes().forEach(sub -> visit(sub, facts, kept, held));
    }

    // a field's type, and the element types of the collections and maps it is (a List<Edge>, Map<K, Statement>)
    private static void heldIn(ParameterizedType type, RustFacts facts, Set<TypeInfo> held) {
        TypeInfo t = candidate(type, facts);
        if (t != null) held.add(t);
        if (type.typeInfo() != null && type.typeInfo().compilationUnit().externalLibrary()) {
            for (ParameterizedType p : type.parameters()) heldIn(p, facts, held);
        }
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
                || RustTypes.isFunctional(f.type()) || candidate(f.type(), facts) != null) {
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

    private static boolean keptVerdict(ParameterInfo p) {
        Value.Independent independent = p.analysis().getOrNull(PropertyImpl.INDEPENDENT_PARAMETER,
                ValueImpl.IndependentImpl.class);
        return independent != null && !independent.isIndependent();
    }

    // a mutable source class, as its struct: a closed hierarchy's root
    private static TypeInfo candidate(ParameterizedType type, RustFacts facts) {
        if (type == null || type.arrays() > 0 || type.isTypeParameter()) return null;
        TypeInfo ti = type.typeInfo();
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
