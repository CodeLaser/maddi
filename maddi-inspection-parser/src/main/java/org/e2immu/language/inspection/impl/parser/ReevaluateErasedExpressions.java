package org.e2immu.language.inspection.impl.parser;

import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.Lambda;
import org.e2immu.language.cst.api.expression.NullConstant;
import org.e2immu.language.cst.api.info.ParameterInfo;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.inspection.api.parser.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReevaluateErasedExpressions {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReevaluateErasedExpressions.class);
    private final GenericsHelper genericsHelper;
    private final Runtime runtime;

    public ReevaluateErasedExpressions(Runtime runtime, GenericsHelper genericsHelper) {
        this.genericsHelper = genericsHelper;
        this.runtime = runtime;
    }

    /*
    see TestVar, methodExplicit(I i). The call to 'collect' in statement 1. Extra maps T->J; this information
    must be passed on!

    NOTE:
    We collect type information in a linear way at the moment, from the first type parameter to the 2nd, etc.
    The alternative is to iterate until all are 'solved', but then we need a good definition of 'solved'.
     */
    List<Expression> reEvaluateErasedExpressions(Context context,
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

        for (int i : positionsToDo) {
            Expression e = evaluatedExpressions.get(i);
            ReEval reEval = reevaluateParameterExpression(context, index, expressions, outsideContext, methodName,
                    e, method, i, cumulative, parameters);
            cumulative = reEval.typeParameterMap;
            newParameterExpressions[i] = reEval.evaluated;
        }
        return Arrays.stream(newParameterExpressions).toList();
    }

    record ReEval(TypeParameterMap typeParameterMap, Expression evaluated) {
    }

    ReEval reevaluateParameterExpression(Context context,
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

    Expression reEvaluateErasedScope(Context context, String index, Expression expressionWithErasedType,
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


    static boolean containsErasedExpressions(Expression start) {
        AtomicBoolean found = new AtomicBoolean();
        start.visit(e -> {
            if (e instanceof ErasedExpression) {
                found.set(true);
            }
            return !found.get();
        });
        return found.get();
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
        return map.entrySet().stream()
                .filter(e -> methodTypeParameter.equals(e.getValue().typeParameter()))
                .map(e -> (TypeParameter) e.getKey()).findFirst().orElse(null);
    }
}
