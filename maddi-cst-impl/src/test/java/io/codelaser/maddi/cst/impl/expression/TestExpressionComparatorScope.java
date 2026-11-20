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

package io.codelaser.maddi.cst.impl.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.expression.eval.EvalBudget;
import io.codelaser.maddi.cst.impl.expression.util.ExpressionComparator;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;

/**
 * ExpressionComparator's per-thread memo was cleared only when full: every thread that had compared expressions kept
 * up to 1,000 of them, and through them their model, alive after the model was dropped (150 MB on retrofit after an
 * idioms run, per long-lived server thread). The memo is scoped to one top-level evaluation now, and gone when it ends.
 */
public class TestExpressionComparatorScope {

    @DisplayName("the memo exists inside an operation only")
    @Test
    public void scope() {
        Runtime rt = new RuntimeImpl();
        Expression e1 = rt.newVariableExpression(rt.newLocalVariable("p", rt.intParameterizedType()));
        Expression e2 = rt.newVariableExpression(rt.newLocalVariable("q", rt.intParameterizedType()));
        int outside = ExpressionComparator.SINGLETON.compare(e1, e2);
        assertFalse(ExpressionComparator.hasThreadMemo(), "no scoped memo outside an operation");
        EvalBudget.enter();
        try {
            assertEquals(Integer.signum(outside), Integer.signum(ExpressionComparator.SINGLETON.compare(e1, e2)));
            assertTrue(ExpressionComparator.hasThreadMemo());
        } finally {
            EvalBudget.exit();
        }
        assertFalse(ExpressionComparator.hasThreadMemo(), "the memo is removed when the operation ends");
    }

    @DisplayName("after the operation, nothing of the compared model is reachable from the thread")
    @Test
    public void noLeak() throws InterruptedException {
        assertCollected(compareInFreshModel());
    }

    @DisplayName("a comparison outside any operation, through the weakly held memo, does not keep its model either")
    @Test
    public void noLeakOutside() throws InterruptedException {
        assertCollected(compareOutsideInFreshModel());
    }

    private static WeakReference<Expression> compareOutsideInFreshModel() {
        Runtime rt = new RuntimeImpl();
        Expression p = rt.newVariableExpression(rt.newLocalVariable("p", rt.intParameterizedType()));
        Expression q = rt.newVariableExpression(rt.newLocalVariable("q", rt.intParameterizedType()));
        ExpressionComparator.SINGLETON.compare(p, q);
        ExpressionComparator.SINGLETON.compare(q, p);
        return new WeakReference<>(p);
    }

    private static void assertCollected(WeakReference<Expression> operand) throws InterruptedException {
        for (int attempt = 0; attempt < 50 && operand.get() != null; attempt++) {
            System.gc();
            Thread.sleep(20);
        }
        assertNull(operand.get(), "an expression of the dropped model is still reachable");
        assertFalse(ExpressionComparator.hasThreadMemo());
    }

    // a model of its own, dropped on return; and(..) opens an operation and sorts its operands through the comparator,
    // negation's scoped memo is exercised by the negated operand
    private static WeakReference<Expression> compareInFreshModel() {
        Runtime rt = new RuntimeImpl();
        Expression p = rt.newVariableExpression(rt.newLocalVariable("p", rt.booleanParameterizedType()));
        Expression q = rt.newVariableExpression(rt.newLocalVariable("q", rt.booleanParameterizedType()));
        Expression s = rt.newVariableExpression(rt.newLocalVariable("s", rt.booleanParameterizedType()));
        Expression and = rt.and(rt.or(p, q), rt.negate(rt.and(q, s)), s);
        assertTrue(and.toString().contains("p"));
        return new WeakReference<>(p);
    }
}
