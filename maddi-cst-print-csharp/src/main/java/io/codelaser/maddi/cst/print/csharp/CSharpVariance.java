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

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

/**
 * The variance C# declares on the type parameters of the program's interfaces and delegates: {@code out} for a
 * parameter that occurs in output positions only (a method's result, the element of an {@code IEnumerable}, the
 * result of a {@code Func}), {@code in} for input positions only (a method's parameter, the argument of an
 * {@code Action}); neither where it occurs in both, or in an invariant position (a {@code List<T>}, a program class's
 * argument, a method type parameter's bound). Java has use-site variance, the wildcards; C# has declaration-site
 * variance, which makes {@code IMapper<Sub, object>} an {@code IMapper<Base, string>} without a cast, and
 * {@code Function<? extends A, ?>} simply {@code IMapper<A, object>}.
 * <p>
 * Positions compose: {@code T} in {@code Func<T>} returned is an output, in {@code Action<T>} returned an input, in
 * {@code Func<T>} taken an input. A library type's variance is the BCL's ({@link #LIBRARY}, by Java name); a program
 * interface's is this analysis's own, a fixpoint over the interfaces that mention one another; a program class's
 * arguments are invariant. Static members do not count (C# does not check them), nor a supertype that is not generic
 * in the parameter. A variant interface may declare no nested type (CS8427): the lambda and erasure adapters are its
 * siblings, and an interface with a nested type of its own declares no variance.
 */
final class CSharpVariance {

    /** The library's generic types C# has variant, by Java name: per parameter, {@code +} out, {@code -} in, {@code 0} invariant. */
    private static final Map<String, String> LIBRARY = Map.ofEntries(
            Map.entry("java.util.function.Function", "-+"),
            Map.entry("java.util.function.BiFunction", "--+"),
            Map.entry("java.util.function.Supplier", "+"),
            Map.entry("java.util.function.Consumer", "-"),
            Map.entry("java.util.function.BiConsumer", "--"),
            Map.entry("java.util.function.Predicate", "-"),
            Map.entry("java.util.function.BiPredicate", "--"),
            Map.entry("java.util.function.IntFunction", "+"),
            Map.entry("java.util.function.LongFunction", "+"),
            Map.entry("java.util.function.DoubleFunction", "+"),
            Map.entry("java.util.function.ToIntFunction", "-"),
            Map.entry("java.util.function.ToLongFunction", "-"),
            Map.entry("java.util.function.ToDoubleFunction", "-"),
            Map.entry("java.util.function.ToIntBiFunction", "--"),
            Map.entry("java.util.function.ToLongBiFunction", "--"),
            Map.entry("java.util.function.ToDoubleBiFunction", "--"),
            Map.entry("java.util.Optional", "+"),
            Map.entry("java.lang.Iterable", "+"),
            Map.entry("java.util.Iterator", "+"),
            Map.entry("java.util.stream.Stream", "+"),
            Map.entry("java.util.Comparator", "-"),
            Map.entry("java.lang.Comparable", "-"));

    private CSharpVariance() {
    }

