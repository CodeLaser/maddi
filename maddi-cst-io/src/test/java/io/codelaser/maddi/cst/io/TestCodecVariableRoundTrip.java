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

import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.FieldReference;
import io.codelaser.maddi.cst.api.variable.LocalVariable;
import io.codelaser.maddi.cst.api.variable.This;
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
        String encoded = "[\"P\",[\"Ta.b.C\",\"SSub(0)\",\"Mmax(0,int,int)\",\"Pp1(1)\"]]";
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
