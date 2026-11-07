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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.NullConstant;
import io.codelaser.maddi.cst.api.expression.TypeExpression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.impl.output.GuideImpl;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;
import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.print.translate.CstSupertypes;

/**
 * Java's capture of a wildcard passed into a generic method, {@link CSharpOpeners}'s for a call: a
 * {@code Score<?>} or raw {@code Score} given to {@code <Score_ extends Score<Score_>> boolean isTerminated(boolean b,
 * Score_ score)}, where no C# type argument satisfies {@code where Score_ : IScore<Score_>} (CS0311), or an
 * {@code InnerScore<?>} given to {@code <Score_ …> void setBestScore(InnerScore<Score_> score)}, from whose wildcard
 * interface C# infers nothing (CS0411). The value names its type argument: the call becomes
 * {@code score.Open_(new IsTerminatedOpener_(this, b))}, whose
 * {@code With<Score_>(IScore<Score_> x) => target_.IsTerminated(b, (Score_) x)} makes the call with it. The opener
 * class is nested in the calling class, which may call its own private methods; the other arguments, and the receiver,
 * are evaluated before the call, after the opened one.
 */
final class CSharpCallOpeners {

    private CSharpCallOpeners() {
    }

    private static final String TARGET = "target_";

    /**
     * An opener: its class's name, the type whose code calls, the call, the argument opened, the opened argument's
     * type, the method's type parameters With names (to the opened type's), the result in With's types (null when
     * void), the caller's type parameters the opener class takes.
     */
    record Site(String name, TypeInfo owner, MethodCall mc, int argument, TypeInfo openedType,
                Map<TypeParameter, TypeParameter> with, ParameterizedType result, List<TypeParameter> typeParameters,
                Map<TypeParameter, ParameterizedType> callee) {
    }

    /**
     * The index of the argument of {@code mc} whose wildcard, or raw type, Java captures into the called method's type
     * parameters; -1 when there is none. Of the program's types only: what the code reaches through the wildcard
     * ({@link CSharpWildcardInterfaces}) counts it as opened.
     */
    static int openedArgument(MethodCall mc) {
        MethodInfo m = mc.methodInfo();
        if (m == null || m.typeParameters().isEmpty() || !CSharpNames.translated(m.typeInfo())) return -1;
        List<ParameterInfo> parameters = m.parameters();
        List<Expression> arguments = mc.parameterExpressions();
        if (parameters.size() != arguments.size() || parameters.stream().anyMatch(ParameterInfo::isVarArgs)) return -1;
        if (parameters.stream().anyMatch(p -> p.parameterizedType().typeInfo() != null
                                             && "java.lang.Class".equals(p.parameterizedType().typeInfo().fullyQualifiedName()))) {
            return -1;
        }
        // C# infers the method's other type parameters from the other arguments
        for (TypeParameter tp : m.typeParameters()) {
            if (parameters.stream().noneMatch(p -> CstQueries.mentions(p.parameterizedType(), tp))) return -1;
        }
        for (int i = 0; i < arguments.size(); i++) {
            Map<TypeParameter, TypeParameter> with = with(m, parameters.get(i).parameterizedType(),
                    arguments.get(i).parameterizedType());
            if (with != null) return i;
        }
        return -1;
    }

