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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.NullConstant;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ForEachStatement;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.print.translate.CstSupertypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The collection parameters that are read-only in C#: {@code IReadOnlyList<T>}, {@code IReadOnlyCollection<T>},
 * {@code IReadOnlySet<T>}, {@code IReadOnlyDictionary<K, V>} for Java's {@code List}, {@code Collection}, {@code Set}
 * and {@code Map}. A caller's {@code List<T>} is one, so the calls do not change; the signature says what the
 * modification analysis proved, that the method does not modify the collection ({@link CSharpFacts#unmodified}).
 * <p>
 * The C# has to compile whatever the analysis's precision, so the parameter's uses decide too: each is a call of a
 * library method on it, the collection of a {@code foreach}, or an argument where any enumerable is accepted (another
 * read-only parameter, a library constructor or method that copies it). A use that stores or returns it keeps the
 * parameter as Java declares it. Every method of an override family has the parameter read-only, or none does; a
 * delegate's, a functional interface's method (its lambda adapter passes the argument on), a record's and a method
 * that a method reference names are not.
 * <p>
 * The callers have to compile too. Java's {@code List} family prints as the class {@code List<T>}, which implements
 * the read-only interfaces, but {@code Map}, {@code Set} and {@code Collection} print as {@code IDictionary},
 * {@code ISet} and {@code ICollection}, which do not. Each argument a call passes must be of a class that implements
 * the parameter's read-only interface ({@link #IMPLEMENTING}), {@code null}, or a read-only parameter itself.
 */
final class CSharpReadOnly {

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

    static Set<ParameterInfo> analyze(CSharpProgramIndex index, CSharpFacts facts, Set<TypeInfo> delegates) {
        if (facts == CSharpFacts.NONE) return Set.of();
        Set<ParameterInfo> candidates = new HashSet<>();
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
        }
        // what each candidate's method does with it: one walk of its body
        Map<ParameterInfo, List<Use>> uses = new HashMap<>();
        for (ParameterInfo p : candidates) uses.put(p, uses(p));
        // the uses, then the families, until nothing changes
        boolean changed = true;
        while (changed) {
            changed = false;
            for (ParameterInfo p : List.copyOf(candidates)) {
                if (!uses.get(p).stream().allMatch(u -> u.readOnly(candidates)) || !familyAgrees(p, candidates, familyOf)
                    || !callersPass(p, passed(index, p), candidates)) {
                    candidates.remove(p);
                    changed = true;
                }
            }
        }
        return Set.copyOf(candidates);
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

    /** Each argument the program passes for {@code p} converts to its read-only interface. */
    private static boolean callersPass(ParameterInfo p, List<Expression> arguments, Set<ParameterInfo> candidates) {
        String readOnly = readOnlyName(p.parameterizedType());
        Set<String> implementing = IMPLEMENTING.get(readOnly);
        for (Expression argument : arguments) {
            Expression e = CSharpExpressionPrinter.unwrap(argument);
            if (e instanceof NullConstant) continue;
            if (e instanceof VariableExpression ve && ve.variable() instanceof ParameterInfo other
                && candidates.contains(other) && converts(readOnlyName(other.parameterizedType()), readOnly)) {
                continue;
            }
            if (!implementing(e.parameterizedType(), implementing)) return false;
        }
        return true;
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
    private static boolean familyAgrees(ParameterInfo p, Set<ParameterInfo> candidates,
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
     * A use of a candidate in its method, read-only as the class's description has it: {@code always}, as the receiver
     * of a library's method or a foreach's collection, or an argument a library takes as an enumerable; or as long as
     * one of {@code whenCandidate} is a candidate, the parameters of the program's methods it is passed for.
     */
    private record Use(boolean always, List<ParameterInfo> whenCandidate) {
        boolean readOnly(Set<ParameterInfo> candidates) {
            return always || whenCandidate.stream().anyMatch(candidates::contains);
        }
    }

    /** The uses of {@code p} in its method. */
    private static List<Use> uses(ParameterInfo p) {
        MethodInfo m = p.methodInfo();
        if (m.methodBody() == null) return List.of();
        List<Expression> uses = new ArrayList<>();
        Set<Expression> always = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<Expression, List<ParameterInfo>> whenCandidate = new IdentityHashMap<>();
        m.methodBody().visit((Element e) -> {
            if (e instanceof VariableExpression ve && p.equals(ve.variable())) uses.add(ve);
            if (e instanceof MethodCall mc) {
                if (mc.object() != null && isUse(mc.object(), p) && !CSharpNames.translated(mc.methodInfo().typeInfo())) {
                    always.add(CSharpExpressionPrinter.unwrap(mc.object()));
                }
                arguments(mc.methodInfo(), mc.parameterExpressions(), p, always, whenCandidate);
            }
            if (e instanceof ConstructorCall cc && cc.constructor() != null) {
                arguments(cc.constructor(), cc.parameterExpressions(), p, always, whenCandidate);
            }
            if (e instanceof ForEachStatement fe && fe.expression() != null && isUse(fe.expression(), p)) {
                always.add(CSharpExpressionPrinter.unwrap(fe.expression()));
            }
            return true;
        });
        return uses.stream().map(u -> new Use(always.contains(u), whenCandidate.getOrDefault(u, List.of()))).toList();
    }

    private static void arguments(MethodInfo callee, List<Expression> arguments, ParameterInfo p, Set<Expression> always,
                                  Map<Expression, List<ParameterInfo>> whenCandidate) {
        for (int i = 0; i < arguments.size(); i++) {
            if (!isUse(arguments.get(i), p)) continue;
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

    private static boolean isUse(Expression e, ParameterInfo p) {
        return CSharpExpressionPrinter.unwrap(e) instanceof VariableExpression ve && p.equals(ve.variable());
    }
}
