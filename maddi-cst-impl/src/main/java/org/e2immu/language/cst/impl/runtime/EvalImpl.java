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

package org.e2immu.language.cst.impl.runtime;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.runtime.Eval;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.expression.eval.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class EvalImpl implements Eval {
    private final EvalProduct evalProduct;
    private final EvalSum evalSum;
    private final EvalNegation evalNegation;
    private final EvalEquals evalEquals;
    private final EvalAnd evalAnd;
    private final EvalOr evalOr;
    private final EvalDivide evalDivide;
    private final EvalInlineConditional evalInlineConditional;
    private final EvalInequality evalInequality;
    private final EvalBinaryOperator evalBinaryOperator;
    private final EvalInstanceOf evalInstanceOf;
    private final EvalUnaryOperator evalUnaryOperator;
    private final EvalRemainder evalRemainder;
    private final EvalCast evalCast;
    private final EvalBoolean evalBoolean;

    public EvalImpl(Runtime runtime, EvalOptions evalOptions) {
        evalProduct = new EvalProduct(runtime);
        evalSum = new EvalSum(runtime);
        evalNegation = new EvalNegation(runtime);
        evalEquals = new EvalEquals(runtime);
        evalAnd = new EvalAnd(runtime, evalOptions);
        evalOr = new EvalOr(runtime, evalOptions);
        evalDivide = new EvalDivide(runtime);
        evalInlineConditional = new EvalInlineConditional(runtime);
        evalInequality = new EvalInequality(runtime);
        evalBinaryOperator = new EvalBinaryOperator(runtime);
        evalInstanceOf = new EvalInstanceOf(runtime);
        evalUnaryOperator = new EvalUnaryOperator(runtime);
        evalRemainder = new EvalRemainder(runtime);
        evalCast = new EvalCast(runtime);
        evalBoolean = new EvalBoolean(runtime);
    }

    @Override
    public Expression cast(Expression evaluated, Cast cast) {
        return evalCast.eval(evaluated, cast);
    }

    @Override
    public Expression instanceOf(Expression evaluated, InstanceOf instanceOf) {
        return evalInstanceOf.eval(evaluated, instanceOf);
    }

    @Override
    public Expression inlineConditional(Expression condition, Expression ifTrue, Expression ifFalse, Variable myself, boolean modifying) {
        return evalInlineConditional.eval(condition, ifTrue, ifFalse, myself, modifying);
    }

    @Override
    public Expression divide(Expression lhs, Expression rhs) {
        return evalDivide.divide(lhs, rhs);
    }

    @Override
    public Expression binaryOperator(Expression lhs, Expression rhs, BinaryOperator binaryOperator) {
        return evalBinaryOperator.eval(lhs, rhs, binaryOperator);
    }

    @Override
    public Expression unaryOperator(Expression expression, UnaryOperator unaryOperator) {
        return evalUnaryOperator.eval(expression, unaryOperator);
    }

    @Override
    public Expression product(Expression lhs, Expression rhs) {
        return evalProduct.eval(lhs, rhs);
    }

    @Override
    public Expression sum(Expression lhs, Expression rhs) {
        return evalSum.eval(lhs, rhs, true);
    }

    @Override
    public Expression negate(Expression expression) {
        return evalNegation.eval(expression);
    }

    @Override
    public Expression remainder(Expression lhs, Expression rhs) {
        return evalRemainder.remainder(lhs, rhs);
    }

    @Override
    public Expression equals(Expression lhs, Expression rhs) {
        return evalEquals.eval(lhs, rhs);
    }

    @Override
    public Expression equalsMethod(MethodCall methodCall, Expression lhs, Expression rhs) {
        return evalEquals.evalMethod(methodCall, lhs, rhs);
    }

    @Override
    public Expression greater(Expression lhs, Expression rhs, boolean allowEquals) {
        return evalInequality.greater(lhs, rhs, allowEquals);
    }

    @Override
    public Expression less(Expression lhs, Expression rhs, boolean allowEquals) {
        return evalInequality.less(lhs, rhs, allowEquals);
    }

    @Override
    public Expression wrapInProduct(Expression[] translated, int length) {
        return evalProduct.wrapInProduct(translated, length);
    }

    @Override
    public Expression wrapInSum(Expression[] translated, int length) {
        return evalSum.wrapInSum(translated, length);
    }

    @Override
    public Expression greaterThanZero(Expression expression, boolean allowEquals) {
        return evalInequality.greaterThanZero(expression, allowEquals);
    }

    @Override
    public Expression or(List<Expression> expressions) {
        return evalOr.eval(expressions);
    }

    @Override
    public Expression or(Expression... expressions) {
        return evalOr.eval(Arrays.stream(expressions).toList());
    }

    @Override
    public Expression and(Expression... expressions) {
        return evalAnd.eval(Arrays.stream(expressions).toList());
    }

    @Override
    public Expression and(List<Expression> expressions) {
        return evalAnd.eval(expressions);
    }

    @Override
    public boolean isNotNull0(Expression expression) {
        // for now, this is the only way we proceed
        return expression.parameterizedType().isPrimitiveExcludingVoid();
    }

    @Override
    public Stream<Expression> expandTerms(Expression expression, boolean negate) {
        return evalSum.expandTerms(expression, negate);
    }

    @Override
    public Stream<Expression> expandFactors(Expression expression) {
        return evalProduct.expandFactors(expression);
    }

    @Override
    public Expression removeClausesFromCondition(Expression expression, Expression clausesToRemove) {
        return evalBoolean.removeClauseFromCondition(expression, clausesToRemove);
    }

    @Override
    public Expression complementOfClausesInCondition(Expression condition, Expression clausesToExclude) {
        return evalBoolean.complementOfClausesInCondition(condition, clausesToExclude);
    }

    @Override
    public Expression complementOfConditions(List<Expression> conditions, Expression baseCondition) {
        return evalBoolean.complementOfConditions(baseCondition, conditions);
    }

    @Override
    public boolean conditionIsMoreSpecificThan(Expression lessSpecific, Expression moreSpecific, boolean allowEquals) {
        return evalBoolean.isMoreSpecificThan(lessSpecific, moreSpecific, allowEquals);
    }

    @Override
    public Expression combineCondition(Expression baseCondition, Expression clause) {
        return evalBoolean.combineCondition(baseCondition, clause);
    }

    @Override
    public boolean conditionIsNotMoreSpecificThanAnyOf(Expression condition, Collection<Expression> bases) {
        return evalBoolean.conditionIsNotMoreSpecificThanAnyOf(condition, bases);
    }

    @Override
    public boolean isNegationOf(Expression e1, Expression e2) {
        if (e1.parameterizedType().isBooleanOrBoxedBoolean()) {
            assert e2.parameterizedType().isBooleanOrBoxedBoolean();
            return evalBoolean.isNegationOf(e1, e2);
        }
        return e1.equals(evalNegation.eval(e2));
    }
}