    /**
     * The method's type parameters the argument's wildcards (or raw type) name, to the argument type's own: from a
     * formal {@code InnerScore<Score_>}, or from a bare {@code Score_} bounded by {@code Score<Score_>}. Null when it
     * names none, or when the formal mentions them elsewhere than there.
     */
    private static Map<TypeParameter, TypeParameter> with(MethodInfo m, ParameterizedType formal, ParameterizedType actual) {
        if (actual == null || formal == null || actual.arrays() != 0 || formal.arrays() != 0) return null;
        TypeInfo x = openedType(actual);
        if (x == null || x.typeParameters().isEmpty() || !CSharpNames.translated(x)) return null;
        boolean raw = actual.isTypeParameter() || actual.parameters().isEmpty();
        if (!raw && actual.parameters().size() != x.typeParameters().size()) return null;
        ParameterizedType shape;
        if (formal.isTypeParameter()) {
            if (!m.typeParameters().contains(formal.typeParameter())) return null;
            shape = formal.typeParameter().typeBounds().stream().filter(b -> x.equals(b.typeInfo())).findFirst().orElse(null);
            if (shape == null || shape.parameters().stream().noneMatch(p -> p.isTypeParameter()
                                                                            && p.typeParameter().equals(formal.typeParameter()))) {
                return null;
            }
        } else {
            shape = formal;
        }
        if (!x.equals(shape.typeInfo()) || shape.parameters().size() != x.typeParameters().size()) return null;
        Map<TypeParameter, TypeParameter> with = new LinkedHashMap<>();
        for (int j = 0; j < x.typeParameters().size(); j++) {
            ParameterizedType a = raw ? null : actual.parameters().get(j);
            boolean open = raw || a.wildcard() != null && a.wildcard().isUnbound();
            ParameterizedType f = shape.parameters().get(j);
            if (!open) {
                if (a.wildcard() != null) return null;
                continue;
            }
            if (!f.isTypeParameter() || f.arrays() != 0 || !m.typeParameters().contains(f.typeParameter())) return null;
            TypeParameter previous = with.put(f.typeParameter(), x.typeParameters().get(j));
            if (previous != null && !previous.equals(x.typeParameters().get(j))) return null;
        }
        if (with.isEmpty()) return null;
        // named only where the wildcards are
        for (int j = 0; j < x.typeParameters().size(); j++) {
            ParameterizedType a = raw ? null : actual.parameters().get(j);
            boolean open = raw || a.wildcard() != null && a.wildcard().isUnbound();
            if (!open && CstQueries.mentionsAny(shape.parameters().get(j), List.copyOf(with.keySet()))) return null;
        }
        return with;
    }

    /**
     * The type an argument opens: its own, or, for the type parameter of a method C# erased that leaks into the call
     * ({@code scope.best().raw()} of {@code <Score_ extends Score<Score_>> Inner<Score_> best()}), its bound's, raw as
     * C# prints it ({@code IScore}).
     */
    static TypeInfo openedType(ParameterizedType actual) {
        if (!actual.isTypeParameter()) return actual.typeInfo();
        TypeParameter tp = actual.typeParameter();
        if (!tp.isMethodTypeParameter()) return null;
        MethodInfo owner = tp.getOwner().getRight();
        if (owner.parameters().stream().anyMatch(p -> CstQueries.mentions(p.parameterizedType(), tp))) return null;
        return tp.typeBounds().stream().filter(b -> b.typeInfo() != null && CstQueries.mentions(b, tp))
                .map(ParameterizedType::typeInfo).findFirst().orElse(null);
    }

    /** The openers of {@code t}'s own code, in order. */
    static List<Site> sites(TypeInfo t) {
        CSharpProgram.Memo memo = CSharpContext.memo();
        List<Site> known = memo.callOpeners.get(t);
        if (known != null) return known;
        List<Site> sites = new ArrayList<>();
        Set<String> names = new HashSet<>();
        if (!t.isInterface() && !(t.typeNature().isEnum() && CSharpNames.simpleEnum(t))) {
            for (Element code : CstQueries.code(t)) {
                code.visit((Element e) -> {
                    if (e instanceof MethodCall mc) {
                        Site site = site(t, mc, names);
                        if (site != null) {
                            sites.add(site);
                            memo.callOpenerSites.put(mc, site);
                        }
                    }
                    return true;
                });
            }
        }
        memo.callOpeners.put(t, List.copyOf(sites));
        return sites;
    }

    /**
     * The opener of {@code mc}, printed in the code of the type that declares it, not in a companion, an anonymous
     * class or an inner class's outer instance; null otherwise.
     */
    static Site site(MethodCall mc) {
        TypeInfo current = CSharpContext.currentType();
        if (current == null) return null;
        sites(current);
        Site site = CSharpContext.memo().callOpenerSites.get(mc);
        if (site == null || !site.owner().equals(current) || CSharpContext.hoistedWithOuter() != null) return null;
        MethodInfo method = CSharpContext.currentMethod();
        if (method != null && CSharpNames.inCompanion(method)) return null;
        MethodInfo m = mc.methodInfo();
        if (!m.isStatic() && implicitThis(mc) && CSharpInner.pathTo(m.typeInfo()) != null) return null;
        return site;
    }

