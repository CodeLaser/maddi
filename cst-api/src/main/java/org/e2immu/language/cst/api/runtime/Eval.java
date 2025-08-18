package org.e2immu.language.cst.api.runtime;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/*
All recursion takes place in sortAndSimplify, when required.
None of the abstract methods should call sortAndSimplify again.
 */
public interface Eval {

    // computes baseCondition & clause,  meant for when clause is not necessarily more specific than baseCondition
    Expression combineCondition(Expression baseCondition, Expression clause);

    // base = A&B, condition = A&B&C -> true
    //             condition = A&D   -> false
    // this computes base & condition == condition
    boolean conditionIsMoreSpecificThan(Expression lessSpecific, Expression moreSpecific, boolean allowEquals);

    // computes or(bases) && condition == false
    boolean conditionIsNotMoreSpecificThanAnyOf(Expression condition, Collection<Expression> bases);

    // given base A&B, conditions for assignments A&B&C, A&B&!D, return A&B&(!C || D)
    // each of the conditions MUST be more specific than the base condition

    Expression complementOfConditions(List<Expression> conditions, Expression baseCondition);

    boolean isNegationOf(Expression e1, Expression e2);

    // return A&B given condition = A&B&C, clauseToRemove = C
    // group of methods to deal with boolean expressions
    Expression removeClausesFromCondition(Expression condition, Expression clauseToRemove);

    // computes expression && !clauseToExclude
    // return A&B&!C given condition A&B&C, clauseToExclude = C
    // return false given condition A&B&C, clauseToExclude = D
    // clauseToExclude does not need to be part of the condition, in which case we return FALSE
    Expression complementOfClausesInCondition(Expression condition, Expression clauseToExclude);

    // end of methods for boolean expressions

    default Expression cast(Cast cast) {
        return cast(cast.expression(), cast);
    }

    Expression cast(Expression evaluated, Cast cast);

    default Expression instanceOf(InstanceOf instanceOf) {
        return instanceOf(instanceOf.expression(), instanceOf);
    }

    Expression instanceOf(Expression evaluated, InstanceOf instanceOf);

    Expression inlineConditional(Expression condition, Expression ifTrue, Expression ifFalse,
                                 Variable myself, boolean modifying);

    Expression product(Expression lhs, Expression rhs);

    Expression sum(Expression lhs, Expression rhs);

    Expression negate(Expression expression);

    Expression remainder(Expression lhs, Expression rhs);

    Expression equals(Expression lhs, Expression rhs);

    Expression equalsMethod(MethodCall methodCall, Expression lhs, Expression rhs);

    Expression greater(Expression lhs, Expression rhs, boolean allowEquals);

    Expression greaterThanZero(Expression expression, boolean allowEquals);

    Expression or(List<Expression> expressions);

    Expression or(Expression... expressions);

    Expression and(Expression... expressions);

    Expression and(List<Expression> expressions);

    Expression divide(Expression lhs, Expression rhs);

    default Expression binaryOperator(BinaryOperator binaryOperator) {
        return binaryOperator(binaryOperator.lhs(), binaryOperator.rhs(), binaryOperator);
    }

    Expression binaryOperator(Expression evaluatedLhs, Expression evaluatedRhs, BinaryOperator binaryOperator);

    default Expression unaryOperator(UnaryOperator unaryOperator) {
        return unaryOperator(unaryOperator.expression(), unaryOperator);
    }

    Expression unaryOperator(Expression evaluated, UnaryOperator unaryOperator);

