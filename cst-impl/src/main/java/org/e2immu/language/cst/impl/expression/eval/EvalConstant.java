package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.util.internal.util.IntUtil;

public class EvalConstant {
    private final Runtime runtime;

    public EvalConstant(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression equalsExpression(ConstantExpression<?> l, ConstantExpression<?> r) {
        if (l instanceof NullConstant || r instanceof NullConstant)
            throw new UnsupportedOperationException("Not for me");

        if (l instanceof StringConstant ls && r instanceof StringConstant rs) {
            return runtime.newBoolean(ls.constant().equals(rs.constant()));
        }
        if (l instanceof BooleanConstant lb && r instanceof BooleanConstant lr) {
            return runtime.newBoolean(lb.constant() == lr.constant());
        }
        if (l instanceof CharConstant lc && r instanceof CharConstant rc) {
            return runtime.newBoolean(lc.constant() == rc.constant());
        }
        if (l instanceof CharConstant lc && r instanceof Numeric rc
            && IntUtil.isMathematicalInteger(rc.doubleValue())) {
            return runtime.newBoolean(lc.constant() == (int) rc.doubleValue());
        }
        if (l instanceof Numeric lc && IntUtil.isMathematicalInteger(lc.doubleValue()) && r instanceof CharConstant rc) {
            return runtime.newBoolean((int) lc.doubleValue() == rc.constant());
        }
        if (l instanceof Numeric ln && r instanceof Numeric rn) {
            return runtime.newBoolean(ln.number().equals(rn.number()));
        }
        throw new UnsupportedOperationException("l = " + l + ", r = " + r);
    }
}
