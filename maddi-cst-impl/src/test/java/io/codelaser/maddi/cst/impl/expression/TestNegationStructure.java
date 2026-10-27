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

import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.InstanceOf;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.impl.expression.eval.EvalBudget;
import io.codelaser.maddi.cst.impl.expression.eval.EvalOptions;
import io.codelaser.maddi.cst.impl.expression.eval.EvalSwitches;
import io.codelaser.maddi.cst.impl.expression.util.AndOrSorter;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The structural answer to isNegationOf for an And against an Or (gate NOSTRUCTNEG), the per-operation negation
 * memo (NOSCOPEDNEGMEMO) and the keyed sort (NOSORTKEYS): each must agree with the path it replaces, except where
 * the loss is documented here.
 */
public class TestNegationStructure extends CommonTest {

    private static final Set<String> ALL_OFF = Set.of("NOSTRUCTNEG", "NOSCOPEDNEGMEMO", "NOSORTKEYS", "NOATOMNEG",
            "NOEARLYCAP");

    @AfterEach
    public void gatesOn() {
        EvalSwitches.off(Set.of());
    }

    private boolean bothWays(BooleanSupplier query, boolean expected) {
        EvalSwitches.off(Set.of());
        boolean structural = query.getAsBoolean();
        EvalSwitches.off(ALL_OFF);
        boolean deMorgan = query.getAsBoolean();
        EvalSwitches.off(Set.of());
        assertEquals(expected, structural, "structural");
        assertEquals(expected, deMorgan, "De Morgan");
        return structural;
    }

    @DisplayName("an And and an Or are negations when their operands are, pairwise, in any order")
    @Test
    public void andVersusOr() {
        Expression and = r.and(a, b);
        Expression or = r.or(r.negate(a), r.negate(b));
        bothWays(() -> r.isNegationOf(and, or), true);
        bothWays(() -> r.isNegationOf(or, and), true);
        bothWays(() -> r.isNegationOf(and, r.or(r.negate(a), c)), false);
        bothWays(() -> r.isNegationOf(r.and(a, b, c), or), false);
        bothWays(() -> r.isNegationOf(r.and(a, b), r.and(r.negate(a), r.negate(b))), false);
    }

    @DisplayName("nested compounds and numeric atoms")
    @Test
    public void nested() {
        Expression and = r.and(a, r.or(b, c));
        Expression or = r.or(r.negate(a), r.and(r.negate(b), r.negate(c)));
        assertEquals("a&&(b||c)", and.toString());
        assertEquals("!a||!b&&!c", or.toString());
        bothWays(() -> r.isNegationOf(and, or), true);
        bothWays(() -> r.isNegationOf(and, r.or(r.negate(a), r.and(r.negate(b), c))), false);

        Expression geq = r.greater(k, r.newInt(3), true);
        Expression lt = r.less(k, r.newInt(3), false);
        bothWays(() -> r.isNegationOf(r.and(geq, a), r.or(lt, r.negate(a))), true);
        bothWays(() -> r.isNegationOf(r.and(geq, a), r.or(geq, r.negate(a))), false);
    }

    @DisplayName("an atom against a compound takes the full route: the instanceof negation is an Or")
    @Test
    public void instanceOfAgainstOr() {
        InstanceOf io = newInstanceOf(s, r.stringTypeInfo().asParameterizedType());
        Expression negated = r.negate(io);
        assertEquals("null==s||!(s instanceof String)", negated.toString());
        bothWays(() -> r.isNegationOf(io, negated), true);
        bothWays(() -> r.isNegationOf(negated, io), true);
        // inside compounds, the Or's two operands stand for one atom: the pairwise walk matches them
        bothWays(() -> r.isNegationOf(r.and(io, a), r.or(negated, r.negate(a))), true);
    }

    private Expression field(String name) {
        TypeInfo ti = r.newTypeInfo(r.stringTypeInfo(), "F");
        FieldInfo fi = r.newFieldInfo(name, true, r.booleanParameterizedType(), ti);
        return r.newVariableExpression(r.newFieldReference(fi));
    }

