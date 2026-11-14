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
package io.codelaser.maddi.inspection.api.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.MethodReference;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.ExplicitConstructorInvocation;
import io.codelaser.maddi.inspection.api.parser.ParseResult;

/**
 * Where the program's code uses its methods and constructors: every place a change to a signature has to look at.
 * As {@link MethodFamilies}, an index of a set of types, built once and built again when they change.
 * <p>
 * A use is of exactly the method the code names: a call through an interface is a use of the interface's method, not
 * of its implementations. Who asks about one method of an override family asks about each of the family
 * ({@link MethodFamilies#family}).
 * <p>
 * The code looked in is every method and constructor body and every field initializer of the types given, lambda
 * bodies included. A use is found once, in the member whose code it is written in: one in an anonymous or a local
 * class's method is in that method, so the types given must include those classes
 * ({@link MethodFamilies#allTypes}, which {@link #of(ParseResult)} does).
 * <p>
 * Not here, as the code does not write them: the {@code super()} a constructor calls without saying so, and the
 * default constructor's.
 */
public final class MethodUses {

    /** What a {@link Use} is of. */
    public enum Kind {
        /** A method call, {@code a.m(x)}: the {@link MethodCall}. */
        CALL,
        /** An object creation, {@code new T(x)}: the {@link ConstructorCall}. */
        CREATION,
        /**
         * The creation of an anonymous class, {@code new T(x) { … }}, which calls a constructor of {@code T}: the
         * {@link ConstructorCall}. The CST does not say which one (the anonymous class has no constructor of its
         * own there), so the use is of every constructor of {@code T} that the number of arguments allows: one too
         * many rather than one missed, where {@code T} has overloads of one arity (CodeLaser/maddi#194).
         */
        ANONYMOUS_CREATION,
        /** {@code this(x)} or {@code super(x)} in a constructor: the {@link ExplicitConstructorInvocation}. */
        CONSTRUCTOR_INVOCATION,
        /** A method reference that names the method, {@code T::m}, {@code T::new}: the {@link MethodReference}. */
        REFERENCE,
        /**
         * A lambda or a method reference that implements the method, a functional interface's single abstract one:
         * the {@link Lambda} or the {@link MethodReference}. No {@link MethodInfo#overrides()} has this
         * implementation, and no family: a change to the method's signature changes what the lambda must be. The
         * method is the functional type's {@link TypeInfo#singleAbstractMethod()}: a library's interface that the
         * front end loaded without one has no such use.
         */
        IMPLEMENTATION
    }

    /**
     * One use.
     *
     * @param kind      what it is
     * @param method    the method or constructor used
     * @param in        the method, constructor or field whose code the use is written in
     * @param element   the expression or statement, of the type its {@link Kind} says
     * @param arguments the arguments of a call, a creation or a constructor invocation; empty for the others
     */
    public record Use(Kind kind, MethodInfo method, Info in, Element element, List<Expression> arguments) {
    }

    private final Map<MethodInfo, List<Use>> byMethod;
    private final Map<Kind, Set<MethodInfo>> byKind;

    private MethodUses(Map<MethodInfo, List<Use>> byMethod, Map<Kind, Set<MethodInfo>> byKind) {
        this.byMethod = byMethod;
        this.byKind = byKind;
    }

    /** The uses in the program's source types, the nested, local and anonymous ones too. */
    public static MethodUses of(ParseResult parseResult) {
        return of(MethodFamilies.allTypes(parseResult.primaryTypes()));
    }

    /**
     * The uses in the code of {@code types}: exactly these types are looked in, so the caller includes the nested,
     * local and anonymous ones ({@link MethodFamilies#allTypes}).
     */
    public static MethodUses of(Collection<TypeInfo> types) {
        Map<MethodInfo, List<Use>> byMethod = new LinkedHashMap<>();
        Map<Kind, Set<MethodInfo>> byKind = new EnumMap<>(Kind.class);
        for (TypeInfo t : types) {
            t.constructorAndMethodStream().filter(m -> m.methodBody() != null)
                    .forEach(m -> collect(m, m.methodBody(), byMethod, byKind));
            for (FieldInfo f : t.fields()) {
                if (f.initializer() != null && !f.initializer().isEmpty()) {
                    collect(f, f.initializer(), byMethod, byKind);
                }
            }
        }
        byMethod.replaceAll((m, uses) -> List.copyOf(uses));
        byKind.replaceAll((k, methods) -> Collections.unmodifiableSet(methods));
        return new MethodUses(byMethod, byKind);
    }

