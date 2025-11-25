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

import org.e2immu.language.cst.api.expression.And;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Or;
import org.e2immu.language.cst.api.runtime.Runtime;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EvalBoolean {
    private final Runtime runtime;

    public EvalBoolean(Runtime runtime) {
        this.runtime = runtime;
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

    public boolean isNegationOf(Expression e1, Expression e2) {
        if (e1 instanceof And && e2 instanceof And) {
            return false;
        }
        if (e1 instanceof Or && e2 instanceof Or) {
            return false;
        }
        return e1.equals(runtime.negate(e2));
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