    private List<Expression> randomTerms(Random random, int n) {
        List<Expression> pool = new ArrayList<>();
        Expression[] bools = {a, b, c, d, field("f"), field("g")};
        Expression[] ints = {i, j, k};
        for (int t = 0; t < n; t++) {
            Expression e = switch (random.nextInt(6)) {
                case 0 -> bools[random.nextInt(bools.length)];
                case 1 -> r.negate(bools[random.nextInt(bools.length)]);
                case 2 -> r.greater(ints[random.nextInt(ints.length)], r.newInt(random.nextInt(5)), random.nextBoolean());
                case 3 -> r.equals(r.nullConstant(), random.nextBoolean() ? s : dd);
                case 4 -> r.negate(r.equals(r.nullConstant(), random.nextBoolean() ? s : dd));
                default -> newInstanceOf(s, r.stringTypeInfo().asParameterizedType());
            };
            pool.add(e);
        }
        return pool;
    }

    @DisplayName("the keyed sort orders as the TreeMap sort did")
    @Test
    public void sortEquivalence() {
        Random random = new Random(7);
        for (int round = 0; round < 200; round++) {
            List<Expression> terms = randomTerms(random, 1 + random.nextInt(12));
            EvalSwitches.off(Set.of());
            List<Expression> keyed = AndOrSorter.sort(terms);
            EvalSwitches.off(Set.of("NOSORTKEYS"));
            List<Expression> treeMap = AndOrSorter.sort(terms);
            EvalSwitches.off(Set.of());
            assertEquals(treeMap, keyed, "round " + round + ": " + terms);
        }
    }

    @DisplayName("And/Or over random terms evaluate the same with and without the gates")
    @Test
    public void evalEquivalence() {
        Random random = new Random(11);
        for (int round = 0; round < 300; round++) {
            List<Expression> terms = randomTerms(random, 2 + random.nextInt(6));
            List<Expression> clauses = new ArrayList<>();
            for (int c = 0; c < 1 + random.nextInt(3); c++) {
                List<Expression> sub = randomTerms(random, 1 + random.nextInt(3));
                clauses.add(random.nextBoolean() ? r.or(sub) : r.and(sub));
            }
            List<Expression> all = new ArrayList<>(terms);
            all.addAll(clauses);
            // fresh runtimes: the negation cache must not carry one side's results to the other
            RuntimeImpl r1 = new RuntimeImpl();
            RuntimeImpl r2 = new RuntimeImpl();
            EvalSwitches.off(Set.of());
            String structural = r1.and(all) + " | " + r1.or(all) + " | " + r1.negate(r1.and(all));
            EvalSwitches.off(ALL_OFF);
            String deMorgan = r2.and(all) + " | " + r2.or(all) + " | " + r2.negate(r2.and(all));
            EvalSwitches.off(Set.of());
            assertEquals(deMorgan, structural, "round " + round + ": " + all);
        }
    }

    /** TestEvalBudget.shared over LOCAL variables, which the global negation cache refuses */
    private Expression shared(int depth) {
        Expression e = a;
        for (int i = 0; i < depth; i++) {
            Expression p = r.newVariableExpression(createVariable("p" + i, r.booleanParameterizedType()));
            Expression q = r.newVariableExpression(createVariable("q" + i, r.booleanParameterizedType()));
            e = r.and(r.or(p, e), r.or(q, e));
        }
        return e;
    }

