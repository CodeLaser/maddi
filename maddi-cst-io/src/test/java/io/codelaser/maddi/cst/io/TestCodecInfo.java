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

package io.codelaser.maddi.cst.io;

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
        String encoded = "[\"Ta.b.C\",\"SSub(0)\",\"Mmax(0,int,int)\"]";
        assertEquals(encoded, codec.encodeInfoOutOfContext(context, max).toString());
        CodecImpl.D d = makeD(encoded);
        assertEquals(max, codec.decodeInfoOutOfContext(context, d));
    }

    @DisplayName("method parameter")
    @Test
    public void test3() {
        String encoded = "[\"Ta.b.C\",\"SSub(0)\",\"Mmax(0,int,int)\",\"Pp1(1)\"]";
        assertEquals(encoded, codec.encodeInfoOutOfContext(context, max1).toString());
        CodecImpl.D d = makeD(encoded);
        assertEquals(max1, codec.decodeInfoOutOfContext(context, d));
    }
}
