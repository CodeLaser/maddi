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
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A method's parameter {@code EmbeddingStoreRequestContext<?> c} takes any context, whose type argument Java captures
 * in the body. C# has no wildcard; the method captures it as a type parameter of its own:
 * {@code void OnRequest<TEmbedded>(EmbeddingStoreRequestContext<TEmbedded> c)}. Only a type of the program is
 * captured (the BCL's types have their own mapping of wildcards), and not in a constructor (which C# does not make
 * generic), a functional interface's method (whose lambdas are not generic) or an override of a library method.
 */
final class CSharpWildcards {

    /** The captured type parameters, by parameter index the names of its wildcard arguments, and the constraints. */
    record Captured(List<String> typeParameters, Map<Integer, List<String>> names, List<String> constraints) {
        static final Captured NONE = new Captured(List.of(), Map.of(), List.of());

        boolean isEmpty() {
            return typeParameters.isEmpty();
        }
    }

    private CSharpWildcards() {
    }

    static Captured captured(MethodInfo method, Qualification q) {
        if (method.isConstructor()) return Captured.NONE;
        if (method.parameters().stream().noneMatch(p -> capturable(p.parameterizedType()))) return Captured.NONE;
        for (MethodInfo m : overriddenAndSelf(method)) {
            if (!CSharpNames.translated(m.typeInfo())) return Captured.NONE;
            if (m.typeInfo().isInterface() && CSharpNames.singleAbstractMethod(m.typeInfo()) == m) return Captured.NONE;
        }
        Set<String> taken = new HashSet<>();
        for (TypeInfo t = method.typeInfo(); t != null; t = CSharpNames.enclosing(t)) {
            t.typeParameters().forEach(tp -> taken.add(CSharpNames.typeParameter(tp)));
        }
        method.typeParameters().forEach(tp -> taken.add(CSharpNames.typeParameter(tp)));
        List<String> typeParameters = new ArrayList<>();
        Map<Integer, List<String>> names = new HashMap<>();
        List<String> constraints = new ArrayList<>();
        for (int i = 0; i < method.parameters().size(); i++) {
            ParameterizedType pt = method.parameters().get(i).parameterizedType();
            if (!capturable(pt)) continue;
            TypeInfo generic = pt.typeInfo();
            List<String> forParameter = new ArrayList<>();
            List<String> forConstraints = new ArrayList<>();
            boolean ok = true;
            for (int k = 0; k < pt.parameters().size() && ok; k++) {
                ParameterizedType a = pt.parameters().get(k);
                if (a.wildcard() == null) continue;
                TypeParameter declared = generic.typeParameters().get(k);
                String base = declared.simpleName();
                String name = base.startsWith("T") && (base.length() == 1 || Character.isUpperCase(base.charAt(1)))
                        ? base : "T" + base;
                String unique = name;
                for (int n = 2; taken.contains(unique); n++) unique = name + n;
                taken.add(unique);
                forParameter.add(unique);
                List<ParameterizedType> bounds = a.wildcard().isExtends() && !a.isJavaLangObject()
                        ? List.of(a.withWildcard(null))
                        : declared.typeBounds().stream().filter(b -> !b.isJavaLangObject()).toList();
                // a bound in the generic type's own type parameters (R extends Result<R>) is not expressible here
                if (bounds.stream().anyMatch(CSharpWildcards::mentionsTypeParameter)) ok = false;
                // a constraint is an interface or a class C# can extend, not string, a delegate or a sealed class
                if (bounds.stream().anyMatch(b -> b.typeInfo() == null || !CSharpNames.translated(b.typeInfo())
                        || CSharpContext.program().delegate(b.typeInfo())
                        || !b.typeInfo().isInterface() && !CSharpContext.program().open(b.typeInfo()))) ok = false;
                if (!bounds.isEmpty()) {
                    forConstraints.add("where " + unique + " : " + String.join(", ",
                            bounds.stream().map(b -> CSharpTypeName.of(b, q)).toList()));
                }
            }
            if (!ok) continue;
            typeParameters.addAll(forParameter);
            names.put(i, forParameter);
            constraints.addAll(forConstraints);
        }
        return typeParameters.isEmpty() ? Captured.NONE : new Captured(typeParameters, names, constraints);
    }

    /** A parameter of the method may be captured: the explicit implementations leave such a method alone. */
    static boolean mayCapture(MethodInfo method) {
        return method.parameters().stream().anyMatch(p -> capturable(p.parameterizedType()));
    }

    /** {@code X<?>}, {@code X<? extends B>} of a generic type of the program. */
    static boolean capturable(ParameterizedType pt) {
        TypeInfo t = pt.typeInfo();
        return pt.arrays() == 0 && pt.wildcard() == null && t != null && CSharpNames.translated(t)
               && !pt.parameters().isEmpty() && pt.parameters().size() == t.typeParameters().size()
               && pt.parameters().stream().anyMatch(a -> a.wildcard() != null && !a.wildcard().isSuper())
               && pt.parameters().stream().noneMatch(a -> a.wildcard() != null && a.wildcard().isSuper());
    }

    /** {@code X<TEmbedded>}: the parameter's type with its wildcards named. */
    static String type(ParameterizedType pt, List<String> names, Qualification q) {
        List<String> printed = new ArrayList<>();
        int n = 0;
        for (ParameterizedType a : pt.parameters()) {
            printed.add(a.wildcard() != null ? names.get(n++) : CSharpTypeName.of(a, q));
        }
        return CSharpTypeName.name(pt.typeInfo(), q) + "<" + String.join(", ", printed) + ">";
    }

    private static boolean mentionsTypeParameter(ParameterizedType pt) {
        return pt.isTypeParameter() || pt.parameters().stream().anyMatch(CSharpWildcards::mentionsTypeParameter);
    }

    private static List<MethodInfo> overriddenAndSelf(MethodInfo method) {
        List<MethodInfo> list = new ArrayList<>(method.overrides());
        if (!list.contains(method)) list.add(method);
        return list;
    }
}
