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
import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.NamedType;
import org.e2immu.language.cst.api.type.ParameterizedType;
import org.e2immu.util.internal.util.ListUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record IndexImpl(List<Integer> list) implements Index, Comparable<Index> {
    public static final int ALL = -1;
    public static final int UNSPECIFIED_MODIFICATION = -2;
    public static final int NO_MODIFICATION = -3;
    public static final Index ALL_INDEX = new IndexImpl(List.of(ALL));
    public static final Index NO_MODIFICATIO_INDEX = new IndexImpl(List.of(NO_MODIFICATION));
    public static final Index UNSPECIFIED_MODIFICATION_INDEX = new IndexImpl(List.of(UNSPECIFIED_MODIFICATION));

    public static Index createZeroes(int arrays) {
        List<Integer> list = new ArrayList<>(arrays);
        for (int i = 0; i < arrays; i++) list.add(0);
        return new IndexImpl(List.copyOf(list));
    }

    @Override
    public Codec.EncodedValue encode(Codec codec, Codec.Context context) {
        List<Codec.EncodedValue> encodedList = list.stream().map(i -> codec.encodeInt(context, i)).toList();
        return codec.encodeList(context, encodedList);
    }

    public static Index decode(Codec codec, Codec.Context context, Codec.EncodedValue encodedValue) {
        List<Codec.EncodedValue> list = codec.decodeList(context, encodedValue);
        List<Integer> indices = new ArrayList<>();
        for (Codec.EncodedValue ev : list) {
            int i = codec.decodeInt(context, ev);
            indices.add(i);
        }
        return new IndexImpl(List.copyOf(indices));
    }

    @Override
    public int compareTo(Index o) {
        return ListUtil.compare(list, o.list());
    }

    @Override
    public String toString() {
        return list.stream().map(IndexImpl::toString).collect(Collectors.joining("."));
    }

    private static String toString(int value) {
        if (value == ALL) return "*";
        if (value == UNSPECIFIED_MODIFICATION) return "?";
        if (value == NO_MODIFICATION) return "X";
        return Integer.toString(value);
    }

    /*
     extract type given the indices. Switch to formal for the last index in the list only!
     Map.Entry<A,B>, with index 0, will return K in Map.Entry
     Set<Map.Entry<A,B>>, with indices 0.1, will return V in Map.Entry.
     */
    @Override
    public ParameterizedType findInFormal(Runtime runtime, ParameterizedType type) {
        return findInFormal(runtime, type, 0, true);
    }

    @Override
    public ParameterizedType find(Runtime ru, ParameterizedType type) {
        return findInFormal(ru, type, 0, false);
    }

    private ParameterizedType findInFormal(Runtime runtime, ParameterizedType type, int pos, boolean switchToFormal) {
        if (type.parameters().isEmpty()) {
            // no generics, so substitute "Object"
            if (type.typeInfo() != null) {
                HiddenContentTypes hct = type.typeInfo().analysis().getOrDefault(HiddenContentTypes.HIDDEN_CONTENT_TYPES, HiddenContentTypes.NO_VALUE);
                NamedType byIndex = hct.typeByIndex(pos);
                if (byIndex != null) {
                    return byIndex.asParameterizedType();
                }
            }
            return runtime.objectParameterizedType();
        }
        int index = list.get(pos);
        if (pos == list.size() - 1) {
            assert type.typeInfo() != null;
            ParameterizedType formal = switchToFormal ? type.typeInfo().asParameterizedType() : type;
            if (index < formal.parameters().size()) {
                return formal.parameters().get(index);
            }
            return null; // signal to keep
        }
        ParameterizedType nextType = type.parameters().get(index);
        assert nextType != null;
        return findInFormal(runtime, nextType, pos + 1, switchToFormal);
    }

    @Override
    public Index replaceLast(int v) {
        if (list.get(list.size() - 1) == v) return this;
        return new IndexImpl(Stream.concat(list.stream().limit(list.size() - 1), Stream.of(v)).toList());
    }

    @Override
    public int countSequentialZeros() {
        int cnt = 0;
        for (int i : list) {
            if (i != 0) return -1;
            cnt++;
        }
        return cnt;
    }

    @Override
    public Index dropFirst() {
        assert list.size() > 1;
        return new IndexImpl(list.subList(1, list.size()));
    }

    @Override
    public Index takeFirst() {
        assert list.size() > 1;
        return new IndexImpl(List.of(list.getFirst()));
    }

    @Override
    public Index prefix(int index) {
        return new IndexImpl(Stream.concat(Stream.of(index), list.stream()).toList());
    }

    @Override
    public Index prepend(Index other) {
        return new IndexImpl(Stream.concat(other.list().stream(), list.stream()).toList());
    }

    @Override
    public Integer single() {
        return list.size() == 1 ? list.getFirst() : null;
    }

    @Override
    public Index map(IntFunction<Integer> intFunction) {
        int index = list.getFirst();
        return new IndexImpl(Stream.concat(Stream.of(intFunction.apply(index)), list.stream().skip(1)).toList());
    }
}
