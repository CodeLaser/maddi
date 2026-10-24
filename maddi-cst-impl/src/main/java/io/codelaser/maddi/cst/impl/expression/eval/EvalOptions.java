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

/**
 * The boolean evaluator's limits.
 *
 * @param maxAndOrComplexity        an And/Or whose operands' summed complexity reaches this is combined, not reduced
 * @param maxEvalEntriesPerTopLevel the work budget of one top-level evaluation (EvalBudget): the evaluators stop
 *                                  reducing once it is spent
 * @param costAwareBudget           charge each And/Or pass by its operands' complexity rather than one unit, so that
 *                                  the budget bounds time rather than calls; off by default, because the default
 *                                  budget was calibrated on unit charges (maddi-mod#61)
 */
public record EvalOptions(int maxAndOrComplexity, int maxEvalEntriesPerTopLevel, boolean costAwareBudget) {
    public static final int DEFAULT_MAX_AND_OR_COMPLEXITY = 500;
    public static final int DEFAULT_MAX_EVAL_ENTRIES_PER_TOP_LEVEL = EvalBudget.DEFAULT_MAX_EVAL_ENTRIES_PER_TOP_LEVEL;

    public EvalOptions(int maxAndOrComplexity) {
        this(maxAndOrComplexity, DEFAULT_MAX_EVAL_ENTRIES_PER_TOP_LEVEL, false);
    }

    public static final EvalOptions DEFAULT = new EvalOptions(DEFAULT_MAX_AND_OR_COMPLEXITY);

    /** The units one And/Or pass over operands of this summed complexity consumes from the budget. */
    public int passCost(int complexity) {
        return costAwareBudget ? 1 + complexity / COMPLEXITY_PER_UNIT : 1;
    }

    // a condition of complexity 100 costs about twice a small one to sort, compare and negate per pass
    private static final int COMPLEXITY_PER_UNIT = 100;
}
