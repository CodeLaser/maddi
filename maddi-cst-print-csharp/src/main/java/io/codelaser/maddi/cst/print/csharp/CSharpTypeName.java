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
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.output.TypeNameImpl;
import io.codelaser.maddi.cst.impl.type.WildcardEnum;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /** A mapping that is its type argument, {@code {0?}}: Optional's. */
    private static final Pattern ARGUMENT_TEMPLATE = Pattern.compile("\\{(\\d+)\\??}");

    /** The value types C# has for Java's types, the primitives and the BCL mapping's structs. */
    private static final Set<String> VALUE_TYPES = Set.of("int", "long", "short", "sbyte", "byte", "char", "bool",
            "double", "float", "decimal", "Guid", "DateOnly", "TimeOnly", "DateTime", "DateTimeOffset", "TimeSpan",
            "KeyValuePair", "ValueTuple");

    /** Collections of {@code ?}: C#'s non-generic interfaces, which all its generic collections implement. */
    private static final Map<String, String> NON_GENERIC = Map.of(
            "java.util.List", "IList",
            "java.util.Collection", "ICollection",
            "java.util.Set", "ICollection",
            "java.lang.Iterable", "IEnumerable",
            "java.util.Map", "IDictionary",
            "java.lang.Comparable", "IComparable");

    /** Collections of {@code ? extends T}: C#'s covariant interfaces of {@code T}. */
    private static final Map<String, String> COVARIANT = Map.of(
            "java.util.List", "IReadOnlyList",
            "java.util.Collection", "IReadOnlyCollection",
            "java.util.Set", "IReadOnlyCollection",
            "java.lang.Iterable", "IEnumerable");

    /** The C# type reference, no leading or trailing space. */
    public static String of(ParameterizedType pt, Qualification q) {
        return of(pt, q, false);
    }

    /**
     * The type of a declaration, with what the analysis knows: a collection parameter the method does not modify is
     * read-only ({@code IReadOnlyList<T>}), a reference type that may be null is {@code T?}.
     */
    static String declared(ParameterizedType pt, boolean readOnly, NullableState nullable,
                           Qualification q) {
        String type;
        String readOnlyName = readOnly ? CSharpReadOnly.readOnlyName(pt) : null;
        if (readOnlyName != null) {
            CSharpContext.using("System.Collections.Generic");
            type = readOnlyName + "<" + String.join(", ", pt.parameters().stream().map(a -> argument(a, q)).toList()) + ">";
        } else {
            type = of(pt, q);
        }
        // a method's own type parameter only where its method's signature allows (CSharpMethodPrinter)
        boolean reference = pt.arrays() > 0 || !pt.isPrimitiveExcludingVoid() && !type.endsWith("?") && !pt.isVoid()
                                               && !(pt.isTypeParameter() && pt.typeParameter().isMethodTypeParameter());
        return nullable == NullableState.NULLABLE && reference && valueTypeFree(pt, type)
               && !unmapped(pt) ? type + "?" : type;
    }

    /**
     * A library type without a C# mapping (UNMAPPED_JDK), or a mapping that is one: C# reads its {@code X?} as {@code Nullable<X>}, so that every
     * use of the variable is one more error, where the undeclared {@code X} alone is reported once.
     */
    private static boolean unmapped(ParameterizedType pt) {
        TypeInfo typeInfo = pt.typeInfo();
        if (pt.arrays() != 0 || typeInfo == null || MAPPED.containsKey(typeInfo.fullyQualifiedName())) return false;
        LibraryMapping.TypeMapping bcl = CSharpContext.libraries().type(typeInfo);
        if (bcl == null) return !CSharpNames.translated(typeInfo);
        // Optional<ByteBuffer> is its argument, {0?}
        Matcher argument = ARGUMENT_TEMPLATE.matcher(bcl.template());
        if (!argument.matches()) return false;
        int index = Integer.parseInt(argument.group(1));
        return index < pt.parameters().size() && unmapped(pt.parameters().get(index));
    }

    /**
     * Not a C# value type, for which {@code ?} would be {@code Nullable<T>}, another type: a simple enum (a C# enum), and those the BCL
     * mapping makes of Java's types (Guid, DateOnly, decimal, KeyValuePair for an entry) stay as they are.
     */
    private static boolean valueTypeFree(ParameterizedType pt, String type) {
        if (pt.arrays() == 0 && pt.typeInfo() != null && pt.typeInfo().typeNature().isEnum()
            && CSharpNames.simpleEnum(pt.typeInfo())) {
            return false;
        }
        int generic = type.indexOf('<');
        String raw = generic < 0 ? type : type.substring(0, generic);
        return !VALUE_TYPES.contains(raw);
    }

    /**
     * A type in a pattern, {@code x is int i}: a nullable value type is not allowed there, and a boxed Java type tested
     * with instanceof is its value type (null is not an instance of anything, in either language).
     */
    public static String pattern(ParameterizedType pt, Qualification q) {
        LibraryMapping.CodeMapping pattern = CSharpContext.libraries().pattern(pt, null);
        if (pattern != null) {
            if (pattern.namespace() != null) CSharpContext.using(pattern.namespace());
            return pattern.value();
        }
        return of(pt, q, true);
    }

    private static String of(ParameterizedType pt, Qualification q, boolean typeArgument) {
        if (pt.arrays() > 0) {
            ParameterizedType element = pt.copyWithoutArrays();
            return of(element, q, false) + "[]".repeat(pt.arrays());
        }
        if (pt.wildcard() != null) {
            if (pt.wildcard().isUnbound() || pt.typeInfo() == null && pt.typeParameter() == null) {
                CSharpContext.message(CSharpPrintCode.WILDCARD_AS_BOUND, null, "?");
                return "object";
            }
            CSharpContext.message(CSharpPrintCode.WILDCARD_AS_BOUND, null, pt.toString());
        }
        List<TypeParameter> erased = CSharpContext.erasedTypeParameters();
        if (pt.isTypeParameter()) {
            // in a method C# prints with its type parameters erased (CSharpProgram): the bound
            return erased.contains(pt.typeParameter()) ? rawArgument(pt.typeParameter(), q)
                    : CSharpNames.typeParameter(pt.typeParameter());
        }
        if (!erased.isEmpty()) pt = erasedToWildcards(pt, erased);
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
            CSharpContext.using("IComparable".equals(nonGeneric) ? "System" : "System.Collections");
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
        // Score<?> of Score<Score_ extends Score<Score_>>: no C# argument satisfies the self-bound, the wildcard interface does
        if (CSharpWildcardInterfaces.isWildcardUse(pt)) return CSharpWildcardInterfaces.wildcardUse(pt, q);
        List<String> printed = new ArrayList<>();
        if (arguments.isEmpty() && !typeInfo.typeParameters().isEmpty()) {
            CSharpContext.message(CSharpPrintCode.RAW_TYPE, null, fqn);
            // R extends Result<R>: the raw Result's argument is Result's own erasure, which is where it stops
            if (!CSharpContext.rawInProgress().add(typeInfo)) {
                typeInfo.typeParameters().forEach(tp -> printed.add("object"));
            } else {
                try {
                    typeInfo.typeParameters().forEach(tp -> printed.add(rawArgument(tp, q)));
                } finally {
                    CSharpContext.rawInProgress().remove(typeInfo);
                }
            }
        }
        for (int i = 0; i < arguments.size(); i++) {
            ParameterizedType a = arguments.get(i);
            // Key<?> where Key<T extends Attribute>: the bound, which satisfies the constraint
            boolean unbound = a.wildcard() != null && a.wildcard().isUnbound() && i < typeInfo.typeParameters().size();
            printed.add(unbound ? rawArgument(typeInfo.typeParameters().get(i), q) : of(a, q, true));
        }
        LibraryMapping.TypeMapping bcl = CSharpContext.libraries().type(typeInfo);
        if (bcl != null) return bcl(bcl, printed, arguments, q);
        String name = name(typeInfo, q);
        return printed.isEmpty() ? name : name + "<" + String.join(", ", printed) + ">";
    }

    /**
     * A JDK type in C# ({@link CSharpBcl}): its name with the type arguments, or its pattern of them, {@code {0}} as a
     * type argument and {@code {0?}} as a type ({@code Optional<Integer>} is {@code int?}).
     */
    private static String bcl(LibraryMapping.TypeMapping bcl, List<String> printed, List<ParameterizedType> arguments,
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
        LibraryMapping.TypeMapping bcl = CSharpContext.libraries().type(typeInfo);
        if (bcl != null) {
            // Func<{0}, bool> is named Func
            if (bcl.namespace() != null) CSharpContext.using(bcl.namespace());
            String template = bcl.template();
            int angle = template.indexOf('<');
            return bcl.ofArgument() ? "object" : angle < 0 ? template : template.substring(0, angle);
        }
        if (!CSharpNames.translated(typeInfo)) {
            CSharpContext.message(CSharpPrintCode.UNMAPPED_JDK, null, typeInfo.fullyQualifiedName());
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
        for (TypeInfo t = CSharpContext.currentType(); t != null; t = CstQueries.enclosing(t)) {
            for (TypeInfo c : CstQueries.classChain(t)) {
                if (!CSharpNames.translated(c)) break;
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
        for (TypeInfo t = typeInfo; t != null; t = CSharpNames.hoisted(t) ? null : CstQueries.enclosing(t)) {
            segments.addFirst(CSharpNames.type(t));
        }
        return String.join(".", segments);
    }

    /** {@code InnerScore<Score_>} with {@code Score_} erased: {@code InnerScore<?>}. */
    private static ParameterizedType erasedToWildcards(ParameterizedType pt, List<TypeParameter> erased) {
        if (pt.parameters().isEmpty()) return pt;
        boolean changed = false;
        List<ParameterizedType> arguments = new ArrayList<>();
        for (ParameterizedType a : pt.parameters()) {
            ParameterizedType e = a.isTypeParameter() && erased.contains(a.typeParameter()) && a.arrays() == 0
                    ? a.withWildcard(WildcardEnum.UNBOUND)
                    : erasedToWildcards(a, erased);
            changed |= e != a;
            arguments.add(e);
        }
        return changed ? pt.withParameters(arguments) : pt;
    }

    /** A raw type's argument for {@code tp}: its bound's erasure, which satisfies the constraint. */
    static String rawArgument(TypeParameter tp, Qualification q) {
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
            // a dictionary's key (CSharpNotNullKeys), unless a class bound says it already
            boolean notNull = CSharpContext.program().notNullKey(tp)
                              && bounds.stream().noneMatch(b -> b.typeInfo() != null && !b.isTypeParameter()
                                                                && !b.typeInfo().isInterface());
            if (bounds.isEmpty() && !notNull) continue;
            List<String> parts = new ArrayList<>();
            if (notNull) parts.add("notnull");
            bounds.forEach(b -> parts.add(of(b, q)));
            constraints.add("where " + CSharpNames.typeParameter(tp) + " : " + String.join(", ", parts));
        }
        return constraints;
    }
}
