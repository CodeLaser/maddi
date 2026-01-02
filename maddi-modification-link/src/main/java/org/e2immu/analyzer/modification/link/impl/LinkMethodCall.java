package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record LinkMethodCall(Runtime runtime,
                             VirtualFieldComputer virtualFieldComputer,
                             AtomicInteger variableCounter,
                             MethodInfo currentMethod) {

    public ExpressionVisitor.Result constructorCall(MethodInfo methodInfo,
                                                    ExpressionVisitor.Result object,
                                                    List<ExpressionVisitor.Result> params,
                                                    MethodLinkedVariables mlv) {
        Map<Variable, Links> extra = new HashMap<>(object.extra().map());
        copyParamsIntoExtra(methodInfo.parameters(), params, extra);

        Links newObjectLinks = parametersToObject(methodInfo, object, params, mlv);

        return new ExpressionVisitor.Result(newObjectLinks, new LinkedVariablesImpl(extra));
    }

    private static void copyParamsIntoExtra(List<ParameterInfo> formalParameters,
                                            List<ExpressionVisitor.Result> params,
                                            Map<Variable, Links> extra) {
        int i = 0;
        for (ExpressionVisitor.Result r : params) {
            ParameterInfo pi = formalParameters.get(Math.min(formalParameters.size() - 1, i));
            if (!pi.parameterizedType().isFunctionalInterface()) {
                if (r.links() != null && r.links().primary() != null) {
                    extra.merge(r.links().primary(), r.links(), Links::merge);
                }
                r.extra().forEach(e ->
                        extra.merge(e.getKey(), e.getValue(), Links::merge));
            }// else: the extra information will be used, but it will not be passed on
            // it contains links related to the lambda parameters, lambda return variable, etc.
            // See e.g. TestForEachLambda,4
            ++i;
        }
    }

    // we're trying for both method calls and normal constructor calls
    // the latter have a newly created temporary local variable as their object primary
    public ExpressionVisitor.Result methodCall(MethodInfo methodInfo,
                                               ParameterizedType concreteReturnType,
                                               ExpressionVisitor.Result object,
                                               List<ExpressionVisitor.Result> params,
                                               MethodLinkedVariables mlv) {
        Map<Variable, Links> extra = new HashMap<>(object.extra().map());
        copyParamsIntoExtra(methodInfo.parameters(), params, extra);
        Variable objectPrimary = object.links().primary();
        if (!object.links().isEmpty()) {
            extra.put(objectPrimary, object.links());
        }
        Links concreteReturnValue;
        if (mlv.ofReturnValue() == null) {
            concreteReturnValue = LinksImpl.EMPTY;
        } else if (Util.methodIsSamOfJavaUtilFunctional(methodInfo)) {
            assert methodInfo == methodInfo.typeInfo().singleAbstractMethod();
            concreteReturnValue = parametersToReturnValue(methodInfo, concreteReturnType, params, mlv,
                    objectPrimary);
        } else {
            concreteReturnValue = objectToReturnValue(methodInfo, concreteReturnType, params, mlv, objectPrimary);
        }
        if (objectPrimary != null) {
            Links newObjectLinks = parametersToObject(methodInfo, object, params, mlv);
            if (newObjectLinks.primary() instanceof This && !newObjectLinks.isEmpty()) {
                newObjectLinks.removeThisAsPrimary().forEach(links ->
                        extra.merge(links.primary(), links, Links::merge));
            } else {
                extra.merge(objectPrimary, newObjectLinks, Links::merge);
            }
        } else {
            linksBetweenParameters(methodInfo, params, mlv, extra);
        }
        return new ExpressionVisitor.Result(concreteReturnValue, new LinkedVariablesImpl(extra));
    }

    private void linksBetweenParameters(MethodInfo methodInfo,
                                        List<ExpressionVisitor.Result> params,
                                        MethodLinkedVariables mlv,
                                        Map<Variable, Links> extra) {
        int i = 0;
        assert mlv.ofParameters().size() == methodInfo.parameters().size();
        for (Links links : mlv.ofParameters()) {
            if (links != null && !links.isEmpty()) {
                for (Link link : links) {
                    ParameterInfo to = Util.parameterPrimary(link.to());
                    Variable toPrimary = params.get(to.index()).links().primary();
                    TranslationMap toTm = runtime.newTranslationMapBuilder().put(to, toPrimary).build();
                    Variable translatedTo = toTm.translateVariableRecursively(link.to());

                    ParameterInfo from = methodInfo.parameters().get(i);
                    if (from.isVarArgs()) {
                        LinkNature linkNature = varargsLinkNature(link.linkNature());
                        for (int j = i; j < params.size(); ++j) {
                            Variable fromPrimary = params.get(j).links().primary();
                            Variable linkFrom = downgrade(link.from(), fromPrimary);
                            // loop over all the elements in the varargs
                            addCrosslink(fromPrimary, extra, from, linkFrom, linkNature, translatedTo);
                        }
                    } else {
                        Variable fromPrimary = params.get(i).links().primary();
                        addCrosslink(fromPrimary, extra, from, link.from(), link.linkNature(), translatedTo);
                    }
                }
            }
            ++i;
        }
    }

    private Variable downgrade(Variable from, Variable fromPrimary) {
        String simpleName = from.simpleName();
        assert simpleName.length() > 1 && simpleName.endsWith("s");
        ParameterizedType newType = from.parameterizedType().copyWithOneFewerArrays();
        String nameOneFewerS = simpleName.substring(0, simpleName.length() - 1);
        if (from.parameterizedType().arrays() > 1) {
            if (from instanceof FieldReference fr) {
                FieldInfo fi = runtime.newFieldInfo(nameOneFewerS, false, newType, fr.fieldInfo().owner());
                return runtime.newFieldReference(fi, fr.scope(), newType);
            }
            if (from instanceof LocalVariable) {
                return runtime.newLocalVariable(nameOneFewerS, newType);
            }
            throw new UnsupportedOperationException();
        }
        if (from instanceof FieldReference) {
            // elements.ts -> t
            return fromPrimary;
        }
        throw new UnsupportedOperationException();
    }

    private void addCrosslink(Variable fromPrimary,
                              Map<Variable, Links> extra,
                              ParameterInfo from,
                              Variable linkFrom,
                              LinkNature linkNature,
                              Variable translatedTo) {
        Links.Builder builder = new LinksImpl.Builder(fromPrimary);
        TranslationMap fromTm = runtime.newTranslationMapBuilder().put(from, fromPrimary).build();
        Variable translatedFrom = fromTm.translateVariableRecursively(linkFrom);
        builder.add(translatedFrom, linkNature, translatedTo);
        Links links = builder.build();
        extra.merge(fromPrimary, links, Links::merge);
    }

    private LinkNature varargsLinkNature(LinkNature linkNature) {
        if (linkNature == LinkNatureImpl.IS_SUBSET_OF) {
            return LinkNatureImpl.IS_ELEMENT_OF;
        }
        if (linkNature == LinkNatureImpl.IS_SUPERSET_OF) {
            return LinkNatureImpl.CONTAINS_AS_MEMBER;
        }
        return linkNature;
    }

    private Links objectToReturnValue(MethodInfo methodInfo,
                                      ParameterizedType concreteReturnType,
                                      List<ExpressionVisitor.Result> params,
                                      MethodLinkedVariables mlv,
                                      Variable objectPrimary) {
        Links ofReturnValue = mlv.ofReturnValue();
        Variable rvPrimary = ofReturnValue.primary();
        if (rvPrimary == null || ofReturnValue.isEmpty()) {
            return LinksImpl.EMPTY;
        }
        assert rvPrimary instanceof ReturnVariable
                : "the links of the method return value must be in the return variable";
        assert !methodInfo.isVoid() || methodInfo.isConstructor()
                : "Cannot be a void function if we have a return variable";
        String name = LinksImpl.INTERMEDIATE_RETURN_VARIABLE + variableCounter.getAndIncrement();
        Variable newPrimary = runtime.newLocalVariable(name, concreteReturnType);

        TranslationMap.Builder tmBuilder = runtime.newTranslationMapBuilder();
        if (objectPrimary != null) {
            addThisHierarchyToObjectPrimaryToTmBuilder(methodInfo, tmBuilder, objectPrimary);
        }
        // the return value can also contain references to parameters... we should replace them by
        // actual arguments
        int index = 0;
        for (ExpressionVisitor.Result pr : params) {
            Variable to = Objects.requireNonNullElseGet(pr.links().primary(), this::newDummyLocalVariable);
            tmBuilder.put(methodInfo.parameters().get(index), to);
            ++index;
        }
        TranslationMap tm = tmBuilder.build();
        Function<Variable, List<Links>> samLinks = v ->
                v instanceof ParameterInfo pi ? List.of(params.get(pi.index()).links()) : List.of();
        TranslationMap newPrimaryTm = runtime.newTranslationMapBuilder().put(ofReturnValue.primary(), newPrimary).build();
        Links.Builder builder = new LinksImpl.Builder(newPrimary);
        for (Link link : ofReturnValue.linkSet()) {
            if (link.from().equals(ofReturnValue.primary())) {
                translateHandleFunctional(tm, link, newPrimary, link.linkNature(), builder, samLinks, objectPrimary);
            } else {
                Variable fromTranslated = newPrimaryTm.translateVariableRecursively(link.from());
                translateHandleFunctional(tm, link, fromTranslated, link.linkNature(), builder, samLinks, objectPrimary);
            }
        }
        return builder.build();
    }

    private LocalVariable newDummyLocalVariable() {
        return runtime.newLocalVariable(LinksImpl.INTERMEDIATE_LOCAL_VARIABLE + variableCounter.getAndIncrement(),
                runtime.objectParameterizedType());
    }

    private void translateHandleFunctional(TranslationMap defaultTm,
                                           Link link,
                                           Variable fromTranslated,
                                           LinkNature linkNature,
                                           Links.Builder builder,
                                           Function<Variable, List<Links>> paramProvider,
                                           Variable objectPrimary) {
        ParameterizedType parameterizedType = link.to().parameterizedType();
        if (parameterizedType.isFunctionalInterface()) {
            List<LinkFunctionalInterface.Triplet> toAdd =
                    new LinkFunctionalInterface(runtime, virtualFieldComputer, currentMethod).go(parameterizedType, fromTranslated,
                            linkNature, builder.primary(), paramProvider.apply(link.to()), objectPrimary);
            toAdd.forEach(t -> builder.add(t.from(), t.linkNature(), t.to()));
            return;

        }
        if (link.to() instanceof AppliedFunctionalInterfaceVariable applied) {
            List<Links> list = paramProvider.apply(applied.sourceOfFunctionalInterface());
            // translate params in list with values from applied.params()
            List<Links> translated = replaceParametersByEvalInApplied(list, applied.params());
            List<LinkFunctionalInterface.Triplet> toAdd =
                    new LinkFunctionalInterface(runtime, virtualFieldComputer, currentMethod)
                            .go(applied.sourceOfFunctionalInterface().parameterizedType(),
                                    fromTranslated, linkNature, builder.primary(), translated,
                                    objectPrimary);
            toAdd.forEach(t -> builder.add(t.from(), t.linkNature(), t.to()));
            return;
        }
        builder.add(fromTranslated, linkNature, defaultTm.translateVariableRecursively(link.to()));
    }

    private List<Links> replaceParametersByEvalInApplied(List<Links> list, List<ExpressionVisitor.Result> params) {
        return list.stream().map(links -> replaceParametersByEvalInApplied(links, params)).toList();
    }

    private Links replaceParametersByEvalInApplied(Links links, List<ExpressionVisitor.Result> params) {
        Links.Builder builder = new LinksImpl.Builder(links.primary());
        for (Link link : links) {
            Set<ParameterInfo> pis = link.to().variableStreamDescend()
                    .filter(v -> v instanceof ParameterInfo)
                    .map(v -> (ParameterInfo) v)
                    .collect(Collectors.toUnmodifiableSet());
            Variable to;
            if (!pis.isEmpty()) {
                TranslationMap.Builder tmb = runtime.newTranslationMapBuilder();
                pis.forEach(pi -> tmb.put(pi,
                        Objects.requireNonNullElse(params.get(pi.index()).links().primary(), pi)));
                to = tmb.build().translateVariableRecursively(link.to());
            } else {
                to = link.to();
            }

            builder.add(link.from(), link.linkNature(), to);
        }
        return builder.build();
    }

    private Links parametersToReturnValue(MethodInfo methodInfo,
                                          ParameterizedType concreteReturnType,
                                          List<ExpressionVisitor.Result> params,
                                          MethodLinkedVariables mlv,
                                          Variable objectPrimary) {
        Links ofReturnValue = mlv.ofReturnValue();
        Variable rvPrimary = ofReturnValue.primary();

        assert rvPrimary instanceof ReturnVariable
                : "the links of the method return value must be in the return variable";
        assert !methodInfo.isVoid() || methodInfo.isConstructor()
                : "Cannot be a void function if we have a return variable";

        Variable applied = new AppliedFunctionalInterfaceVariable(
                LinksImpl.FUNCTIONAL_INTERFACE_VARIABLE + variableCounter.getAndIncrement(),
                concreteReturnType, runtime.newEmptyExpression(),
                objectPrimary instanceof ParameterInfo pi ? pi : null,
                params);
        Links.Builder builder = new LinksImpl.Builder(rvPrimary);
        builder.add(LinkNatureImpl.IS_ASSIGNED_FROM, applied);
        return builder.build();
    }

    private @NotNull Links parametersToObject(MethodInfo methodInfo,
                                              ExpressionVisitor.Result object,
                                              List<ExpressionVisitor.Result> params,
                                              MethodLinkedVariables mlv) {
        Variable objectPrimary = object.links().primary();
        int i = 0;
        Links.Builder builder = new LinksImpl.Builder(objectPrimary);
        TranslationMap.Builder tmBuilder = runtime.newTranslationMapBuilder();
        addThisHierarchyToObjectPrimaryToTmBuilder(methodInfo, tmBuilder, objectPrimary);
        for (ParameterInfo pi : methodInfo.parameters()) {
            if (pi.index() < params.size()) {
                ExpressionVisitor.Result result = params.get(pi.index());
                if (result != null && result.links() != null) {
                    Variable primary = result.links().primary();
                    tmBuilder.put(pi, Objects.requireNonNullElseGet(primary, this::newDummyLocalVariable));
                }
            }
        }
        TranslationMap tm = tmBuilder.build();
        for (Links links : mlv.ofParameters()) {
            assert links != null;
            ParameterInfo pi = methodInfo.parameters().get(i);
            ExpressionVisitor.Result r = params.get(i);
            Links piLinks = r.links();
            if (piLinks != null) {
                Variable paramPrimary = piLinks.primary();
                if (paramPrimary == null || !paramPrimary.parameterizedType().isFunctionalInterface()) {
                    if (!links.isEmpty()) {
                        for (Link link : links) {
                            Variable translatedFrom = tm.translateVariableRecursively(link.from());
                            Variable translatedTo = tm.translateVariableRecursively(link.to());
                            if (Util.isPartOf(objectPrimary, translatedTo)) {
                                builder.add(translatedTo, link.linkNature().reverse(), translatedFrom);
                            } else if (Util.isPartOf(objectPrimary, translatedTo)) {
                                builder.add(translatedTo, link.linkNature(), translatedFrom);
                            }
                        }
                    }
                }
            }
            ParameterizedType parameterizedType = pi.parameterizedType();
            if (parameterizedType.isFunctionalInterface() && !r.extra().isEmpty()) {
                // pi is a consumer, the link information is in the extras

                // link from the method call object (list) into this (not into the parameter, not the return value)
                // returnPrimary ~ this
                // fromTranslated ~ upscale this
                //
                List<Links> linksList = r.extra().map().entrySet().stream()
                        .filter(e -> e.getKey() instanceof ParameterInfo)
                        .map(Map.Entry::getValue)
                        .toList();
                for (Link link : links) {
                    List<LinkFunctionalInterface.Triplet> toAdd =
                            new LinkFunctionalInterface(runtime, virtualFieldComputer, currentMethod).
                                    go(parameterizedType, link.from(),
                                            LinkNatureImpl.SHARES_ELEMENTS,
                                            builder.primary(),
                                            linksList,
                                            objectPrimary);
                    toAdd.forEach(t -> builder.add(t.from(), t.linkNature(), t.to()));
                }
            }
            ++i;
        }
        return builder.build();
    }

    private void addThisHierarchyToObjectPrimaryToTmBuilder(MethodInfo methodInfo,
                                                            TranslationMap.Builder tmBuilder,
                                                            Variable objectPrimary) {
        TypeInfo thisType = methodInfo.typeInfo();
        Stream<TypeInfo> stream = Stream.concat(Stream.of(thisType),
                thisType.superTypesExcludingJavaLangObject().stream());
        stream.forEach(ti -> {
            This thisVar = runtime.newThis(ti.asSimpleParameterizedType());
            tmBuilder.put(thisVar, objectPrimary);
        });
    }

}
