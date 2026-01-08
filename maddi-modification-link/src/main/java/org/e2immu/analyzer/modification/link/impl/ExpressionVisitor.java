package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.impl.localvar.FunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.IntermediateVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.RecordPattern;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.*;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.MODIFIED_COMPONENTS_METHOD;
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.MODIFIED_COMPONENTS_PARAMETER;

public record ExpressionVisitor(Runtime runtime,
                                JavaInspector javaInspector,
                                VirtualFieldComputer virtualFieldComputer,
                                LinkComputerRecursion linkComputer,
                                LinkComputerImpl.SourceMethodComputer sourceMethodComputer,
                                MethodInfo currentMethod,
                                RecursionPrevention recursionPrevention,
                                AtomicInteger variableCounter) {

    public record WriteMethodCall(Expression methodCall, Links linksFromObject) {
    }

    static final Result EMPTY = new Result(LinksImpl.EMPTY, LinkedVariablesImpl.EMPTY, Set.of(), List.of(), Map.of(),
            Set.of(), Set.of());

    public Result visit(Expression expression, VariableData variableData, Stage stage) {
        Result r = switch (expression) {
            case VariableExpression ve -> variableExpression(ve, variableData, stage);
            case Assignment a -> assignment(variableData, stage, a);
            case MethodCall mc -> methodCall(variableData, stage, mc);
            case MethodReference mr -> methodReference(variableData, stage, mr);
            case ConstructorCall cc -> {
                if (cc.anonymousClass() != null) {
                    yield anonymousClassAsFunctionalInterface(variableData, stage, cc);
                }
                if (cc.arrayInitializer() != null) {
                    yield arrayInitializer(variableData, stage, cc);
                }
                yield constructorCall(variableData, stage, cc);
            }
            case Lambda lambda -> lambda(lambda);
            case Cast cast -> cast(variableData, stage, cast);
            case InstanceOf instanceOf -> instanceOf(variableData, stage, instanceOf);
            case ConstantExpression<?> ce -> constantExpression(ce);
            case InlineConditional ic -> inlineConditional(ic, variableData, stage);
            case ArrayInitializer ai -> ai.expressions().stream().map(e -> visit(e, variableData, stage))
                    .reduce(EMPTY, Result::merge);
            case And and -> and.expressions().stream().map(e -> visit(e, variableData, stage))
                    .reduce(EMPTY, Result::merge);
            case Or or -> or.expressions().stream().map(e -> visit(e, variableData, stage))
                    .reduce(EMPTY, Result::merge);
            case CommaExpression ce -> ce.expressions().stream().map(e -> visit(e, variableData, stage))
                    .reduce(EMPTY, Result::merge);
            case ArrayLength al -> visit(al.scope(), variableData, stage).moveLinksToExtra();
            case EnclosedExpression ee -> visit(ee.inner(), variableData, stage);
            case UnaryOperator uo -> unaryOperator(variableData, stage, uo);
            case GreaterThanZero gt0 -> greaterThanZero(variableData, stage, gt0);
            case BinaryOperator bo -> binaryOperator(variableData, stage, bo);
            case TypeExpression _, EmptyExpression _ -> EMPTY;
            default -> throw new UnsupportedOperationException("Implement: " + expression.getClass());
        };
        if (r.getEvaluated() == null) r.setEvaluated(expression);
        return r;
    }

    private @NotNull Result greaterThanZero(VariableData variableData, Stage stage, GreaterThanZero gt0) {
        Result r = visit(gt0.expression(), variableData, stage);
        GreaterThanZero newGt0 = runtime.newGreaterThanZero(r.getEvaluated(), gt0.allowEquals());
        return r.setEvaluated(newGt0);
    }

    private @NotNull Result unaryOperator(VariableData variableData, Stage stage, UnaryOperator uo) {
        Result result = visit(uo.expression(), variableData, stage);
        UnaryOperator newUnary = runtime.newUnaryOperator(uo.comments(), uo.source(), uo.operator(),
                result.getEvaluated(), uo.precedence());
        return result.with(LinksImpl.EMPTY).setEvaluated(newUnary);
    }

    private Result binaryOperator(VariableData variableData, Stage stage, BinaryOperator bo) {
        Result rLhs = visit(bo.lhs(), variableData, stage);
        Result rRhs = visit(bo.rhs(), variableData, stage);
        BinaryOperator evaluated = runtime.newBinaryOperatorBuilder()
                .setLhs(rLhs.getEvaluated())
                .setOperator(bo.operator()).setRhs(rRhs.getEvaluated())
                .setParameterizedType(bo.parameterizedType()).setPrecedence(bo.precedence())
                .setSource(bo.source())
                .build();
        return rLhs.merge(rRhs)
                .with(LinksImpl.EMPTY)
                .setEvaluated(evaluated);
    }

    Result arrayInitializer(VariableData variableData, Stage stage, ConstructorCall cc) {
        List<Result> rs = cc.arrayInitializer().expressions().stream()
                .map(e -> visit(e, variableData, stage)).toList();
        // make new object of the correct array type; this will become the primary
        // then assignments per index
        int i = 0;
        LocalVariable c = IntermediateVariable.newObject(runtime, variableCounter.getAndIncrement(),
                cc.parameterizedType());
        VariableExpression cVe = runtime.newVariableExpression(c);
        LinksImpl.Builder builder = new LinksImpl.Builder(c);
        Result combined = null;
        for (Result r : rs) {
            if (combined == null) {
                combined = r;
            } else {
                combined = combined.merge(r);
            }
            if (r.links().primary() != null) {
                DependentVariable dv = runtime.newDependentVariable(cVe, runtime.newInt(i));
                builder.add(dv, LinkNatureImpl.IS_ASSIGNED_FROM, r.links().primary());
            }
            ++i;
        }
        Links links = builder.build();
        if (rs.isEmpty()) return new Result(links, LinkedVariablesImpl.EMPTY);
        return combined.with(links);
    }

    Result constantExpression(ConstantExpression<?> ce) {
        LocalVariable lv = MarkerVariable.constant(variableCounter.getAndIncrement(), ce.parameterizedType(), ce);
        return new Result(new LinksImpl.Builder(lv).build(), LinkedVariablesImpl.EMPTY)
                .addVariableRepresentingConstant(lv).setEvaluated(ce);
    }

    /*
    simple passthrough, except
    1. narrowing casts on primitives return ERASE (as for an operation on a primitive)
    2. variable casts are stored for the DOWNCAST info
     */
    private Result cast(VariableData variableData, Stage stage, Cast cast) {
        Result r = visit(cast.expression(), variableData, stage);
        if (narrowingCast(cast.expression().parameterizedType(), cast.parameterizedType())) {
            return r.with(LinksImpl.EMPTY);
        }
        if (cast.expression() instanceof VariableExpression ve && cast.parameterizedType().typeInfo() != null) {
            return r.addCast(ve.variable(), cast.parameterizedType().typeInfo());
        }
        return r;
    }

    // long -> int etc.
    private boolean narrowingCast(ParameterizedType from, ParameterizedType to) {
        return from.isPrimitiveExcludingVoid()
               && to.isPrimitiveExcludingVoid()
               && !from.equals(to)
               && runtime.widestType(from, to).equals(from);
    }


    private Result instanceOf(VariableData variableData, Stage stage, InstanceOf instanceOf) {
        if (instanceOf.patternVariable() != null) {
            Result r = visit(instanceOf.expression(), variableData, stage);
            if (r.getEvaluated() instanceof VariableExpression ve) {
                if (instanceOf.testType().typeInfo() != null) {
                    r.addCast(ve.variable(), instanceOf.testType().typeInfo());
                }
                Links.Builder linksBuilder = new LinksImpl.Builder(ve.variable());
                // a instanceof B b -> a ≡ b
                if (instanceOf.patternVariable().localVariable() != null) {
                    LocalVariable lv = instanceOf.patternVariable().localVariable();
                    if (!lv.isUnnamed()) {
                        linksBuilder.add(LinkNatureImpl.IS_ASSIGNED_TO, lv);
                        return r.moveLinksToExtra().with(linksBuilder.build());
                    }
                } else if (instanceOf.patternVariable().recordType() != null) {
                    // a instanceof Point(Coord x, Coord y) -> x is field of a, y is field of a
                    recursivelyAddToBuilder(linksBuilder, instanceOf.patternVariable());
                    return r.moveLinksToExtra().with(linksBuilder.build());
                }
            }
            // no need to set expression, it will be lost
            return r.moveLinksToExtra(); // result is a boolean
        }
        return EMPTY;
    }

    private void recursivelyAddToBuilder(Links.Builder linksBuilder, RecordPattern recordPattern) {
        if (recordPattern.patterns() != null) {
            for (RecordPattern rp : recordPattern.patterns()) {
                recursivelyAddToBuilder(linksBuilder, rp);
            }
        }
        if (recordPattern.localVariable() != null && !recordPattern.localVariable().isUnnamed()) {
            linksBuilder.add(LinkNatureImpl.CONTAINS_AS_FIELD, recordPattern.localVariable());
        }
    }

    private Result methodReference(VariableData variableData, Stage stage, MethodReference mr) {
        MethodLinkedVariables mlv = linkComputer.recurseMethod(mr.methodInfo());

        Result object = visit(mr.scope(), variableData, stage);
        MethodLinkedVariables tMlv;
        if (object.links().primary() != null) {
            This thisVar = runtime.newThis(mr.methodInfo().typeInfo().asParameterizedType());
            TranslationMap tm = new VariableTranslationMap(runtime).put(thisVar, object.links().primary());
            tMlv = mlv.translate(tm);
        } else {
            tMlv = mlv;
        }
        Links newRv = tMlv.ofReturnValue();
        int i = 0;
        Map<Variable, Links> map = new HashMap<>();
        for (Links paramLinks : tMlv.ofParameters()) {
            map.put(mr.methodInfo().parameters().get(i), paramLinks);
            ++i;
        }
        if (object.links().primary() != null) {
            map.merge(object.links().primary(), object.links(), Links::merge);
            object.extra().forEach(e -> map.merge(e.getKey(), e.getValue(), Links::merge));
        }
        Result wrapped = new Result(newRv, new LinkedVariablesImpl(map));
        FunctionalInterfaceVariable fiv = new FunctionalInterfaceVariable(
                runtime,
                variableCounter.getAndIncrement(),
                mr.parameterizedType(),
                wrapped);
        Links links = new LinksImpl.Builder(fiv).build();
        return new Result(links, LinkedVariablesImpl.EMPTY);
    }

    private @NotNull Result inlineConditional(InlineConditional ic, VariableData variableData, Stage stage) {
        Result rc = visit(ic.condition(), variableData, stage);
        Result rt = visit(ic.ifTrue(), variableData, stage);
        Result rf = visit(ic.ifFalse(), variableData, stage);
        Variable newPrimary = IntermediateVariable.inlineCondition(runtime, variableCounter.getAndIncrement(),
                ic.parameterizedType());
        // we must link the new primary to both rt and rf links
        AssignLinksToLocal atl = new AssignLinksToLocal(runtime);
        Links rtChanged = atl.go(newPrimary, rt.links());
        Links rfChanged = atl.go(newPrimary, rf.links());
        Links newLinks = rtChanged.merge(rfChanged);
        Result merge = rc.merge(rt).merge(rf);
        return new Result(newLinks, merge.extra());
    }

    private @NotNull Result variableExpression(VariableExpression ve, VariableData variableData, Stage stage) {
        Variable v = ve.variable();
        LinkedVariables extra = LinkedVariablesImpl.EMPTY;
        if (v instanceof DependentVariable dv) {
            Result rIndex = visit(dv.indexExpression(), variableData, stage).copyLinksToExtra();
            Result rArray = visit(dv.arrayExpression(), variableData, stage);
            DependentVariable newDv;
            if (rIndex.getEvaluated() != dv.indexExpression() || rArray.getEvaluated() != dv.arrayExpression()) {
                newDv = runtime.newDependentVariable(rArray.getEvaluated(), rIndex.getEvaluated());
            } else {
                newDv = dv;
            }
            extra = extra.merge(rIndex.extra());
            v = ((VariableExpression) rArray.getEvaluated()).variable();
            if (v != null) {
                Links vLinks = new LinksImpl.Builder(newDv).add(LinkNatureImpl.IS_ELEMENT_OF, v).build();
                extra = extra.merge(new LinkedVariablesImpl(Map.of(newDv, vLinks)));
            }
            Links.Builder builder = new LinksImpl.Builder(ve.variable());
            Result res = new Result(builder.build(), extra.merge(rArray.extra()));
            VariableExpression newVe = dv == newDv ? ve : runtime.newVariableExpression(newDv);
            return res.setEvaluated(newVe);
        }
        VariableExpression newVE;
        if (v instanceof FieldReference fr) {
            Result r = visit(fr.scope(), variableData, stage);
            extra = extra.merge(r.extra());
            if (r.getEvaluated() != fr.scope()) {
                newVE = runtime.newVariableExpression(runtime.newFieldReference(fr.fieldInfo(), r.getEvaluated(),
                        fr.parameterizedType()));
            } else {
                newVE = ve;
            }
        } else {
            newVE = ve;
        }

        Links.Builder builder = new LinksImpl.Builder(newVE.variable());
        if (newVE.variable().parameterizedType().isFunctionalInterface()
            && variableData != null && variableData.isKnown(newVE.variable().fullyQualifiedName())) {
            VariableInfo vi = variableData.variableInfo(newVE.variable());
            Links links = Objects.requireNonNullElse(vi.linkedVariables(), LinksImpl.EMPTY);
            links.forEach(l -> builder.add(l.from(), l.linkNature(), l.to()));
        }
        return new Result(builder.build(), extra).setEvaluated(newVE);
    }

    private Result assignment(VariableData variableData, Stage stage, Assignment a) {
        Result rValue = visit(a.value(), variableData, stage);
        Result rTarget = visit(a.target(), variableData, stage);
        Links.Builder builder = new LinksImpl.Builder(((VariableExpression) rTarget.getEvaluated()).variable());
        if (rValue.links() != null && rValue.links().primary() != null) {
            builder.add(LinkNatureImpl.IS_ASSIGNED_FROM, rValue.links().primary());
        }
        Result result = new Result(builder.build(), LinkedVariablesImpl.EMPTY);
        if (a.assignmentOperator() != null || rValue.links() == null || rValue.links().primary() == null) {
            result.addErase(a.variableTarget());
        }
        return result
                .merge(rValue)
                .merge(rTarget)
                .addModified(Util.scopeVariables(a.variableTarget()))
                .setEvaluated(rValue.getEvaluated() != a.value() || rTarget.getEvaluated() != a.target()
                        ? runtime.newAssignment((VariableExpression) rTarget.getEvaluated(), rValue.getEvaluated()) : a);
    }

    private Result constructorCall(VariableData variableData, Stage stage, ConstructorCall cc) {
        assert cc.object() == null || cc.object().isEmpty() : "NYI";

        LocalVariable lv = IntermediateVariable.newObject(runtime, variableCounter.getAndIncrement(),
                cc.parameterizedType());
        Result object = new Result(new LinksImpl.Builder(lv).build(), LinkedVariablesImpl.EMPTY);

        // only translate wrt concrete type
        MethodLinkedVariables mlv = recurseIntoLinkComputer(cc.constructor()).removeSomeValue();
        MethodLinkedVariables mlvTranslated1;
        if (mlv.virtual()) {
            VirtualFieldComputer.VfTm vfTm = virtualFieldComputer.compute(cc.parameterizedType(), true);
            mlvTranslated1 = mlv.translate(vfTm.formalToConcrete());
        } else {
            mlvTranslated1 = mlv;
        }
        // NOTE translation with respect to parameters happens in LMC.methodCall()
        List<Result> params = cc.parameterExpressions().stream()
                .map(e -> visit(e, variableData, stage))
                .toList();
        return new LinkMethodCall(runtime, virtualFieldComputer, variableCounter, currentMethod)
                .constructorCall(cc.constructor(), object, params, mlvTranslated1)
                .addVariablesRepresentingConstant(params)
                .addVariablesRepresentingConstant(object);
    }

    private @NotNull Result lambda(Lambda lambda) {
        MethodLinkedVariables mlv = linkComputer.recurseMethod(lambda.methodInfo());

        ParameterizedType concreteObjectType = lambda.concreteReturnType();
        MethodLinkedVariables mlvTranslated;
        if (mlv.virtual()) {
            VirtualFieldComputer.VfTm vfTm = virtualFieldComputer.compute(concreteObjectType, true);
            mlvTranslated = mlv.translate(vfTm.formalToConcrete());
        } else {
            mlvTranslated = mlv;
        }
        int i = 0;
        Map<Variable, Links> map = new HashMap<>();
        for (Links paramLinks : mlvTranslated.ofParameters()) {
            map.put(lambda.methodInfo().parameters().get(i), paramLinks);
            ++i;
        }
        Result wrapped = new Result(mlvTranslated.ofReturnValue(), new LinkedVariablesImpl(map));

        FunctionalInterfaceVariable fiv = new FunctionalInterfaceVariable(
                runtime,
                variableCounter.getAndIncrement(),
                lambda.concreteFunctionalType(),
                wrapped);
        Links links = new LinksImpl.Builder(fiv).build();
        return new Result(links, LinkedVariablesImpl.EMPTY);
    }

    private Result anonymousClassAsFunctionalInterface(VariableData variableData, Stage stage, ConstructorCall cc) {
        TypeInfo anonymousTypeInfo = cc.anonymousClass();

        MethodInfo sami = anonymousTypeImplementsFunctionalInterface(anonymousTypeInfo);
        if (sami != null) {

            MethodLinkedVariables mlv = linkComputer.recurseMethod(sami);
            MethodLinkedVariables mlvTranslated;
            if (mlv.virtual()) {
                VirtualFieldComputer.VfTm vfTm = virtualFieldComputer.compute(cc.parameterizedType(), true);
                mlvTranslated = mlv.translate(vfTm.formalToConcrete());
            } else {
                mlvTranslated = mlv;
            }
            int i = 0;
            Map<Variable, Links> map = new HashMap<>();
            for (Links paramLinks : mlvTranslated.ofParameters()) {
                map.put(sami.parameters().get(i), paramLinks);
                ++i;
            }
            return new Result(mlvTranslated.ofReturnValue(), new LinkedVariablesImpl(map));
        }
        // TODO call recursion
        return EMPTY;
    }

    private MethodInfo anonymousTypeImplementsFunctionalInterface(TypeInfo typeInfo) {
        if (!typeInfo.parentClass().isJavaLangObject()) return null;
        if (!typeInfo.interfacesImplemented().isEmpty()) {
            if (typeInfo.interfacesImplemented().size() > 1) return null;
            if (!typeInfo.interfacesImplemented().getFirst().isFunctionalInterface()) return null;
        }
        List<MethodInfo> methods = typeInfo.methods();
        if (methods.size() != 1) return null;
        return methods.getFirst();
    }


    private Result methodCall(VariableData variableData, Stage stage, MethodCall mc) {
        // recursion, translation
        Result object = visit(mc.object(), variableData, stage);
        Variable objectPrimary = object.links().primary();

        Value.FieldValue fv = mc.methodInfo().getSetField();
        if (fv.field() != null) {
            if (!fv.setter() && mc.parameterExpressions().isEmpty() && objectPrimary != null) {
                VariableExpression ve = runtime.newVariableExpression(
                        runtime.newFieldReference(fv.field(),
                                runtime.newVariableExpression(objectPrimary), mc.concreteReturnType()));
                return variableExpression(ve, variableData, stage);
            }
        }

        MethodLinkedVariables mlv = recurseIntoLinkComputer(mc.methodInfo()).removeSomeValue();
        // translate conditionally wrt concrete type, evaluated object
        MethodLinkedVariables mlvTranslated2;
        if (Util.methodIsSamOfJavaUtilFunctional(mc.methodInfo())) {
            mlvTranslated2 = mlv; // no point doing anything
        } else if (objectPrimary != null) {
            This thisVar = runtime.newThis(mc.methodInfo().typeInfo().asParameterizedType());
            MethodLinkedVariables mlvTranslated1;
            if (!thisVar.equals(objectPrimary)) {
                TranslationMap tm = new VariableTranslationMap(runtime).put(thisVar, objectPrimary);
                mlvTranslated1 = mlv.translate(tm);
            } else {
                mlvTranslated1 = mlv;
            }
            if (!currentMethod.typeInfo().isEqualToOrInnerClassOf(mc.methodInfo().typeInfo())) {
                ParameterizedType concreteObjectType = objectPrimary.parameterizedType();
                if (mlvTranslated1.virtual()) {
                    VirtualFieldComputer.VfTm vfTm = virtualFieldComputer.compute(concreteObjectType, true);
                    TranslationMap tm2;
                    if (!mc.methodInfo().typeParameters().isEmpty()) {
                        // when the return type parameter agrees with the input type parameter, also translate that one!
                        // TP#0 in Optional->Map.Entry.XY[], but U (method TP in .map(...)) needs translating too:
                        // map.§u ⊆ 0:mapper must become map.§xys ⊆ ...
                        tm2 = new VirtualFieldTranslationMapForMethodParameters(virtualFieldComputer, javaInspector().runtime())
                                .go(vfTm.formalToConcrete(), mc);
                    } else {
                        tm2 = vfTm.formalToConcrete();
                    }
                    mlvTranslated2 = mlvTranslated1.translate(tm2);
                } else {
                    mlvTranslated2 = mlvTranslated1;
                }
            } else {
                // no transformation needed
                mlvTranslated2 = mlvTranslated1;
            }
        } else {
            // static method, without object; but there may be method type parameters involved
            TranslationMap vfTm = new VirtualFieldTranslationMapForMethodParameters(virtualFieldComputer, runtime)
                    .staticCall(mc);
            mlvTranslated2 = mlv.translate(vfTm);
        }

        // NOTE translation with respect to parameters happens in LMC.methodCall()
        List<Result> params = mc.parameterExpressions().stream()
                .map(e -> visit(e, variableData, stage))
                .toList();

        // handle all matters 'linking'

        Result r = new LinkMethodCall(runtime, virtualFieldComputer, variableCounter, currentMethod)
                .methodCall(mc.methodInfo(), mc.concreteReturnType(), object, params, mlvTranslated2);

        // handle all matters 'modification'

        Set<Variable> modified = new HashSet<>();

        handleMethodModification(mc, objectPrimary, modified);
        if (!mc.methodInfo().parameters().isEmpty()) {
            PropagateComponents pc = new PropagateComponents(runtime, variableData, stage);
            for (ParameterInfo pi : mc.methodInfo().parameters()) {
                handleParameterModification(mc, pi, params, modified, pc);
            }
            PropagateApplied pa = new PropagateApplied(runtime);
            Set<Variable> modifiedByPropagation = pa.go(variableData, stage, params, mlv.ofParameters());
            modified.addAll(modifiedByPropagation);
        }
        return r.addModified(modified)
                .add(new WriteMethodCall(mc, object.links()))
                .addVariablesRepresentingConstant(params)
                .addVariablesRepresentingConstant(object);
    }

    private void handleParameterModification(MethodCall mc,
                                             ParameterInfo pi,
                                             List<Result> params,
                                             Set<Variable> modified,
                                             PropagateComponents pc) {
        if (pi.isModified()) {
            if (pi.isVarArgs()) {
                for (int i = mc.methodInfo().parameters().size() - 1; i < mc.parameterExpressions().size(); i++) {
                    Result rp = params.get(i);
                    handleModifiedParameter(mc.parameterExpressions().get(i), rp, modified);
                }
            } else {
                Result rp = params.get(pi.index());
                handleModifiedParameter(mc.parameterExpressions().get(pi.index()), rp, modified);
            }
        }
            /*
             when a method reference has been passed on, and that method appears to be modifying, we set the scope
             of the method reference modified as well. If that scope is 'this', the current method will become modifying.
             If the parameters of this method have modified components, we check if we have a value for these components,
             and propagate their modification too.
             */
        /*pc.propagateComponents(MODIFIED_FI_COMPONENTS_PARAMETER, mc, pi,
                (e, mapValue, map) -> {
                    if (e instanceof MethodReference mr) {
                        if (mapValue) {
                            propagateModificationOfObject(modified, mr);
                            for (ParameterInfo mrPi : mr.methodInfo().parameters()) {
                                propagateModificationOfParameter(modified, pi, map, mrPi);
                            }
                        } //TODO else ensureNotModifying(mr);
                    }
                });*/
        pc.propagateComponents(MODIFIED_COMPONENTS_PARAMETER, mc, pi,
                (e, mapValue, map) -> {
                    if (e instanceof VariableExpression ve2 && mapValue) {
                        modified.add(ve2.variable());
                    }
                });
    }

    private void handleMethodModification(MethodCall mc,
                                          Variable objectPrimary,
                                          Set<Variable> modified) {
        if (objectPrimary != null && !mc.methodInfo().isFinalizer()) {
            boolean modifying = mc.methodInfo().isModifying();
            if (modifying) {
                modified.add(objectPrimary);
                Value.VariableBooleanMap modifiedComponents = mc.methodInfo().analysis().getOrNull(MODIFIED_COMPONENTS_METHOD,
                        ValueImpl.VariableBooleanMapImpl.class);
                if (modifiedComponents != null
                    && mc.object() instanceof VariableExpression ve
                    // we must check for 'this', to eliminate simple assignments
                    && !(ve.variable() instanceof This)) {
                    for (Map.Entry<Variable, Boolean> entry : modifiedComponents.map().entrySet()) {
                        // translate "this" of the method's instance type to the current scope
                        ParameterizedType pt = mc.object().parameterizedType().typeInfo().asParameterizedType();
                        This thisInSv = runtime.newThis(pt);
                        TranslationMap tm = new VariableTranslationMap(runtime).put(thisInSv, ve.variable());
                        Variable v = tm.translateVariableRecursively(entry.getKey());
                        modified.add(v);
                    }
                }
            }
        }
    }

    private void handleModifiedParameter(Expression argument, Result rp, Set<Variable> modified) {
        if (rp.links() != null && rp.links().primary() != null) {
            modified.add(rp.links().primary());
        }
        if (argument instanceof MethodReference mr) {
            propagateModificationOfObject(modified, mr);
        }
    }

    private void propagateModificationOfObject(Set<Variable> modified, MethodReference mr) {
        if (mr.methodInfo().isModifying() && mr.scope() instanceof VariableExpression ve) {
            modified.add(ve.variable());
        }
    }

    private void propagateModificationOfParameter(Set<Variable> modified,
                                                  ParameterInfo pi,
                                                  Map<Variable, Expression> map,
                                                  ParameterInfo mrPi) {
        Value.VariableBooleanMap modComp = mrPi.analysis().getOrDefault(MODIFIED_COMPONENTS_PARAMETER,
                ValueImpl.VariableBooleanMapImpl.EMPTY);
        if (!modComp.isEmpty()) {
            TranslationMap tm = new VariableTranslationMap(runtime)
                    .put(mrPi, runtime.newThis(pi.parameterizedType()));
            for (Map.Entry<Variable, Boolean> entry : modComp.map().entrySet()) {
                if (entry.getValue()) {
                    // modified component
                    Variable translated = tm.translateVariableRecursively(entry.getKey());
                    Expression value = map.get(translated);
                    if (value instanceof VariableExpression ve) {
                        modified.add(ve.variable());
                    }
                }
            }
        }
    }

    private MethodLinkedVariables recurseIntoLinkComputer(MethodInfo methodInfo) {
        if (methodInfo.equals(currentMethod)) {
            return new MethodLinkedVariablesImpl(LinksImpl.EMPTY,
                    methodInfo.parameters().stream().map(_ -> LinksImpl.EMPTY).toList());
        }
        RecursionPrevention.How how = recursionPrevention.contains(methodInfo);
        return switch (how) {
            case GET -> methodInfo.analysis().getOrDefault(METHOD_LINKS, MethodLinkedVariablesImpl.EMPTY);
            case SOURCE -> linkComputer.recurseMethod(methodInfo);
            case SHALLOW -> linkComputer.doMethodShallowDoNotWrite(methodInfo);
            case LOCK -> methodInfo.analysis().getOrCreate(METHOD_LINKS, () -> linkComputer.doMethod(methodInfo));
        };
    }

}
