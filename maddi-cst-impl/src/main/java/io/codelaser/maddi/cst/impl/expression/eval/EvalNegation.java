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

package io.codelaser.maddi.cst.impl.expression.eval;

import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;
import io.codelaser.maddi.cst.api.expression.*;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.impl.expression.NegationImpl;

import java.util.HashMap;
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

     NOT keyed on expressions that mention a LOCAL VARIABLE: see the note in LocalVariableImpl -- a local's
     fullyQualifiedName is its BARE NAME, so two same-named locals from different methods are equal() and
     hash alike, while their TYPES may differ. One Runtime spans every method and compilation unit of a
     session, which makes this cache exactly the "cross-method structure keyed by Variable" that note warns
     against: method A caching the negation of `l0` (a Throwable) hands method B, which happens to name a
     boolean `l0` too, A's expression -- and B then trips EvalAnd's "Non-boolean return type for l0 in And:
     Type Throwable". Locals are the only variables whose FQN is not globally unique; descending covers the
     ones reached through a scope or array (`a[i]`, `local.field`).

     The screen sits on the PUT, not the GET, so the hit path stays O(1). That is sound because equality is
     purely FQN-based (VariableImpl.fqnForEquality): a local's FQN is a bare name, a field's is dotted and a
     parameter's is method-qualified, so a local-bearing expression can never be equal() to a local-free one.
     Nothing local-bearing is ever stored, hence nothing local-bearing can ever hit.
     */
    private static final int CACHE_MIN_COMPLEXITY = 3;
    private static final int CACHE_MAX_SIZE = 8192;
    // @IgnoreModifications (road §050): an idempotent memo -- the VariableImpl.cachedHash idiom at cache
    // scale; the degraded-result guard above keeps the memoized values canonical, so the mutation is
    // observationally invisible. The last never-forms holdout of the eventual cluster (2026-08-01).
    @IgnoreModifications
    private final java.util.concurrent.ConcurrentHashMap<Expression, Expression> negationCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    private boolean mentionsLocalVariable(Expression v) {
        return v.variables(runtime.descendModeYes()).anyMatch(x -> x instanceof LocalVariable);
    }

    /*
     The memo for what the cache above refuses: a second, per-thread memo scoped to ONE top-level operation
     (EvalBudget.operationId). Everything negated inside one operation comes from one method's context, so the
     bare-name collision of locals cannot happen there -- and that is where the repetition is: a method body's
     conditions are mostly locals, which left the And/Or fixed point negating the same local-bearing operands over
     and over, De Morgan plus re-simplification each time (a shared And/Or tree of depth 8 over locals: 224 ms,
     the same tree over fields: 0.7 ms). Degraded (over-budget) results may live here: the scope is the operation
     whose budget degraded them, so nothing leaks to another. Cleared when the operation changes.
     */
    private static final EvalSwitches.Gate SCOPED_MEMO_OFF = EvalSwitches.gate("NOSCOPEDNEGMEMO");
    private static final int SCOPED_MAX_SIZE = 8192;

    private static final class ScopedMemo {
        int operationId = -1;
        final HashMap<Expression, Expression> map = new HashMap<>();

        HashMap<Expression, Expression> forCurrentOperation() {
            int current = EvalBudget.operationId();
            if (current != operationId) {
                map.clear();
                operationId = current;
            } else if (map.size() >= SCOPED_MAX_SIZE) {
                map.clear();
            }
            return map;
        }
    }

    @IgnoreModifications
    private static final ThreadLocal<ScopedMemo> SCOPED = ThreadLocal.withInitial(ScopedMemo::new);

    /** The memoized negation of {@code v}, from either memo, or null: a lookup, never a computation. */
    public Expression memoized(Expression v) {
        if (v.complexity() < CACHE_MIN_COMPLEXITY) return null;
        Expression cached = negationCache.get(v);
        if (cached != null) return cached;
        if (SCOPED_MEMO_OFF.get() || EvalBudget.operationId() < 0) return null;
        return SCOPED.get().forCurrentOperation().get(v);
    }

    public Expression eval(@NotNull Expression v) {
        Objects.requireNonNull(v);
        if (v.complexity() < CACHE_MIN_COMPLEXITY) return compute(v);
        Expression cached = negationCache.get(v);
        if (cached != null) return cached;
        if (SCOPED_MEMO_OFF.get()) {
            Expression result = compute(v);
            storeGlobally(v, result);
            return result;
        }
        // negation opens an operation of its own when called at top level, so that the De Morgan recursion below
        // (negate every operand, then and()/or() them) shares one scope and one budget; nested, it joins the caller's
        EvalBudget.enter();
        try {
            HashMap<Expression, Expression> scoped = SCOPED.get().forCurrentOperation();
            Expression inScope = scoped.get(v);
            if (inScope != null) return inScope;
            Expression result = compute(v);
            scoped.put(v, result);
            storeGlobally(v, result);
            return result;
        } finally {
            EvalBudget.exit();
        }
    }

    private void storeGlobally(Expression v, Expression result) {
        // an over-budget compute yields a DEGRADED (unsimplified) result — never memoize those, or budget
        // state leaks nondeterministically across top-level operations (see EvalBudget.overBudget)
        if (!EvalBudget.overBudget() && !mentionsLocalVariable(v)) {
            if (negationCache.size() >= CACHE_MAX_SIZE) negationCache.clear();
            negationCache.put(v, result);
        }
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
        // over budget, De Morgan re-enters the And/Or fixed point once per operand, and without the cache a
        // subtree shared by several operands is negated once per path to it: exponential (maddi-mod#20)
        if ((v instanceof Or || v instanceof And) && EvalBudget.nestedOverBudget()) {
            return new NegationImpl(runtime.logicalNotOperatorBool(), runtime.precedenceUnary(), v);
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
