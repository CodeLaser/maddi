package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.expression.EqualsImpl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class EvalEquals {
    private final Runtime runtime;

    public EvalEquals(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression evalMethod(MethodCall methodCall, Expression lhs, Expression rhs) {
        if (lhs.equals(rhs)) return runtime.constantTrue();
        if (lhs == methodCall.object() && rhs == methodCall.parameterExpressions().get(0)) {
            // don't touch, no change
            return methodCall;
        }
        return runtime.newMethodCallBuilder()
                .setConcreteReturnType(methodCall.concreteReturnType())
                .setModificationTimes(methodCall.modificationTimes())
                .setMethodInfo(methodCall.methodInfo())
                .setObject(lhs)
                .setObjectIsImplicit(methodCall.objectIsImplicit())
                .setParameterExpressions(List.of(rhs))
                .setSource(methodCall.source())
                .addAnnotations(methodCall.annotations())
                .addComments(methodCall.comments())
                .build();
    }

    public Expression eval(Expression lhs, Expression rhs) {
        if (lhs.equals(rhs)) return runtime.constantTrue();

        ConstantExpression<?> lc, rc;
        if ((lc = lhs.asInstanceOf(ConstantExpression.class)) != null
            && ((rc = rhs.asInstanceOf(ConstantExpression.class)) != null
                && !lc.isNullConstant()
                && !rc.isNullConstant())) {
            return new EvalConstant(runtime).equalsExpression(lc, rc);
        }

        InlineConditional inlineLeft;
        if ((inlineLeft = lhs.asInstanceOf(InlineConditional.class)) != null) {
            Expression result = tryToRewriteConstantEqualsInline(rhs, inlineLeft);
            if (result != null) return result;
        }
        InlineConditional inlineRight;
        if ((inlineRight = rhs.asInstanceOf(InlineConditional.class)) != null) {
            Expression result = tryToRewriteConstantEqualsInline(lhs, inlineRight);
            if (result != null) return result;
        }

        EvalSum evalSum = new EvalSum(runtime);
        Expression[] terms = Stream.concat(evalSum.expandTerms(lhs, false),
                evalSum.expandTerms(rhs, true)).toArray(Expression[]::new);
        Arrays.sort(terms);
        Expression[] termsOfProducts = evalSum.makeProducts(terms);

        if (termsOfProducts.length == 0) {
            return runtime.constantTrue();
        }
        if (termsOfProducts.length == 1) {
            if (termsOfProducts[0].isInstanceOf(Numeric.class)) {
                return runtime.constantFalse();
            }
            Negation neg;
            if ((neg = termsOfProducts[0].asInstanceOf(Negation.class)) != null) {
                return new EqualsImpl(runtime, runtime.intZero(), neg.expression());
            }
            // 0 == 3*x --> 0 == x
            Product p;
            if ((p = termsOfProducts[0].asInstanceOf(Product.class)) != null && p.lhs().numericValue() != null) {
                return new EqualsImpl(runtime, runtime.intZero(), p.rhs());
            }
            return new EqualsImpl(runtime, runtime.intZero(), termsOfProducts[0]);
        }
        Expression newLeft;
        Expression newRight;

        // 4 == xx; -4 == -x, ...
        Double d = termsOfProducts[0].numericValue();
        Negation neg;
        if (d != null) {
            // -4 + -x --> -4 == x
            if (d < 0 && termsOfProducts[1].isInstanceOf(Negation.class)) {
                newLeft = termsOfProducts[0];
                newRight = wrapSum(termsOfProducts, true);
                // 4 + i == 0 --> -4 == i
            } else if (d > 0 && !(termsOfProducts[1].isInstanceOf(Negation.class))) {
                newLeft = runtime.intOrDouble(-d);
                newRight = wrapSum(termsOfProducts, false);
                // -4 + x == 0 --> 4 == x
            } else if (d < 0) {
                newLeft = runtime.intOrDouble(-d);
                newRight = wrapSum(termsOfProducts, false);
            } else {
                newLeft = termsOfProducts[0];
                newRight = wrapSum(termsOfProducts, true);
            }
        } else if ((neg = termsOfProducts[0].asInstanceOf(Negation.class)) != null) {
            newLeft = neg.expression();
            newRight = wrapSum(termsOfProducts, false);
        } else {
            newLeft = termsOfProducts[0];
            newRight = wrapSum(termsOfProducts, true);
        }

        // recurse
        return new EqualsImpl(runtime, newLeft, newRight);
    }

    private Expression wrapSum(Expression[] termsOfProducts,
                               boolean negate) {
        if (termsOfProducts.length == 2) {
            return negate ? runtime.negate(termsOfProducts[1]) : termsOfProducts[1];
        }
        return wrapSum(termsOfProducts, 1, termsOfProducts.length, negate);
    }

    private Expression wrapSum(Expression[] termsOfProducts,
                               int start, int end,
                               boolean negate) {
        if (end - start == 2) {
            Expression s1 = termsOfProducts[start];
            Expression t1 = negate ? runtime.negate(s1) : s1;
            Expression s2 = termsOfProducts[start + 1];
            Expression t2 = negate ? runtime.negate(s2) : s2;
            return runtime.sum(t1, t2);
        }
        Expression t1 = wrapSum(termsOfProducts, start, end - 1, negate);
        Expression s2 = termsOfProducts[end - 1];
        Expression t2 = negate ? runtime.negate(s2) : s2;
        return runtime.sum(t1, t2);
    }

    // (a ? null: b) == null with guaranteed b != null --> !a
    // (a ? x: b) == null with guaranteed b != null --> !a&&x==null

    // GENERAL:
    // (a ? x: y) == c  ; if y != c, guaranteed, then the result is a&&x==c
    // (a ? x: y) == c  ; if x != c, guaranteed, then the result is !a&&y==c

    // see test ConditionalChecks_7; TestEqualsConstantInline
    public Expression tryToRewriteConstantEqualsInline(Expression c, InlineConditional inlineConditional) {
        InlineConditional inline2;
        if ((inline2 = c.asInstanceOf(InlineConditional.class)) != null) {
            // silly check a1?b1:c1 == a1?b2:c2 === b1 == b2 && c1 == c2
            if (inline2.condition().equals(inlineConditional.condition())) {
                return runtime.and(
                        eval(inlineConditional.ifTrue(), inline2.ifTrue()),
                        eval(inlineConditional.ifFalse(), inline2.ifFalse()));
            }
            return null;
        }

        boolean ifTrueGuaranteedNotEqual;
        boolean ifFalseGuaranteedNotEqual;

        Expression recursively1;
        InlineConditional inlineTrue;
        if ((inlineTrue = inlineConditional.ifTrue().asInstanceOf(InlineConditional.class)) != null) {
            recursively1 = tryToRewriteConstantEqualsInline(c, inlineTrue);
            ifTrueGuaranteedNotEqual = recursively1 != null && recursively1.isBoolValueFalse();
        } else {
            recursively1 = null;
            if (c.isNullConstant()) {
                ifTrueGuaranteedNotEqual = runtime.isNotNull0(inlineConditional.ifTrue());
            } else {
                ifTrueGuaranteedNotEqual = eval(inlineConditional.ifTrue(), c).isBoolValueFalse();
            }
        }

        if (ifTrueGuaranteedNotEqual) {
            Expression notCondition = runtime.negate(inlineConditional.condition());
            return runtime.and(notCondition, eval(inlineConditional.ifFalse(), c));
        }

        Expression recursively2;
        InlineConditional inlineFalse;
        if ((inlineFalse = inlineConditional.ifFalse().asInstanceOf(InlineConditional.class)) != null) {
            recursively2 = tryToRewriteConstantEqualsInline(c, inlineFalse);
            ifFalseGuaranteedNotEqual = recursively2 != null && recursively2.isBoolValueFalse();
        } else {
            recursively2 = null;
            if (c.isNullConstant()) {
                ifFalseGuaranteedNotEqual = runtime.isNotNull0(inlineConditional.ifFalse());
            } else {
                ifFalseGuaranteedNotEqual = eval(inlineConditional.ifFalse(), c).isBoolValueFalse();
            }
        }

        if (ifFalseGuaranteedNotEqual) {
            return runtime.and(inlineConditional.condition(), eval(inlineConditional.ifTrue(), c));
        }

        // we try to do something with recursive results
        if (recursively1 != null && recursively2 != null) {
            Expression notCondition = runtime.negate(inlineConditional.condition());
            return runtime.or(runtime.and(inlineConditional.condition(), recursively1),
                    runtime.and(notCondition, recursively2));
        }

        if (c.isNullConstant()) {
            if (inlineConditional.ifTrue().isNullConstant()) {
                // null == (a ? null: b) --> a || (b == null)
                return runtime.or(inlineConditional.condition(),
                        eval(runtime.nullConstant(), inlineConditional.ifFalse()));
            }
            if (inlineConditional.ifFalse().isNullConstant()) {
                // null== (a ? b : null) --> !a || (b == null)
                return runtime.or(runtime.negate(inlineConditional.condition()),
                        eval(runtime.nullConstant(), inlineConditional.ifTrue()));
            }
        }
        return null;
    }

    // (a ? null: b) != null --> !a

    // GENERAL:
    // (a ? x: y) != c  ; if y == c, guaranteed, then the result is a&&x!=c
    // (a ? x: y) != c  ; if x == c, guaranteed, then the result is !a&&y!=c

    // see test ConditionalChecks_7; TestEqualsConstantInline
    public Expression tryToRewriteConstantEqualsInlineNegative(Expression c,
                                                               InlineConditional inlineConditional) {
        InlineConditional inline2;
        if ((inline2 = c.asInstanceOf(InlineConditional.class)) != null) {
            // silly check a1?b1:c1 != a1?b2:c2 === b1 != b2 || c1 != c2
            if (inline2.condition().equals(inlineConditional.condition())) {
                return runtime.or(
                        runtime.negate(eval(inlineConditional.ifTrue(), inline2.ifTrue())),
                        runtime.negate(eval(inlineConditional.ifFalse(), inline2.ifFalse())));
            }
            return null;
        }

        boolean ifTrueGuaranteedEqual;
        boolean ifFalseGuaranteedEqual;

        if (c.isNullConstant()) {
            ifTrueGuaranteedEqual = inlineConditional.ifTrue().isNullConstant();
            ifFalseGuaranteedEqual = inlineConditional.ifFalse().isNullConstant();
        } else {
            ifTrueGuaranteedEqual = eval(inlineConditional.ifTrue(), c).isBoolValueTrue();
            ifFalseGuaranteedEqual = eval(inlineConditional.ifFalse(), c).isBoolValueTrue();
        }
        if (ifTrueGuaranteedEqual) {
            Expression notCondition = runtime.negate(inlineConditional.condition());
            return runtime.and(notCondition, runtime.negate(eval(inlineConditional.ifFalse(), c)));
        }
        if (ifFalseGuaranteedEqual) {
            return runtime.and(inlineConditional.condition(), runtime.negate(eval(inlineConditional.ifTrue(), c)));
        }
        return null;
    }

}
