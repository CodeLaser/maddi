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
package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.InlineConditional;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.NullConstant;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.This;
import io.codelaser.maddi.cst.api.variable.Variable;

/**
 * Java's null-check conditionals that Kotlin writes with an operator (CodeLaser/maddi#140, item 1):
 * <ul>
 *     <li>{@code x != null ? x : d} and {@code x == null ? d : x} are {@code x ?: d};</li>
 *     <li>{@code x != null ? x.m(a) : null} and {@code x == null ? null : x.m(a)} are {@code x?.m(a)}; so for a
 *     field read, {@code x?.f}, and an array's length, {@code x?.size};</li>
 *     <li>{@code x != null ? f(x) : null}, x a local or a parameter, is {@code x?.let { f(it) }} (#164).</li>
 * </ul>
 * Only where the Kotlin means what the Java does. {@code x} is a read of a local, a parameter or a field of
 * {@code this}: Java reads it twice, Kotlin once, and nothing runs in between. The arguments of the call do not
 * mention it, as they would rely on a smart cast that {@code ?.} does not give them. And {@code x} is one that Kotlin
 * types nullable: on a non-null one the operator is dead code, and the conditional is printed as written.
 */
final class KotlinNullCheckShapes {

    private KotlinNullCheckShapes() {
    }

    /** {@code value ?: otherwise} */
    record Elvis(Expression value, Expression otherwise) {
    }

    /** {@code receiver?.call}: the call (or field read, or array length) is the conditional's non-null branch */
    record SafeCall(Expression receiver, Expression call) {
    }

    static Elvis elvis(InlineConditional ic) {
        Tested tested = tested(ic);
        if (tested == null) return null;
        Expression nonNull = unwrap(tested.whenNonNull());
        if (nonNull instanceof VariableExpression ve && ve.variable().equals(tested.variable().variable())) {
            return new Elvis(tested.variable(), tested.whenNull());
        }
        return null;
    }

    static SafeCall safeCall(InlineConditional ic) {
        Tested tested = tested(ic);
        if (tested == null || !(unwrap(tested.whenNull()) instanceof NullConstant)) return null;
        Expression nonNull = unwrap(tested.whenNonNull());
        // jenkins's ProxyConfiguration: jenkins != null ? jenkins.proxy : null, jenkins?.proxy
        Expression scope = nonNull instanceof VariableExpression fv && fv.variable() instanceof FieldReference fr
                           && !fr.fieldInfo().isStatic() && fr.scope() != null ? fr.scope()
                : nonNull instanceof io.codelaser.maddi.cst.api.expression.ArrayLength al ? al.scope() : null;
        if (scope != null) {
            return unwrap(scope) instanceof VariableExpression sv && sv.variable().equals(tested.variable().variable())
                    ? new SafeCall(scope, nonNull) : null;
        }
        if (!(nonNull instanceof MethodCall mc) || mc.objectIsImplicit() || mc.object() == null
            || !(unwrap(mc.object()) instanceof VariableExpression ve)
            || !ve.variable().equals(tested.variable().variable())) {
            return null;
        }
        Variable variable = ve.variable();
        for (Expression argument : mc.parameterExpressions()) {
            boolean[] mentions = {false};
            argument.visit((Element e) -> {
                if (e instanceof VariableExpression v && v.variable().equals(variable)) mentions[0] = true;
                return !mentions[0];
            });
            if (mentions[0]) return null;
        }
        return new SafeCall(mc.object(), mc);
    }

    /** {@code variable?.let { body }}: {@code body} with the variable as {@code it}, the conditional's non-null branch */
    record Let(VariableExpression variable, Expression body, boolean bodyWhenTrue) {
    }

    /**
     * {@code x != null ? f(x) : null}: jenkins's PluginManager, {@code context != null ?
     * ServletContextWrapper.toJakartaServletContext(context) : null}. Only a local or a parameter, which nothing in
     * the body can change between the test and its reads (a field could be, by a call in the body: #164's next step,
     * the analysis's non-modifying methods); not where the conditional is {@code x ?: null} or {@code x?.m()} already;
     * not where the body has a lambda or a nested let of its own, whose {@code it} would be ambiguous.
     */
    static Let let(InlineConditional ic) {
        Tested tested = tested(ic);
        if (tested == null || !(unwrap(tested.whenNull()) instanceof NullConstant)) return null;
        Variable variable = tested.variable().variable();
        if (!(variable instanceof LocalVariable) && !(variable instanceof ParameterInfo)) return null;
        Expression body = tested.whenNonNull();
        if (unwrap(body) instanceof VariableExpression ve && ve.variable().equals(variable)) return null;
        if (safeCall(ic) != null) return null;
        boolean[] mentions = {false};
        boolean[] blocked = {false};
        body.visit((Element e) -> {
            if (e instanceof VariableExpression v && v.variable().equals(variable)) mentions[0] = true;
            if (e instanceof io.codelaser.maddi.cst.api.expression.Lambda
                || e instanceof InlineConditional nested && nested != ic && let(nested) != null) {
                blocked[0] = true;
            }
            return !blocked[0];
        });
        if (!mentions[0] || blocked[0]) return null;
        return new Let(tested.variable(), body, unwrap(ic.ifTrue()) == unwrap(body));
    }

