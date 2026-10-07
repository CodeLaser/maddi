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

package io.codelaser.maddi.cst.impl.expression;

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Negation;
import io.codelaser.maddi.cst.impl.expression.eval.EvalBudget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CancellationException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * maddi-mod#20: the boolean evaluator must stop when its thread is interrupted, and negating an And/Or whose
 * operands share subtrees must not recompute them exponentially (the negation cache skips local variables, and
 * over budget it skips everything).
 */
public class TestEvalBudget extends CommonTest {

    /**
     * e_k = (p_k || e_{k-1}) && (q_k || e_{k-1}): every level mentions the level below twice, so the printed size
     * doubles per level; the evaluator must stay proportional to that size, not to the number of paths through it.
     */
    private Expression shared(int depth) {
        Expression e = a;
        for (int i = 0; i < depth; i++) {
            Expression p = r.newVariableExpression(createVariable("p" + i, r.booleanParameterizedType()));
            Expression q = r.newVariableExpression(createVariable("q" + i, r.booleanParameterizedType()));
            e = r.and(r.or(p, e), r.or(q, e));
        }
        return e;
    }

    @DisplayName("over budget, negating an And/Or is the plain negation: no De Morgan")
    @Test
    public void overBudget() {
        Expression and = r.and(a, r.or(b, c));
        Expression other = r.and(b, r.or(c, d));
        assertEquals("!a||!b&&!c", r.negate(and).toString());
        EvalBudget.enter();
        try {
            while (!EvalBudget.exhausted()) {
                // burn the budget of this top-level operation
            }
            Expression negated = r.negate(and);
            assertInstanceOf(Negation.class, negated);
            assertSame(and, ((Negation) negated).expression());
            assertTrue(r.isNegationOf(negated, and));
        } finally {
            EvalBudget.exit();
        }
        // between top-level operations the count still holds the last total: that must not degrade a fresh call
        assertTrue(EvalBudget.overBudget());
        assertEquals("!b||!c&&!d", r.negate(other).toString());
    }

    @DisplayName("building and negating a shared And/Or tree terminates")
    @Test
    public void sharedSubtrees() {
        // a guard against hangs, not a benchmark: overBudget() pins the fix itself
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            Expression e = shared(10);
            Expression negated = r.negate(e);
            assertNotNull(negated);
            r.negate(negated);
        });
    }

    @DisplayName("an interrupted thread stops the evaluator with a CancellationException")
    @Test
    public void interrupted() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> r.and(a, r.or(b, c)));
            assertTrue(Thread.currentThread().isInterrupted(), "the flag is read, not cleared");
        } finally {
            Thread.interrupted();
        }
        // and without the flag, the same call evaluates
        assertEquals("a&&(b||c)", r.and(a, r.or(b, c)).toString());
    }
}
