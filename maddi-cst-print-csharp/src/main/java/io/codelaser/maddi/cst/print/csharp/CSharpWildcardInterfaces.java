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
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.Cast;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.InstanceOf;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Java's wildcard of a self-bounded type, {@code Score<?>} where {@code Score<Score_ extends Score<Score_>>}, has no
 * C# type argument: none satisfies {@code where Score_ : IScore<Score_>}. As C#'s own {@code IComparable} beside
 * {@code IComparable<T>}, such a type has a wildcard interface without its self-bounded type parameters, which the
 * wildcard is: {@code IScore} beside {@code IScore<Score_>}, {@code IInnerScoreDirector<Solution_>} beside
 * {@code IInnerScoreDirector<Solution_, Score_>}; a class's or a record's is the interface {@code I} + its name, which
 * it implements, a record's with its components as properties.
 * <p>
 * Any other wildcard has no C# type argument either, where a method's parameter does not capture it
 * ({@link CSharpWildcards}): a field's {@code TupleLifecycle<?>}, a constructor's, a result's {@code Node<?, ?, ?>}.
 * {@code ITupleLifecycle<ITuple>}, its bound, is no {@code ITupleLifecycle<IUniTuple<A>>}. The type parameters the
 * program declares with {@code ?} ({@link #wildcarded}) are removed as the self-bounded ones are:
 * {@code ITupleLifecycle} beside {@code ITupleLifecycle<Tuple_>}; where the wildcard interface declares every member
 * the code reaches through such a wildcard, not a field.
 * <p>
 * The wildcard interface declares the type's own members that a wildcard's user can call: those whose parameters do
 * not mention a removed type parameter, a class's public and internal ones (Java's package-private), with the removed parameter in the result erased to its bound's wildcard
 * interface ({@code Score_ Negate()} is {@code IScore Negate()}). The type implements each explicitly by calling its
 * own, {@code IScore IScore.Negate() => Negate();}, a default implementation in an interface, so that every
 * implementation of the type is one of the wildcard interface. It extends the wildcard interfaces of the type's
 * supertypes.
 */
final class CSharpWildcardInterfaces {

    private CSharpWildcardInterfaces() {
    }

    /** A member of the wildcard interface: its C# declaration and its bridge, the type's explicit implementation. */
    record Member(String declaration, String bridge) {
    }

    /**
     * The self-bounded and the wildcarded type parameters of a program type, which its wildcard interface does not
     * have; empty when it has none, and for a delegate, which implements no interface.
     */
    static List<TypeParameter> removed(TypeInfo t) {
        if (t == null || !CSharpNames.translated(t) || t.typeParameters().isEmpty() || t.typeNature().isEnum()
            || t.isAnonymous() || t.enclosingMethod() != null) {
            return List.of();
        }
        Map<TypeInfo, List<TypeParameter>> memo = CSharpContext.memo().wildcardInterfaces;
        List<TypeParameter> known = memo.get(t);
        if (known != null) return known;
        // a member's result of another type asks that type's: a cycle back to t has t's candidates
        if (!IN_PROGRESS.get().add(t)) return candidates(t);
        try {
            known = computeRemoved(t);
        } finally {
            IN_PROGRESS.get().remove(t);
        }
        memo.put(t, known);
        return known;
    }

    private static final ThreadLocal<Set<TypeInfo>> IN_PROGRESS = ThreadLocal.withInitial(HashSet::new);

    /**
     * The self-bounded type parameters, and the wildcarded ones where the wildcard interface declares every member that
     * Java's code reaches through the wildcard; else a wildcard is its bound, as before.
     */
    private static List<TypeParameter> computeRemoved(TypeInfo t) {
        List<TypeParameter> candidates = candidates(t);
        List<TypeParameter> selfBounded = t.typeParameters().stream().filter(tp -> selfBounded(t, tp)).toList();
        if (candidates.size() == selfBounded.size() || reachable(t, candidates)) return candidates;
        return selfBounded;
    }

    private static List<TypeParameter> candidates(TypeInfo t) {
        CSharpProgram program = CSharpContext.program();
        // a delegate implements no interface; an interface's companion is its wildcard interface (CSharpTypePrinter),
        // not a self-bounded one's: WeightedScoreImpacter's companion and wildcard interface stay as they were
        Set<TypeParameter> wildcarded = program.delegate(t) || companionOfSelfBounded(t) ? Set.of()
                : program.wildcarded(t);
        return t.typeParameters().stream().filter(tp -> selfBounded(t, tp) || wildcarded.contains(tp)).toList();
    }

    /** An interface with a companion and a self-bounded type parameter. */
    static boolean companionOfSelfBounded(TypeInfo t) {
        return t.isInterface() && CSharpNames.hasCompanion(t)
               && t.typeParameters().stream().anyMatch(tp -> selfBounded(t, tp));
    }

    /** Whether the wildcard interface without {@code removed} declares what the code reaches through t's wildcard. */
    private static boolean reachable(TypeInfo t, List<TypeParameter> removed) {
        Set<Object> declared = declared(t, removed);
        for (Object info : CSharpContext.program().reachedThroughWildcard(t)) {
            if (declared.contains(info)) continue;
            if (info instanceof MethodInfo m) {
                if ("java.lang.Object".equals(m.typeInfo().fullyQualifiedName())) continue;
                if (m.overrides().stream().filter(o -> o != m).map(MethodInfo::typeInfo)
                        .anyMatch(o -> inheritedClosed(t, o, removed)) || inheritedClosed(t, m.typeInfo(), removed)) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    /** {@code owner} is an interface the wildcard interface extends: t's, and closed. */
    private static boolean inheritedClosed(TypeInfo t, TypeInfo owner, List<TypeParameter> removed) {
        if (owner.equals(t) || !owner.isInterface()) return false;
        for (ParameterizedType s : t.interfacesImplemented()) {
            TypeInfo st = s.typeInfo();
            if (st == null || CstQueries.mentionsAny(s, removed)) continue;
            if (st.equals(owner) || st.recursiveSuperTypeStream().anyMatch(owner::equals)) return true;
        }
        return false;
    }

    /** The members the wildcard interface without {@code removed} declares, as {@link #members} prints them. */
    private static Set<Object> declared(TypeInfo t, List<TypeParameter> removed) {
        Set<Object> declared = new HashSet<>();
        if (t.typeNature().isRecord()) {
            for (FieldInfo f : t.fields()) {
                if (f.isStatic() || !erasable(f.type(), removed)) continue;
                declared.add(f);
                t.methods().stream().filter(m -> CstQueries.isRecordAccessor(m) && m.name().equals(f.name()))
                        .forEach(declared::add);
            }
        }
        for (MethodInfo m : t.methods()) {
            if (candidate(t, m, removed) && (m.isVoid() || erasable(m.returnType(), removed))) declared.add(m);
        }
        declared.addAll(readFields(t, removed));
        return declared;
    }

    /**
     * A class's fields that the code reads through the wildcard, which the wildcard interface has as properties of
     * the field's name: {@code Type IMoveSelectorConfig.filterClass => filterClass;}. Not a record's, whose components
     * it has anyway.
     */
    private static List<FieldInfo> readFields(TypeInfo t, List<TypeParameter> removed) {
        if (t.typeNature().isRecord()) return List.of();
        Set<Object> reached = CSharpContext.program().reachedThroughWildcard(t);
        return t.fields().stream().filter(f -> !f.isStatic() && !f.isSynthetic() && reached.contains(f)
                                               && erasable(f.type(), removed)).toList();
    }

    /** Whether {@link #erased} has a type for {@code pt}. */
    private static boolean erasable(ParameterizedType pt, List<TypeParameter> removed) {
        if (!CstQueries.mentionsAny(pt, removed)) return true;
        if (pt.arrays() > 0) return false;
        if (pt.isTypeParameter()) return true;
        TypeInfo t = pt.typeInfo();
        if (t == null) return false;
        if (!CSharpNames.translated(t)) return CSharpTypeName.dropsArguments(t);
        if (!has(t)) return false;
        List<TypeParameter> own = removed(t);
        for (int i = 0; i < t.typeParameters().size(); i++) {
            if (own.contains(t.typeParameters().get(i))) continue;
            if (i >= pt.parameters().size() || CstQueries.mentionsAny(pt.parameters().get(i), removed)) return false;
        }
        return true;
    }

    /**
     * A method the wildcard interface may declare: an instance method reachable outside the type, not an override of
     * the program's (the supertype's wildcard interface has it) or of Object's, not a generic method C# prints erased,
     * with no parameter of a removed type.
     */
    private static boolean candidate(TypeInfo t, MethodInfo m, List<TypeParameter> removed) {
        CSharpProgram program = CSharpContext.program();
        if (CstQueries.isRecordAccessor(m)) return false;
        if (m.isStatic() || m.isSynthetic() || m.isConstructor() || m.access() != null && m.access().isPrivate()
            || m.overrides().stream().anyMatch(o -> o != m && (CSharpNames.translated(o.typeInfo())
                                                              || "java.lang.Object".equals(o.typeInfo().fullyQualifiedName())))
            || CSharpWildcards.mayCapture(m) || program.typeToken(m) >= 0 || program.erasedGeneric(m)
            || CSharpHierarchy.interfaceMethodDefaulted(t, m) != null) {
            return false;
        }
        // a class's members that Java's code reaches through the wildcard: internal ones too
        String access = t.isInterface() ? null : CSharpModifiers.access(m, t);
        if (access != null && !"public".equals(access) && !"internal".equals(access)
            && !"protected internal".equals(access)) {
            return false;
        }
        CSharpProperties.Property accessor = program.accessorOf(m);
        if (accessor != null && accessor.getter() != m) return false;
        if (m.parameters().stream().anyMatch(p -> CstQueries.mentionsAny(p.parameterizedType(), removed) || p.isVarArgs())) {
            return false;
        }
        return m.typeParameters().stream().noneMatch(tp -> tp.typeBounds().stream().anyMatch(b -> CstQueries.mentionsAny(b, removed)));
    }

    private static boolean selfBounded(TypeInfo t, TypeParameter tp) {
        return tp.typeBounds().stream().anyMatch(b -> CstQueries.mentionsAny(b, t.typeParameters()));
    }

    /**
     * The type parameters of the program's generic types that the program declares with an unbound wildcard, where C#
     * has no type argument for it: the type of a field, a constructor's parameter, a
     * method's result, a local, a cast, a bound, a type argument; not a method's parameter that {@link CSharpWildcards}
     * captures. Such a wildcard is the type's wildcard interface, which every {@code X<A>} is, as a self-bounded one
     * is: {@code TupleLifecycle<?>} is {@code ITupleLifecycle} beside {@code ITupleLifecycle<Tuple_>}.
     */
    /** A field the code writes through a wildcard, which the wildcard interface cannot have. */
    private record Written(FieldInfo field) {
    }

    record Uses(Map<TypeInfo, Set<TypeParameter>> wildcarded, Map<TypeInfo, Set<Object>> reached) {
        static final Uses NONE = new Uses(Map.of(), Map.of());
    }

    static Uses wildcarded(List<TypeInfo> all) {
        Map<TypeInfo, Set<TypeParameter>> wildcarded = new HashMap<>();
        Consumer<ParameterizedType> declared = pt -> wildcarded(pt, wildcarded);
        for (TypeInfo t : all) {
            t.typeParameters().forEach(tp -> tp.typeBounds().forEach(declared));
            if (t.parentClass() != null) declared.accept(t.parentClass());
            t.interfacesImplemented().forEach(declared);
            t.fields().forEach(f -> declared.accept(f.type()));
            for (MethodInfo m : t.constructorsAndMethods()) {
                if (!m.isConstructor()) declared.accept(m.returnType());
                m.typeParameters().forEach(tp -> tp.typeBounds().forEach(declared));
                for (int i = 0; i < m.parameters().size(); i++) {
                    ParameterizedType pt = m.parameters().get(i).parameterizedType();
                    // a captured parameter's wildcards are the method's type parameters, its other arguments printed
                    if (CSharpWildcards.capturedParameter(m, i)) {
                        pt.parameters().stream().filter(a -> a.wildcard() == null).forEach(declared);
                    } else {
                        declared.accept(pt);
                    }
                }
            }
            for (Element element : CstQueries.code(t)) {
                element.visit((Element e) -> {
                    // Java's var is C#'s
                    if (e instanceof LocalVariableCreation lvc && !lvc.isVar()) {
                        lvc.localVariableStream().forEach(v -> declared.accept(v.parameterizedType()));
                    }
                    if (e instanceof Cast cast) declared.accept(cast.parameterizedType());
                    if (e instanceof InstanceOf instanceOf && instanceOf.testType() != null) declared.accept(instanceOf.testType());
                    return true;
                });
            }
        }
        Map<TypeInfo, Set<TypeParameter>> result = new HashMap<>();
        wildcarded.forEach((t, tps) -> result.put(t, Set.copyOf(tps)));
        // the members the code reaches through such a wildcard: a method called, a field read
        Map<TypeInfo, Set<Object>> reached = new HashMap<>();
        BiConsumer<Expression, Object> through = (scope, member) -> {
            if (scope == null) return;
            ParameterizedType pt = scope.parameterizedType();
            TypeInfo t = pt == null ? null : pt.typeInfo();
            Set<TypeParameter> tps = t == null ? null : result.get(t);
            if (tps == null || pt.arrays() > 0 || pt.parameters().size() != t.typeParameters().size()) return;
            for (int i = 0; i < pt.parameters().size(); i++) {
                if (tps.contains(t.typeParameters().get(i)) && open(pt.parameters().get(i))) {
                    reached.computeIfAbsent(t, k -> new HashSet<>()).add(member);
                    return;
                }
            }
        };
        for (TypeInfo t : all) {
            for (Element element : CstQueries.code(t)) {
                element.visit((Element e) -> {
                    if (e instanceof MethodCall mc && !mc.methodInfo().isStatic()) through.accept(mc.object(), mc.methodInfo());
                    if (e instanceof VariableExpression ve && ve.variable() instanceof FieldReference fr
                        && !fr.fieldInfo().isStatic()) {
                        through.accept(fr.scope(), fr.fieldInfo());
                    }
                    // a field written through the wildcard: no property of the wildcard interface takes it
                    if (e instanceof Assignment a && a.variableTarget() instanceof FieldReference fr
                        && !fr.fieldInfo().isStatic()) {
                        through.accept(fr.scope(), new Written(fr.fieldInfo()));
                    }
                    return true;
                });
            }
        }
        Map<TypeInfo, Set<Object>> reachedCopy = new HashMap<>();
        reached.forEach((t, members) -> reachedCopy.put(t, Set.copyOf(members)));
        return new Uses(Map.copyOf(result), Map.copyOf(reachedCopy));
    }

    private static void wildcarded(ParameterizedType pt, Map<TypeInfo, Set<TypeParameter>> wildcarded) {
        if (pt == null) return;
        TypeInfo t = pt.typeInfo();
        List<ParameterizedType> arguments = pt.parameters();
        if (t != null && CSharpNames.translated(t) && arguments.size() == t.typeParameters().size()) {
            for (int i = 0; i < arguments.size(); i++) {
                ParameterizedType a = arguments.get(i);
                if (open(a)) {
                    wildcarded.computeIfAbsent(t, k -> new HashSet<>()).add(t.typeParameters().get(i));
                }
            }
        }
        arguments.forEach(a -> wildcarded(a, wildcarded));
    }

    /**
     * {@code ?}, which C#'s bound is not. Not {@code ? extends Mapped_}: the bound, as C# has it, is what the code
     * stores there, {@code TriFunction<? super A, ? super B, ? super C, ? extends Mapped_>} the mapper of a Mapped_.
     */
    private static boolean open(ParameterizedType a) {
        return a.wildcard() != null && a.wildcard().isUnbound();
    }

    static boolean has(TypeInfo t) {
        return !removed(t).isEmpty();
    }

    /** The wildcard interface's name: an interface's own, a class's {@code I} + its own. */
    static String name(TypeInfo t) {
        String own = CSharpNames.type(t);
        return t.isInterface() ? own : "I" + own;
    }

    /** The wildcard interface's type parameters, the type's that remain. */
    static List<TypeParameter> remaining(TypeInfo t) {
        List<TypeParameter> removed = removed(t);
        return t.typeParameters().stream().filter(tp -> !removed.contains(tp)).toList();
    }

    /** The wildcard interface as the type itself names it: {@code IInnerScoreDirector<Solution_>}. */
    static String selfReference(TypeInfo t) {
        List<TypeParameter> remaining = remaining(t);
        return name(t) + CSharpNames.typeParameters(remaining);
    }

    /**
     * Whether {@code pt} is a wildcard or raw use of a type with a wildcard interface, where no C# type argument
     * satisfies the self-bound: {@code Score<?>}, {@code Score<? extends Score<?>>}, a raw {@code Score}.
     */
    static boolean isWildcardUse(ParameterizedType pt) {
        TypeInfo t = pt.typeInfo();
        if (t == null) return false;
        List<TypeParameter> removed = removed(t);
        if (removed.isEmpty()) return false;
        List<ParameterizedType> arguments = pt.parameters();
        // a raw type is its wildcard interface where the type is self-bounded
        if (arguments.isEmpty()) return removed.stream().anyMatch(tp -> selfBounded(t, tp));
        for (int i = 0; i < arguments.size() && i < t.typeParameters().size(); i++) {
            TypeParameter tp = t.typeParameters().get(i);
            if (!removed.contains(tp)) continue;
            ParameterizedType a = arguments.get(i);
            if (a.wildcard() == null) continue;
            if (selfBounded(t, tp) ? a.wildcard().isUnbound() || a.wildcard().isSuper()
                                     || containsWildcard(a.withWildcard(null))
                    : open(a)) {
                return true;
            }
        }
        return false;
    }

    /** {@code pt}, a wildcard use, as its wildcard interface: the remaining arguments printed as they are. */
    static String wildcardUse(ParameterizedType pt, Qualification q) {
        TypeInfo t = pt.typeInfo();
        List<TypeParameter> removed = removed(t);
        List<String> printed = new ArrayList<>();
        List<ParameterizedType> arguments = pt.parameters();
        for (int i = 0; i < t.typeParameters().size(); i++) {
            TypeParameter tp = t.typeParameters().get(i);
            if (removed.contains(tp)) continue;
            if (i < arguments.size()) {
                ParameterizedType a = arguments.get(i);
                boolean unbound = a.wildcard() != null && a.wildcard().isUnbound();
                printed.add(unbound ? CSharpTypeName.rawArgument(tp, q) : CSharpTypeName.argument(a, q));
            } else {
                printed.add(CSharpTypeName.rawArgument(tp, q));
            }
        }
        String name = CSharpTypeName.name(t, q);
        String wildcardInterface = t.isInterface() ? name : qualifiedInterface(name);
        return wildcardInterface + (printed.isEmpty() ? "" : "<" + String.join(", ", printed) + ">");
    }

    /**
     * In an instance member of {@code t} itself, a cast to or a local of its wildcard use is {@code t} with its own
     * type parameters, which have the private members the wildcard interface has not: Java's {@code equals},
     * {@code MinPropertyUniCollector<?, ?, ?> that = (MinPropertyUniCollector<?, ?, ?>) o; … that.propertyMapper},
     * after a {@code GetType()} comparison that C# makes with the type arguments. Null otherwise.
     */
    static String ownUse(ParameterizedType pt, Qualification q) {
        TypeInfo t = pt.typeInfo();
        if (t == null || pt.arrays() != 0 || !t.equals(CSharpContext.currentType()) || !isWildcardUse(pt)) return null;
        MethodInfo m = CSharpContext.currentMethod();
        if (m == null || m.isStatic() || !t.equals(m.typeInfo())) return null;
        return CSharpTypeName.name(t, q) + CSharpNames.typeParameters(t.typeParameters());
    }

    /** {@code Ns.Outer.Config} is {@code Ns.Outer.IConfig}. */
    private static String qualifiedInterface(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "I" + name : name.substring(0, dot + 1) + "I" + name.substring(dot + 1);
    }

    /**
     * A type as the wildcard interface of {@code t} has it: a removed type parameter is its bound's wildcard
     * interface, a type with a wildcard interface whose removed positions mention one is that wildcard interface. Null
     * when the type mentions a removed parameter otherwise ({@code List<Score_>}), which no conversion makes the
     * wildcard interface's.
     */
    static String erased(ParameterizedType pt, List<TypeParameter> removed, Qualification q) {
        if (!CstQueries.mentionsAny(pt, removed)) return CSharpTypeName.of(pt, q);
        if (pt.arrays() > 0) return null;
        if (pt.isTypeParameter()) return erasedBound(pt.typeParameter(), removed, new HashSet<>(), q);
        TypeInfo t = pt.typeInfo();
        // Class<A>: C#'s Type, which has no arguments
        if (t != null && !CSharpNames.translated(t) && CSharpTypeName.dropsArguments(t)) return CSharpTypeName.of(pt, q);
        if (t == null || !has(t)) return null;
        List<TypeParameter> own = removed(t);
        List<String> printed = new ArrayList<>();
        for (int i = 0; i < t.typeParameters().size(); i++) {
            if (own.contains(t.typeParameters().get(i))) continue;
            if (i >= pt.parameters().size()) return null;
            ParameterizedType a = pt.parameters().get(i);
            if (CstQueries.mentionsAny(a, removed)) return null;
            printed.add(CSharpTypeName.argument(a, q));
        }
        String name = CSharpTypeName.name(t, q);
        String wildcardInterface = t.isInterface() ? name : qualifiedInterface(name);
        return wildcardInterface + (printed.isEmpty() ? "" : "<" + String.join(", ", printed) + ">");
    }

    private static String erasedBound(TypeParameter tp, List<TypeParameter> removed, Set<TypeParameter> seen,
                                      Qualification q) {
        if (!seen.add(tp)) return "object";
        ParameterizedType bound = tp.typeBounds().stream().filter(b -> !b.isJavaLangObject()).findFirst().orElse(null);
        if (bound == null) return "object";
        List<TypeParameter> all = new ArrayList<>(removed);
        if (!all.contains(tp)) all.add(tp);
        String erased = erased(bound, all, q);
        return erased == null ? "object" : erased;
    }

    /**
     * The wildcard interface's supertypes: the wildcard interfaces of the type's self-bounded supertypes, and its plain
     * interfaces.
     */
    static List<String> supertypes(TypeInfo t, Qualification q) {
        List<TypeParameter> removed = removed(t);
        Set<String> supers = new LinkedHashSet<>();
        List<ParameterizedType> all = new ArrayList<>(t.interfacesImplemented());
        if (t.parentClass() != null && !t.parentClass().isJavaLangObject()) all.add(t.parentClass());
        for (ParameterizedType s : all) {
            TypeInfo st = s.typeInfo();
            if (st == null) continue;
            if (!CstQueries.mentionsAny(s, removed)) {
                // a closed supertype: an interface is itself, with its own wildcard interface; a class only its wildcard
                // interface
                if (st.isInterface()) supers.add(CSharpTypeName.of(s, q));
                else if (has(st)) supers.add(interfaceOfClosed(s, q));
            } else if (has(st)) {
                String erased = erased(s, removed, q);
                if (erased != null) supers.add(erased);
            } else if (comparableOfRemoved(s, removed, t)) {
                // Comparable<Score_>: C#'s non-generic IComparable, which the type implements by a cast
                CSharpContext.using("System");
                supers.add("IComparable");
            }
        }
        return List.copyOf(supers);
    }

    /** {@code Comparable<Score_>} of a removed type parameter, or {@code Comparable<InnerScore<Score_>>} of the type itself. */
    private static boolean comparableOfRemoved(ParameterizedType s, List<TypeParameter> removed, TypeInfo t) {
        if (s.typeInfo() == null || !"java.lang.Comparable".equals(s.typeInfo().fullyQualifiedName())
            || s.parameters().size() != 1) {
            return false;
        }
        ParameterizedType argument = s.parameters().getFirst();
        return argument.isTypeParameter() ? removed.contains(argument.typeParameter())
                : argument.arrays() == 0 && t.equals(argument.typeInfo());
    }

    private static String interfaceOfClosed(ParameterizedType s, Qualification q) {
        TypeInfo t = s.typeInfo();
        List<TypeParameter> own = removed(t);
        List<String> printed = new ArrayList<>();
        for (int i = 0; i < t.typeParameters().size() && i < s.parameters().size(); i++) {
            if (!own.contains(t.typeParameters().get(i))) printed.add(CSharpTypeName.argument(s.parameters().get(i), q));
        }
        String name = CSharpTypeName.name(t, q);
        String wildcardInterface = t.isInterface() ? name : qualifiedInterface(name);
        return wildcardInterface + (printed.isEmpty() ? "" : "<" + String.join(", ", printed) + ">");
    }

    /** The wildcard interface's members, with their bridges. */
    static List<Member> members(TypeInfo t, Qualification q) {
        List<TypeParameter> removed = removed(t);
        String self = selfReference(t);
        CSharpProgram program = CSharpContext.program();
        List<Member> members = new ArrayList<>();
        Set<String> signatures = new HashSet<>();
        if (t.typeNature().isRecord()) {
            // a record's components, its properties
            for (FieldInfo f : t.fields()) {
                if (f.isStatic()) continue;
                NullableState nullable = program.facts().nullable(f);
                String type = CstQueries.mentionsAny(f.type(), removed) ? erased(f.type(), removed, q)
                        : CSharpTypeName.declared(f.type(), false, nullable, q);
                if (type == null) continue;
                String property = CSharpNames.field(f);
                signatures.add(property);
                boolean assertNotNull = program.facts().nullabilityAnalysed() && CstQueries.mentionsAny(f.type(), removed)
                                        && !type.endsWith("?");
                members.add(new Member(type + " " + property + " { get; }",
                        type + " " + self + "." + property + " => " + property + (assertNotNull ? "!" : "") + ";"));
            }
        }
        for (FieldInfo f : readFields(t, removed)) {
            NullableState nullable = program.facts().nullable(f);
            String type = CstQueries.mentionsAny(f.type(), removed) ? erased(f.type(), removed, q)
                    : CSharpTypeName.declared(f.type(), false, nullable, q);
            if (type == null) continue;
            String property = CSharpNames.field(f);
            if (!signatures.add(property)) continue;
            String modifier = "public".equals(CSharpModifiers.access(f)) ? "" : "internal ";
            boolean assertNotNull = program.facts().nullabilityAnalysed() && CstQueries.mentionsAny(f.type(), removed)
                                    && !type.endsWith("?");
            members.add(new Member(modifier + type + " " + property + " { get; }",
                    type + " " + self + "." + property + " => " + property + (assertNotNull ? "!" : "") + ";"));
        }
        for (MethodInfo m : t.methods()) {
            if (!candidate(t, m, removed)) continue;
            String access = t.isInterface() ? null : CSharpModifiers.access(m, t);
            String modifier = access == null || "public".equals(access) ? "" : "internal ";
            String returnType = m.isVoid() ? "void" : result(m, removed, q);
            if (returnType == null) continue;
            String property = program.propertyRead(m);
            if (property != null) {
                if (!m.parameters().isEmpty() || !signatures.add(property)) continue;
                members.add(new Member(modifier + returnType + " " + property + " { get; }",
                        returnType + " " + self + "." + property + " => " + property + asserted(m, removed, returnType)
                        + ";"));
                continue;
            }
            String name = CSharpNames.method(m);
            String typeParameters = CSharpNames.typeParameters(m.typeParameters());
            List<String> parameters = new ArrayList<>();
            List<String> arguments = new ArrayList<>();
            for (var p : m.parameters()) {
                String pn = CSharpNames.name(p.name());
                // with the nullability the type's own member has, which the bridge passes on (CS8604)
                parameters.add(CSharpTypeName.declared(p.parameterizedType(), false, program.facts().nullable(p), q)
                               + " " + pn);
                arguments.add(pn);
            }
            String signature = name + typeParameters + "(" + String.join(", ", parameters) + ")";
            if (!signatures.add(name + "/" + parameters.stream().map(s -> s.substring(0, s.lastIndexOf(' ')))
                    .collect(Collectors.joining(",")))) {
                continue;
            }
            List<String> constraints = CSharpTypeName.constraints(m.typeParameters(), q);
            String declaration = modifier + returnType + " " + signature
                                 + (constraints.isEmpty() ? "" : " " + String.join(" ", constraints)) + ";";
            String call = name + typeParameters + "(" + String.join(", ", arguments) + ")" + asserted(m, removed, returnType);
            String bridge = returnType + " " + self + "." + signature + " => " + call + ";";
            members.add(new Member(declaration, bridge));
        }
        return members;
    }

    /**
     * A member's result in the wildcard interface: the type's own, with its nullability ({@code string?}), which the
     * bridge returns (CS8603, CS8766); a removed type parameter erased, {@code IScore?} when the member may return
     * null.
     */
    private static String result(MethodInfo m, List<TypeParameter> removed, Qualification q) {
        NullableState nullable = CSharpContext.program().facts().nullable(m);
        if (!CstQueries.mentionsAny(m.returnType(), removed)) return CSharpTypeName.declared(m.returnType(), false, nullable, q);
        String erased = erased(m.returnType(), removed, q);
        if (erased == null) return null;
        boolean nullableResult = nullable == NullableState.NULLABLE
                                 && !erased.endsWith("?");
        return nullableResult ? erased + "?" : erased;
    }

    /**
     * {@code !} on a bridge's call whose result, a removed type parameter, the analysis finds not null: C# sees a type
     * parameter converted to the wildcard interface as possibly null (CS8603), the bridge returns what the member does.
     */
    private static String asserted(MethodInfo m, List<TypeParameter> removed, String returnType) {
        return CSharpContext.program().facts().nullabilityAnalysed() && CstQueries.mentionsAny(m.returnType(), removed)
               && !returnType.endsWith("?") ? "!" : "";
    }

    /** {@code interface IScore : … { members }}, after the type's own declaration. */
    static OutputBuilder declaration(TypeInfo t, String access, Qualification q) {
        return declaration(t, access, q, List.of());
    }

    /** As above, with the static members of an interface's companion, which has the wildcard interface's name. */
    static OutputBuilder declaration(TypeInfo t, String access, Qualification q, List<OutputBuilder> statics) {
        List<TypeParameter> remaining = remaining(t);
        List<TypeParameter> removed = removed(t);
        String typeParameters = CSharpNames.typeParameters(remaining);
        StringBuilder header = new StringBuilder();
        if (access != null) header.append(access).append(' ');
        header.append("interface ").append(name(t)).append(typeParameters);
        List<String> supers = supertypes(t, q);
        if (!supers.isEmpty()) header.append(" : ").append(String.join(", ", supers));
        for (String constraint : CSharpTypeName.constraints(remaining.stream()
                .filter(tp -> tp.typeBounds().stream().noneMatch(b -> CstQueries.mentionsAny(b, removed))).toList(), q)) {
            header.append(' ').append(constraint);
        }
        List<OutputBuilder> body = new ArrayList<>(members(t, q).stream()
                .map(m -> (OutputBuilder) new OutputBuilderImpl().add(new TextImpl(m.declaration()))).toList());
        body.addAll(statics);
        return new OutputBuilderImpl().add(new TextImpl(header.toString())).add(SpaceEnum.ONE)
                .add(CSharpStatementPrinter.braces(body));
    }

    /** The type's explicit implementations of its wildcard interface's members. */
    static List<OutputBuilder> bridges(TypeInfo t, Qualification q) {
        List<OutputBuilder> bridges = new ArrayList<>(members(t, q).stream()
                .map(m -> (OutputBuilder) new OutputBuilderImpl().add(new TextImpl(m.bridge()))).toList());
        List<TypeParameter> removed = removed(t);
        t.interfacesImplemented().stream().filter(s -> comparableOfRemoved(s, removed, t)).findFirst().ifPresent(s -> {
            String object = CSharpContext.program().facts().nullabilityAnalysed() ? "object?" : "object";
            ParameterizedType argument = s.parameters().getFirst();
            String self = argument.isTypeParameter() ? CSharpNames.typeParameter(argument.typeParameter())
                    : CSharpTypeName.of(argument, q);
            bridges.add(new OutputBuilderImpl().add(new TextImpl("int IComparable.CompareTo(" + object + " other) => CompareTo(("
                    + self + ") other!);")));
        });
        return bridges;
    }


    private static boolean containsWildcard(ParameterizedType t) {
        if (t == null) return false;
        if (t.wildcard() != null) return true;
        return t.parameters().stream().anyMatch(CSharpWildcardInterfaces::containsWildcard);
    }
}
