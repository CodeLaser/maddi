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

package io.codelaser.maddi.cst.print.csharp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.Cast;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.InstanceOf;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.type.NamedType;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;
import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.print.translate.CstSupertypes;

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
        if (candidates.size() == selfBounded.size()) return candidates;
        List<Object> unreachable = unreachable(t, candidates);
        if (unreachable.isEmpty()) return candidates;
        CSharpContext.message(CSharpPrintCode.NO_WILDCARD_INTERFACE, t, t.fullyQualifiedName() + ": "
                + unreachable.stream().map(String::valueOf).collect(Collectors.joining("; ")));
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

    /**
     * What the code reaches through t's wildcard that the wildcard interface without {@code removed} does not
     * declare: a member, a field written, a generic parameter the wildcard goes into ({@link #wildcarded}); empty
     * when it declares everything.
     */
    private static List<Object> unreachable(TypeInfo t, List<TypeParameter> removed) {
        List<Object> unreachable = new ArrayList<>();
        Set<Object> declared = declared(t, removed);
        for (Object info : CSharpContext.program().reachedThroughWildcard(t)) {
            if (declared.contains(info) || info == CSharpOpeners.OPENED) continue;
            if (info instanceof MethodInfo m) {
                // Object's, or an override of Object's (javac resolves tag.toString() to Tag's): every C# object has it
                if (m.overrides().stream().anyMatch(o -> "java.lang.Object".equals(o.typeInfo().fullyQualifiedName()))
                    || "java.lang.Object".equals(m.typeInfo().fullyQualifiedName())) {
                    continue;
                }
                if (m.overrides().stream().filter(o -> o != m).map(MethodInfo::typeInfo)
                        .anyMatch(o -> inheritedClosed(t, o, removed)) || inheritedClosed(t, m.typeInfo(), removed)) {
                    continue;
                }
                // a superclass's method its own wildcard interface declares, which t's extends; a super-interface's
                if (declaredBySuperclass(t, m) || declaredBySuperinterface(t, m, removed)) continue;
            }
            unreachable.add(info);
        }
        return unreachable;
    }

    /**
     * {@code m} is a superclass's, declared by that superclass's wildcard interface, which t's wildcard interface
     * extends: each class between has one, extending the next ({@link #supertypes}).
     */
    private static boolean declaredBySuperclass(TypeInfo t, MethodInfo m) {
        TypeInfo owner = m.typeInfo();
        if (owner.equals(t) || owner.isInterface() || !CstQueries.superclasses(t).contains(owner) || !has(owner)) return false;
        for (TypeInfo c = t; !c.equals(owner); ) {
            ParameterizedType s = c.parentClass();
            TypeInfo st = s == null ? null : s.typeInfo();
            if (st == null || !has(st)) return false;
            List<TypeParameter> removedHere = c.equals(t) ? candidates(t) : removed(c);
            if (CstQueries.mentionsAny(s, removedHere) && !erasable(s, removedHere)) return false;
            c = st;
        }
        return declared(owner, removed(owner)).contains(m);
    }

    /**
     * {@code m} is a super-interface's, of an interface, declared by that super-interface's wildcard interface, which
     * t's extends: each super-interface between removes what t passes it of its removed positions, and has a wildcard
     * interface ({@link #supertypes}, {@link #removeThroughSuperinterfaces}).
     */
    private static boolean declaredBySuperinterface(TypeInfo t, MethodInfo m, List<TypeParameter> removed) {
        TypeInfo owner = m.typeInfo();
        if (!t.isInterface() || !owner.isInterface() || owner.equals(t)) return false;
        for (ParameterizedType s : t.interfacesImplemented()) {
            TypeInfo st = s.typeInfo();
            if (st == null || !CstQueries.mentionsAny(s, removed) || !has(st) || !erasable(s, removed)) continue;
            if (st.equals(owner) ? declared(owner, removed(owner)).contains(m)
                    : st.recursiveSuperTypeStream().anyMatch(owner::equals) && declaredBySuperinterface(st, m, removed(st))) {
                return true;
            }
        }
        return false;
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
            if (candidate(t, m, removed) && (m.isVoid() || erasable(m.returnType(), removed)
                                             || erasedDelegate(m.returnType(), removed))) {
                declared.add(m);
            }
        }
        declared.addAll(readFields(t, removed));
        declared.addAll(inheritedReached(t, removed));
        return declared;
    }

    /**
     * The methods of t's supertypes that the code reaches through t's wildcard and that t's wildcard interface
     * declares itself, as t sees them: a supertype that mentions a removed parameter, and so has no wildcard interface
     * t's extends, or one whose own wildcard interface lacks the method, {@code VariableMetaModel.name()} through a
     * {@code ShadowVariableMetaModel<?, ?, ?>}. Each parameter, as t sees it, mentions no removed parameter, the
     * result one the interface can erase.
     */
    private static List<MethodInfo> inheritedReached(TypeInfo t, List<TypeParameter> removed) {
        // not an interface's: the generic interface extends both, and a call through it is ambiguous (CS0121)
        if (t.isInterface()) return List.of();
        CSharpProgram program = program();
        List<MethodInfo> found = new ArrayList<>();
        for (Object info : program.reachedThroughWildcard(t)) {
            if (!(info instanceof MethodInfo m) || m.typeInfo().equals(t) || m.isStatic() || m.isConstructor()
                || !m.typeParameters().isEmpty() || !CSharpNames.translated(m.typeInfo())
                || m.access() != null && m.access().isPrivate()
                || "java.lang.Object".equals(m.typeInfo().fullyQualifiedName())
                || m.overrides().stream().anyMatch(o -> "java.lang.Object".equals(o.typeInfo().fullyQualifiedName()))) {
                continue;
            }
            // what an interface the wildcard interface extends declares already
            if (m.overrides().stream().filter(o -> o != m).map(MethodInfo::typeInfo)
                        .anyMatch(o -> inheritedClosed(t, o, removed)) || inheritedClosed(t, m.typeInfo(), removed)
                || declaredBySuperclass(t, m)) {
                continue;
            }
            Map<TypeParameter, ParameterizedType> seen = seenFrom(t, m.typeInfo());
            if (seen == null) continue;
            CSharpProperties.Property accessor = program.accessorOf(m);
            if (accessor != null && accessor.getter() != m) continue;
            boolean ok = true;
            for (ParameterInfo p : m.parameters()) {
                if (p.isVarArgs() || CstQueries.mentionsAny(CstSupertypes.substitute(p.parameterizedType(), seen), removed)) {
                    ok = false;
                }
            }
            ParameterizedType result = CstSupertypes.substitute(m.returnType(), seen);
            if (ok && (m.isVoid() || erasable(result, removed))) found.add(m);
        }
        found.sort(Comparator.comparing(MethodInfo::fullyQualifiedName));
        return found;
    }

    /** The type parameters of {@code owner} as {@code t} extends or implements it; null when it does not, or raw. */
    private static Map<TypeParameter, ParameterizedType> seenFrom(TypeInfo t, TypeInfo owner) {
        ParameterizedType seen = CstSupertypes.asSeen(t, owner);
        if (seen == null || seen.parameters().size() != owner.typeParameters().size()) {
            return owner.typeParameters().isEmpty() && seen != null ? Map.of() : null;
        }
        Map<TypeParameter, ParameterizedType> map = new HashMap<>();
        for (int i = 0; i < owner.typeParameters().size(); i++) map.put(owner.typeParameters().get(i), seen.parameters().get(i));
        return map;
    }

    private static CSharpProgram program() {
        return CSharpContext.program();
    }

    /**
     * A class's fields that the code reads through the wildcard, which the wildcard interface has as properties of
     * the field's name: {@code Type IMoveSelectorConfig.filterClass => filterClass;}. Not a record's, whose components
     * it has anyway.
     */
    private static List<FieldInfo> readFields(TypeInfo t, List<TypeParameter> removed) {
        if (t.typeNature().isRecord()) return List.of();
        Set<Object> reached = CSharpContext.program().reachedThroughWildcard(t);
        List<FieldInfo> fields = new ArrayList<>();
        for (FieldInfo f : t.fields()) {
            if (!f.isStatic() && !f.isSynthetic() && reached.contains(f) && erasable(f.type(), removed)) fields.add(f);
        }
        // a superclass's field read through the wildcard, that.parent in equals: accessible, as the type sees it
        for (TypeInfo s : CstQueries.superclasses(t)) {
            for (FieldInfo f : s.fields()) {
                if (!f.isStatic() && !f.isSynthetic() && reached.contains(f) && !f.access().isPrivate()
                    && erasable(inheritedType(t, f), removed)) {
                    fields.add(f);
                }
            }
        }
        return fields;
    }

    /** A superclass's field's type as {@code t} sees it, the superclass's type parameters its arguments. */
    private static ParameterizedType inheritedType(TypeInfo t, FieldInfo f) {
        TypeInfo owner = f.typeInfo();
        if (owner.equals(t)) return f.type();
        ParameterizedType seen = CstSupertypes.asSeen(t, owner);
        if (seen == null || seen.parameters().size() != owner.typeParameters().size()) return f.type();
        Map<TypeParameter, ParameterizedType> map = new HashMap<>();
        for (int i = 0; i < owner.typeParameters().size(); i++) map.put(owner.typeParameters().get(i), seen.parameters().get(i));
        return CstSupertypes.substitute(f.type(), map);
    }

    /** Whether {@link #erased} has a type for {@code pt}. */
    private static boolean erasable(ParameterizedType pt, List<TypeParameter> removed) {
        if (!CstQueries.mentionsAny(pt, removed)) return true;
        if (pt.arrays() > 0) return false;
        if (pt.isTypeParameter()) return true;
        TypeInfo t = pt.typeInfo();
        if (t == null) return false;
        if (!CSharpNames.translated(t)) return CSharpTypeName.dropsArguments(t) || covariantElement(pt, removed) != null;
        if (!has(t)) return false;
        List<TypeParameter> own = removed(t);
        for (int i = 0; i < t.typeParameters().size(); i++) {
            if (own.contains(t.typeParameters().get(i))) continue;
            if (i >= pt.parameters().size() || CstQueries.mentionsAny(pt.parameters().get(i), removed)) return false;
        }
        return true;
    }

    /**
     * A generic method C# prints erased ({@link CSharpProgram#erasedGeneric}) whose signature then mentions none of
     * its type parameters, {@code <Value_> Value_ getValue(Object entity)} as {@code object GetValue(object entity)}:
     * the wildcard interface declares it so. Its parameters mention none, its result is one or mentions none.
     */
    private static boolean plainErasure(MethodInfo m) {
        ParameterizedType r = m.returnType();
        return m.parameters().stream().noneMatch(p -> CstQueries.mentionsAny(p.parameterizedType(), m.typeParameters()))
               && (!CstQueries.mentionsAny(r, m.typeParameters())
                   || r.isTypeParameter() && r.arrays() == 0 && m.typeParameters().contains(r.typeParameter()));
    }

    /**
     * A parameter that is a removed type parameter itself, {@code isMovable(Solution_ workingSolution, Object entity)}:
     * the wildcard interface declares it as the bound ({@code object}), and the bridge casts it back. Java's code can
     * pass nothing else through the wildcard than {@code null} or a value of the capture, the receiver's own, for which
     * the cast holds.
     */
    private static boolean erasedParameter(ParameterInfo p, List<TypeParameter> removed) {
        ParameterizedType pt = p.parameterizedType();
        return pt.arrays() == 0 && pt.wildcard() == null && pt.isTypeParameter() && removed.contains(pt.typeParameter());
    }

    /**
     * A method the wildcard interface may declare: an instance method reachable outside the type, not an override of
     * the program's (the supertype's wildcard interface has it) or of Object's, not a generic method C# prints erased,
     * with no parameter of a removed type but one that is a removed type parameter ({@link #erasedParameter}).
     */
    private static boolean candidate(TypeInfo t, MethodInfo m, List<TypeParameter> removed) {
        CSharpProgram program = CSharpContext.program();
        if (CstQueries.isRecordAccessor(m)) return false;
        // an override of the program's: the supertype's wildcard interface has it, where t's extends it; else t's own
        if (m.isStatic() || m.isSynthetic() || m.isConstructor() || m.access() != null && m.access().isPrivate()
            // an override of the program's only where the code reaches it through the wildcard and nothing t's wildcard
            // interface extends declares it: else the supertype's wildcard interface has it, or nobody needs it
            || m.overrides().stream().anyMatch(o -> o != m && ("java.lang.Object".equals(o.typeInfo().fullyQualifiedName())
                                                              || CSharpNames.translated(o.typeInfo())
                                                                 && (inheritedClosed(t, o.typeInfo(), removed)
                                                                     || declaredBySuperclass(t, o)
                                                                     || !reachedOverride(t, m)
                                                                     // an interface's redeclaration is ambiguous with
                                                                     // the supertype's on the interface itself (CS0121)
                                                                     || t.isInterface())))
            || CSharpWildcards.mayCapture(m) || program.typeToken(m) >= 0
            // one C# prints erased only where the code calls it through a wildcard (a subtype's too)
            || program.erasedGeneric(m) && (!plainErasure(m) || !program.reachedThroughAnyWildcard(m))
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
        if (m.parameters().stream().anyMatch(p -> p.isVarArgs() || CstQueries.mentionsAny(p.parameterizedType(), removed)
                                                  && !erasedParameter(p, removed))) {
            return false;
        }
        // one with an erased parameter only where the code calls it through the wildcard: the cast is the bridge's
        if (m.parameters().stream().anyMatch(p -> erasedParameter(p, removed))
            && !program.reachedThroughWildcard(t).contains(m)) {
            return false;
        }
        return m.typeParameters().stream().noneMatch(tp -> tp.typeBounds().stream().anyMatch(b -> CstQueries.mentionsAny(b, removed)));
    }

    /** The code reaches {@code m}, or a method it overrides, through t's wildcard. */
    private static boolean reachedOverride(TypeInfo t, MethodInfo m) {
        Set<Object> reached = CSharpContext.program().reachedThroughWildcard(t);
        return reached.contains(m) || m.overrides().stream().anyMatch(reached::contains);
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
        @Override
        public String toString() {
            return "field " + field.name() + " written";
        }
    }

    /** A wildcard passed where its type is declared with arguments, which the wildcard interface cannot be. */
    private record PassedAsGeneric(ParameterizedType formal, MethodInfo callee, String caller) {
        @Override
        public String toString() {
            return "passed as " + formal + " to " + callee.fullyQualifiedName() + " in " + caller;
        }
    }

    private static void passed(MethodInfo method, List<Expression> arguments, String caller,
                               BiConsumer<Expression, Object> through) {
        List<ParameterInfo> parameters = method.parameters();
        for (int i = 0; i < arguments.size() && !parameters.isEmpty(); i++) {
            ParameterizedType formal = parameters.get(Math.min(i, parameters.size() - 1)).parameterizedType();
            ParameterizedType actual = arguments.get(i).parameterizedType();
            if (formal.arrays() != 0 || actual == null || formal.typeInfo() == null
                || !formal.typeInfo().equals(actual.typeInfo()) || formal.parameters().isEmpty()) {
                continue;
            }
            if (formal.parameters().stream().noneMatch(a -> a.wildcard() != null)) {
                through.accept(arguments.get(i), new PassedAsGeneric(formal, method, caller));
            }
        }
    }

    record Uses(Map<TypeInfo, Set<TypeParameter>> wildcarded, Map<TypeInfo, Set<Object>> reached) {
        static final Uses NONE = new Uses(Map.of(), Map.of());
    }

    static Uses wildcarded(List<TypeInfo> all, Predicate<MethodInfo> erased) {
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
                    // a var of a conditional whose common type is a wildcard use: declared as its wildcard interface
                    if (e instanceof LocalVariableCreation lvc && CSharpStatementPrinter.conditionalWildcard(lvc)) {
                        declared.accept(lvc.localVariable().parameterizedType());
                    }
                    if (e instanceof Cast cast) declared.accept(cast.parameterizedType());
                    if (e instanceof InstanceOf instanceOf && instanceOf.testType() != null) declared.accept(instanceOf.testType());
                    return true;
                });
            }
        }
        Map<TypeInfo, Set<TypeParameter>> result;
        Map<TypeInfo, Set<Object>> reached;
        do {
            result = new HashMap<>();
            for (var entry : wildcarded.entrySet()) result.put(entry.getKey(), Set.copyOf(entry.getValue()));
            // the members the code reaches through such a wildcard: a method called, a field read
            reached = new HashMap<>();
            for (TypeInfo owner : all) {
                for (MethodInfo method : owner.constructorsAndMethods()) {
                    if (method.methodBody() != null) reaches(owner, method, method.methodBody(), result, reached, erased);
                }
                for (FieldInfo f : owner.fields()) {
                    if (f.initializer() != null && !f.initializer().isEmpty()) reaches(owner, null, f.initializer(), result, reached, erased);
                }
            }
        } while (throughResults(reached, wildcarded));
        Map<TypeInfo, Set<Object>> reachedCopy = new HashMap<>();
        reached.forEach((t, members) -> reachedCopy.put(t, Set.copyOf(members)));
        return new Uses(Map.copyOf(result), Map.copyOf(reachedCopy));
    }

    /**
     * The members {@code code}, of {@code method} in {@code owner}, reaches through a wildcard. Not through a cast
     * or a local of the owner's own wildcard use in an instance method, which is the owner with its own type
     * parameters ({@link #ownUse}): {@code that.parent} in {@code equals}, after {@code (Node<?>) o}.
     */
    private static void reaches(TypeInfo owner, MethodInfo method, Element code, Map<TypeInfo, Set<TypeParameter>> wildcarded,
                                Map<TypeInfo, Set<Object>> reached, Predicate<MethodInfo> erased) {
        BiConsumer<Expression, Object> through = (scope, member) -> {
            if (scope == null) return;
            ParameterizedType pt = scope.parameterizedType();
            TypeInfo t = pt == null ? null : pt.typeInfo();
            Set<TypeParameter> tps = t == null ? null : wildcarded.get(t);
            if (tps == null || pt.arrays() > 0 || pt.parameters().size() != t.typeParameters().size()) return;
            // not a captured parameter: with a wildcard interface the parameter is that, not captured
            // (CSharpWildcards), and what the code reaches through it the interface must declare
            Expression inner = CSharpExpressionPrinter.unwrap(scope);
            if (method != null && !method.isStatic() && owner.equals(t)
                && (inner instanceof Cast || inner instanceof VariableExpression ve && ve.variable() instanceof LocalVariable)) {
                return;
            }
            for (int i = 0; i < pt.parameters().size(); i++) {
                if (tps.contains(t.typeParameters().get(i)) && open(pt.parameters().get(i))) {
                    reached.computeIfAbsent(t, k -> new HashSet<>()).add(member);
                    return;
                }
            }
        };
        String caller = owner.fullyQualifiedName() + (method == null ? "" : "." + method.name());
        code.visit((Element e) -> {
            if (e instanceof MethodCall mc && !mc.methodInfo().isStatic()) through.accept(mc.object(), mc.methodInfo());
            // a wildcard passed where the type is declared with arguments, getAttribute(Key<T> key): the
            // wildcard interface infers no T (CS0411)
            if (e instanceof MethodCall mc) {
                // isTerminated(b, score) of a Score<?>: the wildcard interface's Open_ (CSharpCallOpeners)
                int opened = CSharpCallOpeners.openedArgument(mc, erased);
                if (opened >= 0) {
                    Expression arg = mc.parameterExpressions().get(opened);
                    ParameterizedType pt = arg.parameterizedType();
                    // a raw Score, a leaked type parameter of an erased method, a wildcard: the wildcard interface
                    TypeInfo x = CSharpCallOpeners.openedType(pt, erased);
                    if (wildcarded.containsKey(x)) reached.computeIfAbsent(x, k -> new HashSet<>()).add(CSharpOpeners.OPENED);
                }
                passed(mc.methodInfo(), mc.parameterExpressions(), caller, through);
            }
            if (e instanceof ConstructorCall cc && cc.constructor() != null) {
                // new AndThen<>(delegate, f) of a Collector<A, ?, I>: the wildcard interface's Open_ (CSharpOpeners)
                int opened = CSharpOpeners.openedArgument(cc);
                if (opened >= 0) {
                    through.accept(cc.parameterExpressions().get(opened), CSharpOpeners.OPENED);
                } else {
                    passed(cc.constructor(), cc.parameterExpressions(), caller, through);
                }
            }
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

    /**
     * A removed type parameter in the result of a member the code reaches through the wildcard,
     * {@code PlanningEntityMetaModel<Solution_, Entity_> entity()} of a {@code VariableMetaModel<?, ?, ?>}: the
     * wildcard interface can declare the member only as the result's wildcard interface without those positions, so
     * they are removed there too; the reach is then found again, until nothing changes. Through a JDK collection's
     * element too ({@code List<VariableMetaModel<Solution_, Entity_, ?>>}). True when it removed one.
     */
    private static boolean throughResults(Map<TypeInfo, Set<Object>> reached,
                                          Map<TypeInfo, Set<TypeParameter>> wildcarded) {
        boolean changed = false;
        for (var entry : reached.entrySet()) {
            Set<TypeParameter> removed = wildcarded.get(entry.getKey());
            if (removed == null || removed.isEmpty()) continue;
            List<TypeParameter> list = List.copyOf(removed);
            for (Object member : entry.getValue()) {
                // the result as the reached type sees it: an inherited method's in the type's own parameters
                if (member instanceof MethodInfo m && !m.isStatic() && !m.isConstructor()) {
                    Map<TypeParameter, ParameterizedType> seen = m.typeInfo().equals(entry.getKey()) ? Map.of()
                            : seenFrom(entry.getKey(), m.typeInfo());
                    if (seen == null) continue;
                    changed |= removeThrough(CstSupertypes.substitute(m.returnType(), seen), list, wildcarded);
                    // an interface's member a super-interface declares: that one's wildcard interface declares it
                    if (entry.getKey().isInterface() && m.typeInfo().isInterface()) {
                        changed |= removeThroughSuperinterfaces(entry.getKey(), m.typeInfo(), list, wildcarded);
                    }
                }
            }
        }
        return changed;
    }

    /**
     * A super-interface's member reached through an interface's wildcard, {@code name()} of {@code VariableMetaModel}
     * through a {@code ShadowVariableMetaModel<S, E, ?>}: an interface's wildcard interface cannot redeclare it (the
     * generic interface would extend both, CS0121), so each super-interface on the way to the declaring one removes
     * the positions t removes, and t's wildcard interface extends theirs ({@link #declaredBySuperinterface}).
     */
    private static boolean removeThroughSuperinterfaces(TypeInfo t, TypeInfo owner, List<TypeParameter> removed,
                                                        Map<TypeInfo, Set<TypeParameter>> wildcarded) {
        if (t.equals(owner)) return false;
        boolean changed = false;
        for (ParameterizedType s : t.interfacesImplemented()) {
            TypeInfo st = s.typeInfo();
            if (st == null || !CSharpNames.translated(st)
                || !st.equals(owner) && st.recursiveSuperTypeStream().noneMatch(owner::equals)) {
                continue;
            }
            changed |= removeThrough(s, removed, wildcarded);
            Set<TypeParameter> next = wildcarded.get(st);
            if (next != null && !next.isEmpty()) {
                changed |= removeThroughSuperinterfaces(st, owner, List.copyOf(next), wildcarded);
            }
        }
        return changed;
    }

    private static boolean removeThrough(ParameterizedType pt, List<TypeParameter> removed,
                                         Map<TypeInfo, Set<TypeParameter>> wildcarded) {
        if (pt == null || pt.arrays() != 0 || pt.typeInfo() == null) return false;
        TypeInfo b = pt.typeInfo();
        boolean changed = false;
        if (!CSharpNames.translated(b)) {
            if (COVARIANT_COLLECTIONS.contains(b.fullyQualifiedName()) && pt.parameters().size() == 1) {
                ParameterizedType e = pt.parameters().getFirst();
                changed = removeThrough(e.wildcard() != null && e.wildcard().isExtends() ? e.withWildcard(null) : e,
                        removed, wildcarded);
            }
            return changed;
        }
        // a functional interface's result is adapted as a delegate's (erasedDelegate), not its wildcard interface
        if (pt.parameters().size() != b.typeParameters().size()
            || b.isInterface() && CstSupertypes.singleAbstractMethod(b) != null) {
            return false;
        }
        for (int i = 0; i < pt.parameters().size(); i++) {
            ParameterizedType a = pt.parameters().get(i);
            if (a.wildcard() == null && CstQueries.mentionsAny(a, removed)) {
                changed |= wildcarded.computeIfAbsent(b, k -> new HashSet<>()).add(b.typeParameters().get(i));
            }
        }
        return changed;
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
        // List<Descriptor<S>> of a removed S: a read-only collection of the element's wildcard interface
        ParameterizedType element = covariantElement(pt, removed);
        if (element != null) {
            String e = erased(element, removed, q);
            if (e == null) return null;
            CSharpContext.using("System.Collections.Generic");
            return (LIST.equals(t.fullyQualifiedName()) ? "IReadOnlyList<" : "IEnumerable<") + e + ">";
        }
        // List<G> of a removed G: C#'s non-generic IList, which every List<T> is (the bridge casts, nonGenericBridge)
        if (nonGenericErasure(pt, removed)) return nonGenericName(t);
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

    private static final String LIST = "java.util.List";

    /** The JDK's single-element collections whose C# counterpart is an {@code IEnumerable<T>}, a List an {@code IReadOnlyList<T>}. */
    private static final Set<String> COVARIANT_COLLECTIONS = Set.of(LIST, "java.util.Collection",
            "java.util.SequencedCollection", "java.util.Set", "java.lang.Iterable");

    /**
     * The element of a JDK collection, {@code List<Descriptor<S>>} or {@code List<? extends Descriptor<S>>}, that is a
     * program type mentioning a removed type parameter and with a wildcard interface the erasure can name: C#'s
     * covariant {@code IReadOnlyList<IDescriptor>} (for a List; {@code IEnumerable<IDescriptor>} for any other) is
     * what the member's value converts to, without a cast. Not a removed type parameter itself, which covariance does
     * not convert. Null otherwise.
     */
    private static ParameterizedType covariantElement(ParameterizedType pt, List<TypeParameter> removed) {
        TypeInfo t = pt.typeInfo();
        if (t == null || pt.arrays() != 0 || pt.parameters().size() != 1 || CSharpNames.translated(t)
            || !COVARIANT_COLLECTIONS.contains(t.fullyQualifiedName())) {
            return null;
        }
        ParameterizedType a = pt.parameters().getFirst();
        if (a.wildcard() != null) {
            if (!a.wildcard().isExtends()) return null;
            a = a.withWildcard(null);
        }
        if (a.arrays() != 0 || a.isTypeParameter() || a.typeInfo() == null || !CSharpNames.translated(a.typeInfo())
            || !CstQueries.mentionsAny(a, removed) || !erasable(a, removed)) {
            return null;
        }
        return a;
    }

    /** {@code List<G>}, {@code Map<K, G>} of a removed {@code G}: a JDK collection C# has a non-generic interface of. */
    static boolean nonGenericErasure(ParameterizedType pt, List<TypeParameter> removed) {
        TypeInfo t = pt.typeInfo();
        return t != null && pt.arrays() == 0 && covariantElement(pt, removed) == null && !CSharpNames.translated(t) && CSharpTypeName.nonGeneric(t) != null
               && !"java.lang.Comparable".equals(t.fullyQualifiedName()) && CstQueries.mentionsAny(pt, removed);
    }

    /**
     * The non-generic interface: a set or a collection, which may be a HashSet, is only an {@code IEnumerable} (a
     * {@code HashSet<T>} is no {@code ICollection}, the cast would fail when it runs).
     */
    private static String nonGenericName(TypeInfo t) {
        String nonGeneric = CSharpTypeName.nonGeneric(t);
        return "ICollection".equals(nonGeneric) ? "IEnumerable" : nonGeneric;
    }

    /** The bridge's value of such a member: cast, an {@code IList<G>} is no {@code IList}. */
    private static String nonGenericBridge(ParameterizedType declared, List<TypeParameter> removed, String type,
                                           String value) {
        return nonGenericErasure(declared, removed) ? "(" + type + ") " + value : value;
    }

    /**
     * A delegate whose parameters and result are each a removed type parameter or mention none:
     * {@code Supplier<ResultContainer_>}, {@code BiFunction<ResultContainer_, A, Runnable>}. The wildcard interface
     * has it with the removed type parameter erased, {@code Func<object, A, Action>}, which the bridge adapts to by a
     * lambda: a parameter cast to the type parameter, the result converted from it.
     */
    static boolean erasedDelegate(ParameterizedType pt, List<TypeParameter> removed) {
        TypeInfo t = pt.typeInfo();
        if (t == null || pt.arrays() != 0 || !CstQueries.mentionsAny(pt, removed)) return false;
        LibraryMapping.TypeMapping mapping = CSharpNames.translated(t) ? null : CSharpContext.libraries().type(t);
        if (!(CSharpNames.translated(t) ? CSharpContext.program().delegate(t) || adapted(t)
                : mapping != null && mapping.delegate())) {
            return false;
        }
        // a program delegate's bounds, where Score_ : IScore<Score_>, no erased argument meets
        if (CSharpNames.translated(t) && t.typeParameters().stream().anyMatch(tp -> tp.typeBounds().stream()
                .anyMatch(bound -> !bound.isJavaLangObject()))) {
            return false;
        }
        MethodInfo sam = CstSupertypes.singleAbstractMethod(t);
        if (sam == null) return false;
        for (ParameterizedType slot : samTypes(pt, sam)) {
            if (slot == null) return false;
            if (CstQueries.mentionsAny(slot, removed)
                && !(slot.isTypeParameter() && slot.arrays() == 0 && removed.contains(slot.typeParameter()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * A functional interface of the program that C# has with its lambda adapter, {@code ITriFunction<A, B, C, R>}, of its
     * own method: the bridge's lambda goes in as its {@code Lambda}.
     */
    private static boolean adapted(TypeInfo t) {
        if (!CSharpNames.lambdaAdapter(t)) return false;
        MethodInfo sam = CstSupertypes.singleAbstractMethod(t);
        return sam != null && sam.typeInfo().equals(t);
    }

    /** The single abstract method's parameter types, then its result's (null when void), as {@code pt} has them. */
    private static List<ParameterizedType> samTypes(ParameterizedType pt, MethodInfo sam) {
        Map<NamedType, ParameterizedType> map = pt.initialTypeParameterMap();
        List<ParameterizedType> types = new ArrayList<>();
        for (var p : sam.parameters()) types.add(substituted(p.parameterizedType(), map));
        if (!sam.isVoid()) types.add(substituted(sam.returnType(), map));
        return types;
    }

    private static ParameterizedType substituted(ParameterizedType formal,
                                                 Map<NamedType, ParameterizedType> map) {
        if (formal.isTypeParameter()) {
            ParameterizedType concrete = map.get(formal.typeParameter());
            return concrete == null ? formal : concrete;
        }
        return formal;
    }

    /** The delegate as the wildcard interface has it: each removed type parameter its bound's erasure. */
    static String erasedDelegateType(ParameterizedType pt, List<TypeParameter> removed, Qualification q) {
        String printed = CSharpTypeName.of(pt, q);
        for (TypeParameter tp : removed) {
            printed = printed.replaceAll("\\b" + Pattern.quote(CSharpNames.typeParameter(tp)) + "\\b",
                    Matcher.quoteReplacement(erasedBound(tp, removed, new HashSet<>(), q)));
        }
        return printed;
    }

    /**
     * The bridge's body for a member whose result is an erased delegate: the type's own delegate, once, adapted by a
     * lambda, {@code { var f = Accumulator(); return (x0_, x1_) => f((ResultContainer_) x0_, x1_); }}.
     */
    static String delegateBridge(MethodInfo m, String call, List<TypeParameter> removed, Qualification q) {
        return delegateBridge(m, call, removed, q, false);
    }

    /** As above; {@code nullableResult}: the own member's result may be null (its {@code ?}), and the bridge passes the null on (CS8602). */
    static String delegateBridge(MethodInfo m, String call, List<TypeParameter> removed, Qualification q, boolean nullableResult) {
        ParameterizedType pt = m.returnType();
        MethodInfo sam = CstSupertypes.singleAbstractMethod(pt.typeInfo());
        List<ParameterizedType> slots = samTypes(pt, sam);
        int n = sam.parameters().size();
        List<String> names = new ArrayList<>();
        List<String> arguments = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String name = "x" + i + "_";
            names.add(name);
            ParameterizedType slot = slots.get(i);
            arguments.add(slot.isTypeParameter() && removed.contains(slot.typeParameter())
                    ? "(" + CSharpNames.typeParameter(slot.typeParameter()) + ") " + name
                      + (CSharpContext.program().facts().nullabilityAnalysed() ? "!" : "") : name);
        }
        ParameterizedType result = sam.isVoid() ? null : slots.get(n);
        // and a lambda adapter's method of a type parameter, R? Apply(…) as the analysis has it (CS8603)
        boolean assertResult = result != null && CSharpContext.program().facts().nullabilityAnalysed()
                               && (result.isTypeParameter() && removed.contains(result.typeParameter())
                                   || adapted(pt.typeInfo()) && sam.returnType().isTypeParameter());
        String parameters = n == 1 ? names.getFirst() : "(" + String.join(", ", names) + ")";
        String lambda = parameters + " => f" + (adapted(pt.typeInfo()) ? "." + CSharpNames.method(sam) : "") + "("
                        + String.join(", ", arguments) + ")" + (assertResult ? "!" : "");
        // ITriFunction<object, A, B, Action>'s lambda adapter
        String adapter = adapted(pt.typeInfo())
                ? "new " + CSharpNames.lambdaAdapterReference(pt.typeInfo(), erasedDelegateType(pt, removed, q)) + "(" + lambda + ")"
                : lambda;
        return "{ var f = " + call + "; return " + (nullableResult ? "f == null ? null : " : "") + adapter + "; }";
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
                        type + " " + self + "." + property + " => "
                        + nonGenericBridge(f.type(), removed, type, property + (assertNotNull ? "!" : "")) + ";"));
            }
        }
        for (FieldInfo f : readFields(t, removed)) {
            NullableState nullable = program.facts().nullable(f);
            ParameterizedType fieldType = inheritedType(t, f);
            String type = CstQueries.mentionsAny(fieldType, removed) ? erased(fieldType, removed, q)
                    : CSharpTypeName.declared(fieldType, false, nullable, q);
            if (type == null) continue;
            String property = CSharpNames.field(f);
            if (!signatures.add(property)) continue;
            String modifier = "public".equals(CSharpModifiers.access(f)) ? "" : "internal ";
            boolean assertNotNull = program.facts().nullabilityAnalysed() && CstQueries.mentionsAny(fieldType, removed)
                                    && !type.endsWith("?");
            members.add(new Member(modifier + type + " " + property + " { get; }",
                    type + " " + self + "." + property + " => "
                    + nonGenericBridge(fieldType, removed, type, property + (assertNotNull ? "!" : "")) + ";"));
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
                String bridge = CstQueries.mentionsAny(m.returnType(), removed) && erasedDelegate(m.returnType(), removed)
                        // Func<A, B, bool> is no Func<object, object, bool>: the delegate adapted, as a method's is
                        ? returnType + " " + self + "." + property + " { get " + delegateBridge(m, property, removed, q, returnType.endsWith("?")) + " }"
                        : returnType + " " + self + "." + property + " => "
                          + nonGenericBridge(m.returnType(), removed, returnType, property + asserted(m, removed, returnType)) + ";";
                members.add(new Member(modifier + returnType + " " + property + " { get; }", bridge));
                continue;
            }
            String name = CSharpNames.method(m);
            boolean erasedGeneric = program.erasedGeneric(m);
            String typeParameters = erasedGeneric ? "" : CSharpNames.typeParameters(m.typeParameters());
            List<String> parameters = new ArrayList<>();
            List<String> arguments = new ArrayList<>();
            for (var p : m.parameters()) {
                String pn = CSharpNames.name(p.name());
                NullableState nullable = program.facts().nullable(p);
                if (erasedParameter(p, removed)) {
                    // the bound, cast back to the type parameter by the bridge: T? where null may come
                    String bound = CSharpTypeName.rawArgument(p.parameterizedType().typeParameter(), q);
                    boolean maybeNull = nullable == NullableState.NULLABLE;
                    parameters.add(bound + (maybeNull && !bound.endsWith("?") ? "?" : "") + " " + pn);
                    arguments.add("(" + CSharpNames.typeParameter(p.parameterizedType().typeParameter())
                                  + (maybeNull ? "?" : "") + ") " + pn);
                    continue;
                }
                // with the nullability the type's own member has, which the bridge passes on (CS8604), read-only as
                // the member's own (CSharpReadOnly)
                parameters.add(CSharpTypeName.declared(p.parameterizedType(), program.readOnly(p), nullable, q) + " " + pn);
                arguments.add(pn);
            }
            String signature = name + typeParameters + "(" + String.join(", ", parameters) + ")";
            if (!signatures.add(name + "/" + parameters.stream().map(s -> s.substring(0, s.lastIndexOf(' ')))
                    .collect(Collectors.joining(",")))) {
                continue;
            }
            List<String> constraints = erasedGeneric ? List.of() : CSharpTypeName.constraints(m.typeParameters(), q);
            String declaration = modifier + returnType + " " + signature
                                 + (constraints.isEmpty() ? "" : " " + String.join(" ", constraints)) + ";";
            String bridge;
            if (CstQueries.mentionsAny(m.returnType(), removed) && erasedDelegate(m.returnType(), removed)) {
                String own = name + typeParameters + "(" + String.join(", ", arguments) + ")";
                bridge = returnType + " " + self + "." + signature + " " + delegateBridge(m, own, removed, q, returnType.endsWith("?"));
            } else {
                String call = name + typeParameters + "(" + String.join(", ", arguments) + ")" + asserted(m, removed, returnType);
                bridge = returnType + " " + self + "." + signature + " => "
                         + nonGenericBridge(m.returnType(), removed, returnType, call) + ";";
            }
            members.add(new Member(declaration, bridge));
        }
        for (MethodInfo m : inheritedReached(t, removed)) {
            Member member = inheritedMember(t, m, removed, self, signatures, q);
            if (member != null) members.add(member);
        }
        if (CSharpOpeners.opened(t)) members.add(CSharpOpeners.member(t, self, q));
        return members;
    }

    /**
     * A supertype's method the wildcard interface declares itself ({@link #inheritedReached}), as t sees it; the
     * bridge calls t's own, through the interface for a default method a class does not override (C# does not
     * inherit it).
     */
    private static Member inheritedMember(TypeInfo t, MethodInfo m, List<TypeParameter> removed, String self,
                                          Set<String> signatures, Qualification q) {
        CSharpProgram program = program();
        Map<TypeParameter, ParameterizedType> seen = seenFrom(t, m.typeInfo());
        ParameterizedType result = CstSupertypes.substitute(m.returnType(), seen);
        NullableState nullable = program.facts().nullable(m);
        String returnType;
        if (m.isVoid()) {
            returnType = "void";
        } else if (!CstQueries.mentionsAny(result, removed)) {
            returnType = CSharpTypeName.declared(result, false, nullable, q);
        } else {
            returnType = erased(result, removed, q);
            if (returnType == null) return null;
            if (nullable == NullableState.NULLABLE && !returnType.endsWith("?")) returnType += "?";
        }
        String modifier = t.isInterface() || "public".equals(CSharpModifiers.access(m, m.typeInfo())) ? "" : "internal ";
        String receiver = "";
        if (!t.isInterface() && m.typeInfo().isInterface() && !m.isAbstract()
            && CstQueries.classChain(t).stream().flatMap(c -> c.methods().stream()).noneMatch(x -> x.overrides().contains(m))) {
            receiver = "((" + CSharpTypeName.of(CstSupertypes.asSeen(t, m.typeInfo()), q) + ") this).";
        }
        String property = program.propertyRead(m);
        if (property != null) {
            if (!m.parameters().isEmpty() || !signatures.add(property)) return null;
            String bridge = returnType + " " + self + "." + property + " => " + receiver + property + ";";
            return new Member(modifier + returnType + " " + property + " { get; }", bridge);
        }
        String name = CSharpNames.method(m);
        List<String> parameters = new ArrayList<>();
        List<String> arguments = new ArrayList<>();
        for (ParameterInfo p : m.parameters()) {
            String pn = CSharpNames.name(p.name());
            parameters.add(CSharpTypeName.declared(CstSupertypes.substitute(p.parameterizedType(), seen),
                    program.readOnly(p), program.facts().nullable(p), q) + " " + pn);
            arguments.add(pn);
        }
        String signature = name + "(" + String.join(", ", parameters) + ")";
        if (!signatures.add(name + "/" + parameters.stream().map(s -> s.substring(0, s.lastIndexOf(' ')))
                .collect(Collectors.joining(",")))) {
            return null;
        }
        String call = receiver + name + "(" + String.join(", ", arguments) + ")";
        String bridge = returnType + " " + self + "." + signature + " => " + call + ";";
        return new Member(modifier + returnType + " " + signature + ";", bridge);
    }

    /**
     * A member's result in the wildcard interface: the type's own, with its nullability ({@code string?}), which the
     * bridge returns (CS8603, CS8766); a removed type parameter erased, {@code IScore?} when the member may return
     * null.
     */
    private static String result(MethodInfo m, List<TypeParameter> removed, Qualification q) {
        NullableState nullable = CSharpContext.program().facts().nullable(m);
        if (CSharpContext.program().erasedGeneric(m) && m.returnType().isTypeParameter()
            && m.typeParameters().contains(m.returnType().typeParameter())) {
            // as the method prints it, erased: its bound (CSharpMethodPrinter)
            String bound = CSharpTypeName.rawArgument(m.returnType().typeParameter(), q);
            return nullable == NullableState.NULLABLE && !bound.endsWith("?") ? bound + "?" : bound;
        }
        if (!CstQueries.mentionsAny(m.returnType(), removed)) {
            return CSharpTypeName.declared(m.returnType(), CSharpContext.program().readOnly(m), nullable, q);
        }
        String erased = erasedDelegate(m.returnType(), removed) ? erasedDelegateType(m.returnType(), removed, q)
                : erased(m.returnType(), removed, q);
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

    /** As above, with the static members of an interface's companion, which has the wildcard interface's name. */
    static OutputBuilder declaration(TypeInfo t, String access, Qualification q, List<OutputBuilder> statics) {
        List<TypeParameter> remaining = remaining(t);
        List<TypeParameter> removed = removed(t);
        // the type's own type parameters, with the variance the type declares on them
        String typeParameters = CSharpNames.declaredTypeParameters(remaining);
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


    static boolean containsWildcard(ParameterizedType t) {
        if (t == null) return false;
        if (t.wildcard() != null) return true;
        return t.parameters().stream().anyMatch(CSharpWildcardInterfaces::containsWildcard);
    }
}
