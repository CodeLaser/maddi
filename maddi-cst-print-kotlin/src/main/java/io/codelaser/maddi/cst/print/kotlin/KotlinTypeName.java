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

package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.impl.output.TypeNameImpl;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders a {@link ParameterizedType} as a Kotlin type reference: JVM primitives and common JDK types are
 * mapped to their Kotlin names (`int`→`Int`, `java.lang.String`→`String`, `java.lang.Object`→`Any`, …), arrays
 * become `Array<…>` (best-effort; a dedicated `IntArray` etc. is a refinement), and generics recurse. Nullability
 * is not tracked in the CST, so no `?` is emitted.
 */
public class KotlinTypeName {

    private static final Map<String, String> KOTLIN = Map.ofEntries(
            Map.entry("int", "Int"), Map.entry("long", "Long"), Map.entry("short", "Short"),
            Map.entry("byte", "Byte"), Map.entry("char", "Char"), Map.entry("boolean", "Boolean"),
            Map.entry("float", "Float"), Map.entry("double", "Double"), Map.entry("void", "Unit"),
            Map.entry("java.lang.Integer", "Int"), Map.entry("java.lang.Long", "Long"),
            Map.entry("java.lang.Short", "Short"), Map.entry("java.lang.Byte", "Byte"),
            Map.entry("java.lang.Character", "Char"), Map.entry("java.lang.Boolean", "Boolean"),
            Map.entry("java.lang.Float", "Float"), Map.entry("java.lang.Double", "Double"),
            Map.entry("java.lang.Void", "Unit"), Map.entry("java.lang.String", "String"),
            Map.entry("java.lang.Object", "Any"), Map.entry("java.lang.CharSequence", "CharSequence"),
            Map.entry("java.util.List", "List"), Map.entry("java.util.Map", "Map"),
            Map.entry("java.util.Set", "Set"), Map.entry("java.util.Collection", "Collection"));

    /**
     * Java's collection interfaces are mutable; Kotlin's {@code List} is read-only, and {@code ArrayList} is not one.
     * Only for a translation from Java: a {@code List} parsed from Kotlin source is printed back as {@code List}.
     */
    private static final Map<String, String> MUTABLE = Map.ofEntries(
            Map.entry("java.util.List", "MutableList"), Map.entry("java.util.Map", "MutableMap"),
            Map.entry("java.util.Set", "MutableSet"), Map.entry("java.util.Collection", "MutableCollection"),
            Map.entry("java.util.Map.Entry", "MutableMap.MutableEntry"),
            Map.entry("java.util.Iterator", "MutableIterator"), Map.entry("java.util.ListIterator", "MutableListIterator"),
            Map.entry("java.lang.Iterable", "MutableIterable"));

    /** A JDK type Kotlin replaces by its own: never imported, and its static members are reached by its Java name. */
    public static boolean isMapped(String fullyQualifiedName) {
        return KOTLIN.containsKey(fullyQualifiedName) || MUTABLE.containsKey(fullyQualifiedName);
    }

    private static String mapped(String fullyQualifiedName) {
        if (KotlinContext.translatingJava()) {
            String mutable = MUTABLE.get(fullyQualifiedName);
            if (mutable != null) return mutable;
        }
        return KOTLIN.get(fullyQualifiedName);
    }

    /**
     * The qualifier of a static member: {@code java.lang.Integer.parseInt}, not {@code Int.parseInt}, because a
     * Kotlin type has none of its Java counterpart's statics.
     */
    public static String staticOwner(TypeInfo typeInfo, Qualification q) {
        return isMapped(typeInfo.fullyQualifiedName()) ? typeInfo.fullyQualifiedName() : name(typeInfo, q);
    }

    private static final Map<String, String> PRIMITIVE_ARRAY = Map.of("int", "IntArray", "long", "LongArray",
            "short", "ShortArray", "byte", "ByteArray", "char", "CharArray", "boolean", "BooleanArray",
            "float", "FloatArray", "double", "DoubleArray");

    /** The Kotlin type reference as a string (no leading/trailing space); a nullable type gets a trailing `?`. */
    public static String of(ParameterizedType pt) {
        return of(pt, null);
    }

    /**
     * As {@link #of(ParameterizedType)}, with the type's name written the way the qualification says the Java
     * printer would (`Outer.Inner` where `Inner` alone does not resolve). Without a qualification: the simple name.
     */
    public static String of(ParameterizedType pt, Qualification q) {
        return nullable(pt, base(pt, q));
    }

    /** {@code IntArray} for {@code int[]}, {@code Array<String>} for {@code String[]}, null when not an array. */
    public static String primitiveArray(ParameterizedType elementType) {
        if (elementType.arrays() > 0 || elementType.typeInfo() == null) return null;
        return PRIMITIVE_ARRAY.get(elementType.typeInfo().fullyQualifiedName());
    }

    private static String nullable(ParameterizedType pt, String s) {
        // the Kotlin front-end records NULLABLE on the parameterized type; the (Java-oriented) default is UNSPECIFIED
        return pt.nullable() == NullableState.NULLABLE ? s + "?" : s;
    }

