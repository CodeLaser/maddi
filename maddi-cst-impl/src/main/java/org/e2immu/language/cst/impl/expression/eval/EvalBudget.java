/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, Bart Naudts, https://github.com/CodeLaser/maddi
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

/**
 * A work budget over one top-level And/Or evaluation, counting every nested {@link EvalAnd}/{@link EvalOr}
 * entry. {@code maxAndOrComplexity} bounds the operand size of a single evaluation; it does NOT bound the
 * NUMBER of evaluations one top-level operation triggers — negating an And/Or runs De Morgan through the
 * full fixed point, queries spawn evaluations from inside {@code anyMatch} loops, and a pathological
 * condition (a lowered 40-arm switch chain's path condition) multiplies these into minutes of CPU
 * (jfocus-standardize round 10c: two fernflower methods wedged for 30+ minutes).
 * <p>
 * When the budget is exhausted, the evaluators stop reducing and return the sound, unsimplified
 * combination — exactly the established {@code tooComplex} escape hatch, on a different axis. Normal
 * expressions use a few dozen entries; the budget only bites in pathology.
 * <p>
 * Per-thread state (the analyzers evaluate in parallel); depth tracking makes nested entries share the
 * top-level's budget while independent top-level operations each get a fresh one.
 */
public class EvalBudget {
    // calibration (fernflower round 10c): legitimate heavy corpus methods consume tens of thousands of
    // entries once arithmetic ticks count (5_000 flipped eight integration goldens); the pathological
    // dispatchers burn millions before their 30-minute grinds. 100_000 separates the populations.
    private static final int MAX_EVAL_ENTRIES_PER_TOP_LEVEL = 100_000;

    // [0] = depth, [1] = entries consumed under the current top-level operation
    private static final ThreadLocal<int[]> STATE = ThreadLocal.withInitial(() -> new int[2]);

    private EvalBudget() {
    }

    /** Call on evaluator entry; pair with {@link #exit()} in a finally block. */
    public static void enter() {
        int[] state = STATE.get();
        if (state[0]++ == 0) {
            state[1] = 0;
        }
    }

    public static void exit() {
        STATE.get()[0]--;
    }

    /** Consume one unit of work; true when the current top-level operation is over budget. */
    public static boolean exhausted() {
        int[] state = STATE.get();
        return ++state[1] > MAX_EVAL_ENTRIES_PER_TOP_LEVEL;
    }

    /**
     * Consume one unit of work when — and only when — running inside an enclosing budgeted evaluation.
     * For helpers (arithmetic, negation) that are cheap standalone but multiply under the And/Or fixed
     * point: standalone calls stay untouched, nested ones share the top-level budget.
     */
    public static boolean tickNested() {
        int[] state = STATE.get();
        if (state[0] == 0) return false;
        return ++state[1] > MAX_EVAL_ENTRIES_PER_TOP_LEVEL;
    }

    /**
     * Non-consuming check. Results computed while over budget are DEGRADED (not fully simplified) and
     * must not be memoized: a cache is shared across top-level operations, and a degraded entry would leak
     * budget state from one operation into another — nondeterministically, since thread assignment varies
     * (the determinism epic, jfocus-standardize DESIGN round 8, is why this matters).
     */
    public static boolean overBudget() {
        return STATE.get()[1] > MAX_EVAL_ENTRIES_PER_TOP_LEVEL;
    }
}
