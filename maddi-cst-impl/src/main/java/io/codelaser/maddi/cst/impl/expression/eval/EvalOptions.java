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
