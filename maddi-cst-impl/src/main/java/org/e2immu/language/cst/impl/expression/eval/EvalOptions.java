package org.e2immu.language.cst.impl.expression.eval;

public record EvalOptions(int maxAndOrComplexity) {
    public static EvalOptions DEFAULT = new EvalOptions(500);
}
