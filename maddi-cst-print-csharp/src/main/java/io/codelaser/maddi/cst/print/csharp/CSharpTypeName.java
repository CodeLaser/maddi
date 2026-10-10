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

    /** The raw types whose arguments are being printed: an F-bounded type parameter refers back to its type. */
    private static final ThreadLocal<java.util.Set<TypeInfo>> RAW_IN_PROGRESS =
            ThreadLocal.withInitial(java.util.HashSet::new);

    /** Collections of {@code ?}: C#'s non-generic interfaces, which all its generic collections implement. */
    private static final java.util.Map<String, String> NON_GENERIC = java.util.Map.of(
            "java.util.List", "IList",
            "java.util.Collection", "ICollection",
            "java.util.Set", "ICollection",
            "java.lang.Iterable", "IEnumerable",
            "java.util.Map", "IDictionary");

    /** Collections of {@code ? extends T}: C#'s covariant interfaces of {@code T}. */
    private static final java.util.Map<String, String> COVARIANT = java.util.Map.of(
            "java.util.List", "IReadOnlyList",
            "java.util.Collection", "IReadOnlyCollection",
            "java.util.Set", "IReadOnlyCollection",
            "java.lang.Iterable", "IEnumerable");

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
        if (pt.isTypeParameter()) return CSharpNames.typeParameter(pt.typeParameter());
        TypeInfo typeInfo = pt.typeInfo();
        if (typeInfo == null) return "object";
        String fqn = typeInfo.fullyQualifiedName();
        String primitive = PRIMITIVE.get(fqn);
        if (primitive != null) return primitive;
        String boxed = BOXED.get(fqn);
        if (boxed != null) return typeArgument ? boxed : boxed + "?";
        String mapped = MAPPED.get(fqn);
        if (mapped != null) return mapped;

        List<ParameterizedType> arguments = pt.parameters();
        String nonGeneric = NON_GENERIC.get(fqn);
        if (nonGeneric != null && !arguments.isEmpty()
            && arguments.stream().allMatch(a -> a.wildcard() != null && a.wildcard().isUnbound())) {
            // List<?> is any list: C#'s non-generic IList, which every List<T> is
            CSharpContext.using("System.Collections");
            return nonGeneric;
        }
        String covariant = COVARIANT.get(fqn);
        if (covariant != null && arguments.size() == 1 && arguments.getFirst().wildcard() != null
            && arguments.getFirst().wildcard().isExtends() && arguments.getFirst().typeInfo() != null) {
            // Java cannot add to a List<? extends Node>: C#'s covariant read-only interface, IReadOnlyList<Node>,
            // which a List<Block> is
            CSharpContext.using(CSharpBcl.GENERIC);
            CSharpContext.using("System.Linq"); // Contains, on any IEnumerable
            ParameterizedType bound = arguments.getFirst().withWildcard(null);
            return covariant + "<" + of(bound, q, true) + ">";
        }
        List<String> printed = new ArrayList<>();
        if (arguments.isEmpty() && !typeInfo.typeParameters().isEmpty()) {
            CSharpContext.message(CSharpPrintMessage.Code.RAW_TYPE, null, fqn);
            // R extends Result<R>: the raw Result's argument is Result's own erasure, which is where it stops
            if (!RAW_IN_PROGRESS.get().add(typeInfo)) {
                typeInfo.typeParameters().forEach(tp -> printed.add("object"));
            } else {
                try {
                    typeInfo.typeParameters().forEach(tp -> printed.add(rawArgument(tp, q)));
                } finally {
                    RAW_IN_PROGRESS.get().remove(typeInfo);
                }
            }
        }
        for (int i = 0; i < arguments.size(); i++) {
            ParameterizedType a = arguments.get(i);
            // Key<?> where Key<T extends Attribute>: the bound, which satisfies the constraint
            boolean unbound = a.wildcard() != null && a.wildcard().isUnbound() && i < typeInfo.typeParameters().size();
            printed.add(unbound ? rawArgument(typeInfo.typeParameters().get(i), q) : of(a, q, true));
        }
        CSharpBcl.TypeMapping bcl = CSharpBcl.type(typeInfo);
        if (bcl != null) return bcl(bcl, printed, arguments, q);
        String name = name(typeInfo, q);
        return printed.isEmpty() ? name : name + "<" + String.join(", ", printed) + ">";
    }

    /**
     * A JDK type in C# ({@link CSharpBcl}): its name with the type arguments, or its pattern of them, {@code {0}} as a
     * type argument and {@code {0?}} as a type ({@code Optional<Integer>} is {@code int?}).
     */
    private static String bcl(CSharpBcl.TypeMapping bcl, List<String> printed, List<ParameterizedType> arguments,
                              Qualification q) {
        if (bcl.namespace() != null) CSharpContext.using(bcl.namespace());
        String template = bcl.template();
        if (!template.contains("{")) {
            return printed.isEmpty() || template.contains("<") || bcl.dropArguments() ? template
                    : template + "<" + String.join(", ", printed) + ">";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '{') {
                int close = template.indexOf('}', i);
                String token = template.substring(i + 1, close);
                int index = Integer.parseInt(token.replace("?", ""));
                if (token.endsWith("?")) {
                    sb.append(index < arguments.size() ? of(arguments.get(index), q, false) : "object");
                } else {
                    sb.append(index < printed.size() ? printed.get(index) : "object");
                }
                i = close;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * A type's name without type arguments, qualified as far as the Java printer would qualify it: {@code Inner},
     * {@code Outer.Inner}, or with the namespace. Each type segment is named by {@link CSharpNames#type}.
     */
    public static String name(TypeInfo typeInfo, Qualification q) {
        String mapped = MAPPED.get(typeInfo.fullyQualifiedName());
        if (mapped != null) return mapped;
        CSharpBcl.TypeMapping bcl = CSharpBcl.type(typeInfo);
        if (bcl != null) {
            // Func<{0}, bool> is named Func
            if (bcl.namespace() != null) CSharpContext.using(bcl.namespace());
            String template = bcl.template();
            int angle = template.indexOf('<');
            return template.startsWith("{") ? "object" : angle < 0 ? template : template.substring(0, angle);
        }
        if (!CSharpNames.translated(typeInfo)) {
            CSharpContext.message(CSharpPrintMessage.Code.UNMAPPED_JDK, null, typeInfo.fullyQualifiedName());
        }
        if (shadowedByMember(typeInfo) || ambiguousHere(typeInfo)) {
            // VideoContent.Video() hides the type Video in VideoContent's code
            String namespace = CSharpNames.namespace(typeInfo);
            String fromPrimary = fromPrimaryType(typeInfo);
            return namespace.isEmpty() ? fromPrimary : namespace + "." + fromPrimary;
        }
        if (q == null) {
            CSharpContext.referenced(typeInfo);
            return CSharpNames.type(typeInfo);
        }
        TypeNameRequired required = q.qualifierRequired(typeInfo);
        if (required == TypeNameImpl.Required.SIMPLE) {
            CSharpContext.referenced(typeInfo);
            return CSharpNames.type(typeInfo);
        }
        String fromPrimary = fromPrimaryType(typeInfo);
        if (required == TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE
            || required == TypeNameImpl.Required.QUALIFIED_FROM_PRIMARY_TYPE_FOLLOW_EXISTING) {
            // Outer.Inner: Outer must be in scope
            if (typeInfo.primaryType() != null) CSharpContext.referenced(CSharpNames.topLevel(typeInfo));
            return fromPrimary;
        }
        String namespace = CSharpNames.namespace(typeInfo);
        return namespace.isEmpty() ? fromPrimary : namespace + "." + fromPrimary;
    }

    /** The type's top-level name is ambiguous (see {@link CSharpProgram}) and it is not of the printed namespace. */
    private static boolean ambiguousHere(TypeInfo typeInfo) {
        if (!CSharpNames.translated(typeInfo) || typeInfo.isAnonymous()) return false;
        TypeInfo top = CSharpNames.topLevel(typeInfo);
        if (!CSharpContext.program().ambiguous(CSharpNames.type(top))) return false;
        TypeInfo current = CSharpContext.currentType();
        return current == null || !CSharpNames.namespace(current).equals(CSharpNames.namespace(typeInfo));
    }

    /**
     * A member of the type being printed, of a type it is nested in, or of their superclasses, has the type's simple
     * C# name: in C#, the member hides the type.
     */
    private static boolean shadowedByMember(TypeInfo typeInfo) {
        if (!CSharpNames.translated(typeInfo) || typeInfo.isAnonymous()) return false;
        String name = CSharpNames.type(CSharpNames.topLevel(typeInfo));
        for (TypeInfo t = CSharpContext.currentType(); t != null; t = CSharpNames.enclosing(t)) {
            for (TypeInfo c = t; c != null && CSharpNames.translated(c);
                 c = c.parentClass() == null ? null : c.parentClass().typeInfo()) {
                if (c.methods().stream().anyMatch(m -> !m.isSynthetic() && name.equals(CSharpNames.method(m)))
                    || c.typeNature().isRecord() && c.fields().stream().anyMatch(f -> !f.isStatic() && name.equals(CSharpNames.field(f)))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@code Outer.Inner}, each segment by its C# name. */
    static String fromPrimaryType(TypeInfo typeInfo) {
        List<String> segments = new ArrayList<>();
        for (TypeInfo t = typeInfo; t != null; t = CSharpNames.hoisted(t) ? null : CSharpNames.enclosing(t)) {
            segments.addFirst(CSharpNames.type(t));
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
            constraints.add("where " + CSharpNames.typeParameter(tp) + " : "
                            + bounds.stream().map(b -> of(b, q)).collect(Collectors.joining(", ")));
        }
        return constraints;
    }

    /** A primitive, or a boxed primitive's value type: what a {@code default(T)} is not null for. */
    static boolean isValueType(ParameterizedType pt) {
        return pt.arrays() == 0 && pt.typeInfo() != null && PRIMITIVE.containsKey(pt.typeInfo().fullyQualifiedName());
    }
}