    private static boolean implicitThis(MethodCall mc) {
        Expression object = mc.object();
        return object == null || mc.objectIsImplicit() || object instanceof VariableExpression ve && ve.variable() instanceof This;
    }

    private static Site site(TypeInfo owner, MethodCall mc, Set<String> names) {
        int k = openedArgument(mc);
        if (k < 0) return null;
        MethodInfo m = mc.methodInfo();
        if (CSharpContext.program().erasedGeneric(m)) return null;
        ParameterizedType actual = mc.parameterExpressions().get(k).parameterizedType();
        TypeInfo x = openedType(actual);
        if (!CSharpWildcardInterfaces.has(x) || !CSharpOpeners.opened(x)) return null;
        Map<TypeParameter, TypeParameter> with = with(m, m.parameters().get(k).parameterizedType(), actual);
        List<TypeParameter> removed = CSharpWildcardInterfaces.removed(x);
        if (!Set.copyOf(with.values()).equals(Set.copyOf(removed)) || with.size() != removed.size()) return null;
        // the other arguments of x are written
        for (int j = 0; j < x.typeParameters().size() && !actual.parameters().isEmpty(); j++) {
            if (!removed.contains(x.typeParameters().get(j)) && actual.parameters().get(j).wildcard() != null) return null;
        }
        if (actual.parameters().isEmpty() && !CSharpWildcardInterfaces.remaining(x).isEmpty()) return null;
        // the receiver: the class's own (this, a superclass's), a type's static, or a value of a written type
        Expression object = mc.object();
        boolean self = !m.isStatic() && implicitThis(mc);
        if (self && object instanceof VariableExpression ve && ve.variable() instanceof This t && t.writeSuper()) return null;
        if (!m.isStatic() && !self && CSharpWildcardInterfaces.containsWildcard(object.parameterizedType())) return null;
        // the callee's class type parameters, as the receiver has them
        ParameterizedType receiver = m.isStatic() ? null : self ? owner.asParameterizedType() : object.parameterizedType();
        Map<TypeParameter, ParameterizedType> callee = new HashMap<>();
        if (receiver != null && !m.typeInfo().typeParameters().isEmpty()) {
            ParameterizedType seen = CstSupertypes.asSeen(receiver, m.typeInfo());
            if (seen == null) return null;
            callee.putAll(CstSupertypes.typeArguments(seen));
        }
        Map<TypeParameter, ParameterizedType> withTypes = new HashMap<>(callee);
        with.forEach((tp, w) -> withTypes.put(tp, w.asParameterizedType()));
        List<TypeParameter> others = m.typeParameters().stream().filter(tp -> !with.containsKey(tp)).toList();
        // the other arguments: written, and cast to With's types where the formal names them
        for (int i = 0; i < m.parameters().size(); i++) {
            if (i == k) continue;
            Expression a = mc.parameterExpressions().get(i);
            ParameterizedType formal = m.parameters().get(i).parameterizedType();
            if (a instanceof NullConstant || a.parameterizedType() == null) return null;
            if (CstQueries.mentionsAny(formal, List.copyOf(with.keySet())) && CstQueries.mentionsAny(formal, others)) {
                return null;
            }
        }
        ParameterizedType result = null;
        if (!m.isVoid()) {
            ParameterizedType r = m.returnType();
            if (CstQueries.mentionsAny(r, others)) return null;
            result = CstSupertypes.substitute(r, withTypes);
            if (CstQueries.mentionsAny(result, removed) && !result.isTypeParameter()
                && (result.typeInfo() == null || !CSharpWildcardInterfaces.has(result.typeInfo()))) {
                return null;
            }
        }
        // With's type parameters are new names in the opener class
        Set<String> outer = new HashSet<>();
        for (TypeInfo at = owner; at != null; at = CstQueries.enclosing(at)) {
            at.typeParameters().forEach(tp -> outer.add(CSharpNames.typeParameter(tp)));
        }
        Set<String> withNames = removed.stream().map(CSharpNames::typeParameter).collect(Collectors.toSet());
        if (withNames.stream().anyMatch(outer::contains) || withNames.contains(CSharpOpeners.RESULT)) return null;
        // the calling method's type parameters, which the opener class takes
        Set<TypeParameter> mentioned = new LinkedHashSet<>();
        for (int i = 0; i < m.parameters().size(); i++) {
            if (i != k) collect(mc.parameterExpressions().get(i).parameterizedType(), mentioned);
        }
        for (int j = 0; j < actual.parameters().size(); j++) {
            if (!removed.contains(x.typeParameters().get(j))) collect(actual.parameters().get(j), mentioned);
        }
        if (receiver != null && !self) collect(receiver, mentioned);
        if (result != null) collect(result, mentioned);
        mentioned.removeAll(removed);
        mentioned.removeIf(tp -> !tp.isMethodTypeParameter());
        if (mentioned.stream().map(CSharpNames::typeParameter).anyMatch(n -> withNames.contains(n) || outer.contains(n))) {
            return null;
        }
        List<String> parameterNames = m.parameters().stream().map(p -> CSharpNames.name(p.name())).toList();
        if (parameterNames.contains(TARGET) || parameterNames.contains("x")) return null;
        String base = CSharpNames.method(m) + "Opener";
        String name = base + "_";
        for (int n = 2; !names.add(name); n++) name = base + n + "_";
        return new Site(name, owner, mc, k, x, Map.copyOf(with), result, List.copyOf(mentioned), Map.copyOf(callee));
    }

