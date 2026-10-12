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
    private static final io.codelaser.maddi.cst.api.output.element.Symbol NOT_NULL =
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
        return verdict != null ? verdict : fieldInfo.type();
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
        ParameterizedType verdict = verdicts().parameter(parameterInfo);
        return verdict != null ? verdict : declared;
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
        if (verdict != null && isNullable(verdict)
            && (overridesMappedMember(methodInfo) || returnsNullForTypeVariable(methodInfo))) {
            // Kotlin declares the member it overrides, non-null: Any.toString(): String; Iterator<E>.next(): E
            return verdict.withNullable(NullableState.NONNULL);
        }
        return verdict != null ? verdict : methodInfo.returnType();
    }

    /**
     * A nullable override of a member returning a type variable that is not nullable itself: fernflower's
     * {@code FastSparseSetIterator<E>.next()} returns null past the end, but Kotlin's {@code Iterator<E>.next(): E}
     * cannot be overridden by {@code E?}. The override keeps {@code E}, and a returned null is cast to it
     * ({@link #typeVariableReturn}), unchecked, as Java's is.
     */
    static boolean returnsNullForTypeVariable(MethodInfo methodInfo) {
        if (!methodInfo.returnType().isTypeParameter() || methodInfo.returnType().arrays() > 0) return false;
        return methodInfo.overrides().stream().anyMatch(m -> m.returnType().isTypeParameter()
                && !isNullable(declaredReturnType(m)));
    }

    /** A translated method's return verdict; a library's or a Kotlin source's return type as declared. */
    private static ParameterizedType declaredReturnType(MethodInfo m) {
        ParameterizedType verdict = translated(m.typeInfo()) ? verdicts().returnType(m) : null;
        return verdict != null ? verdict : m.returnType();
    }

    /** {@code (value) as E}, for a return from a method {@link #returnsNullForTypeVariable}; null otherwise. */
    static OutputBuilder typeVariableReturn(MethodInfo methodInfo, Expression value, Qualification q) {
        if (!(value instanceof NullConstant) && !nullableInKotlin(value)) return null;
        ParameterizedType verdict = verdicts().returnType(methodInfo);
        if (verdict == null || !isNullable(verdict) || !returnsNullForTypeVariable(methodInfo)) return null;
        KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, value, KotlinContext.describe(value));
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(KotlinExpressionPrinter.print(value, q))
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
        try {
            ParameterizedType asIterable = type.typeInfo() == iterableType ? type
                    : type.concreteSuperType(iterableType.asParameterizedType());
            if (asIterable == null || asIterable.parameters().size() != 1) return null;
            ParameterizedType element = asIterable.parameters().getFirst();
            return element == null || element.wildcard() != null ? null : element;
        } catch (RuntimeException | AssertionError e) {
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

    /** The type of a local variable declared by {@code declaration}; remembered for the uses that follow. */
    static ParameterizedType localType(Element declaration, LocalVariable variable) {
        MethodInfo method = KotlinContext.currentMethod();
        // a verdict that is no decision (a degraded method's unreached local) is nullable, the choice that compiles;
        // without any verdict the declared type stands
        ParameterizedType verdict = method == null ? null
                : NullabilityFacts.decided(verdicts().local(method, declaration, variable));
        ParameterizedType type = verdict != null ? verdict : variable.parameterizedType();
        KotlinContext.localType(variable.simpleName(), type);
        return type;
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

    /** Kotlin will type this value as nullable. */
    static boolean nullableInKotlin(Expression e) {
        if (!KotlinContext.translatingJava()) return false; // Kotlin source wrote its own ?. and !!
        Expression x = KotlinExpressionPrinter.unwrap(e);
        return switch (x) {
            case NullConstant nc -> true;
            case MethodCall mc -> isNullable(throughReceiver(returnType(mc.methodInfo()), mc))
                                  || nullableJdkResult(mc.methodInfo());
            case VariableExpression ve -> switch (ve.variable()) {
                // Kotlin smart-casts a val property, never a var one: a fact about a var field does not hold there
                case FieldReference fr -> isNullable(fieldType(fr.fieldInfo()))
                                          && !(KotlinFieldPrinter.printsAsVal(fr.fieldInfo()) && knownNonNull(fr));
                case ParameterInfo pi -> isNullable(parameterType(pi)) && !knownNonNull(pi);
                case LocalVariable lv -> isNullable(KotlinContext.localType(lv.simpleName())) && !knownNonNull(lv);
                // nor does it smart-cast an element read: only the element's state counts
                case io.codelaser.maddi.cst.api.variable.DependentVariable dv -> {
                    ParameterizedType array = kotlinType(dv.arrayExpression());
                    yield array != null && array.arrays() > 0 && isNullable(array.componentType());
                }
                default -> false;
            };
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
            } catch (RuntimeException | AssertionError e) {
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
        if (seen == null || seen == functional || seen.typeInfo() == null || seen.arrays() > 0) return List.of();
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
        return KotlinContext.state().knownNonNull(verdicts(), variable);
    }

    static boolean nullableJdkResult(MethodInfo methodInfo) {
        if (methodInfo.isStatic()) return false;
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(methodInfo), methodInfo.overrides().stream())
                .anyMatch(m -> NULLABLE_JDK_RESULTS.getOrDefault(m.typeInfo().fullyQualifiedName(), Set.of())
                        .contains(m.name()));
    }

    /** Declared by the code being translated, so Kotlin will hold a value for it to its (non-)nullability. */
    static boolean translated(TypeInfo owner) {
        return !owner.compilationUnit().externalLibrary() && !KotlinTypePrinter.fromKotlinSource(owner);
    }

    /** {@code value}, asserted non-null when Kotlin types it nullable and {@code target} is a non-null declaration. */
    static OutputBuilder toTarget(Expression value, ParameterizedType target, boolean targetTranslated,
                                  OutputBuilder printed, Qualification q) {
        // a library's int parameter is Kotlin's Int all the same: Java unboxes an Integer into it (Math.min(map.get(k), 1))
        boolean unboxed = target != null && target.arrays() == 0 && target.isPrimitiveExcludingVoid()
                          && KotlinExpressionPrinter.primitive(target) == KotlinExpressionPrinter.primitive(value.parameterizedType());
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
            // expects; `!!` would throw first. An unchecked cast to the value's own variable, erased
            KotlinContext.message(KotlinPrintCode.UNCHECKED_CAST, value, KotlinContext.describe(value));
            return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(printed).add(SpaceEnum.ONE)
                    .add(KotlinKeyword.AS).add(SpaceEnum.ONE)
                    .add(new TextImpl(KotlinTypeName.of(valueType.withNullable(NullableState.NONNULL), q)))
                    .add(SymbolEnum.RIGHT_PARENTHESIS);
        }
        KotlinContext.message(target.isPrimitiveExcludingVoid() ? KotlinPrintCode.ASSERT_AT_UNBOXING : KotlinPrintCode.ASSERT_INTO_NON_NULL,
                value, KotlinContext.describe(value));
        return KotlinExpressionPrinter.receiver(value, q).add(NOT_NULL);
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
        return KotlinExpressionPrinter.receiver(e, q).add(NOT_NULL);
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
        return receiver.add(NOT_NULL);
    }

    /** A receiver: {@code x!!.} or {@code x?.} when Kotlin types it nullable, else {@code x.}. */
    static OutputBuilder receiverWithDot(Expression object, Qualification q) {
        OutputBuilder receiver = KotlinExpressionPrinter.receiver(object, q);
        if (!nullableInKotlin(object) || object instanceof NullConstant) return receiver.add(SymbolEnum.DOT);
        if (KotlinContext.options().nullCheck() == KotlinPrintOptions.NullCheck.SAFE_CALL) {
            KotlinContext.message(KotlinPrintCode.SAFE_CALL, object, KotlinContext.describe(object));
            return receiver.add(new SymbolEnum("?.", SpaceEnum.NO_SPACE_SPLIT_ALLOWED, SpaceEnum.NONE, null));
        }
        KotlinContext.message(KotlinPrintCode.ASSERT_AT_DEREFERENCE, object, KotlinContext.describe(object));
        return receiver.add(NOT_NULL).add(SymbolEnum.DOT);
    }
}
