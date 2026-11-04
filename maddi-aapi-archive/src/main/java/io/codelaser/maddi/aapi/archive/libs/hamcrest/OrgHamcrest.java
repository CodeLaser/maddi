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

package io.codelaser.maddi.aapi.archive.libs.hamcrest;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;

import org.hamcrest.Matcher;
import org.hamcrest.collection.IsArray;
import org.hamcrest.core.AnyOf;
import org.hamcrest.core.CombinableMatcher;
import org.w3c.dom.Node;

import io.codelaser.maddi.annotation.NotModified;

/**
 * Hamcrest 2.2, found by the library-call census (TestLibraryCallCensus, 2026-10-12): without hints,
 * {@code assertThat(actual, matcher)} read its matcher as modified, and so did every matcher factory that takes a
 * matcher ({@code not}, {@code allOf}, {@code hasItem}, {@code contains}): 929 calls over three corpora. A matcher
 * only reads: every reference parameter of {@code MatcherAssert}, {@code Matchers} and {@code CoreMatchers} is
 * {@code @NotModified}, generated from {@code javap -public} of the jar. A value parameter that is a bare type
 * variable ({@code is(T)}, {@code equalTo(T)}) was unmodified already; the annotation states it anyway. The
 * {@code Matcher} type itself is left alone: a user's matcher may keep state, and this says nothing about it.
 * Names types in full and imports none, like libs/assertj.
 */
public class OrgHamcrest {
    public static final String PACKAGE_NAME = "org.hamcrest";

    //public class org.hamcrest.MatcherAssert
    class MatcherAssert$ {
        static <T> void assertThat(@NotModified T arg0, @NotModified Matcher<? super T> arg1) { }
        static <T> void assertThat(String arg0, @NotModified T arg1, @NotModified Matcher<? super T> arg2) { }
        static void assertThat(String arg0, boolean arg1) { }
    }

