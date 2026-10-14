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

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.Cast;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.InlineConditional;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.NullConstant;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.output.OutputBuilder;
import io.codelaser.maddi.cst.api.output.Qualification;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.type.NullableState;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.output.OutputBuilderImpl;
import io.codelaser.maddi.cst.impl.output.SpaceEnum;
import io.codelaser.maddi.cst.impl.output.SymbolEnum;

import java.util.List;

/**
 * Where C#'s nullable reference types need a {@code !}: when the analysis's facts say the program's nullability is
 * known ({@link CSharpFacts#nullabilityAnalysed}), a dereference of a value that may be null is {@code x!.m()}. Java
 * throws a NullPointerException there if it is null, and C# a NullReferenceException; the {@code !} only tells the
 * compiler that the code means it, so that it does not warn (CS8602). A value is one that may be null:
 * <ul>
 *   <li>the result of a call whose method may return null, unless the analysis knows this call's result is not;</li>
 *   <li>a field, parameter or local the analysis finds nullable, unless it knows that the variable is not null where
 *   the statement or the call starts (after {@code if (v == null) return;}, inside {@code if (v != null)}): C#'s flow
 *   analysis knows those as well.</li>
 * </ul>
 * Without such facts nothing changes.
 */
final class CSharpNullability {

    /** {@code !}, glued to its operand. */
    static final SymbolEnum NULL_FORGIVING = new SymbolEnum("!", SpaceEnum.NONE, SpaceEnum.NONE, null);

    private CSharpNullability() {
    }

    private static CSharpFacts facts() {
        return CSharpContext.program().facts();
    }

    /** Remembers the nullability of a local just declared by {@code declaration}, for its uses. */
    static void declared(Element declaration, LocalVariable variable) {
        MethodInfo method = CSharpContext.currentMethod();
        if (method == null || !facts().nullabilityAnalysed()) return;
        CSharpContext.localState(variable.simpleName(), facts().nullable(method, declaration, variable));
    }

    /**
     * A local's initializer, {@code printed}: with {@code !} when the analysis asserts the local at its declaration
     * ({@link CSharpFacts#assertedAtDeclaration}) and the value may be null.
     */
    static OutputBuilder initializer(Element declaration, LocalVariable variable, Expression init, OutputBuilder printed) {
        MethodInfo method = CSharpContext.currentMethod();
        if (method == null || !facts().nullabilityAnalysed() || !mayBeNull(init)
            || !facts().assertedAtDeclaration(method, declaration, variable)) {
            return printed;
        }
        return asserted(init, printed);
    }

    /**
     * An argument, {@code printed}, for the {@code i}-th parameter of {@code method}: with {@code !} when it may be
     * null and the parameter is not (CS8604). Java passes the null on, and so does C#: the {@code !} does not throw.
     * A library's parameter is nullable only when the analysis's hints say so; a type argument C# prints is not
     * nullable either ({@code List<Exprent>}), so that {@code list.Add(x!)} is the write the analysis asserts. A
     * collection's element or key ({@code elementOrKey}: Java's {@code contains(Object)}, C#'s {@code Contains(T)})
     * is not nullable whatever the hints, and there even a {@code null} is {@code null!}.
     */
    static OutputBuilder intoParameter(Expression value, MethodInfo method, int i, boolean elementOrKey,
                                       OutputBuilder printed) {
        if (method == null || method.parameters().isEmpty() || !facts().nullabilityAnalysed()) return printed;
        boolean nullConstant = CSharpExpressionPrinter.unwrap(value) instanceof NullConstant;
        if (nullConstant ? !elementOrKey : !mayBeNull(value)) return printed;
        io.codelaser.maddi.cst.api.info.ParameterInfo parameter =
                method.parameters().get(Math.min(i, method.parameters().size() - 1));
        io.codelaser.maddi.cst.api.type.ParameterizedType type = parameter.parameterizedType();
        io.codelaser.maddi.cst.api.type.ParameterizedType valueType = value.parameterizedType();
        // a boxed value is unboxed into a primitive or a type parameter (Dictionary<int, V>'s K): its cast asserts it
        boolean unboxed = valueType != null && valueType.arrays() == 0 && valueType.isBoxedExcludingVoid()
                          && type.arrays() == 0 && (type.isPrimitiveExcludingVoid() || type.isTypeParameter());
        if (unboxed || type.isPrimitiveExcludingVoid() && type.arrays() == 0
            || !elementOrKey && facts().nullable(parameter) == NullableState.NULLABLE) {
            return printed;
        }
        return asserted(value, printed);
    }

