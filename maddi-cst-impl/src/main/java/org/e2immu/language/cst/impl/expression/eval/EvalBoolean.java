package org.e2immu.language.cst.impl.expression.eval;

import org.e2immu.language.cst.api.expression.And;
import org.e2immu.language.cst.api.expression.BooleanConstant;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Or;
import org.e2immu.language.cst.api.runtime.Runtime;

import java.util.Collection;
import java.util.List;

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
        if (clauseToRemove instanceof And) {
            throw new UnsupportedOperationException("Implement"); // TODO
        }
        if (base.equals(clauseToRemove)) {
            return runtime.constantTrue();
        }
        if (clauseToRemove.isBoolValueTrue()) return base;
        assert !base.isBoolValueTrue() : "Removing a condition from true?";
        if (base instanceof And and) {
            Expression[] expressions = and.expressions().stream()
                    .filter(e -> !clauseToRemove.equals(e)).toArray(Expression[]::new);
            if (expressions.length < and.expressions().size()) {
                if (expressions.length == 1) return expressions[0];
                assert expressions.length > 1;
                return runtime.and(expressions);
            }
        }
        throw new UnsupportedOperationException("Should not happen");
    }
}
