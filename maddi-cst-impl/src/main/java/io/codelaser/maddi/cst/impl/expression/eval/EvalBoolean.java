/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.cst.impl.expression.eval;

import io.codelaser.maddi.cst.api.expression.And;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Negation;
import io.codelaser.maddi.cst.api.expression.Or;
import io.codelaser.maddi.cst.api.runtime.Runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EvalBoolean {
    private final Runtime runtime;
    private final EvalNegation evalNegation;

    public EvalBoolean(Runtime runtime, EvalNegation evalNegation) {
        this.runtime = runtime;
        this.evalNegation = evalNegation;
    }

    public Expression combineCondition(Expression baseCondition, Expression clause) {
        return runtime.and(baseCondition, clause);
    }

    public Expression complementOfClausesInCondition(Expression condition, Expression clausesToExclude) {
        return runtime.and(condition, runtime.negate(clausesToExclude));
        // TODO make more efficient given that we know the structure of the And; and that we know it has been sorted!
    }

    public Expression complementOfConditions(Expression baseCondition, List<Expression> conditionsForAssignments) {
        Expression or = runtime.or(conditionsForAssignments);
        Expression negated = runtime.negate(or);
        return runtime.and(baseCondition, negated);
    }

    public boolean conditionIsNotMoreSpecificThanAnyOf(Expression condition, Collection<Expression> bases) {
        return bases.stream().noneMatch(b -> isMoreSpecificThan(condition, b, true));
    }

    public boolean isMoreSpecificThan(Expression lessSpecific, Expression moreSpecific, boolean allowEquals) {
        if (lessSpecific.equals(moreSpecific)) {
            return allowEquals;
        }
        if (lessSpecific.isBoolValueTrue()) {
            return true;
        }
        if (moreSpecific instanceof And andMore) {
            if (lessSpecific instanceof And andLess) {
                return andLess.expressions().stream().allMatch(l -> andMore.expressions().stream().anyMatch(l::equals));
            }
            return andMore.expressions().stream().anyMatch(lessSpecific::equals);
        }
        return runtime.and(lessSpecific, moreSpecific).equals(moreSpecific);
    }

    /*
     isNegationOf(e1, e2) sits inside anyMatch loops over the conjunct/disjunct lists, so it is asked far more often
     than it answers yes. Implemented as e1.equals(negate(e2)), every ask of an And/Or pair ran De Morgan through
     the full EvalAnd/EvalOr fixed point (92-99% of the samples on CNF-shaped input). An And and an Or are each
     other's negation exactly when their operands are, pairwise: that is De Morgan without the re-simplification
     that follows it -- the complement-edge walk of an and-inverter graph, done on demand. What is lost is the rare
     case where the And of the negated operands would have collapsed further (x<3 || x>3 negated is 3==x, one term
     for two); an atom against a compound still takes the full route for that reason (a instanceof T against
     null==a || !(a instanceof T)).
     */
    private static final EvalSwitches.Gate STRUCTURAL_NEGATION_OFF = EvalSwitches.gate("NOSTRUCTNEG");

    public boolean isNegationOf(Expression e1, Expression e2) {
        if (e1 instanceof And && e2 instanceof And) {
            return false;
        }
        if (e1 instanceof Or && e2 instanceof Or) {
            return false;
        }
        // over budget, negate is the unsimplified Negation of an And/Or: compare structurally rather than
        // negate every pair of the sorted operands (maddi-mod#20)
        if (EvalBudget.nestedOverBudget()) {
            return isPlainNegation(e1, e2);
        }
        if (!STRUCTURAL_NEGATION_OFF.get()) {
            if (isPlainNegation(e1, e2)) return true;
            // a memoized negate(e2) is exactly what the full route below computes: it decides, in O(1)
            Expression known = evalNegation.memoized(e2);
            if (known != null) return e1.equals(known);
            // negate(e1) is NOT: it may stay a plain Negation where negate(e2) collapses (3==k against k<3||k>=4),
            // so it can only confirm
            known = evalNegation.memoized(e1);
            if (known != null && e2.equals(known)) return true;
            if (e1 instanceof And and && e2 instanceof Or or) return operandsAreNegations(and, or);
            if (e1 instanceof Or or && e2 instanceof And and) return operandsAreNegations(and, or);
            if (!ATOM_AGAINST_COMPOUND_OFF.get() && !isCompound(e1) && isCompound(e2)) {
                // the full route would negate the compound; negating the atom confirms cheaply. When the compound is
                // e1, the full route below already negates the atom
                if (runtime.negate(e1).equals(e2)) return true;
                if (e2.complexity() > MAX_COMPLEXITY_FULL_NEGATION) return false;
            }
        }
        return e1.equals(runtime.negate(e2));
    }

    /*
     maddi-mod#61: an atom against an And/Or was the one shape left on the full route, negating the compound -- De
     Morgan plus a full And/Or evaluation -- from inside the evaluators' pairwise loops. Negate the atom instead: it is
     cheap and memoized, and its negation is an atom or the two-part instanceof Or. That misses only the case where
     the compound's negation would have collapsed to the atom (k<3 || k>=4 negates to 3==k), so the full route stays
     for small compounds, where it is cheap; a large compound is not the negation of an atom.
     */
    private static final EvalSwitches.Gate ATOM_AGAINST_COMPOUND_OFF = EvalSwitches.gate("NOATOMNEG");
    static final int MAX_COMPLEXITY_FULL_NEGATION = 40;

    private static boolean isPlainNegation(Expression e1, Expression e2) {
        return e1 instanceof Negation n1 && n1.expression().equals(e2)
               || e2 instanceof Negation n2 && n2.expression().equals(e1);
    }

    /*
     Each operand of the And must account for operands of the Or, and nothing of the Or may be left over. An
     And operand is an atom or an Or (an And would have been flattened). An atom's negation is an atom, or an Or
     whose operands were flattened into the Or (a instanceof T negates to null==a || !(a instanceof T)): it accounts
     for all of them. An Or operand's negation is an And nested in the Or, matched recursively -- or an atom of the
     Or whose negation is that Or (the same instanceof shape, seen from the other side).
     */
    private boolean operandsAreNegations(And and, Or or) {
        List<Expression> andOps = and.expressions();
        if (andOps.size() > or.expressions().size()) return false;
        Map<Expression, Integer> remaining = new HashMap<>();
        for (Expression u : or.expressions()) remaining.merge(u, 1, Integer::sum);
        for (Expression t : andOps) {
            if (t instanceof Or tOr) {
                Expression found = null;
                for (Expression u : remaining.keySet()) {
                    boolean match = u instanceof And uAnd ? operandsAreNegations(uAnd, tOr)
                            : !isCompound(u) && runtime.negate(u).equals(t);
                    if (match) {
                        found = u;
                        break;
                    }
                }
                if (found == null || !take(remaining, found)) return false;
            } else {
                Expression negated = runtime.negate(t); // an atom: cheap, and memoized
                if (negated instanceof Or negatedOr) {
                    for (Expression piece : negatedOr.expressions()) {
                        if (!take(remaining, piece)) return false;
                    }
                } else if (!take(remaining, negated)) {
                    return false;
                }
            }
        }
        return remaining.isEmpty();
    }

    private static boolean take(Map<Expression, Integer> remaining, Expression e) {
        Integer count = remaining.get(e);
        if (count == null) return false;
        if (count == 1) remaining.remove(e);
        else remaining.put(e, count - 1);
        return true;
    }

    private static boolean isCompound(Expression e) {
        return e instanceof And || e instanceof Or;
    }

    public Expression removeClauseFromCondition(Expression base, Expression clauseToRemove) {
        if (base.equals(clauseToRemove)) {
            return runtime.constantTrue();
        }
        if (clauseToRemove.isBoolValueTrue()) return base;
        if (runtime.negate(clauseToRemove).equals(base)) return runtime.constantFalse();

        assert !base.isBoolValueTrue() : "Removing a condition from true?";
        if (base instanceof And and) {
            Set<Expression> toRemove;
            if (clauseToRemove instanceof And a) {
                toRemove = a.expressions().stream().collect(Collectors.toUnmodifiableSet());
            } else {
                toRemove = Set.of(clauseToRemove);
            }
            Expression[] expressions = and.expressions().stream()
                    .filter(e -> !toRemove.contains(e)).toArray(Expression[]::new);
            if (expressions.length < and.expressions().size()) {
                if (expressions.length == 1) return expressions[0];
                assert expressions.length > 1;
                return runtime.and(expressions);
            }
        }
        throw new UnsupportedOperationException("Should not happen");
    }
}
