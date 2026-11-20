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

package io.codelaser.maddi.inspection.api.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.codelaser.maddi.cst.api.expression.ConstantExpression;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

/**
 * What another primary type's source can see of a primary type when javac attributes it: the declarations, as text
 * with names instead of identities, so that two generations of the type compare.
 * <p>
 * A re-parse rebuilds the changed types and <em>rewires</em> their dependents: the dependents' code is copied, each
 * reference re-pointed at the new object of the same name. That is right as long as javac would resolve every name in
 * the dependents as it did before, which holds for an edit of a body or a comment and fails for an edit of a
 * declaration: an overload added ({@code scale(String)} next to {@code scale(Object)}), an inherited method now
 * overridden, a supertype, a constant's value. A dependent rewired across such an edit keeps calling the method it
 * called before, and its analysis is that of code that no longer exists
 * (docs/design/incremental-and-overlay.md §5 item 3). So: equal signatures, rewire; different, re-parse the dependents.
 * <p>
 * In: the type's kind, modifiers, type parameters and supertypes; of every member that is not private, its modifiers,
 * name and types (a method's type parameters, parameters, varargs, return type and exceptions); the initializer of a
 * final field of a primitive type or String, which javac may inline in a dependent; the nested types, recursively.
 * Out: bodies, other initializers, comments, annotations (a dependent reads their effect from the analysis, not from
 * the parse), private members (no other primary type can name them), anonymous and local types. The order of the
 * members is out too.
 */
public final class DeclarationSignature {

    private DeclarationSignature() {
    }

    public static String of(TypeInfo primaryType) {
        StringBuilder sb = new StringBuilder();
        type(primaryType, sb);
        return sb.toString();
    }

    private static void type(TypeInfo type, StringBuilder sb) {
        sb.append(sorted(type.typeModifiers())).append(' ').append(type.typeNature()).append(' ')
                .append(type.fullyQualifiedName()).append(typeParameters(type.typeParameters()));
        if (type.parentClass() != null) sb.append(" extends ").append(text(type.parentClass()));
        sb.append(" implements ").append(type.interfacesImplemented().stream().map(DeclarationSignature::text)
                .collect(Collectors.joining(",")));
        if (type.isSealed()) {
            sb.append(" permits ").append(type.permittedWhenSealed().stream().map(TypeInfo::fullyQualifiedName)
                    .sorted().collect(Collectors.joining(",")));
        }
        sb.append('\n');
        TreeSet<String> members = new TreeSet<>();
        for (FieldInfo field : type.fields()) {
            if (field.access().isPrivate()) continue;
            StringBuilder f = new StringBuilder("F ").append(sorted(field.modifiers())).append(' ')
                    .append(text(field.type())).append(' ').append(field.name());
            if (field.isFinal() && inlinable(field.type()) && field.initializer() != null
                && !field.initializer().isEmpty()) {
                f.append(" = ").append(constant(field.initializer()));
            }
            members.add(f.toString());
        }
        type.constructorAndMethodStream().forEach(method -> {
            if (method.access().isPrivate() || method.isStaticInitializer() || method.isInstanceInitializer()) return;
            members.add(method(method));
        });
        members.forEach(m -> sb.append(m).append('\n'));
        List<TypeInfo> nested = new ArrayList<>(type.subTypes());
        nested.sort((a, b) -> a.fullyQualifiedName().compareTo(b.fullyQualifiedName()));
        for (TypeInfo sub : nested) {
            if (!sub.access().isPrivate()) type(sub, sb);
        }
    }

    private static String method(MethodInfo method) {
        StringBuilder sb = new StringBuilder("M ").append(sorted(method.methodModifiers())).append(' ')
                .append(typeParameters(method.typeParameters())).append(' ');
        if (!method.isConstructor()) sb.append(text(method.returnType())).append(' ');
        sb.append(method.name()).append('(');
        boolean first = true;
        for (ParameterInfo parameter : method.parameters()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(text(parameter.parameterizedType()));
            if (parameter.isVarArgs()) sb.append("...");
        }
        sb.append(") throws ").append(method.exceptionTypes().stream().map(DeclarationSignature::text).sorted()
                .collect(Collectors.joining(",")));
        return sb.toString();
    }

    /*
     An initializer's text, stable across generations: a string literal prints as its source position
     ("stringConstant@3-39:3-43"), which moves with every line added above it and says nothing of its value. So the
     positions go, and the values of the constants in the expression are listed after it.
     */
    private static String constant(Expression initializer) {
        StringBuilder values = new StringBuilder();
        initializer.visit(e -> {
            if (e instanceof ConstantExpression<?> ce) values.append('|').append(ce.constant());
            return true;
        });
        return POSITION.matcher(initializer.toString()).replaceAll("") + values;
    }

    private static final Pattern POSITION = Pattern.compile("@\\d+-\\d+:\\d+-\\d+");

    private static boolean inlinable(ParameterizedType type) {
        return type.arrays() == 0 && (type.isPrimitiveExcludingVoid()
                                      || type.typeInfo() != null && type.typeInfo().isJavaLangString());
    }

    private static String typeParameters(List<TypeParameter> typeParameters) {
        if (typeParameters.isEmpty()) return "";
        return typeParameters.stream().map(TypeParameter::toStringWithTypeBounds)
                .collect(Collectors.joining(",", "<", ">"));
    }

    private static String text(ParameterizedType type) {
        return type.detailedString();
    }

    private static String sorted(java.util.Collection<?> modifiers) {
        return modifiers.stream().map(String::valueOf).sorted().collect(Collectors.joining(" "));
    }
}