    /**
     * Per type parameter of a translated interface or delegate of {@code all}: {@code "out"}, {@code "in"}, or
     * absent where C# declares none.
     */
    static Map<TypeParameter, String> analyze(List<TypeInfo> all, Set<TypeInfo> delegates) {
        // what each parameter may still be: + out, - in; both at first, nothing once an invariant position has it
        Map<TypeParameter, Set<Character>> allowed = new HashMap<>();
        List<TypeInfo> types = new ArrayList<>();
        for (TypeInfo t : all) {
            if (!t.isInterface() || t.typeNature().isAnnotation() || !CSharpNames.translated(t)
                || t.typeParameters().isEmpty()) {
                continue;
            }
            types.add(t);
            // an interface with a nested type (CS8427) declares no variance
            boolean nested = t.subTypes().stream().anyMatch(st -> !st.isSynthetic() && !CSharpNames.hoisted(st))
                             || !CSharpAnonymous.in(t).isEmpty();
            if (nested) continue;
            t.typeParameters().forEach(tp -> allowed.put(tp, new HashSet<>(Set.of('+', '-'))));
        }
        // first optimistically, a reference through a parameter not yet decided restricting nothing; then strictly,
        // where such a parameter is the invariant one it prints as: each pass only narrows, so both end
        for (boolean strict : new boolean[] { false, true }) {
            boolean changed = true;
            while (changed) {
                changed = false;
                for (TypeInfo t : types) {
                    if (!allowed.containsKey(t.typeParameters().getFirst())) continue;
                    Walk walk = new Walk(Set.copyOf(t.typeParameters()), allowed, strict);
                    if (delegates.contains(t)) {
                        MethodInfo sam = CSharpProgram.invoked(t);
                        walk.occurs(sam.returnType(), '+');
                        for (ParameterInfo p : sam.parameters()) walk.occurs(p.parameterizedType(), '-');
                    } else {
                        t.interfacesImplemented().forEach(s -> walk.occurs(s, '+'));
                        for (MethodInfo m : t.methods()) {
                            if (m.isStatic() || m.isSynthetic()) continue;
                            walk.occurs(m.returnType(), '+');
                            for (ParameterInfo p : m.parameters()) walk.occurs(p.parameterizedType(), '-');
                            for (TypeParameter mtp : m.typeParameters()) {
                                mtp.typeBounds().forEach(b -> walk.occurs(b, '0'));
                            }
                        }
                    }
                    changed |= walk.changed;
                }
            }
        }
        Map<TypeParameter, String> variance = new HashMap<>();
        allowed.forEach((tp, may) -> {
            if (may.equals(Set.of('+'))) variance.put(tp, "out");
            else if (may.equals(Set.of('-'))) variance.put(tp, "in");
        });
        return Map.copyOf(variance);
    }

    /** The occurrences of one interface's type parameters in a type, with their polarity. */
    private static final class Walk {
        private final Set<TypeParameter> own;
        private final Map<TypeParameter, Set<Character>> allowed;
        private final boolean strict;
        boolean changed;

        Walk(Set<TypeParameter> own, Map<TypeParameter, Set<Character>> allowed, boolean strict) {
            this.own = own;
            this.allowed = allowed;
            this.strict = strict;
        }

        /** {@code pt} at a position of {@code polarity}: + output, - input, 0 invariant, * undecided (restricts nothing). */
        void occurs(ParameterizedType pt, char polarity) {
            if (pt == null || polarity == '*') return;
            if (pt.arrays() > 0) {
                // C# lets an out T be a T[] result and an in T a T[] parameter
                occurs(pt.copyWithoutArrays(), polarity);
                return;
            }
            if (pt.isTypeParameter()) {
                TypeParameter tp = pt.typeParameter();
                if (tp != null && own.contains(tp)) restrict(tp, polarity);
                return;
            }
            TypeInfo t = pt.typeInfo();
            if (t == null || pt.parameters().isEmpty() || CSharpTypeName.dropsArguments(t)) return;
            boolean translated = CSharpNames.translated(t);
            String library = translated ? null : LIBRARY.get(t.fullyQualifiedName());
            List<TypeParameter> parameters = t.typeParameters();
            for (int i = 0; i < pt.parameters().size(); i++) {
                ParameterizedType a = pt.parameters().get(i);
                char of;
                if (translated && t.isInterface() && i < parameters.size() && allowed.containsKey(parameters.get(i))) {
                    Set<Character> may = allowed.get(parameters.get(i));
                    of = may.equals(Set.of('+')) ? '+' : may.equals(Set.of('-')) ? '-' : may.isEmpty() || strict ? '0' : '*';
                } else if (library != null && i < library.length()) {
                    of = library.charAt(i);
                } else {
                    of = '0';
                }
                // an invariant position stays one through any argument
                char composed = polarity == '0' ? '0' : switch (of) {
                    case '+' -> polarity;
                    case '-' -> polarity == '+' ? '-' : '+';
                    case '*' -> '*';
                    default -> '0';
                };
                if (a.wildcard() != null) {
                    // ? extends T prints as its bound, T; an unbound ? mentions nothing
                    if (a.typeInfo() == null && a.typeParameter() == null) continue;
                    occurs(a.withWildcard(null), composed);
                } else {
                    occurs(a, composed);
                }
            }
        }

        private void restrict(TypeParameter tp, char polarity) {
            Set<Character> may = allowed.get(tp);
            int before = may.size();
            switch (polarity) {
                case '+' -> may.retainAll(Set.of('+'));
                case '-' -> may.retainAll(Set.of('-'));
                case '0' -> may.clear();
                default -> {
                }
            }
            changed |= may.size() < before;
        }
    }
}
