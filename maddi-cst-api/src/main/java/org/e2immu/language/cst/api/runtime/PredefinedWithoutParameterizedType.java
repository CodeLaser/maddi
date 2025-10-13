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

package org.e2immu.language.cst.api.runtime;

import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;

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

    MethodInfo remainderOperatorInt();

    MethodInfo signedRightShiftOperatorInt();

    TypeInfo stringTypeInfo();

    MethodInfo unaryMinusOperatorInt();

    MethodInfo unaryPlusOperatorInt();

    TypeInfo unboxed(TypeInfo typeInfo);

    MethodInfo unsignedRightShiftOperatorInt();

    MethodInfo xorOperatorBool();

    MethodInfo xorOperatorInt();
}
