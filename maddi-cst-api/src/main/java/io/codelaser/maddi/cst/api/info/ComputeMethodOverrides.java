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

package io.codelaser.maddi.cst.api.info;

import io.codelaser.maddi.cst.api.type.NamedType;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ComputeMethodOverrides {
    /*
    uses the same machinery as 'overrides', but is executed at a different stage.
     */
    MethodInfo computeFunctionalInterface(TypeInfo typeInfo);

    Set<MethodInfo> overrides(MethodInfo methodInfo);

    Map<NamedType, ParameterizedType> mapOfSuperType(ParameterizedType superType);

    boolean sameParameters(List<ParameterInfo> parametersOfMyMethod,
                           List<ParameterInfo> parametersOfTarget,
                           Map<NamedType, ParameterizedType> translationMap);

    List<ParameterizedType> directSuperTypes(TypeInfo typeInfo);
}
