package org.e2immu.analyzer.modification.prepwork.escape;

import org.e2immu.language.cst.api.expression.ConstructorCall;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.statement.*;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;


public class ComputeAlwaysEscapes {

    private enum Escape {
        ALWAYS, BREAK, MAYBE, NO;

        Escape and(Escape other) {
            if (this == ALWAYS && other == ALWAYS) {
                return ALWAYS;
            }
            if (this == BREAK || other == BREAK) {
                return BREAK;
            }
            if (this == MAYBE || other == MAYBE) {
                return MAYBE;
            }
            return NO;
        }

        Escape or(Escape other) {
            if (this == ALWAYS || other == ALWAYS) return ALWAYS;
            if (this == BREAK || other == BREAK) return BREAK;
            if (this == MAYBE || other == MAYBE) return MAYBE;
            return NO;
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

    private static Escape alwaysEscapes(Statement statement) {
        if (statement instanceof EmptyStatement) return Escape.NO;

        alwaysEscapes(statement.expression());
        Escape escape;
        if (statement instanceof Block block) {
            if (block.isEmpty()) {
                escape = Escape.NO;
            } else {
                escape = block.statements().stream()
                        .map(ComputeAlwaysEscapes::alwaysEscapes)
                        .reduce(Escape.NO, Escape::or);
            }
        } else if (statement instanceof ThrowStatement || statement instanceof ReturnStatement) {
            escape = Escape.ALWAYS;
        } else if (statement instanceof BreakStatement) {
            escape = Escape.BREAK;
        } else if (statement instanceof IfElseStatement ifElse) {
            Escape e1 = alwaysEscapes(ifElse.block());
            Escape e2 = alwaysEscapes(ifElse.elseBlock());
            escape = e1.and(e2);
        } else if (statement instanceof LoopStatement loop) {
            Escape e1 = alwaysEscapes(loop.block());
            if (loop.expression().isBoolValueTrue() && (e1 == Escape.ALWAYS || e1 == Escape.NO)) {
                escape = Escape.ALWAYS;
            } else {
                escape = Escape.NO;
            }
        } else if (statement instanceof TryStatement ts) {
            Escape eMain = alwaysEscapes(ts.block());
            Escape eCatchClauses = ts.catchClauses().stream().map(cc -> alwaysEscapes(cc.block())).reduce(Escape.ALWAYS, Escape::and);
            Escape eFinally = alwaysEscapes(ts.finallyBlock());
            if (eFinally == Escape.ALWAYS) {
                escape = Escape.ALWAYS;
            } else {
                Escape mainAndCatch = eMain.and(eCatchClauses);
                if (mainAndCatch == Escape.ALWAYS) {
                    escape = Escape.ALWAYS;
                } else {
                    escape = mainAndCatch.and(eFinally);
                }
            }
        } else if (statement instanceof SwitchStatementNewStyle newStyle) {
            escape = newStyle.entries().stream().map(e -> alwaysEscapes(e.statement())).reduce(Escape.ALWAYS, Escape::and);
        } else if (statement instanceof SwitchStatementOldStyle oldStyle) {
            escape = alwaysEscapes(oldStyle.block()); // FIXME
        } else if (statement instanceof SynchronizedStatement sync) {
            escape = alwaysEscapes(sync.block());
        } else {
            if (statement instanceof ExplicitConstructorInvocation eci) {
                eci.parameterExpressions().forEach(ComputeAlwaysEscapes::alwaysEscapes);
            } else if (statement instanceof LocalVariableCreation lvc) {
                lvc.localVariableStream().forEach(lv -> alwaysEscapes(lv.assignmentExpression()));
            }
            escape = Escape.NO;
        }

        if (escape == Escape.ALWAYS && !(statement.analysis().haveAnalyzedValueFor(PropertyImpl.ALWAYS_ESCAPES))) {
            statement.analysis().set(PropertyImpl.ALWAYS_ESCAPES, ValueImpl.BoolImpl.TRUE);
        }
        return escape;
    }

}
