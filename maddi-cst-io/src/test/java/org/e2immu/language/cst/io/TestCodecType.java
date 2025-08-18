package org.e2immu.language.cst.io;

import org.e2immu.language.cst.api.type.ParameterizedType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestCodecType extends  CommonTest {

    @DisplayName("int")
    @Test
    public void test1() {
        String encoded = "\"Tint\"";
        assertEquals(encoded, codec.encodeType(context, runtime.intParameterizedType()).toString());
        CodecImpl.D d = makeD(encoded);
        assertEquals(runtime.intParameterizedType(), codec.decodeType(context, d));
    }

    @DisplayName("int[]")
    @Test
    public void test2() {
        String encoded = "[\"Tint\",1,[]]";
        ParameterizedType type = runtime.intParameterizedType().copyWithArrays(1);
        assertEquals(encoded, codec.encodeType(context, type).toString());

        CodecImpl.D d = makeD(encoded);
        assertEquals(type, codec.decodeType(context, d));
    }

    @DisplayName("String<String, String>")
    @Test
    public void test3() {
        String encoded = """
                ["Tjava.lang.String",0,["Tjava.lang.String","Tjava.lang.String"]]\
                """;
        ParameterizedType type = runtime.newParameterizedType(runtime.stringTypeInfo(),
                List.of(runtime.stringParameterizedType(), runtime.stringParameterizedType()));
        assertEquals(encoded, codec.encodeType(context, type).toString());
        CodecImpl.D d = makeD(encoded);
        assertEquals(type, codec.decodeType(context, d));
    }

    @DisplayName("unbound wildcard ?")
    @Test
    public void test4() {
        String encoded = "\"?\"";
        ParameterizedType type = runtime.parameterizedTypeWildcard();
        assertEquals("?", type.toString());
        assertEquals(encoded, codec.encodeType(context, type).toString());
        CodecImpl.D d = makeD(encoded);
        assertEquals(type, codec.decodeType(context, d));
    }

    @DisplayName("? super String")
    @Test
    public void test5() {
        String encoded = "[\"Tjava.lang.String\",0,[],\"S\"]";
        ParameterizedType type = runtime.newParameterizedType(runtime.stringTypeInfo(), 0, runtime.wildcardSuper(), List.of());
        assertEquals("Type ? super String", type.toString());
        assertEquals(encoded, codec.encodeType(context, type).toString());
        CodecImpl.D d = makeD(encoded);
        assertEquals(type, codec.decodeType(context, d));
    }
}
