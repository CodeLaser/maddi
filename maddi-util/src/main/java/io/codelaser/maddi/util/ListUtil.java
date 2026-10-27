/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.util;

import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class ListUtil {

    public record Pair<K, V>(K k, V v) {}

    // joinLists only iterates its inputs. Contracted for the same reason as ZipLists.zip: maddi-util
    // reaches the dogfood as a jar, where an unannotated container parameter reads as possibly-modified.
    @NotModified
    @Independent
    public static <K, L> Stream<Pair<K, L>> joinLists(@NotNull @NotModified List<K> list1,
                                                      @NotNull @NotModified List<L> list2) {
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
