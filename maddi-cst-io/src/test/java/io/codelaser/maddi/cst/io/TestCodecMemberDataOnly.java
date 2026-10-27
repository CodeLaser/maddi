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
import io.codelaser.maddi.cst.api.element.CompilationUnit;
import io.codelaser.maddi.cst.api.info.TypeInfo;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.impl.analysis.PropertyProviderImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An element with no data of its own but with members that carry some must be written. The codec dropped it:
 * an interface computed mutable (the default, so no type-level value) lost its methods' verdicts, and Eclipse
 * Collections' RichIterable (181 methods) and MutableList (67) were silently absent from the written results
 * of a source run. An element without data anywhere is still not written.
 */
public class TestCodecMemberDataOnly {
    private final Runtime runtime = new RuntimeImpl();

    @DisplayName("no type data, one method with data: written; no data anywhere: not written")
    @Test
    public void test() throws IOException {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("a.b").build();
        TypeInfo typeInfo = runtime.newTypeInfo(cu, "C");
        Codec codec = new CodecImpl(runtime, PropertyProviderImpl::get, ValueImpl::decoder,
                fqn -> runtime.getFullyQualified(fqn, true), cu.sourceSet());
        Codec.Context context = new CodecImpl.ContextImpl();

        Codec.EncodedValue method = new CodecImpl.E("\"name\": \"Mm(0)\", \"data\":{\"nonModifyingMethod\":1}", List.of());
        List<Codec.EncodedValue> subs = Arrays.asList(null, method); // a member without data encodes as null

        Codec.EncodedValue typeWithMemberData = codec.encode(context, typeInfo, "", Stream.empty(), subs);
        assertNotNull(typeWithMemberData, "the members' data must survive a type without data of its own");
        StringWriter sw = new StringWriter();
        ((CodecImpl.E) typeWithMemberData).write(sw, 0, true);
        String written = sw.toString().replaceAll("\\s+", "");
        assertTrue(written.contains("\"name\":\"Ta.b.C\",\"data\":{}"), written);
        assertTrue(written.contains("\"nonModifyingMethod\":1"), written);

        assertNull(codec.encode(context, typeInfo, "", Stream.empty(), List.of()), "nothing at all: not written");
        assertNull(codec.encode(context, typeInfo, "", Stream.empty(), Arrays.asList(null, null)), "members without data");
    }
}
