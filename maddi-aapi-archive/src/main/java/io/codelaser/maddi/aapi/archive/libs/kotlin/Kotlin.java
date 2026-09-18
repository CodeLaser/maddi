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

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import kotlin.Pair;

/**
 * The annotated API for the {@code kotlin} package. It began with one type: every {@code val x by lazy { … }} in
 * every Kotlin corpus compiles to a private final {@code x$delegate} field of type {@code kotlin.Lazy}, so this
 * one contract governs the idiom wherever it appears.
 */
public class Kotlin {
    public static final String PACKAGE_NAME = "kotlin";

    /*
    public interface Lazy<out T> { public val value: T; public fun isInitialized(): Boolean }

    ⚠ WHY @ImmutableContainer(hc=true) AND NOT THE EVENTUAL FORM, WHICH IS THE HONEST ONE.

    Lazy has a documented two-state life cycle -- "Once the value was initialized it must not change during the
    rest of lifetime of this Lazy instance", and isInitialized() is exactly a @TestMark for it -- so
    @ImmutableContainer(after="value", hc=true) is what one wants to write. It is not writable. A mark is set by
    a METHOD (PropertyImpl.EVENTUAL_METHOD), and reading kotlin.Lazy's value is not a method call: `val value` is
    a property, which is a FIELD once the type is loaded from bytecode, and the read compiles to
    `this.x$delegate.value`. There is nothing to carry @Mark. Writing @TestMark on isInitialized() alone would
    leave a mark that is tested and never set -- the defect docs/book-vs-support-divergence.md finding 3 records
    in the book's own FirstThen listing.

    So this states the unconditional claim instead: from outside, a Lazy is a fixed container of one hidden
    value. That is the same bargain io.codelaser.maddi.support.Memo makes -- an idempotent slot whose write is
    observationally invisible -- and it is sound for SYNCHRONIZED (the `by lazy {}` default) and PUBLICATION,
    where the value is computed once, never changes, and is safely published. Under LazyThreadSafetyMode.NONE
    Kotlin documents the behaviour as undefined across threads; that is opt-in, and the caller has taken it on.

    ⛔ NOT @IgnoreModifications, which is the annotation this idiom really wants. The class-level disclaimer is
    read from TypeInfo.annotations() by SourceContractMaterializer.materializeIgnoreModificationsFromFieldType,
    i.e. from the annotation on the class file itself, and there is no IGNORE_MODIFICATIONS_TYPE property for it
    to travel as. An annotated API can only set PROPERTIES, so the disclaimer cannot be contracted onto a library
    type we do not compile. Giving it a type-level property would be the principled fix, and would let a Kotlin
    corpus get Memo's exact semantics rather than this approximation.

    What this buys, given that both Kotlin holders already reach @Immutable(hc=true) without it: the REASON.
    Today that verdict rests on Lazy being an interface whose implementations maddi never sees -- nothing says
    the mutation is benign. TestKotlinStdlibParse parses the whole stdlib from source, and the moment
    SynchronizedLazyImpl is in view it mutates in plain sight.
    */
    /*
    ⚠ TWO FRONT ENDS MODEL `val value` DIFFERENTLY, AND THIS FILE SEES ONLY ONE OF THEM.

    Annotated APIs are compiled through the JAVA inspector, which loads kotlin.Lazy from bytecode and sees an
    interface with a `getValue()` METHOD -- so that is what can be decorated here. The Kotlin front end models
    the same property as a FIELD: `kotlin.Lazy` there has `field value : T` and no getValue at all, and a
    delegated read compiles to `this.x$delegate.value`. A field annotation written here matches nothing on the
    Java side and is dropped in silence (it was: the first cut of this file annotated `final T value` and the
    generated Kotlin.json contained no field entry whatsoever).

    So the member annotations below serve JAVA callers of kotlin.Lazy, which mixed projects have. What carries
    the idiom for KOTLIN callers is the type-level contract, which applies whichever way the members are
    modelled.
    */
    @ImmutableContainer(hc = true)
    interface Lazy$<T> {
        //public val value: T -- `getValue()` in the Java view; a field in the Kotlin one, see above
        @Independent(hc = true)
        @NotModified
        T getValue();

        //public fun isInitialized(): Boolean
        @NotModified
        boolean isInitialized();
    }

    /*
    public data class Pair<out A, out B>(public val first: A, public val second: B)

    Two `val`s of hidden-content type and nothing that writes them: a fixed holder of two values, which is what
    @ImmutableContainer(hc=true) says. The components are `getFirst()`/`getSecond()` in the Java view (see the
    note on Lazy: an annotated API is compiled through the Java inspector, so a property is a method here).
    */
    @ImmutableContainer(hc = true)
    class Pair$<A, B> {
        Pair$(A first, B second) { }

        @Independent(hc = true)
        @NotModified
        A getFirst() { return null; }

        @Independent(hc = true)
        @NotModified
        B getSecond() { return null; }
    }

    /*
    public infix fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)

    ⚠ THE FILE FACADE OF AN EXTENSION, whose receiver is the first JVM parameter -- which is how kotlinc emits it
    and, since maddi#43, how maddi models it, so the two agree and a Kotlin `a to b` reaches this contract.

    `a to b` is the reason this file grew past kotlin.Lazy. Once library extension calls resolved (maddi#43),
    every one of them became a call to a method with no annotated API, i.e. a method that may modify what it is
    given -- and 16 detekt verdicts fell, every `DefaultValue` subclass in detekt-generator among them, whose
    `printAsYaml` writes `name to quoted`. The pair holds its two arguments and modifies neither.
    */
    class TuplesKt$ {
        @Independent(hc = true)
        @NotNull
        static <A, B> Pair<A, B> to(@NotModified A receiver, @NotModified B that) { return null; }
    }
}
