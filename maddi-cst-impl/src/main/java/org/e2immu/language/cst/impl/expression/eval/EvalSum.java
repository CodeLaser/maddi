package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.expression.SumImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class EvalSum {

    private final Runtime runtime;

    public EvalSum(Runtime runtime) {
        this.runtime = runtime;
    }

    public Expression eval(Expression l, Expression r, boolean tryAgain) {
        if (l.equals(r))
            return runtime.product(runtime.newInt(2), l);

        Double ln = l.numericValue();
        Double rn = r.numericValue();

        if (ln != null && rn != null) {
            return runtime.intOrDouble(ln + rn);
        }
        if (ln != null && ln == 0.0) return r;
        if (rn != null && rn == 0.0) return l;

        if (l instanceof Negation nl && nl.expression().equals(r) ||
            r instanceof Negation nr && nr.expression().equals(l)) {
            return runtime.intZero();
        }

        // similar code in Equals (common terms)

        Expression[] terms = Stream.concat(expandTerms(l, false),
                expandTerms(r, false)).toArray(Expression[]::new);
        Arrays.sort(terms);
        Expression[] termsOfProducts = makeProducts(terms);
        if (termsOfProducts.length == 0) return runtime.intZero();
        if (termsOfProducts.length == 1) return termsOfProducts[0];
        Expression newL, newR;
        if (termsOfProducts.length == 2) {
            newL = termsOfProducts[0];
            newR = termsOfProducts[1];
        } else {
            newL = wrapInSum(termsOfProducts, termsOfProducts.length - 1);
            newR = termsOfProducts[termsOfProducts.length - 1];
        }

        Sum s = new SumImpl(runtime, newL, newR);

        // re-running the sum to solve substitutions of variables to constants
        if (tryAgain) {
            return eval(s.lhs(), s.rhs(), false);
        }
        return s;
    }

    public Expression[] makeProducts(Expression[] terms) {
        List<Expression> result = new ArrayList<>(terms.length);
        int pos = 1;
        Expression term0 = terms[0];
        assert term0 != null;
        result.add(term0); // set the first
        while (pos < terms.length) {
            Expression e = terms[pos];
            int latestIndex = result.size() - 1;
            Expression latest = result.get(latestIndex);
            assert latest != null;
            Numeric ln;
            Numeric rn;
            if ((ln = e.asInstanceOf(Numeric.class)) != null
                && (rn = latest.asInstanceOf(Numeric.class)) != null) {
                Expression sum = runtime.intOrDouble(ln.doubleValue() + rn.doubleValue());
                result.set(latestIndex, sum);
            } else {
                Factor f1 = getFactor(latest);
                Factor f2 = getFactor(e);
                if (f1.term.equals(f2.term)) {
                    if (f1.factor == -f2.factor) {
                        result.set(latestIndex, runtime.intZero());
                    } else {
                        Expression f = runtime.intOrDouble(f1.factor + f2.factor);
                        Expression product = runtime.product(f, f1.term);
                        result.set(latestIndex, product);
                    }
                } else {
                    result.add(e);
                }
            }
            pos++;
        }
        Collections.sort(result);
        result.removeIf(e -> {
            Numeric n;
            return (n = e.asInstanceOf(Numeric.class)) != null && n.doubleValue() == 0;
        });
        return result.toArray(Expression[]::new);
    }

    protected record Factor(double factor, Expression term) {
    }

    protected Factor getFactor(Expression term) {
        assert term != null;
        if (term instanceof Negation neg) {
            Factor f = getFactor(neg.expression());
            return new Factor(-f.factor, f.term);
        }
        if (term instanceof Product p && p.lhs() instanceof Numeric n) {
            return new Factor(n.doubleValue(), p.rhs());
        }
        return new Factor(1, term);
    }

    // we have more than 2 terms, that's a sum of sums...
    public Expression wrapInSum(Expression[] expressions, int i) {
        assert i >= 2;
        if (i == 2) return runtime.sum(expressions[0], expressions[1]);
        return runtime.sum(wrapInSum(expressions, i - 1), expressions[i - 1]);
    }

    public Stream<Expression> expandTerms(Expression expression, boolean negate) {
        Sum sum;
        if ((sum = expression.asInstanceOf(Sum.class)) != null) {
            return Stream.concat(expandTerms(sum.lhs(), negate),
                    expandTerms(sum.rhs(), negate));
        }
        if (negate) {
            return Stream.of(runtime.negate(expression));
        }
        return Stream.of(expression);
    }
}
