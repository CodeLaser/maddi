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

package io.codelaser.maddi.cst.api.runtime;

import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.cst.api.expression.AnnotationExpression;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

import java.util.Collection;
import java.util.List;

public interface Predefined extends PredefinedWithoutParameterizedType {
    MethodInfo assignOperator(ParameterizedType parameterizedType);

    MethodInfo assignOperatorToBinary(MethodInfo operator);

    ParameterizedType booleanParameterizedType();

    ParameterizedType boxedBooleanParameterizedType();

    TypeInfo boxedLongTypeInfo();

    ParameterizedType byteParameterizedType();

    ParameterizedType charParameterizedType();

    ParameterizedType doubleParameterizedType();

    TypeInfo boxedDoubleTypeInfo();

    TypeInfo boxedFloatTypeInfo();

    TypeInfo boxedByteTypeInfo();

    TypeInfo boxedShortTypeInfo();

    TypeInfo boxedVoidTypeInfo();

    ParameterizedType floatParameterizedType();

    AnnotationExpression functionalInterfaceAnnotationExpression();

    ParameterizedType intParameterizedType();

    int isAssignableFromToForPrimitives(ParameterizedType from, ParameterizedType to, boolean covariant);

    ParameterizedType longParameterizedType();

    ParameterizedType objectParameterizedType();

    List<TypeInfo> predefinedObjects();

    /**
     * The primitive types, shared hidden content: the implementation hands out an immutable copy, so the
     * only thing a caller can reach is the {@code TypeInfo} elements themselves -- the
     * {@code @Independent(hc = true)} contract on the method.
     */
    @Independent(hc = true)
    Collection<TypeInfo> primitives();

    int primitiveTypeOrder(ParameterizedType pt);

    int reversePrimitiveTypeOrder(ParameterizedType pt);

    ParameterizedType shortParameterizedType();

    ParameterizedType stringParameterizedType();

    ParameterizedType voidParameterizedType();

    ParameterizedType widestType(ParameterizedType t1, ParameterizedType t2);

    /*
    used by binaryOperator, the result cannot be null, so must be the unboxed version (this contrasts with
    ParameterizedType.commonType)
     */
    ParameterizedType widestTypeUnbox(ParameterizedType t1, ParameterizedType t2);

    boolean isAssignableFrom(ParameterizedType target, ParameterizedType from);
}
