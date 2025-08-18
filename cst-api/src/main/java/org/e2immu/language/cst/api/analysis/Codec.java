package org.e2immu.language.cst.api.analysis;

import org.e2immu.annotation.Modified;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.Variable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/*
support for reading and writing the property-value pairs in many elements.
 */
public interface Codec {

    int constructorIndex(MethodInfo methodInfo);

    /*
    the reason we write directly is that HCS follows HCT in the same list, and needs its value (see decoder of HCS)
     */
    void decode(Context context, @Modified PropertyValueMap pvm, Stream<EncodedPropertyValue> encodedPropertyValueStream);

    boolean decodeBoolean(Context context, EncodedValue encodedValue);

    Expression decodeExpression(Context context, EncodedValue value);

    FieldInfo decodeFieldInfo(TypeInfo typeInfo, EncodedValue ev);

    Info decodeInfoInContext(Context context, EncodedValue ev);

    Info decodeInfoInContext(Context context, char type, String name);

    Info decodeInfoOutOfContext(Context context, EncodedValue ev);

    int decodeInt(Context context, EncodedValue encodedValue);

    List<EncodedValue> decodeList(Context context, EncodedValue encodedValue);

    Map<EncodedValue, EncodedValue> decodeMap(Context context, EncodedValue encodedValue);

    Map<EncodedValue, EncodedValue> decodeMapAsList(Context context, EncodedValue encodedValue);

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

    EncodedValue encodeSet(Context context, Set<EncodedValue> set);

    EncodedValue encodeString(Context context, String string);

    EncodedValue encodeType(Context context, ParameterizedType type);

    EncodedValue encodeVariable(Context context, Variable variable);

    int fieldIndex(FieldInfo key);

    boolean isList(EncodedValue encodedValue);

    int methodIndex(MethodInfo methodInfo);

    PropertyProvider propertyProvider();

    int subTypeIndex(TypeInfo subType);

    // for extensions
    TypeProvider typeProvider();

    interface Context {
        MethodInfo currentMethod();

        TypeInfo currentType();

        TypeInfo findType(Codec.TypeProvider typeProvider, String typeFqn);

        boolean isEmpty();

        boolean methodBeforeType();

        Info peek(int stepsBack);

        Info pop();

        void push(Info info);
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

