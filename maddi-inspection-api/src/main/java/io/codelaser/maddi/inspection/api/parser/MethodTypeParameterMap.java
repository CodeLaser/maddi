/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.inspection.api.parser;

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.translate.TranslationMap;
import io.codelaser.maddi.cst.api.type.NamedType;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

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
