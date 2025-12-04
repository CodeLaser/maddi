package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.*;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.integration.JavaInspector;

import java.util.ArrayList;
import java.util.List;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;

public record ExpressionVisitor(JavaInspector javaInspector,
                                LinkComputerRecursion linkComputer,
                                LinkComputerImpl.SourceMethodComputer sourceMethodComputer,
                                MethodInfo currentMethod,
                                RecursionPrevention recursionPrevention) {

    /*
    primary = end result
    links = additional links for parts of the result
    extra = link information about unrelated variables
     */
    public record Result(Links links, LinkedVariables extra) {

        public Result with(Links links) {
            return new Result(links, extra);
        }

        public Result merge(Result other) {
            if (other.links.isEmpty() && other.extra.isEmpty()) return this;
            LinkedVariables combinedExtra = extra.isEmpty() ? other.extra : extra.merge(other.extra);
            return new Result(links, combinedExtra);
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
                if (ve.variable().parameterizedType().isPrimitiveStringClass()) yield EMPTY;
                List<Link> links = new ArrayList<>();
                Variable v = ve.variable();
                while (v instanceof DependentVariable dv) {
                    v = dv.arrayVariable();
                    if (v != null) {
                        links.add(new LinkImpl(ve.variable(), LinkNature.IS_ELEMENT_OF, v));
                    }
                }
                LinkedVariables extra;
                if (v instanceof FieldReference fr) {
                    Result r = visit(fr.scope(), variableData);
                    extra = r.extra;
                } else {
                    extra = LinkedVariablesImpl.EMPTY;
                }
                Link link = new LinkImpl(null, LinkNature.IS_IDENTICAL_TO, ve.variable());
                links.addFirst(link);
                yield new Result(new LinksImpl(List.copyOf(links)), extra);
            }
            case MethodCall mc -> methodCall(variableData, mc);
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

    private Result methodCall(VariableData variableData, MethodCall mc) {
        Result object = mc.methodInfo().isStatic() ? EMPTY : visit(mc.object(), variableData);
        MethodLinkedVariables mlv = recurseIntoTypeLinkComputer(mc.methodInfo());
        List<Result> params = mc.parameterExpressions().stream().map(e -> visit(e, variableData)).toList();
        return new LinkMethodCall(javaInspector.runtime()).methodCall(mc, object, params, mlv);
    }

    private MethodLinkedVariables recurseIntoTypeLinkComputer(MethodInfo methodInfo) {
        RecursionPrevention.How how = recursionPrevention.contains(methodInfo);
        return switch (how) {
            case GET -> methodInfo.analysis().getOrDefault(METHOD_LINKS, MethodLinkedVariablesImpl.EMPTY);
            case SOURCE -> linkComputer.recurseMethod(methodInfo);
            case SHALLOW -> linkComputer.doMethodShallowDoNotWrite(methodInfo);
            case LOCK -> methodInfo.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(methodInfo));
        };
    }

}
