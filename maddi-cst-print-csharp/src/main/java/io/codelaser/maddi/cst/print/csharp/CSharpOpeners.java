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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.impl.output.GuideImpl;
import io.codelaser.maddi.cst.impl.output.KeywordImpl;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;
import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.print.translate.CstSupertypes;

/**
 * Java's capture of a wildcard passed into a generic class's creation, {@code new AndThen<>(delegate, f)} with a
 * {@code Collector<A, ?, I> delegate}: Java names the {@code ?} and creates an {@code AndThen<A, CAP#1, I, X>}. C#
 * infers nothing from the wildcard interface, so the object names it: the wildcard interface declares
 * {@code TOpen_ Open_<TOpen_>(ICollectorOpener_<A, I, TOpen_> o)}, which the type implements as
 * {@code o.With<R>(this)}, and the creation becomes
 * {@code delegate.Open_(new CollectorsAndThenOpener_<A, I, X>(f))}, whose {@code With<R>(ICollector<A, R, I> delegate)}
 * creates {@code new AndThen<A, R, I, X>(delegate, f)}. The opener interface is the wildcard interface's sibling,
 * its type parameters' variance the wildcard interface's reversed (it takes the type in); the opener class is the
 * sibling of the type whose code creates.
 */
final class CSharpOpeners {

    private CSharpOpeners() {
    }

    /** What the code reaches through a wildcard when it is opened: the wildcard interface declares {@code Open_}. */
    static final Object OPENED = new Object() {
        @Override
        public String toString() {
            return "opened";
        }
    };

    static final String RESULT = "TOpen_";

    /**
     * The index of the one argument of {@code cc} whose wildcard Java captures into the created class's type
     * parameters; -1 when there is none, or when the creation is not one an opener makes: the argument's wildcards go
     * into bare, unbounded type parameters of the class, the other arguments mention none of them, and the creation
     * names the class's other type arguments.
     */
    static int openedArgument(ConstructorCall cc) {
        MethodInfo constructor = cc.constructor();
        if (constructor == null || cc.anonymousClass() != null || cc.object() != null) return -1;
        TypeInfo created = constructor.typeInfo();
        ParameterizedType type = cc.parameterizedType();
        if (created == null || !CSharpNames.translated(created) || created.typeParameters().isEmpty()
            || type == null || type.arrays() != 0 || created.enclosingMethod() != null) {
            return -1;
        }
        List<ParameterInfo> parameters = constructor.parameters();
        List<Expression> arguments = cc.parameterExpressions();
        if (parameters.size() != arguments.size() || parameters.stream().anyMatch(ParameterInfo::isVarArgs)) return -1;
        int opened = -1;
        for (int i = 0; i < arguments.size(); i++) {
            ParameterizedType formal = parameters.get(i).parameterizedType();
            ParameterizedType actual = arguments.get(i).parameterizedType();
            if (actual == null || formal.arrays() != 0 || formal.typeInfo() == null
                || !formal.typeInfo().equals(actual.typeInfo()) || formal.parameters().isEmpty()
                || formal.parameters().size() != actual.parameters().size()
                || actual.parameters().stream().noneMatch(CSharpOpeners::open)) {
                continue;
            }
            if (opened >= 0) return -1;
            opened = i;
        }
        if (opened < 0) return -1;
        Map<TypeParameter, Integer> bound = bound(cc, opened);
        if (bound == null) return -1;
        for (int i = 0; i < parameters.size(); i++) {
            if (i != opened && CstQueries.mentionsAny(parameters.get(i).parameterizedType(), List.copyOf(bound.keySet()))) {
                return -1;
            }
        }
        // the creation's other type arguments, which the opener passes on
        if (type.parameters().size() != created.typeParameters().size()) return -1;
        for (int i = 0; i < created.typeParameters().size(); i++) {
            if (!bound.containsKey(created.typeParameters().get(i)) && CSharpWildcardInterfaces.containsWildcard(type.parameters().get(i))) {
                return -1;
            }
        }
        return opened;
    }

