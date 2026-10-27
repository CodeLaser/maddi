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
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.info.MethodInfo;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * #30: a decoded {@code SetOfMethodInfo} is a registry like any other. Prep's addImplementation adds to it when a
 * decoded hint landed on a type the project also defines in source; the decoder used to hand out an unmodifiable
 * set, and the first add threw {@code UnsupportedOperationException}.
 */
public class TestCodecSetOfMethodInfo extends CommonTest {

    @DisplayName("a decoded set of methods accepts additions")
    @Test
    public void test() {
        MethodInfo alpha = runtime.newMethod(typeInfo, "alpha", runtime.methodTypeAbstractMethod());
        MethodInfo beta = runtime.newMethod(typeInfo, "beta", runtime.methodTypeAbstractMethod());
        typeInfo.builder().addMethod(alpha);
        typeInfo.builder().addMethod(beta);
        alpha.builder().commit();
        beta.builder().commit();
        context.push(typeInfo);

        Value.SetOfMethodInfo original = new ValueImpl.SetOfMethodInfoImpl();
        assertTrue(original.add(alpha));
        // encode -> toString -> JSON parse (makeD) -> decode, as the other codec round-trips do
        Codec.EncodedValue encoded = makeD(original.encode(codec, context).toString());

        Value.SetOfMethodInfo decoded = ValueImpl.SetOfMethodInfoImpl.from(codec, context, encoded);
        assertEquals(Set.of(alpha), StreamSupport.stream(decoded.methodInfoSet().spliterator(), false)
                .collect(java.util.stream.Collectors.toSet()));
        // the add that used to throw
        assertTrue(decoded.add(beta));
        assertEquals(Set.of(alpha, beta), StreamSupport.stream(decoded.methodInfoSet().spliterator(), false)
                .collect(java.util.stream.Collectors.toSet()));
    }
}
