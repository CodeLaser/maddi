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

package io.codelaser.maddi.cst.api.analysis;

import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.info.FieldInfo;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.Variable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/*
support for reading and writing the property-value pairs in many elements.
 */
public interface Codec {

    /*
    the reason we write directly is that HCS follows HCT in the same list, and needs its value (see decoder of HCS)
     */
    void decode(Context context, @Modified PropertyValueMap pvm, Stream<EncodedPropertyValue> encodedPropertyValueStream);

    boolean decodeBoolean(Context context, EncodedValue encodedValue);

    Expression decodeExpression(Context context, EncodedValue value);

    FieldInfo decodeFieldInfo(TypeAndSorted typeAndSorted, EncodedValue ev);

    Info decodeInfoInContext(Context context, EncodedValue ev);

    Info decodeInfoInContext(Context context, char type, String name);

    Info decodeInfoOutOfContext(Context context, EncodedValue ev);

    int decodeInt(Context context, EncodedValue encodedValue);

    List<EncodedValue> decodeList(Context context, EncodedValue encodedValue);

    Map<EncodedValue, EncodedValue> decodeMap(Context context, EncodedValue encodedValue);

    Map<EncodedValue, EncodedValue> decodeMapAsList(Context context, EncodedValue encodedValue);

    MethodInfo decodeMethodInfo(Context context, EncodedValue e);

    Set<EncodedValue> decodeSet(Context context, EncodedValue encodedValue);

    String decodeString(Context context, EncodedValue encodedValue);

    ParameterizedType decodeType(Context context, EncodedValue encodedValue);

    Variable decodeVariable(Context context, EncodedValue encodedValue);

    default EncodedPropertyValue encode(Context context, Property property, Value value) {
        EncodedValue encodedValue = value.encode(this, context);
        return new EncodedPropertyValue(property.key(), encodedValue);
    }

    EncodedValue encode(Context context, Info info, String index, Stream<EncodedPropertyValue> encodedPropertyValueStream,
                        List<EncodedValue> subs);

    EncodedValue encodeBoolean(Context context, boolean value);

    EncodedValue encodeExpression(Context context, Expression expression);

    EncodedValue encodeInfoInContext(Context context, Info info, String index);

    EncodedValue encodeInfoOutOfContext(Context context, Info info);

    EncodedValue encodeInt(Context context, int value);

    EncodedValue encodeList(Context context, List<EncodedValue> encodedValues);

    EncodedValue encodeMap(Context context, Map<EncodedValue, EncodedValue> map);

    EncodedValue encodeMapAsList(Context context, Map<EncodedValue, EncodedValue> map);

    EncodedValue encodeMethodInfo(Context context, MethodInfo mi);

    EncodedValue encodeSet(Context context, Set<EncodedValue> set);

    EncodedValue encodeString(Context context, String string);

    EncodedValue encodeType(Context context, ParameterizedType type);

    EncodedValue encodeVariable(Context context, Variable variable);

    boolean isList(EncodedValue encodedValue);

    PropertyProvider propertyProvider();

    // for extensions
    TypeProvider typeProvider();

    record TypeAndSorted(TypeInfo typeInfo, List<FieldInfo> sortedFields,
                         List<MethodInfo> sortedConstructors,
                         List<MethodInfo> sortedMethods,
                         List<TypeInfo> sortedSubTypes) {
        public TypeAndSorted(TypeInfo typeInfo) {
            this(typeInfo,
                    typeInfo.fields().stream().sorted(Comparator.comparing(FieldInfo::name)).toList(),
                    typeInfo.constructors().stream().sorted(Comparator.comparing(MethodInfo::fullyQualifiedName)).toList(),
                    typeInfo.methods().stream().sorted(Comparator.comparing(MethodInfo::fullyQualifiedName)).toList(),
                    typeInfo.subTypes().stream().sorted(Comparator.comparing(TypeInfo::simpleName)).toList());
        }

        public int constructorIndex(MethodInfo methodInfo) {
            int i = 0;
            for (MethodInfo mi : sortedConstructors) {
                if (mi == methodInfo) return i;
                ++i;
            }
            throw new UnsupportedOperationException();
        }

        public int methodIndex(MethodInfo methodInfo) {
            int i = 0;
            for (MethodInfo mi : sortedMethods) {
                if (mi == methodInfo) return i;
                ++i;
            }
            throw new UnsupportedOperationException();
        }

        public int fieldIndex(FieldInfo fieldInfo) {
            int i = fieldIndexOrNegative(fieldInfo);
            if (i < 0) throw new UnsupportedOperationException();
            return i;
        }

        /** Like {@link #fieldIndex}, but returns -1 rather than throwing when the field is not among the type's own
         *  fields. A <em>detached</em> field — a virtual field ({@code §…}) the link engine created to stand in for a
         *  shallow/JDK type's hidden content, or a field whose owner was rewritten — has no index; the caller then
         *  encodes it by name. See {@code maddi-modification-link/.../vf/virtual-fields.md}. */
        public int fieldIndexOrNegative(FieldInfo fieldInfo) {
            int i = 0;
            for (FieldInfo fi : sortedFields) {
                if (fi == fieldInfo) return i;
                ++i;
            }
            return -1;
        }

        public int subTypeIndex(TypeInfo typeInfo) {
            int i = 0;
            for (TypeInfo sub : sortedSubTypes) {
                if (sub == typeInfo) return i;
                ++i;
            }
            throw new UnsupportedOperationException();
        }
    }

    interface Context {
        MethodInfo currentMethod();

        TypeAndSorted currentType();

        TypeInfo findType(Codec.TypeProvider typeProvider, String typeFqn);

        boolean isEmpty();

        boolean methodBeforeType();

        Object peek(int stepsBack);

        Object pop();

        void push(Object info);
    }

    interface DI {
        Codec codec();

        Context context();
    }

    interface DecoderProvider {
        BiFunction<DI, EncodedValue, Value> decoder(Class<? extends Value> clazz);
    }

    interface TypeProvider {
        TypeInfo get(String fqn);
    }

    interface PropertyProvider {
        Property get(String propertyName);
    }

    // Info level

    interface EncodedValue {
    }

    record EncodedPropertyValue(String key, EncodedValue encodedValue) {
    }

    record PropertyValue(Property property, Value value) {
    }

    class DecoderException extends RuntimeException {
        public DecoderException(String msg) {
            super(msg);
        }
    }
}

