/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

    /*
     Memoization, exact by purity of negation. The simplifier ASKS for negations far more often than it
     needs new ones: isNegationOf(e1,e2) is implemented as e1.equals(negate(e2)) and sits inside anyMatch
     loops over conjunct lists, so the same instances are negated over and over — and negating an And/Or
     runs the full EvalAnd/EvalOr fixed point (De Morgan + re-simplification). VALUE keys, not identity:
     the And/Or fixed point REBUILDS fresh instances every pass, so identity hits die immediately while
     structurally-equal keys keep hitting (equality itself is cheap: cached hashCode + complexity
     fast-path). The cache lives on THIS EvalNegation instance — one per Runtime — never static: a static
     cache outlives the Runtime, and a structurally-equal key from a different Runtime would hand back a
     negation built from foreign runtime objects. Concurrent (the analyzer evaluates in parallel); crude
     clear-on-cap eviction.
     */
    private static final int CACHE_MIN_COMPLEXITY = 3;
    private static final int CACHE_MAX_SIZE = 8192;
    private final java.util.concurrent.ConcurrentHashMap<Expression, Expression> negationCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    public Expression eval(@NotNull Expression v) {
        Objects.requireNonNull(v);
        if (v.complexity() < CACHE_MIN_COMPLEXITY) return compute(v);
        Expression cached = negationCache.get(v);
        if (cached != null) return cached;
        Expression result = compute(v);
        // an over-budget compute yields a DEGRADED (unsimplified) result — never memoize those, or budget
        // state leaks nondeterministically across top-level operations (see EvalBudget.overBudget)
        if (!EvalBudget.overBudget()) {
            if (negationCache.size() >= CACHE_MAX_SIZE) negationCache.clear();
            negationCache.put(v, result);
        }
        return result;
    }

    private Expression compute(@NotNull Expression v) {
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
