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

package org.e2immu.analyzer.modification.prepwork.hcs;

import org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes;
import org.e2immu.analyzer.modification.prepwork.variable.Index;
import org.e2immu.analyzer.modification.prepwork.variable.Indices;
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.analysis.Value;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.language.cst.impl.analysis.PropertyImpl;
import org.e2immu.language.inspection.api.parser.GenericsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl.ALL_INDICES;
import static org.e2immu.analyzer.modification.prepwork.hcs.IndicesImpl.UNSPECIFIED_MODIFICATION_INDICES;
import static org.e2immu.analyzer.modification.prepwork.hct.HiddenContentTypes.HIDDEN_CONTENT_TYPES;

/*
Numeric encoding of the Indices:

natural numbers: type parameters
-1: the object itself, ALL

as opposed to the numeric encoding of HCT:
natural numbers: hidden content type indices, starting with type parameters from hierarchy,
end with extensible fields and  method parameters
-1: the object itself, when extensible

 */
public class HiddenContentSelector implements Value {
    private static final Logger LOGGER = LoggerFactory.getLogger(HiddenContentSelector.class);

    public static final HiddenContentSelector NONE = new HiddenContentSelector();
    public static final PropertyImpl HCS_METHOD = new PropertyImpl("hcsMethod", NONE);
    public static final PropertyImpl HCS_PARAMETER = new PropertyImpl("hcsParameter", NONE);

    private final HiddenContentTypes hiddenContentTypes;
    /*
    map key: the index in HCT
    map value: how to extract from the type
     */
    private final Map<Integer, Indices> map;

    // only used for NONE
    private HiddenContentSelector() {
        map = Map.of();
        hiddenContentTypes = HiddenContentTypes.OF_PRIMITIVE;
    }

    // note: if map is empty, isNone() will be true
    public HiddenContentSelector(HiddenContentTypes hiddenContentTypes, Map<Integer, Indices> map) {
        this.map = map;
        this.hiddenContentTypes = hiddenContentTypes;
    }

    // used for assertions!
    public boolean compatibleWith(Runtime runtime, ParameterizedType pt) {
        if (isNone()) return true;
        ParameterizedType ptWithoutArrays = pt.copyWithoutArrays();
        if (findType(hiddenContentTypes, runtime, ptWithoutArrays)) return true;
        if (hiddenContentTypes.getHctTypeInfo() != null) {
            if (findType(hiddenContentTypes.getHctTypeInfo(), runtime, ptWithoutArrays)) return true;
        }
        LOGGER.warn("Assertion should fail: HCS {} vs {}", detailed(), pt);
        return false;
    }

    private static boolean findType(HiddenContentTypes hiddenContentTypes, Runtime runtime, ParameterizedType pt) {
        if (hiddenContentTypes.getTypeInfo().asParameterizedType().isAssignableFrom(runtime, pt)) {
            return true;
        }
        return hiddenContentTypes.getTypeToIndex().keySet().stream().anyMatch(nt ->
                nt.asParameterizedType().isAssignableFrom(runtime, pt));
    }

    @Override
    public boolean isDefault() {
        return map.isEmpty() && hiddenContentTypes.isDefault();
    }

