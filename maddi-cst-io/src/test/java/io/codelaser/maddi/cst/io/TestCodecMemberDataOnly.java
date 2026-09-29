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
