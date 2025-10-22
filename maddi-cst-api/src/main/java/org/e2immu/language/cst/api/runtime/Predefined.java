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

    int isAssignableFromToForPrimitives(ParameterizedType from, ParameterizedType to, boolean covariant);

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