    /**
     * The created class's type parameters the opened argument's wildcards go into, with their positions in the
     * argument's type; null when one goes elsewhere than into a bare unbounded type parameter of the class.
     */
    private static Map<TypeParameter, Integer> bound(ConstructorCall cc, int opened) {
        TypeInfo created = cc.constructor().typeInfo();
        ParameterizedType formal = cc.constructor().parameters().get(opened).parameterizedType();
        ParameterizedType actual = cc.parameterExpressions().get(opened).parameterizedType();
        Map<TypeParameter, Integer> bound = new HashMap<>();
        for (int j = 0; j < actual.parameters().size(); j++) {
            ParameterizedType f = formal.parameters().get(j);
            if (open(actual.parameters().get(j))) {
                if (!f.isTypeParameter() || f.arrays() != 0 || !created.typeParameters().contains(f.typeParameter())
                    || f.typeParameter().typeBounds().stream().anyMatch(b -> !b.isJavaLangObject())
                    || bound.put(f.typeParameter(), j) != null) {
                    return null;
                }
            } else if (actual.parameters().get(j).wildcard() != null) {
                return null;
            }
        }
        // a bound type parameter only where the wildcard is
        for (int j = 0; j < formal.parameters().size(); j++) {
            if (!open(actual.parameters().get(j)) && CstQueries.mentionsAny(formal.parameters().get(j), List.copyOf(bound.keySet()))) {
                return null;
            }
        }
        return bound;
    }

    private static boolean open(ParameterizedType a) {
        return a.wildcard() != null && a.wildcard().isUnbound();
    }

    /** The opened type declares {@code Open_}: the code creates from its wildcard ({@link #openedArgument}). */
    static boolean opened(TypeInfo t) {
        return CSharpContext.program().reachedThroughWildcard(t).contains(OPENED);
    }

    /** An opener: the class's name, the creation, the argument opened, the types it works out. */
    record Site(String name, ConstructorCall cc, int argument, TypeInfo openedType,
                Map<TypeParameter, ParameterizedType> arguments, ParameterizedType result,
                List<TypeParameter> typeParameters) {
    }

