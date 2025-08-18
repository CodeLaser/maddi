package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.translate.TranslationMap;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.List;
import java.util.Map;

public interface MethodTypeParameterMap {

    Map<NamedType, ParameterizedType> concreteTypes();

    MethodTypeParameterMap expand(Runtime runtime, TypeInfo primaryType, Map<NamedType, ParameterizedType> mapExpansion);

    /*
        CT = concreteTypes

        CT:  T in Function -> AL<LL<S>>
        F2C: T in Function -> Coll<E>
        result: E in Coll -> LL<S>

        CT:  R in Function -> Stream<? R in flatMap>
        F2C: R in Function -> Stream<E in Coll>
        result: E in Coll = R in flatMap (is of little value here)
         */
    Map<NamedType, ParameterizedType> formalOfSamToConcreteTypes(MethodInfo actualMethod, Runtime runtime);

    ParameterizedType getConcreteReturnType(Runtime runtime);

    ParameterizedType getConcreteTypeOfParameter(Runtime runtime, int i);

    List<ParameterizedType> getConcreteTypeOfParameters(Runtime runtime);

    ParameterizedType inferFunctionalType(Runtime runtime,
                                          List<ParameterizedType> types,
                                          ParameterizedType inferredReturnType);

    boolean isAssignableFrom(MethodTypeParameterMap other);

    boolean isSingleAbstractMethod();

    MethodInfo methodInfo();

    ParameterizedType parameterizedType(int pos);

    MethodTypeParameterMap translate(TranslationMap translationMap);
}
