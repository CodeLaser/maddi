package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.Divide;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.expression.Numeric;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.expression.DivideImpl;
import org.e2immu.language.cst.impl.expression.IntConstantImpl;

public class EvalDivide {
    private final Runtime runtime;

    public EvalDivide(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression divide(Expression lhs, Expression rhs) {

        if (lhs instanceof Numeric ln && ln.doubleValue() == 0) return lhs;
        if (rhs instanceof Numeric rn && rn.doubleValue() == 1) return lhs;
        if (lhs instanceof IntConstant li && rhs instanceof IntConstant ri && ri.constant() != 0)
            return runtime.newInt(li.constant() / ri.constant());

        // a/n1/n2 = a/n1*n2
        if (lhs instanceof Divide d2) {
            return new DivideImpl(runtime, d2.lhs(), runtime.product(d2.rhs(), rhs));
        }
        // any unknown lingering
        if (lhs.isEmpty() || rhs.isEmpty()) throw new UnsupportedOperationException();

        return new DivideImpl(runtime, lhs, rhs);
    }

}
