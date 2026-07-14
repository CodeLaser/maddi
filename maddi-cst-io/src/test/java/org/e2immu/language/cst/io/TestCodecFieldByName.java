/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 */

package org.e2immu.language.cst.io;

import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A field reference decodes by its (unique) name; the encoded positional index is a fast-path hint only. This makes
 * decode robust to a field-set that differs from the encoder's — e.g. private fields present on the source side but
 * historically absent from the bytecode loader — which used to surface as
 * {@code "field index N out of range; ... has 0 field(s)"} (the transform.jar / {@code ...Impl.Builder} case).
 */
public class TestCodecFieldByName extends CommonTest {

    // a fresh type with TWO fields, so "resolve by index" and "resolve by name" give different answers
    private void pushTwoFieldType() {
        FieldInfo a = runtime.newFieldInfo("a", false, runtime.intParameterizedType(), typeInfo);
        FieldInfo b = runtime.newFieldInfo("b", false, runtime.intParameterizedType(), typeInfo);
        // typeInfo already has 'f'; add a and b. Sorted by name: a, b, f.
        typeInfo.builder().addField(a);
        typeInfo.builder().addField(b);
        context.push(typeInfo);
    }

    private Info decode(String token) {
        return codec.decodeInfoInContext(context, makeD("\"" + token + "\""));
    }

    private FieldInfo field(String name) {
        return typeInfo.getFieldByName(name, true);
    }

    @DisplayName("fast path: a correct index resolves directly")
    @Test
    public void testCorrectIndex() {
        pushTwoFieldType();
        // sorted a(0), b(1), f(2)
        assertSame(field("b"), decode("Fb(1)"));
        assertSame(field("f"), decode("Ff(2)"));
    }

    @DisplayName("stale index: the name wins over a wrong index (the transform.jar shape)")
    @Test
    public void testStaleIndexResolvedByName() {
        pushTwoFieldType();
        // index 0 is 'a', but the token names 'b' -> must return b, not a
        assertSame(field("b"), decode("Fb(0)"));
        // index 1 is 'b', but the token names 'a' -> must return a
        assertSame(field("a"), decode("Fa(1)"));
    }

    @DisplayName("out-of-range index: still resolved by name")
    @Test
    public void testOutOfRangeIndexResolvedByName() {
        pushTwoFieldType();
        assertSame(field("b"), decode("Fb(99)"));
    }

    @DisplayName("unknown name: a clear DecoderException, not an AIOOBE")
    @Test
    public void testUnknownName() {
        pushTwoFieldType();
        assertThrows(Codec.DecoderException.class, () -> decode("Fzzz(0)"));
    }
}
