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
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation;
import io.codelaser.maddi.cst.print.translate.CstQueries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the whole-program analyses of {@link CSharpProgram} ask of the program, gathered in one pass over its types and
 * one walk of its code: the methods that override one another, the methods a method reference names, and the
 * arguments of each call.
 *
 * @param types      the program's types, the nested, local and anonymous ones too
 * @param overrides  each method of the program, and each method one overrides, to the methods it overrides or that
 *                   override it, directly; not itself
 * @param families   the methods that override one another, connected: the families of two or more
 * @param referenced the methods a method reference names
 * @param calls      each method or constructor called, to the arguments of its calls ({@code this(…)} and
 *                   {@code super(…)} too)
 */
record CSharpProgramIndex(List<TypeInfo> types, Map<MethodInfo, Set<MethodInfo>> overrides,
                          List<List<MethodInfo>> families, Set<MethodInfo> referenced,
                          Map<MethodInfo, List<List<Expression>>> calls) {

    static CSharpProgramIndex of(List<TypeInfo> all) {
        Map<MethodInfo, Set<MethodInfo>> overrides = new HashMap<>();
        for (TypeInfo t : all) {
            for (MethodInfo m : t.methods()) {
                for (MethodInfo o : m.overrides()) {
                    if (o == m) continue;
                    overrides.computeIfAbsent(m, x -> new HashSet<>()).add(o);
                    overrides.computeIfAbsent(o, x -> new HashSet<>()).add(m);
                }
            }
        }
        Set<MethodInfo> referenced = new HashSet<>();
        Map<MethodInfo, List<List<Expression>>> calls = new HashMap<>();
        for (TypeInfo t : all) {
            for (Element element : CstQueries.code(t)) {
                element.visit((Element e) -> {
                    switch (e) {
                        case MethodReference mr -> referenced.add(mr.methodInfo());
                        case MethodCall mc -> call(calls, mc.methodInfo(), mc.parameterExpressions());
                        case ConstructorCall cc when cc.constructor() != null ->
                                call(calls, cc.constructor(), cc.parameterExpressions());
                        case ExplicitConstructorInvocation eci ->
                                call(calls, eci.methodInfo(), eci.parameterExpressions());
                        default -> {
                        }
                    }
                    return true;
                });
            }
        }
        return new CSharpProgramIndex(List.copyOf(all), overrides, families(all), referenced, calls);
    }

    private static void call(Map<MethodInfo, List<List<Expression>>> calls, MethodInfo callee, List<Expression> arguments) {
        if (callee != null) calls.computeIfAbsent(callee, k -> new ArrayList<>()).add(arguments);
    }

    /** The methods that override {@code m} or that it overrides, directly. */
    Set<MethodInfo> overrides(MethodInfo m) {
        return overrides.getOrDefault(m, Set.of());
    }

    /** The arguments of each call of {@code callee} in the program. */
    List<List<Expression>> calls(MethodInfo callee) {
        return calls.getOrDefault(callee, List.of());
    }

    private static List<List<MethodInfo>> families(List<TypeInfo> all) {
        // union-find: each method to one of its family, up to the family's root
        Map<MethodInfo, MethodInfo> parent = new HashMap<>();
        Set<MethodInfo> members = new HashSet<>();
        for (TypeInfo t : all) {
            for (MethodInfo m : t.methods()) {
                for (MethodInfo o : m.overrides()) {
                    if (o == m) continue;
                    members.add(m);
                    members.add(o);
                    parent.put(root(parent, m), root(parent, o));
                }
            }
        }
        Map<MethodInfo, List<MethodInfo>> byRoot = new HashMap<>();
        for (MethodInfo m : members) byRoot.computeIfAbsent(root(parent, m), k -> new ArrayList<>()).add(m);
        return List.copyOf(byRoot.values());
    }

    private static MethodInfo root(Map<MethodInfo, MethodInfo> parent, MethodInfo m) {
        MethodInfo root = m;
        for (MethodInfo p = parent.get(root); p != null && p != root; p = parent.get(root)) root = p;
        // the path to the root, shortened for the next time
        for (MethodInfo x = m; x != root; ) {
            MethodInfo next = parent.get(x);
            parent.put(x, root);
            x = next;
        }
        return root;
    }
}
