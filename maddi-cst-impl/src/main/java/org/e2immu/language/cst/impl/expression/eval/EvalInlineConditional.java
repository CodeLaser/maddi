/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.expression.util.LhsRhs;

import java.util.concurrent.atomic.AtomicBoolean;

public class EvalInlineConditional {
    private final Runtime runtime;

    public EvalInlineConditional(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression eval(Expression condition, Expression ifTrue, Expression ifFalse,
                           Variable myself, boolean modifying) {
        if (condition.isBooleanConstant()) {
            boolean first = condition.isBoolValueTrue();
            return first ? ifTrue : ifFalse;
        }

        // not x ? a: b --> x ? b: a
        Negation negatedCondition;
        if ((negatedCondition = condition.asInstanceOf(Negation.class)) != null) {
            return eval(negatedCondition.expression(), ifFalse, ifTrue, myself, modifying);
        }

        Expression edgeCase = edgeCases(runtime, condition, ifTrue, ifFalse);
        if (edgeCase != null) return edgeCase;

        // NOTE that x, !x cannot always be detected by the presence of Negation (see GreaterThanZero,
        // x>=10 and x<=9 for integer x
        InlineConditional ifTrueCv;
        if ((ifTrueCv = ifTrue.asInstanceOf(InlineConditional.class)) != null) {
            // x ? (x? a: b): c === x ? a : c
            if (ifTrueCv.condition().equals(condition)) {
                return eval(condition, ifTrueCv.ifTrue(), ifFalse, myself, modifying);
            }
            // x ? (!x ? a: b): c === x ? b : c
            if (ifTrueCv.condition().equals(runtime.negate(condition))) {
                return eval(condition, ifTrueCv.ifFalse(), ifFalse, myself, modifying);
            }
            // x ? (y ? a: b): b --> x && y ? a : b
            // especially important for trailing x?(y ? z: <return variable>):<return variable>
            if (ifFalse.equals(ifTrueCv.ifFalse())) {
                return eval(
                        runtime.and(condition, ifTrueCv.condition()), ifTrueCv.ifTrue(), ifFalse, myself, modifying);
            }
            // x ? (y ? a: b): a --> x && !y ? b: a
            if (ifFalse.equals(ifTrueCv.ifTrue())) {
                return eval(
                        runtime.and(condition, runtime.negate(ifTrueCv.condition())),
                        ifTrueCv.ifFalse(), ifFalse, myself, modifying);
            }
        }
        // x? a: (x? b:c) === x?a:c
        InlineConditional ifFalseCv;
        if ((ifFalseCv = ifFalse.asInstanceOf(InlineConditional.class)) != null) {
            // x ? a: (x ? b:c) === x?a:c
            if (ifFalseCv.condition().equals(condition)) {
                return eval(condition, ifTrue, ifFalseCv.ifFalse(), myself, modifying);
            }
            // x ? a: (!x ? b:c) === x?a:b
            if (ifFalseCv.condition().equals(runtime.negate(condition))) {
                return eval(condition, ifTrue, ifFalseCv.ifTrue(), myself, modifying);
            }
            // x ? a: (y ? a: b) --> x || y ? a: b
            if (ifTrue.equals(ifFalseCv.ifTrue())) {
                return eval(
                        runtime.or(condition, ifFalseCv.condition()), ifTrue, ifFalseCv.ifFalse(), myself, modifying);
            }
            // x ? a: (y ? b: a) --> x || !y ? a: b
            if (ifTrue.equals(ifFalseCv.ifFalse())) {
                return eval(
                        runtime.or(condition, runtime.negate(ifFalseCv.condition())),
                        ifTrue, ifFalseCv.ifTrue(), myself, modifying);
            }
            // 4==i ? a : 3==i ? b : c --> 3==i first, then 4==i
            if (hiddenSwitch(condition, ifFalseCv.condition()) && condition.compareTo(ifFalseCv.condition()) > 0) {
                if (!modifying) {
                    // swap
                    Expression result = eval(condition, ifTrue, ifFalseCv.ifFalse(), myself, modifying);
                    return runtime.newInlineConditional(ifFalseCv.condition(), ifFalseCv.ifTrue(),
                            result);
                }
            }
        }

        // x&y ? (y&z ? a : b) : c --> x&y ? (z ? a : b) : c
        InlineConditional ifTrueInline;
        And and1;
        if ((ifTrueInline = ifTrue.asInstanceOf(InlineConditional.class)) != null
            && (and1 = ifTrueInline.condition().asInstanceOf(And.class)) != null) {
            Expression ifTrueCondition = removeCommonClauses(runtime, condition, and1);
            if (ifTrueCondition != ifTrueInline.condition()) {
                return eval(
                        condition, runtime.newInlineConditional(ifTrueCondition,
                                ifTrueInline.ifTrue(), ifTrueInline.ifFalse()), ifFalse, myself, modifying);
            }
        }


        // x ? x||y : z   -->   x ? true : z   --> x||z
        // x ? !x||y : z  -->   x ? y : z
        Or or1;
        if ((or1 = ifTrue.asInstanceOf(Or.class)) != null) {
            if (or1.expressions().contains(condition)) {
                return runtime.or(condition, ifFalse);
            }
            Expression notCondition = runtime.negate(condition);
            if (or1.expressions().contains(notCondition)) {
                Expression newOr = runtime.or(or1.expressions().stream()
                        .filter(e -> !e.equals(notCondition)).toList());
                return eval(condition, newOr, ifFalse, myself, modifying);
            }
        }
        // x ? y : x||z --> x ? y: z
        // x ? y : !x||z --> x ? y: true --> !x || y
        Or or2;
        if ((or2 = ifFalse.asInstanceOf(Or.class)) != null) {
            if (or2.expressions().contains(condition)) {
                Expression newOr = runtime.or(or2.expressions().stream()
                        .filter(e -> !e.equals(condition)).toList());
                return eval(condition, ifTrue, newOr, myself, modifying);
            }
            Expression notCondition = runtime.negate(condition);
            if (or2.expressions().contains(notCondition)) {
                return runtime.or(notCondition, ifTrue);
            }
        }
        // x ? x&&y : z --> x ? y : z
        // x ? !x&&y : z --> x ? false : z --> !x && z
        And and2;
        if ((and2 = ifTrue.asInstanceOf(And.class)) != null) {
            if (and2.expressions().contains(condition)) {
                Expression newAnd = runtime.and(
                        and2.expressions().stream().filter(e -> !e.equals(condition)).toArray(Expression[]::new));
                return eval(condition, newAnd, ifFalse, myself, modifying);
            }
            Expression notCondition = runtime.negate(condition);
            if (and2.expressions().contains(notCondition)) {
                return runtime.and(notCondition, ifFalse);
            }
        }
        // x ? y : !x&&z => x ? y : z
        // x ? y : x&&z --> x ? y : false --> x && y
        And and3;
        if ((and3 = ifFalse.asInstanceOf(And.class)) != null) {
            if (and3.expressions().contains(condition)) {
                return runtime.and(condition, ifTrue);
            }
            Expression notCondition = runtime.negate(condition);
            if (and3.expressions().contains(notCondition)) {
                Expression newAnd = runtime.and(
                        and3.expressions().stream().filter(e -> !e.equals(notCondition)).toArray(Expression[]::new));
                return eval(condition, ifTrue, newAnd, myself, modifying);
            }
        }

        // myself == x ? x : y --> y
        Equals equals;
        if ((equals = condition.asInstanceOf(Equals.class)) != null && myself != null) {
            VariableExpression vel;
            VariableExpression ver;
            if ((vel = equals.lhs().asInstanceOf(VariableExpression.class)) != null
                && vel.variable().equals(myself) && ifTrue.equals(equals.rhs())
                || (ver = equals.rhs().asInstanceOf(VariableExpression.class)) != null
                   && ver.variable().equals(myself) && ifTrue.equals(equals.lhs())) {
                return ifFalse;
            }
        }

        GreaterThanZero ge0;
        Sum sum;
        if (runtime.configuration().isNormalizeMore()
            && (ge0 = condition.asInstanceOf(GreaterThanZero.class)) != null
            && (sum = ge0.expression().asInstanceOf(Sum.class)) != null) {
            /*
            if lhs is negative, and rhs is positive, keep: a<=b?t:f === -a+b>0?t:f
            if lhs is positive, and rhs is negative, swap: b>a?t:f  === a-b>0?t:f  === a<=b?f:t
             */
            boolean lhsNegative = sum.lhs().isNegatedOrNumericNegative();
            boolean rhsNegative = sum.rhs().isNegatedOrNumericNegative();
            if (!lhsNegative && rhsNegative) {
                Expression newCondition = runtime.negate(condition);
                return runtime.newInlineConditional(newCondition, ifFalse, ifTrue);
            }
        }

        // standardization... we swap!
        // this will result in  a != null ? a: x ==>  null == a ? x : a as the default form
        return runtime.newInlineConditional(condition, ifTrue, ifFalse);
        // TODO more advanced! if a "large" part of ifTrue or ifFalse appears in condition, we should create a temp variable
    }

    private static Expression removeCommonClauses(Runtime runtime, Expression condition, And and) {
        Expression[] filtered = and.expressions().stream().filter(e -> !inExpression(e, condition)).toArray(Expression[]::new);
        if (filtered.length == and.expressions().size()) return and;
        return runtime.and(filtered);
    }

    private static boolean inExpression(Expression e, Expression container) {
        And and;
        if ((and = container.asInstanceOf(And.class)) != null) {
            return and.expressions().contains(e);
        }
        return container.equals(e);
    }

    private static boolean hiddenSwitch(Expression c1, Expression c2) {
        Equals eq1;
        Equals eq2;
        if ((eq1 = c1.asInstanceOf(Equals.class)) != null
            && (eq2 = c2.asInstanceOf(Equals.class)) != null
            && eq1.lhs().isConstant() && eq2.lhs().isConstant() && !eq1.lhs().equals(eq2.lhs())) {
            return eq1.rhs().equals(eq2.rhs());
        }
        MethodCall mc1;
        MethodCall mc2;
        if ((mc1 = c1.asInstanceOf(MethodCall.class)) != null && (mc2 = c2.asInstanceOf(MethodCall.class)) != null) {
            if (mc1.methodInfo().isOverloadOfJLOEquals() && mc2.methodInfo().isOverloadOfJLOEquals()) {
                {
                    if (mc1.object().equals(mc2.object())) {
                        // x.equals(...), x.equals(...)
                        ConstantExpression<?> ce1 = mc1.parameterExpressions().get(0).asInstanceOf(ConstantExpression.class);
                        ConstantExpression<?> ce2 = mc2.parameterExpressions().get(0).asInstanceOf(ConstantExpression.class);
                        return ce1 != null && ce2 != null && !ce1.equals(ce2);
                    }
                }
                {
                    ConstantExpression<?> ce1 = mc1.object().asInstanceOf(ConstantExpression.class);
                    ConstantExpression<?> ce2 = mc2.object().asInstanceOf(ConstantExpression.class);
                    if (ce1 != null && ce2 != null && !ce1.equals(ce2)) {
                        // "a".equals(...), "b".equals(...)
                        Expression p01 = mc1.parameterExpressions().get(0);
                        Expression p02 = mc2.parameterExpressions().get(0);
                        return p01.equals(p02);
                    }
                }
            }
        }
        return false;
    }

    private static boolean containsEmpty(Expression expression) {
        AtomicBoolean found = new AtomicBoolean();
        expression.visit(e -> {
            if (e instanceof EmptyExpression) {
                found.set(true);
            }
            return !found.get();
        });
        return found.get();
    }

    private static Expression edgeCases(Runtime runtime,
                                        Expression condition, Expression ifTrue, Expression ifFalse) {
        if (containsEmpty(ifTrue) || containsEmpty(ifFalse)) {
            /*
             The inline conditional system is used to construct inline conditionals such as x?<no return value>:false
             in the context of constructing a correct return value (e2immu, but also jfocus).
             The edge cases are not applicable in that specific situation.
             */
            return null;
        }
        // x ? a : a == a
        if (ifTrue.equals(ifFalse)) return ifTrue;
        // a ? a : !a == a == !a ? !a : a
        if (condition.equals(ifTrue) && condition.equals(runtime.negate(ifFalse))) {
            return runtime.constantTrue();
        }
        // !a ? a : !a == !a == a ? !a : a --> will not happen, as we've already swapped

        // a ? true: b  --> a || b
        // a ? b : true --> !a || b
        // a ? b : false --> a && b
        // a ? false: b --> !a && b
        if (ifTrue.isBooleanConstant()) {
            if (ifTrue.isBoolValueTrue()) {
                return runtime.or(condition, ifFalse);
            }
            return runtime.and(runtime.negate(condition), ifFalse);
        }
        if (ifFalse.isBooleanConstant()) {
            if (ifFalse.isBoolValueTrue()) {
                return runtime.or(runtime.negate(condition), ifTrue);
            }
            // ifTrue must be boolean too...
            return runtime.and(condition, ifTrue);
        }

        // x ? a : a --> a, but only if x is not modifying TODO needs implementing!!
        if (ifTrue.equals(ifFalse)) {// && evaluationContext.getProperty(condition, VariableProperty.MODIFIED) == Level.FALSE) {
            return ifTrue;
        }

        LhsRhs eq = LhsRhs.equalsMethodCall(condition);
        // a.equals(b) ? a : b ---> b
        if (eq != null && ifTrue.equals(eq.lhs()) && ifFalse.equals(eq.rhs())) {
            return ifFalse;
        }

        // a == b ? a : b ---> b
        Equals equals;
        if ((equals = condition.asInstanceOf(Equals.class)) != null
            && ifTrue.equals(equals.lhs()) && ifFalse.equals(equals.rhs())) {
            return ifFalse;
        }
        // a == b ? b : a --> a
        Equals equals2;
        if ((equals2 = condition.asInstanceOf(Equals.class)) != null
            && ifTrue.equals(equals2.rhs()) && ifFalse.equals(equals2.lhs())) {
            return ifFalse;
        }
        return null;
    }
}
