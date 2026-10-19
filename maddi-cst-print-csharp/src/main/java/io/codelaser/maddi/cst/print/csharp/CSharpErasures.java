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

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;
import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.print.translate.CstSupertypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java's {@code UniConstraintCollector<A, ?, Double>} of a program interface without a wildcard interface
 * ({@link CSharpWildcardInterfaces}: the code passes the wildcard where a generic method captures the type parameter)
 * is C#'s {@code IUniConstraintCollector<A, object, double>}, which an {@code AverageUniCollector<A>}, an
 * {@code IUniConstraintCollector<A, State, double>}, is not: C#'s variance holds for neither. The interface has an
 * erasure adapter, which implements it with {@code object} for the type parameters the program's code leaves unbound,
 * by calling the adapted one's members: a parameter cast, a delegate's adapted by a lambda. A value that goes into such a
 * wildcard is adapted, {@code IUniConstraintCollector<A, State, double>.Erased_(new AverageUniCollector<A>(mapper))};
 * one that is already of object itself.
 * <p>
 * The adapter is another object than the one it adapts, where the code compares them by reference; and only where it
 * adapts every instance method of the interface, which extends no other.
 */
final class CSharpErasures {

    private CSharpErasures() {
    }

    static final String METHOD = "Erased_";
    private static final String CLASS = "Erasure_";
    private static final String FIELD = "erased";

    /**
     * The type parameters the adapter of {@code t} erases: those the program's code declares with an unbound
     * wildcard, themselves unbound; empty when {@code t} has no adapter.
     */
    static List<TypeParameter> erased(TypeInfo t) {
        if (t == null || !t.isInterface() || !CSharpNames.translated(t) || t.typeParameters().isEmpty()
            || t.isAnonymous() || t.enclosingMethod() != null || !t.interfacesImplemented().isEmpty()) {
            return List.of();
        }
        CSharpProgram program = CSharpContext.program();
        Set<TypeParameter> wildcarded = program.wildcarded(t);
        if (program.delegate(t) || wildcarded.isEmpty() || CSharpWildcardInterfaces.has(t)) return List.of();
        List<TypeParameter> erased = t.typeParameters().stream()
                .filter(tp -> wildcarded.contains(tp) && tp.typeBounds().stream().allMatch(ParameterizedType::isJavaLangObject))
                .toList();
        if (erased.isEmpty()) return List.of();
        // another type parameter's bound that mentions one, which object would not meet
        if (t.typeParameters().stream().anyMatch(tp -> tp.typeBounds().stream().anyMatch(b -> CstQueries.mentionsAny(b, erased)))) {
            return List.of();
        }
        for (MethodInfo m : t.methods()) {
            if (instance(m) && !adaptable(m, erased)) return List.of();
        }
        return erased;
    }

    /** An instance method the adapter implements: not Object's, which the adapter has itself. */
    private static boolean instance(MethodInfo m) {
        return !m.isStatic() && !m.isSynthetic() && m.overrides().stream()
                .noneMatch(o -> "java.lang.Object".equals(o.typeInfo().fullyQualifiedName()));
    }

    /**
     * A method whose parameters are each an erased type parameter, cast, or mention none; whose result is one, mentions
     * none, or is a delegate the adapter adapts by a lambda ({@link CSharpWildcardInterfaces#erasedDelegate}). Not a
     * generic one, nor one C# prints as a property.
     */
    private static boolean adaptable(MethodInfo m, List<TypeParameter> erased) {
        CSharpProgram program = CSharpContext.program();
        if (!m.typeParameters().isEmpty() || program.accessorOf(m) != null || program.propertyRead(m) != null) {
            return false;
        }
        for (ParameterInfo p : m.parameters()) {
            if (p.isVarArgs() || CstQueries.mentionsAny(p.parameterizedType(), erased) && !exactly(p.parameterizedType(), erased)) {
                return false;
            }
        }
        ParameterizedType result = m.returnType();
        return m.isVoid() || !CstQueries.mentionsAny(result, erased) || exactly(result, erased)
               || CSharpWildcardInterfaces.erasedDelegate(result, erased);
    }

    private static boolean exactly(ParameterizedType pt, List<TypeParameter> erased) {
        return pt.isTypeParameter() && pt.arrays() == 0 && erased.contains(pt.typeParameter());
    }

    /**
     * The interface's static adapter method and the adapter class:
     * {@code static IC<A, object, R> Erased_(IC<A, RC, R> erased) => erased as IC<A, object, R> ?? new Erasure_(erased);}.
     */
    static List<OutputBuilder> members(TypeInfo t, Qualification q) {
        List<TypeParameter> erased = erased(t);
        if (erased.isEmpty()) return List.of();
        String name = CSharpNames.type(t);
        String self = name + CSharpNames.typeParameters(t.typeParameters());
        String adapter = erase(self, erased);
        List<String> body = new ArrayList<>();
        body.add("private readonly " + self + " " + FIELD + ";");
        body.add("internal " + CLASS + "(" + self + " " + FIELD + ") { this." + FIELD + " = " + FIELD + "; }");
        for (MethodInfo m : t.methods()) {
            if (instance(m)) body.add(forwarding(m, erased, q));
        }
        List<OutputBuilder> members = new ArrayList<>();
        members.add(text("static " + adapter + " " + METHOD + "(" + self + " " + FIELD + ") => " + FIELD + " as "
                         + adapter + " ?? new " + CLASS + "(" + FIELD + ");"));
        members.add(new OutputBuilderImpl().add(text("private sealed class " + CLASS + " : " + adapter))
                .add(SpaceEnum.ONE)
                .add(CSharpStatementPrinter.braces(body.stream().map(CSharpErasures::text).toList())));
        return members;
    }

