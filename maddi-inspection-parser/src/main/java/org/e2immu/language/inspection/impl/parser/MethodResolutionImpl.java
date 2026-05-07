/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.element.Comment;
import org.e2immu.language.cst.api.element.DetailedSources;
import org.e2immu.language.cst.api.element.Source;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.MethodReference;
import org.e2immu.language.cst.api.expression.TypeExpression;
import org.e2immu.language.cst.api.expression.VariableExpression;
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
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static org.e2immu.language.inspection.api.parser.TypeContext.SUBTYPE_HIERARCHY_IN_CONSTRUCTOR_PRIORITY;
import static org.e2immu.language.inspection.impl.parser.ListMethodAndConstructorCandidates.IGNORE_PARAMETER_NUMBERS;
import static org.e2immu.language.inspection.impl.parser.ReevaluateErasedExpressions.containsErasedExpressions;

public class MethodResolutionImpl implements MethodResolution {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodResolutionImpl.class);
    private final Runtime runtime;
    private final GenericsHelper genericsHelper;
    private final HierarchyHelper hierarchyHelper;
    private final int notAssignable;
    private final ReevaluateErasedExpressions reEval;

    public MethodResolutionImpl(Runtime runtime) {
        this.runtime = runtime;
        this.genericsHelper = new GenericsHelperImpl(runtime);
        this.hierarchyHelper = new HierarchyHelperImpl();
        notAssignable = runtime.isNotAssignable();
        this.reEval = new ReevaluateErasedExpressions(runtime, genericsHelper);
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
        Set<MethodTypeParameterMap> methodCandidates = initialMethodCandidates(list, scope, numArguments, methodName);

        Map<Integer, Expression> evaluatedExpressions = new TreeMap<>();
        int i = 0;
        ForwardType forward = context.erasureForwardType();
        for (Object argument : unparsedArguments) {
            Expression evaluated = context.resolver().parseHelper().parseExpression(context, index, forward, argument);
            evaluatedExpressions.put(i++, evaluated);
        }

        // FIXME now come stage1, stage2 etc. adapted to erasure types

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
            types = methodCandidates.stream()
                    .map(mc -> erasureReturnType(mc, evaluatedExpressions, scope))
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
        Set<MethodTypeParameterMap> candidates = list.resolveConstructor(formalType, expectedConcreteType,
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
        if (formalType.hasTypeParameters() && !diamond.isShowAll()) {
            // there's only one method left, so we can derive the parameterized type from the parameters
            Set<ParameterizedType> typeParametersResolved = new HashSet<>(formalType.parameters());
            finalParameterizedType1 = resolveConstructorTypeParameters(formalType, candidate.method(),
                    typeParametersResolved, candidate.newParameterExpressions(), useObjectForUndefinedTypeParameters,
                    typeMap);
        } else {
            // we're not correcting when there are type parameters, and they're explicit (diamond == show all)
            // see e.g. TestTypeParameter,9
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

    private ParameterizedType resolveConstructorTypeParameters(ParameterizedType formalType,
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
        Set<MethodTypeParameterMap> methodCandidates = initialMethodCandidates(list, scope, numArguments,
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

        boolean scopeIsThis = scope.expression() instanceof VariableExpression ve && ve.variable() instanceof This;
        Expression newScope;
        if (scope.expression() != null && containsErasedExpressions(scope.expression())) {
            TypeParameterMap tpm = extra.merge(new TypeParameterMap(candidate.mapExpansion));
            newScope = reEval.reEvaluateErasedScope(context, index, scope.expression(), unparsedScope, tpm);
        } else {
            newScope = scope.ensureExplicit(runtime, hierarchyHelper, resolvedMethod,
                    scopeIsThis, context, context.enclosingType(), unparsedScopeSource);
        }
        if (containsErasedExpressions(newScope)) {
            throw new UnsupportedOperationException("Scope still contains erased expressions");
        }
        ParameterizedType returnType = candidate.returnType(runtime, context.enclosingType().primaryType(), extra);

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
            ParameterizedType pt;
            if (mapExpansion.isEmpty()) {
                pt = method.getConcreteReturnType(runtime);
            } else {
                MethodTypeParameterMap expand = method.expand(runtime, primaryType, mapExpansion);
                pt = expand.getConcreteReturnType(runtime);
            }
            ParameterizedType withExtra = pt.applyTranslation(runtime, extra.map());
            // See TypeParameter_4
            return withExtra.isUnboundWildcard() ? runtime.objectParameterizedType() : withExtra;
        }
    }

    private Set<MethodTypeParameterMap> initialMethodCandidates(ListMethodAndConstructorCandidates list,
                                                                ListMethodAndConstructorCandidates.Scope scope,
                                                                int numArguments,
                                                                String methodName) {
        Set<MethodTypeParameterMap> methodCandidates = new HashSet<>();
        list.recursivelyResolveOverloadedMethods(scope.type(), methodName,
                numArguments, false, scope.typeParameterMap().map(), methodCandidates,
                scope.nature());
        if (methodCandidates.isEmpty()) {
            throw new RuntimeException("No candidates at all for method name " + methodName + ", "
                                       + numArguments + " args in type " + scope.type().detailedString());
        }
        return methodCandidates;
    }

    private ParameterizedType erasureReturnType(MethodTypeParameterMap mc,
                                                Map<Integer, Expression> evaluatedExpressions,
                                                ListMethodAndConstructorCandidates.Scope scope) {
        MethodInfo candidate = mc.methodInfo();
        TypeParameterMap map0 = typeParameterMap(candidate, evaluatedExpressions);
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
                                             Set<MethodTypeParameterMap> methodCandidatesIn,
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
        List<MethodTypeParameterMap> methodCandidatesStage1 = stage1ApplicableMethods(methodCandidatesIn,
                evaluatedExpressions, extra);
        List<MethodTypeParameterMap> sorted = stage2MostSpecificMethod(methodCandidatesStage1);
        if (sorted.isEmpty()) {
            if (complain) {
                noCandidatesError(context.enclosingType(), methodName, evaluatedExpressions);
            }
            return null;
        }
        MethodTypeParameterMap method;
        if (sorted.size() > 1) {
            // Step B1: exactly one non-abstract → it wins (the "nearest concrete" rule)
            List<MethodTypeParameterMap> concrete = sorted.stream()
                    .filter(m -> !m.methodInfo().isAbstract()).toList();
            if (concrete.size() == 1) {
                method = concrete.getFirst();
            } else if (concrete.isEmpty()) {
                // all abstract, most specific return type wins
                // Step B2: all abstract with same erasure → pick most specific return type
                // (arbitrary choice among tied return types — real compilers just pick one)
                List<MethodTypeParameterMap> mostSpecificReturn = sorted.stream()
                        .filter(m1 -> sorted.stream()
                                .filter(m2 -> m2 != m1)
                                .allMatch(m2 -> moreSpecificReturn(m1, m2)))
                        .toList();
                if (!mostSpecificReturn.isEmpty()) {
                    method = mostSpecificReturn.getFirst();
                } else {
                    throw multipleCandidatesError(methodName, sorted, evaluatedExpressions);
                }
            } else {
                throw multipleCandidatesError(methodName, sorted, evaluatedExpressions);
            }
        } else {
            method = sorted.getFirst();
        }
        LOGGER.debug("Found most specific method {}", method.methodInfo());

        TypeParameterMap extra2 = methodTypeArguments.isEmpty() ? extra :
                extra.merge(makeMethodTypeParameterMap(method.methodInfo(), methodTypeArguments));

        List<Expression> newParameterExpressions = reEval.reEvaluateErasedExpressions(context, index, unparsedArguments,
                returnType, extra2, methodName, evaluatedExpressions, method);
        Map<NamedType, ParameterizedType> mapExpansion = computeMapExpansion(method, newParameterExpressions, returnType);
        return new Candidate(newParameterExpressions, mapExpansion, method);
    }

    private boolean moreSpecificReturn(MethodTypeParameterMap m1, MethodTypeParameterMap m2) {
        ParameterizedType pt1 = m1.getConcreteReturnType(runtime);
        ParameterizedType pt2 = m2.getConcreteReturnType(runtime);
        return runtime.isAssignableFrom(pt2, pt1, true);
    }

    private List<MethodTypeParameterMap> stage2MostSpecificMethod(List<MethodTypeParameterMap> methodCandidates) {
        if (methodCandidates.size() < 2) return methodCandidates;
        List<MethodTypeParameterMap> result = new ArrayList<>(methodCandidates);
        result.removeIf(m1 -> methodCandidates.stream()
                .filter(m2 -> m2 != m1)
                .anyMatch(m2 -> moreSpecificThan(m2, m1)    // m2 dominates m1
                                && !moreSpecificThan(m1, m2)));  // strictly (not mutual)
        return result;
    }

    private boolean moreSpecificThan(MethodTypeParameterMap m1, MethodTypeParameterMap m2) {
        List<ParameterInfo> m1Parameters = m1.methodInfo().parameters();
        List<ParameterInfo> m2Parameters = m2.methodInfo().parameters();
        int pos = 0;
        while (true) {
            boolean m1Done = pos >= m1Parameters.size();
            boolean m2Done = pos >= m2Parameters.size();
            if (m1Done && m2Done) return true; // all tested!
            if (m1Done) return true;
            if (m2Done) return false;
            ParameterizedType pt1 = m1Parameters.get(pos).parameterizedType();
            ParameterizedType pt2 = m2Parameters.get(pos).parameterizedType();
            if (!runtime.isAssignableFrom(pt2, pt1, true)) return false;
            ++pos;
        }
    }

    private List<MethodTypeParameterMap> stage1ApplicableMethods(Set<MethodTypeParameterMap> methodCandidates,
                                                                 Map<Integer, Expression> evaluatedExpressions,
                                                                 TypeParameterMap typeParameterMap) {
        Map<Integer, Set<ParameterizedType>> acceptedErasedTypes =
                evaluatedExpressions.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e ->
                        expandErasureTypes(e.getValue()).stream()
                                .map(pt -> pt.applyTranslation(runtime, typeParameterMap.map()))
                                .collect(Collectors.toUnmodifiableSet())));

        // gate 1: strict subtyping
        List<MethodTypeParameterMap> set1 = methodCandidates.stream()
                .filter(mc -> stage1ApplicableMethod(mc, acceptedErasedTypes,
                        false, false))
                .toList();
        if (!set1.isEmpty()) return set1;

        // gate 2: allow widening, boxing, unboxing
        List<MethodTypeParameterMap> set2 = methodCandidates.stream()
                .filter(mc -> stage1ApplicableMethod(mc, acceptedErasedTypes,
                        true, false))
                .toList();
        if (!set2.isEmpty()) return set2;

        // gate 3: also allow varargs
        return methodCandidates.stream().filter(mc -> stage1ApplicableMethod(mc,
                        acceptedErasedTypes, true, true))
                .toList();
    }

    private boolean stage1ApplicableMethod(MethodTypeParameterMap methodCandidate,
                                           Map<Integer, Set<ParameterizedType>> acceptedErasedTypes,
                                           boolean acceptWideningBoxingUnboxing,
                                           boolean acceptVarargs) {
        List<ParameterInfo> parameters = methodCandidate.methodInfo().parameters();
        for (ParameterInfo parameter : parameters) {
            Set<ParameterizedType> actualTypes = acceptedErasedTypes.get(parameter.index());
            if (actualTypes == null) {
                assert parameter.isVarArgs() : "Incorrect arity; this method should not have been a candidate";
                return acceptVarargs;
            }
            ParameterizedType formalType = parameter.parameterizedType();
            if (parameter.isVarArgs()) {
                if (!acceptVarargs) return false;
                for (ParameterizedType actualType : actualTypes) {
                    boolean assignable = runtime.isAssignableFromCovariantErasure(formalType, actualType,
                            acceptWideningBoxingUnboxing);
                    if (assignable) {
                        return true; // matching the array type
                    }
                }
                ParameterizedType elementType = formalType.copyWithOneFewerArrays();
                int pos = parameters.size();
                while (true) {
                    Set<ParameterizedType> concreteVarArgTypes = acceptedErasedTypes.get(pos);
                    if (concreteVarArgTypes == null) break;
                    boolean haveMatch = false;
                    for (ParameterizedType actualType : actualTypes) {
                        boolean assignable = runtime.isAssignableFromCovariantErasure(elementType, actualType,
                                acceptWideningBoxingUnboxing);
                        if (assignable) {
                            haveMatch = true;
                            break;
                        }
                    }
                    if (!haveMatch) return false;
                    ++pos;
                }
                return true;
            }
            // not a varargs type
            boolean accept = false;
            for (ParameterizedType actualType : actualTypes) {
                boolean assignable = runtime.isAssignableFromCovariantErasure(formalType, actualType,
                        acceptWideningBoxingUnboxing);
                if (assignable) {
                    accept = true;
                    break;
                }
            }
            if (!accept) return false;
        }
        return true;
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


    private UnsupportedOperationException multipleCandidatesError(String methodName,
                                                                  List<MethodTypeParameterMap> methodCandidates,
                                                                  Map<Integer, Expression> evaluatedExpressions) {
        LOGGER.error("Multiple candidates for {}", methodName);
        methodCandidates.forEach(m -> LOGGER.error(" -- {}", m.methodInfo()));
        LOGGER.error("{} Evaluated expressions:", evaluatedExpressions.size());
        evaluatedExpressions.forEach((i, e) -> LOGGER.error(" -- index {}: {}, {}, {}", i, e, e.getClass(),
                e instanceof ErasedExpression ? "-" : e.parameterizedType().toString()));
        return new UnsupportedOperationException("Multiple candidates");
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
            if (!expression.isNullConstant()) {
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
            } // else see e.g. dubbo OSS
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


    private void noCandidatesError(TypeInfo typeInfo,
                                   String methodName,
                                   Map<Integer, Expression> evaluatedExpressions) {
        if (!evaluatedExpressions.isEmpty()) {
            LOGGER.error("Evaluated expressions for {}: ", methodName);
            evaluatedExpressions.forEach((i, expr) -> LOGGER.error("  {} = {}, type {}", i, expr, expr.parameterizedType()));
        }
        LOGGER.error("No candidate found for {} in {}", methodName, typeInfo);
    }

    // See Lambda_6, Lambda_13: connect type of evaluated argument result to formal parameter type
    public TypeParameterMap typeParameterMap(MethodInfo candidate, Map<Integer, Expression> evaluatedExpressions) {
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
    public Expression resolveMethodReference(Context context,
                                             List<Comment> comments,
                                             Source source,
                                             String index,
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
            if (i < methodInfo.parameters().size()) {
                ParameterInfo pi = methodInfo.parameters().get(i);
                if (pi.parameterizedType().typeParameter() != null) {
                    // see TestMethodCall13,2,3 (identity, Y -> X)
                    // method is a SAM called in Stream.map(), formally mapping X -> Y
                    // when the output contains Y but the type parameter in Function contains X, replace Y by X
                    int arrayDiff = methodInfo.returnType().arrays() - pi.parameterizedType().arrays();
                    methodMap.put(pi.parameterizedType().typeParameter(), add.copyWithArrays(add.arrays() + arrayDiff));
                } // else: handle matching type parameters in objects TODO
            }
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
                if (map2 == null) {
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
        Set<MethodTypeParameterMap> methodCandidates = methodCandidatesForMethodReference(context, methodName,
                scopeType, isConstructor, numParametersInForwardSam, scopeIsAType);
        if (methodCandidates.isEmpty()) {
            throw new Summary.ParseException(context, "Cannot find a candidate for " + methodName);
        }
        List<MethodTypeParameterMap> sorted;
        if (methodCandidates.size() > 1) {
            sorted = handleMultipleCandidates(sam, methodCandidates, scopeIsAType, isConstructor);
        } else {
            sorted = List.copyOf(methodCandidates);
        }
        if (sorted.isEmpty()) {
            throw new Summary.ParseException(context, "I've killed all the candidates myself??");
        }
        return sorted.getFirst();
    }

    private Set<MethodTypeParameterMap> methodCandidatesForMethodReference(Context context,
                                                                           String methodName,
                                                                           ParameterizedType scopeType,
                                                                           boolean isConstructor,
                                                                           int numParametersInForwardSam,
                                                                           boolean scopeIsAType) {
        ListMethodAndConstructorCandidates list = new ListMethodAndConstructorCandidates(runtime,
                context.typeContext().importMap());
        Map<NamedType, ParameterizedType> typeMap = scopeType.initialTypeParameterMap();

        Set<MethodTypeParameterMap> methodCandidates;
        if (isConstructor) {
            methodCandidates = list.resolveConstructor(scopeType, scopeType, numParametersInForwardSam, typeMap);
        } else {
            ListMethodAndConstructorCandidates.ScopeNature scopeNature = scopeIsAType
                    ? ListMethodAndConstructorCandidates.ScopeNature.STATIC
                    : ListMethodAndConstructorCandidates.ScopeNature.INSTANCE;
            methodCandidates = new HashSet<>();
            list.recursivelyResolveOverloadedMethods(scopeType, methodName, numParametersInForwardSam,
                    scopeIsAType, typeMap, methodCandidates, scopeNature);
        }
        return methodCandidates;
    }

    @Override
    public Either<Set<Count>, Expression> computeMethodReferenceErasureCounts(Context context,
                                                                              List<Comment> comments,
                                                                              Source source,
                                                                              Expression scope,
                                                                              String methodName) {
        ParameterizedType parameterizedType = scope.parameterizedType();
        boolean constructor = "new".equals(methodName);

        Set<MethodTypeParameterMap> methodCandidates;
        ListMethodAndConstructorCandidates list = new ListMethodAndConstructorCandidates(runtime,
                context.typeContext().importMap());
        if (constructor) {
            if (parameterizedType.arrays() > 0) {
                Expression e = arrayConstruction(comments, source, parameterizedType);
                return Either.right(e);
            }
            methodCandidates = list.resolveConstructor(parameterizedType, parameterizedType,
                    IGNORE_PARAMETER_NUMBERS, parameterizedType.initialTypeParameterMap());
        } else {
            methodCandidates = new HashSet<>();
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
        for (MethodTypeParameterMap mt : methodCandidates) {
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
                                                                  Set<MethodTypeParameterMap> methodCandidates,
                                                                  boolean scopeIsAType,
                                                                  boolean constructor) {
        List<MethodTypeParameterMap> sorted = new ArrayList<>(methodCandidates);
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
