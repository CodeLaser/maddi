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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.This;

/**
 * Where the code of an inner class uses an instance of a type around it: what a class captures, and so what making
 * it {@code static}, or translating it to a language without inner classes, has to replace.
 * <p>
 * Two forms, both found on corpora before they were written here:
 * <ul>
 *     <li>a {@link This} that denotes an enclosing instance. A written {@code Outer.this}, and also an unqualified
 *     read of an instance member of a type around the class: the CST binds that to a {@code This} of the type that
 *     declares the member, which may be a superclass of the enclosing type, enclosing nothing itself (elasticsearch's
 *     {@code TransportClusterStatsAction.AdditionalStats} reads {@code clusterService} of a transport-action
 *     superclass);</li>
 *     <li>the unqualified creation of another inner class, whose enclosing instance is passed implicitly:
 *     {@code new Counter()}, {@code new Counter() {…}}, {@code Counter::new} (pulsar's
 *     {@code computeIfAbsent(name, PrometheusCounter::new)}).</li>
 * </ul>
 * The code looked in is that of the class and of every type declared in it, member, local and anonymous: they reach
 * the same instance through it.
 */
public final class EnclosingInstance {

    private EnclosingInstance() {
    }

    /**
     * The type around {@code inner} whose instance a {@code This} bound to {@code boundTo}, written in {@code inner}'s
     * code, denotes; null when it is {@code inner}'s own instance, or nothing around it. The exclusion comes first:
     * when {@code inner} or one of its supertypes is (or declares) {@code boundTo}, the reference is {@code this}, as
     * Java resolves to the innermost declaration.
     */
    public static TypeInfo denotedBy(TypeInfo boundTo, TypeInfo inner) {
        if (boundTo == null || boundTo.equals(inner) || isSupertypeOf(boundTo, inner)) return null;
        for (TypeInfo walk = inner.enclosingType(); walk != null; walk = walk.enclosingType()) {
            if (boundTo.equals(walk) || isSupertypeOf(boundTo, walk)) return walk;
        }
        return null;
    }

    /** A {@code This} bound to {@code boundTo}, in {@code inner}'s code, denotes an instance around it. */
    public static boolean isEnclosingInstance(TypeInfo boundTo, TypeInfo inner) {
        return denotedBy(boundTo, inner) != null;
    }

    /**
     * {@code thisVar}, in {@code inner}'s code, denotes an instance around it. A WRITTEN {@code C.this} names C's
     * lexically enclosing instance (JLS 15.8.4), never a supertype's: guava's {@code SlicedByteSource extends
     * ByteSource}, nested in {@code ByteSource}, reads {@code ByteSource.this.openStream()}, a capture, which the
     * supertype exclusion of {@link #denotedBy} would take for its own {@code this}. A {@code super} is not looked at
     * here: the caller decides what {@code X.super} means to it.
     */
    public static boolean captures(This thisVar, TypeInfo inner) {
        TypeInfo written = thisVar.explicitlyWriteType();
        if (written != null) return isStrictlyEnclosing(written, inner);
        return isEnclosingInstance(thisVar.typeInfo(), inner);
    }

    /** {@code candidate} is a type around {@code inner}: not {@code inner} itself, nor one of its own nested types. */
    public static boolean isStrictlyEnclosing(TypeInfo candidate, TypeInfo inner) {
        if (candidate == null || candidate.equals(inner)) return false;
        for (TypeInfo walk = inner.enclosingType(); walk != null; walk = walk.enclosingType()) {
            if (candidate.equals(walk)) return true;
        }
        return false;
    }

    /**
     * {@code element}, in {@code inner}'s code, is an unqualified creation of an inner class that takes an instance
     * around {@code inner} implicitly: {@code new Counter()}, {@code new Counter() {…}}, {@code Counter::new}. A
     * qualified {@code outer.new Counter()} names its instance; an inner class declared inside {@code inner} takes
     * {@code inner}'s own.
     */
    public static boolean createsWithEnclosingInstance(Element element, TypeInfo inner) {
        TypeInfo created = createdInnerClass(element);
        return created != null && isEnclosingInstance(created.enclosingType(), inner);
    }

