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

package org.e2immu.analyzer.modification.prepwork.escape;

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;

import java.util.List;


public class ComputeAlwaysEscapes {

    /*
    How control can leave a statement (subtree):
     - fallsThrough: it can complete normally, reaching the point right after it;
     - breaksOut:    it can execute a 'break' that targets the nearest enclosing loop or switch.
    A statement "always escapes" (return/throw, or an infinite loop) exactly when it can do neither.

    These two facts are independent: '{ if(c) break; return x; }' never falls through yet can break out. A single
    ordinal lattice cannot represent that, which is why a 'break' followed by an unconditional escape used to hide
    the break from the enclosing loop.
     */
    private record Flow(boolean fallsThrough, boolean breaksOut) {
        static final Flow ESCAPES = new Flow(false, false);
        static final Flow FALLS_THROUGH = new Flow(true, false);
        static final Flow BREAKS = new Flow(false, true);

        boolean alwaysEscapes() {
            return !fallsThrough && !breaksOut;
        }

        Flow union(Flow other) {
            return new Flow(fallsThrough || other.fallsThrough, breaksOut || other.breaksOut);
        }
    }

    public static void go(MethodInfo methodInfo) {
        alwaysEscapes(methodInfo.methodBody());
    }

    public static void go(Block methodBody) {
        alwaysEscapes(methodBody);
    }

    private static void alwaysEscapes(Expression expression) {
        if (expression != null) {
            expression.visit(e -> {
                if (e instanceof Lambda lambda) {
                    alwaysEscapes(lambda.methodBody());
                } else if (e instanceof ConstructorCall cc && cc.anonymousClass() != null) {
                    cc.anonymousClass().methodStream().forEach(ComputeAlwaysEscapes::go);
                }
                return true;
            });
        }
    }

    private static Flow alwaysEscapes(Statement statement) {
        if (statement instanceof EmptyStatement) return Flow.FALLS_THROUGH;

        alwaysEscapes(statement.expression());
        Flow flow;
        if (statement instanceof Block block) {
            flow = sequence(block.statements());
        } else if (statement instanceof ThrowStatement || statement instanceof ReturnStatement) {
            flow = Flow.ESCAPES;
        } else if (statement instanceof BreakStatement) {
            flow = Flow.BREAKS;
        } else if (statement instanceof IfElseStatement ifElse) {
            // the if takes either branch, so its outcomes are the union of the two branches' outcomes
            flow = alwaysEscapes(ifElse.block()).union(alwaysEscapes(ifElse.elseBlock()));
        } else if (statement instanceof LoopStatement loop) {
            Flow body = alwaysEscapes(loop.block());
            // a 'break' in the body leaves THIS loop, so the body's breaks are consumed here. An infinite loop
            // ('while(true)', 'for(;;)') completes normally only via such a break; any other loop can also complete
            // by its condition turning false.
            boolean infinite = loop.expression().isBoolValueTrue();
            flow = new Flow(infinite ? body.breaksOut() : true, false);
        } else if (statement instanceof TryStatement ts) {
            flow = tryFlow(ts);
        } else if (statement instanceof SwitchStatementNewStyle newStyle) {
            flow = switchNewStyle(newStyle);
        } else if (statement instanceof SwitchStatementOldStyle oldStyle) {
            flow = switchOldStyle(oldStyle);
        } else if (statement instanceof SynchronizedStatement sync) {
            flow = alwaysEscapes(sync.block());
        } else {
            if (statement instanceof ExplicitConstructorInvocation eci) {
                eci.parameterExpressions().forEach(ComputeAlwaysEscapes::alwaysEscapes);
            } else if (statement instanceof LocalVariableCreation lvc) {
                lvc.localVariableStream().forEach(lv -> alwaysEscapes(lv.assignmentExpression()));
            }
            // includes 'continue': it does not escape the method and does not break out of a loop
            flow = Flow.FALLS_THROUGH;
        }

        if (flow.alwaysEscapes() && !statement.analysis().haveAnalyzedValueFor(PropertyImpl.ALWAYS_ESCAPES)) {
            statement.analysis().set(PropertyImpl.ALWAYS_ESCAPES, ValueImpl.BoolImpl.TRUE);
        }
        return flow;
    }

    // statements run in order; a statement is reached only if all before it fall through. The block falls through iff
    // the last reached statement does, and can break out if any reached statement can. (All statements are still
    // visited, to set the flag on nested statements, even where source were to contain unreachable code.)
    private static Flow sequence(List<Statement> statements) {
        boolean fallsThrough = true;
        boolean breaksOut = false;
        for (Statement s : statements) {
            Flow f = alwaysEscapes(s);
            if (fallsThrough) {
                breaksOut = breaksOut || f.breaksOut();
                fallsThrough = f.fallsThrough();
            }
        }
        return new Flow(fallsThrough, breaksOut);
    }

    private static Flow tryFlow(TryStatement ts) {
        Flow main = alwaysEscapes(ts.block());
        // the try completes normally if the main block or any catch block does; it escapes only if all of them do
        Flow mainAndCatch = ts.catchClauses().stream()
                .map(cc -> alwaysEscapes(cc.block()))
                .reduce(main, Flow::union);
        Flow fin = alwaysEscapes(ts.finallyBlock());
        if (!fin.fallsThrough()) {
            // the finally always runs; if it never falls through, it determines the try's outcome outright
            return fin;
        }
        // the finally completes normally: the try's outcome is main/catch, plus any break the finally itself adds
        return new Flow(mainAndCatch.fallsThrough(), mainAndCatch.breaksOut() || fin.breaksOut());
    }

    private static Flow switchNewStyle(SwitchStatementNewStyle newStyle) {
        // without a 'default' an unmatched selector falls straight through (we do not prove enum/sealed exhaustiveness)
        boolean canFallThrough = !hasDefault(newStyle.entries().stream().flatMap(e -> e.conditions().stream()).toList());
        for (SwitchEntry entry : newStyle.entries()) {
            Flow f = alwaysEscapes(entry.statement());
            // a 'break' inside an arm leaves the switch (it completes normally), as does an arm that falls through
            if (f.fallsThrough() || f.breaksOut()) canFallThrough = true;
        }
        return new Flow(canFallThrough, false); // a switch consumes the breaks of its arms
    }

    private static Flow switchOldStyle(SwitchStatementOldStyle oldStyle) {
        boolean hasDefault = oldStyle.switchLabels().stream().anyMatch(l -> l.literal().isEmpty());
        Flow body = alwaysEscapes(oldStyle.block());
        // a 'break' inside the switch leaves the switch (it completes normally); the body falling off its end, or no
        // 'default' for an unmatched selector, also lets the switch complete
        boolean canFallThrough = !hasDefault || body.fallsThrough() || body.breaksOut();
        return new Flow(canFallThrough, false); // a switch consumes the breaks of its cases
    }

    private static boolean hasDefault(List<Expression> conditions) {
        return conditions.stream().anyMatch(Expression::isEmpty);
    }
}