    static OutputBuilder intoParameter(Expression value, MethodInfo method, int i, OutputBuilder printed) {
        return intoParameter(value, method, i, false, printed);
    }

    /**
     * The value of a {@code return} in the current method, {@code printed}: with {@code !} when it may be null, a
     * {@code null} too, and the method's result is not nullable (CS8603). A lambda's result is a delegate's type
     * argument, which C# prints without {@code ?}: as a collection's element, it is asserted.
     */
    static OutputBuilder intoReturn(Expression value, OutputBuilder printed) {
        MethodInfo method = CSharpContext.currentMethod();
        if (method == null || !facts().nullabilityAnalysed() || !nullOrMayBeNull(value)
            || valueType(method.returnType())) {
            return printed;
        }
        boolean lambda = method.typeInfo().isAnonymous();
        if (!lambda && facts().nullable(method) == NullableState.NULLABLE) return printed;
        return asserted(value, printed);
    }

    /**
     * The value assigned to {@code target}, {@code printed}: with {@code !} when it may be null, a {@code null} too,
     * and the target is not nullable (CS8625, CS8601): an array's element, which C# prints without {@code ?}
     * ({@code arr[i] = null!} holds the null as Java's does), or a field the analysis finds non-null. A local's type
     * is {@code T?} already.
     */
    static OutputBuilder intoVariable(Expression value, Variable target, OutputBuilder printed) {
        if (!facts().nullabilityAnalysed() || !nullOrMayBeNull(value) || valueType(target.parameterizedType())) {
            return printed;
        }
        boolean nonNull = switch (target) {
            case DependentVariable dv -> true;
            case FieldReference fr -> facts().nullable(fr.fieldInfo()) != NullableState.NULLABLE;
            default -> false;
        };
        return nonNull ? asserted(value, printed) : printed;
    }

    /** An element of an array initializer, {@code printed}: as an assignment to an element ({@link #intoVariable}). */
    static OutputBuilder intoElement(Expression value, io.codelaser.maddi.cst.api.type.ParameterizedType element,
                                     OutputBuilder printed) {
        if (!facts().nullabilityAnalysed() || !nullOrMayBeNull(value) || valueType(element)) return printed;
        return asserted(value, printed);
    }

    /**
     * The operand of an unboxing cast, {@code (int) x}, {@code printed} as a receiver: with {@code !} when it may be null (CS8629,
     * CS8605); the cast throws there, as Java's unboxing does. A boxed type is C#'s {@code int?} whatever the
     * analysis says of its declaration, so only a use-site fact makes it non-null.
     */
    static OutputBuilder unboxed(Expression operand, OutputBuilder printed) {
        if (!facts().nullabilityAnalysed()) return printed;
        Expression x = CSharpExpressionPrinter.unwrap(operand);
        io.codelaser.maddi.cst.api.type.ParameterizedType type = x.parameterizedType();
        boolean boxed = type != null && type.arrays() == 0 && type.isBoxedExcludingVoid();
        boolean mayBe = !boxed ? mayBeNull(x) : switch (x) {
            // a method of Integer is C#'s int?; one of a type parameter its argument, List<int>'s int, unless nullable
            case MethodCall mc -> mc.methodInfo().returnType().isBoxedExcludingVoid() || mayBeNull(mc);
            case VariableExpression ve -> !knownNonNull(ve.variable());
            case Cast c -> !(CSharpExpressionPrinter.unwrap(c.expression()).parameterizedType() instanceof
                    io.codelaser.maddi.cst.api.type.ParameterizedType t && t.isPrimitiveExcludingVoid());
            default -> false;
        };
        return mayBe ? printed.add(NULL_FORGIVING) : printed;
    }

