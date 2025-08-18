package org.e2immu.analyzer.modification.prepwork.hct;

import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.api.info.TypeParameter;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.cst.io.CodecImpl;
import org.parsers.json.ast.StringLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HiddenContentTypes implements Value {
    private static final Logger LOGGER = LoggerFactory.getLogger(HiddenContentTypes.class);

    public static HiddenContentTypes OF_PRIMITIVE = new HiddenContentTypes(null, false, Map.of(), Map.of());
    public static HiddenContentTypes NO_VALUE = new HiddenContentTypes(null, false, Map.of(), Map.of());

    // IMPORTANT: hc is lexicographically less that hcsMethod, hcsParameter, it has to be parsed earlier!
    public static PropertyImpl HIDDEN_CONTENT_TYPES = new PropertyImpl("hc", NO_VALUE);

    public static final int UNSPECIFIED_EXTENSION = -2; // extension of the type itself, only if typeIsExtensible == true

    private final TypeInfo typeInfo;
    private final boolean typeIsExtensible;
    private final int startOfMethodParameters;

    private final HiddenContentTypes hctTypeInfo;
    private final MethodInfo methodInfo;

    private final Map<NamedType, Integer> typeToIndex;
    private final Map<Integer, NamedType> indexToType;

    public static boolean hasHc(TypeInfo typeInfo) {
        HiddenContentTypes hct = typeInfo.analysis().getOrNull(HIDDEN_CONTENT_TYPES, HiddenContentTypes.class);
        assert hct != null;
        return hct.hasHiddenContent();
    }

    static HiddenContentTypes of(TypeInfo typeInfo) {
        return new HiddenContentTypes(typeInfo, false, Map.of(), Map.of());
    }

    static HiddenContentTypes of(TypeInfo typeInfo,
                                 boolean typeIsExtensible,
                                 Map<NamedType, Integer> myTypeToIndex,
                                 Map<NamedType, Integer> superTypeToIndex) {
        Map<NamedType, Integer> combined = new HashMap<>(myTypeToIndex);
        combined.putAll(superTypeToIndex);
        Map<Integer, NamedType> indexToType = new HashMap<>();
        superTypeToIndex.forEach((nt, i) -> indexToType.put(i, nt));
        // it is important here that the ones in myType have priority over the superTypes
        myTypeToIndex.forEach((nt, i) -> indexToType.put(i, nt));
        return new HiddenContentTypes(typeInfo, typeIsExtensible, Map.copyOf(combined), Map.copyOf(indexToType));
    }

    public static HiddenContentTypes of(MethodInfo methodInfo, Map<NamedType, Integer> typeToIndex) {
        TypeInfo methodType = methodInfo.typeInfo();
        HiddenContentTypes hctType = methodType.analysis().getOrCreate(HIDDEN_CONTENT_TYPES, () -> of(methodType));
        assert hctType.typeInfo == methodType : "Problem with HCT of " + methodInfo;
        return new HiddenContentTypes(hctType, methodInfo, typeToIndex, true);
    }

    private HiddenContentTypes(TypeInfo typeInfo, boolean typeIsExtensible,
                               Map<NamedType, Integer> typeToIndex,
                               Map<Integer, NamedType> indexToType) {
        this.typeInfo = typeInfo;
        this.typeIsExtensible = typeIsExtensible;
        this.typeToIndex = typeToIndex;
        this.indexToType = indexToType;
        this.startOfMethodParameters = indexToType.size();
        hctTypeInfo = null;
        methodInfo = null;
    }


    HiddenContentTypes(HiddenContentTypes hctTypeInfo,
                       MethodInfo methodInfo,
                       Map<NamedType, Integer> typeToIndexIn,
                       boolean addStartOfMethodParameters) {
        this.typeIsExtensible = hctTypeInfo.typeIsExtensible;
        this.hctTypeInfo = hctTypeInfo;
        this.typeInfo = hctTypeInfo.typeInfo;
        this.methodInfo = methodInfo;
        assert typeInfo == methodInfo.typeInfo()
                : "HCS typeInfo = " + typeInfo + ", method type info = " + methodInfo.typeInfo();
        this.startOfMethodParameters = hctTypeInfo.startOfMethodParameters;
        Map<Integer, NamedType> i2t = new HashMap<>();
        Map<NamedType, Integer> t2i = new HashMap<>();
        for (Map.Entry<NamedType, Integer> entry : typeToIndexIn.entrySet()) {
            NamedType nt = entry.getKey();
            assert !hctTypeInfo.typeToIndex.containsKey(nt);
            int i = (addStartOfMethodParameters ? startOfMethodParameters : 0) + entry.getValue();
            i2t.put(i, nt);
            t2i.put(nt, i);
        }
        indexToType = Map.copyOf(i2t);
        typeToIndex = Map.copyOf(t2i);
    }

    @Override
    public boolean isDefault() {
        return equals(NO_VALUE);
    }

    public static HiddenContentTypes decode(Codec codec, Codec.Context context, Codec.EncodedValue encodedValue) {
        Map<Codec.EncodedValue, Codec.EncodedValue> map = codec.decodeMap(context, encodedValue);
        boolean isExtensible = false;
        Map<NamedType, Integer> typeToIndex = new HashMap<>();
        TypeInfo currentType = context.currentType();
        MethodInfo currentMethod = context.currentMethod();
        LOGGER.debug("Decoding hct {} {}", currentType, currentMethod);
        // it is a little inefficient, but we need to know 'startOfMethodParameters' before
        // parsing the rest of the map.
        int startOfMethodParameters = 0;
        for (Map.Entry<Codec.EncodedValue, Codec.EncodedValue> entry : map.entrySet()) {
            String key = codec.decodeString(context, entry.getKey());
            if ("M".equals(key)) {
                startOfMethodParameters = codec.decodeInt(context, entry.getValue());
            }
        }
        for (Map.Entry<Codec.EncodedValue, Codec.EncodedValue> entry : map.entrySet()) {
            String key = codec.decodeString(context, entry.getKey());
            if ("E".equals(key)) {
                isExtensible = codec.decodeBoolean(context, entry.getValue());
            } else if (!"M".equals(key)) {
                int index = Integer.parseInt(key);
                NamedType namedType;
                if (entry.getValue() instanceof CodecImpl.D d && d.s() instanceof StringLiteral sl) {
                    String string = CodecImpl.unquote(sl.getSource());
                    char first = string.charAt(0);
                    String rest = string.substring(1);
                    if ('T' == first) {
                        namedType = context.findType(codec.typeProvider(), rest);
                        assert namedType != null : "Cannot find type " + rest;
                    } else if ('P' == first) {
                        int colon = rest.lastIndexOf(':');
                        String indexWithStars = rest.substring(colon + 1);
                        TypeInfo ti = currentType;
                        while (indexWithStars.charAt(0) == '*') {
                            indexWithStars = indexWithStars.substring(1);
                            ti = ti.compilationUnitOrEnclosingType().getRight();
                        }
                        int tpIndex = Integer.parseInt(indexWithStars);
                        if (tpIndex >= startOfMethodParameters) {
                            int mIndex = tpIndex - startOfMethodParameters;
                            assert mIndex < currentMethod.typeParameters().size();
                            namedType = currentMethod.typeParameters().get(mIndex);
                            assert namedType != null;
                        } else {
                            assert tpIndex < ti.typeParameters().size();
                            namedType = ti.typeParameters().get(tpIndex);
                            assert namedType != null;
                        }
                    } else throw new UnsupportedOperationException();
                } else {
                    throw new UnsupportedOperationException();
                }
                typeToIndex.put(namedType, index);
            }
        }
        if (context.methodBeforeType()) {
            HiddenContentTypes hctType = currentType.typeInfo().analysis().getOrCreate(HIDDEN_CONTENT_TYPES,
                    () -> of(currentType.typeInfo()));
            return new HiddenContentTypes(hctType, context.currentMethod(), typeToIndex, false);
        }
        Map<Integer, NamedType> indexToType = typeToIndex.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));
        return new HiddenContentTypes(currentType, isExtensible, typeToIndex, indexToType);
    }

    @Override
    public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
        Map<Codec.EncodedValue, Codec.EncodedValue> map = new HashMap<>();
        if (methodInfo == null && typeIsExtensible) {
            map.put(codec.encodeString(context, "E"), codec.encodeBoolean(context, true));
        }
        indexToType.forEach((key, value) -> {
            Codec.EncodedValue v;
            if (value instanceof TypeInfo ti) {
                v = codec.encodeString(context, "T" + ti.fullyQualifiedName());
            } else if (value instanceof TypeParameter tp) {
                String tpIndex = encodeIndex(typeInfo, startOfMethodParameters, tp);
                v = codec.encodeString(context, "P" + tp.simpleName() + ":" + tpIndex);
            } else throw new UnsupportedOperationException();
            map.put(codec.encodeInt(context, key), v);
        });
        if (startOfMethodParameters != 0) {
            map.put(codec.encodeString(context, "M"), codec.encodeInt(context, startOfMethodParameters));
        }
        // we always write out, even if 'map' is empty; otherwise, methods don't have proper values when their
        // data is empty but their type's is not.
        return codec.encodeMap(context, map);
    }

    /*
    the star means: go up in the hierarchy!
     */
    private static String encodeIndex(TypeInfo typeInfo, int startOfMethodParameters, TypeParameter tp) {
        if (tp.getOwner().isLeft()) {
            TypeInfo owner = tp.getOwner().getLeft();
            if (owner == typeInfo) return "" + tp.getIndex();
            assert !typeInfo.isStatic() && typeInfo.compilationUnitOrEnclosingType().isRight();
            return "*" + encodeIndex(typeInfo.compilationUnitOrEnclosingType().getRight(), -1, tp);
        }
        // we have a method parameter
        return "" + (startOfMethodParameters + tp.getIndex());
    }

    public boolean forMethod() {
        return methodInfo != null;
    }


    public boolean hasHiddenContent() {
        return typeIsExtensible || size() > 0;
    }

    private static NamedType namedType(ParameterizedType type) {
        if (type.typeParameter() != null) return type.typeParameter();
        if (type.typeInfo() != null) return type.typeInfo();
        return null;
    }

    public boolean isEmpty() {
        return (hctTypeInfo == null || hctTypeInfo.isEmpty()) && indexToType.isEmpty();
    }

    @Override
    public String toString() {
        String s = hctTypeInfo == null ? "" : hctTypeInfo + " - ";
        String l = hctTypeInfo == null ? typeInfo.simpleName() : methodInfo.name();
        return s + l + ":" + indexToType.values().stream()
                .map(NamedType::simpleName).sorted().collect(Collectors.joining(", "));
    }

    // for testing codec
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HiddenContentTypes that)) return false;
        return isTypeIsExtensible() == that.isTypeIsExtensible()
               && startOfMethodParameters == that.startOfMethodParameters
               && Objects.equals(getTypeInfo(), that.getTypeInfo())
               && Objects.equals(getHctTypeInfo(), that.getHctTypeInfo())
               && Objects.equals(getMethodInfo(), that.getMethodInfo())
               && Objects.equals(getTypeToIndex(), that.getTypeToIndex())
               && Objects.equals(getIndexToType(), that.getIndexToType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTypeInfo(), isTypeIsExtensible(), startOfMethodParameters, getHctTypeInfo(),
                getMethodInfo(), getTypeToIndex(), getIndexToType());
    }

    public int size() {
        return (hctTypeInfo == null ? 0 : hctTypeInfo.size()) + indexToType.size();
    }

    /*
    make a translation map based on pt2, and translate from formal to concrete.

    If types contains E=formal type parameter of List<E>, and pt = List<T>, we want
    to return a HiddenContentTypes containing T instead of E
     */
    public HiddenContentTypes translate(Runtime runtime, ParameterizedType pt) {
        Map<NamedType, ParameterizedType> map = pt.initialTypeParameterMap();
        Set<ParameterizedType> newTypes = typeToIndex.keySet().stream()
                .map(t -> translate(runtime, pt, map, t))
                .collect(Collectors.toUnmodifiableSet());
        // return new HiddenContentTypes(newTypes);
        throw new UnsupportedOperationException();
    }

    private ParameterizedType translate(Runtime runtime, ParameterizedType pt,
                                        Map<NamedType, ParameterizedType> map,
                                        NamedType t) {
        if (map.isEmpty() && t instanceof TypeParameter tp
            && tp.asSimpleParameterizedType().isAssignableFrom(runtime, pt)) {
            return pt;
        }
        //  return t.applyTranslation(inspectionProvider.getPrimitives(), map);
        throw new UnsupportedOperationException();
    }

    public NamedType typeByIndex(int i) {
        NamedType here = indexToType.get(i);
        if (here != null) return here;
        if (hctTypeInfo != null) {
            return hctTypeInfo.indexToType.get(i);
        }
        return null;
    }

    public Integer indexOf(ParameterizedType type) {
        return indexOfOrNull(type);
    }

    public int indexOf(NamedType namedType) {
        Integer here = typeToIndex.get(namedType);
        if (here != null) return here;
        if (hctTypeInfo != null) return hctTypeInfo.typeToIndex.get(namedType);
        throw new UnsupportedOperationException("Expected " + namedType + " to be known");
    }

    public boolean isKnown(NamedType namedType) {
        return typeToIndex.containsKey(namedType);
    }

    public Integer indexOfOrNull(ParameterizedType type) {
        NamedType namedType = namedType(type);
        if (namedType == null) return null;
        Integer here = typeToIndex.get(namedType);
        if (here != null) return here;
        if (hctTypeInfo != null) return hctTypeInfo.typeToIndex.get(namedType);
        return null;
    }

    public Collection<NamedType> types() {
        return indexToType.values();
    }

    public String detailedSortedTypeToIndex() {
        String s = forMethod() ? hctTypeInfo.detailedSortedTypeToIndex() + " - " : "";
        return s + typeToIndex.entrySet().stream()
                .map(e -> e.getKey().simpleName() + "=" + e.getValue())
                .sorted().collect(Collectors.joining(", "));
    }

    public String detailedSortedTypes() {
        String s = forMethod() ? hctTypeInfo.detailedSortedTypes() + " - " : "";
        return s + indexToType.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().simpleName())
                .sorted().collect(Collectors.joining(", "));
    }


    public Map<Integer, Integer> mapMethodToTypeIndices(ParameterizedType parameterizedType) {
        // FIXME this is not a good implementation
        Map<Integer, Integer> result = new HashMap<>();
        for (int i : indexToType.keySet()) {
            result.put(i, i - startOfMethodParameters);
        }
        return Map.copyOf(result);
    }

    public MethodInfo getMethodInfo() {
        return methodInfo;
    }

    public TypeInfo isExtensible(Integer single) {
        if (single == null) return null;
        NamedType nt = typeByIndex(single);
        return nt instanceof TypeInfo ti ? ti : null;
    }

    public HiddenContentTypes getHctTypeInfo() {
        return hctTypeInfo;
    }

    public Map<Integer, NamedType> getIndexToType() {
        return indexToType;
    }

    public TypeInfo getTypeInfo() {
        return typeInfo;
    }

    public Map<NamedType, Integer> getTypeToIndex() {
        return typeToIndex;
    }

    public Stream<Map.Entry<NamedType, Integer>> typesOfExtensibleFields() {
        HiddenContentTypes hct = hctTypeInfo != null ? hctTypeInfo : this;
        return hct.typeToIndex.entrySet().stream().filter(e -> e.getKey() instanceof TypeInfo);
    }

    public boolean isTypeIsExtensible() {
        return typeIsExtensible;
    }
}
