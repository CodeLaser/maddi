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

package io.codelaser.maddi.aapi.archive.libs.kotlin;

import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.sequences.Sequence;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The annotated API for {@code kotlin.sequences}: the extensions on {@link Sequence}, the census's largest
 * uncontracted part class after the scope functions. A Sequence is an interface the defaults treat as mutable, so
 * every uncontracted call marked its receiver MODIFIED. None of these write it.
 * <p>
 * ⛔ NAME THE PART CLASS, NOT THE FACADE -- see {@link KotlinCollections}.
 */
public class KotlinSequences {
    public static final String PACKAGE_NAME = "kotlin.sequences";

    /*
    public fun <T> Sequence<T>.filter(predicate: (T) -> Boolean): Sequence<T>

    Two kinds. The INTERMEDIATE operations return a lazy view that reads the receiver when it is iterated: dependent
    on it, so they carry no @Independent. The TERMINAL operations walk it once; a collection they build is fresh.
    `find` is @InlineOnly and lowered to firstOrNull by the front end.
    */
    class SequencesKt___SequencesKt$ {
        @NotNull
        static <T> Sequence<T> filter(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return null;
        }

        @NotNull
        static <T> Sequence<T> filterNot(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return null;
        }

        @NotNull
        static <T, R> Sequence<R> map(@NotModified Sequence<? extends T> receiver, Function1<? super T, ? extends R> transform) {
            return null;
        }

        @NotNull
        static <T, R> Sequence<R> mapNotNull(@NotModified Sequence<? extends T> receiver, Function1<? super T, ? extends R> transform) {
            return null;
        }

        @NotNull
        static <T, R> Sequence<R> flatMap(@NotModified Sequence<? extends T> receiver, Function1<? super T, ? extends Sequence<? extends R>> transform) {
            return null;
        }

        @NotNull
        static <T> Sequence<T> onEach(@NotModified Sequence<? extends T> receiver, Function1<? super T, Unit> action) {
            return null;
        }

        @NotNull
        static <T> Sequence<T> takeWhile(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return null;
        }

        @NotNull
        static <T> Sequence<T> take(@NotModified Sequence<? extends T> receiver, int n) {
            return null;
        }

        @NotNull
        static <T> Sequence<T> drop(@NotModified Sequence<? extends T> receiver, int n) {
            return null;
        }

        @NotNull
        static <T> Sequence<T> distinct(@NotModified Sequence<? extends T> receiver) {
            return null;
        }

        @NotNull
        static <T, K> Sequence<T> distinctBy(@NotModified Sequence<? extends T> receiver, Function1<? super T, ? extends K> selector) {
            return null;
        }

        @NotNull
        static <T, R extends Comparable<? super R>> Sequence<T> sortedBy(@NotModified Sequence<? extends T> receiver, Function1<? super T, ? extends R> selector) {
            return null;
        }


        /* terminal */
        @Independent(hc = true)
        @NotNull
        static <T> T first(@NotModified Sequence<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> T first(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return null;
        }

        @Independent(hc = true)
        static <T> T firstOrNull(@NotModified Sequence<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T firstOrNull(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> T last(@NotModified Sequence<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> T last(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return null;
        }

        @Independent(hc = true)
        static <T> T lastOrNull(@NotModified Sequence<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T lastOrNull(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> T single(@NotModified Sequence<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> T single(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return null;
        }

        @Independent(hc = true)
        static <T> T singleOrNull(@NotModified Sequence<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T singleOrNull(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return null;
        }

        static <T> boolean any(@NotModified Sequence<? extends T> receiver) {
            return false;
        }

        static <T> boolean any(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return false;
        }

        static <T> boolean none(@NotModified Sequence<? extends T> receiver) {
            return false;
        }

        static <T> boolean none(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return false;
        }

        static <T> boolean all(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return false;
        }

        static <T> int count(@NotModified Sequence<? extends T> receiver) {
            return 0;
        }

        static <T> int count(@NotModified Sequence<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return 0;
        }

        static <T> void forEach(@NotModified Sequence<? extends T> receiver, Function1<? super T, Unit> action) {
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> toList(@NotModified Sequence<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> toMutableList(@NotModified Sequence<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> Set<T> toSet(@NotModified Sequence<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T, K> Map<K, List<T>> groupBy(@NotModified Sequence<? extends T> receiver, Function1<? super T, ? extends K> keySelector) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T, K> Map<K, T> associateBy(@NotModified Sequence<? extends T> receiver, Function1<? super T, ? extends K> keySelector) {
            return null;
        }

        @NotNull
        static <T> String joinToString(@NotModified Sequence<? extends T> receiver, @NotModified CharSequence separator,
                                       @NotModified CharSequence prefix, @NotModified CharSequence postfix, int limit,
                                       @NotModified CharSequence truncated,
                                       Function1<? super T, ? extends CharSequence> transform) {
            return null;
        }
    }
}
