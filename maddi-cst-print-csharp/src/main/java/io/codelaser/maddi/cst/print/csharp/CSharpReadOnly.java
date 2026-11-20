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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.InlineConditional;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.NullConstant;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ForEachStatement;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.print.translate.CstSupertypes;

/**
 * The collection parameters and fields that are read-only in C#: {@code IReadOnlyList<T>}, {@code IReadOnlyCollection<T>},
 * {@code IReadOnlySet<T>}, {@code IReadOnlyDictionary<K, V>} for Java's {@code List}, {@code Collection}, {@code Set}
 * and {@code Map}. A caller's {@code List<T>} is one, so the calls do not change; the signature says what the
 * modification analysis proved: the method does not modify the collection a parameter holds
 * ({@link CSharpFacts#unmodified}), no code adds to or removes from the collection a private field holds
 * ({@link CSharpFacts#structurallyUnmodified}), a method returns only such fields and parameters or a library's
 * unmodifiable values ({@link CSharpFacts#unmodifiableResult}).
 * <p>
 * The C# has to compile whatever the analysis's precision, so the uses decide too: each is a call of a library
 * method on it, the collection of a {@code foreach}, or an argument where any enumerable is accepted (another
 * read-only parameter, a read-only field it is assigned to, a read-only method that returns it, a library constructor
 * or method that copies it). A use that stores it elsewhere keeps the type as Java declares it. A call of a method
 * with a read-only result is a use of that result, where a library's method on it must add and remove nothing by
 * its contract ({@link CSharpFacts#structurallyNonModifying}). A method's result is read-only only where the method
 * is alone in its family (C#'s results are invariant) and not generic. A view of it ({@code entrySet()},
 * {@code iterator()}, {@code subList()}: {@link #VIEWS}) counts as a read only where the view is iterated or a
 * library reads it in turn, not where another view is taken of it: a removal through a view
 * ({@code map.entrySet().iterator().remove()}) is one the analysis may not see. Every method of an override family has
 * the parameter read-only, or none does; a delegate's, a functional interface's method (its lambda adapter passes
 * the argument on), a record's and a method that a method reference names are not.
 * <p>
 * The callers have to compile too. Java's {@code List} family prints as the class {@code List<T>}, which implements
 * the read-only interfaces, but {@code Map}, {@code Set} and {@code Collection} print as {@code IDictionary},
 * {@code ISet} and {@code ICollection}, which do not. Each argument a call passes for a parameter, and each value a
 * field is given, must be of a class that implements the read-only interface ({@link #IMPLEMENTING}), {@code null},
 * or a read-only parameter or field itself.
 */
final class CSharpReadOnly {

    /** The read-only parameters and fields of the program. */
    record Analysis(Set<ParameterInfo> parameters, Set<FieldInfo> fields, Set<MethodInfo> methods) {
        static final Analysis NONE = new Analysis(Set.of(), Set.of(), Set.of());
    }

    private static final Map<String, String> READ_ONLY = Map.of(
            "java.util.List", "IReadOnlyList",
            "java.util.Collection", "IReadOnlyCollection",
            "java.util.Set", "IReadOnlySet",
            "java.util.Map", "IReadOnlyDictionary");

    /** Library calls an argument of which may be any enumerable: they copy it, or only read it. */
    private static final Set<String> ENUMERABLE_ARGUMENTS = Set.of(
            "java.util.ArrayList.new", "java.util.HashSet.new", "java.util.LinkedHashSet.new", "java.util.TreeSet.new",
            "java.util.LinkedList.new", "java.util.ArrayDeque.new", "java.util.List.copyOf", "java.util.Set.copyOf",
            "java.util.Collection.addAll", "java.util.List.addAll", "java.util.Set.addAll",
            "java.util.Collection.containsAll", "java.util.Collection.removeAll", "java.util.Collection.retainAll",
            "java.util.Set.removeAll", "java.util.Set.retainAll", "java.lang.String.join");

    /** The library methods that return a view of their receiver, through which it can be modified. */
    static final Set<String> VIEWS = Set.of("entrySet", "keySet", "values", "iterator", "listIterator",
            "spliterator", "subList", "headSet", "tailSet", "subSet", "headMap", "tailMap", "subMap", "descendingSet",
            "descendingMap", "navigableKeySet", "descendingKeySet", "descendingIterator");

    /** Per read-only interface, the JDK classes whose C# counterparts implement it, a subclass of one too. */
    private static final Map<String, Set<String>> IMPLEMENTING;

