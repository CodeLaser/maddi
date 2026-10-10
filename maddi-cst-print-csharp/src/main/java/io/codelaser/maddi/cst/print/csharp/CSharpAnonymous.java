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
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.This;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * C# has no anonymous classes. One that does not become a lambda (see {@link CSharpProgram#lambdaLike}) is hoisted:
 * printed as a private sealed nested class of the type whose code creates it, named after what it extends
 * ({@code GraphImpl}). What it captures becomes its constructor's arguments and its fields: the local variables and
 * parameters it uses, and the enclosing instance, {@code outer}, when it uses an instance member of it. Its
 * superclass's constructor arguments come first. A generic method's type parameters become the class's.
 */
final class CSharpAnonymous {

    private CSharpAnonymous() {
    }

    /** A captured local variable or parameter: its Java name and type. */
    record Capture(String name, ParameterizedType type) {
    }

    /**
     * @param name           the C# name
     * @param superArguments the types of the superclass constructor's arguments
     * @param captures       the locals and parameters, in the order of their first use
     * @param outer          the enclosing instance is used
     * @param typeParameters the enclosing generic method's
     */
    record Hoisted(TypeInfo type, String name, List<ParameterizedType> superArguments, List<Capture> captures,
                   boolean outer, List<TypeParameter> typeParameters) {
    }

    /** The anonymous classes created by the code of {@code owner} (not of its nested types) that are hoisted into it. */
    static List<Hoisted> in(TypeInfo owner) {
        List<ConstructorCall> calls = new ArrayList<>();
        List<Element> code = new ArrayList<>();
        Stream.concat(owner.constructors().stream(), owner.methods().stream())
                .filter(m -> m.methodBody() != null).forEach(m -> code.add(m.methodBody()));
        for (FieldInfo f : owner.fields()) {
            if (f.initializer() != null && !f.initializer().isEmpty()) code.add(f.initializer());
        }
        for (Element element : code) {
            element.visit((Element e) -> {
                if (e instanceof ConstructorCall cc && cc.anonymousClass() != null && !CSharpExpressionPrinter.asLambda(cc)) {
                    calls.add(cc);
                }
                return true;
            });
        }
        Set<String> taken = new HashSet<>();
        owner.subTypes().forEach(st -> taken.add(CSharpNames.type(st)));
        List<Hoisted> hoisted = new ArrayList<>();
        for (ConstructorCall cc : calls) {
            TypeInfo anonymous = cc.anonymousClass();
            if (hoisted.stream().anyMatch(h -> h.type().equals(anonymous))) continue;
            // an enum constant's body: see CSharpTypePrinter's enum classes
            if (anonymous.parentClass() != null && anonymous.parentClass().typeInfo() != null
                && anonymous.parentClass().typeInfo().typeNature().isEnum()) {
                continue;
            }
            ParameterizedType base = anonymous.interfacesImplemented().isEmpty() ? anonymous.parentClass()
                    : anonymous.interfacesImplemented().getFirst();
            String baseName = base == null || base.typeInfo() == null ? "Anonymous" : base.typeInfo().simpleName();
            String name = baseName + "Impl";
            for (int i = 2; !taken.add(name); i++) name = baseName + "Impl" + i;
            List<ParameterizedType> superArguments = anonymous.interfacesImplemented().isEmpty()
                    ? cc.parameterExpressions().stream().map(x -> x.parameterizedType()).toList() : List.of();
            Map<String, Capture> captures = new LinkedHashMap<>();
            boolean outer = captures(anonymous, captures);
            MethodInfo enclosingMethod = anonymous.enclosingMethod();
            List<TypeParameter> typeParameters = enclosingMethod == null ? List.of() : enclosingMethod.typeParameters();
            hoisted.add(new Hoisted(anonymous, name, superArguments, List.copyOf(captures.values()),
                    outer && !(enclosingMethod != null && enclosingMethod.isStatic()), typeParameters));
        }
        return hoisted;
    }

    /** Collects the captured locals and parameters; true when the enclosing instance is used. */
    private static boolean captures(TypeInfo anonymous, Map<String, Capture> captures) {
        List<Element> code = new ArrayList<>();
        collectCode(anonymous, code);
        Set<String> inside = new HashSet<>();
        Set<String> insideParameters = new HashSet<>();
        for (Element element : code) {
            element.visit((Element e) -> {
                if (e instanceof LocalVariableCreation lvc) {
                    lvc.localVariableStream().map(LocalVariable::simpleName).forEach(inside::add);
                } else if (e instanceof Lambda lambda) {
                    lambda.parameters().forEach(p -> insideParameters.add(p.name()));
                } else if (e instanceof RecordPattern rp && rp.localVariable() != null) {
                    inside.add(rp.localVariable().simpleName());
                }
                return true;
            });
        }
        boolean[] outer = {false};
        for (Element element : code) {
            element.visit((Element e) -> {
                if (e instanceof VariableExpression ve) {
                    switch (ve.variable()) {
                        case LocalVariable lv when !inside.contains(lv.simpleName()) ->
                                captures.putIfAbsent(lv.simpleName(), new Capture(lv.simpleName(), lv.parameterizedType()));
                        case ParameterInfo pi when pi.methodInfo() != null && !within(pi.methodInfo().typeInfo(), anonymous)
                                                   && !insideParameters.contains(pi.name()) ->
                                captures.putIfAbsent(pi.name(), new Capture(pi.name(), pi.parameterizedType()));
                        case FieldReference fr when !fr.fieldInfo().isStatic() && foreign(fr.fieldInfo().owner(), anonymous)
                                                    && thisScope(fr) -> outer[0] = true;
                        case This t when t.explicitlyWriteType() != null && !within(t.explicitlyWriteType(), anonymous) ->
                                outer[0] = true;
                        default -> {
                        }
                    }
                } else if (e instanceof MethodCall mc && !mc.methodInfo().isStatic() && mc.objectIsImplicit()
                           && foreign(mc.methodInfo().typeInfo(), anonymous)) {
                    outer[0] = true;
                }
                return true;
            });
        }
        return outer[0];
    }

    /** An instance member of the enclosing object, used from {@code hoisted}'s code: through its {@code outer}. */
    static boolean viaOuter(TypeInfo owner, TypeInfo hoisted) {
        return foreign(owner, hoisted);
    }

    static boolean thisScope(FieldReference fr) {
        return fr.isDefaultScope() || fr.scope() instanceof VariableExpression ve && ve.variable() instanceof This;
    }

    private static boolean foreign(TypeInfo owner, TypeInfo anonymous) {
        return !within(owner, anonymous) && !CSharpNames.inherits(anonymous, owner);
    }

    private static void collectCode(TypeInfo type, List<Element> code) {
        Stream.concat(type.constructors().stream(), type.methods().stream())
                .filter(m -> m.methodBody() != null).forEach(m -> code.add(m.methodBody()));
        for (FieldInfo f : type.fields()) {
            if (f.initializer() != null && !f.initializer().isEmpty()) code.add(f.initializer());
        }
        type.subTypes().forEach(st -> collectCode(st, code));
    }

    private static boolean within(TypeInfo t, TypeInfo owner) {
        for (TypeInfo x = t; x != null; x = CSharpNames.enclosing(x)) {
            if (x.equals(owner)) return true;
        }
        return false;
    }
}
