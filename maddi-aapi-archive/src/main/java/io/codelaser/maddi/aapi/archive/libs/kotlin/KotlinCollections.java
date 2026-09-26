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
import kotlin.Pair;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.ranges.IntRange;
import kotlin.sequences.Sequence;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The annotated API for {@code kotlin.collections}: the extension functions every Kotlin codebase calls on a
 * collection. Each is a static whose first parameter is the receiver — what kotlinc emits, and what maddi
 * models since #43.
 * <p>
 * ⛔ NAME THE PART CLASS, NOT THE FACADE. {@code kotlin.collections.CollectionsKt} is a multifile class facade:
 * it declares NOTHING (a private constructor) and {@code extends CollectionsKt___CollectionsKt}, from which it
 * inherits every method. A contract written against the facade is dropped — "Ignoring method … not found in
 * target type" — leaving a {@code .json} that holds the type with no method contracts at all, and a green
 * build. The part class is also what a CALL names: {@code jvmFacadeClassId} reads the symbol's FIR
 * containerSource, whose ClassId for a multifile part is the part
 * ({@code FacadeAndExtensionTest.aMultifileFacadeCallNamesTheClassThatDeclaresIt} pins it).
 */
public class KotlinCollections {
    public static final String PACKAGE_NAME = "kotlin.collections";

    /*
    public inline fun <T, R> Iterable<T>.map(transform: (T) -> R): List<R>

    A fresh list of transformed elements: the receiver is read, never written. What `transform` modifies is the
    caller's business, and its own contract's -- not this one's.
    */
    class CollectionsKt___CollectionsKt$ {
        @Independent(hc = true)
        @NotNull
        static <T, R> List<R> map(@NotModified Iterable<? extends T> receiver,
                                  Function1<? super T, ? extends R> transform) {
            return null;
        }

        /*
        public inline fun <T> Iterable<T>.forEach(action: (T) -> Unit): Unit

        Reads the receiver and hands each element to `action`. It writes nothing itself; whether `action`
        modifies what it is given is `action`'s contract, not this one's.
        */
        static <T> void forEach(@NotModified Iterable<? extends T> receiver,
                                Function1<? super T, Unit> action) {
        }

        /*
        public inline fun <T> Iterable<T>.filter(predicate: (T) -> Boolean): List<T>

        A fresh list holding the elements that passed; the receiver is read, never written. The result shares
        the receiver's ELEMENTS, which is exactly what @Independent(hc=true) says.
        */
        @Independent(hc = true)
        @NotNull
        static <T> List<T> filter(@NotModified Iterable<? extends T> receiver,
                                  Function1<? super T, Boolean> predicate) {
            return null;
        }

        /*
        public fun <T> Iterable<T>.any(): Boolean
        public inline fun <T> Iterable<T>.any(predicate: (T) -> Boolean): Boolean
        public inline fun <T> Iterable<T>.all(predicate: (T) -> Boolean): Boolean
        public fun <T> Iterable<T>.none(): Boolean
        public inline fun <T> Iterable<T>.none(predicate: (T) -> Boolean): Boolean

        The read-only predicates: each walks the receiver and answers a boolean, writing nothing. Uncontracted,
        `includes?.any { it.matches(path) }` marked a field modified that Java's `stream().anyMatch(..)` does not.
        */
        static <T> boolean any(@NotModified Iterable<? extends T> receiver) {
            return false;
        }

        static <T> boolean any(@NotModified Iterable<? extends T> receiver,
                               Function1<? super T, Boolean> predicate) {
            return false;
        }

        static <T> boolean all(@NotModified Iterable<? extends T> receiver,
                               Function1<? super T, Boolean> predicate) {
            return false;
        }

        static <T> boolean none(@NotModified Iterable<? extends T> receiver) {
            return false;
        }

        static <T> boolean none(@NotModified Iterable<? extends T> receiver,
                                Function1<? super T, Boolean> predicate) {
            return false;
        }

        /*
        public operator fun <T> Iterable<T>.contains(element: T): Boolean

        What `x in coll` calls when the element's type is not the collection's (`text in knownAnys`, a String? in a
        Set<String>): K2 resolves the extension, not Set.contains. Once maddi followed K2 there, detekt's
        MethodSignatureKt, SuppressionsKt and StringListSupport went @Immutable(hc=true) -> @FinalFields.
        */
        static <T> boolean contains(@NotModified Iterable<? extends T> receiver, T element) {
            return false;
        }

