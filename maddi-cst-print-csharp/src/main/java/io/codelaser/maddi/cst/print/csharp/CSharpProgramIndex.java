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
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.inspection.api.util.MethodFamilies;
import io.codelaser.maddi.inspection.api.util.MethodUses;

/**
 * What the whole-program analyses of {@link CSharpProgram} ask of the program, gathered in one pass over its types and
 * one walk of its code: the methods that override one another, the methods a method reference names, and the
 * arguments of each call.
 *
 * @param types      the program's types, the nested, local and anonymous ones too
 * @param overrides  each method of the program, and each method one overrides, to the methods it overrides or that
 *                   override it, directly; not itself
 * @param families   the methods that override one another, connected, with a superclass's method that implements a
 *                   subclass's interface method ({@link MethodFamilies}): the families of two or more
 * @param referenced the methods a method reference names
 * @param calls      each method or constructor called, to the arguments of its calls ({@code this(…)} and
 *                   {@code super(…)} too)
 */
record CSharpProgramIndex(List<TypeInfo> types, Map<MethodInfo, Set<MethodInfo>> overrides,
                          List<List<MethodInfo>> families, Set<MethodInfo> referenced,
                          Map<MethodInfo, List<List<Expression>>> calls,
                          Map<TypeInfo, List<MethodInfo>> referencedAs) {

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
        // the calls, the creations and the constructor invocations as the code writes them; a method reference
        // names a method, as the functional type it is passed as
        MethodUses uses = MethodUses.of(all);
        Set<MethodInfo> referenced = new HashSet<>(uses.methods(MethodUses.Kind.REFERENCE));
        Map<MethodInfo, List<List<Expression>>> calls = new HashMap<>();
        Map<TypeInfo, List<MethodInfo>> referencedAs = new HashMap<>();
        for (MethodInfo method : uses.methods()) {
            for (MethodUses.Use use : uses.uses(method)) {
                switch (use.kind()) {
                    case CALL, CREATION, CONSTRUCTOR_INVOCATION ->
                            calls.computeIfAbsent(method, k -> new ArrayList<>()).add(use.arguments());
                    case REFERENCE -> {
                        MethodReference mr = (MethodReference) use.element();
                        TypeInfo functional = mr.parameterizedType() == null ? null : mr.parameterizedType().typeInfo();
                        if (functional != null) {
                            referencedAs.computeIfAbsent(functional, k -> new ArrayList<>()).add(method);
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        return new CSharpProgramIndex(List.copyOf(all), overrides, MethodFamilies.of(all).families().stream().map(List::copyOf).toList(), referenced, calls,
                referencedAs);
    }

    /** The methods that override {@code m} or that it overrides, directly. */
    Set<MethodInfo> overrides(MethodInfo m) {
        return overrides.getOrDefault(m, Set.of());
    }

    /** The arguments of each call of {@code callee} in the program. */
    List<List<Expression>> calls(MethodInfo callee) {
        return calls.getOrDefault(callee, List.of());
    }
}
