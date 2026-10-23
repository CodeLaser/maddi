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
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Kotlin smart-casts a {@code val} to non-null after {@code x!!}, in all the code that assertion dominates: the
 * rest of its statement, the statements after it in its block, and the blocks nested in those. Java has no such
 * thing, so the printer would write {@code x!!} at every use (kotlinc: "unnecessary non-null assertion"). This keeps
 * the parameters and the locals asserted where their statement always evaluates them, per block: what a branch, a loop
 * body or a lambda asserts holds in that block only. A {@code var} is known until a statement assigns it, and not in a
 * lambda.
 * <p>
 * The same for Java's {@code instanceof} and Kotlin's {@code is}: where {@code x is T} holds (an if's block, the right
 * of a {@code &&}, a conditional's branch, the rest of a block after {@code if (x !is T) return}) Kotlin smart-casts
 * x, and Java's {@code (T) x} needs no cast.
 */
final class KotlinSmartCasts {

    /**
     * Where an assertion was printed: its statement, and the asserted expression's place in the source.
     *
     * @param scope for a {@code var}: the body it was asserted in. Kotlin does not smart-cast a var inside a lambda
     *              (which may run after an assignment); null for a val, which it smart-casts there too
     */
    private record Fact(Statement statement, io.codelaser.maddi.cst.api.element.Source at, Object scope) {

        /**
         * After the statement, or later in it than the assertion: Java evaluates left to right, and a second print of
         * the asserted expression itself is at the same place, so it does not take itself for a smart cast.
         */
        boolean holdsAt(Expression use) {
            if (scope != null && scope != KotlinContext.currentScope()) return false;
            if (statement != KotlinContext.currentStatement()) return true;
            io.codelaser.maddi.cst.api.element.Source u = use == null ? null : use.source();
            if (u == null || at == null || u.isNoSource() || at.isNoSource()) return false;
            return u.beginLine() > at.endLine() || u.beginLine() == at.endLine() && u.beginPos() > at.endPos();
        }
    }

    /**
     * A getter call printed as a local val ({@link KotlinValCopies}): {@code val exprents = stat.getExprents()} before
     * the statement, {@code exprents} for each call of the getter on the same receiver after it.
     *
     * @param receiver      the receiver's variable, or {@link #THIS}
     * @param type          the call's Kotlin type, the val's
     * @param statementOnly the copy prints the calls in its statement only, not those after it: the getter's field
     *                      can change, though not in the statement (CodeLaser/maddi#164)
     */
    record Copy(String name, io.codelaser.maddi.cst.api.info.MethodInfo getter, Object receiver, ParameterizedType type,
                boolean statementOnly) {
        boolean copies(io.codelaser.maddi.cst.api.expression.MethodCall call) {
            return call.methodInfo() == getter && call.parameterExpressions().isEmpty() && receiver.equals(receiverKey(call));
        }
    }

    /** The receiver of an implicit or explicit {@code this} call. */
    static final Object THIS = new Object();

    /** The receiver of a call as a key: its variable, {@link #THIS}, or null when it is any other expression. */
    static Object receiverKey(io.codelaser.maddi.cst.api.expression.MethodCall call) {
        if (call.object() == null || call.objectIsImplicit()) return THIS;
        Expression object = KotlinExpressionPrinter.unwrap(call.object());
        if (object instanceof VariableExpression ve) {
            if (ve.variable() instanceof io.codelaser.maddi.cst.api.variable.This) return THIS;
            return ve.variable();
        }
        return null;
    }

    /** What the conditions that hold say: the {@code is} tests' types, and what is not null. */
    record Facts(Map<Variable, ParameterizedType> types, Set<Object> nonNull) {
        Facts() {
            this(new HashMap<>(), new HashSet<>());
        }

        boolean isEmpty() {
            return types.isEmpty() && nonNull.isEmpty();
        }
    }

    /** What holds in a block, or in the part of an expression a condition guards. */
    private record Frame(List<Statement> statements, Map<Object, Fact> asserted,
                         Map<Variable, ParameterizedType> types, Map<Variable, Statement> typeSources,
                         Map<Object, Statement> nonNull, Map<Copy, Statement> copies) {
        Frame(List<Statement> statements) {
            this(statements, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new java.util.LinkedHashMap<>());
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

    /**
     * The variable (or a {@link Copy}), used at {@code use}, is known non-null: a {@code !!} that dominates it, an
     * {@code is} test or a {@code != null} that holds there.
     */
    static boolean asserted(Object key, Expression use) {
        for (Frame frame : FRAMES.get()) {
            Fact fact = frame.asserted().get(key);
            if (fact != null && fact.holdsAt(use)) return true;
            if (frame.types().containsKey(key) && Frame.visible(frame.typeSources().get(key))) return true;
            if (frame.nonNull().containsKey(key) && Frame.visible(frame.nonNull().get(key))) return true;
        }
        return false;
    }

    /** The copy that prints this getter call, if one was declared for the statement or before it in its block. */
    static Copy copy(io.codelaser.maddi.cst.api.expression.MethodCall call) {
        if (call.methodInfo() == null || !call.parameterExpressions().isEmpty()) return null;
        for (Frame frame : FRAMES.get()) {
            for (Copy copy : frame.copies().keySet()) {
                if (copy.copies(call)) return copy;
            }
        }
        return null;
    }

    /** The statement is one of the innermost block's own: a copy can be declared before it. */
    static boolean inBlock(Statement statement) {
        return blockOf(statement) != null;
    }

    /** {@code copy} is declared before {@code statement}, and prints the getter's calls from there to the block's end. */
    static void declare(Copy copy, Statement statement) {
        Frame frame = blockOf(statement);
        if (frame != null) frame.copies().put(copy, statement);
    }

    /** {@code statement} is printed: the copies that serve only it end. */
    static void endStatement(Statement statement) {
        for (Frame frame : FRAMES.get()) {
            frame.copies().entrySet().removeIf(e -> e.getValue() == statement && e.getKey().statementOnly());
        }
    }

    /** The copies declared before {@code statement}: a second print of it declares them again. */
    static void forget(Statement statement) {
        for (Frame frame : FRAMES.get()) frame.copies().values().removeIf(s -> s == statement);
    }

    /** The names of the copies in scope. */
    static Set<String> copyNames() {
        Set<String> names = new HashSet<>();
        for (Frame frame : FRAMES.get()) frame.copies().keySet().forEach(c -> names.add(c.name()));
        return names;
    }

    /** The type an {@code is} test that dominates this point gave the variable; null when none did. */
    static ParameterizedType knownType(Variable variable) {
        for (Frame frame : FRAMES.get()) {
            ParameterizedType type = frame.types().get(variable);
            if (type != null && Frame.visible(frame.typeSources().get(variable))) return type;
        }
        return null;
    }

    /** {@code printer} where {@code facts} hold: an {@code is} test's types, a {@code != null}'s non-null values. */
    static <T> T withTypes(Facts facts, Supplier<T> printer) {
        if (facts.isEmpty()) return printer.get();
        Frame frame = new Frame(null);
        frame.types().putAll(facts.types());
        facts.nonNull().forEach(k -> frame.nonNull().put(k, null));
        FRAMES.get().push(frame);
        try {
            return printer.get();
        } finally {
            FRAMES.get().pop();
        }
    }

    /** From here to the end of the block, the variables of {@code types} are known to be of those types. */
    static void addTypes(Facts facts) {
        Statement statement = KotlinContext.currentStatement();
        Frame frame = blockOf(statement);
        if (frame != null) {
            frame.types().putAll(facts.types());
            facts.types().keySet().forEach(v -> frame.typeSources().put(v, statement));
            facts.nonNull().forEach(k -> frame.nonNull().put(k, statement));
        }
    }

    /** The {@code is} tests that hold when {@code condition} is true: {@code x instanceof T}, and those of a {@code &&}. */
    static Facts whenTrue(Expression condition) {
        Facts result = new Facts();
        tests(condition, true, result);
        return result;
    }

    /** The {@code is} tests that hold when {@code condition} is false: {@code !(x instanceof T)}, and those of a {@code ||}. */
    static Facts whenFalse(Expression condition) {
        Facts result = new Facts();
        tests(condition, false, result);
        return result;
    }

    private static void tests(Expression condition, boolean truth, Facts result) {
        Expression e = KotlinExpressionPrinter.unwrap(condition);
        switch (e) {
            case InstanceOf io when truth && io.patternVariable() == null -> {
                if (KotlinExpressionPrinter.unwrap(io.expression()) instanceof VariableExpression ve && stable(ve.variable())) {
                    result.types().put(ve.variable(), io.testType());
                    result.nonNull().add(ve.variable());
                }
            }
            case BinaryOperator bo when bo.operator() != null
                                        && (truth ? "!=" : "==").equals(bo.operator().name()) -> {
                // x != null holds: a stable x, or a copied getter, is known non-null where it does
                Expression tested = KotlinExpressionPrinter.unwrap(bo.rhs()) instanceof io.codelaser.maddi.cst.api.expression.NullConstant
                        ? bo.lhs() : KotlinExpressionPrinter.unwrap(bo.lhs()) instanceof io.codelaser.maddi.cst.api.expression.NullConstant
                        ? bo.rhs() : null;
                Expression t = tested == null ? null : KotlinExpressionPrinter.unwrap(tested);
                if (t instanceof VariableExpression ve && stable(ve.variable())) result.nonNull().add(ve.variable());
                if (t instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc) {
                    Copy copy = copy(mc);
                    if (copy != null) result.nonNull().add(copy);
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
        if (KotlinExpressionPrinter.unwrap(e) instanceof io.codelaser.maddi.cst.api.expression.MethodCall mc) {
            // a copy is a val: after copy!! it is non-null where the assertion dominates
            Copy copy = copy(mc);
            Statement statement = KotlinContext.currentStatement();
            Frame frame = blockOf(statement);
            if (copy != null && frame != null && unconditional(statement).contains(mc)) {
                frame.asserted().putIfAbsent(copy, new Fact(statement, mc.source(), null));
            }
            return;
        }
        if (!(KotlinExpressionPrinter.unwrap(e) instanceof VariableExpression ve)) return;
        Variable variable = ve.variable();
        if (!(variable instanceof ParameterInfo || variable instanceof LocalVariable)) return;
        boolean isVar = KotlinContext.reassigned(variable);
        Statement statement = KotlinContext.currentStatement();
        Frame frame = blockOf(statement);
        if (frame == null || !unconditional(statement).contains(ve)) return;
        // x = x!!.next(): the statement assigns x after the assertion, which then says nothing about the new value
        if (isVar && KotlinAssignments.assignedIn(statement, variable)) return;
        frame.asserted().putIfAbsent(variable, new Fact(statement, ve.source(), isVar ? KotlinContext.currentScope() : null));
    }

    /**
     * A statement starts: a {@code var} it assigns, anywhere in it (a loop's body included, which runs again after
     * its assignment), is no longer known non-null, here or after it.
     */
    static void beforeStatement(Statement statement) {
        for (Frame frame : FRAMES.get()) {
            frame.asserted().entrySet().removeIf(entry -> entry.getValue().scope() != null
                                                          && entry.getKey() instanceof Variable v
                                                          && KotlinAssignments.assignedIn(statement, v));
        }
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
                case io.codelaser.maddi.cst.api.expression.MethodCall mc -> {
                    result.add(mc);
                    return true;
                }
                default -> {
                    return true;
                }
            }
        });
    }
}
