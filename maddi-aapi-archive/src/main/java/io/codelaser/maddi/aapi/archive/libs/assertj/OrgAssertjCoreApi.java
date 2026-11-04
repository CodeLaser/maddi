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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Spliterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractBigDecimalAssert;
import org.assertj.core.api.AbstractBigIntegerAssert;
import org.assertj.core.api.AbstractBooleanArrayAssert;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractByteArrayAssert;
import org.assertj.core.api.AbstractByteAssert;
import org.assertj.core.api.AbstractCharArrayAssert;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractCharacterAssert;
import org.assertj.core.api.AbstractClassAssert;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.AbstractComparableAssert;
import org.assertj.core.api.AbstractDateAssert;
import org.assertj.core.api.AbstractDoubleArrayAssert;
import org.assertj.core.api.AbstractDoubleAssert;
import org.assertj.core.api.AbstractDurationAssert;
import org.assertj.core.api.AbstractFileAssert;
import org.assertj.core.api.AbstractFloatArrayAssert;
import org.assertj.core.api.AbstractFloatAssert;
import org.assertj.core.api.AbstractFutureAssert;
import org.assertj.core.api.AbstractInputStreamAssert;
import org.assertj.core.api.AbstractInstantAssert;
import org.assertj.core.api.AbstractIntArrayAssert;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.AbstractLocalDateAssert;
import org.assertj.core.api.AbstractLocalDateTimeAssert;
import org.assertj.core.api.AbstractLocalTimeAssert;
import org.assertj.core.api.AbstractLongArrayAssert;
import org.assertj.core.api.AbstractLongAssert;
import org.assertj.core.api.AbstractObjectArrayAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.AbstractOffsetDateTimeAssert;
import org.assertj.core.api.AbstractOffsetTimeAssert;
import org.assertj.core.api.AbstractPathAssert;
import org.assertj.core.api.AbstractPeriodAssert;
import org.assertj.core.api.AbstractShortArrayAssert;
import org.assertj.core.api.AbstractShortAssert;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.AbstractUniversalComparableAssert;
import org.assertj.core.api.AbstractUriAssert;
import org.assertj.core.api.AbstractUrlAssert;
import org.assertj.core.api.AbstractYearMonthAssert;
import org.assertj.core.api.AbstractZonedDateTimeAssert;
import org.assertj.core.api.AssertDelegateTarget;
import org.assertj.core.api.AssertFactory;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.AtomicBooleanAssert;
import org.assertj.core.api.AtomicIntegerArrayAssert;
import org.assertj.core.api.AtomicIntegerAssert;
import org.assertj.core.api.AtomicIntegerFieldUpdaterAssert;
import org.assertj.core.api.AtomicLongArrayAssert;
import org.assertj.core.api.AtomicLongAssert;
import org.assertj.core.api.AtomicLongFieldUpdaterAssert;
import org.assertj.core.api.AtomicMarkableReferenceAssert;
import org.assertj.core.api.AtomicReferenceArrayAssert;
import org.assertj.core.api.AtomicReferenceAssert;
import org.assertj.core.api.AtomicReferenceFieldUpdaterAssert;
import org.assertj.core.api.AtomicStampedReferenceAssert;
import org.assertj.core.api.BigDecimalAssert;
import org.assertj.core.api.BigIntegerAssert;
import org.assertj.core.api.Boolean2DArrayAssert;
import org.assertj.core.api.BooleanArrayAssert;
import org.assertj.core.api.BooleanAssert;
import org.assertj.core.api.Byte2DArrayAssert;
import org.assertj.core.api.ByteArrayAssert;
import org.assertj.core.api.ByteAssert;
import org.assertj.core.api.Char2DArrayAssert;
import org.assertj.core.api.CharArrayAssert;
import org.assertj.core.api.CharSequenceAssert;
import org.assertj.core.api.CharacterAssert;
import org.assertj.core.api.ClassAssert;
import org.assertj.core.api.ClassBasedNavigableIterableAssert;
import org.assertj.core.api.ClassBasedNavigableListAssert;
import org.assertj.core.api.CollectionAssert;
import org.assertj.core.api.CompletableFutureAssert;
import org.assertj.core.api.DateAssert;
import org.assertj.core.api.Double2DArrayAssert;
import org.assertj.core.api.DoubleArrayAssert;
import org.assertj.core.api.DoubleAssert;
import org.assertj.core.api.DurationAssert;
import org.assertj.core.api.FactoryBasedNavigableIterableAssert;
import org.assertj.core.api.FactoryBasedNavigableListAssert;
import org.assertj.core.api.FileAssert;
import org.assertj.core.api.Float2DArrayAssert;
import org.assertj.core.api.FloatArrayAssert;
import org.assertj.core.api.FloatAssert;
import org.assertj.core.api.FutureAssert;
import org.assertj.core.api.InstantAssert;
import org.assertj.core.api.Int2DArrayAssert;
import org.assertj.core.api.IntArrayAssert;
import org.assertj.core.api.IntegerAssert;
import org.assertj.core.api.IterableAssert;
import org.assertj.core.api.IteratorAssert;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.LocalDateAssert;
import org.assertj.core.api.LocalDateTimeAssert;
import org.assertj.core.api.LocalTimeAssert;
import org.assertj.core.api.Long2DArrayAssert;
import org.assertj.core.api.LongAdderAssert;
import org.assertj.core.api.LongArrayAssert;
import org.assertj.core.api.LongAssert;
import org.assertj.core.api.MapAssert;
import org.assertj.core.api.Object2DArrayAssert;
import org.assertj.core.api.ObjectArrayAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.OffsetDateTimeAssert;
import org.assertj.core.api.OffsetTimeAssert;
import org.assertj.core.api.OptionalAssert;
import org.assertj.core.api.OptionalDoubleAssert;
import org.assertj.core.api.OptionalIntAssert;
import org.assertj.core.api.OptionalLongAssert;
import org.assertj.core.api.PathAssert;
import org.assertj.core.api.PeriodAssert;
import org.assertj.core.api.Short2DArrayAssert;
import org.assertj.core.api.ShortArrayAssert;
import org.assertj.core.api.ShortAssert;
import org.assertj.core.api.SpliteratorAssert;
import org.assertj.core.api.TemporalAssert;
import org.assertj.core.api.ThrowableAssert;
import org.assertj.core.api.ThrowableTypeAssert;
import org.assertj.core.api.UriAssert;
import org.assertj.core.api.YearMonthAssert;
import org.assertj.core.api.ZonedDateTimeAssert;

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
        @Independent static AbstractBooleanArrayAssert<?> assertThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent static Boolean2DArrayAssert assertThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent static AbstractByteArrayAssert<?> assertThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent static Byte2DArrayAssert assertThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent static AbstractCharArrayAssert<?> assertThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent static Char2DArrayAssert assertThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent static AbstractDoubleArrayAssert<?> assertThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent static Double2DArrayAssert assertThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent static AbstractFloatArrayAssert<?> assertThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent static AbstractIntArrayAssert<?> assertThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent static Int2DArrayAssert assertThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent static Float2DArrayAssert assertThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent static AbstractLongArrayAssert<?> assertThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent static Long2DArrayAssert assertThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent static AbstractShortArrayAssert<?> assertThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent static Short2DArrayAssert assertThat(@Independent @NotModified short[][] arg0) { return null; }
        static <T> T fail(String arg0, @NotModified Throwable arg1) { return null; }
        static <T> T fail(@NotModified Throwable arg0) { return null; }
        // the callable is CALLED, not modified (ThrowingCallable is not @FunctionalInterface, so the shallow
        // rule for function types does not reach it); the message arguments are formatted, not modified
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, String arg1, @NotModified Object... arg2) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> assertThatCode(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static Throwable catchThrowable(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static <THROWABLE extends Throwable> THROWABLE catchThrowableOfType(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, Class<THROWABLE> arg1) { return null; }
        static <THROWABLE extends Throwable> THROWABLE catchThrowableOfType(Class<THROWABLE> arg0, @IgnoreModifications ThrowableAssert.ThrowingCallable arg1) { return null; }
        static Exception catchException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static RuntimeException catchRuntimeException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static NullPointerException catchNullPointerException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static IllegalArgumentException catchIllegalArgumentException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static IOException catchIOException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static ReflectiveOperationException catchReflectiveOperationException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static IllegalStateException catchIllegalStateException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static IndexOutOfBoundsException catchIndexOutOfBoundsException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        static <RESULT> CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        static <RESULT> CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified CompletionStage<RESULT> arg0) { return null; }
        @Independent
        static <VALUE> OptionalAssert<VALUE> assertThat(@Independent @NotModified Optional<VALUE> arg0) { return null; }
        @Independent
        static OptionalDoubleAssert assertThat(@Independent @NotModified OptionalDouble arg0) { return null; }
        @Independent
        static OptionalIntAssert assertThat(@Independent @NotModified OptionalInt arg0) { return null; }
        @Independent
        static OptionalLongAssert assertThat(@Independent @NotModified OptionalLong arg0) { return null; }
        @Independent
        static AbstractBigDecimalAssert<?> assertThat(@Independent @NotModified BigDecimal arg0) { return null; }
        @Independent
        static AbstractBigIntegerAssert<?> assertThat(@Independent @NotModified BigInteger arg0) { return null; }
        @Independent
        static AbstractUriAssert<?> assertThat(@Independent @NotModified URI arg0) { return null; }
        @Independent
        static AbstractUrlAssert<?> assertThat(@Independent @NotModified URL arg0) { return null; }
        @Independent
        static AbstractBooleanAssert<?> assertThat(@Independent @NotModified Boolean arg0) { return null; }
        @Independent
        static AbstractByteAssert<?> assertThat(@Independent @NotModified Byte arg0) { return null; }
        @Independent
        static AbstractCharacterAssert<?> assertThat(@Independent @NotModified Character arg0) { return null; }
        @Independent
        static ClassAssert assertThat(@Independent @NotModified Class<?> arg0) { return null; }
        @Independent
        static AbstractDoubleAssert<?> assertThat(@Independent @NotModified Double arg0) { return null; }
        @Independent
        static AbstractFileAssert<?> assertThat(@Independent @NotModified File arg0) { return null; }
        @Independent
        static <RESULT> FutureAssert<RESULT> assertThat(@Independent @NotModified Future<RESULT> arg0) { return null; }
        @Independent
        static AbstractFloatAssert<?> assertThat(@Independent @NotModified Float arg0) { return null; }
        @Independent
        static AbstractIntegerAssert<?> assertThat(@Independent @NotModified Integer arg0) { return null; }
        @Independent
        static <ACTUAL extends Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> FactoryBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified Iterable<? extends ELEMENT> arg0, AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ClassBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified ACTUAL arg0, Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> FactoryBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified List<? extends ELEMENT> arg0, AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ELEMENT, ACTUAL extends List<? extends ELEMENT>, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ClassBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified List<? extends ELEMENT> arg0, Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static AbstractLongAssert<?> assertThat(@Independent @NotModified Long arg0) { return null; }
        @Independent
        static <T> ObjectAssert<T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> ObjectArrayAssert<T> assertThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        static <T> Object2DArrayAssert<T> assertThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        static AbstractShortAssert<?> assertThat(@Independent @NotModified Short arg0) { return null; }
        @Independent
        static AbstractDateAssert<?> assertThat(@Independent @NotModified Date arg0) { return null; }
        @Independent
        static AbstractZonedDateTimeAssert<?> assertThat(@Independent @NotModified ZonedDateTime arg0) { return null; }
        @Independent
        static TemporalAssert assertThatTemporal(@Independent @NotModified Temporal arg0) { return null; }
        @Independent
        static AbstractLocalDateTimeAssert<?> assertThat(@Independent @NotModified LocalDateTime arg0) { return null; }
        @Independent
        static AbstractOffsetDateTimeAssert<?> assertThat(@Independent @NotModified OffsetDateTime arg0) { return null; }
        @Independent
        static AbstractOffsetTimeAssert<?> assertThat(@Independent @NotModified OffsetTime arg0) { return null; }
        @Independent
        static AbstractLocalTimeAssert<?> assertThat(@Independent @NotModified LocalTime arg0) { return null; }
        @Independent
        static AbstractLocalDateAssert<?> assertThat(@Independent @NotModified LocalDate arg0) { return null; }
        @Independent
        static AbstractYearMonthAssert<?> assertThat(@Independent @NotModified YearMonth arg0) { return null; }
        @Independent
        static AbstractInstantAssert<?> assertThat(@Independent @NotModified Instant arg0) { return null; }
        @Independent
        static AbstractDurationAssert<?> assertThat(@Independent @NotModified Duration arg0) { return null; }
        @Independent
        static AbstractPeriodAssert<?> assertThat(@Independent @NotModified Period arg0) { return null; }
        @Independent
        static AtomicBooleanAssert assertThat(@Independent @NotModified AtomicBoolean arg0) { return null; }
        @Independent
        static AtomicIntegerAssert assertThat(@Independent @NotModified AtomicInteger arg0) { return null; }
        @Independent
        static AtomicIntegerArrayAssert assertThat(@Independent @NotModified AtomicIntegerArray arg0) { return null; }
        @Independent
        static <OBJECT> AtomicIntegerFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static LongAdderAssert assertThat(@Independent @NotModified LongAdder arg0) { return null; }
        @Independent
        static AtomicLongAssert assertThat(@Independent @NotModified AtomicLong arg0) { return null; }
        @Independent
        static AtomicLongArrayAssert assertThat(@Independent @NotModified AtomicLongArray arg0) { return null; }
        @Independent
        static <OBJECT> AtomicLongFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static <VALUE> AtomicReferenceAssert<VALUE> assertThat(@Independent @NotModified AtomicReference<VALUE> arg0) { return null; }
        @Independent
        static <ELEMENT> AtomicReferenceArrayAssert<ELEMENT> assertThat(@Independent @NotModified AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        static <FIELD, OBJECT> AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> assertThat(@Independent @NotModified AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        static <VALUE> AtomicMarkableReferenceAssert<VALUE> assertThat(@Independent @NotModified AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        static <VALUE> AtomicStampedReferenceAssert<VALUE> assertThat(@Independent @NotModified AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        static <T extends Throwable> AbstractThrowableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> ObjectAssert<T> assertThatObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T extends Throwable> ThrowableTypeAssert<T> assertThatExceptionOfType(@Independent @NotModified Class<? extends T> arg0) { return null; }
        @Independent
        static <T> T assertThat(@Independent @NotModified AssertProvider<T> arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assertThat(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assertThatCharSequence(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assertThat(@Independent @NotModified StringBuilder arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assertThat(@Independent @NotModified StringBuffer arg0) { return null; }
        @Independent
        static <ELEMENT> IterableAssert<ELEMENT> assertThat(@Independent @NotModified Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ELEMENT> IterableAssert<ELEMENT> assertThatIterable(@Independent @NotModified Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <E> AbstractCollectionAssert<?, Collection<? extends E>, E, ObjectAssert<E>> assertThat(@Independent @NotModified Collection<? extends E> arg0) { return null; }
        @Independent
        static <E> AbstractCollectionAssert<?, Collection<? extends E>, E, ObjectAssert<E>> assertThatCollection(@Independent @NotModified Collection<? extends E> arg0) { return null; }
        @Independent
        static <ELEMENT> ListAssert<ELEMENT> assertThat(@Independent @NotModified List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ELEMENT> ListAssert<ELEMENT> assertThatList(@Independent @NotModified List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static AbstractPathAssert<?> assertThat(@Independent @NotModified Path arg0) { return null; }
        @Independent
        static AbstractPathAssert<?> assertThatPath(@Independent @NotModified Path arg0) { return null; }
        @Independent
        static <K, V> MapAssert<K, V> assertThat(@Independent @NotModified Map<K, V> arg0) { return null; }
        @Independent
        static <T extends Comparable<? super T>> AbstractComparableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> AbstractUniversalComparableAssert<?, T> assertThatComparable(@Independent @NotModified Comparable<T> arg0) { return null; }
        @Independent
        static <T extends AssertDelegateTarget> T assertThat(@Independent @NotModified T arg0) { return null; }
    }

    //public class org.assertj.core.api.BDDAssertions
    class BDDAssertions$ {
        @Independent static AbstractBooleanArrayAssert<?> then(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent static Boolean2DArrayAssert then(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent static AbstractByteArrayAssert<?> then(@Independent @NotModified byte[] arg0) { return null; }
        @Independent static Byte2DArrayAssert then(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent static AbstractCharArrayAssert<?> then(@Independent @NotModified char[] arg0) { return null; }
        @Independent static Char2DArrayAssert then(@Independent @NotModified char[][] arg0) { return null; }
        @Independent static AbstractDoubleArrayAssert<?> then(@Independent @NotModified double[] arg0) { return null; }
        @Independent static Double2DArrayAssert then(@Independent @NotModified double[][] arg0) { return null; }
        @Independent static AbstractFloatArrayAssert<?> then(@Independent @NotModified float[] arg0) { return null; }
        @Independent static Float2DArrayAssert then(@Independent @NotModified float[][] arg0) { return null; }
        @Independent static AbstractIntArrayAssert<?> then(@Independent @NotModified int[] arg0) { return null; }
        @Independent static Int2DArrayAssert then(@Independent @NotModified int[][] arg0) { return null; }
        @Independent static AbstractLongArrayAssert<?> then(@Independent @NotModified long[] arg0) { return null; }
        @Independent static Long2DArrayAssert then(@Independent @NotModified long[][] arg0) { return null; }
        @Independent static AbstractShortArrayAssert<?> then(@Independent @NotModified short[] arg0) { return null; }
        @Independent static Short2DArrayAssert then(@Independent @NotModified short[][] arg0) { return null; }
        static <T> T fail(String arg0, @NotModified Throwable arg1) { return null; }
        static <T> T fail(@NotModified Throwable arg0) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> thenThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> thenThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, String arg1, @NotModified Object... arg2) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> thenCode(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static Throwable catchThrowable(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static <THROWABLE extends Throwable> THROWABLE catchThrowableOfType(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, Class<THROWABLE> arg1) { return null; }
        static <THROWABLE extends Throwable> THROWABLE catchThrowableOfType(Class<THROWABLE> arg0, @IgnoreModifications ThrowableAssert.ThrowingCallable arg1) { return null; }
        static Exception catchException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static RuntimeException catchRuntimeException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static NullPointerException catchNullPointerException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static IllegalArgumentException catchIllegalArgumentException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static IOException catchIOException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static ReflectiveOperationException catchReflectiveOperationException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static IllegalStateException catchIllegalStateException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static IndexOutOfBoundsException catchIndexOutOfBoundsException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        static <VALUE> OptionalAssert<VALUE> then(@Independent @NotModified Optional<VALUE> arg0) { return null; }
        @Independent
        static OptionalIntAssert then(@Independent @NotModified OptionalInt arg0) { return null; }
        @Independent
        static OptionalLongAssert then(@Independent @NotModified OptionalLong arg0) { return null; }
        @Independent
        static OptionalDoubleAssert then(@Independent @NotModified OptionalDouble arg0) { return null; }
        @Independent
        static AbstractBigDecimalAssert<?> then(@Independent @NotModified BigDecimal arg0) { return null; }
        @Independent
        static AbstractBigIntegerAssert<?> then(@Independent @NotModified BigInteger arg0) { return null; }
        @Independent
        static AbstractBooleanAssert<?> then(@Independent @NotModified Boolean arg0) { return null; }
        @Independent
        static AbstractByteAssert<?> then(@Independent @NotModified Byte arg0) { return null; }
        @Independent
        static AbstractCharacterAssert<?> then(@Independent @NotModified Character arg0) { return null; }
        @Independent
        static ClassAssert then(@Independent @NotModified Class<?> arg0) { return null; }
        @Independent
        static <T extends Comparable<? super T>> AbstractComparableAssert<?, T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> AbstractUniversalComparableAssert<?, T> thenComparable(@Independent @NotModified Comparable<T> arg0) { return null; }
        @Independent
        static <T> IterableAssert<T> then(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent
        static <ELEMENT> IterableAssert<ELEMENT> thenIterable(@Independent @NotModified Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ACTUAL extends Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> FactoryBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified Iterable<? extends ELEMENT> arg0, AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ClassBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified ACTUAL arg0, Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> FactoryBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified List<? extends ELEMENT> arg0, AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ELEMENT, ACTUAL extends List<? extends ELEMENT>, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ClassBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified List<? extends ELEMENT> arg0, Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static AbstractDoubleAssert<?> then(@Independent @NotModified Double arg0) { return null; }
        @Independent
        static AbstractFileAssert<?> then(@Independent @NotModified File arg0) { return null; }
        @Independent
        static AbstractPathAssert<?> then(@Independent @NotModified Path arg0) { return null; }
        @Independent
        static AbstractPathAssert<?> thenPath(@Independent @NotModified Path arg0) { return null; }
        @Independent
        static <RESULT> FutureAssert<RESULT> then(@Independent @NotModified Future<RESULT> arg0) { return null; }
        @Independent
        static AbstractFloatAssert<?> then(@Independent @NotModified Float arg0) { return null; }
        @Independent
        static AbstractIntegerAssert<?> then(@Independent @NotModified Integer arg0) { return null; }
        @Independent
        static <E> AbstractCollectionAssert<?, Collection<? extends E>, E, ObjectAssert<E>> then(@Independent @NotModified Collection<? extends E> arg0) { return null; }
        @Independent
        static <E> AbstractCollectionAssert<?, Collection<? extends E>, E, ObjectAssert<E>> thenCollection(@Independent @NotModified Collection<? extends E> arg0) { return null; }
        @Independent
        static <T> ListAssert<T> then(@Independent @NotModified List<? extends T> arg0) { return null; }
        @Independent
        static <ELEMENT> ListAssert<ELEMENT> thenList(@Independent @NotModified List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static AbstractLongAssert<?> then(@Independent @NotModified Long arg0) { return null; }
        @Independent
        static <T> ObjectAssert<T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> ObjectArrayAssert<T> then(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        static <T> Object2DArrayAssert<T> then(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        static <K, V> MapAssert<K, V> then(@Independent @NotModified Map<K, V> arg0) { return null; }
        @Independent
        static AbstractShortAssert<?> then(@Independent @NotModified Short arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> then(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> thenCharSequence(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> then(@Independent @NotModified StringBuilder arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> then(@Independent @NotModified StringBuffer arg0) { return null; }
        @Independent
        static AbstractDateAssert<?> then(@Independent @NotModified Date arg0) { return null; }
        @Independent
        static AtomicBooleanAssert then(@Independent @NotModified AtomicBoolean arg0) { return null; }
        @Independent
        static AtomicIntegerAssert then(@Independent @NotModified AtomicInteger arg0) { return null; }
        @Independent
        static AtomicIntegerArrayAssert then(@Independent @NotModified AtomicIntegerArray arg0) { return null; }
        @Independent
        static <OBJECT> AtomicIntegerFieldUpdaterAssert<OBJECT> then(@Independent @NotModified AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static LongAdderAssert then(@Independent @NotModified LongAdder arg0) { return null; }
        @Independent
        static AtomicLongAssert then(@Independent @NotModified AtomicLong arg0) { return null; }
        @Independent
        static AtomicLongArrayAssert then(@Independent @NotModified AtomicLongArray arg0) { return null; }
        @Independent
        static <OBJECT> AtomicLongFieldUpdaterAssert<OBJECT> then(@Independent @NotModified AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static <VALUE> AtomicReferenceAssert<VALUE> then(@Independent @NotModified AtomicReference<VALUE> arg0) { return null; }
        @Independent
        static <ELEMENT> AtomicReferenceArrayAssert<ELEMENT> then(@Independent @NotModified AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        static <FIELD, OBJECT> AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> then(@Independent @NotModified AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        static <VALUE> AtomicMarkableReferenceAssert<VALUE> then(@Independent @NotModified AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        static <VALUE> AtomicStampedReferenceAssert<VALUE> then(@Independent @NotModified AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        static <T extends Throwable> AbstractThrowableAssert<?, T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> ObjectAssert<T> thenObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> ObjectAssert<T> thenWith(@Independent @NotModified T arg0, Consumer<T>... arg1) { return null; }
        @Independent
        static TemporalAssert thenTemporal(@Independent @NotModified Temporal arg0) { return null; }
        @Independent
        static AbstractLocalDateAssert<?> then(@Independent @NotModified LocalDate arg0) { return null; }
        @Independent
        static AbstractYearMonthAssert<?> then(@Independent @NotModified YearMonth arg0) { return null; }
        @Independent
        static AbstractLocalDateTimeAssert<?> then(@Independent @NotModified LocalDateTime arg0) { return null; }
        @Independent
        static AbstractZonedDateTimeAssert<?> then(@Independent @NotModified ZonedDateTime arg0) { return null; }
        @Independent
        static AbstractLocalTimeAssert<?> then(@Independent @NotModified LocalTime arg0) { return null; }
        @Independent
        static AbstractOffsetTimeAssert<?> then(@Independent @NotModified OffsetTime arg0) { return null; }
        @Independent
        static AbstractInstantAssert<?> then(@Independent @NotModified Instant arg0) { return null; }
        @Independent
        static AbstractDurationAssert<?> then(@Independent @NotModified Duration arg0) { return null; }
        @Independent
        static AbstractPeriodAssert<?> then(@Independent @NotModified Period arg0) { return null; }
        @Independent
        static AbstractUriAssert<?> then(@Independent @NotModified URI arg0) { return null; }
        @Independent
        static AbstractUrlAssert<?> then(@Independent @NotModified URL arg0) { return null; }
        @Independent
        static AbstractOffsetDateTimeAssert<?> then(@Independent @NotModified OffsetDateTime arg0) { return null; }
        @Independent
        static <RESULT> CompletableFutureAssert<RESULT> then(@Independent @NotModified CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        static <RESULT> CompletableFutureAssert<RESULT> then(@Independent @NotModified CompletionStage<RESULT> arg0) { return null; }
        @Independent
        static <T extends AssertDelegateTarget> T then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> T then(@Independent @NotModified AssertProvider<T> arg0) { return null; }
        @Independent
        static <T extends Throwable> ThrowableTypeAssert<T> thenExceptionOfType(@Independent @NotModified Class<? extends T> arg0) { return null; }
    }

    //public interface org.assertj.core.api.WithAssertions
    class WithAssertions$ {
        <T> T fail(String arg0, @NotModified Throwable arg1) { return null; }
        <T> T fail(@NotModified Throwable arg0) { return null; }
        @Independent AbstractLongArrayAssert<?> assertThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent Long2DArrayAssert assertThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent AbstractShortArrayAssert<?> assertThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent Short2DArrayAssert assertThat(@Independent @NotModified short[][] arg0) { return null; }
        @Independent AbstractCharArrayAssert<?> assertThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent Char2DArrayAssert assertThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent AbstractBooleanArrayAssert<?> assertThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent Boolean2DArrayAssert assertThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent AbstractByteArrayAssert<?> assertThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent Byte2DArrayAssert assertThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent AbstractIntArrayAssert<?> assertThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent Int2DArrayAssert assertThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent Float2DArrayAssert assertThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent AbstractFloatArrayAssert<?> assertThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent AbstractDoubleArrayAssert<?> assertThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent Double2DArrayAssert assertThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, String arg1, @NotModified Object... arg2) { return null; }
        @Independent AbstractThrowableAssert<?, ? extends Throwable> assertThatCode(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        Throwable catchThrowable(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        <THROWABLE extends Throwable> THROWABLE catchThrowableOfType(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, Class<THROWABLE> arg1) { return null; }
        <THROWABLE extends Throwable> THROWABLE catchThrowableOfType(Class<THROWABLE> arg0, @IgnoreModifications ThrowableAssert.ThrowingCallable arg1) { return null; }
        Exception catchException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        RuntimeException catchRuntimeException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        NullPointerException catchNullPointerException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        IllegalArgumentException catchIllegalArgumentException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        IOException catchIOException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        ReflectiveOperationException catchReflectiveOperationException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        IllegalStateException catchIllegalStateException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        IndexOutOfBoundsException catchIndexOutOfBoundsException(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        <T> ObjectArrayAssert<T> assertThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        <T> Object2DArrayAssert<T> assertThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        <T extends AssertDelegateTarget> T assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <K, V> MapAssert<K, V> assertThat(@Independent @NotModified Map<K, V> arg0) { return null; }
        @Independent
        AbstractLongAssert<?> assertThat(@Independent @NotModified Long arg0) { return null; }
        @Independent
        <T> ObjectAssert<T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        AbstractDateAssert<?> assertThat(@Independent @NotModified Date arg0) { return null; }
        @Independent
        <T extends Throwable> AbstractThrowableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        AbstractBigDecimalAssert<?> assertThat(@Independent @NotModified BigDecimal arg0) { return null; }
        @Independent
        AbstractBigIntegerAssert<?> assertThat(@Independent @NotModified BigInteger arg0) { return null; }
        @Independent
        AtomicBooleanAssert assertThat(@Independent @NotModified AtomicBoolean arg0) { return null; }
        @Independent
        AtomicIntegerAssert assertThat(@Independent @NotModified AtomicInteger arg0) { return null; }
        @Independent
        AtomicIntegerArrayAssert assertThat(@Independent @NotModified AtomicIntegerArray arg0) { return null; }
        @Independent
        <OBJECT> AtomicIntegerFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        LongAdderAssert assertThat(@Independent @NotModified LongAdder arg0) { return null; }
        @Independent
        AtomicLongAssert assertThat(@Independent @NotModified AtomicLong arg0) { return null; }
        @Independent
        AtomicLongArrayAssert assertThat(@Independent @NotModified AtomicLongArray arg0) { return null; }
        @Independent
        <OBJECT> AtomicLongFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        <VALUE> AtomicReferenceAssert<VALUE> assertThat(@Independent @NotModified AtomicReference<VALUE> arg0) { return null; }
        @Independent
        <ELEMENT> AtomicReferenceArrayAssert<ELEMENT> assertThat(@Independent @NotModified AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        <FIELD, OBJECT> AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> assertThat(@Independent @NotModified AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        <VALUE> AtomicMarkableReferenceAssert<VALUE> assertThat(@Independent @NotModified AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        <VALUE> AtomicStampedReferenceAssert<VALUE> assertThat(@Independent @NotModified AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        AbstractCharSequenceAssert<?, ? extends CharSequence> assertThat(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        AbstractCharSequenceAssert<?, ? extends CharSequence> assertThat(@Independent @NotModified StringBuilder arg0) { return null; }
        @Independent
        AbstractCharSequenceAssert<?, ? extends CharSequence> assertThat(@Independent @NotModified StringBuffer arg0) { return null; }
        @Independent
        AbstractShortAssert<?> assertThat(@Independent @NotModified Short arg0) { return null; }
        @Independent
        ClassAssert assertThat(@Independent @NotModified Class<?> arg0) { return null; }
        @Independent
        AbstractCharacterAssert<?> assertThat(@Independent @NotModified Character arg0) { return null; }
        @Independent
        <T extends Comparable<? super T>> AbstractComparableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> AbstractUniversalComparableAssert<?, T> assertThatComparable(@Independent @NotModified Comparable<T> arg0) { return null; }
        @Independent
        <T> IterableAssert<T> assertThat(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent
        <ELEMENT> IterableAssert<ELEMENT> assertThatIterable(@Independent @NotModified Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        <ACTUAL extends Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> FactoryBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified Iterable<? extends ELEMENT> arg0, AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        <ACTUAL extends Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ClassBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified ACTUAL arg0, Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        AbstractBooleanAssert<?> assertThat(@Independent @NotModified Boolean arg0) { return null; }
        @Independent
        AbstractByteAssert<?> assertThat(@Independent @NotModified Byte arg0) { return null; }
        @Independent
        AbstractFileAssert<?> assertThat(@Independent @NotModified File arg0) { return null; }
        @Independent
        <RESULT> FutureAssert<RESULT> assertThat(@Independent @NotModified Future<RESULT> arg0) { return null; }
        @Independent
        AbstractPathAssert<?> assertThat(@Independent @NotModified Path arg0) { return null; }
        @Independent
        AbstractPathAssert<?> assertThatPath(@Independent @NotModified Path arg0) { return null; }
        @Independent
        AbstractFloatAssert<?> assertThat(@Independent @NotModified Float arg0) { return null; }
        @Independent
        AbstractIntegerAssert<?> assertThat(@Independent @NotModified Integer arg0) { return null; }
        @Independent
        AbstractDoubleAssert<?> assertThat(@Independent @NotModified Double arg0) { return null; }
        @Independent
        <E> AbstractCollectionAssert<?, Collection<? extends E>, E, ObjectAssert<E>> assertThat(@Independent @NotModified Collection<? extends E> arg0) { return null; }
        @Independent
        <E> AbstractCollectionAssert<?, Collection<? extends E>, E, ObjectAssert<E>> assertThatCollection(@Independent @NotModified Collection<? extends E> arg0) { return null; }
        @Independent
        <T> ListAssert<T> assertThat(@Independent @NotModified List<? extends T> arg0) { return null; }
        @Independent
        <ELEMENT> ListAssert<ELEMENT> assertThatList(@Independent @NotModified List<? extends ELEMENT> arg0) { return null; }
        @Independent
        <ELEMENT, ACTUAL extends List<? extends ELEMENT>, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ClassBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified List<? extends ELEMENT> arg0, Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        <ACTUAL extends List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> FactoryBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified List<? extends ELEMENT> arg0, AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        AbstractZonedDateTimeAssert<?> assertThat(@Independent @NotModified ZonedDateTime arg0) { return null; }
        @Independent
        <RESULT> CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        <RESULT> CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified CompletionStage<RESULT> arg0) { return null; }
        @Independent
        <VALUE> OptionalAssert<VALUE> assertThat(@Independent @NotModified Optional<VALUE> arg0) { return null; }
        @Independent
        OptionalDoubleAssert assertThat(@Independent @NotModified OptionalDouble arg0) { return null; }
        @Independent
        OptionalIntAssert assertThat(@Independent @NotModified OptionalInt arg0) { return null; }
        @Independent
        OptionalLongAssert assertThat(@Independent @NotModified OptionalLong arg0) { return null; }
        @Independent
        TemporalAssert assertThatTemporal(@Independent @NotModified Temporal arg0) { return null; }
        @Independent
        AbstractLocalDateTimeAssert<?> assertThat(@Independent @NotModified LocalDateTime arg0) { return null; }
        @Independent
        AbstractLocalDateAssert<?> assertThat(@Independent @NotModified LocalDate arg0) { return null; }
        @Independent
        AbstractYearMonthAssert<?> assertThat(@Independent @NotModified YearMonth arg0) { return null; }
        @Independent
        AbstractLocalTimeAssert<?> assertThat(@Independent @NotModified LocalTime arg0) { return null; }
        @Independent
        AbstractInstantAssert<?> assertThat(@Independent @NotModified Instant arg0) { return null; }
        @Independent
        AbstractDurationAssert<?> assertThat(@Independent @NotModified Duration arg0) { return null; }
        @Independent
        AbstractPeriodAssert<?> assertThat(@Independent @NotModified Period arg0) { return null; }
        @Independent
        AbstractOffsetTimeAssert<?> assertThat(@Independent @NotModified OffsetTime arg0) { return null; }
        @Independent
        AbstractOffsetDateTimeAssert<?> assertThat(@Independent @NotModified OffsetDateTime arg0) { return null; }
        @Independent
        <T> ObjectAssert<T> assertThatObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T extends Throwable> ThrowableTypeAssert<T> assertThatExceptionOfType(@Independent @NotModified Class<? extends T> arg0) { return null; }
        @Independent
        AbstractUrlAssert<?> assertThat(@Independent @NotModified URL arg0) { return null; }
        @Independent
        AbstractUriAssert<?> assertThat(@Independent @NotModified URI arg0) { return null; }
        @Independent
        <T> T assertThat(@Independent @NotModified AssertProvider<T> arg0) { return null; }
    }

    //public class org.assertj.core.api.AssertionsForClassTypes
    class AssertionsForClassTypes$ {
        @Independent static AbstractBooleanArrayAssert<?> assertThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent static Boolean2DArrayAssert assertThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent static AbstractByteArrayAssert<?> assertThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent static Byte2DArrayAssert assertThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent static AbstractCharArrayAssert<?> assertThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent static Char2DArrayAssert assertThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent static AbstractDoubleArrayAssert<?> assertThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent static Double2DArrayAssert assertThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent static AbstractFloatArrayAssert<?> assertThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent static Float2DArrayAssert assertThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent static AbstractIntArrayAssert<?> assertThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent static Int2DArrayAssert assertThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent static AbstractLongArrayAssert<?> assertThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent static Long2DArrayAssert assertThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent static AbstractShortArrayAssert<?> assertThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent static Short2DArrayAssert assertThat(@Independent @NotModified short[][] arg0) { return null; }
        static void fail(String arg0, @NotModified Throwable arg1) { }
        static void fail(@NotModified Throwable arg0) { }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, String arg1, @NotModified Object... arg2) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> assertThatCode(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static Throwable catchThrowable(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static <THROWABLE extends Throwable> THROWABLE catchThrowableOfType(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, Class<THROWABLE> arg1) { return null; }
        static <THROWABLE extends Throwable> THROWABLE catchThrowableOfType(Class<THROWABLE> arg0, @IgnoreModifications ThrowableAssert.ThrowingCallable arg1) { return null; }
        @Independent
        static <RESULT> CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        static <VALUE> OptionalAssert<VALUE> assertThat(@Independent @NotModified Optional<VALUE> arg0) { return null; }
        @Independent
        static OptionalDoubleAssert assertThat(@Independent @NotModified OptionalDouble arg0) { return null; }
        @Independent
        static OptionalIntAssert assertThat(@Independent @NotModified OptionalInt arg0) { return null; }
        @Independent
        static OptionalLongAssert assertThat(@Independent @NotModified OptionalLong arg0) { return null; }
        @Independent
        static AbstractBigDecimalAssert<?> assertThat(@Independent @NotModified BigDecimal arg0) { return null; }
        @Independent
        static AbstractUriAssert<?> assertThat(@Independent @NotModified URI arg0) { return null; }
        @Independent
        static AbstractUrlAssert<?> assertThat(@Independent @NotModified URL arg0) { return null; }
        @Independent
        static AbstractBooleanAssert<?> assertThat(@Independent @NotModified Boolean arg0) { return null; }
        @Independent
        static AbstractByteAssert<?> assertThat(@Independent @NotModified Byte arg0) { return null; }
        @Independent
        static AbstractCharacterAssert<?> assertThat(@Independent @NotModified Character arg0) { return null; }
        @Independent
        static ClassAssert assertThat(@Independent @NotModified Class<?> arg0) { return null; }
        @Independent
        static AbstractDoubleAssert<?> assertThat(@Independent @NotModified Double arg0) { return null; }
        @Independent
        static AbstractFileAssert<?> assertThat(@Independent @NotModified File arg0) { return null; }
        @Independent
        static AbstractFloatAssert<?> assertThat(@Independent @NotModified Float arg0) { return null; }
        @Independent
        static AbstractIntegerAssert<?> assertThat(@Independent @NotModified Integer arg0) { return null; }
        @Independent
        static AbstractLongAssert<?> assertThat(@Independent @NotModified Long arg0) { return null; }
        @Independent
        static <T> ObjectAssert<T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> ObjectArrayAssert<T> assertThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        static <T> Object2DArrayAssert<T> assertThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        static AbstractShortAssert<?> assertThat(@Independent @NotModified Short arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assertThat(@Independent @NotModified StringBuilder arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assertThat(@Independent @NotModified StringBuffer arg0) { return null; }
        @Independent
        static AbstractDateAssert<?> assertThat(@Independent @NotModified Date arg0) { return null; }
        @Independent
        static AbstractZonedDateTimeAssert<?> assertThat(@Independent @NotModified ZonedDateTime arg0) { return null; }
        @Independent
        static AbstractLocalDateTimeAssert<?> assertThat(@Independent @NotModified LocalDateTime arg0) { return null; }
        @Independent
        static AbstractOffsetDateTimeAssert<?> assertThat(@Independent @NotModified OffsetDateTime arg0) { return null; }
        @Independent
        static AbstractOffsetTimeAssert<?> assertThat(@Independent @NotModified OffsetTime arg0) { return null; }
        @Independent
        static AbstractLocalTimeAssert<?> assertThat(@Independent @NotModified LocalTime arg0) { return null; }
        @Independent
        static AbstractLocalDateAssert<?> assertThat(@Independent @NotModified LocalDate arg0) { return null; }
        @Independent
        static AbstractYearMonthAssert<?> assertThat(@Independent @NotModified YearMonth arg0) { return null; }
        @Independent
        static AbstractInstantAssert<?> assertThat(@Independent @NotModified Instant arg0) { return null; }
        @Independent
        static AbstractDurationAssert<?> assertThat(@Independent @NotModified Duration arg0) { return null; }
        @Independent
        static AbstractPeriodAssert<?> assertThat(@Independent @NotModified Period arg0) { return null; }
        @Independent
        static <T extends Throwable> AbstractThrowableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T extends Throwable> ThrowableTypeAssert<T> assertThatExceptionOfType(@Independent @NotModified Class<? extends T> arg0) { return null; }
    }

    //public class org.assertj.core.api.AssertionsForInterfaceTypes
    class AssertionsForInterfaceTypes$ {
        @Independent
        static <T> T assertThat(@Independent @NotModified AssertProvider<T> arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assertThat(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        static <ELEMENT> IterableAssert<ELEMENT> assertThat(@Independent @NotModified Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ELEMENT> IterableAssert<ELEMENT> assertThatIterable(@Independent @NotModified Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <E> AbstractCollectionAssert<?, Collection<? extends E>, E, ObjectAssert<E>> assertThat(@Independent @NotModified Collection<? extends E> arg0) { return null; }
        @Independent
        static <E> AbstractCollectionAssert<?, Collection<? extends E>, E, ObjectAssert<E>> assertThatCollection(@Independent @NotModified Collection<? extends E> arg0) { return null; }
        @Independent
        static <ELEMENT> ListAssert<ELEMENT> assertThat(@Independent @NotModified List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ELEMENT> ListAssert<ELEMENT> assertThatList(@Independent @NotModified List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ACTUAL extends Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> FactoryBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified Iterable<? extends ELEMENT> arg0, AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ClassBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified ACTUAL arg0, Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> FactoryBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified List<? extends ELEMENT> arg0, AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ELEMENT, ACTUAL extends List<? extends ELEMENT>, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ClassBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified List<? extends ELEMENT> arg0, Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static AbstractPathAssert<?> assertThat(@Independent @NotModified Path arg0) { return null; }
        @Independent
        static AbstractPathAssert<?> assertThatPath(@Independent @NotModified Path arg0) { return null; }
        @Independent
        static <K, V> MapAssert<K, V> assertThat(@Independent @NotModified Map<K, V> arg0) { return null; }
        @Independent
        static <T extends Comparable<? super T>> AbstractComparableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> AbstractUniversalComparableAssert<?, T> assertThatComparable(@Independent @NotModified Comparable<T> arg0) { return null; }
        @Independent
        static <T extends AssertDelegateTarget> T assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <RESULT> CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified CompletionStage<RESULT> arg0) { return null; }
    }

    //public class org.assertj.core.api.Java6Assertions
    class Java6Assertions$ {
        @Independent static AbstractBooleanArrayAssert<?> assertThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent static Boolean2DArrayAssert assertThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent static AbstractByteArrayAssert<?> assertThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent static Byte2DArrayAssert assertThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent static AbstractCharArrayAssert<?> assertThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent static Char2DArrayAssert assertThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent static AbstractDoubleArrayAssert<?> assertThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent static Double2DArrayAssert assertThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent static AbstractFloatArrayAssert<?> assertThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent static Float2DArrayAssert assertThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent static AbstractIntArrayAssert<?> assertThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent static Int2DArrayAssert assertThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent static AbstractLongArrayAssert<?> assertThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent static Long2DArrayAssert assertThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent static AbstractShortArrayAssert<?> assertThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent static Short2DArrayAssert assertThat(@Independent @NotModified short[][] arg0) { return null; }
        static void fail(String arg0, @NotModified Throwable arg1) { }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, String arg1, @NotModified Object... arg2) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> assertThatCode(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static Throwable catchThrowable(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        static <THROWABLE extends Throwable> THROWABLE catchThrowableOfType(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, Class<THROWABLE> arg1) { return null; }
        static <THROWABLE extends Throwable> THROWABLE catchThrowableOfType(Class<THROWABLE> arg0, @IgnoreModifications ThrowableAssert.ThrowingCallable arg1) { return null; }
        @Independent
        static AtomicBooleanAssert assertThat(@Independent @NotModified AtomicBoolean arg0) { return null; }
        @Independent
        static AtomicIntegerAssert assertThat(@Independent @NotModified AtomicInteger arg0) { return null; }
        @Independent
        static AtomicIntegerArrayAssert assertThat(@Independent @NotModified AtomicIntegerArray arg0) { return null; }
        @Independent
        static <OBJECT> AtomicIntegerFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static AtomicLongAssert assertThat(@Independent @NotModified AtomicLong arg0) { return null; }
        @Independent
        static AtomicLongArrayAssert assertThat(@Independent @NotModified AtomicLongArray arg0) { return null; }
        @Independent
        static <OBJECT> AtomicLongFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static <VALUE> AtomicReferenceAssert<VALUE> assertThat(@Independent @NotModified AtomicReference<VALUE> arg0) { return null; }
        @Independent
        static <ELEMENT> AtomicReferenceArrayAssert<ELEMENT> assertThat(@Independent @NotModified AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        static <FIELD, OBJECT> AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> assertThat(@Independent @NotModified AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        static <VALUE> AtomicMarkableReferenceAssert<VALUE> assertThat(@Independent @NotModified AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        static <VALUE> AtomicStampedReferenceAssert<VALUE> assertThat(@Independent @NotModified AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        static AbstractBigDecimalAssert<?> assertThat(@Independent @NotModified BigDecimal arg0) { return null; }
        @Independent
        static AbstractBigIntegerAssert<?> assertThat(@Independent @NotModified BigInteger arg0) { return null; }
        @Independent
        static AbstractUriAssert<?> assertThat(@Independent @NotModified URI arg0) { return null; }
        @Independent
        static AbstractUrlAssert<?> assertThat(@Independent @NotModified URL arg0) { return null; }
        @Independent
        static AbstractBooleanAssert<?> assertThat(@Independent @NotModified Boolean arg0) { return null; }
        @Independent
        static AbstractByteAssert<?> assertThat(@Independent @NotModified Byte arg0) { return null; }
        @Independent
        static AbstractCharacterAssert<?> assertThat(@Independent @NotModified Character arg0) { return null; }
        @Independent
        static AbstractClassAssert<?> assertThat(@Independent @NotModified Class<?> arg0) { return null; }
        @Independent
        static <T extends Comparable<? super T>> AbstractComparableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> AbstractIterableAssert<?, Iterable<? extends T>, T, ObjectAssert<T>> assertThat(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent
        static AbstractDoubleAssert<?> assertThat(@Independent @NotModified Double arg0) { return null; }
        @Independent
        static AbstractFileAssert<?> assertThat(@Independent @NotModified File arg0) { return null; }
        @Independent
        static <RESULT> AbstractFutureAssert<?, ? extends Future<? extends RESULT>, RESULT> assertThat(@Independent @NotModified Future<RESULT> arg0) { return null; }
        @Independent
        static AbstractFloatAssert<?> assertThat(@Independent @NotModified Float arg0) { return null; }
        @Independent
        static AbstractIntegerAssert<?> assertThat(@Independent @NotModified Integer arg0) { return null; }
        @Independent
        static <T> AbstractCollectionAssert<?, Collection<? extends T>, T, ObjectAssert<T>> assertThat(@Independent @NotModified Collection<? extends T> arg0) { return null; }
        @Independent
        static <T> AbstractListAssert<?, List<? extends T>, T, ObjectAssert<T>> assertThat(@Independent @NotModified List<? extends T> arg0) { return null; }
        @Independent
        static <ACTUAL extends Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> FactoryBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified Iterable<? extends ELEMENT> arg0, AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ClassBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified ACTUAL arg0, Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> FactoryBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified List<? extends ELEMENT> arg0, AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ELEMENT, ACTUAL extends List<? extends ELEMENT>, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ClassBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> assertThat(@Independent @NotModified List<? extends ELEMENT> arg0, Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static AbstractLongAssert<?> assertThat(@Independent @NotModified Long arg0) { return null; }
        @Independent
        static <T> AbstractObjectAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T extends AssertDelegateTarget> T assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> T assertThat(@Independent @NotModified AssertProvider<T> arg0) { return null; }
        @Independent
        static <T> AbstractObjectArrayAssert<?, T> assertThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        static <T> Object2DArrayAssert<T> assertThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        static <K, V> MapAssert<K, V> assertThat(@Independent @NotModified Map<K, V> arg0) { return null; }
        @Independent
        static AbstractShortAssert<?> assertThat(@Independent @NotModified Short arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assertThat(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assertThatCharSequence(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assertThat(@Independent @NotModified StringBuilder arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assertThat(@Independent @NotModified StringBuffer arg0) { return null; }
        @Independent
        static AbstractDateAssert<?> assertThat(@Independent @NotModified Date arg0) { return null; }
        @Independent
        static <T extends Throwable> AbstractThrowableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> AbstractObjectAssert<?, T> assertThatObject(@Independent @NotModified T arg0) { return null; }
    }

    //public class org.assertj.core.api.Java6BDDAssertions
    class Java6BDDAssertions$ {
        @Independent static AbstractBooleanArrayAssert<?> then(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent static Boolean2DArrayAssert then(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent static AbstractByteArrayAssert<?> then(@Independent @NotModified byte[] arg0) { return null; }
        @Independent static Byte2DArrayAssert then(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent static AbstractCharArrayAssert<?> then(@Independent @NotModified char[] arg0) { return null; }
        @Independent static Char2DArrayAssert then(@Independent @NotModified char[][] arg0) { return null; }
        @Independent static AbstractDoubleArrayAssert<?> then(@Independent @NotModified double[] arg0) { return null; }
        @Independent static Double2DArrayAssert then(@Independent @NotModified double[][] arg0) { return null; }
        @Independent static AbstractFloatArrayAssert<?> then(@Independent @NotModified float[] arg0) { return null; }
        @Independent static AbstractIntArrayAssert<?> then(@Independent @NotModified int[] arg0) { return null; }
        @Independent static Int2DArrayAssert then(@Independent @NotModified int[][] arg0) { return null; }
        @Independent static Float2DArrayAssert then(@Independent @NotModified float[][] arg0) { return null; }
        @Independent static AbstractLongArrayAssert<?> then(@Independent @NotModified long[] arg0) { return null; }
        @Independent static Long2DArrayAssert then(@Independent @NotModified long[][] arg0) { return null; }
        @Independent static AbstractShortArrayAssert<?> then(@Independent @NotModified short[] arg0) { return null; }
        @Independent static Short2DArrayAssert then(@Independent @NotModified short[][] arg0) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> thenThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> thenThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, String arg1, @NotModified Object... arg2) { return null; }
        @Independent AbstractThrowableAssert<?, ? extends Throwable> thenCode(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        static AtomicBooleanAssert then(@Independent @NotModified AtomicBoolean arg0) { return null; }
        @Independent
        static AtomicIntegerAssert then(@Independent @NotModified AtomicInteger arg0) { return null; }
        @Independent
        static AtomicIntegerArrayAssert then(@Independent @NotModified AtomicIntegerArray arg0) { return null; }
        @Independent
        static <OBJECT> AtomicIntegerFieldUpdaterAssert<OBJECT> then(@Independent @NotModified AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static AtomicLongAssert then(@Independent @NotModified AtomicLong arg0) { return null; }
        @Independent
        static AtomicLongArrayAssert then(@Independent @NotModified AtomicLongArray arg0) { return null; }
        @Independent
        static <OBJECT> AtomicLongFieldUpdaterAssert<OBJECT> then(@Independent @NotModified AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static <VALUE> AtomicReferenceAssert<VALUE> then(@Independent @NotModified AtomicReference<VALUE> arg0) { return null; }
        @Independent
        static <ELEMENT> AtomicReferenceArrayAssert<ELEMENT> then(@Independent @NotModified AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        static <FIELD, OBJECT> AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> then(@Independent @NotModified AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        static <VALUE> AtomicMarkableReferenceAssert<VALUE> then(@Independent @NotModified AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        static <VALUE> AtomicStampedReferenceAssert<VALUE> then(@Independent @NotModified AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        static AbstractBigDecimalAssert<?> then(@Independent @NotModified BigDecimal arg0) { return null; }
        @Independent
        static AbstractBigIntegerAssert<?> then(@Independent @NotModified BigInteger arg0) { return null; }
        @Independent
        static AbstractBooleanAssert<?> then(@Independent @NotModified Boolean arg0) { return null; }
        @Independent
        static AbstractByteAssert<?> then(@Independent @NotModified Byte arg0) { return null; }
        @Independent
        static AbstractCharacterAssert<?> then(@Independent @NotModified Character arg0) { return null; }
        @Independent
        static AbstractClassAssert<?> then(@Independent @NotModified Class<?> arg0) { return null; }
        @Independent
        static <T> AbstractCollectionAssert<?, Collection<? extends T>, T, ObjectAssert<T>> then(@Independent @NotModified Collection<? extends T> arg0) { return null; }
        @Independent
        static <T extends Comparable<? super T>> AbstractComparableAssert<?, T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> AbstractIterableAssert<?, Iterable<? extends T>, T, ObjectAssert<T>> then(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent
        static <ACTUAL extends Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> FactoryBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified Iterable<? extends ELEMENT> arg0, AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends Iterable<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ClassBasedNavigableIterableAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified ACTUAL arg0, Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ACTUAL extends List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> FactoryBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified List<? extends ELEMENT> arg0, AssertFactory<ELEMENT, ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static <ELEMENT, ACTUAL extends List<? extends ELEMENT>, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> ClassBasedNavigableListAssert<?, ACTUAL, ELEMENT, ELEMENT_ASSERT> then(@Independent @NotModified List<? extends ELEMENT> arg0, Class<ELEMENT_ASSERT> arg1) { return null; }
        @Independent
        static AbstractDoubleAssert<?> then(@Independent @NotModified Double arg0) { return null; }
        @Independent
        static AbstractFileAssert<?> then(@Independent @NotModified File arg0) { return null; }
        @Independent
        static <RESULT> AbstractFutureAssert<?, ? extends Future<? extends RESULT>, RESULT> then(@Independent @NotModified Future<RESULT> arg0) { return null; }
        @Independent
        static AbstractFloatAssert<?> then(@Independent @NotModified Float arg0) { return null; }
        @Independent
        static AbstractIntegerAssert<?> then(@Independent @NotModified Integer arg0) { return null; }
        @Independent
        static <T> AbstractListAssert<?, List<? extends T>, T, ObjectAssert<T>> then(@Independent @NotModified List<? extends T> arg0) { return null; }
        @Independent
        static AbstractLongAssert<?> then(@Independent @NotModified Long arg0) { return null; }
        @Independent
        static <T> AbstractObjectAssert<?, T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> AbstractObjectArrayAssert<?, T> then(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        static <T> Object2DArrayAssert<T> then(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        static <K, V> MapAssert<K, V> then(@Independent @NotModified Map<K, V> arg0) { return null; }
        @Independent
        static AbstractShortAssert<?> then(@Independent @NotModified Short arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> then(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> thenCharSequence(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> then(@Independent @NotModified StringBuilder arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> then(@Independent @NotModified StringBuffer arg0) { return null; }
        @Independent
        static AbstractDateAssert<?> then(@Independent @NotModified Date arg0) { return null; }
        @Independent
        static <T extends Throwable> AbstractThrowableAssert<?, T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static AbstractUriAssert<?> then(@Independent @NotModified URI arg0) { return null; }
        @Independent
        static AbstractUrlAssert<?> then(@Independent @NotModified URL arg0) { return null; }
        @Independent
        static <T extends AssertDelegateTarget> T then(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> T then(@Independent @NotModified AssertProvider<T> arg0) { return null; }
        @Independent
        static <T> AbstractObjectAssert<?, T> thenObject(@Independent @NotModified T arg0) { return null; }
    }

    //public interface org.assertj.core.api.StandardSoftAssertionsProvider
    class StandardSoftAssertionsProvider$ {
        @Independent
        PathAssert assertThat(@Independent @NotModified Path arg0) { return null; }
        @Independent
        AbstractPathAssert<?> assertThatPath(@Independent @NotModified Path arg0) { return null; }
        @Independent
        <VALUE> OptionalAssert<VALUE> assertThat(@Independent @NotModified Optional<VALUE> arg0) { return null; }
        @Independent
        OptionalDoubleAssert assertThat(@Independent @NotModified OptionalDouble arg0) { return null; }
        @Independent
        OptionalLongAssert assertThat(@Independent @NotModified OptionalLong arg0) { return null; }
        @Independent
        OptionalIntAssert assertThat(@Independent @NotModified OptionalInt arg0) { return null; }
        @Independent
        LocalDateAssert assertThat(@Independent @NotModified LocalDate arg0) { return null; }
        @Independent
        YearMonthAssert assertThat(@Independent @NotModified YearMonth arg0) { return null; }
        @Independent
        LocalDateTimeAssert assertThat(@Independent @NotModified LocalDateTime arg0) { return null; }
        @Independent
        ZonedDateTimeAssert assertThat(@Independent @NotModified ZonedDateTime arg0) { return null; }
        @Independent
        LocalTimeAssert assertThat(@Independent @NotModified LocalTime arg0) { return null; }
        @Independent
        OffsetTimeAssert assertThat(@Independent @NotModified OffsetTime arg0) { return null; }
        @Independent
        OffsetDateTimeAssert assertThat(@Independent @NotModified OffsetDateTime arg0) { return null; }
        @Independent
        InstantAssert assertThat(@Independent @NotModified Instant arg0) { return null; }
        @Independent
        DurationAssert assertThat(@Independent @NotModified Duration arg0) { return null; }
        @Independent
        PeriodAssert assertThat(@Independent @NotModified Period arg0) { return null; }
        @Independent
        <RESULT> CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        <RESULT> CompletableFutureAssert<RESULT> assertThat(@Independent @NotModified CompletionStage<RESULT> arg0) { return null; }
        @Independent
        LongAdderAssert assertThat(@Independent @NotModified LongAdder arg0) { return null; }
        @Independent
        TemporalAssert assertThatTemporal(@Independent @NotModified Temporal arg0) { return null; }
    }

    //public interface org.assertj.core.api.Java6StandardSoftAssertionsProvider
    class Java6StandardSoftAssertionsProvider$ {
        @Independent BooleanArrayAssert assertThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent Boolean2DArrayAssert assertThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent ByteArrayAssert assertThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent Byte2DArrayAssert assertThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent CharArrayAssert assertThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent Char2DArrayAssert assertThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent DoubleArrayAssert assertThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent Double2DArrayAssert assertThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent FloatArrayAssert assertThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent Float2DArrayAssert assertThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent IntArrayAssert assertThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent Int2DArrayAssert assertThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent LongArrayAssert assertThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent Long2DArrayAssert assertThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent ShortArrayAssert assertThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent Short2DArrayAssert assertThat(@Independent @NotModified short[][] arg0) { return null; }
        @Independent AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent AbstractThrowableAssert<?, ? extends Throwable> assertThatThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, String arg1, @NotModified Object... arg2) { return null; }
        @Independent AbstractThrowableAssert<?, ? extends Throwable> assertThatCode(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        BigDecimalAssert assertThat(@Independent @NotModified BigDecimal arg0) { return null; }
        @Independent
        BigIntegerAssert assertThat(@Independent @NotModified BigInteger arg0) { return null; }
        @Independent
        BooleanAssert assertThat(@Independent @NotModified Boolean arg0) { return null; }
        @Independent
        ByteAssert assertThat(@Independent @NotModified Byte arg0) { return null; }
        @Independent
        CharacterAssert assertThat(@Independent @NotModified Character arg0) { return null; }
        @Independent
        ClassAssert assertThat(@Independent @NotModified Class<?> arg0) { return null; }
        @Independent
        <T> CollectionAssert<T> assertThat(@Independent @NotModified Collection<? extends T> arg0) { return null; }
        @Independent
        <T> CollectionAssert<T> assertThatCollection(@Independent @NotModified Collection<? extends T> arg0) { return null; }
        @Independent
        <T extends Comparable<? super T>> AbstractComparableAssert<?, T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> AbstractUniversalComparableAssert<?, T> assertThatComparable(@Independent @NotModified Comparable<T> arg0) { return null; }
        @Independent
        <T> IterableAssert<T> assertThat(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent
        <ELEMENT> IterableAssert<ELEMENT> assertThatIterable(@Independent @NotModified Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        DoubleAssert assertThat(@Independent @NotModified Double arg0) { return null; }
        @Independent
        FileAssert assertThat(@Independent @NotModified File arg0) { return null; }
        @Independent
        <RESULT> FutureAssert<RESULT> assertThat(@Independent @NotModified Future<RESULT> arg0) { return null; }
        @Independent
        FloatAssert assertThat(@Independent @NotModified Float arg0) { return null; }
        @Independent
        IntegerAssert assertThat(@Independent @NotModified Integer arg0) { return null; }
        @Independent
        <T> ListAssert<T> assertThat(@Independent @NotModified List<? extends T> arg0) { return null; }
        @Independent
        <ELEMENT> ListAssert<ELEMENT> assertThatList(@Independent @NotModified List<? extends ELEMENT> arg0) { return null; }
        @Independent
        LongAssert assertThat(@Independent @NotModified Long arg0) { return null; }
        @Independent
        <T> ObjectAssert<T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> ObjectArrayAssert<T> assertThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        <T> Object2DArrayAssert<T> assertThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        <K, V> MapAssert<K, V> assertThat(@Independent @NotModified Map<K, V> arg0) { return null; }
        @Independent
        ShortAssert assertThat(@Independent @NotModified Short arg0) { return null; }
        @Independent
        CharSequenceAssert assertThat(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        CharSequenceAssert assertThatCharSequence(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        CharSequenceAssert assertThat(@Independent @NotModified StringBuilder arg0) { return null; }
        @Independent
        CharSequenceAssert assertThat(@Independent @NotModified StringBuffer arg0) { return null; }
        @Independent
        DateAssert assertThat(@Independent @NotModified Date arg0) { return null; }
        @Independent
        AtomicBooleanAssert assertThat(@Independent @NotModified AtomicBoolean arg0) { return null; }
        @Independent
        AtomicIntegerAssert assertThat(@Independent @NotModified AtomicInteger arg0) { return null; }
        @Independent
        AtomicIntegerArrayAssert assertThat(@Independent @NotModified AtomicIntegerArray arg0) { return null; }
        @Independent
        <OBJECT> AtomicIntegerFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        AtomicLongAssert assertThat(@Independent @NotModified AtomicLong arg0) { return null; }
        @Independent
        AtomicLongArrayAssert assertThat(@Independent @NotModified AtomicLongArray arg0) { return null; }
        @Independent
        <OBJECT> AtomicLongFieldUpdaterAssert<OBJECT> assertThat(@Independent @NotModified AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        <VALUE> AtomicReferenceAssert<VALUE> assertThat(@Independent @NotModified AtomicReference<VALUE> arg0) { return null; }
        @Independent
        <ELEMENT> AtomicReferenceArrayAssert<ELEMENT> assertThat(@Independent @NotModified AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        <FIELD, OBJECT> AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> assertThat(@Independent @NotModified AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        <VALUE> AtomicMarkableReferenceAssert<VALUE> assertThat(@Independent @NotModified AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        <VALUE> AtomicStampedReferenceAssert<VALUE> assertThat(@Independent @NotModified AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        <T extends Throwable> ThrowableAssert<T> assertThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> ObjectAssert<T> assertThatObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        UriAssert assertThat(@Independent @NotModified URI arg0) { return null; }
        @Independent
        AbstractUrlAssert<?> assertThat(@Independent @NotModified URL arg0) { return null; }
        @Independent
        <T extends Throwable> ThrowableTypeAssert<T> assertThatExceptionOfType(@Independent @NotModified Class<T> arg0) { return null; }
    }

    //public interface org.assertj.core.api.BDDSoftAssertionsProvider
    class BDDSoftAssertionsProvider$ {
        @Independent
        PathAssert then(@Independent @NotModified Path arg0) { return null; }
        @Independent
        AbstractPathAssert<?> thenPath(@Independent @NotModified Path arg0) { return null; }
        @Independent
        <VALUE> OptionalAssert<VALUE> then(@Independent @NotModified Optional<VALUE> arg0) { return null; }
        @Independent
        OptionalDoubleAssert then(@Independent @NotModified OptionalDouble arg0) { return null; }
        @Independent
        OptionalIntAssert then(@Independent @NotModified OptionalInt arg0) { return null; }
        @Independent
        OptionalLongAssert then(@Independent @NotModified OptionalLong arg0) { return null; }
        @Independent
        TemporalAssert thenTemporal(@Independent @NotModified Temporal arg0) { return null; }
        @Independent
        LocalDateAssert then(@Independent @NotModified LocalDate arg0) { return null; }
        @Independent
        YearMonthAssert then(@Independent @NotModified YearMonth arg0) { return null; }
        @Independent
        LocalDateTimeAssert then(@Independent @NotModified LocalDateTime arg0) { return null; }
        @Independent
        ZonedDateTimeAssert then(@Independent @NotModified ZonedDateTime arg0) { return null; }
        @Independent
        LocalTimeAssert then(@Independent @NotModified LocalTime arg0) { return null; }
        @Independent
        OffsetTimeAssert then(@Independent @NotModified OffsetTime arg0) { return null; }
        @Independent
        OffsetDateTimeAssert then(@Independent @NotModified OffsetDateTime arg0) { return null; }
        @Independent
        InstantAssert then(@Independent @NotModified Instant arg0) { return null; }
        @Independent
        DurationAssert then(@Independent @NotModified Duration arg0) { return null; }
        @Independent
        PeriodAssert then(@Independent @NotModified Period arg0) { return null; }
        @Independent
        <RESULT> CompletableFutureAssert<RESULT> then(@Independent @NotModified CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        <RESULT> CompletableFutureAssert<RESULT> then(@Independent @NotModified CompletionStage<RESULT> arg0) { return null; }
        @Independent
        LongAdderAssert then(@Independent @NotModified LongAdder arg0) { return null; }
    }

    //public interface org.assertj.core.api.Java6BDDSoftAssertionsProvider
    class Java6BDDSoftAssertionsProvider$ {
        @Independent BooleanArrayAssert then(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent Boolean2DArrayAssert then(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent ByteArrayAssert then(@Independent @NotModified byte[] arg0) { return null; }
        @Independent Byte2DArrayAssert then(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent CharArrayAssert then(@Independent @NotModified char[] arg0) { return null; }
        @Independent Char2DArrayAssert then(@Independent @NotModified char[][] arg0) { return null; }
        @Independent DoubleArrayAssert then(@Independent @NotModified double[] arg0) { return null; }
        @Independent Double2DArrayAssert then(@Independent @NotModified double[][] arg0) { return null; }
        @Independent FloatArrayAssert then(@Independent @NotModified float[] arg0) { return null; }
        @Independent Float2DArrayAssert then(@Independent @NotModified float[][] arg0) { return null; }
        @Independent IntArrayAssert then(@Independent @NotModified int[] arg0) { return null; }
        @Independent Int2DArrayAssert then(@Independent @NotModified int[][] arg0) { return null; }
        @Independent LongArrayAssert then(@Independent @NotModified long[] arg0) { return null; }
        @Independent Long2DArrayAssert then(@Independent @NotModified long[][] arg0) { return null; }
        @Independent ShortArrayAssert then(@Independent @NotModified short[] arg0) { return null; }
        @Independent Short2DArrayAssert then(@Independent @NotModified short[][] arg0) { return null; }
        @Independent AbstractThrowableAssert<?, ? extends Throwable> thenThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent AbstractThrowableAssert<?, ? extends Throwable> thenThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0, String arg1, @NotModified Object... arg2) { return null; }
        @Independent AbstractThrowableAssert<?, ? extends Throwable> thenCode(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        BigDecimalAssert then(@Independent @NotModified BigDecimal arg0) { return null; }
        @Independent
        BigIntegerAssert then(@Independent @NotModified BigInteger arg0) { return null; }
        @Independent
        BooleanAssert then(@Independent @NotModified Boolean arg0) { return null; }
        @Independent
        ByteAssert then(@Independent @NotModified Byte arg0) { return null; }
        @Independent
        CharacterAssert then(@Independent @NotModified Character arg0) { return null; }
        @Independent
        ClassAssert then(@Independent @NotModified Class<?> arg0) { return null; }
        @Independent
        <T> CollectionAssert<T> then(@Independent @NotModified Collection<? extends T> arg0) { return null; }
        @Independent
        <E> AbstractCollectionAssert<?, Collection<? extends E>, E, ObjectAssert<E>> thenCollection(@Independent @NotModified Collection<? extends E> arg0) { return null; }
        @Independent
        <T extends Comparable<? super T>> AbstractComparableAssert<?, T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> AbstractUniversalComparableAssert<?, T> thenComparable(@Independent @NotModified Comparable<T> arg0) { return null; }
        @Independent
        <T> IterableAssert<T> then(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent
        <ELEMENT> IterableAssert<ELEMENT> thenIterable(@Independent @NotModified Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        DoubleAssert then(@Independent @NotModified Double arg0) { return null; }
        @Independent
        FileAssert then(@Independent @NotModified File arg0) { return null; }
        @Independent
        <RESULT> FutureAssert<RESULT> then(@Independent @NotModified Future<RESULT> arg0) { return null; }
        @Independent
        FloatAssert then(@Independent @NotModified Float arg0) { return null; }
        @Independent
        IntegerAssert then(@Independent @NotModified Integer arg0) { return null; }
        @Independent
        <T> ListAssert<T> then(@Independent @NotModified List<? extends T> arg0) { return null; }
        @Independent
        <ELEMENT> ListAssert<ELEMENT> thenList(@Independent @NotModified List<? extends ELEMENT> arg0) { return null; }
        @Independent
        LongAssert then(@Independent @NotModified Long arg0) { return null; }
        @Independent
        <T> ObjectAssert<T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> ObjectArrayAssert<T> then(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        <T> Object2DArrayAssert<T> then(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        <K, V> MapAssert<K, V> then(@Independent @NotModified Map<K, V> arg0) { return null; }
        @Independent
        ShortAssert then(@Independent @NotModified Short arg0) { return null; }
        @Independent
        CharSequenceAssert then(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        CharSequenceAssert thenCharSequence(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        CharSequenceAssert then(@Independent @NotModified StringBuilder arg0) { return null; }
        @Independent
        CharSequenceAssert then(@Independent @NotModified StringBuffer arg0) { return null; }
        @Independent
        DateAssert then(@Independent @NotModified Date arg0) { return null; }
        @Independent
        AtomicBooleanAssert then(@Independent @NotModified AtomicBoolean arg0) { return null; }
        @Independent
        AtomicIntegerAssert then(@Independent @NotModified AtomicInteger arg0) { return null; }
        @Independent
        AtomicIntegerArrayAssert then(@Independent @NotModified AtomicIntegerArray arg0) { return null; }
        @Independent
        <OBJECT> AtomicIntegerFieldUpdaterAssert<OBJECT> then(@Independent @NotModified AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        AtomicLongAssert then(@Independent @NotModified AtomicLong arg0) { return null; }
        @Independent
        AtomicLongArrayAssert then(@Independent @NotModified AtomicLongArray arg0) { return null; }
        @Independent
        <OBJECT> AtomicLongFieldUpdaterAssert<OBJECT> then(@Independent @NotModified AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        <VALUE> AtomicReferenceAssert<VALUE> then(@Independent @NotModified AtomicReference<VALUE> arg0) { return null; }
        @Independent
        <ELEMENT> AtomicReferenceArrayAssert<ELEMENT> then(@Independent @NotModified AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        <FIELD, OBJECT> AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> then(@Independent @NotModified AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        <VALUE> AtomicMarkableReferenceAssert<VALUE> then(@Independent @NotModified AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        <VALUE> AtomicStampedReferenceAssert<VALUE> then(@Independent @NotModified AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        <T extends Throwable> ThrowableAssert<T> then(@Independent @NotModified T arg0) { return null; }
        @Independent
        <T> ObjectAssert<T> thenObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        UriAssert then(@Independent @NotModified URI arg0) { return null; }
        @Independent
        AbstractUrlAssert<?> then(@Independent @NotModified URL arg0) { return null; }
        @Independent
        <T extends Throwable> ThrowableTypeAssert<T> thenExceptionOfType(@Independent @NotModified Class<T> arg0) { return null; }
    }

    //public class org.assertj.core.api.Assumptions
    class Assumptions$ {
        @Independent static AbstractBooleanArrayAssert<?> assumeThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent static Boolean2DArrayAssert assumeThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent static AbstractByteArrayAssert<?> assumeThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent static Byte2DArrayAssert assumeThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent static AbstractCharArrayAssert<?> assumeThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent static Char2DArrayAssert assumeThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent static AbstractShortArrayAssert<?> assumeThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent static Short2DArrayAssert assumeThat(@Independent @NotModified short[][] arg0) { return null; }
        @Independent static AbstractIntArrayAssert<?> assumeThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent static Int2DArrayAssert assumeThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent static AbstractLongArrayAssert<?> assumeThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent static Long2DArrayAssert assumeThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent static AbstractFloatArrayAssert<?> assumeThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent static Float2DArrayAssert assumeThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent static AbstractDoubleArrayAssert<?> assumeThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent static Double2DArrayAssert assumeThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> assumeThatThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent static AbstractThrowableAssert<?, ? extends Throwable> assumeThatCode(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        static <T> ObjectAssert<T> assumeThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static AbstractBigDecimalAssert<?> assumeThat(@Independent @NotModified BigDecimal arg0) { return null; }
        @Independent
        static AbstractBigIntegerAssert<?> assumeThat(@Independent @NotModified BigInteger arg0) { return null; }
        @Independent
        static AbstractUriAssert<?> assumeThat(@Independent @NotModified URI arg0) { return null; }
        @Independent
        static AbstractUrlAssert<?> assumeThat(@Independent @NotModified URL arg0) { return null; }
        @Independent
        static AbstractBooleanAssert<?> assumeThat(@Independent @NotModified Boolean arg0) { return null; }
        @Independent
        static AbstractByteAssert<?> assumeThat(@Independent @NotModified Byte arg0) { return null; }
        @Independent
        static AbstractCharacterAssert<?> assumeThat(@Independent @NotModified Character arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assumeThat(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assumeThatCharSequence(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assumeThat(@Independent @NotModified StringBuilder arg0) { return null; }
        @Independent
        static AbstractCharSequenceAssert<?, ? extends CharSequence> assumeThat(@Independent @NotModified StringBuffer arg0) { return null; }
        @Independent
        static AbstractShortAssert<?> assumeThat(@Independent @NotModified Short arg0) { return null; }
        @Independent
        static AbstractIntegerAssert<?> assumeThat(@Independent @NotModified Integer arg0) { return null; }
        @Independent
        static AbstractLongAssert<?> assumeThat(@Independent @NotModified Long arg0) { return null; }
        @Independent
        static AbstractFloatAssert<?> assumeThat(@Independent @NotModified Float arg0) { return null; }
        @Independent
        static AbstractDoubleAssert<?> assumeThat(@Independent @NotModified Double arg0) { return null; }
        @Independent
        static AtomicBooleanAssert assumeThat(@Independent @NotModified AtomicBoolean arg0) { return null; }
        @Independent
        static AtomicIntegerAssert assumeThat(@Independent @NotModified AtomicInteger arg0) { return null; }
        @Independent
        static AtomicIntegerArrayAssert assumeThat(@Independent @NotModified AtomicIntegerArray arg0) { return null; }
        @Independent
        static <OBJECT> AtomicIntegerFieldUpdaterAssert<OBJECT> assumeThat(@Independent @NotModified AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static LongAdderAssert assumeThat(@Independent @NotModified LongAdder arg0) { return null; }
        @Independent
        static AtomicLongAssert assumeThat(@Independent @NotModified AtomicLong arg0) { return null; }
        @Independent
        static AtomicLongArrayAssert assumeThat(@Independent @NotModified AtomicLongArray arg0) { return null; }
        @Independent
        static <OBJECT> AtomicLongFieldUpdaterAssert<OBJECT> assumeThat(@Independent @NotModified AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        static <VALUE> AtomicReferenceAssert<VALUE> assumeThat(@Independent @NotModified AtomicReference<VALUE> arg0) { return null; }
        @Independent
        static <ELEMENT> AtomicReferenceArrayAssert<ELEMENT> assumeThat(@Independent @NotModified AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        static <FIELD, OBJECT> AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> assumeThat(@Independent @NotModified AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        static <VALUE> AtomicMarkableReferenceAssert<VALUE> assumeThat(@Independent @NotModified AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        static <VALUE> AtomicStampedReferenceAssert<VALUE> assumeThat(@Independent @NotModified AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        static ClassAssert assumeThat(@Independent @NotModified Class<?> arg0) { return null; }
        @Independent
        static AbstractDateAssert<?> assumeThat(@Independent @NotModified Date arg0) { return null; }
        @Independent
        static AbstractFileAssert<?> assumeThat(@Independent @NotModified File arg0) { return null; }
        @Independent
        static AbstractPathAssert<?> assumeThat(@Independent @NotModified Path arg0) { return null; }
        @Independent
        static AbstractPathAssert<?> assumeThatPath(@Independent @NotModified Path arg0) { return null; }
        @Independent
        static <RESULT> AbstractFutureAssert<?, ? extends Future<? extends RESULT>, RESULT> assumeThat(@Independent @NotModified Future<RESULT> arg0) { return null; }
        @Independent
        static <ELEMENT> IterableAssert<ELEMENT> assumeThat(@Independent @NotModified Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ELEMENT> IterableAssert<ELEMENT> assumeThatIterable(@Independent @NotModified Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <E> AbstractCollectionAssert<?, Collection<? extends E>, E, ObjectAssert<E>> assumeThat(@Independent @NotModified Collection<? extends E> arg0) { return null; }
        @Independent
        static <E> AbstractCollectionAssert<?, Collection<? extends E>, E, ObjectAssert<E>> assumeThatCollection(@Independent @NotModified Collection<? extends E> arg0) { return null; }
        @Independent
        static <ELEMENT> FactoryBasedNavigableListAssert<ListAssert<ELEMENT>, List<? extends ELEMENT>, ELEMENT, ObjectAssert<ELEMENT>> assumeThat(@Independent @NotModified List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <ELEMENT> FactoryBasedNavigableListAssert<ListAssert<ELEMENT>, List<? extends ELEMENT>, ELEMENT, ObjectAssert<ELEMENT>> assumeThatList(@Independent @NotModified List<? extends ELEMENT> arg0) { return null; }
        @Independent
        static <T> ObjectArrayAssert<T> assumeThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        static <T> Object2DArrayAssert<T> assumeThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        static <K, V> MapAssert<K, V> assumeThat(@Independent @NotModified Map<K, V> arg0) { return null; }
        @Independent
        static <T extends Comparable<? super T>> AbstractComparableAssert<?, T> assumeThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T> AbstractUniversalComparableAssert<?, T> assumeThatComparable(@Independent @NotModified Comparable<T> arg0) { return null; }
        @Independent
        static <T extends Throwable> AbstractThrowableAssert<?, T> assumeThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <T extends Throwable> ThrowableTypeAssert<T> assumeThatExceptionOfType(@Independent @NotModified Class<? extends T> arg0) { return null; }
        @Independent
        static <T> ObjectAssert<T> assumeThatObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        static <RESULT> CompletableFutureAssert<RESULT> assumeThat(@Independent @NotModified CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        static <RESULT> CompletableFutureAssert<RESULT> assumeThat(@Independent @NotModified CompletionStage<RESULT> arg0) { return null; }
        @Independent
        static <VALUE> OptionalAssert<VALUE> assumeThat(@Independent @NotModified Optional<VALUE> arg0) { return null; }
        @Independent
        static OptionalDoubleAssert assumeThat(@Independent @NotModified OptionalDouble arg0) { return null; }
        @Independent
        static OptionalIntAssert assumeThat(@Independent @NotModified OptionalInt arg0) { return null; }
        @Independent
        static OptionalLongAssert assumeThat(@Independent @NotModified OptionalLong arg0) { return null; }
        @Independent
        static TemporalAssert assumeThatTemporal(@Independent @NotModified Temporal arg0) { return null; }
        @Independent
        static AbstractZonedDateTimeAssert<?> assumeThat(@Independent @NotModified ZonedDateTime arg0) { return null; }
        @Independent
        static AbstractLocalDateTimeAssert<?> assumeThat(@Independent @NotModified LocalDateTime arg0) { return null; }
        @Independent
        static AbstractOffsetDateTimeAssert<?> assumeThat(@Independent @NotModified OffsetDateTime arg0) { return null; }
        @Independent
        static AbstractOffsetTimeAssert<?> assumeThat(@Independent @NotModified OffsetTime arg0) { return null; }
        @Independent
        static AbstractLocalTimeAssert<?> assumeThat(@Independent @NotModified LocalTime arg0) { return null; }
        @Independent
        static AbstractLocalDateAssert<?> assumeThat(@Independent @NotModified LocalDate arg0) { return null; }
        @Independent
        static AbstractYearMonthAssert<?> assumeThat(@Independent @NotModified YearMonth arg0) { return null; }
        @Independent
        static AbstractInstantAssert<?> assumeThat(@Independent @NotModified Instant arg0) { return null; }
        @Independent
        static AbstractDurationAssert<?> assumeThat(@Independent @NotModified Duration arg0) { return null; }
        @Independent
        static AbstractPeriodAssert<?> assumeThat(@Independent @NotModified Period arg0) { return null; }
    }

    //public interface org.assertj.core.api.WithAssumptions
    class WithAssumptions$ {
        @Independent AbstractLongArrayAssert<?> assumeThat(@Independent @NotModified long[] arg0) { return null; }
        @Independent Long2DArrayAssert assumeThat(@Independent @NotModified long[][] arg0) { return null; }
        @Independent AbstractShortArrayAssert<?> assumeThat(@Independent @NotModified short[] arg0) { return null; }
        @Independent Short2DArrayAssert assumeThat(@Independent @NotModified short[][] arg0) { return null; }
        @Independent AbstractCharArrayAssert<?> assumeThat(@Independent @NotModified char[] arg0) { return null; }
        @Independent Char2DArrayAssert assumeThat(@Independent @NotModified char[][] arg0) { return null; }
        @Independent AbstractBooleanArrayAssert<?> assumeThat(@Independent @NotModified boolean[] arg0) { return null; }
        @Independent Boolean2DArrayAssert assumeThat(@Independent @NotModified boolean[][] arg0) { return null; }
        @Independent AbstractByteArrayAssert<?> assumeThat(@Independent @NotModified byte[] arg0) { return null; }
        @Independent Byte2DArrayAssert assumeThat(@Independent @NotModified byte[][] arg0) { return null; }
        @Independent AbstractIntArrayAssert<?> assumeThat(@Independent @NotModified int[] arg0) { return null; }
        @Independent Int2DArrayAssert assumeThat(@Independent @NotModified int[][] arg0) { return null; }
        @Independent Float2DArrayAssert assumeThat(@Independent @NotModified float[][] arg0) { return null; }
        @Independent AbstractFloatArrayAssert<?> assumeThat(@Independent @NotModified float[] arg0) { return null; }
        @Independent AbstractDoubleArrayAssert<?> assumeThat(@Independent @NotModified double[] arg0) { return null; }
        @Independent Double2DArrayAssert assumeThat(@Independent @NotModified double[][] arg0) { return null; }
        @Independent AbstractThrowableAssert<?, ? extends Throwable> assumeThatThrownBy(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent AbstractThrowableAssert<?, ? extends Throwable> assumeThatCode(@IgnoreModifications ThrowableAssert.ThrowingCallable arg0) { return null; }
        @Independent
        <T> ObjectArrayAssert<T> assumeThat(@Independent @NotModified T[] arg0) { return null; }
        @Independent
        <T> Object2DArrayAssert<T> assumeThat(@Independent @NotModified T[][] arg0) { return null; }
        @Independent
        <K, V> MapAssert<K, V> assumeThat(@Independent @NotModified Map<K, V> arg0) { return null; }
        @Independent
        AbstractLongAssert<?> assumeThat(@Independent @NotModified Long arg0) { return null; }
        @Independent
        <T> ObjectAssert<T> assumeThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        AbstractDateAssert<?> assumeThat(@Independent @NotModified Date arg0) { return null; }
        @Independent
        <T extends Throwable> AbstractThrowableAssert<?, T> assumeThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        AbstractBigDecimalAssert<?> assumeThat(@Independent @NotModified BigDecimal arg0) { return null; }
        @Independent
        AbstractBigIntegerAssert<?> assumeThat(@Independent @NotModified BigInteger arg0) { return null; }
        @Independent
        AtomicBooleanAssert assumeThat(@Independent @NotModified AtomicBoolean arg0) { return null; }
        @Independent
        AtomicIntegerAssert assumeThat(@Independent @NotModified AtomicInteger arg0) { return null; }
        @Independent
        AtomicIntegerArrayAssert assumeThat(@Independent @NotModified AtomicIntegerArray arg0) { return null; }
        @Independent
        <OBJECT> AtomicIntegerFieldUpdaterAssert<OBJECT> assumeThat(@Independent @NotModified AtomicIntegerFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        LongAdderAssert assumeThat(@Independent @NotModified LongAdder arg0) { return null; }
        @Independent
        AtomicLongAssert assumeThat(@Independent @NotModified AtomicLong arg0) { return null; }
        @Independent
        AtomicLongArrayAssert assumeThat(@Independent @NotModified AtomicLongArray arg0) { return null; }
        @Independent
        <OBJECT> AtomicLongFieldUpdaterAssert<OBJECT> assumeThat(@Independent @NotModified AtomicLongFieldUpdater<OBJECT> arg0) { return null; }
        @Independent
        <VALUE> AtomicReferenceAssert<VALUE> assumeThat(@Independent @NotModified AtomicReference<VALUE> arg0) { return null; }
        @Independent
        <ELEMENT> AtomicReferenceArrayAssert<ELEMENT> assumeThat(@Independent @NotModified AtomicReferenceArray<ELEMENT> arg0) { return null; }
        @Independent
        <FIELD, OBJECT> AtomicReferenceFieldUpdaterAssert<FIELD, OBJECT> assumeThat(@Independent @NotModified AtomicReferenceFieldUpdater<OBJECT, FIELD> arg0) { return null; }
        @Independent
        <VALUE> AtomicMarkableReferenceAssert<VALUE> assumeThat(@Independent @NotModified AtomicMarkableReference<VALUE> arg0) { return null; }
        @Independent
        <VALUE> AtomicStampedReferenceAssert<VALUE> assumeThat(@Independent @NotModified AtomicStampedReference<VALUE> arg0) { return null; }
        @Independent
        AbstractCharSequenceAssert<?, ? extends CharSequence> assumeThat(@Independent @NotModified CharSequence arg0) { return null; }
        @Independent
        AbstractCharSequenceAssert<?, ? extends CharSequence> assumeThat(@Independent @NotModified StringBuilder arg0) { return null; }
        @Independent
        AbstractCharSequenceAssert<?, ? extends CharSequence> assumeThat(@Independent @NotModified StringBuffer arg0) { return null; }
        @Independent
        AbstractShortAssert<?> assumeThat(@Independent @NotModified Short arg0) { return null; }
        @Independent
        ClassAssert assumeThat(@Independent @NotModified Class<?> arg0) { return null; }
        @Independent
        AbstractCharacterAssert<?> assumeThat(@Independent @NotModified Character arg0) { return null; }
        @Independent
        <T extends Comparable<? super T>> AbstractComparableAssert<?, T> assumeThat(@Independent @NotModified T arg0) { return null; }
        @Independent
        <ELEMENT> IterableAssert<ELEMENT> assumeThat(@Independent @NotModified Iterable<? extends ELEMENT> arg0) { return null; }
        @Independent
        AbstractBooleanAssert<?> assumeThat(@Independent @NotModified Boolean arg0) { return null; }
        @Independent
        AbstractByteAssert<?> assumeThat(@Independent @NotModified Byte arg0) { return null; }
        @Independent
        AbstractFileAssert<?> assumeThat(@Independent @NotModified File arg0) { return null; }
        @Independent
        <RESULT> AbstractFutureAssert<?, ? extends Future<? extends RESULT>, RESULT> assumeThat(@Independent @NotModified Future<RESULT> arg0) { return null; }
        @Independent
        AbstractPathAssert<?> assumeThat(@Independent @NotModified Path arg0) { return null; }
        @Independent
        AbstractFloatAssert<?> assumeThat(@Independent @NotModified Float arg0) { return null; }
        @Independent
        AbstractIntegerAssert<?> assumeThat(@Independent @NotModified Integer arg0) { return null; }
        @Independent
        AbstractDoubleAssert<?> assumeThat(@Independent @NotModified Double arg0) { return null; }
        @Independent
        <ELEMENT> FactoryBasedNavigableListAssert<ListAssert<ELEMENT>, List<? extends ELEMENT>, ELEMENT, ObjectAssert<ELEMENT>> assumeThat(@Independent @NotModified List<? extends ELEMENT> arg0) { return null; }
        @Independent
        AbstractZonedDateTimeAssert<?> assumeThat(@Independent @NotModified ZonedDateTime arg0) { return null; }
        @Independent
        <RESULT> CompletableFutureAssert<RESULT> assumeThat(@Independent @NotModified CompletableFuture<RESULT> arg0) { return null; }
        @Independent
        <RESULT> CompletableFutureAssert<RESULT> assumeThat(@Independent @NotModified CompletionStage<RESULT> arg0) { return null; }
        @Independent
        <VALUE> OptionalAssert<VALUE> assumeThat(@Independent @NotModified Optional<VALUE> arg0) { return null; }
        @Independent
        OptionalDoubleAssert assumeThat(@Independent @NotModified OptionalDouble arg0) { return null; }
        @Independent
        OptionalIntAssert assumeThat(@Independent @NotModified OptionalInt arg0) { return null; }
        @Independent
        OptionalLongAssert assumeThat(@Independent @NotModified OptionalLong arg0) { return null; }
        @Independent
        AbstractLocalDateTimeAssert<?> assumeThat(@Independent @NotModified LocalDateTime arg0) { return null; }
        @Independent
        AbstractLocalDateAssert<?> assumeThat(@Independent @NotModified LocalDate arg0) { return null; }
        @Independent
        AbstractYearMonthAssert<?> assumeThat(@Independent @NotModified YearMonth arg0) { return null; }
        @Independent
        AbstractLocalTimeAssert<?> assumeThat(@Independent @NotModified LocalTime arg0) { return null; }
        @Independent
        AbstractInstantAssert<?> assumeThat(@Independent @NotModified Instant arg0) { return null; }
        @Independent
        AbstractDurationAssert<?> assumeThat(@Independent @NotModified Duration arg0) { return null; }
        @Independent
        AbstractPeriodAssert<?> assumeThat(@Independent @NotModified Period arg0) { return null; }
        @Independent
        AbstractOffsetTimeAssert<?> assumeThat(@Independent @NotModified OffsetTime arg0) { return null; }
        @Independent
        AbstractOffsetDateTimeAssert<?> assumeThat(@Independent @NotModified OffsetDateTime arg0) { return null; }
        @Independent
        <T> ObjectAssert<T> assumeThatObject(@Independent @NotModified T arg0) { return null; }
        @Independent
        AbstractUrlAssert<?> assumeThat(@Independent @NotModified URL arg0) { return null; }
        @Independent
        AbstractUriAssert<?> assumeThat(@Independent @NotModified URI arg0) { return null; }
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
    class AbstractInputStreamAssert$<SELF extends AbstractInputStreamAssert<SELF, ACTUAL>, ACTUAL extends InputStream> {
        SELF hasContentEqualTo(@Modified InputStream arg0) { return null; }
        SELF hasSameContentAs(@Modified InputStream arg0) { return null; }
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
        InputStreamAssert$(@Modified InputStream arg0) { }
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
        static <ELEMENT> IteratorAssert<ELEMENT> assertThatIterator(@Modified Iterator<? extends ELEMENT> arg0) { return null; }
        IteratorAssert$(@Modified Iterator<? extends ELEMENT> arg0) { }
    }
    @Container
    class ListAssert$<ELEMENT> {
        static <ELEMENT> ListAssert<ELEMENT> assertThatStream(@Modified Stream<? extends ELEMENT> arg0) { return null; }
        static ListAssert<Double> assertThatDoubleStream(@Modified DoubleStream arg0) { return null; }
        static ListAssert<Long> assertThatLongStream(@Modified LongStream arg0) { return null; }
        static ListAssert<Integer> assertThatIntStream(@Modified IntStream arg0) { return null; }
        ListAssert$(@Modified Stream<? extends ELEMENT> arg0) { }
        ListAssert$(@Modified IntStream arg0) { }
        ListAssert$(@Modified LongStream arg0) { }
        ListAssert$(@Modified DoubleStream arg0) { }
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
        static <ELEMENT> SpliteratorAssert<ELEMENT> assertThatSpliterator(@Modified Spliterator<ELEMENT> arg0) { return null; }
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
