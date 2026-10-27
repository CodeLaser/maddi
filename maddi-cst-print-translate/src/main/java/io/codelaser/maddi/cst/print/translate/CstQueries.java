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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.EnclosedExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.statement.SwitchStatementOldStyle;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Questions about the Java CST that a Java-to-X printer asks whatever its target language: pure functions of the
 * elements, no printing state.
 */
public final class CstQueries {

    private CstQueries() {
    }

    /** The expression inside any parentheses. */
    public static Expression unwrap(Expression e) {
        Expression x = e;
        while (x instanceof EnclosedExpression ee) x = ee.inner();
        return x;
    }

    /** The statements a printer writes: those the source has, without the ones the CST added (synthetic). */
    public static List<Statement> own(List<Statement> statements) {
        return statements.stream().filter(s -> !s.isSynthetic()).toList();
    }

    /**
     * The value of a body that is a single {@code return x} ({@code own}: its {@link #own} statements), for an
     * expression body ({@code fun f() = x}, {@code int F() => x;}, a lambda's {@code x}); null for any other body.
     */
    public static Expression returnedValue(List<Statement> own) {
        return own.size() == 1 && own.getFirst() instanceof ReturnStatement rs && !rs.hasNoValue()
                ? rs.expression() : null;
    }

    /** The type {@code typeInfo} is a member of; null for a top-level type. */
    public static TypeInfo enclosing(TypeInfo typeInfo) {
        var cuOrEnclosing = typeInfo.compilationUnitOrEnclosingType();
        return cuOrEnclosing.isRight() ? cuOrEnclosing.getRight() : null;
    }

    /** The superclass of {@code typeInfo}; null for {@code Object}, an interface without one, or an unknown type. */
    public static TypeInfo superclass(TypeInfo typeInfo) {
        return typeInfo.parentClass() == null ? null : typeInfo.parentClass().typeInfo();
    }

    /** The superclasses of {@code typeInfo}, nearest first, up to {@code Object}; not {@code typeInfo} itself. */
    public static List<TypeInfo> superclasses(TypeInfo typeInfo) {
        List<TypeInfo> superclasses = new ArrayList<>();
        for (TypeInfo t = superclass(typeInfo); t != null; t = superclass(t)) superclasses.add(t);
        return superclasses;
    }

    /** {@code typeInfo}, then its {@link #superclasses}. */
    public static List<TypeInfo> classChain(TypeInfo typeInfo) {
        List<TypeInfo> chain = new ArrayList<>();
        for (TypeInfo t = typeInfo; t != null; t = superclass(t)) chain.add(t);
        return chain;
    }

    /** {@code t} is {@code owner} or a member type of it, at any depth. */
    public static boolean within(TypeInfo t, TypeInfo owner) {
        for (TypeInfo x = t; x != null; x = enclosing(x)) {
            if (x.equals(owner)) return true;
        }
        return false;
    }

    /** A field of {@code this}: {@code f} or {@code this.f}. */
    public static boolean onThis(FieldReference fr) {
        return fr.isDefaultScope() || fr.scope() instanceof VariableExpression ve && ve.variable() instanceof This;
    }

    /**
     * The code of a type itself: its constructors' and methods' bodies and its fields' initializers, not those of its
     * member types (see {@link #codeWithSubTypes}), nor those of the anonymous and local types inside.
     */
    public static List<Element> code(TypeInfo typeInfo) {
        List<Element> code = new ArrayList<>();
        addCode(typeInfo, code);
        return code;
    }

    /** As {@link #code}, with that of the member types, at any depth. */
    public static List<Element> codeWithSubTypes(TypeInfo typeInfo) {
        List<Element> code = new ArrayList<>();
        addCodeWithSubTypes(typeInfo, code);
        return code;
    }

    private static void addCodeWithSubTypes(TypeInfo typeInfo, List<Element> code) {
        addCode(typeInfo, code);
        typeInfo.subTypes().forEach(st -> addCodeWithSubTypes(st, code));
    }

    private static void addCode(TypeInfo typeInfo, List<Element> code) {
        Stream.concat(typeInfo.constructors().stream(), typeInfo.methods().stream())
                .filter(m -> m.methodBody() != null).forEach(m -> code.add(m.methodBody()));
        for (FieldInfo f : typeInfo.fields()) {
            if (f.initializer() != null && !f.initializer().isEmpty()) code.add(f.initializer());
        }
    }

    /** {@code type} is, or has among its type arguments at any depth, the type parameter {@code tp}. */
    public static boolean mentions(ParameterizedType type, TypeParameter tp) {
        if (type == null) return false;
        if (tp.equals(type.typeParameter())) return true;
        return type.parameters().stream().anyMatch(p -> mentions(p, tp));
    }

    /** {@code type} is, or has among its type arguments at any depth, one of {@code typeParameters}. */
    public static boolean mentionsAny(ParameterizedType type, List<TypeParameter> typeParameters) {
        return typeParameters.stream().anyMatch(tp -> mentions(type, tp));
    }

