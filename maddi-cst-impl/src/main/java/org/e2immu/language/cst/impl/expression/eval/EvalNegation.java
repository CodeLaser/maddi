package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.annotation.NotNull;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.expression.NegationImpl;

import java.util.List;
import java.util.Objects;

public class EvalNegation {
    private final Runtime runtime;

    public EvalNegation(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression eval(@NotNull Expression v) {
        Objects.requireNonNull(v);
        if (v instanceof BooleanConstant boolValue) {
            return boolValue.negate();
        }
        if(v instanceof Negation n) {
            return n.expression();
        }
        if (v instanceof Numeric numeric) {
            return numeric.negate();
        }
        if (v.isEmpty()) {
            return v;
        }
        if (v instanceof Or or) {
            List<Expression> negated = or.expressions().stream().map(runtime::negate).toList();
            return runtime.and(negated);
        }
        if (v instanceof And and) {
            List<Expression> negated = and.expressions().stream().map(runtime::negate).toList();
            return runtime.or(negated);
        }
        if (v instanceof Sum sum) {
            return negate(sum);
        }
        if (v instanceof GreaterThanZero greaterThanZero) {
            return negate(greaterThanZero);
        }
        if (v instanceof Equals equals) {
            Expression res = negate(equals);
            if (res != null) return res;
        }

        MethodInfo operator = v.isNumeric() ? runtime.unaryMinusOperatorInt() : runtime.logicalNotOperatorBool();
        Negation negation = new NegationImpl(operator, runtime.precedenceUnary(), v);

        if (v instanceof InstanceOf i) {
            Expression varIsNull = runtime.equals(runtime.nullConstant(), i.expression());
            return runtime.or(negation, varIsNull);
        }
        return negation;
    }

    public Expression negate(Sum sum) {
        return runtime.sum(runtime.negate(sum.lhs()), runtime.negate(sum.rhs()));
    }

    public Expression negate(GreaterThanZero gt0) {
        Expression negated = eval(gt0.expression());
        return runtime.greater(negated, runtime.intZero(), !gt0.allowEquals());
    }

    public Expression negate(Equals equals) {
        InlineConditional icl;
        if ((icl = equals.lhs().asInstanceOf(InlineConditional.class)) != null) {
            Expression result = new EvalEquals(runtime).tryToRewriteConstantEqualsInlineNegative(equals.rhs(), icl);
            if (result != null) return result;
        }
        InlineConditional icr;
        if ((icr = equals.rhs().asInstanceOf(InlineConditional.class)) != null) {
            return new EvalEquals(runtime).tryToRewriteConstantEqualsInlineNegative(equals.lhs(), icr);
        }
        return null;
    }
}
