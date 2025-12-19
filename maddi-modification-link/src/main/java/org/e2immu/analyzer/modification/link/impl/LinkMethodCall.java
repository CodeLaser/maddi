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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
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
        Links concreteReturnValue = mlv.ofReturnValue() == null ? LinksImpl.EMPTY :
                objectToReturnValue(methodInfo, concreteReturnType, params, mlv, objectPrimary);
        if (objectPrimary != null) {
            Links newObjectLinks = parametersToObject(methodInfo, object, params, mlv);
            extra.merge(objectPrimary, newObjectLinks, Links::merge);
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
        if (linkNature == LinkNature.INTERSECTION_NOT_EMPTY || linkNature == LinkNature.IS_IDENTICAL_TO) {
            return LinkNature.IS_ELEMENT_OF;
        }
        return linkNature;
    }

    private Links objectToReturnValue(MethodInfo methodInfo,
                                      ParameterizedType concreteReturnType,
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
                    concreteReturnType);

            TranslationMap.Builder tmBuilder = runtime.newTranslationMapBuilder();
            if (objectPrimary != null) {
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
            return changePrimaryTo(mlv.ofReturnValue(), newPrimary, tmBuilder.build(), i -> params.get(i).links(), objectPrimary);
        }
        return LinksImpl.EMPTY;
    }

    public Links changePrimaryTo(Links ofReturnValue,
                                 Variable newPrimary,
                                 TranslationMap tm,
                                 IntFunction<Links> paramProvider,
                                 Variable objectPrimary) {
        TranslationMap newPrimaryTm = null;
        Links.Builder builder = new LinksImpl.Builder(newPrimary);
        for (Link link : ofReturnValue.linkSet()) {
            if (link.from().equals(ofReturnValue.primary())) {
                translateHandleSupplier(tm, link, newPrimary, link.linkNature(), builder, paramProvider, objectPrimary);
            } else {
                // links that are not 'primary'
                if (newPrimaryTm == null) {
                    newPrimaryTm = runtime.newTranslationMapBuilder().put(ofReturnValue.primary(), newPrimary).build();
                }
                Variable fromTranslated = newPrimaryTm.translateVariableRecursively(link.from());
                translateHandleSupplier(tm, link, fromTranslated, link.linkNature(), builder, paramProvider, objectPrimary);
            }
        }
        return builder.build();
    }

    private void translateHandleSupplier(TranslationMap defaultTm,
                                         Link link,
                                         Variable fromTranslated,
                                         LinkNature linkNature,
                                         Links.Builder builder,
                                         IntFunction<Links> paramProvider,
                                         Variable objectPrimary) {
        if (link.to().parameterizedType().isFunctionalInterface()) {
            if (link.to() instanceof ParameterInfo pi) {
                List<LinkFunctionalInterface.Triplet> toAdd =
                        new LinkFunctionalInterface(runtime, virtualFieldComputer, currentMethod).go(pi, fromTranslated,
                                linkNature, builder.primary(), paramProvider, objectPrimary);
                toAdd.forEach(t -> builder.add(t.from(), t.linkNature(), t.to()));
                return;
            }
        }
        builder.add(fromTranslated, linkNature, defaultTm.translateVariableRecursively(link.to()));
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
            ExpressionVisitor.Result r = params.get(i);
            Links piLinks = r.links();
            if (piLinks != null) {
                Variable paramPrimary = piLinks.primary();
                if (paramPrimary != null && !paramPrimary.parameterizedType().isFunctionalInterface()) {
                    TranslationMap newPrimaryTm = runtime.newTranslationMapBuilder().put(pi, paramPrimary).build();
                    if (!links.isEmpty()) {
                        for (Link link : links) {
                            Variable translatedFrom = newPrimaryTm.translateVariableRecursively(link.from());
                            Variable translatedTo = tm.translateVariableRecursively(link.to());
                            builder.add(translatedTo, link.linkNature().reverse(), translatedFrom);
                        }
                    }
                }
            }
            if (pi.parameterizedType().isFunctionalInterface() && !r.extra().isEmpty()) {
                // pi is a consumer, the link information is in the extras

                // link from the method call object (list) into this (not into the parameter, not the return value)
                // returnPrimary ~ this
                // fromTranslated ~ upscale this
                //
                IntFunction<Links> paramProvider = index -> r.extra().map().entrySet().stream()
                        .filter(e -> e.getKey() instanceof ParameterInfo param && param.index() == index)
                        .map(Map.Entry::getValue)
                        .findFirst().orElse(null);
                for (Link link : links) {
                    List<LinkFunctionalInterface.Triplet> toAdd =
                            new LinkFunctionalInterface(runtime, virtualFieldComputer, currentMethod).
                                    go(pi, link.from(),
                                            LinkNature.INTERSECTION_NOT_EMPTY,
                                            builder.primary(),
                                            paramProvider,
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