    /** {@code type} is, or has among its type arguments at any depth, a type parameter. */
    public static boolean mentionsTypeParameter(ParameterizedType type) {
        return type.isTypeParameter() || type.parameters().stream().anyMatch(CstQueries::mentionsTypeParameter);
    }

    /** A record's accessor of one of its components: an instance method without parameters named as a field. */
    public static boolean isRecordAccessor(MethodInfo m) {
        TypeInfo owner = m.typeInfo();
        return owner.typeNature().isRecord() && !m.isStatic() && m.parameters().isEmpty()
               && owner.fields().stream().anyMatch(f -> !f.isStatic() && f.name().equals(m.name()));
    }

    /** A class code can extend: not final, not a record, not an enum (Kotlin's {@code open}, C#'s non-sealed). */
    public static boolean isExtensible(TypeInfo typeInfo) {
        return typeInfo.typeNature().isClass() && !typeInfo.isFinal() && !typeInfo.typeNature().isRecord()
               && !typeInfo.typeNature().isEnum();
    }

    /**
     * The field's initializer is a compile-time constant of a primitive type or String: with a static final field,
     * Kotlin's {@code const val}, C#'s {@code const}.
     */
    public static boolean constantInitializer(FieldInfo fieldInfo) {
        return constantInitializer(fieldInfo, new java.util.HashSet<>());
    }

    private static boolean constantInitializer(FieldInfo fieldInfo, java.util.Set<FieldInfo> visiting) {
        ParameterizedType t = fieldInfo.type();
        if (fieldInfo.initializer() == null || fieldInfo.initializer().isEmpty() || t.arrays() != 0
            || !(t.isPrimitiveExcludingVoid() || t.isJavaLangString()) || !visiting.add(fieldInfo)) return false;
        return constantExpression(fieldInfo.initializer(), t.isJavaLangString(), visiting);
    }

    /**
     * A compile-time constant as Java's JLS 15.29 has it, and Kotlin's {@code const val} and C#'s {@code const}
     * accept: a literal, or operators over constants and other constant fields (nacos's
     * {@code String ENDPOINT_PLACEHOLDER = "${" + PREFIX + ENDPOINT + ":}"}, an annotation parameter's default). A
     * String one concatenates Strings only: Kotlin folds {@code "a" + 1} at no compile time.
     */
    private static boolean constantExpression(Expression e, boolean string, java.util.Set<FieldInfo> visiting) {
        while (e instanceof EnclosedExpression ee) e = ee.inner();
        if (e.isConstant()) return !string || e.parameterizedType() != null && e.parameterizedType().isJavaLangString();
        if (e instanceof io.codelaser.maddi.cst.api.expression.BinaryOperator bo) {
            return constantExpression(bo.lhs(), string, visiting) && constantExpression(bo.rhs(), string, visiting);
        }
        if (!string && e instanceof io.codelaser.maddi.cst.api.expression.UnaryOperator uo) {
            return constantExpression(uo.expression(), false, visiting);
        }
        if (e instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr) {
            FieldInfo f = fr.fieldInfo();
            boolean isFinal = f.isFinal() || f.owner().isInterface() || f.owner().typeNature().isAnnotation();
            return f.isStatic() && isFinal && (!string || f.type().isJavaLangString())
                   && constantInitializer(f, visiting);
        }
        return false;
    }

    /**
     * A group of an old-style switch: the labels that start at the same statement, and the statements up to the next
     * group's ({@link #own}). A group that does not end in a jump falls through into the next one, which each target
     * handles its own way (Kotlin copies the following groups, C# writes {@code goto case}).
     */
    public record SwitchGroup(List<SwitchStatementOldStyle.SwitchLabel> labels, List<Statement> own) {
    }

    /** The groups of an old-style switch, in source order. */
    public static List<SwitchGroup> switchGroups(SwitchStatementOldStyle sw) {
        List<Statement> statements = sw.block().statements();
        TreeMap<Integer, List<SwitchStatementOldStyle.SwitchLabel>> byStart = new TreeMap<>();
        for (SwitchStatementOldStyle.SwitchLabel l : sw.switchLabels()) {
            byStart.computeIfAbsent(l.startFromPosition(), _ -> new ArrayList<>()).add(l);
        }
        List<Integer> starts = new ArrayList<>(byStart.keySet());
        List<SwitchGroup> groups = new ArrayList<>(starts.size());
        for (int i = 0; i < starts.size(); i++) {
            int from = Math.min(starts.get(i), statements.size());
            int to = i + 1 < starts.size() ? Math.min(starts.get(i + 1), statements.size()) : statements.size();
            groups.add(new SwitchGroup(List.copyOf(byStart.get(starts.get(i))), own(statements.subList(from, to))));
        }
        return List.copyOf(groups);
    }
}
