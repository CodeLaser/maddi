package org.e2immu.language.cst.io;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class TestCodecInfo extends CommonTest {

    @DisplayName("subtype")
    @Test
    public void test1() {
        String encoded = "[\"Ta.b.C\",\"SSub(0)\"]";
        assertEquals(encoded, codec.encodeInfoOutOfContext(context, sub).toString());
        CodecImpl.D d = makeD(encoded);
        assertEquals(sub, codec.decodeInfoOutOfContext(context, d));
    }

    @DisplayName("method in subtype")
    @Test
    public void test2() {
        String encoded = "[\"Ta.b.C\",\"SSub(0)\",\"Mmax(0)\"]";
        assertEquals(encoded, codec.encodeInfoOutOfContext(context, max).toString());
        CodecImpl.D d = makeD(encoded);
        assertEquals(max, codec.decodeInfoOutOfContext(context, d));
    }

    @DisplayName("method parameter")
    @Test
    public void test3() {
        String encoded = "[\"Ta.b.C\",\"SSub(0)\",\"Mmax(0)\",\"Pp1(1)\"]";
        assertEquals(encoded, codec.encodeInfoOutOfContext(context, max1).toString());
        CodecImpl.D d = makeD(encoded);
        assertEquals(max1, codec.decodeInfoOutOfContext(context, d));
    }
}
