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
package io.codelaser.maddi.cst.impl.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.SynchronizedStatement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;

/**
 * Which classes of a program could be value types: a C# {@code readonly struct} or {@code record struct}, a Kotlin
 * {@code value class}, a Rust {@code Copy}/{@code Clone} struct, a Java value class or record. One answer for every
 * translator and refactoring, which each pick the {@link Obstacle}s that matter to them: a record keeps its identity,
 * so the identity obstacles do not stop one; a Java value class has no identity, so they do; a C# struct also cares
 * where its type is given {@code null}.
 * <p>
 * An index of a set of types, as {@code MethodFamilies}: built once, every type of the program given (the local and
 * anonymous classes too, whose code uses the others), and built again when they change. A class is a candidate when
 * it is a class or a record of those types; an interface, an enum, an annotation and an anonymous class are not.
 * <p>
 * Two kinds of obstacle. The class's own shape: its superclass, its subclasses, its fields, its constructors. And the
 * program's uses of its values that need an identity ({@code ==}, {@code synchronized}, an identity hash), or that
 * give it {@code null}: those come with their {@link Site}s, so that a refactoring can show or rewrite them.
 * <p>
 * Deep immutability is not an obstacle here but a fact ({@link Verdict#immutable()}): a Java value class and a C#
 * {@code readonly struct} are shallowly immutable and may hold a mutable list; a Rust {@code Copy} may not.
 */
public final class ValueTypes {

    /** What stands in the way of a class being a value type, for some target. */
    public enum Obstacle {
        /** An abstract class: no value of its own type exists. */
        ABSTRACT,
        /** A class of the program extends it: a value type is final. */
        SUBCLASSED,
        /** It extends a class other than {@code Object} (a record's {@code Record} does not count). */
        SUPERCLASS,
        /** An inner class that uses its enclosing instance: a value would carry it. */
        USES_ENCLOSING_INSTANCE,
        /** An instance field is assigned outside the constructors and initializers of its class. */
        MUTABLE_FIELD,
        /** A constructor hands {@code this} out (an argument, a value assigned) before the value is complete. */
        THIS_ESCAPES,
        /** {@code synchronized} on one of its values, or a {@code synchronized} instance method. */
        SYNCHRONIZED,
        /** Two of its values compared with {@code ==} or {@code !=}, which is identity, not equality. */
        IDENTITY_COMPARISON,
        /** {@code System.identityHashCode}, an {@code IdentityHashMap} or {@code WeakHashMap} key, a reference object. */
        IDENTITY_HASH,
        /** A value of its type is given or compared with {@code null}: a struct needs {@code T?} there. */
        NULL_USED,
        /** It has no instance field. */
        NO_FIELDS;

        /** The uses that need the object's identity: they stop a Java value class, a struct, a Kotlin value class. */
        public boolean isIdentity() {
            return this == SYNCHRONIZED || this == IDENTITY_COMPARISON || this == IDENTITY_HASH;
        }
    }

    /**
     * One place an obstacle is written.
     *
     * @param obstacle what it is
     * @param in       the method, constructor or field whose code it is in; the class itself for a modifier
     * @param element  the expression or statement; null for a modifier or a declaration
     */
    public record Site(Obstacle obstacle, Info in, Element element) {
    }

    /**
     * The answer for one class.
     *
     * @param type      the class
     * @param obstacles every obstacle found, in {@link Obstacle} order
     * @param sites     where the obstacles are written, in the order of the walk
     * @param fields    its instance fields, in declaration order
     * @param immutable the analysis's verdict: true when deeply immutable (with or without hidden content), false
     *                  when not, null when no analysis ran
     * @param ownEquals it declares {@code equals}: its values compare by content already
     */
    public record Verdict(TypeInfo type, Set<Obstacle> obstacles, List<Site> sites, List<FieldInfo> fields,
                          Boolean immutable, boolean ownEquals) {

        /** None of the identity obstacles. */
        public boolean identityFree() {
            return obstacles.stream().noneMatch(Obstacle::isIdentity);
        }

        /** None of the obstacles given. */
        public boolean free(Set<Obstacle> of) {
            return obstacles.stream().noneMatch(of::contains);
        }

        public List<Site> sites(Obstacle obstacle) {
            return sites.stream().filter(s -> s.obstacle() == obstacle).toList();
        }
    }

    private static final Set<String> IDENTITY_KEYED = Set.of("java.util.IdentityHashMap", "java.util.WeakHashMap",
            "java.lang.ref.WeakReference", "java.lang.ref.SoftReference", "java.lang.ref.PhantomReference");

    private final Map<TypeInfo, Verdict> verdicts;

    private ValueTypes(Map<TypeInfo, Verdict> verdicts) {
        this.verdicts = verdicts;
    }