    static {
        Set<String> lists = Set.of("java.util.List", "java.util.ArrayList", "java.util.LinkedList",
                "java.util.AbstractList", "java.util.Deque", "java.util.ArrayDeque", "java.util.Queue");
        Set<String> sets = Set.of("java.util.HashSet", "java.util.LinkedHashSet", "java.util.TreeSet",
                "java.util.SortedSet");
        Set<String> collections = new HashSet<>(lists);
        collections.addAll(sets);
        IMPLEMENTING = Map.of("IReadOnlyList", lists, "IReadOnlySet", sets,
                "IReadOnlyCollection", Set.copyOf(collections),
                "IReadOnlyDictionary", Set.of("java.util.HashMap", "java.util.IdentityHashMap",
                        "java.util.concurrent.ConcurrentHashMap", "java.util.LinkedHashMap", "java.util.EnumMap",
                        "java.util.TreeMap", "java.util.SortedMap"));
    }

    private CSharpReadOnly() {
    }

    /** {@code IReadOnlyList} for a {@code List<T>} without wildcards; null for any other type. */
    static String readOnlyName(ParameterizedType pt) {
        if (pt == null || pt.arrays() > 0 || pt.typeInfo() == null) return null;
        String name = READ_ONLY.get(pt.typeInfo().fullyQualifiedName());
        if (name == null || pt.parameters().isEmpty() || pt.parameters().stream().anyMatch(a -> a.wildcard() != null)) {
            return null;
        }
        return name;
    }

