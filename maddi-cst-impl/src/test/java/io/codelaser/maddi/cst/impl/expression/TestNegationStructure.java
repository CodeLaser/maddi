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
import io.codelaser.maddi.cst.api.expression.InstanceOf;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
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

    private static final Set<String> ALL_OFF = Set.of("NOSTRUCTNEG", "NOSCOPEDNEGMEMO", "NOSORTKEYS");

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
}
