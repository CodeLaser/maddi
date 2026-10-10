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

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.output.TypeNameRequired;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.output.TypeNameImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders a {@link ParameterizedType} as a C# type reference.
 * <ul>
 *   <li>Java's primitives are C#'s, except that Java's signed {@code byte} is {@code sbyte};</li>
 *   <li>{@code String} and {@code Object} are {@code string} and {@code object};</li>
 *   <li>a boxed type is the nullable value type, {@code Integer} is {@code int?}; as a type argument, where Java
 *   boxes only because its generics are erased, the value type itself: {@code List<Integer>} is {@code List<int>};</li>
 *   <li>arrays are written as in Java, {@code int[][]} (a jagged array);</li>
 *   <li>C# generics have no wildcards: {@code ? extends T} and {@code ? super T} are {@code T}, {@code ?} is
 *   {@code object}; and no raw types: a raw {@code List} gets {@code object} arguments.</li>
 * </ul>
 * The JDK's other types keep their Java names until the BCL mapping takes them over.
 */
public final class CSharpTypeName {

    private static final Map<String, String> PRIMITIVE = Map.of("boolean", "bool", "byte", "sbyte", "char", "char",
            "short", "short", "int", "int", "long", "long", "float", "float", "double", "double", "void", "void");

    private static final Map<String, String> BOXED = Map.of("java.lang.Boolean", "bool", "java.lang.Byte", "sbyte",
            "java.lang.Character", "char", "java.lang.Short", "short", "java.lang.Integer", "int",
            "java.lang.Long", "long", "java.lang.Float", "float", "java.lang.Double", "double");

    private static final Map<String, String> MAPPED = Map.of("java.lang.String", "string",
            "java.lang.Object", "object", "java.lang.Void", "object");

    private CSharpTypeName() {
    }

    /** The C# type reference, no leading or trailing space. */
    public static String of(ParameterizedType pt, Qualification q) {
        return of(pt, q, false);
    }

    /**
     * A type in a pattern, {@code x is int i}: a nullable value type is not allowed there, and a boxed Java type tested
     * with instanceof is its value type (null is not an instance of anything, in either language).
     */
    public static String pattern(ParameterizedType pt, Qualification q) {
        return of(pt, q, true);
    }

    private static String of(ParameterizedType pt, Qualification q, boolean typeArgument) {
        if (pt.arrays() > 0) {
            ParameterizedType element = pt.copyWithoutArrays();
            return of(element, q, false) + "[]".repeat(pt.arrays());
        }
        if (pt.wildcard() != null) {
            if (pt.wildcard().isUnbound() || pt.typeInfo() == null && pt.typeParameter() == null) {
                CSharpContext.message(CSharpPrintMessage.Code.WILDCARD_AS_BOUND, null, "?");
                return "object";
            }
            CSharpContext.message(CSharpPrintMessage.Code.WILDCARD_AS_BOUND, null, pt.toString());
        }
        if (pt.isTypeParameter()) return CSharpNames.name(pt.typeParameter().simpleName());
        TypeInfo typeInfo = pt.typeInfo();
        if (typeInfo == null) return "object";
        String fqn = typeInfo.fullyQualifiedName();
        String primitive = PRIMITIVE.get(fqn);
        if (primitive != null) return primitive;
        String boxed = BOXED.get(fqn);
        if (boxed != null) return typeArgument ? boxed : boxed + "?";
        String mapped = MAPPED.get(fqn);
        if (mapped != null) return mapped;

        String name = name(typeInfo, q);
        List<ParameterizedType> arguments = pt.parameters();
        if (arguments.isEmpty() && !typeInfo.typeParameters().isEmpty()) {
            CSharpContext.message(CSharpPrintMessage.Code.RAW_TYPE, null, fqn);
            return name + typeInfo.typeParameters().stream().map(tp -> rawArgument(tp, q))
                    .collect(Collectors.joining(", ", "<", ">"));
        }
        if (arguments.isEmpty()) return name;
        List<String> printed = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            ParameterizedType a = arguments.get(i);
            // Key<?> where Key<T extends Attribute>: the bound, which satisfies the constraint
            boolean unbound = a.wildcard() != null && a.wildcard().isUnbound() && i < typeInfo.typeParameters().size();
            printed.add(unbound ? rawArgument(typeInfo.typeParameters().get(i), q) : of(a, q, true));
        }
        return name + "<" + String.join(", ", printed) + ">";
    }

    /**
     * A type's name without type arguments, qualified as far as the Java printer would qualify it: {@code Inner},
     * {@code Outer.Inner}, or with the namespace. Each type segment is named by {@link CSharpNames#type}.
     */
    public static String name(TypeInfo typeInfo, Qualification q) {
        String mapped = MAPPED.get(typeInfo.fullyQualifiedName());
        if (mapped != null) return mapped;
        if (q == null) return CSharpNames.type(typeInfo);
        TypeNameRequired required = q.qualifierRequired(typeInfo);
        if (required == TypeNameImpl.Required.SIMPLE) {
            CSharpContext.referenced(typeInfo);
            return CSharpNames.type(typeInfo);
        }
        String fromPrimary = fromPrimaryType(typeInfo);
        if (required == TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE
            || required == TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE_FOLLOW_EXISTING) {
            return fromPrimary;
        }
        String namespace = CSharpNames.namespace(typeInfo);
        return namespace.isEmpty() ? fromPrimary : namespace + "." + fromPrimary;
    }

    /** {@code Outer.Inner}, each segment by its C# name. */
    static String fromPrimaryType(TypeInfo typeInfo) {
        List<String> segments = new ArrayList<>();
        for (TypeInfo t = typeInfo; t != null; ) {
            segments.addFirst(CSharpNames.type(t));
            var cuOrEnclosing = t.compilationUnitOrEnclosingType();
            t = cuOrEnclosing.isRight() ? cuOrEnclosing.getRight() : null;
        }
        return String.join(".", segments);
    }

    /** A raw type's argument: the erasure of the type parameter's bound, which satisfies its constraint. */
    private static String rawArgument(TypeParameter tp, Qualification q) {
        ParameterizedType bound = tp.typeBounds().stream().filter(b -> !b.isJavaLangObject()).findFirst().orElse(null);
        if (bound == null || bound.typeInfo() == null || bound.isTypeParameter()) return "object";
        return of(bound.erased(), q, true);
    }

    /** A type argument: a boxed type is its value type, {@code List<int>}. */
    public static String argument(ParameterizedType pt, Qualification q) {
        return of(pt, q, true);
    }

    /** The constraints of type parameters: {@code where T : IComparable<T>}; empty when they have no bounds. */
    static List<String> constraints(List<TypeParameter> typeParameters, Qualification q) {
        List<String> constraints = new ArrayList<>();
        for (TypeParameter tp : typeParameters) {
            List<ParameterizedType> bounds = tp.typeBounds().stream().filter(b -> !b.isJavaLangObject()).toList();
            if (bounds.isEmpty()) continue;
            constraints.add("where " + CSharpNames.name(tp.simpleName()) + " : "
                            + bounds.stream().map(b -> of(b, q)).collect(Collectors.joining(", ")));
        }
        return constraints;
    }

    /** A primitive, or a boxed primitive's value type: what a {@code default(T)} is not null for. */
    static boolean isValueType(ParameterizedType pt) {
        return pt.arrays() == 0 && pt.typeInfo() != null && PRIMITIVE.containsKey(pt.typeInfo().fullyQualifiedName());
    }
}
