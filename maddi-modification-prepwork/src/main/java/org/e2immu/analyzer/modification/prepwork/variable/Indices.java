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
