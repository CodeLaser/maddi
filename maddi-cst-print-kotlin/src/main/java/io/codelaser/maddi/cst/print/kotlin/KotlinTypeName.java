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
            // Kotlin reads Java's Annotation as its own: imported, Java's would be another type
            Map.entry("java.lang.annotation.Annotation", "Annotation"),
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

    /**
     * {@code pt} with Kotlin's read-only collection interface on the outside, {@code List<Statement?>} for
     * {@code MutableList<Statement?>}: covariant, so it takes a {@code MutableList<Statement>} too. Only the outer type:
     * what is known unmodified is the collection itself, not the collections inside it. As {@link #of} otherwise.
     */
    static String readOnly(ParameterizedType pt, Qualification q) {
        String printed = of(pt, q);
        if (pt == null || pt.arrays() > 0 || pt.typeInfo() == null) return printed;
        String fqn = pt.typeInfo().fullyQualifiedName();
        String mutable = MUTABLE.get(fqn);
        String readOnly = KOTLIN.get(fqn);
        if (mutable == null || readOnly == null || !printed.startsWith(mutable)) return printed;
        return readOnly + printed.substring(mutable.length());
    }

    /** A JDK type the printer always writes mutable: MutableIterable, MutableIterator, MutableMap.MutableEntry. */
    static boolean mutableOnly(String fullyQualifiedName) {
        return MUTABLE.containsKey(fullyQualifiedName) && !KOTLIN.containsKey(fullyQualifiedName);
    }

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
        // Enum.valueOf(enumClass, value) (guava's Enums): Kotlin's Enum is kotlin.Enum, which has no valueOf
        return isMapped(typeInfo.fullyQualifiedName()) || STATICS_SHADOWED.contains(typeInfo.fullyQualifiedName())
                ? typeInfo.fullyQualifiedName() : name(typeInfo, q);
    }

    /** java.lang types Kotlin's default imports shadow by a type of the same simple name, whose statics it lacks. */
    private static final java.util.Set<String> STATICS_SHADOWED = java.util.Set.of("java.lang.Enum");

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

    /**
     * {@code @NonNull E} for a type variable Kotlin bounds nullable: {@code E & Any}. guava's NullsFirstOrdering,
     * {@code <S extends @Nullable T> … ordering.<T>reverse().<@NonNull S>nullsLast()}, where nullsLast's S is a T;
     * FluentIterable's {@code Optional<@NonNull E> firstMatch(…)}.
     */
    // printing a parameter's type or a type parameter's bound: an override must repeat those exactly, and guava's
    // implementations of ClassToInstanceMap's <T extends @NonNull B> getInstance(Class<T>) do not say @NonNull
    private static final ThreadLocal<Boolean> IN_SIGNATURE = ThreadLocal.withInitial(() -> false);

    /** {@code print}, without {@code E & Any}: a parameter's type, a type parameter's bound. */
    static String inSignature(java.util.function.Supplier<String> print) {
        return inSignature(false, print);
    }

    /** As {@link #inSignature(java.util.function.Supplier)}; {@code ownSignature}: one no override repeats. */
    static String inSignature(boolean ownSignature, java.util.function.Supplier<String> print) {
        boolean previous = IN_SIGNATURE.get();
        boolean previousOwn = IN_OWN_SIGNATURE.get();
        IN_SIGNATURE.set(true);
        IN_OWN_SIGNATURE.set(ownSignature);
        try {
            return print.get();
        } finally {
            IN_SIGNATURE.set(previous);
            IN_OWN_SIGNATURE.set(previousOwn);
        }
    }

    /** In a signature no override repeats: a {@code ? super @NonNull T} keeps its {@code T & Any}. */
    private static final ThreadLocal<Boolean> IN_OWN_SIGNATURE = ThreadLocal.withInitial(() -> false);

    private static boolean superBoundNonNull(ParameterizedType t) {
        if (!IN_OWN_SIGNATURE.get()) return definitelyNonNull(t);
        IN_SIGNATURE.set(false);
        try {
            return definitelyNonNull(t);
        } finally {
            IN_SIGNATURE.set(true);
        }
    }

    static boolean definitelyNonNull(ParameterizedType t) {
        return KotlinContext.translatingJava() && !IN_SIGNATURE.get() && t.isTypeParameter() && t.arrays() == 0 && t.wildcard() == null
               && t.nullable() != NullableState.NULLABLE && t.annotations().stream()
                       .anyMatch(ae -> io.codelaser.maddi.cst.impl.analysis.NullAnnotations.NON_NULL
                               .contains(ae.typeInfo().simpleName()))
               && nullableBound(t.typeParameter(), 0);
    }

    /** A type variable Kotlin bounds nullable: its values may be null, whatever their own state says. */
    static boolean nullableTypeVariable(ParameterizedType t) {
        return t != null && t.isTypeParameter() && t.arrays() == 0 && t.wildcard() == null
               && nullableBound(t.typeParameter(), 0);
    }

    /**
     * A type parameter the verdicts hold non-null ({@link
     * io.codelaser.maddi.cst.impl.analysis.NullabilityFacts#nonNullTypeParameter}), with no bound of its own: Kotlin's
     * {@code K : Any}. A class's, or a method's that flows into one (langchain4j's {@code <T> Response<T> from(T)}).
     */
    static boolean nonNullClassTypeParameter(io.codelaser.maddi.cst.api.info.TypeParameter tp) {
        return KotlinContext.translatingJava()
               && tp.typeBounds().stream().allMatch(ParameterizedType::isJavaLangObject)
               && KotlinNullability.verdicts().nonNullTypeParameter(tp);
    }

    private static boolean nullableBound(io.codelaser.maddi.cst.api.info.TypeParameter tp, int depth) {
        java.util.List<ParameterizedType> bounds = tp.typeBounds().stream().filter(b -> !b.isJavaLangObject()).toList();
        // langchain4j's Utils.firstNotNull: the printer bounds its own <T> by Any (KotlinMethodPrinter.nonNullBound)
        io.codelaser.maddi.cst.api.info.MethodInfo current = KotlinContext.currentMethod();
        if (bounds.isEmpty() && current != null && current.typeParameters().contains(tp)
            && KotlinMethodPrinter.nonNullBound(current, tp)) {
            return false;
        }
        if (bounds.isEmpty()) return !nonNullClassTypeParameter(tp);
        if (bounds.size() > 1 || depth > 8) return false;
        ParameterizedType bound = KotlinTypePrinter.declared(bounds.getFirst());
        return bound.nullable() == NullableState.NULLABLE
               || bound.isTypeParameter() && bound.arrays() == 0 && nullableBound(bound.typeParameter(), depth + 1);
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
            // ? super @NonNull T, of a nullable-bounded T (guava's Equivalence.Wrapper). Not ? extends @NonNull B:
            // MutableClassToInstanceMap's ForwardingMap<Class<? extends @NonNull B>, B> header would take it, and its
            // overrides' parameters, printed without, would not match
            if (pt.isTypeParameter() && pt.wildcard().isSuper() && superBoundNonNull(pt.withWildcard(null))) {
                bound += " & Any";
            }
            return (pt.wildcard().isSuper() ? "in " : "out ") + bound;
        }
        if (pt.isTypeParameter()) {
            ParameterizedType erasure = constructorTypeParameterErasure(pt);
            if (erasure != null) return of(erasure, q);
            return definitelyNonNull(pt) ? pt.typeParameter().simpleName() + " & Any" : pt.typeParameter().simpleName();
        }
        if (pt.typeInfo() == null) {
            return "Any"; // no type
        }
        String fqn = pt.typeInfo().fullyQualifiedName();
        String base = mapped(fqn);
        if (base == null) base = name(pt.typeInfo(), q);
        if (pt.parameters().isEmpty()) return rawOuter(pt.typeInfo(), base) + starProjections(pt.typeInfo());
        String inner = innerWithOuterArguments(pt, q);
        if (inner != null) return inner;
        StringBuilder sb = new StringBuilder(base).append('<');
        for (int i = 0; i < pt.parameters().size(); i++) {
            if (i > 0) sb.append(", ");
            ParameterizedType argument = pt.parameters().get(i);
            // ListenableFuture<@Nullable Void> is Kotlin's ListenableFuture<Unit>, as the unannotated one is: a Unit?
            // argument makes the two different types (guava's MoreExecutors, TrustedListenableFutureTask<@Nullable
            // Void> into a ListenableFuture<Void>), and a Java Void is null either way. Not a Void the verdicts make
            // nullable in a java.util collection or map without the annotation, where the null is a stored value:
            // jenkins's XmlFile, Map<Object, Void> beingWritten, put(o, null): Unit?. Elsewhere (a Callable<Void>,
            // a visitor's Void) the anonymous classes and futures it meets are printed Unit
            if (argument.arrays() == 0 && argument.wildcard() == null && argument.typeInfo() != null
                && "java.lang.Void".equals(argument.typeInfo().fullyQualifiedName())
                && (argument.nullable() != NullableState.NULLABLE || !KotlinContext.translatingJava()
                    || !pt.typeInfo().fullyQualifiedName().startsWith("java.util.")
                    || pt.typeInfo().fullyQualifiedName().startsWith("java.util.concurrent.")
                       && !pt.typeInfo().fullyQualifiedName().contains("Map")
                    || argument.annotations().stream().anyMatch(ae -> ae.typeInfo() != null
                        && io.codelaser.maddi.cst.impl.analysis.NullAnnotations.NULLABLE.contains(ae.typeInfo().simpleName())))) {
                argument = argument.withNullable(NullableState.NONNULL);
            }
            sb.append(of(argument, q));
        }
        return sb.append('>').toString();
    }

    /**
     * A constructor's own type parameter, which Kotlin's constructors cannot declare: its erasure, as Java's bytecode
     * has it (guava's {@code <M extends AccessibleObject & Member> Invokable(M member)} takes an AccessibleObject; a
     * use as another bound casts). Null for any other type.
     */
    static ParameterizedType constructorTypeParameterErasure(ParameterizedType pt) {
        if (pt == null || !KotlinContext.translatingJava() || !pt.isTypeParameter() || pt.wildcard() != null
            || !pt.typeParameter().isMethodTypeParameter()) {
            return null;
        }
        // its constructor is one of the type being printed, where alone its parameter's type is written
        TypeInfo current = KotlinContext.currentType();
        if (current == null || current.constructors().stream().noneMatch(c -> c.typeParameters().contains(pt.typeParameter()))) {
            return null;
        }
        ParameterizedType bound = pt.typeParameter().typeBounds().stream().filter(b -> !b.isJavaLangObject()).findFirst()
                .orElse(null);
        if (bound == null || bound.isTypeParameter()) return null;
        ParameterizedType erased = bound.withNullable(pt.nullable());
        return pt.arrays() == 0 ? erased : erased.copyWithArrays(pt.arrays());
    }

    /**
     * {@code Run<*, *>.RunExecution}: jenkins's raw {@code Run.RunExecution}, an inner class of a generic class written
     * qualified. Kotlin wants the outer class's type arguments there; a star projection accepts each. {@code base}
     * unchanged otherwise.
     */
    private static String rawOuter(TypeInfo typeInfo, String base) {
        if (!KotlinContext.translatingJava() || !KotlinTypePrinter.inner(typeInfo) || base.indexOf('<') >= 0
            || !base.endsWith("." + KotlinNames.name(typeInfo.simpleName()))) {
            return base;
        }
        TypeInfo outer = typeInfo.enclosingTypeStream().skip(1).findFirst().orElse(null);
        if (outer == null || outer.typeParameters().isEmpty()) return base;
        int dot = base.length() - KotlinNames.name(typeInfo.simpleName()).length() - 1;
        return base.substring(0, dot) + starProjections(outer) + base.substring(dot);
    }

    /**
     * {@code G<String>.In}: an inner class whose {@code parameters()} start with its outer classes' type arguments,
     * each written on its own class, as Java's printer distributes them. Null when the type has only its own.
     */
    private static String innerWithOuterArguments(ParameterizedType pt, Qualification q) {
        TypeInfo t = pt.typeInfo();
        int end = pt.parameters().size();
        if (!t.isInnerClass() || end <= t.typeParameters().size()) return null;
        java.util.List<String> segments = new java.util.ArrayList<>();
        while (true) {
            int own = t.typeParameters().size();
            if (own > end) return null;
            java.util.List<ParameterizedType> args = pt.parameters().subList(end - own, end);
            end -= own;
            TypeInfo outer = t.enclosingTypeStream().skip(1).findFirst().orElse(null);
            boolean first = end == 0 || !t.isInnerClass() || outer == null;
            String head = first ? name(t, q) : KotlinNames.name(t.simpleName());
            segments.addFirst(args.isEmpty() ? head
                    : head + args.stream().map(a -> of(a, q)).collect(Collectors.joining(", ", "<", ">")));
            if (first) return end == 0 ? String.join(".", segments) : null;
            t = outer;
        }
    }

    /**
     * A raw type as Java erases it: each type parameter's bound, {@code Any?} for none ({@code Subscriber<Event>} of
     * nacos's raw {@code Subscriber}, {@code MutableMap<Any?, Any?>} of a raw Map). A generic bound is star-projected,
     * which a type argument may be. Null when {@code raw} is not a raw generic type.
     */
    static String erased(ParameterizedType raw, Qualification q) {
        if (raw == null || raw.typeInfo() == null || raw.arrays() > 0 || !raw.parameters().isEmpty()
            || raw.typeInfo().typeParameters().isEmpty()) return null;
        String name = of(raw.withNullable(NullableState.NONNULL), q);
        int lt = name.indexOf('<');
        if (lt > 0) name = name.substring(0, lt);
        StringBuilder sb = new StringBuilder(name).append('<');
        java.util.List<io.codelaser.maddi.cst.api.info.TypeParameter> tps = raw.typeInfo().typeParameters();
        for (int i = 0; i < tps.size(); i++) {
            if (i > 0) sb.append(", ");
            ParameterizedType bound = tps.get(i).typeBounds().stream().filter(b -> !b.isJavaLangObject()).findFirst()
                    .orElse(null);
            if (bound == null || bound.typeInfo() == null) {
                sb.append("Any?");
            } else {
                String b = of(bound.typeInfo().asSimpleParameterizedType(), q);
                int blt = b.indexOf('<');
                sb.append(bound.typeInfo().typeParameters().isEmpty() || blt < 0 ? b
                        : b.substring(0, blt) + starProjections(bound.typeInfo()));
            }
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
        return KotlinContext.typeParameterInScope(tp);
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
        // <S extends @Nullable T> S reverse(): the bound's type-use annotation, which an override must repeat; more
        // than one bound goes to the where clause
        return bounds.size() == 1 ? name + " : " + inSignature(() -> of(KotlinTypePrinter.declared(bounds.getFirst()), q))
                : name;
    }

    /**
     * guava's ToDoubleRounder<X extends Number & Comparable<X>>: {@code where X : Number, X : Comparable<X>}, after a
     * class's supertypes or a function's result, for the type parameters with more than one bound; empty if none has.
     */
    static String whereClause(java.util.List<io.codelaser.maddi.cst.api.info.TypeParameter> typeParameters,
                              Qualification q) {
        java.util.List<String> constraints = new java.util.ArrayList<>();
        for (io.codelaser.maddi.cst.api.info.TypeParameter tp : typeParameters) {
            java.util.List<ParameterizedType> bounds = tp.typeBounds().stream().filter(b -> !b.isJavaLangObject()).toList();
            if (bounds.size() < 2) continue;
            for (ParameterizedType b : bounds) {
                constraints.add(KotlinNames.name(tp.simpleName()) + " : "
                                + inSignature(() -> of(KotlinTypePrinter.declared(b), q)));
            }
        }
        return constraints.isEmpty() ? "" : "where " + String.join(", ", constraints);
    }

    /** A type's name, without type arguments: as the Java printer would qualify it, segments escaped. */
    /**
     * Kotlin's own types the printer names by their simple name. A Java type of the same simple name is not imported
     * and is written qualified: guava's {@code import java.lang.reflect.Array} would make each {@code Array<V?>} the
     * reflection class, which takes no type arguments.
     */
    static final java.util.Set<String> KOTLIN_BUILT_IN_NAMES = java.util.Set.of("Array", "Any", "Unit", "Nothing");

    /** A Java type whose simple name is one of {@link #KOTLIN_BUILT_IN_NAMES}: always written qualified. */
    static boolean shadowsKotlinBuiltIn(String fullyQualifiedName) {
        int dot = fullyQualifiedName.lastIndexOf('.');
        return dot > 0 && KOTLIN_BUILT_IN_NAMES.contains(fullyQualifiedName.substring(dot + 1))
               && !fullyQualifiedName.startsWith("kotlin.");
    }

    /**
     * An inner class of a generic class, where the code printed is inside that class or a subclass of it (or a type
     * nested there): by its simple name. Qualified, Kotlin wants the outer class's type arguments,
     * {@code Maps.FilteredEntryMap<K, V>.KeySet}, which Java's {@code KeySet} does not spell out (guava's
     * FilteredEntrySortedMap.SortedKeySet extends FilteredEntryMap's KeySet).
     */
    private static boolean innerOfGenericInScope(TypeInfo typeInfo) {
        if (!KotlinContext.translatingJava() || !KotlinTypePrinter.inner(typeInfo)) return false;
        TypeInfo outer = typeInfo.enclosingTypeStream().skip(1).findFirst().orElse(null);
        if (outer == null || outer.enclosingTypeStream().allMatch(t -> t.typeParameters().isEmpty())) return false;
        TypeInfo current = KotlinContext.currentType();
        return current != null && current.enclosingTypeStream()
                .anyMatch(t -> t.meAndMyRecursiveSuperTypeStream().anyMatch(s -> s == outer));
    }

    /** A nested type whose simple name Kotlin does not resolve where the printer is: outside its outer types. */
    private static boolean notInKotlinScope(TypeInfo typeInfo) {
        if (typeInfo.isPrimaryType() || typeInfo.enclosingMethod() != null || !KotlinContext.translatingJava()) {
            return false;
        }
        TypeInfo outer = outer(typeInfo);
        if (outer == null) return false;
        for (TypeInfo t = KotlinContext.currentType(); t != null; t = outer(t)) {
            if (t == outer || t == typeInfo || inherits(t, outer, new java.util.HashSet<>())) return false;
        }
        return true;
    }

    /** The type {@code nested} is declared in; null for a primary type. */
    private static TypeInfo outer(TypeInfo nested) {
        if (nested.isPrimaryType() || nested.primaryType() == null) return null;
        TypeInfo primary = nested.primaryType();
        if (primary.subTypes().contains(nested)) return primary;
        return primary.recursiveSubTypeStream().filter(t -> t.subTypes().contains(nested)).findFirst().orElse(null);
    }

    private static boolean inherits(TypeInfo sub, TypeInfo sup, java.util.Set<TypeInfo> visited) {
        if (sub == null || !visited.add(sub)) return false;
        if (sub == sup) return true;
        if (sub.parentClass() != null && inherits(sub.parentClass().typeInfo(), sup, visited)) return true;
        return sub.interfacesImplemented().stream().anyMatch(i -> inherits(i.typeInfo(), sup, visited));
    }

    public static String name(TypeInfo typeInfo, Qualification q) {
        String mapped = mapped(typeInfo.fullyQualifiedName());
        if (mapped != null) return mapped;
        if (typeInfo.isPrimaryType() && shadowsKotlinBuiltIn(typeInfo.fullyQualifiedName())) {
            return KotlinNames.dotted(typeInfo.fullyQualifiedName());
        }
        if (innerOfGenericInScope(typeInfo)) return KotlinNames.name(typeInfo.simpleName());
        if (q == null) return KotlinNames.name(typeInfo.simpleName());
        String minimal = TypeNameImpl.typeName(typeInfo, q.qualifierRequired(typeInfo), false).minimal();
        if (minimal.indexOf('.') < 0 && notInKotlinScope(typeInfo)) {
            // @ReflectionSupport(ReflectionSupport.Level.FULL) on guava's AbstractFuture: Java's simple name, where
            // Kotlin sees a nested type only inside its outer types (and their subtypes); outside, the outer's name
            minimal = name(outer(typeInfo), q) + "." + typeInfo.simpleName();
            return KotlinNames.dotted(minimal);
        }
        TypeInfo primary = typeInfo.primaryType();
        if (primary != null && (minimal.equals(primary.fullyQualifiedName())
                                || minimal.startsWith(primary.fullyQualifiedName() + "."))
            && primary.fullyQualifiedName().equals(KotlinContext.importedName(primary.simpleName()))) {
            // jenkins's {@link jenkins.model.Jenkins} in a Javadoc: Java's qualification then spells the type fully
            // qualified everywhere, which Kotlin reads as the local `val jenkins` beside it. The file imports it
            minimal = primary.simpleName() + minimal.substring(primary.fullyQualifiedName().length());
        }
        String first = minimal.contains(".") ? minimal.substring(0, minimal.indexOf('.')) : minimal;
        String imported = KotlinContext.importedName(first);
        if (primary != null && first.equals(primary.simpleName()) && imported != null
            && !imported.equals(primary.fullyQualifiedName())) {
            // guava's ImmutableRangeMap: Function<Range<K>, Cut<K>>(…) for com.google.common.base.Function, where the
            // file imports java.util.function.Function
            return KotlinNames.dotted(typeInfo.fullyQualifiedName());
        }
        if (primary != null && (minimal.equals(primary.simpleName()) || minimal.startsWith(primary.simpleName() + "."))) {
            KotlinContext.referencedType(primary);
        }
        return KotlinNames.dotted(minimal);
    }
}
