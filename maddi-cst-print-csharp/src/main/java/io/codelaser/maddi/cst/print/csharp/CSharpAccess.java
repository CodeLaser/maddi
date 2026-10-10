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

import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.FieldModifier;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeModifier;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Java's {@code private} is private to the top-level type: a class reaches the private members of the classes nested
 * in it. C#'s is private to the type itself, and only nested types see their containing types' private members. A
 * private member or nested type that code outside its owner (and outside the types nested in it) reaches is therefore
 * printed {@code internal}; every other one stays private.
 */
final class CSharpAccess {

    private CSharpAccess() {
    }

    /** The private fields, methods, constructors and types of the file that need {@code internal}. */
    static Set<Object> reachedFromOutside(CompilationUnit compilationUnit) {
        Set<Object> reached = new HashSet<>();
        compilationUnit.types().forEach(t -> scan(t, reached));
        return reached;
    }

    private static void scan(TypeInfo x, Set<Object> reached) {
        Stream.concat(x.constructors().stream(), x.methods().stream()).forEach(m -> {
            signature(m.returnType(), x, reached);
            m.parameters().forEach(p -> signature(p.parameterizedType(), x, reached));
            if (m.methodBody() != null) body(m.methodBody(), x, reached);
        });
        for (FieldInfo f : x.fields()) {
            signature(f.type(), x, reached);
            if (f.initializer() != null && !f.initializer().isEmpty()) body(f.initializer(), x, reached);
        }
        if (x.parentClass() != null) signature(x.parentClass(), x, reached);
        x.interfacesImplemented().forEach(i -> signature(i, x, reached));
        x.subTypes().forEach(st -> scan(st, reached));
    }

    private static void body(Element element, TypeInfo x, Set<Object> reached) {
        element.visit((Element e) -> {
            switch (e) {
                case MethodCall mc -> member(mc.methodInfo(), mc.methodInfo().typeInfo(), x, reached);
                case MethodReference mr -> member(mr.methodInfo(), mr.methodInfo().typeInfo(), x, reached);
                case ConstructorCall cc when cc.constructor() != null ->
                        member(cc.constructor(), cc.constructor().typeInfo(), x, reached);
                case VariableExpression ve when ve.variable() instanceof FieldReference fr ->
                        member(fr.fieldInfo(), fr.fieldInfo().owner(), x, reached);
                default -> {
                }
            }
            return true;
        });
        element.typesReferenced(null).forEach(tr -> type(tr.typeInfo(), x, reached));
    }

    private static void signature(ParameterizedType pt, TypeInfo x, Set<Object> reached) {
        if (pt == null) return;
        if (pt.typeInfo() != null) type(pt.typeInfo(), x, reached);
        pt.parameters().forEach(p -> signature(p, x, reached));
    }

    private static void type(TypeInfo t, TypeInfo x, Set<Object> reached) {
        if (t == null || t.isPrimaryType()) return;
        if (t.typeModifiers().stream().anyMatch(TypeModifier::isPrivate)) {
            TypeInfo owner = enclosing(t);
            if (owner != null && !within(x, owner)) reached.add(t);
        }
    }

    private static void member(Object info, TypeInfo owner, TypeInfo x, Set<Object> reached) {
        boolean isPrivate = switch (info) {
            case FieldInfo f -> f.modifiers().stream().anyMatch(FieldModifier::isPrivate);
            case MethodInfo m -> m.access() != null && m.access().isPrivate();
            default -> false;
        };
        if (isPrivate && !within(x, owner)) reached.add(info);
    }

    /** {@code x} is {@code owner}, or nested in it. */
    private static boolean within(TypeInfo x, TypeInfo owner) {
        for (TypeInfo t = x; t != null; t = enclosing(t)) {
            if (t == owner) return true;
        }
        return false;
    }

    private static TypeInfo enclosing(TypeInfo t) {
        var cuOrEnclosing = t.compilationUnitOrEnclosingType();
        return cuOrEnclosing.isRight() ? cuOrEnclosing.getRight() : null;
    }
}