    /**
     * The candidates among {@code types}, judged against the code of all of {@code types}: give every type of the
     * program, member, local and anonymous ones included, or a use is missed.
     */
    public static ValueTypes of(Collection<TypeInfo> types) {
        Map<TypeInfo, Builder> builders = new LinkedHashMap<>();
        for (TypeInfo t : types) {
            if (candidate(t)) builders.put(t, new Builder(t));
        }
        for (TypeInfo t : types) {
            ParameterizedType parent = t.parentClass();
            Builder subclassed = parent == null ? null : builders.get(parent.typeInfo());
            if (subclassed != null) subclassed.add(Obstacle.SUBCLASSED, t, null);
            for (FieldInfo f : t.fields()) {
                if (f.initializer() != null && !f.initializer().isEmpty()) walk(f, f.initializer(), builders);
            }
            t.constructorAndMethodStream().forEach(m -> {
                if (m.methodBody() != null && !m.methodBody().isEmpty()) walk(m, m.methodBody(), builders);
            });
        }
        builders.values().forEach(Builder::shape);
        Map<TypeInfo, Verdict> verdicts = new LinkedHashMap<>();
        builders.forEach((t, b) -> verdicts.put(t, b.build()));
        return new ValueTypes(Collections.unmodifiableMap(verdicts));
    }

    private static boolean candidate(TypeInfo t) {
        return (t.typeNature().isClass() || t.typeNature().isRecord()) && !t.isAnonymous();
    }

    /** The verdict for a candidate; null for a type that is none (an interface, an enum, a type not given). */
    public Verdict verdict(TypeInfo typeInfo) {
        return verdicts.get(typeInfo);
    }

    /** Every candidate's verdict, in the order of the types given. */
    public Collection<Verdict> verdicts() {
        return verdicts.values();
    }

    private static void walk(Info in, Element code, Map<TypeInfo, Builder> builders) {
        code.visit((Element e) -> {
            switch (e) {
                case Assignment a -> {
                    if (a.variableTarget() instanceof FieldReference fr && !fr.fieldInfo().isStatic()) {
                        Builder owner = builders.get(fr.fieldInfo().owner());
                        if (owner != null && !initializes(in, fr.fieldInfo().owner())) {
                            owner.add(Obstacle.MUTABLE_FIELD, in, a);
                        }
                    }
                    if (a.value() != null && a.value().isNullConstant()) {
                        nullFor(typeOf(a.target()), in, a, builders);
                    }
                }
                case BinaryOperator bo when referenceEquality(bo) -> {
                    TypeInfo left = typeOf(bo.lhs());
                    TypeInfo right = typeOf(bo.rhs());
                    if (bo.lhs().isNullConstant()) nullFor(right, in, bo, builders);
                    else if (bo.rhs().isNullConstant()) nullFor(left, in, bo, builders);
                    else {
                        Builder b = builders.get(left);
                        if (b == null) b = builders.get(right);
                        if (b != null) b.add(Obstacle.IDENTITY_COMPARISON, in, bo);
                    }
                }
                case LocalVariableCreation lvc -> lvc.localVariableStream().forEach(lv -> {
                    if (lv.assignmentExpression() != null && lv.assignmentExpression().isNullConstant()) {
                        ParameterizedType pt = lv.parameterizedType();
                        if (pt != null && pt.arrays() == 0) nullFor(pt.typeInfo(), in, lvc, builders);
                    }
                });
                case ReturnStatement r when in instanceof MethodInfo m && r.expression() != null
                                            && r.expression().isNullConstant() && m.returnType() != null
                                            && m.returnType().arrays() == 0 ->
                        nullFor(m.returnType().typeInfo(), in, r, builders);
                case SynchronizedStatement s -> {
                    Builder b = builders.get(typeOf(s.expression()));
                    if (b != null) b.add(Obstacle.SYNCHRONIZED, in, s);
                }
                case MethodCall mc -> {
                    MethodInfo callee = mc.methodInfo();
                    if (callee != null && "identityHashCode".equals(callee.name())
                        && "java.lang.System".equals(callee.typeInfo().fullyQualifiedName())
                        && !mc.parameterExpressions().isEmpty()) {
                        Builder b = builders.get(typeOf(mc.parameterExpressions().getFirst()));
                        if (b != null) b.add(Obstacle.IDENTITY_HASH, in, mc);
                    }
                    if (callee != null) nullArguments(callee, mc.parameterExpressions(), in, mc, builders);
                }
                case ConstructorCall cc -> {
                    if (cc.constructor() != null) nullArguments(cc.constructor(), cc.parameterExpressions(), in, cc, builders);
                    identityKeyed(cc.parameterizedType(), in, cc, builders);
                }
                default -> {
                }
            }
            return true;
        });
    }