    private static void collect(ParameterizedType pt, Set<TypeParameter> into) {
        if (pt == null) return;
        if (pt.isTypeParameter()) into.add(pt.typeParameter());
        pt.parameters().forEach(a -> collect(a, into));
    }

    /** {@code score.Open_(new IsTerminatedOpener_(this, b))}. */
    static OutputBuilder call(Site site, Qualification q) {
        MethodCall mc = site.mc();
        MethodInfo m = mc.methodInfo();
        List<OutputBuilder> passed = new ArrayList<>();
        if (!m.isStatic()) {
            passed.add(implicitThis(mc) ? new OutputBuilderImpl().add(new TextImpl("this"))
                    : CSharpExpressionPrinter.print(mc.object(), q));
        }
        List<Expression> arguments = mc.parameterExpressions();
        for (int i = 0; i < arguments.size(); i++) {
            if (i != site.argument()) passed.add(CSharpExpressionPrinter.print(arguments.get(i), q));
        }
        OutputBuilder creation = new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                .add(new TextImpl(site.name() + CSharpNames.typeParameters(site.typeParameters())))
                .add(passed.stream().collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                        SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.defaultGuideGenerator())));
        return new OutputBuilderImpl().add(CSharpNullability.dereferenced(arguments.get(site.argument()), q))
                .add(SymbolEnum.DOT).add(new TextImpl("Open_")).add(SymbolEnum.LEFT_PARENTHESIS).add(creation)
                .add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    /** What With returns: the result, its wildcard interface where it names With's type parameters; object for void. */
    private static String result(Site site, ParameterizedType opened, Qualification q) {
        ParameterizedType r = site.result();
        if (r == null) return "object";
        List<TypeParameter> removed = CSharpWildcardInterfaces.removed(site.openedType());
        if (!CstQueries.mentionsAny(r, removed)) return CSharpTypeName.of(r, q);
        return CSharpWildcardInterfaces.wildcardUse(r.isTypeParameter() ? opened : r, q);
    }

    /**
     * {@code private sealed class IsTerminatedOpener_(BestScoreTermination<Solution_> target_, bool b) :
     * IScoreOpener_<bool>} with {@code With<Score_>(IScore<Score_> x) => target_.IsTerminated(b, (Score_) x)}.
     */
    static List<OutputBuilder> classDeclarations(TypeInfo owner, Qualification q) {
        List<OutputBuilder> out = new ArrayList<>();
        CSharpProgram program = CSharpContext.program();
        for (Site site : sites(owner)) {
            MethodCall mc = site.mc();
            MethodInfo m = mc.methodInfo();
            TypeInfo x = site.openedType();
            ParameterizedType actual = mc.parameterExpressions().get(site.argument()).parameterizedType();
            List<TypeParameter> removed = CSharpWildcardInterfaces.removed(x);
            List<String> openerArguments = new ArrayList<>();
            Map<TypeParameter, ParameterizedType> xMap = new HashMap<>();
            for (int j = 0; j < x.typeParameters().size(); j++) {
                TypeParameter tp = x.typeParameters().get(j);
                if (removed.contains(tp)) {
                    xMap.put(tp, tp.asParameterizedType());
                } else {
                    openerArguments.add(CSharpTypeName.argument(actual.parameters().get(j), q));
                    xMap.put(tp, actual.parameters().get(j));
                }
            }
            ParameterizedType opened = CstSupertypes.substitute(x.asParameterizedType(), xMap);
            String result = result(site, opened, q);
            openerArguments.add(result);
            String opener = CSharpOpeners.interfaceName(x, q) + "<" + String.join(", ", openerArguments) + ">";
            Map<TypeParameter, ParameterizedType> withTypes = new HashMap<>(site.callee());
            site.with().forEach((tp, w) -> withTypes.put(tp, w.asParameterizedType()));
            List<String> fields = new ArrayList<>();
            List<String> passed = new ArrayList<>();
            if (!m.isStatic()) {
                ParameterizedType receiver = implicitThis(mc) ? owner.asParameterizedType() : mc.object().parameterizedType();
                fields.add(CSharpTypeName.of(receiver, q) + " " + TARGET);
            }
            for (int i = 0; i < m.parameters().size(); i++) {
                ParameterInfo p = m.parameters().get(i);
                ParameterizedType formal = CstSupertypes.substitute(p.parameterizedType(), withTypes);
                if (i == site.argument()) {
                    passed.add(p.parameterizedType().isTypeParameter() ? "(" + CSharpTypeName.of(formal, q) + ") x" : "x");
                    continue;
                }
                String name = CSharpNames.name(p.name());
                ParameterizedType type = mc.parameterExpressions().get(i).parameterizedType();
                fields.add(CSharpTypeName.declared(type, false, program.facts().nullable(p), q) + " " + name);
                if (!CstQueries.mentionsAny(p.parameterizedType(), List.copyOf(site.with().keySet()))) {
                    passed.add(name);
                } else if (formal.isTypeParameter()) {
                    passed.add("(" + CSharpTypeName.of(formal, q) + ") " + name);
                } else {
                    passed.add("(" + CSharpTypeName.of(formal, q) + ") (object) " + name);
                }
            }
            String qualifier = m.isStatic() ? CSharpTypeName.nameInExpression(m.typeInfo(), q) : TARGET;
            String call = qualifier + "." + CSharpNames.method(m) + "(" + String.join(", ", passed) + ")";
            StringBuilder header = new StringBuilder("private sealed class ").append(site.name())
                    .append(CSharpNames.typeParameters(site.typeParameters()));
            if (!fields.isEmpty()) header.append('(').append(String.join(", ", fields)).append(')');
            header.append(" : ").append(opener);
            for (String constraint : CSharpTypeName.constraints(site.typeParameters(), q)) header.append(' ').append(constraint);
            String signature = result + " " + opener + ".With" + CSharpNames.typeParameters(removed) + "("
                               + CSharpTypeName.of(opened, q) + " x)";
            String with = site.result() == null ? signature + " { " + call + "; return this; }"
                    : signature + " => " + call + ";";
            out.add(new OutputBuilderImpl().add(new TextImpl(header.toString())).add(SpaceEnum.ONE)
                    .add(CSharpStatementPrinter.braces(List.of(new OutputBuilderImpl().add(new TextImpl(with))))));
        }
        return out;
    }
}
