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
            // a member that is not private exposes the types of its signature: they cannot be more private
            Exposure exposed = m.isSynthetic() ? Exposure.NONE
                    : cap(m.access() == null || m.access().isPackage() ? Exposure.INTERNAL
                    : m.access().isPrivate() ? Exposure.NONE : Exposure.PUBLIC, x);
            signature(m.returnType(), x, reached, exposed);
            m.parameters().forEach(p -> signature(p.parameterizedType(), x, reached, exposed));
            if (m.methodBody() != null) body(m.methodBody(), x, reached, CSharpNames.inCompanion(m));
        });
        for (FieldInfo f : x.fields()) {
            // a record's components are its properties, as visible as the record
            boolean component = x.typeNature().isRecord() && !f.isStatic();
            signature(f.type(), x, reached, f.isSynthetic() ? Exposure.NONE : cap(component ? Exposure.PUBLIC
                    : f.modifiers().stream().anyMatch(FieldModifier::isPrivate) ? Exposure.NONE
                    : f.modifiers().stream().anyMatch(fm -> fm.isPublic() || fm.isProtected()) ? Exposure.PUBLIC
                    : Exposure.INTERNAL, x));
            if (f.initializer() != null && !f.initializer().isEmpty()) {
                body(f.initializer(), x, reached, CSharpNames.inCompanion(f));
            }
        }
        if (x.parentClass() != null) signature(x.parentClass(), x, reached, Exposure.NONE);
        x.interfacesImplemented().forEach(i -> signature(i, x, reached, Exposure.NONE));
        x.subTypes().forEach(st -> scan(st, reached));
    }

    /** {@code fromCompanion}: code of a static member that C# declares in the companion class of {@code x}. */
    private static void body(Element element, TypeInfo x, Set<Object> reached, boolean fromCompanion) {
        element.visit((Element e) -> {
            switch (e) {
                case MethodCall mc -> member(mc.methodInfo(), mc.methodInfo().typeInfo(), x, reached, fromCompanion);
                case MethodReference mr -> member(mr.methodInfo(), mr.methodInfo().typeInfo(), x, reached, fromCompanion);
                case ConstructorCall cc when cc.constructor() != null ->
                        member(cc.constructor(), cc.constructor().typeInfo(), x, reached, fromCompanion);
                case VariableExpression ve when ve.variable() instanceof FieldReference fr ->
                        member(fr.fieldInfo(), fr.fieldInfo().owner(), x, reached, fromCompanion);
                default -> {
                }
            }
            return true;
        });
        element.typesReferenced(null).forEach(tr -> type(tr.typeInfo(), x, reached));
    }

    /** How far a member's signature is visible: its types must be visible as far. */
    private enum Exposure { NONE, INTERNAL, PUBLIC }

    /** A member is no more visible than its type: a public member of an internal class is internal. */
    private static Exposure cap(Exposure exposure, TypeInfo x) {
        if (exposure != Exposure.PUBLIC) return exposure;
        for (TypeInfo t = x; t != null; t = enclosing(t)) {
            if (t.typeModifiers().stream().anyMatch(TypeModifier::isPrivate)) return Exposure.NONE;
            if (t.typeModifiers().stream().noneMatch(m -> m.isPublic() || m.isProtected())) return Exposure.INTERNAL;
        }
        return Exposure.PUBLIC;
    }

    private static void signature(ParameterizedType pt, TypeInfo x, Set<Object> reached, Exposure exposed) {
        if (pt == null) return;
        if (pt.typeInfo() != null) type(pt.typeInfo(), x, reached, exposed);
        pt.parameters().forEach(p -> signature(p, x, reached, exposed));
    }

    private static void type(TypeInfo t, TypeInfo x, Set<Object> reached) {
        type(t, x, reached, Exposure.NONE);
    }

    private static void type(TypeInfo t, TypeInfo x, Set<Object> reached, Exposure exposed) {
        if (t == null || t.isPrimaryType() || !CSharpNames.translated(t)) return;
        // C#'s nested type is no more visible than the types it is nested in
        if (exposed != Exposure.NONE) type(enclosing(t), x, reached, exposed);
        boolean isPublic = t.typeModifiers().stream().anyMatch(TypeModifier::isPublic);
        // Java's public method may name a less accessible type, C#'s may not: the type becomes as visible
        if (exposed == Exposure.PUBLIC && !isPublic) reached.add(new Exposed(t));
        if (t.typeModifiers().stream().anyMatch(TypeModifier::isPrivate)) {
            TypeInfo owner = enclosing(t);
            if (owner != null && (exposed == Exposure.INTERNAL || !within(x, owner))) reached.add(t);
        }
    }

    /** A nested type that the signature of a member that is not private names. */
    record Exposed(TypeInfo typeInfo) {
    }

    private static void member(Object info, TypeInfo owner, TypeInfo x, Set<Object> reached, boolean fromCompanion) {
        boolean isPrivate = switch (info) {
            case FieldInfo f -> f.modifiers().stream().anyMatch(FieldModifier::isPrivate);
            case MethodInfo m -> m.access() != null && m.access().isPrivate();
            default -> false;
        };
        if (!isPrivate) return;
        // C#'s private is the declaring class's: the companion and its generic type are siblings
        boolean accessible;
        if (CSharpNames.inCompanion(info)) accessible = fromCompanion && x.equals(owner);
        else if (fromCompanion) {
            accessible = !CSharpNames.hoisted(x) && enclosing(x) != null && within(enclosing(x), owner) && !owner.equals(x);
        }
        else accessible = within(x, owner);
        if (!accessible) reached.add(info);
    }

    /** {@code x} is {@code owner}, or nested in it in C#: a hoisted type is not (see {@link CSharpNames#hoisted}). */
    private static boolean within(TypeInfo x, TypeInfo owner) {
        for (TypeInfo t = x; t != null; t = CSharpNames.hoisted(t) ? null : enclosing(t)) {
            if (t == owner) return true;
        }
        return false;
    }

    private static TypeInfo enclosing(TypeInfo t) {
        var cuOrEnclosing = t.compilationUnitOrEnclosingType();
        return cuOrEnclosing.isRight() ? cuOrEnclosing.getRight() : null;
    }
}