    @DisplayName("the per-operation memo makes negating a shared tree over locals proportional to its size")
    @Test
    public void scopedMemo() {
        Expression e = shared(8);
        Expression negated = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> r.negate(e));
        EvalSwitches.off(ALL_OFF);
        Expression reference = new RuntimeImpl().negate(e);
        assertEquals(reference.toString(), negated.toString());
        assertEquals(3063, negated.complexity());
    }

    private List<Expression> booleans(String prefix, int n) {
        List<Expression> list = new ArrayList<>();
        for (int t = 0; t < n; t++) {
            list.add(r.newVariableExpression(createVariable(prefix + t, r.booleanParameterizedType())));
        }
        return list;
    }

    /** the evaluator entries isNegationOf consumes, inside one operation */
    private int entriesOf(BooleanSupplier query, boolean expected) {
        EvalBudget.enter();
        try {
            int before = EvalBudget.consumed();
            assertEquals(expected, query.getAsBoolean());
            return EvalBudget.consumed() - before;
        } finally {
            EvalBudget.exit();
        }
    }

    @DisplayName("maddi-mod#61: an atom against a large compound negates the atom, not the compound")
    @Test
    public void atomAgainstLargeCompound() {
        List<Expression> ps = booleans("p", 20);
        List<Expression> ors = new ArrayList<>();
        for (int t = 0; t + 1 < ps.size(); t += 2) ors.add(r.or(ps.get(t), ps.get(t + 1)));
        Expression bigAnd = r.and(ors);
        assertTrue(bigAnd.complexity() > 40, "complexity " + bigAnd.complexity());
        assertEquals(0, entriesOf(() -> r.isNegationOf(a, bigAnd), false));
        assertEquals(0, entriesOf(() -> r.isNegationOf(bigAnd, a), false));
        EvalSwitches.off(Set.of("NOATOMNEG"));
        assertTrue(entriesOf(() -> r.isNegationOf(a, bigAnd), false) > 0, "the full route evaluates the negation");
    }

    @DisplayName("maddi-mod#61: a small compound whose negation collapses to the atom keeps the full route")
    @Test
    public void smallCompoundCollapsingToAtom() {
        Expression or = r.or(r.less(k, r.newInt(3), false), r.greater(k, r.newInt(3), false));
        assertEquals("k<3||k>=4", or.toString());
        Expression eq = r.equals(r.newInt(3), k);
        assertEquals("3==k", r.negate(or).toString());
        bothWays(() -> r.isNegationOf(eq, or), true);
        // the route negates its SECOND operand, here the atom, whose negation !(3==k) does not collapse: an
        // asymmetry that predates maddi-mod#61, pinned so that a change to it is deliberate
        bothWays(() -> r.isNegationOf(or, eq), false);
        // with the atom's negation memoized first: the memo may confirm, never deny
        EvalBudget.enter();
        try {
            assertEquals("3!=k", r.negate(eq).toString());
            assertTrue(r.isNegationOf(eq, or));
        } finally {
            EvalBudget.exit();
        }
    }

    @DisplayName("maddi-mod#61: over the complexity cap, only the plain A && !A is recognized, and nothing is negated")
    @Test
    public void earlyCap() {
        RuntimeImpl small = new RuntimeImpl(new EvalOptions(6));
        Expression aa = small.newVariableExpression(createVariable("aa", small.booleanParameterizedType()));
        Expression bb = small.newVariableExpression(createVariable("bb", small.booleanParameterizedType()));
        Expression cc = small.newVariableExpression(createVariable("cc", small.booleanParameterizedType()));
        assertEquals("false", small.and(aa, bb, cc, small.negate(aa)).toString());
        assertEquals("true", small.or(aa, bb, cc, small.negate(aa)).toString());
        // under the cap, a compound negation pair is still found
        assertEquals("false", r.and(r.or(a, b), r.and(r.negate(a), r.negate(b))).toString());
    }

    @DisplayName("maddi-mod#61: the work budget is an EvalOptions setting")
    @Test
    public void settableBudget() {
        assertEquals("a", r.and(a, r.or(a, b)).toString());
        RuntimeImpl none = new RuntimeImpl(new EvalOptions(500, 0, false));
        Expression a0 = none.newVariableExpression(createVariable("a", none.booleanParameterizedType()));
        Expression b0 = none.newVariableExpression(createVariable("b", none.booleanParameterizedType()));
        assertEquals("a&&(a||b)", none.and(a0, none.or(a0, b0)).toString(), "no budget: combined, not reduced");
        assertEquals(1, EvalOptions.DEFAULT.passCost(2000));
        assertEquals(21, new EvalOptions(500, 100_000, true).passCost(2000));
    }
}