    private static boolean nullOrMayBeNull(Expression value) {
        return CSharpExpressionPrinter.unwrap(value) instanceof NullConstant || mayBeNull(value);
    }

    /** A primitive or a boxed type, C#'s value type or its {@code T?}: no {@code !} there. */
    private static boolean valueType(io.codelaser.maddi.cst.api.type.ParameterizedType type) {
        return type == null || type.arrays() == 0 && (type.isPrimitiveExcludingVoid() || type.isVoid()
                                                      || type.isBoxedExcludingVoid());
    }

    /** {@code printed!}, in parentheses unless it is a call, a variable or null. */
    private static OutputBuilder asserted(Expression value, OutputBuilder printed) {
        Expression x = CSharpExpressionPrinter.unwrap(value);
        if (x instanceof MethodCall || x instanceof VariableExpression || x instanceof NullConstant) {
            return printed.add(NULL_FORGIVING);
        }
        return new OutputBuilderImpl().add(SymbolEnum.LEFT_PARENTHESIS).add(printed).add(SymbolEnum.RIGHT_PARENTHESIS)
                .add(NULL_FORGIVING);
    }

    /** The receiver of {@code .member}, {@code [i]} or a {@code foreach}: with {@code !} when it may be null. */
    static OutputBuilder dereferenced(Expression e, Qualification q) {
        OutputBuilder receiver = CSharpExpressionPrinter.receiver(e, q);
        if (!mayBeNull(e)) return receiver;
        return receiver.add(NULL_FORGIVING);
    }

    /** C#'s compiler will think this value may be null here. */
    static boolean mayBeNull(Expression e) {
        if (!facts().nullabilityAnalysed()) return false;
        Expression x = CSharpExpressionPrinter.unwrap(e);
        return switch (x) {
            case NullConstant nc -> false; // null.m() is no code worth helping
            // not facts().callResultNonNull(mc): C# types the call by the method's declared T?, whatever this call
            // passes, and warns without the ! (fernflower's lstEdgeClosures[i] = GetMaxBreakLift(...), CS8601)
            case MethodCall mc -> !mc.methodInfo().isVoid()
                                  && (facts().nullable(mc.methodInfo()) == NullableState.NULLABLE
                                      || CSharpExpressionPrinter.nullableResult(mc));
            case VariableExpression ve -> switch (ve.variable()) {
                case FieldReference fr -> facts().nullable(fr.fieldInfo()) == NullableState.NULLABLE
                                          && !knownNonNull(fr);
                case io.codelaser.maddi.cst.api.info.ParameterInfo pi ->
                        facts().nullable(pi) == NullableState.NULLABLE && !knownNonNull(pi);
                case LocalVariable lv -> CSharpContext.localState(lv.simpleName()) == NullableState.NULLABLE
                                         && !knownNonNull(lv);
                default -> false;
            };
            case InlineConditional ic -> branchMayBeNull(ic.ifTrue()) || branchMayBeNull(ic.ifFalse());
            case Cast c -> !c.parameterizedType().isPrimitiveExcludingVoid() && mayBeNull(c.expression());
            default -> false;
        };
    }

    /** A branch of a conditional: its null makes the conditional's value one that may be null. */
    private static boolean branchMayBeNull(Expression branch) {
        return CSharpExpressionPrinter.unwrap(branch) instanceof NullConstant || mayBeNull(branch);
    }

    /** A use-site fact: known non-null where the current statement starts, or at the current call. */
    private static boolean knownNonNull(io.codelaser.maddi.cst.api.variable.Variable variable) {
        return CSharpContext.state().knownNonNull(facts(), variable);
    }
}
