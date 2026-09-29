package io.codelaser.maddi.aapi.archive.libs.guava;
import com.google.common.base.Converter;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.*;
import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.FinalFields;
import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.annotation.Identity;
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.method.GetSet;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;
import io.codelaser.maddi.annotation.type.UtilityClass;
@Independent(absent = true)
public class ComGoogleCommonCollect {
    public static final String PACKAGE_NAME = "com.google.common.collect";
    //public abstract class AbstractIterator extends UnmodifiableIterator<T>
    @Container
    @Independent(absent = true)
    class AbstractIterator$<T> {
        //override from java.util.Iterator
        @Modified
        boolean hasNext() { return false; }

        //override from java.util.Iterator
        @Independent(hc = true) @Modified
        T next() { return null; }
        @Independent(hc = true) @Modified T peek() { return null; }
    }

    //public abstract class AbstractSequentialIterator extends UnmodifiableIterator<T>
    @Container
    @Independent(hc = true)
    class AbstractSequentialIterator$<T> {
        //override from java.util.Iterator
        @NotModified
        boolean hasNext() { return false; }

        //override from java.util.Iterator
        @Independent(hc = true) @Modified
        T next() { return null; }
    }

    //public final class ArrayListMultimap extends AbstractListMultimap<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class ArrayListMultimap$<K, V> {
        @Independent @NotModified static <K, V> ArrayListMultimap<K, V> create() { return null; }
        @Independent @NotModified
        static <K, V> ArrayListMultimap<K, V> create(
            @Independent @NotModified Multimap<? extends K, ? extends V> multimap) { return null; }

