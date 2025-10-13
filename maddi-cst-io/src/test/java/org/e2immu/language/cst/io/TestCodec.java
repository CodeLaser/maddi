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

import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.element.CompilationUnit;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.impl.analysis.PropertyProviderImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.parsers.json.JSONParser;
import org.parsers.json.Node;
import org.parsers.json.ast.JSONObject;
import org.parsers.json.ast.KeyValuePair;
import org.parsers.json.ast.Root;
import org.parsers.json.ast.StringLiteral;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class TestCodec {
    private final Runtime runtime = new RuntimeImpl() {
        @Override
        public TypeInfo getFullyQualified(String name, boolean complain) {
            if ("int".equals(name)) return intTypeInfo();
            throw new UnsupportedOperationException();
        }
    };

    @Test
    public void test() {
        CompilationUnit cu = runtime.newCompilationUnitBuilder().setPackageName("a.b").build();
        TypeInfo typeInfo = runtime.newTypeInfo(cu, "C");

        typeInfo.analysis().set(PropertyImpl.IMMUTABLE_TYPE, ValueImpl.ImmutableImpl.IMMUTABLE);
        typeInfo.analysis().set(PropertyImpl.DEFAULTS_ANALYZER, ValueImpl.BoolImpl.TRUE);
        typeInfo.analysis().set(PropertyImpl.COMMUTABLE_METHODS,
                new ValueImpl.CommutableDataImpl("p1", "p2,p3", "p4"));

        Codec.DecoderProvider decoderProvider = ValueImpl::decoder;
        Codec codec = new CodecImpl(runtime, PropertyProviderImpl::get,
                decoderProvider, fqn -> runtime.getFullyQualified(fqn, true), cu.sourceSet());
        List<Property> properties = List.of(
                PropertyImpl.IMMUTABLE_TYPE,
                PropertyImpl.DEFAULTS_ANALYZER,
                PropertyImpl.COMMUTABLE_METHODS);
        Codec.Context context = new CodecImpl.ContextImpl();
        Stream<Codec.EncodedPropertyValue> epvStream = properties.stream().map(p ->
                codec.encode(context, p, typeInfo.analysis().getOrDefault(p, p.defaultValue())));
        String s = ((CodecImpl.E) codec.encode(context, typeInfo, "", epvStream, List.of())).s();
        assertEquals("""
                "name": "Ta.b.C", "data":{"commutableMethods":["p1","p2,p3","p4"],"defaultsAnalyzer":1,"immutableType":3}\
                """, s);
        JSONParser parser = new JSONParser("{" + s + "}");
        parser.Root();
        Node root = parser.rootNode();
        assertInstanceOf(Root.class, root);

        TypeInfo typeInfo2 = runtime.newTypeInfo(cu, "D");

        if (root.getFirst() instanceof JSONObject jo) {
            if (jo.get(1) instanceof KeyValuePair kvp) {
                if (kvp.get(0) instanceof StringLiteral sl) {
                    assertEquals("\"name\"", sl.getSource());
                } else fail();
                if (kvp.get(2) instanceof StringLiteral sl) {
                    assertEquals("\"Ta.b.C\"", sl.getSource());
                } else fail();
            } else fail();
            if (jo.get(3) instanceof KeyValuePair kvp) {
                if (kvp.get(0) instanceof StringLiteral sl) {
                    assertEquals("\"data\"", sl.getSource());
                } else fail();
                if (kvp.get(2) instanceof JSONObject jo2) {
                    List<Codec.EncodedPropertyValue> epvs = new ArrayList<>();
                    for (int i = 1; i < jo2.size(); i += 2) {
                        if (jo2.get(i) instanceof KeyValuePair kvp2) {
                            String key = CodecImpl.unquote(kvp2.get(0).getSource());
                            epvs.add(new Codec.EncodedPropertyValue(key, new CodecImpl.D(kvp2.get(2))));
                        }
                    }
                    codec.decode(context, typeInfo2.analysis(), epvs.stream());
                    assertEquals(3, typeInfo2.analysis().propertyValueStream().count());
                } else fail();
            } else fail();
        } else fail();

        Assertions.assertEquals("p2,p3", typeInfo2.analysis().getOrDefault(PropertyImpl.COMMUTABLE_METHODS,
                ValueImpl.CommutableDataImpl.NONE).par());
    }
}
