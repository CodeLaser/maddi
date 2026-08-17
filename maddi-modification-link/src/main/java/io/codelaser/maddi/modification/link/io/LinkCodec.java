package io.codelaser.maddi.modification.link.io;

import io.codelaser.maddi.modification.link.impl.LinkedVariablesImpl;
import io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl;
import io.codelaser.maddi.modification.link.impl.Result;
import io.codelaser.maddi.modification.link.impl.localvar.AppliedFunctionalInterfaceVariable;
import io.codelaser.maddi.modification.link.impl.localvar.FunctionalInterfaceVariable;
import io.codelaser.maddi.modification.link.impl.localvar.MarkerVariable;
import io.codelaser.maddi.modification.link.vf.VirtualFieldComputer;
import io.codelaser.maddi.modification.prepwork.Util;
import io.codelaser.maddi.modification.prepwork.variable.Links;
import io.codelaser.maddi.modification.prepwork.variable.ReturnVariable;
import io.codelaser.maddi.modification.prepwork.variable.impl.LinksImpl;
import io.codelaser.maddi.modification.prepwork.variable.impl.ReturnVariableImpl;
import io.codelaser.maddi.cst.api.analysis.Codec;
import io.codelaser.maddi.cst.api.analysis.Property;
import io.codelaser.maddi.cst.api.analysis.Value;
import io.codelaser.maddi.cst.api.element.SourceSet;
import io.codelaser.maddi.cst.api.expression.Expression;
import io.codelaser.maddi.cst.api.expression.IntConstant;
import io.codelaser.maddi.cst.api.info.*;
import io.codelaser.maddi.cst.api.runtime.Runtime;
import io.codelaser.maddi.cst.api.type.ParameterizedType;
import io.codelaser.maddi.cst.api.variable.DependentVariable;
import io.codelaser.maddi.cst.api.variable.Variable;
import io.codelaser.maddi.cst.impl.analysis.PropertyProviderImpl;
import io.codelaser.maddi.cst.impl.analysis.ValueImpl;
import io.codelaser.maddi.cst.io.CodecImpl;
import io.codelaser.maddi.inspection.api.integration.JavaInspector;
import org.jetbrains.annotations.NotNull;
import org.parsers.json.Node;
import org.parsers.json.ast.StringLiteral;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.codelaser.maddi.modification.link.impl.MethodLinkedVariablesImpl.METHOD_LINKS;
import static io.codelaser.maddi.modification.prepwork.callgraph.ComputePartOfConstructionFinalField.PART_OF_CONSTRUCTION;

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
            return javaInspector.compiledTypesManager().type(fqn, sourceSetOfRequest);
        };
        decoderProvider = new D();
        this.propertyProvider = new P();
        this.sourceSetOfRequest = sourceSetOfRequest;
        this.runtime = javaInspector.runtime();
    }

    public Codec codec() {
        return new C();
    }

    /** checkpoint-restore variant (task #34): already-present values win, decode fills the gaps */
    public Codec restoreCodec() {
        return new C() {
            @Override
            protected boolean skipExistingValues() {
                return true;
            }
        };
    }

    class C extends CodecImpl {
        public C() {
            super(runtime, propertyProvider, decoderProvider, typeProvider, sourceSetOfRequest);
        }

        /**
         * One written definition of a marker variable: the element it was written under, and the definition
         * itself, as it appears in the file (null while it is being built, see the encode side).
         */
        private record MarkerDef(Info owner, String definition) {
        }

        /**
         * Marker-variable definitions, by name, then by owning element.
         * <p>
         * Marker variables are numbered PER METHOD by the link computer ($_ce0, $_fi0, $_afi0, ...; see the note
         * in {@code LinkComputerImpl.SourceMethodComputer}, whose "cross-method collisions are harmless" holds
         * inside the engine, where linking graphs are per-method, but not here, where one file carries every
         * method of every type). Keyed by the bare name for a whole file, as this was, the first $_ce0 written
         * won the name and every later one -- a different variable, of a different method -- decoded to it: 28
         * of the 44 back-references in the transform-support archive resolved across elements, one of them
         * handing a Loop method a marker typed Try.TryData.
         * <p>
         * So a definition belongs to the element it is written under, and only a repeat under that same element
         * may be the bare back-reference {@code ["m", name]}. A marker that genuinely recurs under another
         * element -- same name AND byte-identical definition -- still costs one definition: its repeats are
         * back-references that name their owner, {@code ["m", name, owner]}. The owner is omitted exactly when
         * it is the element being written, which is the common case.
         */
        private final Map<String, List<MarkerDef>> encodedMarkerVariables = new HashMap<>();

        /**
         * The element a marker variable written here belongs to. A parameter shares its method's scope: they are
         * one evaluation, and the link computer numbers markers once per method.
         */
        private Info ownerOfMarkerVariables(Context context) {
            if (context.isEmpty()) return null; // no element on the stack: one file-wide scope, as before
            return switch (context.peek(0)) {
                case ParameterInfo pi -> pi.methodInfo();
                case TypeAndSorted tas -> tas.typeInfo();
                case Info info -> info;
                case Object o -> throw new UnsupportedOperationException("Unexpected codec context element " + o);
            };
        }

        private EncodedValue markerBackReference(Context context, String name, Info owner) {
            if (owner == null) {
                return encodeList(context, List.of(encodeString(context, "m"), encodeString(context, name)));
            }
            return encodeList(context, List.of(encodeString(context, "m"), encodeString(context, name),
                    encodeInfoOutOfContext(context, owner)));
        }

        private EncodedValue encodeMarkerVariableDefinition(Context context, MarkerVariable mv, String name) {
            if (mv instanceof AppliedFunctionalInterfaceVariable afi) {
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
            if (mv instanceof FunctionalInterfaceVariable fiv) {
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
                // because they may contain an extensive definition, we cache these marker variables
                // the name has an "M" appended, so that it does not clash with marker variables generated from
                // sources
                String name = mv.simpleName() + "M";
                Info owner = ownerOfMarkerVariables(context);
                List<MarkerDef> defs = encodedMarkerVariables.computeIfAbsent(name, _ -> new ArrayList<>());
                if (defs.stream().anyMatch(d -> Objects.equals(owner, d.owner()))) {
                    return markerBackReference(context, name, null); // repeat under its own element
                }
                // The pending entry goes in BEFORE the definition is built, so that a self-referential marker
                // (an applied functional interface whose own result mentions it) meets the back-reference above
                // instead of recursing. Building can register and nest further definitions, hence the snapshot:
                // dropping this definition below has to drop those with it, or they stand registered as written
                // while the only copy of them goes unwritten.
                Map<String, List<MarkerDef>> snapshot = new HashMap<>();
                encodedMarkerVariables.forEach((k, v) -> snapshot.put(k, new ArrayList<>(v)));
                defs.add(new MarkerDef(owner, null));
                EncodedValue definition = encodeMarkerVariableDefinition(context, mv, name);
                String written = ((E) definition).s();
                MarkerDef identical = encodedMarkerVariables.get(name).stream()
                        .filter(d -> written.equals(d.definition())).findFirst().orElse(null);
                if (identical != null) {
                    // the same marker, already written under another element: point at it, definition and all
                    encodedMarkerVariables.clear();
                    encodedMarkerVariables.putAll(snapshot);
                    return markerBackReference(context, name, identical.owner());
                }
                encodedMarkerVariables.get(name).replaceAll(d ->
                        d.definition() == null && Objects.equals(owner, d.owner()) ? new MarkerDef(owner, written) : d);
                return definition;
            }
            // SharedVariable/IntermediateVariable have no codec branch by design: they are kept out of
            // summaries by acceptForLinkedVariables filtering. If one leaks here, the fallback would
            // silently encode it as an ordinary local — fail at the cause instead.
            assert !(variable instanceof io.codelaser.maddi.modification.link.impl.localvar.SharedVariable)
                   && !(variable instanceof io.codelaser.maddi.modification.link.impl.localvar.IntermediateVariable)
                    : "engine-internal variable leaked into persistence: " + variable;
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

        /**
         * Decoded marker variables, by owning element, then by name -- mirroring {@link #encodedMarkerVariables},
         * whose note explains why the name alone is not a key. The reader pushes the same elements the writer
         * did ({@code LoadAnalysisResults.processSub}), so the owner of an unqualified back-reference is simply
         * the element being read.
         */
        private final Map<Info, Map<String, MarkerVariable>> decodedMarkerVariables = new HashMap<>();

        private void putDecodedMarkerVariable(Context context, String name, MarkerVariable mv) {
            decodedMarkerVariables.computeIfAbsent(ownerOfMarkerVariables(context), _ -> new HashMap<>())
                    .put(name, mv);
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
            if ("m".equals(s)) {
                String name = decodeString(context, list.get(1));
                // a third element names the element the definition was written under; without it, this one
                Info owner = list.size() > 2 ? decodeInfoOutOfContext(context, list.get(2))
                        : ownerOfMarkerVariables(context);
                MarkerVariable mv = decodedMarkerVariables.getOrDefault(owner, Map.of()).get(name);
                assert mv != null : "Cannot find " + name + " under " + owner;
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
                putDecodedMarkerVariable(context, name, afi);
                return afi;
            }
            if ("f".equals(s)) {
                String name = decodeString(context, list.get(1));
                ParameterizedType type = decodeType(context, list.get(2));
                Result result = decodeResult(context, list.get(3));
                FunctionalInterfaceVariable fiv = new FunctionalInterfaceVariable(name, type, runtime, result);
                putDecodedMarkerVariable(context, name, fiv);
                return fiv;
            }
            if ("M".equals(s)) {
                String name = decodeString(context, list.get(1));
                ParameterizedType pt = decodeType(context, list.get(2));
                if (list.size() == 3) {
                    // cache like the other branches: the encode side emits an "m" back-reference for a
                    // repeated marker name, and someValue markers all share the literal name "$_v" —
                    // without this put, the second occurrence hit the "Cannot find" assert above
                    MarkerVariable emptyMv = new MarkerVariable(name, pt, runtime.newEmptyExpression());
                    putDecodedMarkerVariable(context, name, emptyMv);
                    return emptyMv;
                }
                Expression ae = decodeExpression(context, list.get(3));
                MarkerVariable mv = new MarkerVariable(name, pt, ae);
                putDecodedMarkerVariable(context, name, mv);
                return mv;
            }
            return super.decodeVariable(context, s, list);
        }

        private final Set<TypeInfo> duplication = new HashSet<>();

        @Override
        public Stream<EncodedValue> encodeInfoOutOfContextStream(Context context, TypeAndSorted tas, Info info) {
            if (info instanceof TypeInfo ti && Util.isContainerType(ti)) {
                String s = "U" + ti.simpleName();
                Stream<EncodedValue> pre = encodeInfoOutOfContextStream(context,
                        null, // tas unused for a type recursion: the base recomputes it from the info
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
            // the base now derives the sub-type index from the enclosing type itself, so we no longer need to
            // pre-compute the enclosing TypeAndSorted here (this used to work around a base-class bug). For a
            // method/field/parameter the base needs the owning type's TypeAndSorted, which is info.typeInfo().
            return super.encodeInfoOutOfContextStream(context, new TypeAndSorted(info.typeInfo()), info);
        }

        private @NotNull Stream<EncodedValue> streamSyntheticFieldDetails(Context context, FieldInfo fi, String s) {
            Stream<EncodedValue> pre = encodeInfoOutOfContextStream(context,
                    null, // tas unused for a type recursion: the base recomputes it from the info
                    fi.owner());
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
        protected DR decodeInfo(Context context,
                                Info currentType,
                                TypeAndSorted typeAndSorted,
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
                return new DR(containerType, new TypeAndSorted(containerType));
            }
            if ('V' == type || 'G' == type) {
                // decode synthetic field (virtual, or created via makeComparableSub)
                TypeInfo owner = currentType.typeInfo();
                assert list.size() == pos + 2; // pre, this, one extra
                ParameterizedType fieldType = decodeType(context, list.get(pos + 1));
                FieldInfo newField = VirtualFieldComputer.newFieldKeepName(runtime, name, fieldType, owner);
                return new DR(newField, typeAndSorted);
            }
            return super.decodeInfo(context, currentType, typeAndSorted, type, name, list, pos);
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
            if (type.typeInfo() != null && Util.isContainerType(type.typeInfo())) {
                if (!duplication.contains(type.typeInfo())) {
                    Stream<EncodedValue> name = Stream.of(encodeString(context, "V" + type.typeInfo().fullyQualifiedName()));
                    Stream<EncodedValue> arrays = Stream.of(encodeInt(context, type.arrays()));
                    // this one will add to duplication
                    Stream<EncodedValue> typeStream = encodeInfoOutOfContextStream(context,
                            null, // tas unused for a type recursion: the base recomputes it from the info
                            type.typeInfo());
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
            LinksImpl.LINKS.key(), LinksImpl.LINKS,
            // prepwork call-graph property, present on checkpointed methods (task #34)
            io.codelaser.maddi.modification.prepwork.callgraph.ComputeCallGraph.RECURSIVE_METHOD.key(),
            io.codelaser.maddi.modification.prepwork.callgraph.ComputeCallGraph.RECURSIVE_METHOD);

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

