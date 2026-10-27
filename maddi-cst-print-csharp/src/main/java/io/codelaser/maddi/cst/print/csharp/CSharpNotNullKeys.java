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
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.info.TypeParameter;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The type parameters that are {@code where T : notnull} in a nullable context. C#'s Dictionary, SortedDictionary and
 * OrderedDictionary want a key type that is not null (CS8714); Java's HashMap, TreeMap and LinkedHashMap, which they
 * print as, take any. A type parameter that is such a map's key is not null, and so is one that is passed where such a
 * type parameter is expected, through a wildcard too ({@code ? extends K} is {@code K} in C#): a generic type's
 * argument (its supertypes' too), a generic method's argument as the method's parameters receive it, its result and
 * type arguments as the call has them, which C# gets written where it cannot infer them. An override
 * family shares its type parameters' constraints, which C# lets only the first declaration state. {@code Map} itself
 * is {@code IDictionary}, which takes any key; the result of {@code Map.of()}, {@code ofEntries} and {@code copyOf}
 * is a Dictionary.
 */
final class CSharpNotNullKeys {

    /** The Java maps whose C# counterpart wants a key that is not null (CSharpBcl's types). */
    private static final Set<String> NOT_NULL_KEYS = Set.of("java.util.HashMap", "java.util.LinkedHashMap",
            "java.util.TreeMap", "java.util.SortedMap", "java.util.NavigableMap", "java.util.SequencedMap",
            "java.util.EnumMap", "java.util.IdentityHashMap", "java.util.WeakHashMap",
            "java.util.concurrent.ConcurrentHashMap", "java.util.concurrent.ConcurrentSkipListMap");

    /** The JDK's factories of a Map that CSharpBcl prints as a Dictionary, {@code Map.of()}: its result's key. */
    private static final Set<String> DICTIONARY_FACTORIES = Set.of("java.util.Map.of", "java.util.Map.ofEntries",
            "java.util.Map.copyOf");

    /** flagged(key) implies flagged(each value): the type parameter passed where the key is expected. */
    private final Map<TypeParameter, Set<TypeParameter>> passedTo = new HashMap<>();
    private final Set<TypeParameter> seeds = new HashSet<>();

    private CSharpNotNullKeys() {
    }

    static Set<TypeParameter> analyze(List<TypeInfo> all) {
        CSharpNotNullKeys a = new CSharpNotNullKeys();
        for (TypeInfo t : all) a.type(t);
        return a.closure();
    }

    private void type(TypeInfo t) {
        if (t.parentClass() != null) use(t.parentClass());
        t.interfacesImplemented().forEach(this::use);
        for (FieldInfo f : t.fields()) {
            use(f.type());
            if (f.initializer() != null && !f.initializer().isEmpty()) body(f.initializer());
        }
        Stream.concat(t.methods().stream(), t.constructors().stream()).forEach(this::method);
    }

    private void method(MethodInfo m) {
        if (!m.isConstructor()) use(m.returnType());
        m.parameters().forEach(p -> use(p.parameterizedType()));
        // an override family: one declaration's constraints are all of theirs
        for (MethodInfo o : m.overrides()) {
            if (o == m || o.typeParameters().size() != m.typeParameters().size()) continue;
            for (int i = 0; i < m.typeParameters().size(); i++) {
                link(m.typeParameters().get(i), o.typeParameters().get(i));
                link(o.typeParameters().get(i), m.typeParameters().get(i));
            }
        }
        if (m.methodBody() != null) body(m.methodBody());
    }

    private void body(Element body) {
        body.visit(e -> {
            if (e instanceof Expression ex) {
                if (ex.parameterizedType() != null) use(ex.parameterizedType());
                if (ex instanceof MethodCall mc) {
                    call(mc.methodInfo(), mc.parameterExpressions());
                    typeArguments(mc);
                    dictionaryFactory(mc);
                }
                else if (ex instanceof ConstructorCall cc && cc.constructor() != null) {
                    call(cc.constructor(), cc.parameterExpressions());
                }
            } else if (e instanceof LocalVariableCreation lvc) {
                lvc.localVariableStream().forEach(lv -> use(lv.parameterizedType()));
            }
            return true;
        });
    }

    /** {@code Map.of()} of a {@code Map<K, V>}: C#'s {@code new Dictionary<K, V>}, whose K is not null. */
    private void dictionaryFactory(MethodCall mc) {
        MethodInfo m = mc.methodInfo();
        if (m == null || !DICTIONARY_FACTORIES.contains(m.typeInfo().fullyQualifiedName() + "." + m.name())) return;
        ParameterizedType result = mc.parameterizedType();
        if (result != null && !result.parameters().isEmpty() && plain(result.parameters().getFirst())) {
            seeds.add(result.parameters().getFirst().typeParameter());
        }
    }

    /** A generic method's type parameters as its arguments give them. */
    private void call(MethodInfo method, List<Expression> arguments) {
        if (method == null || method.typeParameters().isEmpty()) return;
        List<ParameterInfo> parameters = method.parameters();
        for (int i = 0; i < arguments.size() && i < parameters.size(); i++) {
            ParameterizedType argument = arguments.get(i).parameterizedType();
            if (argument != null) bind(parameters.get(i).parameterizedType(), argument);
        }
    }

    /**
     * A generic method's type parameters as the call's result and explicit type arguments give them: C# gets them
     * written where it cannot infer them, {@code GetFromSolution<Value_>(descriptor, solution, null)}, whose
     * {@code Value_} is the caller's.
     */
    private void typeArguments(MethodCall mc) {
        MethodInfo method = mc.methodInfo();
        if (method == null || method.typeParameters().isEmpty()) return;
        if (mc.concreteReturnType() != null) bind(method.returnType(), mc.concreteReturnType());
        List<ParameterizedType> explicit = mc.typeArguments();
        if (explicit.size() == method.typeParameters().size()) {
            for (int i = 0; i < explicit.size(); i++) {
                if (plain(explicit.get(i))) link(method.typeParameters().get(i), explicit.get(i).typeParameter());
            }
        }
    }

    private void bind(ParameterizedType formal, ParameterizedType actual) {
        if (formal.isTypeParameter() && formal.arrays() == actual.arrays()) {
            if (plain(actual)) link(formal.typeParameter(), actual.typeParameter());
            return;
        }
        if (formal.typeInfo() != null && formal.typeInfo() == actual.typeInfo()
            && formal.parameters().size() == actual.parameters().size()) {
            for (int i = 0; i < formal.parameters().size(); i++) bind(formal.parameters().get(i), actual.parameters().get(i));
        }
    }

    /** A type use: a map's key, and a generic program type's arguments, all the way down. */
    private void use(ParameterizedType pt) {
        if (pt == null) return;
        TypeInfo t = pt.typeInfo();
        List<ParameterizedType> arguments = pt.parameters();
        if (t != null && !arguments.isEmpty()) {
            ParameterizedType key = arguments.getFirst();
            if (NOT_NULL_KEYS.contains(t.fullyQualifiedName()) && plain(key)) seeds.add(key.typeParameter());
            List<TypeParameter> declared = t.typeParameters();
            if (declared.size() == arguments.size()) {
                for (int i = 0; i < arguments.size(); i++) {
                    if (plain(arguments.get(i))) link(declared.get(i), arguments.get(i).typeParameter());
                }
            }
        }
        arguments.forEach(this::use);
    }

    /** {@code T}, {@code ? extends T}, {@code ? super T}: C# has {@code T} for each. */
    private static boolean plain(ParameterizedType pt) {
        return pt.isTypeParameter() && pt.typeParameter() != null && pt.arrays() == 0;
    }

    private void link(TypeParameter expected, TypeParameter passed) {
        if (expected != passed) passedTo.computeIfAbsent(expected, k -> new HashSet<>()).add(passed);
    }

    private Set<TypeParameter> closure() {
        Set<TypeParameter> flagged = new HashSet<>(seeds);
        Deque<TypeParameter> work = new ArrayDeque<>(seeds);
        while (!work.isEmpty()) {
            for (TypeParameter next : passedTo.getOrDefault(work.pop(), Set.of())) {
                if (flagged.add(next)) work.push(next);
            }
        }
        return Set.copyOf(flagged);
    }
}
