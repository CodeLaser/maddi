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

package io.codelaser.maddi.bytecode.java;

import io.codelaser.maddi.bytecode.java.asm.LocalTypeMap;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.expression.ArrayInitializer;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.TypeExpression;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.util.StringUtil;
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
