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

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Java's wildcard of a self-bounded type, {@code Score<?>} where {@code Score<Score_ extends Score<Score_>>}, has no
 * C# type argument: none satisfies {@code where Score_ : IScore<Score_>}. As C#'s own {@code IComparable} beside
 * {@code IComparable<T>}, such a type has a companion without its self-bounded type parameters, which the wildcard is:
 * {@code IScore} beside {@code IScore<Score_>}, {@code IInnerScoreDirector<Solution_>} beside
 * {@code IInnerScoreDirector<Solution_, Score_>}; a class's or a record's is the interface {@code I} + its name, which it
 * implements, a record's with its components as properties.
 * <p>
 * The companion declares the type's own members that a wildcard's user can call: those whose parameters do not
 * mention a removed type parameter, with the removed parameter in the result erased to its bound's companion
 * ({@code Score_ Negate()} is {@code IScore Negate()}). The type implements each explicitly by calling its own,
 * {@code IScore IScore.Negate() => Negate();}, a default implementation in an interface, so that every implementation
 * of the type is one of the companion. The companion extends the companions of the type's supertypes.
 */
final class CSharpCompanions {

    private CSharpCompanions() {
    }

    /** A member of the companion: its C# declaration and its bridge, the type's explicit implementation. */
    record Member(String declaration, String bridge) {
    }

    /** The self-bounded type parameters of a program type, which its companion does not have; empty when it has none. */
    static List<TypeParameter> removed(TypeInfo t) {
        if (t == null || !CSharpNames.translated(t) || t.typeParameters().isEmpty() || t.typeNature().isEnum()
            || t.isAnonymous() || t.enclosingMethod() != null) {
            return List.of();
        }
        return CSharpContext.program().companions().computeIfAbsent(t, CSharpCompanions::computeRemoved);
    }

    private static List<TypeParameter> computeRemoved(TypeInfo t) {
        List<TypeParameter> removed = new ArrayList<>();
        for (TypeParameter tp : t.typeParameters()) {
            if (tp.typeBounds().stream().anyMatch(b -> mentionsAny(b, t.typeParameters()))) removed.add(tp);
        }
        return List.copyOf(removed);
    }

    static boolean has(TypeInfo t) {
        return !removed(t).isEmpty();
    }

    /** The companion's name: an interface's own, a class's {@code I} + its own. */
    static String name(TypeInfo t) {
        String own = CSharpNames.type(t);
        return t.isInterface() ? own : "I" + own;
    }

    /** The companion's type parameters, the type's that remain. */
    static List<TypeParameter> remaining(TypeInfo t) {
        List<TypeParameter> removed = removed(t);
        return t.typeParameters().stream().filter(tp -> !removed.contains(tp)).toList();
    }

    /** The companion as the type itself names it: {@code IInnerScoreDirector<Solution_>}. */
    static String selfReference(TypeInfo t) {
        List<TypeParameter> remaining = remaining(t);
        return name(t) + (remaining.isEmpty() ? "" : remaining.stream().map(CSharpNames::typeParameter)
                .collect(Collectors.joining(", ", "<", ">")));
    }

    /**
     * Whether {@code pt} is a wildcard or raw use of a type with a companion, where no C# type argument satisfies the
     * self-bound: {@code Score<?>}, {@code Score<? extends Score<?>>}, a raw {@code Score}.
     */
    static boolean isWildcardUse(ParameterizedType pt) {
        TypeInfo t = pt.typeInfo();
        if (t == null) return false;
        List<TypeParameter> removed = removed(t);
        if (removed.isEmpty()) return false;
        List<ParameterizedType> arguments = pt.parameters();
        if (arguments.isEmpty()) return true;
        for (int i = 0; i < arguments.size() && i < t.typeParameters().size(); i++) {
            if (!removed.contains(t.typeParameters().get(i))) continue;
            ParameterizedType a = arguments.get(i);
            if (a.wildcard() != null && (a.wildcard().isUnbound() || a.wildcard().isSuper()
                                         || containsWildcard(a.withWildcard(null)))) {
                return true;
            }
        }
        return false;
    }

