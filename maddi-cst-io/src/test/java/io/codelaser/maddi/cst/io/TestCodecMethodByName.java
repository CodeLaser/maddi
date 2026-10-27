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

import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.cst.api.info.Info;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A method reference with a unique (non-overloaded) name decodes by name; the encoded index is a fast-path hint.
 * This makes decode robust when the loaded method set differs from the encoder's — the real transform.jar case was
 * a synthetic {@code <clinit>} present in bytecode but not the encoder's source view, which shifted every index so
 * a token for {@code analyzedPackages} landed on {@code <clinit>} ("Method names do not agree").
 */
public class TestCodecMethodByName extends CommonTest {

    // typeInfo (a.b.C) starts with field 'f' and subtype 'sub'; add two abstract methods so index != name.
    private void pushTwoMethodType() {
        MethodInfo alpha = runtime.newMethod(typeInfo, "alpha", runtime.methodTypeAbstractMethod());
        MethodInfo beta = runtime.newMethod(typeInfo, "beta", runtime.methodTypeAbstractMethod());
        typeInfo.builder().addMethod(alpha);
        typeInfo.builder().addMethod(beta);
        alpha.builder().commit();
        beta.builder().commit();
        context.push(typeInfo);
    }

    private Info decode(String token) {
        return codec.decodeInfoInContext(context, makeD("\"" + token + "\""));
    }

    private MethodInfo method(String name) {
        return typeInfo.methodStream().filter(mi -> mi.name().equals(name)).findFirst().orElseThrow();
    }

    @DisplayName("fast path: a correct index resolves directly")
    @Test
    public void testCorrectIndex() {
        pushTwoMethodType();
        // sorted by FQN: alpha(0), beta(1)
        assertSame(method("beta"), decode("Mbeta(1)"));
        assertSame(method("alpha"), decode("Malpha(0)"));
    }

    @DisplayName("stale index: the (unique) name wins over a wrong index (the <clinit> shape)")
    @Test
    public void testStaleIndexResolvedByName() {
        pushTwoMethodType();
        // index 0 is 'alpha', but the token names 'beta' -> must return beta by name
        assertSame(method("beta"), decode("Mbeta(0)"));
        // out-of-range index, still resolved by name
        assertSame(method("alpha"), decode("Malpha(99)"));
    }

    @DisplayName("unknown method name: a clear DecoderException")
    @Test
    public void testUnknownName() {
        pushTwoMethodType();
        assertThrows(Codec.DecoderException.class, () -> decode("Mzzz(0)"));
    }
}
