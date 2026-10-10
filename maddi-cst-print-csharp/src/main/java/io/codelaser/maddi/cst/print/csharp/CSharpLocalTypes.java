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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.RecordPattern;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.This;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * C# has no local classes. A local class that captures nothing of its method (no local variable, no parameter, no
 * instance member of the enclosing object) is lifted: printed as a private nested type of the enclosing type, where
 * the method's code names it as before. One that captures is not translated ({@code LOCAL_CLASS}).
 */
final class CSharpLocalTypes {

    private CSharpLocalTypes() {
    }

    /** The local types declared in the methods and constructors of {@code typeInfo} that are lifted into it. */
    static List<TypeInfo> lifted(TypeInfo typeInfo) {
        List<TypeInfo> lifted = new ArrayList<>();
        Stream.concat(typeInfo.constructors().stream(), typeInfo.methods().stream()).forEach(m -> {
            if (m.methodBody() == null) return;
            m.methodBody().visit((Element e) -> {
                if (e instanceof LocalTypeDeclaration ltd && liftable(ltd.typeInfo())) lifted.add(ltd.typeInfo());
                return true;
            });
        });
        return lifted;
    }

    static boolean liftable(TypeInfo local) {
        MethodInfo method = local.enclosingMethod();
        if (method == null) return false;
        Set<String> outside = new HashSet<>();
        if (method.methodBody() != null) declared(method.methodBody(), outside);
        Set<String> inside = new HashSet<>();
        List<Element> elements = elements(local, new ArrayList<>());
        elements.forEach(e -> declared(e, inside));
        boolean[] captures = {false};
        for (Element element : elements) {
            element.visit((Element e) -> {
                if (e instanceof VariableExpression ve) {
                    switch (ve.variable()) {
                        case LocalVariable lv when outside.contains(lv.simpleName())
                                                   && !inside.contains(lv.simpleName()) -> captures[0] = true;
                        case ParameterInfo pi when method.parameters().contains(pi) -> captures[0] = true;
                        case FieldReference fr when !fr.fieldInfo().isStatic() && foreign(fr.fieldInfo().owner(), local)
                                                    && thisScope(fr) -> captures[0] = true;
                        default -> {
                        }
                    }
                } else if (e instanceof MethodCall mc && !mc.methodInfo().isStatic() && mc.objectIsImplicit()
                           && foreign(mc.methodInfo().typeInfo(), local)) {
                    captures[0] = true;
                }
                return !captures[0];
            });
            if (captures[0]) return false;
        }
        return true;
    }

    private static boolean thisScope(FieldReference fr) {
        return fr.isDefaultScope() || fr.scope() instanceof VariableExpression ve && ve.variable() instanceof This;
    }

    /** An instance member of {@code owner} is not one of the local type's own, or inherited by it. */
    private static boolean foreign(TypeInfo owner, TypeInfo local) {
        return !within(owner, local) && !CSharpNames.inherits(local, owner);
    }

    /** The code of {@code type} and its nested types: bodies and initializers. */
    private static List<Element> elements(TypeInfo type, List<Element> found) {
        Stream.concat(type.constructors().stream(), type.methods().stream())
                .filter(m -> m.methodBody() != null).forEach(m -> found.add(m.methodBody()));
        for (FieldInfo f : type.fields()) {
            if (f.initializer() != null && !f.initializer().isEmpty()) found.add(f.initializer());
        }
        type.subTypes().forEach(st -> elements(st, found));
        return found;
    }

    private static void declared(Element element, Set<String> names) {
        element.visit((Element e) -> {
            if (e instanceof LocalVariableCreation lvc) {
                lvc.localVariableStream().map(LocalVariable::simpleName).forEach(names::add);
            } else if (e instanceof RecordPattern rp && rp.localVariable() != null) {
                names.add(rp.localVariable().simpleName());
            }
            return true;
        });
    }

    private static boolean within(TypeInfo t, TypeInfo owner) {
        for (TypeInfo x = t; x != null; x = CSharpNames.enclosing(x)) {
            if (x.equals(owner)) return true;
        }
        return false;
    }
}
