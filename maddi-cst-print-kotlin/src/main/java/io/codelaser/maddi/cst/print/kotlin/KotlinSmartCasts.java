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

import io.codelaser.maddi.cst.api.expression.And;
import io.codelaser.maddi.cst.api.expression.InstanceOf;
import io.codelaser.maddi.cst.api.expression.Negation;
import io.codelaser.maddi.cst.api.expression.Or;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Kotlin smart-casts a {@code val} to non-null after {@code x!!}, in all the code that assertion dominates: the
 * statements after it in its block, and the blocks nested in those (not the rest of its own statement, which the
 * printer may print twice). Java has no such
 * thing, so the printer would write {@code x!!} at every use (kotlinc: "unnecessary non-null assertion"). This keeps
 * the parameters and the locals that are never reassigned, asserted where their statement always evaluates them,
 * per block: what a branch, a loop body or a lambda asserts holds in that block only.
 * <p>
 * The same for Java's {@code instanceof} and Kotlin's {@code is}: where {@code x is T} holds (an if's block, the right
 * of a {@code &&}, a conditional's branch, the rest of a block after {@code if (x !is T) return}) Kotlin smart-casts
 * x, and Java's {@code (T) x} needs no cast.
 */
final class KotlinSmartCasts {

    /** What holds in a block, or in the part of an expression a condition guards. */
    private record Frame(List<Statement> statements, Map<Variable, Statement> asserted,
                         Map<Variable, ParameterizedType> types, Map<Variable, Statement> typeSources) {
        Frame(List<Statement> statements) {
            this(statements, new HashMap<>(), new HashMap<>(), new HashMap<>());
        }

        /**
         * A fact holds where it was established from, never in that statement itself: the printer may print a
         * statement, or a part of it, twice, and the second print must not take the first's {@code !!} for a smart cast.
         */
        static boolean visible(Statement source) {
            return source == null || source != KotlinContext.currentStatement();
        }

        /** A statement of this block itself: what it establishes holds for the statements after it. */
        boolean holds(Statement statement) {
            return statements != null && statement != null && statements.stream().anyMatch(s -> s == statement);
        }
    }

    private static final ThreadLocal<Deque<Frame>> FRAMES = ThreadLocal.withInitial(ArrayDeque::new);

    private KotlinSmartCasts() {
    }

    /** A block starts: what its statements assert holds until it ends. */
    static void enterBlock(List<Statement> statements) {
        FRAMES.get().push(new Frame(statements));
    }

    /**
     * The innermost block's frame, when {@code statement} is one of its own statements; null otherwise (a switch
     * entry's statements, printed without a block of their own, whose facts must not reach the other entries).
     */
    private static Frame blockOf(Statement statement) {
        for (Frame frame : FRAMES.get()) {
            if (frame.statements() != null) return frame.holds(statement) ? frame : null;
        }
        return null;
    }

    static void exitBlock() {
        FRAMES.get().pop();
    }

    /** A method body starts: nothing asserted around it holds there (an anonymous class's method). */
    static Deque<?> enterMethod() {
        Deque<Frame> saved = FRAMES.get();
        FRAMES.set(new ArrayDeque<>());
        return saved;
    }

    @SuppressWarnings("unchecked")
    static void exitMethod(Deque<?> saved) {
        FRAMES.set((Deque<Frame>) saved);
    }

    /** The variable was asserted non-null by a {@code !!} that dominates this point. */
    static boolean asserted(Variable variable) {
        for (Frame frame : FRAMES.get()) {
            if (frame.asserted().containsKey(variable) && Frame.visible(frame.asserted().get(variable))) return true;
            if (frame.types().containsKey(variable) && Frame.visible(frame.typeSources().get(variable))) return true;
        }
        return false;
    }

    /** The type an {@code is} test that dominates this point gave the variable; null when none did. */
    static ParameterizedType knownType(Variable variable) {
        for (Frame frame : FRAMES.get()) {
            ParameterizedType type = frame.types().get(variable);
            if (type != null && Frame.visible(frame.typeSources().get(variable))) return type;
        }
        return null;
    }

    /** {@code printer} with the variables of {@code types} known to be of those types: an {@code is} test holds. */
    static <T> T withTypes(Map<Variable, ParameterizedType> types, Supplier<T> printer) {
        if (types.isEmpty()) return printer.get();
        Frame frame = new Frame(null);
        frame.types().putAll(types);
        FRAMES.get().push(frame);
        try {
            return printer.get();
        } finally {
            FRAMES.get().pop();
        }
    }

    /** From here to the end of the block, the variables of {@code types} are known to be of those types. */
    static void addTypes(Map<Variable, ParameterizedType> types) {
        Statement statement = KotlinContext.currentStatement();
        Frame frame = blockOf(statement);
        if (frame != null) {
            frame.types().putAll(types);
            types.keySet().forEach(v -> frame.typeSources().put(v, statement));
        }
    }

    /** The {@code is} tests that hold when {@code condition} is true: {@code x instanceof T}, and those of a {@code &&}. */
    static Map<Variable, ParameterizedType> whenTrue(Expression condition) {
        Map<Variable, ParameterizedType> result = new HashMap<>();
        tests(condition, true, result);
        return result;
    }

    /** The {@code is} tests that hold when {@code condition} is false: {@code !(x instanceof T)}, and those of a {@code ||}. */
    static Map<Variable, ParameterizedType> whenFalse(Expression condition) {
        Map<Variable, ParameterizedType> result = new HashMap<>();
        tests(condition, false, result);
        return result;
    }

    private static void tests(Expression condition, boolean truth, Map<Variable, ParameterizedType> result) {
        Expression e = KotlinExpressionPrinter.unwrap(condition);
        switch (e) {
            case InstanceOf io when truth && io.patternVariable() == null -> {
                if (KotlinExpressionPrinter.unwrap(io.expression()) instanceof VariableExpression ve && stable(ve.variable())) {
                    result.put(ve.variable(), io.testType());
                }
            }
            case Negation neg -> tests(neg.expression(), !truth, result);
            case io.codelaser.maddi.cst.api.expression.UnaryOperator uo when uo.operator() != null
                                                                          && "!".equals(uo.operator().name()) ->
                    tests(uo.expression(), !truth, result);
            case BinaryOperator bo when bo.operator() != null
                                        && (truth ? "&&" : "||").equals(bo.operator().name()) -> {
                tests(bo.lhs(), truth, result);
                tests(bo.rhs(), truth, result);
            }
            case And and when truth -> and.expressions().forEach(x -> tests(x, true, result));
            case Or or when !truth -> or.expressions().forEach(x -> tests(x, false, result));
            default -> {
            }
        }
    }

    /** A parameter, or a local never reassigned: Kotlin smart-casts it. */
    private static boolean stable(Variable variable) {
        return (variable instanceof ParameterInfo || variable instanceof LocalVariable) && !KotlinContext.reassigned(variable);
    }

    /** {@code e!!} was printed: when e is a stable variable its statement always evaluates, what follows knows it. */
    static void assertedAt(Expression e) {
        Deque<Frame> frames = FRAMES.get();
        if (frames.isEmpty() || !KotlinContext.translatingJava()) return;
        if (!(KotlinExpressionPrinter.unwrap(e) instanceof VariableExpression ve)) return;
        Variable variable = ve.variable();
        if (!stable(variable)) return;
        Statement statement = KotlinContext.currentStatement();
        Frame frame = blockOf(statement);
        if (frame != null && unconditional(statement).contains(ve)) frame.asserted().putIfAbsent(variable, statement);
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
