package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.LinkComputer;
import org.e2immu.analyzer.modification.link.LinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.inspection.api.integration.JavaInspector;

public record ExpressionVisitor(JavaInspector javaInspector,
                                LinkComputer linkComputer,
                                LinkComputerImpl.SourceMethodComputer sourceMethodComputer,
                                MethodInfo currentMethod,
                                RecursionPrevention recursionPrevention) {

    public record Result(Links link, LinkedVariables extra) {

        public Result with(Links link) {
            return new Result(link, extra);
        }

        public Result merge(Result other) {
            if (other.extra.isEmpty()) return this;
            LinkedVariables combinedExtra = extra.isEmpty() ? other.extra : extra.merge(other.extra);
            return new Result(link, combinedExtra);
        }

        // keep other.link
        public Result mergeLv(Result other) {
            throw new UnsupportedOperationException("NYI");
        }
    }

    static final Result EMPTY = new Result(LinksImpl.EMPTY, LinkedVariablesImpl.EMPTY);

    public Result visit(Expression expression, VariableData variableData) {
        return switch (expression) {
            case VariableExpression ve -> {
                throw new UnsupportedOperationException("NYI");
            }

            // all rather uninteresting....

            case InlineConditional ic -> {
                Result rc = visit(ic.condition(), variableData);
                Result rt = visit(ic.ifTrue(), variableData);
                Result rf = visit(ic.ifFalse(), variableData);
                yield rt.mergeLv(rf).merge(rc);
            }
            case ArrayInitializer ai -> ai.expressions().stream().map(e -> visit(e, variableData))
                    .reduce(EMPTY, Result::mergeLv);
            case And and -> and.expressions().stream().map(e -> visit(e, variableData))
                    .reduce(EMPTY, Result::merge);
            case Or or -> or.expressions().stream().map(e -> visit(e, variableData))
                    .reduce(EMPTY, Result::merge);
            case CommaExpression ce -> ce.expressions().stream().map(e -> visit(e, variableData))
                    .reduce(EMPTY, Result::merge);
            case ArrayLength al -> visit(al.scope(), variableData).with(LinksImpl.EMPTY);
            case EnclosedExpression ee -> visit(ee.inner(), variableData);
            case UnaryOperator uo -> visit(uo.expression(), variableData);
            case GreaterThanZero gt0 -> visit(gt0.expression(), variableData);
            case BinaryOperator bo -> visit(bo.lhs(), variableData).merge(visit(bo.rhs(), variableData));
            default -> throw new UnsupportedOperationException("Implement: " + expression.getClass());
        };
    }
}