    private record Tested(VariableExpression variable, Expression whenNull, Expression whenNonNull) {
    }

    // the conditional tests a plain read for null; which branch has it non-null
    private static Tested tested(InlineConditional ic) {
        if (!KotlinContext.translatingJava()) return null;
        if (!(unwrap(ic.condition()) instanceof BinaryOperator bo) || bo.operator() == null) return null;
        String operator = bo.operator().name();
        if (!"==".equals(operator) && !"!=".equals(operator)) return null;
        Expression other = unwrap(bo.rhs()) instanceof NullConstant ? bo.lhs()
                : unwrap(bo.lhs()) instanceof NullConstant ? bo.rhs() : null;
        if (other == null || !(unwrap(other) instanceof VariableExpression ve) || !plainRead(ve.variable())) {
            return null;
        }
        if (!KotlinNullability.nullableInKotlin(ve)) return null;
        boolean nullWhenTrue = "==".equals(operator);
        return new Tested(ve, nullWhenTrue ? ic.ifTrue() : ic.ifFalse(), nullWhenTrue ? ic.ifFalse() : ic.ifTrue());
    }

    // a field of another object read through a local or parameter too: guava's ThreadFactoryBuilder,
    // builder.backingThreadFactory != null ? builder.backingThreadFactory : …, which Kotlin cannot smart-cast (a var)
    private static boolean plainRead(Variable variable) {
        return variable instanceof LocalVariable || variable instanceof ParameterInfo
               || variable instanceof FieldReference fr && (fr.scope() == null || fr.fieldInfo().isStatic()
                                                            || unwrap(fr.scope()) instanceof VariableExpression s
                                                               && (s.variable() instanceof This
                                                                   || s.variable() instanceof LocalVariable
                                                                   || s.variable() instanceof ParameterInfo));
    }

    /**
     * A value that Java may read twice where Kotlin reads it once, when nothing runs in between (#164): a field read as
     * in {@link #plainRead}, or a call without arguments of a method the analysis has non-modifying, on {@code this}
     * or a local or a parameter. fernflower's {@code first.getExprents()}.
     */
    static boolean readOnceAsTwice(Expression e) {
        Expression u = unwrap(e);
        if (u instanceof VariableExpression ve) return ve.variable() instanceof FieldReference && plainRead(ve.variable());
        return u instanceof MethodCall mc && mc.parameterExpressions().isEmpty() && mc.methodInfo() != null
               && KotlinContext.options().modification().nonModifying(mc.methodInfo()) && !mc.methodInfo().isStatic()
               && (mc.object() == null || unwrap(mc.object()) instanceof VariableExpression s
                                          && (s.variable() instanceof This || s.variable() instanceof LocalVariable
                                              || s.variable() instanceof ParameterInfo));
    }

    /** Both are a read of the same variable, or the same call of {@link #readOnceAsTwice} on the same receiver. */
    static boolean sameValue(Expression a, Expression b) {
        Expression ua = unwrap(a);
        Expression ub = unwrap(b);
        if (ua instanceof VariableExpression va) return ub instanceof VariableExpression vb && va.variable().equals(vb.variable());
        if (ua instanceof MethodCall ma && ub instanceof MethodCall mb && ma.methodInfo() != null
            && ma.methodInfo().equals(mb.methodInfo()) && ma.parameterExpressions().isEmpty()
            && mb.parameterExpressions().isEmpty()) {
            if (ma.object() == null || mb.object() == null) return ma.object() == mb.object();
            return sameValue(ma.object(), mb.object());
        }
        return false;
    }

    /** Some part of {@code e} is {@link #sameValue} as {@code value}. */
    static boolean mentions(Expression e, Expression value) {
        boolean[] found = {false};
        e.visit((Element element) -> {
            if (element instanceof Expression x && sameValue(x, value)) found[0] = true;
            return !found[0];
        });
        return found[0];
    }

    private static Expression unwrap(Expression e) {
        return KotlinExpressionPrinter.unwrap(e);
    }
}
