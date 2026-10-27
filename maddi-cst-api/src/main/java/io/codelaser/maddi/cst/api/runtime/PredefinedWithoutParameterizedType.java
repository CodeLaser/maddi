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

import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;

public interface PredefinedWithoutParameterizedType {

    MethodInfo andOperatorBool();

    MethodInfo andOperatorInt();

    MethodInfo assignAndOperatorBool();

    MethodInfo assignAndOperatorInt();

    MethodInfo assignDivideOperatorInt();

    MethodInfo assignLeftShiftOperatorInt();

    MethodInfo assignMinusOperatorInt();

    MethodInfo assignMultiplyOperatorInt();

    MethodInfo assignOperatorInt();

    MethodInfo assignOrOperatorBool();

    MethodInfo assignOrOperatorInt();

    MethodInfo assignPlusOperatorInt();

    MethodInfo assignPlusOperatorString();

    MethodInfo assignRemainderOperatorInt();

    MethodInfo assignSignedRightShiftOperatorInt();

    MethodInfo assignUnsignedRightShiftOperatorInt();

    MethodInfo assignXorOperatorInt();

    MethodInfo bitWiseNotOperatorInt();

    MethodInfo bitwiseXorOperatorInt();

    TypeInfo booleanTypeInfo();

    TypeInfo boxed(TypeInfo typeInfo);

    TypeInfo boxedBooleanTypeInfo();

    TypeInfo charTypeInfo();

    TypeInfo characterTypeInfo();

    TypeInfo classTypeInfo();

    MethodInfo divideOperatorInt();

    MethodInfo equalsOperatorInt();

    MethodInfo equalsOperatorObject();

    MethodInfo greaterEqualsOperatorInt();

    MethodInfo greaterOperatorInt();

    TypeInfo intTypeInfo();

    TypeInfo integerTypeInfo();

    boolean isPreOrPostFixOperator(MethodInfo operator);

    boolean isPrefixOperator(MethodInfo operator);

    boolean isPrimitiveWidening(TypeInfo typeInfo, TypeInfo typeInfo1);

    MethodInfo leftShiftOperatorInt();

    MethodInfo lessEqualsOperatorInt();

    MethodInfo lessOperatorInt();

    MethodInfo logicalNotOperatorBool();

    MethodInfo minusOperatorInt();

    MethodInfo multiplyOperatorInt();

    MethodInfo notEqualsOperatorInt();

    MethodInfo notEqualsOperatorObject();

    TypeInfo objectTypeInfo();

    MethodInfo orOperatorBool();

    MethodInfo orOperatorInt();

    MethodInfo plusOperatorInt();

    MethodInfo plusOperatorString();

    MethodInfo postfixDecrementOperatorInt();

    MethodInfo postfixIncrementOperatorInt();

    MethodInfo prePostFixToAssignment(MethodInfo operator);

    MethodInfo prefixDecrementOperatorInt();

    MethodInfo prefixIncrementOperatorInt();

    TypeInfo primitiveByName(String asString);

    TypeInfo primitiveByNameOrNull(String name);

    MethodInfo remainderOperatorInt();

    MethodInfo signedRightShiftOperatorInt();

    TypeInfo stringTypeInfo();

    MethodInfo unaryMinusOperatorInt();

    MethodInfo unaryPlusOperatorInt();

    TypeInfo unboxed(TypeInfo typeInfo);

    MethodInfo unsignedRightShiftOperatorInt();

    MethodInfo xorOperatorBool();

    MethodInfo xorOperatorInt();

    ParameterizedType parameterizedTypeWildcard();
}
