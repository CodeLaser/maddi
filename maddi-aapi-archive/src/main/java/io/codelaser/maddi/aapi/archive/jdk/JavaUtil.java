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

package io.codelaser.maddi.aapi.archive.jdk;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import java.util.function.*;
import java.util.random.RandomGenerator;
import java.util.stream.*;

import io.codelaser.maddi.annotation.*;
import io.codelaser.maddi.annotation.method.GetSet;
import io.codelaser.maddi.annotation.type.UtilityClass;
public class JavaUtil {
    public static final String PACKAGE_NAME = "java.util";
    //public abstract class AbstractCollection implements Collection<E>
    @Container
    class AbstractCollection$<E> {
        //override from java.lang.Iterable, java.util.Collection
        //@NotModified[H] @NotNull[H]
        Iterator<E> iterator() { return null; }

        //override from java.util.Collection
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.Collection
        //@NotModified[H]
        boolean isEmpty() { return false; }

        //override from java.util.Collection
        //@NotModified[H]
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object o) { return false; }

        //override from java.util.Collection
        //@NotModified[H]
        @NotNull(nullableArguments = 0) Object [] toArray() { return null; }

        //override from java.util.Collection
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[O]*/ @NotNull T [] a) { return null; }

        //override from java.util.Collection
        //override has frequency 16
        @Modified boolean add(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { return false; }

        //override from java.util.Collection
        @Modified boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object o) { return false; }

        //override from java.util.Collection
        //@NotModified[H]
        boolean containsAll(/*@Independent[M] @NotModified[O]*/ @NotNull Collection<?> c) { return false; }

        //override from java.util.Collection
        //override has frequency 1
        @Modified boolean addAll(/*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull Collection<? extends E> c) { return false; }

        //override from java.util.Collection
        @Modified boolean removeAll(/*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull Collection<?> c) { return false; }

        //override from java.util.Collection
        @Modified boolean retainAll(/*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull Collection<?> c) { return false; }

        //override from java.util.Collection
        @Modified void clear() { }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public abstract class AbstractList extends AbstractCollection<E> implements List<E>
    @Container
    class AbstractList$<E> {
        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //override has frequency 16
        @Modified boolean add(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { return false; }

        //override from java.util.List
        //@Independent(hc=true)[H] @NotModified[H] @GetSet("_synthetic_list")[H]
        E get(int i) { return null; }

        //override from java.util.List
        //@Independent(hc=true)[H] @GetSet("_synthetic_list")[H]
        @Modified E set(int index, /*@Independent(hc=true)[H] @NotModified[O]*/ E element) { return null; }

        //override from java.util.List
        @Modified void add(int index, /*@Independent(hc=true)[H] @NotModified[O]*/ E element) { }

        //override from java.util.List
        //@Independent(hc=true)[H]
        @Modified E remove(int index) { return null; }

        //override from java.util.List
        int indexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object o) { return 0; }

        //override from java.util.List
        int lastIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object o) { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        @Modified void clear() { }

        //override from java.util.List
        @Modified boolean addAll(int index, /*@NotModified[O]*/ @NotNull Collection<? extends E> c) { return false; }

        //override from java.lang.Iterable, java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H] @NotNull[H]
        Iterator<E> iterator() { return null; }

        //override from java.util.List
        ListIterator<E> listIterator() { return null; }

        //override from java.util.List
        ListIterator<E> listIterator(int index) { return null; }

        //override from java.util.List
        //@NotModified[H]
        List<E> subList(int fromIndex, int toIndex) { return null; }

        //override from java.lang.Object, java.util.Collection, java.util.List
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object o) { return false; }

        //override from java.lang.Object, java.util.Collection, java.util.List
        //@NotModified[H]
        public int hashCode() { return 0; }
    }

    //public abstract class AbstractMap implements Map<K,V>
    @Container
    class AbstractMap$<K, V> {
        //public static class SimpleEntry implements Map.Entry<K,V>, Serializable
        class SimpleEntry<K, V> {
            SimpleEntry(
                /*@Independent(hc=true)[T] @NotModified[T]*/ K key,
                /*@Independent(hc=true)[T] @NotModified[T]*/ V value) { }
            SimpleEntry(@NotNull Map.Entry<? extends K, ? extends V> entry) { }
            //override from java.util.Map.Entry
            //@Independent(hc=true)[H] @NotModified[H]
            K getKey() { return null; }

            //override from java.util.Map.Entry
            //@Independent(hc=true)[H] @NotModified[H]
            V getValue() { return null; }

            //override from java.util.Map.Entry
            //@Independent(hc=true)[H]
            @Modified V setValue(/*@Independent(hc=true)[H] @NotModified[T]*/ V value) { return null; }

            //override from java.lang.Object, java.util.Map.Entry
            //@NotModified[H]
            public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object o) { return false; }

            //override from java.lang.Object, java.util.Map.Entry
            //@NotModified[H]
            public int hashCode() { return 0; }

            //override from java.lang.Object
            //@NotModified[H] @NotNull[H]
            public String toString() { return null; }
        }

        //public static class SimpleImmutableEntry implements Map.Entry<K,V>, Serializable
        class SimpleImmutableEntry<K, V> {
            SimpleImmutableEntry(
                /*@Independent(hc=true)[T] @NotModified[T]*/ K key,
                /*@Independent(hc=true)[T] @NotModified[T]*/ V value) { }
            SimpleImmutableEntry(@NotNull Map.Entry<? extends K, ? extends V> entry) { }
            //override from java.util.Map.Entry
            //@Independent(hc=true)[H] @NotModified[H]
            K getKey() { return null; }

            //override from java.util.Map.Entry
            //@Independent(hc=true)[H] @NotModified[H]
            V getValue() { return null; }

            //override from java.util.Map.Entry
            //@Independent(hc=true)[H]
            @Modified V setValue(/*@Independent(hc=true)[H] @NotModified[T]*/ V value) { return null; }

            //override from java.lang.Object, java.util.Map.Entry
            //@NotModified[H]
            public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object o) { return false; }

            //override from java.lang.Object, java.util.Map.Entry
            //@NotModified[H]
            public int hashCode() { return 0; }

            //override from java.lang.Object
            //@NotModified[H] @NotNull[H]
            public String toString() { return null; }
        }

        //override from java.util.Map
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.Map
        //@NotModified[H]
        boolean isEmpty() { return false; }

        //override from java.util.Map
        boolean containsValue(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object value) {
            return false;
        }

        //override from java.util.Map
        //override has frequency 2
        //@NotModified[H]
        boolean containsKey(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object key) { return false; }

        //override from java.util.Map
        //override has frequency 2
        //@Independent(hc=true)[H] @NotModified[H]
        @Nullable V get(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object key) { return null; }

        //override from java.util.Map
        //override has frequency 1
        //@Independent(hc=true)[H]

        @Modified @Nullable V put(/*@Independent(hc=true)[H] @NotModified[O]*/ K key, /*@Independent(hc=true)[H] @NotModified[O]*/ V value) {
            return null;
        }

        //override from java.util.Map
        //@Independent(hc=true)[H]
        @Modified @Nullable V remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object key) { return null; }

        //override from java.util.Map
        @Modified void putAll(/*@NotModified[O]*/ @NotNull Map<? extends K, ? extends V> m) { }

        //override from java.util.Map
        @Modified void clear() { }

        //override from java.util.Map
        //@NotModified[H]
        Set<K> keySet() { return null; }

        //override from java.util.Map
        //override has frequency 2
        //@NotModified[H]
        Collection<V> values() { return null; }

        //override from java.util.Map
        //@NotModified[H]
        Set<Map.Entry<K, V>> entrySet() { return null; }

        //override from java.lang.Object, java.util.Map
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object o) { return false; }

        //override from java.lang.Object, java.util.Map
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public abstract class AbstractQueue extends AbstractCollection<E> implements Queue<E>
    @Container
    class AbstractQueue$<E> { 
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified boolean addAll(@NotNull java.util.Collection<? extends E> p0) { return false; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified E remove() { return null; }
    }

    //public abstract class AbstractSequentialList extends AbstractList<E>
    @Container
    class AbstractSequentialList$<E> { 
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified boolean addAll(int p0, @NotNull java.util.Collection<? extends E> p1) { return false; }
    }

    //public abstract class AbstractSet extends AbstractCollection<E> implements Set<E>
    @Container
    class AbstractSet$<E> {
        //override from java.lang.Object, java.util.Collection, java.util.Set
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object o) { return false; }

        //override from java.lang.Object, java.util.Collection, java.util.Set
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        @Modified boolean removeAll(/*@Independent(hc=true)[H] @NotModified[H]*/ @NotNull Collection<?> c) { return false; }
    }

    //public class ArrayDeque<E> extends AbstractCollection<E> implements Deque<E>, Cloneable, Serializable
    @Container
    class ArrayDeque$<E> {
        ArrayDeque$(@NotNull @Independent(hc = true) @NotModified Collection<? extends E> c) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified boolean addAll(@NotNull java.util.Collection<? extends E> p0) { return false; }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        void forEach(@NotNull java.util.function.Consumer<? super E> p0) { }
        @Nullable E peek() { return null; }
        @Nullable E peekFirst() { return null; }
        @Nullable E peekLast() { return null; }
        @Modified @Nullable E poll() { return null; }
        @Modified @Nullable E pollFirst() { return null; }
        @Modified @Nullable E pollLast() { return null; }
        @Modified boolean remove(@Nullable java.lang.Object p0) { return false; }
        @Modified boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
        @Modified boolean removeFirstOccurrence(@Nullable java.lang.Object p0) { return false; }
        @Modified boolean removeIf(@NotNull java.util.function.Predicate<? super E> p0) { return false; }
        @Modified boolean removeLastOccurrence(@Nullable java.lang.Object p0) { return false; }
        @Modified boolean retainAll(@NotNull java.util.Collection<?> p0) { return false; }
        <T> T[] toArray(@NotNull T[] p0) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified boolean offer(E p0) { return false; }
        @Modified boolean offerFirst(E p0) { return false; }
        @Modified boolean offerLast(E p0) { return false; }
        @Modified E pop() { return null; }
        @Modified void push(E p0) { }
        @Modified E remove() { return null; }
    }

    //public class ArrayList extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable
    @Container
    class ArrayList$<E> {
        ArrayList$(@NotNull @Independent(hc = true) @NotModified Collection<? extends E> c) { }
        @Modified void trimToSize() { }
        @Modified void ensureCapacity(int minCapacity) { }
        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        boolean isEmpty() { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object o) { return false; }

        //override from java.util.AbstractList, java.util.List
        int indexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object o) { return 0; }

        //override from java.util.AbstractList, java.util.List
        int lastIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object o) { return 0; }

        //override from java.lang.Object
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        @NotNull(nullableArguments = 0) Object [] toArray() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[O]*/ @NotNull T [] a) { return null; }

        //override from java.util.AbstractList, java.util.List
        //@Independent(hc=true)[H] @NotModified[H] @GetSet("_synthetic_list")[H]
        E get(int index) { return null; }

        //override from java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H] @NotModified[H]
        E getFirst() { return null; }

        //override from java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H] @NotModified[H]
        E getLast() { return null; }

        //override from java.util.AbstractList, java.util.List
        //@Independent(hc=true)[H] @GetSet("_synthetic_list")[H]
        @Modified E set(int index, /*@Independent(hc=true)[H] @NotModified[O]*/ E element) { return null; }

        //override from java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.List
        //frequency 16
        @Modified boolean add(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { return false; }

        //override from java.util.AbstractList, java.util.List
        @Modified void add(int index, /*@Independent(hc=true)[H] @NotModified[O]*/ E element) { }

        //override from java.util.List, java.util.SequencedCollection
        @Modified void addFirst(/*@Independent(hc=true)[H] @NotModified[O]*/ E element) { }

        //override from java.util.List, java.util.SequencedCollection
        @Modified void addLast(/*@Independent(hc=true)[H] @NotModified[O]*/ E element) { }

        //override from java.util.AbstractList, java.util.List
        //@Independent(hc=true)[H]
        @Modified E remove(int index) { return null; }

        //override from java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H]
        @Modified E removeFirst() { return null; }

        //override from java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H]
        @Modified E removeLast() { return null; }

        //override from java.lang.Object, java.util.AbstractList, java.util.Collection, java.util.List
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object o) { return false; }

        //override from java.lang.Object, java.util.AbstractList, java.util.Collection, java.util.List
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        @Modified boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object o) { return false; }

        //override from java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.List
        @Modified void clear() { }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //frequency 1
        @Modified boolean addAll(/*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull Collection<? extends E> c) { return false; }

        //override from java.util.AbstractList, java.util.List
        @Modified boolean addAll(int index, /*@NotModified[O]*/ @NotNull Collection<? extends E> c) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        @Modified boolean removeAll(/*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull Collection<?> c) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        @Modified boolean retainAll(/*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull Collection<?> c) { return false; }

        //override from java.util.AbstractList, java.util.List
        ListIterator<E> listIterator(int index) { return null; }

        //override from java.util.AbstractList, java.util.List
        ListIterator<E> listIterator() { return null; }

        //override from java.lang.Iterable, java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.List
        //@NotModified[H] @NotNull[H]
        Iterator<E> iterator() { return null; }

        //override from java.util.AbstractList, java.util.List
        //@NotModified[H]
        List<E> subList(int fromIndex, int toIndex) { return null; }

        //override from java.lang.Iterable
        //@NotModified[H]
        void forEach(/*@IgnoreModifications[T] @Independent[M] @NotNull[H]*/ @NotNull Consumer<? super E> action) { }

        //override from java.lang.Iterable, java.util.Collection, java.util.List
        //@Independent(hc=true)[H] @NotModified[H] @NotNull[H]
        Spliterator<E> spliterator() { return null; }

        //override from java.util.Collection
        @Modified boolean removeIf(/*@IgnoreModifications[T] @Independent(hc=true)[H]*/ @NotNull Predicate<? super E> filter) {
            return false;
        }

        //override from java.util.List
        @Modified void replaceAll(/*@IgnoreModifications[T]*/ @NotNull UnaryOperator<E> operator) { }

        //override from java.util.List
        //frequency 1
        @Modified void sort(/*@Immutable(hc=true)[T]*/ @Nullable @Independent(hc = true) @NotModified Comparator<? super E> c) { }
    }

    //public final class Arrays
    @UtilityClass
    class Arrays$ {
        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull int [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull int [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull long [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull long [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull short [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull short [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull char [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull char [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull byte [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull byte [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull float [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull float [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull double [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull double [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull byte [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull byte [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull char [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull char [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull short [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull short [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull int [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull int [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull long [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull long [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull float [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull float [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull double [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ @NotNull double [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static <T extends Comparable<? super T>> void parallelSort(/*@Independent[M]*/ @NotNull T [] a) { }

        //@NotModified[T]
        static <T extends Comparable<? super T>> void parallelSort(
            /*@Independent[M]*/ @NotNull T [] a,
            int fromIndex,
            int toIndex) { }

        //@NotModified[T]
        static <T> void parallelSort(
            /*@Independent[M]*/ @NotNull T [] a,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Comparator<? super T> cmp) { }

        //@NotModified[T]
        static <T> void parallelSort(
            /*@Independent[M]*/ @NotNull T [] a,
            int fromIndex,
            int toIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Comparator<? super T> cmp) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull Object [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ @NotNull Object [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static <T> void sort(
            /*@Independent[M]*/ @NotNull T [] a,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Comparator<? super T> c) { }

        //@NotModified[T]
        static <T> void sort(
            /*@Independent[M]*/ @NotNull T [] a,
            int fromIndex,
            int toIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Comparator<? super T> c) { }

        //@NotModified[T]
        static <T> void parallelPrefix(
            /*@Independent[M]*/ @NotNull T [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull BinaryOperator<T> op) { }

        //@NotModified[T]
        static <T> void parallelPrefix(
            /*@Independent[M]*/ @NotNull T [] array,
            int fromIndex,
            int toIndex,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull BinaryOperator<T> op) { }

        //@NotModified[T]
        static void parallelPrefix(
            /*@Independent[M]*/ @NotNull long [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull LongBinaryOperator op) { }

        //@NotModified[T]
        static void parallelPrefix(
            /*@Independent[M]*/ @NotNull long [] array,
            int fromIndex,
            int toIndex,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull LongBinaryOperator op) { }

        //@NotModified[T]
        static void parallelPrefix(
            /*@Independent[M]*/ @NotNull double [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull DoubleBinaryOperator op) { }

        //@NotModified[T]
        static void parallelPrefix(
            /*@Independent[M]*/ @NotNull double [] array,
            int fromIndex,
            int toIndex,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull DoubleBinaryOperator op) { }

        //@NotModified[T]
        static void parallelPrefix(
            /*@Independent[M]*/ @NotNull int [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntBinaryOperator op) { }

        //@NotModified[T]
        static void parallelPrefix(
            /*@Independent[M]*/ @NotNull int [] array,
            int fromIndex,
            int toIndex,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntBinaryOperator op) { }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified long [] a, long key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified long [] a, int fromIndex, int toIndex, long key) {
            return 0;
        }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified int [] a, int key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified int [] a, int fromIndex, int toIndex, int key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified short [] a, short key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified short [] a, int fromIndex, int toIndex, short key) {
            return 0;
        }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified char [] a, char key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified char [] a, int fromIndex, int toIndex, char key) {
            return 0;
        }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified byte [] a, byte key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified byte [] a, int fromIndex, int toIndex, byte key) {
            return 0;
        }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified double [] a, double key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified double [] a, int fromIndex, int toIndex, double key) {
            return 0;
        }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified float [] a, float key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotNull @NotModified float [] a, int fromIndex, int toIndex, float key) {
            return 0;
        }

        //@NotModified[T]
        static int binarySearch(
            /*@Independent[M]*/ @NotNull @NotModified Object [] a,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull Object key) { return 0; }

        //@NotModified[T]
        static int binarySearch(
            /*@Independent[M]*/ @NotNull @NotModified Object [] a,
            int fromIndex,
            int toIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull Object key) { return 0; }

        //@NotModified[T]
        static <T> int binarySearch(
            /*@Independent[M]*/ @NotNull @NotModified T [] a,
            /*@Independent[M] @NotModified[T]*/ T key,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Comparator<? super T> c) { return 0; }

        //@NotModified[T]
        static <T> int binarySearch(
            /*@Independent[M]*/ @NotNull @NotModified T [] a,
            int fromIndex,
            int toIndex,
            /*@Independent[M] @NotModified[T]*/ T key,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Comparator<? super T> c) { return 0; }

        //@NotModified[T]
        static boolean equals(/*@Independent[M]*/ @NotModified long [] a, /*@Independent[M]*/ @NotModified long [] a2) {
            return false;
        }

        //@NotModified[T]
        static boolean equals(
            /*@Independent[M]*/ @NotModified long [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotModified long [] b,
            int bFromIndex,
            int bToIndex) { return false; }

        //@NotModified[T]
        static boolean equals(/*@Independent[M]*/ @NotModified int [] a, /*@Independent[M]*/ @NotModified int [] a2) {
            return false;
        }

        //@NotModified[T]
        static boolean equals(
            /*@Independent[M]*/ @NotModified int [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotModified int [] b,
            int bFromIndex,
            int bToIndex) { return false; }

        //@NotModified[T]
        static boolean equals(/*@Independent[M]*/ @NotModified short [] a, /*@Independent[M]*/ @NotModified short [] a2) {
            return false;
        }

        //@NotModified[T]
        static boolean equals(
            /*@Independent[M]*/ @NotModified short [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotModified short [] b,
            int bFromIndex,
            int bToIndex) { return false; }

        //@NotModified[T]
        static boolean equals(/*@Independent[M]*/ @NotModified char [] a, /*@Independent[M]*/ @NotModified char [] a2) {
            return false;
        }

        //@NotModified[T]
        static boolean equals(
            /*@Independent[M]*/ char [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ char [] b,
            int bFromIndex,
            int bToIndex) { return false; }

        //@NotModified[T]
        static boolean equals(/*@Independent[M]*/ @NotModified byte [] a, /*@Independent[M]*/ @NotModified byte [] a2) {
            return false;
        }

        //@NotModified[T]
        static boolean equals(
            /*@Independent[M]*/ @NotModified byte [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotModified byte [] b,
            int bFromIndex,
            int bToIndex) { return false; }

        //@NotModified[T]
        static boolean equals(
            /*@Independent[M]*/ @NotModified boolean [] a,
            /*@Independent[M]*/ @NotModified boolean [] a2) { return false; }

        //@NotModified[T]
        static boolean equals(
            /*@Independent[M]*/ @NotModified boolean [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotModified boolean [] b,
            int bFromIndex,
            int bToIndex) { return false; }

        //@NotModified[T]
        static boolean equals(
            /*@Independent[M]*/ @NotModified double [] a,
            /*@Independent[M]*/ @NotModified double [] a2) { return false; }

        //@NotModified[T]
        static boolean equals(
            /*@Independent[M]*/ @NotModified double [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ double [] b,
            int bFromIndex,
            int bToIndex) { return false; }

        //@NotModified[T]
        static boolean equals(/*@Independent[M]*/ @NotModified float [] a, /*@Independent[M]*/ @NotModified float [] a2) {
            return false;
        }

        //@NotModified[T]
        static boolean equals(
            /*@Independent[M]*/ @NotModified float [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotModified float [] b,
            int bFromIndex,
            int bToIndex) { return false; }

        //frequency 1
        //@NotModified[T]

        static boolean equals(
            /*@Independent[M]*/ @NotModified Object [] a,
            /*@Independent[M]*/ @NotModified Object [] a2) { return false; }

        //@NotModified[T]
        static boolean equals(
            /*@Independent[M]*/ @NotModified Object [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotModified Object [] b,
            int bFromIndex,
            int bToIndex) { return false; }

        //@NotModified[T]
        static <T> boolean equals(
            /*@Independent[M]*/ @NotModified T [] a,
            /*@Independent[M]*/ @NotModified T [] a2,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> cmp) { return false; }

        //@NotModified[T]
        static <T> boolean equals(
            /*@Independent[M]*/ @NotModified T [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotModified T [] b,
            int bFromIndex,
            int bToIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> cmp) { return false; }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull long [] a, long val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull long [] a, int fromIndex, int toIndex, long val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull int [] a, int val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull int [] a, int fromIndex, int toIndex, int val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull short [] a, short val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull short [] a, int fromIndex, int toIndex, short val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull char [] a, char val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull char [] a, int fromIndex, int toIndex, char val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull byte [] a, byte val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull byte [] a, int fromIndex, int toIndex, byte val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull boolean [] a, boolean val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull boolean [] a, int fromIndex, int toIndex, boolean val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull double [] a, double val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull double [] a, int fromIndex, int toIndex, double val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull float [] a, float val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ @NotNull float [] a, int fromIndex, int toIndex, float val) { }

        //@NotModified[T]
        static void fill(
            @NotNull(nullableArguments = 0) @Independent(hcParameters = { 1 }) Object [] a,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Object val) { }

        //@NotModified[T]
        static void fill(
            @NotNull(nullableArguments = 0) @Independent(hcParameters = { 3 }) Object [] a,
            int fromIndex,
            int toIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Object val) { }

        //@Independent[O] @NotModified[T]
        @NotNull(nullableArguments = 0) static <T> T [] copyOf(@NotNull(nullableArguments = 0) @Independent(hcReturnValue = true) @NotModified T [] original, int newLength) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        @NotNull(nullableArguments = 0) static <T, U> T [] copyOf(
            @NotNull(nullableArguments = 0) @Independent(hcReturnValue = true) @NotModified U [] original,
            int newLength,
            @NotNull Class<? extends T []> newType) { return null; }

        //@Independent[O] @NotModified[T]
        static byte [] copyOf(/*@Independent[M]*/ @NotNull @NotModified byte [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static short [] copyOf(/*@Independent[M]*/ @NotNull @NotModified short [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static int [] copyOf(/*@Independent[M]*/ @NotNull @NotModified int [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static long [] copyOf(/*@Independent[M]*/ @NotNull @NotModified long [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static char [] copyOf(/*@Independent[M]*/ @NotNull @NotModified char [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static float [] copyOf(/*@Independent[M]*/ @NotNull @NotModified float [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static double [] copyOf(/*@Independent[M]*/ @NotNull @NotModified double [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static boolean [] copyOf(/*@Independent[M]*/ @NotNull @NotModified boolean [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        @NotNull(nullableArguments = 0) static <T> T [] copyOfRange(/*@Independent[M]*/ @NotNull(nullableArguments = 0) @NotModified T [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        @NotNull(nullableArguments = 0) static <T, U> T [] copyOfRange(
            @NotNull(nullableArguments = 0) @Independent(hcReturnValue = true) @NotModified U [] original,
            int from,
            int to,
            @NotNull Class<? extends T []> newType) { return null; }

        //@Independent[O] @NotModified[T]
        static byte [] copyOfRange(/*@Independent[M]*/ @NotNull @NotModified byte [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static short [] copyOfRange(/*@Independent[M]*/ @NotNull @NotModified short [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static int [] copyOfRange(/*@Independent[M]*/ @NotNull @NotModified int [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static long [] copyOfRange(/*@Independent[M]*/ @NotNull @NotModified long [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static char [] copyOfRange(/*@Independent[M]*/ @NotNull @NotModified char [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static float [] copyOfRange(/*@Independent[M]*/ @NotNull @NotModified float [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static double [] copyOfRange(/*@Independent[M]*/ @NotNull @NotModified double [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static boolean [] copyOfRange(/*@Independent[M]*/ @NotNull @NotModified boolean [] original, int from, int to) {
            return null;
        }

        //frequency 21
        //@Independent[O]
        @NotModified
        static <T> List<T> asList(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) T ... a) { return null; }

        //frequency 1
        @NotModified
        static int hashCode(/*@Independent[M]*/ @NotModified long [] a) { return 0; }

        //@NotModified[T]
        static int hashCode(/*@Independent[M]*/ int [] a) { return 0; }

        //@NotModified[T]
        static int hashCode(/*@Independent[M]*/ short [] a) { return 0; }

        //@NotModified[T]
        static int hashCode(/*@Independent[M]*/ char [] a) { return 0; }

        //@NotModified[T]
        static int hashCode(/*@Independent[M]*/ byte [] a) { return 0; }

        //@NotModified[T]
        static int hashCode(/*@Independent[M]*/ boolean [] a) { return 0; }

        //@NotModified[T]
        static int hashCode(/*@Independent[M]*/ float [] a) { return 0; }

        //@NotModified[T]
        static int hashCode(/*@Independent[M]*/ double [] a) { return 0; }

        //frequency 1
        @NotModified
        static int hashCode(/*@Independent[M]*/ @NotModified Object [] a) { return 0; }

        //@NotModified[T]
        static int deepHashCode(/*@Independent[M]*/ @Nullable(nullableArguments = 0) Object [] a) { return 0; }

        //@NotModified[T]
        static boolean deepEquals(/*@Independent[M]*/ @Nullable(nullableArguments = 0) Object [] a1, /*@Independent[M]*/ @Nullable(nullableArguments = 0) Object [] a2) { return false; }

        //@NotModified[T]
        static String toString(/*@Independent[M]*/ long [] a) { return null; }

        //@NotModified[T]
        static String toString(/*@Independent[M]*/ int [] a) { return null; }

        //@NotModified[T]
        static String toString(/*@Independent[M]*/ short [] a) { return null; }

        //@NotModified[T]
        static String toString(/*@Independent[M]*/ char [] a) { return null; }

        //@NotModified[T]
        static String toString(/*@Independent[M]*/ byte [] a) { return null; }

        //@NotModified[T]
        static String toString(/*@Independent[M]*/ boolean [] a) { return null; }

        //@NotModified[T]
        static String toString(/*@Independent[M]*/ float [] a) { return null; }

        //@NotModified[T]
        static String toString(/*@Independent[M]*/ double [] a) { return null; }

        //frequency 1
        @NotModified
        static String toString(/*@Independent[M]*/ @NotModified Object [] a) { return null; }

        //@NotModified[T]
        static String deepToString(/*@Independent[M]*/ @Nullable(nullableArguments = 0) @NotModified Object [] a) { return null; }

        //@NotModified[T]
        static <T> void setAll(
            /*@Independent[M]*/ @NotNull T [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntFunction<? extends T> generator) { }

        //@NotModified[T]
        static <T> void parallelSetAll(
            /*@Independent[M]*/ @NotNull T [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntFunction<? extends T> generator) { }

        //@NotModified[T]
        static void setAll(
            /*@Independent[M]*/ @NotNull int [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntUnaryOperator generator) { }

        //@NotModified[T]
        static void parallelSetAll(
            /*@Independent[M]*/ @NotNull int [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntUnaryOperator generator) { }

        //@NotModified[T]
        static void setAll(
            /*@Independent[M]*/ @NotNull long [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntToLongFunction generator) { }

        //@NotModified[T]
        static void parallelSetAll(
            /*@Independent[M]*/ @NotNull long [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntToLongFunction generator) { }

        //@NotModified[T]
        static void setAll(
            /*@Independent[M]*/ @NotNull double [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntToDoubleFunction generator) { }

        //@NotModified[T]
        static void parallelSetAll(
            /*@Independent[M]*/ @NotNull double [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntToDoubleFunction generator) { }

        //@Independent[O] @NotModified[T]
        static <T> Spliterator<T> spliterator(/*@Independent[M]*/ @NotNull T [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> Spliterator<T> spliterator(/*@Independent[M]*/ @NotNull T [] array, int startInclusive, int endExclusive) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static Spliterator.OfInt spliterator(/*@Independent[M]*/ @NotNull int [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static Spliterator.OfInt spliterator(/*@Independent[M]*/ @NotNull int [] array, int startInclusive, int endExclusive) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static Spliterator.OfLong spliterator(/*@Independent[M]*/ @NotNull long [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static Spliterator.OfLong spliterator(/*@Independent[M]*/ @NotNull long [] array, int startInclusive, int endExclusive) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static Spliterator.OfDouble spliterator(/*@Independent[M]*/ @NotNull double [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static Spliterator.OfDouble spliterator(
            /*@Independent[M]*/ @NotNull double [] array,
            int startInclusive,
            int endExclusive) { return null; }

        //frequency 15
        //@Independent[O]
        @NotModified
        static <T> Stream<T> stream(@NotNull @Independent(hc = true) @NotModified T [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> Stream<T> stream(/*@Independent[M]*/ @NotNull T [] array, int startInclusive, int endExclusive) { return null; }

        //@Independent[O] @NotModified[T]
        static IntStream stream(/*@Independent[M]*/ @NotNull int [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static IntStream stream(/*@Independent[M]*/ @NotNull int [] array, int startInclusive, int endExclusive) { return null; }

        //frequency 1
        //@Independent[O]
        @NotModified
        static LongStream stream(@NotNull @Independent @NotModified long [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static LongStream stream(/*@Independent[M]*/ @NotNull long [] array, int startInclusive, int endExclusive) { return null; }

        //@Independent[O] @NotModified[T]
        static DoubleStream stream(/*@Independent[M]*/ @NotNull double [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static DoubleStream stream(/*@Independent[M]*/ @NotNull double [] array, int startInclusive, int endExclusive) {
            return null;
        }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ @Nullable boolean [] a, /*@Independent[M]*/ @Nullable boolean [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ @NotNull boolean [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull boolean [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ @Nullable byte [] a, /*@Independent[M]*/ @Nullable byte [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ @NotNull byte [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull byte [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(/*@Independent[M]*/ @Nullable byte [] a, /*@Independent[M]*/ @Nullable byte [] b) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(
            /*@Independent[M]*/ @NotNull byte [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull byte [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ @Nullable short [] a, /*@Independent[M]*/ @Nullable short [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ @NotNull short [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull short [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(/*@Independent[M]*/ @Nullable short [] a, /*@Independent[M]*/ @Nullable short [] b) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(
            /*@Independent[M]*/ @NotNull short [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull short [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ @Nullable char [] a, /*@Independent[M]*/ @Nullable char [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ @NotNull char [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull char [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ @Nullable int [] a, /*@Independent[M]*/ @Nullable int [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ @NotNull int [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull int [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(/*@Independent[M]*/ @Nullable int [] a, /*@Independent[M]*/ @Nullable int [] b) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(
            /*@Independent[M]*/ @NotNull int [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull int [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ @Nullable long [] a, /*@Independent[M]*/ @Nullable long [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ @NotNull long [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull long [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(/*@Independent[M]*/ @Nullable long [] a, /*@Independent[M]*/ @Nullable long [] b) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(
            /*@Independent[M]*/ @NotNull long [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull long [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ @Nullable float [] a, /*@Independent[M]*/ @Nullable float [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ @NotNull float [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull float [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ @Nullable double [] a, /*@Independent[M]*/ @Nullable double [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ @NotNull double [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull double [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static <T extends Comparable<? super T>> int compare(/*@Independent[M]*/ @Nullable(nullableArguments = 0) T [] a, /*@Independent[M]*/ @Nullable(nullableArguments = 0) T [] b) {
            return 0;
        }

        //@NotModified[T]
        static <T extends Comparable<? super T>> int compare(
            /*@Independent[M]*/ @NotNull T [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull T [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static <T> int compare(
            /*@Independent[M]*/ @Nullable T [] a,
            /*@Independent[M]*/ @Nullable T [] b,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull Comparator<? super T> cmp) { return 0; }

        //@NotModified[T]
        static <T> int compare(
            /*@Independent[M]*/ @NotNull T [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull T [] b,
            int bFromIndex,
            int bToIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull Comparator<? super T> cmp) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ @NotNull boolean [] a, /*@Independent[M]*/ @NotNull boolean [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ @NotNull boolean [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull boolean [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ @NotNull byte [] a, /*@Independent[M]*/ @NotNull byte [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ @NotNull byte [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull byte [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ @NotNull char [] a, /*@Independent[M]*/ @NotNull char [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ @NotNull char [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull char [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ @NotNull short [] a, /*@Independent[M]*/ @NotNull short [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ @NotNull short [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull short [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ @NotNull int [] a, /*@Independent[M]*/ @NotNull int [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ @NotNull int [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull int [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ @NotNull long [] a, /*@Independent[M]*/ @NotNull long [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ @NotNull long [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull long [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ @NotNull float [] a, /*@Independent[M]*/ @NotNull float [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ @NotNull float [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull float [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ @NotNull double [] a, /*@Independent[M]*/ @NotNull double [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ @NotNull double [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull double [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ @NotNull(nullableArguments = 0) Object [] a, /*@Independent[M]*/ @NotNull(nullableArguments = 0) Object [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ @NotNull(nullableArguments = 0) Object [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull(nullableArguments = 0) Object [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static <T> int mismatch(
            /*@Independent[M]*/ @NotNull T [] a,
            /*@Independent[M]*/ @NotNull T [] b,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull Comparator<? super T> cmp) { return 0; }

        //@NotModified[T]
        static <T> int mismatch(
            /*@Independent[M]*/ @NotNull T [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ @NotNull T [] b,
            int bFromIndex,
            int bToIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @NotNull Comparator<? super T> cmp) { return 0; }
    }

    //public class Base64
    @UtilityClass
    class Base64$ {
        //public static class Decoder
        @UtilityClass
        class Decoder {
            //@Independent[O] @NotModified[T]
            byte [] decode(/*@Independent[M]*/ @NotModified byte [] src) { return null; }

            //frequency 2
            //@Independent[O] @NotModified[T]
            byte [] decode(String src) { return null; }

            //@NotModified[T]
            int decode(/*@Independent[M]*/ @NotModified byte [] src, /*@Independent[M]*/ byte [] dst) { return 0; }

            //@Independent[O] @NotModified[T]
            ByteBuffer decode(/*@Independent[M]*/ ByteBuffer buffer) { return null; }

            //@Independent[O] @NotModified[T]
            InputStream wrap(/*@Independent[M]*/ InputStream is) { return null; }
        }

        //public static class Encoder
        @UtilityClass
        class Encoder {
            //@Independent[O] @NotModified[T]
            byte [] encode(/*@Independent[M]*/ @NotModified byte [] src) { return null; }

            //@NotModified[T]
            int encode(/*@Independent[M]*/ @NotModified byte [] src, /*@Independent[M]*/ @Modified byte [] dst) { return 0; }

            //frequency 2
            //@NotModified[T]
            String encodeToString(/*@Independent[M]*/ @NotModified byte [] src) { return null; }

            //@Independent[O] @NotModified[T]
            ByteBuffer encode(/*@Independent[M]*/ ByteBuffer buffer) { return null; }

            //@Independent[O] @NotModified[T]
            OutputStream wrap(/*@Independent[M]*/ OutputStream os) { return null; }

            //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
            Base64.Encoder withoutPadding() { return null; }
        }

        //frequency 1
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static Base64.Encoder getEncoder() { return null; }

        //frequency 1
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static Base64.Encoder getUrlEncoder() { return null; }

        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static Base64.Encoder getMimeEncoder() { return null; }

        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static Base64.Encoder getMimeEncoder(int lineLength, /*@Independent[T]*/ byte [] lineSeparator) { return null; }

        //frequency 1
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static Base64.Decoder getDecoder() { return null; }

        //frequency 1
        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static Base64.Decoder getUrlDecoder() { return null; }

        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static Base64.Decoder getMimeDecoder() { return null; }
    }

    //public class BitSet implements Cloneable, Serializable
    // mutable, but no method modifies a parameter's content (and(BitSet) etc. read the argument).
    // set/clear/flip/and/or/xor/andNot modify this and stay @Modified; the queries below do not.
    @Container
    class BitSet$ {
        @NotModified boolean get(int bitIndex) { return false; }
        @NotModified BitSet get(int fromIndex, int toIndex) { return null; }
        @NotModified int cardinality() { return 0; }
        @NotModified boolean isEmpty() { return false; }
        @NotModified int length() { return 0; }
        @NotModified int size() { return 0; }
        @NotModified int nextSetBit(int fromIndex) { return 0; }
        @NotModified int nextClearBit(int fromIndex) { return 0; }
        @NotModified int previousSetBit(int fromIndex) { return 0; }
        @NotModified int previousClearBit(int fromIndex) { return 0; }
        @NotModified boolean intersects(@NotNull BitSet set) { return false; }
        @NotModified byte [] toByteArray() { return null; }
        @NotModified long [] toLongArray() { return null; }
        @NotModified IntStream stream() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void and(@NotNull java.util.BitSet p0) { }
        void andNot(@NotNull java.util.BitSet p0) { }
        void or(@NotNull java.util.BitSet p0) { }
        static java.util.BitSet valueOf(@NotNull java.nio.ByteBuffer p0) { return null; }
        static java.util.BitSet valueOf(@NotNull java.nio.LongBuffer p0) { return null; }
        static java.util.BitSet valueOf(@NotNull byte[] p0) { return null; }
        static java.util.BitSet valueOf(@NotNull long[] p0) { return null; }
        void xor(@NotNull java.util.BitSet p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void clear() { }
        @Modified void clear(int p0) { }
        @Modified void clear(int p0, int p1) { }
        @Modified void set(int p0) { }
        @Modified void set(int p0, boolean p1) { }
        @Modified void set(int p0, int p1) { }
        @Modified void set(int p0, int p1, boolean p2) { }
    }

    //public interface Collection implements Iterable<E>
    @Container
    class Collection$<E> {
        //override has frequency 12
        @NotModified
        int size() { return 0; }

        //override has frequency 18
        @NotModified
        boolean isEmpty() { return false; }

        //frequency 4
        @NotModified
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object object) { return false; }

        //override from java.lang.Iterable
        //override has frequency 2
        //@NotNull[H]
        @NotModified
        Iterator<E> iterator() { return null; }

        //override has frequency 3
        @NotNull(nullableArguments = 0) @Independent(hcReturnValue = true)
        @NotModified
        Object [] toArray() { return null; }

        //override has frequency 3
        @Independent(hcReturnValue = true)
        @NotModified
        <T> T [] toArray(/*@Independent[M] @NotModified[O]*/ @NotNull T [] t) { return null; }

        @Independent(hcReturnValue = true)
        @NotModified
        <T> T [] toArray(/*@IgnoreModifications[T]*/ @NotNull IntFunction<T []> generator) { return null; }

        //frequency 2
        @Modified boolean add(/*@Independent(hc=true)[T] @NotModified[O]*/ E e) { return false; }

        //override has frequency 3
        @Modified boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ @Nullable Object object) {
            return false;
        }

        @NotModified
        boolean containsAll(/*@NotModified[O]*/ @NotNull @Independent(hc = true) Collection<?> collection) { return false; }

        //override has frequency 5
        @Modified boolean addAll(/*@NotModified[O]*/ @NotNull @Independent(hc = true) Collection<? extends E> collection) { return false; }
        @Modified boolean removeAll(/*@NotModified[O]*/ @NotNull @Independent(hc = true) Collection<?> collection) { return false; }
        //frequency 3
        @Modified boolean removeIf(/*@IgnoreModifications[T]*/ @NotNull @Independent(hc = true) Predicate<? super E> filter) {
            return false;
        }

        //override has frequency 1
        @Modified boolean retainAll(/*@NotModified[O]*/ @NotNull @Independent(hc = true) Collection<?> collection) { return false; }
        @Modified void clear() { }
        //override from java.lang.Object
        //override has frequency 1
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object object) { return false; }

        //override from java.lang.Object
        //override has frequency 4
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Iterable
        //@Independent(hc=true)[H] @NotModified[H] @NotNull[H]
        Spliterator<E> spliterator() { return null; }

        //frequency 238
        @Independent(hc = true) @NotModified @NotNull(content = true)
        Stream<E> stream() { return null; }
        @Independent(hc = true) @NotModified @NotNull(content = true) Stream<E> parallelStream() { return null; }
    }

    //public class Collections
    @UtilityClass
    class Collections$ {
        //@NotModified[O]
        static final Set EMPTY_SET = null;

        //@NotModified[O]
        static final List EMPTY_LIST = null;

        //@NotModified[O]
        static final Map EMPTY_MAP = null;

        //@NotModified[T]
        static <T extends Comparable<? super T>> void sort(/*@Independent[M]*/ @NotNull List<T> list) { }

        //@NotModified[T]
        static <T> void sort(
            /*@Independent[M]*/ @NotNull List<T> list,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Comparator<? super T> c) { }

        //@NotModified[T]
        static <T> int binarySearch(
            /*@Independent[M]*/ @NotNull @NotModified List<? extends Comparable<? super T>> list,
            /*@Independent[M] @NotModified[T]*/ T key) { return 0; }

        //@NotModified[T]
        static <T> int binarySearch(
            /*@Independent[M]*/ @NotNull @NotModified List<? extends T> list,
            /*@Independent[M] @NotModified[T]*/ T key,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Comparator<? super T> c) { return 0; }

        //frequency 1
        //@NotModified[T]
        static void reverse(/*@Independent[M]*/ @NotNull List<?> list) { }

        //frequency 2
        //@NotModified[T]
        static void shuffle(/*@Independent[M]*/ @NotNull List<?> list) { }

        //@NotModified[T]
        static void shuffle(/*@Independent[M]*/ @NotNull List<?> list, /*@Independent[M]*/ @NotNull Random rnd) { }

        //@NotModified[T]
        static void shuffle(/*@Independent[M]*/ @NotNull List<?> list, /*@Independent[M]*/ @NotNull RandomGenerator rnd) { }

        //@NotModified[T]
        static void swap(/*@Independent[M]*/ @NotNull List<?> list, int i, int j) { }

        //@NotModified[T]
        static <T> void fill(
            @NotNull @Independent(hcParameters = { 1 }) List<? super T> list,
            /*@Independent[M] @NotModified[T]*/ T obj) { }

        //@NotModified[T]
        static <T> void copy(
            @NotNull @Independent(hcParameters = { 1 }) List<? super T> dest,
            /*@Independent[M]*/ @NotNull @NotModified List<? extends T> src) { }

        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static <T extends Comparable<? super T>> T min(/*@Independent[M]*/ @NotNull @NotModified Collection<? extends T> coll) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> T min(
            /*@Independent[M]*/ @NotNull @NotModified Collection<? extends T> coll,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Comparator<? super T> comp) { return null; }

        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static <T extends Comparable<? super T>> T max(/*@Independent[M]*/ @NotNull @NotModified Collection<? extends T> coll) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> T max(
            /*@Independent[M]*/ @NotNull @NotModified Collection<? extends T> coll,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Comparator<? super T> comp) { return null; }

        //@NotModified[T]
        static void rotate(/*@Independent[M]*/ @NotNull List<?> list, int distance) { }

        //@NotModified[T]
        static <T> boolean replaceAll(
            @NotNull @Independent(hcParameters = { 2 }) List<T> list,
            /*@Independent[M] @NotModified[T]*/ @Nullable T oldVal,
            /*@Independent[M] @NotModified[T]*/ T newVal) { return false; }

        //@NotModified[T]
        static int indexOfSubList(/*@Independent[M]*/ @NotNull @NotModified List<?> source, /*@Independent[M]*/ @NotNull @NotModified List<?> target) { return 0; }

        //@NotModified[T]
        static int lastIndexOfSubList(/*@Independent[M]*/ @NotNull @NotModified List<?> source, /*@Independent[M]*/ @NotNull @NotModified List<?> target) { return 0; }

        //@Independent[O] @NotModified[T]
        @UnmodifiableView static <T> Collection<T> unmodifiableCollection(
            /*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) Collection<? extends T> c) { return null; }

        //@Independent[O] @NotModified[T]
        @UnmodifiableView static <T> SequencedCollection<T> unmodifiableSequencedCollection(
            /*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) SequencedCollection<? extends T> c) { return null; }

        //@Independent[O] @NotModified[T]
        @UnmodifiableView static <T> Set<T> unmodifiableSet(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) Set<? extends T> s) { return null; }

        //@Independent[O] @NotModified[T]
        @UnmodifiableView static <T> SequencedSet<T> unmodifiableSequencedSet(
            /*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) SequencedSet<? extends T> s) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        @UnmodifiableView static <T> SortedSet<T> unmodifiableSortedSet(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) SortedSet<T> s) { return null; }

        //@Independent[O] @NotModified[T]
        @UnmodifiableView static <T> NavigableSet<T> unmodifiableNavigableSet(
            /*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) NavigableSet<T> s) { return null; }

        //@Independent[O] @NotModified[T]
        @UnmodifiableView static <T> List<T> unmodifiableList(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) List<? extends T> list) { return null; }

        //@Independent[O] @NotModified[T]
        @UnmodifiableView static <K, V> Map<K, V> unmodifiableMap(
            /*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) Map<? extends K, ? extends V> m) { return null; }

        //@Independent[O] @NotModified[T]
        @UnmodifiableView static <K, V> SequencedMap<K, V> unmodifiableSequencedMap(
            /*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) SequencedMap<? extends K, ? extends V> m) { return null; }

        //@Independent[O] @NotModified[T]
        @UnmodifiableView static <K, V> SortedMap<K, V> unmodifiableSortedMap(
            /*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) SortedMap<K, ? extends V> m) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        @UnmodifiableView static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(
            /*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) NavigableMap<K, ? extends V> m) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        // the view factories (synchronized*, checked*, newSetFromMap, asLifoQueue): the call modifies nothing, a
        // modification through the returned view modifies the argument (dependentReturnValue, CodeLaser/maddi-mod#40)
        static <T> Collection<T> synchronizedCollection(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) Collection<T> c) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> Set<T> synchronizedSet(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) Set<T> s) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> SortedSet<T> synchronizedSortedSet(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) SortedSet<T> s) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> NavigableSet<T> synchronizedNavigableSet(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) NavigableSet<T> s) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> List<T> synchronizedList(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) List<T> list) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> Map<K, V> synchronizedMap(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) Map<K, V> m) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> SortedMap<K, V> synchronizedSortedMap(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) SortedMap<K, V> m) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> NavigableMap<K, V> synchronizedNavigableMap(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) NavigableMap<K, V> m) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static <E> Collection<E> checkedCollection(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) Collection<E> c, @NotNull Class<E> type) { return null; }

        //@Independent[O] @NotModified[T]
        static <E> Queue<E> checkedQueue(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) Queue<E> queue, @NotNull Class<E> type) { return null; }

        //@Independent[O] @NotModified[T]
        static <E> Set<E> checkedSet(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) Set<E> s, @NotNull Class<E> type) { return null; }

        //@Independent[O] @NotModified[T]
        static <E> SortedSet<E> checkedSortedSet(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) SortedSet<E> s, @NotNull Class<E> type) { return null; }

        //@Independent[O] @NotModified[T]
        static <E> NavigableSet<E> checkedNavigableSet(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) NavigableSet<E> s, @NotNull Class<E> type) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static <E> List<E> checkedList(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) List<E> list, @NotNull Class<E> type) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> Map<K, V> checkedMap(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) Map<K, V> m, @NotNull Class<K> keyType, @NotNull Class<V> valueType) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static <K, V> SortedMap<K, V> checkedSortedMap(
            /*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) SortedMap<K, V> m,
            @NotNull Class<K> keyType,
            @NotNull Class<V> valueType) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> NavigableMap<K, V> checkedNavigableMap(
            /*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) NavigableMap<K, V> m,
            @NotNull Class<K> keyType,
            @NotNull Class<V> valueType) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> Iterator<T> emptyIterator() { return null; }

        //@Independent[O] @NotModified[T]
        static <T> ListIterator<T> emptyListIterator() { return null; }

        //@Independent[O] @NotModified[T]
        static <T> Enumeration<T> emptyEnumeration() { return null; }

        //frequency 15
        //@Independent[O] @NotModified[T]
        static <T> Set<T> emptySet() { return null; }

        //@Independent[O] @NotModified[T]
        static <E> SortedSet<E> emptySortedSet() { return null; }

        //@Independent[O] @NotModified[T]
        static <E> NavigableSet<E> emptyNavigableSet() { return null; }

        //frequency 13
        //@Independent[O] @NotModified[T]
        static <T> List<T> emptyList() { return null; }

        //frequency 1
        //@Independent[O] @NotModified[T]
        static <K, V> Map<K, V> emptyMap() { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> SortedMap<K, V> emptySortedMap() { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> NavigableMap<K, V> emptyNavigableMap() { return null; }

        //frequency 83
        //@Independent[O] @NotModified[T]
        static <T> Set<T> singleton(/*@Independent[M] @NotModified[T]*/ T o) { return null; }

        //frequency 27
        //@Independent[O] @NotModified[T]
        static <T> List<T> singletonList(/*@Independent[M] @NotModified[T]*/ T o) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> Map<K, V> singletonMap(
            /*@Independent[M] @NotModified[T]*/ K key,
            /*@Independent[M] @NotModified[T]*/ V value) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> List<T> nCopies(int n, /*@Independent[M] @NotModified[T]*/ T o) { return null; }

        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static <T> Comparator<T> reverseOrder() { return null; }

        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static <T> Comparator<T> reverseOrder(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Comparator<T> cmp) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> Enumeration<T> enumeration(/*@Independent[M]*/ @NotNull @NotModified @Independent(hc = true) Collection<T> c) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> ArrayList<T> list(/*@Independent[M]*/ @NotNull Enumeration<T> e) { return null; }

        //@NotModified[T]
        static int frequency(
            /*@Independent[M]*/ @NotNull @NotModified Collection<?> c,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Object o) { return 0; }

        //@NotModified[T]
        static boolean disjoint(/*@Independent[M]*/ @NotNull @NotModified Collection<?> c1, /*@Independent[M]*/ @NotNull @NotModified Collection<?> c2) {
            return false;
        }

        //@NotModified[T]
        static <T> boolean addAll(
            @Independent(hcParameters = { 1 }) @NotNull Collection<? super T> c,
            /*@Independent[M]*/ @NotNull @NotModified T ... elements) { return false; }

        //@Independent[O] @NotModified[T]
        static <E> Set<E> newSetFromMap(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) Map<E, Boolean> map) { return null; }

        //@Independent[O] @NotModified[T]
        static <E> SequencedSet<E> newSequencedSetFromMap(/*@Independent[M]*/ @NotNull SequencedMap<E, Boolean> map) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static <T> Queue<T> asLifoQueue(/*@Independent[M]*/ @NotNull @NotModified @Independent(dependentReturnValue = true) Deque<T> deque) { return null; }
    }

    //public interface Comparator
    @ImmutableContainer(hc = true)
    class Comparator$<T> {
        //@NotModified[T]
        int compare(/*@Independent[M] @NotModified[O]*/ T t, /*@Independent[M] @NotModified[O]*/ T t1) { return 0; }

        //override from java.lang.Object
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object object) { return false; }

        //frequency 3
        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Comparator<T> reversed() { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Comparator<T> thenComparing(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull Comparator<? super T> other) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        <U> Comparator<T> thenComparing(
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull Function<? super T, ? extends U> keyExtractor,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull Comparator<? super U> keyComparator) {
            return null;
        }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        <U extends Comparable<? super U>> Comparator<T> thenComparing(
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull Function<? super T, ? extends U> keyExtractor) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Comparator<T> thenComparingInt(
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull ToIntFunction<? super T> keyExtractor) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Comparator<T> thenComparingLong(
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull ToLongFunction<? super T> keyExtractor) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Comparator<T> thenComparingDouble(
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull ToDoubleFunction<? super T> keyExtractor) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T extends Comparable<? super T>> Comparator<T> reverseOrder() { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T extends Comparable<? super T>> Comparator<T> naturalOrder() { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        @NotNull(nullableArguments = 0) static <T> Comparator<T> nullsFirst(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ @Nullable Comparator<? super T> comparator) {
            return null;
        }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        @NotNull(nullableArguments = 0) static <T> Comparator<T> nullsLast(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ @Nullable Comparator<? super T> comparator) {
            return null;
        }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T, U> Comparator<T> comparing(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull Function<? super T, ? extends U> keyExtractor,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ @NotNull Comparator<? super U> keyComparator) {
            return null;
        }

        //frequency 11
        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]

        static <T, U extends Comparable<? super U>> Comparator<T> comparing(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull Function<? super T, ? extends U> keyExtractor) {
            return null;
        }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T> Comparator<T> comparingInt(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull ToIntFunction<? super T> keyExtractor) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T> Comparator<T> comparingLong(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull ToLongFunction<? super T> keyExtractor) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T> Comparator<T> comparingDouble(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ @NotNull ToDoubleFunction<? super T> keyExtractor) {
            return null;
        }
    }

    //public final class Currency implements Serializable
    @ImmutableContainer
    class Currency$ { 
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.lang.String getDisplayName(@NotNull java.util.Locale p0) { return null; }
        @Nullable static java.util.Currency getInstance(@NotNull java.util.Locale p0) { return null; }
        static java.util.Currency getInstance(@NotNull java.lang.String p0) { return null; }
        java.lang.String getSymbol(@NotNull java.util.Locale p0) { return null; }
    }

    //public class Date implements Serializable, Cloneable, Comparable<Date>
    class Date$ {
        //frequency 7
        Date$() { }

        //frequency 1
        Date$(long date) { }
        Date$(int year, int month, int date) { }
        Date$(int year, int month, int date, int hrs, int min) { }
        Date$(int year, int month, int date, int hrs, int min, int sec) { }
        Date$(@NotNull String s) { }
        //override from java.lang.Object
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }
        static long UTC(int year, int month, int date, int hrs, int min, int sec) { return 0L; }
        static long parse(@NotNull String s) { return 0L; }
        @NotModified int getYear() { return 0; }
        @Modified void setYear(int year) { }
        @NotModified int getMonth() { return 0; }
        @Modified void setMonth(int month) { }
        @NotModified int getDate() { return 0; }
        @Modified void setDate(int date) { }
        @NotModified int getDay() { return 0; }
        @NotModified int getHours() { return 0; }
        @Modified void setHours(int hours) { }
        @NotModified int getMinutes() { return 0; }
        @Modified void setMinutes(int minutes) { }
        @NotModified int getSeconds() { return 0; }
        @Modified void setSeconds(int seconds) { }
        //frequency 1
        @NotModified
        long getTime() { return 0L; }
        @Modified void setTime(long time) { }
        @NotModified boolean before(/*@Independent[M] @NotModified[O]*/ @NotNull Date when) { return false; }
        @NotModified boolean after(/*@Independent[M] @NotModified[O]*/ @NotNull Date when) { return false; }
        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull Date anotherDate) { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        @NotModified String toLocaleString() { return null; }
        @NotModified String toGMTString() { return null; }
        @NotModified int getTimezoneOffset() { return 0; }
        static Date from(@NotNull Instant instant) { return null; }
        @NotModified Instant toInstant() { return null; }
    }

    //public interface EventListener
    //public interface Deque<E> extends Queue<E>, SequencedCollection<E>
    @Container
    class Deque$<E> {
        @Modified @Nullable E pollFirst() { return null; }
        @Modified @Nullable E pollLast() { return null; }
        @Nullable @NotModified E peekFirst() { return null; }
        @Nullable @NotModified E peekLast() { return null; }
        @NotModified E element() { return null; }
        @Nullable @NotModified E peek() { return null; }
        @NotModified @Independent(hc = true, except = "remove") Iterator<E> descendingIterator() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified boolean addAll(@NotNull java.util.Collection<? extends E> p0) { return false; }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        @Modified @Nullable E poll() { return null; }
        @Modified boolean remove(@Nullable java.lang.Object p0) { return false; }
        @Modified boolean removeFirstOccurrence(@Nullable java.lang.Object p0) { return false; }
        @Modified boolean removeLastOccurrence(@Nullable java.lang.Object p0) { return false; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified boolean offer(E p0) { return false; }
        @Modified boolean offerFirst(E p0) { return false; }
        @Modified boolean offerLast(E p0) { return false; }
        @Modified E pop() { return null; }
        @Modified void push(E p0) { }
        @Modified E remove() { return null; }
    }

    //public abstract class Dictionary<K, V>
    @Container
    class Dictionary$<K, V> {
        @NotModified int size() { return 0; }
        @NotModified boolean isEmpty() { return false; }
        @NotModified Enumeration<K> keys() { return null; }
        @NotModified Enumeration<V> elements() { return null; }
        @Nullable @NotModified V get(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull Object key) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified @Nullable V put(K p0, V p1) { return null; }
        @Modified @Nullable V remove(@NotNull java.lang.Object p0) { return null; }
    }

    //public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> implements Cloneable, Serializable
    @Container
    class EnumMap$<K extends Enum<K>, V> {
        EnumMap$(/*@NotModified[O]*/ @Independent(hc = true) @NotNull Map<K, ? extends V> m) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        EnumMap$(@NotNull java.lang.Class<K> p0) { }
        EnumMap$(@NotNull java.util.EnumMap<K, ? extends V> p0) { }
        boolean containsKey(@Nullable java.lang.Object p0) { return false; }
        boolean containsValue(@Nullable java.lang.Object p0) { return false; }
        @Nullable V get(@Nullable java.lang.Object p0) { return null; }
        @Modified @Nullable V put(K p0, V p1) { return null; }
        @Modified void putAll(@NotNull java.util.Map<? extends K, ? extends V> p0) { }
        @Modified @Nullable V remove(@Nullable java.lang.Object p0) { return null; }
    }

    //public abstract class EnumSet<E extends Enum<E>> extends AbstractSet<E> implements Cloneable, Serializable
    @Container
    class EnumSet$<E> { 
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static <E extends java.lang.Enum<E>> java.util.EnumSet<E> allOf(@NotNull java.lang.Class<E> p0) { return null; }
        static <E extends java.lang.Enum<E>> java.util.EnumSet<E> complementOf(@NotNull java.util.EnumSet<E> p0) { return null; }
        static <E extends java.lang.Enum<E>> java.util.EnumSet<E> copyOf(@NotNull java.util.Collection<E> p0) { return null; }
        static <E extends java.lang.Enum<E>> java.util.EnumSet<E> copyOf(@NotNull java.util.EnumSet<E> p0) { return null; }
        static <E extends java.lang.Enum<E>> java.util.EnumSet<E> noneOf(@NotNull java.lang.Class<E> p0) { return null; }
        static <E extends java.lang.Enum<E>> java.util.EnumSet<E> of(E p0, @NotNull E... p1) { return null; }
    }

    //public interface Enumeration<E>
    @Container
    @Independent(hc = true)
    class Enumeration$<E> {
        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified E nextElement() { return null; }
    }

    //@Container[M]
    @Independent(hc = true) class EventListener$ { }

    //public class HashMap extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable
    @Container
    class HashMap$<K, V> {
        HashMap$(/*@NotModified[O]*/ @Independent(hc = true) @NotNull Map<? extends K, ? extends V> m) { }
        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        boolean isEmpty() { return false; }

        //override from java.util.AbstractMap, java.util.Map
        //frequency 2
        //@Independent(hc=true)[H] @NotModified[H]
        @Nullable V get(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object key) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        //frequency 2
        //@NotModified[H]
        boolean containsKey(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object key) { return false; }

        //override from java.util.AbstractMap, java.util.Map
        //frequency 4
        //@Independent(hc=true)[H]

        @Modified @Nullable V put(/*@Independent(hc=true)[H] @NotModified[O]*/ K key, /*@Independent(hc=true)[H] @NotModified[O]*/ V value) {
            return null;
        }

        //override from java.util.AbstractMap, java.util.Map
        @Modified void putAll(/*@NotModified[O]*/ @NotNull Map<? extends K, ? extends V> m) { }

        //override from java.util.AbstractMap, java.util.Map
        //@Independent(hc=true)[H]
        @Modified @Nullable V remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object key) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        @Modified void clear() { }

        //override from java.util.AbstractMap, java.util.Map
        boolean containsValue(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object value) {
            return false;
        }

        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        Set<K> keySet() { return null; }

        //override from java.util.AbstractMap, java.util.Map
        //frequency 2
        //@NotModified[H]
        Collection<V> values() { return null; }

        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        Set<Map.Entry<K, V>> entrySet() { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H] @NotModified[H]

        V getOrDefault(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object key,
            /*@Independent[M] @NotModified[O]*/ @Nullable V defaultValue) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Modified @Nullable V putIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V value) { return null; }

        //override from java.util.Map
        @Modified boolean remove(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object key,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object value) { return false; }

        //override from java.util.Map
        @Modified boolean replace(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V oldValue,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V newValue) { return false; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Modified @Nullable V replace(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V value) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Modified V computeIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ @NotNull Function<? super K, ? extends V> mappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Modified @Nullable V computeIfPresent(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 2) BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Modified V compute(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {1, 2}) BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Modified V merge(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull V value,
            /*@IgnoreModifications[T] @Independent(hc=true)[H]*/ @NotNull(nullableArguments = 2) BiFunction<? super V, ? super V, ? extends V>
                remappingFunction) { return null; }

        //override from java.util.Map
        //@NotModified[H]
        void forEach(/*@IgnoreModifications[T] @Independent[M]*/ @NotNull BiConsumer<? super K, ? super V> action) { }

        //override from java.util.Map
        @Modified void replaceAll(/*@IgnoreModifications[T]*/ @NotNull BiFunction<? super K, ? super V, ? extends V> function) { }

        //override from java.lang.Object, java.util.AbstractMap
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }
        static <K, V> HashMap<K, V> newHashMap(int numMappings) { return null; }
    }

    //public class HashSet extends AbstractSet<E> implements Set<E>, Cloneable, Serializable
    @Container
    class HashSet$<E> {
        HashSet$(/*@NotModified[O]*/ @Independent(hc = true) @NotNull Collection<? extends E> c) { }
        //override from java.lang.Iterable, java.util.AbstractCollection, java.util.Collection, java.util.Set
        //@NotModified[H] @NotNull[H]
        Iterator<E> iterator() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        //@NotModified[H]
        boolean isEmpty() { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        //@NotModified[H]
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object o) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        @Modified boolean add(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        @Modified boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object o) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        @Modified void clear() { }

        //override from java.lang.Object
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }

        //override from java.lang.Iterable, java.util.Collection, java.util.Set
        //@Independent(hc=true)[H] @NotModified[H] @NotNull[H]
        Spliterator<E> spliterator() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        //@NotModified[H]
        @NotNull(nullableArguments = 0) Object [] toArray() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[O]*/ @NotNull T [] a) { return null; }
        static <T> HashSet<T> newHashSet(int numElements) { return null; }
    }

    //public class Hashtable<K, V> extends Dictionary<K, V> implements Map<K, V>, Cloneable, Serializable
    @Container
    class Hashtable$<K, V> {
        Hashtable$(/*@NotModified[O]*/ @Independent(hc = true) @NotNull Map<? extends K, ? extends V> m) { }
        @NotModified boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull Object value) {
            return false;
        }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified V compute(K p0, @NotNull(nullableArguments = {1, 2}) java.util.function.BiFunction<? super K, ? super V, ? extends V> p1) { return null; }
        @Modified V computeIfAbsent(K p0, @NotNull java.util.function.Function<? super K, ? extends V> p1) { return null; }
        @Modified @Nullable V computeIfPresent(K p0, @NotNull(nullableArguments = 2) java.util.function.BiFunction<? super K, ? super V, ? extends V> p1) { return null; }
        boolean containsKey(@NotNull java.lang.Object p0) { return false; }
        boolean containsValue(@NotNull java.lang.Object p0) { return false; }
        void forEach(@NotNull java.util.function.BiConsumer<? super K, ? super V> p0) { }
        @Nullable V get(@NotNull java.lang.Object p0) { return null; }
        V getOrDefault(@NotNull java.lang.Object p0, @Nullable V p1) { return null; }
        @Modified V merge(K p0, V p1, @NotNull(nullableArguments = 2) java.util.function.BiFunction<? super V, ? super V, ? extends V> p2) { return null; }
        @Modified @Nullable V put(K p0, V p1) { return null; }
        @Modified void putAll(@NotNull java.util.Map<? extends K, ? extends V> p0) { }
        @Modified @Nullable V putIfAbsent(K p0, V p1) { return null; }
        @Modified @Nullable V remove(@NotNull java.lang.Object p0) { return null; }
        @Modified boolean remove(@NotNull java.lang.Object p0, @NotNull java.lang.Object p1) { return false; }
        @Modified @Nullable V replace(K p0, V p1) { return null; }
        @Modified void replaceAll(@NotNull java.util.function.BiFunction<? super K, ? super V, ? extends V> p0) { }
    }

    //public class IdentityHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>, Serializable, Cloneable
    @Container
    class IdentityHashMap$<K, V> {
        IdentityHashMap$(/*@NotModified[O]*/ @Independent(hc = true) @NotNull Map<? extends K, ? extends V> m) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean containsKey(@Nullable java.lang.Object p0) { return false; }
        boolean containsValue(@Nullable java.lang.Object p0) { return false; }
        void forEach(@NotNull java.util.function.BiConsumer<? super K, ? super V> p0) { }
        @Nullable V get(@Nullable java.lang.Object p0) { return null; }
        @Modified @Nullable V put(K p0, V p1) { return null; }
        @Modified void putAll(@NotNull java.util.Map<? extends K, ? extends V> p0) { }
        @Modified @Nullable V remove(@Nullable java.lang.Object p0) { return null; }
        @Modified boolean remove(@Nullable java.lang.Object p0, @Nullable java.lang.Object p1) { return false; }
        @Modified void replaceAll(@NotNull java.util.function.BiFunction<? super K, ? super V, ? extends V> p0) { }
    }

    //public interface Iterator
    @Container
    @Independent(hc = true)
    class Iterator$<E> {
        boolean hasNext() { return false; }
        //frequency 2
        @Modified @Independent(hc = true)
        E next() { return null; }
        @Modified void remove() { }
        void forEachRemaining(/*@IgnoreModifications[T]*/ @Independent(hc = true) @NotNull Consumer<? super E> action) { }
    }

    //public class LinkedHashMap<K, V> extends HashMap<K, V> implements SequencedMap<K, V>
    @Container
    class LinkedHashMap$<K, V> {
        LinkedHashMap$(/*@NotModified[O]*/ @Independent(hc = true) @NotNull Map<? extends K, ? extends V> m) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean containsValue(@Nullable java.lang.Object p0) { return false; }
        void forEach(@NotNull java.util.function.BiConsumer<? super K, ? super V> p0) { }
        @Nullable V get(@Nullable java.lang.Object p0) { return null; }
        V getOrDefault(@Nullable java.lang.Object p0, @Nullable V p1) { return null; }
        @Modified @Nullable V putFirst(K p0, V p1) { return null; }
        @Modified @Nullable V putLast(K p0, V p1) { return null; }
        boolean removeEldestEntry(@NotNull java.util.Map.Entry<K, V> p0) { return false; }
        @Modified void replaceAll(@NotNull java.util.function.BiFunction<? super K, ? super V, ? extends V> p0) { }
    }

    //public class LinkedHashSet<E> extends HashSet<E> implements SequencedSet<E>, Cloneable, Serializable
    @Container
    class LinkedHashSet$<E> {
        LinkedHashSet$(@NotNull @Independent(hc = true) @NotModified Collection<? extends E> c) { }
    }

    //public class LinkedList extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, Serializable
    @Container
    class LinkedList$<E> {
        LinkedList$(@NotNull @Independent(hc = true) @NotModified Collection<? extends E> c) { }
        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H] @NotModified[H]
        E getFirst() { return null; }

        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H] @NotModified[H]
        E getLast() { return null; }

        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H]
        @Modified E removeFirst() { return null; }

        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H]
        @Modified E removeLast() { return null; }

        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        @Modified void addFirst(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { }

        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        @Modified void addLast(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Deque, java.util.List
        //@NotModified[H]
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Object o) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Deque, java.util.List
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.Deque, java.util.List, java.util.Queue
        @Modified boolean add(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Deque, java.util.List
        @Modified boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @Nullable Object o) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Deque, java.util.List
        @Modified boolean addAll(/*@Independent(hc=true)[H] @NotModified[H]*/ @NotNull Collection<? extends E> c) { return false; }

        //override from java.util.AbstractList, java.util.AbstractSequentialList, java.util.List
        @Modified boolean addAll(int index, /*@NotModified[H]*/ @NotNull Collection<? extends E> c) { return false; }

        //override from java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.List
        @Modified void clear() { }

        //override from java.util.AbstractList, java.util.AbstractSequentialList, java.util.List
        //@Independent(hc=true)[H] @NotModified[H] @GetSet("_synthetic_list")[H]
        E get(int index) { return null; }

        //override from java.util.AbstractList, java.util.AbstractSequentialList, java.util.List
        //@Independent(hc=true)[H] @GetSet("_synthetic_list")[H]
        @Modified E set(int index, /*@Independent(hc=true)[H] @NotModified[T]*/ E element) { return null; }

        //override from java.util.AbstractList, java.util.AbstractSequentialList, java.util.List
        @Modified void add(int index, /*@Independent(hc=true)[H] @NotModified[T]*/ E element) { }

        //override from java.util.AbstractList, java.util.AbstractSequentialList, java.util.List
        //@Independent(hc=true)[H]
        @Modified E remove(int index) { return null; }

        //override from java.util.AbstractList, java.util.List
        int indexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @Nullable Object o) { return 0; }

        //override from java.util.AbstractList, java.util.List
        int lastIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @Nullable Object o) { return 0; }

        //override from java.util.Deque, java.util.Queue
        //@Independent(hc=true)[H]
        @Nullable E peek() { return null; }

        //override from java.util.Deque, java.util.Queue
        //@Independent(hc=true)[H]
        E element() { return null; }

        //override from java.util.Deque, java.util.Queue
        //@Independent(hc=true)[H]
        @Modified @Nullable E poll() { return null; }

        //override from java.util.Deque, java.util.Queue
        //@Independent(hc=true)[H]
        @Modified E remove() { return null; }

        //override from java.util.Deque, java.util.Queue
        @Modified boolean offer(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { return false; }

        //override from java.util.Deque
        @Modified boolean offerFirst(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { return false; }

        //override from java.util.Deque
        @Modified boolean offerLast(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { return false; }

        //override from java.util.Deque
        //@Independent(hc=true)[H]
        @Nullable E peekFirst() { return null; }

        //override from java.util.Deque
        //@Independent(hc=true)[H]
        @Nullable E peekLast() { return null; }

        //override from java.util.Deque
        //@Independent(hc=true)[H]
        @Modified @Nullable E pollFirst() { return null; }

        //override from java.util.Deque
        //@Independent(hc=true)[H]
        @Modified @Nullable E pollLast() { return null; }

        //override from java.util.Deque
        @Modified void push(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { }

        //override from java.util.Deque
        //@Independent(hc=true)[H]
        @Modified E pop() { return null; }

        //override from java.util.Deque
        @Modified boolean removeFirstOccurrence(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @Nullable Object o) {
            return false;
        }

        //override from java.util.Deque
        @Modified boolean removeLastOccurrence(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @Nullable Object o) {
            return false;
        }

        //override from java.util.AbstractList, java.util.AbstractSequentialList, java.util.List
        ListIterator<E> listIterator(int index) { return null; }

        //override from java.util.Deque
        Iterator<E> descendingIterator() { return null; }

        //override from java.lang.Object
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        @NotNull(nullableArguments = 0) Object [] toArray() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[H]*/ @NotNull T [] a) { return null; }

        //override from java.lang.Iterable, java.util.Collection, java.util.List
        //@Independent(hc=true)[H] @NotModified[H] @NotNull[H]
        Spliterator<E> spliterator() { return null; }

        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        LinkedList<E> reversed() { return null; }
    }

    //public interface List implements SequencedCollection<E>
    @Container
    class List$<E> {
        //override from java.util.Collection
        //frequency 68
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.Collection
        //frequency 51
        //@NotModified[H]
        boolean isEmpty() { return false; }

        //override from java.util.Collection
        //frequency 6
        //@NotModified[H]
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object object) { return false; }

        //override from java.lang.Iterable, java.util.Collection
        //@NotModified[H] @NotNull[H]
        Iterator<E> iterator() { return null; }

        //override from java.util.Collection
        //@NotModified[H]
        @NotNull(nullableArguments = 0) Object [] toArray() { return null; }

        //override from java.util.Collection
        //frequency 3
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[O]*/ @NotNull T [] t) { return null; }

        //override from java.util.Collection
        //frequency 52
        @Modified boolean add(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { return false; }

        //override from java.util.Collection
        @Modified boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object object) {
            return false;
        }

        //override from java.util.Collection
        //@NotModified[H]
        boolean containsAll(/*@Independent[M] @NotModified[O]*/ @NotNull Collection<?> collection) { return false; }

        //override from java.util.Collection
        //frequency 8
        @Modified boolean addAll(/*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull Collection<? extends E> collection) { return false; }
        @Modified boolean addAll(int i, /*@NotModified[O]*/ @NotNull Collection<? extends E> collection) { return false; }
        //override from java.util.Collection
        @Modified boolean removeAll(/*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull Collection<?> collection) { return false; }

        //override from java.util.Collection
        @Modified boolean retainAll(/*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull Collection<?> collection) { return false; }
        @Modified void replaceAll(/*@IgnoreModifications[T]*/ @NotNull UnaryOperator<E> operator) { }
        //frequency 4
        @Modified void sort(/*@Immutable(hc=true)[T] @NotModified[O]*/ @Nullable @Independent(hc = true) Comparator<? super E> c) { }

        //override from java.util.Collection
        @Modified void clear() { }

        //override from java.lang.Object, java.util.Collection
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object object) { return false; }

        //override from java.lang.Object, java.util.Collection
        //frequency 4
        //@NotModified[H]
        public int hashCode() { return 0; }

        //frequency 5
        @Independent(hc = true) @NotModified @GetSet("_synthetic_list")
        E get(int i) { return null; }

        //@Independent(hc=true)[T]
        @Modified @GetSet("_synthetic_list")
        E set(int i, /*@Independent(hc=true)[T] @NotModified[O]*/ E e) { return null; }
        @Modified void add(int i, /*@Independent(hc=true)[T] @NotModified[O]*/ E e) { }
        //@Independent(hc=true)[T]
        @Modified E remove(int i) { return null; }
        @NotModified
        int indexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ @Nullable Object object) { return 0; }
        @NotModified
        int lastIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ @Nullable Object object) { return 0; }
        // like Iterable.iterator(): the elements are shared, and only remove, set and add write through to the list
        @NotModified @Independent(hc = true, except = {"remove", "set", "add"}) ListIterator<E> listIterator() { return null; }
        @NotModified @Independent(hc = true, except = {"remove", "set", "add"}) ListIterator<E> listIterator(int i) { return null; }
        //frequency 3
        @NotModified
        List<E> subList(int i, int i1) { return null; }

        //override from java.lang.Iterable, java.util.Collection
        //@Independent(hc=true)[H] @NotModified[H] @NotNull[H]
        Spliterator<E> spliterator() { return null; }

        //override from java.util.SequencedCollection
        @Modified void addFirst(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { }

        //override from java.util.SequencedCollection
        @Modified void addLast(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { }

        //override from java.util.SequencedCollection
        //frequency 351
        @Independent(hc = true) @NotModified
        E getFirst() { return null; }

        //override from java.util.SequencedCollection
        //frequency 7
        @Independent(hc = true) @NotModified
        E getLast() { return null; }

        //override from java.util.SequencedCollection
        //@Independent(hc=true)[H]
        @Modified E removeFirst() { return null; }

        //override from java.util.SequencedCollection
        //@Independent(hc=true)[H]
        @Modified E removeLast() { return null; }

        //override from java.util.SequencedCollection
        @NotModified List<E> reversed() { return null; }

        //frequency 10
        @ImmutableContainer @NotNull(content = true) @NotModified
        static <E> List<E> of() { return null; }

        //frequency 25
        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <E> List<E> of(/*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e1) { return null; }

        //frequency 4
        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <E> List<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e2) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <E> List<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e3) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <E> List<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e4) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <E> List<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e5) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <E> List<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e6) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <E> List<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e7) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <E> List<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e8) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <E> List<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e8,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e9) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <E> List<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e8,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e9,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull E e10) { return null; }

        //frequency 1
        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <E> List<E> of(/*@NotModified[O]*/ @NotNull E ... elements) { return null; }

        //frequency 6
        @ImmutableContainer(hc = true) @NotNull @NotModified
        static <E> List<E> copyOf(/*@NotModified[O]*/ @NotNull @Independent(hc = true) Collection<? extends E> coll) {
            return null;
        }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static <E> java.util.List<E> ofLazy(int p0, @NotNull java.util.function.IntFunction<? extends E> p1) { return null; }
    }

    //public interface ListIterator<E> extends Iterator<E>
    @Container
    @Independent(hc = true)
    class ListIterator$<E> {
        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void add(E p0) { }
        @Modified int nextIndex() { return 0; }
        @Modified void set(E p0) { }
    }

    //public final class Locale implements Cloneable, Serializable
    // final value type, all fields immutable -> deep @Immutable (hc=false); Builder (nested) is mutable
    @ImmutableContainer
    class Locale$ {
        static final Locale ENGLISH = null;
        static final Locale FRENCH = null;
        static final Locale GERMAN = null;
        static final Locale ITALIAN = null;
        static final Locale JAPANESE = null;
        static final Locale KOREAN = null;
        static final Locale CHINESE = null;
        static final Locale SIMPLIFIED_CHINESE = null;
        static final Locale TRADITIONAL_CHINESE = null;
        static final Locale FRANCE = null;
        static final Locale GERMANY = null;
        static final Locale ITALY = null;
        static final Locale JAPAN = null;
        static final Locale KOREA = null;
        static final Locale UK = null;
        static final Locale US = null;
        static final Locale CANADA = null;
        static final Locale CANADA_FRENCH = null;
        static final Locale ROOT = null;
        static final Locale CHINA = null;
        static final Locale PRC = null;
        static final Locale TAIWAN = null;
        static final char PRIVATE_USE_EXTENSION = '\0';
        static final char UNICODE_LOCALE_EXTENSION = '\0';
        //public static final class Builder
        class Builder {
            Builder() { }
            @Modified Locale.Builder setLocale(@NotNull Locale locale) { return null; }
            @Modified Locale.Builder setLanguageTag(@Nullable String languageTag) { return null; }
            @Modified Locale.Builder setLanguage(@Nullable String language) { return null; }
            @Modified Locale.Builder setScript(@Nullable String script) { return null; }
            @Modified Locale.Builder setRegion(@Nullable String region) { return null; }
            @Modified Locale.Builder setVariant(@Nullable String variant) { return null; }
            @Modified Locale.Builder setExtension(char key, @Nullable String value) { return null; }
            @Modified Locale.Builder setUnicodeLocaleKeyword(@NotNull String key, @Nullable String type) { return null; }
            @Modified Locale.Builder addUnicodeLocaleAttribute(@NotNull String attribute) { return null; }
            @Modified Locale.Builder removeUnicodeLocaleAttribute(@NotNull String attribute) { return null; }
            @Modified Locale.Builder clear() { return null; }
            @Modified Locale.Builder clearExtensions() { return null; }
            Locale build() { return null; }
        }

        //public enum Category extends Enum<Category>
        class Category {
            //@NotNull[O]
            static final Locale.Category DISPLAY = null;

            //@NotNull[O]
            static final Locale.Category FORMAT = null;
            static Locale.Category [] values() { return null; }
            static Locale.Category valueOf(String name) { return null; }
        }

        //public enum FilteringMode extends Enum<FilteringMode>
        class FilteringMode {
            //@NotNull[O]
            static final Locale.FilteringMode AUTOSELECT_FILTERING = null;

            //@NotNull[O]
            static final Locale.FilteringMode EXTENDED_FILTERING = null;

            //@NotNull[O]
            static final Locale.FilteringMode IGNORE_EXTENDED_RANGES = null;

            //@NotNull[O]
            static final Locale.FilteringMode MAP_EXTENDED_RANGES = null;

            //@NotNull[O]
            static final Locale.FilteringMode REJECT_EXTENDED_RANGES = null;
            static Locale.FilteringMode [] values() { return null; }
            static Locale.FilteringMode valueOf(String name) { return null; }
        }

        //public enum IsoCountryCode extends Enum<IsoCountryCode>
        class IsoCountryCode {
            //@NotNull[O]
            static final Locale.IsoCountryCode PART1_ALPHA2 = null;

            //@NotNull[O]
            static final Locale.IsoCountryCode PART1_ALPHA3 = null;

            //@NotNull[O]
            static final Locale.IsoCountryCode PART3 = null;
            static Locale.IsoCountryCode [] values() { return null; }
            static Locale.IsoCountryCode valueOf(String name) { return null; }
        }

        //public static final class LanguageRange
        class LanguageRange {
            static final double MAX_WEIGHT = 0.0;
            static final double MIN_WEIGHT = 0.0;
            LanguageRange(@NotNull String range) { }
            LanguageRange(@NotNull String range, double weight) { }
            String getRange() { return null; }
            double getWeight() { return 0.0; }
            //@Independent[T]
            static List<Locale.LanguageRange> parse(@NotNull String ranges) { return null; }

            //@Independent[T]
            static List<Locale.LanguageRange> parse(@NotNull String ranges, @NotNull @NotModified Map<String, List<String>> map) { return null; }

            //@Independent[T]
            static List<Locale.LanguageRange> mapEquivalents(
                @NotNull @NotModified List<Locale.LanguageRange> priorityList,
                @NotNull @NotModified Map<String, List<String>> map) { return null; }

            //override from java.lang.Object
            //@NotModified[H]
            public int hashCode() { return 0; }

            //override from java.lang.Object
            //@NotModified[H]
            public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

            //override from java.lang.Object
            //@NotModified[H] @NotNull[H]
            public String toString() { return null; }
        }
        Locale$(@NotNull String language, @NotNull String country, @NotNull String variant) { }
        Locale$(@NotNull String language, @NotNull String country) { }
        Locale$(@NotNull String language) { }
        static Locale of(@NotNull String language, @NotNull String country, @NotNull String variant) { return null; }
        static Locale of(@NotNull String language, @NotNull String country) { return null; }
        static Locale of(@NotNull String language) { return null; }
        static Locale getDefault() { return null; }
        static Locale getDefault(@NotNull Locale.Category category) { return null; }
        static void setDefault(@NotNull Locale newLocale) { }
        static void setDefault(@NotNull Locale.Category category, @NotNull Locale newLocale) { }
        static Locale [] getAvailableLocales() { return null; }
        //@Independent[T]
        static Stream<Locale> availableLocales() { return null; }

        //@Independent[T]
        static String [] getISOCountries() { return null; }

        //@Independent[T]
        static Set<String> getISOCountries(@NotNull Locale.IsoCountryCode type) { return null; }

        //@Independent[T]
        static String [] getISOLanguages() { return null; }

        //frequency 2
        @NotModified
        String getLanguage() { return null; }
        String getScript() { return null; }
        String getCountry() { return null; }
        String getVariant() { return null; }
        boolean hasExtensions() { return false; }
        Locale stripExtensions() { return null; }
        @Nullable String getExtension(char key) { return null; }
        Set<Character> getExtensionKeys() { return null; }
        Set<String> getUnicodeLocaleAttributes() { return null; }
        @Nullable String getUnicodeLocaleType(@NotNull String key) { return null; }
        Set<String> getUnicodeLocaleKeys() { return null; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        String toLanguageTag() { return null; }
        static String caseFoldLanguageTag(@NotNull String languageTag) { return null; }
        //frequency 1
        @Independent @NotModified
        static Locale forLanguageTag(@NotNull String languageTag) { return null; }
        String getISO3Language() { return null; }
        String getISO3Country() { return null; }
        String getDisplayLanguage() { return null; }
        String getDisplayLanguage(@NotNull Locale inLocale) { return null; }
        String getDisplayScript() { return null; }
        String getDisplayScript(@NotNull Locale inLocale) { return null; }
        String getDisplayCountry() { return null; }
        String getDisplayCountry(@NotNull Locale inLocale) { return null; }
        String getDisplayVariant() { return null; }
        String getDisplayVariant(@NotNull Locale inLocale) { return null; }
        String getDisplayName() { return null; }
        String getDisplayName(@NotNull Locale inLocale) { return null; }
        //override from java.lang.Object
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //@Independent[T]
        static List<Locale> filter(
            @NotNull List<Locale.LanguageRange> priorityList,
            @NotNull Collection<Locale> locales,
            @NotNull Locale.FilteringMode mode) { return null; }

        //@Independent[T]
        static List<Locale> filter(@NotNull List<Locale.LanguageRange> priorityList, @NotNull Collection<Locale> locales) { return null; }

        //@Independent[T]
        static List<String> filterTags(
            @NotNull List<Locale.LanguageRange> priorityList,
            @NotNull Collection<String> tags,
            @NotNull Locale.FilteringMode mode) { return null; }

        //@Independent[T]
        static List<String> filterTags(@NotNull List<Locale.LanguageRange> priorityList, @NotNull Collection<String> tags) { return null; }
        @Nullable static Locale lookup(@NotNull List<Locale.LanguageRange> priorityList, @NotNull Collection<Locale> locales) { return null; }
        @Nullable static String lookupTag(@NotNull List<Locale.LanguageRange> priorityList, @NotNull Collection<String> tags) { return null; }
    }

    //public interface Map
    @Container
    class Map$<K, V> {
        //public interface Entry
        @Container
        @Independent(hc = true)
        class Entry<K, V> {
            //frequency 1
            @Independent(hc = true) @NotModified
            K getKey() { return null; }

            //frequency 1
            @Independent(hc = true) @NotModified
            V getValue() { return null; }

            //@Independent(hc=true)[O]
            @Modified V setValue(/*@Independent(hc=true)[T] @NotModified[O]*/ V v) { return null; }

            //override from java.lang.Object
            //@NotModified[H]

            public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object object) {
                return false;
            }

            //override from java.lang.Object
            //@NotModified[H]
            public int hashCode() { return 0; }

            //@Immutable(hc=true)[T] @Independent[T]
            static <K extends Comparable<? super K>, V> Comparator<Map.Entry<K, V>> comparingByKey() { return null; }

            //@Immutable(hc=true)[T] @Independent[T]
            static <K, V extends Comparable<? super V>> Comparator<Map.Entry<K, V>> comparingByValue() { return null; }

            //@Immutable(hc=true)[T] @Independent[T]
            static <K, V> Comparator<Map.Entry<K, V>> comparingByKey(
                /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ @NotNull Comparator<? super K> cmp) {
                return null;
            }

            //@Immutable(hc=true)[T] @Independent[T]
            static <K, V> Comparator<Map.Entry<K, V>> comparingByValue(
                /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ @NotNull Comparator<? super V> cmp) {
                return null;
            }

            //@Independent(hc=true)[O]
            static <K, V> Map.Entry<K, V> copyOf(
                /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull Map.Entry<? extends K, ? extends V> e) { return null; }
        }
        @NotModified int size() { return 0; }
        @NotModified boolean isEmpty() { return false; }
        //frequency 5
        @NotModified
        boolean containsKey(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object object) { return false; }

        @NotModified
        boolean containsValue(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ @Nullable Object object) {
            return false;
        }

        //frequency 57
        @Independent(hc = true) @NotModified
        @Nullable V get(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object object) { return null; }

        //frequency 17
        //@Independent(hc=true)[T]

        @Modified @Nullable V put(/*@Independent(hc=true)[T] @NotModified[O]*/ K k, /*@Independent(hc=true)[T] @NotModified[O]*/ V v) {
            return null;
        }

        //@Independent(hc=true)[T]
        @Modified @Nullable V remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ @Nullable Object object) { return null; }
        @Modified void putAll(/*@NotModified[O]*/ @NotNull Map<? extends K, ? extends V> map) { }
        @Modified void clear() { }
        @NotModified Set<K> keySet() { return null; }
        //frequency 2
        @NotModified
        Collection<V> values() { return null; }

        //frequency 1
        @NotModified
        Set<Map.Entry<K, V>> entrySet() { return null; }

        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object object) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //@Independent(hc=true)[T]
        @NotModified

        V getOrDefault(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object key,
            @Nullable @Independent(hcReturnValue = true)  /* @NotModified[O]*/ V defaultValue) { return null; }

        //frequency 1
        @NotModified
        void forEach(/*@IgnoreModifications[T]*/ @NotNull @Independent(hc = true) BiConsumer<? super K, ? super V> action) { }
        @Modified void replaceAll(/*@IgnoreModifications[T]*/ @NotNull BiFunction<? super K, ? super V, ? extends V> function) { }
        //frequency 1
        //@Independent(hc=true)[T]

        @Modified @Nullable V putIfAbsent(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V value) { return null; }

        @Modified boolean remove(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ @Nullable Object key,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ @Nullable Object value) { return false; }

        @Modified boolean replace(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V oldValue,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V newValue) { return false; }

        //@Independent(hc=true)[T]
        @Modified @Nullable V replace(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V value) { return null; }

        //@Independent(hc=true)[T]
        @Modified V computeIfAbsent(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ @NotNull Function<? super K, ? extends V> mappingFunction) { return null; }

        //@Independent(hc=true)[T]
        @Modified @Nullable V computeIfPresent(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 2) BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //@Independent(hc=true)[T]
        @Modified @Nullable V compute(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {1, 2}) BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //frequency 2
        //@Independent(hc=true)[T]

        @Modified @Nullable V merge(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull V value,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 2) @Independent(hc = true) @NotModified BiFunction<
                ? super V,
                ? super V,
                ? extends V> remappingFunction) { return null; }

        //frequency 2
        @ImmutableContainer @NotNull @NotModified
        static <K, V> Map<K, V> of() { return null; }

        //frequency 21
        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <K, V> Map<K, V> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v1) { return null; }

        //frequency 6
        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <K, V> Map<K, V> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v2) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <K, V> Map<K, V> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v3) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <K, V> Map<K, V> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v4) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <K, V> Map<K, V> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v5) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <K, V> Map<K, V> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v6) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <K, V> Map<K, V> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v7) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <K, V> Map<K, V> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k8,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v8) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <K, V> Map<K, V> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k8,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v8,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k9,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v9) { return null; }

        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <K, V> Map<K, V> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k8,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v8,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k9,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v9,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k10,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v10) { return null; }

        static <K, V> Map<K, V> ofEntries(/*@NotModified[O]*/ @NotNull Map.Entry<? extends K, ? extends V> ... entries) {
            return null;
        }

        @NotNull @NotModified @Independent
        static <K, V> Map.Entry<K, V> entry(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V v) { return null; }

        //@Independent(hc=true)[T]
        @Immutable(hc = true) @NotNull @NotModified
        static <K, V> Map<K, V> copyOf(
            /*@NotModified[O]*/ @Independent(hc = true) @NotNull Map<? extends K, ? extends V> map) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static <K, V> java.util.Map<K, V> ofLazy(@NotNull java.util.Set<? extends K> p0, @NotNull java.util.function.Function<? super K, ? extends V> p1) { return null; }
    }

    //public interface NavigableMap implements SortedMap<K,V>
    @Container
    class NavigableMap$<K, V> {
        @Independent(hc = true) @Nullable @NotModified Map.Entry<K, V> lowerEntry(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        //@Independent(hc=true)[T]
        @NotModified
        @Nullable K lowerKey(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        @Independent(hc = true) @Nullable @NotModified Map.Entry<K, V> floorEntry(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        //@Independent(hc=true)[T]
        @NotModified
        @Nullable K floorKey(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        @Independent(hc = true) @Nullable @NotModified Map.Entry<K, V> ceilingEntry(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        //@Independent(hc=true)[T]
        @NotModified
        @Nullable K ceilingKey(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        @Independent(hc = true) @Nullable @NotModified Map.Entry<K, V> higherEntry(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        //@Independent(hc=true)[T]
        @NotModified
        @Nullable K higherKey(/*@Independent[M] @NotModified[O]*/ K k) { return null; }

        //override from java.util.SequencedMap
        @NotModified
        @Independent(hc = true) @Nullable Map.Entry<K, V> firstEntry() { return null; }

        //override from java.util.SequencedMap
        @NotModified
        @Independent(hc = true) @Nullable Map.Entry<K, V> lastEntry() { return null; }

        //override from java.util.SequencedMap
        @Modified @Independent(hc = true) @Nullable Map.Entry<K, V> pollFirstEntry() { return null; }

        //override from java.util.SequencedMap
        @Modified @Independent(hc = true) @Nullable Map.Entry<K, V> pollLastEntry() { return null; }
        @NotModified NavigableMap<K, V> descendingMap() { return null; }
        @NotModified NavigableSet<K> navigableKeySet() { return null; }
        @NotModified NavigableSet<K> descendingKeySet() { return null; }
        @NotModified
        NavigableMap<K, V> subMap(
            /*@Independent[M] @NotModified[O]*/ K k,
            boolean b,
            /*@Independent[M] @NotModified[O]*/ K k1,
            boolean b1) { return null; }
        @NotModified NavigableMap<K, V> headMap(/*@Independent[M] @NotModified[O]*/ K k, boolean b) { return null; }
        @NotModified NavigableMap<K, V> tailMap(/*@Independent[M] @NotModified[O]*/ K k, boolean b) { return null; }
        //override from java.util.SortedMap
        @NotModified

        SortedMap<K, V> subMap(/*@Independent[M] @NotModified[O]*/ K k, /*@Independent[M] @NotModified[O]*/ K k1) {
            return null;
        }

        //override from java.util.SortedMap
        @NotModified
        SortedMap<K, V> headMap(/*@Independent[M] @NotModified[O]*/ K k) { return null; }

        //override from java.util.SortedMap
        @NotModified
        SortedMap<K, V> tailMap(/*@Independent[M] @NotModified[O]*/ K k) { return null; }

        //override from java.util.SequencedMap, java.util.SortedMap
        @NotModified
        NavigableMap<K, V> reversed() { return null; }
    }

    //public interface NavigableSet<E> extends SortedSet<E>
    @Container
    class NavigableSet$<E> {
        @Modified @Nullable E pollFirst() { return null; }
        @Modified @Nullable E pollLast() { return null; }
        @Nullable @NotModified E lower(/*@Independent[M] @NotModified[O]*/ E e) { return null; }
        @Nullable @NotModified E floor(/*@Independent[M] @NotModified[O]*/ E e) { return null; }
        @Nullable @NotModified E ceiling(/*@Independent[M] @NotModified[O]*/ E e) { return null; }
        @Nullable @NotModified E higher(/*@Independent[M] @NotModified[O]*/ E e) { return null; }
        @NotModified NavigableSet<E> descendingSet() { return null; }
        @NotModified @Independent(hc = true, except = "remove") Iterator<E> descendingIterator() { return null; }
        @NotModified
        NavigableSet<E> subSet(
            /*@Independent[M] @NotModified[O]*/ E fromElement,
            boolean fromInclusive,
            /*@Independent[M] @NotModified[O]*/ E toElement,
            boolean toInclusive) { return null; }
        @NotModified NavigableSet<E> headSet(/*@Independent[M] @NotModified[O]*/ E toElement, boolean inclusive) {
            return null;
        }
        @NotModified NavigableSet<E> tailSet(/*@Independent[M] @NotModified[O]*/ E fromElement, boolean inclusive) {
            return null;
        }
    }

    //public class NoSuchElementException extends RuntimeException
    class NoSuchElementException$ {
        NoSuchElementException$() { }
        NoSuchElementException$(@Nullable String s, @Nullable @NotModified Throwable cause) { }
        NoSuchElementException$(@Nullable @NotModified Throwable cause) { }
        //frequency 12
        NoSuchElementException$(@Nullable String s) { }
    }

    //public final class Objects
    @Container
    @UtilityClass
    class Objects$ {
        //frequency 128
        //@NotModified[T]

        static boolean equals(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object a,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object b) { return false; }

        //frequency 1
        //@NotModified[T]

        static boolean deepEquals(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object a,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object b) { return false; }

        //frequency 26
        //@NotModified[T]
        static int hashCode(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object o) { return 0; }

        //frequency 20
        //@NotModified[T]
        static int hash(/*@Independent[M] @NotModified[O]*/ @Nullable(nullableArguments = 0) Object ... values) { return 0; }

        //@NotModified[T]
        @NotNull
        static String toString(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object o) { return null; }

        //frequency 5
        //@NotModified[T]
        @NotNull

        static String toString(
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object o,
            @NotNull String nullDefault) { return null; }

        //@NotModified[T]
        @NotNull

        static String toIdentityString(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull Object o) {
            return null;
        }

        //@NotModified[T]
        static <T> int compare(
            /*@Independent[M] @NotModified[O]*/ @Nullable T a,
            /*@Independent[M] @NotModified[O]*/ @Nullable T b,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull Comparator<? super T> c) { return 0; }

        //frequency 9
        //@Independent[O] @NotModified[T]
        @Identity @NotNull
        static <T> T requireNonNull(/*@Independent[M] @NotModified[O]*/ @NotNull T obj) { return null; }

        //frequency 2
        //@Independent[O] @NotModified[T]
        @Identity @NotNull
        static <T> T requireNonNull(/*@Independent[M] @NotModified[O]*/ @NotNull T obj, @Nullable String message) { return null; }

        //@NotModified[T]
        static boolean isNull(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object obj) { return false; }

        //frequency 1
        //@NotModified[T]
        static boolean nonNull(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object obj) { return false; }

        //frequency 1
        //@Independent[O] @NotModified[T]
        @Identity @NotNull

        static <T> T requireNonNullElse(
            /*@Independent[M] @NotModified[O]*/ @Nullable T obj,
            /*@Independent[M] @NotModified[O]*/ @NotNull T defaultObj) { return null; }

        //@Independent[O] @NotModified[T]
        @Identity @NotNull

        static <T> T requireNonNullElseGet(
            /*@Independent[M] @NotModified[O]*/ @Nullable T obj,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull(nullableArguments = 0) Supplier<? extends T> supplier) { return null; }

        //@Independent[O] @NotModified[T]
        @Identity @NotNull

        static <T> T requireNonNull(
            /*@Independent[M] @NotModified[O]*/ @Nullable T obj,
            /*@IgnoreModifications[T] @Independent[M]*/ @Nullable Supplier<String> messageSupplier) { return null; }

        //@NotModified[T]
        static int checkIndex(int index, int length) { return 0; }

        //@NotModified[T]
        static int checkFromToIndex(int fromIndex, int toIndex, int length) { return 0; }

        //@NotModified[T]
        static int checkFromIndexSize(int fromIndex, int size, int length) { return 0; }

        //@NotModified[T]
        static long checkIndex(long index, long l) { return 0L; }

        //@NotModified[T]
        static long checkFromToIndex(long fromIndex, long l, long toIndex) { return 0L; }

        //@NotModified[T]
        static long checkFromIndexSize(long fromIndex, long l, long size) { return 0L; }
    }

    //public final class Optional
    @ImmutableContainer(hc = true)
    class Optional$<T> {
        //frequency 31
        //@NotModified[T]
        @Immutable @Independent
        static <T> Optional<T> empty() { return null; }

        //frequency 56
        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        @NotNull
        static <T> Optional<T> of(/*@Independent(hc=true)[T] @NotModified[O]*/ @NotNull T value) { return null; }

        //frequency 11
        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        @NotNull
        static <T> Optional<T> ofNullable(/*@Independent(hc=true)[T] @NotModified[O]*/ @Nullable T value) { return null; }

        //frequency 88
        //@Independent(hc=true)[O] @NotModified[T]
        T get() { return null; }

        //frequency 31
        //@NotModified[T]
        boolean isPresent() { return false; }

        //frequency 27
        //@NotModified[T]
        boolean isEmpty() { return false; }

        //frequency 99
        //@NotModified[T]
        void ifPresent(/*@IgnoreModifications[T] @Independent[M]*/ @NotNull Consumer<? super T> action) { }

        //frequency 39
        //@NotModified[T]

        void ifPresentOrElse(
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull Consumer<? super T> action,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull Runnable emptyAction) { }

        //frequency 20
        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Optional<T> filter(/*@IgnoreModifications[T] @Independent[M]*/ @NotNull Predicate<? super T> predicate) { return null; }

        //frequency 211
        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]

        <U> Optional<U> map(/*@IgnoreModifications[T] @Independent[M]*/ @NotNull(nullableArguments = 1) Function<? super T, ? extends U> mapper) {
            return null;
        }

        //frequency 18
        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]

        <U> Optional<U> flatMap(
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull Function<? super T, ? extends Optional<? extends U>> mapper) {
            return null;
        }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Optional<T> or(/*@IgnoreModifications[T] @Independent[M]*/ @NotNull Supplier<? extends Optional<? extends T>> supplier) {
            return null;
        }

        //frequency 3
        //@Independent(hc=true)[O] @NotModified[T]
        Stream<T> stream() { return null; }

        //frequency 131
        //@Independent(hc=true)[O] @NotModified[T]
        T orElse(@Nullable @Independent(hcReturnValue = true) /*@NotModified[O]*/ T other) { return null; }

        //frequency 16
        //@Independent(hc=true)[O] @NotModified[T]
        T orElseGet(/*@IgnoreModifications[T] @Independent[M]*/ @NotNull(nullableArguments = 0) Supplier<? extends T> supplier) { return null; }

        //frequency 54
        //@Independent(hc=true)[O] @NotModified[T]
        T orElseThrow() { return null; }

        //frequency 39
        //@Independent(hc=true)[O] @NotModified[T]

        <X extends Throwable> T orElseThrow(
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull Supplier<? extends X> exceptionSupplier) { return null; }

        //override from java.lang.Object
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

        //override from java.lang.Object
        //@NotModified[T]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }
    }

    //public final class OptionalDouble
    @ImmutableContainer
    class OptionalDouble$ {
        //@Immutable[T] @Independent[O] @NotModified[T]
        static OptionalDouble empty() { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static OptionalDouble of(double value) { return null; }

        //@NotModified[T]
        double getAsDouble() { return 0.0; }

        //@NotModified[T]
        boolean isPresent() { return false; }

        //@NotModified[T]
        boolean isEmpty() { return false; }

        //@NotModified[T]
        void ifPresent(/*@IgnoreModifications[T] @Independent[M]*/ @NotNull DoubleConsumer action) { }

        //@NotModified[T]
        void ifPresentOrElse(
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull DoubleConsumer action,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull Runnable emptyAction) { }

        //@Independent[O] @NotModified[T]
        DoubleStream stream() { return null; }

        //frequency 1
        //@NotModified[T]
        double orElse(double other) { return 0.0; }

        //@NotModified[T]
        double orElseGet(/*@IgnoreModifications[T] @Independent[M]*/ @NotNull DoubleSupplier supplier) { return 0.0; }

        //@NotModified[T]
        double orElseThrow() { return 0.0; }

        //@NotModified[T]
        <X extends Throwable> double orElseThrow(
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull Supplier<? extends X> exceptionSupplier) { return 0.0; }

        //override from java.lang.Object
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

        //override from java.lang.Object
        //@NotModified[T]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }
    }

    //public final class OptionalInt
    @ImmutableContainer
    class OptionalInt$ {
        //@Immutable[T] @Independent[O] @NotModified[T]
        static OptionalInt empty() { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static OptionalInt of(int value) { return null; }

        //@NotModified[T]
        int getAsInt() { return 0; }

        //@NotModified[T]
        boolean isPresent() { return false; }

        //@NotModified[T]
        boolean isEmpty() { return false; }

        //@NotModified[T]
        void ifPresent(/*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntConsumer action) { }

        //@NotModified[T]
        void ifPresentOrElse(
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntConsumer action,
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull Runnable emptyAction) { }

        //@Independent[O] @NotModified[T]
        IntStream stream() { return null; }

        //frequency 1
        //@NotModified[T]
        int orElse(int other) { return 0; }

        //@NotModified[T]
        int orElseGet(/*@IgnoreModifications[T] @Independent[M]*/ @NotNull IntSupplier supplier) { return 0; }

        //@NotModified[T]
        int orElseThrow() { return 0; }

        //@NotModified[T]
        <X extends Throwable> int orElseThrow(
            /*@IgnoreModifications[T] @Independent[M]*/ @NotNull Supplier<? extends X> exceptionSupplier) { return 0; }

        //override from java.lang.Object
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

        //override from java.lang.Object
        //@NotModified[T]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }
    }

    //public class PriorityQueue<E> extends AbstractQueue<E> implements Serializable
    @Container
    class PriorityQueue$<E> {
        PriorityQueue$(@NotNull @Independent(hc = true) @NotModified Collection<? extends E> c) { }
        @Nullable @NotModified Comparator<? super E> comparator() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PriorityQueue$(@Nullable java.util.Comparator<? super E> p0) { }
        PriorityQueue$(@NotNull @Independent(hc = true) @NotModified java.util.PriorityQueue<? extends E> p0) { }
        PriorityQueue$(@NotNull @Independent(hc = true) @NotModified java.util.SortedSet<? extends E> p0) { }
        PriorityQueue$(int p0, @Nullable java.util.Comparator<? super E> p1) { }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        void forEach(@NotNull java.util.function.Consumer<? super E> p0) { }
        @Nullable E peek() { return null; }
        @Modified @Nullable E poll() { return null; }
        @Modified boolean remove(@Nullable java.lang.Object p0) { return false; }
        @Modified boolean removeAll(@NotNull java.util.Collection<?> p0) { return false; }
        @Modified boolean removeIf(@NotNull java.util.function.Predicate<? super E> p0) { return false; }
        @Modified boolean retainAll(@NotNull java.util.Collection<?> p0) { return false; }
        <T> T[] toArray(@NotNull T[] p0) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified boolean offer(E p0) { return false; }
    }

    //public class Properties extends Hashtable<Object, Object>
    @Container
    class Properties$ {
        @Nullable @NotModified String getProperty(/*@Independent[M] @NotModified[O]*/ @NotNull String key) { return null; }
        @NotModified
        String getProperty(/*@Independent[M] @NotModified[O]*/ @NotNull String key, @Nullable String defaultValue) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Properties$(@Nullable java.util.Properties p0) { }
        @Modified java.lang.Object compute(@NotNull java.lang.Object p0, @NotNull(nullableArguments = 1) java.util.function.BiFunction<? super java.lang.Object, ? super java.lang.Object, ?> p1) { return null; }
        @Modified java.lang.Object computeIfAbsent(@NotNull java.lang.Object p0, @NotNull java.util.function.Function<? super java.lang.Object, ?> p1) { return null; }
        @Modified @Nullable java.lang.Object computeIfPresent(@NotNull java.lang.Object p0, @NotNull java.util.function.BiFunction<? super java.lang.Object, ? super java.lang.Object, ?> p1) { return null; }
        boolean contains(@NotNull java.lang.Object p0) { return false; }
        boolean containsKey(@NotNull java.lang.Object p0) { return false; }
        boolean containsValue(@NotNull java.lang.Object p0) { return false; }
        void forEach(@NotNull java.util.function.BiConsumer<? super java.lang.Object, ? super java.lang.Object> p0) { }
        @Nullable java.lang.Object get(@NotNull java.lang.Object p0) { return null; }
        java.lang.Object getOrDefault(@NotNull java.lang.Object p0, @Nullable java.lang.Object p1) { return null; }
        void list(@NotNull java.io.PrintStream p0) { }
        void list(@NotNull java.io.PrintWriter p0) { }
        @Modified void load(@NotNull java.io.InputStream p0) { }
        @Modified void load(@NotNull java.io.Reader p0) { }
        @Modified void loadFromXML(@NotNull java.io.InputStream p0) { }
        @Modified java.lang.Object merge(@NotNull java.lang.Object p0, @NotNull java.lang.Object p1, @NotNull java.util.function.BiFunction<? super java.lang.Object, ? super java.lang.Object, ?> p2) { return null; }
        @Modified @Nullable java.lang.Object put(@NotNull java.lang.Object p0, @NotNull java.lang.Object p1) { return null; }
        @Modified void putAll(@NotNull java.util.Map<?, ?> p0) { }
        @Modified @Nullable java.lang.Object putIfAbsent(@NotNull java.lang.Object p0, @NotNull java.lang.Object p1) { return null; }
        @Modified @Nullable java.lang.Object remove(@NotNull java.lang.Object p0) { return null; }
        @Modified boolean remove(@NotNull java.lang.Object p0, @NotNull java.lang.Object p1) { return false; }
        @Modified @Nullable java.lang.Object replace(@NotNull java.lang.Object p0, @NotNull java.lang.Object p1) { return null; }
        @Modified boolean replace(@NotNull java.lang.Object p0, @NotNull java.lang.Object p1, @NotNull java.lang.Object p2) { return false; }
        @Modified void replaceAll(@NotNull java.util.function.BiFunction<? super java.lang.Object, ? super java.lang.Object, ?> p0) { }
        void save(@NotNull java.io.OutputStream p0, @Nullable java.lang.String p1) { }
        @Modified @Nullable java.lang.Object setProperty(@NotNull java.lang.String p0, @NotNull java.lang.String p1) { return null; }
        void store(@NotNull java.io.OutputStream p0, @Nullable java.lang.String p1) { }
        void store(@NotNull java.io.Writer p0, @Nullable java.lang.String p1) { }
        @Modified void storeToXML(@NotNull java.io.OutputStream p0, @Nullable java.lang.String p1) { }
        @Modified void storeToXML(@NotNull java.io.OutputStream p0, @Nullable java.lang.String p1, @NotNull java.nio.charset.Charset p2) { }
        @Modified void storeToXML(@NotNull java.io.OutputStream p0, @Nullable java.lang.String p1, @NotNull java.lang.String p2) { }
    }

    //public interface Queue<E> extends Collection<E>
    @Container
    class Queue$<E> {
        @Modified @Nullable E poll() { return null; }
        @Nullable @NotModified E peek() { return null; }
        @NotModified E element() { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified boolean offer(E p0) { return false; }
        @Modified E remove() { return null; }
    }

    //public class Random implements RandomGenerator, Serializable
    class Random$ {
        //frequency 3
        Random$() { }
        Random$(long seed) { }
        static Random from(@NotNull @NotModified RandomGenerator generator) { return null; }
        @Modified void setSeed(long seed) { }
        //override from java.util.random.RandomGenerator
        //override has frequency 1
        @Modified void nextBytes(@NotNull byte [] bytes) { }

        //override from java.util.random.RandomGenerator
        @Modified int nextInt() { return 0; }

        //override from java.util.random.RandomGenerator
        //frequency 3
        @Modified int nextInt(int bound) { return 0; }

        //override from java.util.random.RandomGenerator
        @Modified long nextLong() { return 0L; }

        //override from java.util.random.RandomGenerator
        @Modified boolean nextBoolean() { return false; }

        //override from java.util.random.RandomGenerator
        @Modified float nextFloat() { return 0.0F; }

        //override from java.util.random.RandomGenerator
        //override has frequency 1
        @Modified double nextDouble() { return 0.0; }

        //override from java.util.random.RandomGenerator
        @Modified double nextGaussian() { return 0.0; }

        //override from java.util.random.RandomGenerator
        IntStream ints(long streamSize) { return null; }

        //override from java.util.random.RandomGenerator
        IntStream ints() { return null; }

        //override from java.util.random.RandomGenerator
        IntStream ints(long streamSize, int i, int randomNumberOrigin) { return null; }

        //override from java.util.random.RandomGenerator
        IntStream ints(int randomNumberOrigin, int randomNumberBound) { return null; }

        //override from java.util.random.RandomGenerator
        LongStream longs(long streamSize) { return null; }

        //override from java.util.random.RandomGenerator
        LongStream longs() { return null; }

        //override from java.util.random.RandomGenerator
        LongStream longs(long streamSize, long l, long randomNumberOrigin) { return null; }

        //override from java.util.random.RandomGenerator
        LongStream longs(long randomNumberOrigin, long l) { return null; }

        //override from java.util.random.RandomGenerator
        DoubleStream doubles(long streamSize) { return null; }

        //override from java.util.random.RandomGenerator
        DoubleStream doubles() { return null; }

        //override from java.util.random.RandomGenerator
        DoubleStream doubles(long streamSize, double d, double randomNumberOrigin) { return null; }

        //override from java.util.random.RandomGenerator
        DoubleStream doubles(double randomNumberOrigin, double d) { return null; }
    }

    //public interface RandomAccess
    //@Container[M]
    @Independent(hc = true) class RandomAccess$ { }

    //public interface SequencedCollection implements Collection<E>
    @Container
    class SequencedCollection$<E> {
        @NotModified SequencedCollection<E> reversed() { return null; }
        @Modified void addFirst(/*@Independent(hc=true)[T] @NotModified[O]*/ E e) { }
        @Modified void addLast(/*@Independent(hc=true)[T] @NotModified[O]*/ E e) { }
        //override has frequency 351
        //@Independent(hc=true)[T]
        @NotModified E getFirst() { return null; }

        //override has frequency 7
        //@Independent(hc=true)[T]
        @NotModified E getLast() { return null; }

        //@Independent(hc=true)[T]
        @Modified E removeFirst() { return null; }

        //@Independent(hc=true)[T]
        @Modified E removeLast() { return null; }
    }

    //public interface SequencedMap implements Map<K,V>
    @Container
    class SequencedMap$<K, V> {
        @NotModified SequencedMap<K, V> reversed() { return null; }
        @Independent(hc = true) @Nullable @NotModified Map.Entry<K, V> firstEntry() { return null; }
        @Independent(hc = true) @Nullable @NotModified Map.Entry<K, V> lastEntry() { return null; }
        @Modified @Independent(hc = true) @Nullable Map.Entry<K, V> pollFirstEntry() { return null; }
        @Modified @Independent(hc = true) @Nullable Map.Entry<K, V> pollLastEntry() { return null; }
        //@Independent(hc=true)[T]
        @Modified @Nullable V putFirst(/*@Independent(hc=true)[T] @NotModified[O]*/ K k, /*@Independent(hc=true)[T] @NotModified[O]*/ V v) {
            return null;
        }

        //@Independent(hc=true)[T]
        @Modified @Nullable V putLast(/*@Independent(hc=true)[T] @NotModified[O]*/ K k, /*@Independent(hc=true)[T] @NotModified[O]*/ V v) {
            return null;
        }
        @NotModified SequencedSet<K> sequencedKeySet() { return null; }
        @NotModified SequencedCollection<V> sequencedValues() { return null; }
        @NotModified SequencedSet<Map.Entry<K, V>> sequencedEntrySet() { return null; }
    }

    //public interface SequencedSet<E> extends SequencedCollection<E>, Set<E>
    @Container
    class SequencedSet$<E> { }

    //public interface Set implements Collection<E>
    @Container
    class Set$<E> {
        //override from java.util.Collection
        //frequency 12
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.Collection
        //frequency 18
        //@NotModified[H]
        boolean isEmpty() { return false; }

        //override from java.util.Collection
        //frequency 23
        //@NotModified[H]
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object object) { return false; }

        //override from java.lang.Iterable, java.util.Collection
        //frequency 2
        //@NotModified[H] @NotNull[H]
        Iterator<E> iterator() { return null; }

        //override from java.util.Collection
        //frequency 3
        //@NotModified[H]
        @NotNull(nullableArguments = 0) Object [] toArray() { return null; }

        //override from java.util.Collection
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[O]*/ @NotNull T [] t) { return null; }

        //override from java.util.Collection
        //frequency 27
        @Modified boolean add(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { return false; }

        //override from java.util.Collection
        //frequency 3

        @Modified boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object object) {
            return false;
        }

        //override from java.util.Collection
        //@NotModified[H]
        boolean containsAll(/*@Independent[M] @NotModified[O]*/ @NotNull Collection<?> collection) { return false; }

        //override from java.util.Collection
        //frequency 5
        @Modified boolean addAll(/*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull Collection<? extends E> collection) { return false; }

        //override from java.util.Collection
        //frequency 1
        @Modified boolean retainAll(/*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull Collection<?> collection) { return false; }

        //override from java.util.Collection
        @Modified boolean removeAll(/*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull Collection<?> collection) { return false; }

        //override from java.util.Collection
        @Modified void clear() { }

        //override from java.lang.Object, java.util.Collection
        //frequency 1
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object object) { return false; }

        //override from java.lang.Object, java.util.Collection
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Iterable, java.util.Collection
        //@Independent(hc=true)[H] @NotModified[H] @NotNull[H]
        Spliterator<E> spliterator() { return null; }

        //frequency 1
        @NotModified
        @ImmutableContainer @NotNull
        static <E> Set<E> of() { return null; }

        //frequency 55
        @NotModified @ImmutableContainer(hc = true) @NotNull(content = true)
        static <E> Set<E> of(/*@Independent(hc=true)[T] @NotModified[O]*/ E e1) { return null; }

        //frequency 14
        @NotModified @ImmutableContainer(hc = true) @NotNull(content = true)
        static <E> Set<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e2) { return null; }

        //frequency 1
        @NotModified @ImmutableContainer(hc = true) @NotNull(content = true)
        static <E> Set<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e3) { return null; }

        @NotModified @ImmutableContainer(hc = true) @NotNull(content = true)
        static <E> Set<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e4) { return null; }

        @NotModified @ImmutableContainer(hc = true) @NotNull(content = true)
        static <E> Set<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e5) { return null; }

        @NotModified @ImmutableContainer(hc = true) @NotNull(content = true)
        static <E> Set<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e6) { return null; }

        @NotModified @ImmutableContainer(hc = true) @NotNull(content = true)
        static <E> Set<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e7) { return null; }

        @NotModified @ImmutableContainer(hc = true) @NotNull(content = true)
        static <E> Set<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e8) { return null; }

        @NotModified @ImmutableContainer(hc = true) @NotNull(content = true)
        static <E> Set<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e8,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e9) { return null; }

        @NotModified @ImmutableContainer(hc = true) @NotNull(content = true)
        static <E> Set<E> of(
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e1,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e2,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e3,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e4,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e5,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e6,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e7,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e8,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e9,
            /*@Independent(hc=true)[T] @NotModified[O]*/ @Commutable @NotNull E e10) { return null; }

        //frequency 5
        @NotModified @ImmutableContainer(hc = true) @NotNull(content = true)
        static <E> Set<E> of(/*@NotModified[O]*/ @NotNull E ... elements) { return null; }
        @NotModified @ImmutableContainer(hc = true) @NotNull
        static <E> Set<E> copyOf(/*@NotModified[O]*/ @NotNull @Independent(hc = true) Collection<? extends E> coll) { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static <E> java.util.Set<E> ofLazy(@NotNull java.util.Set<? extends E> p0, @NotNull java.util.function.Predicate<? super E> p1) { return null; }
    }

    //public interface SortedMap implements SequencedMap<K,V>
    @Container
    class SortedMap$<K, V> {
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @Nullable @NotModified Comparator<? super K> comparator() { return null; }

        @NotModified
        SortedMap<K, V> subMap(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k1) { return null; }
        @NotModified SortedMap<K, V> headMap(/*@Independent(hc=true)[T] @NotModified[O]*/ K k) { return null; }
        @NotModified SortedMap<K, V> tailMap(/*@Independent(hc=true)[T] @NotModified[O]*/ K k) { return null; }
        //@Independent(hc=true)[T]
        @NotModified K firstKey() { return null; }

        //@Independent(hc=true)[T]
        @NotModified K lastKey() { return null; }

        //override from java.util.Map
        //@NotModified[H]
        Set<K> keySet() { return null; }

        //override from java.util.Map
        //@NotModified[H]
        Collection<V> values() { return null; }

        //override from java.util.Map
        //@NotModified[H]
        Set<Map.Entry<K, V>> entrySet() { return null; }

        //override from java.util.SequencedMap
        //@Independent(hc=true)[H]

        @Modified @Nullable V putFirst(/*@Independent(hc=true)[H] @NotModified[O]*/ K k, /*@Independent(hc=true)[H] @NotModified[O]*/ V v) {
            return null;
        }

        //override from java.util.SequencedMap
        //@Independent(hc=true)[H]

        @Modified @Nullable V putLast(/*@Independent(hc=true)[H] @NotModified[O]*/ K k, /*@Independent(hc=true)[H] @NotModified[O]*/ V v) {
            return null;
        }

        //override from java.util.SequencedMap
        //@NotModified[H]
        SortedMap<K, V> reversed() { return null; }
    }

    //public interface SortedSet<E> extends Set<E>, SequencedCollection<E>
    @Container
    class SortedSet$<E> {
        @Nullable @NotModified Comparator<? super E> comparator() { return null; }
        @NotModified
        SortedSet<E> subSet(
            /*@Independent[M] @NotModified[O]*/ E fromElement,
            /*@Independent[M] @NotModified[O]*/ E toElement) { return null; }
        @NotModified SortedSet<E> headSet(/*@Independent[M] @NotModified[O]*/ E toElement) { return null; }
        @NotModified SortedSet<E> tailSet(/*@Independent[M] @NotModified[O]*/ E fromElement) { return null; }
        @NotModified E first() { return null; }
        @NotModified E last() { return null; }
    }

    //public interface Spliterator<T>
    @Container
    @Independent(hc = true)
    class Spliterator$<T> {
        @NotModified boolean hasCharacteristics(int characteristics) { return false; }
        @NotModified long getExactSizeIfKnown() { return 0L; }
        @Nullable @NotModified Comparator<? super T> getComparator() { return null; }
    
        //public java.util.Spliterator.OfDouble (shadow created for its nullability, generated)
        class OfDouble {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            void forEachRemaining(@NotNull java.util.function.Consumer<? super java.lang.Double> p0) { }
            void forEachRemaining(@NotNull java.util.function.DoubleConsumer p0) { }
            boolean tryAdvance(@NotNull java.util.function.Consumer<? super java.lang.Double> p0) { return false; }
            boolean tryAdvance(@NotNull java.util.function.DoubleConsumer p0) { return false; }
            @Nullable java.util.Spliterator.OfDouble trySplit() { return null; }
        }

        //public java.util.Spliterator.OfInt (shadow created for its nullability, generated)
        class OfInt {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            void forEachRemaining(@NotNull java.util.function.Consumer<? super java.lang.Integer> p0) { }
            void forEachRemaining(@NotNull java.util.function.IntConsumer p0) { }
            boolean tryAdvance(@NotNull java.util.function.Consumer<? super java.lang.Integer> p0) { return false; }
            boolean tryAdvance(@NotNull java.util.function.IntConsumer p0) { return false; }
            @Nullable java.util.Spliterator.OfInt trySplit() { return null; }
        }

        //public java.util.Spliterator.OfLong (shadow created for its nullability, generated)
        class OfLong {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            void forEachRemaining(@NotNull java.util.function.Consumer<? super java.lang.Long> p0) { }
            void forEachRemaining(@NotNull java.util.function.LongConsumer p0) { }
            boolean tryAdvance(@NotNull java.util.function.Consumer<? super java.lang.Long> p0) { return false; }
            boolean tryAdvance(@NotNull java.util.function.LongConsumer p0) { return false; }
            @Nullable java.util.Spliterator.OfLong trySplit() { return null; }
        }

        //public java.util.Spliterator.OfPrimitive (shadow created for its nullability, generated)
        class OfPrimitive<T, T_CONS, T_SPLITR extends java.util.Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Nullable T_SPLITR trySplit() { return null; }
        }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void forEachRemaining(@NotNull java.util.function.Consumer<? super T> p0) { }
        boolean tryAdvance(@NotNull java.util.function.Consumer<? super T> p0) { return false; }
        @Nullable java.util.Spliterator<T> trySplit() { return null; }
    }

    //public class Stack<E> extends Vector<E>
    @Container
    class Stack$<E> {
        @NotModified E peek() { return null; }
        @NotModified boolean empty() { return false; }
        @NotModified int search(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @NotNull Object o) { return 0; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified E pop() { return null; }
        @Modified E push(E p0) { return null; }
    }

    //public final class StringJoiner
    // mutable, but add(CharSequence)/merge(StringJoiner) read their argument without modifying it.
    // add/merge/setEmptyValue modify this; length()/toString() do not.
    @Container
    class StringJoiner$ {
        @NotModified int length() { return 0; }
        //override from java.lang.Object
        @NotModified public String toString() { return null; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        StringJoiner$(@NotNull java.lang.CharSequence p0) { }
        StringJoiner$(@NotNull java.lang.CharSequence p0, @NotNull java.lang.CharSequence p1, @NotNull java.lang.CharSequence p2) { }
        @Modified java.util.StringJoiner add(@Nullable java.lang.CharSequence p0) { return null; }
        @Modified java.util.StringJoiner merge(@NotNull java.util.StringJoiner p0) { return null; }
        @Modified java.util.StringJoiner setEmptyValue(@NotNull java.lang.CharSequence p0) { return null; }
    }

    //public class StringTokenizer implements Enumeration<Object>
    // nextToken()/nextElement() advance the tokenizer (modifying); the counts/queries do not.
    @Container
    class StringTokenizer$ {
        @NotModified int countTokens() { return 0; }
        @NotModified boolean hasMoreTokens() { return false; }
        @NotModified boolean hasMoreElements() { return false; }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        StringTokenizer$(@NotNull java.lang.String p0) { }
        StringTokenizer$(@NotNull java.lang.String p0, @Nullable java.lang.String p1) { }
        StringTokenizer$(@NotNull java.lang.String p0, @Nullable java.lang.String p1, boolean p2) { }
        @Modified java.lang.String nextToken(@NotNull java.lang.String p0) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified Object nextElement() { return null; }
        @Modified String nextToken() { return null; }
    }

    //public class TreeMap extends AbstractMap<K,V> implements NavigableMap<K,V>, Cloneable, Serializable
    @Container
    class TreeMap$<K, V> {
        TreeMap$(/*@NotModified[O]*/ @Independent(hc = true) @NotNull Map<? extends K, ? extends V> m) { }
        TreeMap$(/*@NotModified[O]*/ @Independent(hc = true) @NotNull SortedMap<K, ? extends V> m) { }
        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        boolean containsKey(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object key) { return false; }

        //override from java.util.AbstractMap, java.util.Map
        boolean containsValue(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object value) {
            return false;
        }

        //override from java.util.AbstractMap, java.util.Map
        //@Independent(hc=true)[H] @NotModified[H]
        @Nullable V get(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ @Nullable Object key) { return null; }

        //override from java.util.SortedMap
        //@Immutable(hc=true)[T] @Independent(hc=true)[H]
        @Nullable Comparator<? super K> comparator() { return null; }

        //override from java.util.SortedMap
        //@Independent(hc=true)[H]
        K firstKey() { return null; }

        //override from java.util.SortedMap
        //@Independent(hc=true)[H]
        K lastKey() { return null; }

        //override from java.util.SequencedMap, java.util.SortedMap
        //@Independent(hc=true)[H]

        @Modified @Nullable V putFirst(/*@Independent(hc=true)[H] @NotModified[O]*/ K k, /*@Independent(hc=true)[H] @NotModified[O]*/ V v) {
            return null;
        }

        //override from java.util.SequencedMap, java.util.SortedMap
        //@Independent(hc=true)[H]

        @Modified @Nullable V putLast(/*@Independent(hc=true)[H] @NotModified[O]*/ K k, /*@Independent(hc=true)[H] @NotModified[O]*/ V v) {
            return null;
        }

        //override from java.util.AbstractMap, java.util.Map
        @Modified void putAll(/*@NotModified[O]*/ @NotNull Map<? extends K, ? extends V> map) { }

        //override from java.util.AbstractMap, java.util.Map
        //@Independent(hc=true)[H]

        @Modified @Nullable V put(/*@Independent(hc=true)[H] @NotModified[O]*/ K key, /*@Independent(hc=true)[H] @NotModified[O]*/ V value) {
            return null;
        }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Modified @Nullable V putIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V value) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Modified V computeIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ @NotNull Function<? super K, ? extends V> mappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Modified @Nullable V computeIfPresent(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = 2) BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Modified V compute(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ @NotNull(nullableArguments = {1, 2}) BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Modified V merge(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ @NotNull V value,
            /*@IgnoreModifications[T] @Independent(hc=true)[H]*/ @NotNull(nullableArguments = 2) BiFunction<? super V, ? super V, ? extends V>
                remappingFunction) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        //@Independent(hc=true)[H]
        @Modified @Nullable V remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ @Nullable Object key) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        @Modified void clear() { }

        //override from java.lang.Object, java.util.AbstractMap
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        //@NotModified[H]
        @Independent(hc = true) @Nullable Map.Entry<K, V> firstEntry() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        //@NotModified[H]
        @Independent(hc = true) @Nullable Map.Entry<K, V> lastEntry() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        @Modified @Independent(hc = true) @Nullable Map.Entry<K, V> pollFirstEntry() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        @Modified @Independent(hc = true) @Nullable Map.Entry<K, V> pollLastEntry() { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        @Independent(hc = true) @Nullable Map.Entry<K, V> lowerEntry(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@Independent(hc=true)[H] @NotModified[H]
        @Nullable K lowerKey(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        @Independent(hc = true) @Nullable Map.Entry<K, V> floorEntry(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@Independent(hc=true)[H] @NotModified[H]
        @Nullable K floorKey(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        @Independent(hc = true) @Nullable Map.Entry<K, V> ceilingEntry(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@Independent(hc=true)[H] @NotModified[H]
        @Nullable K ceilingKey(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        @Independent(hc = true) @Nullable Map.Entry<K, V> higherEntry(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@Independent(hc=true)[H] @NotModified[H]
        @Nullable K higherKey(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.AbstractMap, java.util.Map, java.util.SortedMap
        //@NotModified[H]
        Set<K> keySet() { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        NavigableSet<K> navigableKeySet() { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        NavigableSet<K> descendingKeySet() { return null; }

        //override from java.util.AbstractMap, java.util.Map, java.util.SortedMap
        //@NotModified[H]
        Collection<V> values() { return null; }

        //override from java.util.AbstractMap, java.util.Map, java.util.SortedMap
        //@NotModified[H]
        Set<Map.Entry<K, V>> entrySet() { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        NavigableMap<K, V> descendingMap() { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]

        NavigableMap<K, V> subMap(
            /*@Independent[M] @NotModified[O]*/ K fromKey,
            boolean fromInclusive,
            /*@Independent[M] @NotModified[O]*/ K toKey,
            boolean toInclusive) { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        NavigableMap<K, V> headMap(/*@Independent[M] @NotModified[O]*/ K toKey, boolean inclusive) { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        NavigableMap<K, V> tailMap(/*@Independent[M] @NotModified[O]*/ K fromKey, boolean inclusive) { return null; }

        //override from java.util.NavigableMap, java.util.SortedMap
        //@NotModified[H]

        SortedMap<K, V> subMap(
            /*@Independent[M] @NotModified[O]*/ K fromKey,
            /*@Independent[M] @NotModified[O]*/ K toKey) { return null; }

        //override from java.util.NavigableMap, java.util.SortedMap
        //@NotModified[H]
        SortedMap<K, V> headMap(/*@Independent[M] @NotModified[O]*/ K toKey) { return null; }

        //override from java.util.NavigableMap, java.util.SortedMap
        //@NotModified[H]
        SortedMap<K, V> tailMap(/*@Independent[M] @NotModified[O]*/ K fromKey) { return null; }

        //override from java.util.Map
        @Modified boolean replace(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V oldValue,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V newValue) { return false; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        @Modified @Nullable V replace(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V value) { return null; }

        //override from java.util.Map
        //@NotModified[H]
        void forEach(/*@IgnoreModifications[T] @Independent[M]*/ @NotNull BiConsumer<? super K, ? super V> action) { }

        //override from java.util.Map
        @Modified void replaceAll(/*@IgnoreModifications[T]*/ @NotNull BiFunction<? super K, ? super V, ? extends V> function) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        TreeMap$(@Nullable java.util.Comparator<? super K> p0) { }
    }

    //public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E>, Cloneable, Serializable
    @Container
    class TreeSet$<E> {
        TreeSet$(@NotNull @Independent(hc = true) @NotModified Collection<? extends E> c) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        TreeSet$(@Nullable java.util.Comparator<? super E> p0) { }
        TreeSet$(@NotNull @Independent(hc = true) @NotModified java.util.SortedSet<E> p0) { }
        @Modified boolean addAll(@NotNull java.util.Collection<? extends E> p0) { return false; }
        @Nullable E ceiling(E p0) { return null; }
        @Nullable java.util.Comparator<? super E> comparator() { return null; }
        boolean contains(@Nullable java.lang.Object p0) { return false; }
        @Nullable E floor(E p0) { return null; }
        @Nullable E higher(E p0) { return null; }
        @Nullable E lower(E p0) { return null; }
        @Modified @Nullable E pollFirst() { return null; }
        @Modified @Nullable E pollLast() { return null; }
        @Modified boolean remove(@Nullable java.lang.Object p0) { return false; }
    }

    //public final class UUID implements Serializable, Comparable<UUID>
    @ImmutableContainer
    class UUID$ {
        UUID$(long mostSigBits, long l) { }
        //frequency 3
        //@Immutable[T] @Independent[O] @NotModified[T]
        static UUID randomUUID() { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static UUID nameUUIDFromBytes(/*@Independent[T] @NotModified[O]*/ @NotNull byte [] name) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static UUID fromString(@NotNull String name) { return null; }

        //@NotModified[T]
        long getLeastSignificantBits() { return 0L; }

        //@NotModified[T]
        long getMostSignificantBits() { return 0L; }

        //@NotModified[T]
        int version() { return 0; }

        //@NotModified[T]
        int variant() { return 0; }

        //@NotModified[T]
        long timestamp() { return 0L; }

        //@NotModified[T]
        int clockSequence() { return 0; }

        //@NotModified[T]
        long node() { return 0L; }

        //override from java.lang.Object
        //@NotModified[T] @NotNull[H]
        public String toString() { return null; }

        //override from java.lang.Object
        //@NotModified[T]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[T]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

        //override from java.lang.Comparable
        //@NotModified[T]
        int compareTo(/*@Immutable[T] @Independent[T] @NotModified[O] @NotNull[H]*/ @NotNull UUID val) { return 0; }
    }

    //public class Vector extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable
    @Container
    class Vector$<E> {
        Vector$(@NotNull @Independent(hc = true) @NotModified Collection<? extends E> c) { }
        @NotModified void copyInto(@NotNull(nullableArguments = 0) Object [] anArray) { }
        @Modified void trimToSize() { }
        @Modified void ensureCapacity(int minCapacity) { }
        @Modified void setSize(int newSize) { }
        @NotModified int capacity() { return 0; }
        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        boolean isEmpty() { return false; }
        @NotModified Enumeration<E> elements() { return null; }
        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ @Nullable Object o) { return false; }

        //override from java.util.AbstractList, java.util.List
        int indexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @Nullable Object o) { return 0; }

        @NotModified
        int indexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Nullable Object o, int index) { return 0; }

        //override from java.util.AbstractList, java.util.List
        int lastIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @Nullable Object o) { return 0; }

        @NotModified
        int lastIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Nullable Object o, int index) {
            return 0;
        }

        //@Independent(hc=true)[T]
        @NotModified E elementAt(int index) { return null; }

        //@Independent(hc=true)[T]
        @NotModified E firstElement() { return null; }

        //@Independent(hc=true)[T]
        @NotModified E lastElement() { return null; }
        @Modified void setElementAt(/*@Independent(hc=true)[T] @NotModified[T]*/ E obj, int index) { }
        @Modified void removeElementAt(int index) { }
        @Modified void insertElementAt(/*@Independent(hc=true)[T] @NotModified[T]*/ E obj, int index) { }
        @Modified void addElement(/*@Independent(hc=true)[T] @NotModified[T]*/ E obj) { }
        @Modified boolean removeElement(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ @Nullable Object obj) {
            return false;
        }
        @Modified void removeAllElements() { }
        //override from java.lang.Object
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        @NotNull(nullableArguments = 0) Object [] toArray() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[H]*/ @NotNull T [] a) { return null; }

        //override from java.util.AbstractList, java.util.List
        //@Independent(hc=true)[H] @NotModified[H] @GetSet("_synthetic_list")[H]
        E get(int index) { return null; }

        //override from java.util.AbstractList, java.util.List
        //@Independent(hc=true)[H] @GetSet("_synthetic_list")[H]
        @Modified E set(int index, /*@Independent(hc=true)[H] @NotModified[T]*/ E element) { return null; }

        //override from java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.List
        @Modified boolean add(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        @Modified boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @Nullable Object o) { return false; }

        //override from java.util.AbstractList, java.util.List
        @Modified void add(int index, /*@Independent(hc=true)[H] @NotModified[T]*/ E element) { }

        //override from java.util.AbstractList, java.util.List
        //@Independent(hc=true)[H]
        @Modified E remove(int index) { return null; }

        //override from java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.List
        @Modified void clear() { }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        boolean containsAll(/*@Independent[M] @NotModified[H]*/ @NotNull Collection<?> c) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        @Modified boolean addAll(/*@Independent(hc=true)[H] @NotModified[H]*/ @NotNull Collection<? extends E> c) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        @Modified boolean removeAll(/*@Independent(hc=true)[H] @NotModified[H]*/ @NotNull Collection<?> c) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        @Modified boolean retainAll(/*@Independent(hc=true)[H] @NotModified[H]*/ @NotNull Collection<?> c) { return false; }

        //override from java.util.Collection
        @Modified boolean removeIf(/*@IgnoreModifications[T] @Independent(hc=true)[H]*/ @NotNull Predicate<? super E> filter) {
            return false;
        }

        //override from java.util.AbstractList, java.util.List
        @Modified boolean addAll(int index, /*@NotModified[H]*/ @NotNull Collection<? extends E> c) { return false; }

        //override from java.lang.Object, java.util.AbstractList, java.util.Collection, java.util.List
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object o) { return false; }

        //override from java.lang.Object, java.util.AbstractList, java.util.Collection, java.util.List
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object, java.util.AbstractCollection
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }

        //override from java.util.AbstractList, java.util.List
        //@NotModified[H]
        List<E> subList(int fromIndex, int toIndex) { return null; }

        //override from java.util.AbstractList, java.util.List
        ListIterator<E> listIterator(int index) { return null; }

        //override from java.util.AbstractList, java.util.List
        ListIterator<E> listIterator() { return null; }

        //override from java.lang.Iterable, java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.List
        //@NotModified[H] @NotNull[H]
        Iterator<E> iterator() { return null; }

        //override from java.lang.Iterable
        //@NotModified[H]
        void forEach(/*@IgnoreModifications[T] @Independent[M] @NotNull[H]*/ @NotNull Consumer<? super E> action) { }

        //override from java.util.List
        @Modified void replaceAll(/*@IgnoreModifications[T]*/ @NotNull UnaryOperator<E> operator) { }

        //override from java.util.List
        @Modified void sort(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ @Nullable Comparator<? super E> c) { }

        //override from java.lang.Iterable, java.util.Collection, java.util.List
        //@Independent(hc=true)[H] @NotModified[H] @NotNull[H]
        Spliterator<E> spliterator() { return null; }
    }

    //public class WeakHashMap<K, V> extends AbstractMap<K, V> implements Map<K, V>
    @Container
    class WeakHashMap$<K, V> {
        WeakHashMap$(/*@NotModified[O]*/ @Independent(hc = true) @NotNull Map<? extends K, ? extends V> m) { }
    
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean containsKey(@Nullable java.lang.Object p0) { return false; }
        boolean containsValue(@Nullable java.lang.Object p0) { return false; }
        void forEach(@NotNull java.util.function.BiConsumer<? super K, ? super V> p0) { }
        @Nullable V get(@Nullable java.lang.Object p0) { return null; }
        @Modified @Nullable V put(K p0, V p1) { return null; }
        @Modified void putAll(@NotNull java.util.Map<? extends K, ? extends V> p0) { }
        @Modified @Nullable V remove(@Nullable java.lang.Object p0) { return null; }
        @Modified void replaceAll(@NotNull java.util.function.BiFunction<? super K, ? super V, ? extends V> p0) { }
    }

    //public java.util.Calendar (shadow created for its nullability, generated)
    class Calendar$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Calendar$(@NotNull java.util.TimeZone p0, @NotNull java.util.Locale p1) { }
        boolean after(@NotNull java.lang.Object p0) { return false; }
        boolean before(@NotNull java.lang.Object p0) { return false; }
        int compareTo(@NotNull java.util.Calendar p0) { return 0; }
        @Nullable java.lang.String getDisplayName(int p0, int p1, @NotNull java.util.Locale p2) { return null; }
        @Nullable java.util.Map<java.lang.String, java.lang.Integer> getDisplayNames(int p0, int p1, @NotNull java.util.Locale p2) { return null; }
        static java.util.Calendar getInstance(@NotNull java.util.Locale p0) { return null; }
        static java.util.Calendar getInstance(@NotNull java.util.TimeZone p0) { return null; }
        static java.util.Calendar getInstance(@NotNull java.util.TimeZone p0, @NotNull java.util.Locale p1) { return null; }
        @Modified void setTime(@NotNull java.util.Date p0) { }
        @Modified void setTimeZone(@NotNull java.util.TimeZone p0) { }

        //public java.util.Calendar.Builder (shadow created for its nullability, generated)
        class Builder {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Modified java.util.Calendar.Builder setCalendarType(@NotNull java.lang.String p0) { return null; }
            @Modified java.util.Calendar.Builder setFields(@NotNull int... p0) { return null; }
            @Modified java.util.Calendar.Builder setInstant(@NotNull java.util.Date p0) { return null; }
            @Modified java.util.Calendar.Builder setLocale(@NotNull java.util.Locale p0) { return null; }
            @Modified java.util.Calendar.Builder setTimeZone(@NotNull java.util.TimeZone p0) { return null; }
        }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void add(int p0, int p1) { }
        @Modified void clear() { }
        @Modified void clear(int p0) { }
        @Modified void set(int p0, int p1) { }
        @Modified void set(int p0, int p1, int p2) { }
        @Modified void set(int p0, int p1, int p2, int p3, int p4) { }
        @Modified void set(int p0, int p1, int p2, int p3, int p4, int p5) { }
        @Modified void setFirstDayOfWeek(int p0) { }
        @Modified void setLenient(boolean p0) { }
        @Modified void setMinimalDaysInFirstWeek(int p0) { }
        @Modified void setTimeInMillis(long p0) { }
        @Modified void setWeekDate(int p0, int p1, int p2) { }
    }

    //public java.util.DoubleSummaryStatistics (shadow created for its nullability, generated)
    class DoubleSummaryStatistics$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void combine(@NotNull java.util.DoubleSummaryStatistics p0) { }
    }

    //public java.util.EventObject (shadow created for its nullability, generated)
    class EventObject$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        EventObject$(@NotNull java.lang.Object p0) { }
    }

    //public java.util.Formattable (shadow created for its nullability, generated)
    class Formattable$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void formatTo(@NotNull java.util.Formatter p0, int p1, int p2, int p3) { }
    }

    //public java.util.Formatter (shadow created for its nullability, generated)
    class Formatter$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Formatter$(@Nullable java.lang.Appendable p0) { }
        Formatter$(@Nullable java.lang.Appendable p0, @Nullable java.util.Locale p1) { }
        Formatter$(@NotNull java.io.File p0) { }
        Formatter$(@NotNull java.io.File p0, @NotNull java.nio.charset.Charset p1, @Nullable java.util.Locale p2) { }
        Formatter$(@NotNull java.io.File p0, @NotNull java.lang.String p1) { }
        Formatter$(@NotNull java.io.File p0, @NotNull java.lang.String p1, @Nullable java.util.Locale p2) { }
        Formatter$(@Nullable java.util.Locale p0) { }
        Formatter$(@NotNull java.io.OutputStream p0) { }
        Formatter$(@NotNull java.io.OutputStream p0, @NotNull java.nio.charset.Charset p1, @Nullable java.util.Locale p2) { }
        Formatter$(@NotNull java.io.OutputStream p0, @NotNull java.lang.String p1) { }
        Formatter$(@NotNull java.io.OutputStream p0, @NotNull java.lang.String p1, @Nullable java.util.Locale p2) { }
        Formatter$(@NotNull java.io.PrintStream p0) { }
        Formatter$(@NotNull java.lang.String p0) { }
        Formatter$(@NotNull java.lang.String p0, @NotNull java.nio.charset.Charset p1, @Nullable java.util.Locale p2) { }
        Formatter$(@NotNull java.lang.String p0, @NotNull java.lang.String p1) { }
        Formatter$(@NotNull java.lang.String p0, @NotNull java.lang.String p1, @Nullable java.util.Locale p2) { }
        java.util.Formatter format(@Nullable java.util.Locale p0, @NotNull java.lang.String p1, @NotNull(nullableArguments = 0) java.lang.Object... p2) { return null; }
        java.util.Formatter format(@NotNull java.lang.String p0, @NotNull(nullableArguments = 0) java.lang.Object... p1) { return null; }
        @Nullable java.io.IOException ioException() { return null; }
        @Nullable java.util.Locale locale() { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void flush() { }
    }

    //public java.util.GregorianCalendar (shadow created for its nullability, generated)
    class GregorianCalendar$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        GregorianCalendar$(@NotNull java.util.Locale p0) { }
        GregorianCalendar$(@NotNull java.util.TimeZone p0) { }
        GregorianCalendar$(@NotNull java.util.TimeZone p0, @NotNull java.util.Locale p1) { }
        static java.util.GregorianCalendar from(@NotNull java.time.ZonedDateTime p0) { return null; }
        @Modified void setGregorianChange(@NotNull java.util.Date p0) { }
        @Modified void setTimeZone(@NotNull java.util.TimeZone p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void add(int p0, int p1) { }
        @Modified void setWeekDate(int p0, int p1, int p2) { }
    }

    //public java.util.HexFormat (shadow created for its nullability, generated)
    // value-based, so immutable; NOT a @Container: formatHex(A, byte[]) appends to its Appendable argument
    @Immutable
    class HexFormat$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        <A extends java.lang.Appendable> A formatHex(A p0, @NotNull byte[] p1) { return null; }
        <A extends java.lang.Appendable> A formatHex(A p0, @NotNull byte[] p1, int p2, int p3) { return null; }
        java.lang.String formatHex(@NotNull byte[] p0) { return null; }
        java.lang.String formatHex(@NotNull byte[] p0, int p1, int p2) { return null; }
        static int fromHexDigits(@NotNull java.lang.CharSequence p0) { return 0; }
        static int fromHexDigits(@NotNull java.lang.CharSequence p0, int p1, int p2) { return 0; }
        static long fromHexDigitsToLong(@NotNull java.lang.CharSequence p0) { return 0; }
        static long fromHexDigitsToLong(@NotNull java.lang.CharSequence p0, int p1, int p2) { return 0; }
        static java.util.HexFormat ofDelimiter(@NotNull java.lang.String p0) { return null; }
        byte[] parseHex(@NotNull java.lang.CharSequence p0) { return null; }
        byte[] parseHex(@NotNull java.lang.CharSequence p0, int p1, int p2) { return null; }
        byte[] parseHex(@NotNull char[] p0, int p1, int p2) { return null; }
        java.util.HexFormat withDelimiter(@NotNull java.lang.String p0) { return null; }
        java.util.HexFormat withPrefix(@NotNull java.lang.String p0) { return null; }
        java.util.HexFormat withSuffix(@NotNull java.lang.String p0) { return null; }
    }

    //public java.util.IntSummaryStatistics (shadow created for its nullability, generated)
    class IntSummaryStatistics$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void combine(@NotNull java.util.IntSummaryStatistics p0) { }
    }

    //public java.util.ListResourceBundle (shadow created for its nullability, generated)
    class ListResourceBundle$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.Object handleGetObject(@Nullable java.lang.String p0) { return null; }
    }

    //public java.util.LongSummaryStatistics (shadow created for its nullability, generated)
    class LongSummaryStatistics$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void combine(@NotNull java.util.LongSummaryStatistics p0) { }
    }

    //public java.util.Observable (shadow created for its nullability, generated)
    class Observable$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified void addObserver(@NotNull java.util.Observer p0) { }
        @Modified void deleteObserver(@Nullable java.util.Observer p0) { }
        void notifyObservers(@Nullable java.lang.Object p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void deleteObservers() { }
    }

    //public java.util.Observer (shadow created for its nullability, generated)
    class Observer$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified void update(@NotNull java.util.Observable p0, @Nullable java.lang.Object p1) { }
    }

    //public java.util.OptionalLong (shadow created for its nullability, generated)
    @ImmutableContainer
    class OptionalLong$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void ifPresent(@NotNull java.util.function.LongConsumer p0) { }
        void ifPresentOrElse(@NotNull java.util.function.LongConsumer p0, @NotNull java.lang.Runnable p1) { }
        long orElseGet(@NotNull java.util.function.LongSupplier p0) { return 0; }
        <X extends java.lang.Throwable> long orElseThrow(@NotNull java.util.function.Supplier<? extends X> p0) { return 0; }
    }

    //public java.util.PropertyPermission (shadow created for its nullability, generated)
    class PropertyPermission$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PropertyPermission$(@NotNull java.lang.String p0, @Nullable java.lang.String p1) { }
        boolean implies(@NotNull java.security.Permission p0) { return false; }
    }

    //public java.util.ResourceBundle (shadow created for its nullability, generated)
    class ResourceBundle$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static void clearCache(@NotNull java.lang.ClassLoader p0) { }
        boolean containsKey(@NotNull java.lang.String p0) { return false; }
        @Nullable java.lang.String getBaseBundleName() { return null; }
        static java.util.ResourceBundle getBundle(@NotNull java.lang.String p0) { return null; }
        static java.util.ResourceBundle getBundle(@NotNull java.lang.String p0, @NotNull java.util.ResourceBundle.Control p1) { return null; }
        static java.util.ResourceBundle getBundle(@NotNull java.lang.String p0, @NotNull java.util.Locale p1) { return null; }
        static java.util.ResourceBundle getBundle(@NotNull java.lang.String p0, @NotNull java.util.Locale p1, @NotNull java.lang.ClassLoader p2) { return null; }
        static java.util.ResourceBundle getBundle(@NotNull java.lang.String p0, @NotNull java.util.Locale p1, @NotNull java.lang.ClassLoader p2, @NotNull java.util.ResourceBundle.Control p3) { return null; }
        static java.util.ResourceBundle getBundle(@NotNull java.lang.String p0, @NotNull java.util.Locale p1, @NotNull java.util.ResourceBundle.Control p2) { return null; }
        static java.util.ResourceBundle getBundle(@NotNull java.lang.String p0, @NotNull java.util.Locale p1, @NotNull java.lang.Module p2) { return null; }
        static java.util.ResourceBundle getBundle(@NotNull java.lang.String p0, @NotNull java.lang.Module p1) { return null; }
        java.lang.Object getObject(@NotNull java.lang.String p0) { return null; }
        java.lang.String getString(@NotNull java.lang.String p0) { return null; }
        java.lang.String[] getStringArray(@NotNull java.lang.String p0) { return null; }
        @Nullable java.lang.Object handleGetObject(@NotNull java.lang.String p0) { return null; }
        void setParent(@Nullable java.util.ResourceBundle p0) { }

        //public java.util.ResourceBundle.Control (shadow created for its nullability, generated)
        class Control {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            java.util.List<java.util.Locale> getCandidateLocales(@NotNull java.lang.String p0, @NotNull java.util.Locale p1) { return null; }
            static java.util.ResourceBundle.Control getControl(@NotNull java.util.List<java.lang.String> p0) { return null; }
            @Nullable java.util.Locale getFallbackLocale(@NotNull java.lang.String p0, @NotNull java.util.Locale p1) { return null; }
            java.util.List<java.lang.String> getFormats(@NotNull java.lang.String p0) { return null; }
            static java.util.ResourceBundle.Control getNoFallbackControl(@NotNull java.util.List<java.lang.String> p0) { return null; }
            long getTimeToLive(@NotNull java.lang.String p0, @NotNull java.util.Locale p1) { return 0; }
            boolean needsReload(@NotNull java.lang.String p0, @NotNull java.util.Locale p1, @NotNull java.lang.String p2, @NotNull java.lang.ClassLoader p3, @NotNull java.util.ResourceBundle p4, long p5) { return false; }
            @Nullable java.util.ResourceBundle newBundle(@NotNull java.lang.String p0, @NotNull java.util.Locale p1, @NotNull java.lang.String p2, @NotNull java.lang.ClassLoader p3, boolean p4) { return null; }
            java.lang.String toBundleName(@NotNull java.lang.String p0, @NotNull java.util.Locale p1) { return null; }
            java.lang.String toResourceName(@NotNull java.lang.String p0, @NotNull java.lang.String p1) { return null; }
        }
    }

    //public java.util.Scanner (shadow created for its nullability, generated)
    class Scanner$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Scanner$(@NotNull java.io.File p0) { }
        Scanner$(@NotNull java.io.File p0, @NotNull java.nio.charset.Charset p1) { }
        Scanner$(@NotNull java.io.File p0, @NotNull java.lang.String p1) { }
        Scanner$(@NotNull java.io.InputStream p0) { }
        Scanner$(@NotNull java.io.InputStream p0, @NotNull java.nio.charset.Charset p1) { }
        Scanner$(@NotNull java.io.InputStream p0, @NotNull java.lang.String p1) { }
        Scanner$(@NotNull java.nio.file.Path p0) { }
        Scanner$(@NotNull java.nio.file.Path p0, @NotNull java.nio.charset.Charset p1) { }
        Scanner$(@NotNull java.nio.file.Path p0, @NotNull java.lang.String p1) { }
        Scanner$(@NotNull java.lang.Readable p0) { }
        Scanner$(@NotNull java.nio.channels.ReadableByteChannel p0) { }
        Scanner$(@NotNull java.nio.channels.ReadableByteChannel p0, @NotNull java.nio.charset.Charset p1) { }
        Scanner$(@NotNull java.nio.channels.ReadableByteChannel p0, @NotNull java.lang.String p1) { }
        Scanner$(@NotNull java.lang.String p0) { }
        java.util.stream.Stream<java.util.regex.MatchResult> findAll(@NotNull java.util.regex.Pattern p0) { return null; }
        java.util.stream.Stream<java.util.regex.MatchResult> findAll(@NotNull java.lang.String p0) { return null; }
        @Nullable java.lang.String findInLine(@NotNull java.util.regex.Pattern p0) { return null; }
        @Nullable java.lang.String findInLine(@NotNull java.lang.String p0) { return null; }
        @Nullable java.lang.String findWithinHorizon(@NotNull java.util.regex.Pattern p0, int p1) { return null; }
        @Nullable java.lang.String findWithinHorizon(@NotNull java.lang.String p0, int p1) { return null; }
        boolean hasNext(@NotNull java.util.regex.Pattern p0) { return false; }
        boolean hasNext(@NotNull java.lang.String p0) { return false; }
        @Nullable java.io.IOException ioException() { return null; }
        @Modified java.lang.String next(@NotNull java.util.regex.Pattern p0) { return null; }
        @Modified java.lang.String next(@NotNull java.lang.String p0) { return null; }
        @Modified java.util.Scanner skip(@NotNull java.util.regex.Pattern p0) { return null; }
        @Modified java.util.Scanner skip(@NotNull java.lang.String p0) { return null; }
        java.util.Scanner useDelimiter(@NotNull java.util.regex.Pattern p0) { return null; }
        java.util.Scanner useDelimiter(@NotNull java.lang.String p0) { return null; }
        java.util.Scanner useLocale(@NotNull java.util.Locale p0) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified java.math.BigDecimal nextBigDecimal() { return null; }
        @Modified java.math.BigInteger nextBigInteger() { return null; }
        @Modified java.math.BigInteger nextBigInteger(int p0) { return null; }
        @Modified boolean nextBoolean() { return false; }
        @Modified byte nextByte() { return 0; }
        @Modified byte nextByte(int p0) { return 0; }
        @Modified double nextDouble() { return 0; }
        @Modified float nextFloat() { return 0; }
        @Modified int nextInt() { return 0; }
        @Modified int nextInt(int p0) { return 0; }
        @Modified String nextLine() { return null; }
        @Modified long nextLong() { return 0; }
        @Modified long nextLong(int p0) { return 0; }
        @Modified short nextShort() { return 0; }
        @Modified short nextShort(int p0) { return 0; }
        @Modified java.util.Scanner reset() { return null; }
    }

    //public java.util.ServiceLoader (shadow created for its nullability, generated)
    class ServiceLoader$<S> {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static <S> java.util.ServiceLoader<S> load(@NotNull java.lang.Class<S> p0) { return null; }
        static <S> java.util.ServiceLoader<S> load(@NotNull java.lang.Class<S> p0, @Nullable java.lang.ClassLoader p1) { return null; }
        static <S> java.util.ServiceLoader<S> load(@NotNull java.lang.ModuleLayer p0, @NotNull java.lang.Class<S> p1) { return null; }
        static <S> java.util.ServiceLoader<S> loadInstalled(@NotNull java.lang.Class<S> p0) { return null; }
    }

    //public java.util.SimpleTimeZone (shadow created for its nullability, generated)
    class SimpleTimeZone$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SimpleTimeZone$(int p0, @NotNull java.lang.String p1) { }
        SimpleTimeZone$(int p0, @NotNull java.lang.String p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9) { }
        SimpleTimeZone$(int p0, @NotNull java.lang.String p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10) { }
        SimpleTimeZone$(int p0, @NotNull java.lang.String p1, int p2, int p3, int p4, int p5, int p6, int p7, int p8, int p9, int p10, int p11, int p12) { }
        boolean hasSameRules(@NotNull java.util.TimeZone p0) { return false; }
        boolean inDaylightTime(@NotNull java.util.Date p0) { return false; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setDSTSavings(int p0) { }
        @Modified void setEndRule(int p0, int p1, int p2) { }
        @Modified void setEndRule(int p0, int p1, int p2, int p3) { }
        @Modified void setEndRule(int p0, int p1, int p2, int p3, boolean p4) { }
        @Modified void setRawOffset(int p0) { }
        @Modified void setStartRule(int p0, int p1, int p2) { }
        @Modified void setStartRule(int p0, int p1, int p2, int p3) { }
        @Modified void setStartRule(int p0, int p1, int p2, int p3, boolean p4) { }
        @Modified void setStartYear(int p0) { }
    }

    //public java.util.Spliterators (shadow created for its nullability, generated)
    class Spliterators$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static java.util.PrimitiveIterator.OfDouble iterator(@NotNull java.util.Spliterator.OfDouble p0) { return null; }
        static java.util.PrimitiveIterator.OfInt iterator(@NotNull java.util.Spliterator.OfInt p0) { return null; }
        static java.util.PrimitiveIterator.OfLong iterator(@NotNull java.util.Spliterator.OfLong p0) { return null; }
        static <T> java.util.Iterator<T> iterator(@NotNull java.util.Spliterator<? extends T> p0) { return null; }
        static <T> java.util.Spliterator<T> spliterator(@NotNull java.util.Collection<? extends T> p0, int p1) { return null; }
        static <T> java.util.Spliterator<T> spliterator(@NotNull java.util.Iterator<? extends T> p0, long p1, int p2) { return null; }
        static <T> java.util.Spliterator<T> spliterator(@NotNull(nullableArguments = 0) java.lang.Object[] p0, int p1) { return null; }
        static <T> java.util.Spliterator<T> spliterator(@NotNull(nullableArguments = 0) java.lang.Object[] p0, int p1, int p2, int p3) { return null; }
        static java.util.Spliterator.OfDouble spliterator(@NotNull java.util.PrimitiveIterator.OfDouble p0, long p1, int p2) { return null; }
        static java.util.Spliterator.OfInt spliterator(@NotNull java.util.PrimitiveIterator.OfInt p0, long p1, int p2) { return null; }
        static java.util.Spliterator.OfLong spliterator(@NotNull java.util.PrimitiveIterator.OfLong p0, long p1, int p2) { return null; }
        static java.util.Spliterator.OfDouble spliterator(@NotNull double[] p0, int p1) { return null; }
        static java.util.Spliterator.OfDouble spliterator(@NotNull double[] p0, int p1, int p2, int p3) { return null; }
        static java.util.Spliterator.OfInt spliterator(@NotNull int[] p0, int p1) { return null; }
        static java.util.Spliterator.OfInt spliterator(@NotNull int[] p0, int p1, int p2, int p3) { return null; }
        static java.util.Spliterator.OfLong spliterator(@NotNull long[] p0, int p1) { return null; }
        static java.util.Spliterator.OfLong spliterator(@NotNull long[] p0, int p1, int p2, int p3) { return null; }
        static <T> java.util.Spliterator<T> spliteratorUnknownSize(@NotNull java.util.Iterator<? extends T> p0, int p1) { return null; }
        static java.util.Spliterator.OfDouble spliteratorUnknownSize(@NotNull java.util.PrimitiveIterator.OfDouble p0, int p1) { return null; }
        static java.util.Spliterator.OfInt spliteratorUnknownSize(@NotNull java.util.PrimitiveIterator.OfInt p0, int p1) { return null; }
        static java.util.Spliterator.OfLong spliteratorUnknownSize(@NotNull java.util.PrimitiveIterator.OfLong p0, int p1) { return null; }

        //public java.util.Spliterators.AbstractDoubleSpliterator (shadow created for its nullability, generated)
        class AbstractDoubleSpliterator {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Nullable java.util.Spliterator.OfDouble trySplit() { return null; }
        }
        //public java.util.Spliterators.AbstractIntSpliterator (shadow created for its nullability, generated)
        class AbstractIntSpliterator {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Nullable java.util.Spliterator.OfInt trySplit() { return null; }
        }
        //public java.util.Spliterators.AbstractLongSpliterator (shadow created for its nullability, generated)
        class AbstractLongSpliterator {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Nullable java.util.Spliterator.OfLong trySplit() { return null; }
        }
        //public java.util.Spliterators.AbstractSpliterator (shadow created for its nullability, generated)
        class AbstractSpliterator<T> {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            @Nullable java.util.Spliterator<T> trySplit() { return null; }
        }
    }

    //public java.util.TimeZone (shadow created for its nullability, generated)
    class TimeZone$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.lang.String getDisplayName(@NotNull java.util.Locale p0) { return null; }
        java.lang.String getDisplayName(boolean p0, int p1, @NotNull java.util.Locale p2) { return null; }
        static java.util.TimeZone getTimeZone(@NotNull java.lang.String p0) { return null; }
        static java.util.TimeZone getTimeZone(@NotNull java.time.ZoneId p0) { return null; }
        boolean hasSameRules(@Nullable java.util.TimeZone p0) { return false; }
        boolean inDaylightTime(@NotNull java.util.Date p0) { return false; }
        static void setDefault(@Nullable java.util.TimeZone p0) { }
        @Modified void setID(@NotNull java.lang.String p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setRawOffset(int p0) { }
    }

    //public java.util.Timer (shadow created for its nullability, generated)
    class Timer$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Timer$(@NotNull java.lang.String p0) { }
        Timer$(@NotNull java.lang.String p0, boolean p1) { }
        @Modified void schedule(@NotNull java.util.TimerTask p0, @NotNull java.util.Date p1) { }
        @Modified void schedule(@NotNull java.util.TimerTask p0, @NotNull java.util.Date p1, long p2) { }
        @Modified void schedule(@NotNull java.util.TimerTask p0, long p1) { }
        @Modified void schedule(@NotNull java.util.TimerTask p0, long p1, long p2) { }
        @Modified void scheduleAtFixedRate(@NotNull java.util.TimerTask p0, @NotNull java.util.Date p1, long p2) { }
        @Modified void scheduleAtFixedRate(@NotNull java.util.TimerTask p0, long p1, long p2) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void cancel() { }
    }

    //public java.util.PrimitiveIterator (shadow created to hold nested types, generated)
    class PrimitiveIterator$<T, T_CONS> {
        //public java.util.PrimitiveIterator.OfDouble (shadow created for its nullability, generated)
        class OfDouble {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            void forEachRemaining(@NotNull java.util.function.Consumer<? super java.lang.Double> p0) { }
            void forEachRemaining(@NotNull java.util.function.DoubleConsumer p0) { }
        }
        //public java.util.PrimitiveIterator.OfInt (shadow created for its nullability, generated)
        class OfInt {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            void forEachRemaining(@NotNull java.util.function.Consumer<? super java.lang.Integer> p0) { }
            void forEachRemaining(@NotNull java.util.function.IntConsumer p0) { }
        }
        //public java.util.PrimitiveIterator.OfLong (shadow created for its nullability, generated)
        class OfLong {
            // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
            void forEachRemaining(@NotNull java.util.function.Consumer<? super java.lang.Long> p0) { }
            void forEachRemaining(@NotNull java.util.function.LongConsumer p0) { }
        }
    }
}
