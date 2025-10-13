package org.e2immu.analyzer.modification.linkedvariables.impl;

import org.e2immu.analyzer.modification.common.AnalysisHelper;
import org.e2immu.analyzer.modification.linkedvariables.lv.*;
import org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector;
import org.e2immu.analyzer.modification.prepwork.hcs.IndexImpl;
import org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl;
import org.e2immu.analyzer.modification.prepwork.hct.ComputeHiddenContent;
import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.analyzer.modification.prepwork.variable.*;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodCall;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.linkedvariables.lv.LVImpl.*;
import static org.e2immu.analyzer.modification.linkedvariables.lv.LinkedVariablesImpl.LINKED_VARIABLES_PARAMETER;
import static org.e2immu.analyzer.modification.prepwork.hcs.HiddenContentSelector.*;
import static org.e2immu.language.cst.api.analysis.Value.FieldValue;
import static org.e2immu.language.cst.api.analysis.Value.Immutable;
import static org.e2immu.language.cst.api.analysis.Value.Independent;
import static org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl.ALL_INDICES;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.NO_VALUE;
import static org.e2immu.language.cst.impl.analysis.ValueImpl.IndependentImpl.*;

class LinkHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(LinkHelper.class);

    private final Runtime runtime;
    private final GenericsHelper genericsHelper;
    private final AnalysisHelper analysisHelper;

    private final TypeInfo currentPrimaryType;
    private final MethodInfo methodInfo;
    private final HiddenContentTypes hiddenContentTypes;
    private final HiddenContentSelector hcsObject;

    public LinkHelper(Runtime runtime,
                      GenericsHelper genericsHelper,
                      AnalysisHelper analysisHelper,
                      MethodInfo currentMethod,
                      MethodInfo methodInfo) {
        this.runtime = runtime;
        this.analysisHelper = analysisHelper;
        this.genericsHelper = genericsHelper;
        this.currentPrimaryType = currentMethod.primaryType();
        this.hiddenContentTypes = methodInfo.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        this.methodInfo = methodInfo;
        ParameterizedType formalObject = this.methodInfo.typeInfo().asParameterizedType();
        this.hcsObject = HiddenContentSelector.selectAll(hiddenContentTypes, formalObject);
    }

    /*
    Linked variables of parameter.
    There are 2 types involved:
      1. type in declaration = formalParameterType
      2. type in method call = parameterExpression.returnType() == concreteParameterType
    The hcsSource
    In the case of links between parameters, the "source" becomes the object.


    This is a prep method, where we re-compute the base of the parameter's link to the hidden content types of the method.
    The minimum link level is LINK_DEPENDENT.
    The direction of the links is from the method to the variables linked to the parameter, correcting for the
    concrete parameter type.
    All Indices on the 'from'-side are single HCT indices.

    If the formalParameterType is a type parameter, we'll have an index of hc wrt the method:
    If the argument is a variable, the link is typically v:0; we'll link 'index of hc' -2-> ALL if the concrete type
      allows so.
    If the argument is dependently linked to a variable, e.g. v.subList(..), then we'll still link DEPENDENT, and
      add the 'index of hc' -2-> ALL.
    If the argument is HC linked to a variable, e.g. new ArrayList<>(..), we'll link 'index of hc' -4-> ALL.

    If formalParameterType is not a type parameter, we'll compute a translation map which expresses
    the hidden content of the method, expressed in formalParameterType, to indices in the parameter type.

    IMPORTANT: these links are meant to be combined with either links to object, or links to other parameters.
    This code is different from the normal linkedVariables(...) method.

     */
    private LinkedVariables linkedVariablesOfParameter(ParameterizedType formalParameterType,
                                                       ParameterizedType concreteParameterType,
                                                       LinkedVariables linkedVariablesOfParameter,
                                                       HiddenContentSelector hcsSource,
                                                       boolean allowVarargs) {
        LinkedVariables res = linkedVariablesOfParameter(hiddenContentTypes, formalParameterType, concreteParameterType,
                linkedVariablesOfParameter, hcsSource, allowVarargs);
        LOGGER.debug("LV of parameter {}; {}; {}; {} = {}", formalParameterType, concreteParameterType,
                linkedVariablesOfParameter, hcsSource, res);
        return res;
    }

    // recursive!
    private LinkedVariables linkedVariablesOfParameter(HiddenContentTypes hiddenContentTypes,
                                                       ParameterizedType formalParameterType,
                                                       ParameterizedType concreteParameterTypeIn,
                                                       LinkedVariables linkedVariablesOfParameter,
                                                       HiddenContentSelector hcsSource,
                                                       boolean allowVarargs) {
        ParameterizedType concreteParameterType = ensureTypeParameters(concreteParameterTypeIn);
        Immutable immutable = analysisHelper.typeImmutable(currentPrimaryType, concreteParameterType);
        if (immutable != null && immutable.isImmutable()) {
            return LinkedVariablesImpl.EMPTY;
        }

        Integer index = hiddenContentTypes.indexOfOrNull(formalParameterType);
        if (index != null && formalParameterType.arrays() > 0
            && concreteParameterType.arrays() == formalParameterType.arrays()) {
            // see TestRecursiveCall
            return linkedVariablesOfParameter(hiddenContentTypes, formalParameterType.copyWithOneFewerArrays(),
                    concreteParameterType.copyWithOneFewerArrays(), linkedVariablesOfParameter, hcsSource, allowVarargs);
        }
        if (index != null && formalParameterType.parameters().isEmpty()) {
            if (!concreteParameterType.parameters().isEmpty()) {
                // recursion at the level of the type parameters
                HiddenContentTypes newHiddenContentTypes = concreteParameterType.typeInfo().analysis()
                        .getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
                ParameterizedType newParameterMethodType = concreteParameterType.typeInfo().asParameterizedType()
                        // add the arrays, e.g. because of Class<?>[]
                        .copyWithArrays(concreteParameterType.arrays());
                HiddenContentSelector newHcsSource = HiddenContentSelector.selectAll(newHiddenContentTypes,
                        newParameterMethodType);
                LinkedVariables recursive = linkedVariablesOfParameter(newHiddenContentTypes,
                        newParameterMethodType, concreteParameterType, linkedVariablesOfParameter, newHcsSource,
                        allowVarargs);
                return recursive.map(lv -> lv.prefixMine(index));
            }
        }

        // the current formal type is not one of the hidden content types
        Map<Indices, HiddenContentSelector.IndicesAndType> targetData = hcsSource
                .translateHcs(runtime, genericsHelper, formalParameterType, concreteParameterType, allowVarargs);
        Map<Variable, LV> map = new HashMap<>();
        linkedVariablesOfParameter.stream().forEach(e -> {
            Value.Immutable immutableOfKey = analysisHelper.typeImmutable(currentPrimaryType, e.getKey().parameterizedType());
            if (!immutableOfKey.isImmutable()) {
                LV newLv = lvOfParameter(e, targetData, concreteParameterType);
                if (newLv != null) {
                    map.put(e.getKey(), newLv);
                }
            }
        });
        return LinkedVariablesImpl.of(map);
    }

    private LV lvOfParameter(Map.Entry<Variable, LV> e,
                             Map<Indices, IndicesAndType> targetData,
                             ParameterizedType concreteParameterType) {
        LV lv = e.getValue();
        if (targetData != null && !targetData.isEmpty()) {
            Map<Indices, Link> linkMap = new HashMap<>();
            Collection<Indices> targetDataKeys;
            // NOTE: this type of filter occurs in 'continueLinkedVariables' as well
            if (lv.haveLinks()) {
                targetDataKeys = lv.links().map().keySet().stream().filter(targetData::containsKey).toList();
            } else {
                targetDataKeys = targetData.keySet();
            }
            for (Indices iInHctSource : targetDataKeys) {
                IndicesAndType value = targetData.get(iInHctSource);
                Indices iInHctTarget = lv.haveLinks() && lv.theirsIsAll() ? ALL_INDICES : value.indices();
                ParameterizedType type = value.type();
                assert type != null;
                Immutable immutable = analysisHelper.typeImmutable(currentPrimaryType, type);
                if (immutable.isImmutable()) {
                    continue;
                }
                boolean mutable = immutable.isMutable();
                assert iInHctTarget != null;
                if (!(iInHctSource.isAll() && iInHctTarget.isAll())) {
                    Link prev = linkMap.put(iInHctSource, new LinkImpl(iInHctTarget, mutable));
                    assert prev == null;
                }
            }
            if (linkMap.isEmpty()) {
                return createDependent(linkAllSameType(concreteParameterType));
            }
            Links links = new LinksImpl(Map.copyOf(linkMap));
            boolean independentHc = lv.isCommonHC();
            return independentHc ? LVImpl.createHC(links) : LVImpl.createDependent(links);
        }
        return createDependent(linkAllSameType(concreteParameterType));
    }

    private ParameterizedType ensureTypeParameters(ParameterizedType pt) {
        if (!pt.parameters().isEmpty() || pt.typeInfo() == null) return pt;
        ParameterizedType formal = pt.typeInfo().asParameterizedType();
        List<ParameterizedType> parameters = new ArrayList<>();
        for (int i = 0; i < formal.parameters().size(); ++i) parameters.add(runtime.objectParameterizedType());
        return pt.withParameters(List.copyOf(parameters));
    }

    public record FromParameters(EvaluationResult intoObject, EvaluationResult intoResult) {
    }

    /*
    Add all necessary links from parameters into scope, and in-between parameters
     */
    public FromParameters linksInvolvingParameters(ParameterizedType objectPt,
                                                   ParameterizedType resultPt,
                                                   List<Expression> parameterExpressions,
                                                   List<EvaluationResult> linkedVariables) {
        EvaluationResult.Builder intoObjectBuilder = new EvaluationResult.Builder();
        EvaluationResult.Builder intoResultBuilder = resultPt == null || resultPt.isVoid() ? null
                : new EvaluationResult.Builder();
        if (!methodInfo.parameters().isEmpty()) {
            boolean isFactoryMethod = methodInfo.isFactoryMethod();
            // links between object/return value and parameters
            int i = 0;
            int nMinusOne = methodInfo.parameters().size() - 1;
            for (Expression parameterExpression : parameterExpressions) {
                ParameterInfo pi = methodInfo.parameters().get(Math.min(nMinusOne, i));
                EvaluationResult evaluationResult = linkedVariables.get(i);
                if (!evaluationResult.linkedVariables().isEmpty()) {
                    linkParameterToObjectOrResult(pi, objectPt, resultPt, parameterExpression, evaluationResult,
                            isFactoryMethod, intoResultBuilder, intoObjectBuilder);
                } // else: see e.g. link.TestArrayInitializer
                ++i;
            }
            linksBetweenParameters(intoObjectBuilder, methodInfo, parameterExpressions, linkedVariables);
        }
        return new FromParameters(intoObjectBuilder.build(), intoResultBuilder == null ? null : intoResultBuilder.build());
    }

    private void linkParameterToObjectOrResult(ParameterInfo pi,
                                               ParameterizedType objectPt,
                                               ParameterizedType resultPt,
                                               Expression parameterExpression,
                                               EvaluationResult evaluationResultOfParameter,
                                               boolean isFactoryMethod,
                                               EvaluationResult.Builder intoResultBuilder,
                                               EvaluationResult.Builder intoObjectBuilder) {
        Independent formalParameterIndependent = pi.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER,
                DEPENDENT);
        LinkedVariables lvsFactory = isFactoryMethod ? pi.analysis().getOrDefault(LINKED_VARIABLES_PARAMETER,
                LinkedVariablesImpl.EMPTY) : LinkedVariablesImpl.EMPTY;
        LinkedVariables lvsToResult = lvsFactory.merge(linkedVariablesToResult(pi, formalParameterIndependent));
        boolean inResult = intoResultBuilder != null && !lvsToResult.isEmpty();
        if (!inResult && objectPt == null) return; // no chance
        if (formalParameterIndependent.isIndependent() && !inResult) {
            // formal parameter independent -> not linked to object
            // but if we're only interested in the object, there's nothing we can do
            return;
        }
        LinkedVariables lvsArgument = evaluationResultOfParameter.linkedVariables();
        if (inResult && pi.index() == 0 && methodInfo.isIdentity()) {
            intoResultBuilder.setLinkedVariables(lvsArgument);
            return;
        }
        ParameterizedType concreteArgumentType = parameterExpression.parameterizedType();
        LinkedVariables lvsArgumentCorrectedToMethod = linkedVariablesOfParameter(pi.parameterizedType(),
                concreteArgumentType, lvsArgument, hcsObject, pi.isVarArgs());
        LinkedVariables lvsArgumentCorrectedToObjectOrReturnValue;
        Independent independentOfObjectOrReturnValue;
        EvaluationResult.Builder builder;
        ParameterizedType concreteObjectOrReturnType;
        ParameterizedType formalObjectOrReturnType;
        if (inResult) {
            /*
            change the links of the parameter to the value of the return variable (see also MethodReference,
            computation of links when modified is true)
             */
            LV valueOfReturnValue = lvsToResult.stream().filter(e -> e.getKey() instanceof ReturnVariable)
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            if (valueOfReturnValue != null) {
                lvsArgumentCorrectedToObjectOrReturnValue = goFromArgumentToReturnVariable(lvsArgumentCorrectedToMethod, valueOfReturnValue);
                independentOfObjectOrReturnValue = valueOfReturnValue.isCommonHC() ? INDEPENDENT_HC : DEPENDENT;
            } else {
                lvsArgumentCorrectedToObjectOrReturnValue = LinkedVariablesImpl.EMPTY;
                independentOfObjectOrReturnValue = INDEPENDENT;
            }
            builder = intoResultBuilder;
            concreteObjectOrReturnType = resultPt;
            formalObjectOrReturnType = methodInfo.returnType();
        } else {
            lvsArgumentCorrectedToObjectOrReturnValue = lvsArgumentCorrectedToMethod;
            independentOfObjectOrReturnValue = formalParameterIndependent;
            builder = intoObjectBuilder;
            concreteObjectOrReturnType = objectPt;
            formalObjectOrReturnType = methodInfo.typeInfo().asParameterizedType();
        }
        copyAdditionalLinksIntoBuilder(evaluationResultOfParameter, builder);

        LinkedVariables lv = computeLvForParameter(pi, inResult, concreteArgumentType, lvsArgumentCorrectedToObjectOrReturnValue,
                independentOfObjectOrReturnValue, concreteObjectOrReturnType, formalObjectOrReturnType);
        LOGGER.debug("LV for parameter {}; {}: {}", pi, lvsArgumentCorrectedToObjectOrReturnValue, lv);
        if (lv != null) {
            builder.mergeLinkedVariablesOfExpression(lv);
        }
    }

    private static LinkedVariables goFromArgumentToReturnVariable(LinkedVariables lvsArgumentToMethod,
                                                                  LV lvReturnValue) {
        LinkedVariables lvsToObjectOrReturnVariable;
        Map<Variable, LV> map = new HashMap<>();
        for (Map.Entry<Variable, LV> e : lvsArgumentToMethod) {
            LV follow = follow(lvReturnValue, e.getValue());
            if (follow != null) {
                map.put(e.getKey(), follow);
            }
        }
        lvsToObjectOrReturnVariable = LinkedVariablesImpl.of(map);
        return lvsToObjectOrReturnVariable;
    }

    private static void copyAdditionalLinksIntoBuilder(EvaluationResult evaluationResultOfParameter,
                                                       EvaluationResult.Builder builder) {
        evaluationResultOfParameter.links().forEach((v, lvs1) ->
                lvs1.forEach(e -> {
                    if (!e.getValue().isStaticallyAssignedOrAssigned()) {
                        builder.merge(v, LinkedVariablesImpl.of(e.getKey(), e.getValue()));
                    } // IMPROVE this is not the best way to approach copying, what with -1- links??
                }));
    }

    private LinkedVariables computeLvForParameter(ParameterInfo pi,
                                                  boolean toReturnVariable,
                                                  ParameterizedType concreteTypeOfArgument,
                                                  LinkedVariables lvsArgumentInFunctionOfMethod,
                                                  Independent formalParameterIndependent,
                                                  ParameterizedType concreteTypeOfObjectOrReturnVariable,
                                                  ParameterizedType formalTypeOfObjectOrReturnVariable) {
        Integer indexToDirectlyLinkedField = computeIndexToDirectlyLinkedField(pi);
        HiddenContentSelector hcsParameter = pi.analysis().getOrDefault(HCS_PARAMETER, NONE);
        if (toReturnVariable) {
            // parameter -> return variable
            HiddenContentSelector hcsMethod = methodInfo.analysis().getOrDefault(HCS_METHOD, NONE);
            return linkedVariables(this.hcsObject, concreteTypeOfArgument, pi.parameterizedType(),
                    hcsParameter, lvsArgumentInFunctionOfMethod, false,
                    formalParameterIndependent, concreteTypeOfObjectOrReturnVariable, formalTypeOfObjectOrReturnVariable,
                    hcsMethod, false, indexToDirectlyLinkedField);
        }
        Immutable mutable = analysisHelper.typeImmutable(currentPrimaryType, pi.parameterizedType());
        if (pi.parameterizedType().isTypeParameter() && !concreteTypeOfArgument.parameters().isEmpty()) {
            if (mutable.isMutable()) {
                return lvsArgumentInFunctionOfMethod;
            }
            return lvsArgumentInFunctionOfMethod.map(LV::changeToHc);
        }
        if (!mutable.isImmutable()) {
            // object -> parameter (rather than the other way around)
            return linkedVariables(this.hcsObject, concreteTypeOfObjectOrReturnVariable,
                    formalTypeOfObjectOrReturnVariable, this.hcsObject, lvsArgumentInFunctionOfMethod, pi.isVarArgs(),
                    formalParameterIndependent, concreteTypeOfArgument, pi.parameterizedType(),
                    hcsParameter, true, indexToDirectlyLinkedField);
        }
        return null;
    }

    private static Integer computeIndexToDirectlyLinkedField(ParameterInfo pi) {
        // this block of code may have to be moved up for linkedVariablesOfParameter to use it
        // see TestLinkConstructorInMethodCall,2 for an example
        Integer indexToDirectlyLinkedField;
        StaticValues svPi = pi.analysis().getOrDefault(StaticValuesImpl.STATIC_VALUES_PARAMETER, StaticValuesImpl.NONE);
        if (svPi.expression() instanceof VariableExpression ve
            && ve.variable() instanceof FieldReference fr
            && fr.scopeIsRecursivelyThis()) {
            indexToDirectlyLinkedField = fr.fieldInfo().indexInType();
            // IMPROVE only works for first order at the moment
            // IMPROVE only works for @Final fields in the same type, because SV_PARAM is computed afterwards for
            // non-final fields
        } else {
            indexToDirectlyLinkedField = null;
        }
        return indexToDirectlyLinkedField;
    }

    public void linksBetweenParameters(EvaluationResult.Builder builder,
                                       MethodInfo methodInfo,
                                       List<Expression> parameterExpressions,
                                       List<EvaluationResult> linkedVariables) {
        Map<ParameterInfo, LinkedVariables> crossLinks = translateLinksToParameters(methodInfo);
        if (crossLinks.isEmpty()) return;
        crossLinks.forEach((pi, lv) ->
                doCrossLinkOfParameter(builder, methodInfo, parameterExpressions, linkedVariables, pi, lv));
    }

    private void doCrossLinkOfParameter(EvaluationResult.Builder builder,
                                        MethodInfo methodInfo,
                                        List<Expression> parameterExpressions,
                                        List<EvaluationResult> linkedVariables,
                                        ParameterInfo pi,
                                        LinkedVariables lv) {
        boolean sourceIsVarArgs = pi.isVarArgs();
        assert !sourceIsVarArgs : "Varargs must always be a target";
        HiddenContentSelector hcsSource = methodInfo.parameters().get(pi.index()).analysis()
                .getOrDefault(HCS_PARAMETER, NONE);
        ParameterizedType sourceType = parameterExpressions.get(pi.index()).parameterizedType();
        LinkedVariables sourceLvs = linkedVariablesOfParameter(pi.parameterizedType(),
                parameterExpressions.get(pi.index()).parameterizedType(),
                linkedVariables.get(pi.index()).linkedVariables(), hcsSource, false);

        lv.stream().forEach(e -> {
            ParameterInfo target = (ParameterInfo) e.getKey();
            doCrossLinkFromTo(builder, methodInfo, parameterExpressions, linkedVariables, pi, e, target, hcsSource,
                    sourceType, sourceLvs);
        });
    }

    private void doCrossLinkFromTo(EvaluationResult.Builder builder,
                                   MethodInfo methodInfo,
                                   List<Expression> parameterExpressions,
                                   List<EvaluationResult> linkedVariables,
                                   ParameterInfo pi,
                                   Map.Entry<Variable, LV> e,
                                   ParameterInfo target,
                                   HiddenContentSelector hcsSource,
                                   ParameterizedType sourceType,
                                   LinkedVariables sourceLvs) {
        boolean targetIsVarArgs = target.isVarArgs();
        if (!targetIsVarArgs || linkedVariables.size() > target.index()) {

            LV level = e.getValue();

            for (int i = target.index(); i < linkedVariables.size(); i++) {
                ParameterizedType targetType = parameterExpressions.get(target.index()).parameterizedType();
                HiddenContentSelector hcsTarget = methodInfo.parameters().get(target.index()).analysis()
                        .getOrDefault(HCS_PARAMETER, NONE);

                LinkedVariables targetLinkedVariables = linkedVariablesOfParameter(target.parameterizedType(),
                        parameterExpressions.get(i).parameterizedType(),
                        linkedVariables.get(i).linkedVariables(), hcsSource, pi.isVarArgs());

                Independent independentDv = level.isCommonHC() ? INDEPENDENT_HC
                        : DEPENDENT;
                LinkedVariables mergedLvs = linkedVariables(hcsSource, targetType, target.parameterizedType(), hcsSource,
                        targetLinkedVariables, targetIsVarArgs, independentDv, sourceType, pi.parameterizedType(),
                        hcsTarget, targetIsVarArgs, null); // IMPROVE indexOfDirectlyLinkedField??
                crossLink(sourceLvs, mergedLvs, builder);
            }
        } // else: no value... empty varargs
    }

    private LinkedVariables linkedVariablesToResult(ParameterInfo pi, Independent formalParameterIndependent) {
        Integer lvToResult = formalParameterIndependent.linkToParametersReturnValue() == null ? null :
                formalParameterIndependent.linkToParametersReturnValue().get(-1);
        if (lvToResult != null) {
            // we know that there is linking
            ReturnVariable rv = new ReturnVariableImpl(pi.methodInfo());
            if (lvToResult == 0) return LinkedVariablesImpl.of(rv, LINK_DEPENDENT);
            assert lvToResult == 1;
            // this is either a *-4-n or n-4-m
            Links links = hcLinkParameterToResult(pi);
            return LinkedVariablesImpl.of(rv, LVImpl.createHC(links));
        }
        LinkedVariables lvMethod = pi.methodInfo().analysis().getOrDefault(LinkedVariablesImpl.LINKED_VARIABLES_METHOD,
                LinkedVariablesImpl.EMPTY);
        LV lv = lvMethod.stream().filter(e -> e.getKey() == pi)
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
        if (pi.methodInfo().isIdentity() && pi.index() == 0) {
            return LinkedVariablesImpl.of(pi, LINK_ASSIGNED);
        }
        if (lv == null) return LinkedVariablesImpl.EMPTY;
        ReturnVariable rv = new ReturnVariableImpl(pi.methodInfo());
        LV reverse = lv.reverse(); // we must link towards to result!!!
        return LinkedVariablesImpl.of(rv, reverse);
    }

    // hasPi 1=*
    // hasMe 1=0, 2=*
    private Links hcLinkParameterToResult(ParameterInfo pi) {
        HiddenContentSelector hcsPi = pi.analysis().getOrDefault(HCS_PARAMETER, NONE);
        HiddenContentSelector hcsMe = pi.methodInfo().analysis().getOrDefault(HCS_METHOD, NONE);
        Map<Indices, Link> map = new HashMap<>();
        for (Map.Entry<Integer, Indices> e : hcsPi.getMap().entrySet()) {
            Indices inMethod = hcsMe.getMap().get(e.getKey());
            if (inMethod != null) {
                map.put(e.getValue(), new LinkImpl(inMethod, false));
            }
        }
        return new LinksImpl(map);
    }

    private Map<ParameterInfo, LinkedVariables> translateLinksToParameters(MethodInfo methodInfo) {
        Map<ParameterInfo, Map<Variable, LV>> res = new HashMap<>();
        for (ParameterInfo pi : methodInfo.parameters()) {
            Independent independent = pi.analysis().getOrDefault(PropertyImpl.INDEPENDENT_PARAMETER,
                    DEPENDENT);
            Map<Variable, LV> lvMap = new HashMap<>();
            for (Map.Entry<Integer, Integer> e : independent.linkToParametersReturnValue().entrySet()) {
                if (e.getKey() >= 0) {
                    ParameterInfo target = methodInfo.parameters().get(e.getKey());
                    LV lv;
                    if (e.getValue() == 0) {
                        lv = LINK_DEPENDENT;
                    } else {
                        lv = createHC(linkAllSameType(pi.parameterizedType()));
                    }
                    LV prev = lvMap.put(target, lv);
                    assert prev == null;
                }
            }
            if (!lvMap.isEmpty()) res.put(pi, lvMap);
        }
        return res.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                e -> LinkedVariablesImpl.of(e.getValue())));
    }

    public Links linkAllSameType(ParameterizedType parameterizedType) {
        TypeInfo typeInfo = parameterizedType.bestTypeInfo();
        if (typeInfo == null) return LinksImpl.NO_LINKS;
        HiddenContentTypes hct = typeInfo.analysis().getOrCreate(HIDDEN_CONTENT_TYPES,
                () -> new ComputeHiddenContent(runtime).compute(typeInfo));
        if (hct.hasHiddenContent()) {
            Map<Indices, Link> map = new HashMap<>();
            for (int i = 0; i < hct.size(); i++) {
                // not mutable, because hidden content
                // from i to i, because we have a -1- relation, so the type must be the same
                map.put(new IndicesImpl(i), new LinkImpl(new IndicesImpl(i), false));
            }
            return new LinksImpl(Map.copyOf(map));
        }
        return LinksImpl.NO_LINKS;
    }

    /*
   In general, the method result 'a', in 'a = b.method(c, d)', can link to 'b', 'c' and/or 'd'.
   Independence and immutability restrict the ability to link.

   The current implementation is heavily focused on understanding links towards the fields of a type,
   i.e., in sub = list.subList(0, 10), we want to link sub to list.

   Links from the parameters to the result (from 'c' to 'a', from 'd' to 'a') have currently only
   been implemented for @Identity methods (i.e., between 'a' and 'c').

   So we implement
   1/ void methods cannot link
   2/ if the method is @Identity, the result is linked to the 1st parameter 'c'
   3/ if the method is a factory method, the result is linked to the parameter values

   all other rules now determine whether we return an empty set, or the set {'a'}.

   4/ independence is determined by the independence value of the method, and the independence value of the object 'a'
    */

    public LinkedVariables linkedVariablesMethodCallObjectToReturnType(MethodCall methodCall,
                                                                       ParameterizedType objectType,
                                                                       LinkedVariables linkedVariablesOfObjectIn,
                                                                       List<LinkedVariables> linkedVariables,
                                                                       ParameterizedType returnType) {
        // RULE 1: void methods cannot link
        if (methodInfo.noReturnValue()) return LinkedVariablesImpl.EMPTY;

        // RULE 2: @Identity links to the 1st parameter
        if (methodInfo.isIdentity()) {
            return linkedVariables.getFirst().maximum(LINK_ASSIGNED);
        }
        LinkedVariables linkedVariablesOfObject = linkedVariablesOfObjectIn.maximum(LINK_ASSIGNED);

        // RULE 3: @Fluent simply returns the same object, hence, the same linked variables
        Value.Bool fluent = methodInfo.analysis().getOrDefault(PropertyImpl.FLUENT_METHOD, ValueImpl.BoolImpl.FALSE);
        if (fluent.isTrue()) {
            return linkedVariablesOfObject;
        }

        Value.Independent independent = methodInfo.analysis().getOrDefault(PropertyImpl.INDEPENDENT_METHOD,
                DEPENDENT);
        Value.Immutable immutable = methodInfo.analysis().getOrDefault(PropertyImpl.IMMUTABLE_METHOD,
                ValueImpl.ImmutableImpl.MUTABLE);
        assert !immutable.isImmutable() || independent.isIndependent();
        assert !immutable.isImmutableHC() || independent.isAtLeastIndependentHc();

        ParameterizedType methodType = methodInfo.typeInfo().asParameterizedType();
        ParameterizedType methodReturnType = methodInfo.returnType();

        HiddenContentSelector hcsTarget = methodInfo.analysis().getOrDefault(HCS_METHOD, NONE);

        Value.FieldValue fieldValue = methodInfo.getSetField();
        Integer indexOfDirectlyLinkedField = fieldValue.field() != null && !fieldValue.setter()
                ? fieldValue.field().indexInType() : null;

        LinkedVariables lvs = linkedVariables(hcsObject, objectType,
                methodType, hcsObject, linkedVariablesOfObject,
                false,
                independent, returnType, methodReturnType, hcsTarget,
                false, indexOfDirectlyLinkedField);

        Value.FieldValue getSetField = methodInfo.getSetField();
        if (getSetField.field() != null && !getSetField.setter()) {
            return handleGetter(methodCall, returnType, getSetField, lvs, linkedVariablesOfObject);
        }
        return lvs;
    }

    private LinkedVariables handleGetter(MethodCall methodCall,
                                         ParameterizedType returnType,
                                         FieldValue getSetField,
                                         LinkedVariables lvs,
                                         LinkedVariables linkedVariablesOfObject) {
        Immutable immutable = analysisHelper.typeImmutable(getSetField.field().type());
        if (immutable.isImmutable()) return lvs;
        Map<Variable, LV> lvMap = new HashMap<>();
        linkedVariablesOfObject.variablesAssigned().forEach(v -> {
            // NOTE: pretty similar code in Factory.getterVariable(MethodCall)
            Variable variable;
            if (methodCall.parameterExpressions().isEmpty()) {
                variable = runtime.newFieldReference(getSetField.field(), runtime.newVariableExpression(v), returnType);
            } else {
                // indexing
                FieldReference fr = runtime.newFieldReference(getSetField.field(), runtime.newVariableExpression(v),
                        returnType.copyWithArrays(returnType.arrays() + 1));
                Expression array = runtime.newVariableExpressionBuilder()
                        .setVariable(fr).setSource(methodCall.object().source())
                        .build();
                Expression index = methodCall.parameterExpressions().getFirst();
                assert index.parameterizedType().isMathematicallyInteger();
                variable = runtime.newDependentVariable(array, index, returnType);
                Immutable immutableMethodReturnType = analysisHelper.typeImmutable(returnType);
                if (!immutableMethodReturnType.isImmutable()) {
                    LV lv;
                    if (immutableMethodReturnType.isImmutableHC()) {
                        lv = createHC(new LinksImpl(IndexImpl.ALL, 0, false));
                    } else {
                        lv = createDependent(new LinksImpl(IndexImpl.ALL, 0, false));
                    }
                    LV prev = lvMap.put(fr, lv);
                    assert prev == null;
                }
            }
            LV prev = lvMap.put(variable, LINK_ASSIGNED);
            assert prev == null;
        });
        return LinkedVariablesImpl.of(lvMap).merge(lvs);
    }

    /**
     * Important: this method does not deal with hidden content specific to the method, because it has been designed
     * to connect the object to the return value, as called from <code>linkedVariablesMethodCallObjectToReturnType</code>.
     * Calls originating from <code>linksInvolvingParameters</code> must take this into account.
     * <p>
     * IMPORTANT: this method reports the linked values of the TARGET, starting off with the linked variables of the SOURCE.
     *
     * @param sourceTypeIn                  must be type of object or parameterExpression, return type, non-evaluated
     * @param methodSourceType              the method declaration's type of the source
     * @param hiddenContentSelectorOfSource with respect to the method's HCT and methodSourceType
     * @param sourceLvs                     linked variables of the source
     * @param sourceIsVarArgs               allow for a correction of array -> element
     * @param transferIndependent           the transfer mode (dependent, independent HC, independent)
     * @param targetTypeIn                  must be type of object or parameterExpression, return type, non-evaluated
     * @param methodTargetType              the method declaration's type of the target
     * @param hiddenContentSelectorOfTarget with respect to the method's HCT and methodTargetType
     * @param reverse                       reverse the link, because we're reversing source and target, because we
     *                                      only deal with *->0 in this method, never 0->*,
     * @return the linked values of the target.
     */
    private LinkedVariables linkedVariables(HiddenContentSelector hcsObject,
                                            ParameterizedType sourceTypeIn,
                                            ParameterizedType methodSourceType,
                                            HiddenContentSelector hiddenContentSelectorOfSource,
                                            LinkedVariables sourceLvs,
                                            boolean sourceIsVarArgs,
                                            Value.Independent transferIndependent,
                                            ParameterizedType targetTypeIn,
                                            ParameterizedType methodTargetType,
                                            HiddenContentSelector hiddenContentSelectorOfTarget,
                                            boolean reverse,
                                            Integer indexOfDirectlyLinkedField) {
        assert sourceTypeIn != null;
        //   assert hiddenContentSelectorOfSource.compatibleWith(runtime, methodSourceType);
        //   assert sourceLvs.compatibleWith(hiddenContentSelectorOfSource);
        assert hiddenContentSelectorOfTarget.compatibleWith(runtime, methodTargetType);

        ParameterizedType sourceType = ensureTypeParameters(sourceTypeIn); // Pair -> Pair<Object, Object>
        assert targetTypeIn != null;
        ParameterizedType targetType = ensureTypeParameters(targetTypeIn);
        // RULE 1: no linking when the source is not linked or there is no transfer
        if (sourceLvs.isEmpty() || transferIndependent.isIndependent()) {
            return LinkedVariablesImpl.EMPTY;
        }
        if (hiddenContentSelectorOfTarget.isNone() && transferIndependent.isIndependentHc()) {
            LOGGER.warn("Impossible to have no knowledge of hidden content, and INDEPENDENT_HC: {}", methodInfo);
            return LinkedVariablesImpl.EMPTY;
        }
        Value.Immutable immutableOfSource = analysisHelper.typeImmutable(currentPrimaryType, sourceType);

        // RULE 3: immutable -> no link
        if (immutableOfSource.isImmutable()) {
            /*
             if the result type immutable because of a choice in type parameters, methodIndependent will return
             INDEPENDENT_HC, but the concrete type is deeply immutable
             */
            return LinkedVariablesImpl.EMPTY;
        }

        LinkedVariables lvFunctional = lvFunctional(transferIndependent, hiddenContentSelectorOfTarget, reverse,
                targetType, sourceType, immutableOfSource, sourceLvs, sourceIsVarArgs, indexOfDirectlyLinkedField);
        if (lvFunctional != null) return lvFunctional;

        Supplier<Map<Indices, HiddenContentSelector.IndicesAndType>> hctMethodToHctSourceSupplier =
                () -> hcsObject.translateHcs(runtime, genericsHelper, methodSourceType, sourceType, sourceIsVarArgs);

        Value.Immutable immutableOfFormalSource;
        if (sourceType.typeInfo() != null) {
            ParameterizedType formalSource = sourceType.typeInfo().asParameterizedType();
            immutableOfFormalSource = analysisHelper.typeImmutable(currentPrimaryType, formalSource);
            if (immutableOfFormalSource.isImmutable()) {
                return LinkedVariablesImpl.EMPTY;
            }
        } else {
            immutableOfFormalSource = immutableOfSource;
        }
        Value.Immutable immutable1 = analysisHelper.typeImmutable(currentPrimaryType, targetType);
        if (immutable1.isImmutable()) return LinkedVariablesImpl.EMPTY;
        Value.Immutable immutable2 = analysisHelper.typeImmutable(currentPrimaryType, methodTargetType);
        if (immutable2.isImmutable()) return LinkedVariablesImpl.EMPTY;
        try {
            return continueLinkedVariables(
                    hiddenContentSelectorOfSource,
                    sourceLvs, sourceIsVarArgs, transferIndependent, immutableOfFormalSource, targetType,
                    methodTargetType, hiddenContentSelectorOfTarget, hctMethodToHctSourceSupplier, reverse,
                    indexOfDirectlyLinkedField);
        } catch (RuntimeException re) {
            LOGGER.warn("Temporarily disabled exceptions, return EMPTY");
            return LinkedVariablesImpl.EMPTY;
        }
    }

    private LinkedVariables lvFunctional(Value.Independent transferIndependent,
                                         HiddenContentSelector hiddenContentSelectorOfTarget,
                                         boolean reverse,
                                         ParameterizedType targetType,
                                         ParameterizedType sourceType,
                                         Value.Immutable immutableOfSource,
                                         LinkedVariables sourceLvs,
                                         boolean sourceIsVarArgs,
                                         Integer indexOfDirectlyLinkedField) {

        // special code block for functional interfaces with both return value and parameters (i.e. variants
        // on Function<T,R>, BiFunction<T,S,R> etc. Not Consumers (no return value) nor Suppliers (no parameters))
        if (!hiddenContentSelectorOfTarget.isOnlyAll() || !transferIndependent.isIndependentHc()) {
            return null;
        }

        HiddenContentTypes hctContext = methodInfo.analysis().getOrDefault(HIDDEN_CONTENT_TYPES, NO_VALUE);
        HiddenContentSelector hcsTargetContext = HiddenContentSelector.selectAll(hctContext, targetType);
        HiddenContentSelector hcsSourceContext = HiddenContentSelector.selectAll(hctContext, sourceType);
        Set<Integer> set = new HashSet<>(hcsSourceContext.set());
        set.retainAll(hcsTargetContext.set());
        if (set.isEmpty()) {
            return null;
        }

        List<LinkedVariables> lvsList = new ArrayList<>();
        for (int index : set) {
            LOGGER.debug("Linked variables functional: do {} in hcs source {} overlapping with hcsTarget {}: {}", index,
                    hcsSourceContext, hcsTargetContext, set);
            Indices indices = hcsSourceContext.getMap().get(index);
            if (indices.containsSize2Plus()) {
                Indices newIndices = indices.size2PlusDropOne();
                Indices base = indices.first();
                HiddenContentSelector newHiddenContentSelectorOfSource
                        = new HiddenContentSelector(hctContext, Map.of(index, newIndices));
                ParameterizedType newSourceType = base.find(runtime, sourceType);
                Supplier<Map<Indices, HiddenContentSelector.IndicesAndType>> hctMethodToHctSourceSupplier =
                        () -> Map.of(newIndices, new HiddenContentSelector.IndicesAndType(newIndices, newSourceType));
                HiddenContentSelector newHcsTarget;
                ParameterizedType newTargetType;
                if (reverse && !targetType.isTypeParameter()) {
                    // List<T> as parameter
                    newHcsTarget = newHiddenContentSelectorOfSource;
                    newTargetType = newSourceType;
                } else if (!reverse && !targetType.isTypeParameter()) {
                    // List<T> as return type
                    newTargetType = targetType;
                    newHcsTarget = newHiddenContentSelectorOfSource;
                } else {
                    // object -> return
                    newHcsTarget = new HiddenContentSelector(hctContext, Map.of(index, ALL_INDICES));
                    newTargetType = targetType;
                }

                Value.Immutable immutable = analysisHelper.typeImmutable(currentPrimaryType, targetType);
                if (!immutable.isImmutable()) {
                    try {
                        LinkedVariables lvs = continueLinkedVariables(newHiddenContentSelectorOfSource,
                                sourceLvs, sourceIsVarArgs, transferIndependent, immutableOfSource,
                                newTargetType, newTargetType, newHcsTarget, hctMethodToHctSourceSupplier,
                                reverse, indexOfDirectlyLinkedField);
                        lvsList.add(lvs);
                    } catch (RuntimeException re) {
                        LOGGER.warn("Temporarily disabled exceptions; return empty linking");
                    }
                }
            }
        }
        if (!lvsList.isEmpty()) {
            return lvsList.stream().reduce(LinkedVariablesImpl.EMPTY, LinkedVariables::merge);
        }
        return null;
    }

    private LinkedVariables continueLinkedVariables(HiddenContentSelector hiddenContentSelectorOfSource,
                                                    LinkedVariables sourceLvs,
                                                    boolean sourceIsVarArgs,
                                                    Value.Independent transferIndependent,
                                                    Value.Immutable immutableOfFormalSource,
                                                    ParameterizedType targetType,
                                                    ParameterizedType methodTargetType,
                                                    HiddenContentSelector hiddenContentSelectorOfTarget,
                                                    Supplier<Map<Indices, IndicesAndType>> hctMethodToHctSourceSupplier,
                                                    boolean reverse,
                                                    Integer indexOfDirectlyLinkedField) {
        Map<Indices, HiddenContentSelector.IndicesAndType> hctMethodToHcsTarget = hiddenContentSelectorOfTarget
                .translateHcs(runtime, genericsHelper, methodTargetType, targetType, sourceIsVarArgs);
        LOGGER.debug("Linked variables: hcs method to hcs target: {}", hctMethodToHcsTarget);
        LOGGER.debug("Linked variables: hcs source: {}, target: {}", hiddenContentSelectorOfSource,
                hiddenContentSelectorOfTarget);
        Value.Independent correctedIndependent = correctIndependent(immutableOfFormalSource, transferIndependent,
                targetType, hiddenContentSelectorOfTarget, hctMethodToHcsTarget);

        if (correctedIndependent.isIndependent()) {
            return LinkedVariablesImpl.EMPTY;
        }

        Map<Indices, HiddenContentSelector.IndicesAndType> hctMethodToHctSource = hctMethodToHctSourceSupplier.get();
        Map<Variable, LV> newLinked = new HashMap<>();

        for (Map.Entry<Variable, LV> e : sourceLvs) {
            LOGGER.debug("Linked variables: do {} in sourceLvs {}", e, sourceLvs);

            ParameterizedType pt = e.getKey().parameterizedType();
            // for the purpose of this algorithm, unbound type parameters are HC
            Value.Immutable immutable = analysisHelper.typeImmutable(currentPrimaryType, pt);
            LV lv = e.getValue();
            assert lv.lt(LINK_INDEPENDENT);

            assert !immutable.isImmutable();

            if (hiddenContentSelectorOfTarget.isNone()) {
                LV prev = newLinked.put(e.getKey(), LINK_DEPENDENT);
                assert prev == null;
            } else {
                // from mine==target to theirs==source
                Map<Indices, Link> linkMap = new HashMap<>();

                Boolean correctForVarargsMutable = null;

                Set<Map.Entry<Integer, Indices>> entrySet = hiddenContentSelectorOfTarget.getMap().entrySet();
                for (Map.Entry<Integer, Indices> entry : entrySet) {
                    LOGGER.debug("Linked variables: do {} in entry set {}", entry, entrySet);
                    Indices indicesInTargetWrtMethod = entry.getValue();

                    HiddenContentSelector.IndicesAndType targetAndType = hctMethodToHcsTarget.get(indicesInTargetWrtMethod);
                    if (targetAndType == null) {
                        continue; // see TestConsumer
                    }
                    ParameterizedType type = targetAndType.type();
                    assert type != null;
                    Indices targetIndices = targetAndType.indices();

                    Value.Immutable typeImmutable = analysisHelper.typeImmutable(currentPrimaryType, type);
                    if (typeImmutable.isImmutable()) {
                        continue;
                    }
                    boolean mutable = typeImmutable.isMutable();
                    if (sourceIsVarArgs) {
                        // we're in a varargs situation: the first element is the type itself
                        correctForVarargsMutable = mutable;
                    }

                    /*
                    example: HCT of List (0=E). HCS source = 0(HC type E)->0(index in List's parameters)
                    entry:0->0
                     */
                    Indices indicesInSourceWrtMethod = hiddenContentSelectorOfSource.getMap().get(entry.getKey());
                    if (indicesInSourceWrtMethod == null) {
                        continue;
                    }
                    assert hctMethodToHctSource != null;
                    HiddenContentSelector.IndicesAndType indicesAndType = hctMethodToHctSource.get(indicesInSourceWrtMethod);
                    if (indicesAndType == null) {
                        continue;
                    }
                    Indices indicesInSourceWrtType = indicesAndType.indices();
                    if (indicesInSourceWrtType == null) {
                        continue; // see TestVarargs,3
                    }

                    // IMPROVE this feels rather arbitrary, see Linking_0P.reverse4 yet the 2nd clause seems needed for 1A.f10()
                    Indices indicesInTargetWrtType = (lv.theirsIsAll()
                                                      && entrySet.size() < hiddenContentSelectorOfTarget.getMap().size()
                                                      && reverse) ? ALL_INDICES : targetIndices;
                    Indices correctedIndicesInTargetWrtType;
                    if (correctForVarargsMutable != null) {
                        correctedIndicesInTargetWrtType = ALL_INDICES;
                    } else {
                        correctedIndicesInTargetWrtType = indicesInTargetWrtType;
                    }
                    assert correctedIndicesInTargetWrtType != null;
                    // see TestLinkToReturnValueMap,1(copy8) for an example of merging
                    if (!(correctedIndicesInTargetWrtType.isAll() && indicesInSourceWrtType.isAll())) {
                        linkMap.merge(correctedIndicesInTargetWrtType, new LinkImpl(indicesInSourceWrtType, mutable),
                                Link::merge);
                    }
                }

                boolean createDependentLink = immutable.isMutable() && isDependent(transferIndependent,
                        correctedIndependent, immutableOfFormalSource, lv);
                LV theLink;
                if (createDependentLink) {
                    if (linkMap.isEmpty()) {
                        theLink = LINK_DEPENDENT;
                    } else {
                        Links links = buildLinks(hiddenContentSelectorOfTarget, immutable, linkMap,
                                indexOfDirectlyLinkedField);
                        theLink = reverse ? LVImpl.createDependent(links.reverse()) : LVImpl.createDependent(links);
                    }
                } else if (!linkMap.isEmpty()) {
                    Links links = new LinksImpl(Map.copyOf(linkMap));
                    theLink = reverse ? LVImpl.createHC(links.reverse()) : LVImpl.createHC(links);
                } else {
                    theLink = null;
                }
                if (theLink != null) {
                    LV prev = newLinked.put(e.getKey(), theLink);
                    assert prev == null;
                }
            }
        }
        return LinkedVariablesImpl.of(newLinked);
    }

    /*
    special code to add the modificationArea objects in case of a Getter
     */
    private Links buildLinks(HiddenContentSelector hiddenContentSelectorOfTarget,
                             Immutable immutable,
                             Map<Indices, Link> linkMap,
                             Integer indexOfDirectlyLinkedField) {
        Indices modificationAreaSource;
        Indices modificationAreaTarget;
        if (immutable.isAtLeastImmutableHC()) {
            modificationAreaSource = IndicesImpl.NO_MODIFICATION_INDICES;
            modificationAreaTarget = IndicesImpl.NO_MODIFICATION_INDICES;
        } else {
            modificationAreaSource = ALL_INDICES;
            if (hiddenContentSelectorOfTarget.containsAll() && indexOfDirectlyLinkedField != null) {
                modificationAreaTarget = new IndicesImpl(indexOfDirectlyLinkedField);
            } else {
                modificationAreaTarget = ALL_INDICES;
            }
        }
        return new LinksImpl(Map.copyOf(linkMap), modificationAreaSource, modificationAreaTarget);
    }

    private boolean isDependent(Value.Independent transferIndependent,
                                Value.Independent correctedIndependent,
                                Value.Immutable immutableOfSource,
                                LV lv) {
        return
                // situation immutable(mutable), we'll have to override
                transferIndependent.isIndependentHc()
                && correctedIndependent.isDependent()
                ||
                // situation mutable(immutable), dependent method,
                transferIndependent.isDependent()
                && !lv.isCommonHC()
                && !immutableOfSource.isAtLeastImmutableHC();
    }
    
    /*
     Important: the last three parameters should form a consistent set, all computed with respect to the same
     formal type (targetType.typeInfo).
    
     First translate the HCS from the method target to the target!
     */

    private Value.Independent correctIndependent(Value.Immutable immutableOfSource,
                                                 Value.Independent independent,
                                                 ParameterizedType targetType,
                                                 HiddenContentSelector hiddenContentSelectorOfTarget,
                                                 Map<Indices, HiddenContentSelector.IndicesAndType> hctMethodToHcsTarget) {
        // immutableOfSource is not recursively immutable, independent is not fully independent
        // remaining values immutable: mutable, immutable HC
        // remaining values independent: dependent, independent hc
        if (independent.isDependent()) {
            if (immutableOfSource.isAtLeastImmutableHC()) {
                return INDEPENDENT_HC;
            }

            // if all types of the hcs are independent HC, then we can upgrade
            Map<Integer, Indices> selectorSet = hiddenContentSelectorOfTarget.getMap();
            boolean allIndependentHC = true;
            assert hctMethodToHcsTarget != null;
            for (Map.Entry<Indices, HiddenContentSelector.IndicesAndType> entry : hctMethodToHcsTarget.entrySet()) {
                if (selectorSet.containsValue(entry.getKey())) {
                    if (hiddenContentSelectorOfTarget.hiddenContentTypes().isExtensible(entry.getKey().single()) != null) {
                        return INDEPENDENT_HC;
                    }
                    Value.Immutable immutablePt = analysisHelper.typeImmutable(currentPrimaryType, entry.getValue().type());
                    if (!immutablePt.isAtLeastImmutableHC()) {
                        allIndependentHC = false;
                        break;
                    }
                }
            }
            if (allIndependentHC) {
                return INDEPENDENT_HC;
            }

        }
        if (independent.isIndependentHc()) {
            if (hiddenContentSelectorOfTarget.isOnlyAll()) {
                Value.Immutable immutablePt = analysisHelper.typeImmutable(currentPrimaryType, targetType);
                if (immutablePt.isImmutable()) {
                    return INDEPENDENT;
                }
            } else {
                assert !hiddenContentSelectorOfTarget.isNone();
            }
        }
        return independent;
    }

    public void crossLink(LinkedVariables linkedVariablesOfObject,
                          LinkedVariables linkedVariablesOfObjectFromParams,
                          EvaluationResult.Builder link) {
        linkedVariablesOfObject.stream().forEach(e ->
                linkedVariablesOfObjectFromParams.stream().forEach(e2 -> {
                    Variable from = e.getKey();
                    Variable to = e2.getKey();
                    LV fromLv = e.getValue();
                    LV toLv = e2.getValue();
                    LV lv = follow(fromLv, toLv);
                    if (lv != null) {
                        link.merge(from, LinkedVariablesImpl.of(to, lv));
                    }
                })
        );
    }

    private static LV follow(LV fromLv, LV toLv) {
        boolean fromLvHaveLinks = fromLv.haveLinks();
        boolean toLvHaveLinks = toLv.haveLinks();
        if (!fromLvHaveLinks && !toLvHaveLinks) {
            return fromLv.max(toLv); // -0- and -1-, -1- and -2-
        }
        if (fromLv.isStaticallyAssignedOrAssigned()) {
            return toLv; // -0- 0-4-1
        }
        if (toLv.isStaticallyAssignedOrAssigned()) {
            return fromLv; // 1-2-1 -1-
        }
        if (fromLvHaveLinks && toLvHaveLinks) {
            boolean fromLvMineIsAll = fromLv.mineIsAll();
            boolean toLvMineIsAll = toLv.mineIsAll();
            if (fromLvMineIsAll && !toLvMineIsAll) {
                return fromLv.reverse();
            }
            if (toLvMineIsAll) {  //X-Y *-Z
                if (!fromLvMineIsAll) return toLv;
                // *-Y *-Z
                return LVImpl.createHC(fromLv.links().theirsToTheirs(toLv.links()));
            }
            boolean fromLvTheirsIsAll = fromLv.theirsIsAll();
            boolean toLvTheirsIsAll = toLv.theirsIsAll();
            if (fromLvTheirsIsAll && !toLvTheirsIsAll) {
                return LVImpl.createHC(toLv.links().theirsToTheirs(fromLv.links()));
            }
            if (toLvTheirsIsAll && fromLvTheirsIsAll) {
                return null;
            }
            return LVImpl.createHC(fromLv.links().mineToTheirs(toLv.links()));
        }
        if (fromLv.isDependent() == toLv.isDependent()) {
            return fromLv;
        }
        return null;
    }
}

