package org.e2immu.language.cst.io;

import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Property;
import org.e2immu.language.cst.api.analysis.PropertyValueMap;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.expression.VariableExpression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.api.type.Wildcard;
import org.e2immu.language.cst.api.variable.*;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.parsers.json.Node;
import org.parsers.json.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CodecImpl implements Codec {
    private static final Logger LOGGER = LoggerFactory.getLogger(CodecImpl.class);

    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("(.+)\\.([^.]+)");
    private static final Pattern NAME_INDEX_PATTERN = Pattern.compile("(.+)\\((\\d+)\\)");
    private static final Pattern NUM_PATTERN = Pattern.compile("-?\\.?[0-9]+");
    private final DecoderProvider decoderProvider;
    private final TypeProvider typeProvider;
    private final PropertyProvider propertyProvider;
    private final Runtime runtime;

    public CodecImpl(Runtime runtime, PropertyProvider propertyProvider, DecoderProvider decoderProvider, TypeProvider typeProvider) {
        this.decoderProvider = decoderProvider;
        this.typeProvider = typeProvider;
        this.propertyProvider = propertyProvider;
        this.runtime = runtime;
    }

    public static String potentiallyUnquote(String s) {
        if (!s.isEmpty() && s.charAt(0) == '"') return unquote(s);
        return s;
    }

    public static String quote(String s) {
        return "\"" + s.replace("\"", "\\\"") + "\"";
    }

    private static String quoteNumber(String s) {
        if (NUM_PATTERN.matcher(s).matches()) return quote(s);
        return s;
    }

    public static String unquote(String s) {
        String s1 = s.substring(1, s.length() - 1);
        return s1.replace("\\\"", "\"");
    }

    @Override
    public int constructorIndex(MethodInfo methodInfo) {
        int i = 0;
        for (MethodInfo mi : methodInfo.typeInfo().constructors()) {
            if (mi == methodInfo) return i;
            ++i;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public void decode(Context context,
                       PropertyValueMap pvm,
                       Stream<EncodedPropertyValue> encodedPropertyValueStream) {
        encodedPropertyValueStream.map(epv -> {
            String key = epv.key();
            Property property = propertyProvider.get(key);
            assert property != null : "Have no property object for key " + key;
            Class<? extends Value> clazz = property.classOfValue();
            BiFunction<DI, EncodedValue, Value> decoder = decoderProvider.decoder(clazz);
            D d = (D) epv.encodedValue();
            Value value = decoder.apply(new DII(this, context), d);
            return new PropertyValue(property, value);
        }).forEach(pv -> {
            // the GET_SET_FIELD property can already be set (by GetSetUtil) at byte-code loading
            if (!pv.property().equals(PropertyImpl.GET_SET_FIELD) || !pvm.haveAnalyzedValueFor(PropertyImpl.GET_SET_FIELD)) {
                pvm.set(pv.property(), pv.value());
            }
        });
    }

    @Override
    public boolean decodeBoolean(Context context, EncodedValue encodedValue) {
        if (encodedValue instanceof D(Node s) && s instanceof Literal l) {
            return "true".equals(l.getSource());
        } else throw new UnsupportedOperationException();
    }

    private MethodInfo decodeConstructor(TypeInfo typeInfo, String fqnNameIndex) {
        Matcher m = NAME_INDEX_PATTERN.matcher(fqnNameIndex);
        if (m.matches()) {
            int index = Integer.parseInt(m.group(2));
            MethodInfo methodInfo = typeInfo.constructors().get(index);
            assert methodInfo.isConstructor();
            return methodInfo;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Expression decodeExpression(Context context, EncodedValue value) {
        return new ExpressionCodec(runtime, this, context).decodeExpression(value);
    }

    @Override
    public FieldInfo decodeFieldInfo(TypeInfo typeInfo, EncodedValue ev) {
        String s = decodeString(null, ev);
        return decodeFieldInfo(typeInfo, s.substring(1));
    }

    private FieldInfo decodeFieldInfo(TypeInfo typeInfo, String nameIndex) {
        Matcher m = NAME_INDEX_PATTERN.matcher(nameIndex);
        if (m.matches()) {
            int index = Integer.parseInt(m.group(2));
            if (index >= typeInfo.fields().size()) {
                throw new UnsupportedOperationException("Index " + index
                                                        + " greater than the number of fields in " + typeInfo);
            }
            FieldInfo fieldInfo = typeInfo.fields().get(index);
            assert fieldInfo.name().equals(m.group(1));
            return fieldInfo;
        } else {
            Matcher m2 = FIELD_NAME_PATTERN.matcher(nameIndex);
            if (m2.matches()) {
                TypeInfo owner = runtime.getFullyQualified(m2.group(1), true);
                return owner.getFieldByName(m2.group(2), true);
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    private Info decodeInfo(Info current, char type, String name) {
        return switch (type) {
            case 'F' -> decodeFieldInfo((TypeInfo) current, name);
            case 'C' -> decodeConstructor((TypeInfo) current, name);
            case 'M' -> decodeMethodInfo((TypeInfo) current, name);
            case 'T' -> typeProvider.get(name);
            case 'S' -> decodeSubType((TypeInfo) current, name);
            case 'P' -> decodeParameterInfo((MethodInfo) current, name);
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public Info decodeInfoInContext(Context context, EncodedValue ev) {
        if (ev instanceof D(Node s) && s instanceof StringLiteral sl) {
            String source = unquote(sl.getSource());
            char type = source.charAt(0);
            String name = source.substring(1);
            return decodeInfoInContext(context, type, name);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Info decodeInfoInContext(Context context, char type, String name) {
        return switch (type) {
            case 'F' -> decodeFieldInfo(context.currentType(), name);
            case 'C' -> decodeConstructor(context.currentType(), name);
            case 'M' -> decodeMethodInfo(context.currentType(), name);
            case 'T' -> typeProvider.get(name);
            case 'S' -> decodeSubType(context.currentType(), name);
            case 'P' -> decodeParameterInfo(context.currentMethod(), name);
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public Info decodeInfoOutOfContext(Context context, EncodedValue encodedValue) {
        List<EncodedValue> list = decodeList(context, encodedValue);
        Info current = null;
        for (EncodedValue ev : list) {
            if (ev instanceof D(Node s) && s instanceof StringLiteral sl) {
                String src = unquote(sl.getSource());
                current = decodeInfo(current, src.charAt(0), src.substring(1));
            } else throw new UnsupportedOperationException();
        }
        return current;
    }

    @Override
    public int decodeInt(Context context, EncodedValue encodedValue) {
        if (encodedValue instanceof D(Node s) && s instanceof Literal l) {
            return Integer.parseInt(potentiallyUnquote(l.getSource()));
        } else throw new UnsupportedOperationException();
    }

    @Override
    public List<EncodedValue> decodeList(Context context, EncodedValue encodedValue) {
        List<EncodedValue> list = new LinkedList<>();
        if (encodedValue instanceof D(Node s) && s instanceof Array jo && jo.size() > 2) {
            for (int i = 1; i < jo.size(); i += 2) {
                list.add(new D(jo.get(i)));
            }
        }
        return list;
    }

    @Override
    public Map<EncodedValue, EncodedValue> decodeMap(Context context, EncodedValue encodedValue) {
        Map<EncodedValue, EncodedValue> map = new LinkedHashMap<>();
        if (encodedValue instanceof D(Node s) && s instanceof JSONObject jo && jo.size() > 2) {
            // kv pairs, starting with 1 unless empty
            for (int i = 1; i < jo.size(); i += 2) {
                if (jo.get(i) instanceof KeyValuePair kvp) {
                    map.put(new D(kvp.getFirst()), new D(kvp.get(2)));
                } else throw new UnsupportedOperationException();
            }
        }
        return map;
    }

    @Override
    public Map<EncodedValue, EncodedValue> decodeMapAsList(Context context, EncodedValue encodedValue) {
        Map<EncodedValue, EncodedValue> map = new LinkedHashMap<>();
        List<EncodedValue> list = decodeList(context, encodedValue);
        Iterator<EncodedValue> it = list.iterator();
        while (it.hasNext()) {
            EncodedValue key = it.next();
            if (!it.hasNext()) throw new UnsupportedOperationException();
            EncodedValue value = it.next();
            map.put(key, value);
        }
        return map;
    }

    private MethodInfo decodeMethodInfo(TypeInfo typeInfo, String nameIndex) {
        Matcher m = NAME_INDEX_PATTERN.matcher(nameIndex);
        if (m.matches()) {
            int index = Integer.parseInt(m.group(2));
            MethodInfo methodInfo = typeInfo.methods().get(index);
            assert !methodInfo.isConstructor();
            assert methodInfo.name().equals(m.group(1)) : "Method names do not agree: " + methodInfo + " vs " + m.group(1);
            return methodInfo;
        } else throw new UnsupportedOperationException();
    }

    private ParameterInfo decodeParameterInfo(MethodInfo methodInfo, String nameIndex) {
        Matcher m = NAME_INDEX_PATTERN.matcher(nameIndex);
        if (m.matches()) {
            int index = Integer.parseInt(m.group(2));
            if (index >= methodInfo.parameters().size()) {
                LOGGER.error("Method {} has {} parameters, looking for index {}",
                        methodInfo, methodInfo.parameters().size(), index);
                throw new DecoderException("parameter " + index + " of " + methodInfo + " does not exist");
            }
            return methodInfo.parameters().get(index);
        } else throw new UnsupportedOperationException();
    }

    @Override
    public Set<EncodedValue> decodeSet(Context context, EncodedValue encodedValue) {
        Set<EncodedValue> list = new LinkedHashSet<>();
        if (encodedValue instanceof D(Node s) && s instanceof Array jo && jo.size() > 2) {
            for (int i = 1; i < jo.size(); i += 2) {
                list.add(new D(jo.get(i)));
            }
        }
        return list;
    }

    private ParameterizedType decodeSimpleType(Context context, StringLiteral sl) {
        String fqn = unquote(sl.getSource());
        if (fqn.isEmpty()) return null;
        char first = fqn.charAt(0);
        return switch (first) {
            case 'X' -> runtime.parameterizedTypeNullConstant();
            case '?' -> runtime.parameterizedTypeWildcard();
            case 'T' -> context.findType(typeProvider, fqn.substring(1)).asSimpleParameterizedType();
            case 'M' -> {
                // method type parameter in current context
                int index = Integer.parseInt(fqn.substring(1));
                yield context.currentMethod().typeParameters().get(index).asParameterizedType();
            }
            case 'P' -> {
                // type parameter in current context
                int index = Integer.parseInt(fqn.substring(1));
                yield context.currentType().typeParameters().get(index).asParameterizedType();
            }
            default -> throw new UnsupportedOperationException("TODO: " + fqn);
        };
    }

    @Override
    public String decodeString(Context context, EncodedValue encodedValue) {
        if (encodedValue instanceof D(Node s) && s instanceof StringLiteral l) {
            return unquote(l.getSource());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private TypeInfo decodeSubType(TypeInfo typeInfo, String nameIndex) {
        Matcher m = NAME_INDEX_PATTERN.matcher(nameIndex);
        if (m.matches()) {
            int index = Integer.parseInt(m.group(2));
            assert typeInfo != null;
            TypeInfo subType = typeInfo.subTypes().get(index);
            assert subType.simpleName().equals(m.group(1));
            return subType;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ParameterizedType decodeType(Context context, EncodedValue encodedValue) {
        if (encodedValue instanceof D(Node s) && s instanceof StringLiteral sl) {
            return decodeSimpleType(context, sl);
        }
        List<EncodedValue> list = decodeList(context, encodedValue);

        // list index 0: named type
        int i = 1;
        NamedType nt;
        if (list.getFirst() instanceof D(Node s) && s instanceof StringLiteral sl) {
            String fqn = unquote(sl.getSource());
            char first = fqn.charAt(0);
            if ('T' == first) {
                nt = context.findType(typeProvider, fqn.substring(1));
            } else {
                int index = Integer.parseInt(fqn.substring(1));
                boolean ownerNotInContext = !(list.get(i) instanceof D(Node s1) && s1 instanceof NumberLiteral);
                if ('M' == first) {
                    MethodInfo owner = ownerNotInContext ? (MethodInfo) this.decodeInfoInContext(context, list.get(i)) : context.currentMethod();
                    nt = owner.typeParameters().get(index);
                } else if ('P' == first) {
                    TypeInfo owner = ownerNotInContext ? (TypeInfo) this.decodeInfoInContext(context, list.get(i)) : context.currentType();
                    nt = owner.typeParameters().get(index);
                } else throw new UnsupportedOperationException();
                if (ownerNotInContext) {
                    ++i;
                }
            }
        } else throw new UnsupportedOperationException();

        // list index 2: arrays
        int arrays;
        if (list.get(i) instanceof D(Node s1) && s1 instanceof NumberLiteral nl) {
            arrays = Integer.parseInt(nl.getSource());
        } else throw new UnsupportedOperationException();

        // list index 3: type parameters
        ++i;
        List<ParameterizedType> parameters = decodeList(context, list.get(i)).stream().map(ev -> decodeType(context, ev)).toList();

        // list index 4, optional: wildcard
        Wildcard wildCard;
        ++i;
        if (list.size() <= i) {
            wildCard = null;
        } else if (list.get(i) instanceof D(Node s2) && s2 instanceof StringLiteral sl2) {
            String src = unquote(sl2.getSource());
            wildCard = "E".equals(src) ? runtime.wildcardExtends() : runtime.wildcardSuper();
        } else throw new UnsupportedOperationException();

        // create
        if (nt instanceof TypeInfo ti) {
            return runtime.newParameterizedType(ti, arrays, wildCard, parameters);
        }
        if (nt instanceof TypeParameter tp) {
            return runtime.newParameterizedType(tp, arrays, wildCard);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Variable decodeVariable(Context context, EncodedValue encodedValue) {
        List<EncodedValue> list = decodeList(context, encodedValue);
        assert !list.isEmpty();
        String s = decodeString(context, list.get(0));
        return switch (s) {
            case "P" -> (ParameterInfo) this.decodeInfoOutOfContext(context, list.get(1));
            case "F" -> {
                FieldInfo fieldInfo = (FieldInfo) this.decodeInfoOutOfContext(context, list.get(1));
                if (list.size() == 2) {
                    yield runtime.newFieldReference(fieldInfo);
                }
                Expression scope = decodeExpression(context, list.get(2));
                yield runtime.newFieldReference(fieldInfo, scope, fieldInfo.type());
            }
            case "T" -> {
                TypeInfo typeInfo = (TypeInfo) this.decodeInfoOutOfContext(context, list.get(1));
                yield runtime.newThis(typeInfo.asParameterizedType());
            }
            case "L" -> {
                String name = decodeString(context, list.get(1));
                ParameterizedType type = decodeType(context, list.get(2));
                yield runtime.newLocalVariable(name, type);
            }
            case "D" -> {
                Expression a = decodeExpression(context, list.get(1));
                Expression i = decodeExpression(context, list.get(2));
                if (a instanceof VariableExpression ve) {
                    // explicitly mention the type, to allow for this[xxx] in StaticValues
                    yield runtime.newDependentVariable(ve, i, ve.parameterizedType());
                }
                yield runtime.newDependentVariable(a, i);
            }
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public EncodedValue encode(Context context,
                               Info info,
                               String index,
                               Stream<EncodedPropertyValue> encodedPropertyValueStream,
                               List<EncodedValue> subs) {
        String fqn = encodeInfoInContextToString(context, info, index);
        String pvStream = encodedPropertyValueStream
                .filter(epv -> epv.encodedValue() != null)
                .map(epv -> '"' + epv.key() + "\":" + ((E) epv.encodedValue()).s)
                .sorted()
                .collect(Collectors.joining(",", "{", "}"));
        if ("{}".equalsIgnoreCase(pvStream)) {
            // no data, we'll not write
            return null;
        }
        String all = "\"name\": " + quote(fqn) + ", \"data\":" + pvStream;
        return new E(all, subs);
    }

    @Override
    public EncodedValue encodeBoolean(Context context, boolean value) {
        return new E(Boolean.toString(value));
    }

    @Override
    public EncodedValue encodeExpression(Context context, Expression expression) {
        return new ExpressionCodec(runtime, this, context).encodeExpression(expression);
    }

    @Override
    public EncodedValue encodeInfoInContext(Context context, Info info, String index) {
        return new E(quote(encodeInfoInContextToString(context, info, index)));
    }

    private String encodeInfoInContextToString(Context context, Info info, String index) {
        if (info instanceof TypeInfo typeInfo) {
            if (typeInfo.isPrimaryType()) {
                return "T" + typeInfo.fullyQualifiedName();
            }
            if (typeInfo.isAnonymous()) {
                assert context.currentMethod() != null;
                return "A" + typeInfo.enclosingMethod() + "(" + index + ")";
            }
            return "S" + typeInfo.simpleName() + "(" + index + ")";
        }
        assert index != null && !index.isBlank();
        if (info instanceof MethodInfo methodInfo) {
            assert methodInfo.typeInfo() == context.currentType();
            if (methodInfo.isConstructor()) {
                return "C" + MethodInfo.CONSTRUCTOR_NAME + "(" + index + ")";
            }
            return "M" + methodInfo.name() + "(" + index + ")";
        }
        if (info instanceof FieldInfo fieldInfo) {
            if (context.currentType() == fieldInfo.owner()) {
                return "F" + fieldInfo.name() + "(" + index + ")";
            }
            return "F" + fieldInfo.fullyQualifiedName();
        }
        if (info instanceof ParameterInfo pi) {
            assert context.currentMethod() == pi.methodInfo();
            return "P" + pi.name() + "(" + index + ")";
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public EncodedValue encodeInfoOutOfContext(Context context, Info info) {
        return encodeList(context, encodeInfoOutOfContextStream(context, info).toList());
    }

    Stream<EncodedValue> encodeInfoOutOfContextStream(Context context, Info info) {
        String s;
        Stream<EncodedValue> prev;
        switch (info) {
            case TypeInfo typeInfo -> {
                if (typeInfo.isPrimaryType()) {
                    s = "T" + typeInfo.fullyQualifiedName();
                    prev = Stream.of();
                } else {
                    assert !typeInfo.isAnonymous();
                    int index = subTypeIndex(typeInfo);
                    s = "S" + typeInfo.simpleName() + "(" + index + ")";
                    prev = encodeInfoOutOfContextStream(context, typeInfo.compilationUnitOrEnclosingType().getRight());
                }
            }
            case MethodInfo methodInfo -> {
                prev = encodeInfoOutOfContextStream(context, methodInfo.typeInfo());
                if (methodInfo.isConstructor()) {
                    s = "C" + MethodInfo.CONSTRUCTOR_NAME + "(" + constructorIndex(methodInfo) + ")";
                } else {
                    s = "M" + methodInfo.name() + "(" + methodIndex(methodInfo) + ")";
                }
            }
            case FieldInfo fieldInfo -> {
                prev = encodeInfoOutOfContextStream(context, fieldInfo.owner());
                s = "F" + fieldInfo.name() + "(" + fieldIndex(fieldInfo) + ")";
            }
            case ParameterInfo pi -> {
                prev = encodeInfoOutOfContextStream(context, pi.methodInfo());
                s = "P" + pi.name() + "(" + pi.index() + ")";
            }
            case null, default -> throw new UnsupportedOperationException();
        }
        return Stream.concat(prev, Stream.of(encodeString(context, s)));
    }

    @Override
    public EncodedValue encodeInt(Context context, int value) {
        return new E(Integer.toString(value));
    }

    @Override
    public EncodedValue encodeList(Context context, List<EncodedValue> encodedValues) {
        String e = encodedValues.stream().map(ev -> ((E) ev).s)
                .collect(Collectors.joining(",", "[", "]"));
        return new E(e);
    }

    @Override
    public EncodedValue encodeMap(Context context, Map<EncodedValue, EncodedValue> map) {
        String encoded = map.entrySet().stream()
                .map(e -> quoteNumber(((E) e.getKey()).s) + ":" + ((E) e.getValue()).s)
                .sorted()
                .collect(Collectors.joining(",", "{", "}"));
        return new E(encoded);
    }

    private record R(EncodedValue key, EncodedValue value) implements Comparable<R> {
        @Override
        public int compareTo(R o) {
            return ((E) key).s.compareTo(((E) o.key).s);
        }
    }

    @Override
    public EncodedValue encodeMapAsList(Context context, Map<EncodedValue, EncodedValue> map) {
        List<EncodedValue> list = map.entrySet().stream().map(e -> new R(e.getKey(), e.getValue()))
                .sorted().flatMap(r -> Stream.of(r.key, r.value)).toList();
        return encodeList(context, list);
    }

    private EncodedValue encodeOwnerOfTypeParameter(Context context, TypeParameter typeParameter) {
        if (typeParameter.isMethodTypeParameter()) {
            MethodInfo methodInfo = typeParameter.getOwner().getRight();
            if (methodInfo.equals(context.currentMethod())) return null;
            return encodeInfoInContext(context, methodInfo, "" + methodIndex(methodInfo));
        }
        TypeInfo typeInfo = typeParameter.getOwner().getLeft();
        if (typeInfo.equals(context.currentType())) return null;
        return encodeInfoInContext(context, typeInfo, null);
    }

    @Override
    public EncodedValue encodeSet(Context context, Set<EncodedValue> set) {
        String e = set.stream().map(ev -> ((E) ev).s).sorted()
                .collect(Collectors.joining(",", "[", "]"));
        return new E(e);
    }

    @Override
    public EncodedValue encodeString(Context context, String string) {
        return new E(quote(string));
    }

    @Override
    public EncodedValue encodeType(Context context, ParameterizedType type) {
        if (type == null) return encodeString(context, "");
        String s0;
        if (type.typeInfo() != null) {
            s0 = "T" + type.typeInfo().fullyQualifiedName();
        } else if (type.typeParameter() != null) {
            s0 = (type.typeParameter().isMethodTypeParameter() ? "M" : "P") + type.typeParameter().getIndex();
        } else {
            s0 = type.isUnboundWildcard() ? "?" : "X";
        }
        EncodedValue e0 = encodeString(context, s0);
        EncodedValue e1 = type.isTypeParameter() ? encodeOwnerOfTypeParameter(context, type.typeParameter()) : null;
        if (type.arrays() == 0 && type.parameters().isEmpty()
            && e1 == null && (type.wildcard() == null || type.wildcard().isUnbound())) {
            return e0;
        }
        EncodedValue e2 = encodeInt(context, type.arrays());
        EncodedValue e3 = encodeList(context, type.parameters().stream().map(p -> encodeType(context, p)).toList());
        EncodedValue e4 = encodeWildcard(context, type.wildcard());
        return encodeList(context, Stream.concat(Stream.concat(Stream.concat(
                Stream.concat(Stream.of(e0), Stream.ofNullable(e1)), Stream.of(e2)), Stream.of(e3)), Stream.ofNullable(e4)).toList());
    }

    @Override
    public EncodedValue encodeVariable(Context context, Variable variable) {
        return switch (variable) {
            case ParameterInfo pi ->
                    encodeList(context, List.of(encodeString(context, "P"), encodeInfoOutOfContext(context, pi)));
            case FieldReference fr -> {
                EncodedValue f = encodeString(context, "F");
                EncodedValue encodedFieldInfo = encodeInfoOutOfContext(context, fr.fieldInfo());
                if (fr.isDefaultScope()) {
                    yield encodeList(context, List.of(f, encodedFieldInfo));
                }
                yield encodeList(context, List.of(f, encodedFieldInfo, encodeExpression(context, fr.scope())));
            }
            case This thisVar -> encodeList(context, List.of(encodeString(context, "T"),
                    encodeInfoOutOfContext(context, thisVar.typeInfo())));
            case LocalVariable lv ->
                    encodeList(context, List.of(encodeString(context, "L"), encodeString(context, lv.simpleName()),
                            encodeType(context, lv.parameterizedType())));
            case DependentVariable dv -> encodeList(context, List.of(encodeString(context, "D"),
                    encodeExpression(context, dv.arrayExpression()), encodeExpression(context, dv.indexExpression())));
            default -> throw new UnsupportedOperationException();
        };
    }

    private EncodedValue encodeWildcard(Context context, Wildcard wildcard) {
        if (wildcard == null || wildcard.isUnbound()) return null;
        String s;
        if (wildcard.isExtends()) s = "E";
        else if (wildcard.isSuper()) s = "S";
        else throw new UnsupportedOperationException();
        return encodeString(context, s);
    }

    @Override
    public int fieldIndex(FieldInfo fieldInfo) {
        int i = 0;
        for (FieldInfo fi : fieldInfo.owner().fields()) {
            if (fi == fieldInfo) return i;
            ++i;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isList(EncodedValue encodedValue) {
        if (encodedValue instanceof D(Node s)) {
            return s instanceof Array;
        }
        throw new UnsupportedOperationException("Expect D object, not E object");
    }

    @Override
    public int methodIndex(MethodInfo methodInfo) {
        int i = 0;
        for (MethodInfo mi : methodInfo.typeInfo().methods()) {
            if (mi == methodInfo) return i;
            ++i;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public PropertyProvider propertyProvider() {
        return propertyProvider;
    }

    @Override
    public int subTypeIndex(TypeInfo typeInfo) {
        int i = 0;
        for (TypeInfo sub : typeInfo.compilationUnitOrEnclosingType().getRight().subTypes()) {
            if (sub == typeInfo) return i;
            ++i;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public TypeProvider typeProvider() {
        return typeProvider;
    }

    public record D(Node s) implements EncodedValue {
    }

    public record E(String s, List<EncodedValue> subs) implements EncodedValue {
        public E(String s) {
            this(s, null);
        }

        @Override
        public String toString() {
            try (StringWriter sw = new StringWriter()) {
                write(sw, Integer.MIN_VALUE, false);
                return sw.toString();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }
        }

        public void write(Writer writer, int tab, boolean surround) throws IOException {
            if (tab >= -1) {
                writer.write("\n");
                if (tab >= 0) writer.write(" ".repeat(tab));
            }
            if (surround) writer.write("{");
            writer.write(s);
            if (subs != null && !subs.isEmpty()) {
                if (subs.size() == 1) {
                    EncodedValue sub0 = subs.getFirst();
                    if (sub0 != null) {
                        writer.write(", \"sub\":");
                        ((E) sub0).write(writer, tab + 1, true);
                    }
                } else {
                    StringWriter sw = new StringWriter();
                    boolean first = true;
                    boolean wrote = false;
                    for (EncodedValue sub : subs) {
                        if (sub != null) {
                            if (first) first = false;
                            else sw.append(",");
                            ((E) sub).write(sw, tab + 1, true);
                            wrote = true;
                        }
                    }
                    if (wrote) {
                        writer.write(", \"subs\":[");
                        writer.write(sw.toString());
                        writer.write("]");
                    }
                }
            }
            if (surround) writer.write("}");
        }
    }

    public record DII(Codec codec, Context context) implements DI {
    }

    public static class ContextImpl implements Context {
        private final Stack<Info> stack = new Stack<>();

        @Override
        public MethodInfo currentMethod() {
            for (int i = 0; i < stack.size(); i++) {
                if (peek(i) instanceof MethodInfo mi) return mi;
            }
            return null;
        }

        @Override
        public TypeInfo currentType() {
            for (int i = 0; i < stack.size(); i++) {
                if (peek(i) instanceof TypeInfo ti) return ti;
            }
            return null;
        }

        @Override
        public TypeInfo findType(Codec.TypeProvider typeProvider, String typeFqn) {
            for (int i = 0; i < stack.size(); i++) {
                if (peek(i) instanceof TypeInfo ti && typeFqn.equals(ti.fullyQualifiedName())) return ti;
            }
            return typeProvider.get(typeFqn);
        }

        @Override
        public boolean isEmpty() {
            return stack.isEmpty();
        }

        @Override
        public boolean methodBeforeType() {
            for (int i = 0; i < stack.size(); i++) {
                Info peek = peek(i);
                if (peek instanceof TypeInfo) return false;
                if (peek instanceof MethodInfo) return true;
            }
            return false;
        }

        @Override
        public Info peek(int stepsBack) {
            return stack.elementAt(stack.size() - 1 - stepsBack);
        }

        @Override
        public Info pop() {
            return stack.pop();
        }

        @Override
        public void push(Info info) {
            stack.push(info);
        }
    }
}
