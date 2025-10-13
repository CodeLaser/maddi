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
