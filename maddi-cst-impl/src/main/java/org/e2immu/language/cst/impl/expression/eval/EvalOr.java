package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.expression.OrImpl;
import org.e2immu.language.cst.impl.expression.util.AndOrSorter;
import org.e2immu.util.internal.util.IntUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EvalOr {
    private static final Logger LOGGER = LoggerFactory.getLogger(EvalOr.class);

    private final Runtime runtime;
    private final int maxAndOrComplexity;

    public EvalOr(Runtime runtime, EvalOptions evalOptions) {
        this.runtime = runtime;
        maxAndOrComplexity = evalOptions.maxAndOrComplexity();
    }

    private record R(boolean change, Expression escape) {
    }

    private static final R NO_CHANGE = new R(false, null);
    private static final R CHANGE = new R(true, null);

    public Expression eval(List<Expression> values) {

        // STEP 1: trivial reductions

        if (values.isEmpty()) {
            return runtime.constantFalse();
        }
        if (values.size() == 1) {
            return values.get(0);
        }

        // STEP 2: concat everything A || (B || C) === A || B || C

        ArrayList<Expression> concat = new ArrayList<>(values.size());
        recursivelyAdd(concat, values);

        // STEP 3: loop
        boolean changes = true;

        while (changes) {
            changes = false;

            // STEP 4a: sort

            concat = AndOrSorter.sort(concat);

            // STEP 4b: trivial observations

            Iterator<Expression> iterator = concat.iterator();
            Expression p = null;
            while (iterator.hasNext()) {
                Expression e = iterator.next();
                if (e instanceof BooleanConstant bc) {
                    if (bc.constant()) {
                        LOGGER.debug("Return TRUE in Or, found TRUE");
                        return runtime.constantTrue();
                    }
                    // remove a "false"
                    iterator.remove();
                } else if (e.equals(p)) {
                    // A || A == A
                    iterator.remove();
                } else if (p != null && runtime.isNegationOf(p, e)) {
                    // A || !A == true
                    return runtime.constantTrue();
                }
                p = e;
            }

            // STEP 4c: complexity check

            int complexity = concat.stream().mapToInt(Expression::complexity).sum();
            boolean tooComplex = complexity >= maxAndOrComplexity;
            if (tooComplex) {
                LOGGER.warn("Not analysing OR operation, complexity {}", complexity);
                return runtime.newOrBuilder().addExpressions(concat).build();
            }

            // STEP 4d: reductions

            ArrayList<Expression> newConcat = new ArrayList<>(concat.size());
            for (Expression value : concat) {
                Expression prev = newConcat.isEmpty() ? null : newConcat.get(newConcat.size() - 1);
                R r = actions(prev, value, newConcat);
                if (r.escape != null) return r.escape;
                changes |= r.change;
            }
            concat = newConcat;
        }
        ArrayList<Expression> finalValues = concat;
        if (finalValues.size() == 1) return finalValues.get(0);

        for (Expression value : finalValues) {
            if (value.isEmpty()) throw new UnsupportedOperationException();
        }

        if (finalValues.isEmpty()) {
            LOGGER.debug("Empty disjunction returned as false");
            return runtime.constantFalse();
        }
        return new OrImpl(runtime, finalValues);
    }

    private R actions(Expression prev, Expression value, ArrayList<Expression> newConcat) {
        R r1 = numericComparisons(prev, value, newConcat);
        if (r1.change) return r1;

        R r2 = twoAnds(prev, value, newConcat);
        if (r2.change) return r2;

        if (prev != null) {
            R r3 = removeFromAnds(value, newConcat);
            if (r3.change) return r3;


            R r4 = inAllClausesOfAnd(prev, value, newConcat);
            if (r4.change) return r4;

            R r5 = removeAnds(prev, value);
            if (r5.change) return r5;
        }
        // default action
        newConcat.add(value);
        return NO_CHANGE;
    }

    private R removeAnds(Expression prev, Expression value) {
        // A || A&&B  -> remove A&&B
        if (value instanceof And and && and.expressions().stream().anyMatch(prev::equals)) {
            return CHANGE;
        }
        return NO_CHANGE;
    }

    private R removeFromAnds(Expression value, ArrayList<Expression> newConcat) {
        // A ||  !A & B & C         -> A || B && C
        // A || B || !A && !B && C  -> A || B || C
        if (value instanceof And and) {
            boolean change = false;
            List<Expression> newList = new ArrayList<>(and.expressions().size());
            for (Expression e : and.expressions()) {
                if (newConcat.stream().anyMatch(f -> runtime.isNegationOf(e, f))) {
                    change = true;
                } else {
                    newList.add(e);
                }
            }
            if (change) {
                newConcat.add(runtime.and(newList));
                return CHANGE;
            }
        }
        return NO_CHANGE;
    }

    // A || ((A||B)&&(A||C))
    private R inAllClausesOfAnd(Expression prev, Expression value, ArrayList<Expression> newConcat) {
        if (value instanceof And and) {
            boolean inAllClauses = true;
            for (Expression e : and.expressions()) {
                if (!(e instanceof Or or) || or.expressions().stream().noneMatch(prev::equals)) {
                    inAllClauses = false;
                    break;
                }
            }
            if (inAllClauses) {
                // remove prev
                newConcat.set(newConcat.size() - 1, value);
                return CHANGE;
            }
        }
        return NO_CHANGE;
    }

    private R twoAnds(Expression prev, Expression value, ArrayList<Expression> newConcat) {
        if (value instanceof And and2 && and2.expressions().size() == 2
            && prev instanceof And and1 && and1.expressions().size() == 2) {
            Expression and10 = and1.expressions().get(0);
            Expression and20 = and2.expressions().get(0);
            Expression and11 = and1.expressions().get(1);
            if (and10.equals(and20) && runtime.isNegationOf(and11, and2.expressions().get(1))) {
                // A && B  ||  A && !B  ==  A
                newConcat.set(newConcat.size() - 1, and10);
                return CHANGE;
            }
            if (and11.equals(and2.expressions().get(1)) && and10.equals(runtime.negate(and20))) {
                // A && B  ||  !A && B  ==  B
                newConcat.set(newConcat.size() - 1, and11);
                return CHANGE;
            }
        }
        return NO_CHANGE;
    }

    private R numericComparisons(Expression prev, Expression value, List<Expression> newConcat) {

        GreaterThanZero gt0;
        GreaterThanZero gt1;
        if ((gt1 = value.asInstanceOf(GreaterThanZero.class)) != null
            && prev != null
            && (gt0 = prev.asInstanceOf(GreaterThanZero.class)) != null) {
            GreaterThanZero.XB xb0 = gt0.extract(runtime);
            GreaterThanZero.XB xb1 = gt1.extract(runtime);
            if (xb0.x().equals(xb1.x())) {

                // x>=a || x <= a-1
                if (xb0.lessThan() == !xb1.lessThan() && orComparisonTrue(xb0.lessThan(), xb0.b(), xb1.b())) {
                    return new R(true, runtime.constantTrue());
                }
                // x<=a || x<=b --> x<=max(a,b)
                if (xb0.lessThan() && xb1.lessThan()) {
                    if (xb0.b() < xb1.b()) {
                        // replace previous
                        newConcat.set(newConcat.size() - 1, value);
                    }  // else ignore this one
                    return CHANGE;
                }

                // x>=a || x>=b --> x>=min(a,b)
                if (!xb0.lessThan() && !xb1.lessThan()) {
                    if (xb0.b() > xb1.b() || !gt0.allowEquals()) {
                        // replace previous
                        newConcat.set(newConcat.size() - 1, value);
                    }  // else ignore this one
                    return CHANGE;
                }
            }
            Expression notXb1 = runtime.negate(xb1.x());
            if (xb0.x().equals(notXb1)) {
                if (xb0.b() == xb1.b()) {
                    if (gt0.allowEquals() || gt1.allowEquals()) {
                        return new R(true, runtime.constantTrue());
                    }
                    // x < a || x > a ==> x != a
                    newConcat.set(newConcat.size() - 1,
                            runtime.negate(runtime.equals(xb0.x(), runtime.intOrDouble(xb0.b()))));
                    return CHANGE;
                }
            }
            if (xb0.b() == xb1.b() && xb0.x() instanceof Sum s1 && xb1.x() instanceof Sum s2) {
                if (s1.lhs().equals(s2.lhs()) && s1.rhs().equals(runtime.negate(s2.rhs()))) {
                    // l+m >= 0, l-m >= 0 ===> l >= Math.min(-m, m), l >= -Math.abs(m)
                    // changes = true;
                    // newConcat.set(newConcat.size() - 1, abs);
                    //  continue;
                }
                if (s1.rhs().equals(s2.rhs()) && s1.lhs().equals(runtime.negate(s2.lhs()))) {
                    // -l+m >= 0, l+m >= 0 ===>  m >= Math.min(-l, l)
                    // changes = true;
                    // newConcat.set(newConcat.size() - 1, abs);
                    // continue;
                }
            }
        }
        return NO_CHANGE;
    }

    protected boolean orComparisonTrue(boolean d0IsLt, double d0, double d1) {
        boolean i0 = IntUtil.isMathematicalInteger(d0);
        boolean i1 = IntUtil.isMathematicalInteger(d1);
        if (i0 && i1) {
            if (d0IsLt) {
                //d0IsLt == true: x <= 4 || x >= 5
                return d1 - 1 <= d0; // 5-1<=4, 3-1<=4 but not 10-1<=4
            }
            // d0IsLt == false: x >= 4 || x <= 3
            return d0 - 1 <= d1; // 4-1 <= 3  1-1<=3 but not 10-1<= 3
        }
        return d0 == d1;
    }

    protected void recursivelyAdd(ArrayList<Expression> concat, List<Expression> collect) {
        for (Expression value : collect) {
            Or or;
            if ((or = value.asInstanceOf(Or.class)) != null) {
                concat.addAll(or.expressions());
            } else {
                concat.add(value);
            }
        }
    }

}
