package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.util.internal.util.IntUtil;

import java.util.Arrays;

public class EvalInequality {
    private final Runtime runtime;

    public EvalInequality(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression greaterThanZero(Expression expression, boolean allowEquals) {
        return compute(expression, allowEquals);
    }

    public Expression less(Expression lhs, Expression rhs, boolean allowEquals) {
        return greater(rhs, lhs, allowEquals);
    }

    public Expression greater(Expression lhs, Expression rhs, boolean allowEquals) {
        Expression sum = runtime.sum(lhs, runtime.negate(rhs));
        return compute(sum, allowEquals);
    }


    private Expression compute(Expression expression, boolean allowEquals) {
        Expression[] terms = runtime.expandTerms(expression, false).toArray(Expression[]::new);
        Arrays.sort(terms);

        Numeric n0 = terms[0].asInstanceOf(Numeric.class);
        if (terms.length == 1) {
            return oneTerm(allowEquals, terms, n0);
        }
        if (terms.length == 2 && n0 != null
            && IntUtil.isMathematicalInteger(n0.doubleValue())
            && terms[1].parameterizedType().isMathematicallyInteger()) {
            return twoTerms(allowEquals, terms, n0);
        }
        if (terms.length == 2 && terms[0].parameterizedType().isMathematicallyInteger()
            && terms[1].parameterizedType().isMathematicallyInteger()) {
            if (!allowEquals) {
                // +-i +-j > 0
                expression = runtime.sum(runtime.intMinusOne(), runtime.sum(terms[0], terms[1]));
                allowEquals = true;
            }
        }
        if (terms.length == 3 && n0 != null
            && IntUtil.isMathematicalInteger(n0.doubleValue())
            && terms[1].parameterizedType().isMathematicallyInteger()
            && terms[2].parameterizedType().isMathematicallyInteger()) {

            if (n0.doubleValue() >= 0 && !allowEquals) {
                // special cases, i>=j == i-j >=0, 1+i-j>0
                IntConstant minusOne = runtime.newInt(-1 + (int) n0.doubleValue());
                expression = runtime.sum(minusOne, runtime.sum(terms[1], terms[2]));
                allowEquals = true;
            }
        }
        // fallback
        return runtime.newGreaterThanZero(expression, allowEquals);
    }

    private GreaterThanZero twoTerms(boolean allowEquals, Expression[] terms, Numeric n0) {
        // basic int comparisons, take care that we use >= and <
        boolean n0Negated = n0.doubleValue() < 0;
        boolean n1Negated = terms[1] instanceof Negation;

        Expression sum;
        boolean newAllowEquals;
        if (n0Negated) {
            if (!n1Negated) {
                newAllowEquals = true;
                if (!allowEquals) {
                    // -3 + x > 0 == x>3 == x>=4 == -4 + x >= 0
                    IntConstant minusOne = runtime.newInt(-1 + (int) n0.doubleValue());
                    sum = runtime.sum(minusOne, terms[1]);
                } else {
                    // -3 + x >= 0 == x >= 3 OK
                    sum = runtime.sum(terms[0], terms[1]);
                }
            } else {
                newAllowEquals = false;
                if (!allowEquals) {
                    // -3 - x > 0 == x<-3 OK
                    sum = runtime.sum(terms[0], terms[1]);
                } else {
                    // -3 - x >= 0 == x<=-3 == x<-2 == -2 - x > 0
                    IntConstant plusOne = runtime.newInt(1 + (int) n0.doubleValue());
                    sum = runtime.sum(plusOne, terms[1]);
                }
            }
        } else {
            if (!n1Negated) {
                newAllowEquals = true;
                if (!allowEquals) {
                    // 3 + x > 0 == x>-3 == x>=-2 == 2 + x >= 0
                    IntConstant minusOne = runtime.newInt(-1 + (int) n0.doubleValue());
                    sum = runtime.sum(minusOne, terms[1]);
                } else {
                    // 3 + x >= 0 == x>=-3 OK
                    sum = runtime.sum(terms[0], terms[1]);
                }
            } else {
                newAllowEquals = false;
                if (!allowEquals) {
                    // 3 - x > 0 == x<3 OK
                    sum = runtime.sum(terms[0], terms[1]);
                } else {
                    // 3 - x >= 0 == x<=3 == x<4 == 4 - x > 0
                    IntConstant plusOne = runtime.newInt(1 + (int) n0.doubleValue());
                    sum = runtime.sum(plusOne, terms[1]);
                }
            }
        }
        return runtime.newGreaterThanZero(sum, newAllowEquals);
    }

    private Expression oneTerm(boolean allowEquals, Expression[] terms, Numeric n0) {
        if (n0 != null) {
            boolean accept = n0.doubleValue() > 0.0 || allowEquals && n0.doubleValue() == 0.0;
            return runtime.newBoolean(accept);
        }
        Expression term;
        boolean newAllowEquals;
        if (terms[0].parameterizedType().isMathematicallyInteger()) {
            // some int expression >= 0
            if (terms[0] instanceof Negation) {
                newAllowEquals = false;
                if (allowEquals) {
                    // -x >= 0 == x <= 0 == x < 1 == 1 - x > 0
                    term = runtime.sum(runtime.intOne(), terms[0]);
                } else {
                    // -x > 0 == x < 0 OK
                    term = terms[0];
                }
            } else {
                newAllowEquals = true;
                if (allowEquals) {
                    // x >= 0 OK
                    term = terms[0];
                } else {
                    // x > 0 == x >= 1 == -1+x >= 0
                    term = runtime.sum(runtime.intMinusOne(), terms[0]);
                }
            }
        } else {
            term = terms[0];
            newAllowEquals = allowEquals;
        }
        // expr >= 0, expr > 0
        return runtime.newGreaterThanZero(term, newAllowEquals);
    }
}
