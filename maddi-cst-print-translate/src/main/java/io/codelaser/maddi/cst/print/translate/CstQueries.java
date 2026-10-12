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
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.This;

import java.util.ArrayList;
import java.util.List;
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
        if (fieldInfo.initializer() == null || !fieldInfo.initializer().isConstant()) return false;
        ParameterizedType t = fieldInfo.type();
        return t.arrays() == 0 && (t.isPrimitiveExcludingVoid() || t.isJavaLangString());
    }
}
