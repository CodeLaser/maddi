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
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.print.translate.CstQueries;
import io.codelaser.maddi.cst.print.translate.CstSupertypes;

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
    }

    private CSharpWildcards() {
    }

    /** {@code q} null: the names only, without the constraints. */
    static Captured captured(MethodInfo method, Qualification q) {
        if (!mayBeGeneric(method)) return Captured.NONE;
        Set<String> taken = new HashSet<>();
        for (TypeInfo t = method.typeInfo(); t != null; t = CstQueries.enclosing(t)) {
            t.typeParameters().forEach(tp -> taken.add(CSharpNames.typeParameter(tp)));
        }
        method.typeParameters().forEach(tp -> taken.add(CSharpNames.typeParameter(tp)));
        List<String> typeParameters = new ArrayList<>();
        Map<Integer, List<String>> names = new HashMap<>();
        List<String> constraints = new ArrayList<>();
        for (int i = 0; i < method.parameters().size(); i++) {
            ParameterizedType pt = method.parameters().get(i).parameterizedType();
            // a type whose wildcard interface the wildcard is: the parameter is that, which the code may pass (a
            // List<AbstractDatasetInstance<Solution_, ?>>'s element); not beside an overload, which C# would find as
            // specific as the generic one (CS0121), where Java picks the more specific wildcard
            if (!capturable(pt) || CSharpWildcardInterfaces.isWildcardUse(pt) && !overloaded(method)) continue;
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
                List<ParameterizedType> bounds = bounds(declared, a);
                if (!constrainable(bounds)) ok = false;
                // the generic type's own is a dictionary's key (CSharpNotNullKeys): the capture is, as it goes there
                boolean notNull = q != null && CSharpContext.program().notNullKey(declared)
                                  && bounds.stream().noneMatch(b -> b.typeInfo() != null && !b.typeInfo().isInterface());
                if ((!bounds.isEmpty() || notNull) && q != null) {
                    List<String> parts = new ArrayList<>();
                    if (notNull) parts.add("notnull");
                    bounds.forEach(b -> parts.add(CSharpTypeName.of(b, q)));
                    forConstraints.add("where " + unique + " : " + String.join(", ", parts));
                }
            }
            if (!ok) continue;
            typeParameters.addAll(forParameter);
            names.put(i, forParameter);
            constraints.addAll(forConstraints);
        }
        return typeParameters.isEmpty() ? Captured.NONE : new Captured(typeParameters, names, constraints);
    }

    /**
     * A method that may have type parameters of its own: not a constructor, a functional interface's method or an
     * override of a library method, and with a capturable parameter.
     */
    private static boolean mayBeGeneric(MethodInfo method) {
        if (method.isConstructor()) return false;
        if (method.parameters().stream().noneMatch(p -> capturable(p.parameterizedType()))) return false;
        for (MethodInfo m : overriddenAndSelf(method)) {
            if (!CSharpNames.translated(m.typeInfo())) return false;
            if (m.typeInfo().isInterface() && CstSupertypes.singleAbstractMethod(m.typeInfo()) == m) return false;
        }
        return true;
    }

    /**
     * The method has an overload of the same arity: in the type that declares what it overrides, whose decision an
     * override follows (an abstract class's private {@code groupBy(keyA, keyB)} must not make its
     * {@code groupBy(key, collector)} capture where the interface's prints the wildcard interface, CS0535).
     */
    private static boolean overloaded(MethodInfo method) {
        List<MethodInfo> overridden = method.overrides().stream()
                .filter(o -> o != method && CSharpNames.translated(o.typeInfo())).toList();
        if (!overridden.isEmpty()) return overridden.stream().anyMatch(CSharpWildcards::overloadedInOwnType);
        return overloadedInOwnType(method);
    }

    private static boolean overloadedInOwnType(MethodInfo method) {
        return method.typeInfo().methods().stream().anyMatch(m -> m != method && m.name().equals(method.name())
                                                                   && m.parameters().size() == method.parameters().size());
    }

    /** Whether {@link #captured} names the wildcards of the method's parameter: it prints no wildcard of its own. */
    static boolean capturedParameter(MethodInfo method, int index) {
        if (!mayBeGeneric(method)) return false;
        ParameterizedType pt = method.parameters().get(index).parameterizedType();
        if (!capturable(pt)) return false;
        for (int k = 0; k < pt.parameters().size(); k++) {
            ParameterizedType a = pt.parameters().get(k);
            if (a.wildcard() != null && !constrainable(bounds(pt.typeInfo().typeParameters().get(k), a))) return false;
        }
        return true;
    }

    /** The expression is a parameter of the method being printed whose wildcards are captured. */
    static boolean capturedParameterUse(Expression e) {
        MethodInfo method = CSharpContext.currentMethod();
        return method != null && CSharpExpressionPrinter.unwrap(e) instanceof VariableExpression ve
               && ve.variable() instanceof ParameterInfo pi && pi.methodInfo() == method
               && capturedParameter(method, pi.index());
    }

    /** A captured wildcard's constraints: its own bound, else the type parameter's. */
    private static List<ParameterizedType> bounds(TypeParameter declared, ParameterizedType a) {
        return a.wildcard().isExtends() && !a.isJavaLangObject() ? List.of(a.withWildcard(null))
                : declared.typeBounds().stream().filter(b -> !b.isJavaLangObject()).toList();
    }

    private static boolean constrainable(List<ParameterizedType> bounds) {
        // a bound in the generic type's own type parameters (R extends Result<R>) is not expressible here; a
        // constraint is an interface or a class C# can extend, not string, a delegate or a sealed class
        return bounds.stream().noneMatch(b -> CstQueries.mentionsTypeParameter(b) || b.typeInfo() == null
                || !CSharpNames.translated(b.typeInfo()) || CSharpContext.program().delegate(b.typeInfo())
                || !b.typeInfo().isInterface() && !CSharpContext.program().open(b.typeInfo()));
    }

    /** A parameter of the method may be captured: the explicit implementations leave such a method alone. */
    static boolean mayCapture(MethodInfo method) {
        return method.parameters().stream().anyMatch(p -> capturable(p.parameterizedType()));
    }

    /**
     * {@code X<?>}, {@code X<? extends B>} of a generic type of the program. Captured even where the type's variance
     * ({@link CSharpVariance}) would take the bound: an argument that is a type parameter has no variance conversion
     * (CS1503), a captured one takes it.
     */
    static boolean capturable(ParameterizedType pt) {
        TypeInfo t = pt.typeInfo();
        return pt.arrays() == 0 && pt.wildcard() == null && t != null
               && (CSharpNames.translated(t) || capturableLibrary(pt))
               && !pt.parameters().isEmpty() && pt.parameters().size() == t.typeParameters().size()
               && pt.parameters().stream().anyMatch(a -> a.wildcard() != null && !a.wildcard().isSuper())
               && pt.parameters().stream().noneMatch(a -> a.wildcard() != null && a.wildcard().isSuper());
    }

    /**
     * {@code Collection<?>}, {@code Set<?>}: C#'s non-generic {@code ICollection} is no {@code HashSet<T>}'s,
     * {@code ICollection<T>}'s nor {@code ISet<T>}'s; captured, {@code ICollection<TE>} is every collection's.
     */
    private static boolean capturableLibrary(ParameterizedType pt) {
        return CAPTURED_LIBRARY.contains(pt.typeInfo().fullyQualifiedName()) && pt.parameters().size() == 1
               && pt.parameters().getFirst().wildcard() != null && pt.parameters().getFirst().wildcard().isUnbound();
    }

    private static final Set<String> CAPTURED_LIBRARY = Set.of("java.util.Collection", "java.util.Set");

    /** {@code X<TEmbedded>}: the parameter's type with its wildcards named. */
    static String type(ParameterizedType pt, List<String> names, Qualification q) {
        List<String> printed = new ArrayList<>();
        int n = 0;
        for (ParameterizedType a : pt.parameters()) {
            printed.add(a.wildcard() != null ? names.get(n++) : CSharpTypeName.of(a, q));
        }
        return CSharpTypeName.name(pt.typeInfo(), q) + "<" + String.join(", ", printed) + ">";
    }


    private static List<MethodInfo> overriddenAndSelf(MethodInfo method) {
        List<MethodInfo> list = new ArrayList<>(method.overrides());
        if (!list.contains(method)) list.add(method);
        return list;
    }
}