    /** The openers of {@code t}'s own code, in order, which {@link #site} then knows. */
    static List<Site> sites(TypeInfo t) {
        CSharpProgram.Memo memo = CSharpContext.memo();
        List<Site> known = memo.openers.get(t);
        if (known != null) return known;
        List<Site> sites = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Element code : CstQueries.code(t)) {
            code.visit((Element e) -> {
                if (e instanceof ConstructorCall cc) {
                    Site site = site(t, cc, names);
                    if (site != null) {
                        sites.add(site);
                        memo.openerSites.put(cc, site);
                    }
                }
                return true;
            });
        }
        memo.openers.put(t, List.copyOf(sites));
        return sites;
    }

    /** The opener of {@code cc}, of a type whose {@link #sites} are known; null when it has none. */
    static Site site(ConstructorCall cc) {
        TypeInfo current = CSharpContext.currentType();
        if (current != null) sites(current);
        return CSharpContext.memo().openerSites.get(cc);
    }

    private static Site site(TypeInfo owner, ConstructorCall cc, Set<String> names) {
        int k = openedArgument(cc);
        if (k < 0) return null;
        ParameterizedType actual = cc.parameterExpressions().get(k).parameterizedType();
        TypeInfo x = actual.typeInfo();
        if (!CSharpWildcardInterfaces.has(x) || !opened(x)) return null;
        // every removed type parameter is a wildcard here: With names them all
        List<TypeParameter> removed = CSharpWildcardInterfaces.removed(x);
        for (int j = 0; j < x.typeParameters().size(); j++) {
            if (removed.contains(x.typeParameters().get(j)) != open(actual.parameters().get(j))) return null;
        }
        TypeInfo created = cc.constructor().typeInfo();
        Map<TypeParameter, Integer> bound = bound(cc, k);
        Map<TypeParameter, ParameterizedType> arguments = new HashMap<>();
        for (int i = 0; i < created.typeParameters().size(); i++) {
            TypeParameter tp = created.typeParameters().get(i);
            Integer j = bound.get(tp);
            arguments.put(tp, j != null ? x.typeParameters().get(j).asParameterizedType()
                    : cc.parameterizedType().parameters().get(i));
        }
        ParameterizedType result = result(created, bound.keySet(), arguments);
        if (result == null) return null;
        // the call site's type parameters the opener class takes
        Set<TypeParameter> mentioned = new LinkedHashSet<>();
        for (int j = 0; j < actual.parameters().size(); j++) {
            if (!open(actual.parameters().get(j))) collect(actual.parameters().get(j), mentioned);
        }
        arguments.forEach((tp, a) -> {
            if (!bound.containsKey(tp)) collect(a, mentioned);
        });
        collect(result, mentioned);
        mentioned.removeAll(removed);
        Set<String> taken = removed.stream().map(CSharpNames::typeParameter).collect(Collectors.toSet());
        if (mentioned.stream().map(CSharpNames::typeParameter).anyMatch(n -> taken.contains(n) || RESULT.equals(n))) {
            return null;
        }
        String base = CSharpNames.type(owner) + CSharpNames.type(created) + "Opener";
        String name = base + "_";
        for (int n = 2; !names.add(name); n++) name = base + n + "_";
        return new Site(name, cc, k, x, Map.copyOf(arguments), result, List.copyOf(mentioned));
    }

    private static void collect(ParameterizedType pt, Set<TypeParameter> into) {
        if (pt == null) return;
        if (pt.isTypeParameter()) into.add(pt.typeParameter());
        pt.parameters().forEach(a -> collect(a, into));
    }

    /**
     * What {@code Open_} returns: the created class, or its first supertype, breadth first, that has the bound type
     * parameters only where its wildcard interface removed ones, as that wildcard interface; else the first that has
     * none of them (not Object). In the call site's types, the bound ones With's.
     */
    private static ParameterizedType result(TypeInfo created, Set<TypeParameter> bound,
                                            Map<TypeParameter, ParameterizedType> arguments) {
        List<TypeParameter> boundList = List.copyOf(bound);
        List<ParameterizedType> order = new ArrayList<>();
        Deque<ParameterizedType> todo = new ArrayDeque<>();
        Set<TypeInfo> seen = new HashSet<>();
        todo.add(created.asParameterizedType());
        while (!todo.isEmpty()) {
            ParameterizedType pt = todo.poll();
            TypeInfo t = pt.typeInfo();
            if (t == null || !seen.add(t)) continue;
            order.add(pt);
            Map<TypeParameter, ParameterizedType> map = CstSupertypes.typeArguments(pt);
            if (t.parentClass() != null) todo.add(CstSupertypes.substitute(t.parentClass(), map));
            t.interfacesImplemented().forEach(s -> todo.add(CstSupertypes.substitute(s, map)));
        }
        // the one whose wildcard interface hides the fewest other arguments, breadth first: AndThen's
        // Collector<A, R, X> rather than AndThen's own wildcard interface, which hides all four
        ParameterizedType best = null;
        int fewest = Integer.MAX_VALUE;
        if (created.simpleName().startsWith("AndThenUni")) System.err.println("OPENER-DEBUG order " + order + " bound " + boundList);
        for (ParameterizedType pt : order) {
            TypeInfo t = pt.typeInfo();
            if (!CstQueries.mentionsAny(pt, boundList) || !CSharpWildcardInterfaces.has(t)
                || pt.parameters().size() != t.typeParameters().size()) {
                continue;
            }
            List<TypeParameter> removed = CSharpWildcardInterfaces.removed(t);
            boolean fits = true;
            int hiddenOthers = 0;
            for (int i = 0; i < pt.parameters().size() && fits; i++) {
                boolean mentions = CstQueries.mentionsAny(pt.parameters().get(i), boundList);
                boolean hidden = removed.contains(t.typeParameters().get(i));
                fits = hidden || !mentions;
                if (hidden && !mentions) hiddenOthers++;
            }
            if (created.simpleName().startsWith("AndThenUni")) {
                System.err.println("OPENER-DEBUG " + created.simpleName() + " candidate " + pt + " fits " + fits + " hidden " + hiddenOthers
                                   + " removed " + removed + " bound " + boundList + " mentions " + pt.parameters().stream().map(a -> CstQueries.mentionsAny(a, boundList)).toList());
            }
            if (fits && hiddenOthers < fewest) {
                best = pt;
                fewest = hiddenOthers;
            }
        }
        if (best != null) return CstSupertypes.substitute(best, arguments);
        for (ParameterizedType pt : order) {
            if (!CstQueries.mentionsAny(pt, boundList) && !pt.isJavaLangObject() && CSharpNames.translated(pt.typeInfo())) {
                return CstSupertypes.substitute(pt, arguments);
            }
        }
        return null;
    }

    /** The result type, printed: the wildcard interface where it mentions With's type parameters. */
    private static String result(Site site, Qualification q) {
        List<TypeParameter> removed = CSharpWildcardInterfaces.removed(site.openedType());
        return CstQueries.mentionsAny(site.result(), removed) ? CSharpWildcardInterfaces.wildcardUse(site.result(), q)
                : CSharpTypeName.of(site.result(), q);
    }

    /** {@code delegate.Open_(new CollectorsAndThenOpener_<A, I, X>(f))}. */
    static OutputBuilder call(Site site, Qualification q, BiFunction<Integer, Expression, OutputBuilder> argument) {
        List<Expression> arguments = site.cc().parameterExpressions();
        List<OutputBuilder> passed = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            if (i != site.argument()) passed.add(argument.apply(i, arguments.get(i)));
        }
        OutputBuilder creation = new OutputBuilderImpl().add(KeywordImpl.NEW).add(SpaceEnum.ONE)
                .add(new TextImpl(site.name() + CSharpNames.typeParameters(site.typeParameters())))
                .add(passed.stream().collect(OutputBuilderImpl.joining(SymbolEnum.COMMA, SymbolEnum.LEFT_PARENTHESIS,
                        SymbolEnum.RIGHT_PARENTHESIS, GuideImpl.defaultGuideGenerator())));
        return new OutputBuilderImpl().add(CSharpNullability.dereferenced(arguments.get(site.argument()), q))
                .add(SymbolEnum.DOT).add(new TextImpl("Open_")).add(SymbolEnum.LEFT_PARENTHESIS).add(creation)
                .add(SymbolEnum.RIGHT_PARENTHESIS);
    }

    /** The opener interface's name, the wildcard interface's + {@code Opener_}. */
    static String interfaceName(TypeInfo t, Qualification q) {
        String wildcardInterface = CSharpWildcardInterfaces.wildcardUse(t.asParameterizedType(), q);
        int lt = wildcardInterface.indexOf('<');
        return (lt < 0 ? wildcardInterface : wildcardInterface.substring(0, lt)) + "Opener_";
    }

    /** {@code TOpen_ Open_<TOpen_>(ICollectorOpener_<A, I, TOpen_> o)}: the wildcard interface's member. */
    static CSharpWildcardInterfaces.Member member(TypeInfo t, String self, Qualification q) {
        List<String> remaining = CSharpWildcardInterfaces.remaining(t).stream().map(CSharpNames::typeParameter).toList();
        String opener = interfaceName(t, q) + "<" + String.join(", ", plus(remaining, RESULT)) + ">";
        String signature = "Open_<" + RESULT + ">(" + opener + " o)";
        List<String> removed = CSharpWildcardInterfaces.removed(t).stream().map(CSharpNames::typeParameter).toList();
        return new CSharpWildcardInterfaces.Member(RESULT + " " + signature + ";",
                RESULT + " " + self + "." + signature + " => o.With<" + String.join(", ", removed) + ">(this);");
    }

    private static List<String> plus(List<String> list, String last) {
        List<String> all = new ArrayList<>(list);
        all.add(last);
        return all;
    }

    /**
     * {@code interface ICollectorOpener_<out A, in I, out TOpen_> { TOpen_ With<R>(ICollector<A, R, I> x); }}: the
     * remaining type parameters' variance reversed, which a variant wildcard interface's {@code Open_} takes.
     */
    static OutputBuilder interfaceDeclaration(TypeInfo t, String access, Qualification q) {
        List<TypeParameter> remaining = CSharpWildcardInterfaces.remaining(t);
        List<TypeParameter> removed = CSharpWildcardInterfaces.removed(t);
        CSharpProgram program = CSharpContext.program();
        List<String> parameters = new ArrayList<>(remaining.stream().map(tp -> {
            String variance = program.variance(tp);
            String flipped = "in".equals(variance) ? "out " : "out".equals(variance) ? "in " : "";
            return flipped + CSharpNames.typeParameter(tp);
        }).toList());
        parameters.add("out " + RESULT);
        StringBuilder header = new StringBuilder();
        if (access != null) header.append(access).append(' ');
        header.append("interface ").append(interfaceName(t, q)).append('<').append(String.join(", ", parameters)).append('>');
        for (String constraint : CSharpTypeName.constraints(remaining.stream()
                .filter(tp -> tp.typeBounds().stream().noneMatch(b -> CstQueries.mentionsAny(b, removed))).toList(), q)) {
            header.append(' ').append(constraint);
        }
        List<String> constraints = CSharpTypeName.constraints(removed, q);
        String with = RESULT + " With" + CSharpNames.typeParameters(removed) + "(" + CSharpTypeName.of(t.asParameterizedType(), q)
                      + " x)" + (constraints.isEmpty() ? "" : " " + String.join(" ", constraints)) + ";";
        return new OutputBuilderImpl().add(new TextImpl(header.toString())).add(SpaceEnum.ONE)
                .add(CSharpStatementPrinter.braces(List.of(new OutputBuilderImpl().add(new TextImpl(with)))));
    }

    /**
     * {@code internal sealed class CollectorsAndThenOpener_<A, I, X>(Func<I, X> f) : ICollectorOpener_<A, I, ICollector<A, X>>}
     * with {@code With<R>(ICollector<A, R, I> delegate) => new AndThen<A, R, I, X>(delegate, f)}.
     */
    static List<OutputBuilder> classDeclarations(TypeInfo owner, Qualification q) {
        List<OutputBuilder> out = new ArrayList<>();
        CSharpProgram program = CSharpContext.program();
        for (Site site : sites(owner)) {
            TypeInfo x = site.openedType();
            ParameterizedType actual = site.cc().parameterExpressions().get(site.argument()).parameterizedType();
            List<TypeParameter> removed = CSharpWildcardInterfaces.removed(x);
            List<String> openerArguments = new ArrayList<>();
            Map<TypeParameter, ParameterizedType> withMap = new HashMap<>();
            for (int j = 0; j < x.typeParameters().size(); j++) {
                TypeParameter tp = x.typeParameters().get(j);
                if (removed.contains(tp)) continue;
                openerArguments.add(CSharpTypeName.argument(actual.parameters().get(j), q));
                withMap.put(tp, actual.parameters().get(j));
            }
            String result = result(site, q);
            openerArguments.add(result);
            String opener = interfaceName(x, q) + "<" + String.join(", ", openerArguments) + ">";
            MethodInfo constructor = site.cc().constructor();
            List<String> fields = new ArrayList<>();
            List<String> names = new ArrayList<>();
            String parameter = null;
            for (int i = 0; i < constructor.parameters().size(); i++) {
                ParameterInfo p = constructor.parameters().get(i);
                String name = CSharpNames.name(p.name());
                names.add(name);
                if (i == site.argument()) {
                    parameter = CSharpTypeName.of(CstSupertypes.substitute(x.asParameterizedType(), withMap), q) + " " + name;
                } else {
                    fields.add(CSharpTypeName.declared(CstSupertypes.substitute(p.parameterizedType(), site.arguments()),
                            false, program.facts().nullable(p), q) + " " + name);
                }
            }
            String created = CSharpTypeName.of(CstSupertypes.substitute(constructor.typeInfo().asParameterizedType(),
                    site.arguments()), q);
            StringBuilder header = new StringBuilder("internal sealed class ").append(site.name())
                    .append(CSharpNames.typeParameters(site.typeParameters()));
            if (!fields.isEmpty()) header.append('(').append(String.join(", ", fields)).append(')');
            header.append(" : ").append(opener);
            for (String constraint : CSharpTypeName.constraints(site.typeParameters(), q)) header.append(' ').append(constraint);
            String with = result + " " + opener + ".With" + CSharpNames.typeParameters(removed) + "(" + parameter
                          + ") => new " + created + "(" + String.join(", ", names) + ");";
            out.add(new OutputBuilderImpl().add(new TextImpl(header.toString())).add(SpaceEnum.ONE)
                    .add(CSharpStatementPrinter.braces(List.of(new OutputBuilderImpl().add(new TextImpl(with))))));
        }
        return out;
    }
}