        @Independent @NotModified
        static <K, V> ArrayListMultimap<K, V> create(int expectedKeys, int expectedValuesPerKey) { return null; }
        @NotModified void trimToSize() { }
    }

    //public final class ArrayTable extends AbstractTable<R,C,V> implements Table<R,C,V>, Serializable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class ArrayTable$<R, C, V> {
        @Independent @NotModified V at(int rowIndex, int columnIndex) { return null; }
        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent(absent = true) @Modified
        Set<Table.Cell<R, C, V>> cellSet() { return null; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Modified
        void clear() { }

        //override from com.google.common.collect.Table
        @Independent @NotModified
        Map<R, V> column(@Independent @NotModified C columnKey) { return null; }

        @Independent(absent = true) @NotModified @GetSet("columnList")
        ImmutableList<C> columnKeyList() { return null; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent(absent = true) @NotModified
        ImmutableSet<C> columnKeySet() { return null; }

        //override from com.google.common.collect.Table
        @Independent(absent = true) @Modified
        Map<C, Map<R, V>> columnMap() { return null; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @NotModified

        boolean contains(@Independent @NotModified Object rowKey, @Independent @NotModified Object columnKey) {
            return false;
        }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @NotModified
        boolean containsColumn(@Independent @NotModified Object columnKey) { return false; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @NotModified
        boolean containsRow(@Independent @NotModified Object rowKey) { return false; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @NotModified
        boolean containsValue(@Independent @NotModified Object value) { return false; }

        @Independent @NotModified
        static <R, C, V> ArrayTable<R, C, V> create(
            @Independent @NotModified Iterable<? extends R> rowKeys,
            @Independent @NotModified Iterable<? extends C> columnKeys) { return null; }

        @Independent @NotModified
        static <R, C, V> ArrayTable<R, C, V> create(@Independent @NotModified Table<R, C, ? extends V> table) {
            return null;
        }

        @Independent(hc = true) @Modified
        V erase(@Independent @NotModified Object rowKey, @Independent @NotModified Object columnKey) { return null; }
        @Modified void eraseAll() { }
        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent @NotModified
        V get(@Independent @NotModified Object rowKey, @Independent @NotModified Object columnKey) { return null; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @NotModified
        boolean isEmpty() { return false; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent(hc = true) @Modified

        V put(
            @Independent @NotModified R rowKey,
            @Independent @NotModified C columnKey,
            @Independent(hc = true) @NotModified V value) { return null; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Modified
        void putAll(@Independent @NotModified Table<? extends R, ? extends C, ? extends V> table) { }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent @Modified
        V remove(@Independent @NotModified Object rowKey, @Independent @NotModified Object columnKey) { return null; }

        //override from com.google.common.collect.Table
        @Independent @NotModified
        Map<C, V> row(@Independent @NotModified R rowKey) { return null; }
        @Independent(absent = true) @NotModified @GetSet("rowList") ImmutableList<R> rowKeyList() { return null; }
        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent(absent = true) @NotModified
        ImmutableSet<R> rowKeySet() { return null; }

        //override from com.google.common.collect.Table
        @Independent(absent = true) @NotModified
        Map<R, Map<C, V>> rowMap() { return null; }

        @Independent(hc = true) @Modified
        V set(int rowIndex, int columnIndex, @Independent(hc = true) @NotModified V value) { return null; }

        //override from com.google.common.collect.Table
        @NotModified
        int size() { return 0; }
        @Independent @NotModified V [][] toArray(Class<V> valueClass) { return null; }
        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent(absent = true) @Modified
        Collection<V> values() { return null; }
    }

    //public interface BiMap implements Map<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- a collection contract with mutators, shared by mutable and immutable implementations (GUAVA.md)
    @Container
    @Independent(absent = true)
    class BiMap$<K, V> {
        @Independent(hc = true) @Modified
        V forcePut(@Independent(hc = true) @NotModified K key, @Independent(hc = true) @NotModified V value) {
            return null;
        }
        @Independent(absent = true) @NotModified BiMap<V, K> inverse() { return null; }
        //override from java.util.Map
        @Independent(hc = true) @Modified
        V put(@Independent(hc = true) @NotModified K key, @Independent(hc = true) @NotModified V value) { return null; }

        //override from java.util.Map
        @Modified
        void putAll(@Independent @NotModified Map<? extends K, ? extends V> map) { }

        //override from java.util.Map
        @Independent(absent = true) @NotModified
        Set<V> values() { return null; }
    }

    //public enum BoundType extends Enum<BoundType>
    //annotated as EXPECTED; computed @Immutable (1) -- a value (GUAVA.md)
    @Immutable(hc = true)
    class BoundType$ {
        @Independent @NotModified static final BoundType CLOSED = null;
        @Independent @NotModified static final BoundType OPEN = null;
    }

    //public interface ClassToInstanceMap implements Map<Class<? extends B>,B>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- a collection contract with mutators, shared by mutable and immutable implementations (GUAVA.md)
    @Container
    @Independent(absent = true)
    class ClassToInstanceMap$<B> {
        @Independent(hc = true) @Modified <T extends B> T getInstance(Class<T> type) { return null; }
        @Independent @Modified
        <T extends B> T putInstance(Class<T> type, @Independent @NotModified T value) { return null; }
    }

    //public final class Collections2
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @UtilityClass
    class Collections2$ {
        @Independent @NotModified
        static <E> Collection<E> filter(
            @Independent @Modified Collection<E> unfiltered,
            @Independent @NotModified Predicate<? super E> predicate) { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> Collection<List<E>> orderedPermutations(
            @Independent @NotModified Iterable<E> elements) { return null; }

        @Independent @NotModified
        static <E> Collection<List<E>> orderedPermutations(
            @Independent @NotModified Iterable<E> elements,
            @Independent @NotModified Comparator<? super E> comparator) { return null; }

        @Independent @NotModified
        static <E> Collection<List<E>> permutations(@Independent @NotModified Collection<E> elements) { return null; }

        @Independent @NotModified
        static <F, T> Collection<T> transform(
            @Independent @Modified Collection<F> fromCollection,
            @Independent @NotModified Function<? super F, T> function) { return null; }
    }

    //public final class Comparators
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @Container
    @UtilityClass
    class Comparators$ {
        @Independent @NotModified
        static <T> Comparator<java.util.Optional<T>> emptiesFirst(
            @Independent @NotModified Comparator<? super T> valueComparator) { return null; }

        @Independent @NotModified
        static <T> Comparator<java.util.Optional<T>> emptiesLast(
            @Independent @NotModified Comparator<? super T> valueComparator) { return null; }

        @Independent @NotModified
        static <T> Collector<T, ?, List<T>> greatest(int k, @Independent @NotModified Comparator<? super T> comparator) {
            return null;
        }

        @NotModified
        static <T> boolean isInOrder(
            @Independent @NotModified Iterable<? extends T> iterable,
            @Independent @NotModified Comparator<T> comparator) { return false; }

        @NotModified
        static <T> boolean isInStrictOrder(
            @Independent @NotModified Iterable<? extends T> iterable,
            @Independent @NotModified Comparator<T> comparator) { return false; }

        @Independent @NotModified
        static <T> Collector<T, ?, List<T>> least(int k, @Independent @NotModified Comparator<? super T> comparator) {
            return null;
        }

        @Independent @NotModified
        static <T, S extends T> Comparator<Iterable<S>> lexicographical(
            @Independent @NotModified Comparator<T> comparator) { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> T max(@Independent @NotModified T a, @Independent @NotModified T b) {
            return null;
        }

        @Independent @NotModified
        static <T> T max(
            @Independent @NotModified T a,
            @Independent @NotModified T b,
            @Independent @NotModified Comparator<? super T> comparator) { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> T min(@Independent @NotModified T a, @Independent @NotModified T b) {
            return null;
        }

        @Independent @NotModified
        static <T> T min(
            @Independent @NotModified T a,
            @Independent @NotModified T b,
            @Independent @NotModified Comparator<? super T> comparator) { return null; }
    }

    //public abstract class ComparisonChain
    //annotated as EXPECTED; computed @Immutable(hc = true) (1) -- a value (GUAVA.md)
    @Immutable(hc = true)
    class ComparisonChain$ {
        @Independent @NotModified
        ComparisonChain compare(@Independent @NotModified Boolean left, @Independent @NotModified Boolean right) {
            return null;
        }

        @Independent @NotModified
        ComparisonChain compare(
            @Independent @NotModified Comparable<?> left,
            @Independent @NotModified Comparable<?> right) { return null; }

        @Independent @NotModified
        <T> ComparisonChain compare(
            @Independent @NotModified T left,
            @Independent @NotModified T right,
            @Independent @NotModified Comparator<T> comparator) { return null; }
        @Independent @NotModified ComparisonChain compare(double left, double right) { return null; }
        @Independent @NotModified ComparisonChain compare(float left, float right) { return null; }
        @Independent @NotModified ComparisonChain compare(int left, int right) { return null; }
        @Independent @NotModified ComparisonChain compare(long left, long right) { return null; }
        @Independent @NotModified ComparisonChain compareFalseFirst(boolean left, boolean right) { return null; }
        @Independent @NotModified ComparisonChain compareTrueFirst(boolean left, boolean right) { return null; }
        @NotModified int result() { return 0; }
        @Independent @NotModified static ComparisonChain start() { return null; }
    }

    //public class ComputationException extends RuntimeException
    @FinalFields
    @Container
    @Independent(absent = true)
    class ComputationException$ {ComputationException$(@Independent @NotModified Throwable cause) { } }

    //public final class ConcurrentHashMultiset extends AbstractMultiset<E> implements Serializable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class ConcurrentHashMultiset$<E> {
        //override from com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        @Modified
        int add(@Independent @NotModified E element, int occurrences) { return 0; }

        //override from com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
        @Modified
        void clear() { }

        //override from com.google.common.collect.Multiset
        @NotModified
        int count(@Independent @NotModified Object element) { return 0; }
        @Independent @NotModified static <E> ConcurrentHashMultiset<E> create() { return null; }
        @Independent @NotModified
        static <E> ConcurrentHashMultiset<E> create(@Independent @NotModified Iterable<? extends E> elements) {
            return null;
        }

        @Independent @NotModified
        static <E> ConcurrentHashMultiset<E> create(@Independent @NotModified ConcurrentMap<E, AtomicInteger> countMap) {
            return null;
        }

        //override from com.google.common.collect.AbstractMultiset
        @Independent @NotModified
        Set<Multiset.Entry<E>> createEntrySet() { return null; }

        //override from com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
        @NotModified
        boolean isEmpty() { return false; }

        //override from com.google.common.collect.Multiset, java.lang.Iterable, java.util.AbstractCollection, java.util.Collection
        @Fluent @Independent @NotModified
        Iterator<E> iterator() { return null; }

        //override from com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        @Modified
        int remove(@Independent @NotModified Object element, int occurrences) { return 0; }
        @Modified boolean removeExactly(@Independent @NotModified Object element, int occurrences) { return false; }
        //override from com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        @Modified
        int setCount(@Independent @NotModified E element, int count) { return 0; }

        //override from com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        @Modified
        boolean setCount(@Independent @NotModified E element, int expectedOldCount, int newCount) { return false; }

        //override from com.google.common.collect.Multiset, java.util.AbstractCollection, java.util.Collection
        @NotModified
        int size() { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection
        @Independent @NotModified
        Object [] toArray() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection
        @Independent @NotModified
        <T> T [] toArray(@Independent @NotModified T [] array) { return null; }
    }

    //public abstract class ContiguousSet extends ImmutableSortedSet<C>
    @Independent(absent = true)
    class ContiguousSet$<C extends Comparable> {
        @Independent @NotModified static <E> ImmutableSortedSet.Builder<E> builder() { return null; }
        @Independent @NotModified static ContiguousSet<Integer> closed(int lower, int upper) { return null; }
        @Independent @NotModified static ContiguousSet<Long> closed(long lower, long upper) { return null; }
        @Independent @NotModified static ContiguousSet<Integer> closedOpen(int lower, int upper) { return null; }
        @Independent @NotModified static ContiguousSet<Long> closedOpen(long lower, long upper) { return null; }
        @Independent @NotModified
        static <C extends Comparable> ContiguousSet<C> create(
            @Independent @Modified Range<C> range,
            @Independent @NotModified DiscreteDomain<C> domain) { return null; }

        //override from com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet, java.util.SortedSet
        @Independent @NotModified
        ContiguousSet<C> headSet(@Independent @NotModified C toElement) { return null; }

        //override from com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet
        @Independent @NotModified
        ContiguousSet<C> headSet(@Independent @NotModified C toElement, boolean inclusive) { return null; }

        @Independent @NotModified
        ContiguousSet<C> intersection(@Independent @NotModified ContiguousSet<C> other) { return null; }
        @Independent @NotModified Range<C> range() { return null; }
        @Independent @NotModified
        Range<C> range(
            @Independent @NotModified BoundType lowerBoundType,
            @Independent @NotModified BoundType upperBoundType) { return null; }

        //override from com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet, java.util.SortedSet
        @Independent @NotModified

        ContiguousSet<C> subSet(@Independent @NotModified C fromElement, @Independent @NotModified C toElement) {
            return null;
        }

        //override from com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet
        @Independent @NotModified

        ContiguousSet<C> subSet(
            @Independent @NotModified C fromElement,
            boolean fromInclusive,
            @Independent @NotModified C toElement,
            boolean toInclusive) { return null; }

        //override from com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet, java.util.SortedSet
        @Independent @NotModified
        ContiguousSet<C> tailSet(@Independent @NotModified C fromElement) { return null; }

        //override from com.google.common.collect.ImmutableSortedSet, java.util.NavigableSet
        @Independent @NotModified
        ContiguousSet<C> tailSet(@Independent @NotModified C fromElement, boolean inclusive) { return null; }

        //override from java.lang.Object, java.util.AbstractCollection
        @NotModified
        public String toString() { return null; }
    }

    //public abstract class DiscreteDomain
    //annotated as EXPECTED; computed @Immutable(hc = true) (1) -- a value (GUAVA.md)
    @Immutable(hc = true)
    class DiscreteDomain$<C extends Comparable> {
        @Independent @NotModified static DiscreteDomain<BigInteger> bigIntegers() { return null; }
        @NotModified long distance(@Independent @NotModified C start, @Independent @NotModified C end) { return 0L; }
        @Independent @NotModified static DiscreteDomain<Integer> integers() { return null; }
        @Independent @NotModified static DiscreteDomain<Long> longs() { return null; }
        @Independent @NotModified C maxValue() { return null; }
        @Independent @NotModified C minValue() { return null; }
        @Independent @NotModified C next(@Independent @NotModified C value) { return null; }
        @Independent @NotModified C previous(@Independent @NotModified C value) { return null; }
    }

    //public final class EnumBiMap extends AbstractBiMap<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class EnumBiMap$<K extends Enum<K>, V extends Enum<V>> {
        @Independent @NotModified
        static <K extends Enum<K>, V extends Enum<V>> EnumBiMap<K, V> create(Class<K> keyType, Class<V> valueType) {
            return null;
        }

        @Independent @NotModified
        static <K extends Enum<K>, V extends Enum<V>> EnumBiMap<K, V> create(@Independent @NotModified Map<K, V> map) {
            return null;
        }
        @NotModified @GetSet("keyTypeOrObjectUnderJ2cl") Class<K> keyType() { return null; }
        @NotModified @GetSet("valueTypeOrObjectUnderJ2cl") Class<V> valueType() { return null; }
    }

    //public final class EnumHashBiMap extends AbstractBiMap<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class EnumHashBiMap$<K extends Enum<K>, V> {
        @Independent @NotModified
        static <K extends Enum<K>, V> EnumHashBiMap<K, V> create(Class<K> keyType) { return null; }

        @Independent @NotModified
        static <K extends Enum<K>, V> EnumHashBiMap<K, V> create(@Independent @NotModified Map<K, ? extends V> map) {
            return null;
        }

        //override from com.google.common.collect.AbstractBiMap, com.google.common.collect.BiMap
        @Independent(hc = true) @Modified

        V forcePut(@Independent(hc = true) @NotModified K key, @Independent(hc = true) @NotModified V value) {
            return null;
        }
        @NotModified @GetSet("keyTypeOrObjectUnderJ2cl") Class<K> keyType() { return null; }
        //override from com.google.common.collect.AbstractBiMap, com.google.common.collect.BiMap, com.google.common.collect.ForwardingMap, java.util.Map
        @Independent(hc = true) @Modified
        V put(@Independent(hc = true) @NotModified K key, @Independent(hc = true) @NotModified V value) { return null; }
    }

    //public final class EnumMultiset extends AbstractMultiset<E> implements Serializable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class EnumMultiset$<E extends Enum<E>> {
        //override from com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        @Modified
        int add(@Independent @NotModified E element, int occurrences) { return 0; }

        //override from com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
        @Modified
        void clear() { }

        //override from com.google.common.collect.Multiset
        @NotModified
        int count(@Independent @NotModified Object element) { return 0; }
        @Independent @NotModified static <E extends Enum<E>> EnumMultiset<E> create(Class<E> type) { return null; }
        @Independent @NotModified
        static <E extends Enum<E>> EnumMultiset<E> create(@Independent @NotModified Iterable<E> elements) { return null; }

        @Independent @NotModified
        static <E extends Enum<E>> EnumMultiset<E> create(@Independent @NotModified Iterable<E> elements, Class<E> type) {
            return null;
        }

        //override from com.google.common.collect.Multiset
        @Modified
        void forEachEntry(@Independent @NotModified ObjIntConsumer<? super E> action) { }

        //override from com.google.common.collect.Multiset, java.lang.Iterable, java.util.AbstractCollection, java.util.Collection
        @Fluent @Independent @NotModified
        Iterator<E> iterator() { return null; }

        //override from com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        @Modified
        int remove(@Independent @NotModified Object element, int occurrences) { return 0; }

        //override from com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        @Modified
        int setCount(@Independent @NotModified E element, int count) { return 0; }

        //override from com.google.common.collect.Multiset, java.util.AbstractCollection, java.util.Collection
        @NotModified
        int size() { return 0; }
    }

    //public final class EvictingQueue extends ForwardingQueue<E> implements Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class EvictingQueue$<E> {
        //override from com.google.common.collect.ForwardingCollection, java.util.Collection, java.util.Queue
        @Modified
        boolean add(@Independent @NotModified E e) { return false; }

        //override from com.google.common.collect.ForwardingCollection, java.util.Collection
        @Modified
        boolean addAll(@Independent @NotModified Collection<? extends E> collection) { return false; }
        @Independent @NotModified static <E> EvictingQueue<E> create(int maxSize) { return null; }
        //override from com.google.common.collect.ForwardingQueue, java.util.Queue
        @Modified
        boolean offer(@Independent @NotModified E e) { return false; }
        @Modified int remainingCapacity() { return 0; }
        //override from com.google.common.collect.ForwardingCollection, java.util.Collection
        @Independent @NotModified
        Object [] toArray() { return null; }
    }

    //public abstract class FluentIterable implements Iterable<E>
    @FinalFields
    @Independent(hc = true)
    class FluentIterable$<E> {
        @NotModified boolean allMatch(@Independent @Modified Predicate<? super E> predicate) { return false; }
        @NotModified boolean anyMatch(@Independent @Modified Predicate<? super E> predicate) { return false; }
        @Independent @NotModified
        FluentIterable<E> append(@Independent @NotModified Iterable<? extends E> other) { return null; }
        @Independent @NotModified FluentIterable<E> append(@Independent @NotModified E ... elements) { return null; }
        @Independent @NotModified
        static <T> FluentIterable<T> concat(@Independent @NotModified Iterable<? extends Iterable<? extends T>> inputs) {
            return null;
        }

        @Independent @NotModified
        static <T> FluentIterable<T> concat(
            @Independent @NotModified Iterable<? extends T> a,
            @Independent @NotModified Iterable<? extends T> b) { return null; }

        @Independent @NotModified
        static <T> FluentIterable<T> concat(
            @Independent @NotModified Iterable<? extends T> a,
            @Independent @NotModified Iterable<? extends T> b,
            @Independent @NotModified Iterable<? extends T> c) { return null; }

        @Independent @NotModified
        static <T> FluentIterable<T> concat(
            @Independent @NotModified Iterable<? extends T> a,
            @Independent @NotModified Iterable<? extends T> b,
            @Independent @NotModified Iterable<? extends T> c,
            @Independent @NotModified Iterable<? extends T> d) { return null; }

        @Independent @NotModified
        static <T> FluentIterable<T> concat(@Independent @NotModified Iterable<? extends T> ... inputs) { return null; }
        @NotModified boolean contains(@Independent @NotModified Object target) { return false; }
        @Identity @Independent @NotModified
        <C extends Collection<? super E>> C copyInto(@Independent @Modified C collection) { return null; }
        @Independent @Modified @NotModified(after = "iterableDelegate") FluentIterable<E> cycle() { return null; }
        @Independent @NotModified <T> FluentIterable<T> filter(Class<T> type) { return null; }
        @Independent @NotModified
        FluentIterable<E> filter(@Independent @Modified Predicate<? super E> predicate) { return null; }
        @Independent @NotModified Optional<E> first() { return null; }
        @Independent @NotModified
        Optional<E> firstMatch(@Independent @Modified Predicate<? super E> predicate) { return null; }

        @Identity @Independent @NotModified
        static <E> FluentIterable<E> from(@Independent @NotModified Iterable<E> iterable) { return null; }

        @Independent @NotModified
        static <E> FluentIterable<E> from(@Independent @NotModified E [] elements) { return null; }

        @Identity @Independent @NotModified
        static <E> FluentIterable<E> from(@Independent @NotModified FluentIterable<E> iterable) { return null; }
        @Independent @NotModified E get(int position) { return null; }
        @Independent @NotModified
        <K> ImmutableListMultimap<K, E> index(@Independent @Modified Function<? super E, K> keyFunction) { return null; }
        @NotModified boolean isEmpty() { return false; }
        @NotModified String join(@Independent @NotModified Joiner joiner) { return null; }
        @Independent @NotModified Optional<E> last() { return null; }
        @Independent @Modified @NotModified(after = "iterableDelegate")
        FluentIterable<E> limit(int maxSize) { return null; }
        @Independent @NotModified static <E> FluentIterable<E> of() { return null; }
        @Independent @NotModified
        static <E> FluentIterable<E> of(@Independent @NotModified E element, @Independent @NotModified E ... elements) {
            return null;
        }
        @NotModified int size() { return 0; }
        @Independent @Modified @NotModified(after = "iterableDelegate")
        FluentIterable<E> skip(int numberToSkip) { return null; }
        @Independent @NotModified Stream<E> stream() { return null; }
        @Independent @NotModified E [] toArray(Class<E> type) { return null; }
        @Independent @NotModified ImmutableList<E> toList() { return null; }
        @Independent @NotModified
        <V> ImmutableMap<E, V> toMap(@Independent @Modified Function<? super E, V> valueFunction) { return null; }

        @Independent @Modified @NotModified(after = "iterableDelegate")
        ImmutableMultiset<E> toMultiset() { return null; }
        @Fluent @Independent @NotModified ImmutableSet<E> toSet() { return null; }
        @Independent @NotModified
        ImmutableList<E> toSortedList(@Independent @NotModified Comparator<? super E> comparator) { return null; }

        @Independent @NotModified
        ImmutableSortedSet<E> toSortedSet(@Independent @NotModified Comparator<? super E> comparator) { return null; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent @Modified @NotModified(after = "iterableDelegate")
        <T> FluentIterable<T> transform(@Independent @Modified Function<? super E, T> function) { return null; }

        @Independent @Modified @NotModified(after = "iterableDelegate")
        <T> FluentIterable<T> transformAndConcat(
            @Independent @Modified Function<? super E, ? extends Iterable<? extends T>> function) { return null; }

        @Independent @NotModified
        <K> ImmutableMap<K, E> uniqueIndex(@Independent @Modified Function<? super E, K> keyFunction) { return null; }
    }

    //public abstract class ForwardingBlockingDeque extends ForwardingDeque<E> implements BlockingDeque<E>
    @FinalFields
    @Independent(absent = true)
    class ForwardingBlockingDeque$<E> {
        //override from java.util.concurrent.BlockingQueue
        @Modified
        int drainTo(@Independent @Modified Collection<? super E> c) { return 0; }

        //override from java.util.concurrent.BlockingQueue
        @Modified
        int drainTo(@Independent @Modified Collection<? super E> c, int maxElements) { return 0; }

        //override from java.util.concurrent.BlockingDeque, java.util.concurrent.BlockingQueue
        @Modified
        boolean offer(@Independent @NotModified E e, long timeout, @Independent @Modified TimeUnit unit) { return false; }

        //override from java.util.concurrent.BlockingDeque
        @Modified

        boolean offerFirst(@Independent @NotModified E e, long timeout, @Independent @Modified TimeUnit unit) {
            return false;
        }

        //override from java.util.concurrent.BlockingDeque
        @Modified

        boolean offerLast(@Independent @NotModified E e, long timeout, @Independent @Modified TimeUnit unit) {
            return false;
        }

        //override from java.util.concurrent.BlockingDeque, java.util.concurrent.BlockingQueue
        @Independent @Modified
        E poll(long timeout, @Independent @Modified TimeUnit unit) { return null; }

        //override from java.util.concurrent.BlockingDeque
        @Independent @Modified
        E pollFirst(long timeout, @Independent @Modified TimeUnit unit) { return null; }

        //override from java.util.concurrent.BlockingDeque
        @Independent @Modified
        E pollLast(long timeout, @Independent @Modified TimeUnit unit) { return null; }

        //override from java.util.concurrent.BlockingDeque, java.util.concurrent.BlockingQueue
        @Modified
        void put(@Independent @NotModified E e) { }

        //override from java.util.concurrent.BlockingDeque
        @Modified
        void putFirst(@Independent @NotModified E e) { }

        //override from java.util.concurrent.BlockingDeque
        @Modified
        void putLast(@Independent @NotModified E e) { }

        //override from java.util.concurrent.BlockingQueue
        @Modified
        int remainingCapacity() { return 0; }

        //override from java.util.concurrent.BlockingDeque, java.util.concurrent.BlockingQueue
        @Independent @Modified
        E take() { return null; }

        //override from java.util.concurrent.BlockingDeque
        @Independent @Modified
        E takeFirst() { return null; }

        //override from java.util.concurrent.BlockingDeque
        @Independent @Modified
        E takeLast() { return null; }
    }

    //public abstract class ForwardingCollection extends ForwardingObject implements Collection<E>
    @FinalFields
    @Independent(absent = true)
    class ForwardingCollection$<E> {
        //override from java.util.Collection
        @Modified
        boolean add(@Independent @NotModified E element) { return false; }

        //override from java.util.Collection
        @Modified
        boolean addAll(@Independent @NotModified Collection<? extends E> collection) { return false; }

        //override from java.util.Collection
        @Modified
        void clear() { }

        //override from java.util.Collection
        @NotModified
        boolean contains(@Independent @NotModified Object object) { return false; }

        //override from java.util.Collection
        @NotModified
        boolean containsAll(@Independent @NotModified Collection<?> collection) { return false; }

        //override from java.util.Collection
        @NotModified
        boolean isEmpty() { return false; }

        //override from java.lang.Iterable, java.util.Collection
        @Independent @NotModified
        Iterator<E> iterator() { return null; }

        //override from java.util.Collection
        @Modified
        boolean remove(@Independent @NotModified Object object) { return false; }

        //override from java.util.Collection
        @Modified
        boolean removeAll(@Independent @NotModified Collection<?> collection) { return false; }

        //override from java.util.Collection
        @Modified
        boolean retainAll(@Independent @NotModified Collection<?> collection) { return false; }

        //override from java.util.Collection
        @NotModified
        int size() { return 0; }

        //override from java.util.Collection
        @Independent @NotModified
        Object [] toArray() { return null; }

        //override from java.util.Collection
        @Independent @NotModified
        <T> T [] toArray(@Independent @NotModified T [] array) { return null; }
    }

    //public abstract class ForwardingConcurrentMap extends ForwardingMap<K,V> implements ConcurrentMap<K,V>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingConcurrentMap$<K, V> {
        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        @Independent @Modified
        V putIfAbsent(@Independent @NotModified K key, @Independent @NotModified V value) { return null; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        @Modified
        boolean remove(@Independent @NotModified Object key, @Independent @NotModified Object value) { return false; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        @Independent @Modified
        V replace(@Independent @NotModified K key, @Independent @NotModified V value) { return null; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        @Modified

        boolean replace(
            @Independent @NotModified K key,
            @Independent @NotModified V oldValue,
            @Independent @NotModified V newValue) { return false; }
    }

    //public abstract class ForwardingDeque extends ForwardingQueue<E> implements Deque<E>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingDeque$<E> {
        //override from java.util.Deque, java.util.SequencedCollection
        @Modified
        void addFirst(@Independent @NotModified E e) { }

        //override from java.util.Deque, java.util.SequencedCollection
        @Modified
        void addLast(@Independent @NotModified E e) { }

        //override from java.util.Deque
        @Independent @NotModified
        Iterator<E> descendingIterator() { return null; }

        //override from java.util.Deque, java.util.SequencedCollection
        @Independent @Modified
        E getFirst() { return null; }

        //override from java.util.Deque, java.util.SequencedCollection
        @Independent @Modified
        E getLast() { return null; }

        //override from java.util.Deque
        @Modified
        boolean offerFirst(@Independent @NotModified E e) { return false; }

        //override from java.util.Deque
        @Modified
        boolean offerLast(@Independent @NotModified E e) { return false; }

        //override from java.util.Deque
        @Independent @Modified
        E peekFirst() { return null; }

        //override from java.util.Deque
        @Independent @Modified
        E peekLast() { return null; }

        //override from java.util.Deque
        @Independent @Modified
        E pollFirst() { return null; }

        //override from java.util.Deque
        @Independent @Modified
        E pollLast() { return null; }

        //override from java.util.Deque
        @Independent @Modified
        E pop() { return null; }

        //override from java.util.Deque
        @Modified
        void push(@Independent @NotModified E e) { }

        //override from java.util.Deque, java.util.SequencedCollection
        @Independent @Modified
        E removeFirst() { return null; }

        //override from java.util.Deque
        @Modified
        boolean removeFirstOccurrence(@Independent @NotModified Object o) { return false; }

        //override from java.util.Deque, java.util.SequencedCollection
        @Independent @Modified
        E removeLast() { return null; }

        //override from java.util.Deque
        @Modified
        boolean removeLastOccurrence(@Independent @NotModified Object o) { return false; }
    }

    //public abstract class ForwardingIterator extends ForwardingObject implements Iterator<T>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingIterator$<T> {
        //override from java.util.Iterator
        @Modified
        boolean hasNext() { return false; }

        //override from java.util.Iterator
        @Independent @Modified
        T next() { return null; }

        //override from java.util.Iterator
        @Modified
        void remove() { }
    }

    //public abstract class ForwardingList extends ForwardingCollection<E> implements List<E>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingList$<E> {
        //override from java.util.List
        @NotModified
        void add(int index, @Independent @NotModified E element) { }

        //override from java.util.List
        @NotModified
        boolean addAll(int index, @Independent @NotModified Collection<? extends E> elements) { return false; }

        //override from java.lang.Object, java.util.Collection, java.util.List
        @NotModified
        public boolean equals(@Independent @NotModified Object object) { return false; }

        //override from java.util.List
        @Independent @NotModified
        E get(int index) { return null; }

        //override from java.lang.Object, java.util.Collection, java.util.List
        @NotModified
        public int hashCode() { return 0; }

        //override from java.util.List
        @NotModified
        int indexOf(@Independent @NotModified Object element) { return 0; }

        //override from java.util.List
        @NotModified
        int lastIndexOf(@Independent @NotModified Object element) { return 0; }

        //override from java.util.List
        @Independent @NotModified
        ListIterator<E> listIterator() { return null; }

        //override from java.util.List
        @Independent @NotModified
        ListIterator<E> listIterator(int index) { return null; }

        //override from java.util.List
        @Independent @NotModified
        E remove(int index) { return null; }

        //override from java.util.List
        @Independent @NotModified
        E set(int index, @Independent @NotModified E element) { return null; }

        //override from java.util.List
        @Independent @NotModified
        List<E> subList(int fromIndex, int toIndex) { return null; }
    }

    //public abstract class ForwardingListIterator extends ForwardingIterator<E> implements ListIterator<E>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingListIterator$<E> {
        //override from java.util.ListIterator
        @Modified
        void add(@Independent @NotModified E element) { }

        //override from java.util.ListIterator
        @Modified
        boolean hasPrevious() { return false; }

        //override from java.util.ListIterator
        @Modified
        int nextIndex() { return 0; }

        //override from java.util.ListIterator
        @Independent @Modified
        E previous() { return null; }

        //override from java.util.ListIterator
        @Modified
        int previousIndex() { return 0; }

        //override from java.util.ListIterator
        @Modified
        void set(@Independent @NotModified E element) { }
    }

    //public abstract class ForwardingListMultimap extends ForwardingMultimap<K,V> implements ListMultimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class ForwardingListMultimap$<K, V> {
        //override from com.google.common.collect.ForwardingMultimap, com.google.common.collect.ListMultimap, com.google.common.collect.Multimap
        @Independent @NotModified
        List<V> get(@Independent @Modified K key) { return null; }

        //override from com.google.common.collect.ForwardingMultimap, com.google.common.collect.ListMultimap, com.google.common.collect.Multimap
        @Independent @Modified
        List<V> removeAll(@Independent @Modified Object key) { return null; }

        //override from com.google.common.collect.ForwardingMultimap, com.google.common.collect.ListMultimap, com.google.common.collect.Multimap
        @Independent @Modified

        List<V> replaceValues(@Independent @Modified K key, @Independent @Modified Iterable<? extends V> values) {
            return null;
        }
    }

    //public abstract class ForwardingMap extends ForwardingObject implements Map<K,V>
    @FinalFields
    @Independent(absent = true)
    class ForwardingMap$<K, V> {
        //override from java.util.Map
        @Modified
        void clear() { }

        //override from java.util.Map
        @NotModified
        boolean containsKey(@Independent @NotModified Object key) { return false; }

        //override from java.util.Map
        @NotModified
        boolean containsValue(@Independent @NotModified Object value) { return false; }

        //override from java.util.Map
        @Independent @NotModified
        Set<Map.Entry<K, V>> entrySet() { return null; }

        //override from java.lang.Object, java.util.Map
        @NotModified
        public boolean equals(@Independent @NotModified Object object) { return false; }

        //override from java.util.Map
        @Independent @NotModified
        V get(@Independent @NotModified Object key) { return null; }

        //override from java.lang.Object, java.util.Map
        @NotModified
        public int hashCode() { return 0; }

        //override from java.util.Map
        @NotModified
        boolean isEmpty() { return false; }

        //override from java.util.Map
        @Independent @NotModified
        Set<K> keySet() { return null; }

        //override from java.util.Map
        @Independent @Modified
        V put(@Independent @NotModified K key, @Independent @NotModified V value) { return null; }

        //override from java.util.Map
        @Modified
        void putAll(@Independent @Modified Map<? extends K, ? extends V> map) { }

        //override from java.util.Map
        @Independent @Modified
        V remove(@Independent @NotModified Object key) { return null; }

        //override from java.util.Map
        @NotModified
        int size() { return 0; }

        //override from java.util.Map
        @Independent @NotModified
        Collection<V> values() { return null; }
    }

    //public abstract class ForwardingMapEntry extends ForwardingObject implements Map.Entry<K,V>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingMapEntry$<K, V> {
        //override from java.lang.Object, java.util.Map.Entry
        @NotModified
        public boolean equals(@Independent @NotModified Object object) { return false; }

        //override from java.util.Map.Entry
        @Independent @NotModified
        K getKey() { return null; }

        //override from java.util.Map.Entry
        @Independent @NotModified
        V getValue() { return null; }

        //override from java.lang.Object, java.util.Map.Entry
        @NotModified
        public int hashCode() { return 0; }

        //override from java.util.Map.Entry
        @Independent @Modified
        V setValue(@Independent @NotModified V value) { return null; }
    }

    //public abstract class ForwardingMultimap extends ForwardingObject implements Multimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class ForwardingMultimap$<K, V> {
        //override from com.google.common.collect.Multimap
        @Independent @NotModified
        Map<K, Collection<V>> asMap() { return null; }

        //override from com.google.common.collect.Multimap
        @Modified
        void clear() { }

        //override from com.google.common.collect.Multimap
        @NotModified

        boolean containsEntry(@Independent @NotModified Object key, @Independent @NotModified Object value) {
            return false;
        }

        //override from com.google.common.collect.Multimap
        @NotModified
        boolean containsKey(@Independent @Modified Object key) { return false; }

        //override from com.google.common.collect.Multimap
        @NotModified
        boolean containsValue(@Independent @NotModified Object value) { return false; }

        //override from com.google.common.collect.Multimap
        @Independent @NotModified
        Collection<Map.Entry<K, V>> entries() { return null; }

        //override from com.google.common.collect.Multimap, java.lang.Object
        @NotModified
        public boolean equals(@Independent @Modified Object object) { return false; }

        //override from com.google.common.collect.Multimap
        @Independent @NotModified
        Collection<V> get(@Independent @Modified K key) { return null; }

        //override from com.google.common.collect.Multimap, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from com.google.common.collect.Multimap
        @NotModified
        boolean isEmpty() { return false; }

        //override from com.google.common.collect.Multimap
        @Independent @NotModified
        Set<K> keySet() { return null; }

        //override from com.google.common.collect.Multimap
        @Independent @NotModified
        Multiset<K> keys() { return null; }

        //override from com.google.common.collect.Multimap
        @Modified
        boolean put(@Independent @Modified K key, @Independent @NotModified V value) { return false; }

        //override from com.google.common.collect.Multimap
        @Modified
        boolean putAll(@Independent @Modified K key, @Independent @Modified Iterable<? extends V> values) { return false; }

        //override from com.google.common.collect.Multimap
        @Modified
        boolean putAll(@Independent @Modified Multimap<? extends K, ? extends V> multimap) { return false; }

        //override from com.google.common.collect.Multimap
        @Modified
        boolean remove(@Independent @Modified Object key, @Independent @NotModified Object value) { return false; }

        //override from com.google.common.collect.Multimap
        @Independent @Modified
        Collection<V> removeAll(@Independent @Modified Object key) { return null; }

        //override from com.google.common.collect.Multimap
        @Independent @Modified

        Collection<V> replaceValues(@Independent @Modified K key, @Independent @Modified Iterable<? extends V> values) {
            return null;
        }

        //override from com.google.common.collect.Multimap
        @NotModified
        int size() { return 0; }

        //override from com.google.common.collect.Multimap
        @Independent @NotModified
        Collection<V> values() { return null; }
    }

    //public abstract class ForwardingMultiset extends ForwardingCollection<E> implements Multiset<E>
    @FinalFields
    @Independent(absent = true)
    class ForwardingMultiset$<E> {
        //override from com.google.common.collect.Multiset
        @Modified
        int add(@Independent @Modified E element, int occurrences) { return 0; }

        //override from com.google.common.collect.Multiset
        @NotModified
        int count(@Independent @Modified Object element) { return 0; }

        //override from com.google.common.collect.Multiset
        @Independent @NotModified
        Set<E> elementSet() { return null; }

        //override from com.google.common.collect.Multiset
        @Independent @NotModified
        Set<Multiset.Entry<E>> entrySet() { return null; }

        //override from com.google.common.collect.Multiset, java.lang.Object, java.util.Collection
        @NotModified
        public boolean equals(@Independent @Modified Object object) { return false; }

        //override from com.google.common.collect.Multiset, java.lang.Object, java.util.Collection
        @NotModified
        public int hashCode() { return 0; }

        //override from com.google.common.collect.Multiset
        @Modified
        int remove(@Independent @Modified Object element, int occurrences) { return 0; }

        //override from com.google.common.collect.Multiset
        @Modified
        int setCount(@Independent @Modified E element, int count) { return 0; }

        //override from com.google.common.collect.Multiset
        @Modified
        boolean setCount(@Independent @Modified E element, int oldCount, int newCount) { return false; }
    }

    //public abstract class ForwardingNavigableMap extends ForwardingSortedMap<K,V> implements NavigableMap<K,V>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingNavigableMap$<K, V> {
        //override from java.util.NavigableMap
        @Independent @Modified
        Map.Entry<K, V> ceilingEntry(@Independent @NotModified K key) { return null; }

        //override from java.util.NavigableMap
        @Independent @Modified
        K ceilingKey(@Independent @NotModified K key) { return null; }

        //override from java.util.NavigableMap
        @Independent @Modified
        NavigableSet<K> descendingKeySet() { return null; }

        //override from java.util.NavigableMap
        @Independent @NotModified
        NavigableMap<K, V> descendingMap() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        @Independent @Modified
        Map.Entry<K, V> firstEntry() { return null; }

        //override from java.util.NavigableMap
        @Independent @Modified
        Map.Entry<K, V> floorEntry(@Independent @NotModified K key) { return null; }

        //override from java.util.NavigableMap
        @Independent @Modified
        K floorKey(@Independent @NotModified K key) { return null; }

        //override from java.util.NavigableMap
        @Independent @NotModified
        NavigableMap<K, V> headMap(@Independent @NotModified K toKey, boolean inclusive) { return null; }

        //override from java.util.NavigableMap
        @Independent @Modified
        Map.Entry<K, V> higherEntry(@Independent @NotModified K key) { return null; }

        //override from java.util.NavigableMap
        @Independent @Modified
        K higherKey(@Independent @NotModified K key) { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        @Independent @Modified
        Map.Entry<K, V> lastEntry() { return null; }

        //override from java.util.NavigableMap
        @Independent @Modified
        Map.Entry<K, V> lowerEntry(@Independent @NotModified K key) { return null; }

        //override from java.util.NavigableMap
        @Independent @Modified
        K lowerKey(@Independent @NotModified K key) { return null; }

        //override from java.util.NavigableMap
        @Independent @Modified
        NavigableSet<K> navigableKeySet() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        @Independent @Modified
        Map.Entry<K, V> pollFirstEntry() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        @Independent @Modified
        Map.Entry<K, V> pollLastEntry() { return null; }

        //override from java.util.NavigableMap
        @Independent @NotModified

        NavigableMap<K, V> subMap(
            @Independent @NotModified K fromKey,
            boolean fromInclusive,
            @Independent @NotModified K toKey,
            boolean toInclusive) { return null; }

        //override from java.util.NavigableMap
        @Independent @NotModified
        NavigableMap<K, V> tailMap(@Independent @NotModified K fromKey, boolean inclusive) { return null; }
    }

    //public abstract class ForwardingNavigableSet extends ForwardingSortedSet<E> implements NavigableSet<E>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingNavigableSet$<E> {
        //override from java.util.NavigableSet
        @Independent @NotModified
        E ceiling(@Independent @NotModified E e) { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        Iterator<E> descendingIterator() { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        NavigableSet<E> descendingSet() { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        E floor(@Independent @NotModified E e) { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        NavigableSet<E> headSet(@Independent @NotModified E toElement, boolean inclusive) { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        E higher(@Independent @NotModified E e) { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        E lower(@Independent @NotModified E e) { return null; }

        //override from java.util.NavigableSet
        @Independent @Modified
        E pollFirst() { return null; }

        //override from java.util.NavigableSet
        @Independent @Modified
        E pollLast() { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified

        NavigableSet<E> subSet(
            @Independent @NotModified E fromElement,
            boolean fromInclusive,
            @Independent @NotModified E toElement,
            boolean toInclusive) { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        NavigableSet<E> tailSet(@Independent @NotModified E fromElement, boolean inclusive) { return null; }
    }

    //public abstract class ForwardingObject
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingObject$ {
        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }
    }

    //public abstract class ForwardingQueue extends ForwardingCollection<E> implements Queue<E>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingQueue$<E> {
        //override from java.util.Queue
        @Independent @NotModified
        E element() { return null; }

        //override from java.util.Queue
        @Modified
        boolean offer(@Independent @NotModified E o) { return false; }

        //override from java.util.Queue
        @Independent @NotModified
        E peek() { return null; }

        //override from java.util.Queue
        @Independent @Modified
        E poll() { return null; }

        //override from java.util.Queue
        @Independent @Modified
        E remove() { return null; }
    }

    //public abstract class ForwardingSet extends ForwardingCollection<E> implements Set<E>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingSet$<E> {
        //override from java.lang.Object, java.util.Collection, java.util.Set
        @NotModified
        public boolean equals(@Independent @NotModified Object object) { return false; }

        //override from java.lang.Object, java.util.Collection, java.util.Set
        @NotModified
        public int hashCode() { return 0; }
    }

    //public abstract class ForwardingSetMultimap extends ForwardingMultimap<K,V> implements SetMultimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class ForwardingSetMultimap$<K, V> {
        ForwardingSetMultimap$() { }
        //override from com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent @NotModified
        Set<Map.Entry<K, V>> entries() { return null; }

        //override from com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent @NotModified
        Set<V> get(@Independent @Modified K key) { return null; }

        //override from com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent @Modified
        Set<V> removeAll(@Independent @Modified Object key) { return null; }

        //override from com.google.common.collect.ForwardingMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent @Modified

        Set<V> replaceValues(@Independent @Modified K key, @Independent @Modified Iterable<? extends V> values) {
            return null;
        }
    }

    //public abstract class ForwardingSortedMap extends ForwardingMap<K,V> implements SortedMap<K,V>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingSortedMap$<K, V> {
        //override from java.util.SortedMap
        @Independent @NotModified
        Comparator<? super K> comparator() { return null; }

        //override from java.util.SortedMap
        @Independent @NotModified
        K firstKey() { return null; }

        //override from java.util.SortedMap
        @Independent @NotModified
        SortedMap<K, V> headMap(@Independent @NotModified K toKey) { return null; }

        //override from java.util.SortedMap
        @Independent @NotModified
        K lastKey() { return null; }

        //override from java.util.SortedMap
        @Independent @NotModified
        SortedMap<K, V> subMap(@Independent @NotModified K fromKey, @Independent @NotModified K toKey) { return null; }

        //override from java.util.SortedMap
        @Independent @NotModified
        SortedMap<K, V> tailMap(@Independent @NotModified K fromKey) { return null; }
    }

    //public abstract class ForwardingSortedMultiset extends ForwardingMultiset<E> implements SortedMultiset<E>
    @FinalFields
    @Independent(absent = true)
    class ForwardingSortedMultiset$<E> {
        //override from com.google.common.collect.SortedIterable, com.google.common.collect.SortedMultiset
        @Independent @NotModified
        Comparator<? super E> comparator() { return null; }

        //override from com.google.common.collect.SortedMultiset
        @Independent @Modified
        SortedMultiset<E> descendingMultiset() { return null; }

        //override from com.google.common.collect.ForwardingMultiset, com.google.common.collect.Multiset, com.google.common.collect.SortedMultiset, com.google.common.collect.SortedMultisetBridge
        @Independent @NotModified
        NavigableSet<E> elementSet() { return null; }

        //override from com.google.common.collect.SortedMultiset
        @Independent @Modified
        Multiset.Entry<E> firstEntry() { return null; }

        //override from com.google.common.collect.SortedMultiset
        @Independent @Modified

        SortedMultiset<E> headMultiset(
            @Independent @NotModified E upperBound,
            @Independent @NotModified BoundType boundType) { return null; }

        //override from com.google.common.collect.SortedMultiset
        @Independent @Modified
        Multiset.Entry<E> lastEntry() { return null; }

        //override from com.google.common.collect.SortedMultiset
        @Independent @Modified
        Multiset.Entry<E> pollFirstEntry() { return null; }

        //override from com.google.common.collect.SortedMultiset
        @Independent @Modified
        Multiset.Entry<E> pollLastEntry() { return null; }

        //override from com.google.common.collect.SortedMultiset
        @Independent @Modified

        SortedMultiset<E> subMultiset(
            @Independent @NotModified E lowerBound,
            @Independent @NotModified BoundType lowerBoundType,
            @Independent @NotModified E upperBound,
            @Independent @NotModified BoundType upperBoundType) { return null; }

        //override from com.google.common.collect.SortedMultiset
        @Independent @Modified

        SortedMultiset<E> tailMultiset(
            @Independent @NotModified E lowerBound,
            @Independent @NotModified BoundType boundType) { return null; }
    }

    //public abstract class ForwardingSortedSet extends ForwardingSet<E> implements SortedSet<E>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ForwardingSortedSet$<E> {
        //override from java.util.SortedSet
        @Independent @NotModified
        Comparator<? super E> comparator() { return null; }

        //override from java.util.SortedSet
        @Independent @NotModified
        E first() { return null; }

        //override from java.util.SortedSet
        @Independent @NotModified
        SortedSet<E> headSet(@Independent @NotModified E toElement) { return null; }

        //override from java.util.SortedSet
        @Independent @NotModified
        E last() { return null; }

        //override from java.util.SortedSet
        @Independent @NotModified

        SortedSet<E> subSet(@Independent @NotModified E fromElement, @Independent @NotModified E toElement) {
            return null;
        }

        //override from java.util.SortedSet
        @Independent @NotModified
        SortedSet<E> tailSet(@Independent @NotModified E fromElement) { return null; }
    }

    //public abstract class ForwardingSortedSetMultimap extends ForwardingSetMultimap<K,V> implements SortedSetMultimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class ForwardingSortedSetMultimap$<K, V> {
        //override from com.google.common.collect.ForwardingMultimap, com.google.common.collect.ForwardingSetMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap, com.google.common.collect.SortedSetMultimap
        @Independent @NotModified
        SortedSet<V> get(@Independent @NotModified K key) { return null; }

        //override from com.google.common.collect.ForwardingMultimap, com.google.common.collect.ForwardingSetMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap, com.google.common.collect.SortedSetMultimap
        @Independent @Modified
        SortedSet<V> removeAll(@Independent @NotModified Object key) { return null; }

        //override from com.google.common.collect.ForwardingMultimap, com.google.common.collect.ForwardingSetMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap, com.google.common.collect.SortedSetMultimap
        @Independent @Modified

        SortedSet<V> replaceValues(@Independent @NotModified K key, @Independent @Modified Iterable<? extends V> values) {
            return null;
        }

        //override from com.google.common.collect.SortedSetMultimap
        @Independent @Modified
        Comparator<? super V> valueComparator() { return null; }
    }

    //public abstract class ForwardingTable extends ForwardingObject implements Table<R,C,V>
    @FinalFields
    @Independent(absent = true)
    class ForwardingTable$<R, C, V> {
        //override from com.google.common.collect.Table
        @Independent @NotModified
        Set<Table.Cell<R, C, V>> cellSet() { return null; }

        //override from com.google.common.collect.Table
        @Modified @NotModified(after = "delegate")
        void clear() { }

        //override from com.google.common.collect.Table
        @Independent @NotModified
        Map<R, V> column(@Independent @NotModified C columnKey) { return null; }

        //override from com.google.common.collect.Table
        @Independent @NotModified
        Set<C> columnKeySet() { return null; }

        //override from com.google.common.collect.Table
        @Independent @NotModified
        Map<C, Map<R, V>> columnMap() { return null; }

        //override from com.google.common.collect.Table
        @NotModified

        boolean contains(@Independent @NotModified Object rowKey, @Independent @NotModified Object columnKey) {
            return false;
        }

        //override from com.google.common.collect.Table
        @NotModified
        boolean containsColumn(@Independent @NotModified Object columnKey) { return false; }

        //override from com.google.common.collect.Table
        @NotModified
        boolean containsRow(@Independent @NotModified Object rowKey) { return false; }

        //override from com.google.common.collect.Table
        @NotModified
        boolean containsValue(@Independent @Modified Object value) { return false; }

        //override from com.google.common.collect.Table, java.lang.Object
        @NotModified
        public boolean equals(@Independent @Modified Object obj) { return false; }

        //override from com.google.common.collect.Table
        @Independent @NotModified
        V get(@Independent @NotModified Object rowKey, @Independent @NotModified Object columnKey) { return null; }

        //override from com.google.common.collect.Table, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from com.google.common.collect.Table
        @NotModified
        boolean isEmpty() { return false; }

        //override from com.google.common.collect.Table
        @Independent @Modified @NotModified(after = "delegate")

        V put(
            @Independent @NotModified R rowKey,
            @Independent @NotModified C columnKey,
            @Independent @NotModified V value) { return null; }

        //override from com.google.common.collect.Table
        @Modified @NotModified(after = "delegate")
        void putAll(@Independent @Modified Table<? extends R, ? extends C, ? extends V> table) { }

        //override from com.google.common.collect.Table
        @Independent @Modified @NotModified(after = "delegate")
        V remove(@Independent @NotModified Object rowKey, @Independent @NotModified Object columnKey) { return null; }

        //override from com.google.common.collect.Table
        @Independent @NotModified
        Map<C, V> row(@Independent @NotModified R rowKey) { return null; }

        //override from com.google.common.collect.Table
        @Independent @NotModified
        Set<R> rowKeySet() { return null; }

        //override from com.google.common.collect.Table
        @Independent @NotModified
        Map<R, Map<C, V>> rowMap() { return null; }

        //override from com.google.common.collect.Table
        @NotModified
        int size() { return 0; }

        //override from com.google.common.collect.Table
        @Independent @NotModified
        Collection<V> values() { return null; }
    }

    //public class HashBasedTable extends StandardTable<R,C,V>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class HashBasedTable$<R, C, V> {
        @Independent @NotModified static <R, C, V> HashBasedTable<R, C, V> create() { return null; }
        @Independent @NotModified
        static <R, C, V> HashBasedTable<R, C, V> create(
            @Independent @NotModified Table<? extends R, ? extends C, ? extends V> table) { return null; }

        @Independent @NotModified
        static <R, C, V> HashBasedTable<R, C, V> create(int expectedRows, int expectedCellsPerRow) { return null; }
    }

    //public final class HashBiMap extends IteratorBasedAbstractMap<K,V> implements BiMap<K,V>, Serializable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class HashBiMap$<K, V> {
        //override from com.google.common.collect.Maps.IteratorBasedAbstractMap, java.util.AbstractMap, java.util.Map
        @Modified
        void clear() { }

        //override from java.util.AbstractMap, java.util.Map
        @NotModified
        boolean containsKey(@Independent @NotModified Object key) { return false; }

        //override from java.util.AbstractMap, java.util.Map
        @NotModified
        boolean containsValue(@Independent @NotModified Object value) { return false; }
        @Independent @NotModified static <K, V> HashBiMap<K, V> create() { return null; }
        @Independent @NotModified static <K, V> HashBiMap<K, V> create(int expectedSize) { return null; }
        @Independent @NotModified
        static <K, V> HashBiMap<K, V> create(@Independent @NotModified Map<? extends K, ? extends V> map) { return null; }

        //override from java.util.Map
        @Modified
        void forEach(@Independent @NotModified BiConsumer<? super K, ? super V> action) { }

        //override from com.google.common.collect.BiMap
        @Independent @Modified
        V forcePut(@Independent @NotModified K key, @Independent @NotModified V value) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        @Independent @NotModified
        V get(@Independent @NotModified Object key) { return null; }

        //override from com.google.common.collect.BiMap
        @Fluent @Independent @NotModified
        BiMap<V, K> inverse() { return null; }

        //override from java.util.AbstractMap, java.util.Map
        @Independent @NotModified
        Set<K> keySet() { return null; }

        //override from com.google.common.collect.BiMap, java.util.AbstractMap, java.util.Map
        @Independent @Modified
        V put(@Independent @NotModified K key, @Independent @NotModified V value) { return null; }

        //override from java.util.AbstractMap, java.util.Map
        @Independent @Modified
        V remove(@Independent @NotModified Object key) { return null; }

        //override from java.util.Map
        @Modified
        void replaceAll(@Independent @NotModified BiFunction<? super K, ? super V, ? extends V> function) { }

        //override from com.google.common.collect.Maps.IteratorBasedAbstractMap, java.util.AbstractMap, java.util.Map
        @NotModified @GetSet("size")
        int size() { return 0; }

        //override from com.google.common.collect.BiMap, java.util.AbstractMap, java.util.Map
        @Independent(hc = true) @NotModified
        Set<V> values() { return null; }
    }

    //public final class HashMultimap extends AbstractSetMultimap<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class HashMultimap$<K, V> {
        @Independent @NotModified static <K, V> HashMultimap<K, V> create() { return null; }
        @Independent @NotModified
        static <K, V> HashMultimap<K, V> create(@Independent @NotModified Multimap<? extends K, ? extends V> multimap) {
            return null;
        }

        @Independent @NotModified
        static <K, V> HashMultimap<K, V> create(int expectedKeys, int expectedValuesPerKey) { return null; }
    }

    //public final class HashMultiset extends AbstractMapBasedMultiset<E>
    //annotated as EXPECTED; computed mutable @Dependent @Container (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class HashMultiset$<E> {
        @Independent @NotModified static <E> HashMultiset<E> create() { return null; }
        @Independent @NotModified
        static <E> HashMultiset<E> create(@Independent @NotModified Iterable<? extends E> elements) { return null; }
        @Independent @NotModified static <E> HashMultiset<E> create(int distinctElements) { return null; }
    }

    //public abstract class ImmutableBiMap extends ImmutableMap<K,V> implements BiMap<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableBiMap$<K, V> {
        //public static final class Builder extends Builder<K,V>
        //annotated as EXPECTED; computed mutable @Dependent @Container (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<K, V> {
            Builder() { }
            //override from com.google.common.collect.ImmutableMap.Builder
            @Independent(absent = true) @Modified
            ImmutableBiMap<K, V> build() { return null; }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Independent @NotModified
            ImmutableBiMap<K, V> buildKeepingLast() { return null; }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Independent(absent = true) @Modified
            ImmutableBiMap<K, V> buildOrThrow() { return null; }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Fluent @Independent @Modified

            ImmutableBiMap.Builder<K, V> orderEntriesByValue(
                @Independent @NotModified Comparator<? super V> valueComparator) { return null; }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Fluent @Independent @Modified

            ImmutableBiMap.Builder<K, V> put(@Independent @NotModified K key, @Independent @NotModified V value) {
                return null;
            }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Fluent @Independent @Modified

            ImmutableBiMap.Builder<K, V> put(@Independent @NotModified Map.Entry<? extends K, ? extends V> entry) {
                return null;
            }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Fluent @Independent @Modified

            ImmutableBiMap.Builder<K, V> putAll(
                @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
                return null;
            }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Fluent @Independent @Modified

            ImmutableBiMap.Builder<K, V> putAll(@Independent @NotModified Map<? extends K, ? extends V> map) {
                return null;
            }
        }
        @Independent @NotModified static <K, V> ImmutableBiMap.Builder<K, V> builder() { return null; }
        @Independent @NotModified
        static <K, V> ImmutableBiMap.Builder<K, V> builderWithExpectedSize(int expectedSize) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> copyOf(
            @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> copyOf(@Independent @NotModified Map<? extends K, ? extends V> map) {
            return null;
        }

        //override from com.google.common.collect.BiMap
        @Independent @NotModified
        V forcePut(@Independent @NotModified K key, @Independent @NotModified V value) { return null; }

        //override from com.google.common.collect.BiMap
        @Independent(hc = true) @NotModified
        ImmutableBiMap<V, K> inverse() { return null; }
        @Independent @NotModified static <K, V> ImmutableBiMap<K, V> of() { return null; }
        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> of(@Independent @NotModified K k1, @Independent @NotModified V v1) {
            return null;
        }

        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8,
            @Independent @NotModified K k9,
            @Independent @NotModified V v9) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8,
            @Independent @NotModified K k9,
            @Independent @NotModified V v9,
            @Independent @NotModified K k10,
            @Independent @NotModified V v10) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableBiMap<K, V> ofEntries(
            @Independent @NotModified Map.Entry<? extends K, ? extends V> ... entries) { return null; }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableBiMap<K, V>> toImmutableBiMap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction) { return null; }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction) { return null; }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction,
            @Independent @NotModified BinaryOperator<V> mergeFunction) { return null; }

        //override from com.google.common.collect.BiMap, com.google.common.collect.ImmutableMap, java.util.Map
        @Independent @NotModified
        ImmutableSet<V> values() { return null; }
    }

    //public final class ImmutableClassToInstanceMap extends ForwardingMap<Class<? extends B>,B> implements ClassToInstanceMap<B>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableClassToInstanceMap$<B> {
        //public static final class Builder
        //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<B> {
            Builder() { }
            @Independent(absent = true) @Modified ImmutableClassToInstanceMap<B> build() { return null; }
            @Fluent @Independent @Modified
            <T extends B> ImmutableClassToInstanceMap.Builder<B> put(Class<T> key, @Independent @NotModified T value) {
                return null;
            }

            @Fluent @Independent @Modified
            <T extends B> ImmutableClassToInstanceMap.Builder<B> putAll(
                @Independent @NotModified Map<? extends Class<? extends T>, ? extends T> map) { return null; }
        }
        @Independent @NotModified static <B> ImmutableClassToInstanceMap.Builder<B> builder() { return null; }
        @Independent @NotModified
        static <B, S extends B> ImmutableClassToInstanceMap<B> copyOf(
            @Independent @NotModified Map<? extends Class<? extends S>, ? extends S> map) { return null; }

        //override from com.google.common.collect.ClassToInstanceMap
        @Independent(hc = true) @NotModified
        <T extends B> T getInstance(Class<T> type) { return null; }
        @Independent @NotModified static <B> ImmutableClassToInstanceMap<B> of() { return null; }
        @Independent @NotModified
        static <B, T extends B> ImmutableClassToInstanceMap<B> of(Class<T> type, @Independent @NotModified T value) {
            return null;
        }

        //override from com.google.common.collect.ClassToInstanceMap
        @Independent @NotModified
        <T extends B> T putInstance(Class<T> type, @Independent @NotModified T value) { return null; }
    }

    //public abstract class ImmutableCollection extends AbstractCollection<E> implements Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableCollection$<E> {
        //public abstract static class Builder
        //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<E> {
            @Independent @Modified
            ImmutableCollection.Builder<E> add(@Independent(hc = true) @NotModified E element) { return null; }

            @Fluent @Independent @Modified
            ImmutableCollection.Builder<E> add(@Independent @NotModified E ... elements) { return null; }

            @Fluent @Independent @Modified
            ImmutableCollection.Builder<E> addAll(@Independent @NotModified Iterable<? extends E> elements) {
                return null;
            }

            @Fluent @Independent @Modified
            ImmutableCollection.Builder<E> addAll(@Independent @NotModified Iterator<? extends E> elements) {
                return null;
            }
            @Independent(absent = true) @Modified ImmutableCollection<E> build() { return null; }
        }

        //override from java.util.AbstractCollection, java.util.Collection
        @NotModified
        boolean add(@Independent @NotModified E e) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection
        @NotModified
        boolean addAll(@Independent @NotModified Collection<? extends E> newElements) { return false; }
        @Independent @NotModified ImmutableList<E> asList() { return null; }
        //override from java.util.AbstractCollection, java.util.Collection
        @NotModified
        void clear() { }

        //override from java.util.AbstractCollection, java.util.Collection
        @NotModified
        boolean contains(@Independent @NotModified Object object) { return false; }

        //override from java.lang.Iterable, java.util.AbstractCollection, java.util.Collection
        @Independent(hc = true) @NotModified @NotNull
        UnmodifiableIterator<E> iterator() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection
        @NotModified
        boolean remove(@Independent @NotModified Object object) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection
        @NotModified
        boolean removeAll(@Independent @NotModified Collection<?> oldElements) { return false; }

        //override from java.util.Collection
        @NotModified
        boolean removeIf(@Independent @NotModified java.util.function.Predicate<? super E> filter) { return false; }

        //override from java.util.AbstractCollection, java.util.Collection
        @NotModified
        boolean retainAll(@Independent @NotModified Collection<?> elementsToKeep) { return false; }

        //override from java.lang.Iterable, java.util.Collection
        @Independent @NotModified
        Spliterator<E> spliterator() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection
        @Independent @NotModified
        Object [] toArray() { return null; }

        //override from java.util.AbstractCollection, java.util.Collection
        @Independent @NotModified
        <T> T [] toArray(@Independent @NotModified T [] other) { return null; }
    }

    //public abstract class ImmutableList extends ImmutableCollection<E> implements List<E>, RandomAccess
    //annotated as EXPECTED; computed @FinalFields @Dependent (1), mutable @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableList$<E> {
        //public static final class Builder extends Builder<E>
        //annotated as EXPECTED; computed mutable @Dependent (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<E> {
            Builder() { }
            //override from com.google.common.collect.ImmutableCollection.Builder
            @Fluent @Independent @Modified
            ImmutableList.Builder<E> add(@Independent(hc = true) @NotModified E element) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder
            @Fluent @Independent @Modified
            ImmutableList.Builder<E> add(@Independent @NotModified E ... elements) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder
            @Fluent @Independent @Modified
            ImmutableList.Builder<E> addAll(@Independent @NotModified Iterable<? extends E> elements) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder
            @Fluent @Independent @Modified
            ImmutableList.Builder<E> addAll(@Independent @NotModified Iterator<? extends E> elements) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder
            @Independent(absent = true) @Modified
            ImmutableList<E> build() { return null; }
        }

        //override from java.util.List
        @NotModified
        void add(int index, @Independent @NotModified E element) { }

        //override from java.util.List
        @NotModified
        boolean addAll(int index, @Independent @NotModified Collection<? extends E> newElements) { return false; }

        //override from com.google.common.collect.ImmutableCollection
        @Fluent @Independent @NotModified
        ImmutableList<E> asList() { return null; }
        @Independent @NotModified static <E> ImmutableList.Builder<E> builder() { return null; }
        @Independent @NotModified
        static <E> ImmutableList.Builder<E> builderWithExpectedSize(int expectedSize) { return null; }

        //override from com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.util.List
        @NotModified
        boolean contains(@Independent @NotModified Object object) { return false; }

        @Independent @NotModified
        static <E> ImmutableList<E> copyOf(@Independent @NotModified Iterable<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> copyOf(@Independent @NotModified E [] elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> copyOf(@Independent @NotModified Collection<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> copyOf(@Independent @NotModified Iterator<? extends E> elements) { return null; }

        //override from java.lang.Object, java.util.Collection, java.util.List
        @NotModified
        public boolean equals(@Independent @NotModified Object obj) { return false; }

        //override from java.lang.Iterable
        @NotModified
        void forEach(@Independent @NotModified Consumer<? super E> consumer) { }

        //override from java.lang.Object, java.util.Collection, java.util.List
        @NotModified
        public int hashCode() { return 0; }

        //override from java.util.List
        @NotModified
        int indexOf(@Independent @NotModified Object object) { return 0; }

        //override from com.google.common.collect.ImmutableCollection, java.lang.Iterable, java.util.AbstractCollection, java.util.Collection, java.util.List
        @Independent @NotModified
        UnmodifiableIterator<E> iterator() { return null; }

        //override from java.util.List
        @NotModified
        int lastIndexOf(@Independent @NotModified Object object) { return 0; }

        //override from java.util.List
        @Independent @NotModified
        UnmodifiableListIterator<E> listIterator() { return null; }

        //override from java.util.List
        @Independent @NotModified
        UnmodifiableListIterator<E> listIterator(int index) { return null; }
        @Independent @NotModified static <E> ImmutableList<E> of() { return null; }
        @Independent @NotModified static <E> ImmutableList<E> of(@Independent @NotModified E e1) { return null; }
        @Independent @NotModified
        static <E> ImmutableList<E> of(@Independent @NotModified E e1, @Independent @NotModified E e2) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5,
            @Independent @NotModified E e6) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5,
            @Independent @NotModified E e6,
            @Independent @NotModified E e7) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5,
            @Independent @NotModified E e6,
            @Independent @NotModified E e7,
            @Independent @NotModified E e8) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5,
            @Independent @NotModified E e6,
            @Independent @NotModified E e7,
            @Independent @NotModified E e8,
            @Independent @NotModified E e9) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5,
            @Independent @NotModified E e6,
            @Independent @NotModified E e7,
            @Independent @NotModified E e8,
            @Independent @NotModified E e9,
            @Independent @NotModified E e10) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5,
            @Independent @NotModified E e6,
            @Independent @NotModified E e7,
            @Independent @NotModified E e8,
            @Independent @NotModified E e9,
            @Independent @NotModified E e10,
            @Independent @NotModified E e11) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5,
            @Independent @NotModified E e6,
            @Independent @NotModified E e7,
            @Independent @NotModified E e8,
            @Independent @NotModified E e9,
            @Independent @NotModified E e10,
            @Independent @NotModified E e11,
            @Independent @NotModified E e12,
            @Independent @NotModified E ... others) { return null; }

        //override from java.util.List
        @Independent @NotModified
        E remove(int index) { return null; }

        //override from java.util.List
        @NotModified
        void replaceAll(@Independent @NotModified UnaryOperator<E> operator) { }
        @Fluent @Independent @NotModified ImmutableList<E> reverse() { return null; }
        //override from java.util.List
        @Independent @NotModified
        E set(int index, @Independent @NotModified E element) { return null; }

        //override from java.util.List
        @NotModified
        void sort(@Independent @NotModified Comparator<? super E> c) { }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableList<E> sortedCopyOf(
            @Independent @NotModified Iterable<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableList<E> sortedCopyOf(
            @Independent @NotModified Comparator<? super E> comparator,
            @Independent @NotModified Iterable<? extends E> elements) { return null; }

        //override from com.google.common.collect.ImmutableCollection, java.lang.Iterable, java.util.Collection, java.util.List
        @Independent @NotModified
        Spliterator<E> spliterator() { return null; }

        //override from java.util.List
        @Fluent @Independent @NotModified
        ImmutableList<E> subList(int fromIndex, int toIndex) { return null; }
        @Independent @NotModified static <E> Collector<E, ?, ImmutableList<E>> toImmutableList() { return null; }
    }

    //public class ImmutableListMultimap extends ImmutableMultimap<K,V> implements ListMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1), mutable @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableListMultimap$<K, V> {
        //public static final class Builder extends Builder<K,V>
        //annotated as EXPECTED; computed mutable @Dependent (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<K, V> {
            Builder() { }
            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Independent @Modified
            ImmutableListMultimap<K, V> build() { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified
            ImmutableListMultimap.Builder<K, V> expectedValuesPerKey(int expectedValuesPerKey) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableListMultimap.Builder<K, V> orderKeysBy(
                @Independent @NotModified Comparator<? super K> keyComparator) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableListMultimap.Builder<K, V> orderValuesBy(
                @Independent @NotModified Comparator<? super V> valueComparator) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableListMultimap.Builder<K, V> put(
                @Independent(hc = true) @NotModified K key,
                @Independent @NotModified V value) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableListMultimap.Builder<K, V> put(
                @Independent(hc = true) @NotModified Map.Entry<? extends K, ? extends V> entry) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableListMultimap.Builder<K, V> putAll(
                @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
                return null;
            }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableListMultimap.Builder<K, V> putAll(
                @Independent(hc = true) @NotModified K key,
                @Independent @NotModified Iterable<? extends V> values) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableListMultimap.Builder<K, V> putAll(
                @Independent(hc = true) @NotModified K key,
                @Independent @NotModified V ... values) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableListMultimap.Builder<K, V> putAll(
                @Independent @NotModified Multimap<? extends K, ? extends V> multimap) { return null; }
        }
        @Independent @NotModified static <K, V> ImmutableListMultimap.Builder<K, V> builder() { return null; }
        @Independent @NotModified
        static <K, V> ImmutableListMultimap.Builder<K, V> builderWithExpectedKeys(int expectedKeys) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableListMultimap<K, V> copyOf(
            @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableListMultimap<K, V> copyOf(
            @Independent @NotModified Multimap<? extends K, ? extends V> multimap) { return null; }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableListMultimap<K, V>> flatteningToImmutableListMultimap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends Stream<? extends V>>
                valuesFunction) { return null; }

        //override from com.google.common.collect.ImmutableMultimap, com.google.common.collect.ListMultimap, com.google.common.collect.Multimap
        @Independent(hc = true) @NotModified
        ImmutableList<V> get(@Independent @NotModified K key) { return null; }

        //override from com.google.common.collect.ImmutableMultimap
        @Fluent @Independent @NotModified
        ImmutableListMultimap<V, K> inverse() { return null; }
        @Independent @NotModified static <K, V> ImmutableListMultimap<K, V> of() { return null; }
        @Independent @NotModified
        static <K, V> ImmutableListMultimap<K, V> of(@Independent @NotModified K k1, @Independent @NotModified V v1) {
            return null;
        }

        @Independent @NotModified
        static <K, V> ImmutableListMultimap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableListMultimap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableListMultimap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableListMultimap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5) { return null; }

        //override from com.google.common.collect.ImmutableMultimap, com.google.common.collect.ListMultimap, com.google.common.collect.Multimap
        @Independent @NotModified
        ImmutableList<V> removeAll(@Independent @NotModified Object key) { return null; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.ImmutableMultimap, com.google.common.collect.ListMultimap, com.google.common.collect.Multimap
        @Independent @NotModified

        ImmutableList<V> replaceValues(
            @Independent @NotModified K key,
            @Independent @NotModified Iterable<? extends V> values) { return null; }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableListMultimap<K, V>> toImmutableListMultimap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction) { return null; }
    }

    //public abstract class ImmutableMap implements Map<K,V>, Serializable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableMap$<K, V> {
        //public static class Builder
        //annotated as EXPECTED; computed mutable @Dependent @Container (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<K, V> {
            Builder() { }
            @Independent(absent = true) @Modified ImmutableMap<K, V> build() { return null; }
            @Independent(absent = true) @Modified ImmutableMap<K, V> buildKeepingLast() { return null; }
            @Independent(absent = true) @Modified ImmutableMap<K, V> buildOrThrow() { return null; }
            @Fluent @Independent @Modified
            ImmutableMap.Builder<K, V> orderEntriesByValue(
                @Independent @NotModified Comparator<? super V> valueComparator) { return null; }

            @Fluent @Independent @Modified
            ImmutableMap.Builder<K, V> put(@Independent @NotModified K key, @Independent @NotModified V value) {
                return null;
            }

            @Fluent @Independent @Modified
            ImmutableMap.Builder<K, V> put(@Independent @NotModified Map.Entry<? extends K, ? extends V> entry) {
                return null;
            }

            @Fluent @Independent @Modified
            ImmutableMap.Builder<K, V> putAll(
                @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
                return null;
            }

            @Fluent @Independent @Modified
            ImmutableMap.Builder<K, V> putAll(@Independent @NotModified Map<? extends K, ? extends V> map) {
                return null;
            }
        }
        @Independent(hc = true) @NotModified ImmutableSetMultimap<K, V> asMultimap() { return null; }
        @Independent @NotModified static <K, V> ImmutableMap.Builder<K, V> builder() { return null; }
        @Independent @NotModified
        static <K, V> ImmutableMap.Builder<K, V> builderWithExpectedSize(int expectedSize) { return null; }

        //override from java.util.Map
        @NotModified
        void clear() { }

        //override from java.util.Map
        @Independent @NotModified

        V compute(
            @Independent @NotModified K key,
            @Independent @NotModified BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from java.util.Map
        @Independent @NotModified

        V computeIfAbsent(
            @Independent @NotModified K key,
            @Independent @NotModified java.util.function.Function<? super K, ? extends V> mappingFunction) {
            return null;
        }

        //override from java.util.Map
        @Independent @NotModified

        V computeIfPresent(
            @Independent @NotModified K key,
            @Independent @NotModified BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from java.util.Map
        @NotModified
        boolean containsKey(@Independent @NotModified Object key) { return false; }

        //override from java.util.Map
        @NotModified
        boolean containsValue(@Independent @NotModified Object value) { return false; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> copyOf(
            @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> copyOf(@Independent @NotModified Map<? extends K, ? extends V> map) {
            return null;
        }

        //override from java.util.Map
        @Independent @NotModified
        ImmutableSet<Map.Entry<K, V>> entrySet() { return null; }

        //override from java.lang.Object, java.util.Map
        @NotModified
        public boolean equals(@Independent @NotModified Object object) { return false; }

        //override from java.util.Map
        @Independent(hc = true) @NotModified
        V get(@Independent @NotModified Object key) { return null; }

        //override from java.util.Map
        @Independent @NotModified
        V getOrDefault(@Independent @NotModified Object key, @Independent @NotModified V defaultValue) { return null; }

        //override from java.lang.Object, java.util.Map
        @NotModified
        public int hashCode() { return 0; }

        //override from java.util.Map
        @NotModified
        boolean isEmpty() { return false; }

        //override from java.util.Map
        @Independent @NotModified
        ImmutableSet<K> keySet() { return null; }

        //override from java.util.Map
        @Independent @NotModified

        V merge(
            @Independent @NotModified K key,
            @Independent @NotModified V value,
            @Independent @NotModified BiFunction<? super V, ? super V, ? extends V> function) { return null; }
        @Independent @NotModified static <K, V> ImmutableMap<K, V> of() { return null; }
        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> of(@Independent @NotModified K k1, @Independent @NotModified V v1) {
            return null;
        }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8,
            @Independent @NotModified K k9,
            @Independent @NotModified V v9) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8,
            @Independent @NotModified K k9,
            @Independent @NotModified V v9,
            @Independent @NotModified K k10,
            @Independent @NotModified V v10) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> ofEntries(
            @Independent @NotModified Map.Entry<? extends K, ? extends V> ... entries) { return null; }

        //override from java.util.Map
        @Independent @NotModified
        V put(@Independent @NotModified K k, @Independent @NotModified V v) { return null; }

        //override from java.util.Map
        @NotModified
        void putAll(@Independent @NotModified Map<? extends K, ? extends V> map) { }

        //override from java.util.Map
        @Independent @NotModified
        V putIfAbsent(@Independent @NotModified K key, @Independent @NotModified V value) { return null; }

        //override from java.util.Map
        @Independent @NotModified
        V remove(@Independent @NotModified Object o) { return null; }

        //override from java.util.Map
        @NotModified
        boolean remove(@Independent @NotModified Object key, @Independent @NotModified Object value) { return false; }

        //override from java.util.Map
        @Independent @NotModified
        V replace(@Independent @NotModified K key, @Independent @NotModified V value) { return null; }

        //override from java.util.Map
        @NotModified

        boolean replace(
            @Independent @NotModified K key,
            @Independent @NotModified V oldValue,
            @Independent @NotModified V newValue) { return false; }

        //override from java.util.Map
        @NotModified
        void replaceAll(@Independent @NotModified BiFunction<? super K, ? super V, ? extends V> function) { }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction) { return null; }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction,
            @Independent @NotModified BinaryOperator<V> mergeFunction) { return null; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }

        //override from java.util.Map
        @Independent(hc = true) @NotModified
        ImmutableCollection<V> values() { return null; }
    }

    //public abstract class ImmutableMultimap extends BaseImmutableMultimap<K,V> implements Serializable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableMultimap$<K, V> {
        //public static class Builder
        //annotated as EXPECTED; computed mutable @Dependent (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<K, V> {
            Builder() { }
            @Independent @Modified ImmutableMultimap<K, V> build() { return null; }
            @Fluent @Independent @Modified
            ImmutableMultimap.Builder<K, V> expectedValuesPerKey(int expectedValuesPerKey) { return null; }

            @Fluent @Independent @Modified
            ImmutableMultimap.Builder<K, V> orderKeysBy(@Independent @NotModified Comparator<? super K> keyComparator) {
                return null;
            }

            @Fluent @Independent @Modified
            ImmutableMultimap.Builder<K, V> orderValuesBy(
                @Independent @NotModified Comparator<? super V> valueComparator) { return null; }

            @Fluent @Independent @Modified
            ImmutableMultimap.Builder<K, V> put(
                @Independent(hc = true) @NotModified K key,
                @Independent @NotModified V value) { return null; }

            @Fluent @Independent @Modified
            ImmutableMultimap.Builder<K, V> put(
                @Independent(hc = true) @NotModified Map.Entry<? extends K, ? extends V> entry) { return null; }

            @Fluent @Independent @Modified
            ImmutableMultimap.Builder<K, V> putAll(
                @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
                return null;
            }

            @Fluent @Independent @Modified
            ImmutableMultimap.Builder<K, V> putAll(
                @Independent(hc = true) @NotModified K key,
                @Independent @NotModified Iterable<? extends V> values) { return null; }

            @Fluent @Independent @Modified
            ImmutableMultimap.Builder<K, V> putAll(
                @Independent(hc = true) @NotModified K key,
                @Independent @NotModified V ... values) { return null; }

            @Fluent @Independent @Modified
            ImmutableMultimap.Builder<K, V> putAll(
                @Independent @NotModified Multimap<? extends K, ? extends V> multimap) { return null; }
        }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @Independent(hc = true) @NotModified
        ImmutableMap<K, Collection<V>> asMap() { return null; }
        @Independent @NotModified static <K, V> ImmutableMultimap.Builder<K, V> builder() { return null; }
        @Independent @NotModified
        static <K, V> ImmutableMultimap.Builder<K, V> builderWithExpectedKeys(int expectedKeys) { return null; }

        //override from com.google.common.collect.Multimap
        @NotModified
        void clear() { }

        //override from com.google.common.collect.Multimap
        @NotModified
        boolean containsKey(@Independent @NotModified Object key) { return false; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @NotModified
        boolean containsValue(@Independent @NotModified Object value) { return false; }

        @Independent @NotModified
        static <K, V> ImmutableMultimap<K, V> copyOf(
            @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMultimap<K, V> copyOf(
            @Independent @NotModified Multimap<? extends K, ? extends V> multimap) { return null; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @Independent(hc = true) @NotModified
        ImmutableCollection<Map.Entry<K, V>> entries() { return null; }

        //override from com.google.common.collect.Multimap
        @NotModified
        void forEach(@Independent @NotModified BiConsumer<? super K, ? super V> action) { }

        //override from com.google.common.collect.Multimap
        @Independent(hc = true) @NotModified
        ImmutableCollection<V> get(@Independent @NotModified K key) { return null; }
        @Independent @NotModified ImmutableMultimap<V, K> inverse() { return null; }
        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @Independent @NotModified
        ImmutableSet<K> keySet() { return null; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @Independent @NotModified
        ImmutableMultiset<K> keys() { return null; }
        @Independent @NotModified static <K, V> ImmutableMultimap<K, V> of() { return null; }
        @Independent @NotModified
        static <K, V> ImmutableMultimap<K, V> of(@Independent @NotModified K k1, @Independent @NotModified V v1) {
            return null;
        }

        @Independent @NotModified
        static <K, V> ImmutableMultimap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMultimap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMultimap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMultimap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5) { return null; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @NotModified
        boolean put(@Independent @NotModified K key, @Independent @NotModified V value) { return false; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @NotModified

        boolean putAll(@Independent @NotModified K key, @Independent @NotModified Iterable<? extends V> values) {
            return false;
        }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @NotModified
        boolean putAll(@Independent @NotModified Multimap<? extends K, ? extends V> multimap) { return false; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @NotModified
        boolean remove(@Independent @NotModified Object key, @Independent @NotModified Object value) { return false; }

        //override from com.google.common.collect.Multimap
        @Independent @NotModified
        ImmutableCollection<V> removeAll(@Independent @NotModified Object key) { return null; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @Independent @NotModified

        ImmutableCollection<V> replaceValues(
            @Independent @NotModified K key,
            @Independent @NotModified Iterable<? extends V> values) { return null; }

        //override from com.google.common.collect.Multimap
        @NotModified @GetSet("size")
        int size() { return 0; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @Independent(hc = true) @NotModified
        ImmutableCollection<V> values() { return null; }
    }

    //public abstract class ImmutableMultiset extends ImmutableCollection<E> implements Multiset<E>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableMultiset$<E> {
        //public static class Builder extends Builder<E>
        //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<E> {
            Builder() { }
            //override from com.google.common.collect.ImmutableCollection.Builder
            @Fluent @Independent @Modified @NotModified(after = "contents")
            ImmutableMultiset.Builder<E> add(@Independent @NotModified E element) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder
            @Fluent @Independent @Modified
            ImmutableMultiset.Builder<E> add(@Independent @NotModified E ... elements) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder
            @Fluent @Independent @Modified
            ImmutableMultiset.Builder<E> addAll(@Independent @NotModified Iterable<? extends E> elements) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder
            @Fluent @Independent @Modified
            ImmutableMultiset.Builder<E> addAll(@Independent @NotModified Iterator<? extends E> elements) { return null; }

            @Fluent @Independent @Modified @NotModified(after = "contents")
            ImmutableMultiset.Builder<E> addCopies(@Independent(hc = true) @NotModified E element, int occurrences) {
                return null;
            }

            //override from com.google.common.collect.ImmutableCollection.Builder
            @Independent @Modified @NotModified(after = "contents")
            ImmutableMultiset<E> build() { return null; }

            @Fluent @Independent @Modified @NotModified(after = "contents")
            ImmutableMultiset.Builder<E> setCount(@Independent(hc = true) @NotModified E element, int count) {
                return null;
            }
        }

        //override from com.google.common.collect.Multiset
        @NotModified
        int add(@Independent @NotModified E element, int occurrences) { return 0; }

        //override from com.google.common.collect.ImmutableCollection
        @Independent(hc = true) @NotModified
        ImmutableList<E> asList() { return null; }
        @Independent @NotModified static <E> ImmutableMultiset.Builder<E> builder() { return null; }
        //override from com.google.common.collect.ImmutableCollection, com.google.common.collect.Multiset, java.util.AbstractCollection, java.util.Collection
        @NotModified
        boolean contains(@Independent @NotModified Object object) { return false; }

        @Independent @NotModified
        static <E> ImmutableMultiset<E> copyOf(@Independent @NotModified Iterable<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableMultiset<E> copyOf(@Independent @NotModified E [] elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableMultiset<E> copyOf(@Independent @NotModified Iterator<? extends E> elements) { return null; }

        //override from com.google.common.collect.Multiset
        @Independent(hc = true) @NotModified
        ImmutableSet<E> elementSet() { return null; }

        //override from com.google.common.collect.Multiset
        @Independent @NotModified
        ImmutableSet<Multiset.Entry<E>> entrySet() { return null; }

        //override from com.google.common.collect.Multiset, java.lang.Object, java.util.Collection
        @NotModified
        public boolean equals(@Independent @NotModified Object object) { return false; }

        //override from com.google.common.collect.Multiset, java.lang.Object, java.util.Collection
        @NotModified
        public int hashCode() { return 0; }

        //override from com.google.common.collect.ImmutableCollection, com.google.common.collect.Multiset, java.lang.Iterable, java.util.AbstractCollection, java.util.Collection
        @Independent @NotModified
        UnmodifiableIterator<E> iterator() { return null; }
        @Independent @NotModified static <E> ImmutableMultiset<E> of() { return null; }
        @Independent @NotModified static <E> ImmutableMultiset<E> of(@Independent @NotModified E e1) { return null; }
        @Independent @NotModified
        static <E> ImmutableMultiset<E> of(@Independent @NotModified E e1, @Independent @NotModified E e2) { return null; }

        @Independent @NotModified
        static <E> ImmutableMultiset<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3) { return null; }

        @Independent @NotModified
        static <E> ImmutableMultiset<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4) { return null; }

        @Independent @NotModified
        static <E> ImmutableMultiset<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5) { return null; }

        @Independent @NotModified
        static <E> ImmutableMultiset<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5,
            @Independent @NotModified E e6,
            @Independent @NotModified E ... others) { return null; }

        //override from com.google.common.collect.Multiset
        @NotModified
        int remove(@Independent @NotModified Object element, int occurrences) { return 0; }

        //override from com.google.common.collect.Multiset
        @NotModified
        int setCount(@Independent @NotModified E element, int count) { return 0; }

        //override from com.google.common.collect.Multiset
        @NotModified
        boolean setCount(@Independent @NotModified E element, int oldCount, int newCount) { return false; }

        @Independent @NotModified
        static <E> Collector<E, ?, ImmutableMultiset<E>> toImmutableMultiset() { return null; }

        @Independent @NotModified
        static <T, E> Collector<T, ?, ImmutableMultiset<E>> toImmutableMultiset(
            @Independent @NotModified java.util.function.Function<? super T, ? extends E> elementFunction,
            @Independent @NotModified ToIntFunction<? super T> countFunction) { return null; }

        //override from com.google.common.collect.Multiset, java.lang.Object, java.util.AbstractCollection
        @NotModified
        public String toString() { return null; }
    }

    //public class ImmutableRangeMap implements RangeMap<K,V>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a value (GUAVA.md)
    @Immutable(hc = true)
    class ImmutableRangeMap$<K extends Comparable<?>, V> {
        //public static final class Builder
        //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<K extends Comparable<?>, V> {
            Builder() { }
            @Independent @Modified ImmutableRangeMap<K, V> build() { return null; }
            @Fluent @Independent @Modified
            ImmutableRangeMap.Builder<K, V> put(
                @Independent @NotModified Range<K> range,
                @Independent @NotModified V value) { return null; }

            @Fluent @Independent @Modified
            ImmutableRangeMap.Builder<K, V> putAll(@Independent @NotModified RangeMap<K, ? extends V> rangeMap) {
                return null;
            }
        }

        //override from com.google.common.collect.RangeMap
        @Independent @NotModified
        ImmutableMap<Range<K>, V> asDescendingMapOfRanges() { return null; }

        //override from com.google.common.collect.RangeMap
        @Independent @NotModified
        ImmutableMap<Range<K>, V> asMapOfRanges() { return null; }

        @Independent @NotModified
        static <K extends Comparable<?>, V> ImmutableRangeMap.Builder<K, V> builder() { return null; }

        //override from com.google.common.collect.RangeMap
        @NotModified
        void clear() { }

        @Independent @NotModified
        static <K extends Comparable<?>, V> ImmutableRangeMap<K, V> copyOf(
            @Independent @Modified @NotModified(after = "ranges,values") RangeMap<K, ? extends V> rangeMap) {
            return null;
        }

        //override from com.google.common.collect.RangeMap, java.lang.Object
        @NotModified
        public boolean equals(@Independent @Modified Object o) { return false; }

        //override from com.google.common.collect.RangeMap
        @Independent(hc = true) @NotModified
        V get(@Independent @NotModified K key) { return null; }

        //override from com.google.common.collect.RangeMap
        @Independent @NotModified
        Map.Entry<Range<K>, V> getEntry(@Independent @NotModified K key) { return null; }

        //override from com.google.common.collect.RangeMap, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from com.google.common.collect.RangeMap
        @NotModified

        void merge(
            @Independent @NotModified Range<K> range,
            @Independent @NotModified V value,
            @Independent @NotModified BiFunction<? super V, ? super V, ? extends V> remappingFunction) { }
        @Independent @NotModified static <K extends Comparable<?>, V> ImmutableRangeMap<K, V> of() { return null; }
        @Independent @NotModified
        static <K extends Comparable<?>, V> ImmutableRangeMap<K, V> of(
            @Independent @NotModified Range<K> range,
            @Independent @NotModified V value) { return null; }

        //override from com.google.common.collect.RangeMap
        @NotModified
        void put(@Independent @NotModified Range<K> range, @Independent @NotModified V value) { }

        //override from com.google.common.collect.RangeMap
        @NotModified
        void putAll(@Independent @NotModified RangeMap<K, ? extends V> rangeMap) { }

        //override from com.google.common.collect.RangeMap
        @NotModified
        void putCoalescing(@Independent @NotModified Range<K> range, @Independent @NotModified V value) { }

        //override from com.google.common.collect.RangeMap
        @NotModified
        void remove(@Independent @NotModified Range<K> range) { }

        //override from com.google.common.collect.RangeMap
        @Independent @NotModified
        Range<K> span() { return null; }

        //override from com.google.common.collect.RangeMap
        @Fluent @Independent @NotModified
        ImmutableRangeMap<K, V> subRangeMap(@Independent @Modified Range<K> range) { return null; }

        @Independent @NotModified
        static <T, K extends Comparable<? super K>, V> Collector<T, ?, ImmutableRangeMap<K, V>> toImmutableRangeMap(
            @Independent @Modified java.util.function.Function<? super T, Range<K>> keyFunction,
            @Independent @Modified java.util.function.Function<? super T, ? extends V> valueFunction) { return null; }

        //override from com.google.common.collect.RangeMap, java.lang.Object
        @NotModified
        public String toString() { return null; }
    }

    //public final class ImmutableRangeSet extends AbstractRangeSet<C> implements Serializable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a value (GUAVA.md)
    @Immutable(hc = true)
    class ImmutableRangeSet$<C extends Comparable> {
        //public static class Builder
        //annotated as EXPECTED; computed @FinalFields @Independent (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent
        class Builder<C extends Comparable<?>> {
            Builder() { }
            @Fluent @Independent @Modified
            ImmutableRangeSet.Builder<C> add(@Independent @NotModified Range<C> range) { return null; }

            @Fluent @Independent @Modified
            ImmutableRangeSet.Builder<C> addAll(@Independent(hc = true) @NotModified Iterable<Range<C>> ranges) {
                return null;
            }

            @Fluent @Independent @Modified
            ImmutableRangeSet.Builder<C> addAll(@Independent(hc = true) @NotModified RangeSet<C> ranges) { return null; }
            @Independent @Modified ImmutableRangeSet<C> build() { return null; }
        }

        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @NotModified
        void add(@Independent @NotModified Range<C> range) { }

        //override from com.google.common.collect.RangeSet
        @NotModified
        void addAll(@Independent @NotModified Iterable<Range<C>> other) { }

        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @NotModified
        void addAll(@Independent @NotModified RangeSet<C> other) { }

        //override from com.google.common.collect.RangeSet
        @Independent(hc = true) @NotModified
        ImmutableSet<Range<C>> asDescendingSetOfRanges() { return null; }

        //override from com.google.common.collect.RangeSet
        @Independent(hc = true) @NotModified
        ImmutableSet<Range<C>> asRanges() { return null; }

        @Independent @NotModified
        ImmutableSortedSet<C> asSet(@Independent @NotModified DiscreteDomain<C> domain) { return null; }

        @Independent @NotModified
        static <C extends Comparable<?>> ImmutableRangeSet.Builder<C> builder() { return null; }

        //override from com.google.common.collect.RangeSet
        @Fluent @Independent @NotModified
        ImmutableRangeSet<C> complement() { return null; }

        @Independent @NotModified
        static <C extends Comparable<?>> ImmutableRangeSet<C> copyOf(@Independent @Modified Iterable<Range<C>> ranges) {
            return null;
        }

        @Independent @NotModified
        static <C extends Comparable> ImmutableRangeSet<C> copyOf(@Independent @Modified RangeSet<C> rangeSet) {
            return null;
        }

        @Independent @NotModified
        ImmutableRangeSet<C> difference(@Independent @Modified RangeSet<C> other) { return null; }

        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @NotModified
        boolean encloses(@Independent @NotModified Range<C> otherRange) { return false; }

        @Independent @NotModified
        ImmutableRangeSet<C> intersection(@Independent @Modified RangeSet<C> other) { return null; }

        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @NotModified

        boolean intersects(@Independent @Modified @NotModified(after = "lowerBound,upperBound") Range<C> otherRange) {
            return false;
        }

        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @NotModified
        boolean isEmpty() { return false; }
        @Independent @NotModified static <C extends Comparable> ImmutableRangeSet<C> of() { return null; }
        @Independent @NotModified
        static <C extends Comparable> ImmutableRangeSet<C> of(@Independent @NotModified Range<C> range) { return null; }

        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @Independent @NotModified
        Range<C> rangeContaining(@Independent @NotModified C value) { return null; }

        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @NotModified
        void remove(@Independent @NotModified Range<C> range) { }

        //override from com.google.common.collect.RangeSet
        @NotModified
        void removeAll(@Independent @NotModified Iterable<Range<C>> other) { }

        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @NotModified
        void removeAll(@Independent @NotModified RangeSet<C> other) { }

        //override from com.google.common.collect.RangeSet
        @Independent @NotModified
        Range<C> span() { return null; }

        //override from com.google.common.collect.RangeSet
        @Independent @NotModified

        ImmutableRangeSet<C> subRangeSet(
            @Independent @Modified @NotModified(after = "lowerBound,upperBound") Range<C> range) { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> Collector<Range<E>, ?, ImmutableRangeSet<E>> toImmutableRangeSet() {
            return null;
        }
        @Independent @NotModified ImmutableRangeSet<C> union(@Independent @Modified RangeSet<C> other) { return null; }
        @Independent @NotModified
        static <C extends Comparable<?>> ImmutableRangeSet<C> unionOf(
            @Independent @NotModified Iterable<Range<C>> ranges) { return null; }
    }

    //public abstract class ImmutableSet extends ImmutableCollection<E> implements Set<E>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1), mutable @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableSet$<E> {
        //public static class Builder extends Builder<E>
        //annotated as EXPECTED; computed mutable @Dependent (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<E> {
            Builder() { }
            //override from com.google.common.collect.ImmutableCollection.Builder
            @Fluent @Independent @Modified
            ImmutableSet.Builder<E> add(@Independent(hc = true) @NotModified E element) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder
            @Fluent @Independent @Modified
            ImmutableSet.Builder<E> add(@Independent @NotModified E ... elements) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder
            @Fluent @Independent @Modified
            ImmutableSet.Builder<E> addAll(@Independent @NotModified Iterable<? extends E> elements) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder
            @Fluent @Independent @Modified
            ImmutableSet.Builder<E> addAll(@Independent @NotModified Iterator<? extends E> elements) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder
            @Independent(hc = true) @Modified
            ImmutableSet<E> build() { return null; }
        }
        @Independent @NotModified static <E> ImmutableSet.Builder<E> builder() { return null; }
        @Independent @NotModified
        static <E> ImmutableSet.Builder<E> builderWithExpectedSize(int expectedSize) { return null; }

        @Independent @NotModified
        static <E> ImmutableSet<E> copyOf(@Independent @NotModified Iterable<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableSet<E> copyOf(@Independent @NotModified E [] elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableSet<E> copyOf(@Independent @NotModified Collection<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableSet<E> copyOf(@Independent @NotModified Iterator<? extends E> elements) { return null; }

        //override from java.lang.Object, java.util.Collection, java.util.Set
        @NotModified
        public boolean equals(@Independent @NotModified Object object) { return false; }

        //override from java.lang.Object, java.util.Collection, java.util.Set
        @NotModified
        public int hashCode() { return 0; }

        //override from com.google.common.collect.ImmutableCollection, java.lang.Iterable, java.util.AbstractCollection, java.util.Collection, java.util.Set
        @Independent(hc = true) @NotModified @NotNull
        UnmodifiableIterator<E> iterator() { return null; }
        @Independent @NotModified static <E> ImmutableSet<E> of() { return null; }
        @Independent @NotModified static <E> ImmutableSet<E> of(@Independent @NotModified E e1) { return null; }
        @Independent @NotModified
        static <E> ImmutableSet<E> of(@Independent @NotModified E e1, @Independent @NotModified E e2) { return null; }

        @Independent @NotModified
        static <E> ImmutableSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3) { return null; }

        @Independent @NotModified
        static <E> ImmutableSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4) { return null; }

        @Independent @NotModified
        static <E> ImmutableSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5) { return null; }

        @Independent @NotModified
        static <E> ImmutableSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5,
            @Independent @NotModified E e6,
            @Independent @NotModified E ... others) { return null; }
        @Independent @NotModified static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() { return null; }
    }

    //public class ImmutableSetMultimap extends ImmutableMultimap<K,V> implements SetMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1), mutable @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableSetMultimap$<K, V> {
        //public static final class Builder extends Builder<K,V>
        //annotated as EXPECTED; computed mutable @Dependent (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<K, V> {
            Builder() { }
            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Independent @Modified
            ImmutableSetMultimap<K, V> build() { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified
            ImmutableSetMultimap.Builder<K, V> expectedValuesPerKey(int expectedValuesPerKey) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableSetMultimap.Builder<K, V> orderKeysBy(
                @Independent @NotModified Comparator<? super K> keyComparator) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableSetMultimap.Builder<K, V> orderValuesBy(
                @Independent @NotModified Comparator<? super V> valueComparator) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableSetMultimap.Builder<K, V> put(
                @Independent(hc = true) @NotModified K key,
                @Independent @NotModified V value) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableSetMultimap.Builder<K, V> put(
                @Independent(hc = true) @NotModified Map.Entry<? extends K, ? extends V> entry) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableSetMultimap.Builder<K, V> putAll(
                @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
                return null;
            }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableSetMultimap.Builder<K, V> putAll(
                @Independent(hc = true) @NotModified K key,
                @Independent @NotModified Iterable<? extends V> values) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableSetMultimap.Builder<K, V> putAll(
                @Independent(hc = true) @NotModified K key,
                @Independent @NotModified V ... values) { return null; }

            //override from com.google.common.collect.ImmutableMultimap.Builder
            @Fluent @Independent @Modified

            ImmutableSetMultimap.Builder<K, V> putAll(
                @Independent @NotModified Multimap<? extends K, ? extends V> multimap) { return null; }
        }
        @Independent @NotModified static <K, V> ImmutableSetMultimap.Builder<K, V> builder() { return null; }
        @Independent @NotModified
        static <K, V> ImmutableSetMultimap.Builder<K, V> builderWithExpectedKeys(int expectedKeys) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableSetMultimap<K, V> copyOf(
            @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableSetMultimap<K, V> copyOf(
            @Independent @NotModified Multimap<? extends K, ? extends V> multimap) { return null; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.ImmutableMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Fluent @Independent @NotModified
        ImmutableSet<Map.Entry<K, V>> entries() { return null; }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableSetMultimap<K, V>> flatteningToImmutableSetMultimap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends Stream<? extends V>>
                valuesFunction) { return null; }

        //override from com.google.common.collect.ImmutableMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent(hc = true) @NotModified
        ImmutableSet<V> get(@Independent @NotModified K key) { return null; }

        //override from com.google.common.collect.ImmutableMultimap
        @Fluent @Independent @NotModified
        ImmutableSetMultimap<V, K> inverse() { return null; }
        @Independent @NotModified static <K, V> ImmutableSetMultimap<K, V> of() { return null; }
        @Independent @NotModified
        static <K, V> ImmutableSetMultimap<K, V> of(@Independent @NotModified K k1, @Independent @NotModified V v1) {
            return null;
        }

        @Independent @NotModified
        static <K, V> ImmutableSetMultimap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableSetMultimap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableSetMultimap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableSetMultimap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5) { return null; }

        //override from com.google.common.collect.ImmutableMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent @NotModified
        ImmutableSet<V> removeAll(@Independent @NotModified Object key) { return null; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.ImmutableMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent @NotModified

        ImmutableSet<V> replaceValues(
            @Independent @NotModified K key,
            @Independent @NotModified Iterable<? extends V> values) { return null; }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableSetMultimap<K, V>> toImmutableSetMultimap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction) { return null; }
    }

    //public final class ImmutableSortedMap extends ImmutableMap<K,V> implements NavigableMap<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableSortedMap$<K, V> {
        //public static class Builder extends Builder<K,V>
        //annotated as EXPECTED; computed mutable @Dependent @Container (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<K, V> {
            Builder(@Independent @NotModified Comparator<? super K> comparator) { }
            //override from com.google.common.collect.ImmutableMap.Builder
            @Independent @Modified
            ImmutableSortedMap<K, V> build() { return null; }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Independent @NotModified
            ImmutableSortedMap<K, V> buildKeepingLast() { return null; }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Independent @Modified
            ImmutableSortedMap<K, V> buildOrThrow() { return null; }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Independent @NotModified

            ImmutableSortedMap.Builder<K, V> orderEntriesByValue(
                @Independent @NotModified Comparator<? super V> valueComparator) { return null; }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Fluent @Independent @Modified

            ImmutableSortedMap.Builder<K, V> put(@Independent @NotModified K key, @Independent @NotModified V value) {
                return null;
            }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Fluent @Independent @Modified

            ImmutableSortedMap.Builder<K, V> put(@Independent @NotModified Map.Entry<? extends K, ? extends V> entry) {
                return null;
            }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Fluent @Independent @Modified

            ImmutableSortedMap.Builder<K, V> putAll(
                @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) {
                return null;
            }

            //override from com.google.common.collect.ImmutableMap.Builder
            @Fluent @Independent @Modified

            ImmutableSortedMap.Builder<K, V> putAll(@Independent @NotModified Map<? extends K, ? extends V> map) {
                return null;
            }
        }
        @Independent @NotModified static <K, V> ImmutableSortedMap.Builder<K, V> builder() { return null; }
        @Independent @NotModified
        static <K, V> ImmutableSortedMap.Builder<K, V> builderWithExpectedSize(int expectedSize) { return null; }

        //override from java.util.NavigableMap
        @Independent @NotModified
        Map.Entry<K, V> ceilingEntry(@Independent @NotModified K key) { return null; }

        //override from java.util.NavigableMap
        @Independent @NotModified
        K ceilingKey(@Independent @NotModified K key) { return null; }

        //override from java.util.SortedMap
        @Independent(hc = true) @NotModified
        Comparator<? super K> comparator() { return null; }

        @Independent @NotModified
        static <K, V> ImmutableSortedMap<K, V> copyOf(
            @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableSortedMap<K, V> copyOf(
            @Independent @NotModified Iterable<? extends Map.Entry<? extends K, ? extends V>> entries,
            @Independent @NotModified Comparator<? super K> comparator) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableSortedMap<K, V> copyOf(@Independent @NotModified Map<? extends K, ? extends V> map) {
            return null;
        }

        @Independent @NotModified
        static <K, V> ImmutableSortedMap<K, V> copyOf(
            @Independent @NotModified Map<? extends K, ? extends V> map,
            @Independent @NotModified Comparator<? super K> comparator) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableSortedMap<K, V> copyOfSorted(@Independent @NotModified SortedMap<K, ? extends V> map) {
            return null;
        }

        //override from java.util.NavigableMap
        @Independent(hc = true) @NotModified
        ImmutableSortedSet<K> descendingKeySet() { return null; }

        //override from java.util.NavigableMap
        @Fluent @Independent @NotModified
        ImmutableSortedMap<K, V> descendingMap() { return null; }

        //override from com.google.common.collect.ImmutableMap, java.util.Map, java.util.SortedMap
        @Independent(hc = true) @NotModified
        ImmutableSet<Map.Entry<K, V>> entrySet() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        @Independent @NotModified
        Map.Entry<K, V> firstEntry() { return null; }

        //override from java.util.SortedMap
        @Independent @NotModified
        K firstKey() { return null; }

        //override from java.util.NavigableMap
        @Independent @NotModified
        Map.Entry<K, V> floorEntry(@Independent @NotModified K key) { return null; }

        //override from java.util.NavigableMap
        @Independent @NotModified
        K floorKey(@Independent @NotModified K key) { return null; }

        //override from java.util.Map
        @NotModified
        void forEach(@Independent @NotModified BiConsumer<? super K, ? super V> action) { }

        //override from com.google.common.collect.ImmutableMap, java.util.Map
        @Independent(hc = true) @NotModified
        V get(@Independent @NotModified Object key) { return null; }

        //override from java.util.NavigableMap, java.util.SortedMap
        @Fluent @Independent @NotModified
        ImmutableSortedMap<K, V> headMap(@Independent @NotModified K toKey) { return null; }

        //override from java.util.NavigableMap
        @Fluent @Independent @NotModified
        ImmutableSortedMap<K, V> headMap(@Independent @NotModified K toKey, boolean inclusive) { return null; }

        //override from java.util.NavigableMap
        @Independent @NotModified
        Map.Entry<K, V> higherEntry(@Independent @NotModified K key) { return null; }

        //override from java.util.NavigableMap
        @Independent @NotModified
        K higherKey(@Independent @NotModified K key) { return null; }

        //override from com.google.common.collect.ImmutableMap, java.util.Map, java.util.SortedMap
        @Independent(hc = true) @NotModified @GetSet("keySet")
        ImmutableSortedSet<K> keySet() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        @Independent @NotModified
        Map.Entry<K, V> lastEntry() { return null; }

        //override from java.util.SortedMap
        @Independent @NotModified
        K lastKey() { return null; }

        //override from java.util.NavigableMap
        @Independent @NotModified
        Map.Entry<K, V> lowerEntry(@Independent @NotModified K key) { return null; }

        //override from java.util.NavigableMap
        @Independent @NotModified
        K lowerKey(@Independent @NotModified K key) { return null; }

        @Independent @NotModified
        static <K extends Comparable<?>, V> ImmutableSortedMap.Builder<K, V> naturalOrder() { return null; }

        //override from java.util.NavigableMap
        @Independent(hc = true) @NotModified @GetSet("keySet")
        ImmutableSortedSet<K> navigableKeySet() { return null; }
        @Independent @NotModified static <K, V> ImmutableSortedMap<K, V> of() { return null; }
        @Independent @NotModified
        static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8,
            @Independent @NotModified K k9,
            @Independent @NotModified V v9) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4,
            @Independent @NotModified K k5,
            @Independent @NotModified V v5,
            @Independent @NotModified K k6,
            @Independent @NotModified V v6,
            @Independent @NotModified K k7,
            @Independent @NotModified V v7,
            @Independent @NotModified K k8,
            @Independent @NotModified V v8,
            @Independent @NotModified K k9,
            @Independent @NotModified V v9,
            @Independent @NotModified K k10,
            @Independent @NotModified V v10) { return null; }

        @Independent(hc = true) @Modified
        static <K, V> ImmutableSortedMap<K, V> of(
            @Independent(hc = true) @NotModified K k1,
            @Independent(hc = true) @NotModified V v1) { return null; }

        @Independent(hc = true) @Modified
        static <K, V> ImmutableSortedMap<K, V> of(
            @Independent(hc = true) @NotModified K k1,
            @Independent(hc = true) @NotModified V v1,
            @Independent(hc = true) @NotModified K k2,
            @Independent(hc = true) @NotModified V v2) { return null; }

        @Independent(hc = true) @Modified
        static <K, V> ImmutableSortedMap<K, V> of(
            @Independent(hc = true) @NotModified K k1,
            @Independent(hc = true) @NotModified V v1,
            @Independent(hc = true) @NotModified K k2,
            @Independent(hc = true) @NotModified V v2,
            @Independent(hc = true) @NotModified K k3,
            @Independent(hc = true) @NotModified V v3) { return null; }

        @Independent(hc = true) @Modified
        static <K, V> ImmutableSortedMap<K, V> of(
            @Independent(hc = true) @NotModified K k1,
            @Independent(hc = true) @NotModified V v1,
            @Independent(hc = true) @NotModified K k2,
            @Independent(hc = true) @NotModified V v2,
            @Independent(hc = true) @NotModified K k3,
            @Independent(hc = true) @NotModified V v3,
            @Independent(hc = true) @NotModified K k4,
            @Independent(hc = true) @NotModified V v4) { return null; }

        @Independent(hc = true) @Modified
        static <K, V> ImmutableSortedMap<K, V> of(
            @Independent(hc = true) @NotModified K k1,
            @Independent(hc = true) @NotModified V v1,
            @Independent(hc = true) @NotModified K k2,
            @Independent(hc = true) @NotModified V v2,
            @Independent(hc = true) @NotModified K k3,
            @Independent(hc = true) @NotModified V v3,
            @Independent(hc = true) @NotModified K k4,
            @Independent(hc = true) @NotModified V v4,
            @Independent(hc = true) @NotModified K k5,
            @Independent(hc = true) @NotModified V v5) { return null; }

        @Independent(hc = true) @Modified
        static <K, V> ImmutableSortedMap<K, V> of(
            @Independent(hc = true) @NotModified K k1,
            @Independent(hc = true) @NotModified V v1,
            @Independent(hc = true) @NotModified K k2,
            @Independent(hc = true) @NotModified V v2,
            @Independent(hc = true) @NotModified K k3,
            @Independent(hc = true) @NotModified V v3,
            @Independent(hc = true) @NotModified K k4,
            @Independent(hc = true) @NotModified V v4,
            @Independent(hc = true) @NotModified K k5,
            @Independent(hc = true) @NotModified V v5,
            @Independent(hc = true) @NotModified K k6,
            @Independent(hc = true) @NotModified V v6) { return null; }

        @Independent(hc = true) @Modified
        static <K, V> ImmutableSortedMap<K, V> of(
            @Independent(hc = true) @NotModified K k1,
            @Independent(hc = true) @NotModified V v1,
            @Independent(hc = true) @NotModified K k2,
            @Independent(hc = true) @NotModified V v2,
            @Independent(hc = true) @NotModified K k3,
            @Independent(hc = true) @NotModified V v3,
            @Independent(hc = true) @NotModified K k4,
            @Independent(hc = true) @NotModified V v4,
            @Independent(hc = true) @NotModified K k5,
            @Independent(hc = true) @NotModified V v5,
            @Independent(hc = true) @NotModified K k6,
            @Independent(hc = true) @NotModified V v6,
            @Independent(hc = true) @NotModified K k7,
            @Independent(hc = true) @NotModified V v7) { return null; }

        @Independent(hc = true) @Modified
        static <K, V> ImmutableSortedMap<K, V> of(
            @Independent(hc = true) @NotModified K k1,
            @Independent(hc = true) @NotModified V v1,
            @Independent(hc = true) @NotModified K k2,
            @Independent(hc = true) @NotModified V v2,
            @Independent(hc = true) @NotModified K k3,
            @Independent(hc = true) @NotModified V v3,
            @Independent(hc = true) @NotModified K k4,
            @Independent(hc = true) @NotModified V v4,
            @Independent(hc = true) @NotModified K k5,
            @Independent(hc = true) @NotModified V v5,
            @Independent(hc = true) @NotModified K k6,
            @Independent(hc = true) @NotModified V v6,
            @Independent(hc = true) @NotModified K k7,
            @Independent(hc = true) @NotModified V v7,
            @Independent(hc = true) @NotModified K k8,
            @Independent(hc = true) @NotModified V v8) { return null; }

        @Independent(hc = true) @Modified
        static <K, V> ImmutableSortedMap<K, V> of(
            @Independent(hc = true) @NotModified K k1,
            @Independent(hc = true) @NotModified V v1,
            @Independent(hc = true) @NotModified K k2,
            @Independent(hc = true) @NotModified V v2,
            @Independent(hc = true) @NotModified K k3,
            @Independent(hc = true) @NotModified V v3,
            @Independent(hc = true) @NotModified K k4,
            @Independent(hc = true) @NotModified V v4,
            @Independent(hc = true) @NotModified K k5,
            @Independent(hc = true) @NotModified V v5,
            @Independent(hc = true) @NotModified K k6,
            @Independent(hc = true) @NotModified V v6,
            @Independent(hc = true) @NotModified K k7,
            @Independent(hc = true) @NotModified V v7,
            @Independent(hc = true) @NotModified K k8,
            @Independent(hc = true) @NotModified V v8,
            @Independent(hc = true) @NotModified K k9,
            @Independent(hc = true) @NotModified V v9) { return null; }

        @Independent(hc = true) @Modified
        static <K, V> ImmutableSortedMap<K, V> of(
            @Independent(hc = true) @NotModified K k1,
            @Independent(hc = true) @NotModified V v1,
            @Independent(hc = true) @NotModified K k2,
            @Independent(hc = true) @NotModified V v2,
            @Independent(hc = true) @NotModified K k3,
            @Independent(hc = true) @NotModified V v3,
            @Independent(hc = true) @NotModified K k4,
            @Independent(hc = true) @NotModified V v4,
            @Independent(hc = true) @NotModified K k5,
            @Independent(hc = true) @NotModified V v5,
            @Independent(hc = true) @NotModified K k6,
            @Independent(hc = true) @NotModified V v6,
            @Independent(hc = true) @NotModified K k7,
            @Independent(hc = true) @NotModified V v7,
            @Independent(hc = true) @NotModified K k8,
            @Independent(hc = true) @NotModified V v8,
            @Independent(hc = true) @NotModified K k9,
            @Independent(hc = true) @NotModified V v9,
            @Independent(hc = true) @NotModified K k10,
            @Independent(hc = true) @NotModified V v10) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableSortedMap<K, V> ofEntries(
            @Independent @NotModified Map.Entry<? extends K, ? extends V> ... entries) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableSortedMap.Builder<K, V> orderedBy(@Independent @NotModified Comparator<K> comparator) {
            return null;
        }

        //override from java.util.NavigableMap, java.util.SequencedMap
        @Independent @NotModified
        Map.Entry<K, V> pollFirstEntry() { return null; }

        //override from java.util.NavigableMap, java.util.SequencedMap
        @Independent @NotModified
        Map.Entry<K, V> pollLastEntry() { return null; }

        @Independent @NotModified
        static <K extends Comparable<?>, V> ImmutableSortedMap.Builder<K, V> reverseOrder() { return null; }

        //override from java.util.Map
        @NotModified
        int size() { return 0; }

        //override from java.util.NavigableMap, java.util.SortedMap
        @Independent @NotModified

        ImmutableSortedMap<K, V> subMap(@Independent @NotModified K fromKey, @Independent @NotModified K toKey) {
            return null;
        }

        //override from java.util.NavigableMap
        @Independent @NotModified

        ImmutableSortedMap<K, V> subMap(
            @Independent @NotModified K fromKey,
            boolean fromInclusive,
            @Independent @NotModified K toKey,
            boolean toInclusive) { return null; }

        //override from java.util.NavigableMap, java.util.SortedMap
        @Fluent @Independent @NotModified
        ImmutableSortedMap<K, V> tailMap(@Independent @NotModified K fromKey) { return null; }

        //override from java.util.NavigableMap
        @Fluent @Independent @NotModified
        ImmutableSortedMap<K, V> tailMap(@Independent @NotModified K fromKey, boolean inclusive) { return null; }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction) { return null; }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction,
            @Independent @NotModified BinaryOperator<V> mergeFunction) { return null; }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
            @Independent @NotModified Comparator<? super K> comparator,
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction) { return null; }

        @Independent @NotModified
        static <T, K, V> Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
            @Independent @NotModified Comparator<? super K> comparator,
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction,
            @Independent @NotModified BinaryOperator<V> mergeFunction) { return null; }

        @Independent @NotModified
        static <T, K extends Comparable<? super K>, V> Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction) { return null; }

        @Independent @NotModified
        static <T, K extends Comparable<? super K>, V> Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
            @Independent @NotModified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction,
            @Independent @NotModified BinaryOperator<V> mergeFunction) { return null; }

        //override from com.google.common.collect.ImmutableMap, java.util.Map, java.util.SortedMap
        @Independent(hc = true) @NotModified @GetSet("valueList")
        ImmutableCollection<V> values() { return null; }
    }

    //public abstract class ImmutableSortedMultiset extends ImmutableMultiset<E> implements SortedMultiset<E>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableSortedMultiset$<E> {
        //public static class Builder extends Builder<E>
        //annotated as EXPECTED; computed @FinalFields (eventual) @Dependent (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<E> {
            Builder(@Independent @NotModified Comparator<? super E> comparator) { }
            //override from com.google.common.collect.ImmutableCollection.Builder, com.google.common.collect.ImmutableMultiset.Builder
            @Fluent @Independent @Modified @NotModified(after = "contents")
            ImmutableSortedMultiset.Builder<E> add(@Independent @NotModified E element) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder, com.google.common.collect.ImmutableMultiset.Builder
            @Fluent @Independent @Modified
            ImmutableSortedMultiset.Builder<E> add(@Independent @NotModified E ... elements) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder, com.google.common.collect.ImmutableMultiset.Builder
            @Fluent @Independent @Modified

            ImmutableSortedMultiset.Builder<E> addAll(@Independent @NotModified Iterable<? extends E> elements) {
                return null;
            }

            //override from com.google.common.collect.ImmutableCollection.Builder, com.google.common.collect.ImmutableMultiset.Builder
            @Fluent @Independent @Modified

            ImmutableSortedMultiset.Builder<E> addAll(@Independent @NotModified Iterator<? extends E> elements) {
                return null;
            }

            //override from com.google.common.collect.ImmutableMultiset.Builder
            @Fluent @Independent @Modified @NotModified(after = "contents")

            ImmutableSortedMultiset.Builder<E> addCopies(
                @Independent(hc = true) @NotModified E element,
                int occurrences) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder, com.google.common.collect.ImmutableMultiset.Builder
            @Independent @Modified @NotModified(after = "contents")
            ImmutableSortedMultiset<E> build() { return null; }

            //override from com.google.common.collect.ImmutableMultiset.Builder
            @Fluent @Independent @Modified @NotModified(after = "contents")

            ImmutableSortedMultiset.Builder<E> setCount(@Independent(hc = true) @NotModified E element, int count) {
                return null;
            }
        }
        @Independent @NotModified static <E> ImmutableSortedMultiset.Builder<E> builder() { return null; }
        //override from com.google.common.collect.SortedIterable, com.google.common.collect.SortedMultiset
        @Independent @NotModified
        Comparator<? super E> comparator() { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> copyOf(
            @Independent @NotModified E [] elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedMultiset<E> copyOf(@Independent @NotModified Iterable<? extends E> elements) {
            return null;
        }

        @Independent @NotModified
        static <Z> ImmutableSortedMultiset<Z> copyOf(@Independent @NotModified Z [] elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedMultiset<E> copyOf(
            @Independent @NotModified Comparator<? super E> comparator,
            @Independent @NotModified Iterable<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedMultiset<E> copyOf(
            @Independent @NotModified Comparator<? super E> comparator,
            @Independent @NotModified Iterator<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedMultiset<E> copyOf(@Independent @NotModified Iterator<? extends E> elements) {
            return null;
        }

        @Independent @NotModified
        static <E> ImmutableSortedMultiset<E> copyOfSorted(@Independent @NotModified SortedMultiset<E> sortedMultiset) {
            return null;
        }

        //override from com.google.common.collect.SortedMultiset
        @Fluent @Independent @NotModified
        ImmutableSortedMultiset<E> descendingMultiset() { return null; }

        //override from com.google.common.collect.ImmutableMultiset, com.google.common.collect.Multiset, com.google.common.collect.SortedMultiset, com.google.common.collect.SortedMultisetBridge
        @Independent(hc = true) @NotModified
        ImmutableSortedSet<E> elementSet() { return null; }

        //override from com.google.common.collect.SortedMultiset
        @Independent(hc = true) @NotModified

        ImmutableSortedMultiset<E> headMultiset(
            @Independent(hc = true) @NotModified E upperBound,
            @Independent @NotModified BoundType boundType) { return null; }

        @Independent @NotModified
        static <E extends Comparable<?>> ImmutableSortedMultiset.Builder<E> naturalOrder() { return null; }
        @Independent @NotModified static <E> ImmutableSortedMultiset<E> of() { return null; }
        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(@Independent @NotModified E e1) {
            return null;
        }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2) { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3) { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4) { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5) { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5,
            @Independent @NotModified E e6,
            @Independent @NotModified E ... remaining) { return null; }

        @Independent(hc = true) @Modified
        static <E> ImmutableSortedMultiset<E> of(@Independent(hc = true) @NotModified E e1) { return null; }

        @Independent(hc = true) @Modified
        static <E> ImmutableSortedMultiset<E> of(
            @Independent(hc = true) @NotModified E e1,
            @Independent(hc = true) @NotModified E e2) { return null; }

        @Independent(hc = true) @Modified
        static <E> ImmutableSortedMultiset<E> of(
            @Independent(hc = true) @NotModified E e1,
            @Independent(hc = true) @NotModified E e2,
            @Independent(hc = true) @NotModified E e3) { return null; }

        @Independent(hc = true) @Modified
        static <E> ImmutableSortedMultiset<E> of(
            @Independent(hc = true) @NotModified E e1,
            @Independent(hc = true) @NotModified E e2,
            @Independent(hc = true) @NotModified E e3,
            @Independent(hc = true) @NotModified E e4) { return null; }

        @Independent(hc = true) @Modified
        static <E> ImmutableSortedMultiset<E> of(
            @Independent(hc = true) @NotModified E e1,
            @Independent(hc = true) @NotModified E e2,
            @Independent(hc = true) @NotModified E e3,
            @Independent(hc = true) @NotModified E e4,
            @Independent(hc = true) @NotModified E e5) { return null; }

        @Independent(hc = true) @Modified
        static <E> ImmutableSortedMultiset<E> of(
            @Independent(hc = true) @NotModified E e1,
            @Independent(hc = true) @NotModified E e2,
            @Independent(hc = true) @NotModified E e3,
            @Independent(hc = true) @NotModified E e4,
            @Independent(hc = true) @NotModified E e5,
            @Independent(hc = true) @NotModified E e6,
            @Independent(hc = true) @NotModified E ... remaining) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedMultiset.Builder<E> orderedBy(@Independent @NotModified Comparator<E> comparator) {
            return null;
        }

        //override from com.google.common.collect.SortedMultiset
        @Independent @NotModified
        Multiset.Entry<E> pollFirstEntry() { return null; }

        //override from com.google.common.collect.SortedMultiset
        @Independent @NotModified
        Multiset.Entry<E> pollLastEntry() { return null; }

        @Independent @NotModified
        static <E extends Comparable<?>> ImmutableSortedMultiset.Builder<E> reverseOrder() { return null; }

        //override from com.google.common.collect.SortedMultiset
        @Independent @NotModified

        ImmutableSortedMultiset<E> subMultiset(
            @Independent @NotModified E lowerBound,
            @Independent @NotModified BoundType lowerBoundType,
            @Independent @NotModified E upperBound,
            @Independent @NotModified BoundType upperBoundType) { return null; }

        //override from com.google.common.collect.SortedMultiset
        @Independent(hc = true) @NotModified

        ImmutableSortedMultiset<E> tailMultiset(
            @Independent(hc = true) @NotModified E lowerBound,
            @Independent @NotModified BoundType boundType) { return null; }

        @Independent @NotModified
        static <E> Collector<E, ?, ImmutableMultiset<E>> toImmutableMultiset() { return null; }

        @Independent @NotModified
        static <T, E> Collector<T, ?, ImmutableMultiset<E>> toImmutableMultiset(
            @Independent @NotModified java.util.function.Function<? super T, ? extends E> elementFunction,
            @Independent @NotModified ToIntFunction<? super T> countFunction) { return null; }

        @Independent @NotModified
        static <E> Collector<E, ?, ImmutableSortedMultiset<E>> toImmutableSortedMultiset(
            @Independent @NotModified Comparator<? super E> comparator) { return null; }

        @Independent @NotModified
        static <T, E> Collector<T, ?, ImmutableSortedMultiset<E>> toImmutableSortedMultiset(
            @Independent @NotModified Comparator<? super E> comparator,
            @Independent @NotModified java.util.function.Function<? super T, ? extends E> elementFunction,
            @Independent @NotModified ToIntFunction<? super T> countFunction) { return null; }
    }

    //public abstract class ImmutableSortedSet extends CachingAsList<E> implements NavigableSet<E>, SortedIterable<E>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableSortedSet$<E> {
        //public static final class Builder extends Builder<E>
        //annotated as EXPECTED; computed mutable @Dependent (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(absent = true)
        class Builder<E> {
            Builder(@Independent @NotModified Comparator<? super E> comparator) { }
            //override from com.google.common.collect.ImmutableCollection.Builder, com.google.common.collect.ImmutableSet.Builder
            @Fluent @Independent @Modified
            ImmutableSortedSet.Builder<E> add(@Independent(hc = true) @NotModified E element) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder, com.google.common.collect.ImmutableSet.Builder
            @Fluent @Independent @Modified
            ImmutableSortedSet.Builder<E> add(@Independent(hc = true) @NotModified E ... elements) { return null; }

            //override from com.google.common.collect.ImmutableCollection.Builder, com.google.common.collect.ImmutableSet.Builder
            @Fluent @Independent @Modified

            ImmutableSortedSet.Builder<E> addAll(@Independent @NotModified Iterable<? extends E> elements) {
                return null;
            }

            //override from com.google.common.collect.ImmutableCollection.Builder, com.google.common.collect.ImmutableSet.Builder
            @Fluent @Independent @Modified

            ImmutableSortedSet.Builder<E> addAll(@Independent @NotModified Iterator<? extends E> elements) {
                return null;
            }

            //override from com.google.common.collect.ImmutableCollection.Builder, com.google.common.collect.ImmutableSet.Builder
            @Independent(absent = true) @Modified
            ImmutableSortedSet<E> build() { return null; }
        }
        @Independent @NotModified static <E> ImmutableSortedSet.Builder<E> builder() { return null; }
        @Independent @NotModified
        static <E> ImmutableSortedSet.Builder<E> builderWithExpectedSize(int expectedSize) { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        E ceiling(@Independent @NotModified E e) { return null; }

        //override from com.google.common.collect.SortedIterable, java.util.SortedSet
        @Independent(hc = true) @NotModified @GetSet("comparator")
        Comparator<? super E> comparator() { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedSet<E> copyOf(@Independent @NotModified E [] elements) {
            return null;
        }

        @Independent @NotModified
        static <E> ImmutableSortedSet<E> copyOf(@Independent @NotModified Iterable<? extends E> elements) { return null; }

        @Independent @NotModified
        static <Z> ImmutableSortedSet<Z> copyOf(@Independent @NotModified Z [] elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedSet<E> copyOf(@Independent @NotModified Collection<? extends E> elements) {
            return null;
        }

        @Independent @NotModified
        static <E> ImmutableSortedSet<E> copyOf(
            @Independent @NotModified Comparator<? super E> comparator,
            @Independent @NotModified Iterable<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedSet<E> copyOf(
            @Independent @NotModified Comparator<? super E> comparator,
            @Independent @NotModified Collection<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedSet<E> copyOf(
            @Independent @NotModified Comparator<? super E> comparator,
            @Independent @NotModified Iterator<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedSet<E> copyOf(@Independent @NotModified Iterator<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedSet<E> copyOfSorted(@Independent @NotModified SortedSet<E> sortedSet) { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        UnmodifiableIterator<E> descendingIterator() { return null; }

        //override from java.util.NavigableSet
        @Fluent @Independent @NotModified
        ImmutableSortedSet<E> descendingSet() { return null; }

        //override from java.util.SortedSet
        @Independent @NotModified
        E first() { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        E floor(@Independent @NotModified E e) { return null; }

        //override from java.util.NavigableSet, java.util.SortedSet
        @Independent @NotModified
        ImmutableSortedSet<E> headSet(@Independent @NotModified E toElement) { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        ImmutableSortedSet<E> headSet(@Independent @NotModified E toElement, boolean inclusive) { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        E higher(@Independent @NotModified E e) { return null; }

        //override from com.google.common.collect.ImmutableCollection, com.google.common.collect.ImmutableSet, com.google.common.collect.SortedIterable, java.lang.Iterable, java.util.AbstractCollection, java.util.Collection, java.util.NavigableSet, java.util.Set
        @Independent @NotModified @NotNull
        UnmodifiableIterator<E> iterator() { return null; }

        //override from java.util.SortedSet
        @Independent @NotModified
        E last() { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        E lower(@Independent @NotModified E e) { return null; }

        @Independent @NotModified
        static <E extends Comparable<?>> ImmutableSortedSet.Builder<E> naturalOrder() { return null; }
        @Independent @NotModified static <E> ImmutableSortedSet<E> of() { return null; }
        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(@Independent @NotModified E e1) { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2) { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3) { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4) { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5) { return null; }

        @Independent @NotModified
        static <E extends Comparable<? super E>> ImmutableSortedSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5,
            @Independent @NotModified E e6,
            @Independent @NotModified E ... remaining) { return null; }
        @Independent @NotModified static <E> ImmutableSortedSet<E> of(@Independent @NotModified E e1) { return null; }
        @Independent @NotModified
        static <E> ImmutableSortedSet<E> of(@Independent @NotModified E e1, @Independent @NotModified E e2) {
            return null;
        }

        @Independent @NotModified
        static <E> ImmutableSortedSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedSet<E> of(
            @Independent @NotModified E e1,
            @Independent @NotModified E e2,
            @Independent @NotModified E e3,
            @Independent @NotModified E e4,
            @Independent @NotModified E e5,
            @Independent @NotModified E e6,
            @Independent @NotModified E ... remaining) { return null; }

        @Independent @NotModified
        static <E> ImmutableSortedSet.Builder<E> orderedBy(@Independent @NotModified Comparator<E> comparator) {
            return null;
        }

        //override from java.util.NavigableSet
        @Independent @NotModified
        E pollFirst() { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        E pollLast() { return null; }

        @Independent @NotModified
        static <E extends Comparable<?>> ImmutableSortedSet.Builder<E> reverseOrder() { return null; }

        //override from com.google.common.collect.ImmutableCollection, java.lang.Iterable, java.util.Collection, java.util.Set, java.util.SortedSet
        @Independent @NotModified
        Spliterator<E> spliterator() { return null; }

        //override from java.util.NavigableSet, java.util.SortedSet
        @Independent @NotModified

        ImmutableSortedSet<E> subSet(@Independent @NotModified E fromElement, @Independent @NotModified E toElement) {
            return null;
        }

        //override from java.util.NavigableSet
        @Independent @NotModified

        ImmutableSortedSet<E> subSet(
            @Independent @NotModified E fromElement,
            boolean fromInclusive,
            @Independent @NotModified E toElement,
            boolean toInclusive) { return null; }

        //override from java.util.NavigableSet, java.util.SortedSet
        @Independent @NotModified
        ImmutableSortedSet<E> tailSet(@Independent @NotModified E fromElement) { return null; }

        //override from java.util.NavigableSet
        @Independent @NotModified
        ImmutableSortedSet<E> tailSet(@Independent @NotModified E fromElement, boolean inclusive) { return null; }
        @Independent @NotModified static <E> Collector<E, ?, ImmutableSet<E>> toImmutableSet() { return null; }
        @Independent @NotModified
        static <E> Collector<E, ?, ImmutableSortedSet<E>> toImmutableSortedSet(
            @Independent @NotModified Comparator<? super E> comparator) { return null; }
    }

    //public abstract class ImmutableTable extends AbstractTable<R,C,V> implements Serializable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- G1: the abstract class is implemented by package-private classes sharing code with the mutable ones (GUAVA.md)
    @ImmutableContainer(hc = true)
    class ImmutableTable$<R, C, V> {
        //public static final class Builder
        //annotated as EXPECTED; computed mutable @Independent(hc = true) (1) -- a builder: mutable, stores its arguments, never modifies them (GUAVA.md)
        @Container
        @Independent(hc = true)
        class Builder<R, C, V> {
            Builder() { }
            @Independent @Modified ImmutableTable<R, C, V> build() { return null; }
            @Independent @Modified ImmutableTable<R, C, V> buildOrThrow() { return null; }
            @Fluent @Independent @Modified
            ImmutableTable.Builder<R, C, V> orderColumnsBy(
                @Independent @NotModified Comparator<? super C> columnComparator) { return null; }

            @Fluent @Independent @Modified
            ImmutableTable.Builder<R, C, V> orderRowsBy(@Independent @NotModified Comparator<? super R> rowComparator) {
                return null;
            }

            @Fluent @Independent @Modified
            ImmutableTable.Builder<R, C, V> put(
                @Independent @NotModified R rowKey,
                @Independent @NotModified C columnKey,
                @Independent @NotModified V value) { return null; }

            @Fluent @Independent @Modified
            ImmutableTable.Builder<R, C, V> put(
                @Independent @NotModified Table.Cell<? extends R, ? extends C, ? extends V> cell) { return null; }

            @Fluent @Independent @Modified
            ImmutableTable.Builder<R, C, V> putAll(
                @Independent @NotModified Table<? extends R, ? extends C, ? extends V> table) { return null; }
        }
        @Independent @NotModified static <R, C, V> ImmutableTable.Builder<R, C, V> builder() { return null; }
        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent(hc = true) @NotModified
        ImmutableSet<Table.Cell<R, C, V>> cellSet() { return null; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @NotModified
        void clear() { }

        //override from com.google.common.collect.Table
        @Independent @NotModified
        ImmutableMap<R, V> column(@Independent @NotModified C columnKey) { return null; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent @NotModified
        ImmutableSet<C> columnKeySet() { return null; }

        //override from com.google.common.collect.Table
        @Independent(hc = true) @NotModified
        ImmutableMap<C, Map<R, V>> columnMap() { return null; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @NotModified

        boolean contains(@Independent @NotModified Object rowKey, @Independent @NotModified Object columnKey) {
            return false;
        }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @NotModified
        boolean containsValue(@Independent @NotModified Object value) { return false; }

        @Independent @NotModified
        static <R, C, V> ImmutableTable<R, C, V> copyOf(
            @Independent @NotModified Table<? extends R, ? extends C, ? extends V> table) { return null; }
        @Independent @NotModified static <R, C, V> ImmutableTable<R, C, V> of() { return null; }
        @Independent @NotModified
        static <R, C, V> ImmutableTable<R, C, V> of(
            @Independent @NotModified R rowKey,
            @Independent @NotModified C columnKey,
            @Independent @NotModified V value) { return null; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent @NotModified

        V put(
            @Independent @NotModified R rowKey,
            @Independent @NotModified C columnKey,
            @Independent @NotModified V value) { return null; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @NotModified
        void putAll(@Independent @NotModified Table<? extends R, ? extends C, ? extends V> table) { }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent @NotModified
        V remove(@Independent @NotModified Object rowKey, @Independent @NotModified Object columnKey) { return null; }

        //override from com.google.common.collect.Table
        @Independent @NotModified
        ImmutableMap<C, V> row(@Independent @NotModified R rowKey) { return null; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent @NotModified
        ImmutableSet<R> rowKeySet() { return null; }

        //override from com.google.common.collect.Table
        @Independent(hc = true) @NotModified
        ImmutableMap<R, Map<C, V>> rowMap() { return null; }

        @Independent @NotModified
        static <T, R, C, V> Collector<T, ?, ImmutableTable<R, C, V>> toImmutableTable(
            @Independent @NotModified java.util.function.Function<? super T, ? extends R> rowFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends C> columnFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction) { return null; }

        @Independent @NotModified
        static <T, R, C, V> Collector<T, ?, ImmutableTable<R, C, V>> toImmutableTable(
            @Independent @NotModified java.util.function.Function<? super T, ? extends R> rowFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends C> columnFunction,
            @Independent @NotModified java.util.function.Function<? super T, ? extends V> valueFunction,
            @Independent @NotModified BinaryOperator<V> mergeFunction) { return null; }

        //override from com.google.common.collect.AbstractTable, com.google.common.collect.Table
        @Independent(hc = true) @NotModified
        ImmutableCollection<V> values() { return null; }
    }

    //public interface Interner
    @FinalFields
    @Independent
    class Interner$<E> {@Independent @Modified E intern(@Independent @Modified E sample) { return null; } }

    //public final class Interners
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @Container
    @UtilityClass
    class Interners$ {
        //public static class InternerBuilder
        @Container
        @Independent(absent = true)
        class InternerBuilder {
            @Independent(absent = true) @Modified @NotModified(after = "mapMaker")
            <E> Interner<E> build() { return null; }

            @Fluent @Independent @Modified @NotModified(after = "mapMaker")
            Interners.InternerBuilder concurrencyLevel(int concurrencyLevel) { return null; }
            @Fluent @Independent @Modified Interners.InternerBuilder strong() { return null; }
            @Fluent @Independent @Modified Interners.InternerBuilder weak() { return null; }
        }

        @Independent @NotModified
        static <E> Function<E, E> asFunction(@Independent @NotModified Interner<E> interner) { return null; }
        @Independent @NotModified static Interners.InternerBuilder newBuilder() { return null; }
        @Independent @NotModified static <E> Interner<E> newStrongInterner() { return null; }
        @Independent @NotModified static <E> Interner<E> newWeakInterner() { return null; }
    }

    //public final class Iterables
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @UtilityClass
    class Iterables$ {
        @NotModified
        static <T> boolean addAll(
            @Independent @Modified Collection<T> addTo,
            @Independent @NotModified Iterable<? extends T> elementsToAdd) { return false; }

        @NotModified
        static <T> boolean all(
            @Independent @NotModified Iterable<T> iterable,
            @Independent @Modified Predicate<? super T> predicate) { return false; }

        @NotModified
        static <T> boolean any(
            @Independent @NotModified Iterable<T> iterable,
            @Independent @Modified Predicate<? super T> predicate) { return false; }

        @Independent @NotModified
        static <T> Iterable<T> concat(@Independent @NotModified Iterable<? extends Iterable<? extends T>> inputs) {
            return null;
        }

        @Independent @NotModified
        static <T> Iterable<T> concat(
            @Independent @NotModified Iterable<? extends T> a,
            @Independent @NotModified Iterable<? extends T> b) { return null; }

        @Independent @NotModified
        static <T> Iterable<T> concat(
            @Independent @NotModified Iterable<? extends T> a,
            @Independent @NotModified Iterable<? extends T> b,
            @Independent @NotModified Iterable<? extends T> c) { return null; }

        @Independent @NotModified
        static <T> Iterable<T> concat(
            @Independent @NotModified Iterable<? extends T> a,
            @Independent @NotModified Iterable<? extends T> b,
            @Independent @NotModified Iterable<? extends T> c,
            @Independent @NotModified Iterable<? extends T> d) { return null; }

        @Independent @NotModified
        static <T> Iterable<T> concat(@Independent @NotModified Iterable<? extends T> ... inputs) { return null; }

        @Independent @NotModified
        static <T> Iterable<T> consumingIterable(@Independent @Modified Iterable<T> iterable) { return null; }

        @NotModified
        static boolean contains(
            @Independent @NotModified Iterable<?> iterable,
            @Independent @NotModified Object element) { return false; }

        @Independent @NotModified
        static <T> Iterable<T> cycle(@Independent @Modified Iterable<T> iterable) { return null; }

        @Independent @NotModified
        static <T> Iterable<T> cycle(@Independent @NotModified T ... elements) { return null; }

        @NotModified
        static boolean elementsEqual(
            @Independent @NotModified Iterable<?> iterable1,
            @Independent @NotModified Iterable<?> iterable2) { return false; }

        @Independent @NotModified
        static <T> Iterable<T> filter(@Independent @NotModified Iterable<?> unfiltered, Class<T> desiredType) {
            return null;
        }

        @Independent @NotModified
        static <T> Iterable<T> filter(
            @Independent @NotModified Iterable<T> unfiltered,
            @Independent @Modified Predicate<? super T> retainIfTrue) { return null; }

        @Independent @NotModified
        static <T> T find(
            @Independent @NotModified Iterable<T> iterable,
            @Independent @Modified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        static <T> T find(
            @Independent @NotModified Iterable<? extends T> iterable,
            @Independent @Modified Predicate<? super T> predicate,
            @Independent @NotModified T defaultValue) { return null; }

        @NotModified
        static int frequency(@Independent @NotModified Iterable<?> iterable, @Independent @Modified Object element) {
            return 0;
        }

        @Independent @NotModified
        static <T> T get(@Independent @NotModified Iterable<T> iterable, int position) { return null; }

        @Independent @NotModified
        static <T> T get(
            @Independent @NotModified Iterable<? extends T> iterable,
            int position,
            @Independent @NotModified T defaultValue) { return null; }

        @Independent @NotModified
        static <T> T getFirst(
            @Independent @NotModified Iterable<? extends T> iterable,
            @Independent @NotModified T defaultValue) { return null; }
        @Independent @NotModified static <T> T getLast(@Independent @NotModified Iterable<T> iterable) { return null; }
        @Independent @NotModified
        static <T> T getLast(
            @Independent @NotModified Iterable<? extends T> iterable,
            @Independent @NotModified T defaultValue) { return null; }

        @Independent @NotModified
        static <T> T getOnlyElement(@Independent @NotModified Iterable<T> iterable) { return null; }

        @Independent @NotModified
        static <T> T getOnlyElement(
            @Independent @NotModified Iterable<? extends T> iterable,
            @Independent @NotModified T defaultValue) { return null; }

        @NotModified
        static <T> int indexOf(
            @Independent @NotModified Iterable<T> iterable,
            @Independent @Modified Predicate<? super T> predicate) { return 0; }
        @NotModified static boolean isEmpty(@Independent @Modified Iterable<?> iterable) { return false; }
        @Independent @NotModified
        static <T> Iterable<T> limit(@Independent @Modified Iterable<T> iterable, int limitSize) { return null; }

        @Independent @NotModified
        static <T> Iterable<T> mergeSorted(
            @Independent @Modified Iterable<? extends Iterable<? extends T>> iterables,
            @Independent @NotModified Comparator<? super T> comparator) { return null; }

        @Independent @NotModified
        static <T> Iterable<List<T>> paddedPartition(@Independent @NotModified Iterable<T> iterable, int size) {
            return null;
        }

        @Independent @NotModified
        static <T> Iterable<List<T>> partition(@Independent @NotModified Iterable<T> iterable, int size) { return null; }

        @NotModified
        static boolean removeAll(
            @Independent @NotModified Iterable<?> removeFrom,
            @Independent @NotModified Collection<?> elementsToRemove) { return false; }

        @NotModified
        static <T> boolean removeIf(
            @Independent @NotModified Iterable<T> removeFrom,
            @Independent @Modified Predicate<? super T> predicate) { return false; }

        @NotModified
        static boolean retainAll(
            @Independent @NotModified Iterable<?> removeFrom,
            @Independent @NotModified Collection<?> elementsToRetain) { return false; }
        @NotModified static int size(@Independent @NotModified Iterable<?> iterable) { return 0; }
        @Independent @NotModified
        static <T> Iterable<T> skip(@Independent @Modified Iterable<T> iterable, int numberToSkip) { return null; }

        @Independent @NotModified
        static <T> T [] toArray(@Independent @NotModified Iterable<? extends T> iterable, Class<T> type) { return null; }
        @NotModified static String toString(@Independent @NotModified Iterable<?> iterable) { return null; }
        @Independent @NotModified
        static <F, T> Iterable<T> transform(
            @Independent @Modified Iterable<F> fromIterable,
            @Independent @Modified Function<? super F, ? extends T> function) { return null; }

        @Independent @NotModified
        static <T> Optional<T> tryFind(
            @Independent @NotModified Iterable<T> iterable,
            @Independent @Modified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        static <T> Iterable<T> unmodifiableIterable(@Independent @Modified Iterable<? extends T> iterable) {
            return null;
        }

        @Identity @Independent @NotModified
        static <E> Iterable<E> unmodifiableIterable(@Independent @NotModified ImmutableCollection<E> iterable) {
            return null;
        }
    }

    //public final class Iterators
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @UtilityClass
    class Iterators$ {
        @NotModified
        static <T> boolean addAll(
            @Independent @Modified Collection<T> addTo,
            @Independent @Modified Iterator<? extends T> iterator) { return false; }
        @NotModified static int advance(@Independent @Modified Iterator<?> iterator, int numberToAdvance) { return 0; }
        @NotModified
        static <T> boolean all(
            @Independent @Modified Iterator<T> iterator,
            @Independent @Modified Predicate<? super T> predicate) { return false; }

        @NotModified
        static <T> boolean any(
            @Independent @Modified Iterator<T> iterator,
            @Independent @Modified Predicate<? super T> predicate) { return false; }

        @Independent @NotModified
        static <T> Enumeration<T> asEnumeration(@Independent @Modified Iterator<T> iterator) { return null; }

        @Independent @NotModified
        static <T> Iterator<T> concat(@Independent @Modified Iterator<? extends Iterator<? extends T>> inputs) {
            return null;
        }

        @Independent @NotModified
        static <T> Iterator<T> concat(
            @Independent @Modified Iterator<? extends T> a,
            @Independent @NotModified Iterator<? extends T> b) { return null; }

        @Independent @NotModified
        static <T> Iterator<T> concat(
            @Independent @Modified Iterator<? extends T> a,
            @Independent @NotModified Iterator<? extends T> b,
            @Independent @NotModified Iterator<? extends T> c) { return null; }

        @Independent @NotModified
        static <T> Iterator<T> concat(
            @Independent @Modified Iterator<? extends T> a,
            @Independent @NotModified Iterator<? extends T> b,
            @Independent @NotModified Iterator<? extends T> c,
            @Independent @NotModified Iterator<? extends T> d) { return null; }

        @Independent @NotModified
        static <T> Iterator<T> concat(@Independent @NotModified Iterator<? extends T> ... inputs) { return null; }

        @Independent @NotModified
        static <T> Iterator<T> consumingIterator(@Independent @Modified Iterator<T> iterator) { return null; }

        @NotModified
        static boolean contains(@Independent @Modified Iterator<?> iterator, @Independent @NotModified Object element) {
            return false;
        }

        @Independent @NotModified
        static <T> Iterator<T> cycle(@Independent @Modified Iterable<T> iterable) { return null; }

        @Independent @NotModified
        static <T> Iterator<T> cycle(@Independent @NotModified T ... elements) { return null; }

        @NotModified
        static boolean elementsEqual(
            @Independent @Modified Iterator<?> iterator1,
            @Independent @Modified Iterator<?> iterator2) { return false; }

        @Independent @NotModified
        static <T> UnmodifiableIterator<T> filter(@Independent @Modified Iterator<?> unfiltered, Class<T> desiredType) {
            return null;
        }

        @Independent @NotModified
        static <T> UnmodifiableIterator<T> filter(
            @Independent @Modified Iterator<T> unfiltered,
            @Independent @Modified Predicate<? super T> retainIfTrue) { return null; }

        @Independent @NotModified
        static <T> T find(
            @Independent @Modified Iterator<T> iterator,
            @Independent @Modified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        static <T> T find(
            @Independent @Modified Iterator<? extends T> iterator,
            @Independent @Modified Predicate<? super T> predicate,
            @Independent @NotModified T defaultValue) { return null; }

        @Independent @NotModified
        static <T> UnmodifiableIterator<T> forArray(@Independent @NotModified T ... array) { return null; }

        @Independent @NotModified
        static <T> UnmodifiableIterator<T> forEnumeration(@Independent @Modified Enumeration<T> enumeration) {
            return null;
        }

        @NotModified
        static int frequency(@Independent @Modified Iterator<?> iterator, @Independent @NotModified Object element) {
            return 0;
        }

        @Independent @NotModified
        static <T> T get(@Independent @Modified Iterator<T> iterator, int position) { return null; }

        @Independent @NotModified
        static <T> T get(
            @Independent @Modified Iterator<? extends T> iterator,
            int position,
            @Independent @NotModified T defaultValue) { return null; }
        @Independent @NotModified static <T> T getLast(@Independent @Modified Iterator<T> iterator) { return null; }
        @Independent @NotModified
        static <T> T getLast(
            @Independent @Modified Iterator<? extends T> iterator,
            @Independent @NotModified T defaultValue) { return null; }

        @Independent @NotModified
        static <T> T getNext(
            @Independent @Modified Iterator<? extends T> iterator,
            @Independent @NotModified T defaultValue) { return null; }

        @Independent @NotModified
        static <T> T getOnlyElement(@Independent @Modified Iterator<T> iterator) { return null; }

        @Independent @NotModified
        static <T> T getOnlyElement(
            @Independent @Modified Iterator<? extends T> iterator,
            @Independent @NotModified T defaultValue) { return null; }

        @NotModified
        static <T> int indexOf(
            @Independent @Modified Iterator<T> iterator,
            @Independent @Modified Predicate<? super T> predicate) { return 0; }

        @Independent @NotModified
        static <T> Iterator<T> limit(@Independent @Modified Iterator<T> iterator, int limitSize) { return null; }

        @Independent @NotModified
        static <T> UnmodifiableIterator<T> mergeSorted(
            @Independent @NotModified Iterable<? extends Iterator<? extends T>> iterators,
            @Independent @NotModified Comparator<? super T> comparator) { return null; }

        @Independent @NotModified
        static <T> UnmodifiableIterator<List<T>> paddedPartition(@Independent @Modified Iterator<T> iterator, int size) {
            return null;
        }

        @Independent @NotModified
        static <T> UnmodifiableIterator<List<T>> partition(@Independent @Modified Iterator<T> iterator, int size) {
            return null;
        }

        @Identity @Independent @NotModified
        static <T> PeekingIterator<T> peekingIterator(@Independent @NotModified PeekingIterator<T> iterator) {
            return null;
        }

        @Independent @NotModified
        static <T> PeekingIterator<T> peekingIterator(@Independent @Modified Iterator<? extends T> iterator) {
            return null;
        }

        @NotModified
        static boolean removeAll(
            @Independent @Modified Iterator<?> removeFrom,
            @Independent @NotModified Collection<?> elementsToRemove) { return false; }

        @NotModified
        static <T> boolean removeIf(
            @Independent @Modified Iterator<T> removeFrom,
            @Independent @Modified Predicate<? super T> predicate) { return false; }

        @NotModified
        static boolean retainAll(
            @Independent @Modified Iterator<?> removeFrom,
            @Independent @NotModified Collection<?> elementsToRetain) { return false; }

        @Independent @NotModified
        static <T> UnmodifiableIterator<T> singletonIterator(@Independent @NotModified T value) { return null; }
        @NotModified static int size(@Independent @Modified Iterator<?> iterator) { return 0; }
        @Independent @NotModified
        static <T> T [] toArray(@Independent @Modified Iterator<? extends T> iterator, Class<T> type) { return null; }
        @NotModified static String toString(@Independent @Modified Iterator<?> iterator) { return null; }
        @Independent @NotModified
        static <F, T> Iterator<T> transform(
            @Independent @NotModified Iterator<F> fromIterator,
            @Independent @Modified Function<? super F, ? extends T> function) { return null; }

        @Independent @NotModified
        static <T> Optional<T> tryFind(
            @Independent @Modified Iterator<T> iterator,
            @Independent @Modified Predicate<? super T> predicate) { return null; }

        @Identity @Independent @NotModified
        static <T> UnmodifiableIterator<T> unmodifiableIterator(
            @Independent @NotModified UnmodifiableIterator<T> iterator) { return null; }

        @Independent @NotModified
        static <T> UnmodifiableIterator<T> unmodifiableIterator(@Independent @Modified Iterator<? extends T> iterator) {
            return null;
        }
    }

    //public final class LinkedHashMultimap extends AbstractSetMultimap<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class LinkedHashMultimap$<K, V> {
        @Independent @NotModified static <K, V> LinkedHashMultimap<K, V> create() { return null; }
        @Independent @NotModified
        static <K, V> LinkedHashMultimap<K, V> create(
            @Independent @NotModified Multimap<? extends K, ? extends V> multimap) { return null; }

        @Independent @NotModified
        static <K, V> LinkedHashMultimap<K, V> create(int expectedKeys, int expectedValuesPerKey) { return null; }

        //override from com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractMultimap, com.google.common.collect.AbstractSetMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent(absent = true) @NotModified
        Set<Map.Entry<K, V>> entries() { return null; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @Independent(absent = true) @NotModified
        Set<K> keySet() { return null; }

        //override from com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractMultimap, com.google.common.collect.AbstractSetMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent @Modified

        Set<V> replaceValues(
            @Independent(hc = true) @NotModified K key,
            @Independent @NotModified Iterable<? extends V> values) { return null; }

        //override from com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @Independent(absent = true) @NotModified
        Collection<V> values() { return null; }
    }

    //public final class LinkedHashMultiset extends AbstractMapBasedMultiset<E>
    //annotated as EXPECTED; computed mutable @Dependent @Container (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class LinkedHashMultiset$<E> {
        @Independent @NotModified static <E> LinkedHashMultiset<E> create() { return null; }
        @Independent @NotModified
        static <E> LinkedHashMultiset<E> create(@Independent @NotModified Iterable<? extends E> elements) { return null; }
        @Independent @NotModified static <E> LinkedHashMultiset<E> create(int distinctElements) { return null; }
    }

    //public class LinkedListMultimap extends AbstractMultimap<K,V> implements ListMultimap<K,V>, Serializable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class LinkedListMultimap$<K, V> {
        //override from com.google.common.collect.Multimap
        @Modified
        void clear() { }

        //override from com.google.common.collect.Multimap
        @NotModified
        boolean containsKey(@Independent @NotModified Object key) { return false; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @NotModified
        boolean containsValue(@Independent @NotModified Object value) { return false; }
        @Independent @NotModified static <K, V> LinkedListMultimap<K, V> create() { return null; }
        @Independent @NotModified
        static <K, V> LinkedListMultimap<K, V> create(
            @Independent @NotModified Multimap<? extends K, ? extends V> multimap) { return null; }
        @Independent @NotModified static <K, V> LinkedListMultimap<K, V> create(int expectedKeys) { return null; }
        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @Independent(absent = true) @NotModified
        List<Map.Entry<K, V>> entries() { return null; }

        //override from com.google.common.collect.ListMultimap, com.google.common.collect.Multimap
        @Independent @NotModified
        List<V> get(@Independent @NotModified K key) { return null; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @NotModified
        boolean isEmpty() { return false; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @Modified
        boolean put(@Independent(hc = true) @NotModified K key, @Independent @NotModified V value) { return false; }

        //override from com.google.common.collect.ListMultimap, com.google.common.collect.Multimap
        @Independent @Modified
        List<V> removeAll(@Independent @NotModified Object key) { return null; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.ListMultimap, com.google.common.collect.Multimap
        @Independent @Modified

        List<V> replaceValues(@Independent @NotModified K key, @Independent @NotModified Iterable<? extends V> values) {
            return null;
        }

        //override from com.google.common.collect.Multimap
        @NotModified @GetSet("size")
        int size() { return 0; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.Multimap
        @Independent(absent = true) @NotModified
        List<V> values() { return null; }
    }

    //public interface ListMultimap implements Multimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a collection contract with mutators, shared by mutable and immutable implementations (GUAVA.md)
    @Container
    @Independent(absent = true)
    class ListMultimap$<K, V> {
        //override from com.google.common.collect.Multimap
        @Independent(hc = true) @NotModified
        Map<K, Collection<V>> asMap() { return null; }

        //override from com.google.common.collect.Multimap, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object obj) { return false; }

        //override from com.google.common.collect.Multimap
        @Independent(absent = true) @NotModified
        List<V> get(@Independent(hc = true) @NotModified K key) { return null; }

        //override from com.google.common.collect.Multimap
        @Independent(hc = true) @Modified
        List<V> removeAll(@Independent @NotModified Object key) { return null; }

        //override from com.google.common.collect.Multimap
        @Independent @Modified

        List<V> replaceValues(
            @Independent(hc = true) @NotModified K key,
            @Independent(hc = true) @NotModified Iterable<? extends V> values) { return null; }
    }

    //public final class Lists
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @UtilityClass
    class Lists$ {
        @Independent @NotModified
        static <E> List<E> asList(
            @Independent @NotModified E first,
            @Independent @NotModified E second,
            @Independent @NotModified E [] rest) { return null; }

        @Independent @NotModified
        static <E> List<E> asList(@Independent @NotModified E first, @Independent @NotModified E [] rest) { return null; }

        @Independent @NotModified
        static <B> List<List<B>> cartesianProduct(@Independent @NotModified List<? extends List<? extends B>> lists) {
            return null;
        }

        @Independent @NotModified
        static <B> List<List<B>> cartesianProduct(@Independent @NotModified List<? extends B> ... lists) { return null; }

        @Independent @NotModified
        static List<Character> charactersOf(@Independent @NotModified CharSequence sequence) { return null; }
        @Independent @NotModified static ImmutableList<Character> charactersOf(String string) { return null; }
        @Independent @NotModified static <E> ArrayList<E> newArrayList() { return null; }
        @Independent @NotModified
        static <E> ArrayList<E> newArrayList(@Independent @NotModified Iterable<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ArrayList<E> newArrayList(@Independent @NotModified E ... elements) { return null; }

        @Independent @NotModified
        static <E> ArrayList<E> newArrayList(@Independent @Modified Iterator<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> ArrayList<E> newArrayListWithCapacity(int initialArraySize) { return null; }

        @Independent @NotModified
        static <E> ArrayList<E> newArrayListWithExpectedSize(int estimatedSize) { return null; }
        @Independent @NotModified static <E> CopyOnWriteArrayList<E> newCopyOnWriteArrayList() { return null; }
        @Independent @NotModified
        static <E> CopyOnWriteArrayList<E> newCopyOnWriteArrayList(
            @Independent @Modified Iterable<? extends E> elements) { return null; }
        @Independent @NotModified static <E> LinkedList<E> newLinkedList() { return null; }
        @Independent @NotModified
        static <E> LinkedList<E> newLinkedList(@Independent @NotModified Iterable<? extends E> elements) { return null; }

        @Independent @NotModified
        static <T> List<List<T>> partition(@Independent @NotModified List<T> list, int size) { return null; }
        @Independent @NotModified static <T> List<T> reverse(@Independent @Modified List<T> list) { return null; }
        @Independent @NotModified
        static <F, T> List<T> transform(
            @Independent @Modified List<F> fromList,
            @Independent @NotModified Function<? super F, ? extends T> function) { return null; }
    }

    //public interface MapDifference
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- a value (GUAVA.md)
    @Immutable(hc = true)
    class MapDifference$<K, V> {
        //interface ValueDifference
        @ImmutableContainer(hc = true)
        class ValueDifference<V> {
            //override from java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object other) { return false; }

            //override from java.lang.Object
            @NotModified
            public int hashCode() { return 0; }
            @Independent(hc = true) @NotModified V leftValue() { return null; }
            @Independent(hc = true) @NotModified V rightValue() { return null; }
        }
        @NotModified boolean areEqual() { return false; }
        @Independent(hc = true) @NotModified
        Map<K, MapDifference.ValueDifference<V>> entriesDiffering() { return null; }
        @Independent(hc = true) @NotModified Map<K, V> entriesInCommon() { return null; }
        @Independent(hc = true) @NotModified Map<K, V> entriesOnlyOnLeft() { return null; }
        @Independent(hc = true) @NotModified Map<K, V> entriesOnlyOnRight() { return null; }
        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object object) { return false; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }
    }

    //public final class MapMaker
    @Independent
    class MapMaker$ {
        MapMaker$() { }
        @Fluent @Independent @Modified MapMaker concurrencyLevel(int concurrencyLevel) { return null; }
        @Fluent @Independent @Modified MapMaker initialCapacity(int initialCapacity) { return null; }
        @Fluent @Independent @Modified @NotModified(after = "keyEquivalence")
        <K, V> ConcurrentMap<K, V> makeMap() { return null; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }
        @Fluent @Independent @Modified MapMaker weakKeys() { return null; }
        @Fluent @Independent @Modified MapMaker weakValues() { return null; }
    }

    //public final class Maps
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @UtilityClass
    class Maps$ {
        //public interface EntryTransformer
        @FinalFields
        @Container
        @Independent
        class EntryTransformer<K, V1, V2> {
            @Independent(absent = true) @Modified
            V2 transformEntry(
                @Independent(absent = true) @NotModified K key,
                @Independent(absent = true) @NotModified V1 value) { return null; }
        }

        @Independent @NotModified
        static <A, B> Converter<A, B> asConverter(@Independent @Modified BiMap<A, B> bimap) { return null; }

        @Independent @NotModified
        static <K, V> NavigableMap<K, V> asMap(
            @Independent @Modified NavigableSet<K> set,
            @Independent @NotModified Function<? super K, V> function) { return null; }

        @Independent @NotModified
        static <K, V> Map<K, V> asMap(
            @Independent @Modified Set<K> set,
            @Independent @NotModified Function<? super K, V> function) { return null; }

        @Independent @NotModified
        static <K, V> SortedMap<K, V> asMap(
            @Independent @NotModified SortedSet<K> set,
            @Independent @NotModified Function<? super K, V> function) { return null; }

        @Independent @NotModified
        static <K, V> MapDifference<K, V> difference(
            @Independent @NotModified Map<? extends K, ? extends V> left,
            @Independent @NotModified Map<? extends K, ? extends V> right) { return null; }

        @Independent @NotModified
        static <K, V> MapDifference<K, V> difference(
            @Independent @NotModified Map<? extends K, ? extends V> left,
            @Independent @NotModified Map<? extends K, ? extends V> right,
            @Independent @Modified Equivalence<? super V> valueEquivalence) { return null; }

        @Independent @NotModified
        static <K, V> SortedMapDifference<K, V> difference(
            @Independent @NotModified SortedMap<K, ? extends V> left,
            @Independent @NotModified Map<? extends K, ? extends V> right) { return null; }

        @Independent @NotModified
        static <K, V> BiMap<K, V> filterEntries(
            @Independent @Modified BiMap<K, V> unfiltered,
            @Independent @Modified Predicate<? super Map.Entry<K, V>> entryPredicate) { return null; }

        @Independent @NotModified
        static <K, V> Map<K, V> filterEntries(
            @Independent @NotModified Map<K, V> unfiltered,
            @Independent @NotModified Predicate<? super Map.Entry<K, V>> entryPredicate) { return null; }

        @Independent @NotModified
        static <K, V> NavigableMap<K, V> filterEntries(
            @Independent @Modified NavigableMap<K, V> unfiltered,
            @Independent @Modified Predicate<? super Map.Entry<K, V>> entryPredicate) { return null; }

        @Independent @NotModified
        static <K, V> SortedMap<K, V> filterEntries(
            @Independent @NotModified SortedMap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super Map.Entry<K, V>> entryPredicate) { return null; }

        @Independent @NotModified
        static <K, V> BiMap<K, V> filterKeys(
            @Independent @Modified BiMap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super K> keyPredicate) { return null; }

        @Independent @NotModified
        static <K, V> Map<K, V> filterKeys(
            @Independent @NotModified Map<K, V> unfiltered,
            @Independent @Modified Predicate<? super K> keyPredicate) { return null; }

        @Independent @NotModified
        static <K, V> NavigableMap<K, V> filterKeys(
            @Independent @Modified NavigableMap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super K> keyPredicate) { return null; }

        @Independent @NotModified
        static <K, V> SortedMap<K, V> filterKeys(
            @Independent @NotModified SortedMap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super K> keyPredicate) { return null; }

        @Independent @NotModified
        static <K, V> BiMap<K, V> filterValues(
            @Independent @Modified BiMap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super V> valuePredicate) { return null; }

        @Independent @NotModified
        static <K, V> Map<K, V> filterValues(
            @Independent @NotModified Map<K, V> unfiltered,
            @Independent @NotModified Predicate<? super V> valuePredicate) { return null; }

        @Independent @NotModified
        static <K, V> NavigableMap<K, V> filterValues(
            @Independent @Modified NavigableMap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super V> valuePredicate) { return null; }

        @Independent @NotModified
        static <K, V> SortedMap<K, V> filterValues(
            @Independent @NotModified SortedMap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super V> valuePredicate) { return null; }

        @Independent @NotModified
        static ImmutableMap<String, String> fromProperties(@Independent @Modified Properties properties) { return null; }

        @Independent @NotModified
        static <K, V> Map.Entry<K, V> immutableEntry(@Independent @NotModified K key, @Independent @NotModified V value) {
            return null;
        }

        @Independent @NotModified
        static <K extends Enum<K>, V> ImmutableMap<K, V> immutableEnumMap(
            @Independent @NotModified Map<K, ? extends V> map) { return null; }
        @Independent @NotModified static <K, V> ConcurrentMap<K, V> newConcurrentMap() { return null; }
        @Independent @NotModified
        static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(Class<K> type) { return null; }

        @Independent @NotModified
        static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(@Independent @NotModified Map<K, ? extends V> map) {
            return null;
        }
        @Independent @NotModified static <K, V> HashMap<K, V> newHashMap() { return null; }
        @Independent @NotModified
        static <K, V> HashMap<K, V> newHashMap(@Independent @NotModified Map<? extends K, ? extends V> map) {
            return null;
        }

        @Independent @NotModified
        static <K, V> HashMap<K, V> newHashMapWithExpectedSize(int expectedSize) { return null; }
        @Independent @NotModified static <K, V> IdentityHashMap<K, V> newIdentityHashMap() { return null; }
        @Independent @NotModified static <K, V> LinkedHashMap<K, V> newLinkedHashMap() { return null; }
        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> newLinkedHashMap(@Independent @NotModified Map<? extends K, ? extends V> map) {
            return null;
        }

        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> newLinkedHashMapWithExpectedSize(int expectedSize) { return null; }
        @Independent @NotModified static <K extends Comparable, V> TreeMap<K, V> newTreeMap() { return null; }
        @Independent @NotModified
        static <C, K extends C, V> TreeMap<K, V> newTreeMap(@Independent @NotModified Comparator<C> comparator) {
            return null;
        }

        @Independent @NotModified
        static <K, V> TreeMap<K, V> newTreeMap(@Independent @NotModified SortedMap<K, ? extends V> map) { return null; }

        @Identity @Independent @NotModified
        static <K extends Comparable<? super K>, V> NavigableMap<K, V> subMap(
            @Independent @NotModified NavigableMap<K, V> map,
            @Independent @NotModified Range<K> range) { return null; }

        @Independent @NotModified
        static <K, V> BiMap<K, V> synchronizedBiMap(@Independent @NotModified BiMap<K, V> bimap) { return null; }

        @Independent @NotModified
        static <K, V> NavigableMap<K, V> synchronizedNavigableMap(
            @Independent @NotModified NavigableMap<K, V> navigableMap) { return null; }

        @Independent @NotModified
        static <T, K extends Enum<K>, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableEnumMap(
            @Independent @Modified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @Modified java.util.function.Function<? super T, ? extends V> valueFunction) { return null; }

        @Independent @NotModified
        static <T, K extends Enum<K>, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableEnumMap(
            @Independent @Modified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @Modified java.util.function.Function<? super T, ? extends V> valueFunction,
            @Independent @NotModified BinaryOperator<V> mergeFunction) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> toMap(
            @Independent @NotModified Iterable<K> keys,
            @Independent @Modified Function<? super K, V> valueFunction) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> toMap(
            @Independent @Modified Iterator<K> keys,
            @Independent @Modified Function<? super K, V> valueFunction) { return null; }

        @Independent @NotModified
        static <K, V1, V2> Map<K, V2> transformEntries(
            @Independent @Modified Map<K, V1> fromMap,
            @Independent @NotModified Maps.EntryTransformer<? super K, ? super V1, V2> transformer) { return null; }

        @Independent @NotModified
        static <K, V1, V2> NavigableMap<K, V2> transformEntries(
            @Independent @NotModified NavigableMap<K, V1> fromMap,
            @Independent @NotModified Maps.EntryTransformer<? super K, ? super V1, V2> transformer) { return null; }

        @Independent @NotModified
        static <K, V1, V2> SortedMap<K, V2> transformEntries(
            @Independent @NotModified SortedMap<K, V1> fromMap,
            @Independent @NotModified Maps.EntryTransformer<? super K, ? super V1, V2> transformer) { return null; }

        @Independent @NotModified
        static <K, V1, V2> Map<K, V2> transformValues(
            @Independent @Modified Map<K, V1> fromMap,
            @Independent @Modified Function<? super V1, V2> function) { return null; }

        @Independent @NotModified
        static <K, V1, V2> NavigableMap<K, V2> transformValues(
            @Independent @NotModified NavigableMap<K, V1> fromMap,
            @Independent @Modified Function<? super V1, V2> function) { return null; }

        @Independent @NotModified
        static <K, V1, V2> SortedMap<K, V2> transformValues(
            @Independent @NotModified SortedMap<K, V1> fromMap,
            @Independent @Modified Function<? super V1, V2> function) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> uniqueIndex(
            @Independent @NotModified Iterable<V> values,
            @Independent @Modified Function<? super V, K> keyFunction) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableMap<K, V> uniqueIndex(
            @Independent @Modified Iterator<V> values,
            @Independent @Modified Function<? super V, K> keyFunction) { return null; }

        @Independent @NotModified
        static <K, V> BiMap<K, V> unmodifiableBiMap(@Independent @Modified BiMap<? extends K, ? extends V> bimap) {
            return null;
        }

        @Independent @NotModified
        static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(
            @Independent @Modified NavigableMap<K, ? extends V> map) { return null; }
    }

    //public final class MinMaxPriorityQueue extends AbstractQueue<E>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class MinMaxPriorityQueue$<E> {
        //public static final class Builder
        @Container
        @Independent
        class Builder<B> {
            @Independent @NotModified <T extends B> MinMaxPriorityQueue<T> create() { return null; }
            @Independent @NotModified
            <T extends B> MinMaxPriorityQueue<T> create(@Independent @NotModified Iterable<? extends T> initialContents) {
                return null;
            }

            @Fluent @Independent(absent = true) @Modified @GetSet("expectedSize")
            MinMaxPriorityQueue.Builder<B> expectedSize(int expectedSize) { return null; }

            @Fluent @Independent(absent = true) @Modified @GetSet("maximumSize")
            MinMaxPriorityQueue.Builder<B> maximumSize(int maximumSize) { return null; }
        }

        //override from java.util.AbstractCollection, java.util.AbstractQueue, java.util.Collection, java.util.Queue
        @Modified
        boolean add(@Independent @NotModified E element) { return false; }

        //override from java.util.AbstractCollection, java.util.AbstractQueue, java.util.Collection
        @Modified
        boolean addAll(@Independent @NotModified Collection<? extends E> newElements) { return false; }

        //override from java.util.AbstractCollection, java.util.AbstractQueue, java.util.Collection
        @Modified
        void clear() { }

        @Independent(absent = true) @NotModified @GetSet("ordering")
        Comparator<? super E> comparator() { return null; }
        @Independent @NotModified static <E extends Comparable<E>> MinMaxPriorityQueue<E> create() { return null; }
        @Independent @NotModified
        static <E extends Comparable<E>> MinMaxPriorityQueue<E> create(
            @Independent @NotModified Iterable<? extends E> initialContents) { return null; }

        @Independent @NotModified
        static MinMaxPriorityQueue.Builder<Comparable> expectedSize(int expectedSize) { return null; }

        //override from java.lang.Iterable, java.util.AbstractCollection, java.util.Collection
        @Independent @NotModified
        Iterator<E> iterator() { return null; }

        @Independent @NotModified
        static MinMaxPriorityQueue.Builder<Comparable> maximumSize(int maximumSize) { return null; }

        //override from java.util.Queue
        @Modified
        boolean offer(@Independent @NotModified E element) { return false; }

        @Independent @NotModified
        static <B> MinMaxPriorityQueue.Builder<B> orderedBy(@Independent @NotModified Comparator<B> comparator) {
            return null;
        }

        //override from java.util.Queue
        @Independent(hc = true) @NotModified
        E peek() { return null; }
        @Independent(hc = true) @NotModified E peekFirst() { return null; }
        @Independent(hc = true) @Modified @NotModified(after = "maxHeap") E peekLast() { return null; }
        //override from java.util.Queue
        @Independent(hc = true) @Modified
        E poll() { return null; }
        @Independent(hc = true) @Modified E pollFirst() { return null; }
        @Independent(hc = true) @Modified E pollLast() { return null; }
        @Independent @Modified E removeFirst() { return null; }
        @Independent(hc = true) @Modified E removeLast() { return null; }
        //override from java.util.AbstractCollection, java.util.Collection
        @NotModified @GetSet("size")
        int size() { return 0; }

        //override from java.util.AbstractCollection, java.util.Collection
        @Independent @NotModified
        Object [] toArray() { return null; }
    }

    //public final class MoreCollectors
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- static methods only (GUAVA.md)
    @Container
    @UtilityClass
    class MoreCollectors$ {
        @Independent @NotModified static <T> Collector<T, ?, T> onlyElement() { return null; }
        @Independent @NotModified static <T> Collector<T, ?, java.util.Optional<T>> toOptional() { return null; }
    }

    //public interface Multimap
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a collection contract with mutators, shared by mutable and immutable implementations (GUAVA.md)
    @Container
    @Independent(absent = true)
    class Multimap$<K, V> {
        @Independent(absent = true) @NotModified Map<K, Collection<V>> asMap() { return null; }
        @Modified void clear() { }
        @NotModified
        boolean containsEntry(@Independent @NotModified Object key, @Independent @NotModified Object value) {
            return false;
        }
        @NotModified boolean containsKey(@Independent @NotModified Object key) { return false; }
        @NotModified boolean containsValue(@Independent @NotModified Object value) { return false; }
        @Independent(absent = true) @NotModified Collection<Map.Entry<K, V>> entries() { return null; }
        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object obj) { return false; }
        @Modified void forEach(@Independent @NotModified BiConsumer<? super K, ? super V> action) { }
        @Independent(absent = true) @NotModified
        Collection<V> get(@Independent(hc = true) @NotModified K key) { return null; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }
        @NotModified boolean isEmpty() { return false; }
        @Independent(absent = true) @NotModified Set<K> keySet() { return null; }
        @Independent(absent = true) @NotModified Multiset<K> keys() { return null; }
        @Modified
        boolean put(@Independent(hc = true) @NotModified K key, @Independent(hc = true) @NotModified V value) {
            return false;
        }

        @Modified
        boolean putAll(
            @Independent(hc = true) @NotModified K key,
            @Independent(hc = true) @NotModified Iterable<? extends V> values) { return false; }

        @Modified
        boolean putAll(@Independent @NotModified Multimap<? extends K, ? extends V> multimap) { return false; }

        @Modified
        boolean remove(@Independent(hc = true) @NotModified Object key, @Independent @NotModified Object value) {
            return false;
        }

        @Independent(absent = true) @Modified
        Collection<V> removeAll(@Independent @NotModified Object key) { return null; }

        @Independent @Modified
        Collection<V> replaceValues(
            @Independent(hc = true) @NotModified K key,
            @Independent(hc = true) @NotModified Iterable<? extends V> values) { return null; }
        @NotModified int size() { return 0; }
        @Independent(absent = true) @NotModified Collection<V> values() { return null; }
    }

    //public abstract class MultimapBuilder
    @Immutable(hc = true)
    @Independent
    class MultimapBuilder$<K0, V0> {
        //public abstract static class ListMultimapBuilder extends MultimapBuilder<K0,V0>
        @Immutable(hc = true)
        @Independent
        class ListMultimapBuilder<K0, V0> {
            //override from com.google.common.collect.MultimapBuilder
            @Independent @NotModified
            <K extends K0, V extends V0> ListMultimap<K, V> build() { return null; }

            //override from com.google.common.collect.MultimapBuilder
            @Independent @NotModified

            <K extends K0, V extends V0> ListMultimap<K, V> build(
                @Independent @Modified Multimap<? extends K, ? extends V> multimap) { return null; }
        }

        //public abstract static class MultimapBuilderWithKeys
        @ImmutableContainer(hc = true)
        @Independent
        class MultimapBuilderWithKeys<K0> {
            @Independent @NotModified
            MultimapBuilder.ListMultimapBuilder<K0, Object> arrayListValues() { return null; }

            @Independent @NotModified
            MultimapBuilder.ListMultimapBuilder<K0, Object> arrayListValues(int expectedValuesPerKey) { return null; }

            @Independent @NotModified
            <V0 extends Enum<V0>> MultimapBuilder.SetMultimapBuilder<K0, V0> enumSetValues(Class<V0> valueClass) {
                return null;
            }
            @Independent @NotModified MultimapBuilder.SetMultimapBuilder<K0, Object> hashSetValues() { return null; }
            @Independent @NotModified
            MultimapBuilder.SetMultimapBuilder<K0, Object> hashSetValues(int expectedValuesPerKey) { return null; }

            @Independent @NotModified
            MultimapBuilder.SetMultimapBuilder<K0, Object> linkedHashSetValues() { return null; }

            @Independent @NotModified
            MultimapBuilder.SetMultimapBuilder<K0, Object> linkedHashSetValues(int expectedValuesPerKey) { return null; }

            @Independent @NotModified
            MultimapBuilder.ListMultimapBuilder<K0, Object> linkedListValues() { return null; }

            @Independent @NotModified
            MultimapBuilder.SortedSetMultimapBuilder<K0, Comparable> treeSetValues() { return null; }

            @Independent @NotModified
            <V0> MultimapBuilder.SortedSetMultimapBuilder<K0, V0> treeSetValues(
                @Independent @NotModified Comparator<V0> comparator) { return null; }
        }

        //public abstract static class SetMultimapBuilder extends MultimapBuilder<K0,V0>
        @Immutable(hc = true)
        @Independent
        class SetMultimapBuilder<K0, V0> {
            //override from com.google.common.collect.MultimapBuilder
            @Independent @NotModified
            <K extends K0, V extends V0> SetMultimap<K, V> build() { return null; }

            //override from com.google.common.collect.MultimapBuilder
            @Independent @NotModified

            <K extends K0, V extends V0> SetMultimap<K, V> build(
                @Independent @Modified Multimap<? extends K, ? extends V> multimap) { return null; }
        }

        //public abstract static class SortedSetMultimapBuilder extends SetMultimapBuilder<K0,V0>
        @Immutable(hc = true)
        @Independent
        class SortedSetMultimapBuilder<K0, V0> {
            //override from com.google.common.collect.MultimapBuilder, com.google.common.collect.MultimapBuilder.SetMultimapBuilder
            @Independent @NotModified
            <K extends K0, V extends V0> SortedSetMultimap<K, V> build() { return null; }

            //override from com.google.common.collect.MultimapBuilder, com.google.common.collect.MultimapBuilder.SetMultimapBuilder
            @Independent @NotModified

            <K extends K0, V extends V0> SortedSetMultimap<K, V> build(
                @Independent @Modified Multimap<? extends K, ? extends V> multimap) { return null; }
        }
        @Independent @NotModified <K extends K0, V extends V0> Multimap<K, V> build() { return null; }
        @Independent @NotModified
        <K extends K0, V extends V0> Multimap<K, V> build(
            @Independent @Modified Multimap<? extends K, ? extends V> multimap) { return null; }

        @Independent @NotModified
        static <K0 extends Enum<K0>> MultimapBuilder.MultimapBuilderWithKeys<K0> enumKeys(Class<K0> keyClass) {
            return null;
        }
        @Independent @NotModified static MultimapBuilder.MultimapBuilderWithKeys<Object> hashKeys() { return null; }
        @Independent @NotModified
        static MultimapBuilder.MultimapBuilderWithKeys<Object> hashKeys(int expectedKeys) { return null; }

        @Independent @NotModified
        static MultimapBuilder.MultimapBuilderWithKeys<Object> linkedHashKeys() { return null; }

        @Independent @NotModified
        static MultimapBuilder.MultimapBuilderWithKeys<Object> linkedHashKeys(int expectedKeys) { return null; }

        @Independent @NotModified
        static MultimapBuilder.MultimapBuilderWithKeys<Comparable> treeKeys() { return null; }

        @Independent @NotModified
        static <K0> MultimapBuilder.MultimapBuilderWithKeys<K0> treeKeys(
            @Independent @NotModified Comparator<K0> comparator) { return null; }
    }

    //public final class Multimaps
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @UtilityClass
    class Multimaps$ {
        @Independent @NotModified
        static <K, V> Map<K, List<V>> asMap(@Independent @Modified ListMultimap<K, V> multimap) { return null; }

        @Independent @NotModified
        static <K, V> Map<K, Collection<V>> asMap(@Independent @Modified Multimap<K, V> multimap) { return null; }

        @Independent @NotModified
        static <K, V> Map<K, Set<V>> asMap(@Independent @Modified SetMultimap<K, V> multimap) { return null; }

        @Independent @NotModified
        static <K, V> Map<K, SortedSet<V>> asMap(@Independent @Modified SortedSetMultimap<K, V> multimap) { return null; }

        @Independent @NotModified
        static <K, V> Multimap<K, V> filterEntries(
            @Independent @Modified Multimap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super Map.Entry<K, V>> entryPredicate) { return null; }

        @Independent @NotModified
        static <K, V> SetMultimap<K, V> filterEntries(
            @Independent @NotModified SetMultimap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super Map.Entry<K, V>> entryPredicate) { return null; }

        @Independent @NotModified
        static <K, V> ListMultimap<K, V> filterKeys(
            @Independent @NotModified ListMultimap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super K> keyPredicate) { return null; }

        @Independent @NotModified
        static <K, V> Multimap<K, V> filterKeys(
            @Independent @Modified Multimap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super K> keyPredicate) { return null; }

        @Independent @NotModified
        static <K, V> SetMultimap<K, V> filterKeys(
            @Independent @NotModified SetMultimap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super K> keyPredicate) { return null; }

        @Independent @NotModified
        static <K, V> Multimap<K, V> filterValues(
            @Independent @Modified Multimap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super V> valuePredicate) { return null; }

        @Independent @NotModified
        static <K, V> SetMultimap<K, V> filterValues(
            @Independent @NotModified SetMultimap<K, V> unfiltered,
            @Independent @NotModified Predicate<? super V> valuePredicate) { return null; }

        @Independent @NotModified
        static <T, K, V, M extends Multimap<K, V>> Collector<T, ?, M> flatteningToMultimap(
            @Independent @Modified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @Modified java.util.function.Function<? super T, ? extends Stream<? extends V>> valueFunction,
            @Independent @NotModified Supplier<M> multimapSupplier) { return null; }

        @Independent @NotModified
        static <K, V> SetMultimap<K, V> forMap(@Independent @Modified Map<K, V> map) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableListMultimap<K, V> index(
            @Independent @NotModified Iterable<V> values,
            @Independent @Modified Function<? super V, K> keyFunction) { return null; }

        @Independent @NotModified
        static <K, V> ImmutableListMultimap<K, V> index(
            @Independent @Modified Iterator<V> values,
            @Independent @Modified Function<? super V, K> keyFunction) { return null; }

        @Independent @NotModified
        static <K, V, M extends Multimap<K, V>> M invertFrom(
            @Independent @Modified Multimap<? extends V, ? extends K> source,
            @Independent @Modified M dest) { return null; }

        @Independent @NotModified
        static <K, V> ListMultimap<K, V> newListMultimap(
            @Independent @NotModified Map<K, Collection<V>> map,
            @Independent @NotModified com.google.common.base.Supplier<? extends List<V>> factory) { return null; }

        @Independent @NotModified
        static <K, V> Multimap<K, V> newMultimap(
            @Independent @NotModified Map<K, Collection<V>> map,
            @Independent @NotModified com.google.common.base.Supplier<? extends Collection<V>> factory) { return null; }

        @Independent @NotModified
        static <K, V> SetMultimap<K, V> newSetMultimap(
            @Independent @NotModified Map<K, Collection<V>> map,
            @Independent @NotModified com.google.common.base.Supplier<? extends Set<V>> factory) { return null; }

        @Independent @NotModified
        static <K, V> SortedSetMultimap<K, V> newSortedSetMultimap(
            @Independent @NotModified Map<K, Collection<V>> map,
            @Independent @Modified com.google.common.base.Supplier<? extends SortedSet<V>> factory) { return null; }

        @Independent @NotModified
        static <K, V> ListMultimap<K, V> synchronizedListMultimap(@Independent @NotModified ListMultimap<K, V> multimap) {
            return null;
        }

        @Independent @NotModified
        static <K, V> Multimap<K, V> synchronizedMultimap(@Independent @NotModified Multimap<K, V> multimap) {
            return null;
        }

        @Independent @NotModified
        static <K, V> SetMultimap<K, V> synchronizedSetMultimap(@Independent @NotModified SetMultimap<K, V> multimap) {
            return null;
        }

        @Independent @NotModified
        static <K, V> SortedSetMultimap<K, V> synchronizedSortedSetMultimap(
            @Independent @NotModified SortedSetMultimap<K, V> multimap) { return null; }

        @Independent @NotModified
        static <T, K, V, M extends Multimap<K, V>> Collector<T, ?, M> toMultimap(
            @Independent @Modified java.util.function.Function<? super T, ? extends K> keyFunction,
            @Independent @Modified java.util.function.Function<? super T, ? extends V> valueFunction,
            @Independent @NotModified Supplier<M> multimapSupplier) { return null; }

        @Independent @NotModified
        static <K, V1, V2> ListMultimap<K, V2> transformEntries(
            @Independent @NotModified ListMultimap<K, V1> fromMap,
            @Independent @NotModified Maps.EntryTransformer<? super K, ? super V1, V2> transformer) { return null; }

        @Independent @NotModified
        static <K, V1, V2> Multimap<K, V2> transformEntries(
            @Independent @Modified Multimap<K, V1> fromMap,
            @Independent @NotModified Maps.EntryTransformer<? super K, ? super V1, V2> transformer) { return null; }

        @Independent @NotModified
        static <K, V1, V2> ListMultimap<K, V2> transformValues(
            @Independent @NotModified ListMultimap<K, V1> fromMultimap,
            @Independent @Modified Function<? super V1, V2> function) { return null; }

        @Independent @NotModified
        static <K, V1, V2> Multimap<K, V2> transformValues(
            @Independent @Modified Multimap<K, V1> fromMultimap,
            @Independent @Modified Function<? super V1, V2> function) { return null; }

        @Identity @Independent @NotModified
        static <K, V> ListMultimap<K, V> unmodifiableListMultimap(
            @Independent @NotModified ImmutableListMultimap<K, V> delegate) { return null; }

        @Independent @NotModified
        static <K, V> ListMultimap<K, V> unmodifiableListMultimap(@Independent @NotModified ListMultimap<K, V> delegate) {
            return null;
        }

        @Identity @Independent @NotModified
        static <K, V> Multimap<K, V> unmodifiableMultimap(@Independent @NotModified ImmutableMultimap<K, V> delegate) {
            return null;
        }

        @Independent @NotModified
        static <K, V> Multimap<K, V> unmodifiableMultimap(@Independent @Modified Multimap<K, V> delegate) { return null; }

        @Identity @Independent @NotModified
        static <K, V> SetMultimap<K, V> unmodifiableSetMultimap(
            @Independent @NotModified ImmutableSetMultimap<K, V> delegate) { return null; }

        @Independent @NotModified
        static <K, V> SetMultimap<K, V> unmodifiableSetMultimap(@Independent @NotModified SetMultimap<K, V> delegate) {
            return null;
        }

        @Independent @NotModified
        static <K, V> SortedSetMultimap<K, V> unmodifiableSortedSetMultimap(
            @Independent @NotModified SortedSetMultimap<K, V> delegate) { return null; }
    }

    //public interface Multiset implements Collection<E>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a collection contract with mutators, shared by mutable and immutable implementations (GUAVA.md)
    @Container
    @Independent(absent = true)
    class Multiset$<E> {
        //interface Entry
        @FinalFields
        @Independent(hc = true)
        @Immutable(hc = true, after = "comparator")
        class Entry<E> {
            //override from java.lang.Object
            @NotModified
            public boolean equals(@Independent @Modified Object o) { return false; }
            @Modified @NotModified(after = "comparator") int getCount() { return 0; }
            @Independent(hc = true) @NotModified E getElement() { return null; }
            //override from java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from java.lang.Object
            @NotModified @NotNull
            public String toString() { return null; }
        }

        //override from java.util.Collection
        @Modified
        boolean add(@Independent @NotModified E element) { return false; }
        @Modified int add(@Independent(hc = true) @NotModified E element, int occurrences) { return 0; }
        //override from java.util.Collection
        @NotModified
        boolean contains(@Independent @NotModified Object element) { return false; }

        //override from java.util.Collection
        @NotModified
        boolean containsAll(@Independent(absent = true) @NotModified Collection<?> elements) { return false; }
        @NotModified int count(@Independent @NotModified Object element) { return 0; }
        @Independent(absent = true) @NotModified Set<E> elementSet() { return null; }
        @Independent(absent = true) @NotModified Set<Multiset.Entry<E>> entrySet() { return null; }
        //override from java.lang.Object, java.util.Collection
        @NotModified
        public boolean equals(@Independent @NotModified Object object) { return false; }

        //override from java.lang.Iterable
        @Modified
        void forEach(@Independent @NotModified Consumer<? super E> action) { }
        @Modified void forEachEntry(@Independent @NotModified ObjIntConsumer<? super E> action) { }
        //override from java.lang.Object, java.util.Collection
        @NotModified
        public int hashCode() { return 0; }

        //override from java.lang.Iterable, java.util.Collection
        @Independent(hc = true) @NotModified @NotNull
        Iterator<E> iterator() { return null; }

        //override from java.util.Collection
        @Modified
        boolean remove(@Independent @NotModified Object element) { return false; }
        @Modified int remove(@Independent @NotModified Object element, int occurrences) { return 0; }
        //override from java.util.Collection
        @Modified
        boolean removeAll(@Independent @NotModified Collection<?> c) { return false; }

        //override from java.util.Collection
        @Modified
        boolean retainAll(@Independent @NotModified Collection<?> c) { return false; }
        @Modified int setCount(@Independent(hc = true) @NotModified E element, int count) { return 0; }
        @Modified
        boolean setCount(@Independent(hc = true) @NotModified E element, int oldCount, int newCount) { return false; }

        //override from java.util.Collection
        @NotModified
        int size() { return 0; }

        //override from java.lang.Iterable, java.util.Collection
        @Independent @NotModified
        Spliterator<E> spliterator() { return null; }

        //override from java.lang.Object
        @NotModified @NotNull
        public String toString() { return null; }
    }

    //public final class Multisets
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @UtilityClass
    class Multisets$ {
        @NotModified
        static boolean containsOccurrences(
            @Independent @Modified Multiset<?> superMultiset,
            @Independent @Modified Multiset<?> subMultiset) { return false; }

        @Independent @NotModified
        static <E> ImmutableMultiset<E> copyHighestCountFirst(@Independent @Modified Multiset<E> multiset) {
            return null;
        }

        @Independent @NotModified
        static <E> Multiset<E> difference(
            @Independent @Modified Multiset<E> multiset1,
            @Independent @Modified Multiset<?> multiset2) { return null; }

        @Independent @NotModified
        static <E> Multiset<E> filter(
            @Independent @Modified Multiset<E> unfiltered,
            @Independent @NotModified Predicate<? super E> predicate) { return null; }

        @Independent @NotModified
        static <E> Multiset.Entry<E> immutableEntry(@Independent @NotModified E e, int n) { return null; }

        @Independent @NotModified
        static <E> Multiset<E> intersection(
            @Independent @Modified Multiset<E> multiset1,
            @Independent @Modified Multiset<?> multiset2) { return null; }

        @NotModified
        static boolean removeOccurrences(
            @Independent @Modified Multiset<?> multisetToModify,
            @Independent @Modified Iterable<?> occurrencesToRemove) { return false; }

        @NotModified
        static boolean removeOccurrences(
            @Independent @Modified Multiset<?> multisetToModify,
            @Independent @Modified Multiset<?> occurrencesToRemove) { return false; }

        @NotModified
        static boolean retainOccurrences(
            @Independent @Modified Multiset<?> multisetToModify,
            @Independent @Modified Multiset<?> multisetToRetain) { return false; }

        @Independent @NotModified
        static <E> Multiset<E> sum(
            @Independent @Modified Multiset<? extends E> multiset1,
            @Independent @Modified Multiset<? extends E> multiset2) { return null; }

        @Independent @NotModified
        static <T, E, M extends Multiset<E>> Collector<T, ?, M> toMultiset(
            @Independent @Modified java.util.function.Function<? super T, E> elementFunction,
            @Independent @Modified ToIntFunction<? super T> countFunction,
            @Independent @NotModified Supplier<M> multisetSupplier) { return null; }

        @Independent @NotModified
        static <E> Multiset<E> union(
            @Independent @Modified Multiset<? extends E> multiset1,
            @Independent @Modified Multiset<? extends E> multiset2) { return null; }

        @Identity @Independent @NotModified
        static <E> Multiset<E> unmodifiableMultiset(@Independent @NotModified ImmutableMultiset<E> multiset) {
            return null;
        }

        @Independent @NotModified
        static <E> Multiset<E> unmodifiableMultiset(@Independent @Modified Multiset<? extends E> multiset) {
            return null;
        }

        @Independent @NotModified
        static <E> SortedMultiset<E> unmodifiableSortedMultiset(
            @Independent @NotModified SortedMultiset<E> sortedMultiset) { return null; }
    }

    //public final class MutableClassToInstanceMap extends ForwardingMap<Class<? extends B>,B> implements ClassToInstanceMap<B>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class MutableClassToInstanceMap$<B> {
        @Independent @NotModified static <B> MutableClassToInstanceMap<B> create() { return null; }
        @Independent @NotModified
        static <B> MutableClassToInstanceMap<B> create(@Independent @NotModified Map<Class<? extends B>, B> backingMap) {
            return null;
        }

        //override from com.google.common.collect.ForwardingMap, java.util.Map
        @Independent @NotModified
        Set<Map.Entry<Class<? extends B>, B>> entrySet() { return null; }

        //override from com.google.common.collect.ClassToInstanceMap
        @Independent @Modified
        <T extends B> T getInstance(Class<T> type) { return null; }

        //override from com.google.common.collect.ForwardingMap, java.util.Map
        @Independent @Modified
        B put(Class<? extends B> key, @Independent @NotModified B value) { return null; }

        //override from com.google.common.collect.ForwardingMap, java.util.Map
        @Modified
        void putAll(@Independent @NotModified Map<? extends Class<? extends B>, ? extends B> map) { }

        //override from com.google.common.collect.ClassToInstanceMap
        @Independent @Modified
        <T extends B> T putInstance(Class<T> type, @Independent @NotModified T value) { return null; }
    }

    //public final class ObjectArrays
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @UtilityClass
    class ObjectArrays$ {
        @Independent @NotModified
        static <T> T [] concat(@Independent @NotModified T element, @Independent @NotModified T [] array) { return null; }

        @Independent @NotModified
        static <T> T [] concat(@Independent @NotModified T [] array, @Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> T [] concat(
            @Independent @NotModified T [] first,
            @Independent @NotModified T [] second,
            Class<T> type) { return null; }
        @Independent @NotModified static <T> T [] newArray(Class<T> type, int length) { return null; }
        @Independent @NotModified
        static <T> T [] newArray(@Independent @NotModified T [] reference, int length) { return null; }
    }

    //public abstract class Ordering implements Comparator<T>
    @FinalFields
    @Independent(hc = true)
    class Ordering$<T> {
        @Independent @NotModified static Ordering<Object> allEqual() { return null; }
        @Independent @NotModified static Ordering<Object> arbitrary() { return null; }
        @NotModified
        int binarySearch(@Independent @Modified List<? extends T> sortedList, @Independent @NotModified T key) {
            return 0;
        }

        //override from java.util.Comparator
        @Modified
        int compare(@Independent(hc = true) @Modified T left, @Independent(hc = true) @Modified T right) { return 0; }

        @Independent @NotModified
        static <T> Ordering<T> compound(@Independent @NotModified Iterable<? extends Comparator<? super T>> comparators) {
            return null;
        }

        @Fluent @Independent @NotModified
        <U extends T> Ordering<U> compound(@Independent @NotModified Comparator<? super U> secondaryComparator) {
            return null;
        }

        @Independent @NotModified
        static <T> Ordering<T> explicit(
            @Independent @NotModified T leastValue,
            @Independent @NotModified T ... remainingValuesInOrder) { return null; }

        @Independent @NotModified
        static <T> Ordering<T> explicit(@Independent @NotModified List<T> valuesInOrder) { return null; }

        @Identity @Independent @NotModified
        static <T> Ordering<T> from(@Independent @NotModified Ordering<T> ordering) { return null; }

        @Independent @NotModified
        static <T> Ordering<T> from(@Independent @NotModified Comparator<T> comparator) { return null; }

        @Independent @Modified
        <E extends T> List<E> greatestOf(@Independent @NotModified Iterable<E> iterable, int k) { return null; }

        @Independent @Modified
        <E extends T> List<E> greatestOf(@Independent @Modified Iterator<E> iterator, int k) { return null; }

        @Independent @NotModified
        <E extends T> ImmutableList<E> immutableSortedCopy(@Independent @NotModified Iterable<E> elements) {
            return null;
        }
        @Modified boolean isOrdered(@Independent @NotModified Iterable<? extends T> iterable) { return false; }
        @Modified boolean isStrictlyOrdered(@Independent @NotModified Iterable<? extends T> iterable) { return false; }
        @Independent @NotModified
        <E extends T> List<E> leastOf(@Independent @NotModified Iterable<E> iterable, int k) { return null; }

        @Independent @NotModified
        <E extends T> List<E> leastOf(@Independent @Modified Iterator<E> iterator, int k) { return null; }
        @Fluent @Independent @NotModified <S extends T> Ordering<Iterable<S>> lexicographical() { return null; }
        @Independent @Modified <E extends T> E max(@Independent @NotModified Iterable<E> iterable) { return null; }
        //override from java.util.Comparator
        @Independent @Modified
        <E extends T> E max(@Independent @Modified E a, @Independent @Modified E b) { return null; }

        @Independent @Modified
        <E extends T> E max(
            @Independent @Modified E a,
            @Independent @Modified E b,
            @Independent @Modified E c,
            @Independent @Modified E ... rest) { return null; }
        @Independent @Modified <E extends T> E max(@Independent @Modified Iterator<E> iterator) { return null; }
        @Independent @Modified <E extends T> E min(@Independent @NotModified Iterable<E> iterable) { return null; }
        //override from java.util.Comparator
        @Independent @Modified
        <E extends T> E min(@Independent @Modified E a, @Independent @Modified E b) { return null; }

        @Independent @Modified
        <E extends T> E min(
            @Independent @Modified E a,
            @Independent @Modified E b,
            @Independent @Modified E c,
            @Independent @Modified E ... rest) { return null; }
        @Independent @Modified <E extends T> E min(@Independent @Modified Iterator<E> iterator) { return null; }
        @Independent @NotModified static <C extends Comparable> Ordering<C> natural() { return null; }
        @Fluent @Independent @Modified <S extends T> Ordering<S> nullsFirst() { return null; }
        @Fluent @Independent @Modified <S extends T> Ordering<S> nullsLast() { return null; }
        @Fluent @Independent @Modified
        <F> Ordering<F> onResultOf(@Independent @NotModified Function<F, ? extends T> function) { return null; }
        @Fluent @Independent @NotModified <S extends T> Ordering<S> reverse() { return null; }
        @Independent @NotModified
        <E extends T> List<E> sortedCopy(@Independent @NotModified Iterable<E> elements) { return null; }
        @Independent @NotModified static Ordering<Object> usingToString() { return null; }
    }

    //public interface PeekingIterator implements Iterator<E>
    @FinalFields
    @Container
    @Independent(hc = true)
    class PeekingIterator$<E> {
        //override from java.util.Iterator
        @Independent @Modified
        E next() { return null; }
        @Independent(hc = true) @Modified E peek() { return null; }
        //override from java.util.Iterator
        @Modified
        void remove() { }
    }

    //public final class Queues
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @UtilityClass
    class Queues$ {
        @NotModified
        static <E> int drain(
            @Independent @Modified BlockingQueue<E> q,
            @Independent @Modified Collection<? super E> buffer,
            int numElements,
            @Independent @NotModified Duration timeout) { return 0; }

        @NotModified
        static <E> int drain(
            @Independent @Modified BlockingQueue<E> q,
            @Independent @Modified Collection<? super E> buffer,
            int numElements,
            long timeout,
            @Independent @Modified TimeUnit unit) { return 0; }

        @NotModified
        static <E> int drainUninterruptibly(
            @Independent @Modified BlockingQueue<E> q,
            @Independent @Modified Collection<? super E> buffer,
            int numElements,
            @Independent @NotModified Duration timeout) { return 0; }

        @NotModified
        static <E> int drainUninterruptibly(
            @Independent @Modified BlockingQueue<E> q,
            @Independent @Modified Collection<? super E> buffer,
            int numElements,
            long timeout,
            @Independent @Modified TimeUnit unit) { return 0; }
        @Independent @NotModified static <E> ArrayBlockingQueue<E> newArrayBlockingQueue(int capacity) { return null; }
        @Independent @NotModified static <E> ArrayDeque<E> newArrayDeque() { return null; }
        @Independent @NotModified
        static <E> ArrayDeque<E> newArrayDeque(@Independent @NotModified Iterable<? extends E> elements) { return null; }
        @Independent @NotModified static <E> ConcurrentLinkedQueue<E> newConcurrentLinkedQueue() { return null; }
        @Independent @NotModified
        static <E> ConcurrentLinkedQueue<E> newConcurrentLinkedQueue(
            @Independent @Modified Iterable<? extends E> elements) { return null; }
        @Independent @NotModified static <E> LinkedBlockingDeque<E> newLinkedBlockingDeque() { return null; }
        @Independent @NotModified
        static <E> LinkedBlockingDeque<E> newLinkedBlockingDeque(@Independent @Modified Iterable<? extends E> elements) {
            return null;
        }

        @Independent @NotModified
        static <E> LinkedBlockingDeque<E> newLinkedBlockingDeque(int capacity) { return null; }
        @Independent @NotModified static <E> LinkedBlockingQueue<E> newLinkedBlockingQueue() { return null; }
        @Independent @NotModified
        static <E> LinkedBlockingQueue<E> newLinkedBlockingQueue(@Independent @Modified Iterable<? extends E> elements) {
            return null;
        }

        @Independent @NotModified
        static <E> LinkedBlockingQueue<E> newLinkedBlockingQueue(int capacity) { return null; }

        @Independent @NotModified
        static <E extends Comparable> PriorityBlockingQueue<E> newPriorityBlockingQueue() { return null; }

        @Independent @NotModified
        static <E extends Comparable> PriorityBlockingQueue<E> newPriorityBlockingQueue(
            @Independent @Modified Iterable<? extends E> elements) { return null; }
        @Independent @NotModified static <E extends Comparable> PriorityQueue<E> newPriorityQueue() { return null; }
        @Independent @NotModified
        static <E extends Comparable> PriorityQueue<E> newPriorityQueue(
            @Independent @NotModified Iterable<? extends E> elements) { return null; }
        @Independent @NotModified static <E> SynchronousQueue<E> newSynchronousQueue() { return null; }
        @Independent @NotModified
        static <E> Deque<E> synchronizedDeque(@Independent @NotModified Deque<E> deque) { return null; }

        @Independent @NotModified
        static <E> Queue<E> synchronizedQueue(@Independent @NotModified Queue<E> queue) { return null; }
    }

    //public final class Range implements Predicate<C>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a value (GUAVA.md)
    @Immutable(hc = true)
    class Range$<C extends Comparable> {
        @Independent @NotModified static <C extends Comparable<?>> Range<C> all() { return null; }
        //override from com.google.common.base.Predicate
        @NotModified
        boolean apply(@Independent @NotModified C input) { return false; }

        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> atLeast(@Independent @NotModified C endpoint) { return null; }

        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> atMost(@Independent @NotModified C endpoint) { return null; }

        @Fluent @Independent @NotModified
        Range<C> canonical(@Independent @NotModified DiscreteDomain<C> domain) { return null; }

        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> closed(
            @Independent @NotModified C lower,
            @Independent @NotModified C upper) { return null; }

        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> closedOpen(
            @Independent @NotModified C lower,
            @Independent @NotModified C upper) { return null; }
        @NotModified boolean contains(@Independent @NotModified C value) { return false; }
        @NotModified
        boolean containsAll(@Independent(hc = true) @Modified Iterable<? extends C> values) { return false; }

        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> downTo(
            @Independent @NotModified C endpoint,
            @Independent @NotModified BoundType boundType) { return null; }

        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> encloseAll(@Independent @NotModified Iterable<C> values) {
            return null;
        }
        @NotModified boolean encloses(@Independent @NotModified Range<C> other) { return false; }
        //override from com.google.common.base.Predicate, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object object) { return false; }

        @Fluent @Independent @NotModified
        Range<C> gap(@Independent @Modified @NotModified(after = "lowerBound,upperBound") Range<C> otherRange) {
            return null;
        }

        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> greaterThan(@Independent @NotModified C endpoint) { return null; }
        @NotModified boolean hasLowerBound() { return false; }
        @NotModified boolean hasUpperBound() { return false; }
        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Fluent @Independent @NotModified
        Range<C> intersection(
            @Independent(hc = true) @Modified @NotModified(after = "lowerBound,upperBound") Range<C> connectedRange) {
            return null;
        }
        @NotModified boolean isConnected(@Independent @NotModified Range<C> other) { return false; }
        @NotModified boolean isEmpty() { return false; }
        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> lessThan(@Independent @NotModified C endpoint) { return null; }
        @Independent @NotModified BoundType lowerBoundType() { return null; }
        @Independent(hc = true) @NotModified C lowerEndpoint() { return null; }
        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> open(
            @Independent @NotModified C lower,
            @Independent @NotModified C upper) { return null; }

        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> openClosed(
            @Independent @NotModified C lower,
            @Independent @NotModified C upper) { return null; }

        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> range(
            @Independent @NotModified C lower,
            @Independent @NotModified BoundType lowerType,
            @Independent @NotModified C upper,
            @Independent @NotModified BoundType upperType) { return null; }

        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> singleton(@Independent @NotModified C value) { return null; }

        @Fluent @Independent @NotModified
        Range<C> span(@Independent(hc = true) @Modified @NotModified(after = "lowerBound,upperBound") Range<C> other) {
            return null;
        }

        //override from com.google.common.base.Predicate, java.util.function.Predicate
        @NotModified
        boolean test(@Independent @NotModified C input) { return false; }

        //override from java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent @NotModified
        static <C extends Comparable<?>> Range<C> upTo(
            @Independent @NotModified C endpoint,
            @Independent @NotModified BoundType boundType) { return null; }
        @Independent @NotModified BoundType upperBoundType() { return null; }
        @Independent(hc = true) @NotModified C upperEndpoint() { return null; }
    }

    //public interface RangeMap
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a collection contract with mutators, shared by mutable and immutable implementations (GUAVA.md)
    @Container
    @Independent(absent = true)
    class RangeMap$<K extends Comparable, V> {
        @Independent @Modified @NotModified(after = "ranges,values")
        Map<Range<K>, V> asDescendingMapOfRanges() { return null; }

        @Independent(absent = true) @Modified @NotModified(after = "ranges,values")
        Map<Range<K>, V> asMapOfRanges() { return null; }
        @Modified void clear() { }
        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }
        @Independent(hc = true) @NotModified V get(@Independent @NotModified K key) { return null; }
        @Independent(absent = true) @Modified
        Map.Entry<Range<K>, V> getEntry(@Independent @NotModified K key) { return null; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Modified
        void merge(
            @Independent(hc = true) @NotModified Range<K> range,
            @Independent @NotModified V value,
            @IgnoreModifications @Independent @NotModified BiFunction<? super V, ? super V, ? extends V>
                remappingFunction) { }
        @Modified void put(@Independent(hc = true) @NotModified Range<K> range, @Independent @NotModified V value) { }
        @Modified void putAll(@Independent @NotModified RangeMap<K, ? extends V> rangeMap) { }
        @Modified
        void putCoalescing(@Independent(hc = true) @NotModified Range<K> range, @Independent @NotModified V value) { }
        @Modified void remove(@Independent(hc = true) @NotModified Range<K> range) { }
        @Independent(hc = true) @NotModified Range<K> span() { return null; }
        @Independent(absent = true) @Modified
        RangeMap<K, V> subRangeMap(@Independent(absent = true) @NotModified Range<K> range) { return null; }

        //override from java.lang.Object
        @NotModified @NotNull
        public String toString() { return null; }
    }

    //public interface RangeSet
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a collection contract with mutators, shared by mutable and immutable implementations (GUAVA.md)
    @Container
    @Independent(absent = true)
    class RangeSet$<C extends Comparable> {
        @Modified void add(@Independent(hc = true) @NotModified Range<C> range) { }
        @Modified void addAll(@Independent @NotModified Iterable<Range<C>> ranges) { }
        @Modified void addAll(@Independent @NotModified RangeSet<C> other) { }
        @Independent(absent = true) @NotModified Set<Range<C>> asDescendingSetOfRanges() { return null; }
        @Independent(absent = true) @NotModified Set<Range<C>> asRanges() { return null; }
        @Modified void clear() { }
        @Independent(absent = true) @NotModified RangeSet<C> complement() { return null; }
        @NotModified boolean contains(@Independent @NotModified C value) { return false; }
        @NotModified boolean encloses(@Independent @NotModified Range<C> otherRange) { return false; }
        @NotModified boolean enclosesAll(@Independent @NotModified Iterable<Range<C>> other) { return false; }
        @NotModified boolean enclosesAll(@Independent @NotModified RangeSet<C> other) { return false; }
        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object obj) { return false; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }
        @NotModified boolean intersects(@Independent @NotModified Range<C> otherRange) { return false; }
        @NotModified boolean isEmpty() { return false; }
        @Independent(absent = true) @NotModified
        Range<C> rangeContaining(@Independent @NotModified C value) { return null; }
        @Modified void remove(@Independent(hc = true) @NotModified Range<C> range) { }
        @Modified void removeAll(@Independent @NotModified Iterable<Range<C>> ranges) { }
        @Modified void removeAll(@Independent @NotModified RangeSet<C> other) { }
        @Independent @NotModified Range<C> span() { return null; }
        @Independent @NotModified
        RangeSet<C> subRangeSet(@Independent(absent = true) @NotModified Range<C> view) { return null; }

        //override from java.lang.Object
        @NotModified @NotNull
        public String toString() { return null; }
    }

    //public interface RowSortedTable implements Table<R,C,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- a collection contract with mutators, shared by mutable and immutable implementations (GUAVA.md)
    @Container
    @Independent(absent = true)
    class RowSortedTable$<R, C, V> {
        //override from com.google.common.collect.Table
        @Independent(hc = true) @NotModified
        SortedSet<R> rowKeySet() { return null; }

        //override from com.google.common.collect.Table
        @Independent(absent = true) @NotModified
        SortedMap<R, Map<C, V>> rowMap() { return null; }
    }

    //public interface SetMultimap implements Multimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a collection contract with mutators, shared by mutable and immutable implementations (GUAVA.md)
    @Container
    @Independent(absent = true)
    class SetMultimap$<K, V> {
        //override from com.google.common.collect.Multimap
        @Independent(absent = true) @NotModified
        Map<K, Collection<V>> asMap() { return null; }

        //override from com.google.common.collect.Multimap
        @Independent(absent = true) @NotModified
        Set<Map.Entry<K, V>> entries() { return null; }

        //override from com.google.common.collect.Multimap, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object obj) { return false; }

        //override from com.google.common.collect.Multimap
        @Independent(absent = true) @NotModified
        Set<V> get(@Independent(hc = true) @NotModified K key) { return null; }

        //override from com.google.common.collect.Multimap
        @Independent(absent = true) @Modified
        Set<V> removeAll(@Independent @NotModified Object key) { return null; }

        //override from com.google.common.collect.Multimap
        @Independent @Modified

        Set<V> replaceValues(
            @Independent(hc = true) @NotModified K key,
            @Independent(hc = true) @NotModified Iterable<? extends V> values) { return null; }
    }

    //public final class Sets
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @UtilityClass
    class Sets$ {
        //public abstract static class SetView extends AbstractSet<E>
        @FinalFields
        @Independent(absent = true)
        class SetView<E> {
            //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
            @NotModified
            boolean add(@Independent @NotModified E e) { return false; }

            //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
            @NotModified
            boolean addAll(@Independent @NotModified Collection<? extends E> newElements) { return false; }

            //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
            @NotModified
            void clear() { }

            @Identity @Independent @NotModified
            <S extends Set<E>> S copyInto(@Independent @Modified S set) { return null; }

            //override from java.lang.Object, java.util.AbstractSet, java.util.Collection, java.util.Set
            @NotModified
            public boolean equals(@Independent @NotModified Object object) { return false; }
            @Independent @NotModified ImmutableSet<E> immutableCopy() { return null; }
            //override from java.lang.Iterable, java.util.AbstractCollection, java.util.Collection, java.util.Set
            @Independent @NotModified @NotNull
            UnmodifiableIterator<E> iterator() { return null; }

            //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
            @NotModified
            boolean remove(@Independent @NotModified Object object) { return false; }

            //override from java.util.AbstractCollection, java.util.AbstractSet, java.util.Collection, java.util.Set
            @NotModified
            boolean removeAll(@Independent @NotModified Collection<?> oldElements) { return false; }

            //override from java.util.Collection
            @NotModified
            boolean removeIf(@Independent @NotModified java.util.function.Predicate<? super E> filter) { return false; }

            //override from java.util.AbstractCollection, java.util.Collection, java.util.Set
            @NotModified
            boolean retainAll(@Independent @NotModified Collection<?> elementsToKeep) { return false; }
        }

        @Independent @NotModified
        static <B> Set<List<B>> cartesianProduct(@Independent @NotModified List<? extends Set<? extends B>> sets) {
            return null;
        }

        @Independent @NotModified
        static <B> Set<List<B>> cartesianProduct(@Independent @NotModified Set<? extends B> ... sets) { return null; }

        @Independent @NotModified
        static <E> Set<Set<E>> combinations(@Independent @NotModified Set<E> set, int size) { return null; }

        @Independent @NotModified
        static <E extends Enum<E>> EnumSet<E> complementOf(@Independent @Modified Collection<E> collection) {
            return null;
        }

        @Independent @NotModified
        static <E extends Enum<E>> EnumSet<E> complementOf(
            @Independent @NotModified Collection<E> collection,
            Class<E> type) { return null; }

        @Independent @NotModified
        static <E> Sets.SetView<E> difference(
            @Independent @NotModified Set<E> set1,
            @Independent @NotModified Set<?> set2) { return null; }

        @Independent @NotModified
        static <E> NavigableSet<E> filter(
            @Independent @NotModified NavigableSet<E> unfiltered,
            @Independent @NotModified Predicate<? super E> predicate) { return null; }

        @Independent @NotModified
        static <E> Set<E> filter(
            @Independent @NotModified Set<E> unfiltered,
            @Independent @NotModified Predicate<? super E> predicate) { return null; }

        @Independent @NotModified
        static <E> SortedSet<E> filter(
            @Independent @NotModified SortedSet<E> unfiltered,
            @Independent @NotModified Predicate<? super E> predicate) { return null; }

        @Independent @NotModified
        static <E extends Enum<E>> ImmutableSet<E> immutableEnumSet(
            @Independent @NotModified E anElement,
            @Independent @NotModified E ... otherElements) { return null; }

        @Independent @NotModified
        static <E extends Enum<E>> ImmutableSet<E> immutableEnumSet(@Independent @NotModified Iterable<E> elements) {
            return null;
        }

        @Independent @NotModified
        static <E> Sets.SetView<E> intersection(@Independent @Modified Set<E> set1, @Independent @Modified Set<?> set2) {
            return null;
        }
        @Independent @NotModified static <E> Set<E> newConcurrentHashSet() { return null; }
        @Independent @NotModified
        static <E> Set<E> newConcurrentHashSet(@Independent @NotModified Iterable<? extends E> elements) { return null; }
        @Independent @NotModified static <E> CopyOnWriteArraySet<E> newCopyOnWriteArraySet() { return null; }
        @Independent @NotModified
        static <E> CopyOnWriteArraySet<E> newCopyOnWriteArraySet(@Independent @Modified Iterable<? extends E> elements) {
            return null;
        }

        @Independent @NotModified
        static <E extends Enum<E>> EnumSet<E> newEnumSet(
            @Independent @NotModified Iterable<E> iterable,
            Class<E> elementType) { return null; }
        @Independent @NotModified static <E> HashSet<E> newHashSet() { return null; }
        @Independent @NotModified
        static <E> HashSet<E> newHashSet(@Independent @NotModified Iterable<? extends E> elements) { return null; }

        @Independent @NotModified
        static <E> HashSet<E> newHashSet(@Independent @NotModified E ... elements) { return null; }

        @Independent @NotModified
        static <E> HashSet<E> newHashSet(@Independent @Modified Iterator<? extends E> elements) { return null; }
        @Independent @NotModified static <E> HashSet<E> newHashSetWithExpectedSize(int expectedSize) { return null; }
        @Independent @NotModified static <E> Set<E> newIdentityHashSet() { return null; }
        @Independent @NotModified static <E> LinkedHashSet<E> newLinkedHashSet() { return null; }
        @Independent @NotModified
        static <E> LinkedHashSet<E> newLinkedHashSet(@Independent @NotModified Iterable<? extends E> elements) {
            return null;
        }

        @Independent @NotModified
        static <E> LinkedHashSet<E> newLinkedHashSetWithExpectedSize(int expectedSize) { return null; }

        @Independent @NotModified
        static <E> Set<E> newSetFromMap(@Independent @Modified Map<E, Boolean> map) { return null; }
        @Independent @NotModified static <E extends Comparable> TreeSet<E> newTreeSet() { return null; }
        @Independent @NotModified
        static <E extends Comparable> TreeSet<E> newTreeSet(@Independent @NotModified Iterable<? extends E> elements) {
            return null;
        }

        @Independent @NotModified
        static <E> TreeSet<E> newTreeSet(@Independent @NotModified Comparator<? super E> comparator) { return null; }

        @Independent @NotModified
        static <E> Set<Set<E>> powerSet(@Independent @NotModified Set<E> set) { return null; }

        @Identity @Independent @NotModified
        static <K extends Comparable<? super K>> NavigableSet<K> subSet(
            @Independent @NotModified NavigableSet<K> set,
            @Independent @NotModified Range<K> range) { return null; }

        @Independent @NotModified
        static <E> Sets.SetView<E> symmetricDifference(
            @Independent @NotModified Set<? extends E> set1,
            @Independent @NotModified Set<? extends E> set2) { return null; }

        @Independent @NotModified
        static <E> NavigableSet<E> synchronizedNavigableSet(@Independent @NotModified NavigableSet<E> navigableSet) {
            return null;
        }

        @Independent @NotModified
        static <E extends Enum<E>> Collector<E, ?, ImmutableSet<E>> toImmutableEnumSet() { return null; }

        @Independent @NotModified
        static <E> Sets.SetView<E> union(
            @Independent @NotModified Set<? extends E> set1,
            @Independent @NotModified Set<? extends E> set2) { return null; }

        @Independent @NotModified
        static <E> NavigableSet<E> unmodifiableNavigableSet(@Independent @Modified NavigableSet<E> set) { return null; }
    }

    //public interface SortedMapDifference implements MapDifference<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- a value (GUAVA.md)
    @Immutable(hc = true)
    class SortedMapDifference$<K, V> {
        //override from com.google.common.collect.MapDifference
        @Independent(hc = true) @NotModified
        SortedMap<K, MapDifference.ValueDifference<V>> entriesDiffering() { return null; }

        //override from com.google.common.collect.MapDifference
        @Independent(hc = true) @NotModified
        SortedMap<K, V> entriesInCommon() { return null; }

        //override from com.google.common.collect.MapDifference
        @Independent(hc = true) @NotModified
        SortedMap<K, V> entriesOnlyOnLeft() { return null; }

        //override from com.google.common.collect.MapDifference
        @Independent(hc = true) @NotModified
        SortedMap<K, V> entriesOnlyOnRight() { return null; }
    }

    //public interface SortedMultiset implements SortedMultisetBridge<E>, SortedIterable<E>
    //annotated as EXPECTED; computed @FinalFields @Independent @Container (1) -- a collection contract with mutators, shared by mutable and immutable implementations (GUAVA.md)
    @Container
    @Independent
    class SortedMultiset$<E> {
        //override from com.google.common.collect.SortedIterable
        @Independent(absent = true) @NotModified
        Comparator<? super E> comparator() { return null; }
        @Independent(absent = true) @Modified SortedMultiset<E> descendingMultiset() { return null; }
        //override from com.google.common.collect.Multiset, com.google.common.collect.SortedMultisetBridge
        @Independent(absent = true) @NotModified
        NavigableSet<E> elementSet() { return null; }

        //override from com.google.common.collect.Multiset
        @Independent(absent = true) @NotModified
        Set<Multiset.Entry<E>> entrySet() { return null; }
        @Independent(hc = true) @Modified Multiset.Entry<E> firstEntry() { return null; }
        @Independent(absent = true) @Modified
        SortedMultiset<E> headMultiset(
            @Independent(hc = true) @NotModified E upperBound,
            @Independent @NotModified BoundType boundType) { return null; }

        //override from com.google.common.collect.Multiset, com.google.common.collect.SortedIterable, java.lang.Iterable, java.util.Collection
        @Independent @NotModified @NotNull
        Iterator<E> iterator() { return null; }
        @Independent(hc = true) @Modified Multiset.Entry<E> lastEntry() { return null; }
        @Independent @Modified Multiset.Entry<E> pollFirstEntry() { return null; }
        @Independent @Modified Multiset.Entry<E> pollLastEntry() { return null; }
        @Independent @Modified
        SortedMultiset<E> subMultiset(
            @Independent(hc = true) @NotModified E lowerBound,
            @Independent @NotModified BoundType lowerBoundType,
            @Independent(hc = true) @NotModified E upperBound,
            @Independent @NotModified BoundType upperBoundType) { return null; }

        @Independent(absent = true) @Modified
        SortedMultiset<E> tailMultiset(
            @Independent(hc = true) @NotModified E lowerBound,
            @Independent @NotModified BoundType boundType) { return null; }
    }

    //public interface SortedSetMultimap implements SetMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a collection contract with mutators, shared by mutable and immutable implementations (GUAVA.md)
    @Container
    @Independent(absent = true)
    class SortedSetMultimap$<K, V> {
        //override from com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent(absent = true) @NotModified
        Map<K, Collection<V>> asMap() { return null; }

        //override from com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent @NotModified
        SortedSet<V> get(@Independent(hc = true) @NotModified K key) { return null; }

        //override from com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent @Modified
        SortedSet<V> removeAll(@Independent @NotModified Object key) { return null; }

        //override from com.google.common.collect.Multimap, com.google.common.collect.SetMultimap
        @Independent @Modified

        SortedSet<V> replaceValues(
            @Independent(hc = true) @NotModified K key,
            @Independent(hc = true) @NotModified Iterable<? extends V> values) { return null; }
        @Independent(hc = true) @Modified Comparator<? super V> valueComparator() { return null; }
    }

    //public final class Streams
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @UtilityClass
    class Streams$ {
        //public interface DoubleFunctionWithIndex
        @FinalFields
        @Container
        @Independent
        class DoubleFunctionWithIndex<R> {
            @Independent(absent = true) @Modified R apply(double from, long index) { return null; }
        }

        //public interface FunctionWithIndex
        @FinalFields
        @Container
        @Independent
        class FunctionWithIndex<T, R> {
            @Independent(absent = true) @Modified
            R apply(@Independent(absent = true) @NotModified T from, long index) { return null; }
        }

        //public interface IntFunctionWithIndex
        @FinalFields
        @Container
        @Independent
        class IntFunctionWithIndex<R> {
            @Independent(absent = true) @Modified R apply(int from, long index) { return null; }
        }

        //public interface LongFunctionWithIndex
        @FinalFields
        @Container
        @Independent
        class LongFunctionWithIndex<R> {
            @Independent(absent = true) @Modified R apply(long from, long index) { return null; }
        }

        @Independent @NotModified
        static DoubleStream concat(@Independent @NotModified DoubleStream ... streams) { return null; }

        @Independent @NotModified
        static IntStream concat(@Independent @NotModified IntStream ... streams) { return null; }

        @Independent @NotModified
        static LongStream concat(@Independent @NotModified LongStream ... streams) { return null; }

        @Independent @NotModified
        static <T> Stream<T> concat(@Independent @NotModified Stream<? extends T> ... streams) { return null; }

        @Independent @NotModified
        static OptionalDouble findLast(@Independent @NotModified DoubleStream stream) { return null; }
        @Independent @NotModified static OptionalInt findLast(@Independent @Modified IntStream stream) { return null; }
        @Independent @NotModified
        static OptionalLong findLast(@Independent @Modified LongStream stream) { return null; }

        @Independent @NotModified
        static <T> java.util.Optional<T> findLast(@Independent @Modified Stream<T> stream) { return null; }

        @NotModified
        static <A, B> void forEachPair(
            @Independent @Modified Stream<A> streamA,
            @Independent @Modified Stream<B> streamB,
            @Independent @Modified BiConsumer<? super A, ? super B> consumer) { }

        @Independent @NotModified
        static <R> Stream<R> mapWithIndex(
            @Independent @Modified DoubleStream stream,
            @Independent @Modified Streams.DoubleFunctionWithIndex<R> function) { return null; }

        @Independent @NotModified
        static <R> Stream<R> mapWithIndex(
            @Independent @Modified IntStream stream,
            @Independent @Modified Streams.IntFunctionWithIndex<R> function) { return null; }

        @Independent @NotModified
        static <R> Stream<R> mapWithIndex(
            @Independent @Modified LongStream stream,
            @Independent @Modified Streams.LongFunctionWithIndex<R> function) { return null; }

        @Independent @NotModified
        static <T, R> Stream<R> mapWithIndex(
            @Independent @Modified Stream<T> stream,
            @Independent @Modified Streams.FunctionWithIndex<? super T, ? extends R> function) { return null; }

        @Independent @NotModified
        static <T> Stream<T> stream(@Independent @Modified Iterable<T> iterable) { return null; }

        @Independent @NotModified
        static <T> Stream<T> stream(@Independent @NotModified Optional<T> optional) { return null; }

        @Independent @NotModified
        static <T> Stream<T> stream(@Independent @NotModified Collection<T> collection) { return null; }

        @Independent @NotModified
        static <T> Stream<T> stream(@Independent @Modified Iterator<T> iterator) { return null; }

        @Independent @NotModified
        static <T> Stream<T> stream(@Independent @NotModified java.util.Optional<T> optional) { return null; }

        @Independent @NotModified
        static DoubleStream stream(@Independent @NotModified OptionalDouble optional) { return null; }

        @Independent @NotModified
        static IntStream stream(@Independent @NotModified OptionalInt optional) { return null; }

        @Independent @NotModified
        static LongStream stream(@Independent @Modified OptionalLong optional) { return null; }

        @Independent @NotModified
        static <A, B, R> Stream<R> zip(
            @Independent @Modified Stream<A> streamA,
            @Independent @Modified Stream<B> streamB,
            @Independent @Modified BiFunction<? super A, ? super B, R> function) { return null; }
    }

    //public interface Table
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a collection contract with mutators, shared by mutable and immutable implementations (GUAVA.md)
    @Container
    @Independent(absent = true)
    class Table$<R, C, V> {
        //interface Cell
        @ImmutableContainer(hc = true)
        class Cell<R, C, V> {
            //override from java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object obj) { return false; }
            @Independent(hc = true) @NotModified C getColumnKey() { return null; }
            @Independent(hc = true) @NotModified R getRowKey() { return null; }
            @Independent(hc = true) @NotModified V getValue() { return null; }
            //override from java.lang.Object
            @NotModified
            public int hashCode() { return 0; }
        }
        @Independent(absent = true) @NotModified Set<Table.Cell<R, C, V>> cellSet() { return null; }
        @Modified void clear() { }
        @Independent(hc = true) @NotModified
        Map<R, V> column(@Independent(hc = true) @NotModified C columnKey) { return null; }
        @Independent(absent = true) @NotModified Set<C> columnKeySet() { return null; }
        @Independent(absent = true) @NotModified Map<C, Map<R, V>> columnMap() { return null; }
        @NotModified
        boolean contains(@Independent @NotModified Object rowKey, @Independent @NotModified Object columnKey) {
            return false;
        }
        @NotModified boolean containsColumn(@Independent @NotModified Object columnKey) { return false; }
        @NotModified boolean containsRow(@Independent @NotModified Object rowKey) { return false; }
        @NotModified boolean containsValue(@Independent @NotModified Object value) { return false; }
        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object obj) { return false; }

        @Independent(hc = true) @NotModified
        V get(@Independent @NotModified Object rowKey, @Independent @NotModified Object columnKey) { return null; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }
        @NotModified boolean isEmpty() { return false; }
        @Independent(hc = true) @Modified
        V put(
            @Independent(hc = true) @NotModified R rowKey,
            @Independent(hc = true) @NotModified C columnKey,
            @Independent(hc = true) @NotModified V value) { return null; }
        @Modified void putAll(@Independent @NotModified Table<? extends R, ? extends C, ? extends V> table) { }
        @Independent @Modified
        V remove(@Independent @NotModified Object rowKey, @Independent @NotModified Object columnKey) { return null; }
        @Independent @NotModified Map<C, V> row(@Independent(hc = true) @NotModified R rowKey) { return null; }
        @Independent(absent = true) @NotModified Set<R> rowKeySet() { return null; }
        @Independent(absent = true) @NotModified Map<R, Map<C, V>> rowMap() { return null; }
        @NotModified int size() { return 0; }
        @Independent(absent = true) @NotModified Collection<V> values() { return null; }
    }

    //public final class Tables
    //annotated as EXPECTED; computed @Immutable (1) -- static methods only (GUAVA.md)
    @UtilityClass
    class Tables$ {
        @Independent @NotModified
        static <R, C, V> Table.Cell<R, C, V> immutableCell(
            @Independent @NotModified R rowKey,
            @Independent @NotModified C columnKey,
            @Independent @NotModified V value) { return null; }

        @Independent @NotModified
        static <R, C, V> Table<R, C, V> newCustomTable(
            @Independent @Modified Map<R, Map<C, V>> backingMap,
            @Independent @Modified com.google.common.base.Supplier<? extends Map<C, V>> factory) { return null; }

        @Independent @NotModified
        static <R, C, V> Table<R, C, V> synchronizedTable(@Independent @NotModified Table<R, C, V> table) { return null; }

        @Independent @NotModified
        static <T, R, C, V, I extends Table<R, C, V>> Collector<T, ?, I> toTable(
            @Independent @Modified java.util.function.Function<? super T, ? extends R> rowFunction,
            @Independent @Modified java.util.function.Function<? super T, ? extends C> columnFunction,
            @Independent @Modified java.util.function.Function<? super T, ? extends V> valueFunction,
            @Independent @Modified BinaryOperator<V> mergeFunction,
            @Independent @NotModified Supplier<I> tableSupplier) { return null; }

        @Independent @NotModified
        static <T, R, C, V, I extends Table<R, C, V>> Collector<T, ?, I> toTable(
            @Independent @Modified java.util.function.Function<? super T, ? extends R> rowFunction,
            @Independent @Modified java.util.function.Function<? super T, ? extends C> columnFunction,
            @Independent @Modified java.util.function.Function<? super T, ? extends V> valueFunction,
            @Independent @NotModified Supplier<I> tableSupplier) { return null; }

        @Independent @NotModified
        static <R, C, V1, V2> Table<R, C, V2> transformValues(
            @Independent @Modified Table<R, C, V1> fromTable,
            @Independent @NotModified Function<? super V1, V2> function) { return null; }

        @Independent @NotModified
        static <R, C, V> Table<C, R, V> transpose(@Independent @Modified Table<R, C, V> table) { return null; }

        @Independent @NotModified
        static <R, C, V> RowSortedTable<R, C, V> unmodifiableRowSortedTable(
            @Independent @NotModified RowSortedTable<R, ? extends C, ? extends V> table) { return null; }

        @Independent @NotModified
        static <R, C, V> Table<R, C, V> unmodifiableTable(
            @Independent @NotModified Table<? extends R, ? extends C, ? extends V> table) { return null; }
    }

    //public class TreeBasedTable extends StandardRowSortedTable<R,C,V>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class TreeBasedTable$<R, C, V> {
        @Independent(hc = true) @NotModified @GetSet("columnComparator")
        Comparator<? super C> columnComparator() { return null; }

        @Independent @NotModified
        static <R extends Comparable, C extends Comparable, V> TreeBasedTable<R, C, V> create() { return null; }

        @Independent @NotModified
        static <R, C, V> TreeBasedTable<R, C, V> create(
            @Independent @NotModified TreeBasedTable<R, C, ? extends V> table) { return null; }

        @Independent @NotModified
        static <R, C, V> TreeBasedTable<R, C, V> create(
            @Independent @NotModified Comparator<? super R> rowComparator,
            @Independent @NotModified Comparator<? super C> columnComparator) { return null; }

        //override from com.google.common.collect.StandardTable, com.google.common.collect.Table
        @Independent @NotModified
        SortedMap<C, V> row(@Independent @NotModified R rowKey) { return null; }
        @Independent @Modified Comparator<? super R> rowComparator() { return null; }
    }

    //public class TreeMultimap extends AbstractSortedKeySortedSetMultimap<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class TreeMultimap$<K, V> {
        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.AbstractSetMultimap, com.google.common.collect.AbstractSortedKeySortedSetMultimap, com.google.common.collect.AbstractSortedSetMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap, com.google.common.collect.SortedSetMultimap
        @Independent(absent = true) @Modified
        NavigableMap<K, Collection<V>> asMap() { return null; }

        @Independent @NotModified
        static <K extends Comparable, V extends Comparable> TreeMultimap<K, V> create() { return null; }

        @Independent @NotModified
        static <K extends Comparable, V extends Comparable> TreeMultimap<K, V> create(
            @Independent @NotModified Multimap<? extends K, ? extends V> multimap) { return null; }

        @Independent @NotModified
        static <K, V> TreeMultimap<K, V> create(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified Comparator<? super V> valueComparator) { return null; }

        //override from com.google.common.collect.AbstractMapBasedMultimap, com.google.common.collect.AbstractSetMultimap, com.google.common.collect.AbstractSortedSetMultimap, com.google.common.collect.Multimap, com.google.common.collect.SetMultimap, com.google.common.collect.SortedSetMultimap
        @Independent @NotModified
        NavigableSet<V> get(@Independent @NotModified K key) { return null; }

        @Independent(hc = true) @NotModified @GetSet("keyComparator")
        Comparator<? super K> keyComparator() { return null; }

        //override from com.google.common.collect.AbstractMultimap, com.google.common.collect.AbstractSortedKeySortedSetMultimap, com.google.common.collect.Multimap
        @Independent(absent = true) @NotModified
        NavigableSet<K> keySet() { return null; }

        //override from com.google.common.collect.SortedSetMultimap
        @Independent(hc = true) @NotModified @GetSet("valueComparator")
        Comparator<? super V> valueComparator() { return null; }
    }

    //public final class TreeMultiset extends AbstractSortedMultiset<E> implements Serializable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class TreeMultiset$<E> {
        //override from com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        @Modified
        int add(@Independent(hc = true) @NotModified E element, int occurrences) { return 0; }

        //override from com.google.common.collect.AbstractMultiset, java.util.AbstractCollection, java.util.Collection
        @Modified
        void clear() { }

        //override from com.google.common.collect.Multiset
        @NotModified
        int count(@Independent @NotModified Object element) { return 0; }
        @Independent @NotModified static <E extends Comparable> TreeMultiset<E> create() { return null; }
        @Independent @NotModified
        static <E extends Comparable> TreeMultiset<E> create(@Independent @NotModified Iterable<? extends E> elements) {
            return null;
        }

        @Independent @NotModified
        static <E> TreeMultiset<E> create(@Independent @NotModified Comparator<? super E> comparator) { return null; }

        //override from com.google.common.collect.Multiset
        @Modified @NotModified(after = "comparator")
        void forEachEntry(@Independent @NotModified ObjIntConsumer<? super E> action) { }

        //override from com.google.common.collect.SortedMultiset
        @Independent(absent = true) @Modified

        SortedMultiset<E> headMultiset(
            @Independent @NotModified E upperBound,
            @Independent @NotModified BoundType boundType) { return null; }

        //override from com.google.common.collect.Multiset, com.google.common.collect.SortedIterable, com.google.common.collect.SortedMultiset, java.lang.Iterable, java.util.AbstractCollection, java.util.Collection
        @Fluent @Independent @NotModified
        Iterator<E> iterator() { return null; }

        //override from com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        @Modified
        int remove(@Independent @NotModified Object element, int occurrences) { return 0; }

        //override from com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        @Modified
        int setCount(@Independent(hc = true) @NotModified E element, int count) { return 0; }

        //override from com.google.common.collect.AbstractMultiset, com.google.common.collect.Multiset
        @Modified
        boolean setCount(@Independent(hc = true) @NotModified E element, int oldCount, int newCount) { return false; }

        //override from com.google.common.collect.Multiset, java.util.AbstractCollection, java.util.Collection
        @NotModified
        int size() { return 0; }

        //override from com.google.common.collect.SortedMultiset
        @Independent(absent = true) @Modified

        SortedMultiset<E> tailMultiset(
            @Independent @NotModified E lowerBound,
            @Independent @NotModified BoundType boundType) { return null; }
    }

    //public final class TreeRangeMap implements RangeMap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class TreeRangeMap$<K extends Comparable, V> {
        //override from com.google.common.collect.RangeMap
        @Independent @NotModified
        Map<Range<K>, V> asDescendingMapOfRanges() { return null; }

        //override from com.google.common.collect.RangeMap
        @Independent(absent = true) @NotModified
        Map<Range<K>, V> asMapOfRanges() { return null; }

        //override from com.google.common.collect.RangeMap
        @Modified
        void clear() { }

        @Independent @NotModified
        static <K extends Comparable<?>, V> TreeRangeMap<K, V> copyOf(
            @Independent @NotModified RangeMap<K, ? extends V> rangeMap) { return null; }
        @Independent @NotModified static <K extends Comparable, V> TreeRangeMap<K, V> create() { return null; }
        //override from com.google.common.collect.RangeMap, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        //override from com.google.common.collect.RangeMap
        @Independent @NotModified
        V get(@Independent @NotModified K key) { return null; }

        //override from com.google.common.collect.RangeMap
        @Independent @NotModified
        Map.Entry<Range<K>, V> getEntry(@Independent @NotModified K key) { return null; }

        //override from com.google.common.collect.RangeMap, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from com.google.common.collect.RangeMap
        @Modified

        void merge(
            @Independent(hc = true) @NotModified Range<K> range,
            @Independent @NotModified V value,
            @Independent @NotModified BiFunction<? super V, ? super V, ? extends V> remappingFunction) { }

        //override from com.google.common.collect.RangeMap
        @Modified
        void put(@Independent(hc = true) @NotModified Range<K> range, @Independent @NotModified V value) { }

        //override from com.google.common.collect.RangeMap
        @Modified
        void putAll(@Independent @NotModified RangeMap<K, ? extends V> rangeMap) { }

        //override from com.google.common.collect.RangeMap
        @Modified
        void putCoalescing(@Independent(hc = true) @NotModified Range<K> range, @Independent @NotModified V value) { }

        //override from com.google.common.collect.RangeMap
        @Modified
        void remove(@Independent(hc = true) @NotModified Range<K> rangeToRemove) { }

        //override from com.google.common.collect.RangeMap
        @Independent @NotModified
        Range<K> span() { return null; }

        //override from com.google.common.collect.RangeMap
        @Fluent @Independent @NotModified
        RangeMap<K, V> subRangeMap(@Independent @NotModified Range<K> subRange) { return null; }

        //override from com.google.common.collect.RangeMap, java.lang.Object
        @NotModified
        public String toString() { return null; }
    }

    //public class TreeRangeSet extends AbstractRangeSet<C> implements Serializable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (GUAVA.md)
    @Container
    @Independent(absent = true)
    class TreeRangeSet$<C extends Comparable<?>> {
        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @Modified
        void add(@Independent(hc = true) @NotModified Range<C> rangeToAdd) { }

        //override from com.google.common.collect.RangeSet
        @Independent(absent = true) @NotModified
        Set<Range<C>> asDescendingSetOfRanges() { return null; }

        //override from com.google.common.collect.RangeSet
        @Independent(absent = true) @NotModified
        Set<Range<C>> asRanges() { return null; }

        //override from com.google.common.collect.RangeSet
        @Independent(absent = true) @Modified
        RangeSet<C> complement() { return null; }
        @Independent @NotModified static <C extends Comparable<?>> TreeRangeSet<C> create() { return null; }
        @Independent @NotModified
        static <C extends Comparable<?>> TreeRangeSet<C> create(@Independent @NotModified Iterable<Range<C>> ranges) {
            return null;
        }

        @Independent @NotModified
        static <C extends Comparable<?>> TreeRangeSet<C> create(@Independent @NotModified RangeSet<C> rangeSet) {
            return null;
        }

        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @NotModified
        boolean encloses(@Independent @NotModified Range<C> range) { return false; }

        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @NotModified
        boolean intersects(@Independent @NotModified Range<C> range) { return false; }

        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @Independent @NotModified
        Range<C> rangeContaining(@Independent @NotModified C value) { return null; }

        //override from com.google.common.collect.AbstractRangeSet, com.google.common.collect.RangeSet
        @Modified
        void remove(@Independent(hc = true) @NotModified Range<C> rangeToRemove) { }

        //override from com.google.common.collect.RangeSet
        @Independent @NotModified
        Range<C> span() { return null; }

        //override from com.google.common.collect.RangeSet
        @Fluent @Independent @NotModified
        RangeSet<C> subRangeSet(@Independent @NotModified Range<C> view) { return null; }
    }

    //public abstract class TreeTraverser
    @Immutable(hc = true)
    @Independent
    class TreeTraverser$<T> {
        TreeTraverser$() { }
        @Independent @NotModified
        FluentIterable<T> breadthFirstTraversal(@Independent @NotModified T root) { return null; }
        @Independent @NotModified Iterable<T> children(@Independent @Modified T root) { return null; }
        @Independent @NotModified FluentIterable<T> postOrderTraversal(@Independent @Modified T root) { return null; }
        @Independent @NotModified FluentIterable<T> preOrderTraversal(@Independent @Modified T root) { return null; }
        @Independent @NotModified
        static <T> TreeTraverser<T> using(
            @Independent @Modified Function<T, ? extends Iterable<T>> nodeToChildrenFunction) { return null; }
    }

    //public abstract class UnmodifiableIterator implements Iterator<E>
    @FinalFields
    @Container
    @Independent(hc = true)
    class UnmodifiableIterator$<E> {
        //override from java.util.Iterator
        @NotModified
        void remove() { }
    }

    //public abstract class UnmodifiableListIterator extends UnmodifiableIterator<E> implements ListIterator<E>
    @FinalFields
    @Container
    @Independent(hc = true)
    class UnmodifiableListIterator$<E> {
        //override from java.util.ListIterator
        @NotModified
        void add(@Independent @NotModified E e) { }

        //override from java.util.ListIterator
        @NotModified
        void set(@Independent @NotModified E e) { }
    }
}