    /** The adapter's implementation of {@code m}: a call of the adapted one's. */
    private static String forwarding(MethodInfo m, List<TypeParameter> erased, Qualification q) {
        CSharpProgram program = CSharpContext.program();
        boolean facts = program.facts().nullabilityAnalysed();
        List<String> parameters = new ArrayList<>();
        List<String> arguments = new ArrayList<>();
        for (ParameterInfo p : m.parameters()) {
            String pn = CSharpNames.name(p.name());
            String type = CSharpTypeName.declared(p.parameterizedType(), false, program.facts().nullable(p), q);
            parameters.add(erase(type, erased) + " " + pn);
            arguments.add(exactly(p.parameterizedType(), erased)
                    ? "(" + CSharpNames.typeParameter(p.parameterizedType().typeParameter()) + ") " + pn + (facts ? "!" : "")
                    : pn);
        }
        String name = CSharpNames.method(m);
        String call = FIELD + "." + name + "(" + String.join(", ", arguments) + ")";
        String signature = name + "(" + String.join(", ", parameters) + ")";
        if (m.isVoid()) return "public void " + signature + " => " + call + ";";
        ParameterizedType result = m.returnType();
        String type = erase(CSharpTypeName.declared(result, false, program.facts().nullable(m), q), erased);
        if (exactly(result, erased)) {
            return "public " + type + " " + signature + " => " + call + (facts && !type.endsWith("?") ? "!" : "") + ";";
        }
        if (CstQueries.mentionsAny(result, erased)) {
            return "public " + type + " " + signature + " " + CSharpWildcardInterfaces.delegateBridge(m, call, erased, q);
        }
        return "public " + type + " " + signature + " => " + call + ";";
    }

    /** A printed type with each erased type parameter {@code object}. */
    private static String erase(String printed, List<TypeParameter> erased) {
        for (TypeParameter tp : erased) {
            printed = printed.replaceAll("\\b" + Pattern.quote(CSharpNames.typeParameter(tp)) + "\\b",
                    Matcher.quoteReplacement("object"));
        }
        return printed;
    }

    /**
     * {@code value}'s type as the target's interface, where the target has an unbound wildcard at each type parameter
     * the interface's adapter erases, its other type arguments those of the value's: {@code IC<A, State, double>} of an
     * {@code AverageUniCollector<A>} where an {@code IC<A, ?, Double>} is expected. Null when the value needs no
     * adapter, being of object there, or the adapter does not fit.
     */
    static ParameterizedType adapted(Expression value, ParameterizedType target) {
        if (target == null || target.arrays() != 0 || target.typeInfo() == null || target.parameters().isEmpty()
            || target.parameters().stream().noneMatch(CSharpErasures::open)) {
            return null;
        }
        TypeInfo t = target.typeInfo();
        if (target.parameters().size() != t.typeParameters().size()) return null;
        List<TypeParameter> erased = erased(t);
        if (erased.isEmpty()) return null;
        ParameterizedType from = value.parameterizedType();
        if (from == null || from.arrays() != 0 || from.typeInfo() == null || from.isTypeParameter()) return null;
        ParameterizedType seen = CstSupertypes.asSeen(from, t);
        if (seen == null || seen.parameters().size() != t.typeParameters().size()) return null;
        ParameterizedType asTarget = CstSupertypes.extendsAsBound(seen);
        boolean differs = false;
        for (int i = 0; i < t.typeParameters().size(); i++) {
            ParameterizedType expected = target.parameters().get(i);
            ParameterizedType actual = asTarget.parameters().get(i);
            if (CSharpWildcardInterfaces.containsWildcard(actual)) return null;
            boolean open = open(expected);
            if (open != erased.contains(t.typeParameters().get(i))) return null;
            if (open) differs |= !actual.isJavaLangObject();
            else if (!expected.equals(actual)) return null;
        }
        return differs ? asTarget : null;
    }

    private static boolean open(ParameterizedType a) {
        return a.wildcard() != null && a.wildcard().isUnbound();
    }

    /** {@code IC<A, State, double>.Erased_(value)}. */
    static OutputBuilder adapter(ParameterizedType asTarget, OutputBuilder value, Qualification q) {
        return new OutputBuilderImpl().add(text(CSharpTypeName.of(asTarget, q) + "." + METHOD))
                .add(SymbolEnum.LEFT_PARENTHESIS).add(value).add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    private static OutputBuilder text(String s) {
        return new OutputBuilderImpl().add(new TextImpl(s));
    }
}
