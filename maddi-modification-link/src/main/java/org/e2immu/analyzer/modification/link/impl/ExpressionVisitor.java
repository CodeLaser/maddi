package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.LinkedVariables;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.VariableData;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;

public record ExpressionVisitor(JavaInspector javaInspector,
                                LinkComputerRecursion linkComputer,
                                LinkComputerImpl.SourceMethodComputer sourceMethodComputer,
                                MethodInfo currentMethod,
                                RecursionPrevention recursionPrevention,
                                AtomicInteger variableCounter) {

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
            case VariableExpression ve -> variableExpression(ve, variableData);
            case Assignment a -> assignment(variableData, a);
            case MethodCall mc -> methodCall(variableData, mc);
            // all rather uninteresting....

            case InlineConditional ic -> inlineConditional(ic, variableData);
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
            case ConstantExpression<?> _ -> EMPTY;
            default -> throw new UnsupportedOperationException("Implement: " + expression.getClass());
        };
    }

    private @NotNull Result inlineConditional(InlineConditional ic, VariableData variableData) {
        Result rc = visit(ic.condition(), variableData);
        Result rt = visit(ic.ifTrue(), variableData);
        Result rf = visit(ic.ifFalse(), variableData);
        Runtime runtime = javaInspector.runtime();
        Variable newPrimary = runtime.newLocalVariable("ic" + variableCounter.getAndIncrement(),
                ic.parameterizedType());
        // we must link the new primary to both rt and rf links
        AssignLinksToLocal atl = new AssignLinksToLocal(runtime);
        Links rtChanged = atl.go(newPrimary, rt.links);
        Links rfChanged = atl.go(newPrimary, rf.links);
        Links newLinks = rtChanged.merge(rfChanged);
        Result merge = rc.merge(rt).merge(rf);
        return new Result(newLinks, merge.extra);
    }

    private @NotNull Result variableExpression(VariableExpression ve, VariableData variableData) {
        if (ve.variable().parameterizedType().isPrimitiveStringClass()) return EMPTY;
        Variable v = ve.variable();
        LinkedVariables extra = LinkedVariablesImpl.EMPTY;
        while (v instanceof DependentVariable dv) {
            v = dv.arrayVariable();
            if (v != null) {
                Links vLinks = new LinksImpl.Builder(dv).add(LinkNature.IS_ELEMENT_OF, v).build();
                extra = extra.merge(new LinkedVariablesImpl(Map.of(dv, vLinks)));
            }
        }

        if (v instanceof FieldReference fr) {
            Result r = visit(fr.scope(), variableData);
            extra = extra.merge(r.extra);
        }
        Links.Builder builder = new LinksImpl.Builder(ve.variable());
        // Link link = new LinkImpl(null, LinkNature.IS_IDENTICAL_TO, ve.variable());
        // links.addFirst(link);
        return new Result(builder.build(), extra);
    }

    private Result assignment(VariableData variableData, Assignment a) {
        Links.Builder builder = new LinksImpl.Builder(a.variableTarget());
        Result rValue = visit(a.value(), variableData);
        Result rTarget = visit(a.target(), variableData);
        builder.add(LinkNature.IS_IDENTICAL_TO, rValue.links.primary());
        return new Result(builder.build(), rValue.extra.merge(rTarget.extra));
    }

    private Result methodCall(VariableData variableData, MethodCall mc) {
        Result object = mc.methodInfo().isStatic() ? EMPTY : visit(mc.object(), variableData);
        MethodLinkedVariables mlv = recurseIntoTypeLinkComputer(mc.methodInfo());
        List<Result> params = mc.parameterExpressions().stream().map(e -> visit(e, variableData)).toList();
        return new LinkMethodCall(javaInspector.runtime(), variableCounter).methodCall(mc, object, params, mlv);
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
