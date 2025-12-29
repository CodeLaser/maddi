package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.analysis.Value;
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
import static org.e2immu.language.cst.impl.analysis.PropertyImpl.*;

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
                         Set<Variable> modifiedFunctionalInterfaceComponents,
                         List<WriteMethodCall> writeMethodCalls,
                         Map<Variable, Set<ParameterizedType>> casts,
                         Set<Variable> erase) {
        public Result(Links links, LinkedVariables extra) {
            this(links, extra, new HashSet<>(), new HashSet<>(), new ArrayList<>(), new HashMap<>(), new HashSet<>());
        }

        public void addErase(Variable variable) {
            this.erase.add(variable);
        }

        public @NotNull Result addExtra(Map<Variable, Links> linkedVariables) {
            if (!linkedVariables.isEmpty()) {
                return new Result(links, extra.merge(new LinkedVariablesImpl(linkedVariables)),
                        modified, modifiedFunctionalInterfaceComponents, writeMethodCalls, casts, erase);
            }
            return this;
        }

        public Result addModified(Set<Variable> modified) {
            this.modified.addAll(modified);
            return this;
        }

        public Result addModifiedFunctionalInterfaceComponents(Set<Variable> set) {
            this.modifiedFunctionalInterfaceComponents.addAll(set);
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
            return new Result(links, extra, modified, modifiedFunctionalInterfaceComponents, writeMethodCalls, casts,
                    erase);
        }

        public Result merge(Result other) {
            if (this == EMPTY) return other;
            if (other == EMPTY) return this;
            LinkedVariables combinedExtra = extra.isEmpty() ? other.extra : extra.merge(other.extra);
            if (other.links != null && other.links.primary() != null) {
                combinedExtra = combinedExtra.merge(new LinkedVariablesImpl(Map.of(other.links.primary(), other.links)));
            }
            Result r = new Result(this.links, combinedExtra,
                    new HashSet<>(this.modified),
                    new HashSet<>(modifiedFunctionalInterfaceComponents),
                    new ArrayList<>(this.writeMethodCalls),
                    new HashMap<>(this.casts),
                    new HashSet<>(this.erase));
            r.writeMethodCalls.addAll(other.writeMethodCalls);
            r.modified.addAll(other.modified);
            r.modifiedFunctionalInterfaceComponents.addAll(other.modifiedFunctionalInterfaceComponents);
            other.casts.forEach((v, set) ->
                    r.casts.computeIfAbsent(v, _ -> new HashSet<>()).addAll(set));
            r.erase.addAll(other.erase);
            return r;
        }

        public Result moveLinksToExtra() {
            if (links.primary() != null) {
                LinkedVariables newExtra = this.extra.merge(new LinkedVariablesImpl(Map.of(links.primary(), links)));
                return new Result(LinksImpl.EMPTY, newExtra, modified, modifiedFunctionalInterfaceComponents,
                        writeMethodCalls, casts, erase);
            }
            return this;
        }

        public Result copyLinksToExtra() {
            if (links.primary() != null) {
                LinkedVariables newExtra = this.extra.merge(new LinkedVariablesImpl(Map.of(links.primary(), links)));
                return new Result(links, newExtra, modified, modifiedFunctionalInterfaceComponents,
                        writeMethodCalls, casts, erase);
            }
            return this;
        }
    }

    static final Result EMPTY = new Result(LinksImpl.EMPTY, LinkedVariablesImpl.EMPTY, Set.of(), Set.of(),
            List.of(), Map.of(), Set.of());

    public Result visit(Expression expression, VariableData variableData, Stage stage) {
        return switch (expression) {
            case VariableExpression ve -> variableExpression(ve, variableData, stage);
            case Assignment a -> assignment(variableData, stage, a);
            case MethodCall mc -> methodCall(variableData, stage, mc);
            case MethodReference mr -> methodReference(variableData, stage, mr);
            case ConstructorCall cc -> {
                if (cc.anonymousClass() != null) {
                    yield anonymousClassAsFunctionalInterface(variableData, stage, cc);
                }
                yield constructorCall(variableData, stage, cc);
            }
            case Lambda lambda -> lambda(lambda);
            case Cast cast -> cast(variableData, stage, cast);
            case InstanceOf instanceOf -> instanceOf(variableData, instanceOf);

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
            case UnaryOperator uo -> visit(uo.expression(), variableData, stage).with(LinksImpl.EMPTY);
            case GreaterThanZero gt0 -> visit(gt0.expression(), variableData, stage);
            case BinaryOperator bo -> visit(bo.lhs(), variableData, stage)
                    .merge(visit(bo.rhs(), variableData, stage)).with(LinksImpl.EMPTY);
            case ConstantExpression<?> _, TypeExpression _ -> EMPTY;
            default -> throw new UnsupportedOperationException("Implement: " + expression.getClass());
        };
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

    private Result methodReference(VariableData variableData, Stage stage, MethodReference mr) {
        MethodLinkedVariables mlv = linkComputer.recurseMethod(mr.methodInfo());

        Result object = visit(mr.scope(), variableData, stage);
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

    private @NotNull Result inlineConditional(InlineConditional ic, VariableData variableData, Stage stage) {
        Result rc = visit(ic.condition(), variableData, stage);
        Result rt = visit(ic.ifTrue(), variableData, stage);
        Result rf = visit(ic.ifFalse(), variableData, stage);
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

    private @NotNull Result variableExpression(VariableExpression ve, VariableData variableData, Stage stage) {
        Variable v = ve.variable();
        LinkedVariables extra = LinkedVariablesImpl.EMPTY;
        while (v instanceof DependentVariable dv) {
            v = dv.arrayVariable();
            if (v != null) {
                Links vLinks = new LinksImpl.Builder(dv).add(LinkNatureImpl.IS_ELEMENT_OF, v).build();
                extra = extra.merge(new LinkedVariablesImpl(Map.of(dv, vLinks)));
            }
        }

        if (v instanceof FieldReference fr) {
            Result r = visit(fr.scope(), variableData, stage);
            extra = extra.merge(r.extra);
        }
        Links.Builder builder = new LinksImpl.Builder(ve.variable());
        if (ve.variable().parameterizedType().isFunctionalInterface()
            && variableData != null && variableData.isKnown(ve.variable().fullyQualifiedName())) {
            VariableInfo vi = variableData.variableInfo(ve.variable());
            Links links = Objects.requireNonNullElse(vi.linkedVariables(), LinksImpl.EMPTY);
            links.forEach(l -> builder.add(l.from(), l.linkNature(), l.to()));
        }
        return new Result(builder.build(), extra);
    }

    private Result assignment(VariableData variableData, Stage stage, Assignment a) {
        Links.Builder builder = new LinksImpl.Builder(a.variableTarget());
        Result rValue = visit(a.value(), variableData, stage);
        Result rTarget = visit(a.target(), variableData, stage);
        if (rValue.links != null && rValue.links.primary() != null) {
            builder.add(LinkNatureImpl.IS_IDENTICAL_TO, rValue.links.primary());
        }
        Result result = new Result(builder.build(), LinkedVariablesImpl.EMPTY);
        if (a.assignmentOperator() != null || rValue.links == null || rValue.links.primary() == null) {
            result.addErase(a.variableTarget());
        }
        return result
                .merge(rValue)
                .merge(rTarget)
                .addModified(Util.scopeVariables(a.variableTarget()));
    }

    private Result constructorCall(VariableData variableData, Stage stage, ConstructorCall cc) {
        assert cc.object() == null || cc.object().isEmpty() : "NYI";

        LocalVariable lv = javaInspector.runtime().newLocalVariable("$__c" + variableCounter.getAndIncrement(),
                cc.parameterizedType());
        Result object = new Result(new LinksImpl.Builder(lv).build(), LinkedVariablesImpl.EMPTY);

        // only translate wrt concrete type
        MethodLinkedVariables mlv = recurseIntoLinkComputer(cc.constructor());
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
        return new LinkMethodCall(javaInspector.runtime(), virtualFieldComputer, variableCounter, currentMethod)
                .constructorCall(cc.constructor(), object, params, mlvTranslated1);
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
        return new Result(mlvTranslated.ofReturnValue(), new LinkedVariablesImpl(map));
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

        Result object = mc.methodInfo().isStatic() ? EMPTY : visit(mc.object(), variableData, stage);
        MethodLinkedVariables mlv = recurseIntoLinkComputer(mc.methodInfo());

        // translate conditionally wrt concrete type, evaluated object
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
                if (mlvTranslated1.virtual()) {
                    VirtualFieldComputer.VfTm vfTm = virtualFieldComputer.compute(concreteObjectType, true);
                    mlvTranslated2 = mlvTranslated1.translate(vfTm.formalToConcrete());
                } else {
                    mlvTranslated2 = mlvTranslated1;
                }
            } else {
                // no transformation needed
                mlvTranslated2 = mlvTranslated1;
            }
        } else {
            // static method, without object; but there may be method type parameters involved
            TranslationMap vfTm = new VirtualFieldTranslationMapForStaticMethods(javaInspector.runtime())
                    .go(virtualFieldComputer, mc);
            mlvTranslated2 = mlv.translate(vfTm);
        }

        // NOTE translation with respect to parameters happens in LMC.methodCall()
        List<Result> params = mc.parameterExpressions().stream().map(e -> visit(e, variableData, stage))
                .toList();

        // handle all matters 'linking'

        Result r = new LinkMethodCall(javaInspector.runtime(), virtualFieldComputer, variableCounter, currentMethod)
                .methodCall(mc.methodInfo(), mc.concreteReturnType(), object, params, mlvTranslated2);

        // handle all matters 'modification'

        Set<Variable> modified = new HashSet<>();
        Set<Variable> modifiedFunctionalInterfaceComponents = new HashSet<>();

        handleMethodModification(mc, objectPrimary, modifiedFunctionalInterfaceComponents, modified);
        if (!mc.methodInfo().parameters().isEmpty()) {
            PropagateComponents pc = new PropagateComponents(javaInspector.runtime(), variableData, stage);
            for (ParameterInfo pi : mc.methodInfo().parameters()) {
                handleParameterModification(mc, pi, params, modified, pc);
            }
        }
        return r.addModified(modified)
                .addModifiedFunctionalInterfaceComponents(modifiedFunctionalInterfaceComponents)
                .add(new WriteMethodCall(mc, object.links));
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
        pc.propagateComponents(MODIFIED_FI_COMPONENTS_PARAMETER, mc, pi,
                (e, mapValue, map) -> {
                    if (e instanceof MethodReference mr) {
                        if (mapValue) {
                            propagateModificationOfObject(modified, mr);
                            for (ParameterInfo mrPi : mr.methodInfo().parameters()) {
                                propagateModificationOfParameter(modified, pi, map, mrPi);
                            }
                        } //TODO else ensureNotModifying(mr);
                    }
                });
        pc.propagateComponents(MODIFIED_COMPONENTS_PARAMETER, mc, pi,
                (e, mapValue, map) -> {
                    if (e instanceof VariableExpression ve2 && mapValue) {
                        modified.add(ve2.variable());
                    }
                });
    }

    private void handleMethodModification(MethodCall mc,
                                          Variable objectPrimary,
                                          Set<Variable> modifiedFunctionalInterfaceComponents,
                                          Set<Variable> modified) {
        if (objectPrimary != null && !mc.methodInfo().isFinalizer()) {
            boolean modifying = mc.methodInfo().isModifying();
            if (objectPrimary.parameterizedType().isFunctionalInterface()
                && objectPrimary instanceof FieldReference fr
                && !fr.isStatic() && !fr.scopeIsRecursivelyThis()) {
                modifiedFunctionalInterfaceComponents.add(objectPrimary);
            } else if (modifying) {
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
                        This thisInSv = javaInspector.runtime().newThis(pt);
                        TranslationMap tm = javaInspector.runtime().newTranslationMapBuilder()
                                .put(thisInSv, ve.variable())
                                .build();
                        Variable v = tm.translateVariableRecursively(entry.getKey());
                        modified.add(v);
                    }
                }
            }
        }
    }

    private void handleModifiedParameter(Expression argument, Result rp, Set<Variable> modified) {
        if (rp.links != null && rp.links.primary() != null) {
            modified.add(rp.links.primary());
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
            TranslationMap tm = javaInspector.runtime().newTranslationMapBuilder()
                    .put(mrPi, javaInspector.runtime().newThis(pi.parameterizedType()))
                    .build();
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
