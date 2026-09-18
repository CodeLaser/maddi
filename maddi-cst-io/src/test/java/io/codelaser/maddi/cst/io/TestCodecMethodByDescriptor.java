/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 */

package io.codelaser.maddi.cst.io;

import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.api.info.ParameterInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A method token carries the erased parameter types, {@code Mname(index,p1,p2)}, because for an OVERLOADED name a
 * stale index leaves nothing else to go on — the name does not discriminate, and picking either overload would be a
 * guess. The archive hit this for real: a Kotlin stdlib contract for {@code mapOf} was dropped with "ambiguous
 * method 'mapOf' (2 overloads) with a stale index", the encoder having seen a multifile part class's method list
 * and the decoder the Kotlin front end's.
 * <p>
 * The parameter types are OPTIONAL in the token, so every archive written before they existed — the 27 JDK files
 * among them — decodes exactly as it did.
 */
public class TestCodecMethodByDescriptor extends CommonTest {

    /** A second `max`, of one argument: now the name alone cannot say which is meant. */
    private MethodInfo pushOverloadedType() {
        MethodInfo other = runtime.newMethod(sub, "max", runtime.methodTypeAbstractMethod());
        ParameterInfo p = other.builder().addParameter("p0", runtime.stringParameterizedType());
        p.builder().commit();
        sub.builder().addMethod(other);
        context.push(typeInfo);
        context.push(sub);
        return other;
    }

    private Info decode(String token) {
        return codec.decodeInfoInContext(context, makeD("\"" + token + "\""));
    }

    @DisplayName("an overload is resolved by its parameter types, whatever the index says")
    @Test
    public void testOverloadResolvedByDescriptor() {
        MethodInfo other = pushOverloadedType();
        // both indices deliberately wrong: only the types can tell these apart
        assertSame(max, decode("Mmax(99,int,int)"));
        assertSame(other, decode("Mmax(99,java.lang.String)"));
    }

    @DisplayName("a token written before parameter types existed still decodes")
    @Test
    public void testIndexOnlyTokenStillDecodes() {
        context.push(typeInfo);
        context.push(sub);
        assertSame(max, decode("Mmax(0)"));
    }

    @DisplayName("an overload whose parameter types match nothing is a clear DecoderException")
    @Test
    public void testNoSuchOverload() {
        pushOverloadedType();
        assertThrows(Codec.DecoderException.class, () -> decode("Mmax(99,double)"));
    }
}
