package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.*;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.Diamond;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.This;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.inspection.api.parser.*;
import org.e2immu.support.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.api.parser.TypeContext.SUBTYPE_HIERARCHY_IN_CONSTRUCTOR_PRIORITY;
import static org.e2immu.language.inspection.impl.parser.ListMethodAndConstructorCandidates.IGNORE_PARAMETER_NUMBERS;

public class MethodResolutionImpl implements MethodResolution {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodResolutionImpl.class);
    private final Runtime runtime;
    private final GenericsHelper genericsHelper;
    private final HierarchyHelper hierarchyHelper;
    private final int notAssignable;

    public MethodResolutionImpl(Runtime runtime) {
        this.runtime = runtime;
        this.genericsHelper = new GenericsHelperImpl(runtime);
        this.hierarchyHelper = new HierarchyHelperImpl();
        notAssignable = runtime.isNotAssignable();
    }


    @Override
    public Set<ParameterizedType> computeScope(Context context,
                                               String index,
                                               String methodName,
                                               Object unparsedScope,
                                               List<Object> unparsedArguments) {
        // we must create it here, because the importMap only exists once we're parsing a compilation unit
        ListMethodAndConstructorCandidates list = new ListMethodAndConstructorCandidates(runtime,
                context.typeContext().importMap());
        ListMethodAndConstructorCandidates.Scope scope = list
                .computeScope(context.parseHelper(), context, index, unparsedScope, TypeParameterMap.EMPTY);
        int numArguments = unparsedArguments.size();
        Map<MethodTypeParameterMap, Integer> methodCandidates = initialMethodCandidates(list, scope, numArguments,
                methodName);

        FilterResult filterResult = filterMethodCandidatesInErasureMode(context, methodCandidates, unparsedArguments);
        if (methodCandidates.size() > 1) {
            trimMethodsWithBestScore(methodCandidates, filterResult.compatibilityScore);
            if (methodCandidates.size() > 1) {
                trimMethodsByCandidateScore(methodCandidates);
                if (methodCandidates.size() > 1) {
                    trimVarargsVsMethodsWithFewerParameters(methodCandidates);
                }
            }
        }
        sortRemainingCandidatesByShallowPublic(methodCandidates);

        if (methodCandidates.isEmpty()) {
            throw new RuntimeException("Have no method candidates remaining in erasure mode for "
                                       + methodName + ", " + numArguments);
        }

        Set<ParameterizedType> types;
        if (scope.expression() != null
            && !(scope.expression() instanceof ErasedExpression)
            && scope.expression().parameterizedType().arrays() > 0
            && "clone".equals(methodName)) {
            /* this condition is hyper-specialized (see MethodCall_54; but the alternative would be to return JLO,
               and that causes problems along the way
             */
            types = Set.of(scope.expression().parameterizedType());
        } else {
            types = methodCandidates.keySet().stream()
                    .map(mc -> erasureReturnType(mc, filterResult, scope))
                    .collect(Collectors.toUnmodifiableSet());
        }
        return types;
    }

    private record ContextAndScope(Context context, Expression scope) {
    }


    private static ContextAndScope determineTypeContextAndScope(Context context, String index, Object unparsedScope) {
        Context newContext;
        Expression scope;
        if (unparsedScope != null) {
            scope = context.parseHelper().parseExpression(context, index, context.emptyForwardType(), unparsedScope);
            newContext = context.newTypeContext();
            TypeInfo bestType = scope.parameterizedType().bestTypeInfo();
            if (bestType != null) {
                for (TypeInfo sub : bestType.subTypes()) {
                    newContext.typeContext().addToContext(sub, SUBTYPE_HIERARCHY_IN_CONSTRUCTOR_PRIORITY);
                }
                // TODO are there other things we should add to this context??
            }
        } else {
            newContext = context;
            scope = null;
        }
        return new ContextAndScope(newContext, scope);
    }

    @Override
    public Expression resolveConstructor(Context contextIn,
                                         List<Comment> comments,
                                         Source source,
                                         String index,
                                         ParameterizedType formalType,
                                         ParameterizedType expectedConcreteType,
                                         Diamond diamond,
                                         Object unparsedObject,
                                         Source unparsedObjectSource,
                                         List<Object> unparsedArguments,
                                         List<ParameterizedType> methodTypeArguments,
                                         boolean complain,
                                         boolean useObjectForUndefinedTypeParameters) {
        ContextAndScope cas = determineTypeContextAndScope(contextIn, index, unparsedObject);
        Context context = cas.context;
        ListMethodAndConstructorCandidates list = new ListMethodAndConstructorCandidates(runtime,
                context.typeContext().importMap());
        Map<NamedType, ParameterizedType> typeMap = expectedConcreteType == null ? Map.of() :
                expectedConcreteType.initialTypeParameterMap();
        TypeParameterMap typeParameterMap = new TypeParameterMap(typeMap);
        Map<MethodTypeParameterMap, Integer> candidates = list.resolveConstructor(formalType, expectedConcreteType,
                unparsedArguments.size(), typeMap);
        Candidate candidate = chooseCandidateAndEvaluateCall(context, index, MethodInfo.CONSTRUCTOR_NAME,
                methodTypeArguments, candidates, unparsedArguments, formalType, typeParameterMap, complain);
        if (candidate == null) {
            if (complain) {
                throw new UnsupportedOperationException("No candidate for constructor, "
                                                        + unparsedArguments.size() + " args, formal type " + formalType);
            }
            return null;
        }

        ParameterizedType finalParameterizedType1;
        if (formalType.hasTypeParameters()) {
            // there's only one method left, so we can derive the parameterized type from the parameters
            Set<ParameterizedType> typeParametersResolved = new HashSet<>(formalType.parameters());
            finalParameterizedType1 = tryToResolveTypeParameters(formalType, candidate.method(),
                    typeParametersResolved, candidate.newParameterExpressions(), useObjectForUndefinedTypeParameters,
                    typeMap);
        } else {
            finalParameterizedType1 = expectedConcreteType;
        }
        ParameterizedType finalParameterizedType = Objects.requireNonNullElseGet(finalParameterizedType1,
                () -> expectedConcreteType == null ? formalType.withParameters(List.of()) : expectedConcreteType);

        // IMPORTANT: every newly created object is different from each other, UNLESS we're a record, then
        // we can check the constructors... See EqualityMode
        return runtime.newConstructorCallBuilder()
                .setConstructor(candidate.method.methodInfo())
                .setObject(cas.scope)
                .setDiamond(diamond)
                .setConcreteReturnType(finalParameterizedType)
                .setParameterExpressions(candidate.newParameterExpressions)
                .setTypeArguments(methodTypeArguments)
                .setSource(source)
                .addComments(comments)
                .build();
    }

    private ParameterizedType tryToResolveTypeParameters(ParameterizedType formalType,
                                                         MethodTypeParameterMap method,
                                                         Set<ParameterizedType> typeParametersResolved,
                                                         List<Expression> newParameterExpressions,
                                                         boolean useObjectForUnresolvedTypeParameters,
                                                         Map<NamedType, ParameterizedType> expectedTypeMap) {
        int i = 0;
        Map<NamedType, ParameterizedType> map = new HashMap<>();
        for (Expression parameterExpression : newParameterExpressions) {
            ParameterizedType formalParameterType = method.methodInfo().typeOfParameterHandleVarargs(i++);
            ParameterizedType concreteArgumentType = parameterExpression.parameterizedType();
            tryToResolveTypeParametersBasedOnOneParameter(formalParameterType, concreteArgumentType, map);
            typeParametersResolved.removeIf(pt -> map.containsKey(pt.typeParameter()));
            if (typeParametersResolved.isEmpty()) {
                List<ParameterizedType> concreteParameters = formalType.parameters().stream()
                        .map(pt -> map.getOrDefault(pt.typeParameter(), pt))
                        .map(pt -> pt.ensureBoxed(runtime))
                        .toList();
                return runtime.newParameterizedType(formalType.typeInfo(), concreteParameters);
            }
        }
        if (useObjectForUnresolvedTypeParameters && !typeParametersResolved.isEmpty() && !expectedTypeMap.isEmpty()) {
            List<ParameterizedType> concreteParameters = formalType.parameters().stream()
                    .map(pt -> {
                        if (pt.typeParameter() != null)
                            return expectedTypeMap.getOrDefault(pt.typeParameter(), runtime.objectParameterizedType());
                        return runtime.objectParameterizedType();
                    }).toList();
            // FIXME this does not do the correct recursions; we should translate "unknown" to "Object"
            return runtime.newParameterizedType(formalType.typeInfo(), concreteParameters);
        }
        return null; // solved later
    }