    /** {@code pt}, a wildcard use, as its companion: the remaining arguments printed as they are. */
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
                printed.add(unbound ? CSharpTypeName.rawArgumentOf(tp, q) : CSharpTypeName.argument(a, q));
            } else {
                printed.add(CSharpTypeName.rawArgumentOf(tp, q));
            }
        }
        String name = CSharpTypeName.name(t, q);
        String companion = t.isInterface() ? name : qualifiedCompanion(name);
        return companion + (printed.isEmpty() ? "" : "<" + String.join(", ", printed) + ">");
    }

    /** {@code Ns.Outer.Config} is {@code Ns.Outer.IConfig}. */
    private static String qualifiedCompanion(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "I" + name : name.substring(0, dot + 1) + "I" + name.substring(dot + 1);
    }

    /**
     * A type as the companion of {@code t} has it: a removed type parameter is its bound's companion, a type with a
     * companion whose removed positions mention one is that companion. Null when the type mentions a removed
     * parameter otherwise ({@code List<Score_>}), which no conversion makes the companion's.
     */
    static String erased(ParameterizedType pt, List<TypeParameter> removed, Qualification q) {
        if (!mentionsAny(pt, removed)) return CSharpTypeName.of(pt, q);
        if (pt.arrays() > 0) return null;
        if (pt.isTypeParameter()) return erasedBound(pt.typeParameter(), removed, new HashSet<>(), q);
        TypeInfo t = pt.typeInfo();
        if (t == null || !has(t)) return null;
        List<TypeParameter> own = removed(t);
        List<String> printed = new ArrayList<>();
        for (int i = 0; i < t.typeParameters().size(); i++) {
            if (own.contains(t.typeParameters().get(i))) continue;
            if (i >= pt.parameters().size()) return null;
            ParameterizedType a = pt.parameters().get(i);
            if (mentionsAny(a, removed)) return null;
            printed.add(CSharpTypeName.argument(a, q));
        }
        String name = CSharpTypeName.name(t, q);
        String companion = t.isInterface() ? name : qualifiedCompanion(name);
        return companion + (printed.isEmpty() ? "" : "<" + String.join(", ", printed) + ">");
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

    /** The companion's supertypes: the companions of the type's self-bounded supertypes, and its plain interfaces. */
    static List<String> supertypes(TypeInfo t, Qualification q) {
        List<TypeParameter> removed = removed(t);
        Set<String> supers = new LinkedHashSet<>();
        List<ParameterizedType> all = new ArrayList<>(t.interfacesImplemented());
        if (t.parentClass() != null && !t.parentClass().isJavaLangObject()) all.add(t.parentClass());
        for (ParameterizedType s : all) {
            TypeInfo st = s.typeInfo();
            if (st == null) continue;
            if (!mentionsAny(s, removed)) {
                // a closed supertype: an interface is itself, and its own companion with it; a class only its companion
                if (st.isInterface()) supers.add(CSharpTypeName.of(s, q));
                else if (has(st)) supers.add(companionOfClosed(s, q));
            } else if (has(st)) {
                String erased = erased(s, removed, q);
                if (erased != null) supers.add(erased);
            }
        }
        return List.copyOf(supers);
    }

    private static String companionOfClosed(ParameterizedType s, Qualification q) {
        TypeInfo t = s.typeInfo();
        List<TypeParameter> own = removed(t);
        List<String> printed = new ArrayList<>();
        for (int i = 0; i < t.typeParameters().size() && i < s.parameters().size(); i++) {
            if (!own.contains(t.typeParameters().get(i))) printed.add(CSharpTypeName.argument(s.parameters().get(i), q));
        }
        String name = CSharpTypeName.name(t, q);
        String companion = t.isInterface() ? name : qualifiedCompanion(name);
        return companion + (printed.isEmpty() ? "" : "<" + String.join(", ", printed) + ">");
    }

    /** The companion's members, with their bridges. */
    static List<Member> members(TypeInfo t, Qualification q) {
        List<TypeParameter> removed = removed(t);
        String self = selfReference(t);
        CSharpProgram program = CSharpContext.program();
        List<Member> members = new ArrayList<>();
        Set<String> signatures = new HashSet<>();
        if (t.typeNature().isRecord()) {
            // a record's components, its properties
            for (io.codelaser.maddi.cst.api.info.FieldInfo f : t.fields()) {
                if (f.isStatic()) continue;
                io.codelaser.maddi.cst.api.type.NullableState nullable = program.facts().nullable(f);
                String type = mentionsAny(f.type(), removed) ? erased(f.type(), removed, q)
                        : CSharpTypeName.declared(f.type(), false, nullable, q);
                if (type == null) continue;
                String property = CSharpNames.field(f);
                signatures.add(property);
                boolean assertNotNull = program.facts().nullabilityAnalysed() && mentionsAny(f.type(), removed)
                                        && !type.endsWith("?");
                members.add(new Member(type + " " + property + " { get; }",
                        type + " " + self + "." + property + " => " + property + (assertNotNull ? "!" : "") + ";"));
            }
        }
        for (MethodInfo m : t.methods()) {
            if (CSharpTypePrinter.isRecordAccessor(m)) continue;
            if (m.isStatic() || m.isSynthetic() || m.isConstructor() || m.access() != null && m.access().isPrivate()
                || m.overrides().stream().anyMatch(o -> o != m) || CSharpWildcards.mayCapture(m)
                || program.typeToken(m) >= 0 || CSharpTypePrinter.interfaceMethodDefaulted(t, m) != null) {
                continue;
            }
            if (!t.isInterface() && !"public".equals(CSharpModifiers.access(m, t))) continue;
            CSharpProperties.Property accessor = program.accessorOf(m);
            if (accessor != null && accessor.getter() != m) continue;
            if (m.parameters().stream().anyMatch(p -> mentionsAny(p.parameterizedType(), removed) || p.isVarArgs())) {
                continue;
            }
            if (m.typeParameters().stream().anyMatch(tp -> tp.typeBounds().stream().anyMatch(b -> mentionsAny(b, removed)))) {
                continue;
            }
            String returnType = m.isVoid() ? "void" : result(m, removed, q);
            if (returnType == null) continue;
            String property = program.propertyRead(m);
            if (property != null) {
                if (!m.parameters().isEmpty() || !signatures.add(property)) continue;
                members.add(new Member(returnType + " " + property + " { get; }",
                        returnType + " " + self + "." + property + " => " + property + asserted(m, removed, returnType)
                        + ";"));
                continue;
            }
            String name = CSharpNames.method(m);
            String typeParameters = m.typeParameters().isEmpty() ? "" : m.typeParameters().stream()
                    .map(CSharpNames::typeParameter).collect(Collectors.joining(", ", "<", ">"));
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
            String declaration = returnType + " " + signature
                                 + (constraints.isEmpty() ? "" : " " + String.join(" ", constraints)) + ";";
            String call = name + typeParameters + "(" + String.join(", ", arguments) + ")" + asserted(m, removed, returnType);
            String bridge = returnType + " " + self + "." + signature + " => " + call + ";";
            members.add(new Member(declaration, bridge));
        }
        return members;
    }

    /**
     * A member's result in the companion: the type's own, with its nullability ({@code string?}), which the bridge
     * returns (CS8603, CS8766); a removed type parameter erased, {@code IScore?} when the member may return null.
     */
    private static String result(MethodInfo m, List<TypeParameter> removed, Qualification q) {
        io.codelaser.maddi.cst.api.type.NullableState nullable = CSharpContext.program().facts().nullable(m);
        if (!mentionsAny(m.returnType(), removed)) return CSharpTypeName.declared(m.returnType(), false, nullable, q);
        String erased = erased(m.returnType(), removed, q);
        if (erased == null) return null;
        boolean nullableResult = nullable == io.codelaser.maddi.cst.api.type.NullableState.NULLABLE
                                 && !erased.endsWith("?");
        return nullableResult ? erased + "?" : erased;
    }

    /**
     * {@code !} on a bridge's call whose result, a removed type parameter, the analysis finds not null: C# sees a type
     * parameter converted to the companion as possibly null (CS8603), the bridge returns what the member does.
     */
    private static String asserted(MethodInfo m, List<TypeParameter> removed, String returnType) {
        return CSharpContext.program().facts().nullabilityAnalysed() && mentionsAny(m.returnType(), removed)
               && !returnType.endsWith("?") ? "!" : "";
    }

    /** {@code interface IScore : … { members }}, after the type's own declaration. */
    static OutputBuilder declaration(TypeInfo t, String access, Qualification q) {
        List<TypeParameter> remaining = remaining(t);
        List<TypeParameter> removed = removed(t);
        String typeParameters = remaining.isEmpty() ? "" : remaining.stream().map(CSharpNames::typeParameter)
                .collect(Collectors.joining(", ", "<", ">"));
        StringBuilder header = new StringBuilder();
        if (access != null) header.append(access).append(' ');
        header.append("interface ").append(name(t)).append(typeParameters);
        List<String> supers = supertypes(t, q);
        if (!supers.isEmpty()) header.append(" : ").append(String.join(", ", supers));
        for (String constraint : CSharpTypeName.constraints(remaining.stream()
                .filter(tp -> tp.typeBounds().stream().noneMatch(b -> mentionsAny(b, removed))).toList(), q)) {
            header.append(' ').append(constraint);
        }
        List<OutputBuilder> body = members(t, q).stream()
                .map(m -> (OutputBuilder) new OutputBuilderImpl().add(new TextImpl(m.declaration()))).toList();
        return new OutputBuilderImpl().add(new TextImpl(header.toString())).add(io.codelaser.maddi.cst.impl.output.SpaceEnum.ONE)
                .add(CSharpStatementPrinter.braces(body));
    }

    /** The type's explicit implementations of its companion's members. */
    static List<OutputBuilder> bridges(TypeInfo t, Qualification q) {
        return members(t, q).stream()
                .map(m -> (OutputBuilder) new OutputBuilderImpl().add(new TextImpl(m.bridge()))).toList();
    }

    static boolean mentionsAny(ParameterizedType t, List<TypeParameter> tps) {
        return mentionsAny(t, tps, new HashSet<>());
    }

    private static boolean mentionsAny(ParameterizedType t, List<TypeParameter> tps, Set<ParameterizedType> seen) {
        if (t == null || !seen.add(t)) return false;
        if (t.isTypeParameter()) return tps.contains(t.typeParameter());
        return t.parameters().stream().anyMatch(a -> mentionsAny(a, tps, seen));
    }

    private static boolean containsWildcard(ParameterizedType t) {
        if (t == null) return false;
        if (t.wildcard() != null) return true;
        return t.parameters().stream().anyMatch(CSharpCompanions::containsWildcard);
    }
}