    //public class org.hamcrest.Matchers
    class Matchers$ {
        static <T> Matcher<T> allOf(@NotModified Iterable<Matcher<? super T>> arg0) { return null; }
        static <T> Matcher<T> allOf(@NotModified Matcher<? super T>... arg0) { return null; }
        static <T> Matcher<T> allOf(@NotModified Matcher<? super T> arg0, @NotModified Matcher<? super T> arg1) { return null; }
        static <T> Matcher<T> allOf(@NotModified Matcher<? super T> arg0, @NotModified Matcher<? super T> arg1, @NotModified Matcher<? super T> arg2) { return null; }
        static <T> Matcher<T> allOf(@NotModified Matcher<? super T> arg0, @NotModified Matcher<? super T> arg1, @NotModified Matcher<? super T> arg2, @NotModified Matcher<? super T> arg3) { return null; }
        static <T> Matcher<T> allOf(@NotModified Matcher<? super T> arg0, @NotModified Matcher<? super T> arg1, @NotModified Matcher<? super T> arg2, @NotModified Matcher<? super T> arg3, @NotModified Matcher<? super T> arg4) { return null; }
        static <T> Matcher<T> allOf(@NotModified Matcher<? super T> arg0, @NotModified Matcher<? super T> arg1, @NotModified Matcher<? super T> arg2, @NotModified Matcher<? super T> arg3, @NotModified Matcher<? super T> arg4, @NotModified Matcher<? super T> arg5) { return null; }
        static <T> AnyOf<T> anyOf(@NotModified Iterable<Matcher<? super T>> arg0) { return null; }
        static <T> AnyOf<T> anyOf(@NotModified Matcher<? super T>... arg0) { return null; }
        static <T> AnyOf<T> anyOf(@NotModified Matcher<? super T> arg0, @NotModified Matcher<? super T> arg1) { return null; }
        static <T> AnyOf<T> anyOf(@NotModified Matcher<? super T> arg0, @NotModified Matcher<? super T> arg1, @NotModified Matcher<? super T> arg2) { return null; }
        static <T> AnyOf<T> anyOf(@NotModified Matcher<? super T> arg0, @NotModified Matcher<? super T> arg1, @NotModified Matcher<? super T> arg2, @NotModified Matcher<? super T> arg3) { return null; }
        static <T> AnyOf<T> anyOf(@NotModified Matcher<? super T> arg0, @NotModified Matcher<? super T> arg1, @NotModified Matcher<? super T> arg2, @NotModified Matcher<? super T> arg3, @NotModified Matcher<? super T> arg4) { return null; }
        static <T> AnyOf<T> anyOf(@NotModified Matcher<? super T> arg0, @NotModified Matcher<? super T> arg1, @NotModified Matcher<? super T> arg2, @NotModified Matcher<? super T> arg3, @NotModified Matcher<? super T> arg4, @NotModified Matcher<? super T> arg5) { return null; }
        static <LHS> CombinableMatcher.CombinableBothMatcher<LHS> both(@NotModified Matcher<? super LHS> arg0) { return null; }
        static <LHS> CombinableMatcher.CombinableEitherMatcher<LHS> either(@NotModified Matcher<? super LHS> arg0) { return null; }
        static <T> Matcher<T> describedAs(String arg0, @NotModified Matcher<T> arg1, @NotModified Object... arg2) { return null; }
        static <U> Matcher<Iterable<? extends U>> everyItem(@NotModified Matcher<U> arg0) { return null; }
        static <T> Matcher<T> is(@NotModified Matcher<T> arg0) { return null; }
        static <T> Matcher<T> is(@NotModified T arg0) { return null; }
        static <T> Matcher<T> isA(@NotModified Class<?> arg0) { return null; }
        static Matcher<Object> anything() { return null; }
        static Matcher<Object> anything(String arg0) { return null; }
        static <T> Matcher<Iterable<? super T>> hasItem(@NotModified Matcher<? super T> arg0) { return null; }
        static <T> Matcher<Iterable<? super T>> hasItem(@NotModified T arg0) { return null; }
        static <T> Matcher<Iterable<T>> hasItems(@NotModified Matcher<? super T>... arg0) { return null; }
        static <T> Matcher<Iterable<T>> hasItems(@NotModified T... arg0) { return null; }
        static <T> Matcher<T> equalTo(@NotModified T arg0) { return null; }
        static Matcher<Object> equalToObject(@NotModified Object arg0) { return null; }
        static <T> Matcher<T> any(@NotModified Class<T> arg0) { return null; }
        static <T> Matcher<T> instanceOf(@NotModified Class<?> arg0) { return null; }
        static <T> Matcher<T> not(@NotModified Matcher<T> arg0) { return null; }
        static <T> Matcher<T> not(@NotModified T arg0) { return null; }
        static Matcher<Object> notNullValue() { return null; }
        static <T> Matcher<T> notNullValue(@NotModified Class<T> arg0) { return null; }
        static Matcher<Object> nullValue() { return null; }
        static <T> Matcher<T> nullValue(@NotModified Class<T> arg0) { return null; }
        static <T> Matcher<T> sameInstance(@NotModified T arg0) { return null; }
        static <T> Matcher<T> theInstance(@NotModified T arg0) { return null; }
        static Matcher<String> containsString(String arg0) { return null; }
        static Matcher<String> containsStringIgnoringCase(String arg0) { return null; }
        static Matcher<String> startsWith(String arg0) { return null; }
        static Matcher<String> startsWithIgnoringCase(String arg0) { return null; }
        static Matcher<String> endsWith(String arg0) { return null; }
        static Matcher<String> endsWithIgnoringCase(String arg0) { return null; }
        static Matcher<String> matchesRegex(@NotModified Pattern arg0) { return null; }
        static Matcher<String> matchesRegex(String arg0) { return null; }
        static <T> IsArray<T> array(@NotModified Matcher<? super T>... arg0) { return null; }
        static <T> Matcher<T[]> hasItemInArray(@NotModified Matcher<? super T> arg0) { return null; }
        static <T> Matcher<T[]> hasItemInArray(@NotModified T arg0) { return null; }
        static <E> Matcher<E[]> arrayContaining(@NotModified E... arg0) { return null; }
        static <E> Matcher<E[]> arrayContaining(@NotModified Matcher<? super E>... arg0) { return null; }
        static <E> Matcher<E[]> arrayContaining(@NotModified List<Matcher<? super E>> arg0) { return null; }
        static <E> Matcher<E[]> arrayContainingInAnyOrder(@NotModified Matcher<? super E>... arg0) { return null; }
        static <E> Matcher<E[]> arrayContainingInAnyOrder(@NotModified Collection<Matcher<? super E>> arg0) { return null; }
        static <E> Matcher<E[]> arrayContainingInAnyOrder(@NotModified E... arg0) { return null; }
        static <E> Matcher<E[]> arrayWithSize(@NotModified Matcher<? super Integer> arg0) { return null; }
        static <E> Matcher<E[]> arrayWithSize(int arg0) { return null; }
        static <E> Matcher<E[]> emptyArray() { return null; }
        static <K, V> Matcher<Map<? extends K, ? extends V>> aMapWithSize(@NotModified Matcher<? super Integer> arg0) { return null; }
        static <K, V> Matcher<Map<? extends K, ? extends V>> aMapWithSize(int arg0) { return null; }
        static <K, V> Matcher<Map<? extends K, ? extends V>> anEmptyMap() { return null; }
        static <E> Matcher<Collection<? extends E>> hasSize(@NotModified Matcher<? super Integer> arg0) { return null; }
        static <E> Matcher<Collection<? extends E>> hasSize(int arg0) { return null; }
        static <E> Matcher<Collection<? extends E>> empty() { return null; }
        static <E> Matcher<Collection<E>> emptyCollectionOf(@NotModified Class<E> arg0) { return null; }
        static <E> Matcher<Iterable<? extends E>> emptyIterable() { return null; }
        static <E> Matcher<Iterable<E>> emptyIterableOf(@NotModified Class<E> arg0) { return null; }
        static <E> Matcher<Iterable<? extends E>> contains(@NotModified E... arg0) { return null; }
        static <E> Matcher<Iterable<? extends E>> contains(@NotModified Matcher<? super E> arg0) { return null; }
        static <E> Matcher<Iterable<? extends E>> contains(@NotModified Matcher<? super E>... arg0) { return null; }
        static <E> Matcher<Iterable<? extends E>> contains(@NotModified List<Matcher<? super E>> arg0) { return null; }
        static <T> Matcher<Iterable<? extends T>> containsInAnyOrder(@NotModified Matcher<? super T>... arg0) { return null; }
        static <T> Matcher<Iterable<? extends T>> containsInAnyOrder(@NotModified T... arg0) { return null; }
        static <T> Matcher<Iterable<? extends T>> containsInAnyOrder(@NotModified Collection<Matcher<? super T>> arg0) { return null; }
        static <E> Matcher<Iterable<? extends E>> containsInRelativeOrder(@NotModified E... arg0) { return null; }
        static <E> Matcher<Iterable<? extends E>> containsInRelativeOrder(@NotModified Matcher<? super E>... arg0) { return null; }
        static <E> Matcher<Iterable<? extends E>> containsInRelativeOrder(@NotModified List<Matcher<? super E>> arg0) { return null; }
        static <E> Matcher<Iterable<E>> iterableWithSize(@NotModified Matcher<? super Integer> arg0) { return null; }
        static <E> Matcher<Iterable<E>> iterableWithSize(int arg0) { return null; }
        static <K, V> Matcher<Map<? extends K, ? extends V>> hasEntry(@NotModified Matcher<? super K> arg0, @NotModified Matcher<? super V> arg1) { return null; }
        static <K, V> Matcher<Map<? extends K, ? extends V>> hasEntry(@NotModified K arg0, @NotModified V arg1) { return null; }
        static <K> Matcher<Map<? extends K, ?>> hasKey(@NotModified Matcher<? super K> arg0) { return null; }
        static <K> Matcher<Map<? extends K, ?>> hasKey(@NotModified K arg0) { return null; }
        static <V> Matcher<Map<?, ? extends V>> hasValue(@NotModified Matcher<? super V> arg0) { return null; }
        static <V> Matcher<Map<?, ? extends V>> hasValue(@NotModified V arg0) { return null; }
        static <T> Matcher<T> in(@NotModified Collection<T> arg0) { return null; }
        static <T> Matcher<T> in(@NotModified T[] arg0) { return null; }
        static <T> Matcher<T> isIn(@NotModified Collection<T> arg0) { return null; }
        static <T> Matcher<T> isIn(@NotModified T[] arg0) { return null; }
        static <T> Matcher<T> isOneOf(@NotModified T... arg0) { return null; }
        static <T> Matcher<T> oneOf(@NotModified T... arg0) { return null; }
        static Matcher<Double> closeTo(double arg0, double arg1) { return null; }
        static Matcher<Double> notANumber() { return null; }
        static Matcher<BigDecimal> closeTo(@NotModified BigDecimal arg0, @NotModified BigDecimal arg1) { return null; }
        static <T extends Comparable<T>> Matcher<T> comparesEqualTo(@NotModified T arg0) { return null; }
        static <T extends Comparable<T>> Matcher<T> greaterThan(@NotModified T arg0) { return null; }
        static <T extends Comparable<T>> Matcher<T> greaterThanOrEqualTo(@NotModified T arg0) { return null; }
        static <T extends Comparable<T>> Matcher<T> lessThan(@NotModified T arg0) { return null; }
        static <T extends Comparable<T>> Matcher<T> lessThanOrEqualTo(@NotModified T arg0) { return null; }
        static Matcher<String> equalToIgnoringCase(String arg0) { return null; }
        static Matcher<String> equalToIgnoringWhiteSpace(String arg0) { return null; }
        static Matcher<String> equalToCompressingWhiteSpace(String arg0) { return null; }
        static Matcher<String> emptyOrNullString() { return null; }
        static Matcher<String> emptyString() { return null; }
        static Matcher<String> isEmptyOrNullString() { return null; }
        static Matcher<String> isEmptyString() { return null; }
        static Matcher<String> blankOrNullString() { return null; }
        static Matcher<String> blankString() { return null; }
        static Matcher<String> matchesPattern(@NotModified Pattern arg0) { return null; }
        static Matcher<String> matchesPattern(String arg0) { return null; }
        static Matcher<String> stringContainsInOrder(@NotModified Iterable<String> arg0) { return null; }
        static Matcher<String> stringContainsInOrder(@NotModified String... arg0) { return null; }
        static Matcher<CharSequence> hasLength(int arg0) { return null; }
        static <T> Matcher<T> hasToString(@NotModified Matcher<? super String> arg0) { return null; }
        static <T> Matcher<T> hasToString(String arg0) { return null; }
        static <T> Matcher<Class<?>> typeCompatibleWith(@NotModified Class<T> arg0) { return null; }
        static Matcher<EventObject> eventFrom(@NotModified Class<? extends EventObject> arg0, @NotModified Object arg1) { return null; }
        static Matcher<EventObject> eventFrom(@NotModified Object arg0) { return null; }
        static <T> Matcher<T> hasProperty(String arg0) { return null; }
        static <T> Matcher<T> hasProperty(String arg0, @NotModified Matcher<?> arg1) { return null; }
        static <B> Matcher<B> samePropertyValuesAs(@NotModified B arg0, @NotModified String... arg1) { return null; }
        static Matcher<Node> hasXPath(String arg0, @NotModified Matcher<String> arg1) { return null; }
        static Matcher<Node> hasXPath(String arg0, @NotModified NamespaceContext arg1, @NotModified Matcher<String> arg2) { return null; }
        static Matcher<Node> hasXPath(String arg0) { return null; }
        static Matcher<Node> hasXPath(String arg0, @NotModified NamespaceContext arg1) { return null; }
    }