// concreteType Collection<X>, formalType Collection<E>, with E being the parameter in HashSet<E> which implements Collection<E>
// add E -> X to the map
// we need the intermediate step to original because the result of translateMap contains E=#0 in Collection

    private void tryToResolveTypeParametersBasedOnOneParameter(ParameterizedType formalType,
                                                               ParameterizedType concreteType,
                                                               Map<NamedType, ParameterizedType> mapAll) {
        if (formalType.typeParameter() != null) {
            mapAll.put(formalType.typeParameter(), concreteType);
            return;
        }
        if (formalType.typeInfo() != null) {
            Map<NamedType, ParameterizedType> map = genericsHelper.translateMap(formalType, concreteType,
                    true);
            mapAll.putAll(map);
            map.forEach((namedType, pt) -> {
                if (namedType instanceof TypeParameter tp &&
                    tp.getOwner().isLeft() && formalType.typeInfo().equals(tp.getOwner().getLeft())) {
                    ParameterizedType original = formalType.parameters().get(tp.getIndex());
                    if (original.typeParameter() != null) {
                        mapAll.put(original.typeParameter(), pt);
                    }
                }
            });
            return;
        }
        throw new UnsupportedOperationException("?");
    }

    @Override
    public Expression resolveMethod(Context context,
                                    List<Comment> comments,
                                    Source source,
                                    Source sourceOfName,
                                    String index,
                                    ForwardType forwardType,
                                    String methodName,
                                    Object unparsedScope,
                                    Source unparsedScopeSource,
                                    List<ParameterizedType> methodTypeArguments,
                                    DetailedSources.Builder typeArgumentsDetailedSources,
                                    List<Object> unparsedArguments) {
        // we must create it here, because the importMap only exists once we're parsing a compilation unit
        ListMethodAndConstructorCandidates list = new ListMethodAndConstructorCandidates(runtime,
                context.typeContext().importMap());
        ListMethodAndConstructorCandidates.Scope scope = list
                .computeScope(context.parseHelper(), context, index, unparsedScope, TypeParameterMap.EMPTY);
        int numArguments = unparsedArguments.size();
        Map<MethodTypeParameterMap, Integer> methodCandidates = initialMethodCandidates(list, scope, numArguments,
                methodName);
        if (methodCandidates.isEmpty()) {
            throw new Summary.ParseException(context,
                    "No method candidates for " + methodName + ", " + numArguments + " arg(s)");
        }
        TypeParameterMap extra = forwardType.extra().merge(scope.typeParameterMap());
        Candidate candidate = chooseCandidateAndEvaluateCall(context, index, methodName, methodTypeArguments,
                methodCandidates, unparsedArguments, forwardType.type(), extra, true);

        if (candidate == null) {
            throw new Summary.ParseException(context, "Failed to find a unique method candidate");
        }
        MethodInfo resolvedMethod = candidate.method.methodInfo();
        //LOGGER.info("Resulting method is {}, type params {}", resolvedMethod, resolvedMethod.typeParameters()
        //        .stream().map(TypeParameter::toStringWithTypeBounds).collect(Collectors.joining(", ")));

        boolean scopeIsThis = scope.expression() instanceof VariableExpression ve && ve.variable() instanceof This;
        Expression newScope;
        if (scope.expression() != null && containsErasedExpressions(scope.expression())) {
            TypeParameterMap tpm = extra.merge(new TypeParameterMap(candidate.mapExpansion));
            newScope = reEvaluateErasedScope(context, index, scope.expression(), unparsedScope, tpm);
        } else {
            newScope = scope.ensureExplicit(runtime, hierarchyHelper, resolvedMethod,
                    scopeIsThis, context, context.enclosingType(), unparsedScopeSource);
        }
        if (containsErasedExpressions(newScope)) {
            throw new UnsupportedOperationException("Scope still contains erased expressions");
        }
        //LOGGER.info("- Type's type parameters {}", resolvedMethod.typeInfo().typeParameters().stream()
        //        .map(TypeParameter::toStringWithTypeBounds).collect(Collectors.joining(", ")));
        //LOGGER.info("- Evaluated scope is {}, type {}, extra {}", newScope, newScope.parameterizedType().detailedString(),
        //        extra.map());
        ParameterizedType returnType = candidate.returnType(runtime, context.enclosingType().primaryType(), extra);
        //LOGGER.info("- Concrete return type of {} is {}", methodName, returnType.detailedString());

        if (typeArgumentsDetailedSources != null) {
            typeArgumentsDetailedSources.put(resolvedMethod.name(), sourceOfName);
        }
        return runtime.newMethodCallBuilder()
                .setSource(typeArgumentsDetailedSources != null
                        ? source.withDetailedSources(typeArgumentsDetailedSources.build()) : source)
                .addComments(comments)
                .setObjectIsImplicit(scope.objectIsImplicit())
                .setObject(newScope)
                .setMethodInfo(resolvedMethod)
                .setConcreteReturnType(returnType)
                .setTypeArguments(methodTypeArguments)
                .setParameterExpressions(candidate.newParameterExpressions).build();
    }

    record Candidate(List<Expression> newParameterExpressions,
                     Map<NamedType, ParameterizedType> mapExpansion,
                     MethodTypeParameterMap method) {

        ParameterizedType returnType(Runtime runtime,
                                     TypeInfo primaryType,
                                     TypeParameterMap extra) {
            //LOGGER.info(" - mapExpansion {}", mapExpansion);
            //LOGGER.info(" - method concrete type map {}", method.concreteTypes());
            ParameterizedType pt;
            if (mapExpansion.isEmpty()) {
                pt = method.getConcreteReturnType(runtime);
            } else {
                MethodTypeParameterMap expand = method.expand(runtime, primaryType, mapExpansion);
                //LOGGER.info(" - expand = {}", expand);
                pt = expand.getConcreteReturnType(runtime);
            }
            ParameterizedType withExtra = pt.applyTranslation(runtime, extra.map());
            // See TypeParameter_4
            //LOGGER.info(" - withExtra: {}", withExtra.detailedString());
            return withExtra.isUnboundWildcard() ? runtime.objectParameterizedType() : withExtra;
        }
    }

    private Map<MethodTypeParameterMap, Integer> initialMethodCandidates(ListMethodAndConstructorCandidates list,
                                                                         ListMethodAndConstructorCandidates.Scope scope,
                                                                         int numArguments,
                                                                         String methodName) {
        Map<MethodTypeParameterMap, Integer> methodCandidates = new HashMap<>();
        list.recursivelyResolveOverloadedMethods(scope.type(), methodName,
                numArguments, false, scope.typeParameterMap().map(), methodCandidates,
                scope.nature());
        if (methodCandidates.isEmpty()) {
            throw new RuntimeException("No candidates at all for method name " + methodName + ", "
                                       + numArguments + " args in type " + scope.type().detailedString());
        }
        return methodCandidates;
    }

    private ParameterizedType erasureReturnType(MethodTypeParameterMap mc, FilterResult filterResult,
                                                ListMethodAndConstructorCandidates.Scope scope) {
        MethodInfo candidate = mc.methodInfo();
        TypeParameterMap map0 = filterResult.typeParameterMap(candidate);
        TypeParameterMap map1 = map0.merge(scope.typeParameterMap());
        TypeInfo methodType = candidate.typeInfo();
        TypeInfo scopeType = scope.type().bestTypeInfo();
        TypeParameterMap merged;
        if (scopeType != null && !methodType.equals(scopeType)) {
            // method is defined in a super-type, so we need an additional translation
            ParameterizedType superType = methodType.asParameterizedType();
            Map<NamedType, ParameterizedType> sm = genericsHelper.mapInTermsOfParametersOfSuperType(scopeType, superType);
            merged = sm == null ? map1 : map1.merge(new TypeParameterMap(sm));
        } else {
            merged = map1;
        }
        ParameterizedType returnType = candidate.returnType();
        Map<NamedType, ParameterizedType> map2 = merged.map();
        // IMPROVE at some point, compare to mc.method().concreteType; redundant code?
        return returnType.applyTranslation(runtime, map2);
    }


    Candidate chooseCandidateAndEvaluateCall(Context context,
                                             String index,
                                             String methodName,
                                             List<ParameterizedType> methodTypeArguments,
                                             Map<MethodTypeParameterMap, Integer> methodCandidates,
                                             List<Object> unparsedArguments,
                                             ParameterizedType returnType,
                                             TypeParameterMap extra,
                                             boolean complain) {

        Map<Integer, Expression> evaluatedExpressions = new TreeMap<>();
        int i = 0;
        ForwardType forward = context.erasureForwardType();
        for (Object argument : unparsedArguments) {
            Expression evaluated = context.resolver().parseHelper().parseExpression(context, index, forward, argument);
            evaluatedExpressions.put(i++, evaluated);
        }

        FilterResult filterResult = filterCandidatesByParameters(methodCandidates, evaluatedExpressions, extra);

        // now we need to ensure that there is only 1 method left, but, there can be overloads and
        // methods with implicit type conversions, varargs, etc. etc.
        if (methodCandidates.isEmpty()) {
            if (complain) {
                noCandidatesError(context.enclosingType(), methodName, filterResult.evaluatedExpressions);
            }
            return null;
        }

        // DISTANCE IN THE HIERARCHY
        if (methodCandidates.size() > 1) {
            trimMethodsWithBestScore(methodCandidates, filterResult.compatibilityScore);
        }
        // equal distance: most specific return type
        if (methodCandidates.size() > 1) {
            trimMethodsKeepMostSpecificReturnType(context.enclosingType().primaryType(), methodCandidates);
        }
        // return type of erased lambdas
        if (methodCandidates.size() > 1) {
            trimMethodsByReevaluatingErasedParameterExpressions(context, index, filterResult.evaluatedExpressions,
                    unparsedArguments, methodCandidates, returnType, extra);
        }
        // varargs vs single element
        if (methodCandidates.size() > 1) {
            trimVarargsVsMethodsWithFewerParameters(methodCandidates);
        }
        List<MethodTypeParameterMap> sorted = sortRemainingCandidatesByShallowPublic(methodCandidates);
        if (sorted.size() > 1) {
            multipleCandidatesError(methodName, methodCandidates, filterResult.evaluatedExpressions);
        }
        MethodTypeParameterMap method = sorted.getFirst();
        LOGGER.debug("Found method {}", method.methodInfo());

        TypeParameterMap extra2 = methodTypeArguments.isEmpty() ? extra :
                extra.merge(makeMethodTypeParameterMap(method.methodInfo(), methodTypeArguments));

        List<Expression> newParameterExpressions = reEvaluateErasedExpression(context, index, unparsedArguments,
                returnType, extra2, methodName, filterResult.evaluatedExpressions, method);
        Map<NamedType, ParameterizedType> mapExpansion = computeMapExpansion(method, newParameterExpressions, returnType);
        return new Candidate(newParameterExpressions, mapExpansion, method);
    }

    private TypeParameterMap makeMethodTypeParameterMap(MethodInfo methodInfo, List<ParameterizedType> methodTypeArguments) {
        int i = 0;
        Map<NamedType, ParameterizedType> map = new HashMap<>();
        for (TypeParameter typeParameter : methodInfo.typeParameters()) {
            if (i < methodTypeArguments.size()) {
                map.put(typeParameter, methodTypeArguments.get(i));
            }
            ++i;
        }
        return new TypeParameterMap(map);
    }

    private void trimMethodsByReevaluatingErasedParameterExpressions(Context context,
                                                                     String index,
                                                                     Map<Integer, Expression> evaluatedExpressions,
                                                                     List<Object> unparsedArguments,
                                                                     Map<MethodTypeParameterMap, Integer> methodCandidates,
                                                                     ParameterizedType outsideContext,
                                                                     TypeParameterMap extra) {
        List<Integer> erased = new ArrayList<>(unparsedArguments.size());
        for (Map.Entry<Integer, Expression> entry : evaluatedExpressions.entrySet()) {
            if (entry.getValue() instanceof ErasedExpression) {
                erased.add(entry.getKey());
            }
        }
        if (erased.isEmpty()) return; // there are no erased expressions to re-evaluate
        Map<MethodTypeParameterMap, Integer> scores = new HashMap<>();
        int bestScore = Integer.MAX_VALUE;
        for (MethodTypeParameterMap method : methodCandidates.keySet()) {
            int score = computeScoreForReevaluationOfErasedParameterExpressions(context, index, unparsedArguments,
                    evaluatedExpressions, method, erased, outsideContext, extra);
            scores.put(method, score);
            if (score >= 0 && score < bestScore) bestScore = score;
        }
        LOGGER.debug("Best score is {}", bestScore);
        if (bestScore != Integer.MAX_VALUE) {
            int finalBestScore = bestScore;
            scores.forEach((method, score) -> {
                if (score < 0 || score > finalBestScore) methodCandidates.remove(method);
            });
            LOGGER.debug("Remaining: {}", methodCandidates.size());
        }
    }

    // this is pretty expensive code, but should be rarely used. See TestMethodCall8,assertDoesNotThrow
    private int computeScoreForReevaluationOfErasedParameterExpressions(Context context,
                                                                        String index,
                                                                        List<Object> unparsedArguments,
                                                                        Map<Integer, Expression> evaluatedExpressions,
                                                                        MethodTypeParameterMap method,
                                                                        List<Integer> erased,
                                                                        ParameterizedType outsideContext,
                                                                        TypeParameterMap extra) {
        int score = 0;
        TypeParameterMap cumulative = extra;
        List<ParameterInfo> parameters = method.methodInfo().parameters();
        for (int i : erased) {
            LOGGER.debug("Reevaluating erased expression for score computation, {}, pos {}", method.methodInfo(), i);
            Expression evEx = evaluatedExpressions.get(i);
            ReEval reEval = reevaluateParameterExpression(context, index, unparsedArguments, outsideContext,
                    method.methodInfo().name(), evEx, method, i, cumulative, parameters);
            cumulative = reEval.typeParameterMap;
            ParameterizedType pt;
            if (i >= parameters.size()) {
                ParameterInfo parameterInfo = parameters.getLast();
                assert parameterInfo.isVarArgs();
                pt = parameterInfo.parameterizedType().copyWithOneFewerArrays();
            } else {
                ParameterInfo parameterInfo = parameters.get(i);
                pt = parameterInfo.parameterizedType();
            }
            score += compatibleParameter(reEval.evaluated, pt);
        }
        return score;
    }

    private void multipleCandidatesError(String methodName,
                                         Map<MethodTypeParameterMap, Integer> methodCandidates,
                                         Map<Integer, Expression> evaluatedExpressions) {
        LOGGER.error("Multiple candidates for {}", methodName);
        methodCandidates.forEach((m, d) -> LOGGER.error(" -- {}", m.methodInfo()));
        LOGGER.error("{} Evaluated expressions:", evaluatedExpressions.size());
        evaluatedExpressions.forEach((i, e) -> LOGGER.error(" -- index {}: {}, {}, {}", i, e, e.getClass(),
                e instanceof ErasedExpression ? "-" : e.parameterizedType().toString()));
        throw new UnsupportedOperationException("Multiple candidates");
    }

    private Map<NamedType, ParameterizedType> computeMapExpansion(MethodTypeParameterMap method,
                                                                  List<Expression> newParameterExpressions,
                                                                  ParameterizedType forwardedReturnType) {
        Map<NamedType, ParameterizedType> mapExpansion = new HashMap<>();
        // fill in the map expansion, deal with variable arguments!
        int i = 0;
        List<ParameterInfo> formalParameters = method.methodInfo().parameters();

        for (Expression expression : newParameterExpressions) {
            LOGGER.debug("Examine parameter {}", i);
            ParameterizedType concreteParameterType;
            ParameterizedType formalParameterType;
            ParameterInfo formalParameter = formalParameters.get(i);
            if (formalParameters.size() - 1 == i && formalParameter.isVarArgs()) {
                formalParameterType = formalParameter.parameterizedType().copyWithOneFewerArrays();
                if (newParameterExpressions.size() > formalParameters.size()
                    || formalParameterType.arrays() == expression.parameterizedType().arrays()) {
                    concreteParameterType = expression.parameterizedType();
                } else {
                    concreteParameterType = expression.parameterizedType().copyWithOneFewerArrays();
                    assert formalParameterType.isAssignableFrom(runtime, concreteParameterType);
                }
            } else {
                formalParameterType = formalParameters.get(i).parameterizedType();
                concreteParameterType = expression.parameterizedType();
            }
            Map<NamedType, ParameterizedType> translated = genericsHelper.translateMap(formalParameterType,
                    concreteParameterType, true);
            ParameterizedType concreteTypeInMethod = method.getConcreteTypeOfParameter(runtime, i);

            if (concreteTypeInMethod.typeInfo() != null
                && concreteParameterType.typeInfo() == concreteTypeInMethod.typeInfo()) {
                // the following code should run at the level of the respective type parameters... See TestTranslate,1
                // FIXME should we make this recursive? what if the type parameters only start another level down?
                for (int j = 0; j < formalParameterType.parameters().size(); ++j) {
                    ParameterizedType ctim = concreteTypeInMethod.parameters().get(j);
                    translate(translated, ctim, mapExpansion);
                }
            } else {
                translate(translated, concreteTypeInMethod, mapExpansion);
            }
            i++;
            if (i >= formalParameters.size()) break; // varargs... we have more than there are
        }

        // finally, look at the return type
        ParameterizedType formalReturnType = method.methodInfo().returnType();
        if (forwardedReturnType != null) {
            // see MethodCall11,4; we want Boolean to override the forward type 'int'
            mapExpansion.putAll(method.concreteTypes());
            Map<NamedType, ParameterizedType> map = formalReturnType.formalToConcrete(forwardedReturnType);
            // see TestMethodCall0,3 for the "ifAbsent" aspect; TestVar,1 for the put.
            // FIXME it is not immediately clear to me why 2 successive genericsHelper.translateMap calls don't work
            map.forEach(mapExpansion::putIfAbsent);
        }

        return mapExpansion;
    }

    private static void translate(Map<NamedType, ParameterizedType> translated,
                                  ParameterizedType concreteTypeInMethod,
                                  Map<NamedType, ParameterizedType> mapExpansion) {
        translated.forEach((k, v) -> {
            // we can go in two directions here.
            // either the type parameter gets a proper value by the concreteParameterType, or the concreteParameter type should
            // agree with the concrete types map in the method candidate.
            // It is quite possible that concreteParameterType == ParameterizedType.NULL,
            // and then the value in the map should prevail
            ParameterizedType valueToAdd;
            if (betterDefinedThan(concreteTypeInMethod, v)) {
                valueToAdd = concreteTypeInMethod;
            } else {
                valueToAdd = v;
            }
            // Example: Ecoll -> String, in case the formal parameter was Collection<E>, and the concrete Set<String>
            if (!mapExpansion.containsKey(k)) {
                mapExpansion.put(k, valueToAdd);
            }
        });
    }

    private static boolean betterDefinedThan(ParameterizedType pt1, ParameterizedType pt2) {
        return (pt1.typeParameter() != null || pt1.typeInfo() != null) && pt2.typeParameter() == null && pt2.typeInfo() == null;
    }


    /*
    see TestVar, methodExplicit(I i). The call to 'collect' in statement 1. Extra maps T->J; this information
    must be passed on!
     */
    private List<Expression> reEvaluateErasedExpression(Context context,
                                                        String index,
                                                        List<Object> expressions,
                                                        ParameterizedType outsideContext,
                                                        TypeParameterMap extra,
                                                        String methodName,
                                                        Map<Integer, Expression> evaluatedExpressions,
                                                        MethodTypeParameterMap method) {
        Expression[] newParameterExpressions = new Expression[evaluatedExpressions.size()];
        TypeParameterMap cumulative = extra;
        List<Integer> positionsToDo = new ArrayList<>(evaluatedExpressions.size());
        List<ParameterInfo> parameters = method.methodInfo().parameters();

        for (int i = 0; i < expressions.size(); i++) {
            Expression e = evaluatedExpressions.get(i);
            assert e != null;
            if (containsErasedExpressions(e)) {
                positionsToDo.add(i);
            } else {
                newParameterExpressions[i] = e;
                if (!(e instanceof NullConstant)) {
                    Map<NamedType, ParameterizedType> learned = e.parameterizedType().initialTypeParameterMap();
                    ParameterizedType formal = i < parameters.size() ? parameters.get(i).parameterizedType() :
                            parameters.getLast().parameterizedType().copyWithOneFewerArrays();
                    Map<NamedType, ParameterizedType> inMethod = formal.forwardTypeParameterMap();
                    Map<NamedType, ParameterizedType> combined = genericsHelper.combineMaps(learned, inMethod);
                    if (!combined.isEmpty()) {
                        cumulative = cumulative.merge(new TypeParameterMap(combined));
                    }
                    if (formal.typeParameter() != null) {
                        Map<NamedType, ParameterizedType> map = Map.of(formal.typeParameter(), e.parameterizedType().copyWithoutArrays());
                        cumulative = cumulative.merge(new TypeParameterMap(map));
                    }
                }
            }
        }

        // FIXME we're doing this in order now, but we may have to iterate until all solved
        //  but then we'd have to know what 'solved' means
        for (int i : positionsToDo) {
            Expression e = evaluatedExpressions.get(i);
            ReEval reEval = reevaluateParameterExpression(context, index, expressions, outsideContext, methodName,
                    e, method, i, cumulative, parameters);
            cumulative = reEval.typeParameterMap;
            newParameterExpressions[i] = reEval.evaluated;
        }
        return Arrays.stream(newParameterExpressions).toList();
    }

    private record ReEval(TypeParameterMap typeParameterMap, Expression evaluated) {
    }

    private ReEval reevaluateParameterExpression(Context context,
                                                 String index,
                                                 List<Object> expressions,
                                                 ParameterizedType outsideContext,
                                                 String methodName,
                                                 Expression e,
                                                 MethodTypeParameterMap method,
                                                 int paramIndex,
                                                 TypeParameterMap cumulativeIn,
                                                 List<ParameterInfo> parameters) {
        assert e != null;

        LOGGER.debug("Reevaluating erased expression on {}, pos {}", methodName, paramIndex);
        TypeParameterMap cumulative = cumulativeIn;
        ForwardType newForward = determineForwardReturnTypeInfo(method, paramIndex, outsideContext, cumulative);

        Expression reParsed = context.resolver().parseHelper().parseExpression(context, index, newForward,
                expressions.get(paramIndex));
        if (containsErasedExpressions(reParsed)) {
            throw new UnsupportedOperationException("Argument at position " + paramIndex + " contains erased expressions");
        }

        ParameterInfo pi = parameters.get(Math.min(paramIndex, parameters.size() - 1));
        ParameterizedType modifiedReparsedType = reParsed instanceof Lambda lambda ? lambda.concreteFunctionalType() : reParsed.parameterizedType();
        try {
            if (pi.parameterizedType().hasTypeParameters()) {
                Map<NamedType, ParameterizedType> learned = genericsHelper.translateMap(pi.parameterizedType(),
                        modifiedReparsedType, true);
                if (!learned.isEmpty()) {
                    cumulative = cumulative.merge(new TypeParameterMap(learned));
                }

                // try to reconcile the type parameters with the ones in reParsed, see Lambda_16
                Map<NamedType, ParameterizedType> forward = pi.parameterizedType().initialTypeParameterMap();
                if (!forward.isEmpty()) {
                    cumulative = cumulative.merge(new TypeParameterMap(forward));
                }
            }
        } catch (RuntimeException re) {
            LOGGER.error("Caught exception re-evaluating erased expression, pi = {}, type {}", pi, pi.parameterizedType());
            LOGGER.error("reParsed = {}, type {}", reParsed, modifiedReparsedType);
            throw re;
        }
        return new ReEval(cumulative, reParsed);
    }

    private Expression reEvaluateErasedScope(Context context, String index, Expression expressionWithErasedType,
                                             Object constuctorCallObject,
                                             TypeParameterMap tpm) {
        ForwardType newForward;
        ParameterizedType parameterizedType = expressionWithErasedType.parameterizedType();
        if (tpm.map().isEmpty()) {
            newForward = new ForwardTypeImpl(parameterizedType, false, tpm);
        } else {
            ParameterizedType translated = parameterizedType.applyTranslation(runtime, tpm.map());
            newForward = new ForwardTypeImpl(translated, false, tpm);
        }
        return context.resolver().parseHelper().parseExpression(context, index, newForward, constuctorCallObject);
    }


    private void noCandidatesError(TypeInfo typeInfo,
                                   String methodName,
                                   Map<Integer, Expression> evaluatedExpressions) {
        if (!evaluatedExpressions.isEmpty()) {
            LOGGER.error("Evaluated expressions for {}: ", methodName);
            evaluatedExpressions.forEach((i, expr) -> LOGGER.error("  {} = {}, type {}", i, expr, expr.parameterizedType()));
        }
        LOGGER.error("No candidate found for {} in {}", methodName, typeInfo);
    }

    private record FilterResult(Map<Integer, Expression> evaluatedExpressions,
                                Map<MethodInfo, Integer> compatibilityScore) {

        // See Lambda_6, Lambda_13: connect type of evaluated argument result to formal parameter type
        public TypeParameterMap typeParameterMap(MethodInfo candidate) {
            Map<NamedType, ParameterizedType> result = new HashMap<>();
            int i = 0;
            for (ParameterInfo parameterInfo : candidate.parameters()) {
                if (i >= evaluatedExpressions.size()) break;
                Expression expression = evaluatedExpressions.get(i);
                // we have a match between the return type of the expression, and the type of the parameter
                // expression: MethodCallErasure, with Collector<T,?,Set<T>> as concrete type
                // parameter type: Collector<? super T,A,R>   (T belongs to Stream, A,R to the collect method)
                // we want to add R --> Set<T> to the type map
                Map<NamedType, ParameterizedType> map = new HashMap<>();
                Set<ParameterizedType> erasureTypes = expandErasureTypes(expression);
                for (ParameterizedType pt : erasureTypes) {
                    map.putAll(pt.initialTypeParameterMap());
                }

                // we now have R as #2 in Collector mapped to Set<T>, and we need to replace that by the
                // actual type parameter of the formal type of parameterInfo
                //result.putAll( parameterInfo.parameterizedType.translateMap(typeContext, map));
                int j = 0;
                boolean isVarargs = parameterInfo.isVarArgs();
                for (ParameterizedType tp : parameterInfo.parameterizedType().parameters()) {
                    if (tp.typeParameter() != null) {
                        int index = j;
                        map.entrySet().stream()
                                .filter(e -> e.getKey() instanceof TypeParameter t && t.getIndex() == index)
                                // we do not add to the map when the result is one type parameter to the next (MethodCall_19)
                                .filter(e -> e.getValue().bestTypeInfo() != null)
                                .map(Map.Entry::getValue)
                                .findFirst()
                                .ifPresent(inMap -> {
                                    // see MethodCall_60,_61,_62,_63 for the array count computation
                                    ParameterizedType target = inMap.copyWithArrays(inMap.arrays() - tp.arrays());
                                    result.merge(tp.typeParameter(), target, ParameterizedType::bestDefined);
                                });
                    }
                    j++;
                }
                if (parameterInfo.parameterizedType().isTypeParameter() && erasureTypes.isEmpty()) {
                    // see MethodCall_48; MethodCall_3 shows why we omit ErasureExpression
                    // see MethodCall_60,_61,_62,_63 for the array count computation
                    boolean oneFewer = expression.parameterizedType().arrays() == parameterInfo.parameterizedType().arrays() - 1;
                    int paramArrays = parameterInfo.parameterizedType().arrays() - (isVarargs && oneFewer ? 1 : 0);
                    int arrays = expression.parameterizedType().arrays() - paramArrays;
                    ParameterizedType target = expression.parameterizedType().copyWithArrays(arrays);
                    result.merge(parameterInfo.parameterizedType().typeParameter(), target, ParameterizedType::bestDefined);
                }
                i++;
            }
            return new TypeParameterMap(result);
        }
    }

    private FilterResult filterCandidatesByParameters(Map<MethodTypeParameterMap, Integer> methodCandidates,
                                                      Map<Integer, Expression> evaluatedExpressions,
                                                      TypeParameterMap typeParameterMap) {
        Map<Integer, Set<ParameterizedType>> acceptedErasedTypes =
                evaluatedExpressions.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e ->
                        expandErasureTypes(e.getValue()).stream()
                                .map(pt -> pt.applyTranslation(runtime, typeParameterMap.map()))
                                .collect(Collectors.toUnmodifiableSet())));

        Map<Integer, ParameterizedType> acceptedErasedTypesCombination = null;
        Map<MethodInfo, Integer> compatibilityScore = new HashMap<>();

        for (Map.Entry<MethodTypeParameterMap, Integer> entry : methodCandidates.entrySet()) {
            int sumScore = 0;
            boolean foundCombination = true;
            List<ParameterInfo> parameters = entry.getKey().methodInfo().parameters();
            int pos = 0;
            Map<Integer, ParameterizedType> thisAcceptedErasedTypesCombination = new TreeMap<>();
            for (ParameterInfo parameterInfo : parameters) {

                Set<ParameterizedType> acceptedErased = acceptedErasedTypes.get(pos);
                ParameterizedType bestAcceptedType = null;
                int bestCompatible = Integer.MIN_VALUE;

                int varargsPenalty;
                ParameterizedType formalType;
                if (parameterInfo.isVarArgs()) {
                    if (acceptedErased == null) {
                        // the parameter is a varargs, and we have the empty array
                        assert parameters.size() == evaluatedExpressions.size() + 1;
                        break;
                    }
                    if (pos == parameters.size() - 1) {
                        // this one can be either the array matching the type, an element of the array
                        ParameterizedType arrayType = parameterInfo.parameterizedType();

                        // here comes a bit of code duplication...
                        for (ParameterizedType actualType : acceptedErased) {

                            int compatible;
                            if (isUnboundMethodTypeParameter(actualType) && actualType.arrays() == arrayType.arrays()) {
                                compatible = 5;
                            } else {
                                compatible = callIsAssignableFrom(actualType, arrayType);
                            }
                            if (compatible >= 0 && (bestCompatible == Integer.MIN_VALUE || compatible < bestCompatible)) {
                                bestCompatible = compatible;
                                bestAcceptedType = actualType;
                            }
                        }

                        // and break off the code duplication, because we cannot set foundCombination to false
                        if (bestCompatible >= 0) {
                            sumScore += bestCompatible;
                            thisAcceptedErasedTypesCombination.put(pos, bestAcceptedType);
                            break;
                        } // else: we have another one to try!
                        // see Constructor_18 for example where varargsPenalty is important
                        varargsPenalty = 500;
                    } else {
                        varargsPenalty = 0;
                    }
                    formalType = parameterInfo.parameterizedType().copyWithOneFewerArrays();
                } else {
                    assert acceptedErased != null;
                    formalType = parameterInfo.parameterizedType();
                    varargsPenalty = 0;
                }

                for (ParameterizedType actualType : acceptedErased) {
                    int penaltyForReturnType = computePenaltyForReturnType(actualType, formalType);
                    for (ParameterizedType actualTypeReplaced : actualType.replaceByTypeBounds()) {
                        for (ParameterizedType formalTypeReplaced : formalType.replaceByTypeBounds()) {

                            boolean paramIsErasure = containsErasedExpressions(evaluatedExpressions.get(pos));
                            int compatible;
                            if (actualTypeReplaced.isTypeOfNullConstant()) {
                                // compute the distance to Object, so that the nearest one loses. See MethodCall_66
                                // IMPROVE why 100?
                                if (formalTypeReplaced.isPrimitiveExcludingVoid()) {
                                    compatible = -1; // MethodCall_69
                                } else {
                                    // note: always assignable! array penalties easily go into the 100's so 1000 seems safe
                                    ParameterizedType objectPt = runtime.objectParameterizedType();
                                    int c = callIsAssignableFrom(formalTypeReplaced, objectPt);
                                    assert c >= 0;
                                    // See MethodCall_66, resp. _74 for the '-' and the '1000'
                                    compatible = varargsPenalty + 10000 - c;
                                }
                            } else if (paramIsErasure && actualTypeReplaced != actualType) {
                                /*
                                 See 'method' call in TestMethodCall_3,2; this feels like a hack.
                                 Map.get(e.getKey()) call in TestMethodCall_3,7 shows the opposite direction; so we do Max.
                                 Feels even more like a hack.
                                 Same hack in compatibleParameter(); see TestMethod9,3 as well
                                 */
                                int a = callIsAssignableFrom(formalTypeReplaced, actualTypeReplaced);
                                int b = callIsAssignableFrom(actualTypeReplaced, formalTypeReplaced);
                                compatible = a < 0 || b < 0 ? Math.max(a, b) : Math.min(a, b);
                            } else {
                                int c = callIsAssignableFrom(actualTypeReplaced, formalTypeReplaced);
                                compatible = c < 0 ? c : varargsPenalty + c;
                            }

                            if (compatible >= 0 && (bestCompatible == Integer.MIN_VALUE
                                                    || (compatible + penaltyForReturnType) < bestCompatible)) {
                                bestCompatible = compatible + penaltyForReturnType;
                                bestAcceptedType = actualType;
                            }
                        }
                    }
                }
                if (bestCompatible < 0) {
                    foundCombination = false;
                } else {
                    sumScore += bestCompatible;
                    thisAcceptedErasedTypesCombination.put(pos, bestAcceptedType);
                }
                pos++;
            }
            if (!foundCombination) {
                sumScore = -1; // to be removed immediately
            } else {
                int varargsSkipped = Math.abs(evaluatedExpressions.size() - parameters.size());
                int methodDistance = entry.getValue();
                sumScore += 100 * methodDistance + 10 * varargsSkipped;
                if (acceptedErasedTypesCombination == null) {
                    acceptedErasedTypesCombination = thisAcceptedErasedTypesCombination;
                } else if (!acceptedErasedTypesCombination.equals(thisAcceptedErasedTypesCombination)) {
                    LOGGER.debug("Looks like multiple, different, combinations? {} to {}", acceptedErasedTypesCombination,
                            thisAcceptedErasedTypesCombination);
                }
            }
            compatibilityScore.put(entry.getKey().methodInfo(), sumScore);
        }

        // remove those with a negative compatibility score
        methodCandidates.entrySet().removeIf(e -> {
            int score = compatibilityScore.get(e.getKey().methodInfo());
            return score < 0;
        });

        return new FilterResult(evaluatedExpressions, compatibilityScore);
    }

    private int computePenaltyForReturnType(ParameterizedType actualType,
                                            ParameterizedType formalType) {
        if (actualType.typeInfo() == null || formalType.typeInfo() == null) return 0;
        MethodInfo actual = actualType.typeInfo().singleAbstractMethod();
        if (actual == null) return 0; // not worth the effort
        MethodInfo formal = formalType.typeInfo().singleAbstractMethod();
        if (formal == null) return 0;
        if (actual.isVoid() && !formal.isVoid()) return notAssignable;
        // we have to have a small penalty in the other direction, to give preference to a Consumer when a Function is competing
        if (!actual.isVoid() && formal.isVoid()) return 5;
        return 0;
    }

    private boolean isUnboundMethodTypeParameter(ParameterizedType actualType) {
        return actualType.typeParameter() != null
               && actualType.typeParameter().isMethodTypeParameter()
               && actualType.typeParameter().typeBounds().isEmpty();
    }

    private FilterResult filterMethodCandidatesInErasureMode(Context context,
                                                             Map<MethodTypeParameterMap, Integer> methodCandidates,
                                                             List<Object> expressions) {
        Map<Integer, Expression> evaluatedExpressions = new HashMap<>();
        Map<MethodInfo, Integer> compatibilityScore = new HashMap<>();
        int pos = 0;
        while (!methodCandidates.isEmpty() && evaluatedExpressions.size() < expressions.size()) {
            ForwardType fwd = context.erasureForwardType();
            Expression evaluatedExpression = context.parseHelper().parseExpression(context, "", fwd,
                    expressions.get(pos));
            evaluatedExpressions.put(pos, Objects.requireNonNull(evaluatedExpression));
            filterCandidatesByParameter(evaluatedExpression, pos, methodCandidates, compatibilityScore);
            pos++;
        }
        return new FilterResult(evaluatedExpressions, compatibilityScore);
    }


    /**
     * StringBuilder.length() is in public interface CharSequence and in the private type AbstractStringBuilder.
     * We prioritise the CharSequence version, because that one can be annotated using annotated APIs.
     *
     * @param methodCandidates the candidates to sort
     * @return a list of size>1 when also candidate 1 is accessible... this will result in an error?
     */
    private List<MethodTypeParameterMap> sortRemainingCandidatesByShallowPublic
    (Map<MethodTypeParameterMap, Integer> methodCandidates) {
        if (methodCandidates.size() > 1) {
            Comparator<MethodTypeParameterMap> comparator =
                    (m1, m2) -> {
                        boolean m1Accessible = m1.methodInfo().isPubliclyAccessible();
                        boolean m2Accessible = m2.methodInfo().isPubliclyAccessible();
                        if (m1Accessible && !m2Accessible) return -1;
                        if (m2Accessible && !m1Accessible) return 1;
                        return 0; // don't know what to prioritize
                    };
            List<MethodTypeParameterMap> sorted = new ArrayList<>(methodCandidates.keySet());
            sorted.sort(comparator);
            MethodTypeParameterMap m1 = sorted.get(1);
            if (m1.methodInfo().hasBeenAnalyzed()) {
                return sorted;
            }
            return List.of(sorted.get(0));
        }
        // not two accessible
        return List.copyOf(methodCandidates.keySet());
    }

    /**
     * Build the correct ForwardReturnTypeInfo to properly evaluate the argument at position i
     *
     * @param method         the method candidate that won the selection, so that the formal parameter type can be determined
     * @param i              the position of the argument
     * @param outsideContext contextual information to be merged with the formal parameter type
     * @param extra          information about type parameters gathered earlier
     * @return the contextual information merged with the formal parameter type info, so that evaluation can start
     */
    private ForwardType determineForwardReturnTypeInfo(MethodTypeParameterMap method,
                                                       int i,
                                                       ParameterizedType outsideContext,
                                                       TypeParameterMap extra) {
        Objects.requireNonNull(method);
        ParameterizedType parameterType = method.getConcreteTypeOfParameter(runtime, i);
        if (outsideContext == null || outsideContext.isVoid() || outsideContext.typeInfo() == null) {
            // Cannot do better than parameter type, have no outside context;
            ParameterizedType translated = parameterType.applyTranslation(runtime, extra.map());
            return new ForwardTypeImpl(translated, false, extra);
        }
        Set<TypeParameter> typeParameters = parameterType.extractTypeParameters();
        Map<NamedType, ParameterizedType> outsideMap = outsideContext.initialTypeParameterMap();
        Map<NamedType, ParameterizedType> map = new HashMap<>(extra.map());
        ParameterizedType returnType = method.getConcreteReturnType(runtime);
            /* here we test whether the return type of the method is a method type parameter. If so,
               we have and outside type that we can assign to it. See MethodCall_68, assigning B to type parameter T
               See TestParseMethods,6
             */
        if (returnType.typeParameter() != null) {
            map.put(returnType.typeParameter(), outsideContext);
        } // else e.g. TestMethodCall9,4
        ParameterizedType translated = parameterType.applyTranslation(runtime, map);

        for (TypeParameter typeParameter : typeParameters) {
            // can we match? if both are functional interfaces, we know exactly which parameter to match

            // otherwise, we're in a bit of a bind -- they need not necessarily agree
            // List.of(E) --> return is List<E>
            ParameterizedType inMap = outsideMap.get(typeParameter);
            if (inMap != null) {
                map.put(typeParameter, inMap);
            } else if (typeParameter.isMethodTypeParameter()) {
                // return type is List<E> where E is the method type param; need to match to the type's type param
                TypeParameter typeTypeParameter = tryToFindTypeTypeParameter(method, typeParameter);
                if (typeTypeParameter != null) {
                    ParameterizedType inMap2 = outsideMap.get(typeTypeParameter);
                    if (inMap2 != null) {
                        map.put(typeParameter, inMap2);
                    }
                }
                ParameterizedType inExtra = extra.map().get(typeParameter);
                if (inExtra != null) {
                    // see TestMethodCall7,9, from min -> talk.getTimeSlotStart()
                    map.merge(typeParameter, inExtra, ParameterizedType::bestDefined);
                }
            }
        }
        if (map.isEmpty()) {
            // Nothing to translate
            return new ForwardTypeImpl(parameterType, false, extra);
        }
        ParameterizedType translated2 = parameterType.applyTranslation(runtime, map);
        // Translated context and parameter
        return new ForwardTypeImpl(translated2, false, extra);
    }


    private TypeParameter tryToFindTypeTypeParameter(MethodTypeParameterMap method,
                                                     TypeParameter methodTypeParameter) {
        ParameterizedType formalReturnType = method.getConcreteReturnType(runtime);
        Map<NamedType, ParameterizedType> map = formalReturnType.initialTypeParameterMap();
        // map points from E as 0 in List to E as 0 in List.of()
        return map.entrySet().stream().filter(e -> methodTypeParameter.equals(e.getValue().typeParameter()))
                .map(e -> (TypeParameter) e.getKey()).findFirst().orElse(null);
    }

    private void trimMethodsKeepMostSpecificReturnType(TypeInfo currentPrimaryType, Map<MethodTypeParameterMap, Integer> methodCandidates) {
        Map<ParameterizedType, List<MethodTypeParameterMap>> perPt = new HashMap<>();
        for (MethodTypeParameterMap method : methodCandidates.keySet()) {
            ParameterizedType erased = method.getConcreteReturnType(runtime).erased();
            if (!erased.isVoidOrJavaLangVoid()) {
                perPt.computeIfAbsent(erased,
                        pt -> new ArrayList<>()).add(method);
            } // else: see TestMethodCall9,8; void is not in competition with others
        }
        if (perPt.size() > 1) {
            Set<ParameterizedType> mostSpecific = new HashSet<>();
            for (ParameterizedType pt : perPt.keySet()) {
                if (mostSpecific.isEmpty()) mostSpecific.add(pt);
                else {
                    Boolean add = null;
                    boolean independent = false;
                    Iterator<ParameterizedType> iterator = mostSpecific.iterator();
                    while (iterator.hasNext()) {
                        ParameterizedType inMostSpecific = iterator.next();
                        ParameterizedType ms = inMostSpecific.mostSpecific(runtime, currentPrimaryType, pt);
                        ParameterizedType ms2 = pt.mostSpecific(runtime, currentPrimaryType, inMostSpecific);

                        boolean newOneIsStrictlyMoreSpecific = ms == pt && ms2 == pt;
                        boolean existingOneIsStrictlyMoreSpecific = ms == inMostSpecific && ms2 == inMostSpecific;

                        if (newOneIsStrictlyMoreSpecific) {
                            add = true;
                            iterator.remove(); // replace by new one
                        } else if (existingOneIsStrictlyMoreSpecific) {
                            // there is a more specific one:
                            add = false;
                        } else {
                            independent = true;
                        }
                    }
                    if (add != null && add || add == null && independent) {
                        mostSpecific.add(pt);
                    }
                }
            }
            for (Map.Entry<ParameterizedType, List<MethodTypeParameterMap>> entry : perPt.entrySet()) {
                if (!mostSpecific.contains(entry.getKey().erased())) {
                    entry.getValue().forEach(methodCandidates::remove);
                }
            }
        }
    }

    private void trimMethodsByCandidateScore(Map<MethodTypeParameterMap, Integer> methodCandidates) {
        int min = methodCandidates.values().stream()
                .mapToInt(i -> i)
                .min().orElseThrow();
        if (min == notAssignable) throw new UnsupportedOperationException();
        methodCandidates.entrySet().removeIf(e -> e.getValue() > min);
    }

    private void trimMethodsWithBestScore(Map<MethodTypeParameterMap, Integer> methodCandidates,
                                          Map<MethodInfo, Integer> compatibilityScore) {
        int min = methodCandidates.keySet().stream()
                .mapToInt(mc -> compatibilityScore.getOrDefault(mc.methodInfo(), 0))
                .min().orElseThrow();
        if (min == notAssignable) throw new UnsupportedOperationException();
        methodCandidates.keySet().removeIf(e ->
                compatibilityScore.getOrDefault(e.methodInfo(), 0) > min);
    }

    // remove varargs if there's also non-varargs solutions
    //
    // this step if AFTER the score step, so we've already dealt with type conversions.
    // we still have to deal with overloads in supertypes, methods with the same type signature
    private static void trimVarargsVsMethodsWithFewerParameters
    (Map<MethodTypeParameterMap, Integer> methodCandidates) {
        int countVarargs = (int) methodCandidates.keySet().stream().filter(e -> e.methodInfo().isVarargs()).count();
        if (countVarargs > 0 && countVarargs < methodCandidates.size()) {
            methodCandidates.keySet().removeIf(e -> e.methodInfo().isVarargs());
        }
    }

    private void filterCandidatesByParameter(Expression expression,
                                             int pos,
                                             Map<MethodTypeParameterMap, Integer> methodCandidates,
                                             Map<MethodInfo, Integer> compatibilityScore) {
        methodCandidates.keySet().removeIf(mc -> {
            int score = compatibleParameter(expression, pos, mc.methodInfo());
            if (score >= 0) {
                Integer inMap = compatibilityScore.get(mc.methodInfo());
                inMap = inMap == null ? score : score + inMap;
                compatibilityScore.put(mc.methodInfo(), inMap);
            }
            return score < 0;
        });
    }

    // different situations with varargs: method(int p1, String... args)
    // 1: method(1) is possible, but pos will not get here, so there's no reason for incompatibility
    // 2: pos == params.size()-1: method(p, "abc")
    // 3: pos == params.size()-1: method(p, new String[] { "a", "b"} )
    // 4: pos >= params.size(): method(p, "a", "b")  -> we need the base type
    private int compatibleParameter(Expression expression, int pos, MethodInfo methodInspection) {
        assert !expression.isEmpty() : "Should we return NOT_ASSIGNABLE?";
        List<ParameterInfo> params = methodInspection.parameters();

        if (pos >= params.size()) {
            ParameterInfo lastParameter = params.getLast();
            assert lastParameter.isVarArgs();
            ParameterizedType typeOfParameter = lastParameter.parameterizedType().copyWithOneFewerArrays();
            return compatibleParameter(expression, typeOfParameter);
        }
        ParameterInfo parameterInfo = params.get(pos);
        if (pos == params.size() - 1 && parameterInfo.isVarArgs()) {
            int withArrays = compatibleParameter(expression, parameterInfo.parameterizedType());
            int withoutArrays = compatibleParameter(expression, parameterInfo.parameterizedType().copyWithOneFewerArrays());
            if (withArrays == -1) return withoutArrays;
            if (withoutArrays == -1) return withArrays;
            return Math.min(withArrays, withoutArrays);
        }
        // the normal situation
        return compatibleParameter(expression, parameterInfo.parameterizedType());
    }

    private int compatibleParameter(Expression evaluatedExpression, ParameterizedType typeOfParameter) {
        Set<ParameterizedType> erasureTypes = expandErasureTypes(evaluatedExpression);
        return erasureTypes.stream().mapToInt(type -> {
                    // Hack? see also paramIsErasure higher up in filterCandidatesByParameters
                    int a = callIsAssignableFrom(type, typeOfParameter);
                    int b = callIsAssignableFrom(typeOfParameter, type);
                    return a < 0 || b < 0 ? Math.max(a, b) : Math.min(a, b);
                })
                .reduce(notAssignable, (v0, v1) -> {
                    if (v0 < 0) return v1;
                    if (v1 < 0) return v0;
                    return Math.min(v0, v1);
                });
    }

    private int callIsAssignableFrom(ParameterizedType actualType, ParameterizedType typeOfParameter) {
        return runtime.isAssignableFromCovariantErasure(typeOfParameter, actualType);
    }

    private static boolean containsErasedExpressions(Expression start) {
        AtomicBoolean found = new AtomicBoolean();
        start.visit(e -> {
            if (e instanceof ErasedExpression) {
                found.set(true);
            }
            return !found.get();
        });
        return found.get();
    }

    // replace an ErasedExpression with its erasure types
    private static Set<ParameterizedType> expandErasureTypes(Expression start) {
        Set<ParameterizedType> set = new HashSet<>();
        if (!(start instanceof ErasedExpression)) {
            set.add(start.parameterizedType());
        }
        start.visit(e -> {
            if (e instanceof ErasedExpression erasedExpression) {
                set.addAll(erasedExpression.erasureTypes());
            }
            /*
            See TestOverload1,3: if the ErasedExpression is hidden inside the scope of a variable, it should not
            appear as a possibility in "set".
             */
            return !(e instanceof VariableExpression);
        });
        return Set.copyOf(set);
    }


    @Override
    public GenericsHelper genericsHelper() {
        return genericsHelper;
    }

    /*
    with regard to generics:
     - there is information from the forward type;
     - there is information from the method (formal) and scope (concrete).

    we aim to provide the newly created MethodReference object with the most specific information.

     */
    @Override
    public Expression resolveMethodReference(Context context, List<Comment> comments, Source source, String
                                                     index,
                                             ForwardType forwardType,
                                             Expression scope, String methodName) {
        assert !forwardType.erasure();
        MethodTypeParameterMap sam = forwardType.computeSAM(context.runtime(), context.genericsHelper(),
                context.enclosingType());
        assert sam != null && sam.isSingleAbstractMethod();

        ParameterizedType scopeType = scope.parameterizedType();
        boolean isConstructor = "new".equals(methodName);
        if (isConstructor && scopeType.arrays() > 0) {
            return arrayConstruction(comments, source, scopeType);
        }
        boolean scopeIsAType = scope instanceof TypeExpression;
        int numParametersInForwardSam = sam.methodInfo().parameters().size();

        MethodTypeParameterMap method = methodTypeParameterMapForMethodReference(context, methodName, scopeType,
                isConstructor, numParametersInForwardSam, scopeIsAType, sam);
        // the typeMap in method is the result of the recursive procedure to find methods in the hierarchy
        // we still must make the connection between the method's parameters and the sam's

        MethodInfo methodInfo = method.methodInfo();
        Map<NamedType, ParameterizedType> methodMap = new HashMap<>(method.concreteTypes());

        // there are 2 possible paths
        // we can solve the types from the parameters, and use that info to solve the return type
        // or, we can solve the return type, and then use that info to solve the parameters
        // TODO only one implemented at the moment: from parameters to return type
        FT ft = computeFunctionalType(context, methodInfo, method, numParametersInForwardSam, sam, isConstructor,
                scopeType, methodMap);

        // the exact methodInfo().name() string must be added to the detailed sources
        DetailedSources.Builder dsb = context.newDetailedSourcesBuilder();
        Source sourceWithMethodName;
        if (dsb != null) {
            Source sourceOfMethodName = source.detailedSources().detail(methodName);
            dsb.addAll(source.detailedSources())
                    .put(methodInfo.name(), sourceOfMethodName);
            sourceWithMethodName = source.mergeDetailedSources(dsb.build());
        } else {
            sourceWithMethodName = source;
        }
        return runtime.newMethodReferenceBuilder().setSource(sourceWithMethodName).addComments(comments)
                .setMethod(methodInfo)
                .setScope(scope)
                .setConcreteFunctionalType(ft.functionalType)
                .setConcreteParameterTypes(ft.concreteTypesOfParametersCorrected(methodInfo))
                .setConcreteReturnType(ft.concreteReturnType)
                .build();
    }

    private record FT(ParameterizedType functionalType, ParameterizedType concreteReturnType,
                      List<ParameterizedType> concreteTypesOfParameters) {
        public List<ParameterizedType> concreteTypesOfParametersCorrected(MethodInfo methodInfo) {
            int n = methodInfo.parameters().size();
            int c = concreteTypesOfParameters.size();
            assert n == c || n == c - 1;
            if (n == 0) return List.of();
            return n == c ? concreteTypesOfParameters : concreteTypesOfParameters.subList(1, c);
        }
    }

    // this is one direction: assume that the inference comes from the parameters, and helps sort out the return value
    // See TestMethodCall8,7B,7C
    private FT computeFunctionalType(Context context,
                                     MethodInfo methodInfo,
                                     MethodTypeParameterMap method,
                                     int numParametersInForwardSam,
                                     MethodTypeParameterMap sam,
                                     boolean isConstructor,
                                     ParameterizedType scopeType,
                                     Map<NamedType, ParameterizedType> methodMap) {
        List<ParameterizedType> typesOfParametersFromMethod = inputTypes(methodInfo, method,
                numParametersInForwardSam);
        List<ParameterizedType> typesOfParametersFromForward = sam.getConcreteTypeOfParameters(runtime);

        int max = Math.max(typesOfParametersFromForward.size(), typesOfParametersFromMethod.size());
        List<ParameterizedType> typesOfParameters = new ArrayList<>(max);
        for (int i = 0; i < max; ++i) {
            ParameterizedType add;
            if (i >= typesOfParametersFromForward.size()) add = typesOfParametersFromMethod.get(i);
            else if (i >= typesOfParametersFromMethod.size()) add = typesOfParametersFromForward.get(i);
            else add = bestType(context.enclosingType().primaryType(), typesOfParametersFromMethod.get(i),
                        typesOfParametersFromForward.get(i), methodMap);
            typesOfParameters.add(add);
        }

        ParameterizedType returnTypeFromMethod;
        ParameterizedType returnTypeFromForward = sam.getConcreteReturnType(runtime);
        if (isConstructor) {
            returnTypeFromMethod = scopeType;
        } else {
            returnTypeFromMethod = method.methodInfo().returnType().applyTranslation(runtime, methodMap);
        }
        ParameterizedType returnType = bestType(context.enclosingType().primaryType(),
                returnTypeFromMethod, returnTypeFromForward);

        ParameterizedType ft = sam.inferFunctionalType(runtime, typesOfParameters, returnType);
        return new FT(ft, returnType, typesOfParameters);
    }


    private void infer(Map<NamedType, ParameterizedType> map, ParameterizedType best, ParameterizedType worse) {
        if (best.typeParameter() == null && worse.typeParameter() != null) {
            map.put(worse.typeParameter(), best);
            return;
        }
        if (best.typeParameter() != null) return;
        if (best.typeInfo() == worse.typeInfo()) {
            Map<NamedType, ParameterizedType> m = worse.formalToConcrete(best);
            map.putAll(m);
            return;
        }
        ParameterizedType concrete = best.concreteSuperType(worse);
        if (concrete != null) {
            Map<NamedType, ParameterizedType> m = worse.formalToConcrete(concrete);
            map.putAll(m);
        }
    }

    private ParameterizedType bestType(TypeInfo currentPrimaryType,
                                       ParameterizedType mostConcrete,
                                       ParameterizedType middle,
                                       Map<NamedType, ParameterizedType> inferred) {
        ParameterizedType best = bestType(currentPrimaryType, mostConcrete, middle);
        if (best != mostConcrete) infer(inferred, best, mostConcrete);
        if (best != middle) infer(inferred, best, middle);
        return best;
    }

    private ParameterizedType bestType(TypeInfo currentPrimaryType,
                                       ParameterizedType mostConcrete,
                                       ParameterizedType middle) {
        if (mostConcrete.equals(middle)) return mostConcrete; // no point in doing any work
        ParameterizedType pt = mostConcrete.mostSpecific(runtime, currentPrimaryType, middle);

        // what if we're missing type parameters?
        if (pt.typeInfo() != null && !pt.typeInfo().typeParameters().isEmpty() && pt.parameters().isEmpty()) {
            if (middle.typeInfo() == null) return pt;
            Map<NamedType, ParameterizedType> map1 = genericsHelper
                    .translateMap(middle.typeInfo().asParameterizedType(), middle, true);
            // one of the two has the best type parameters

            Map<NamedType, ParameterizedType> map;
            if (pt.bestTypeInfo() != middle.typeInfo()) {
                Map<NamedType, ParameterizedType> map2 = genericsHelper
                        .mapInTermsOfParametersOfSubType(pt.bestTypeInfo(), middle);
                if(map2 == null) {
                    map = map1;
                } else {
                    map = genericsHelper.combineMaps(map1, map2);
                }
            } else {
                map = map1;
            }

            // can we derive type parameters from middle? or are our own best?
            return pt.typeInfo().asParameterizedType().applyTranslation(runtime, map);
        }
        return pt;
    }

    private MethodTypeParameterMap methodTypeParameterMapForMethodReference(Context context,
                                                                            String methodName,
                                                                            ParameterizedType scopeType,
                                                                            boolean isConstructor,
                                                                            int numParametersInForwardSam,
                                                                            boolean scopeIsAType,
                                                                            MethodTypeParameterMap sam) {
        Map<MethodTypeParameterMap, Integer> methodCandidates = methodCandidatesForMethodReference(context, methodName,
                scopeType, isConstructor, numParametersInForwardSam, scopeIsAType);
        if (methodCandidates.isEmpty()) {
            throw new Summary.ParseException(context, "Cannot find a candidate for " + methodName);
        }
        List<MethodTypeParameterMap> sorted;
        if (methodCandidates.size() > 1) {
            sorted = handleMultipleCandidates(sam, methodCandidates, scopeIsAType, isConstructor);
        } else {
            sorted = List.copyOf(methodCandidates.keySet());
        }
        if (sorted.isEmpty()) {
            throw new Summary.ParseException(context, "I've killed all the candidates myself??");
        }
        return sorted.getFirst();
    }

    private Map<MethodTypeParameterMap, Integer> methodCandidatesForMethodReference(Context context,
                                                                                    String methodName,
                                                                                    ParameterizedType scopeType,
                                                                                    boolean isConstructor,
                                                                                    int numParametersInForwardSam,
                                                                                    boolean scopeIsAType) {
        ListMethodAndConstructorCandidates list = new ListMethodAndConstructorCandidates(runtime,
                context.typeContext().importMap());
        Map<NamedType, ParameterizedType> typeMap = scopeType.initialTypeParameterMap();

        Map<MethodTypeParameterMap, Integer> methodCandidates;
        if (isConstructor) {
            methodCandidates = list.resolveConstructor(scopeType, scopeType, numParametersInForwardSam, typeMap);
        } else {
            ListMethodAndConstructorCandidates.ScopeNature scopeNature = scopeIsAType
                    ? ListMethodAndConstructorCandidates.ScopeNature.STATIC
                    : ListMethodAndConstructorCandidates.ScopeNature.INSTANCE;
            methodCandidates = new HashMap<>();
            list.recursivelyResolveOverloadedMethods(scopeType, methodName, numParametersInForwardSam,
                    scopeIsAType, typeMap, methodCandidates, scopeNature);
        }
        return methodCandidates;
    }

    @Override
    public Either<Set<Count>, Expression> computeMethodReferenceErasureCounts(Context
                                                                                      context, List<Comment> comments,
                                                                              Source source, Expression scope, String methodName) {
        ParameterizedType parameterizedType = scope.parameterizedType();
        boolean constructor = "new".equals(methodName);

        Map<MethodTypeParameterMap, Integer> methodCandidates;
        ListMethodAndConstructorCandidates list = new ListMethodAndConstructorCandidates(runtime, context.typeContext().importMap());
        if (constructor) {
            if (parameterizedType.arrays() > 0) {
                Expression e = arrayConstruction(comments, source, parameterizedType);
                return Either.right(e);
            }
            methodCandidates = list.resolveConstructor(parameterizedType, parameterizedType,
                    IGNORE_PARAMETER_NUMBERS, parameterizedType.initialTypeParameterMap());
        } else {
            methodCandidates = new HashMap<>();
            list.recursivelyResolveOverloadedMethods(parameterizedType,
                    methodName, IGNORE_PARAMETER_NUMBERS, false,
                    parameterizedType.initialTypeParameterMap(), methodCandidates,
                    ListMethodAndConstructorCandidates.ScopeNature.INSTANCE);
        }
        if (methodCandidates.isEmpty()) {
            throw new UnsupportedOperationException("Cannot find a candidate for " +
                                                    (constructor ? "constructor" : methodName) + " at " + source);
        }
        Set<Count> erasures = new HashSet<>();
        for (MethodTypeParameterMap mt : methodCandidates.keySet()) {
            MethodInfo methodInfo = mt.methodInfo();
            LOGGER.debug("Found method reference candidate, this can work: {}", methodInfo);
            boolean scopeIsType = scope instanceof TypeExpression;
            boolean addOne = scopeIsType && !methodInfo.isConstructor() && !methodInfo.isStatic();
            int n = methodInfo.parameters().size() + (addOne ? 1 : 0);
            boolean isVoid = !constructor && methodInfo.isVoid();
            erasures.add(new Count(n, isVoid));
            // we'll allow for empty var-args as well! NOTE: we do not go "up"!
            if (!methodInfo.parameters().isEmpty() && methodInfo.parameters().getLast().isVarArgs()) {
                erasures.add(new Count(n - 1, isVoid));
            }
        }
        LOGGER.debug("End parsing unevaluated method reference {}, found counts {}", methodName, erasures);
        return Either.left(erasures);
    }

    private List<MethodTypeParameterMap> handleMultipleCandidates(MethodTypeParameterMap singleAbstractMethod,
                                                                  Map<MethodTypeParameterMap, Integer> methodCandidates,
                                                                  boolean scopeIsAType,
                                                                  boolean constructor) {
        List<MethodTypeParameterMap> sorted = new ArrayList<>(methodCandidates.keySet());
        // check types of parameters in SAM
        // see if the method candidate's type fits the SAMs
        for (int i = 0; i < singleAbstractMethod.methodInfo().parameters().size(); i++) {
            final int index = i;
            ParameterizedType concreteType = singleAbstractMethod.getConcreteTypeOfParameter(runtime, i);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Have {} candidates, try to weed out based on compatibility of {} with parameter {}",
                        sorted.size(), concreteType.detailedString(), i);
            }
            List<MethodTypeParameterMap> copy = new ArrayList<>(sorted);
            copy.removeIf(mt -> {
                ParameterizedType typeOfMethodCandidate = typeOfMethodCandidate(mt, index, scopeIsAType, constructor);
                boolean isAssignable = typeOfMethodCandidate.isAssignableFrom(runtime, concreteType);
                return !isAssignable;
            });
            // only accept of this is an improvement
            // there are situations where this method kills all, as the concrete type
            // can be a type parameter while the method candidates only have concrete types
            if (!copy.isEmpty() && copy.size() < sorted.size()) {
                sorted.retainAll(copy);
            }
            // sort on assignability to parameter "index"
            sorted.sort((mc1, mc2) -> {
                ParameterizedType typeOfMc1 = typeOfMethodCandidate(mc1, index, scopeIsAType, constructor);
                ParameterizedType typeOfMc2 = typeOfMethodCandidate(mc2, index, scopeIsAType, constructor);
                if (typeOfMc1.equals(typeOfMc2)) return 0;
                return typeOfMc2.isAssignableFrom(runtime, typeOfMc1) ? -1 : 1;
            });
        }
        if (sorted.size() > 1) {
            LOGGER.debug("Trying to weed out those of the same type, static vs instance");
            staticVsInstance(sorted);
            if (sorted.size() > 1) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Still have {}", methodCandidates.size());
                    sorted.forEach(mc -> LOGGER.debug("- {}", mc.methodInfo()));
                }
                // method candidates have been sorted; the first one should be the one we're after and others should be
                // higher in the hierarchy (interfaces, parent classes)
            }
        }
        return sorted;
    }


    private static void staticVsInstance(List<MethodTypeParameterMap> methodCandidates) {
        Set<TypeInfo> haveInstance = new HashSet<>();
        methodCandidates.stream()
                .filter(mt -> !mt.methodInfo().isStatic())
                .forEach(mt -> haveInstance.add(mt.methodInfo().typeInfo()));
        methodCandidates
                .removeIf(mt -> mt.methodInfo().isStatic() && haveInstance.contains(mt.methodInfo().typeInfo()));
    }

    private ParameterizedType typeOfMethodCandidate(MethodTypeParameterMap mt,
                                                    int index,
                                                    boolean scopeIsAType,
                                                    boolean constructor) {
        MethodInfo methodInfo = mt.methodInfo();
        int param = scopeIsAType && !constructor && !methodInfo.isStatic() ? index - 1 : index;
        if (param == -1) {
            return methodInfo.typeInfo().asParameterizedType();
        }
        if (param >= methodInfo.parameters().size()) {
            return methodInfo.parameters().getLast().parameterizedType();
        }
        return methodInfo.parameters().get(param).parameterizedType();
    }

    /**
     * In this method we provide the types that the "inferFunctionalType" method will use to determine
     * the functional type. We must provide a concrete type for each of the real method's parameters.
     */
    private List<ParameterizedType> inputTypes(MethodInfo methodInfo,
                                               MethodTypeParameterMap method,
                                               int parametersPresented) {
        ParameterizedType formalMethodType = methodInfo.typeInfo().asParameterizedType();
        List<ParameterizedType> types = new ArrayList<>();
        if (method.methodInfo().parameters().size() < parametersPresented) {
            types.add(formalMethodType);
        }
        method.methodInfo().parameters().stream()
                .map(Variable::parameterizedType)
                .forEach(pt -> {
                    ParameterizedType translated = pt.applyTranslation(runtime, method.concreteTypes());
                    types.add(translated);
                });
        return types;
    }

    private MethodReference arrayConstruction(List<Comment> comments,
                                              Source source,
                                              ParameterizedType parameterizedType) {
        MethodInfo methodInfo = runtime.newArrayCreationConstructor(parameterizedType);
        TypeInfo intFunction = runtime.getFullyQualified(IntFunction.class, true);
        ParameterizedType concreteReturnType = runtime.newParameterizedType(intFunction, List.of(parameterizedType));
        return runtime.newMethodReferenceBuilder().setSource(source).addComments(comments)
                .setMethod(methodInfo)
                .setScope(runtime.newTypeExpression(parameterizedType, runtime.diamondNo()))
                .setConcreteFunctionalType(concreteReturnType)
                .setConcreteReturnType(parameterizedType)
                .setConcreteParameterTypes(List.of(runtime.intParameterizedType()))
                .build();
    }
}
