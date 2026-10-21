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
package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.impl.analysis.NullabilityFacts;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;
import io.codelaser.maddi.cst.impl.output.TextImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The {@link NullabilityFacts} applied: a declaration's type as the verdict has it, and at a use site, the
 * {@link KotlinPrintOptions.NullCheck} where a value Kotlin types as nullable is used as non-null.
 * <p>
 * Kotlin types a value as nullable when it comes from a declaration whose verdict is NULLABLE, or from a JDK
 * method that can return null ({@code Map.get}). A target is non-null when it is declared by the code being
 * translated (a Java library's parameter is a platform type, which takes a nullable value) and its verdict is not
 * NULLABLE. Without verdicts, nothing here changes the output.
 */
final class KotlinNullability {

    /** {@code !!}, glued to its operand. */
    static final io.codelaser.maddi.cst.api.output.element.Symbol NOT_NULL =
            new SymbolEnum("!!", SpaceEnum.NONE, SpaceEnum.NONE, null);

    /**
     * JDK methods that return null for a missing element; their Kotlin signatures say so (V?, E?). The analysis
     * knows them from its hint archives and stores NULLABILITY_METHOD on them, which {@link NullabilityFacts#STORED}
     * reads; this list is for a translation without the analysis. Kotlin's Throwable declares {@code open val cause:
     * Throwable?} and {@code message: String?}: open, so no {@code != null} test smart-casts them.
     */
    private static final Map<String, Set<String>> NULLABLE_JDK_RESULTS = Map.of(
            "java.lang.Throwable", Set.of("getCause", "getMessage"),
            "java.util.Map", Set.of("get", "remove", "put", "putIfAbsent"),
            "java.util.Queue", Set.of("poll", "peek"),
            "java.util.Deque", Set.of("pollFirst", "pollLast", "peekFirst", "peekLast"));

    private KotlinNullability() {
    }

    static NullabilityFacts verdicts() {
        return KotlinContext.options().verdicts();
    }

    static ParameterizedType fieldType(FieldInfo fieldInfo) {
        ParameterizedType verdict = verdicts().field(fieldInfo);
        ParameterizedType type = verdict != null ? verdict : fieldInfo.type();
        // guava's Ordering.ARBITRARY_ORDERING: an Ordering<@Nullable Object>, which arbitrary() returns as one
        return KotlinContext.translatingJava()
                ? withDeclaredArgumentStates(type, KotlinTypePrinter.declaredArguments(fieldInfo.type())) : type;
    }

    static ParameterizedType parameterType(ParameterInfo parameterInfo) {
        ParameterizedType lambdaParameter = KotlinContext.lambdaParameterType(parameterInfo);
        if (lambdaParameter != null) return lambdaParameter;
        FieldInfo property = KotlinTypePrinter.propertyOf(parameterInfo);
        if (property != null) return fieldType(property); // class Foo(val id: T): the property's type
        // a parameter the body assigns is printed as `var p = p`, typed by what the body assigns
        // (KotlinStatementPrinter.reassignedParameters): every read in the body is the local's
        ParameterizedType shadow = KotlinContext.currentMethod() == parameterInfo.methodInfo()
                ? KotlinContext.localType(parameterInfo.name()) : null;
        if (shadow != null) return shadow;
        ParameterizedType declared = parameterInfo.parameterizedType();
        MethodInfo method = parameterInfo.methodInfo();
        int index = parameterInfo.index();
        if (isBoxed(declared) && (boxedAbi(method, m -> index < m.parameters().size()
                ? m.parameters().get(index).parameterizedType() : null) || boxedClash(method))) {
            return declared.withNullable(NullableState.NULLABLE);
        }
        if (overridesRawTypeVariable(method, index)) return declared.withNullable(NullableState.NULLABLE);
        ParameterizedType verdict = verdicts().parameter(parameterInfo);
        // guava's TreeRangeMap: merge(…, BiFunction<? super Object, ? super @Nullable Object, ? extends @Nullable
        // Object> f) overriding RangeMap's: the declaration's type-use annotations on its arguments, wildcard bounds
        // included, which an override must repeat
        ParameterizedType type = verdict != null ? verdict : declared;
        if (KotlinContext.translatingJava()) type = withDeclaredArgumentStates(type, KotlinTypePrinter.declaredArguments(declared));
        return INFERRED_NULLABLE.get().containsKey(parameterInfo) ? type.withNullable(NullableState.NULLABLE) : type;
    }

    /**
     * jenkins's HyperlinkNote extends the raw ConsoleNote, whose header erases it to {@code ConsoleNote<Any?>}
     * (KotlinTypePrinter.rawSupertype): {@code annotate(Object context, …)} overrides {@code annotate(T context, …)},
     * a parameter Kotlin then sees as an {@code Any?}, whatever the override's verdict. Only an unbounded T: a bound's
     * erasure keeps the bound's state.
     */
    private static boolean overridesRawTypeVariable(MethodInfo method, int index) {
        if (!KotlinContext.translatingJava() || method.isStatic() || method.isConstructor()) return false;
        TypeInfo owner = method.typeInfo();
        return method.overrides().stream().anyMatch(m -> {
            if (index >= m.parameters().size()) return false;
            ParameterizedType p = m.parameters().get(index).parameterizedType();
            io.codelaser.maddi.cst.api.info.TypeParameter tp = p.typeParameter();
            if (tp == null || p.arrays() > 0 || tp.isMethodTypeParameter() || !m.typeInfo().typeParameters().contains(tp)
                || tp.typeBounds().stream().anyMatch(b -> !b.isJavaLangObject())) {
                return false;
            }
            return java.util.stream.Stream.concat(java.util.stream.Stream.ofNullable(owner.parentClass()),
                            owner.interfacesImplemented().stream())
                    .anyMatch(s -> s.typeInfo() == m.typeInfo() && s.parameters().isEmpty());
        });
    }

    /**
     * Java's {@code Long} in a signature is Kotlin's {@code Long?}: a non-null {@code Long} is the JVM's {@code long},
     * and Java code calling {@code from(Long)} or {@code Long total()} finds no such method. Not in a private member,
     * which Java code cannot call; not where it overrides a member typed by a type parameter, whose {@code Long?}
     * would not override {@code compareTo(other: Int)}.
     */
    private static boolean boxedAbi(MethodInfo method,
                                    java.util.function.Function<MethodInfo, ParameterizedType> typeIn) {
        if (!KotlinContext.translatingJava() || !translated(method.typeInfo()) || method.isSynthetic()
            || method.access() != null && method.access().isPrivate() || method.typeInfo().isAnonymous()) {
            return false;
        }
        return method.overrides().stream().allMatch(m -> {
            ParameterizedType t = typeIn.apply(m);
            return t != null && isBoxed(t);
        });
    }

    /**
     * Java overloads on {@code int} and {@code Integer}, Kotlin cannot: both are {@code Int}. When this method's Kotlin
     * signature equals a sibling's, its boxed parameters are written {@code Int?}, Java's {@code Integer} with its
     * null; an {@code Int} argument then picks the primitive overload, as in Java.
     */
    private static boolean boxedClash(MethodInfo methodInfo) {
        if (KotlinTypePrinter.fromKotlinSource(methodInfo.typeInfo())
            || methodInfo.parameters().stream().noneMatch(p -> isBoxed(p.parameterizedType()))) {
            return false;
        }
        java.util.List<MethodInfo> siblings = methodInfo.isConstructor() ? methodInfo.typeInfo().constructors()
                : methodInfo.typeInfo().methods().stream().filter(m -> m.name().equals(methodInfo.name())).toList();
        String signature = kotlinSignature(methodInfo);
        return siblings.stream().anyMatch(m -> m != methodInfo && !m.isSynthetic()
                                               && m.parameters().size() == methodInfo.parameters().size()
                                               && signature.equals(kotlinSignature(m)));
    }

    private static String kotlinSignature(MethodInfo m) {
        return m.parameters().stream().map(p -> KotlinTypeName.of(p.parameterizedType()))
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static boolean isBoxed(ParameterizedType type) {
        return type.arrays() == 0 && type.typeInfo() != null && type.isBoxedExcludingVoid();
    }


    static ParameterizedType returnType(MethodInfo methodInfo) {
        if (isBoxed(methodInfo.returnType()) && boxedAbi(methodInfo, MethodInfo::returnType)) {
            return methodInfo.returnType().withNullable(NullableState.NULLABLE);
        }
        ParameterizedType verdict = verdicts().returnType(methodInfo);
        if (verdict == null && libraryNullableResult(methodInfo)) {
            // jenkins's WhoAmI: Spring Security 7's @Nullable GrantedAuthority.getAuthority(), which Kotlin reads as
            // String?: what it is put into asserts it (authorities.add(a.getAuthority()!!))
            verdict = methodInfo.returnType().withNullable(NullableState.NULLABLE);
        }
        if (verdict != null && isNullable(verdict)
            && (overridesMappedMember(methodInfo) || returnsNullForTypeVariable(methodInfo))) {
            // Kotlin declares the member it overrides, non-null: Any.toString(): String; Iterator<E>.next(): E
            return withDeclaredArgumentStates(methodInfo, verdict.withNullable(NullableState.NONNULL));
        }
        ParameterizedType result = withDeclaredArgumentStates(methodInfo,
                verdict != null ? verdict : methodInfo.returnType());
        ParameterizedType typed = rawOverrideResult(methodInfo, result);
        return typed != null ? typed : result;
    }

    /**
     * jenkins's Notifier: {@code BuildStepDescriptor getDescriptor()} overriding {@code Descriptor<Publisher>
     * getDescriptor()}, a raw result Java accepts unchecked. Kotlin's {@code BuildStepDescriptor<*>} is no subtype of
     * {@code Descriptor<Publisher>}; the raw class with the overridden result's arguments is,
     * {@code BuildStepDescriptor<Publisher>}, where each of its type parameters is one of the overridden type's
     * arguments. Null otherwise.
     */
    private static ParameterizedType rawOverrideResult(MethodInfo methodInfo, ParameterizedType result) {
        TypeInfo raw = result.typeInfo();
        if (!KotlinContext.translatingJava() || raw == null || result.arrays() > 0 || !result.parameters().isEmpty()
            || raw.typeParameters().isEmpty() || methodInfo.overrides().isEmpty()) {
            return null;
        }
        for (MethodInfo o : methodInfo.overrides()) {
            ParameterizedType theirs = o.returnType();
            if (theirs == null || theirs.typeInfo() == null || theirs.parameters().isEmpty() || theirs.arrays() > 0) {
                continue;
            }
            ParameterizedType seen = seenThrough(theirs, o.typeInfo(), methodInfo.typeInfo().asParameterizedType());
            if (seen.typeInfo() == null || seen.parameters().size() != theirs.parameters().size()) continue;
            ParameterizedType asSuper;
            try {
                asSuper = raw == seen.typeInfo() ? raw.asParameterizedType()
                        : raw.asParameterizedType().concreteSuperType(seen.typeInfo().asParameterizedType());
            } catch (RuntimeException | AssertionError e) {
                continue;
            }
            if (asSuper == null || asSuper.parameters().size() != seen.parameters().size()) continue;
            List<ParameterizedType> arguments = new ArrayList<>();
            for (io.codelaser.maddi.cst.api.info.TypeParameter tp : raw.typeParameters()) {
                ParameterizedType argument = null;
                for (int i = 0; i < asSuper.parameters().size(); i++) {
                    ParameterizedType a = asSuper.parameters().get(i);
                    if (a != null && a.typeParameter() == tp && a.arrays() == 0 && a.wildcard() == null) {
                        argument = seen.parameters().get(i);
                        break;
                    }
                }
                if (argument == null) break;
                arguments.add(argument);
            }
            if (arguments.size() == raw.typeParameters().size()) {
                return result.withParameters(arguments);
            }
        }
        return null;
    }

    /*
     The declaration's type-use annotations on the result's arguments, as for a field, a parameter and a local:
     guava's Ordering<@Nullable Object> arbitrary(), returning the field ARBITRARY_ORDERING, printed as
     Ordering<Any> beside the field's Ordering<Any?> once the field kept its annotations (maddi#145).
     */
    private static ParameterizedType withDeclaredArgumentStates(MethodInfo methodInfo, ParameterizedType type) {
        return KotlinContext.translatingJava()
                ? withDeclaredArgumentStates(type, KotlinTypePrinter.declaredArguments(methodInfo.returnType())) : type;
    }

    /**
     * A nullable override of a member returning a type variable that is not nullable itself: fernflower's
     * {@code FastSparseSetIterator<E>.next()} returns null past the end, but Kotlin's {@code Iterator<E>.next(): E}
     * cannot be overridden by {@code E?}. The override keeps {@code E}, and a returned null is cast to it
     * ({@link #typeVariableReturn}), unchecked, as Java's is.
     */
    static boolean returnsNullForTypeVariable(MethodInfo methodInfo) {
        if (!methodInfo.returnType().isTypeParameter() || methodInfo.returnType().arrays() > 0) return false;
        if (kotlinNullableMapResult(methodInfo)) return false;
        return methodInfo.overrides().stream().anyMatch(m -> m.returnType().isTypeParameter()
                && declaredInKotlin(m.typeInfo()) && !isNullable(declaredReturnType(m)));
    }

    /**
     * Kotlin sees the type's members as declared: a mapped type ({@code Iterator<E>.next(): E}) or one translated or
     * compiled from Kotlin. A Java library type's are platform types, which an override may make nullable: guava's
     * {@code ImmutableSortedSet.lower(e): E?} overrides {@code NavigableSet<E>.lower(e): E!}. Except where kotlinc
     * enhances the JDK's own: {@code Supplier<T>.get(): T}.
     */
    private static boolean declaredInKotlin(TypeInfo owner) {
        String fqn = owner.fullyQualifiedName();
        return !owner.compilationUnit().externalLibrary() || KotlinTypeName.isMapped(fqn)
               || KotlinTypePrinter.fromKotlinSource(owner)
               || fqn.startsWith("java.util.function.") || "java.util.Optional".equals(fqn);
    }

    /**
     * Kotlin declares these members of its Map nullable, {@code get(key: K): V?}, {@code replace(key, value): V?},
     * {@code compute(…): V?}: an override may return {@code V?} (guava's Maps, ForwardingConcurrentMap, LocalCache).
     */
    private static final Set<String> KOTLIN_NULLABLE_MAP_RESULTS = Set.of("get", "put", "remove", "putIfAbsent",
            "replace", "compute", "computeIfPresent", "merge");

    private static boolean kotlinNullableMapResult(MethodInfo methodInfo) {
        return methodInfo.overrides().stream().anyMatch(m -> "java.util.Map".equals(m.typeInfo().fullyQualifiedName())
                && KOTLIN_NULLABLE_MAP_RESULTS.contains(m.name()));
    }

    /** A translated method's return verdict; a library's or a Kotlin source's return type as declared. */
    private static ParameterizedType declaredReturnType(MethodInfo m) {
        ParameterizedType verdict = translated(m.typeInfo()) ? verdicts().returnType(m) : null;
        return verdict != null ? verdict : m.returnType();
    }

    /**
     * {@code (value) as E}, for a return from a method {@link #returnsNullForTypeVariable}; null otherwise. Whether the
     * verdict is nullable (the override keeps E all the same) or the pass holds it non-null for Kotlin's member
     * (maddi-mod a8cac5ca): Java returns the null, and so does the cast, where {@code !!} would throw.
     */
    static OutputBuilder typeVariableReturn(MethodInfo methodInfo, Expression value, Qualification q) {
        if (!(value instanceof NullConstant) && !nullableInKotlin(value)) return null;
        ParameterizedType verdict = verdicts().returnType(methodInfo);
        if (verdict == null || !returnsNullForTypeVariable(methodInfo)) return null;
        KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, value, KotlinContext.describe(value));
        // the cast's operand has no expected type of its own: guava's CompactHashMap, return unsafeNull() with T
        // inferred V, is (unsafeNull<V>()) as V; without, kotlinc takes the cast's V and the call's T? clashes
        OutputBuilder printed = KotlinExpressionPrinter.withoutExpectedType(value,
                () -> KotlinExpressionPrinter.print(value, q));
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(printed)
                .add(SymbolEnum.RIGHT_PARENTHESIS).add(SpaceEnum.ONE).add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                .add(new TextImpl(KotlinTypeName.of(methodInfo.returnType(), q)));
    }

    /** An override of a member of a type Kotlin maps to its own (Object, String, the collections): no platform type. */
    private static boolean overridesMappedMember(MethodInfo methodInfo) {
        return methodInfo.overrides().stream()
                .anyMatch(m -> KotlinTypeName.isMapped(m.typeInfo().fullyQualifiedName()) && !m.returnType().isTypeParameter());
    }

    /**
     * The element type a for-each loop variable gets in Kotlin, which takes it from what it loops over and cannot be
     * told otherwise: an array's component type, or an {@code Iterable}'s type argument (found through the
     * hierarchy: {@code VBStyleCollection<Statement?, Int?>} is an {@code Iterable<Statement?>}). Null when unknown.
     */
    static ParameterizedType elementType(Expression iterable) {
        ParameterizedType type = kotlinType(iterable);
        if (type == null) return null;
        if (type.arrays() > 0) return type.componentType();
        TypeInfo iterableType = supertype(type.typeInfo(), "java.lang.Iterable", new java.util.HashSet<>());
        if (iterableType == null) return null;
        // concreteSuperType walks the hierarchy recursively: a deep or cyclic one overflows, as caught across maddi
        try {
            ParameterizedType asIterable = type.typeInfo() == iterableType ? type
                    : type.concreteSuperType(iterableType.asParameterizedType());
            if (asIterable == null || asIterable.parameters().size() != 1) return null;
            ParameterizedType element = asIterable.parameters().getFirst();
            return element == null || element.wildcard() != null ? null : element;
        } catch (RuntimeException | AssertionError | StackOverflowError e) {
            return null;
        }
    }

    private static TypeInfo supertype(TypeInfo typeInfo, String fullyQualifiedName, Set<TypeInfo> visited) {
        if (typeInfo == null || !visited.add(typeInfo)) return null;
        if (fullyQualifiedName.equals(typeInfo.fullyQualifiedName())) return typeInfo;
        ParameterizedType parent = typeInfo.parentClass();
        TypeInfo found = parent == null ? null : supertype(parent.typeInfo(), fullyQualifiedName, visited);
        if (found != null) return found;
        for (ParameterizedType i : typeInfo.interfacesImplemented()) {
            found = supertype(i.typeInfo(), fullyQualifiedName, visited);
            if (found != null) return found;
        }
        return null;
    }

    // lambda parameters Kotlin types nullable by inference, whatever their verdict (inferLambdaParameters)
    private static final ThreadLocal<java.util.Map<ParameterInfo, Boolean>> INFERRED_NULLABLE =
            ThreadLocal.withInitial(java.util.WeakHashMap::new);

    /**
     * A lambda printed without parameter types takes them from the parameter it is passed to, instantiated by the
     * call's receiver: {@code metadata.forEach { key, value -> validate(key, value) }} on a {@code Map<String?, *>}
     * has a {@code key: String?}, through forEach's {@code BiConsumer<? super K, ? super V>}, whatever the verdict of
     * the lambda's own {@code key}. Only the nullable direction: a {@code !!} on a value Kotlin knows non-null is a
     * warning, a missing one an error.
     */
    static void inferLambdaParameters(Lambda lambda, MethodInfo method, int index, MethodCall call) {
        if (method == null || index >= method.parameters().size() || call.object() == null) return;
        ParameterizedType formal = method.parameters().get(index).parameterizedType();
        ParameterizedType receiver = kotlinType(call.object());
        if (formal.typeInfo() == null || receiver == null || receiver.typeInfo() != method.typeInfo()) return;
        MethodInfo sam = lambda.methodInfo().overrides().stream()
                .filter(o -> o.typeInfo() == formal.typeInfo()).findFirst().orElse(null);
        if (sam == null) return;
        List<ParameterInfo> parameters = lambda.parameters();
        for (int j = 0; j < parameters.size() && j < sam.parameters().size(); j++) {
            io.codelaser.maddi.cst.api.info.TypeParameter samVariable = sam.parameters().get(j).parameterizedType()
                    .typeParameter();
            if (samVariable == null || samVariable.isMethodTypeParameter()
                || samVariable.getIndex() >= formal.parameters().size()) continue;
            io.codelaser.maddi.cst.api.info.TypeParameter receiverVariable = formal.parameters()
                    .get(samVariable.getIndex()).typeParameter();
            if (receiverVariable == null || receiverVariable.isMethodTypeParameter()
                || !method.typeInfo().typeParameters().contains(receiverVariable)
                || receiverVariable.getIndex() >= receiver.parameters().size()) continue;
            if (isNullable(receiver.parameters().get(receiverVariable.getIndex()))) {
                INFERRED_NULLABLE.get().put(parameters.get(j), true);
            }
        }
    }

    /** The type of a local variable declared by {@code declaration}; remembered for the uses that follow. */
    static ParameterizedType localType(Element declaration, LocalVariable variable) {
        MethodInfo method = KotlinContext.currentMethod();
        // a verdict that is no decision (a degraded method's unreached local) is nullable, the choice that compiles;
        // without any verdict the declared type stands
        ParameterizedType verdict = method == null ? null
                : NullabilityFacts.decided(verdicts().local(method, declaration, variable));
        ParameterizedType type = verdict != null ? verdict : variable.parameterizedType();
        // ListenableFuture<List<@Nullable V>> nullable = new ListFuture<V>(…) (guava's Futures.allAsList): the
        // declaration's type-use annotations on its arguments, as in a class header
        type = withDeclaredArgumentStates(type, KotlinTypePrinter.declaredArguments(variable.parameterizedType()));
        KotlinContext.localType(variable.simpleName(), type);
        return type;
    }

    /** {@code type}, with each type argument nullable that {@code declared}, of the same shape, makes nullable. */
    private static ParameterizedType withDeclaredArgumentStates(ParameterizedType type, ParameterizedType declared) {
        if (type == null || declared == null || type.parameters().isEmpty()
            || type.parameters().size() != declared.parameters().size()
            || !java.util.Objects.equals(type.typeInfo(), declared.typeInfo()) || type.arrays() != declared.arrays()) {
            return type;
        }
        boolean changed = false;
        java.util.List<ParameterizedType> arguments = new java.util.ArrayList<>(type.parameters().size());
        for (int i = 0; i < type.parameters().size(); i++) {
            ParameterizedType mine = type.parameters().get(i);
            ParameterizedType theirs = declared.parameters().get(i);
            ParameterizedType merged = withDeclaredArgumentStates(mine, theirs);
            if (theirs != null && mine != null && isNullable(theirs) && !isNullable(merged)
                && (theirs.wildcard() == null || mine.wildcard() != null && !theirs.isUnboundWildcard()
                                                 && theirs.wildcard().isSuper() == mine.wildcard().isSuper())) {
                merged = merged.withNullable(NullableState.NULLABLE);
            }
            changed |= merged != mine;
            arguments.add(merged);
        }
        return changed ? type.withParameters(arguments) : type;
    }

    /** The pass asserts this local at its declaration: see {@link NullabilityFacts#assertedAtDeclaration}. */
    static boolean assertedAtDeclaration(Element declaration, LocalVariable variable) {
        MethodInfo method = KotlinContext.currentMethod();
        return method != null && verdicts().assertedAtDeclaration(method, declaration, variable);
    }

    /** See {@link NullabilityFacts#unobservedBeforeDereference}. */
    static boolean unobservedBeforeDereference(Element declaration, LocalVariable variable) {
        MethodInfo method = KotlinContext.currentMethod();
        return method != null && verdicts().unobservedBeforeDereference(method, declaration, variable);
    }

    static boolean isNullable(ParameterizedType type) {
        return type != null && type.nullable() == NullableState.NULLABLE;
    }

    /**
     * The loop variables over a raw or star-projected collection being printed: Kotlin's {@code Any?}, which the
     * verdicts, holding the variable non-null, do not know (jenkins's RedactSecretJsonInErrorMessageSanitizer).
     */
    static final ThreadLocal<Set<String>> RAW_LOOP_VARIABLES = ThreadLocal.withInitial(java.util.HashSet::new);

    /** Kotlin will type this value as nullable. */
    static boolean nullableInKotlin(Expression e) {
        if (!KotlinContext.translatingJava()) return false; // Kotlin source wrote its own ?. and !!
        Expression x = KotlinExpressionPrinter.unwrap(e);
        return switch (x) {
            case NullConstant nc -> true;
            case MethodCall mc when KotlinSmartCasts.copy(mc) != null ->
                    isNullable(KotlinSmartCasts.copy(mc).type()) && !KotlinSmartCasts.asserted(KotlinSmartCasts.copy(mc), mc);
            case MethodCall mc -> isNullable(throughReceiver(returnType(mc.methodInfo()), mc))
                                  || nullableJdkResult(mc.methodInfo());
            case VariableExpression ve -> switch (ve.variable()) {
                // Kotlin smart-casts a val property, never a var one: a fact about a var field does not hold there
                case FieldReference fr -> isNullable(fieldType(fr.fieldInfo()))
                                          && !(KotlinFieldPrinter.printsAsVal(fr.fieldInfo()) && knownNonNull(fr));
                case ParameterInfo pi -> isNullable(parameterType(pi)) && !knownNonNull(pi, ve);
                // a loop variable over a raw collection is nullable in Kotlin only: no verdict fact smart-casts it
                case LocalVariable lv when RAW_LOOP_VARIABLES.get().contains(lv.simpleName()) ->
                        !KotlinSmartCasts.asserted(lv, ve);
                case LocalVariable lv -> isNullable(KotlinContext.localType(lv.simpleName())) && !knownNonNull(lv, ve);
                // nor does it smart-cast an element read: only the element's state counts
                case io.codelaser.maddi.cst.api.variable.DependentVariable dv -> {
                    ParameterizedType array = kotlinType(dv.arrayExpression());
                    yield array != null && array.arrays() > 0 && isNullable(array.componentType());
                }
                default -> false;
            };
            case InlineConditional ic when KotlinNullCheckShapes.elvis(ic) != null ->
                    nullableInKotlin(KotlinNullCheckShapes.elvis(ic).otherwise());
            case InlineConditional ic -> nullableInKotlin(ic.ifTrue()) || nullableInKotlin(ic.ifFalse());
            default -> false;
        };
    }

    /**
     * A reference cast of a value Kotlin types nullable: {@code x as T} throws on null where Java's cast does not.
     * Into a nullable target it prints {@code x as T?} (see {@link KotlinExpressionPrinter#widened}); elsewhere the
     * value is dereferenced, unboxed or tested, where Java throws as well.
     */
    static boolean nullableCast(Cast cast) {
        return !cast.parameterizedType().isPrimitiveExcludingVoid() && nullableInKotlin(cast.expression());
    }

    /** The type Kotlin gives this expression, with the verdicts' states; null when not known here. */
    static ParameterizedType kotlinType(Expression e) {
        return switch (KotlinExpressionPrinter.unwrap(e)) {
            case MethodCall mc -> throughReceiver(returnType(mc.methodInfo()), mc);
            case VariableExpression ve -> switch (ve.variable()) {
                case FieldReference fr -> fieldType(fr.fieldInfo());
                case ParameterInfo pi -> parameterType(pi);
                case LocalVariable lv -> KotlinContext.localType(lv.simpleName()) != null
                        ? KotlinContext.localType(lv.simpleName()) : lv.parameterizedType();
                case io.codelaser.maddi.cst.api.variable.DependentVariable dv -> {
                    ParameterizedType array = kotlinType(dv.arrayExpression());
                    yield array == null || array.arrays() == 0 ? null : array.componentType();
                }
                default -> null;
            };
            // (if (i == 0) shortRange else longRange).entries: Kotlin's type of the if is its branches'
            case InlineConditional ic -> {
                ParameterizedType both = join(kotlinType(ic.ifTrue()), kotlinType(ic.ifFalse()));
                if (both == null) yield null;
                yield nullableInKotlin(ic) ? both.withNullable(NullableState.NULLABLE) : both;
            }
            default -> null;
        };
    }

    /**
     * Kotlin's type of a conditional whose branches are typed {@code t} and {@code f}: the branch that is known when
     * only one is; their common type when both are, where a type argument or array component nullable in either
     * branch is nullable ({@code if (c) preds else succs} over a {@code Set<Block>} and a {@code Set<Block?>}: a
     * {@code Set<out Block?>}, whose iteration yields a {@code Block?}). Null when the branches' types differ.
     */
    private static ParameterizedType join(ParameterizedType t, ParameterizedType f) {
        if (t == null || f == null) return t != null ? t : f;
        if (t.typeInfo() != f.typeInfo() || t.arrays() != f.arrays()) return null;
        if (t.arrays() > 0) {
            ParameterizedType component = join(t.componentType(), f.componentType());
            if (component == null) return null;
            return isNullable(f.componentType()) && !isNullable(t.componentType())
                    ? t.withComponentType(component.withNullable(NullableState.NULLABLE)) : t;
        }
        if (t.parameters().isEmpty() || t.parameters().size() != f.parameters().size()) return t;
        List<ParameterizedType> arguments = new java.util.ArrayList<>();
        boolean changed = false;
        for (int i = 0; i < t.parameters().size(); i++) {
            ParameterizedType a = t.parameters().get(i);
            ParameterizedType b = f.parameters().get(i);
            if (a != null && b != null && a.wildcard() == null && isNullable(b) && !isNullable(a)) {
                a = a.withNullable(NullableState.NULLABLE);
                changed = true;
            }
            arguments.add(a);
        }
        return changed ? t.withParameters(arguments) : t;
    }

    /**
     * A member's parameter or result type as the receiver instantiates it: {@code E} of {@code list.add(e)} or
     * {@code list.get(i)} is the receiver's type argument, with that argument's state ({@code List<String?>}: the
     * element may be null; {@code List<String>}: a nullable value is asserted where it is written). Only for a type
     * parameter of the receiver's type or of the supertype that declares the member ({@code ArrayList<E>} calling
     * {@code Collection.add(E)}); anything else keeps the declared type.
     */
    static ParameterizedType throughReceiver(ParameterizedType declared, MethodCall call) {
        return throughReceiver(declared, call, false);
    }

    /**
     * As {@link #throughReceiver(ParameterizedType, MethodCall)}; for a parameter ({@code input}), a {@code ? super X}
     * argument is its bound: {@code lst.add(0, post)} on a {@code MutableList<in Statement?>} takes a Statement?.
     */
    static ParameterizedType throughReceiver(ParameterizedType declared, MethodCall call, boolean input) {
        if (declared == null || !declared.isTypeParameter() && declared.parameters().isEmpty() || declared.arrays() > 0
            || call.objectIsImplicit() || call.object() == null) {
            return declared;
        }
        ParameterizedType receiver = kotlinType(call.object());
        if (receiver == null || receiver.typeInfo() == null || receiver.arrays() > 0) return declared;
        TypeInfo declaring = call.methodInfo().typeInfo();
        if (receiver.typeInfo() != declaring && !declaring.typeParameters().isEmpty()) {
            // an inherited member (ArrayList<E> calling Collection.add(E)): the receiver as its declaring type
            try {
                receiver = receiver.concreteSuperType(declaring.asParameterizedType());
            } catch (RuntimeException | AssertionError | StackOverflowError e) {
                return declared;
            }
            if (receiver == null || receiver.typeInfo() != declaring) return declared;
        }
        return substitute(declared, receiver, input);
    }

    /**
     * {@code declared} with the receiver's type parameters replaced by its arguments, nested ones included
     * ({@code entrySet(): Set<Entry<K, V>>} on a {@code Map<String, VarVersion?>}). The member's own {@code ?}
     * stays: {@code getWithKey(k): E?} on a collection of {@code MethodWrapper} is a {@code MethodWrapper?}.
     */
    private static ParameterizedType substitute(ParameterizedType declared, ParameterizedType receiver) {
        return substitute(declared, receiver, false);
    }

    /**
     * A member's type {@code declared} in {@code declaring}, as a supertype {@code receiver} of a class sees it:
     * Collection's {@code spliterator(): Spliterator<E>} through a {@code Set<V>} is a {@code Spliterator<V>}.
     */
    static ParameterizedType seenThrough(ParameterizedType declared, TypeInfo declaring, ParameterizedType receiver) {
        ParameterizedType seen;
        try {
            seen = receiver.typeInfo() == declaring ? receiver : receiver.concreteSuperType(declaring.asParameterizedType());
        } catch (RuntimeException | AssertionError e) {
            seen = null;
        }
        if (seen != null && seen.typeInfo() != null) seen = erasedRaw(seen, receiver);
        return seen == null || seen.typeInfo() == null ? declared : substitute(declared, seen);
    }

    /**
     * A raw supertype as Java erases it, each type parameter its bound's class or a nullable Object: jenkins's
     * JellyMap implements a raw Map, a {@code MutableMap<Any?, Any?>}, whose keySet() is a {@code MutableSet<Any?>}.
     */
    static ParameterizedType erasedRaw(ParameterizedType seen, ParameterizedType receiver) {
        TypeInfo raw = seen.typeInfo();
        if (!KotlinContext.translatingJava() || !seen.parameters().isEmpty() || seen.arrays() > 0
            || raw.typeParameters().isEmpty() || receiver.typeInfo() == null) {
            return seen;
        }
        // the printer has no runtime: Object is the top of the receiver's superclasses
        ParameterizedType object = receiver.typeInfo().parentClass();
        while (object != null && !object.isJavaLangObject() && object.typeInfo() != null) {
            object = object.typeInfo().parentClass();
        }
        if (object == null || !object.isJavaLangObject()) return seen;
        List<ParameterizedType> arguments = new ArrayList<>();
        for (io.codelaser.maddi.cst.api.info.TypeParameter tp : raw.typeParameters()) {
            ParameterizedType bound = tp.typeBounds().stream().filter(b -> !b.isJavaLangObject() && b.typeInfo() != null)
                    .findFirst().orElse(null);
            if (bound != null && !bound.typeInfo().typeParameters().isEmpty()) return seen;
            arguments.add(bound != null ? bound.typeInfo().asSimpleParameterizedType()
                    : object.withNullable(NullableState.NULLABLE));
        }
        return seen.withParameters(arguments);
    }

    private static ParameterizedType substitute(ParameterizedType declared, ParameterizedType receiver, boolean input) {
        if (declared == null) return null;
        if (declared.isTypeParameter() && declared.arrays() == 0) {
            int index = receiver.typeInfo().typeParameters().indexOf(declared.typeParameter());
            if (index < 0 || index >= receiver.parameters().size()) return declared;
            ParameterizedType argument = receiver.parameters().get(index);
            if (input && argument != null && argument.wildcard() != null && argument.wildcard().isSuper()
                && (argument.typeInfo() != null || argument.isTypeParameter())) {
                // MutableList<in Statement?>.add(e): what goes in is a Statement?, an `in` projection's
                argument = argument.withWildcard(null);
            }
            if (argument != null && argument.wildcard() != null && argument.wildcard().isExtendsNoIntersection()
                && (argument.typeInfo() != null || argument.isTypeParameter())) {
                // List<? extends Statement?>.get(i): what comes out is a Statement?, a Kotlin `out` projection's
                argument = argument.withWildcard(null);
            }
            if (argument != null && !input && argument.isUnboundWildcard()) {
                // comparisonValues.iterator().next() on a Collection<*>: what comes out of a star is an Any?
                return declared.withNullable(NullableState.NULLABLE);
            }
            if (argument == null || argument.wildcard() != null
                || argument.typeInfo() == null && !argument.isTypeParameter()) {
                return declared;
            }
            return isNullable(declared) ? argument.withNullable(NullableState.NULLABLE) : argument;
        }
        if (declared.arrays() > 0 || declared.parameters().isEmpty()) return declared;
        List<ParameterizedType> arguments = new java.util.ArrayList<>();
        boolean changed = false;
        for (ParameterizedType p : declared.parameters()) {
            // a bounded wildcard on a type parameter (? super T) is that parameter's argument for nullability
            ParameterizedType s = p == null || p.wildcard() != null && !p.isTypeParameter() ? p : substitute(p, receiver);
            changed |= s != p;
            arguments.add(s);
        }
        return changed ? declared.withParameters(arguments) : declared;
    }

    /**
     * The types Kotlin gives a lambda's parameters where the lambda is an argument: the functional interface's
     * single abstract method, seen through the parameter's type, itself seen through the call's receiver
     * ({@code stream.filter { e -> … }} on a {@code Stream<Exprent?>}: {@code Predicate<? super T>} is a
     * {@code Predicate<Exprent?>}, whose {@code test(T)} takes an {@code Exprent?}). Empty when not known.
     */
    static List<ParameterizedType> lambdaParameterTypes(MethodCall call, ParameterizedType functional) {
        ParameterizedType seen = throughReceiver(functional, call);
        if (seen == null || seen == functional) return List.of();
        return samParameterTypes(seen);
    }

    /**
     * The parameter types of a functional type's single abstract method, seen through its type arguments
     * ({@code BinaryOperator<Acc<Object>>} takes two {@code Acc<Object>}). Empty when not known.
     */
    static List<ParameterizedType> samParameterTypes(ParameterizedType seen) {
        if (seen == null || seen.typeInfo() == null || seen.arrays() > 0) return List.of();
        MethodInfo sam;
        try {
            sam = seen.typeInfo().singleAbstractMethod();
        } catch (RuntimeException e) {
            sam = null;
        }
        if (sam == null) {
            // not computed for every library interface (java.util.function.Predicate): its one abstract method
            List<MethodInfo> abstracts = seen.typeInfo().methodStream().filter(MethodInfo::isAbstract).toList();
            if (abstracts.size() != 1) return List.of();
            sam = abstracts.getFirst();
        }
        return sam.parameters().stream().map(p -> substitute(p.parameterizedType(), seen)).toList();
    }

    /** A use-site fact: known non-null where the current statement starts, or at the current call. */
    private static boolean knownNonNull(io.codelaser.maddi.cst.api.variable.Variable variable) {
        return knownNonNull(variable, null);
    }

    /** As {@link #knownNonNull(io.codelaser.maddi.cst.api.variable.Variable)}, used at {@code use}: also smart-cast by an earlier {@code !!}. */
    private static boolean knownNonNull(io.codelaser.maddi.cst.api.variable.Variable variable, Expression use) {
        return KotlinContext.state().knownNonNull(verdicts(), variable) || KotlinSmartCasts.asserted(variable, use);
    }

    /**
     * A local or parameter Kotlin types nullable, not already asserted by an earlier {@code !!} the printer wrote:
     * whatever the facts say at {@code e} itself.
     */
    static boolean declaredNullableVariable(Expression e) {
        if (!KotlinContext.translatingJava() || !(KotlinExpressionPrinter.unwrap(e) instanceof VariableExpression ve)) {
            return false;
        }
        ParameterizedType type = switch (ve.variable()) {
            case ParameterInfo pi -> parameterType(pi);
            case LocalVariable lv -> KotlinContext.localType(lv.simpleName());
            default -> null;
        };
        return type != null && isNullable(type) && !KotlinSmartCasts.asserted(ve.variable(), e);
    }

    static boolean nullableJdkResult(MethodInfo methodInfo) {
        if (methodInfo.isStatic()) return false;
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(methodInfo), methodInfo.overrides().stream())
                .anyMatch(m -> NULLABLE_JDK_RESULTS.getOrDefault(m.typeInfo().fullyQualifiedName(), Set.of())
                        .contains(m.name()));
    }

    /** The code being printed is a test: its nulls into non-null parameters are there to be rejected. */
    private static boolean inTestSource() {
        MethodInfo method = KotlinContext.currentMethod();
        return method != null && method.typeInfo().compilationUnit() != null
               && method.typeInfo().compilationUnit().sourceSet() != null
               && method.typeInfo().compilationUnit().sourceSet().test();
    }

    /**
     * A test's bare null into a non-null parameter, which the test expects to throw: asserted. Not into a type
     * variable's own slot, {@code <T> range(T lower)}, which the call may instantiate nullable (guava's
     * GeneralRangeTest passes null to range(ORDERING, null, OPEN, 5, CLOSED) and expects no throw); a type that only
     * names a type variable, {@code List<X>}, is non-null whatever X is.
     */
    static boolean testNullAsserted(Expression value, ParameterizedType target) {
        return target != null && value instanceof NullConstant && !isNullable(target)
               && !target.isPrimitiveExcludingVoid() && !(target.typeParameter() != null && target.arrays() == 0)
               && inTestSource();
    }

    /** No type variable in it: a parameter's type at the call names none the caller does not know. */
    private static boolean writable(ParameterizedType type) {
        if (type.typeParameter() != null) return false;
        return type.parameters().stream().allMatch(p -> p.wildcard() != null && p.typeInfo() == null || writable(p));
    }

    /** Declared by the code being translated, so Kotlin will hold a value for it to its (non-)nullability. */
    /** A library method whose result its annotations declare {@code @Nullable}, as Kotlin reads them. */
    private static boolean libraryNullableResult(MethodInfo methodInfo) {
        return KotlinContext.translatingJava() && !methodInfo.isConstructor() && !translated(methodInfo.typeInfo())
               && !methodInfo.returnType().isPrimitiveExcludingVoid() && !methodInfo.returnType().isVoidOrJavaLangVoid()
               && io.codelaser.maddi.cst.impl.analysis.NullAnnotations.explicitState(methodInfo) == NullableState.NULLABLE;
    }

    /**
     * A library parameter Kotlin reads as non-null: annotated so, or unannotated in a JSpecify {@code @NullMarked}
     * scope and not a type variable (Spring Security 7's {@code SimpleGrantedAuthority(String role)}). A nullable
     * argument is asserted there, as into a translated non-null parameter; Java's caller met the library's own check.
     */
    static boolean libraryNonNullParameter(ParameterInfo pi) {
        if (!KotlinContext.translatingJava() || translated(pi.methodInfo().typeInfo())
            || pi.parameterizedType().isPrimitiveExcludingVoid()) {
            return false;
        }
        NullableState explicit = io.codelaser.maddi.cst.impl.analysis.NullAnnotations.explicitState(pi);
        if (explicit != null) return explicit == NullableState.NONNULL;
        return !pi.parameterizedType().isTypeParameter()
               && io.codelaser.maddi.cst.impl.analysis.NullAnnotations.inNullMarkedScope(pi.methodInfo());
    }

    static boolean translated(TypeInfo owner) {
        return !owner.compilationUnit().externalLibrary() && !KotlinTypePrinter.fromKotlinSource(owner);
    }

    /** {@code value}, asserted non-null when Kotlin types it nullable and {@code target} is a non-null declaration. */
    static OutputBuilder toTarget(Expression value, ParameterizedType target, boolean targetTranslated,
                                  OutputBuilder printed, Qualification q) {
        // a library's int parameter is Kotlin's Int all the same: Java unboxes an Integer into it (Math.min(map.get(k), 1))
        boolean unboxed = target != null && target.arrays() == 0 && target.isPrimitiveExcludingVoid()
                          && KotlinExpressionPrinter.primitive(target) == KotlinExpressionPrinter.primitive(value.parameterizedType());
        if (targetTranslated && target != null && value instanceof NullConstant && target.arrays() == 0
            && !isNullable(target) && unboundTypeVariable(target)) {
            // lstKeys.add(null) into a MutableList<K>: null stops at a type-variable slot (maddi-mod 77674c0c), and
            // the null passes as in Java: an unchecked cast, erased
            KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, value, KotlinContext.describe(value));
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(printed).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.AS).add(SpaceEnum.ONE).add(new TextImpl(KotlinTypeName.of(target, q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        if (targetTranslated && target != null && !isNullable(target) && typeVariableArgument(target)
            && nullableLambdaResultInChain(value)) {
            // results.stream().filter(…).map { it.response() }.toList() into a List<T>: Kotlin infers List<T?> from the
            // nullable response(), where null stops at the declaration's type-variable slot; Java's list holds the
            // nulls all the same: an unchecked cast, erased
            KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, value, KotlinContext.describe(value));
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(printed).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.AS).add(SpaceEnum.ONE).add(new TextImpl(KotlinTypeName.of(target, q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        if (targetTranslated && testNullAsserted(value, target)) {
            // assertThrows(NullPointerException.class, () -> map.putInstance(null, 1)) into a non-null parameter: the
            // test expects the throw, which `!!` gives; typed, so that overload resolution sees the parameter's type,
            // or Nothing where that type names the callee's type variables (the call then writes the type arguments
            // Kotlin cannot infer from a Nothing: KotlinExpressionPrinter.nullOnlyTypeArguments)
            KotlinContext.message(KotlinPrintCode.ASSERT_INTO_NON_NULL, value, KotlinContext.describe(value));
            if (!writable(target)) return new OutputBuilderImpl().add(printed).add(NOT_NULL);
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(printed).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                    .add(new TextImpl(KotlinTypeName.of(target.withNullable(NullableState.NULLABLE), q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS).add(NOT_NULL);
        }
        if (!targetTranslated && !unboxed || target == null || isNullable(target) || value instanceof NullConstant
            || !nullableInKotlin(value) && !nullableTypeVariable(value, target)) {
            return printed;
        }
        KotlinExpressionPrinter.Primitive from = KotlinExpressionPrinter.primitive(value.parameterizedType());
        if (target.arrays() == 0 && target.isPrimitiveExcludingVoid() && from != null
            && from != KotlinExpressionPrinter.primitive(target)) {
            return printed; // widened, x!!.toLong(), asserted there
        }
        io.codelaser.maddi.cst.api.info.MethodInfo method = KotlinContext.currentMethod();
        if (target.arrays() == 0 && target.typeParameter() != null && method != null
            && method.typeParameters().contains(target.typeParameter())
            && KotlinMethodPrinter.typeArgumentOfParameter(method, target.typeParameter())) {
            // getOrDefault's value ?: supplier.get() into its T: null where T is instantiated nullable, as in Java; an
            // unchecked cast, where `!!` would throw
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS)
                    .add(SymbolEnum.LEFT_PARENTHESIS).add(printed).add(SymbolEnum.RIGHT_PARENTHESIS)
                    .add(SpaceEnum.ONE).add(KotlinKeyword.AS)
                    .add(SpaceEnum.ONE)
                    .add(new TextImpl(KotlinTypeName.of(target, q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        ParameterizedType valueType = kotlinType(value);
        if (target.arrays() == 0 && unboundTypeVariable(target) && valueType != null && valueType.arrays() == 0
            && unboundTypeVariable(valueType)) {
            // Success<T>(response) with a T? response: a parameter typed by an unbounded type variable is not checked
            // at runtime, so the null reaches the callee as in Java, whose ensureNotNull throws what the caller
            // expects; `!!` would throw first. An unchecked cast to the value's own variable, erased; to the target's
            // where that is a supertype's, out of scope (get(type) of a Forwarding<K, V> superclass, its V a B here)
            KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, value, KotlinContext.describe(value));
            ParameterizedType castTo = KotlinContext.typeParameterInScope(valueType.typeParameter()) ? valueType : target;
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(printed).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                    .add(new TextImpl(KotlinTypeName.of(castTo.withNullable(NullableState.NONNULL), q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        KotlinContext.message(target.isPrimitiveExcludingVoid() ? KotlinPrintCode.ASSERT_AT_UNBOXING : KotlinPrintCode.ASSERT_INTO_NON_NULL,
                value, KotlinContext.describe(value));
        OutputBuilder assertedValue = KotlinExpressionPrinter.receiver(value, q).add(NOT_NULL);
        KotlinSmartCasts.assertedAt(value);
        return assertedValue;
    }

    /**
     * A value of an unbounded type variable, whose upper bound is Kotlin's {@code Any?}, into a declaration that is
     * neither nullable nor a type variable: {@code successWith(json, r.value)} with an {@code Any} parameter.
     */
    private static boolean nullableTypeVariable(Expression value, ParameterizedType target) {
        Expression unwrapped = KotlinExpressionPrinter.unwrap(value);
        if (!KotlinContext.translatingJava() || target.isTypeParameter() || target.isPrimitiveExcludingVoid()
            || !(unwrapped instanceof VariableExpression || unwrapped instanceof MethodCall)) {
            return false;
        }
        ParameterizedType type = kotlinType(value);
        if (type == null || !type.isTypeParameter() || type.arrays() > 0) return false;
        io.codelaser.maddi.cst.api.info.TypeParameter tp = type.typeParameter();
        // ArgumentMatchers.any(): the callee's own T, which Kotlin infers from the parameter
        if (unwrapped instanceof MethodCall mc && mc.methodInfo().typeParameters().contains(tp)) return false;
        if (tp.typeBounds().stream().anyMatch(b -> !b.isJavaLangObject())) return false;
        MethodInfo method = KotlinContext.currentMethod();
        return !tp.isMethodTypeParameter() || method == null || !method.typeParameters().contains(tp)
               || !KotlinMethodPrinter.nonNullBound(method, tp);
    }

    /** A type variable whose upper bound is Kotlin's {@code Any?}: no {@code T : Any} printed for it. */
    /** A type argument of {@code type}, at the top level, is an unbound type variable printed non-null. */
    private static boolean typeVariableArgument(ParameterizedType type) {
        return type.arrays() == 0 && type.parameters().stream()
                .anyMatch(a -> a != null && a.arrays() == 0 && unboundTypeVariable(a) && !isNullable(a));
    }

    /**
     * {@code value} is a chain of calls on a stream, one of which takes a lambda or method reference whose result
     * Kotlin types nullable ({@code map(BatchItemResult::response)} with a nullable response()): Kotlin infers the
     * elements nullable from it.
     */
    private static boolean nullableLambdaResultInChain(Expression value) {
        Expression e = KotlinExpressionPrinter.unwrap(value);
        while (e instanceof MethodCall mc) {
            if (mc.methodInfo() != null && "java.util.stream.Stream".equals(mc.methodInfo().typeInfo().fullyQualifiedName())) {
                for (Expression a : mc.parameterExpressions()) {
                    Expression u = KotlinExpressionPrinter.unwrap(a);
                    if (u instanceof io.codelaser.maddi.cst.api.expression.Lambda l && lambdaResultNullable(l)
                        || u instanceof MethodReference mr && mr.methodInfo() != null && !mr.methodInfo().isConstructor()
                           && isNullable(returnType(mr.methodInfo()))) {
                        return true;
                    }
                }
            }
            e = mc.object() == null ? null : KotlinExpressionPrinter.unwrap(mc.object());
        }
        return false;
    }

    private static boolean lambdaResultNullable(io.codelaser.maddi.cst.api.expression.Lambda l) {
        io.codelaser.maddi.cst.api.statement.Block body = l.methodInfo().methodBody();
        if (body == null || body.statements().size() != 1) return false;
        return body.statements().getFirst() instanceof io.codelaser.maddi.cst.api.statement.ReturnStatement rs
               && rs.expression() != null && nullableInKotlin(rs.expression());
    }

    private static boolean unboundTypeVariable(ParameterizedType type) {
        if (!type.isTypeParameter() || type.wildcard() != null) return false;
        io.codelaser.maddi.cst.api.info.TypeParameter tp = type.typeParameter();
        if (tp.typeBounds().stream().anyMatch(b -> !b.isJavaLangObject())) return false;
        if (!tp.isMethodTypeParameter()) return true;
        // a method's: only the current one's, whose bound is known here (a callee's may be printed T : Any)
        MethodInfo method = KotlinContext.currentMethod();
        return method != null && method.typeParameters().contains(tp) && !KotlinMethodPrinter.nonNullBound(method, tp);
    }

    /** What a for-each loops over: {@code xs!!} when Kotlin types it nullable (a loop over null throws in Java too). */
    static OutputBuilder iterable(Expression e, Qualification q) {
        if (!nullableInKotlin(e) || e instanceof NullConstant) return KotlinExpressionPrinter.print(e, q);
        KotlinContext.message(KotlinPrintCode.ASSERT_AT_DEREFERENCE, e, KotlinContext.describe(e));
        OutputBuilder iterated = KotlinExpressionPrinter.receiver(e, q).add(NOT_NULL);
        KotlinSmartCasts.assertedAt(e);
        return iterated;
    }

    /** An array that is indexed: {@code a!![i]} when Kotlin types it nullable ({@code ?.} cannot index a target). */
    static OutputBuilder asserted(Expression array, Qualification q) {
        return asserted(array, KotlinPrintCode.ASSERT_AT_DEREFERENCE, q);
    }

    /** As {@link #asserted(Expression, Qualification)}, the {@code !!} reported as {@code code}. */
    static OutputBuilder asserted(Expression e, KotlinPrintCode code, Qualification q) {
        OutputBuilder receiver = KotlinExpressionPrinter.receiver(e, q);
        if (!nullableInKotlin(e) || e instanceof NullConstant) return receiver;
        KotlinContext.message(code, e, KotlinContext.describe(e));
        KotlinSmartCasts.assertedAt(e);
        return receiver.add(NOT_NULL);
    }

    /** A receiver: {@code x!!.} or {@code x?.} when Kotlin types it nullable, else {@code x.}. */
    // the receiver of a call that KotlinNullCheckShapes prints as x?.m(); set while that call is printed
    private static final ThreadLocal<Expression[]> SAFE_RECEIVER = new ThreadLocal<>();

    /**
     * Print a call with {@code ?.} after {@code receiver}; null when the call does not print its receiver through
     * {@link #receiverWithDot} (a member Kotlin writes otherwise), so that the caller prints the conditional instead.
     */
    static OutputBuilder withSafeReceiver(Expression receiver, java.util.function.Supplier<OutputBuilder> call) {
        Expression[] previous = SAFE_RECEIVER.get();
        Expression[] slot = {KotlinExpressionPrinter.unwrap(receiver), null};
        SAFE_RECEIVER.set(slot);
        try {
            OutputBuilder printed = call.get();
            return slot[1] != null ? printed : null;
        } finally {
            SAFE_RECEIVER.set(previous);
        }
    }

    static OutputBuilder receiverWithDot(Expression object, Qualification q) {
        Expression[] safe = SAFE_RECEIVER.get();
        if (safe != null && safe[1] == null && KotlinExpressionPrinter.unwrap(object) == safe[0]) {
            safe[1] = object; // consumed: once
            return KotlinExpressionPrinter.receiver(object, q)
                    .add(new SymbolEnum("?.", SpaceEnum.NO_SPACE_SPLIT_ALLOWED, SpaceEnum.NONE, null));
        }
        OutputBuilder receiver = KotlinExpressionPrinter.receiver(object, q);
        if (!nullableInKotlin(object) || object instanceof NullConstant) return receiver.add(SymbolEnum.DOT);
        if (KotlinContext.options().nullCheck() == KotlinPrintOptions.NullCheck.SAFE_CALL) {
            KotlinContext.message(KotlinPrintCode.SAFE_CALL, object, KotlinContext.describe(object));
            return receiver.add(new SymbolEnum("?.", SpaceEnum.NO_SPACE_SPLIT_ALLOWED, SpaceEnum.NONE, null));
        }
        KotlinContext.message(KotlinPrintCode.ASSERT_AT_DEREFERENCE, object, KotlinContext.describe(object));
        KotlinSmartCasts.assertedAt(object);
        return receiver.add(NOT_NULL).add(SymbolEnum.DOT);
    }
}
