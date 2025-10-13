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

import java.util.List;
import java.util.function.IntFunction;

public interface Index extends Comparable<Index> {
    int countSequentialZeros();

    Index dropFirst();

    Codec.EncodedValue encode(Codec codec, Codec.Context context);

    ParameterizedType find(Runtime ru, ParameterizedType type);

    /*
         extract type given the indices. Switch to formal for the last index in the list only!
         Map.Entry<A,B>, with index 0, will return K in Map.Entry
         Set<Map.Entry<A,B>>, with indices 0.1, will return V in Map.Entry.
         */
    ParameterizedType findInFormal(Runtime runtime, ParameterizedType type);

    List<Integer> list();

    Index map(IntFunction<Integer> intFunction);

    Index prefix(int index);

    Index prepend(Index other);

    Index replaceLast(int v);

    Integer single();

    Index takeFirst();
}
