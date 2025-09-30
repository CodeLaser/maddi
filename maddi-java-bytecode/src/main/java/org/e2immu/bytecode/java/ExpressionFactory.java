/*
 * e2immu: a static code analyser for effective and eventual immutability
 * Copyright 2020-2021, Bart Naudts, https://www.e2immu.org
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

package org.e2immu.bytecode.java;

import org.e2immu.bytecode.java.asm.LocalTypeMap;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.ArrayInitializer;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.TypeExpression;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.util.internal.util.StringUtil;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExpressionFactory {

    public static Expression from(Runtime runtime, SourceSet sourceSetOfRequest,
                                  SourceSet nearestSourceSet,
                                  LocalTypeMap localTypeMap, Object value) {
        return switch (value) {
            case null -> runtime.nullConstant();
            case String s -> runtime.newStringConstant(s);
            case int[] intArray -> parseArray(runtime, runtime.intParameterizedType(), Arrays.stream(intArray)
                    .mapToObj(i -> from(runtime, sourceSetOfRequest, nearestSourceSet, localTypeMap, i)).toList());
            case Integer i -> runtime.newInt(i);
            case Short s -> runtime.newShort(s);
            case long[] longArray -> parseArray(runtime, runtime.longParameterizedType(), Arrays.stream(longArray)
                    .mapToObj(i -> from(runtime, sourceSetOfRequest, nearestSourceSet, localTypeMap, i)).toList());
            case Long l -> runtime.newLong(l);
            case Byte b -> runtime.newByte(b);
            case double[] doubleArray ->
                    parseArray(runtime, runtime.doubleParameterizedType(), Arrays.stream(doubleArray)
                            .mapToObj(i -> from(runtime, sourceSetOfRequest, nearestSourceSet, localTypeMap, i)).toList());
            case Double d -> runtime.newDouble(d);
            case Float f -> runtime.newFloat(f);
            case char[] chars -> parseCharArray(runtime, chars);
            case Character c -> runtime.newChar(c);
            case Boolean b -> runtime.newBoolean(b);
            case Object[] objectArray ->
                    parseArray(runtime, runtime.objectParameterizedType(), Arrays.stream(objectArray)
                            .map(i -> from(runtime, sourceSetOfRequest, nearestSourceSet, localTypeMap, i)).toList());
            case Type t -> parseTypeExpression(runtime, sourceSetOfRequest, nearestSourceSet, localTypeMap, t);
            default -> runtime.newEmptyExpression(); // will trigger a warning
        };
    }

    private static Expression parseArray(Runtime runtime, ParameterizedType commonType, List<Expression> components) {
        return runtime.newArrayInitializerBuilder().setCommonType(commonType).setExpressions(components).build();
    }

    private static Expression parseCharArray(Runtime runtime, char[] chars) {
        ArrayInitializer.Builder builder = runtime.newArrayInitializerBuilder()
                .setCommonType(runtime.charParameterizedType());
        List<Expression> expressions = new ArrayList<>(chars.length);
        for (char c : chars) expressions.add(runtime.newChar(c));
        return builder.setExpressions(expressions).build();
    }

    private static TypeExpression parseTypeExpression(Runtime runtime,
                                                      SourceSet sourceSetOfRequest,
                                                      SourceSet nearestSourceSet,
                                                      LocalTypeMap localTypeMap,
                                                      Type t) {
        String className = StringUtil.replaceSlashDollar(t.getClassName());
        ParameterizedType parameterizedType =
                switch (className) {
                    case "boolean" -> runtime.booleanParameterizedType();
                    case "byte" -> runtime.byteParameterizedType();
                    case "char" -> runtime.charParameterizedType();
                    case "double" -> runtime.doubleParameterizedType();
                    case "float" -> runtime.floatParameterizedType();
                    case "int" -> runtime.intParameterizedType();
                    case "long" -> runtime.longParameterizedType();
                    case "short" -> runtime.shortParameterizedType();
                    case "void" -> runtime.voidParameterizedType();
                    default -> {
                        TypeInfo ti = localTypeMap.getOrCreate(className, sourceSetOfRequest, nearestSourceSet,
                                LocalTypeMap.LoadMode.NOW);
                        if (ti == null) {
                            throw new UnsupportedOperationException("Cannot load type " + className);
                        }
                        yield ti.asParameterizedType();
                    }
                };
        return runtime.newTypeExpression(parameterizedType, runtime.diamondShowAll());
    }
}
