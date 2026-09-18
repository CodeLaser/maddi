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

import java.util.List;
import java.util.Map;

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
    }
}
