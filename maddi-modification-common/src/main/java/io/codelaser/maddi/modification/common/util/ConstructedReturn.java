/*
 * Copyright (c) 2022-2026, CodeLaser BV, Belgium.
 * Unauthorized copying of this file, via any medium, is strictly prohibited.
 * Proprietary and confidential.
 */
package io.codelaser.maddi.modification.common.util;

import io.codelaser.maddi.cst.api.element.Visitor;
import io.codelaser.maddi.cst.api.expression.Assignment;
import io.codelaser.maddi.cst.api.expression.Cast;
import io.codelaser.maddi.cst.api.expression.ConstructorCall;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.expression.MethodCall;
import io.codelaser.maddi.cst.api.expression.VariableExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.statement.Block;
import io.codelaser.maddi.cst.api.statement.LocalTypeDeclaration;
import io.codelaser.maddi.cst.api.statement.LocalVariableCreation;
import io.codelaser.maddi.cst.api.statement.ReturnStatement;
import io.codelaser.maddi.cst.api.statement.Statement;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Does a method <b>construct what it returns</b>? Decided from the shape of its body alone, so that a stub of it
 * can say {@code return new T();} instead of {@code return null;}.
 *
 * <h2>Why a stub's body matters</h2>
 * A stub is a declaration; its body is never run. But the analyses that consume an isolate read bodies, and
 * {@code return null;} tells them the factory returns <em>nothing anyone can reason about</em>: a local bound to
 * such a factory has no creation, so every write through it stays an unexplained write on an unknown object.
 * The link analysis, asked whether the returned object is fresh, answers "no summary" — and absence is refusal.
 * On a corpus where the flagship type is handed out by factories almost exclusively, that one line turned
 * every factory-anchored fill site into a site the modernization lane could not touch. The stub, not the code,
 * was answering "unknown" to a question the original answers plainly.
 *
 * <h2>The rule</h2>
 * Every {@code return} of the body (lambdas and local classes excluded) returns one of
 * <ul>
 *   <li>a plain construction, {@code new T(...)} — no anonymous body, no array;</li>
 *   <li>a call to a method that itself satisfies this rule (delegation, followed transitively through the
 *       declared bodies with a depth bound and a cycle guard);</li>
 *   <li>a local variable every assignment of which is one of the two above, and which is <b>only ever the
 *       receiver</b>: the object of a call ({@code t.set(..)}), the scope of a field write ({@code t.f = ..}),
 *       or the returned expression. A local that is passed as an argument, stored in a field or assigned to
 *       another variable has been published, and the method is refused.</li>
 * </ul>
 * The answer is the type constructed at the leaf, which the caller compares with the return type it is stubbing.
 *
 * <h2>What it gets wrong, on purpose</h2>
 * This is syntax, not the link analysis, and it is applied to the original's declared body without asking who
 * overrides it. Three shapes it misjudges: a receiver-only call that registers the object from the inside
 * ({@code t.attach(this.registry)}) passes; a factory returning a fresh object as an interface it implements
 * is refused because the constructed class is not the return type (the caller's check, not this one); a
 * non-final method whose override returns a pooled object passes on the base body. The first is the price
 * of not running an analysis on the corpus the isolate is taken from; the other two are recall, not
 * correctness — a refused method keeps {@code return null;}, which is what every stub said before.
 */
final class ConstructedReturn {

    private static final int MAX_DELEGATION_DEPTH = 6;

    private ConstructedReturn() {
    }

    /**
     * @return the class constructed on every return path of {@code methodInfo}, or {@code null} when the body
     * is absent, empty, or returns anything else on some path.
     */
    static TypeInfo of(MethodInfo methodInfo) {
        return of(methodInfo, new HashSet<>());
    }

    private static TypeInfo of(MethodInfo methodInfo, Set<MethodInfo> visiting) {
        if (methodInfo == null || visiting.size() >= MAX_DELEGATION_DEPTH || !visiting.add(methodInfo)) return null;
        try {
            Block body = methodInfo.methodBody();
            if (body == null || body.isEmpty()) return null;
            Walk walk = new Walk(visiting);
            body.visit(walk);
            return walk.verdict();
        } finally {
            visiting.remove(methodInfo);
        }
    }

    private static final class Walk implements Visitor {
        private final Set<MethodInfo> visiting;
        // a local initialised by a construction (or by a qualifying call), and what it constructs
        private final Map<Variable, TypeInfo> candidates = new HashMap<>();
        private final Set<Variable> disqualified = new HashSet<>();
        // occurrences of candidate locals, and those that are receiver positions or the returned expression
        private final Map<Variable, Integer> occurrences = new HashMap<>();
        private final Set<Expression> allowed = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Map<Variable, Integer> allowedCount = new HashMap<>();
        // what each return statement returns: the constructed type, or null for "something else"
        private final List<TypeInfo> returns = new ArrayList<>();
        private boolean refused;

        Walk(Set<MethodInfo> visiting) {
            this.visiting = visiting;
        }

        @Override
        public boolean beforeStatement(Statement statement) {
            if (refused) return false;
            // a local class has returns of its own; they are not this method's
            if (statement instanceof LocalTypeDeclaration) return false;
            if (statement instanceof LocalVariableCreation lvc) {
                lvc.localVariableStream().forEach(lv -> {
                    Expression init = lv.assignmentExpression();
                    if (init != null && !init.isEmpty()) {
                        TypeInfo constructed = constructs(init);
                        if (constructed != null) candidates.put(lv, constructed);
                    }
                });
            } else if (statement instanceof ReturnStatement rs) {
                // a Kotlin non-local return inside a lambda leaves a method other than the lambda: which one this
                // walk does not track, so it makes no claim
                if (rs.isNonLocal()) {
                    refused = true;
                    return false;
                }
                Expression e = rs.expression();
                if (e == null || e.isEmpty()) {
                    refused = true;
                    return false;
                }
                TypeInfo constructed = constructs(e);
                if (constructed != null) {
                    returns.add(constructed);
                } else if (e instanceof VariableExpression ve && ve.variable() instanceof LocalVariable lv) {
                    allowed.add(e);
                    returns.add(candidates.get(lv));   // null when the local was never a candidate
                } else {
                    returns.add(null);
                }
            }
            return true;
        }

        @Override
        public boolean beforeExpression(Expression expression) {
            if (refused) return false;
            // a lambda's or an anonymous body's statements are not this method's; a candidate captured by one
            // still counts as an occurrence that is not a receiver position, which is what refuses it
            if (expression instanceof Lambda) return false;
            if (expression instanceof ConstructorCall cc && cc.anonymousClass() != null) return false;
            if (expression instanceof MethodCall mc && mc.object() instanceof VariableExpression ve
                && ve.variable() instanceof LocalVariable) {
                allowed.add(mc.object());
            } else if (expression instanceof Assignment a) {
                Variable target = a.variableTarget();
                if (target instanceof LocalVariable lv) {
                    // re-initialisation keeps the candidate only when the new value constructs the same type
                    TypeInfo constructed = constructs(a.value());
                    TypeInfo candidate = candidates.get(lv);
                    if (candidate != null && candidate.equals(constructed)) allowed.add(a.target());
                    else if (candidate != null) disqualified.add(lv);
                } else {
                    // 't.f = v', 't.f.g = v': the scope of the written field is a receiver position
                    while (target instanceof FieldReference fr) {
                        if (fr.scopeVariable() instanceof LocalVariable && fr.scope() != null) {
                            allowed.add(fr.scope());
                        }
                        target = fr.scopeVariable();
                    }
                }
            } else if (expression instanceof VariableExpression ve && ve.variable() instanceof LocalVariable lv
                       && candidates.containsKey(lv)) {
                occurrences.merge(lv, 1, Integer::sum);
                if (allowed.contains(expression)) allowedCount.merge(lv, 1, Integer::sum);
            }
            return true;
        }

        /** The class this expression constructs, directly or through a delegating call; null otherwise. */
        private TypeInfo constructs(Expression e) {
            if (e instanceof Cast cast) return constructs(cast.expression());
            if (e instanceof ConstructorCall cc) {
                if (cc.anonymousClass() != null || cc.arrayInitializer() != null || cc.constructor() == null
                    || cc.parameterizedType().arrays() > 0) {
                    return null;
                }
                return cc.parameterizedType().typeInfo();
            }
            if (e instanceof MethodCall mc) return of(mc.methodInfo(), visiting);
            return null;
        }

        TypeInfo verdict() {
            if (refused || returns.isEmpty()) return null;
            TypeInfo constructed = null;
            for (TypeInfo t : returns) {
                if (t == null) return null;
                if (constructed == null) constructed = t;
                else if (!constructed.equals(t)) return null;
            }
            for (Map.Entry<Variable, TypeInfo> c : candidates.entrySet()) {
                if (!c.getValue().equals(constructed)) continue;    // never returned: irrelevant
                if (disqualified.contains(c.getKey())) return null;
                if (!occurrences.getOrDefault(c.getKey(), 0).equals(allowedCount.getOrDefault(c.getKey(), 0))) {
                    return null;    // published: an argument, a stored value, a captured variable
                }
            }
            return constructed;
        }
    }

}
