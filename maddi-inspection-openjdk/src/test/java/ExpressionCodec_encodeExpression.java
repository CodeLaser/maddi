import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ExpressionCodec_encodeExpression {
    Codec codec;
    Context context;
    Map<String, ECodec> map;
    Runtime runtime;
    class Codec {
        EncodedValue encodeList(Context context, List<EncodedValue> encodedValues) { return null; }
        EncodedValue encodeString(Context context, String string) { return null; }
    }
    class Context { }
    class ECodec {List<EncodedValue> encode(Expression e) { return null; } }
    class EncodedValue { }
    class Expression {String name() { return null; }Source source() { return null; } }
    class Runtime {Source noSource() { return null; } }
    class Source {String compact2() { return null; } }

    public EncodedValue encodeExpression(Expression e) {
        if (e == null) {
            return codec.encodeList(context, List.of());
        }
        ECodec eCodec = map.get(e.name());
        assert eCodec != null : "No codec yet for " + e.name();
        List<EncodedValue> list = eCodec.encode(e);
        EncodedValue e0 = codec.encodeString(context, e.name());
        Source source = e.source() != null ? e.source() : runtime.noSource();
        EncodedValue e1 = codec.encodeString(context, source.compact2());
        return codec.encodeList(context, Stream.concat(Stream.of(e0, e1), list.stream()).toList());
    }
}
