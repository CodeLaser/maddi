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
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Round-trips for the in-context info encoding ({@link CodecImpl#encodeInfoInContext} / {@code decodeInfoInContext}):
 * a single short token (e.g. {@code "Ff(0)"}) resolved against the {@code Context} stack (current type / method).
 * Covers the primary type (T), sub-type (S), method (M), field (F) and parameter (P) cases.
 */
public class TestCodecInfoInContext extends CommonTest {

    @DisplayName("primary type (T) round-trip via type provider")
    @Test
    public void testPrimaryType() {
        String encoded = "\"Ta.b.C\"";
        assertEquals(encoded, codec.encodeInfoInContext(context, typeInfo, "").toString());
        assertSame(typeInfo, codec.decodeInfoInContext(context, makeD(encoded)));
    }

    @DisplayName("sub-type (S) resolved in the enclosing type")
    @Test
    public void testSubType() {
        context.push(typeInfo); // current type = C
        String encoded = "\"SSub(0)\"";
        assertEquals(encoded, codec.encodeInfoInContext(context, sub, "0").toString());
        assertSame(sub, codec.decodeInfoInContext(context, makeD(encoded)));
    }

    @DisplayName("field (F) resolved in the current type")
    @Test
    public void testField() {
        context.push(typeInfo); // current type = C (owner of f)
        String encoded = "\"Ff(0)\"";
        assertEquals(encoded, codec.encodeInfoInContext(context, f, "0").toString());
        assertSame(f, codec.decodeInfoInContext(context, makeD(encoded)));
    }

    @DisplayName("method (M) resolved in the current type")
    @Test
    public void testMethod() {
        context.push(sub); // current type = Sub (owner of max)
        String encoded = "\"Mmax(0)\"";
        assertEquals(encoded, codec.encodeInfoInContext(context, max, "0").toString());
        assertSame(max, codec.decodeInfoInContext(context, makeD(encoded)));
    }

    @DisplayName("parameter (P) resolved in the current method")
    @Test
    public void testParameter() {
        context.push(sub);  // current type = Sub
        context.push(max);  // current method = max
        String encoded = "\"Pp1(1)\"";
        assertEquals(encoded, codec.encodeInfoInContext(context, max1, "1").toString());
        assertSame(max1, codec.decodeInfoInContext(context, makeD(encoded)));
    }
}
