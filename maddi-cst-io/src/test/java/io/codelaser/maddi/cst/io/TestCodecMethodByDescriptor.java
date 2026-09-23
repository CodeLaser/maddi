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

import java.util.Comparator;
import java.util.List;

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

    /**
     * The shape that bit the JDK hints: a new JDK inserts a method, every later member's index shifts by one, and a
     * stale index then lands IN RANGE on the neighbouring overload of the same name. Measured 2026-09-23: loading the
     * JDK-26 hints on JDK 27 (which adds {@code String.encodedLength}) resolved 38 method tokens to the wrong
     * overload, two of them with different data ({@code String.getBytes}). The name-only fast path accepted them.
     */
    @DisplayName("a stale index landing on a same-named overload is overruled by the parameter types")
    @Test
    public void testStaleIndexOnNeighbouringOverload() {
        MethodInfo other = pushOverloadedType();
        List<MethodInfo> sorted = sub.methods().stream()
                .sorted(Comparator.comparing(MethodInfo::fullyQualifiedName)).toList();
        int indexOfMax = sorted.indexOf(max);
        int indexOfOther = sorted.indexOf(other);
        // premise: each index, taken alone, points at a real method named 'max'
        assertSame(max, decode("Mmax(" + indexOfMax + ",int,int)"));
        assertSame(other, decode("Mmax(" + indexOfOther + ",java.lang.String)"));
        // the swap: each token's index points at the OTHER overload
        assertSame(max, decode("Mmax(" + indexOfOther + ",int,int)"));
        assertSame(other, decode("Mmax(" + indexOfMax + ",java.lang.String)"));
    }

    /**
     * The encoder writes the parameter types whenever there are any ({@code CodecImpl.methodToken}), so a token
     * WITHOUT types is a zero-argument method. With a stale index on an overloaded name that is the only thing left to
     * go on -- on JDK 27 with the JDK-26 hints, {@code BigDecimal.abs()} and 112 other Guava-reachable tokens were
     * skipped as "ambiguous" and their hints lost.
     */
    @DisplayName("a type-less token with a stale index resolves to the zero-argument overload")
    @Test
    public void testZeroArgumentOverload() {
        MethodInfo noArgs = runtime.newMethod(sub, "max", runtime.methodTypeAbstractMethod());
        sub.builder().addMethod(noArgs);
        context.push(typeInfo);
        context.push(sub);
        List<MethodInfo> sorted = sub.methods().stream()
                .sorted(Comparator.comparing(MethodInfo::fullyQualifiedName)).toList();
        assertSame(noArgs, decode("Mmax(99)"));
        // the index lands on the two-argument overload: the missing types say it is the wrong one
        assertSame(noArgs, decode("Mmax(" + sorted.indexOf(max) + ")"));
        assertSame(max, decode("Mmax(" + sorted.indexOf(noArgs) + ",int,int)"));
    }

    /**
     * Constructors were resolved by index ALONE, without even the name check methods had: on JDK 27 with the JDK-26
     * hints, {@code BigDecimal}'s constructor tokens landed on neighbouring constructors ("BigDecimal.<init>(long)
     * has 1 parameters, looking for index 1") and their parameter hints were attached to the wrong one or dropped.
     */
    @DisplayName("a constructor token is resolved by its parameter types when its index is stale")
    @Test
    public void testConstructorStaleIndex() {
        MethodInfo ofString = runtime.newConstructor(typeInfo);
        ofString.builder().addParameter("s", runtime.stringParameterizedType()).builder().commit();
        ofString.builder().commit();
        MethodInfo ofTwoInts = runtime.newConstructor(typeInfo);
        ofTwoInts.builder().addParameter("a", runtime.intParameterizedType()).builder().commit();
        ofTwoInts.builder().addParameter("b", runtime.intParameterizedType()).builder().commit();
        ofTwoInts.builder().commit();
        typeInfo.builder().addConstructor(ofString).addConstructor(ofTwoInts);
        context.push(typeInfo);
        List<MethodInfo> sorted = typeInfo.constructors().stream()
                .sorted(Comparator.comparing(MethodInfo::fullyQualifiedName)).toList();
        int iString = sorted.indexOf(ofString);
        int iInts = sorted.indexOf(ofTwoInts);
        // premise
        assertSame(ofString, decode("C<init>(" + iString + ",java.lang.String)"));
        // the swap: each index points at the other constructor
        assertSame(ofString, decode("C<init>(" + iInts + ",java.lang.String)"));
        assertSame(ofTwoInts, decode("C<init>(" + iString + ",int,int)"));
        assertThrows(Codec.DecoderException.class, () -> decode("C<init>(0,double)"));
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