    static Analysis analyze(CSharpProgramIndex index, CSharpFacts facts, Set<TypeInfo> delegates) {
        if (facts == CSharpFacts.NONE) return Analysis.NONE;
        // the candidates: parameters and fields, which a use may pass on to or store in one another
        Set<Object> candidates = new HashSet<>();
        Set<MethodInfo> referenced = index.referenced();
        Map<MethodInfo, List<MethodInfo>> familyOf = new HashMap<>();
        for (List<MethodInfo> family : index.families()) family.forEach(m -> familyOf.put(m, family));
        for (TypeInfo t : index.types()) {
            if (!CSharpNames.translated(t) || t.typeNature().isRecord() || delegates.contains(t)
                || t.isAnonymous() && CSharpProgram.lambdaLike(t)) {
                continue;
            }
            List<MethodInfo> methods = new ArrayList<>(t.methods());
            methods.addAll(t.constructors());
            for (MethodInfo m : methods) {
                if (m.isSynthetic() || m.methodBody() == null && !m.isAbstract() || referenced.contains(m)) continue;
                if (t.isInterface() && m.equals(CstSupertypes.singleAbstractMethod(t))) continue;
                for (ParameterInfo p : m.parameters()) {
                    if (!p.isVarArgs() && readOnlyName(p.parameterizedType()) != null && facts.unmodified(p)) {
                        candidates.add(p);
                    }
                }
            }
            // a private field: every use of it is in the program
            for (FieldInfo f : t.fields()) {
                if (!f.isSynthetic() && f.access() != null && f.access().isPrivate() && readOnlyName(f.type()) != null
                    && facts.structurallyUnmodified(f)) {
                    candidates.add(f);
                }
            }
            // a class's method alone in its family: C#'s results are invariant, and no other method shares the type
            if (!t.isInterface()) {
                for (MethodInfo m : t.methods()) {
                    if (resultCandidate(m, familyOf, referenced)) candidates.add(m);
                }
            }
        }
        // each method's own returns, not those of its lambdas
        Map<ReturnStatement, MethodInfo> returnsOf = new IdentityHashMap<>();
        Map<MethodInfo, List<Expression>> returned = new HashMap<>();
        for (TypeInfo t : index.types()) {
            for (MethodInfo m : t.methods()) {
                if (m.methodBody() == null) continue;
                for (ReturnStatement rs : returnStatements(m)) {
                    returnsOf.put(rs, m);
                    if (candidates.contains(m)) branches(rs.expression(), returned.computeIfAbsent(m, k -> new ArrayList<>()));
                }
            }
        }
        // a method returns only read-only fields and parameters, the results of other such methods, and the
        // unmodifiable values of library calls (List.of, Collections.unmodifiableList): the analysis's verdicts
        // say that nothing is added to them
        // (and one value at least that is not null: such a method gives the type nothing to say)
        candidates.removeIf(c -> c instanceof MethodInfo m && (!returned.getOrDefault(m, List.of()).stream()
                .allMatch(e -> e != null && returnable(e, facts)) || returned.getOrDefault(m, List.of()).stream()
                .allMatch(e -> CSharpExpressionPrinter.unwrap(e) instanceof NullConstant)));
        // what the code does with each candidate, in one pass over the program
        List<Element> program = index.types().stream().flatMap(t -> CstQueries.code(t).stream()).toList();
        Map<Object, List<Use>> uses = uses(candidates, program, returnsOf, facts);
        Map<FieldInfo, List<Expression>> values = new HashMap<>();
        for (Object c : candidates) {
            if (c instanceof FieldInfo f) values.put(f, values(f, program));
        }
        // the uses, then the families and the callers, until nothing changes
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Object c : List.copyOf(candidates)) {
                boolean keep = uses.getOrDefault(c, List.of()).stream().allMatch(u -> u.readOnly(candidates));
                if (keep && c instanceof MethodInfo m) {
                    keep = returned.getOrDefault(m, List.of()).stream().allMatch(e -> CSharpExpressionPrinter.unwrap(e) instanceof NullConstant
                            || candidate(CSharpExpressionPrinter.unwrap(e)) == null || candidates.contains(
                            candidate(CSharpExpressionPrinter.unwrap(e))))
                           && convert(readOnlyName(m.returnType()), returned.getOrDefault(m, List.of()), candidates);
                } else if (keep && c instanceof ParameterInfo p) {
                    keep = familyAgrees(p, candidates, familyOf)
                           && convert(readOnlyName(p.parameterizedType()), passed(index, p), candidates);
                } else if (keep && c instanceof FieldInfo f) {
                    keep = convert(readOnlyName(f.type()), values.get(f), candidates);
                }
                if (!keep) {
                    candidates.remove(c);
                    changed = true;
                }
            }
        }
        Set<ParameterInfo> parameters = new HashSet<>();
        Set<FieldInfo> fields = new HashSet<>();
        Set<MethodInfo> methods = new HashSet<>();
        for (Object c : candidates) {
            if (c instanceof ParameterInfo p) parameters.add(p);
            else if (c instanceof FieldInfo f) fields.add(f);
            else if (c instanceof MethodInfo m) methods.add(m);
        }
        return new Analysis(Set.copyOf(parameters), Set.copyOf(fields), Set.copyOf(methods));
    }

    /**
     * A method whose result may be read-only: of a class, with a body, no type parameters (an opener's result,
     * {@link CSharpCallOpeners}, is the declared type), overriding nothing, overridden by nothing, and named by no
     * method reference.
     */
    private static boolean resultCandidate(MethodInfo m, Map<MethodInfo, List<MethodInfo>> familyOf,
                                           Set<MethodInfo> referenced) {
        return !m.isSynthetic() && !m.isConstructor() && !m.isAbstract() && m.methodBody() != null
               && m.typeParameters().isEmpty() && readOnlyName(m.returnType()) != null && !referenced.contains(m)
               && !familyOf.containsKey(m) && m.overrides().stream().allMatch(o -> o == m);
    }

    /** {@code e} may be the value of a read-only result: null, a candidate, or a library's unmodifiable value. */
    private static boolean returnable(Expression e, CSharpFacts facts) {
        Expression u = CSharpExpressionPrinter.unwrap(e);
        if (u instanceof NullConstant || candidate(u) != null) return true;
        return u instanceof MethodCall mc && !CSharpNames.translated(mc.methodInfo().typeInfo())
               && facts.unmodifiableResult(mc.methodInfo());
    }

    /** The values {@code e} may have: the branches of a conditional, at any depth. */
    private static void branches(Expression e, List<Expression> values) {
        if (CSharpExpressionPrinter.unwrap(e) instanceof InlineConditional ic) {
            branches(ic.ifTrue(), values);
            branches(ic.ifFalse(), values);
        } else {
            values.add(e);
        }
    }

    /** The method's own {@code return}s: not those of its lambdas. */
    private static List<ReturnStatement> returnStatements(MethodInfo m) {
        List<ReturnStatement> returns = new ArrayList<>();
        m.methodBody().visit((Element e) -> {
            if (e instanceof Lambda) return false;
            if (e instanceof ReturnStatement rs && rs.expression() != null && !rs.expression().isEmpty()) {
                returns.add(rs);
            }
            return true;
        });
        return returns;
    }

    /** The arguments the program passes for {@code p}. */
    private static List<Expression> passed(CSharpProgramIndex index, ParameterInfo p) {
        MethodInfo callee = p.methodInfo();
        if (!CSharpNames.translated(callee.typeInfo())) return List.of();
        List<Expression> passed = new ArrayList<>();
        for (List<Expression> arguments : index.calls(callee)) {
            if (p.index() < arguments.size()) passed.add(arguments.get(p.index()));
        }
        return passed;
    }

    /** The values the program gives field {@code f}: its initializer, and every assignment to it. */
    private static List<Expression> values(FieldInfo f, List<Element> program) {
        List<Expression> values = new ArrayList<>();
        if (f.initializer() != null && !f.initializer().isEmpty()) values.add(f.initializer());
        for (Element element : program) {
            element.visit((Element e) -> {
                if (e instanceof Assignment a && a.variableTarget() instanceof FieldReference fr
                    && fr.fieldInfo().equals(f)) {
                    values.add(a.value());
                }
                return true;
            });
        }
        return values;
    }

    /** Each of {@code values} converts to the read-only interface {@code readOnly}. */
    private static boolean convert(String readOnly, List<Expression> values, Set<Object> candidates) {
        Set<String> implementing = IMPLEMENTING.get(readOnly);
        for (Expression value : values) {
            Expression e = CSharpExpressionPrinter.unwrap(value);
            if (e instanceof NullConstant) continue;
            Object other = candidate(e);
            if (other != null && candidates.contains(other) && converts(readOnlyName(typeOf(other)), readOnly)) {
                continue;
            }
            if (!implementing(e.parameterizedType(), implementing)) return false;
        }
        return true;
    }

    /** The parameter or field {@code e} reads; null for any other expression. */
    private static Object candidate(Expression e) {
        if (e instanceof MethodCall mc) return CSharpNames.translated(mc.methodInfo().typeInfo()) ? mc.methodInfo() : null;
        if (!(e instanceof VariableExpression ve)) return null;
        return switch (ve.variable()) {
            case ParameterInfo p -> p;
            case FieldReference fr -> fr.fieldInfo();
            default -> null;
        };
    }

    private static ParameterizedType typeOf(Object candidate) {
        return switch (candidate) {
            case ParameterInfo p -> p.parameterizedType();
            case FieldInfo f -> f.type();
            default -> ((MethodInfo) candidate).returnType();
        };
    }

    /** A read-only {@code from} is a read-only {@code to}: the same, or a list or set as a collection. */
    private static boolean converts(String from, String to) {
        return from.equals(to) || "IReadOnlyCollection".equals(to) && !"IReadOnlyDictionary".equals(from);
    }

    /** The type is one of the classes, or a class of the program that extends one. */
    private static boolean implementing(ParameterizedType pt, Set<String> classes) {
        if (pt == null || pt.arrays() > 0) return false;
        TypeInfo t = pt.typeInfo();
        while (t != null) {
            if (classes.contains(t.fullyQualifiedName())) return true;
            if (t.isInterface() || t.parentClass() == null) return false;
            t = t.parentClass().typeInfo();
        }
        return false;
    }

    /** Every method of {@code p}'s override family, all of the program, has its parameter a candidate. */
    private static boolean familyAgrees(ParameterInfo p, Set<Object> candidates,
                                        Map<MethodInfo, List<MethodInfo>> familyOf) {
        List<MethodInfo> family = familyOf.get(p.methodInfo());
        if (family == null) return true;
        for (MethodInfo m : family) {
            if (!CSharpNames.translated(m.typeInfo()) || m.parameters().size() <= p.index()
                || !candidates.contains(m.parameters().get(p.index()))) {
                return false;
            }
        }
        return true;
    }

    /**
     * A use of a candidate, read-only as the class's description has it: {@code always}, as the receiver of a
     * library's method or a foreach's collection, or an argument a library takes as an enumerable; or as long as
     * one of {@code whenCandidate} is a candidate, the parameters of the program's methods it is passed for and the
     * fields it is assigned to.
     */
    private record Use(boolean always, List<Object> whenCandidate) {
        boolean readOnly(Set<Object> candidates) {
            return always || whenCandidate.stream().anyMatch(candidates::contains);
        }
    }

    /**
     * The uses of the candidates in {@code code}: their reads, not the targets of assignments to them. A method's
     * result is used where it is called; a library's method on it is a read only where it adds and removes nothing,
     * since no verdict on the result says so.
     */
    private static Map<Object, List<Use>> uses(Set<Object> candidates, List<Element> code,
                                               Map<ReturnStatement, MethodInfo> returnsOf, CSharpFacts facts) {
        Map<Expression, Object> uses = new IdentityHashMap<>();
        Set<Expression> always = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Expression> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<Expression, List<Object>> whenCandidate = new IdentityHashMap<>();
        for (Element element : code) {
            if (element == null) continue;
            element.visit((Element e) -> {
                Object used = use(e, candidates);
                if (used != null) uses.put((Expression) e, used);
                if (e instanceof MethodCall mc) {
                    if (mc.object() != null && !CSharpNames.translated(mc.methodInfo().typeInfo())) {
                        // a library's method on it, not a view; a library's read, by its contract, on a view of it
                        boolean view = VIEWS.contains(mc.methodInfo().name());
                        Object receiver = use(mc.object(), candidates);
                        if (!view && receiver != null
                            && (!(receiver instanceof MethodInfo) || facts.structurallyNonModifying(mc.methodInfo()))) {
                            always.add(CSharpExpressionPrinter.unwrap(mc.object()));
                        }
                        Expression viewed = view || !facts.structurallyNonModifying(mc.methodInfo()) ? null
                                : viewOf(mc.object(), candidates);
                        if (viewed != null) always.add(viewed);
                    }
                    arguments(mc.methodInfo(), mc.parameterExpressions(), candidates, always, whenCandidate);
                }
                if (e instanceof ConstructorCall cc && cc.callsConstructorOfCreatedType()) {
                    arguments(cc.constructor(), cc.parameterExpressions(), candidates, always, whenCandidate);
                }
                if (e instanceof ForEachStatement fe && fe.expression() != null) {
                    if (use(fe.expression(), candidates) != null) {
                        always.add(CSharpExpressionPrinter.unwrap(fe.expression()));
                    }
                    Expression viewed = viewOf(fe.expression(), candidates);
                    if (viewed != null) always.add(viewed);
                }
                if (e instanceof Assignment a) {
                    if (use(a.target(), candidates) != null) targets.add(CSharpExpressionPrinter.unwrap(a.target()));
                    if (a.variableTarget() instanceof FieldReference fr && use(a.value(), candidates) != null) {
                        whenCandidate.computeIfAbsent(CSharpExpressionPrinter.unwrap(a.value()), k -> new ArrayList<>())
                                .add(fr.fieldInfo());
                    }
                }
                if (e instanceof ReturnStatement rs && returnsOf.get(rs) != null
                    && use(rs.expression(), candidates) != null) {
                    whenCandidate.computeIfAbsent(CSharpExpressionPrinter.unwrap(rs.expression()), k -> new ArrayList<>())
                            .add(returnsOf.get(rs));
                }
                return true;
            });
        }
        Map<Object, List<Use>> result = new HashMap<>();
        uses.forEach((u, c) -> {
            if (!targets.contains(u)) {
                result.computeIfAbsent(c, k -> new ArrayList<>())
                        .add(new Use(always.contains(u), whenCandidate.getOrDefault(u, List.of())));
            }
        });
        return result;
    }

    private static void arguments(MethodInfo callee, List<Expression> arguments, Set<Object> candidates,
                                  Set<Expression> always, Map<Expression, List<Object>> whenCandidate) {
        for (int i = 0; i < arguments.size(); i++) {
            if (use(arguments.get(i), candidates) == null) continue;
            Expression use = CSharpExpressionPrinter.unwrap(arguments.get(i));
            if (CSharpNames.translated(callee.typeInfo())) {
                if (i < callee.parameters().size()) {
                    whenCandidate.computeIfAbsent(use, k -> new ArrayList<>()).add(callee.parameters().get(i));
                }
            } else {
                String key = callee.typeInfo().fullyQualifiedName() + "." + (callee.isConstructor() ? "new" : callee.name());
                if (ENUMERABLE_ARGUMENTS.contains(key) || callee.overrides().stream().anyMatch(o ->
                        ENUMERABLE_ARGUMENTS.contains(o.typeInfo().fullyQualifiedName() + "." + o.name()))) {
                    always.add(use);
                }
            }
        }
    }

    /** {@code e} is a view call on the candidate, {@code map.entrySet()}: the candidate's use inside it. */
    private static Expression viewOf(Expression e, Set<Object> candidates) {
        if (!(CSharpExpressionPrinter.unwrap(e) instanceof MethodCall mc) || mc.object() == null
            || CSharpNames.translated(mc.methodInfo().typeInfo()) || !VIEWS.contains(mc.methodInfo().name())
            || use(mc.object(), candidates) == null) {
            return null;
        }
        return CSharpExpressionPrinter.unwrap(mc.object());
    }

    /** The candidate {@code e} reads, unwrapped; null when it reads none. */
    private static Object use(Element e, Set<Object> candidates) {
        if (!(e instanceof Expression x)) return null;
        Object c = candidate(CSharpExpressionPrinter.unwrap(x));
        return c != null && candidates.contains(c) ? c : null;
    }
}
