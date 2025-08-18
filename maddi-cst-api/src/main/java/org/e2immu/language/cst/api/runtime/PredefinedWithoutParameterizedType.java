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
