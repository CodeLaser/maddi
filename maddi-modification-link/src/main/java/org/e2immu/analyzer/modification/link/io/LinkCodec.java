package org.e2immu.analyzer.modification.link.io;

import org.e2immu.analyzer.modification.link.impl.LinkNatureImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
import org.e2immu.analyzer.modification.prepwork.variable.LinkNature;
import org.e2immu.analyzer.modification.prepwork.variable.Links;
import org.e2immu.analyzer.modification.prepwork.variable.ReturnVariable;
import org.e2immu.analyzer.modification.prepwork.variable.impl.LinksImpl;
import org.e2immu.analyzer.modification.prepwork.variable.impl.ReturnVariableImpl;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.element.SourceSet;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.IntConstant;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyProviderImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.io.CodecImpl;
import org.parsers.json.ast.StringLiteral;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static org.e2immu.analyzer.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION;

public class LinkCodec {

    private final Codec.TypeProvider typeProvider;
    private final Codec.DecoderProvider decoderProvider;
    private final Codec.PropertyProvider propertyProvider;
    private final Runtime runtime;
    private final SourceSet sourceSetOfRequest;

    public LinkCodec(Runtime runtime, SourceSet sourceSetOfRequest) {
        this.typeProvider = fqn -> runtime.getFullyQualified(fqn, true, sourceSetOfRequest);
        decoderProvider = new D();
        this.propertyProvider = new P();
        this.sourceSetOfRequest = sourceSetOfRequest;
        this.runtime = runtime;
    }

    public Codec codec() {
        return new C();
    }

    class C extends CodecImpl {
        public C() {
            super(runtime, propertyProvider, decoderProvider, typeProvider, sourceSetOfRequest);
        }

        @Override
        public EncodedValue encodeVariable(Context context, Variable variable) {
            if (variable instanceof DependentVariable dv && dv.indexExpression()
                    instanceof IntConstant ic && ic.constant() < 0) {
                List<EncodedValue> list = new ArrayList<>();
                list.add(encodeString(context, "D"));
                list.add(encodeExpression(context, dv.arrayExpression()));
                list.add(encodeExpression(context, dv.indexExpression()));
                // slice!
                list.add(encodeType(context, dv.parameterizedType()));
                return encodeList(context, list);
            }
            if (variable instanceof ReturnVariable rv) {
                return encodeList(context, List.of(encodeString(context, "R"),
                        encodeMethodInfo(context, rv.methodInfo())));
            }
            return super.encodeVariable(context, variable);
        }

        @Override
        public Variable decodeVariable(Context context, String s, List<EncodedValue> list) {
            if ("D".equals(s) && list.size() == 4) {
                Expression a = decodeExpression(context, list.get(1));
                Expression i = decodeExpression(context, list.get(2));
                ParameterizedType pt = decodeType(context, list.get(3));
                return runtime.newDependentVariable(a, i, pt);
            }
            if ("R".equals(s)) {
                MethodInfo methodInfo = decodeMethodInfo(context, list.get(1));
                return new ReturnVariableImpl(methodInfo);
            }
            return super.decodeVariable(context, s, list);
        }

        @Override
        public Stream<EncodedValue> encodeInfoOutOfContextStream(Context context, Info info) {
            if (info instanceof TypeInfo ti && ti.typeNature() == VirtualFieldComputer.VIRTUAL_FIELD) {
                String s = "U" + ti.simpleName();
                Stream<EncodedValue> prev = encodeInfoOutOfContextStream(context,
                        ti.compilationUnitOrEnclosingType().getRight());
                Stream<EncodedValue> post = ti.fields()
                        .stream().map(f -> encodeInfoOutOfContext(context, f));
                return Stream.concat(Stream.concat(prev, Stream.of(encodeString(context, s))),
                        post);
            }
            if (info instanceof FieldInfo fi && Util.virtual(fi)) {
                Stream<EncodedValue> pre = encodeInfoOutOfContextStream(context, fi.owner());
                String s = "V" + fi.name();
                Stream<EncodedValue> post = Stream.of(encodeType(context, fi.type()));
                return Stream.concat(Stream.concat(pre, Stream.of(encodeString(context, s))), post);
            }
            return super.encodeInfoOutOfContextStream(context, info);
        }

        private final Map<String, TypeInfo> virtualTypes = new HashMap<>();

