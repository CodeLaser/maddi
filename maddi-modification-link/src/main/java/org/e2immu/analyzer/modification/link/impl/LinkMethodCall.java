package org.e2immu.analyzer.modification.link.impl;

import org.e2immu.analyzer.modification.link.Link;
import org.e2immu.analyzer.modification.link.LinkNature;
import org.e2immu.analyzer.modification.link.Links;
import org.e2immu.analyzer.modification.link.MethodLinkedVariables;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public record LinkMethodCall(Runtime runtime, AtomicInteger variableCounter) {

    public ExpressionVisitor.Result constructorCall(MethodInfo methodInfo,
                                                    ExpressionVisitor.Result object,
                                                    List<ExpressionVisitor.Result> params,
                                                    MethodLinkedVariables mlv) {
        Map<Variable, Links> extra = new HashMap<>(object.extra().map());
        params.forEach(r -> r.extra().forEach(e ->
                extra.merge(e.getKey(), e.getValue(), Links::merge)));

        Links newObjectLinks = parametersToObject(methodInfo, object, params, mlv);

        return new ExpressionVisitor.Result(newObjectLinks, new LinkedVariablesImpl(extra));
    }

    // we're trying for both method calls and normal constructor calls
    // the latter have a newly created temporary local variable as their object primary
    public ExpressionVisitor.Result methodCall(MethodInfo methodInfo,
                                               ExpressionVisitor.Result object,
                                               List<ExpressionVisitor.Result> params,
                                               MethodLinkedVariables mlv) {
        Map<Variable, Links> extra = new HashMap<>(object.extra().map());
        params.forEach(r -> r.extra().forEach(e ->
                extra.merge(e.getKey(), e.getValue(), Links::merge)));
        Variable objectPrimary = object.links().primary();
        if (!object.links().isEmpty()) {
            extra.put(objectPrimary, object.links());
        }
        Links concreteReturnValue = mlv.ofReturnValue() == null ? LinksImpl.EMPTY :
                objectToReturnValue(methodInfo, params, mlv, objectPrimary);
        Links callReturn;
        if (objectPrimary != null) {
            Links newObjectLinks = parametersToObject(methodInfo, object, params, mlv);
            callReturn = concreteReturnValue == LinksImpl.EMPTY ? newObjectLinks : concreteReturnValue.merge(newObjectLinks);
        } else {
            linksBetweenParameters(methodInfo, params, mlv, extra);
            callReturn = concreteReturnValue;
        }
        return new ExpressionVisitor.Result(callReturn, new LinkedVariablesImpl(extra));
    }

    private void linksBetweenParameters(MethodInfo methodInfo,
                                        List<ExpressionVisitor.Result> params,
                                        MethodLinkedVariables mlv,
                                        Map<Variable, Links> extra) {
        int i = 0;
        assert mlv.ofParameters().size() == methodInfo.parameters().size();
        for (Links links : mlv.ofParameters()) {
            if (!links.isEmpty()) {
                for (Link link : links) {
                    ParameterInfo to = Util.parameterPrimary(link.to());
                    Variable toPrimary = params.get(to.index()).links().primary();
                    TranslationMap toTm = runtime.newTranslationMapBuilder().put(to, toPrimary).build();
                    Variable translatedTo = toTm.translateVariableRecursively(link.to());

                    ParameterInfo from = methodInfo.parameters().get(i);
                    if (from.isVarArgs()) {
                        for (int j = i; j < params.size(); ++j) {
                            // loop over all the elements in the varargs
                            addCrosslink(params, extra, link, true, j, from, translatedTo);
                        }
                    } else {
                        addCrosslink(params, extra, link, false, i, from, translatedTo);
                    }
                }
            }
            ++i;
        }
    }

    private void addCrosslink(List<ExpressionVisitor.Result> params,
                              Map<Variable, Links> extra,
                              Link link,
                              boolean varargs,
                              int i,
                              ParameterInfo from,
                              Variable translatedTo) {
        Variable fromPrimary = params.get(i).links().primary();
        Links.Builder builder = new LinksImpl.Builder(fromPrimary);
        TranslationMap fromTm = runtime.newTranslationMapBuilder().put(from, fromPrimary).build();
        Variable translatedFrom = fromTm.translateVariableRecursively(link.from());
        LinkNature linkNature = varargs ? varargsLinkNature(link.linkNature(), from, fromPrimary) : link.linkNature();
        builder.add(translatedFrom, linkNature, translatedTo);
        extra.merge(fromPrimary, builder.build(), Links::merge);
    }

    private LinkNature varargsLinkNature(LinkNature linkNature, ParameterInfo from, Variable fromPrimary) {
        if (linkNature == LinkNature.INTERSECTION_NOT_EMPTY || linkNature == LinkNature.IS_IDENTICAL_TO) {
            return LinkNature.IS_ELEMENT_OF;
        }
        return linkNature;
    }

    private Links objectToReturnValue(MethodInfo methodInfo,
                                      List<ExpressionVisitor.Result> params,
                                      MethodLinkedVariables mlv,
                                      Variable objectPrimary) {
        Variable rvPrimary = mlv.ofReturnValue().primary();
        if (rvPrimary != null && !mlv.ofReturnValue().isEmpty()) {
            assert rvPrimary instanceof ReturnVariable
                    : "the links of the method return value must be in the return variable";
            assert !methodInfo.isVoid() || methodInfo.isConstructor()
                    : "Cannot be a void function if we have a return variable";
            Variable newPrimary = runtime.newLocalVariable("$__rv" + variableCounter.getAndIncrement(),
                    rvPrimary.parameterizedType());

            TranslationMap.Builder tmBuilder = runtime.newTranslationMapBuilder();
            if (objectPrimary != null) {
                assert !methodInfo.isStatic() : """
                        objectPrimary!=null indicates that we have an instance function.
                        Therefore we must translate 'this' to the method's object primary""";
                newPrimary = runtime.newLocalVariable("$__rv" + variableCounter.getAndIncrement(),
                        rvPrimary.parameterizedType());
                addThisHierarchyToObjectPrimaryToTmBuilder(methodInfo, tmBuilder, objectPrimary);
            }
            // the return value can also contain references to parameters... we should replace them by
            // actual arguments
            int index = 0;
            for (ExpressionVisitor.Result pr : params) {
                if (pr.links().primary() != null) {
                    tmBuilder.put(methodInfo.parameters().get(index), pr.links().primary());
                }
                ++index;
            }
            return mlv.ofReturnValue().changePrimaryTo(runtime, newPrimary, tmBuilder.build());
        }
        return LinksImpl.EMPTY;
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
        TranslationMap tm = tmBuilder.build();
        for (Links links : mlv.ofParameters()) {
            ParameterInfo pi = methodInfo.parameters().get(i);
            Variable paramPrimary = params.get(i).links().primary();
            TranslationMap newPrimaryTm = runtime.newTranslationMapBuilder().put(pi, paramPrimary).build();
            if (!links.isEmpty()) {
                for (Link link : links) {
                    Variable translatedFrom = newPrimaryTm.translateVariableRecursively(link.from());
                    Variable translatedTo = tm.translateVariableRecursively(link.to());
                    builder.add(translatedTo, link.linkNature().reverse(), translatedFrom);
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