    @Override
    public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
        Map<Codec.EncodedValue, Codec.EncodedValue> mapOfEncoded = map.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(e -> codec.encodeInt(context, e.getKey()),
                        e -> e.getValue().encode(codec, context)));
        if (mapOfEncoded.isEmpty()) return null;
        return codec.encodeMap(context, mapOfEncoded);
    }

    public static HiddenContentSelector decode(Codec codec, Codec.Context context, Codec.EncodedValue encodedValue) {
        Map<Codec.EncodedValue, Codec.EncodedValue> map = codec.decodeMap(context, encodedValue);
        Map<Integer, Indices> intIndices = new HashMap<>();
        for (Map.Entry<Codec.EncodedValue, Codec.EncodedValue> entry : map.entrySet()) {
            int i = codec.decodeInt(context, entry.getKey());
            Indices indices = IndicesImpl.decode(codec, context, entry.getValue());
            intIndices.put(i, indices);
        }
        MethodInfo methodInfo = context.currentMethod();
        assert methodInfo != null : "HCS is computed for parameters and methods";
        HiddenContentTypes hct = methodInfo.analysis().getOrNull(HIDDEN_CONTENT_TYPES, HiddenContentTypes.class);
        assert hct != null;
        return new HiddenContentSelector(hct, Map.copyOf(intIndices));
    }

    public HiddenContentTypes hiddenContentTypes() {
        return hiddenContentTypes;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String detailed() {
        return toString(true);
    }

    private String toString(boolean detailed) {
        if (map.isEmpty()) {
            return "X"; // None
        }
        return map.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(e -> print(e.getKey(), e.getValue(), detailed))
                .collect(Collectors.joining(","));
    }

    /*
    '*' means: the whole object; otherwise, we're digging deeper
     */
    private static String print(int i, Indices indices, boolean detailed) {
        if (indices.isAll()) return detailed ? i + "=*" : "*";
        String is = indices.toString();
        String iToString = "" + i;
        if (!detailed && is.equals(iToString)) return iToString;
        return iToString + "=" + is;
    }

    public Set<Integer> set() {
        return map.keySet();
    }

    public Map<Integer, Indices> getMap() {
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HiddenContentSelector hcs = (HiddenContentSelector) o;
        return hiddenContentTypes.equals(hcs.hiddenContentTypes) && map.equals(hcs.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hiddenContentTypes, map);
    }

    public boolean selectArrayElement(int arrays) {
        if (map.size() == 1) {
            Indices indices = map.values().stream().findFirst().orElseThrow();
            return indices.set().size() == 1
                   && indices.set().stream().findFirst().orElseThrow().countSequentialZeros() == arrays;
        }
        return false;
    }

    /*
     Take in a type, and return the hidden content components of this type, with respect to the hidden content types
     of the current type or method.
     Set<Map.Entry<K, V>> will return the indices of K and V mapped to their position in the formal type,
     i.e., 0 -> 0.0, 1 -> 0.1
     Collection<V> will return 1 -> 0

     in the following context:
     static <X> X method(...) { Supplier<X> s = new Supplier<>() { ... }}
     the anonymous type $1 has no direct type parameters, but its enclosing method does. We'll replace $2 by
     Supplier<X>.
     */
    public static HiddenContentSelector selectAll(HiddenContentTypes hiddenContentTypes,
                                                  ParameterizedType typeIn) {
        assert hiddenContentTypes != null && typeIn != null;
        if (typeIn.typeInfo() == null && typeIn.typeParameter() == null) {
            // type is "?", or "?" extends Object
            return new HiddenContentSelector(hiddenContentTypes,
                    Map.of(HiddenContentTypes.UNSPECIFIED_EXTENSION, UNSPECIFIED_MODIFICATION_INDICES));
        }

        boolean haveArrays = typeIn.arrays() > 0;
        ParameterizedType type = typeIn.copyWithoutArrays();
        Integer index = hiddenContentTypes.indexOfOrNull(type);
        Map<Integer, Indices> map = new HashMap<>();

        if (index != null) {
            if (haveArrays) {
                map.put(index, new IndicesImpl(index));
            } else {
                map.put(index, ALL_INDICES);
            }
        }
        if (type.typeInfo() != null && type.typeInfo().equals(hiddenContentTypes.getTypeInfo())) {
            // we're on our own type, add indices for types; only for the object itself we use ALL_INDICES
            hiddenContentTypes.typesOfExtensibleFields().forEach(e -> map.put(e.getValue(),
                    e.getKey() instanceof TypeInfo ti && (ti == type.typeInfo() || type.typeInfo().superTypesExcludingJavaLangObject().contains(ti))
                            ? ALL_INDICES : new IndicesImpl(e.getValue())
            ));
        }
        if (type.typeInfo() != null) {
            recursivelyCollectHiddenContentParameters(hiddenContentTypes, type, new Stack<>(), map);
        }
        return new HiddenContentSelector(hiddenContentTypes, Map.copyOf(map));
    }

    private static void recursivelyCollectHiddenContentParameters(HiddenContentTypes hiddenContentTypes,
                                                                  ParameterizedType type,
                                                                  Stack<Integer> prefix,
                                                                  Map<Integer, Indices> map) {
        Integer index = hiddenContentTypes.indexOfOrNull(type.copyWithoutArrays());
        if (index != null && type.parameters().isEmpty() && !prefix.isEmpty()) {
            map.merge(index, new IndicesImpl(Set.of(new IndexImpl(List.copyOf(prefix)))), Indices::merge);
        } else {
            int i = 0;
            for (ParameterizedType parameter : type.parameters()) {
                prefix.push(i);
                recursivelyCollectHiddenContentParameters(hiddenContentTypes, parameter, prefix, map);
                prefix.pop();
                i++;
            }
        }
    }

    public boolean isNone() {
        return map.isEmpty();
    }

    // useful for testing
    public boolean isOnlyAll() {
        return map.keySet().size() == 1 && map.entrySet().stream().findFirst().orElseThrow().getValue().isAll();
    }

    public boolean containsAll() {
        return map.values().stream().anyMatch(Indices::isAll);
    }
     /*
     The hidden content selector's hct indices (the keys in the map) are computed with respect to 'this'.
     They map to indices (the values in the map) which exist in 'from'.

     'to' is a concrete type for 'from'. We'll map the indices of the selector to indices wrt the formal
     type of to, also attaching the concrete types at those indices.

     E.g. method context is the type ArrayList<EA>.new ArrayList<>(Collection<? extends EA>)
     concrete constructor call is new ArrayList<>(List<M>)

     'this' is with respect to ArrayList<EA> and the constructor, mapping EA=0 (and EL=0, EC=0 for List, Collection)
     'from' is Collection<? extends EA>, formal type Collection<EC>.
     The hidden content selector is 0=0.

     'to' is List<M>, with formal type List<EL>.
     The result maps 'indices' 0 to the combination of "M" and indices 0.
    */

    public record IndicesAndType(Indices indices, ParameterizedType type) {
    }

    public Map<Indices, IndicesAndType> translateHcs(Runtime runtime,
                                                     GenericsHelper genericsHelper,
                                                     ParameterizedType from,
                                                     ParameterizedType to,
                                                     boolean allowVarargs) {
        if (allowVarargs && from.arrays() == to.arrays() + 1) {
            return translateHcs(runtime, genericsHelper, from.copyWithOneFewerArrays(), to, false);
        }
        if (from.typeInfo() != null && "java.lang.Iterable".equals(from.typeInfo().fullyQualifiedName())
            && to.arrays() > 0) {
            // e.g. from = Iterable<T>, to = Closeable[]
            Map<Indices, ParameterizedType> map1 = extract(runtime, from);
            assert map1.size() == 1;
            Map.Entry<Indices, ParameterizedType> entry1 = map1.entrySet().stream().findFirst().orElseThrow();
            assert "0".equals(entry1.getKey().toString());
            return Map.of(entry1.getKey(), new IndicesAndType(entry1.getKey(), to.copyWithFewerArrays(1)));
        }
        assert from.isAssignableFrom(runtime, to) : from + " is not assignable from " + to;

        if (isNone() || to.isTypeOfNullConstant()) return Map.of();

        // which indices are we talking about? 'from' is a type that can be expressed in the context of 'this',
        // the HCS of a method. Concretely, it is the method's formal return type, or a formal parameter.
        Map<Indices, ParameterizedType> map1 = extract(runtime, from);

        Map<Indices, IndicesAndType> result = new HashMap<>();
        for (Map.Entry<Indices, ParameterizedType> entry1 : map1.entrySet()) {
            IndicesAndType iat;
            if (from.arrays() > 0 && selectArrayElement(from.arrays())) {
                Indices indices = new IndicesImpl(Set.of(IndexImpl.createZeroes(from.arrays())));
                iat = new IndicesAndType(indices, to);
            } else if (from.equals(to)) {
                iat = new IndicesAndType(entry1.getKey(), entry1.getValue());
            } else if (from.typeParameter() != null || entry1.getKey().isAll()) {
                iat = new IndicesAndType(entry1.getKey(), to);
            } else {
                iat = findAll(runtime, genericsHelper, entry1.getKey(), entry1.getValue(), from, to);
            }
            if (iat != null) {
                result.put(entry1.getKey(), iat);
            }
        }
        return Map.copyOf(result);
    }

    /*
    Return those HCT components that are available in the formal return or parameter type of the method.

    We ignore fields, because there could be multiple, and we would not know where to map them to.
     */
    Map<Indices, ParameterizedType> extract(Runtime runtime, ParameterizedType type) {
        assert this != NONE;
        return map.values().stream().collect(Collectors.toUnmodifiableMap(i -> i, i -> extract(runtime, type, i)));
    }

    private ParameterizedType extract(Runtime runtime, ParameterizedType type, Indices i) {
        if (i.isAll()) return type;
        ParameterizedType inFormal = i.findInFormal(runtime, type);
        if (inFormal == null) {
            NamedType namedType = hiddenContentTypes.typeByIndex(i.single());
            assert namedType != null : type + " has no index " + i + " in " + hiddenContentTypes.detailedSortedTypes()
                                       + " of " + hiddenContentTypes.getTypeInfo();
            return namedType.asParameterizedType();
        }
        return inFormal;
    }


    /*
    if what is a type parameter, is will be with respect to the formal type of that level of generics.

    what == the result of 'ptInFrom in from' translated to ('to' == where)

    Given what=EL, with where==List<String>, return 0,String
    Given what=K in Map.Entry, with where = Set<Map.Entry<A,B>>, return 0.0,A

    If from=Set<Set<K>>, and we extract K at 0.0
    If to=Collection<ArrayList<String>>, we'll have to return 0.0, String
     */
    private IndicesAndType findAll(Runtime runtime,
                                   GenericsHelper genericsHelper,
                                   Indices indices,
                                   ParameterizedType ptInFrom,
                                   ParameterizedType from,
                                   ParameterizedType to) {
        Integer single = indices.single();
        if (single != null && single >= from.typeInfo().asParameterizedType().parameters().size()) {
            NamedType namedType = hiddenContentTypes.typeByIndex(single);
            if (namedType == null) {
                return null; // see TestNullInHCSFindAll
            }
            return new IndicesAndType(indices, namedType.asParameterizedType());
        }
        // it does not matter with which index we start
        Index index = indices.set().stream().findFirst().orElseThrow();
        IndicesAndType res = findAll(runtime, genericsHelper, index, 0, ptInFrom, from, to);
        if (res == null) return null;
        // but once we have found it, we must make sure that we return all occurrences
        assert res.indices.set().size() == 1;
        if (res.type == null) {
            throw new NullPointerException("findAll: res.type null: indices = " + indices + ", ptInFrom = "
                                           + ptInFrom + ", from = " + from + "; to = " + to);
        }
        Indices findAll = IndicesImpl.staticAllOccurrencesOf(res.type, to);
        return new IndicesAndType(findAll, res.type);
    }


    private IndicesAndType findAll(Runtime runtime,
                                   GenericsHelper genericsHelper,
                                   Index index,
                                   int pos,
                                   ParameterizedType ptFrom,
                                   ParameterizedType from,
                                   ParameterizedType to) {
        int atPos = index.list().get(pos);
        TypeInfo bestTo = to.bestTypeInfo();

        if (pos == index.list().size() - 1) {
            // the last entry
            assert from.typeInfo() != null;
            ParameterizedType formalFrom = from.typeInfo().asParameterizedType();
            assert atPos >= 0;
            assert atPos < formalFrom.parameters().size() : "Has been picked up in parent method";
            assert formalFrom.parameters().get(atPos).equals(ptFrom);
            if (formalFrom.typeInfo() == bestTo) {
                ParameterizedType concrete;
                if (atPos >= to.parameters().size()) {
                    // type parameters are missing, we'd expect <> so that they get filled in automatically
                    concrete = runtime.objectParameterizedType();
                } else {
                    concrete = to.parameters().get(atPos);
                }
                return new IndicesAndType(new IndicesImpl(Set.of(index)), concrete);
            }
            ParameterizedType formalTo = bestTo.asParameterizedType();
            Map<NamedType, ParameterizedType> map1;
            if (formalFrom.isAssignableFrom(runtime, formalTo)) {
                map1 = genericsHelper.mapInTermsOfParametersOfSuperType(bestTo, formalFrom);
            } else {
                map1 = genericsHelper.mapInTermsOfParametersOfSubType(from.typeInfo(), formalTo);
            }
            assert map1 != null;
            ParameterizedType ptTo = map1.get(ptFrom.namedType());
            assert ptTo != null;
            HiddenContentTypes hct = bestTo.analysis().getOrNull(HIDDEN_CONTENT_TYPES, HiddenContentTypes.class);
            assert hct != null : "No HCT for " + bestTo;
            Integer iTo = hct.indexOf(ptTo);
            if (iTo == null) {
                return null; // see TestConsumer
            }
            Index indexTo = index.replaceLast(iTo);
            Indices indicesTo = new IndicesImpl(Set.of(indexTo));
            Map<NamedType, ParameterizedType> map2 = to.initialTypeParameterMap();
            ParameterizedType concreteTypeTo = map2.get(ptTo.namedType());
            if (concreteTypeTo != null) {
                return new IndicesAndType(indicesTo, concreteTypeTo);
            }
            // concrete = Properties ~ HashTable<Object,Object> ~ Map<K, V> there are no explicit hidden content types in Properties
            // See TestTypeParameterChoices
            return null;
        }
        if (from.typeInfo() == bestTo) {
            ParameterizedType inFrom = from.parameters().get(atPos);
            ParameterizedType inTo = to.parameters().get(atPos);
            return findAll(runtime, genericsHelper, index, pos + 1, ptFrom, inFrom, inTo);
        }
        throw new UnsupportedOperationException();
    }
}