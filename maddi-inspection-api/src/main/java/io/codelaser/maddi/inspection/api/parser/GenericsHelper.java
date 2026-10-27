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

import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.cst.api.element.Element;
import io.codelaser.maddi.cst.api.element.Source;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.Lambda;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.NamedType;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.Map;

public interface GenericsHelper {

    MethodTypeParameterMap newMethodTypeParameterMap(MethodInfo methodInfo, Map<NamedType, ParameterizedType> concreteTypes);

    /*
     StringMap<V> -> HashMap<K,V> -> Map<K, V>

     M2: K(map) -> K(hashmap), M1: K(hashmap) -> String

     M1: E=0 in Set -> String, M2: Target = E=0 in Collection.
     We must make the link between E in Collection and E in set

     TODO this code is pretty dedicated; it should be recursive? TODO clean-up, extend
  */
    Map<NamedType, ParameterizedType> biDirectionalCombineMaps(Map<NamedType, ParameterizedType> m1,
                                                               Map<NamedType, ParameterizedType> m2);

    /*
        StringMap<V> -> HashMap<K,V> -> Map<K, V>

        M2: K(map) -> K(hashmap), M1: K(hashmap) -> String
        */
    Map<NamedType, ParameterizedType> combineMaps(Map<NamedType, ParameterizedType> m1,
                                                  Map<NamedType, ParameterizedType> m2);

    MethodTypeParameterMap findSingleAbstractMethodOfInterface(ParameterizedType parameterizedType);

    MethodTypeParameterMap findSingleAbstractMethodOfInterface(
            ParameterizedType parameterizedType,
            boolean complain);

    // practically the duplicate of the previous, except that we should parameterize initialTypeParameterMap as well to collapse them
    Map<NamedType, ParameterizedType> mapInTermsOfParametersOfSubType(TypeInfo ti,
                                                                      ParameterizedType superType);

    Map<NamedType, ParameterizedType> mapInTermsOfParametersOfSuperType(TypeInfo ti,
                                                                        ParameterizedType superType);

    Map<NamedType, ParameterizedType> translateMap(ParameterizedType formalType,
                                                   ParameterizedType concreteType,
                                                   boolean concreteTypeIsAssignableToThis);

    ExpressionBuilder newLambdaExpressionBuilder();

    interface ExpressionBuilder extends Element.Builder<ExpressionBuilder> {
        // use when defining field expressions
        @Fluent
        ExpressionBuilder setEnclosingType(TypeInfo typeInfo);

        // use in methods
        @Fluent
        ExpressionBuilder setEnclosingMethod(MethodInfo methodInfo);

        // the variables of the expression which will become parameters
        @Fluent
        ExpressionBuilder addVariable(Variable variable);

        @Fluent
        ExpressionBuilder setExpression(Expression expression);

        @Fluent
        ExpressionBuilder setForwardType(ParameterizedType forwardType);

        @Fluent
        ExpressionBuilder setSourceForStatement(Source source);

        Lambda build();
    }
}
