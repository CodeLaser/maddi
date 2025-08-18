package org.e2immu.language.cst.api.runtime;

import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.Collection;
import java.util.List;

public interface Predefined extends PredefinedWithoutParameterizedType {
    MethodInfo assignOperator(ParameterizedType parameterizedType);

    ParameterizedType booleanParameterizedType();

    ParameterizedType boxedBooleanParameterizedType();

    TypeInfo boxedLongTypeInfo();

    ParameterizedType byteParameterizedType();

    ParameterizedType charParameterizedType();

    ParameterizedType doubleParameterizedType();

    ParameterizedType floatParameterizedType();

    AnnotationExpression functionalInterfaceAnnotationExpression();

    ParameterizedType intParameterizedType();

    int isAssignableFromTo(ParameterizedType from, ParameterizedType to, boolean covariant);

    default boolean isAssignableFromTo(ParameterizedType from, ParameterizedType to) {
        return isAssignableFromTo(from, to, true) != -1;
    }

    ParameterizedType longParameterizedType();

    ParameterizedType objectParameterizedType();

    List<TypeInfo> predefinedObjects();

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

}