    private static String base(ParameterizedType pt, Qualification q) {
        if (pt.arrays() > 0) {
            // the state on an array type is the array's; its elements' is on its component type
            ParameterizedType element = pt.componentType();
            String primitive = primitiveArray(element);
            return primitive != null ? primitive : "Array<" + of(element, q) + ">";
        }
        if (pt.wildcard() != null) {
            if (pt.wildcard().isUnbound()) return "*";
            String bound = pt.typeInfo() == null && !pt.isTypeParameter() ? "Any" : withoutWildcard(pt, q);
            return (pt.wildcard().isSuper() ? "in " : "out ") + bound;
        }
        if (pt.isTypeParameter()) {
            return pt.typeParameter().simpleName();
        }
        if (pt.typeInfo() == null) {
            return "Any"; // no type
        }
        String fqn = pt.typeInfo().fullyQualifiedName();
        String base = mapped(fqn);
        if (base == null) base = name(pt.typeInfo(), q);
        if (pt.parameters().isEmpty()) return base + starProjections(pt.typeInfo());
        StringBuilder sb = new StringBuilder(base).append('<');
        for (int i = 0; i < pt.parameters().size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(of(pt.parameters().get(i), q));
        }
        return sb.append('>').toString();
    }

    private static String withoutWildcard(ParameterizedType pt, Qualification q) {
        if (pt.isTypeParameter()) return pt.typeParameter().simpleName();
        String base = mapped(pt.typeInfo().fullyQualifiedName());
        if (base == null) base = name(pt.typeInfo(), q);
        if (pt.parameters().isEmpty()) return base + starProjections(pt.typeInfo());
        return base + pt.parameters().stream().map(p -> of(p, q)).collect(Collectors.joining(", ", "<", ">"));
    }

    /**
     * {@code <*>} for a raw use of a generic Java type: Kotlin has no raw types, and a star projection is what accepts
     * every instantiation. Not for a constructor call, which leaves its type arguments to inference instead.
     */
    static String starProjections(TypeInfo typeInfo) {
        if (!KotlinContext.translatingJava() || typeInfo.typeParameters().isEmpty()) return "";
        return typeInfo.typeParameters().stream().map(tp -> "*").collect(Collectors.joining(", ", "<", ">"));
    }

    /**
     * The type of a constructor call, {@code ArrayList<String>}; just {@code ArrayList} where Kotlin must infer the
     * arguments: a raw type, a projection (which a call cannot take), or an unresolved diamond that still carries
     * a type parameter that is not in scope (the class's own, or a called method's).
     */
    static String constructed(ParameterizedType pt, Qualification q) {
        if (pt.typeInfo() == null || pt.arrays() > 0 || pt.isTypeParameter()) return withoutNullable(of(pt, q));
        boolean infer = pt.parameters().isEmpty() || pt.parameters().stream()
                .anyMatch(p -> p.wildcard() != null || p.isTypeParameter() && !inScope(p.typeParameter()));
        return infer ? name(pt.typeInfo(), q) : withoutNullable(of(pt, q));
    }

    private static boolean inScope(io.codelaser.maddi.cst.api.info.TypeParameter tp) {
        io.codelaser.maddi.cst.api.info.MethodInfo method = KotlinContext.currentMethod();
        return tp.isMethodTypeParameter() ? method != null && method.typeParameters().contains(tp)
                : KotlinContext.typeParameterInScope(tp);
    }

    private static String withoutNullable(String s) {
        return s.endsWith("?") ? s.substring(0, s.length() - 1) : s;
    }

    /**
     * A type parameter's declaration: {@code T : MutableCollection<E>} for {@code <T extends Collection<E>>}, or the
     * members of its bound do not resolve on a T. Only a single bound: more than one needs a {@code where} clause.
     */
    static String typeParameter(io.codelaser.maddi.cst.api.info.TypeParameter tp, Qualification q) {
        java.util.List<ParameterizedType> bounds = tp.typeBounds().stream().filter(b -> !b.isJavaLangObject()).toList();
        String name = KotlinNames.name(tp.simpleName());
        return bounds.size() == 1 ? name + " : " + of(bounds.getFirst(), q) : name;
    }

    /** A type's name, without type arguments: as the Java printer would qualify it, segments escaped. */
    public static String name(TypeInfo typeInfo, Qualification q) {
        String mapped = mapped(typeInfo.fullyQualifiedName());
        if (mapped != null) return mapped;
        if (q == null) return KotlinNames.name(typeInfo.simpleName());
        String minimal = TypeNameImpl.typeName(typeInfo, q.qualifierRequired(typeInfo), false).minimal();
        TypeInfo primary = typeInfo.primaryType();
        if (primary != null && (minimal.equals(primary.simpleName()) || minimal.startsWith(primary.simpleName() + "."))) {
            KotlinContext.referencedType(primary);
        }
        return KotlinNames.dotted(minimal);
    }
}
