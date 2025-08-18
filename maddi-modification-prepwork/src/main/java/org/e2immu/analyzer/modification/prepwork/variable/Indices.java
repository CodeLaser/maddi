package org.e2immu.analyzer.modification.prepwork.variable;

import org.e2immu.language.cst.api.analysis.Codec;
import org.e2immu.language.cst.api.runtime.Runtime;
import org.e2immu.language.cst.api.type.ParameterizedType;

import java.util.Set;
import java.util.function.IntFunction;

public interface Indices extends Comparable<Indices> {
    Codec.EncodedValue encode(Codec codec, Codec.Context context);

    default boolean haveValue() {
        return !isNoModification() && !isAll();
    }

    boolean intersectionNonEmpty(Indices indices);

    boolean isAll();

    boolean isNoModification();

    boolean isUnspecified();

    Indices merge(Indices indices);

    ParameterizedType findInFormal(Runtime runtime, ParameterizedType type);

    ParameterizedType find(Runtime runtime, ParameterizedType type);

    Indices allOccurrencesOf(Runtime runtime, ParameterizedType where);

    boolean containsSize2Plus();

    Indices prepend(Indices modificationAreaTarget);

    Indices size2PlusDropOne();

    Indices first();

    Indices prefix(int index);

    Integer single();

    Indices map(IntFunction<Integer> intFunction);

    Set<Index> set();
}
