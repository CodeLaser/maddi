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