        /*
        THE READ-ONLY EXTENSIONS, ranked by the library-call census (-Dmaddi.libraryCallDump over detekt, coil and
        javalin): the calls into kotlin.collections that pass a receiver the defaults would call MODIFIED. Measured
        before these existed (TestKotlinCollectionReadsVsJava): a field only read through `s.joinToString()`,
        `l.firstOrNull()`, `s.mapNotNull { .. }` and 17 more was reported modified, where the Java twin -- the same
        read -- is not.

        Each reads its receiver and writes nothing. A fresh collection is @Independent(hc=true): it shares the
        receiver's elements and nothing else. An ELEMENT returned is @Independent(hc=true) too, as java.util.List.get
        is. A VIEW over the receiver (asSequence, withIndex) is dependent, so it carries no @Independent; nor does
        fold, which returns its accumulator. What a lambda argument does is its own contract's business, as for map.

        ⛔ Not here: the @InlineOnly members (isNotEmpty, orEmpty, find, error, require, ...). kotlinc inlines them and
        emits no method, so there is nothing for a contract to name; the front end lowers them instead.
        */
        @NotNull
        static <T> String joinToString(@NotModified Iterable<? extends T> receiver, @NotModified CharSequence separator,
                                       @NotModified CharSequence prefix, @NotModified CharSequence postfix, int limit,
                                       @NotModified CharSequence truncated,
                                       Function1<? super T, ? extends CharSequence> transform) {
            return null;
        }