        @Override
        protected ParameterizedType decodeSimpleType(Context context, StringLiteral sl) {
            String fqn = unquote(sl.getSource());
            TypeInfo virtualType = virtualTypes.get(fqn);
            if (virtualType != null) {
                // virtual types have no type parameters
                return virtualType.asSimpleParameterizedType();
            }
            return super.decodeSimpleType(context, sl);
        }

        @Override
        protected Info decodeInfo(Context context,
                                  Info currentType,
                                  char type,
                                  String name,
                                  List<EncodedValue> list,
                                  int pos) {
            if ('U' == type) {
                // decode virtual container type
                List<EncodedValue> tail = list.subList(pos + 1, list.size());
                int arrays = decodeInt(context, tail.getFirst());
                List<FieldInfo> fields = tail.stream().skip(1)
                        .map(ev -> (FieldInfo) decodeInfoOutOfContext(context, ev))
                        .toList();
                String typeName = fields.stream().map(this::nameComponent).collect(Collectors.joining())
                                  + "S".repeat(arrays);
                TypeInfo owner = context.currentType();
                TypeInfo containerType = VirtualFieldComputer.makeContainer(runtime, owner, typeName, fields);
                virtualTypes.put(containerType.fullyQualifiedName(), containerType);
                return VirtualFieldComputer.newField(runtime, typeName.toLowerCase(), containerType.asParameterizedType(),
                        owner);
            }
            if ('V' == type) {
                // decode virtual field
                TypeInfo owner = context.currentType();
                assert list.size() == pos + 2; // pre, this, one extra
                ParameterizedType fieldType = decodeType(context, list.get(pos + 1));
                return VirtualFieldComputer.newField(runtime, name, fieldType, owner);
            }
            return super.decodeInfo(context, currentType, type, name, list, pos);
        }

        private String nameComponent(FieldInfo fieldInfo) {
            String base;
            if (fieldInfo.type().typeParameter() != null) {
                base = fieldInfo.type().typeParameter().simpleName();
            } else {
                base = VirtualFieldComputer.VF_CONCRETE;
            }
            return base + "S".repeat(fieldInfo.type().arrays());
        }
    }

    private static final Map<String, Property> PROPERTY_MAP = Map.of(
            PART_OF_CONSTRUCTION.key(), PART_OF_CONSTRUCTION,
            METHOD_LINKS.key(), METHOD_LINKS);

    static class P implements Codec.PropertyProvider {
        @Override
        public Property get(String propertyName) {
            Property inMap = PROPERTY_MAP.get(propertyName);
            if (inMap != null) return inMap;
            return PropertyProviderImpl.get(propertyName);
        }
    }

    static class D implements Codec.DecoderProvider {

        @Override
        public BiFunction<Codec.DI, Codec.EncodedValue, Value> decoder(Class<? extends Value> clazz) {
            if (MethodLinkedVariablesImpl.class.equals(clazz)) {
                return (di, ev) -> MethodLinkedVariablesImpl.decode(di.codec(), di.context(), ev);
            }
            if (LinksImpl.class.equals(clazz)) {
                return (di, ev) -> decodeLinks(di.codec(), di.context(), ev);
            }
            // part of construction uses "set of info", which is in ValueImpl.
            return ValueImpl.decoder(clazz);
        }
    }


    public static Links decodeLinks(Codec codec, Codec.Context context, Codec.EncodedValue encodedValue) {
        List<Codec.EncodedValue> list = codec.decodeList(context, encodedValue);
        if (list.isEmpty()) return LinksImpl.EMPTY;
        Variable primary = codec.decodeVariable(context, list.getFirst());
        LinksImpl.Builder builder = new LinksImpl.Builder(primary);
        list.stream().skip(1).forEach(ev -> decodeLink(codec, context, ev, builder));
        return builder.build();
    }

    private static void decodeLink(Codec codec,
                                   Codec.Context context,
                                   Codec.EncodedValue ev,
                                   LinksImpl.Builder builder) {
        List<Codec.EncodedValue> list = codec.decodeList(context, ev);
        Variable from = codec.decodeVariable(context, list.getFirst());
        LinkNature linkNature = LinkNatureImpl.decode(codec.decodeString(context, list.get(1)));
        Variable to = codec.decodeVariable(context, list.getLast());
        builder.add(from, linkNature, to);
    }
}

