package org.e2immu.analyzer.aapi.archive.v2.jdk;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;
import java.util.function.*;
import java.util.random.RandomGenerator;
import java.util.stream.*;

import org.e2immu.annotation.*;
import org.e2immu.annotation.method.GetSet;
import org.e2immu.annotation.type.UtilityClass;
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
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object o) { return false; }

        //override from java.util.Collection
        //@NotModified[H]
        Object [] toArray() { return null; }

        //override from java.util.Collection
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[O]*/ T [] a) { return null; }

        //override from java.util.Collection
        //override has frequency 16
        boolean add(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { return false; }

        //override from java.util.Collection
        boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object o) { return false; }

        //override from java.util.Collection
        //@NotModified[H]
        boolean containsAll(/*@Independent[M] @NotModified[O]*/ Collection<?> c) { return false; }

        //override from java.util.Collection
        //override has frequency 1
        boolean addAll(/*@Independent(hc=true)[H] @NotModified[O]*/ Collection<? extends E> c) { return false; }

        //override from java.util.Collection
        boolean removeAll(/*@Independent(hc=true)[H] @NotModified[O]*/ Collection<?> c) { return false; }

        //override from java.util.Collection
        boolean retainAll(/*@Independent(hc=true)[H] @NotModified[O]*/ Collection<?> c) { return false; }

        //override from java.util.Collection
        void clear() { }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
    }

    //public abstract class AbstractList extends AbstractCollection<E> implements List<E>
    @Container
    class AbstractList$<E> {
        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //override has frequency 16
        boolean add(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { return false; }

        //override from java.util.List
        //@Independent(hc=true)[H] @NotModified[H] @GetSet("_synthetic_list")[H]
        E get(int i) { return null; }

        //override from java.util.List
        //@Independent(hc=true)[H] @GetSet("_synthetic_list")[H]
        E set(int index, /*@Independent(hc=true)[H] @NotModified[O]*/ E element) { return null; }

        //override from java.util.List
        void add(int index, /*@Independent(hc=true)[H] @NotModified[O]*/ E element) { }

        //override from java.util.List
        //@Independent(hc=true)[H]
        E remove(int index) { return null; }

        //override from java.util.List
        int indexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object o) { return 0; }

        //override from java.util.List
        int lastIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object o) { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        void clear() { }

        //override from java.util.List
        boolean addAll(int index, /*@NotModified[O]*/ Collection<? extends E> c) { return false; }

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
            SimpleEntry(Map.Entry<? extends K, ? extends V> entry) { }
            //override from java.util.Map.Entry
            //@Independent(hc=true)[H] @NotModified[H]
            K getKey() { return null; }

            //override from java.util.Map.Entry
            //@Independent(hc=true)[H] @NotModified[H]
            V getValue() { return null; }

            //override from java.util.Map.Entry
            //@Independent(hc=true)[H]
            V setValue(/*@Independent(hc=true)[H] @NotModified[T]*/ V value) { return null; }

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
            SimpleImmutableEntry(Map.Entry<? extends K, ? extends V> entry) { }
            //override from java.util.Map.Entry
            //@Independent(hc=true)[H] @NotModified[H]
            K getKey() { return null; }

            //override from java.util.Map.Entry
            //@Independent(hc=true)[H] @NotModified[H]
            V getValue() { return null; }

            //override from java.util.Map.Entry
            //@Independent(hc=true)[H]
            V setValue(/*@Independent(hc=true)[H] @NotModified[T]*/ V value) { return null; }

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
        boolean containsValue(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object value) {
            return false;
        }

        //override from java.util.Map
        //override has frequency 2
        //@NotModified[H]
        boolean containsKey(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object key) { return false; }

        //override from java.util.Map
        //override has frequency 2
        //@Independent(hc=true)[H] @NotModified[H]
        V get(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object key) { return null; }

        //override from java.util.Map
        //override has frequency 1
        //@Independent(hc=true)[H]

        V put(/*@Independent(hc=true)[H] @NotModified[O]*/ K key, /*@Independent(hc=true)[H] @NotModified[O]*/ V value) {
            return null;
        }

        //override from java.util.Map
        //@Independent(hc=true)[H]
        V remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object key) { return null; }

        //override from java.util.Map
        void putAll(/*@NotModified[O]*/ Map<? extends K, ? extends V> m) { }

        //override from java.util.Map
        void clear() { }

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

    //public abstract class AbstractSet extends AbstractCollection<E> implements Set<E>
    class AbstractSet$<E> {
        //override from java.lang.Object, java.util.Collection, java.util.Set
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object o) { return false; }

        //override from java.lang.Object, java.util.Collection, java.util.Set
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        boolean removeAll(/*@Independent(hc=true)[H] @NotModified[H]*/ Collection<?> c) { return false; }
    }

    //public class ArrayList extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable
    @Container
    class ArrayList$<E> {
        //frequency 1
        ArrayList$(int initialCapacity) { }

        //frequency 58
        ArrayList$() { }

        //frequency 5
        ArrayList$(@Independent(hc = true) @NotModified Collection<? extends E> c) { }
        void trimToSize() { }
        void ensureCapacity(int minCapacity) { }
        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        boolean isEmpty() { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object o) { return false; }

        //override from java.util.AbstractList, java.util.List
        int indexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object o) { return 0; }

        //override from java.util.AbstractList, java.util.List
        int lastIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object o) { return 0; }

        //override from java.lang.Object
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        Object [] toArray() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[O]*/ T [] a) { return null; }

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
        E set(int index, /*@Independent(hc=true)[H] @NotModified[O]*/ E element) { return null; }

        //override from java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.List
        //frequency 16
        boolean add(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { return false; }

        //override from java.util.AbstractList, java.util.List
        void add(int index, /*@Independent(hc=true)[H] @NotModified[O]*/ E element) { }

        //override from java.util.List, java.util.SequencedCollection
        void addFirst(/*@Independent(hc=true)[H] @NotModified[O]*/ E element) { }

        //override from java.util.List, java.util.SequencedCollection
        void addLast(/*@Independent(hc=true)[H] @NotModified[O]*/ E element) { }

        //override from java.util.AbstractList, java.util.List
        //@Independent(hc=true)[H]
        E remove(int index) { return null; }

        //override from java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H]
        E removeFirst() { return null; }

        //override from java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H]
        E removeLast() { return null; }

        //override from java.lang.Object, java.util.AbstractList, java.util.Collection, java.util.List
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object o) { return false; }

        //override from java.lang.Object, java.util.AbstractList, java.util.Collection, java.util.List
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object o) { return false; }

        //override from java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.List
        void clear() { }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //frequency 1
        boolean addAll(/*@Independent(hc=true)[H] @NotModified[O]*/ Collection<? extends E> c) { return false; }

        //override from java.util.AbstractList, java.util.List
        boolean addAll(int index, /*@NotModified[O]*/ Collection<? extends E> c) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        boolean removeAll(/*@Independent(hc=true)[H] @NotModified[O]*/ Collection<?> c) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        boolean retainAll(/*@Independent(hc=true)[H] @NotModified[O]*/ Collection<?> c) { return false; }

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
        void forEach(/*@IgnoreModifications[T] @Independent[M] @NotNull[H]*/ Consumer<? super E> action) { }

        //override from java.lang.Iterable, java.util.Collection, java.util.List
        //@Independent(hc=true)[H] @NotModified[H] @NotNull[H]
        Spliterator<E> spliterator() { return null; }

        //override from java.util.Collection
        boolean removeIf(/*@IgnoreModifications[T] @Independent(hc=true)[H]*/ Predicate<? super E> filter) {
            return false;
        }

        //override from java.util.List
        void replaceAll(/*@IgnoreModifications[T]*/ UnaryOperator<E> operator) { }

        //override from java.util.List
        //frequency 1
        void sort(/*@Immutable(hc=true)[T]*/ @Independent(hc = true) @NotModified Comparator<? super E> c) { }
    }

    //public final class Arrays
    @UtilityClass
    class Arrays$ {
        //@NotModified[T]
        static void sort(/*@Independent[M]*/ int [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ int [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ long [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ long [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ short [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ short [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ char [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ char [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ byte [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ byte [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ float [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ float [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ double [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ double [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ byte [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ byte [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ char [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ char [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ short [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ short [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ int [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ int [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ long [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ long [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ float [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ float [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ double [] a) { }

        //@NotModified[T]
        static void parallelSort(/*@Independent[M]*/ double [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static <T extends Comparable<? super T>> void parallelSort(/*@Independent[M]*/ T [] a) { }

        //@NotModified[T]
        static <T extends Comparable<? super T>> void parallelSort(
            /*@Independent[M]*/ T [] a,
            int fromIndex,
            int toIndex) { }

        //@NotModified[T]
        static <T> void parallelSort(
            /*@Independent[M]*/ T [] a,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> cmp) { }

        //@NotModified[T]
        static <T> void parallelSort(
            /*@Independent[M]*/ T [] a,
            int fromIndex,
            int toIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> cmp) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ Object [] a) { }

        //@NotModified[T]
        static void sort(/*@Independent[M]*/ Object [] a, int fromIndex, int toIndex) { }

        //@NotModified[T]
        static <T> void sort(
            /*@Independent[M]*/ T [] a,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> c) { }

        //@NotModified[T]
        static <T> void sort(
            /*@Independent[M]*/ T [] a,
            int fromIndex,
            int toIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> c) { }

        //@NotModified[T]
        static <T> void parallelPrefix(
            /*@Independent[M]*/ T [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ BinaryOperator<T> op) { }

        //@NotModified[T]
        static <T> void parallelPrefix(
            /*@Independent[M]*/ T [] array,
            int fromIndex,
            int toIndex,
            /*@IgnoreModifications[T] @Independent[M]*/ BinaryOperator<T> op) { }

        //@NotModified[T]
        static void parallelPrefix(
            /*@Independent[M]*/ long [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ LongBinaryOperator op) { }

        //@NotModified[T]
        static void parallelPrefix(
            /*@Independent[M]*/ long [] array,
            int fromIndex,
            int toIndex,
            /*@IgnoreModifications[T] @Independent[M]*/ LongBinaryOperator op) { }

        //@NotModified[T]
        static void parallelPrefix(
            /*@Independent[M]*/ double [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ DoubleBinaryOperator op) { }

        //@NotModified[T]
        static void parallelPrefix(
            /*@Independent[M]*/ double [] array,
            int fromIndex,
            int toIndex,
            /*@IgnoreModifications[T] @Independent[M]*/ DoubleBinaryOperator op) { }

        //@NotModified[T]
        static void parallelPrefix(
            /*@Independent[M]*/ int [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ IntBinaryOperator op) { }

        //@NotModified[T]
        static void parallelPrefix(
            /*@Independent[M]*/ int [] array,
            int fromIndex,
            int toIndex,
            /*@IgnoreModifications[T] @Independent[M]*/ IntBinaryOperator op) { }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified long [] a, long key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified long [] a, int fromIndex, int toIndex, long key) {
            return 0;
        }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified int [] a, int key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified int [] a, int fromIndex, int toIndex, int key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified short [] a, short key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified short [] a, int fromIndex, int toIndex, short key) {
            return 0;
        }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified char [] a, char key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified char [] a, int fromIndex, int toIndex, char key) {
            return 0;
        }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified byte [] a, byte key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified byte [] a, int fromIndex, int toIndex, byte key) {
            return 0;
        }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified double [] a, double key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified double [] a, int fromIndex, int toIndex, double key) {
            return 0;
        }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified float [] a, float key) { return 0; }

        //@NotModified[T]
        static int binarySearch(/*@Independent[M]*/ @NotModified float [] a, int fromIndex, int toIndex, float key) {
            return 0;
        }

        //@NotModified[T]
        static int binarySearch(
            /*@Independent[M]*/ @NotModified Object [] a,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object key) { return 0; }

        //@NotModified[T]
        static int binarySearch(
            /*@Independent[M]*/ @NotModified Object [] a,
            int fromIndex,
            int toIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object key) { return 0; }

        //@NotModified[T]
        static <T> int binarySearch(
            /*@Independent[M]*/ @NotModified T [] a,
            /*@Independent[M] @NotModified[T]*/ T key,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> c) { return 0; }

        //@NotModified[T]
        static <T> int binarySearch(
            /*@Independent[M]*/ @NotModified T [] a,
            int fromIndex,
            int toIndex,
            /*@Independent[M] @NotModified[T]*/ T key,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> c) { return 0; }

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
        static void fill(/*@Independent[M]*/ long [] a, long val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ long [] a, int fromIndex, int toIndex, long val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ int [] a, int val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ int [] a, int fromIndex, int toIndex, int val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ short [] a, short val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ short [] a, int fromIndex, int toIndex, short val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ char [] a, char val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ char [] a, int fromIndex, int toIndex, char val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ byte [] a, byte val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ byte [] a, int fromIndex, int toIndex, byte val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ boolean [] a, boolean val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ boolean [] a, int fromIndex, int toIndex, boolean val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ double [] a, double val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ double [] a, int fromIndex, int toIndex, double val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ float [] a, float val) { }

        //@NotModified[T]
        static void fill(/*@Independent[M]*/ float [] a, int fromIndex, int toIndex, float val) { }

        //@NotModified[T]
        static void fill(
            @Independent(hcParameters = { 1 }) Object [] a,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object val) { }

        //@NotModified[T]
        static void fill(
            @Independent(hcParameters = { 3 }) Object [] a,
            int fromIndex,
            int toIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object val) { }

        //@Independent[O] @NotModified[T]
        static <T> T [] copyOf(@Independent(hcReturnValue = true) @NotModified T [] original, int newLength) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static <T, U> T [] copyOf(
            @Independent(hcReturnValue = true) @NotModified U [] original,
            int newLength,
            Class<? extends T []> newType) { return null; }

        //@Independent[O] @NotModified[T]
        static byte [] copyOf(/*@Independent[M]*/ @NotModified byte [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static short [] copyOf(/*@Independent[M]*/ @NotModified short [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static int [] copyOf(/*@Independent[M]*/ @NotModified int [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static long [] copyOf(/*@Independent[M]*/ @NotModified long [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static char [] copyOf(/*@Independent[M]*/ @NotModified char [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static float [] copyOf(/*@Independent[M]*/ @NotModified float [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static double [] copyOf(/*@Independent[M]*/ @NotModified double [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static boolean [] copyOf(/*@Independent[M]*/ @NotModified boolean [] original, int newLength) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> T [] copyOfRange(/*@Independent[M]*/ @NotModified T [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static <T, U> T [] copyOfRange(
            @Independent(hcReturnValue = true) @NotModified U [] original,
            int from,
            int to,
            Class<? extends T []> newType) { return null; }

        //@Independent[O] @NotModified[T]
        static byte [] copyOfRange(/*@Independent[M]*/ @NotModified byte [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static short [] copyOfRange(/*@Independent[M]*/ @NotModified short [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static int [] copyOfRange(/*@Independent[M]*/ @NotModified int [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static long [] copyOfRange(/*@Independent[M]*/ @NotModified long [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static char [] copyOfRange(/*@Independent[M]*/ @NotModified char [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static float [] copyOfRange(/*@Independent[M]*/ @NotModified float [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static double [] copyOfRange(/*@Independent[M]*/ @NotModified double [] original, int from, int to) { return null; }

        //@Independent[O] @NotModified[T]
        static boolean [] copyOfRange(/*@Independent[M]*/ @NotModified boolean [] original, int from, int to) {
            return null;
        }

        //frequency 21
        //@Independent[O]
        @NotModified
        static <T> List<T> asList(/*@Independent[M]*/ @NotModified T ... a) { return null; }

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
        static int deepHashCode(/*@Independent[M]*/ Object [] a) { return 0; }

        //@NotModified[T]
        static boolean deepEquals(/*@Independent[M]*/ Object [] a1, /*@Independent[M]*/ Object [] a2) { return false; }

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
        static String deepToString(/*@Independent[M]*/ @NotModified Object [] a) { return null; }

        //@NotModified[T]
        static <T> void setAll(
            /*@Independent[M]*/ T [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ IntFunction<? extends T> generator) { }

        //@NotModified[T]
        static <T> void parallelSetAll(
            /*@Independent[M]*/ T [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ IntFunction<? extends T> generator) { }

        //@NotModified[T]
        static void setAll(
            /*@Independent[M]*/ int [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ IntUnaryOperator generator) { }

        //@NotModified[T]
        static void parallelSetAll(
            /*@Independent[M]*/ int [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ IntUnaryOperator generator) { }

        //@NotModified[T]
        static void setAll(
            /*@Independent[M]*/ long [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ IntToLongFunction generator) { }

        //@NotModified[T]
        static void parallelSetAll(
            /*@Independent[M]*/ long [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ IntToLongFunction generator) { }

        //@NotModified[T]
        static void setAll(
            /*@Independent[M]*/ double [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ IntToDoubleFunction generator) { }

        //@NotModified[T]
        static void parallelSetAll(
            /*@Independent[M]*/ double [] array,
            /*@IgnoreModifications[T] @Independent[M]*/ IntToDoubleFunction generator) { }

        //@Independent[O] @NotModified[T]
        static <T> Spliterator<T> spliterator(/*@Independent[M]*/ T [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> Spliterator<T> spliterator(/*@Independent[M]*/ T [] array, int startInclusive, int endExclusive) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static Spliterator.OfInt spliterator(/*@Independent[M]*/ int [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static Spliterator.OfInt spliterator(/*@Independent[M]*/ int [] array, int startInclusive, int endExclusive) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static Spliterator.OfLong spliterator(/*@Independent[M]*/ long [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static Spliterator.OfLong spliterator(/*@Independent[M]*/ long [] array, int startInclusive, int endExclusive) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static Spliterator.OfDouble spliterator(/*@Independent[M]*/ double [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static Spliterator.OfDouble spliterator(
            /*@Independent[M]*/ double [] array,
            int startInclusive,
            int endExclusive) { return null; }

        //frequency 15
        //@Independent[O]
        @NotModified
        static <T> Stream<T> stream(@Independent(hc = true) @NotModified T [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> Stream<T> stream(/*@Independent[M]*/ T [] array, int startInclusive, int endExclusive) { return null; }

        //@Independent[O] @NotModified[T]
        static IntStream stream(/*@Independent[M]*/ int [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static IntStream stream(/*@Independent[M]*/ int [] array, int startInclusive, int endExclusive) { return null; }

        //frequency 1
        //@Independent[O]
        @NotModified
        static LongStream stream(@Independent @NotModified long [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static LongStream stream(/*@Independent[M]*/ long [] array, int startInclusive, int endExclusive) { return null; }

        //@Independent[O] @NotModified[T]
        static DoubleStream stream(/*@Independent[M]*/ double [] array) { return null; }

        //@Independent[O] @NotModified[T]
        static DoubleStream stream(/*@Independent[M]*/ double [] array, int startInclusive, int endExclusive) {
            return null;
        }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ boolean [] a, /*@Independent[M]*/ boolean [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ boolean [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ boolean [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ byte [] a, /*@Independent[M]*/ byte [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ byte [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ byte [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(/*@Independent[M]*/ byte [] a, /*@Independent[M]*/ byte [] b) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(
            /*@Independent[M]*/ byte [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ byte [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ short [] a, /*@Independent[M]*/ short [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ short [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ short [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(/*@Independent[M]*/ short [] a, /*@Independent[M]*/ short [] b) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(
            /*@Independent[M]*/ short [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ short [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ char [] a, /*@Independent[M]*/ char [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ char [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ char [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ int [] a, /*@Independent[M]*/ int [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ int [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ int [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(/*@Independent[M]*/ int [] a, /*@Independent[M]*/ int [] b) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(
            /*@Independent[M]*/ int [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ int [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ long [] a, /*@Independent[M]*/ long [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ long [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ long [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(/*@Independent[M]*/ long [] a, /*@Independent[M]*/ long [] b) { return 0; }

        //@NotModified[T]
        static int compareUnsigned(
            /*@Independent[M]*/ long [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ long [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ float [] a, /*@Independent[M]*/ float [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ float [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ float [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int compare(/*@Independent[M]*/ double [] a, /*@Independent[M]*/ double [] b) { return 0; }

        //@NotModified[T]
        static int compare(
            /*@Independent[M]*/ double [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ double [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static <T extends Comparable<? super T>> int compare(/*@Independent[M]*/ T [] a, /*@Independent[M]*/ T [] b) {
            return 0;
        }

        //@NotModified[T]
        static <T extends Comparable<? super T>> int compare(
            /*@Independent[M]*/ T [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ T [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static <T> int compare(
            /*@Independent[M]*/ T [] a,
            /*@Independent[M]*/ T [] b,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> cmp) { return 0; }

        //@NotModified[T]
        static <T> int compare(
            /*@Independent[M]*/ T [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ T [] b,
            int bFromIndex,
            int bToIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> cmp) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ boolean [] a, /*@Independent[M]*/ boolean [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ boolean [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ boolean [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ byte [] a, /*@Independent[M]*/ byte [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ byte [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ byte [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ char [] a, /*@Independent[M]*/ char [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ char [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ char [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ short [] a, /*@Independent[M]*/ short [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ short [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ short [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ int [] a, /*@Independent[M]*/ int [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ int [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ int [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ long [] a, /*@Independent[M]*/ long [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ long [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ long [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ float [] a, /*@Independent[M]*/ float [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ float [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ float [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ double [] a, /*@Independent[M]*/ double [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ double [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ double [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static int mismatch(/*@Independent[M]*/ Object [] a, /*@Independent[M]*/ Object [] b) { return 0; }

        //@NotModified[T]
        static int mismatch(
            /*@Independent[M]*/ Object [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ Object [] b,
            int bFromIndex,
            int bToIndex) { return 0; }

        //@NotModified[T]
        static <T> int mismatch(
            /*@Independent[M]*/ T [] a,
            /*@Independent[M]*/ T [] b,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> cmp) { return 0; }

        //@NotModified[T]
        static <T> int mismatch(
            /*@Independent[M]*/ T [] a,
            int aFromIndex,
            int aToIndex,
            /*@Independent[M]*/ T [] b,
            int bFromIndex,
            int bToIndex,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> cmp) { return 0; }
    }

    //public class Base64
    @UtilityClass
    class Base64$ {
        //public static class Decoder
        @UtilityClass
        class Decoder {
            //@Independent[O] @NotModified[T]
            byte [] decode(/*@Independent[M]*/ byte [] src) { return null; }

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
            byte [] encode(/*@Independent[M]*/ byte [] src) { return null; }

            //@NotModified[T]
            int encode(/*@Independent[M]*/ byte [] src, /*@Independent[M]*/ byte [] dst) { return 0; }

            //frequency 2
            //@NotModified[T]
            String encodeToString(/*@Independent[M]*/ byte [] src) { return null; }

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
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object object) { return false; }

        //override from java.lang.Iterable
        //override has frequency 2
        //@NotNull[H]
        @NotModified
        Iterator<E> iterator() { return null; }

        //override has frequency 3
        @NotModified
        Object [] toArray() { return null; }

        //override has frequency 3
        @NotModified
        <T> T [] toArray(/*@Independent[M] @NotModified[O]*/ T [] t) { return null; }

        @NotModified
        <T> T [] toArray(/*@IgnoreModifications[T]*/ @Independent(hc = true) IntFunction<T []> generator) { return null; }

        //frequency 2
        boolean add(/*@Independent(hc=true)[T] @NotModified[O]*/ E e) { return false; }

        //override has frequency 3
        boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Object object) {
            return false;
        }

        @NotModified
        boolean containsAll(/*@NotModified[O]*/ @Independent(hc = true) Collection<?> collection) { return false; }

        //override has frequency 5
        boolean addAll(/*@NotModified[O]*/ @Independent(hc = true) Collection<? extends E> collection) { return false; }
        boolean removeAll(/*@NotModified[O]*/ @Independent(hc = true) Collection<?> collection) { return false; }
        //frequency 3
        boolean removeIf(/*@IgnoreModifications[T]*/ @Independent(hc = true) Predicate<? super E> filter) {
            return false;
        }

        //override has frequency 1
        boolean retainAll(/*@NotModified[O]*/ @Independent(hc = true) Collection<?> collection) { return false; }
        void clear() { }
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
        static <T extends Comparable<? super T>> void sort(/*@Independent[M]*/ List<T> list) { }

        //@NotModified[T]
        static <T> void sort(
            /*@Independent[M]*/ List<T> list,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> c) { }

        //@NotModified[T]
        static <T> int binarySearch(
            /*@Independent[M]*/ List<? extends Comparable<? super T>> list,
            /*@Independent[M] @NotModified[T]*/ T key) { return 0; }

        //@NotModified[T]
        static <T> int binarySearch(
            /*@Independent[M]*/ List<? extends T> list,
            /*@Independent[M] @NotModified[T]*/ T key,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> c) { return 0; }

        //frequency 1
        //@NotModified[T]
        static void reverse(/*@Independent[M]*/ List<?> list) { }

        //frequency 2
        //@NotModified[T]
        static void shuffle(/*@Independent[M]*/ List<?> list) { }

        //@NotModified[T]
        static void shuffle(/*@Independent[M]*/ List<?> list, /*@Independent[M]*/ Random rnd) { }

        //@NotModified[T]
        static void shuffle(/*@Independent[M]*/ List<?> list, /*@Independent[M]*/ RandomGenerator rnd) { }

        //@NotModified[T]
        static void swap(/*@Independent[M]*/ List<?> list, int i, int j) { }

        //@NotModified[T]
        static <T> void fill(
            @Independent(hcParameters = { 1 }) List<? super T> list,
            /*@Independent[M] @NotModified[T]*/ T obj) { }

        //@NotModified[T]
        static <T> void copy(
            @Independent(hcParameters = { 1 }) List<? super T> dest,
            /*@Independent[M]*/ @NotModified List<? extends T> src) { }

        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static <T extends Comparable<? super T>> T min(/*@Independent[M]*/ Collection<? extends T> coll) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> T min(
            /*@Independent[M]*/ Collection<? extends T> coll,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> comp) { return null; }

        //@Immutable(hc=true)[T] @Independent[O] @NotModified[T]
        static <T extends Comparable<? super T>> T max(/*@Independent[M]*/ Collection<? extends T> coll) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> T max(
            /*@Independent[M]*/ Collection<? extends T> coll,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<? super T> comp) { return null; }

        //@NotModified[T]
        static void rotate(/*@Independent[M]*/ List<?> list, int distance) { }

        //@NotModified[T]
        static <T> boolean replaceAll(
            @Independent(hcParameters = { 2 }) List<T> list,
            /*@Independent[M] @NotModified[T]*/ T oldVal,
            /*@Independent[M] @NotModified[T]*/ T newVal) { return false; }

        //@NotModified[T]
        static int indexOfSubList(/*@Independent[M]*/ List<?> source, /*@Independent[M]*/ List<?> target) { return 0; }

        //@NotModified[T]
        static int lastIndexOfSubList(/*@Independent[M]*/ List<?> source, /*@Independent[M]*/ List<?> target) { return 0; }

        //@Independent[O] @NotModified[T]
        static <T> Collection<T> unmodifiableCollection(/*@Independent[M]*/ Collection<? extends T> c) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> SequencedCollection<T> unmodifiableSequencedCollection(
            /*@Independent[M]*/ SequencedCollection<? extends T> c) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> Set<T> unmodifiableSet(/*@Independent[M]*/ Set<? extends T> s) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> SequencedSet<T> unmodifiableSequencedSet(/*@Independent[M]*/ SequencedSet<? extends T> s) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static <T> SortedSet<T> unmodifiableSortedSet(/*@Independent[M]*/ SortedSet<T> s) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> NavigableSet<T> unmodifiableNavigableSet(/*@Independent[M]*/ NavigableSet<T> s) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> List<T> unmodifiableList(/*@Independent[M]*/ List<? extends T> list) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> Map<K, V> unmodifiableMap(/*@Independent[M]*/ Map<? extends K, ? extends V> m) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> SequencedMap<K, V> unmodifiableSequencedMap(
            /*@Independent[M]*/ SequencedMap<? extends K, ? extends V> m) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> SortedMap<K, V> unmodifiableSortedMap(/*@Independent[M]*/ SortedMap<K, ? extends V> m) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(/*@Independent[M]*/ NavigableMap<K, ? extends V> m) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static <T> Collection<T> synchronizedCollection(/*@Independent[M]*/ Collection<T> c) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> Set<T> synchronizedSet(/*@Independent[M]*/ Set<T> s) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> SortedSet<T> synchronizedSortedSet(/*@Independent[M]*/ SortedSet<T> s) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> NavigableSet<T> synchronizedNavigableSet(/*@Independent[M]*/ NavigableSet<T> s) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> List<T> synchronizedList(/*@Independent[M]*/ List<T> list) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> Map<K, V> synchronizedMap(/*@Independent[M]*/ Map<K, V> m) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> SortedMap<K, V> synchronizedSortedMap(/*@Independent[M]*/ SortedMap<K, V> m) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> NavigableMap<K, V> synchronizedNavigableMap(/*@Independent[M]*/ NavigableMap<K, V> m) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static <E> Collection<E> checkedCollection(/*@Independent[M]*/ Collection<E> c, Class<E> type) { return null; }

        //@Independent[O] @NotModified[T]
        static <E> Queue<E> checkedQueue(/*@Independent[M]*/ Queue<E> queue, Class<E> type) { return null; }

        //@Independent[O] @NotModified[T]
        static <E> Set<E> checkedSet(/*@Independent[M]*/ Set<E> s, Class<E> type) { return null; }

        //@Independent[O] @NotModified[T]
        static <E> SortedSet<E> checkedSortedSet(/*@Independent[M]*/ SortedSet<E> s, Class<E> type) { return null; }

        //@Independent[O] @NotModified[T]
        static <E> NavigableSet<E> checkedNavigableSet(/*@Independent[M]*/ NavigableSet<E> s, Class<E> type) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static <E> List<E> checkedList(/*@Independent[M]*/ List<E> list, Class<E> type) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> Map<K, V> checkedMap(/*@Independent[M]*/ Map<K, V> m, Class<K> keyType, Class<V> valueType) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static <K, V> SortedMap<K, V> checkedSortedMap(
            /*@Independent[M]*/ SortedMap<K, V> m,
            Class<K> keyType,
            Class<V> valueType) { return null; }

        //@Independent[O] @NotModified[T]
        static <K, V> NavigableMap<K, V> checkedNavigableMap(
            /*@Independent[M]*/ NavigableMap<K, V> m,
            Class<K> keyType,
            Class<V> valueType) { return null; }

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
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Comparator<T> cmp) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> Enumeration<T> enumeration(/*@Independent[M]*/ Collection<T> c) { return null; }

        //@Independent[O] @NotModified[T]
        static <T> ArrayList<T> list(/*@Independent[M]*/ Enumeration<T> e) { return null; }

        //@NotModified[T]
        static int frequency(
            /*@Independent[M]*/ Collection<?> c,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object o) { return 0; }

        //@NotModified[T]
        static boolean disjoint(/*@Independent[M]*/ Collection<?> c1, /*@Independent[M]*/ Collection<?> c2) {
            return false;
        }

        //@NotModified[T]
        static <T> boolean addAll(
            @Independent(hcParameters = { 1 }) @NotNull Collection<? super T> c,
            /*@Independent[M]*/ @NotModified T ... elements) { return false; }

        //@Independent[O] @NotModified[T]
        static <E> Set<E> newSetFromMap(/*@Independent[M]*/ Map<E, Boolean> map) { return null; }

        //@Independent[O] @NotModified[T]
        static <E> SequencedSet<E> newSequencedSetFromMap(/*@Independent[M]*/ SequencedMap<E, Boolean> map) {
            return null;
        }

        //@Independent[O] @NotModified[T]
        static <T> Queue<T> asLifoQueue(/*@Independent[M]*/ Deque<T> deque) { return null; }
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
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Comparator<? super T> other) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        <U> Comparator<T> thenComparing(
            /*@IgnoreModifications[T] @Independent[M]*/ Function<? super T, ? extends U> keyExtractor,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Comparator<? super U> keyComparator) {
            return null;
        }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        <U extends Comparable<? super U>> Comparator<T> thenComparing(
            /*@IgnoreModifications[T] @Independent[M]*/ Function<? super T, ? extends U> keyExtractor) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Comparator<T> thenComparingInt(
            /*@IgnoreModifications[T] @Independent[M]*/ ToIntFunction<? super T> keyExtractor) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Comparator<T> thenComparingLong(
            /*@IgnoreModifications[T] @Independent[M]*/ ToLongFunction<? super T> keyExtractor) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Comparator<T> thenComparingDouble(
            /*@IgnoreModifications[T] @Independent[M]*/ ToDoubleFunction<? super T> keyExtractor) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T extends Comparable<? super T>> Comparator<T> reverseOrder() { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T extends Comparable<? super T>> Comparator<T> naturalOrder() { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T> Comparator<T> nullsFirst(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Comparator<? super T> comparator) {
            return null;
        }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T> Comparator<T> nullsLast(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Comparator<? super T> comparator) {
            return null;
        }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T, U> Comparator<T> comparing(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ Function<? super T, ? extends U> keyExtractor,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Comparator<? super U> keyComparator) {
            return null;
        }

        //frequency 11
        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]

        static <T, U extends Comparable<? super U>> Comparator<T> comparing(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ Function<? super T, ? extends U> keyExtractor) {
            return null;
        }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T> Comparator<T> comparingInt(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ ToIntFunction<? super T> keyExtractor) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T> Comparator<T> comparingLong(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ ToLongFunction<? super T> keyExtractor) { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        static <T> Comparator<T> comparingDouble(
            /*@IgnoreModifications[T] @Independent(hc=true)[T]*/ ToDoubleFunction<? super T> keyExtractor) {
            return null;
        }
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
        Date$(String s) { }
        //override from java.lang.Object
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }
        static long UTC(int year, int month, int date, int hrs, int min, int sec) { return 0L; }
        static long parse(String s) { return 0L; }
        int getYear() { return 0; }
        void setYear(int year) { }
        int getMonth() { return 0; }
        void setMonth(int month) { }
        int getDate() { return 0; }
        void setDate(int date) { }
        int getDay() { return 0; }
        int getHours() { return 0; }
        void setHours(int hours) { }
        int getMinutes() { return 0; }
        void setMinutes(int minutes) { }
        int getSeconds() { return 0; }
        void setSeconds(int seconds) { }
        //frequency 1
        @NotModified
        long getTime() { return 0L; }
        void setTime(long time) { }
        boolean before(Date when) { return false; }
        boolean after(Date when) { return false; }
        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ Date anotherDate) { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        String toLocaleString() { return null; }
        String toGMTString() { return null; }
        int getTimezoneOffset() { return 0; }
        static Date from(Instant instant) { return null; }
        Instant toInstant() { return null; }
    }

    //public interface EventListener
    //@Container[M]
    @Independent(hc = true) class EventListener$ { }

    //public class HashMap extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable
    @Container
    class HashMap$<K, V> {
        HashMap$(int initialCapacity, float loadFactor) { }
        HashMap$(int initialCapacity) { }
        //frequency 15
        HashMap$() { }
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
        V get(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object key) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        //frequency 2
        //@NotModified[H]
        boolean containsKey(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object key) { return false; }

        //override from java.util.AbstractMap, java.util.Map
        //frequency 4
        //@Independent(hc=true)[H]

        V put(/*@Independent(hc=true)[H] @NotModified[O]*/ K key, /*@Independent(hc=true)[H] @NotModified[O]*/ V value) {
            return null;
        }

        //override from java.util.AbstractMap, java.util.Map
        void putAll(/*@NotModified[O]*/ Map<? extends K, ? extends V> m) { }

        //override from java.util.AbstractMap, java.util.Map
        //@Independent(hc=true)[H]
        V remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object key) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        void clear() { }

        //override from java.util.AbstractMap, java.util.Map
        boolean containsValue(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object value) {
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
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object key,
            /*@Independent[M] @NotModified[O]*/ V defaultValue) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V putIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V value) { return null; }

        //override from java.util.Map
        boolean remove(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object key,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object value) { return false; }

        //override from java.util.Map
        boolean replace(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V oldValue,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V newValue) { return false; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V replace(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V value) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V computeIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ Function<? super K, ? extends V> mappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V computeIfPresent(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V compute(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V merge(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V value,
            /*@IgnoreModifications[T] @Independent(hc=true)[H]*/ BiFunction<? super V, ? super V, ? extends V>
                remappingFunction) { return null; }

        //override from java.util.Map
        //@NotModified[H]
        void forEach(/*@IgnoreModifications[T] @Independent[M]*/ BiConsumer<? super K, ? super V> action) { }

        //override from java.util.Map
        void replaceAll(/*@IgnoreModifications[T]*/ BiFunction<? super K, ? super V, ? extends V> function) { }

        //override from java.lang.Object, java.util.AbstractMap
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }
        static <K, V> HashMap<K, V> newHashMap(int numMappings) { return null; }
    }

    //public class HashSet extends AbstractSet<E> implements Set<E>, Cloneable, Serializable
    @Container
    class HashSet$<E> {
        //frequency 41
        HashSet$() { }

        //frequency 25
        HashSet$(/*@NotModified[O]*/ @Independent(hc = true) @NotNull Collection<? extends E> c) { }
        HashSet$(int initialCapacity, float loadFactor) { }
        HashSet$(int initialCapacity) { }
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
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object o) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        boolean add(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object o) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        void clear() { }

        //override from java.lang.Object
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }

        //override from java.lang.Iterable, java.util.Collection, java.util.Set
        //@Independent(hc=true)[H] @NotModified[H] @NotNull[H]
        Spliterator<E> spliterator() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        //@NotModified[H]
        Object [] toArray() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[O]*/ T [] a) { return null; }
        static <T> HashSet<T> newHashSet(int numElements) { return null; }
    }

    //public interface Iterator
    @Container
    @Independent(hc = true)
    class Iterator$<E> {
        boolean hasNext() { return false; }
        //frequency 2
        @Independent(hc = true)
        E next() { return null; }
        void remove() { }
        void forEachRemaining(/*@IgnoreModifications[T]*/ @Independent(hc = true) @NotNull Consumer<? super E> action) { }
    }

    //public class LinkedList extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, Serializable
    class LinkedList$<E> {
        LinkedList$() { }
        LinkedList$(@Independent(hc = true) @NotModified Collection<? extends E> c) { }
        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H] @NotModified[H]
        E getFirst() { return null; }

        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H] @NotModified[H]
        E getLast() { return null; }

        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H]
        E removeFirst() { return null; }

        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        //@Independent(hc=true)[H]
        E removeLast() { return null; }

        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        void addFirst(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { }

        //override from java.util.Deque, java.util.List, java.util.SequencedCollection
        void addLast(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Deque, java.util.List
        //@NotModified[H]
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object o) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Deque, java.util.List
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.Deque, java.util.List, java.util.Queue
        boolean add(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Deque, java.util.List
        boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object o) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.Deque, java.util.List
        boolean addAll(/*@Independent(hc=true)[H] @NotModified[H]*/ Collection<? extends E> c) { return false; }

        //override from java.util.AbstractList, java.util.AbstractSequentialList, java.util.List
        boolean addAll(int index, /*@NotModified[H]*/ Collection<? extends E> c) { return false; }

        //override from java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.List
        void clear() { }

        //override from java.util.AbstractList, java.util.AbstractSequentialList, java.util.List
        //@Independent(hc=true)[H] @NotModified[H] @GetSet("_synthetic_list")[H]
        E get(int index) { return null; }

        //override from java.util.AbstractList, java.util.AbstractSequentialList, java.util.List
        //@Independent(hc=true)[H] @GetSet("_synthetic_list")[H]
        E set(int index, /*@Independent(hc=true)[H] @NotModified[T]*/ E element) { return null; }

        //override from java.util.AbstractList, java.util.AbstractSequentialList, java.util.List
        void add(int index, /*@Independent(hc=true)[H] @NotModified[T]*/ E element) { }

        //override from java.util.AbstractList, java.util.AbstractSequentialList, java.util.List
        //@Independent(hc=true)[H]
        E remove(int index) { return null; }

        //override from java.util.AbstractList, java.util.List
        int indexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object o) { return 0; }

        //override from java.util.AbstractList, java.util.List
        int lastIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object o) { return 0; }

        //override from java.util.Deque, java.util.Queue
        //@Independent(hc=true)[H]
        E peek() { return null; }

        //override from java.util.Deque, java.util.Queue
        //@Independent(hc=true)[H]
        E element() { return null; }

        //override from java.util.Deque, java.util.Queue
        //@Independent(hc=true)[H]
        E poll() { return null; }

        //override from java.util.Deque, java.util.Queue
        //@Independent(hc=true)[H]
        E remove() { return null; }

        //override from java.util.Deque, java.util.Queue
        boolean offer(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { return false; }

        //override from java.util.Deque
        boolean offerFirst(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { return false; }

        //override from java.util.Deque
        boolean offerLast(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { return false; }

        //override from java.util.Deque
        //@Independent(hc=true)[H]
        E peekFirst() { return null; }

        //override from java.util.Deque
        //@Independent(hc=true)[H]
        E peekLast() { return null; }

        //override from java.util.Deque
        //@Independent(hc=true)[H]
        E pollFirst() { return null; }

        //override from java.util.Deque
        //@Independent(hc=true)[H]
        E pollLast() { return null; }

        //override from java.util.Deque
        void push(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { }

        //override from java.util.Deque
        //@Independent(hc=true)[H]
        E pop() { return null; }

        //override from java.util.Deque
        boolean removeFirstOccurrence(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object o) {
            return false;
        }

        //override from java.util.Deque
        boolean removeLastOccurrence(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object o) {
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
        Object [] toArray() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[H]*/ T [] a) { return null; }

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
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object object) { return false; }

        //override from java.lang.Iterable, java.util.Collection
        //@NotModified[H] @NotNull[H]
        Iterator<E> iterator() { return null; }

        //override from java.util.Collection
        //@NotModified[H]
        Object [] toArray() { return null; }

        //override from java.util.Collection
        //frequency 3
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[O]*/ T [] t) { return null; }

        //override from java.util.Collection
        //frequency 52
        boolean add(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { return false; }

        //override from java.util.Collection
        boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object object) {
            return false;
        }

        //override from java.util.Collection
        //@NotModified[H]
        boolean containsAll(/*@Independent[M] @NotModified[O]*/ Collection<?> collection) { return false; }

        //override from java.util.Collection
        //frequency 8
        boolean addAll(/*@Independent(hc=true)[H] @NotModified[O]*/ Collection<? extends E> collection) { return false; }
        boolean addAll(int i, /*@NotModified[O]*/ Collection<? extends E> collection) { return false; }
        //override from java.util.Collection
        boolean removeAll(/*@Independent(hc=true)[H] @NotModified[O]*/ Collection<?> collection) { return false; }

        //override from java.util.Collection
        boolean retainAll(/*@Independent(hc=true)[H] @NotModified[O]*/ Collection<?> collection) { return false; }
        void replaceAll(/*@IgnoreModifications[T]*/ UnaryOperator<E> operator) { }
        //frequency 4
        void sort(/*@Immutable(hc=true)[T] @NotModified[O]*/ @Independent(hc = true) Comparator<? super E> c) { }

        //override from java.util.Collection
        void clear() { }

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
        @GetSet("_synthetic_list")
        E set(int i, /*@Independent(hc=true)[T] @NotModified[O]*/ E e) { return null; }
        void add(int i, /*@Independent(hc=true)[T] @NotModified[O]*/ E e) { }
        //@Independent(hc=true)[T]
        E remove(int i) { return null; }
        int indexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Object object) { return 0; }
        int lastIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Object object) { return 0; }
        ListIterator<E> listIterator() { return null; }
        ListIterator<E> listIterator(int i) { return null; }
        //frequency 3
        @NotModified
        List<E> subList(int i, int i1) { return null; }

        //override from java.lang.Iterable, java.util.Collection
        //@Independent(hc=true)[H] @NotModified[H] @NotNull[H]
        Spliterator<E> spliterator() { return null; }

        //override from java.util.SequencedCollection
        void addFirst(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { }

        //override from java.util.SequencedCollection
        void addLast(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { }

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
        E removeFirst() { return null; }

        //override from java.util.SequencedCollection
        //@Independent(hc=true)[H]
        E removeLast() { return null; }

        //override from java.util.SequencedCollection
        List<E> reversed() { return null; }

        //frequency 10
        @ImmutableContainer @NotNull(content = true) @NotModified
        static <E> List<E> of() { return null; }

        //frequency 25
        @ImmutableContainer(hc = true) @NotNull(content = true) @NotModified
        static <E> List<E> of(/*@Independent(hc=true)[T] @NotModified[O]*/ @Independent(hcReturnValue = true) @NotNull E e1) { return null; }

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
        static <E> List<E> of(/*@NotModified[O]*/ E ... elements) { return null; }

        //frequency 6
        @ImmutableContainer(hc = true) @NotNull @NotModified
        static <E> List<E> copyOf(/*@NotModified[O]*/ @Independent(hc = true) Collection<? extends E> coll) {
            return null;
        }
    }

    //public final class Locale implements Cloneable, Serializable
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
            Locale.Builder setLocale(Locale locale) { return null; }
            Locale.Builder setLanguageTag(String languageTag) { return null; }
            Locale.Builder setLanguage(String language) { return null; }
            Locale.Builder setScript(String script) { return null; }
            Locale.Builder setRegion(String region) { return null; }
            Locale.Builder setVariant(String variant) { return null; }
            Locale.Builder setExtension(char key, String value) { return null; }
            Locale.Builder setUnicodeLocaleKeyword(String key, String type) { return null; }
            Locale.Builder addUnicodeLocaleAttribute(String attribute) { return null; }
            Locale.Builder removeUnicodeLocaleAttribute(String attribute) { return null; }
            Locale.Builder clear() { return null; }
            Locale.Builder clearExtensions() { return null; }
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
            LanguageRange(String range) { }
            LanguageRange(String range, double weight) { }
            String getRange() { return null; }
            double getWeight() { return 0.0; }
            //@Independent[T]
            static List<Locale.LanguageRange> parse(String ranges) { return null; }

            //@Independent[T]
            static List<Locale.LanguageRange> parse(String ranges, Map<String, List<String>> map) { return null; }

            //@Independent[T]
            static List<Locale.LanguageRange> mapEquivalents(
                List<Locale.LanguageRange> priorityList,
                Map<String, List<String>> map) { return null; }

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
        Locale$(String language, String country, String variant) { }
        Locale$(String language, String country) { }
        Locale$(String language) { }
        static Locale of(String language, String country, String variant) { return null; }
        static Locale of(String language, String country) { return null; }
        static Locale of(String language) { return null; }
        static Locale getDefault() { return null; }
        static Locale getDefault(Locale.Category category) { return null; }
        static void setDefault(Locale newLocale) { }
        static void setDefault(Locale.Category category, Locale newLocale) { }
        static Locale [] getAvailableLocales() { return null; }
        //@Independent[T]
        static Stream<Locale> availableLocales() { return null; }

        //@Independent[T]
        static String [] getISOCountries() { return null; }

        //@Independent[T]
        static Set<String> getISOCountries(Locale.IsoCountryCode type) { return null; }

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
        String getExtension(char key) { return null; }
        Set<Character> getExtensionKeys() { return null; }
        Set<String> getUnicodeLocaleAttributes() { return null; }
        String getUnicodeLocaleType(String key) { return null; }
        Set<String> getUnicodeLocaleKeys() { return null; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        String toLanguageTag() { return null; }
        static String caseFoldLanguageTag(String languageTag) { return null; }
        //frequency 1
        @Independent @NotModified
        static Locale forLanguageTag(String languageTag) { return null; }
        String getISO3Language() { return null; }
        String getISO3Country() { return null; }
        String getDisplayLanguage() { return null; }
        String getDisplayLanguage(Locale inLocale) { return null; }
        String getDisplayScript() { return null; }
        String getDisplayScript(Locale inLocale) { return null; }
        String getDisplayCountry() { return null; }
        String getDisplayCountry(Locale inLocale) { return null; }
        String getDisplayVariant() { return null; }
        String getDisplayVariant(Locale inLocale) { return null; }
        String getDisplayName() { return null; }
        String getDisplayName(Locale inLocale) { return null; }
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
            List<Locale.LanguageRange> priorityList,
            Collection<Locale> locales,
            Locale.FilteringMode mode) { return null; }

        //@Independent[T]
        static List<Locale> filter(List<Locale.LanguageRange> priorityList, Collection<Locale> locales) { return null; }

        //@Independent[T]
        static List<String> filterTags(
            List<Locale.LanguageRange> priorityList,
            Collection<String> tags,
            Locale.FilteringMode mode) { return null; }

        //@Independent[T]
        static List<String> filterTags(List<Locale.LanguageRange> priorityList, Collection<String> tags) { return null; }
        static Locale lookup(List<Locale.LanguageRange> priorityList, Collection<Locale> locales) { return null; }
        static String lookupTag(List<Locale.LanguageRange> priorityList, Collection<String> tags) { return null; }
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
            V setValue(/*@Independent(hc=true)[T] @NotModified[O]*/ V v) { return null; }

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
                /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Comparator<? super K> cmp) {
                return null;
            }

            //@Immutable(hc=true)[T] @Independent[T]
            static <K, V> Comparator<Map.Entry<K, V>> comparingByValue(
                /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Comparator<? super V> cmp) {
                return null;
            }

            //@Independent(hc=true)[O]
            static <K, V> Map.Entry<K, V> copyOf(
                /*@Independent(hc=true)[T] @NotModified[O]*/ Map.Entry<? extends K, ? extends V> e) { return null; }
        }
        @NotModified int size() { return 0; }
        @NotModified boolean isEmpty() { return false; }
        //frequency 5
        @NotModified
        boolean containsKey(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object object) { return false; }

        boolean containsValue(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Object object) {
            return false;
        }

        //frequency 57
        @Independent(hc = true) @NotModified
        V get(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object object) { return null; }

        //frequency 17
        //@Independent(hc=true)[T]

        V put(/*@Independent(hc=true)[T] @NotModified[O]*/ K k, /*@Independent(hc=true)[T] @NotModified[O]*/ V v) {
            return null;
        }

        //@Independent(hc=true)[T]
        V remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Object object) { return null; }
        void putAll(/*@NotModified[O]*/ Map<? extends K, ? extends V> map) { }
        void clear() { }
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
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object key,
            /*@Independent[M] @NotModified[O]*/ V defaultValue) { return null; }

        //frequency 1
        @NotModified
        void forEach(/*@IgnoreModifications[T]*/ @Independent(hc = true) BiConsumer<? super K, ? super V> action) { }
        void replaceAll(/*@IgnoreModifications[T]*/ BiFunction<? super K, ? super V, ? extends V> function) { }
        //frequency 1
        //@Independent(hc=true)[T]

        V putIfAbsent(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V value) { return null; }

        boolean remove(
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Object key,
            /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Object value) { return false; }

        boolean replace(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V oldValue,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V newValue) { return false; }

        //@Independent(hc=true)[T]
        V replace(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V value) { return null; }

        //@Independent(hc=true)[T]
        V computeIfAbsent(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ Function<? super K, ? extends V> mappingFunction) { return null; }

        //@Independent(hc=true)[T]
        V computeIfPresent(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //@Independent(hc=true)[T]
        V compute(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //frequency 2
        //@Independent(hc=true)[T]

        V merge(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[T] @NotModified[O]*/ V value,
            /*@IgnoreModifications[T]*/ @Independent(hc = true) @NotModified BiFunction<
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

        static <K, V> Map<K, V> ofEntries(/*@NotModified[O]*/ Map.Entry<? extends K, ? extends V> ... entries) {
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
    }

    //public interface NavigableMap implements SortedMap<K,V>
    @Container
    class NavigableMap$<K, V> {
        @NotModified Map.Entry<K, V> lowerEntry(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        //@Independent(hc=true)[T]
        @NotModified
        K lowerKey(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        @NotModified Map.Entry<K, V> floorEntry(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        //@Independent(hc=true)[T]
        @NotModified
        K floorKey(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        @NotModified Map.Entry<K, V> ceilingEntry(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        //@Independent(hc=true)[T]
        @NotModified
        K ceilingKey(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        @NotModified Map.Entry<K, V> higherEntry(/*@Independent[M] @NotModified[O]*/ K k) { return null; }
        //@Independent(hc=true)[T]
        @NotModified
        K higherKey(/*@Independent[M] @NotModified[O]*/ K k) { return null; }

        //override from java.util.SequencedMap
        @NotModified
        Map.Entry<K, V> firstEntry() { return null; }

        //override from java.util.SequencedMap
        @NotModified
        Map.Entry<K, V> lastEntry() { return null; }

        //override from java.util.SequencedMap
        Map.Entry<K, V> pollFirstEntry() { return null; }

        //override from java.util.SequencedMap
        Map.Entry<K, V> pollLastEntry() { return null; }
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

    //public class NoSuchElementException extends RuntimeException
    class NoSuchElementException$ {
        NoSuchElementException$() { }
        NoSuchElementException$(String s, Throwable cause) { }
        NoSuchElementException$(Throwable cause) { }
        //frequency 12
        NoSuchElementException$(String s) { }
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
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object a,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object b) { return false; }

        //frequency 26
        //@NotModified[T]
        static int hashCode(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object o) { return 0; }

        //frequency 20
        //@NotModified[T]
        static int hash(/*@Independent[M] @NotModified[O]*/ Object ... values) { return 0; }

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

        static String toIdentityString(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object o) {
            return null;
        }

        //@NotModified[T]
        static <T> int compare(
            /*@Independent[M] @NotModified[O]*/ T a,
            /*@Independent[M] @NotModified[O]*/ T b,
            /*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Comparator<? super T> c) { return 0; }

        //frequency 9
        //@Independent[O] @NotModified[T]
        @Identity @NotNull
        static <T> T requireNonNull(/*@Independent[M] @NotModified[O]*/ @NotNull T obj) { return null; }

        //frequency 2
        //@Independent[O] @NotModified[T]
        @Identity @NotNull
        static <T> T requireNonNull(/*@Independent[M] @NotModified[O]*/ @NotNull T obj, String message) { return null; }

        //@NotModified[T]
        static boolean isNull(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

        //frequency 1
        //@NotModified[T]
        static boolean nonNull(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object obj) { return false; }

        //frequency 1
        //@Independent[O] @NotModified[T]
        @Identity @NotNull

        static <T> T requireNonNullElse(
            /*@Independent[M] @NotModified[O]*/ T obj,
            /*@Independent[M] @NotModified[O]*/ @NotNull T defaultObj) { return null; }

        //@Independent[O] @NotModified[T]
        @Identity @NotNull

        static <T> T requireNonNullElseGet(
            /*@Independent[M] @NotModified[O]*/ T obj,
            /*@IgnoreModifications[T] @Independent[M]*/ Supplier<? extends T> supplier) { return null; }

        //@Independent[O] @NotModified[T]
        @Identity @NotNull

        static <T> T requireNonNull(
            /*@Independent[M] @NotModified[O]*/ T obj,
            /*@IgnoreModifications[T] @Independent[M]*/ Supplier<String> messageSupplier) { return null; }

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
        static <T> Optional<T> ofNullable(/*@Independent(hc=true)[T] @NotModified[O]*/ T value) { return null; }

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
        void ifPresent(/*@IgnoreModifications[T] @Independent[M]*/ Consumer<? super T> action) { }

        //frequency 39
        //@NotModified[T]

        void ifPresentOrElse(
            /*@IgnoreModifications[T] @Independent[M]*/ Consumer<? super T> action,
            /*@IgnoreModifications[T] @Independent[M]*/ Runnable emptyAction) { }

        //frequency 20
        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Optional<T> filter(/*@IgnoreModifications[T] @Independent[M]*/ Predicate<? super T> predicate) { return null; }

        //frequency 211
        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]

        <U> Optional<U> map(/*@IgnoreModifications[T] @Independent[M]*/ Function<? super T, ? extends U> mapper) {
            return null;
        }

        //frequency 18
        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]

        <U> Optional<U> flatMap(
            /*@IgnoreModifications[T] @Independent[M]*/ Function<? super T, ? extends Optional<? extends U>> mapper) {
            return null;
        }

        //@Immutable(hc=true)[T] @Independent(hc=true)[O] @NotModified[T]
        Optional<T> or(/*@IgnoreModifications[T] @Independent[M]*/ Supplier<? extends Optional<? extends T>> supplier) {
            return null;
        }

        //frequency 3
        //@Independent(hc=true)[O] @NotModified[T]
        Stream<T> stream() { return null; }

        //frequency 131
        //@Independent(hc=true)[O] @NotModified[T]
        T orElse(/*@Independent[M] @NotModified[O]*/ T other) { return null; }

        //frequency 16
        //@Independent(hc=true)[O] @NotModified[T]
        T orElseGet(/*@IgnoreModifications[T] @Independent[M]*/ Supplier<? extends T> supplier) { return null; }

        //frequency 54
        //@Independent(hc=true)[O] @NotModified[T]
        T orElseThrow() { return null; }

        //frequency 39
        //@Independent(hc=true)[O] @NotModified[T]

        <X extends Throwable> T orElseThrow(
            /*@IgnoreModifications[T] @Independent[M]*/ Supplier<? extends X> exceptionSupplier) { return null; }

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
        void ifPresent(/*@IgnoreModifications[T] @Independent[M]*/ DoubleConsumer action) { }

        //@NotModified[T]
        void ifPresentOrElse(
            /*@IgnoreModifications[T] @Independent[M]*/ DoubleConsumer action,
            /*@IgnoreModifications[T] @Independent[M]*/ Runnable emptyAction) { }

        //@Independent[O] @NotModified[T]
        DoubleStream stream() { return null; }

        //frequency 1
        //@NotModified[T]
        double orElse(double other) { return 0.0; }

        //@NotModified[T]
        double orElseGet(/*@IgnoreModifications[T] @Independent[M]*/ DoubleSupplier supplier) { return 0.0; }

        //@NotModified[T]
        double orElseThrow() { return 0.0; }

        //@NotModified[T]
        <X extends Throwable> double orElseThrow(
            /*@IgnoreModifications[T] @Independent[M]*/ Supplier<? extends X> exceptionSupplier) { return 0.0; }

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
        void ifPresent(/*@IgnoreModifications[T] @Independent[M]*/ IntConsumer action) { }

        //@NotModified[T]
        void ifPresentOrElse(
            /*@IgnoreModifications[T] @Independent[M]*/ IntConsumer action,
            /*@IgnoreModifications[T] @Independent[M]*/ Runnable emptyAction) { }

        //@Independent[O] @NotModified[T]
        IntStream stream() { return null; }

        //frequency 1
        //@NotModified[T]
        int orElse(int other) { return 0; }

        //@NotModified[T]
        int orElseGet(/*@IgnoreModifications[T] @Independent[M]*/ IntSupplier supplier) { return 0; }

        //@NotModified[T]
        int orElseThrow() { return 0; }

        //@NotModified[T]
        <X extends Throwable> int orElseThrow(
            /*@IgnoreModifications[T] @Independent[M]*/ Supplier<? extends X> exceptionSupplier) { return 0; }

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

    //public class Random implements RandomGenerator, Serializable
    class Random$ {
        //frequency 3
        Random$() { }
        Random$(long seed) { }
        static Random from(@NotModified RandomGenerator generator) { return null; }
        void setSeed(long seed) { }
        //override from java.util.random.RandomGenerator
        //override has frequency 1
        void nextBytes(byte [] bytes) { }

        //override from java.util.random.RandomGenerator
        int nextInt() { return 0; }

        //override from java.util.random.RandomGenerator
        //frequency 3
        int nextInt(int bound) { return 0; }

        //override from java.util.random.RandomGenerator
        long nextLong() { return 0L; }

        //override from java.util.random.RandomGenerator
        boolean nextBoolean() { return false; }

        //override from java.util.random.RandomGenerator
        float nextFloat() { return 0.0F; }

        //override from java.util.random.RandomGenerator
        //override has frequency 1
        double nextDouble() { return 0.0; }

        //override from java.util.random.RandomGenerator
        double nextGaussian() { return 0.0; }

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
        SequencedCollection<E> reversed() { return null; }
        void addFirst(/*@Independent(hc=true)[T] @NotModified[O]*/ E e) { }
        void addLast(/*@Independent(hc=true)[T] @NotModified[O]*/ E e) { }
        //override has frequency 351
        //@Independent(hc=true)[T]
        E getFirst() { return null; }

        //override has frequency 7
        //@Independent(hc=true)[T]
        E getLast() { return null; }

        //@Independent(hc=true)[T]
        E removeFirst() { return null; }

        //@Independent(hc=true)[T]
        E removeLast() { return null; }
    }

    //public interface SequencedMap implements Map<K,V>
    @Container
    class SequencedMap$<K, V> {
        @NotModified SequencedMap<K, V> reversed() { return null; }
        @NotModified Map.Entry<K, V> firstEntry() { return null; }
        @NotModified Map.Entry<K, V> lastEntry() { return null; }
        Map.Entry<K, V> pollFirstEntry() { return null; }
        Map.Entry<K, V> pollLastEntry() { return null; }
        //@Independent(hc=true)[T]
        V putFirst(/*@Independent(hc=true)[T] @NotModified[O]*/ K k, /*@Independent(hc=true)[T] @NotModified[O]*/ V v) {
            return null;
        }

        //@Independent(hc=true)[T]
        V putLast(/*@Independent(hc=true)[T] @NotModified[O]*/ K k, /*@Independent(hc=true)[T] @NotModified[O]*/ V v) {
            return null;
        }
        @NotModified SequencedSet<K> sequencedKeySet() { return null; }
        @NotModified SequencedCollection<V> sequencedValues() { return null; }
        @NotModified SequencedSet<Map.Entry<K, V>> sequencedEntrySet() { return null; }
    }

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
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object object) { return false; }

        //override from java.lang.Iterable, java.util.Collection
        //frequency 2
        //@NotModified[H] @NotNull[H]
        Iterator<E> iterator() { return null; }

        //override from java.util.Collection
        //frequency 3
        //@NotModified[H]
        Object [] toArray() { return null; }

        //override from java.util.Collection
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[O]*/ T [] t) { return null; }

        //override from java.util.Collection
        //frequency 27
        boolean add(/*@Independent(hc=true)[H] @NotModified[O]*/ E e) { return false; }

        //override from java.util.Collection
        //frequency 3

        boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object object) {
            return false;
        }

        //override from java.util.Collection
        //@NotModified[H]
        boolean containsAll(/*@Independent[M] @NotModified[O]*/ Collection<?> collection) { return false; }

        //override from java.util.Collection
        //frequency 5
        boolean addAll(/*@Independent(hc=true)[H] @NotModified[O]*/ Collection<? extends E> collection) { return false; }

        //override from java.util.Collection
        //frequency 1
        boolean retainAll(/*@Independent(hc=true)[H] @NotModified[O]*/ Collection<?> collection) { return false; }

        //override from java.util.Collection
        boolean removeAll(/*@Independent(hc=true)[H] @NotModified[O]*/ Collection<?> collection) { return false; }

        //override from java.util.Collection
        void clear() { }

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
        static <E> Set<E> of(/*@NotModified[O]*/ E ... elements) { return null; }
        @NotModified @ImmutableContainer(hc = true) @NotNull
        static <E> Set<E> copyOf(/*@NotModified[O]*/ Collection<? extends E> coll) { return null; }
    }

    //public interface SortedMap implements SequencedMap<K,V>
    @Container
    class SortedMap$<K, V> {
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Comparator<? super K> comparator() { return null; }

        SortedMap<K, V> subMap(
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k,
            /*@Independent(hc=true)[T] @NotModified[O]*/ K k1) { return null; }
        SortedMap<K, V> headMap(/*@Independent(hc=true)[T] @NotModified[O]*/ K k) { return null; }
        SortedMap<K, V> tailMap(/*@Independent(hc=true)[T] @NotModified[O]*/ K k) { return null; }
        //@Independent(hc=true)[T]
        K firstKey() { return null; }

        //@Independent(hc=true)[T]
        K lastKey() { return null; }

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

        V putFirst(/*@Independent(hc=true)[H] @NotModified[O]*/ K k, /*@Independent(hc=true)[H] @NotModified[O]*/ V v) {
            return null;
        }

        //override from java.util.SequencedMap
        //@Independent(hc=true)[H]

        V putLast(/*@Independent(hc=true)[H] @NotModified[O]*/ K k, /*@Independent(hc=true)[H] @NotModified[O]*/ V v) {
            return null;
        }

        //override from java.util.SequencedMap
        //@NotModified[H]
        SortedMap<K, V> reversed() { return null; }
    }

    //public class TreeMap extends AbstractMap<K,V> implements NavigableMap<K,V>, Cloneable, Serializable
    @Container
    class TreeMap$<K, V> {
        TreeMap$() { }
        TreeMap$(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[O]*/ Comparator<? super K> comparator) { }
        TreeMap$(/*@NotModified[O]*/ Map<? extends K, ? extends V> m) { }
        TreeMap$(/*@NotModified[O]*/ SortedMap<K, ? extends V> m) { }
        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.AbstractMap, java.util.Map
        //@NotModified[H]
        boolean containsKey(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object key) { return false; }

        //override from java.util.AbstractMap, java.util.Map
        boolean containsValue(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object value) {
            return false;
        }

        //override from java.util.AbstractMap, java.util.Map
        //@Independent(hc=true)[H] @NotModified[H]
        V get(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[O]*/ Object key) { return null; }

        //override from java.util.SortedMap
        //@Immutable(hc=true)[T] @Independent(hc=true)[H]
        Comparator<? super K> comparator() { return null; }

        //override from java.util.SortedMap
        //@Independent(hc=true)[H]
        K firstKey() { return null; }

        //override from java.util.SortedMap
        //@Independent(hc=true)[H]
        K lastKey() { return null; }

        //override from java.util.SequencedMap, java.util.SortedMap
        //@Independent(hc=true)[H]

        V putFirst(/*@Independent(hc=true)[H] @NotModified[O]*/ K k, /*@Independent(hc=true)[H] @NotModified[O]*/ V v) {
            return null;
        }

        //override from java.util.SequencedMap, java.util.SortedMap
        //@Independent(hc=true)[H]

        V putLast(/*@Independent(hc=true)[H] @NotModified[O]*/ K k, /*@Independent(hc=true)[H] @NotModified[O]*/ V v) {
            return null;
        }

        //override from java.util.AbstractMap, java.util.Map
        void putAll(/*@NotModified[O]*/ Map<? extends K, ? extends V> map) { }

        //override from java.util.AbstractMap, java.util.Map
        //@Independent(hc=true)[H]

        V put(/*@Independent(hc=true)[H] @NotModified[O]*/ K key, /*@Independent(hc=true)[H] @NotModified[O]*/ V value) {
            return null;
        }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V putIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V value) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V computeIfAbsent(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ Function<? super K, ? extends V> mappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V computeIfPresent(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V compute(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@IgnoreModifications[T]*/ BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V merge(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V value,
            /*@IgnoreModifications[T] @Independent(hc=true)[H]*/ BiFunction<? super V, ? super V, ? extends V>
                remappingFunction) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        //@Independent(hc=true)[H]
        V remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[O]*/ Object key) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        void clear() { }

        //override from java.lang.Object, java.util.AbstractMap
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        //@NotModified[H]
        Map.Entry<K, V> firstEntry() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        //@NotModified[H]
        Map.Entry<K, V> lastEntry() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        Map.Entry<K, V> pollFirstEntry() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        Map.Entry<K, V> pollLastEntry() { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        Map.Entry<K, V> lowerEntry(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@Independent(hc=true)[H] @NotModified[H]
        K lowerKey(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        Map.Entry<K, V> floorEntry(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@Independent(hc=true)[H] @NotModified[H]
        K floorKey(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        Map.Entry<K, V> ceilingEntry(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@Independent(hc=true)[H] @NotModified[H]
        K ceilingKey(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@NotModified[H]
        Map.Entry<K, V> higherEntry(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

        //override from java.util.NavigableMap
        //@Independent(hc=true)[H] @NotModified[H]
        K higherKey(/*@Independent[M] @NotModified[O]*/ K key) { return null; }

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
        boolean replace(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V oldValue,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V newValue) { return false; }

        //override from java.util.Map
        //@Independent(hc=true)[H]

        V replace(
            /*@Independent(hc=true)[H] @NotModified[O]*/ K key,
            /*@Independent(hc=true)[H] @NotModified[O]*/ V value) { return null; }

        //override from java.util.Map
        //@NotModified[H]
        void forEach(/*@IgnoreModifications[T] @Independent[M]*/ BiConsumer<? super K, ? super V> action) { }

        //override from java.util.Map
        void replaceAll(/*@IgnoreModifications[T]*/ BiFunction<? super K, ? super V, ? extends V> function) { }
    }

    //public final class UUID implements Serializable, Comparable<UUID>
    @ImmutableContainer
    class UUID$ {
        UUID$(long mostSigBits, long l) { }
        //frequency 3
        //@Immutable[T] @Independent[O] @NotModified[T]
        static UUID randomUUID() { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static UUID nameUUIDFromBytes(/*@Independent[T] @NotModified[O]*/ byte [] name) { return null; }

        //@Immutable[T] @Independent[O] @NotModified[T]
        static UUID fromString(String name) { return null; }

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
        int compareTo(/*@Immutable[T] @Independent[T] @NotModified[O] @NotNull[H]*/ UUID val) { return 0; }
    }

    //public class Vector extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, Serializable
    class Vector$<E> {
        Vector$(int initialCapacity, int capacityIncrement) { }
        Vector$(int initialCapacity) { }
        Vector$() { }
        Vector$(Collection<? extends E> c) { }
        void copyInto(Object [] anArray) { }
        void trimToSize() { }
        void ensureCapacity(int minCapacity) { }
        void setSize(int newSize) { }
        int capacity() { return 0; }
        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        int size() { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        boolean isEmpty() { return false; }
        Enumeration<E> elements() { return null; }
        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        boolean contains(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object o) { return false; }

        //override from java.util.AbstractList, java.util.List
        int indexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object o) { return 0; }

        int indexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object o, int index) { return 0; }

        //override from java.util.AbstractList, java.util.List
        int lastIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object o) { return 0; }

        int lastIndexOf(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object o, int index) {
            return 0;
        }

        //@Independent(hc=true)[T]
        E elementAt(int index) { return null; }

        //@Independent(hc=true)[T]
        E firstElement() { return null; }

        //@Independent(hc=true)[T]
        E lastElement() { return null; }
        void setElementAt(/*@Independent(hc=true)[T] @NotModified[T]*/ E obj, int index) { }
        void removeElementAt(int index) { }
        void insertElementAt(/*@Independent(hc=true)[T] @NotModified[T]*/ E obj, int index) { }
        void addElement(/*@Independent(hc=true)[T] @NotModified[T]*/ E obj) { }
        boolean removeElement(/*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Object obj) {
            return false;
        }
        void removeAllElements() { }
        //override from java.lang.Object
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        protected Object clone() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        Object [] toArray() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        <T> T [] toArray(/*@Independent[M] @NotModified[H]*/ T [] a) { return null; }

        //override from java.util.AbstractList, java.util.List
        //@Independent(hc=true)[H] @NotModified[H] @GetSet("_synthetic_list")[H]
        E get(int index) { return null; }

        //override from java.util.AbstractList, java.util.List
        //@Independent(hc=true)[H] @GetSet("_synthetic_list")[H]
        E set(int index, /*@Independent(hc=true)[H] @NotModified[T]*/ E element) { return null; }

        //override from java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.List
        boolean add(/*@Independent(hc=true)[H] @NotModified[T]*/ E e) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        boolean remove(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Object o) { return false; }

        //override from java.util.AbstractList, java.util.List
        void add(int index, /*@Independent(hc=true)[H] @NotModified[T]*/ E element) { }

        //override from java.util.AbstractList, java.util.List
        //@Independent(hc=true)[H]
        E remove(int index) { return null; }

        //override from java.util.AbstractCollection, java.util.AbstractList, java.util.Collection, java.util.List
        void clear() { }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        //@NotModified[H]
        boolean containsAll(/*@Independent[M] @NotModified[H]*/ Collection<?> c) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        boolean addAll(/*@Independent(hc=true)[H] @NotModified[H]*/ Collection<? extends E> c) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        boolean removeAll(/*@Independent(hc=true)[H] @NotModified[H]*/ Collection<?> c) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection, java.util.List
        boolean retainAll(/*@Independent(hc=true)[H] @NotModified[H]*/ Collection<?> c) { return false; }

        //override from java.util.Collection
        boolean removeIf(/*@IgnoreModifications[T] @Independent(hc=true)[H]*/ Predicate<? super E> filter) {
            return false;
        }

        //override from java.util.AbstractList, java.util.List
        boolean addAll(int index, /*@NotModified[H]*/ Collection<? extends E> c) { return false; }

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
        void forEach(/*@IgnoreModifications[T] @Independent[M] @NotNull[H]*/ Consumer<? super E> action) { }

        //override from java.util.List
        void replaceAll(/*@IgnoreModifications[T]*/ UnaryOperator<E> operator) { }

        //override from java.util.List
        void sort(/*@Immutable(hc=true)[T] @Independent(hc=true)[H] @NotModified[T]*/ Comparator<? super E> c) { }

        //override from java.lang.Iterable, java.util.Collection, java.util.List
        //@Independent(hc=true)[H] @NotModified[H] @NotNull[H]
        Spliterator<E> spliterator() { return null; }
    }
}
