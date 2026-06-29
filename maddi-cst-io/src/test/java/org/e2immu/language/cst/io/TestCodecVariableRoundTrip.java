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

import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.FieldReference;
import org.e2immu.language.cst.api.variable.LocalVariable;
import org.e2immu.language.cst.api.variable.This;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Round-trips for every variable kind handled by {@link CodecImpl#encodeVariable} / {@code decodeVariable}:
 * parameter (P), field reference (F), {@code this} (T), local variable (L) and dependent/array-access (D).
 * Each goes encode -> toString -> JSON parse ({@code makeD}) -> decode and must equal the original.
 */
public class TestCodecVariableRoundTrip extends CommonTest {

    @DisplayName("parameter variable (P)")
    @Test
    public void testParameter() {
        context.push(typeInfo);
        String encoded = "[\"P\",[\"Ta.b.C\",\"SSub(0)\",\"Mmax(0)\",\"Pp1(1)\"]]";
        assertEquals(encoded, codec.encodeVariable(context, max1).toString());
        assertEquals(max1, codec.decodeVariable(context, makeD(encoded)));
    }

    @DisplayName("field reference, default scope (F)")
    @Test
    public void testFieldReferenceDefaultScope() {
        context.push(typeInfo);
        FieldReference fr = runtime.newFieldReference(f);
        String encoded = "[\"F\",[\"Ta.b.C\",\"Ff(0)\"]]";
        assertEquals(encoded, codec.encodeVariable(context, fr).toString());
        assertEquals(fr, codec.decodeVariable(context, makeD(encoded)));
    }

    @DisplayName("this (T)")
    @Test
    public void testThis() {
        context.push(typeInfo);
        This thisVar = runtime.newThis(typeInfo.asParameterizedType());
        String encoded = "[\"T\",[\"Ta.b.C\"]]";
        assertEquals(encoded, codec.encodeVariable(context, thisVar).toString());
        assertEquals(thisVar, codec.decodeVariable(context, makeD(encoded)));
    }

    @DisplayName("local variable (L)")
    @Test
    public void testLocalVariable() {
        context.push(typeInfo);
        LocalVariable lv = runtime.newLocalVariable("x", runtime.intParameterizedType());
        String encoded = "[\"L\",\"x\",\"Tint\"]";
        assertEquals(encoded, codec.encodeVariable(context, lv).toString());
        assertEquals(lv, codec.decodeVariable(context, makeD(encoded)));
    }

    @DisplayName("local variable with array type (L)")
    @Test
    public void testLocalVariableArrayType() {
        context.push(typeInfo);
        LocalVariable lv = runtime.newLocalVariable("arr", runtime.intParameterizedType().copyWithArrays(1));
        String encoded = "[\"L\",\"arr\",[\"Tint\",1,[]]]";
        assertEquals(encoded, codec.encodeVariable(context, lv).toString());
        assertEquals(lv, codec.decodeVariable(context, makeD(encoded)));
    }

    @DisplayName("dependent / array-access variable (D)")
    @Test
    public void testDependentVariable() {
        context.push(typeInfo);
        LocalVariable arr = runtime.newLocalVariable("arr", runtime.intParameterizedType().copyWithArrays(1));
        LocalVariable idx = runtime.newLocalVariable("i", runtime.intParameterizedType());
        DependentVariable dv = runtime.newDependentVariable(
                runtime.newVariableExpression(arr), runtime.newVariableExpression(idx));
        String encoded = codec.encodeVariable(context, dv).toString();
        assertEquals(dv, codec.decodeVariable(context, makeD(encoded)));
    }
}
