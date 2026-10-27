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

package io.codelaser.maddi.aapi.archive.libs.assertj;

import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;

/**
 * The annotated API for AssertJ's {@code org.assertj.core.api} (compiled against assertj-core 3.27.7; CodeLaser/maddi#116).
 * Without it, an unannotated library parameter is modified, so every {@code assertThat(x)} in a project's tests made
 * {@code x} modified -- and through the reachability pass every field and parameter behind it. On langchain4j-core,
 * {@code UserMessage.contents}, {@code ChatRequest.messages} and {@code AiMessage.toolExecutionRequests} were
 * modified for that reason alone.
 * <p>
 * Three rules, applied by a generator over {@code javap -public} of the jar:
 * <ul>
 * <li><b>Entry points.</b> The "actual" argument -- the first -- of every {@code assertThat*}, {@code then*} and
 *     {@code assumeThat*} method of the thirteen types that declare them ({@code Assertions},
 *     {@code BDDAssertions}, {@code WithAssertions}, the soft-assertion providers, {@code Assumptions}, the Java 6
 *     and ForClassTypes/ForInterfaceTypes variants) is {@code @NotModified}. Left out, so at the shallow default
 *     (modified): an actual that the assertion CONSUMES or advances ({@code Iterator}, {@code Spliterator}, the
 *     streams, {@code InputStream}, a stateful {@code Matcher}), and a functional actual ({@code ThrowingCallable},
 *     the predicates), which the assertion calls. A primitive or {@code String} actual needs nothing. Each entry
 *     point is also {@code @Independent}, method and actual: the assert object holds the actual, but nothing done
 *     to the assert object -- every chained assertion modifies it, by the shallow default -- modifies the actual.
 *     For a static method that is the shallow default already; {@code WithAssertions} and the soft-assertion
 *     providers declare their entry points as instance (default) methods, whose result would otherwise be
 *     dependent.</li>
 * <li><b>Assert types.</b> Every {@code *Assert} type, and their supertypes {@code Descriptable} and
 *     {@code ExtensionPoints}, is {@code @Container}: no method modifies its arguments, the expected values of
 *     {@code isEqualTo}, {@code containsExactly}, {@code hasSameElementsAs}, ... A standard functional argument ({@code satisfies(Consumer)}) keeps its {@code @IgnoreModifications}: the shallow analyzer
 *     decides that before it reads the owner's {@code @Container}.</li>
 * <li><b>Consumed arguments of assert types</b> are written out {@code @Modified}: the stream and iterator factories
 *     of {@code ListAssert} and {@code IteratorAssert}, {@code SpliteratorAssert}'s, and the expected
 *     {@code InputStream} of {@code hasSameContentAs}/{@code hasContentEqualTo}.</li>
 * </ul>
 * Other AssertJ versions load these results too: a method a version lacks is skipped with a warning, and one it
 * adds takes the shallow default (langchain4j uses 4.0.0-M1).
 */
public class OrgAssertjCoreApi {
    public static final String PACKAGE_NAME = "org.assertj.core.api";

