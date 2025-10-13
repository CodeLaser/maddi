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

package org.e2immu.util.internal.util;

import org.e2immu.annotation.Independent;
import org.e2immu.annotation.NotModified;
import org.e2immu.annotation.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class ListUtil {

    public record Pair<K, V>(K k, V v) {}

    @Independent
    public static <K, L> Stream<Pair<K, L>> joinLists(@NotNull List<K> list1, @NotNull List<L> list2) {
        Stream.Builder<Pair<K, L>> builder = Stream.builder();
        Iterator<L> it2 = list2.iterator();
        for (K t1 : list1) {
            if (!it2.hasNext()) break;
            L t2 = it2.next();
            builder.accept(new Pair<>(t1, t2));
        }
        return builder.build();
    }

    @NotModified
    public static <T extends Comparable<? super T>> int compare(@NotModified List<T> values1,
                                                                @NotModified List<T> values2) {
        Iterator<T> it2 = values2.iterator();
        for (T t1 : values1) {
            if (!it2.hasNext()) return 1;
            T t2 = it2.next();
            int c = t1.compareTo(t2);
            if (c != 0) return c;
        }
        if (it2.hasNext()) return -1;
        return 0;
    }

    @SafeVarargs
    @NotNull(content = true)
    @Independent
    public static <T> List<T> immutableConcat(@NotNull(content = true)
                                              @NotModified
                                              @Independent(hcReturnValue = true)
                                              Iterable<? extends T>... lists) {
        List<T> builder = new LinkedList<>();
        for (Iterable<? extends T> list : lists) {
            for (T t : list) {
                builder.add(t);
            }
        }
        return List.copyOf(builder);
    }
}
