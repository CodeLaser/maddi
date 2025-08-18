package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.expression.Numeric;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.expression.DivideImpl;
import org.e2immu.language.cst.impl.expression.RemainderImpl;

public class EvalRemainder {
    private final Runtime runtime;

    public EvalRemainder(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression remainder(Expression lhs, Expression rhs) {
        if (lhs instanceof Numeric ln && ln.doubleValue() == 0) return lhs;
        if (rhs instanceof Numeric rn && rn.doubleValue() == 1) return lhs;
        if (lhs instanceof IntConstant li && rhs instanceof IntConstant ri)
            return runtime.newInt(li.constant() % ri.constant());

        // any unknown lingering
        if (lhs.isEmpty() || rhs.isEmpty()) throw new UnsupportedOperationException();

        return new RemainderImpl(runtime, lhs, rhs);
    }

}
