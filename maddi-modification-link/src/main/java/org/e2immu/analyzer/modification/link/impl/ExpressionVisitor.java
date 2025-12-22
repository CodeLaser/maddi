package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.*;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;

public record ExpressionVisitor(JavaInspector javaInspector,
                                VirtualFieldComputer virtualFieldComputer,
                                LinkComputerRecursion linkComputer,
                                LinkComputerImpl.SourceMethodComputer sourceMethodComputer,
                                MethodInfo currentMethod,
                                RecursionPrevention recursionPrevention,
                                AtomicInteger variableCounter) {

    public record WriteMethodCall(Expression methodCall, Links linksFromObject) {
    }

    /*
    primary = end result
    links = additional links for parts of the result
    extra = link information about unrelated variables
     */
    public record Result(Links links,
                         LinkedVariables extra,
                         Set<Variable> modified,
                         List<WriteMethodCall> writeMethodCalls,
                         Map<Variable, Set<ParameterizedType>> casts) {
        public Result(Links links, LinkedVariables extra) {
            this(links, extra, new HashSet<>(), new ArrayList<>(), new HashMap<>());
        }

        public Result addModified(Set<Variable> modified) {
            this.modified.addAll(modified);
            return this;
        }

        public Result add(WriteMethodCall writeMethodCall) {
            this.writeMethodCalls.add(writeMethodCall);
            return this;
        }

        public Result addCast(Variable variable, ParameterizedType pt) {
            this.casts.computeIfAbsent(variable, _ -> new HashSet<>()).add(pt);
            return this;
        }

        public Result with(Links links) {
            return new Result(links, extra, modified, writeMethodCalls, casts);
        }

        public Result merge(Result other) {
            LinkedVariables combinedExtra = extra.isEmpty() ? other.extra : extra.merge(other.extra);
            if(other.links != null && other.links.primary() != null) {
                combinedExtra = combinedExtra.merge(new LinkedVariablesImpl(Map.of(other.links.primary(), other.links)));
            }
            this.writeMethodCalls.addAll(other.writeMethodCalls);
            this.modified.addAll(other.modified);
            other.casts.forEach((v, set) ->
                    casts.computeIfAbsent(v, vv -> new HashSet<>()).addAll(set));
            return new Result(this.links, combinedExtra, this.modified, this.writeMethodCalls, this.casts);
        }

        public Result moveLinksToExtra() {
            if (links.primary() != null) {
                this.extra.merge(new LinkedVariablesImpl(Map.of(links.primary(), links)));
            }
            return new Result(LinksImpl.EMPTY, extra, modified, writeMethodCalls, casts);
        }

        public LinkedVariables extraAndLinks() {
            Map<Variable, Links> map = new HashMap<>(extra.map());
            map.merge(links.primary(), links, Links::merge);
            return new LinkedVariablesImpl(map);
        }
    }

    static final Result EMPTY = new Result(LinksImpl.EMPTY, LinkedVariablesImpl.EMPTY);

    public Result visit(Expression expression, VariableData variableData) {
        return switch (expression) {
            case VariableExpression ve -> variableExpression(ve, variableData);
            case Assignment a -> assignment(variableData, a);
            case MethodCall mc -> methodCall(variableData, mc);
            case MethodReference mr -> methodReference(variableData, mr);
            case ConstructorCall cc -> constructorCall(variableData, cc);
            case Lambda lambda -> lambda(lambda);
            case Cast cast -> cast(variableData, cast);
            case InstanceOf instanceOf -> instanceOf(variableData, instanceOf);

            case InlineConditional ic -> inlineConditional(ic, variableData);
            case ArrayInitializer ai -> ai.expressions().stream().map(e -> visit(e, variableData))
                    .reduce(EMPTY, Result::merge);
            case And and -> and.expressions().stream().map(e -> visit(e, variableData))
                    .reduce(EMPTY, Result::merge);
            case Or or -> or.expressions().stream().map(e -> visit(e, variableData))
                    .reduce(EMPTY, Result::merge);
            case CommaExpression ce -> ce.expressions().stream().map(e -> visit(e, variableData))
                    .reduce(EMPTY, Result::merge);
            case ArrayLength al -> visit(al.scope(), variableData).moveLinksToExtra();
            case EnclosedExpression ee -> visit(ee.inner(), variableData);
            case UnaryOperator uo -> visit(uo.expression(), variableData);
            case GreaterThanZero gt0 -> visit(gt0.expression(), variableData);
            case BinaryOperator bo -> visit(bo.lhs(), variableData).merge(visit(bo.rhs(), variableData));
            case ConstantExpression<?> _, TypeExpression _ -> EMPTY;
            default -> throw new UnsupportedOperationException("Implement: " + expression.getClass());
        };
    }

    /*
    simple passthrough, except
    1. narrowing casts on primitives return EMPTY
    2. variable casts are stored for the DOWNCAST info
     */
    private Result cast(VariableData variableData, Cast cast) {
        if (narrowingCast(cast.expression().parameterizedType(), cast.parameterizedType())) {
            return EMPTY;
        }
        Result r = visit(cast.expression(), variableData);
        if (cast.expression() instanceof VariableExpression ve) {
            return r.addCast(ve.variable(), cast.parameterizedType());
        }
        return r;
    }

    // long -> int etc.
    private boolean narrowingCast(ParameterizedType from, ParameterizedType to) {
        return from.isPrimitiveExcludingVoid()
               && to.isPrimitiveExcludingVoid()
               && !from.equals(to)
               && javaInspector.runtime().widestType(from, to).equals(from);
    }


    private Result instanceOf(VariableData variableData, InstanceOf instanceOf) {
        throw new UnsupportedOperationException();
    }

    private Result methodReference(VariableData variableData, MethodReference mr) {
        MethodLinkedVariables mlv = linkComputer.recurseMethod(mr.methodInfo());

        Result object = visit(mr.scope(), variableData);
        Links newRv;
        if (object.links.primary() != null) {
            This thisVar = javaInspector.runtime().newThis(mr.methodInfo().typeInfo().asParameterizedType());
            TranslationMap tm = javaInspector.runtime().newTranslationMapBuilder()
                    .put(thisVar, object.links.primary())
                    .build();
            newRv = mlv.ofReturnValue().translate(tm);
        } else {
            newRv = mlv.ofReturnValue();
        }
        int i = 0;
        Map<Variable, Links> map = new HashMap<>();
        for (Links paramLinks : mlv.ofParameters()) {
            map.put(mr.methodInfo().parameters().get(i), paramLinks);
            ++i;
        }
        if (object.links.primary() != null) {
            map.merge(object.links.primary(), object.links, Links::merge);
            object.extra.forEach(e -> map.merge(e.getKey(), e.getValue(), Links::merge));
        }
        return new Result(newRv, new LinkedVariablesImpl(map));
    }

    private @NotNull Result lambda(Lambda lambda) {
        MethodLinkedVariables mlv = linkComputer.recurseMethod(lambda.methodInfo());

        ParameterizedType concreteObjectType = lambda.concreteReturnType();
        VirtualFieldComputer.VfTm vfTm = virtualFieldComputer.compute(concreteObjectType, true);
        MethodLinkedVariables mlvTranslated = mlv.translate(vfTm.formalToConcrete());
        int i = 0;
        Map<Variable, Links> map = new HashMap<>();
        for (Links paramLinks : mlvTranslated.ofParameters()) {
            map.put(lambda.methodInfo().parameters().get(i), paramLinks);
            ++i;
        }
        return new Result(mlvTranslated.ofReturnValue(), new LinkedVariablesImpl(map));
    }

    private @NotNull Result inlineConditional(InlineConditional ic, VariableData variableData) {
        Result rc = visit(ic.condition(), variableData);
        Result rt = visit(ic.ifTrue(), variableData);
        Result rf = visit(ic.ifFalse(), variableData);
        Runtime runtime = javaInspector.runtime();
        Variable newPrimary = runtime.newLocalVariable("$__ic" + variableCounter.getAndIncrement(),
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
        if (ve.variable().parameterizedType().isFunctionalInterface()
            && variableData != null && variableData.isKnown(ve.variable().fullyQualifiedName())) {
            VariableInfo vi = variableData.variableInfo(ve.variable());
            Links links = Objects.requireNonNullElse(vi.linkedVariables(), LinksImpl.EMPTY);
            links.forEach(l -> builder.add(l.from(), l.linkNature(), l.to()));
        }
        return new Result(builder.build(), extra);
    }

    private Result assignment(VariableData variableData, Assignment a) {
        Links.Builder builder = new LinksImpl.Builder(a.variableTarget());
        Result rValue = visit(a.value(), variableData);
        Result rTarget = visit(a.target(), variableData);
        if (rValue.links != null && rValue.links.primary() != null) {
            builder.add(LinkNature.IS_IDENTICAL_TO, rValue.links.primary());
        }
        return new Result(builder.build(), LinkedVariablesImpl.EMPTY)
                .merge(rValue)
                .merge(rTarget)
                .addModified(Util.scopeVariables(a.variableTarget()));
    }

    private Result constructorCall(VariableData variableData, ConstructorCall cc) {
        Result object;
        if (cc.object() == null || cc.object().isEmpty()) {
            LocalVariable lv = javaInspector.runtime().newLocalVariable("$__c" + variableCounter.getAndIncrement(),
                    cc.parameterizedType());
            object = new Result(new LinksImpl.Builder(lv).build(), LinkedVariablesImpl.EMPTY);
        } else {
            object = visit(cc.object(), variableData);
        }
        MethodLinkedVariables mlv = recurseIntoLinkComputer(cc.constructor());

        VirtualFieldComputer.VfTm vfTm = virtualFieldComputer.compute(cc.parameterizedType(), true);
        MethodLinkedVariables mlvTranslated1 = mlv.translate(vfTm.formalToConcrete());

        MethodLinkedVariables mlvTranslated;
        if (!cc.parameterExpressions().isEmpty() && cc.constructor() != null) {
            TranslationMap.Builder builder = javaInspector.runtime().newTranslationMapBuilder();
            for (ParameterInfo pi : cc.constructor().parameters()) {
                if (!pi.isVarArgs() || cc.constructor().parameters().size() >= pi.index()) {
                    builder.addVariableExpression(pi, cc.parameterExpressions().get(pi.index()));
                }
            }
            mlvTranslated = mlvTranslated1.translate(builder.build());
        } else {
            mlvTranslated = mlvTranslated1;
        }

        List<Result> params = cc.parameterExpressions().stream().map(e -> visit(e, variableData)).toList();
        return new LinkMethodCall(javaInspector.runtime(), virtualFieldComputer, variableCounter, currentMethod)
                .constructorCall(cc.constructor(), object, params, mlvTranslated);
    }

    private Result methodCall(VariableData variableData, MethodCall mc) {
        Result object = mc.methodInfo().isStatic() ? EMPTY : visit(mc.object(), variableData);
        MethodLinkedVariables mlv = recurseIntoLinkComputer(mc.methodInfo());

        MethodLinkedVariables mlvTranslated2;
        Variable objectPrimary = object.links.primary();
        if (objectPrimary != null) {
            This thisVar = javaInspector.runtime().newThis(mc.methodInfo().typeInfo().asParameterizedType());
            MethodLinkedVariables mlvTranslated1;
            if (!thisVar.equals(objectPrimary)) {
                TranslationMap tm = javaInspector.runtime().newTranslationMapBuilder()
                        .put(thisVar, objectPrimary)
                        .build();
                mlvTranslated1 = mlv.translate(tm);
            } else {
                mlvTranslated1 = mlv;
            }
            if (!currentMethod.typeInfo().isEqualToOrInnerClassOf(mc.methodInfo().typeInfo())) {
                ParameterizedType concreteObjectType = objectPrimary.parameterizedType();
                VirtualFieldComputer.VfTm vfTm = virtualFieldComputer.compute(concreteObjectType, true);
                mlvTranslated2 = mlvTranslated1.translate(vfTm.formalToConcrete());
            } else {
                mlvTranslated2 = mlvTranslated1;
            }
        } else {
            // static method, without object
            mlvTranslated2 = mlv;
        }
        MethodLinkedVariables mlvTranslated;
        if (!mc.parameterExpressions().isEmpty()) {
            TranslationMap.Builder builder = javaInspector.runtime().newTranslationMapBuilder();
            for (ParameterInfo pi : mc.methodInfo().parameters()) {
                if (!pi.isVarArgs() || mc.methodInfo().parameters().size() >= pi.index()) {
                    builder.addVariableExpression(pi, mc.parameterExpressions().get(pi.index()));
                }
            }
            mlvTranslated = mlvTranslated2.translate(builder.build());
        } else {
            mlvTranslated = mlvTranslated2;
        }
        List<Result> params = mc.parameterExpressions().stream().map(e -> visit(e, variableData)).toList();
        Result r = new LinkMethodCall(javaInspector.runtime(), virtualFieldComputer, variableCounter, currentMethod)
                .methodCall(mc.methodInfo(), mc.concreteReturnType(), object, params, mlvTranslated);
        Set<Variable> modified = new HashSet<>();

        if (mc.methodInfo().isModifying() && objectPrimary != null) {
            modified.add(objectPrimary);
        }
        for (ParameterInfo pi : mc.methodInfo().parameters()) {
            if (pi.isModified() && params.size() > pi.index()) {
                Result rp = params.get(pi.index());
                if (rp.links != null && rp.links.primary() != null) {
                    modified.add(rp.links.primary());
                }
            }
        }
        return r.addModified(modified).add(new WriteMethodCall(mc, object.links));
    }

    private MethodLinkedVariables recurseIntoLinkComputer(MethodInfo methodInfo) {
        RecursionPrevention.How how = recursionPrevention.contains(methodInfo);
        return switch (how) {
            case GET -> methodInfo.analysis().getOrDefault(METHOD_LINKS, MethodLinkedVariablesImpl.EMPTY);
            case SOURCE -> linkComputer.recurseMethod(methodInfo);
            case SHALLOW -> linkComputer.doMethodShallowDoNotWrite(methodInfo);
            case LOCK -> methodInfo.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(methodInfo));
        };
    }

}
