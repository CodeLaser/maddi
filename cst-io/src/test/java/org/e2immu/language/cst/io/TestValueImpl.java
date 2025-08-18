package org.e2immu.language.cst.io;

import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.impl.analysis.PropertyProviderImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.impl.runtime.RuntimeImpl;
import org.junit.jupiter.api.Test;
import org.parsers.json.JSONParser;
import org.parsers.json.Node;
import org.parsers.json.ast.Array;
import org.parsers.json.ast.JSONObject;
import org.parsers.json.ast.KeyValuePair;
import org.parsers.json.ast.StringLiteral;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TestValueImpl {
    Runtime runtime = new RuntimeImpl();
    Codec.DecoderProvider decoderProvider = ValueImpl::decoder;
    Codec.PropertyProvider propertyProvider = PropertyProviderImpl::get;
    Codec.TypeProvider typeProvider = fqn -> runtime.getFullyQualified(fqn, true);
    CodecImpl codec = new CodecImpl(runtime, propertyProvider, decoderProvider, typeProvider);

    @Test
    public void test() {
        Codec.Context context = new CodecImpl.ContextImpl();
        ValueImpl.IndependentImpl i = new ValueImpl.IndependentImpl(1, Map.of(0, 0));
        assertEquals("@Independent(hc=true, dependentParameters={0})", i.toString());
        Codec.EncodedValue ev = i.encode(codec, context);
        assertEquals("[1,{\"0\":0}]", ev.toString());

        Value.Independent i2 = decodeIndependent(codec, context, ev);
        assertTrue(i2.isIndependentHc());
        assertEquals(0, i2.linkToParametersReturnValue().get(0));
    }

    private Value.Independent decodeIndependent(CodecImpl codec, Codec.Context context, Codec.EncodedValue ev) {
        JSONParser parser = new JSONParser("{\"a\":" + ev + "}");
        parser.Root();
        Node root = parser.rootNode();
        if (root.get(0) instanceof JSONObject jo) {
            if (jo.get(1) instanceof KeyValuePair kvp) {
                if (kvp.get(0) instanceof StringLiteral sl) {
                    assertEquals("\"a\"", sl.getSource());
                } else fail();
                if (kvp.get(2) instanceof Array a) {
                    assertEquals(ev.toString(), a.getSource());
                    CodecImpl.D d = new CodecImpl.D(a);
                    return ValueImpl.decodeIndependentImpl(codec, context, d);
                } else fail();
            }
        }
        throw new UnsupportedOperationException();
    }

    @Test
    public void test2() {
        ValueImpl.IndependentImpl i = new ValueImpl.IndependentImpl(1, Map.of(-1, 1, 1, 1));
        assertEquals("@Independent(hc=true, hcReturnValue=true, hcParameters={1})", i.toString());
        Codec.Context context = new CodecImpl.ContextImpl();

        Codec.EncodedValue ev = i.encode(codec, context);
        assertEquals("[1,{\"-1\":1,\"1\":1}]", ev.toString());
        Value.Independent i2 = decodeIndependent(codec, context, ev);
        assertTrue(i2.isIndependentHc());
        assertEquals(1, i2.linkToParametersReturnValue().get(1));
        assertEquals(1, i2.linkToParametersReturnValue().get(-1));
    }
}