    private static void collect(Info in, Element code, Map<MethodInfo, List<Use>> byMethod,
                                Map<Kind, Set<MethodInfo>> byKind) {
        // the visit enters a lambda's body, not an anonymous or a local class's: those are types of their own
        code.visit((Element e) -> {
            switch (e) {
                case MethodCall mc -> add(Kind.CALL, mc.methodInfo(), in, mc, mc.parameterExpressions(), byMethod, byKind);
                case ConstructorCall cc -> {
                    add(Kind.CREATION, cc.constructor(), in, cc, cc.parameterExpressions(), byMethod, byKind);
                    if (!cc.callsConstructorOfCreatedType()) {
                        for (MethodInfo candidate : superConstructors(cc)) {
                            add(Kind.ANONYMOUS_CREATION, candidate, in, cc, cc.parameterExpressions(), byMethod, byKind);
                        }
                    }
                }
                case ExplicitConstructorInvocation eci -> add(Kind.CONSTRUCTOR_INVOCATION, eci.methodInfo(), in, eci,
                        eci.parameterExpressions(), byMethod, byKind);
                case MethodReference mr -> {
                    add(Kind.REFERENCE, mr.methodInfo(), in, mr, List.of(), byMethod, byKind);
                    TypeInfo functional = mr.parameterizedType() == null ? null : mr.parameterizedType().typeInfo();
                    add(Kind.IMPLEMENTATION, functional == null ? null : functional.singleAbstractMethod(), in, mr,
                            List.of(), byMethod, byKind);
                }
                case Lambda lambda -> {
                    TypeInfo functional = lambda.abstractFunctionalTypeInfo();
                    add(Kind.IMPLEMENTATION, functional == null ? null : functional.singleAbstractMethod(), in,
                            lambda, List.of(), byMethod, byKind);
                }
                default -> {
                }
            }
            return true;
        });
    }

    /** The constructors of an anonymous class's superclass that its creation's arguments can be for. */
    private static List<MethodInfo> superConstructors(ConstructorCall cc) {
        TypeInfo anonymous = cc.anonymousClass();
        TypeInfo parent = anonymous == null || anonymous.parentClass() == null ? null : anonymous.parentClass().typeInfo();
        if (parent == null) return List.of();
        int arguments = cc.parameterExpressions().size();
        return parent.constructors().stream().filter(c -> c.isVarargs()
                ? arguments >= c.parameters().size() - 1 : arguments == c.parameters().size()).toList();
    }

    private static void add(Kind kind, MethodInfo method, Info in, Element element, List<Expression> arguments,
                            Map<MethodInfo, List<Use>> byMethod, Map<Kind, Set<MethodInfo>> byKind) {
        // an anonymous class's creation has no constructor in the CST; a type that is not functional no method
        if (method == null) return;
        byMethod.computeIfAbsent(method, k -> new ArrayList<>())
                .add(new Use(kind, method, in, element, arguments == null ? List.of() : List.copyOf(arguments)));
        byKind.computeIfAbsent(kind, k -> new LinkedHashSet<>()).add(method);
    }

    /** Every use of {@code method}, in the order of the types given and of their code. */
    public List<Use> uses(MethodInfo method) {
        return byMethod.getOrDefault(method, List.of());
    }

    /** The uses of {@code method} of one kind. */
    public List<Use> uses(MethodInfo method, Kind kind) {
        return uses(method).stream().filter(use -> use.kind() == kind).toList();
    }

    /** The methods that have a use of {@code kind}: those a method reference names, those a lambda implements. */
    public Set<MethodInfo> methods(Kind kind) {
        return byKind.getOrDefault(kind, Set.of());
    }

    /** The methods and constructors that have a use, of any kind. */
    public Set<MethodInfo> methods() {
        return Collections.unmodifiableSet(byMethod.keySet());
    }
}
