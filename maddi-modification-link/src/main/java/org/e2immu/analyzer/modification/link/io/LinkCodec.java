package org.e2immu.analyzer.modification.link.io;

import org.e2immu.analyzer.modification.link.impl.LinkedVariablesImpl;
import org.e2immu.analyzer.modification.link.impl.MethodLinkedVariablesImpl;
import org.e2immu.analyzer.modification.link.impl.Result;
import org.e2immu.analyzer.modification.link.impl.localvar.AppliedFunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.FunctionalInterfaceVariable;
import org.e2immu.analyzer.modification.link.impl.localvar.MarkerVariable;
import org.e2immu.analyzer.modification.link.vf.VirtualFieldComputer;
import org.e2immu.analyzer.modification.prepwork.Util;
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
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.variable.DependentVariable;
import org.e2immu.language.cst.api.variable.Variable;
import org.e2immu.language.cst.impl.analysis.PropertyProviderImpl;
import org.e2immu.language.cst.impl.analysis.ValueImpl;
import org.e2immu.language.cst.io.CodecImpl;
import org.e2immu.language.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;
import org.parsers.json.Node;
import org.parsers.json.ast.StringLiteral;

import java.util.*;
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

    public LinkCodec(JavaInspector javaInspector) {
        this(javaInspector, javaInspector.mainSources());
    }

    public LinkCodec(JavaInspector javaInspector, SourceSet sourceSetOfRequest) {
        this.typeProvider = fqn -> {
            TypeInfo primitive = javaInspector.runtime().primitiveByNameOrNull(fqn);
            if (primitive != null) return primitive;
            return javaInspector.compiledTypesManager().getOrLoad(fqn, sourceSetOfRequest);
        };
        decoderProvider = new D();
        this.propertyProvider = new P();
        this.sourceSetOfRequest = sourceSetOfRequest;
        this.runtime = javaInspector.runtime();
    }

    public Codec codec() {
        return new C();
    }

    class C extends CodecImpl {
        public C() {
            super(runtime, propertyProvider, decoderProvider, typeProvider, sourceSetOfRequest);
        }

        private final Set<String> encodedMarkerVariables = new HashSet<>();

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
            if (variable instanceof MarkerVariable mv) {
                String name = mv.simpleName() + "M";
                // because they may contain an extensive definition, we cache these marker variables
                // the name has an "M" appended, so that it does not clash with marker variables generated from
                // sources
                if (encodedMarkerVariables.add(name)) {
                    if (variable instanceof AppliedFunctionalInterfaceVariable afi) {
                        List<EncodedValue> list = new ArrayList<>();
                        Collections.addAll(list, encodeString(context, "a"),
                                encodeString(context, name),
                                encodeType(context, afi.parameterizedType()),
                                encodeList(context, afi.params().stream()
                                        .map(r -> encodeResult(context, r)).toList()));
                        if (afi.sourceOfFunctionalInterface() != null) {
                            list.add(encodeInfoOutOfContext(context, afi.sourceOfFunctionalInterface()));
                        }
                        return encodeList(context, list);
                    }
                    if (variable instanceof FunctionalInterfaceVariable fiv) {
                        return encodeList(context, List.of(encodeString(context, "f"),
                                encodeString(context, name),
                                encodeType(context, fiv.parameterizedType()),
                                encodeResult(context, fiv.result())
                        ));
                    }
                    if (mv.isSomeValue()) {
                        return encodeList(context, List.of(encodeString(context, "M"),
                                encodeString(context, name),
                                encodeType(context, mv.parameterizedType())));
                    }
                    assert mv.isConstant();
                    return encodeList(context, List.of(encodeString(context, "M"),
                            encodeString(context, name),
                            encodeType(context, mv.parameterizedType()),
                            encodeExpression(context, mv.assignmentExpression())));
                }
                return encodeList(context, List.of(encodeString(context, "m"), encodeString(context, name)));
            }
            return super.encodeVariable(context, variable);
        }

        // links, extra, modified
        public Result decodeResult(Context context, EncodedValue encodedValue) {
            List<EncodedValue> list = decodeList(context, encodedValue);
            Links links = MethodLinkedVariablesImpl.decodeLinks(this, context, list.get(0));
            List<List<EncodedValue>> extraList = decodeList(context, list.get(1)).stream().map(ev ->
                    decodeList(context, ev)).toList();
            Map<Variable, Links> extra = extraList.stream().collect(Collectors.toUnmodifiableMap(
                    evList -> decodeVariable(context, evList.getFirst()),
                    evList -> MethodLinkedVariablesImpl.decodeLinks(this, context,
                            evList.getLast())));
            List<List<EncodedValue>> modifiedList = decodeList(context, list.get(2))
                    .stream().map(ev -> decodeList(context, ev)).toList();
            Map<Variable, Set<MethodInfo>> modified = modifiedList.stream().collect(Collectors.toUnmodifiableMap(
                    l -> decodeVariable(context, l.getFirst()),
                    l -> l.size() == 1 ? Set.of() : decodeList(context, l.get(1)).stream()
                            .map(ev -> (MethodInfo) decodeInfoOutOfContext(context, ev))
                            .collect(Collectors.toUnmodifiableSet())));
            return new Result(links, new LinkedVariablesImpl(extra), modified, List.of(), Map.of(), Set.of(), Set.of());
        }

        public EncodedValue encodeResult(Context context, Result result) {
            EncodedValue links = result.links().encode(this, context);
            EncodedValue extra = encodeList(context, result.extra().stream()
                    .map(e -> encodeList(context, List.of(encodeVariable(context, e.getKey()),
                            e.getValue().encode(this, context)))).toList());
            EncodedValue modified = encodeList(context, result.modified().entrySet().stream()
                    .map(e -> encodeList(context, e.getValue() == null
                            ? List.of(encodeVariable(context, e.getKey()))
                            : List.of(encodeVariable(context, e.getKey()),
                            encodeList(context, e.getValue().stream()
                                    .map(mi -> encodeInfoOutOfContext(context, mi)).toList())))).toList());
            return encodeList(context, List.of(links, extra, modified));
        }

        private final Map<String, MarkerVariable> decodedMarkerVariables = new HashMap<>();

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
            if ("m".equals(s)) {
                String name = decodeString(context, list.get(1));
                MarkerVariable mv = decodedMarkerVariables.get(name);
                assert mv != null : "Cannot find" + name;
                return mv;
            }
            if ("a".equals(s)) {
                String name = decodeString(context, list.get(1));
                ParameterizedType type = decodeType(context, list.get(2));
                List<Result> params = decodeList(context, list.get(3)).stream()
                        .map(ev -> decodeResult(context, ev)).toList();
                ParameterInfo source = list.size() > 4
                        ? (ParameterInfo) decodeInfoOutOfContext(context, list.get(4)) : null;
                AppliedFunctionalInterfaceVariable afi = new AppliedFunctionalInterfaceVariable(name, type, runtime,
                        source, params);
                decodedMarkerVariables.put(name, afi);
                return afi;
            }
            if ("f".equals(s)) {
                String name = decodeString(context, list.get(1));
                ParameterizedType type = decodeType(context, list.get(2));
                Result result = decodeResult(context, list.get(3));
                FunctionalInterfaceVariable fiv = new FunctionalInterfaceVariable(name, type, runtime, result);
                decodedMarkerVariables.put(name, fiv);
                return fiv;
            }
            if ("M".equals(s)) {
                String name = decodeString(context, list.get(1));
                ParameterizedType pt = decodeType(context, list.get(2));
                if (list.size() == 3) {
                    return new MarkerVariable(name, pt, runtime.newEmptyExpression());
                }
                Expression ae = decodeExpression(context, list.get(3));
                MarkerVariable mv = new MarkerVariable(name, pt, ae);
                decodedMarkerVariables.put(mv.simpleName(), mv);
                return mv;
            }
            return super.decodeVariable(context, s, list);
        }

        private final Set<TypeInfo> duplication = new HashSet<>();

        @Override
        public Stream<EncodedValue> encodeInfoOutOfContextStream(Context context, Info info) {
            if (info instanceof TypeInfo ti && ti.typeNature() == VirtualFieldComputer.VIRTUAL_FIELD) {
                String s = "U" + ti.simpleName();
                Stream<EncodedValue> pre = encodeInfoOutOfContextStream(context,
                        ti.compilationUnitOrEnclosingType().getRight());
                Stream<EncodedValue> post;
                int n;
                if (duplication.add(ti)) {
                    post = ti.fields()
                            .stream().map(f -> encodeInfoOutOfContext(context, f));
                    n = ti.fields().size();
                } else {
                    post = Stream.empty();
                    n = 0;
                }
                return Stream.concat(Stream.concat(pre, Stream.of(encodeString(context, s))),
                        Stream.concat(Stream.of(encodeInt(context, n)), post));
            }
            if (info instanceof FieldInfo fi && Util.virtual(fi)) {
                String s = "V" + fi.name();
                return streamSyntheticFieldDetails(context, fi, s);
            }
            if (info instanceof FieldInfo fi) {
                int fieldIndex = fieldIndexOrNegative(fi);
                if (fieldIndex < 0) {
                    // LinkGraph.makeComparableSub has changed the owner...
                    String s = "G" + fi.name();
                    return streamSyntheticFieldDetails(context, fi, s);
                }
            }
            return super.encodeInfoOutOfContextStream(context, info);
        }

        private @NotNull Stream<EncodedValue> streamSyntheticFieldDetails(Context context, FieldInfo fi, String s) {
            Stream<EncodedValue> pre = encodeInfoOutOfContextStream(context, fi.owner());
            Stream<EncodedValue> post = Stream.of(encodeType(context, fi.type()));
            return Stream.concat(Stream.concat(pre, Stream.of(encodeString(context, s))), post);
        }

        public int fieldIndexOrNegative(FieldInfo fieldInfo) {
            int i = 0;
            for (FieldInfo fi : fieldInfo.owner().fields()) {
                if (fi == fieldInfo) return i;
                ++i;
            }
            return -1;
        }

        private final Map<String, TypeInfo> virtualTypes = new HashMap<>();

        @Override
        protected ParameterizedType decodeSimpleType(Context context, StringLiteral sl) {
            String fqn = unquote(sl.getSource()).substring(1);
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
                int numFields = decodeInt(context, tail.getFirst());
                TypeInfo containerType;
                if (numFields == 0) {
                    String fqn = currentType.typeInfo().fullyQualifiedName() + "." + name;
                    containerType = virtualTypes.get(fqn);
                    assert containerType != null;
                } else {
                    List<FieldInfo> fields = tail.stream().skip(1).limit(numFields)
                            .map(ev -> (FieldInfo) decodeInfoOutOfContext(context, ev))
                            .toList();
                    String typeName = fields.stream().map(this::nameComponent).collect(Collectors.joining());
                    TypeInfo owner = currentType.typeInfo();
                    containerType = VirtualFieldComputer.makeContainer(runtime, owner, typeName, fields);
                }
                virtualTypes.put(containerType.fullyQualifiedName(), containerType);
                return containerType;
            }
            if ('V' == type || 'G' == type) {
                // decode synthetic field (virtual, or created via makeComparableSub)
                TypeInfo owner = currentType.typeInfo();
                assert list.size() == pos + 2; // pre, this, one extra
                ParameterizedType fieldType = decodeType(context, list.get(pos + 1));
                return VirtualFieldComputer.newFieldKeepName(runtime, name, fieldType, owner);
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

        @Override
        public EncodedValue encodeType(Context context, ParameterizedType type) {
            if (type.typeInfo() != null && type.typeInfo().typeNature() == VirtualFieldComputer.VIRTUAL_FIELD) {
                if (!duplication.contains(type.typeInfo())) {
                    Stream<EncodedValue> name = Stream.of(encodeString(context, "V" + type.typeInfo().fullyQualifiedName()));
                    Stream<EncodedValue> arrays = Stream.of(encodeInt(context, type.arrays()));
                    // this one will add to duplication
                    Stream<EncodedValue> typeStream = encodeInfoOutOfContextStream(context, type.typeInfo());
                    List<EncodedValue> list = Stream.concat(Stream.concat(name, arrays), typeStream).toList();
                    return encodeList(context, list);
                } else {
                    EncodedValue name = encodeString(context, "R" + type.typeInfo().fullyQualifiedName());
                    return encodeList(context, List.of(name, encodeInt(context, type.arrays())));
                }
            }
            return super.encodeType(context, type);
        }

        @Override
        protected ParameterizedType decodeComplexType(Context context, List<EncodedValue> list) {
            if (list.getFirst() instanceof D(Node s) && s instanceof StringLiteral sl) {
                String fqn = unquote(sl.getSource());
                char first = fqn.charAt(0);
                if ('V' == first) {
                    int arrays = decodeInt(context, list.get(1));
                    TypeInfo typeInfo = (TypeInfo) decodeInfoOutOfContext(context, list.subList(2, list.size()));
                    return runtime.newParameterizedType(typeInfo, arrays);
                } else if ('R' == first) {
                    TypeInfo typeInfo = virtualTypes.get(fqn.substring(1));
                    int arrays = decodeInt(context, list.get(1));
                    return runtime.newParameterizedType(typeInfo, arrays);
                }
            }
            return super.decodeComplexType(context, list);
        }
    }

    private static final Map<String, Property> PROPERTY_MAP = Map.of(
            PART_OF_CONSTRUCTION.key(), PART_OF_CONSTRUCTION,
            METHOD_LINKS.key(), METHOD_LINKS,
            LinksImpl.LINKS.key(), LinksImpl.LINKS);

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
                return (di, ev) -> MethodLinkedVariablesImpl.decodeLinks(di.codec(), di.context(), ev);
            }
            // part of construction uses "set of info", which is in ValueImpl.
            return ValueImpl.decoder(clazz);
        }
    }

}

