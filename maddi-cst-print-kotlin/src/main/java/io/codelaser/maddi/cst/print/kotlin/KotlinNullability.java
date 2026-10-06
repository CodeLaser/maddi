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
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;

import java.util.Map;
import java.util.Set;

/**
 * The {@link NullabilityVerdicts} applied: a declaration's type as the verdict has it, and at a use site, the
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

    /** JDK methods that return null for a missing element; their Kotlin signatures say so (V?, E?). */
    private static final Map<String, Set<String>> NULLABLE_JDK_RESULTS = Map.of(
            "java.util.Map", Set.of("get", "remove", "put", "putIfAbsent"),
            "java.util.Queue", Set.of("poll", "peek"),
            "java.util.Deque", Set.of("pollFirst", "pollLast", "peekFirst", "peekLast"));

    private KotlinNullability() {
    }

    private static NullabilityVerdicts verdicts() {
        return KotlinContext.options().verdicts();
    }

    static ParameterizedType fieldType(FieldInfo fieldInfo) {
        ParameterizedType verdict = verdicts().field(fieldInfo);
        return verdict != null ? verdict : fieldInfo.type();
    }

    static ParameterizedType parameterType(ParameterInfo parameterInfo) {
        ParameterizedType verdict = verdicts().parameter(parameterInfo);
        if (verdict != null) return verdict;
        ParameterizedType declared = parameterInfo.parameterizedType();
        return isBoxed(declared) && boxedClash(parameterInfo.methodInfo())
                ? declared.withNullable(NullableState.NULLABLE) : declared;
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
        ParameterizedType verdict = verdicts().returnType(methodInfo);
        return verdict != null ? verdict : methodInfo.returnType();
    }

    /** The type of a local variable declared by {@code declaration}; remembered for the uses that follow. */
    static ParameterizedType localType(Element declaration, LocalVariable variable) {
        MethodInfo method = KotlinContext.currentMethod();
        ParameterizedType verdict = method == null ? null : verdicts().local(method, declaration, variable);
        ParameterizedType type = verdict != null ? verdict : variable.parameterizedType();
        KotlinContext.localType(variable.simpleName(), type);
        return type;
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
            case MethodCall mc -> isNullable(returnType(mc.methodInfo())) || nullableJdkResult(mc.methodInfo());
            case VariableExpression ve -> switch (ve.variable()) {
                case FieldReference fr -> isNullable(fieldType(fr.fieldInfo()));
                case ParameterInfo pi -> isNullable(parameterType(pi));
                case LocalVariable lv -> isNullable(KotlinContext.localType(lv.simpleName()));
                default -> false;
            };
            case InlineConditional ic -> nullableInKotlin(ic.ifTrue()) || nullableInKotlin(ic.ifFalse());
            default -> false;
        };
    }

    private static boolean nullableJdkResult(MethodInfo methodInfo) {
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
        if (!targetTranslated || target == null || isNullable(target) || value instanceof NullConstant
            || !nullableInKotlin(value)) {
            return printed;
        }
        return KotlinExpressionPrinter.receiver(value, q).add(NOT_NULL);
    }

    /** What a for-each loops over: {@code xs!!} when Kotlin types it nullable (a loop over null throws in Java too). */
    static OutputBuilder iterable(Expression e, Qualification q) {
        if (!nullableInKotlin(e) || e instanceof NullConstant) return KotlinExpressionPrinter.print(e, q);
        return KotlinExpressionPrinter.receiver(e, q).add(NOT_NULL);
    }

    /** A receiver: {@code x!!.} or {@code x?.} when Kotlin types it nullable, else {@code x.}. */
    static OutputBuilder receiverWithDot(Expression object, Qualification q) {
        OutputBuilder receiver = KotlinExpressionPrinter.receiver(object, q);
        if (!nullableInKotlin(object) || object instanceof NullConstant) return receiver.add(SymbolEnum.DOT);
        if (KotlinContext.options().nullCheck() == KotlinPrintOptions.NullCheck.SAFE_CALL) {
            return receiver.add(new SymbolEnum("?.", SpaceEnum.NO_SPACE_SPLIT_ALLOWED, SpaceEnum.NONE, null));
        }
        return receiver.add(NOT_NULL).add(SymbolEnum.DOT);
    }
}
