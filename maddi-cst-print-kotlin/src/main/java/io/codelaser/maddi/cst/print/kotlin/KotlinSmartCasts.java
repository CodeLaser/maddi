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
package io.codelaser.maddi.cst.print.kotlin;

import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.expression.BinaryOperator;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.InlineConditional;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.SwitchExpression;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import io.codelaser.maddi.cst.api.statement.AssertStatement;
import io.codelaser.maddi.cst.api.statement.ForStatement;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.statement.TryStatement;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Kotlin smart-casts a {@code val} to non-null after {@code x!!}, in all the code that assertion dominates: the rest of
 * its statement after it, the statements after it in its block, and the blocks nested in those. Java has no such
 * thing, so the printer would write {@code x!!} at every use (kotlinc: "unnecessary non-null assertion"). This keeps
 * the parameters and the locals that are never reassigned, asserted where their statement always evaluates them,
 * per block: what a branch, a loop body or a lambda asserts holds in that block only.
 */
final class KotlinSmartCasts {

    private static final ThreadLocal<Deque<Set<Variable>>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);

    private KotlinSmartCasts() {
    }

    /** A block starts: what it asserts holds until it ends. */
    static void enterBlock() {
        FRAMES.get().push(new HashSet<>());
    }

    static void exitBlock() {
        FRAMES.get().pop();
    }

    /** A method body starts: nothing asserted around it holds there (an anonymous class's method). */
    static Deque<Set<Variable>> enterMethod() {
        Deque<Set<Variable>> saved = FRAMES.get();
        FRAMES.set(new ArrayDeque<>());
        return saved;
    }

    static void exitMethod(Deque<Set<Variable>> saved) {
        FRAMES.set(saved);
    }

    /** The variable was asserted non-null by a {@code !!} that dominates this point. */
    static boolean asserted(Variable variable) {
        for (Set<Variable> frame : FRAMES.get()) {
            if (frame.contains(variable)) return true;
        }
        return false;
    }

    /** {@code e!!} was printed: when e is a stable variable its statement always evaluates, what follows knows it. */
    static void assertedAt(Expression e) {
        Deque<Set<Variable>> frames = FRAMES.get();
        if (frames.isEmpty() || !KotlinContext.translatingJava()) return;
        if (!(KotlinExpressionPrinter.unwrap(e) instanceof VariableExpression ve)) return;
        Variable variable = ve.variable();
        if (!(variable instanceof ParameterInfo || variable instanceof LocalVariable) || KotlinContext.reassigned(variable)) {
            return;
        }
        Statement statement = KotlinContext.currentStatement();
        if (statement != null && unconditional(statement).contains(ve)) frames.peek().add(variable);
    }

    /**
     * The variable expressions a statement evaluates whenever it runs, before its nested blocks: its header expressions,
     * leaving out a lambda's body, the branches of {@code ?:} and of a switch expression, and the right-hand side of a
     * boolean operator.
     */
    static Set<Expression> unconditional(Statement statement) {
        Set<Expression> result = Collections.newSetFromMap(new IdentityHashMap<>());
        if (statement instanceof AssertStatement || statement instanceof TryStatement) return result;
        if (statement instanceof LocalVariableCreation lvc) {
            lvc.localVariableStream().forEach(lv -> {
                if (lv.assignmentExpression() != null) collect(lv.assignmentExpression(), result);
            });
            return result;
        }
        if (statement instanceof ForStatement) {
            // the condition: its initialisers are statements, its updaters run after the body, if ever
            if (statement.expression() != null) collect(statement.expression(), result);
            return result;
        }
        if (statement.expression() != null) collect(statement.expression(), result);
        return result;
    }

    private static void collect(Expression expression, Set<Expression> result) {
        expression.visit((Element e) -> {
            switch (e) {
                case Lambda l -> {
                    return false;
                }
                case InlineConditional ic -> {
                    collect(ic.condition(), result);
                    return false;
                }
                case SwitchExpression se -> {
                    collect(se.selector(), result);
                    return false;
                }
                case BinaryOperator bo when bo.operator() != null
                                            && ("&&".equals(bo.operator().name()) || "||".equals(bo.operator().name())) -> {
                    collect(bo.lhs(), result);
                    return false;
                }
                // a conjunction or disjunction in canonical order, not the evaluation's: none of it is certain
                case io.codelaser.maddi.cst.api.expression.And and -> {
                    return false;
                }
                case io.codelaser.maddi.cst.api.expression.Or or -> {
                    return false;
                }
                case VariableExpression ve -> {
                    result.add(ve);
                    return true;
                }
                default -> {
                    return true;
                }
            }
        });
    }
}