    default Expression sortAndSimplify(boolean recurse, Expression expression) {
        if (expression instanceof Sum sum) {
            Expression lhs = recurse ? sortAndSimplify(true, sum.lhs()) : sum.lhs();
            Expression rhs = recurse ? sortAndSimplify(true, sum.rhs()) : sum.rhs();
            return sum(lhs, rhs);
        }
        if (expression instanceof Product product) {
            Expression lhs = recurse ? sortAndSimplify(true, product.lhs()) : product.lhs();
            Expression rhs = recurse ? sortAndSimplify(true, product.rhs()) : product.rhs();
            return product(lhs, rhs);
        }
        if (expression instanceof InlineConditional i) {
            Expression condition = recurse ? sortAndSimplify(true, i.condition()) : i.condition();
            Expression ifTrue = recurse ? sortAndSimplify(true, i.ifTrue()) : i.ifTrue();
            Expression ifFalse = recurse ? sortAndSimplify(true, i.ifFalse()) : i.ifFalse();
            return inlineConditional(condition, ifTrue, ifFalse, null, true);
        }
        if (expression instanceof Or or) {
            List<Expression> expressions = recurse
                    ? or.expressions().stream().map(e -> sortAndSimplify(true, e)).toList()
                    : or.expressions();
            return or(expressions);
        }
        if (expression instanceof And and) {
            List<Expression> expressions = recurse
                    ? and.expressions().stream().map(e -> sortAndSimplify(true, e)).toList()
                    : and.expressions();
            return and(expressions);
        }
        if (expression instanceof GreaterThanZero gt0) {
            Expression e = recurse ? sortAndSimplify(true, gt0.expression()) : gt0.expression();
            return greaterThanZero(e, gt0.allowEquals());
        }
        if (expression instanceof MethodCall mc && mc.methodInfo().isOverloadOfJLOEquals()) {
            Expression lhs = recurse ? sortAndSimplify(true, mc.object()) : mc.object();
            Expression rhs = recurse ? sortAndSimplify(true, mc.parameterExpressions().get(0)) : mc.parameterExpressions().get(0);
            return equalsMethod(mc, lhs, rhs);
        }
        if (expression instanceof Equals equals) {
            Expression lhs = recurse ? sortAndSimplify(true, equals.lhs()) : equals.lhs();
            Expression rhs = recurse ? sortAndSimplify(true, equals.rhs()) : equals.rhs();
            return equals(lhs, rhs);
        }
        if (expression instanceof Negation negation) {
            Expression e = recurse ? sortAndSimplify(true, negation.expression()) : negation.expression();
            return negate(e);
        }
        if (expression instanceof Divide divide) {
            Expression lhs = recurse ? sortAndSimplify(true, divide.lhs()) : divide.lhs();
            Expression rhs = recurse ? sortAndSimplify(true, divide.rhs()) : divide.rhs();
            return divide(lhs, rhs);
        }
        if (expression instanceof BinaryOperator binaryOperator) {
            Expression lhs = recurse ? sortAndSimplify(true, binaryOperator.lhs()) : binaryOperator.lhs();
            Expression rhs = recurse ? sortAndSimplify(true, binaryOperator.rhs()) : binaryOperator.rhs();
            return binaryOperator(lhs, rhs, binaryOperator);
        }
        if (expression instanceof UnaryOperator unaryOperator) {
            Expression e = recurse ? sortAndSimplify(true, unaryOperator.expression()) : unaryOperator.expression();
            return unaryOperator(e, unaryOperator);
        }
        if (expression instanceof InstanceOf instanceOf) {
            Expression e = recurse ? sortAndSimplify(true, instanceOf.expression()) : instanceOf.expression();
            return instanceOf(e, instanceOf);
        }
        if (expression instanceof Cast cast) {
            Expression e = recurse ? sortAndSimplify(true, cast.expression()) : cast.expression();
            return cast(e, cast);
        }
        return expression;
    }

    boolean isNotNull0(Expression expression);

    Stream<Expression> expandTerms(Expression expression, boolean negate);

    Stream<Expression> expandFactors(Expression expression);

    Expression less(Expression lhs, Expression rhs, boolean allowEquals);

    Expression wrapInProduct(Expression[] translated, int length);

    Expression wrapInSum(Expression[] translated, int length);
}