    //public class org.hamcrest.CoreMatchers
    class CoreMatchers$ {
        static <T> Matcher<T> allOf(@NotModified Iterable<Matcher<? super T>> arg0) { return null; }
        static <T> Matcher<T> allOf(@NotModified Matcher<? super T>... arg0) { return null; }
        static <T> AnyOf<T> anyOf(@NotModified Iterable<Matcher<? super T>> arg0) { return null; }
        static <T> AnyOf<T> anyOf(@NotModified Matcher<? super T>... arg0) { return null; }
        static <LHS> CombinableMatcher.CombinableBothMatcher<LHS> both(@NotModified Matcher<? super LHS> arg0) { return null; }
        static <LHS> CombinableMatcher.CombinableEitherMatcher<LHS> either(@NotModified Matcher<? super LHS> arg0) { return null; }
        static <T> Matcher<T> describedAs(String arg0, @NotModified Matcher<T> arg1, @NotModified Object... arg2) { return null; }
        static <U> Matcher<Iterable<? extends U>> everyItem(@NotModified Matcher<U> arg0) { return null; }
        static <T> Matcher<T> is(@NotModified Matcher<T> arg0) { return null; }
        static <T> Matcher<T> is(@NotModified T arg0) { return null; }
        static <T> Matcher<T> isA(@NotModified Class<T> arg0) { return null; }
        static Matcher<Object> anything() { return null; }
        static Matcher<Object> anything(String arg0) { return null; }
        static <T> Matcher<Iterable<? super T>> hasItem(@NotModified Matcher<? super T> arg0) { return null; }
        static <T> Matcher<Iterable<? super T>> hasItem(@NotModified T arg0) { return null; }
        static <T> Matcher<Iterable<T>> hasItems(@NotModified Matcher<? super T>... arg0) { return null; }
        static <T> Matcher<Iterable<T>> hasItems(@NotModified T... arg0) { return null; }
        static <T> Matcher<T> equalTo(@NotModified T arg0) { return null; }
        static Matcher<Object> equalToObject(@NotModified Object arg0) { return null; }
        static <T> Matcher<T> any(@NotModified Class<T> arg0) { return null; }
        static <T> Matcher<T> instanceOf(@NotModified Class<?> arg0) { return null; }
        static <T> Matcher<T> not(@NotModified Matcher<T> arg0) { return null; }
        static <T> Matcher<T> not(@NotModified T arg0) { return null; }
        static Matcher<Object> notNullValue() { return null; }
        static <T> Matcher<T> notNullValue(@NotModified Class<T> arg0) { return null; }
        static Matcher<Object> nullValue() { return null; }
        static <T> Matcher<T> nullValue(@NotModified Class<T> arg0) { return null; }
        static <T> Matcher<T> sameInstance(@NotModified T arg0) { return null; }
        static <T> Matcher<T> theInstance(@NotModified T arg0) { return null; }
        static Matcher<String> containsString(String arg0) { return null; }
        static Matcher<String> containsStringIgnoringCase(String arg0) { return null; }
        static Matcher<String> startsWith(String arg0) { return null; }
        static Matcher<String> startsWithIgnoringCase(String arg0) { return null; }
        static Matcher<String> endsWith(String arg0) { return null; }
        static Matcher<String> endsWithIgnoringCase(String arg0) { return null; }
    }
}
