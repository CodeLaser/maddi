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
        static <T> void assertThat(@NotModified T arg0, @NotModified org.hamcrest.Matcher<? super T> arg1) { }
        static <T> void assertThat(java.lang.String arg0, @NotModified T arg1, @NotModified org.hamcrest.Matcher<? super T> arg2) { }
        static void assertThat(java.lang.String arg0, boolean arg1) { }
    }

    //public class org.hamcrest.Matchers
    class Matchers$ {
        static <T> org.hamcrest.Matcher<T> allOf(@NotModified java.lang.Iterable<org.hamcrest.Matcher<? super T>> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> allOf(@NotModified org.hamcrest.Matcher<? super T>... arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> allOf(@NotModified org.hamcrest.Matcher<? super T> arg0, @NotModified org.hamcrest.Matcher<? super T> arg1) { return null; }
        static <T> org.hamcrest.Matcher<T> allOf(@NotModified org.hamcrest.Matcher<? super T> arg0, @NotModified org.hamcrest.Matcher<? super T> arg1, @NotModified org.hamcrest.Matcher<? super T> arg2) { return null; }
        static <T> org.hamcrest.Matcher<T> allOf(@NotModified org.hamcrest.Matcher<? super T> arg0, @NotModified org.hamcrest.Matcher<? super T> arg1, @NotModified org.hamcrest.Matcher<? super T> arg2, @NotModified org.hamcrest.Matcher<? super T> arg3) { return null; }
        static <T> org.hamcrest.Matcher<T> allOf(@NotModified org.hamcrest.Matcher<? super T> arg0, @NotModified org.hamcrest.Matcher<? super T> arg1, @NotModified org.hamcrest.Matcher<? super T> arg2, @NotModified org.hamcrest.Matcher<? super T> arg3, @NotModified org.hamcrest.Matcher<? super T> arg4) { return null; }
        static <T> org.hamcrest.Matcher<T> allOf(@NotModified org.hamcrest.Matcher<? super T> arg0, @NotModified org.hamcrest.Matcher<? super T> arg1, @NotModified org.hamcrest.Matcher<? super T> arg2, @NotModified org.hamcrest.Matcher<? super T> arg3, @NotModified org.hamcrest.Matcher<? super T> arg4, @NotModified org.hamcrest.Matcher<? super T> arg5) { return null; }
        static <T> org.hamcrest.core.AnyOf<T> anyOf(@NotModified java.lang.Iterable<org.hamcrest.Matcher<? super T>> arg0) { return null; }
        static <T> org.hamcrest.core.AnyOf<T> anyOf(@NotModified org.hamcrest.Matcher<? super T>... arg0) { return null; }
        static <T> org.hamcrest.core.AnyOf<T> anyOf(@NotModified org.hamcrest.Matcher<? super T> arg0, @NotModified org.hamcrest.Matcher<? super T> arg1) { return null; }
        static <T> org.hamcrest.core.AnyOf<T> anyOf(@NotModified org.hamcrest.Matcher<? super T> arg0, @NotModified org.hamcrest.Matcher<? super T> arg1, @NotModified org.hamcrest.Matcher<? super T> arg2) { return null; }
        static <T> org.hamcrest.core.AnyOf<T> anyOf(@NotModified org.hamcrest.Matcher<? super T> arg0, @NotModified org.hamcrest.Matcher<? super T> arg1, @NotModified org.hamcrest.Matcher<? super T> arg2, @NotModified org.hamcrest.Matcher<? super T> arg3) { return null; }
        static <T> org.hamcrest.core.AnyOf<T> anyOf(@NotModified org.hamcrest.Matcher<? super T> arg0, @NotModified org.hamcrest.Matcher<? super T> arg1, @NotModified org.hamcrest.Matcher<? super T> arg2, @NotModified org.hamcrest.Matcher<? super T> arg3, @NotModified org.hamcrest.Matcher<? super T> arg4) { return null; }
        static <T> org.hamcrest.core.AnyOf<T> anyOf(@NotModified org.hamcrest.Matcher<? super T> arg0, @NotModified org.hamcrest.Matcher<? super T> arg1, @NotModified org.hamcrest.Matcher<? super T> arg2, @NotModified org.hamcrest.Matcher<? super T> arg3, @NotModified org.hamcrest.Matcher<? super T> arg4, @NotModified org.hamcrest.Matcher<? super T> arg5) { return null; }
        static <LHS> org.hamcrest.core.CombinableMatcher.CombinableBothMatcher<LHS> both(@NotModified org.hamcrest.Matcher<? super LHS> arg0) { return null; }
        static <LHS> org.hamcrest.core.CombinableMatcher.CombinableEitherMatcher<LHS> either(@NotModified org.hamcrest.Matcher<? super LHS> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> describedAs(java.lang.String arg0, @NotModified org.hamcrest.Matcher<T> arg1, @NotModified java.lang.Object... arg2) { return null; }
        static <U> org.hamcrest.Matcher<java.lang.Iterable<? extends U>> everyItem(@NotModified org.hamcrest.Matcher<U> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> is(@NotModified org.hamcrest.Matcher<T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> is(@NotModified T arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> isA(@NotModified java.lang.Class<?> arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.Object> anything() { return null; }
        static org.hamcrest.Matcher<java.lang.Object> anything(java.lang.String arg0) { return null; }
        static <T> org.hamcrest.Matcher<java.lang.Iterable<? super T>> hasItem(@NotModified org.hamcrest.Matcher<? super T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<java.lang.Iterable<? super T>> hasItem(@NotModified T arg0) { return null; }
        static <T> org.hamcrest.Matcher<java.lang.Iterable<T>> hasItems(@NotModified org.hamcrest.Matcher<? super T>... arg0) { return null; }
        static <T> org.hamcrest.Matcher<java.lang.Iterable<T>> hasItems(@NotModified T... arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> equalTo(@NotModified T arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.Object> equalToObject(@NotModified java.lang.Object arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> any(@NotModified java.lang.Class<T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> instanceOf(@NotModified java.lang.Class<?> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> not(@NotModified org.hamcrest.Matcher<T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> not(@NotModified T arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.Object> notNullValue() { return null; }
        static <T> org.hamcrest.Matcher<T> notNullValue(@NotModified java.lang.Class<T> arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.Object> nullValue() { return null; }
        static <T> org.hamcrest.Matcher<T> nullValue(@NotModified java.lang.Class<T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> sameInstance(@NotModified T arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> theInstance(@NotModified T arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> containsString(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> containsStringIgnoringCase(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> startsWith(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> startsWithIgnoringCase(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> endsWith(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> endsWithIgnoringCase(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> matchesRegex(@NotModified java.util.regex.Pattern arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> matchesRegex(java.lang.String arg0) { return null; }
        static <T> org.hamcrest.collection.IsArray<T> array(@NotModified org.hamcrest.Matcher<? super T>... arg0) { return null; }
        static <T> org.hamcrest.Matcher<T[]> hasItemInArray(@NotModified org.hamcrest.Matcher<? super T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T[]> hasItemInArray(@NotModified T arg0) { return null; }
        static <E> org.hamcrest.Matcher<E[]> arrayContaining(@NotModified E... arg0) { return null; }
        static <E> org.hamcrest.Matcher<E[]> arrayContaining(@NotModified org.hamcrest.Matcher<? super E>... arg0) { return null; }
        static <E> org.hamcrest.Matcher<E[]> arrayContaining(@NotModified java.util.List<org.hamcrest.Matcher<? super E>> arg0) { return null; }
        static <E> org.hamcrest.Matcher<E[]> arrayContainingInAnyOrder(@NotModified org.hamcrest.Matcher<? super E>... arg0) { return null; }
        static <E> org.hamcrest.Matcher<E[]> arrayContainingInAnyOrder(@NotModified java.util.Collection<org.hamcrest.Matcher<? super E>> arg0) { return null; }
        static <E> org.hamcrest.Matcher<E[]> arrayContainingInAnyOrder(@NotModified E... arg0) { return null; }
        static <E> org.hamcrest.Matcher<E[]> arrayWithSize(@NotModified org.hamcrest.Matcher<? super java.lang.Integer> arg0) { return null; }
        static <E> org.hamcrest.Matcher<E[]> arrayWithSize(int arg0) { return null; }
        static <E> org.hamcrest.Matcher<E[]> emptyArray() { return null; }
        static <K, V> org.hamcrest.Matcher<java.util.Map<? extends K, ? extends V>> aMapWithSize(@NotModified org.hamcrest.Matcher<? super java.lang.Integer> arg0) { return null; }
        static <K, V> org.hamcrest.Matcher<java.util.Map<? extends K, ? extends V>> aMapWithSize(int arg0) { return null; }
        static <K, V> org.hamcrest.Matcher<java.util.Map<? extends K, ? extends V>> anEmptyMap() { return null; }
        static <E> org.hamcrest.Matcher<java.util.Collection<? extends E>> hasSize(@NotModified org.hamcrest.Matcher<? super java.lang.Integer> arg0) { return null; }
        static <E> org.hamcrest.Matcher<java.util.Collection<? extends E>> hasSize(int arg0) { return null; }
        static <E> org.hamcrest.Matcher<java.util.Collection<? extends E>> empty() { return null; }
        static <E> org.hamcrest.Matcher<java.util.Collection<E>> emptyCollectionOf(@NotModified java.lang.Class<E> arg0) { return null; }
        static <E> org.hamcrest.Matcher<java.lang.Iterable<? extends E>> emptyIterable() { return null; }
        static <E> org.hamcrest.Matcher<java.lang.Iterable<E>> emptyIterableOf(@NotModified java.lang.Class<E> arg0) { return null; }
        static <E> org.hamcrest.Matcher<java.lang.Iterable<? extends E>> contains(@NotModified E... arg0) { return null; }
        static <E> org.hamcrest.Matcher<java.lang.Iterable<? extends E>> contains(@NotModified org.hamcrest.Matcher<? super E> arg0) { return null; }
        static <E> org.hamcrest.Matcher<java.lang.Iterable<? extends E>> contains(@NotModified org.hamcrest.Matcher<? super E>... arg0) { return null; }
        static <E> org.hamcrest.Matcher<java.lang.Iterable<? extends E>> contains(@NotModified java.util.List<org.hamcrest.Matcher<? super E>> arg0) { return null; }
        static <T> org.hamcrest.Matcher<java.lang.Iterable<? extends T>> containsInAnyOrder(@NotModified org.hamcrest.Matcher<? super T>... arg0) { return null; }
        static <T> org.hamcrest.Matcher<java.lang.Iterable<? extends T>> containsInAnyOrder(@NotModified T... arg0) { return null; }
        static <T> org.hamcrest.Matcher<java.lang.Iterable<? extends T>> containsInAnyOrder(@NotModified java.util.Collection<org.hamcrest.Matcher<? super T>> arg0) { return null; }
        static <E> org.hamcrest.Matcher<java.lang.Iterable<? extends E>> containsInRelativeOrder(@NotModified E... arg0) { return null; }
        static <E> org.hamcrest.Matcher<java.lang.Iterable<? extends E>> containsInRelativeOrder(@NotModified org.hamcrest.Matcher<? super E>... arg0) { return null; }
        static <E> org.hamcrest.Matcher<java.lang.Iterable<? extends E>> containsInRelativeOrder(@NotModified java.util.List<org.hamcrest.Matcher<? super E>> arg0) { return null; }
        static <E> org.hamcrest.Matcher<java.lang.Iterable<E>> iterableWithSize(@NotModified org.hamcrest.Matcher<? super java.lang.Integer> arg0) { return null; }
        static <E> org.hamcrest.Matcher<java.lang.Iterable<E>> iterableWithSize(int arg0) { return null; }
        static <K, V> org.hamcrest.Matcher<java.util.Map<? extends K, ? extends V>> hasEntry(@NotModified org.hamcrest.Matcher<? super K> arg0, @NotModified org.hamcrest.Matcher<? super V> arg1) { return null; }
        static <K, V> org.hamcrest.Matcher<java.util.Map<? extends K, ? extends V>> hasEntry(@NotModified K arg0, @NotModified V arg1) { return null; }
        static <K> org.hamcrest.Matcher<java.util.Map<? extends K, ?>> hasKey(@NotModified org.hamcrest.Matcher<? super K> arg0) { return null; }
        static <K> org.hamcrest.Matcher<java.util.Map<? extends K, ?>> hasKey(@NotModified K arg0) { return null; }
        static <V> org.hamcrest.Matcher<java.util.Map<?, ? extends V>> hasValue(@NotModified org.hamcrest.Matcher<? super V> arg0) { return null; }
        static <V> org.hamcrest.Matcher<java.util.Map<?, ? extends V>> hasValue(@NotModified V arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> in(@NotModified java.util.Collection<T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> in(@NotModified T[] arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> isIn(@NotModified java.util.Collection<T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> isIn(@NotModified T[] arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> isOneOf(@NotModified T... arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> oneOf(@NotModified T... arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.Double> closeTo(double arg0, double arg1) { return null; }
        static org.hamcrest.Matcher<java.lang.Double> notANumber() { return null; }
        static org.hamcrest.Matcher<java.math.BigDecimal> closeTo(@NotModified java.math.BigDecimal arg0, @NotModified java.math.BigDecimal arg1) { return null; }
        static <T extends java.lang.Comparable<T>> org.hamcrest.Matcher<T> comparesEqualTo(@NotModified T arg0) { return null; }
        static <T extends java.lang.Comparable<T>> org.hamcrest.Matcher<T> greaterThan(@NotModified T arg0) { return null; }
        static <T extends java.lang.Comparable<T>> org.hamcrest.Matcher<T> greaterThanOrEqualTo(@NotModified T arg0) { return null; }
        static <T extends java.lang.Comparable<T>> org.hamcrest.Matcher<T> lessThan(@NotModified T arg0) { return null; }
        static <T extends java.lang.Comparable<T>> org.hamcrest.Matcher<T> lessThanOrEqualTo(@NotModified T arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> equalToIgnoringCase(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> equalToIgnoringWhiteSpace(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> equalToCompressingWhiteSpace(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> emptyOrNullString() { return null; }
        static org.hamcrest.Matcher<java.lang.String> emptyString() { return null; }
        static org.hamcrest.Matcher<java.lang.String> isEmptyOrNullString() { return null; }
        static org.hamcrest.Matcher<java.lang.String> isEmptyString() { return null; }
        static org.hamcrest.Matcher<java.lang.String> blankOrNullString() { return null; }
        static org.hamcrest.Matcher<java.lang.String> blankString() { return null; }
        static org.hamcrest.Matcher<java.lang.String> matchesPattern(@NotModified java.util.regex.Pattern arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> matchesPattern(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> stringContainsInOrder(@NotModified java.lang.Iterable<java.lang.String> arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> stringContainsInOrder(@NotModified java.lang.String... arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.CharSequence> hasLength(int arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> hasToString(@NotModified org.hamcrest.Matcher<? super java.lang.String> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> hasToString(java.lang.String arg0) { return null; }
        static <T> org.hamcrest.Matcher<java.lang.Class<?>> typeCompatibleWith(@NotModified java.lang.Class<T> arg0) { return null; }
        static org.hamcrest.Matcher<java.util.EventObject> eventFrom(@NotModified java.lang.Class<? extends java.util.EventObject> arg0, @NotModified java.lang.Object arg1) { return null; }
        static org.hamcrest.Matcher<java.util.EventObject> eventFrom(@NotModified java.lang.Object arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> hasProperty(java.lang.String arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> hasProperty(java.lang.String arg0, @NotModified org.hamcrest.Matcher<?> arg1) { return null; }
        static <B> org.hamcrest.Matcher<B> samePropertyValuesAs(@NotModified B arg0, @NotModified java.lang.String... arg1) { return null; }
        static org.hamcrest.Matcher<org.w3c.dom.Node> hasXPath(java.lang.String arg0, @NotModified org.hamcrest.Matcher<java.lang.String> arg1) { return null; }
        static org.hamcrest.Matcher<org.w3c.dom.Node> hasXPath(java.lang.String arg0, @NotModified javax.xml.namespace.NamespaceContext arg1, @NotModified org.hamcrest.Matcher<java.lang.String> arg2) { return null; }
        static org.hamcrest.Matcher<org.w3c.dom.Node> hasXPath(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<org.w3c.dom.Node> hasXPath(java.lang.String arg0, @NotModified javax.xml.namespace.NamespaceContext arg1) { return null; }
    }

    //public class org.hamcrest.CoreMatchers
    class CoreMatchers$ {
        static <T> org.hamcrest.Matcher<T> allOf(@NotModified java.lang.Iterable<org.hamcrest.Matcher<? super T>> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> allOf(@NotModified org.hamcrest.Matcher<? super T>... arg0) { return null; }
        static <T> org.hamcrest.core.AnyOf<T> anyOf(@NotModified java.lang.Iterable<org.hamcrest.Matcher<? super T>> arg0) { return null; }
        static <T> org.hamcrest.core.AnyOf<T> anyOf(@NotModified org.hamcrest.Matcher<? super T>... arg0) { return null; }
        static <LHS> org.hamcrest.core.CombinableMatcher.CombinableBothMatcher<LHS> both(@NotModified org.hamcrest.Matcher<? super LHS> arg0) { return null; }
        static <LHS> org.hamcrest.core.CombinableMatcher.CombinableEitherMatcher<LHS> either(@NotModified org.hamcrest.Matcher<? super LHS> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> describedAs(java.lang.String arg0, @NotModified org.hamcrest.Matcher<T> arg1, @NotModified java.lang.Object... arg2) { return null; }
        static <U> org.hamcrest.Matcher<java.lang.Iterable<? extends U>> everyItem(@NotModified org.hamcrest.Matcher<U> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> is(@NotModified org.hamcrest.Matcher<T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> is(@NotModified T arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> isA(@NotModified java.lang.Class<T> arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.Object> anything() { return null; }
        static org.hamcrest.Matcher<java.lang.Object> anything(java.lang.String arg0) { return null; }
        static <T> org.hamcrest.Matcher<java.lang.Iterable<? super T>> hasItem(@NotModified org.hamcrest.Matcher<? super T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<java.lang.Iterable<? super T>> hasItem(@NotModified T arg0) { return null; }
        static <T> org.hamcrest.Matcher<java.lang.Iterable<T>> hasItems(@NotModified org.hamcrest.Matcher<? super T>... arg0) { return null; }
        static <T> org.hamcrest.Matcher<java.lang.Iterable<T>> hasItems(@NotModified T... arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> equalTo(@NotModified T arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.Object> equalToObject(@NotModified java.lang.Object arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> any(@NotModified java.lang.Class<T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> instanceOf(@NotModified java.lang.Class<?> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> not(@NotModified org.hamcrest.Matcher<T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> not(@NotModified T arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.Object> notNullValue() { return null; }
        static <T> org.hamcrest.Matcher<T> notNullValue(@NotModified java.lang.Class<T> arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.Object> nullValue() { return null; }
        static <T> org.hamcrest.Matcher<T> nullValue(@NotModified java.lang.Class<T> arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> sameInstance(@NotModified T arg0) { return null; }
        static <T> org.hamcrest.Matcher<T> theInstance(@NotModified T arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> containsString(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> containsStringIgnoringCase(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> startsWith(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> startsWithIgnoringCase(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> endsWith(java.lang.String arg0) { return null; }
        static org.hamcrest.Matcher<java.lang.String> endsWithIgnoringCase(java.lang.String arg0) { return null; }
    }
}