    //public class org.assertj.core.api.Assertions
    class Assertions$ {
        // primitive-array actuals (an entry point like any other: read, not consumed) and fail(.., cause), 2026-10-13
        @Independent static org.assertj.core.api.AbstractBooleanArrayAssert<?> assertThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent static org.assertj.core.api.Boolean2DArrayAssert assertThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractByteArrayAssert<?> assertThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent static org.assertj.core.api.Byte2DArrayAssert assertThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractCharArrayAssert<?> assertThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent static org.assertj.core.api.Char2DArrayAssert assertThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractDoubleArrayAssert<?> assertThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent static org.assertj.core.api.Double2DArrayAssert assertThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractFloatArrayAssert<?> assertThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractIntArrayAssert<?> assertThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent static org.assertj.core.api.Int2DArrayAssert assertThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent static org.assertj.core.api.Float2DArrayAssert assertThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractLongArrayAssert<?> assertThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent static org.assertj.core.api.Long2DArrayAssert assertThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractShortArrayAssert<?> assertThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent static org.assertj.core.api.Short2DArrayAssert assertThat(@Independent @NotModified short[][] arg0) { return null; }
        static <T> T fail(java.lang.String arg0, @NotModified java.lang.Throwable arg1) { return null; }
        static <T> T fail(@NotModified java.lang.Throwable arg0) { return null; }
        // the callable is CALLED, not modified (ThrowingCallable is not @FunctionalInterface, so the shallow
        // rule for function types does not reach it); the message arguments are formatted, not modified
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.String arg1, @NotModified java.lang.Object... arg2) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatCode(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.Throwable catchThrowable(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static <THROWABLE extends java.lang.Throwable> THROWABLE catchThrowableOfType(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.Class<THROWABLE> arg1) { return null; }
        static <THROWABLE extends java.lang.Throwable> THROWABLE catchThrowableOfType(java.lang.Class<THROWABLE> arg0, @IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg1) { return null; }
        static java.lang.Exception catchException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.RuntimeException catchRuntimeException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.NullPointerException catchNullPointerException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.IllegalArgumentException catchIllegalArgumentException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.io.IOException catchIOException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.ReflectiveOperationException catchReflectiveOperationException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.IllegalStateException catchIllegalStateException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.IndexOutOfBoundsException catchIndexOutOfBoundsException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified java.util.concurrent.CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified java.util.concurrent.CompletionStage<RESULT> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.OptionalAssert<VALUE> assertThat(@Independent @NotModified java.util.Optional<VALUE> arg0) { return null; }
        @Independent
        static org.assertj.core.api.OptionalDoubleAssert assertThat(@Independent @NotModified java.util.OptionalDouble arg0) { return null; }
        @Independent
        static org.assertj.core.api.OptionalIntAssert assertThat(@Independent @NotModified java.util.OptionalInt arg0) { return null; }
        @Independent
        static org.assertj.core.api.OptionalLongAssert assertThat(@Independent @NotModified java.util.OptionalLong arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBigDecimalAssert<?> assertThat(@Independent @NotModified java.math.BigDecimal arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBigIntegerAssert<?> assertThat(@Independent @NotModified java.math.BigInteger arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractUriAssert<?> assertThat(@Independent @NotModified java.net.URI arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractUrlAssert<?> assertThat(@Independent @NotModified java.net.URL arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBooleanAssert<?> assertThat(@Independent @NotModified java.lang.Boolean arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractByteAssert<?> assertThat(@Independent @NotModified java.lang.Byte arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharacterAssert<?> assertThat(@Independent @NotModified java.lang.Character arg0) { return null; }
        @Independent
        static org.assertj.core.api.ClassAssert assertThat(@Independent @NotModified java.lang.Class<?> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDoubleAssert<?> assertThat(@Independent @NotModified java.lang.Double arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractFileAssert<?> assertThat(@Independent @NotModified java.io.File arg0) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.FutureAssert<RESULT> assertThat(@Independent @NotModified java.util.concurrent.Future<RESULT> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractFloatAssert<?> assertThat(@Independent @NotModified java.lang.Float arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractIntegerAssert<?> assertThat(@Independent @NotModified java.lang.Integer arg0) { return null; }
        @Independent
        static <ACTUAL extends java.lang.Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.FactoryBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0, org.assertj.core.api.AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends java.lang.Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.ClassBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified ACTUAL arg0, java.lang.Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends java.util.List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.FactoryBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified java.util.List<? extends ELEMENT> arg0, org.assertj.core.api.AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ELEMENT, ACTUAL extends java.util.List<? extends ELEMENT>, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.ClassBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified java.util.List<? extends ELEMENT> arg0, java.lang.Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLongAssert<?> assertThat(@Independent @NotModified java.lang.Long arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ObjectAssert<T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ObjectArrayAssert<T> assertThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.Object2DArrayAssert<T> assertThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractShortAssert<?> assertThat(@Independent @NotModified java.lang.Short arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDateAssert<?> assertThat(@Independent @NotModified java.util.Date arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractZonedDateTimeAssert<?> assertThat(@Independent @NotModified java.time.ZonedDateTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.TemporalAssert assertThatTemporal(@Independent @NotModified java.time.temporal.Temporal arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLocalDateTimeAssert<?> assertThat(@Independent @NotModified java.time.LocalDateTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractOffsetDateTimeAssert<?> assertThat(@Independent @NotModified java.time.OffsetDateTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractOffsetTimeAssert<?> assertThat(@Independent @NotModified java.time.OffsetTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLocalTimeAssert<?> assertThat(@Independent @NotModified java.time.LocalTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLocalDateAssert<?> assertThat(@Independent @NotModified java.time.LocalDate arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractYearMonthAssert<?> assertThat(@Independent @NotModified java.time.YearMonth arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractInstantAssert<?> assertThat(@Independent @NotModified java.time.Instant arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDurationAssert<?> assertThat(@Independent @NotModified java.time.Duration arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractPeriodAssert<?> assertThat(@Independent @NotModified java.time.Period arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicBooleanAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicBoolean arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicIntegerAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicInteger arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicIntegerArrayAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerArray arg0) { return null; }
        @Independent
        static <OBJECT> org.assertj.core.api.AtomicIntegerFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static org.assertj.core.api.LongAdderAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.LongAdder arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicLongAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLong arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicLongArrayAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLongArray arg0) { return null; }
        @Independent
        static <OBJECT> org.assertj.core.api.AtomicLongFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicReferenceAssert<VALUE> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReference<VALUE> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.AtomicReferenceArrayAssert<ELEMENT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        static <FIELD, OBJECT> org.assertj.core.api.AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicMarkableReferenceAssert<VALUE> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicStampedReferenceAssert<VALUE> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        static <T extends java.lang.Throwable> org.assertj.core.api.AbstractThrowableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ObjectAssert<T> assertThatObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T extends java.lang.Throwable> org.assertj.core.api.ThrowableTypeAssert<T> assertThatExceptionOfType(@Independent @NotModified java.lang.Class<? extends T> arg0) { return null; }
        @Independent
        static <T> T assertThat(@Independent @NotModified org.assertj.core.api.AssertProvider<T> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThat(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThatCharSequence(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThat(@Independent @NotModified java.lang.StringBuilder arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThat(@Independent @NotModified java.lang.StringBuffer arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.IterableAssert<ELEMENT> assertThat(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.IterableAssert<ELEMENT> assertThatIterable(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <E> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends E>, E, org.assertj.core.api.ObjectAssert<E>> assertThat(@Independent @NotModified java.util.Collection<? extends E> arg0) { return null; }
        @Independent
        static <E> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends E>, E, org.assertj.core.api.ObjectAssert<E>> assertThatCollection(@Independent @NotModified java.util.Collection<? extends E> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.ListAssert<ELEMENT> assertThat(@Independent @NotModified java.util.List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.ListAssert<ELEMENT> assertThatList(@Independent @NotModified java.util.List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractPathAssert<?> assertThat(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractPathAssert<?> assertThatPath(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        static <K, V> org.assertj.core.api.MapAssert<K, V> assertThat(@Independent @NotModified java.util.Map<K, V> arg0) { return null; }
        @Independent
        static <T extends java.lang.Comparable<? super T>> org.assertj.core.api.AbstractComparableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractUniversalComparableAssert<?, T> assertThatComparable(@Independent @NotModified java.lang.Comparable<T> arg0) { return null; }
        @Independent
        static <T extends org.assertj.core.api.AssertDelegateTarget> T assertThat(@Independent @NotModified T arg0) { return null; }
    }

    //public class org.assertj.core.api.BDDAssertions
    class BDDAssertions$ {
        @Independent static org.assertj.core.api.AbstractBooleanArrayAssert<?> then(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent static org.assertj.core.api.Boolean2DArrayAssert then(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractByteArrayAssert<?> then(@Independent @NotModified byte[] arg0) { return null; }
        @Independent static org.assertj.core.api.Byte2DArrayAssert then(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractCharArrayAssert<?> then(@Independent @NotModified char[] arg0) { return null; }
        @Independent static org.assertj.core.api.Char2DArrayAssert then(@Independent @NotModified char[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractDoubleArrayAssert<?> then(@Independent @NotModified double[] arg0) { return null; }
        @Independent static org.assertj.core.api.Double2DArrayAssert then(@Independent @NotModified double[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractFloatArrayAssert<?> then(@Independent @NotModified float[] arg0) { return null; }
        @Independent static org.assertj.core.api.Float2DArrayAssert then(@Independent @NotModified float[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractIntArrayAssert<?> then(@Independent @NotModified int[] arg0) { return null; }
        @Independent static org.assertj.core.api.Int2DArrayAssert then(@Independent @NotModified int[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractLongArrayAssert<?> then(@Independent @NotModified long[] arg0) { return null; }
        @Independent static org.assertj.core.api.Long2DArrayAssert then(@Independent @NotModified long[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractShortArrayAssert<?> then(@Independent @NotModified short[] arg0) { return null; }
        @Independent static org.assertj.core.api.Short2DArrayAssert then(@Independent @NotModified short[][] arg0) { return null; }
        static <T> T fail(java.lang.String arg0, @NotModified java.lang.Throwable arg1) { return null; }
        static <T> T fail(@NotModified java.lang.Throwable arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> thenThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> thenThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.String arg1, @NotModified java.lang.Object... arg2) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> thenCode(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.Throwable catchThrowable(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static <THROWABLE extends java.lang.Throwable> THROWABLE catchThrowableOfType(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.Class<THROWABLE> arg1) { return null; }
        static <THROWABLE extends java.lang.Throwable> THROWABLE catchThrowableOfType(java.lang.Class<THROWABLE> arg0, @IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg1) { return null; }
        static java.lang.Exception catchException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.RuntimeException catchRuntimeException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.NullPointerException catchNullPointerException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.IllegalArgumentException catchIllegalArgumentException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.io.IOException catchIOException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.ReflectiveOperationException catchReflectiveOperationException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.IllegalStateException catchIllegalStateException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.IndexOutOfBoundsException catchIndexOutOfBoundsException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.OptionalAssert<VALUE> then(@Independent @NotModified java.util.Optional<VALUE> arg0) { return null; }
        @Independent
        static org.assertj.core.api.OptionalIntAssert then(@Independent @NotModified java.util.OptionalInt arg0) { return null; }
        @Independent
        static org.assertj.core.api.OptionalLongAssert then(@Independent @NotModified java.util.OptionalLong arg0) { return null; }
        @Independent
        static org.assertj.core.api.OptionalDoubleAssert then(@Independent @NotModified java.util.OptionalDouble arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBigDecimalAssert<?> then(@Independent @NotModified java.math.BigDecimal arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBigIntegerAssert<?> then(@Independent @NotModified java.math.BigInteger arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBooleanAssert<?> then(@Independent @NotModified java.lang.Boolean arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractByteAssert<?> then(@Independent @NotModified java.lang.Byte arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharacterAssert<?> then(@Independent @NotModified java.lang.Character arg0) { return null; }
        @Independent
        static org.assertj.core.api.ClassAssert then(@Independent @NotModified java.lang.Class<?> arg0) { return null; }
        @Independent
        static <T extends java.lang.Comparable<? super T>> org.assertj.core.api.AbstractComparableAssert<?, T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractUniversalComparableAssert<?, T> thenComparable(@Independent @NotModified java.lang.Comparable<T> arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.IterableAssert<T> then(@Independent @NotModified java.lang.Iterable<? extends T> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.IterableAssert<ELEMENT> thenIterable(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ACTUAL extends java.lang.Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.FactoryBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0, org.assertj.core.api.AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends java.lang.Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.ClassBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified ACTUAL arg0, java.lang.Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends java.util.List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.FactoryBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified java.util.List<? extends ELEMENT> arg0, org.assertj.core.api.AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ELEMENT, ACTUAL extends java.util.List<? extends ELEMENT>, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.ClassBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified java.util.List<? extends ELEMENT> arg0, java.lang.Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDoubleAssert<?> then(@Independent @NotModified java.lang.Double arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractFileAssert<?> then(@Independent @NotModified java.io.File arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractPathAssert<?> then(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractPathAssert<?> thenPath(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.FutureAssert<RESULT> then(@Independent @NotModified java.util.concurrent.Future<RESULT> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractFloatAssert<?> then(@Independent @NotModified java.lang.Float arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractIntegerAssert<?> then(@Independent @NotModified java.lang.Integer arg0) { return null; }
        @Independent
        static <E> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends E>, E, org.assertj.core.api.ObjectAssert<E>> then(@Independent @NotModified java.util.Collection<? extends E> arg0) { return null; }
        @Independent
        static <E> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends E>, E, org.assertj.core.api.ObjectAssert<E>> thenCollection(@Independent @NotModified java.util.Collection<? extends E> arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ListAssert<T> then(@Independent @NotModified java.util.List<? extends T> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.ListAssert<ELEMENT> thenList(@Independent @NotModified java.util.List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLongAssert<?> then(@Independent @NotModified java.lang.Long arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ObjectAssert<T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ObjectArrayAssert<T> then(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.Object2DArrayAssert<T> then(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        static <K, V> org.assertj.core.api.MapAssert<K, V> then(@Independent @NotModified java.util.Map<K, V> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractShortAssert<?> then(@Independent @NotModified java.lang.Short arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> then(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> thenCharSequence(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> then(@Independent @NotModified java.lang.StringBuilder arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> then(@Independent @NotModified java.lang.StringBuffer arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDateAssert<?> then(@Independent @NotModified java.util.Date arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicBooleanAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicBoolean arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicIntegerAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicInteger arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicIntegerArrayAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerArray arg0) { return null; }
        @Independent
        static <OBJECT> org.assertj.core.api.AtomicIntegerFieldUpdaterAssert<OBJECT> then(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static org.assertj.core.api.LongAdderAssert then(@Independent @NotModified java.util.concurrent.atomic.LongAdder arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicLongAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicLong arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicLongArrayAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicLongArray arg0) { return null; }
        @Independent
        static <OBJECT> org.assertj.core.api.AtomicLongFieldUpdaterAssert<OBJECT> then(@Independent @NotModified java.util.concurrent.atomic.AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicReferenceAssert<VALUE> then(@Independent @NotModified java.util.concurrent.atomic.AtomicReference<VALUE> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.AtomicReferenceArrayAssert<ELEMENT> then(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        static <FIELD, OBJECT> org.assertj.core.api.AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> then(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicMarkableReferenceAssert<VALUE> then(@Independent @NotModified java.util.concurrent.atomic.AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicStampedReferenceAssert<VALUE> then(@Independent @NotModified java.util.concurrent.atomic.AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        static <T extends java.lang.Throwable> org.assertj.core.api.AbstractThrowableAssert<?, T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ObjectAssert<T> thenObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ObjectAssert<T> thenWith(@Independent @NotModified T arg0, java.util.function.Consumer<T>... arg1) { return null; }
        @Independent
        static org.assertj.core.api.TemporalAssert thenTemporal(@Independent @NotModified java.time.temporal.Temporal arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLocalDateAssert<?> then(@Independent @NotModified java.time.LocalDate arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractYearMonthAssert<?> then(@Independent @NotModified java.time.YearMonth arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLocalDateTimeAssert<?> then(@Independent @NotModified java.time.LocalDateTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractZonedDateTimeAssert<?> then(@Independent @NotModified java.time.ZonedDateTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLocalTimeAssert<?> then(@Independent @NotModified java.time.LocalTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractOffsetTimeAssert<?> then(@Independent @NotModified java.time.OffsetTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractInstantAssert<?> then(@Independent @NotModified java.time.Instant arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDurationAssert<?> then(@Independent @NotModified java.time.Duration arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractPeriodAssert<?> then(@Independent @NotModified java.time.Period arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractUriAssert<?> then(@Independent @NotModified java.net.URI arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractUrlAssert<?> then(@Independent @NotModified java.net.URL arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractOffsetDateTimeAssert<?> then(@Independent @NotModified java.time.OffsetDateTime arg0) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> then(@Independent @NotModified java.util.concurrent.CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> then(@Independent @NotModified java.util.concurrent.CompletionStage<RESULT> arg0) { return null; }
        @Independent
        static <T extends org.assertj.core.api.AssertDelegateTarget> T then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> T then(@Independent @NotModified org.assertj.core.api.AssertProvider<T> arg0) { return null; }
        @Independent
        static <T extends java.lang.Throwable> org.assertj.core.api.ThrowableTypeAssert<T> thenExceptionOfType(@Independent @NotModified java.lang.Class<? extends T> arg0) { return null; }
    }

    //public interface org.assertj.core.api.WithAssertions
    class WithAssertions$ {
        <T> T fail(java.lang.String arg0, @NotModified java.lang.Throwable arg1) { return null; }
        <T> T fail(@NotModified java.lang.Throwable arg0) { return null; }
        @Independent org.assertj.core.api.AbstractLongArrayAssert<?> assertThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent org.assertj.core.api.Long2DArrayAssert assertThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractShortArrayAssert<?> assertThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent org.assertj.core.api.Short2DArrayAssert assertThat(@Independent @NotModified short[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractCharArrayAssert<?> assertThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent org.assertj.core.api.Char2DArrayAssert assertThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractBooleanArrayAssert<?> assertThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent org.assertj.core.api.Boolean2DArrayAssert assertThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractByteArrayAssert<?> assertThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent org.assertj.core.api.Byte2DArrayAssert assertThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractIntArrayAssert<?> assertThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent org.assertj.core.api.Int2DArrayAssert assertThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent org.assertj.core.api.Float2DArrayAssert assertThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractFloatArrayAssert<?> assertThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractDoubleArrayAssert<?> assertThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent org.assertj.core.api.Double2DArrayAssert assertThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.String arg1, @NotModified java.lang.Object... arg2) { return null; }
        @Independent org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatCode(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        java.lang.Throwable catchThrowable(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        <THROWABLE extends java.lang.Throwable> THROWABLE catchThrowableOfType(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.Class<THROWABLE> arg1) { return null; }
        <THROWABLE extends java.lang.Throwable> THROWABLE catchThrowableOfType(java.lang.Class<THROWABLE> arg0, @IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg1) { return null; }
        java.lang.Exception catchException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        java.lang.RuntimeException catchRuntimeException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        java.lang.NullPointerException catchNullPointerException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        java.lang.IllegalArgumentException catchIllegalArgumentException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        java.io.IOException catchIOException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        java.lang.ReflectiveOperationException catchReflectiveOperationException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        java.lang.IllegalStateException catchIllegalStateException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        java.lang.IndexOutOfBoundsException catchIndexOutOfBoundsException(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ObjectArrayAssert<T> assertThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.Object2DArrayAssert<T> assertThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        <T extends org.assertj.core.api.AssertDelegateTarget> T assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <K, V> org.assertj.core.api.MapAssert<K, V> assertThat(@Independent @NotModified java.util.Map<K, V> arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractLongAssert<?> assertThat(@Independent @NotModified java.lang.Long arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ObjectAssert<T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractDateAssert<?> assertThat(@Independent @NotModified java.util.Date arg0) { return null; }
        @Independent
        <T extends java.lang.Throwable> org.assertj.core.api.AbstractThrowableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractBigDecimalAssert<?> assertThat(@Independent @NotModified java.math.BigDecimal arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractBigIntegerAssert<?> assertThat(@Independent @NotModified java.math.BigInteger arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicBooleanAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicBoolean arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicIntegerAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicInteger arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicIntegerArrayAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerArray arg0) { return null; }
        @Independent
        <OBJECT> org.assertj.core.api.AtomicIntegerFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        org.assertj.core.api.LongAdderAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.LongAdder arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicLongAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLong arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicLongArrayAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLongArray arg0) { return null; }
        @Independent
        <OBJECT> org.assertj.core.api.AtomicLongFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.AtomicReferenceAssert<VALUE> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReference<VALUE> arg0) { return null; }
        @Independent
        <ELEMENT> org.assertj.core.api.AtomicReferenceArrayAssert<ELEMENT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        <FIELD, OBJECT> org.assertj.core.api.AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.AtomicMarkableReferenceAssert<VALUE> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.AtomicStampedReferenceAssert<VALUE> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThat(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThat(@Independent @NotModified java.lang.StringBuilder arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThat(@Independent @NotModified java.lang.StringBuffer arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractShortAssert<?> assertThat(@Independent @NotModified java.lang.Short arg0) { return null; }
        @Independent
        org.assertj.core.api.ClassAssert assertThat(@Independent @NotModified java.lang.Class<?> arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractCharacterAssert<?> assertThat(@Independent @NotModified java.lang.Character arg0) { return null; }
        @Independent
        <T extends java.lang.Comparable<? super T>> org.assertj.core.api.AbstractComparableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.AbstractUniversalComparableAssert<?, T> assertThatComparable(@Independent @NotModified java.lang.Comparable<T> arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.IterableAssert<T> assertThat(@Independent @NotModified java.lang.Iterable<? extends T> arg0) { return null; }
        @Independent
        <ELEMENT> org.assertj.core.api.IterableAssert<ELEMENT> assertThatIterable(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        <ACTUAL extends java.lang.Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.FactoryBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0, org.assertj.core.api.AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        <ACTUAL extends java.lang.Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.ClassBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified ACTUAL arg0, java.lang.Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        org.assertj.core.api.AbstractBooleanAssert<?> assertThat(@Independent @NotModified java.lang.Boolean arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractByteAssert<?> assertThat(@Independent @NotModified java.lang.Byte arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractFileAssert<?> assertThat(@Independent @NotModified java.io.File arg0) { return null; }
        @Independent
        <RESULT> org.assertj.core.api.FutureAssert<RESULT> assertThat(@Independent @NotModified java.util.concurrent.Future<RESULT> arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractPathAssert<?> assertThat(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractPathAssert<?> assertThatPath(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractFloatAssert<?> assertThat(@Independent @NotModified java.lang.Float arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractIntegerAssert<?> assertThat(@Independent @NotModified java.lang.Integer arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractDoubleAssert<?> assertThat(@Independent @NotModified java.lang.Double arg0) { return null; }
        @Independent
        <E> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends E>, E, org.assertj.core.api.ObjectAssert<E>> assertThat(@Independent @NotModified java.util.Collection<? extends E> arg0) { return null; }
        @Independent
        <E> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends E>, E, org.assertj.core.api.ObjectAssert<E>> assertThatCollection(@Independent @NotModified java.util.Collection<? extends E> arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ListAssert<T> assertThat(@Independent @NotModified java.util.List<? extends T> arg0) { return null; }
        @Independent
        <ELEMENT> org.assertj.core.api.ListAssert<ELEMENT> assertThatList(@Independent @NotModified java.util.List<? extends ELEMENT> arg0) { return null; }
        @Independent
        <ELEMENT, ACTUAL extends java.util.List<? extends ELEMENT>, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.ClassBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified java.util.List<? extends ELEMENT> arg0, java.lang.Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        <ACTUAL extends java.util.List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.FactoryBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified java.util.List<? extends ELEMENT> arg0, org.assertj.core.api.AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        org.assertj.core.api.AbstractZonedDateTimeAssert<?> assertThat(@Independent @NotModified java.time.ZonedDateTime arg0) { return null; }
        @Independent
        <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified java.util.concurrent.CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified java.util.concurrent.CompletionStage<RESULT> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.OptionalAssert<VALUE> assertThat(@Independent @NotModified java.util.Optional<VALUE> arg0) { return null; }
        @Independent
        org.assertj.core.api.OptionalDoubleAssert assertThat(@Independent @NotModified java.util.OptionalDouble arg0) { return null; }
        @Independent
        org.assertj.core.api.OptionalIntAssert assertThat(@Independent @NotModified java.util.OptionalInt arg0) { return null; }
        @Independent
        org.assertj.core.api.OptionalLongAssert assertThat(@Independent @NotModified java.util.OptionalLong arg0) { return null; }
        @Independent
        org.assertj.core.api.TemporalAssert assertThatTemporal(@Independent @NotModified java.time.temporal.Temporal arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractLocalDateTimeAssert<?> assertThat(@Independent @NotModified java.time.LocalDateTime arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractLocalDateAssert<?> assertThat(@Independent @NotModified java.time.LocalDate arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractYearMonthAssert<?> assertThat(@Independent @NotModified java.time.YearMonth arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractLocalTimeAssert<?> assertThat(@Independent @NotModified java.time.LocalTime arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractInstantAssert<?> assertThat(@Independent @NotModified java.time.Instant arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractDurationAssert<?> assertThat(@Independent @NotModified java.time.Duration arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractPeriodAssert<?> assertThat(@Independent @NotModified java.time.Period arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractOffsetTimeAssert<?> assertThat(@Independent @NotModified java.time.OffsetTime arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractOffsetDateTimeAssert<?> assertThat(@Independent @NotModified java.time.OffsetDateTime arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ObjectAssert<T> assertThatObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T extends java.lang.Throwable> org.assertj.core.api.ThrowableTypeAssert<T> assertThatExceptionOfType(@Independent @NotModified java.lang.Class<? extends T> arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractUrlAssert<?> assertThat(@Independent @NotModified java.net.URL arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractUriAssert<?> assertThat(@Independent @NotModified java.net.URI arg0) { return null; }
        @Independent
        <T> T assertThat(@Independent @NotModified org.assertj.core.api.AssertProvider<T> arg0) { return null; }
    }

    //public class org.assertj.core.api.AssertionsForClassTypes
    class AssertionsForClassTypes$ {
        @Independent static org.assertj.core.api.AbstractBooleanArrayAssert<?> assertThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent static org.assertj.core.api.Boolean2DArrayAssert assertThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractByteArrayAssert<?> assertThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent static org.assertj.core.api.Byte2DArrayAssert assertThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractCharArrayAssert<?> assertThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent static org.assertj.core.api.Char2DArrayAssert assertThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractDoubleArrayAssert<?> assertThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent static org.assertj.core.api.Double2DArrayAssert assertThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractFloatArrayAssert<?> assertThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent static org.assertj.core.api.Float2DArrayAssert assertThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractIntArrayAssert<?> assertThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent static org.assertj.core.api.Int2DArrayAssert assertThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractLongArrayAssert<?> assertThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent static org.assertj.core.api.Long2DArrayAssert assertThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractShortArrayAssert<?> assertThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent static org.assertj.core.api.Short2DArrayAssert assertThat(@Independent @NotModified short[][] arg0) { return null; }
        static void fail(java.lang.String arg0, @NotModified java.lang.Throwable arg1) { }
        static void fail(@NotModified java.lang.Throwable arg0) { }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.String arg1, @NotModified java.lang.Object... arg2) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatCode(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.Throwable catchThrowable(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static <THROWABLE extends java.lang.Throwable> THROWABLE catchThrowableOfType(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.Class<THROWABLE> arg1) { return null; }
        static <THROWABLE extends java.lang.Throwable> THROWABLE catchThrowableOfType(java.lang.Class<THROWABLE> arg0, @IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg1) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified java.util.concurrent.CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.OptionalAssert<VALUE> assertThat(@Independent @NotModified java.util.Optional<VALUE> arg0) { return null; }
        @Independent
        static org.assertj.core.api.OptionalDoubleAssert assertThat(@Independent @NotModified java.util.OptionalDouble arg0) { return null; }
        @Independent
        static org.assertj.core.api.OptionalIntAssert assertThat(@Independent @NotModified java.util.OptionalInt arg0) { return null; }
        @Independent
        static org.assertj.core.api.OptionalLongAssert assertThat(@Independent @NotModified java.util.OptionalLong arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBigDecimalAssert<?> assertThat(@Independent @NotModified java.math.BigDecimal arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractUriAssert<?> assertThat(@Independent @NotModified java.net.URI arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractUrlAssert<?> assertThat(@Independent @NotModified java.net.URL arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBooleanAssert<?> assertThat(@Independent @NotModified java.lang.Boolean arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractByteAssert<?> assertThat(@Independent @NotModified java.lang.Byte arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharacterAssert<?> assertThat(@Independent @NotModified java.lang.Character arg0) { return null; }
        @Independent
        static org.assertj.core.api.ClassAssert assertThat(@Independent @NotModified java.lang.Class<?> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDoubleAssert<?> assertThat(@Independent @NotModified java.lang.Double arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractFileAssert<?> assertThat(@Independent @NotModified java.io.File arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractFloatAssert<?> assertThat(@Independent @NotModified java.lang.Float arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractIntegerAssert<?> assertThat(@Independent @NotModified java.lang.Integer arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLongAssert<?> assertThat(@Independent @NotModified java.lang.Long arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ObjectAssert<T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ObjectArrayAssert<T> assertThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.Object2DArrayAssert<T> assertThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractShortAssert<?> assertThat(@Independent @NotModified java.lang.Short arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThat(@Independent @NotModified java.lang.StringBuilder arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThat(@Independent @NotModified java.lang.StringBuffer arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDateAssert<?> assertThat(@Independent @NotModified java.util.Date arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractZonedDateTimeAssert<?> assertThat(@Independent @NotModified java.time.ZonedDateTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLocalDateTimeAssert<?> assertThat(@Independent @NotModified java.time.LocalDateTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractOffsetDateTimeAssert<?> assertThat(@Independent @NotModified java.time.OffsetDateTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractOffsetTimeAssert<?> assertThat(@Independent @NotModified java.time.OffsetTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLocalTimeAssert<?> assertThat(@Independent @NotModified java.time.LocalTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLocalDateAssert<?> assertThat(@Independent @NotModified java.time.LocalDate arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractYearMonthAssert<?> assertThat(@Independent @NotModified java.time.YearMonth arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractInstantAssert<?> assertThat(@Independent @NotModified java.time.Instant arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDurationAssert<?> assertThat(@Independent @NotModified java.time.Duration arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractPeriodAssert<?> assertThat(@Independent @NotModified java.time.Period arg0) { return null; }
        @Independent
        static <T extends java.lang.Throwable> org.assertj.core.api.AbstractThrowableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T extends java.lang.Throwable> org.assertj.core.api.ThrowableTypeAssert<T> assertThatExceptionOfType(@Independent @NotModified java.lang.Class<? extends T> arg0) { return null; }
    }

    //public class org.assertj.core.api.AssertionsForInterfaceTypes
    class AssertionsForInterfaceTypes$ {
        @Independent
        static <T> T assertThat(@Independent @NotModified org.assertj.core.api.AssertProvider<T> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThat(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.IterableAssert<ELEMENT> assertThat(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.IterableAssert<ELEMENT> assertThatIterable(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <E> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends E>, E, org.assertj.core.api.ObjectAssert<E>> assertThat(@Independent @NotModified java.util.Collection<? extends E> arg0) { return null; }
        @Independent
        static <E> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends E>, E, org.assertj.core.api.ObjectAssert<E>> assertThatCollection(@Independent @NotModified java.util.Collection<? extends E> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.ListAssert<ELEMENT> assertThat(@Independent @NotModified java.util.List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.ListAssert<ELEMENT> assertThatList(@Independent @NotModified java.util.List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ACTUAL extends java.lang.Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.FactoryBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0, org.assertj.core.api.AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends java.lang.Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.ClassBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified ACTUAL arg0, java.lang.Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends java.util.List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.FactoryBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified java.util.List<? extends ELEMENT> arg0, org.assertj.core.api.AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ELEMENT, ACTUAL extends java.util.List<? extends ELEMENT>, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.ClassBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified java.util.List<? extends ELEMENT> arg0, java.lang.Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static org.assertj.core.api.AbstractPathAssert<?> assertThat(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractPathAssert<?> assertThatPath(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        static <K, V> org.assertj.core.api.MapAssert<K, V> assertThat(@Independent @NotModified java.util.Map<K, V> arg0) { return null; }
        @Independent
        static <T extends java.lang.Comparable<? super T>> org.assertj.core.api.AbstractComparableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractUniversalComparableAssert<?, T> assertThatComparable(@Independent @NotModified java.lang.Comparable<T> arg0) { return null; }
        @Independent
        static <T extends org.assertj.core.api.AssertDelegateTarget> T assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified java.util.concurrent.CompletionStage<RESULT> arg0) { return null; }
    }

    //public class org.assertj.core.api.Java6Assertions
    class Java6Assertions$ {
        @Independent static org.assertj.core.api.AbstractBooleanArrayAssert<?> assertThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent static org.assertj.core.api.Boolean2DArrayAssert assertThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractByteArrayAssert<?> assertThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent static org.assertj.core.api.Byte2DArrayAssert assertThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractCharArrayAssert<?> assertThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent static org.assertj.core.api.Char2DArrayAssert assertThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractDoubleArrayAssert<?> assertThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent static org.assertj.core.api.Double2DArrayAssert assertThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractFloatArrayAssert<?> assertThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent static org.assertj.core.api.Float2DArrayAssert assertThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractIntArrayAssert<?> assertThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent static org.assertj.core.api.Int2DArrayAssert assertThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractLongArrayAssert<?> assertThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent static org.assertj.core.api.Long2DArrayAssert assertThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractShortArrayAssert<?> assertThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent static org.assertj.core.api.Short2DArrayAssert assertThat(@Independent @NotModified short[][] arg0) { return null; }
        static void fail(java.lang.String arg0, @NotModified java.lang.Throwable arg1) { }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.String arg1, @NotModified java.lang.Object... arg2) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatCode(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static java.lang.Throwable catchThrowable(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        static <THROWABLE extends java.lang.Throwable> THROWABLE catchThrowableOfType(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.Class<THROWABLE> arg1) { return null; }
        static <THROWABLE extends java.lang.Throwable> THROWABLE catchThrowableOfType(java.lang.Class<THROWABLE> arg0, @IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg1) { return null; }
        @Independent
        static org.assertj.core.api.AtomicBooleanAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicBoolean arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicIntegerAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicInteger arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicIntegerArrayAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerArray arg0) { return null; }
        @Independent
        static <OBJECT> org.assertj.core.api.AtomicIntegerFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicLongAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLong arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicLongArrayAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLongArray arg0) { return null; }
        @Independent
        static <OBJECT> org.assertj.core.api.AtomicLongFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicReferenceAssert<VALUE> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReference<VALUE> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.AtomicReferenceArrayAssert<ELEMENT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        static <FIELD, OBJECT> org.assertj.core.api.AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicMarkableReferenceAssert<VALUE> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicStampedReferenceAssert<VALUE> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBigDecimalAssert<?> assertThat(@Independent @NotModified java.math.BigDecimal arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBigIntegerAssert<?> assertThat(@Independent @NotModified java.math.BigInteger arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractUriAssert<?> assertThat(@Independent @NotModified java.net.URI arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractUrlAssert<?> assertThat(@Independent @NotModified java.net.URL arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBooleanAssert<?> assertThat(@Independent @NotModified java.lang.Boolean arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractByteAssert<?> assertThat(@Independent @NotModified java.lang.Byte arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharacterAssert<?> assertThat(@Independent @NotModified java.lang.Character arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractClassAssert<?> assertThat(@Independent @NotModified java.lang.Class<?> arg0) { return null; }
        @Independent
        static <T extends java.lang.Comparable<? super T>> org.assertj.core.api.AbstractComparableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractIterableAssert<?, java.lang.Iterable<? extends T>, T, org.assertj.core.api.ObjectAssert<T>> assertThat(@Independent @NotModified java.lang.Iterable<? extends T> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDoubleAssert<?> assertThat(@Independent @NotModified java.lang.Double arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractFileAssert<?> assertThat(@Independent @NotModified java.io.File arg0) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.AbstractFutureAssert<?, ? extends java.util.concurrent.Future<? extends RESULT>, RESULT> assertThat(@Independent @NotModified java.util.concurrent.Future<RESULT> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractFloatAssert<?> assertThat(@Independent @NotModified java.lang.Float arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractIntegerAssert<?> assertThat(@Independent @NotModified java.lang.Integer arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends T>, T, org.assertj.core.api.ObjectAssert<T>> assertThat(@Independent @NotModified java.util.Collection<? extends T> arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractListAssert<?, java.util.List<? extends T>, T, org.assertj.core.api.ObjectAssert<T>> assertThat(@Independent @NotModified java.util.List<? extends T> arg0) { return null; }
        @Independent
        static <ACTUAL extends java.lang.Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.FactoryBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0, org.assertj.core.api.AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends java.lang.Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.ClassBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified ACTUAL arg0, java.lang.Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends java.util.List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.FactoryBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified java.util.List<? extends ELEMENT> arg0, org.assertj.core.api.AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ELEMENT, ACTUAL extends java.util.List<? extends ELEMENT>, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.ClassBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified java.util.List<? extends ELEMENT> arg0, java.lang.Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLongAssert<?> assertThat(@Independent @NotModified java.lang.Long arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractObjectAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T extends org.assertj.core.api.AssertDelegateTarget> T assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> T assertThat(@Independent @NotModified org.assertj.core.api.AssertProvider<T> arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractObjectArrayAssert<?, T> assertThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.Object2DArrayAssert<T> assertThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        static <K, V> org.assertj.core.api.MapAssert<K, V> assertThat(@Independent @NotModified java.util.Map<K, V> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractShortAssert<?> assertThat(@Independent @NotModified java.lang.Short arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThat(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThatCharSequence(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThat(@Independent @NotModified java.lang.StringBuilder arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assertThat(@Independent @NotModified java.lang.StringBuffer arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDateAssert<?> assertThat(@Independent @NotModified java.util.Date arg0) { return null; }
        @Independent
        static <T extends java.lang.Throwable> org.assertj.core.api.AbstractThrowableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.AbstractObjectAssert<?, T> assertThatObject(@Independent @NotModified T arg0) { return null; }
    }

    //public class org.assertj.core.api.Java6BDDAssertions
    class Java6BDDAssertions$ {
        @Independent static org.assertj.core.api.AbstractBooleanArrayAssert<?> then(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent static org.assertj.core.api.Boolean2DArrayAssert then(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractByteArrayAssert<?> then(@Independent @NotModified byte[] arg0) { return null; }
        @Independent static org.assertj.core.api.Byte2DArrayAssert then(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractCharArrayAssert<?> then(@Independent @NotModified char[] arg0) { return null; }
        @Independent static org.assertj.core.api.Char2DArrayAssert then(@Independent @NotModified char[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractDoubleArrayAssert<?> then(@Independent @NotModified double[] arg0) { return null; }
        @Independent static org.assertj.core.api.Double2DArrayAssert then(@Independent @NotModified double[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractFloatArrayAssert<?> then(@Independent @NotModified float[] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractIntArrayAssert<?> then(@Independent @NotModified int[] arg0) { return null; }
        @Independent static org.assertj.core.api.Int2DArrayAssert then(@Independent @NotModified int[][] arg0) { return null; }
        @Independent static org.assertj.core.api.Float2DArrayAssert then(@Independent @NotModified float[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractLongArrayAssert<?> then(@Independent @NotModified long[] arg0) { return null; }
        @Independent static org.assertj.core.api.Long2DArrayAssert then(@Independent @NotModified long[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractShortArrayAssert<?> then(@Independent @NotModified short[] arg0) { return null; }
        @Independent static org.assertj.core.api.Short2DArrayAssert then(@Independent @NotModified short[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> thenThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> thenThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.String arg1, @NotModified java.lang.Object... arg2) { return null; }
        @Independent org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> thenCode(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicBooleanAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicBoolean arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicIntegerAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicInteger arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicIntegerArrayAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerArray arg0) { return null; }
        @Independent
        static <OBJECT> org.assertj.core.api.AtomicIntegerFieldUpdaterAssert<OBJECT> then(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicLongAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicLong arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicLongArrayAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicLongArray arg0) { return null; }
        @Independent
        static <OBJECT> org.assertj.core.api.AtomicLongFieldUpdaterAssert<OBJECT> then(@Independent @NotModified java.util.concurrent.atomic.AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicReferenceAssert<VALUE> then(@Independent @NotModified java.util.concurrent.atomic.AtomicReference<VALUE> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.AtomicReferenceArrayAssert<ELEMENT> then(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        static <FIELD, OBJECT> org.assertj.core.api.AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> then(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicMarkableReferenceAssert<VALUE> then(@Independent @NotModified java.util.concurrent.atomic.AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicStampedReferenceAssert<VALUE> then(@Independent @NotModified java.util.concurrent.atomic.AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBigDecimalAssert<?> then(@Independent @NotModified java.math.BigDecimal arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBigIntegerAssert<?> then(@Independent @NotModified java.math.BigInteger arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBooleanAssert<?> then(@Independent @NotModified java.lang.Boolean arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractByteAssert<?> then(@Independent @NotModified java.lang.Byte arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharacterAssert<?> then(@Independent @NotModified java.lang.Character arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractClassAssert<?> then(@Independent @NotModified java.lang.Class<?> arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends T>, T, org.assertj.core.api.ObjectAssert<T>> then(@Independent @NotModified java.util.Collection<? extends T> arg0) { return null; }
        @Independent
        static <T extends java.lang.Comparable<? super T>> org.assertj.core.api.AbstractComparableAssert<?, T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractIterableAssert<?, java.lang.Iterable<? extends T>, T, org.assertj.core.api.ObjectAssert<T>> then(@Independent @NotModified java.lang.Iterable<? extends T> arg0) { return null; }
        @Independent
        static <ACTUAL extends java.lang.Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.FactoryBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0, org.assertj.core.api.AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends java.lang.Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.ClassBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified ACTUAL arg0, java.lang.Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends java.util.List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.FactoryBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified java.util.List<? extends ELEMENT> arg0, org.assertj.core.api.AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ELEMENT, ACTUAL extends java.util.List<? extends ELEMENT>, ELEMENT_ASSERT extends org.assertj.core.api.AbstractAssert<ELEMENT_ASSERT, ELEMENT>> org.assertj.core.api.ClassBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified java.util.List<? extends ELEMENT> arg0, java.lang.Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDoubleAssert<?> then(@Independent @NotModified java.lang.Double arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractFileAssert<?> then(@Independent @NotModified java.io.File arg0) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.AbstractFutureAssert<?, ? extends java.util.concurrent.Future<? extends RESULT>, RESULT> then(@Independent @NotModified java.util.concurrent.Future<RESULT> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractFloatAssert<?> then(@Independent @NotModified java.lang.Float arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractIntegerAssert<?> then(@Independent @NotModified java.lang.Integer arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractListAssert<?, java.util.List<? extends T>, T, org.assertj.core.api.ObjectAssert<T>> then(@Independent @NotModified java.util.List<? extends T> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLongAssert<?> then(@Independent @NotModified java.lang.Long arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractObjectAssert<?, T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractObjectArrayAssert<?, T> then(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.Object2DArrayAssert<T> then(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        static <K, V> org.assertj.core.api.MapAssert<K, V> then(@Independent @NotModified java.util.Map<K, V> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractShortAssert<?> then(@Independent @NotModified java.lang.Short arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> then(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> thenCharSequence(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> then(@Independent @NotModified java.lang.StringBuilder arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> then(@Independent @NotModified java.lang.StringBuffer arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDateAssert<?> then(@Independent @NotModified java.util.Date arg0) { return null; }
        @Independent
        static <T extends java.lang.Throwable> org.assertj.core.api.AbstractThrowableAssert<?, T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractUriAssert<?> then(@Independent @NotModified java.net.URI arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractUrlAssert<?> then(@Independent @NotModified java.net.URL arg0) { return null; }
        @Independent
        static <T extends org.assertj.core.api.AssertDelegateTarget> T then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> T then(@Independent @NotModified org.assertj.core.api.AssertProvider<T> arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractObjectAssert<?, T> thenObject(@Independent @NotModified T arg0) { return null; }
    }

    //public interface org.assertj.core.api.StandardSoftAssertionsProvider
    class StandardSoftAssertionsProvider$ {
        @Independent
        org.assertj.core.api.PathAssert assertThat(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractPathAssert<?> assertThatPath(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.OptionalAssert<VALUE> assertThat(@Independent @NotModified java.util.Optional<VALUE> arg0) { return null; }
        @Independent
        org.assertj.core.api.OptionalDoubleAssert assertThat(@Independent @NotModified java.util.OptionalDouble arg0) { return null; }
        @Independent
        org.assertj.core.api.OptionalLongAssert assertThat(@Independent @NotModified java.util.OptionalLong arg0) { return null; }
        @Independent
        org.assertj.core.api.OptionalIntAssert assertThat(@Independent @NotModified java.util.OptionalInt arg0) { return null; }
        @Independent
        org.assertj.core.api.LocalDateAssert assertThat(@Independent @NotModified java.time.LocalDate arg0) { return null; }
        @Independent
        org.assertj.core.api.YearMonthAssert assertThat(@Independent @NotModified java.time.YearMonth arg0) { return null; }
        @Independent
        org.assertj.core.api.LocalDateTimeAssert assertThat(@Independent @NotModified java.time.LocalDateTime arg0) { return null; }
        @Independent
        org.assertj.core.api.ZonedDateTimeAssert assertThat(@Independent @NotModified java.time.ZonedDateTime arg0) { return null; }
        @Independent
        org.assertj.core.api.LocalTimeAssert assertThat(@Independent @NotModified java.time.LocalTime arg0) { return null; }
        @Independent
        org.assertj.core.api.OffsetTimeAssert assertThat(@Independent @NotModified java.time.OffsetTime arg0) { return null; }
        @Independent
        org.assertj.core.api.OffsetDateTimeAssert assertThat(@Independent @NotModified java.time.OffsetDateTime arg0) { return null; }
        @Independent
        org.assertj.core.api.InstantAssert assertThat(@Independent @NotModified java.time.Instant arg0) { return null; }
        @Independent
        org.assertj.core.api.DurationAssert assertThat(@Independent @NotModified java.time.Duration arg0) { return null; }
        @Independent
        org.assertj.core.api.PeriodAssert assertThat(@Independent @NotModified java.time.Period arg0) { return null; }
        @Independent
        <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified java.util.concurrent.CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified java.util.concurrent.CompletionStage<RESULT> arg0) { return null; }
        @Independent
        org.assertj.core.api.LongAdderAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.LongAdder arg0) { return null; }
        @Independent
        org.assertj.core.api.TemporalAssert assertThatTemporal(@Independent @NotModified java.time.temporal.Temporal arg0) { return null; }
    }

    //public interface org.assertj.core.api.Java6StandardSoftAssertionsProvider
    class Java6StandardSoftAssertionsProvider$ {
        @Independent org.assertj.core.api.BooleanArrayAssert assertThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent org.assertj.core.api.Boolean2DArrayAssert assertThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent org.assertj.core.api.ByteArrayAssert assertThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent org.assertj.core.api.Byte2DArrayAssert assertThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent org.assertj.core.api.CharArrayAssert assertThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent org.assertj.core.api.Char2DArrayAssert assertThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent org.assertj.core.api.DoubleArrayAssert assertThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent org.assertj.core.api.Double2DArrayAssert assertThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent org.assertj.core.api.FloatArrayAssert assertThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent org.assertj.core.api.Float2DArrayAssert assertThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent org.assertj.core.api.IntArrayAssert assertThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent org.assertj.core.api.Int2DArrayAssert assertThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent org.assertj.core.api.LongArrayAssert assertThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent org.assertj.core.api.Long2DArrayAssert assertThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent org.assertj.core.api.ShortArrayAssert assertThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent org.assertj.core.api.Short2DArrayAssert assertThat(@Independent @NotModified short[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.String arg1, @NotModified java.lang.Object... arg2) { return null; }
        @Independent org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assertThatCode(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        org.assertj.core.api.BigDecimalAssert assertThat(@Independent @NotModified java.math.BigDecimal arg0) { return null; }
        @Independent
        org.assertj.core.api.BigIntegerAssert assertThat(@Independent @NotModified java.math.BigInteger arg0) { return null; }
        @Independent
        org.assertj.core.api.BooleanAssert assertThat(@Independent @NotModified java.lang.Boolean arg0) { return null; }
        @Independent
        org.assertj.core.api.ByteAssert assertThat(@Independent @NotModified java.lang.Byte arg0) { return null; }
        @Independent
        org.assertj.core.api.CharacterAssert assertThat(@Independent @NotModified java.lang.Character arg0) { return null; }
        @Independent
        org.assertj.core.api.ClassAssert assertThat(@Independent @NotModified java.lang.Class<?> arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.CollectionAssert<T> assertThat(@Independent @NotModified java.util.Collection<? extends T> arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.CollectionAssert<T> assertThatCollection(@Independent @NotModified java.util.Collection<? extends T> arg0) { return null; }
        @Independent
        <T extends java.lang.Comparable<? super T>> org.assertj.core.api.AbstractComparableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.AbstractUniversalComparableAssert<?, T> assertThatComparable(@Independent @NotModified java.lang.Comparable<T> arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.IterableAssert<T> assertThat(@Independent @NotModified java.lang.Iterable<? extends T> arg0) { return null; }
        @Independent
        <ELEMENT> org.assertj.core.api.IterableAssert<ELEMENT> assertThatIterable(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        org.assertj.core.api.DoubleAssert assertThat(@Independent @NotModified java.lang.Double arg0) { return null; }
        @Independent
        org.assertj.core.api.FileAssert assertThat(@Independent @NotModified java.io.File arg0) { return null; }
        @Independent
        <RESULT> org.assertj.core.api.FutureAssert<RESULT> assertThat(@Independent @NotModified java.util.concurrent.Future<RESULT> arg0) { return null; }
        @Independent
        org.assertj.core.api.FloatAssert assertThat(@Independent @NotModified java.lang.Float arg0) { return null; }
        @Independent
        org.assertj.core.api.IntegerAssert assertThat(@Independent @NotModified java.lang.Integer arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ListAssert<T> assertThat(@Independent @NotModified java.util.List<? extends T> arg0) { return null; }
        @Independent
        <ELEMENT> org.assertj.core.api.ListAssert<ELEMENT> assertThatList(@Independent @NotModified java.util.List<? extends ELEMENT> arg0) { return null; }
        @Independent
        org.assertj.core.api.LongAssert assertThat(@Independent @NotModified java.lang.Long arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ObjectAssert<T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ObjectArrayAssert<T> assertThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.Object2DArrayAssert<T> assertThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        <K, V> org.assertj.core.api.MapAssert<K, V> assertThat(@Independent @NotModified java.util.Map<K, V> arg0) { return null; }
        @Independent
        org.assertj.core.api.ShortAssert assertThat(@Independent @NotModified java.lang.Short arg0) { return null; }
        @Independent
        org.assertj.core.api.CharSequenceAssert assertThat(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        org.assertj.core.api.CharSequenceAssert assertThatCharSequence(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        org.assertj.core.api.CharSequenceAssert assertThat(@Independent @NotModified java.lang.StringBuilder arg0) { return null; }
        @Independent
        org.assertj.core.api.CharSequenceAssert assertThat(@Independent @NotModified java.lang.StringBuffer arg0) { return null; }
        @Independent
        org.assertj.core.api.DateAssert assertThat(@Independent @NotModified java.util.Date arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicBooleanAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicBoolean arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicIntegerAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicInteger arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicIntegerArrayAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerArray arg0) { return null; }
        @Independent
        <OBJECT> org.assertj.core.api.AtomicIntegerFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicLongAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLong arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicLongArrayAssert assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLongArray arg0) { return null; }
        @Independent
        <OBJECT> org.assertj.core.api.AtomicLongFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.AtomicReferenceAssert<VALUE> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReference<VALUE> arg0) { return null; }
        @Independent
        <ELEMENT> org.assertj.core.api.AtomicReferenceArrayAssert<ELEMENT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        <FIELD, OBJECT> org.assertj.core.api.AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.AtomicMarkableReferenceAssert<VALUE> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.AtomicStampedReferenceAssert<VALUE> assertThat(@Independent @NotModified java.util.concurrent.atomic.AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        <T extends java.lang.Throwable> org.assertj.core.api.ThrowableAssert<T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ObjectAssert<T> assertThatObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        org.assertj.core.api.UriAssert assertThat(@Independent @NotModified java.net.URI arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractUrlAssert<?> assertThat(@Independent @NotModified java.net.URL arg0) { return null; }
        @Independent
        <T extends java.lang.Throwable> org.assertj.core.api.ThrowableTypeAssert<T> assertThatExceptionOfType(@Independent @NotModified java.lang.Class<T> arg0) { return null; }
    }

    //public interface org.assertj.core.api.BDDSoftAssertionsProvider
    class BDDSoftAssertionsProvider$ {
        @Independent
        org.assertj.core.api.PathAssert then(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractPathAssert<?> thenPath(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.OptionalAssert<VALUE> then(@Independent @NotModified java.util.Optional<VALUE> arg0) { return null; }
        @Independent
        org.assertj.core.api.OptionalDoubleAssert then(@Independent @NotModified java.util.OptionalDouble arg0) { return null; }
        @Independent
        org.assertj.core.api.OptionalIntAssert then(@Independent @NotModified java.util.OptionalInt arg0) { return null; }
        @Independent
        org.assertj.core.api.OptionalLongAssert then(@Independent @NotModified java.util.OptionalLong arg0) { return null; }
        @Independent
        org.assertj.core.api.TemporalAssert thenTemporal(@Independent @NotModified java.time.temporal.Temporal arg0) { return null; }
        @Independent
        org.assertj.core.api.LocalDateAssert then(@Independent @NotModified java.time.LocalDate arg0) { return null; }
        @Independent
        org.assertj.core.api.YearMonthAssert then(@Independent @NotModified java.time.YearMonth arg0) { return null; }
        @Independent
        org.assertj.core.api.LocalDateTimeAssert then(@Independent @NotModified java.time.LocalDateTime arg0) { return null; }
        @Independent
        org.assertj.core.api.ZonedDateTimeAssert then(@Independent @NotModified java.time.ZonedDateTime arg0) { return null; }
        @Independent
        org.assertj.core.api.LocalTimeAssert then(@Independent @NotModified java.time.LocalTime arg0) { return null; }
        @Independent
        org.assertj.core.api.OffsetTimeAssert then(@Independent @NotModified java.time.OffsetTime arg0) { return null; }
        @Independent
        org.assertj.core.api.OffsetDateTimeAssert then(@Independent @NotModified java.time.OffsetDateTime arg0) { return null; }
        @Independent
        org.assertj.core.api.InstantAssert then(@Independent @NotModified java.time.Instant arg0) { return null; }
        @Independent
        org.assertj.core.api.DurationAssert then(@Independent @NotModified java.time.Duration arg0) { return null; }
        @Independent
        org.assertj.core.api.PeriodAssert then(@Independent @NotModified java.time.Period arg0) { return null; }
        @Independent
        <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> then(@Independent @NotModified java.util.concurrent.CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> then(@Independent @NotModified java.util.concurrent.CompletionStage<RESULT> arg0) { return null; }
        @Independent
        org.assertj.core.api.LongAdderAssert then(@Independent @NotModified java.util.concurrent.atomic.LongAdder arg0) { return null; }
    }

    //public interface org.assertj.core.api.Java6BDDSoftAssertionsProvider
    class Java6BDDSoftAssertionsProvider$ {
        @Independent org.assertj.core.api.BooleanArrayAssert then(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent org.assertj.core.api.Boolean2DArrayAssert then(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent org.assertj.core.api.ByteArrayAssert then(@Independent @NotModified byte[] arg0) { return null; }
        @Independent org.assertj.core.api.Byte2DArrayAssert then(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent org.assertj.core.api.CharArrayAssert then(@Independent @NotModified char[] arg0) { return null; }
        @Independent org.assertj.core.api.Char2DArrayAssert then(@Independent @NotModified char[][] arg0) { return null; }
        @Independent org.assertj.core.api.DoubleArrayAssert then(@Independent @NotModified double[] arg0) { return null; }
        @Independent org.assertj.core.api.Double2DArrayAssert then(@Independent @NotModified double[][] arg0) { return null; }
        @Independent org.assertj.core.api.FloatArrayAssert then(@Independent @NotModified float[] arg0) { return null; }
        @Independent org.assertj.core.api.Float2DArrayAssert then(@Independent @NotModified float[][] arg0) { return null; }
        @Independent org.assertj.core.api.IntArrayAssert then(@Independent @NotModified int[] arg0) { return null; }
        @Independent org.assertj.core.api.Int2DArrayAssert then(@Independent @NotModified int[][] arg0) { return null; }
        @Independent org.assertj.core.api.LongArrayAssert then(@Independent @NotModified long[] arg0) { return null; }
        @Independent org.assertj.core.api.Long2DArrayAssert then(@Independent @NotModified long[][] arg0) { return null; }
        @Independent org.assertj.core.api.ShortArrayAssert then(@Independent @NotModified short[] arg0) { return null; }
        @Independent org.assertj.core.api.Short2DArrayAssert then(@Independent @NotModified short[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> thenThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> thenThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0, java.lang.String arg1, @NotModified java.lang.Object... arg2) { return null; }
        @Independent org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> thenCode(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        org.assertj.core.api.BigDecimalAssert then(@Independent @NotModified java.math.BigDecimal arg0) { return null; }
        @Independent
        org.assertj.core.api.BigIntegerAssert then(@Independent @NotModified java.math.BigInteger arg0) { return null; }
        @Independent
        org.assertj.core.api.BooleanAssert then(@Independent @NotModified java.lang.Boolean arg0) { return null; }
        @Independent
        org.assertj.core.api.ByteAssert then(@Independent @NotModified java.lang.Byte arg0) { return null; }
        @Independent
        org.assertj.core.api.CharacterAssert then(@Independent @NotModified java.lang.Character arg0) { return null; }
        @Independent
        org.assertj.core.api.ClassAssert then(@Independent @NotModified java.lang.Class<?> arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.CollectionAssert<T> then(@Independent @NotModified java.util.Collection<? extends T> arg0) { return null; }
        @Independent
        <E> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends E>, E, org.assertj.core.api.ObjectAssert<E>> thenCollection(@Independent @NotModified java.util.Collection<? extends E> arg0) { return null; }
        @Independent
        <T extends java.lang.Comparable<? super T>> org.assertj.core.api.AbstractComparableAssert<?, T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.AbstractUniversalComparableAssert<?, T> thenComparable(@Independent @NotModified java.lang.Comparable<T> arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.IterableAssert<T> then(@Independent @NotModified java.lang.Iterable<? extends T> arg0) { return null; }
        @Independent
        <ELEMENT> org.assertj.core.api.IterableAssert<ELEMENT> thenIterable(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        org.assertj.core.api.DoubleAssert then(@Independent @NotModified java.lang.Double arg0) { return null; }
        @Independent
        org.assertj.core.api.FileAssert then(@Independent @NotModified java.io.File arg0) { return null; }
        @Independent
        <RESULT> org.assertj.core.api.FutureAssert<RESULT> then(@Independent @NotModified java.util.concurrent.Future<RESULT> arg0) { return null; }
        @Independent
        org.assertj.core.api.FloatAssert then(@Independent @NotModified java.lang.Float arg0) { return null; }
        @Independent
        org.assertj.core.api.IntegerAssert then(@Independent @NotModified java.lang.Integer arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ListAssert<T> then(@Independent @NotModified java.util.List<? extends T> arg0) { return null; }
        @Independent
        <ELEMENT> org.assertj.core.api.ListAssert<ELEMENT> thenList(@Independent @NotModified java.util.List<? extends ELEMENT> arg0) { return null; }
        @Independent
        org.assertj.core.api.LongAssert then(@Independent @NotModified java.lang.Long arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ObjectAssert<T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ObjectArrayAssert<T> then(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.Object2DArrayAssert<T> then(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        <K, V> org.assertj.core.api.MapAssert<K, V> then(@Independent @NotModified java.util.Map<K, V> arg0) { return null; }
        @Independent
        org.assertj.core.api.ShortAssert then(@Independent @NotModified java.lang.Short arg0) { return null; }
        @Independent
        org.assertj.core.api.CharSequenceAssert then(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        org.assertj.core.api.CharSequenceAssert thenCharSequence(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        org.assertj.core.api.CharSequenceAssert then(@Independent @NotModified java.lang.StringBuilder arg0) { return null; }
        @Independent
        org.assertj.core.api.CharSequenceAssert then(@Independent @NotModified java.lang.StringBuffer arg0) { return null; }
        @Independent
        org.assertj.core.api.DateAssert then(@Independent @NotModified java.util.Date arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicBooleanAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicBoolean arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicIntegerAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicInteger arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicIntegerArrayAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerArray arg0) { return null; }
        @Independent
        <OBJECT> org.assertj.core.api.AtomicIntegerFieldUpdaterAssert<OBJECT> then(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicLongAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicLong arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicLongArrayAssert then(@Independent @NotModified java.util.concurrent.atomic.AtomicLongArray arg0) { return null; }
        @Independent
        <OBJECT> org.assertj.core.api.AtomicLongFieldUpdaterAssert<OBJECT> then(@Independent @NotModified java.util.concurrent.atomic.AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.AtomicReferenceAssert<VALUE> then(@Independent @NotModified java.util.concurrent.atomic.AtomicReference<VALUE> arg0) { return null; }
        @Independent
        <ELEMENT> org.assertj.core.api.AtomicReferenceArrayAssert<ELEMENT> then(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        <FIELD, OBJECT> org.assertj.core.api.AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> then(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.AtomicMarkableReferenceAssert<VALUE> then(@Independent @NotModified java.util.concurrent.atomic.AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.AtomicStampedReferenceAssert<VALUE> then(@Independent @NotModified java.util.concurrent.atomic.AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        <T extends java.lang.Throwable> org.assertj.core.api.ThrowableAssert<T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ObjectAssert<T> thenObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        org.assertj.core.api.UriAssert then(@Independent @NotModified java.net.URI arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractUrlAssert<?> then(@Independent @NotModified java.net.URL arg0) { return null; }
        @Independent
        <T extends java.lang.Throwable> org.assertj.core.api.ThrowableTypeAssert<T> thenExceptionOfType(@Independent @NotModified java.lang.Class<T> arg0) { return null; }
    }

    //public class org.assertj.core.api.Assumptions
    class Assumptions$ {
        @Independent static org.assertj.core.api.AbstractBooleanArrayAssert<?> assumeThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent static org.assertj.core.api.Boolean2DArrayAssert assumeThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractByteArrayAssert<?> assumeThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent static org.assertj.core.api.Byte2DArrayAssert assumeThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractCharArrayAssert<?> assumeThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent static org.assertj.core.api.Char2DArrayAssert assumeThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractShortArrayAssert<?> assumeThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent static org.assertj.core.api.Short2DArrayAssert assumeThat(@Independent @NotModified short[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractIntArrayAssert<?> assumeThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent static org.assertj.core.api.Int2DArrayAssert assumeThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractLongArrayAssert<?> assumeThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent static org.assertj.core.api.Long2DArrayAssert assumeThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractFloatArrayAssert<?> assumeThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent static org.assertj.core.api.Float2DArrayAssert assumeThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractDoubleArrayAssert<?> assumeThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent static org.assertj.core.api.Double2DArrayAssert assumeThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assumeThatThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent static org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assumeThatCode(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ObjectAssert<T> assumeThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBigDecimalAssert<?> assumeThat(@Independent @NotModified java.math.BigDecimal arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBigIntegerAssert<?> assumeThat(@Independent @NotModified java.math.BigInteger arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractUriAssert<?> assumeThat(@Independent @NotModified java.net.URI arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractUrlAssert<?> assumeThat(@Independent @NotModified java.net.URL arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractBooleanAssert<?> assumeThat(@Independent @NotModified java.lang.Boolean arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractByteAssert<?> assumeThat(@Independent @NotModified java.lang.Byte arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharacterAssert<?> assumeThat(@Independent @NotModified java.lang.Character arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assumeThat(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assumeThatCharSequence(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assumeThat(@Independent @NotModified java.lang.StringBuilder arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assumeThat(@Independent @NotModified java.lang.StringBuffer arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractShortAssert<?> assumeThat(@Independent @NotModified java.lang.Short arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractIntegerAssert<?> assumeThat(@Independent @NotModified java.lang.Integer arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLongAssert<?> assumeThat(@Independent @NotModified java.lang.Long arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractFloatAssert<?> assumeThat(@Independent @NotModified java.lang.Float arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDoubleAssert<?> assumeThat(@Independent @NotModified java.lang.Double arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicBooleanAssert assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicBoolean arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicIntegerAssert assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicInteger arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicIntegerArrayAssert assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerArray arg0) { return null; }
        @Independent
        static <OBJECT> org.assertj.core.api.AtomicIntegerFieldUpdaterAssert<OBJECT> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static org.assertj.core.api.LongAdderAssert assumeThat(@Independent @NotModified java.util.concurrent.atomic.LongAdder arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicLongAssert assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLong arg0) { return null; }
        @Independent
        static org.assertj.core.api.AtomicLongArrayAssert assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLongArray arg0) { return null; }
        @Independent
        static <OBJECT> org.assertj.core.api.AtomicLongFieldUpdaterAssert<OBJECT> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicReferenceAssert<VALUE> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReference<VALUE> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.AtomicReferenceArrayAssert<ELEMENT> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        static <FIELD, OBJECT> org.assertj.core.api.AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicMarkableReferenceAssert<VALUE> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.AtomicStampedReferenceAssert<VALUE> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        static org.assertj.core.api.ClassAssert assumeThat(@Independent @NotModified java.lang.Class<?> arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDateAssert<?> assumeThat(@Independent @NotModified java.util.Date arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractFileAssert<?> assumeThat(@Independent @NotModified java.io.File arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractPathAssert<?> assumeThat(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractPathAssert<?> assumeThatPath(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.AbstractFutureAssert<?, ? extends java.util.concurrent.Future<? extends RESULT>, RESULT> assumeThat(@Independent @NotModified java.util.concurrent.Future<RESULT> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.IterableAssert<ELEMENT> assumeThat(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.IterableAssert<ELEMENT> assumeThatIterable(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <E> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends E>, E, org.assertj.core.api.ObjectAssert<E>> assumeThat(@Independent @NotModified java.util.Collection<? extends E> arg0) { return null; }
        @Independent
        static <E> org.assertj.core.api.AbstractCollectionAssert<?, java.util.Collection<? extends E>, E, org.assertj.core.api.ObjectAssert<E>> assumeThatCollection(@Independent @NotModified java.util.Collection<? extends E> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.FactoryBasedNavigableListAssert<org.assertj.core.api.ListAssert<ELEMENT>, java.util.List<? extends ELEMENT>, ELEMENT, org.assertj.core.api.ObjectAssert<ELEMENT>> assumeThat(@Independent @NotModified java.util.List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ELEMENT> org.assertj.core.api.FactoryBasedNavigableListAssert<org.assertj.core.api.ListAssert<ELEMENT>, java.util.List<? extends ELEMENT>, ELEMENT, org.assertj.core.api.ObjectAssert<ELEMENT>> assumeThatList(@Independent @NotModified java.util.List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ObjectArrayAssert<T> assumeThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.Object2DArrayAssert<T> assumeThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        static <K, V> org.assertj.core.api.MapAssert<K, V> assumeThat(@Independent @NotModified java.util.Map<K, V> arg0) { return null; }
        @Independent
        static <T extends java.lang.Comparable<? super T>> org.assertj.core.api.AbstractComparableAssert<?, T> assumeThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.AbstractUniversalComparableAssert<?, T> assumeThatComparable(@Independent @NotModified java.lang.Comparable<T> arg0) { return null; }
        @Independent
        static <T extends java.lang.Throwable> org.assertj.core.api.AbstractThrowableAssert<?, T> assumeThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T extends java.lang.Throwable> org.assertj.core.api.ThrowableTypeAssert<T> assumeThatExceptionOfType(@Independent @NotModified java.lang.Class<? extends T> arg0) { return null; }
        @Independent
        static <T> org.assertj.core.api.ObjectAssert<T> assumeThatObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> assumeThat(@Independent @NotModified java.util.concurrent.CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        static <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> assumeThat(@Independent @NotModified java.util.concurrent.CompletionStage<RESULT> arg0) { return null; }
        @Independent
        static <VALUE> org.assertj.core.api.OptionalAssert<VALUE> assumeThat(@Independent @NotModified java.util.Optional<VALUE> arg0) { return null; }
        @Independent
        static org.assertj.core.api.OptionalDoubleAssert assumeThat(@Independent @NotModified java.util.OptionalDouble arg0) { return null; }
        @Independent
        static org.assertj.core.api.OptionalIntAssert assumeThat(@Independent @NotModified java.util.OptionalInt arg0) { return null; }
        @Independent
        static org.assertj.core.api.OptionalLongAssert assumeThat(@Independent @NotModified java.util.OptionalLong arg0) { return null; }
        @Independent
        static org.assertj.core.api.TemporalAssert assumeThatTemporal(@Independent @NotModified java.time.temporal.Temporal arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractZonedDateTimeAssert<?> assumeThat(@Independent @NotModified java.time.ZonedDateTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLocalDateTimeAssert<?> assumeThat(@Independent @NotModified java.time.LocalDateTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractOffsetDateTimeAssert<?> assumeThat(@Independent @NotModified java.time.OffsetDateTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractOffsetTimeAssert<?> assumeThat(@Independent @NotModified java.time.OffsetTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLocalTimeAssert<?> assumeThat(@Independent @NotModified java.time.LocalTime arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractLocalDateAssert<?> assumeThat(@Independent @NotModified java.time.LocalDate arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractYearMonthAssert<?> assumeThat(@Independent @NotModified java.time.YearMonth arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractInstantAssert<?> assumeThat(@Independent @NotModified java.time.Instant arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractDurationAssert<?> assumeThat(@Independent @NotModified java.time.Duration arg0) { return null; }
        @Independent
        static org.assertj.core.api.AbstractPeriodAssert<?> assumeThat(@Independent @NotModified java.time.Period arg0) { return null; }
    }

    //public interface org.assertj.core.api.WithAssumptions
    class WithAssumptions$ {
        @Independent org.assertj.core.api.AbstractLongArrayAssert<?> assumeThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent org.assertj.core.api.Long2DArrayAssert assumeThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractShortArrayAssert<?> assumeThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent org.assertj.core.api.Short2DArrayAssert assumeThat(@Independent @NotModified short[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractCharArrayAssert<?> assumeThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent org.assertj.core.api.Char2DArrayAssert assumeThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractBooleanArrayAssert<?> assumeThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent org.assertj.core.api.Boolean2DArrayAssert assumeThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractByteArrayAssert<?> assumeThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent org.assertj.core.api.Byte2DArrayAssert assumeThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractIntArrayAssert<?> assumeThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent org.assertj.core.api.Int2DArrayAssert assumeThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent org.assertj.core.api.Float2DArrayAssert assumeThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractFloatArrayAssert<?> assumeThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractDoubleArrayAssert<?> assumeThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent org.assertj.core.api.Double2DArrayAssert assumeThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assumeThatThrownBy(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent org.assertj.core.api.AbstractThrowableAssert<?, ? extends java.lang.Throwable> assumeThatCode(@IgnoreModifications org.assertj.core.api.ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ObjectArrayAssert<T> assumeThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.Object2DArrayAssert<T> assumeThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        <K, V> org.assertj.core.api.MapAssert<K, V> assumeThat(@Independent @NotModified java.util.Map<K, V> arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractLongAssert<?> assumeThat(@Independent @NotModified java.lang.Long arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ObjectAssert<T> assumeThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractDateAssert<?> assumeThat(@Independent @NotModified java.util.Date arg0) { return null; }
        @Independent
        <T extends java.lang.Throwable> org.assertj.core.api.AbstractThrowableAssert<?, T> assumeThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractBigDecimalAssert<?> assumeThat(@Independent @NotModified java.math.BigDecimal arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractBigIntegerAssert<?> assumeThat(@Independent @NotModified java.math.BigInteger arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicBooleanAssert assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicBoolean arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicIntegerAssert assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicInteger arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicIntegerArrayAssert assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerArray arg0) { return null; }
        @Independent
        <OBJECT> org.assertj.core.api.AtomicIntegerFieldUpdaterAssert<OBJECT> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        org.assertj.core.api.LongAdderAssert assumeThat(@Independent @NotModified java.util.concurrent.atomic.LongAdder arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicLongAssert assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLong arg0) { return null; }
        @Independent
        org.assertj.core.api.AtomicLongArrayAssert assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLongArray arg0) { return null; }
        @Independent
        <OBJECT> org.assertj.core.api.AtomicLongFieldUpdaterAssert<OBJECT> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.AtomicReferenceAssert<VALUE> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReference<VALUE> arg0) { return null; }
        @Independent
        <ELEMENT> org.assertj.core.api.AtomicReferenceArrayAssert<ELEMENT> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        <FIELD, OBJECT> org.assertj.core.api.AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.AtomicMarkableReferenceAssert<VALUE> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.AtomicStampedReferenceAssert<VALUE> assumeThat(@Independent @NotModified java.util.concurrent.atomic.AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assumeThat(@Independent @NotModified java.lang.CharSequence arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assumeThat(@Independent @NotModified java.lang.StringBuilder arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractCharSequenceAssert<?, ? extends java.lang.CharSequence> assumeThat(@Independent @NotModified java.lang.StringBuffer arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractShortAssert<?> assumeThat(@Independent @NotModified java.lang.Short arg0) { return null; }
        @Independent
        org.assertj.core.api.ClassAssert assumeThat(@Independent @NotModified java.lang.Class<?> arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractCharacterAssert<?> assumeThat(@Independent @NotModified java.lang.Character arg0) { return null; }
        @Independent
        <T extends java.lang.Comparable<? super T>> org.assertj.core.api.AbstractComparableAssert<?, T> assumeThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <ELEMENT> org.assertj.core.api.IterableAssert<ELEMENT> assumeThat(@Independent @NotModified java.lang.Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractBooleanAssert<?> assumeThat(@Independent @NotModified java.lang.Boolean arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractByteAssert<?> assumeThat(@Independent @NotModified java.lang.Byte arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractFileAssert<?> assumeThat(@Independent @NotModified java.io.File arg0) { return null; }
        @Independent
        <RESULT> org.assertj.core.api.AbstractFutureAssert<?, ? extends java.util.concurrent.Future<? extends RESULT>, RESULT> assumeThat(@Independent @NotModified java.util.concurrent.Future<RESULT> arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractPathAssert<?> assumeThat(@Independent @NotModified java.nio.file.Path arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractFloatAssert<?> assumeThat(@Independent @NotModified java.lang.Float arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractIntegerAssert<?> assumeThat(@Independent @NotModified java.lang.Integer arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractDoubleAssert<?> assumeThat(@Independent @NotModified java.lang.Double arg0) { return null; }
        @Independent
        <ELEMENT> org.assertj.core.api.FactoryBasedNavigableListAssert<org.assertj.core.api.ListAssert<ELEMENT>, java.util.List<? extends ELEMENT>, ELEMENT, org.assertj.core.api.ObjectAssert<ELEMENT>> assumeThat(@Independent @NotModified java.util.List<? extends ELEMENT> arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractZonedDateTimeAssert<?> assumeThat(@Independent @NotModified java.time.ZonedDateTime arg0) { return null; }
        @Independent
        <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> assumeThat(@Independent @NotModified java.util.concurrent.CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        <RESULT> org.assertj.core.api.CompletableFutureAssert<RESULT> assumeThat(@Independent @NotModified java.util.concurrent.CompletionStage<RESULT> arg0) { return null; }
        @Independent
        <VALUE> org.assertj.core.api.OptionalAssert<VALUE> assumeThat(@Independent @NotModified java.util.Optional<VALUE> arg0) { return null; }
        @Independent
        org.assertj.core.api.OptionalDoubleAssert assumeThat(@Independent @NotModified java.util.OptionalDouble arg0) { return null; }
        @Independent
        org.assertj.core.api.OptionalIntAssert assumeThat(@Independent @NotModified java.util.OptionalInt arg0) { return null; }
        @Independent
        org.assertj.core.api.OptionalLongAssert assumeThat(@Independent @NotModified java.util.OptionalLong arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractLocalDateTimeAssert<?> assumeThat(@Independent @NotModified java.time.LocalDateTime arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractLocalDateAssert<?> assumeThat(@Independent @NotModified java.time.LocalDate arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractYearMonthAssert<?> assumeThat(@Independent @NotModified java.time.YearMonth arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractLocalTimeAssert<?> assumeThat(@Independent @NotModified java.time.LocalTime arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractInstantAssert<?> assumeThat(@Independent @NotModified java.time.Instant arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractDurationAssert<?> assumeThat(@Independent @NotModified java.time.Duration arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractPeriodAssert<?> assumeThat(@Independent @NotModified java.time.Period arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractOffsetTimeAssert<?> assumeThat(@Independent @NotModified java.time.OffsetTime arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractOffsetDateTimeAssert<?> assumeThat(@Independent @NotModified java.time.OffsetDateTime arg0) { return null; }
        @Independent
        <T> org.assertj.core.api.ObjectAssert<T> assumeThatObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractUrlAssert<?> assumeThat(@Independent @NotModified java.net.URL arg0) { return null; }
        @Independent
        org.assertj.core.api.AbstractUriAssert<?> assumeThat(@Independent @NotModified java.net.URI arg0) { return null; }
    }

    // the assert types: no method modifies its arguments, the expected values (see the class comment)
    @Container
    class Abstract2DArrayAssert$ { }
    // not named *Assert, but the chain of assertThatExceptionOfType(..).isThrownBy(..): withMessage(fmt, args...),
    // withMessageContainingAll(values...) read their arguments (2026-10-13, the library-call census)
    @Container
    class ThrowableAssertAlternative$ { }
    @Container
    class AbstractArrayAssert$ { }
    @Container
    class AbstractAssert$ { }
    @Container
    class AbstractAtomicFieldUpdaterAssert$ { }
    @Container
    class AbstractAtomicReferenceAssert$ { }
    @Container
    class AbstractBigDecimalAssert$ { }
    @Container
    class AbstractBigDecimalScaleAssert$ { }
    @Container
    class AbstractBigIntegerAssert$ { }
    @Container
    class AbstractBooleanArrayAssert$ { }
    @Container
    class AbstractBooleanAssert$ { }
    @Container
    class AbstractByteArrayAssert$ { }
    @Container
    class AbstractByteAssert$ { }
    @Container
    class AbstractCharArrayAssert$ { }
    @Container
    class AbstractCharSequenceAssert$ { }
    @Container
    class AbstractCharacterAssert$ { }
    @Container
    class AbstractClassAssert$ { }
    @Container
    class AbstractCollectionAssert$ { }
    @Container
    class AbstractComparableAssert$ { }
    @Container
    class AbstractCompletableFutureAssert$ { }
    @Container
    class AbstractDateAssert$ { }
    @Container
    class AbstractDoubleArrayAssert$ { }
    @Container
    class AbstractDoubleAssert$ { }
    @Container
    class AbstractDurationAssert$ { }
    @Container
    class AbstractEnumerableAssert$ { }
    @Container
    class AbstractFileAssert$ { }
    @Container
    class AbstractFileSizeAssert$ { }
    @Container
    class AbstractFloatArrayAssert$ { }
    @Container
    class AbstractFloatAssert$ { }
    @Container
    class AbstractFutureAssert$ { }
    @Container
    class AbstractInputStreamAssert$<SELF extends org.assertj.core.api.AbstractInputStreamAssert<SELF, ACTUAL>, ACTUAL extends java.io.InputStream> {
        SELF hasContentEqualTo(@Modified java.io.InputStream arg0) { return null; }
        SELF hasSameContentAs(@Modified java.io.InputStream arg0) { return null; }
    }
    @Container
    class AbstractInstantAssert$ { }
    @Container
    class AbstractIntArrayAssert$ { }
    @Container
    class AbstractIntegerAssert$ { }
    @Container
    class AbstractIterableAssert$ { }
    @Container
    class AbstractIterableSizeAssert$ { }
    @Container
    class AbstractIteratorAssert$ { }
    @Container
    class AbstractListAssert$ { }
    @Container
    class AbstractLocalDateAssert$ { }
    @Container
    class AbstractLocalDateTimeAssert$ { }
    @Container
    class AbstractLocalTimeAssert$ { }
    @Container
    class AbstractLongAdderAssert$ { }
    @Container
    class AbstractLongArrayAssert$ { }
    @Container
    class AbstractLongAssert$ { }
    @Container
    class AbstractMapAssert$ { }
    @Container
    class AbstractMapSizeAssert$ { }
    @Container
    class AbstractMatcherAssert$ { }
    @Container
    class AbstractObjectArrayAssert$ { }
    @Container
    class AbstractObjectAssert$ { }
    @Container
    class AbstractOffsetDateTimeAssert$ { }
    @Container
    class AbstractOffsetTimeAssert$ { }
    @Container
    class AbstractOptionalAssert$ { }
    @Container
    class AbstractOptionalDoubleAssert$ { }
    @Container
    class AbstractOptionalIntAssert$ { }
    @Container
    class AbstractOptionalLongAssert$ { }
    @Container
    class AbstractPathAssert$ { }
    @Container
    class AbstractPeriodAssert$ { }
    @Container
    class AbstractPredicateAssert$ { }
    @Container
    class AbstractPredicateLikeAssert$ { }
    @Container
    class AbstractShortArrayAssert$ { }
    @Container
    class AbstractShortAssert$ { }
    @Container
    class AbstractSpliteratorAssert$ { }
    @Container
    class AbstractStringAssert$ { }
    @Container
    class AbstractTemporalAssert$ { }
    @Container
    class AbstractThrowableAssert$ { }
    @Container
    class AbstractUniversalComparableAssert$ { }
    @Container
    class AbstractUriAssert$ { }
    @Container
    class AbstractUrlAssert$ { }
    @Container
    class AbstractYearMonthAssert$ { }
    @Container
    class AbstractZonedDateTimeAssert$ { }
    @Container
    class Array2DAssert$ { }
    @Container
    class ArraySortedAssert$ { }
    @Container
    class Assert$ { }
    @Container
    class AtomicBooleanAssert$ { }
    @Container
    class AtomicIntegerArrayAssert$ { }
    @Container
    class AtomicIntegerAssert$ { }
    @Container
    class AtomicIntegerFieldUpdaterAssert$ { }
    @Container
    class AtomicLongArrayAssert$ { }
    @Container
    class AtomicLongAssert$ { }
    @Container
    class AtomicLongFieldUpdaterAssert$ { }
    @Container
    class AtomicMarkableReferenceAssert$ { }
    @Container
    class AtomicReferenceArrayAssert$ { }
    @Container
    class AtomicReferenceAssert$ { }
    @Container
    class AtomicReferenceFieldUpdaterAssert$ { }
    @Container
    class AtomicStampedReferenceAssert$ { }
    @Container
    class BigDecimalAssert$ { }
    @Container
    class BigDecimalScaleAssert$ { }
    @Container
    class BigIntegerAssert$ { }
    @Container
    class Boolean2DArrayAssert$ { }
    @Container
    class BooleanArrayAssert$ { }
    @Container
    class BooleanAssert$ { }
    @Container
    class Byte2DArrayAssert$ { }
    @Container
    class ByteArrayAssert$ { }
    @Container
    class ByteAssert$ { }
    @Container
    class Char2DArrayAssert$ { }
    @Container
    class CharArrayAssert$ { }
    @Container
    class CharSequenceAssert$ { }
    @Container
    class CharacterAssert$ { }
    @Container
    class ClassAssert$ { }
    @Container
    class ClassBasedNavigableIterableAssert$ { }
    @Container
    class ClassBasedNavigableListAssert$ { }
    @Container
    class CollectionAssert$ { }
    @Container
    class ComparableAssert$ { }
    @Container
    class CompletableFutureAssert$ { }
    @Container
    class DateAssert$ { }
    @Container
    class Descriptable$ { }
    @Container
    class Double2DArrayAssert$ { }
    @Container
    class DoubleArrayAssert$ { }
    @Container
    class DoubleAssert$ { }
    @Container
    class DoublePredicateAssert$ { }
    @Container
    class DurationAssert$ { }
    @Container
    class EnumerableAssert$ { }
    @Container
    class ExtensionPoints$ { }
    @Container
    class FactoryBasedNavigableIterableAssert$ { }
    @Container
    class FactoryBasedNavigableListAssert$ { }
    @Container
    class FileAssert$ { }
    @Container
    class FileSizeAssert$ { }
    @Container
    class Float2DArrayAssert$ { }
    @Container
    class FloatArrayAssert$ { }
    @Container
    class FloatAssert$ { }
    @Container
    class FloatingPointNumberAssert$ { }
    @Container
    class FutureAssert$ { }
    @Container
    class GenericComparableAssert$ { }
    @Container
    class IndexedObjectEnumerableAssert$ { }
    @Container
    class InputStreamAssert$ {
        InputStreamAssert$(@Modified java.io.InputStream arg0) { }
    }
    @Container
    class InstantAssert$ { }
    @Container
    class Int2DArrayAssert$ { }
    @Container
    class IntArrayAssert$ { }
    @Container
    class IntPredicateAssert$ { }
    @Container
    class IntegerAssert$ { }
    @Container
    class IterableAssert$ { }
    @Container
    class IterableSizeAssert$ { }
    @Container
    class IteratorAssert$<ELEMENT> {
        static <ELEMENT> org.assertj.core.api.IteratorAssert<ELEMENT> assertThatIterator(@Modified java.util.Iterator<? extends ELEMENT> arg0) { return null; }
        IteratorAssert$(@Modified java.util.Iterator<? extends ELEMENT> arg0) { }
    }
    @Container
    class ListAssert$<ELEMENT> {
        static <ELEMENT> org.assertj.core.api.ListAssert<ELEMENT> assertThatStream(@Modified java.util.stream.Stream<? extends ELEMENT> arg0) { return null; }
        static org.assertj.core.api.ListAssert<java.lang.Double> assertThatDoubleStream(@Modified java.util.stream.DoubleStream arg0) { return null; }
        static org.assertj.core.api.ListAssert<java.lang.Long> assertThatLongStream(@Modified java.util.stream.LongStream arg0) { return null; }
        static org.assertj.core.api.ListAssert<java.lang.Integer> assertThatIntStream(@Modified java.util.stream.IntStream arg0) { return null; }
        ListAssert$(@Modified java.util.stream.Stream<? extends ELEMENT> arg0) { }
        ListAssert$(@Modified java.util.stream.IntStream arg0) { }
        ListAssert$(@Modified java.util.stream.LongStream arg0) { }
        ListAssert$(@Modified java.util.stream.DoubleStream arg0) { }
    }
    @Container
    class LocalDateAssert$ { }
    @Container
    class LocalDateTimeAssert$ { }
    @Container
    class LocalTimeAssert$ { }
    @Container
    class Long2DArrayAssert$ { }
    @Container
    class LongAdderAssert$ { }
    @Container
    class LongArrayAssert$ { }
    @Container
    class LongAssert$ { }
    @Container
    class LongPredicateAssert$ { }
    @Container
    class MapAssert$ { }
    @Container
    class MapSizeAssert$ { }
    @Container
    class MatcherAssert$ { }
    @Container
    class NotThrownAssert$ { }
    @Container
    class NumberAssert$ { }
    @Container
    class Object2DArrayAssert$ { }
    @Container
    class ObjectArrayAssert$ { }
    @Container
    class ObjectAssert$ { }
    @Container
    class ObjectEnumerableAssert$ { }
    @Container
    class OffsetDateTimeAssert$ { }
    @Container
    class OffsetTimeAssert$ { }
    @Container
    class OptionalAssert$ { }
    @Container
    class OptionalDoubleAssert$ { }
    @Container
    class OptionalIntAssert$ { }
    @Container
    class OptionalLongAssert$ { }
    @Container
    class PathAssert$ { }
    @Container
    class PeriodAssert$ { }
    @Container
    class PredicateAssert$ { }
    @Container
    class RecursiveAssertionAssert$ { }
    @Container
    class RecursiveComparisonAssert$ { }
    @Container
    class Short2DArrayAssert$ { }
    @Container
    class ShortArrayAssert$ { }
    @Container
    class ShortAssert$ { }
    @Container
    class SoftThrowableTypeAssert$ { }
    @Container
    class SpliteratorAssert$<ELEMENT> {
        static <ELEMENT> org.assertj.core.api.SpliteratorAssert<ELEMENT> assertThatSpliterator(@Modified java.util.Spliterator<ELEMENT> arg0) { return null; }
    }
    @Container
    class StringAssert$ { }
    @Container
    class TemporalAssert$ { }
    @Container
    class ThrowableAssert$ { }
    @Container
    class ThrowableTypeAssert$ { }
    @Container
    class UniversalComparableAssert$ { }
    @Container
    class UriAssert$ { }
    @Container
    class UrlAssert$ { }
    @Container
    class YearMonthAssert$ { }
    @Container
    class ZonedDateTimeAssert$ { }
}
