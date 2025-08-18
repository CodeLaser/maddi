package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;

public class EvalUnaryOperator {
    private final Runtime runtime;

    public EvalUnaryOperator(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression eval(Expression value, UnaryOperator unaryOperator) {
        MethodInfo operator = unaryOperator.operator();

        // !!, ~~
        if (value instanceof UnaryOperator second && second.operator() == operator) {
            return second.expression();
        }
        if (runtime.bitWiseNotOperatorInt() == operator) {
            return runtime.newBitwiseNegation(unaryOperator.comments(), unaryOperator.source(), value);
        }
        if (runtime.unaryPlusOperatorInt() == operator) {
            return value;
        }
        assert runtime.unaryMinusOperatorInt() == operator || runtime.logicalNotOperatorBool() == operator;
        return runtime.negate(value);
    }
}