    /** {@code in} is code that may assign the fields of {@code owner}: one of its constructors or initializers. */
    private static boolean initializes(Info in, TypeInfo owner) {
        if (in instanceof MethodInfo m) {
            return m.typeInfo() == owner && (m.isConstructor() || m.isInstanceInitializer());
        }
        return in instanceof FieldInfo f && f.owner() == owner && !f.isStatic();
    }

    private static boolean referenceEquality(BinaryOperator bo) {
        MethodInfo operator = bo.operator();
        return operator != null && operator.typeInfo() != null && operator.typeInfo().isJavaLangObject()
               && ("==".equals(operator.name()) || "!=".equals(operator.name()));
    }

    private static TypeInfo typeOf(Expression e) {
        ParameterizedType pt = e == null ? null : e.parameterizedType();
        return pt == null || pt.arrays() > 0 ? null : pt.typeInfo();
    }

    private static void nullFor(TypeInfo type, Info in, Element e, Map<TypeInfo, Builder> builders) {
        Builder b = type == null ? null : builders.get(type);
        if (b != null) b.add(Obstacle.NULL_USED, in, e);
    }

    private static void nullArguments(MethodInfo callee, List<Expression> arguments, Info in, Element e,
                                      Map<TypeInfo, Builder> builders) {
        List<ParameterInfo> parameters = callee.parameters();
        for (int i = 0; i < arguments.size() && i < parameters.size(); i++) {
            if (!arguments.get(i).isNullConstant()) continue;
            ParameterizedType type = parameters.get(i).parameterizedType();
            if (type != null && type.arrays() == 0) nullFor(type.typeInfo(), in, e, builders);
        }
    }

    private static void identityKeyed(ParameterizedType pt, Info in, Element e, Map<TypeInfo, Builder> builders) {
        if (pt == null || pt.typeInfo() == null || pt.parameters().isEmpty()
            || !IDENTITY_KEYED.contains(pt.typeInfo().fullyQualifiedName())) return;
        Builder b = builders.get(pt.parameters().getFirst().typeInfo());
        if (b != null) b.add(Obstacle.IDENTITY_HASH, in, e);
    }

    private static final class Builder {
        private final TypeInfo type;
        private final Set<Obstacle> obstacles = EnumSet.noneOf(Obstacle.class);
        private final List<Site> sites = new ArrayList<>();

        Builder(TypeInfo type) {
            this.type = type;
        }

        void add(Obstacle obstacle, Info in, Element element) {
            obstacles.add(obstacle);
            sites.add(new Site(obstacle, in, element));
        }

        /** The obstacles of the class's own declaration. */
        void shape() {
            if (type.isAbstract()) add(Obstacle.ABSTRACT, type, null);
            ParameterizedType parent = type.parentClass();
            if (parent != null && parent.typeInfo() != null && !parent.typeInfo().isJavaLangObject()
                && !"java.lang.Record".equals(parent.typeInfo().fullyQualifiedName())) {
                add(Obstacle.SUPERCLASS, type, null);
            }
            if (type.isInnerClass() && type.usesEnclosingInstance()) add(Obstacle.USES_ENCLOSING_INSTANCE, type, null);
            type.methods().stream().filter(m -> m.isSynchronized() && !m.isStatic())
                    .forEach(m -> add(Obstacle.SYNCHRONIZED, m, null));
            type.constructors().forEach(c -> {
                if (c.methodBody() != null && !c.methodBody().isEmpty()) thisEscapes(c);
            });
            if (fields().isEmpty()) add(Obstacle.NO_FIELDS, type, null);
        }

        private void thisEscapes(MethodInfo constructor) {
            constructor.methodBody().visit((Element e) -> {
                List<Expression> handedOut = switch (e) {
                    case MethodCall mc -> mc.parameterExpressions();
                    case ConstructorCall cc -> cc.parameterExpressions();
                    case Assignment a -> a.value() == null ? List.of() : List.of(a.value());
                    default -> List.of();
                };
                for (Expression x : handedOut) {
                    if (x instanceof VariableExpression ve && ve.variable() instanceof This th && !th.writeSuper()
                        && th.explicitlyWriteType() == null && th.typeInfo() == type) {
                        add(Obstacle.THIS_ESCAPES, constructor, e);
                    }
                }
                return true;
            });
        }

        private List<FieldInfo> fields() {
            return type.fields().stream().filter(f -> !f.isStatic()).toList();
        }

        Verdict build() {
            Value.Immutable immutable = type.analysis().getOrNull(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.class);
            Boolean deep = immutable == null ? null : immutable.isAtLeastImmutableHC();
            boolean ownEquals = type.methods().stream().anyMatch(m -> "equals".equals(m.name())
                                                                      && m.parameters().size() == 1 && !m.isStatic());
            return new Verdict(type, Collections.unmodifiableSet(EnumSet.copyOf(obstacles.isEmpty()
                    ? EnumSet.noneOf(Obstacle.class) : obstacles)), List.copyOf(sites), fields(), deep, ownEquals);
        }
    }
}