        @Independent(hc = true)
        static <T> T first(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T first(@NotModified List<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T first(@NotModified Iterable<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return null;
        }

        @Independent(hc = true)
        static <T> T firstOrNull(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T firstOrNull(@NotModified List<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T firstOrNull(@NotModified Iterable<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return null;
        }

        @Independent(hc = true)
        static <T> T last(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T last(@NotModified List<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T lastOrNull(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T lastOrNull(@NotModified List<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T single(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T single(@NotModified List<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T singleOrNull(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T singleOrNull(@NotModified List<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T> T singleOrNull(@NotModified Iterable<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return null;
        }

        @Independent(hc = true)
        static <T> T elementAt(@NotModified Iterable<? extends T> receiver, int index) {
            return null;
        }

        @Independent(hc = true)
        static <T> T getOrNull(@NotModified List<? extends T> receiver, int index) {
            return null;
        }

        static <T> int count(@NotModified Iterable<? extends T> receiver) {
            return 0;
        }

        static <T> int count(@NotModified Iterable<? extends T> receiver, Function1<? super T, Boolean> predicate) {
            return 0;
        }

        static <T> int indexOf(@NotModified Iterable<? extends T> receiver, @NotModified T element) {
            return 0;
        }

        static <T> int indexOf(@NotModified List<? extends T> receiver, @NotModified T element) {
            return 0;
        }

        @Independent(hc = true)
        @NotNull
        static <T, R> List<R> mapNotNull(@NotModified Iterable<? extends T> receiver,
                                         Function1<? super T, ? extends R> transform) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T, R> List<R> flatMap(@NotModified Iterable<? extends T> receiver,
                                      Function1<? super T, ? extends Iterable<? extends R>> transform) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> filterNot(@NotModified Iterable<? extends T> receiver,
                                     Function1<? super T, Boolean> predicate) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> toList(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> Set<T> toSet(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> toMutableList(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> toMutableList(@NotModified Collection<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> Set<T> toMutableSet(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> distinct(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> reversed(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> take(@NotModified Iterable<? extends T> receiver, int n) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> drop(@NotModified Iterable<? extends T> receiver, int n) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> takeWhile(@NotModified Iterable<? extends T> receiver,
                                     Function1<? super T, Boolean> predicate) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> sortedWith(@NotModified Iterable<? extends T> receiver,
                                      @NotModified Comparator<? super T> comparator) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T, R extends Comparable<? super R>> List<T> sortedBy(@NotModified Iterable<? extends T> receiver,
                                                                     Function1<? super T, ? extends R> selector) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T, R extends Comparable<? super R>> List<T> sortedByDescending(
                @NotModified Iterable<? extends T> receiver, Function1<? super T, ? extends R> selector) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T extends Comparable<? super T>> List<T> sorted(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T, K> Map<K, List<T>> groupBy(@NotModified Iterable<? extends T> receiver,
                                              Function1<? super T, ? extends K> keySelector) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T, K> Map<K, T> associateBy(@NotModified Iterable<? extends T> receiver,
                                            Function1<? super T, ? extends K> keySelector) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T, K, V> Map<K, V> associate(@NotModified Iterable<? extends T> receiver,
                                             Function1<? super T, ? extends Pair<? extends K, ? extends V>> transform) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> Pair<List<T>, List<T>> partition(@NotModified Iterable<? extends T> receiver,
                                                   Function1<? super T, Boolean> predicate) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> plus(@NotModified Iterable<? extends T> receiver, @NotModified T element) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> plus(@NotModified Collection<? extends T> receiver, @NotModified T element) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> plus(@NotModified Iterable<? extends T> receiver,
                                @NotModified Iterable<? extends T> elements) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> plus(@NotModified Collection<? extends T> receiver,
                                @NotModified Iterable<? extends T> elements) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> minus(@NotModified Iterable<? extends T> receiver, @NotModified T element) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> minus(@NotModified Iterable<? extends T> receiver,
                                 @NotModified Iterable<? extends T> elements) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> Set<T> union(@NotModified Iterable<? extends T> receiver, @NotModified Iterable<? extends T> other) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> Set<T> intersect(@NotModified Iterable<? extends T> receiver,
                                    @NotModified Iterable<? extends T> other) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> Set<T> subtract(@NotModified Iterable<? extends T> receiver,
                                   @NotModified Iterable<? extends T> other) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T, R> List<Pair<T, R>> zip(@NotModified Iterable<? extends T> receiver,
                                           @NotModified Iterable<? extends R> other) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<List<T>> chunked(@NotModified Iterable<? extends T> receiver, int size) {
            return null;
        }

        @Independent(hc = true)
        static <T extends Comparable<? super T>> T maxOrNull(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        @Independent(hc = true)
        static <T extends Comparable<? super T>> T minOrNull(@NotModified Iterable<? extends T> receiver) {
            return null;
        }

        /* the accumulator comes back: dependent on `initial`, so no @Independent */
        static <T, R> R fold(@NotModified Iterable<? extends T> receiver, R initial,
                             Function2<? super R, ? super T, ? extends R> operation) {
            return null;
        }

        /* VIEWS over the receiver: they read it later, so they are dependent on it -- no @Independent */
        @NotNull
        static <T> Sequence<T> asSequence(@NotModified Iterable<? extends T> receiver) {
            return null;
        }
    }

    /*
    public fun <K, V> mapOf(vararg pairs: Pair<K, V>): Map<K, V>

    Builds a map holding the pairs' contents as hidden content; it modifies none of them.
    */
    class MapsKt__MapsKt$ {
        @Independent(hc = true)
        @NotNull
        @SafeVarargs
        static <K, V> Map<K, V> mapOf(@NotModified Pair<? extends K, ? extends V>... pairs) {
            return null;
        }

        /* a fresh mutable copy: shares the receiver's keys and values, nothing else */
        @Independent(hc = true)
        @NotNull
        static <K, V> Map<K, V> toMutableMap(@NotModified Map<? extends K, ? extends V> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <K, V> Map<K, V> emptyMap() {
            return null;
        }
    }

    /*
    The read-only collection factories. `listOf`, `setOf` and their empty forms are the most-called functions in
    any Kotlin codebase, and a `val x = setOf(..)` or `= emptySet()` field stays @Dependent without them — which
    caps its type. Each returns a fresh (or, for the empty forms, a shared constant) read-only collection holding
    the arguments as hidden content, and modifies nothing it is given.

    ⛔ The single-element overloads live in the JVM part class, not the common one.
    */
    class CollectionsKt__CollectionsKt$ {
        @Independent(hc = true)
        @NotNull
        @SafeVarargs
        static <T> List<T> listOf(@NotModified T... elements) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> List<T> emptyList() {
            return null;
        }

        /* `collection.indices`: a fresh IntRange, reading only the size */
        @Independent
        @NotNull
        static IntRange getIndices(@NotModified Collection<?> receiver) {
            return null;
        }

        /* `list.lastIndex` */
        static <T> int getLastIndex(@NotModified List<? extends T> receiver) {
            return 0;
        }
    }

    /*
    public fun <K, V> mapOf(pair: Pair<K, V>): Map<K, V>

    The single-pair overload lives in the JVM part class, like listOf(element); 96 calls over the three corpora
    reached it uncontracted while the vararg one was contracted.
    */
    class MapsKt__MapsJVMKt$ {
        @Independent(hc = true)
        @NotNull
        static <K, V> Map<K, V> mapOf(@NotModified Pair<? extends K, ? extends V> pair) {
            return null;
        }
    }

    /*
    The read-only extensions on a Map: the receiver is read, never written.
    */
    class MapsKt___MapsKt$ {
        static <K, V> void forEach(@NotModified Map<? extends K, ? extends V> receiver,
                                   Function1<? super Map.Entry<? extends K, ? extends V>, Unit> action) {
        }

        @Independent(hc = true)
        @NotNull
        static <K, V> List<Pair<K, V>> toList(@NotModified Map<? extends K, ? extends V> receiver) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <K, V, R> List<R> map(@NotModified Map<? extends K, ? extends V> receiver,
                                     Function1<? super Map.Entry<? extends K, ? extends V>, ? extends R> transform) {
            return null;
        }
    }

    class CollectionsKt__CollectionsJVMKt$ {
        @Independent(hc = true)
        @NotNull
        static <T> List<T> listOf(@NotModified T element) {
            return null;
        }
    }

    class SetsKt__SetsKt$ {
        @Independent(hc = true)
        @NotNull
        @SafeVarargs
        static <T> Set<T> setOf(@NotModified T... elements) {
            return null;
        }

        @Independent(hc = true)
        @NotNull
        static <T> Set<T> emptySet() {
            return null;
        }
    }

    class SetsKt__SetsJVMKt$ {
        @Independent(hc = true)
        @NotNull
        static <T> Set<T> setOf(@NotModified T element) {
            return null;
        }
    }
}