    /**
     * The inner class an unqualified creation creates, whose enclosing instance it passes implicitly; null for
     * anything else: a qualified creation, a static, local or anonymous class, an element that creates nothing.
     */
    public static TypeInfo createdInnerClass(Element element) {
        TypeInfo created = null;
        if (element instanceof ConstructorCall cc && cc.object() == null) {
            if (cc.anonymousClass() != null) {
                ParameterizedType parent = cc.anonymousClass().parentClass();
                created = parent == null ? null : parent.typeInfo();
            } else if (cc.callsConstructorOfCreatedType()) {
                created = cc.constructor().typeInfo();
            }
        } else if (element instanceof MethodReference mr && mr.methodInfo() != null
                   && mr.methodInfo().isConstructor()) {
            created = mr.methodInfo().typeInfo();
        }
        if (created == null || !created.isInnerClass() || created.isAnonymous() || created.enclosingMethod() != null
            || created.enclosingType() == null) {
            return null;
        }
        return created;
    }

    /**
     * Every use of an instance around {@code inner} in its code and in that of the types declared in it: the
     * {@code This} expressions that denote one (not a {@code super}), and the creations that pass one. In the order
     * of the walk; its size is a cost: each one is a place to rewrite.
     */
    public static List<Element> uses(TypeInfo inner) {
        List<Element> uses = new ArrayList<>();
        walk(inner, element -> {
            if (element instanceof VariableExpression ve && ve.variable() instanceof This thisVar
                && !thisVar.writeSuper() && captures(thisVar, inner)) {
                uses.add(element);
            }
            if (createsWithEnclosingInstance(element, inner)) uses.add(element);
            return true;
        });
        return List.copyOf(uses);
    }

    /**
     * {@code inner}, or a type declared in it, names a type parameter declared by a TYPE around {@code inner}, in a
     * signature, a field's type, a supertype or an expression's type. A static nested class would have to declare
     * each of them itself. A method's own type parameters travel with the method and do not count.
     */
    public static boolean usesTypeParameters(TypeInfo inner) {
        boolean[] found = new boolean[1];
        inner.recursiveSubTypeStreamIncludingLocal().forEach(t -> {
            t.constructorAndMethodStream().forEach(m -> {
                m.parameters().forEach(p -> check(p.parameterizedType(), inner, found));
                check(m.returnType(), inner, found);
            });
            t.fields().forEach(f -> check(f.type(), inner, found));
            t.interfacesImplemented().forEach(i -> check(i, inner, found));
            if (t.parentClass() != null) check(t.parentClass(), inner, found);
        });
        if (found[0]) return true;
        walk(inner, element -> {
            if (element instanceof Expression e) check(e.parameterizedType(), inner, found);
            return !found[0];
        });
        return found[0];
    }

    private static void check(ParameterizedType type, TypeInfo inner, boolean[] found) {
        if (found[0] || type == null) return;
        TypeParameter tp = type.typeParameter();
        if (tp != null && tp.getOwner().isLeft() && isStrictlyEnclosing(tp.getOwner().getLeft(), inner)) {
            found[0] = true;
            return;
        }
        type.parameters().forEach(p -> check(p, inner, found));
    }

    private static boolean isSupertypeOf(TypeInfo candidate, TypeInfo type) {
        return type.superTypesExcludingJavaLangObject().contains(candidate);
    }

    /** The field initializers, constructors and method bodies of {@code type} and of every type declared in it. */
    private static void walk(TypeInfo type, Predicate<Element> visitor) {
        Set<TypeInfo> seen = new HashSet<>();
        ArrayDeque<TypeInfo> queue = new ArrayDeque<>(type.recursiveSubTypeStreamIncludingLocal().toList());
        while (!queue.isEmpty()) {
            TypeInfo t = queue.poll();
            if (!seen.add(t)) continue;
            for (FieldInfo field : t.fields()) {
                if (field.initializer() != null && !field.initializer().isEmpty()) field.initializer().visit(visitor);
            }
            t.constructorAndMethodStream().forEach(m -> {
                if (m.methodBody() != null && !m.methodBody().isEmpty()) m.methodBody().visit(visitor);
            });
        }
    }
}
