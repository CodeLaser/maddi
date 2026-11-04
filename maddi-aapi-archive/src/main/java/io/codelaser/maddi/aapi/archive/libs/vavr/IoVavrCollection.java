package io.codelaser.maddi.aapi.archive.libs.vavr;
import io.vavr.*;
import io.vavr.collection.Array;
import io.vavr.collection.BitSet;
import io.vavr.collection.CharSeq;
import io.vavr.collection.HashMultimap;
import io.vavr.collection.IndexedSeq;
import io.vavr.collection.Iterator;
import io.vavr.collection.LinearSeq;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.LinkedHashMultimap;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.List;
import io.vavr.collection.Multimap;
import io.vavr.collection.PriorityQueue;
import io.vavr.collection.Queue;
import io.vavr.collection.Seq;
import io.vavr.collection.SortedMap;
import io.vavr.collection.SortedMultimap;
import io.vavr.collection.Traversable;
import io.vavr.collection.Tree;
import io.vavr.collection.TreeMap;
import io.vavr.collection.TreeMultimap;
import io.vavr.collection.TreeSet;
import io.vavr.collection.Vector;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;
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
@Independent(absent = true)
public class IoVavrCollection {
    public static final String PACKAGE_NAME = "io.vavr.collection";
    //public final class Array implements IndexedSeq<T>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class Array$<T> {
        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        Array<T> append(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> appendAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        java.util.List<T> asJava() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Array<T> asJava(@Independent @NotModified Consumer<? super java.util.List<T>> action) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        java.util.List<T> asJavaMutable() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Array<T> asJavaMutable(@Independent @NotModified Consumer<? super java.util.List<T>> action) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <R> Array<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> partialFunction) {
            return null;
        }
        @Independent @NotModified static <T> Collector<T, ArrayList<T>, Array<T>> collector() { return null; }
        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Array<Array<T>> combinations() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Array<Array<T>> combinations(int k) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Iterator<Array<T>> crossProduct(int power) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> distinct() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> distinctBy(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        <U> Array<T> distinctBy(@Independent @NotModified Function<? super T, ? extends U> keyExtractor) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Array<T> distinctByKeepLast(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U> Array<T> distinctByKeepLast(@Independent @NotModified Function<? super T, ? extends U> keyExtractor) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> drop(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> dropRight(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Array<T> dropRightUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Array<T> dropRightWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Array<T> dropUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Array<T> dropWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }
        @Independent @NotModified static <T> Array<T> empty() { return null; }
        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }
        @Independent @NotModified static <T> Array<T> fill(int n, @Independent @NotModified T element) { return null; }
        @Independent @NotModified
        static <T> Array<T> fill(int n, @Independent @NotModified Supplier<? extends T> s) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> filter(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Array<U> flatMap(@Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        T get(int index) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, Array<T>> groupBy(
            @Independent @NotModified Function<? super T, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Array<T>> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        T head() { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int indexOf(@Independent @NotModified T element, int from) { return 0; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> init() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Option<Array<T>> initOption() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Array<T> insert(int index, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> insertAll(int index, @Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> intersperse(@Independent @NotModified T element) { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent @NotModified
        Iterator<T> iterator() { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int lastIndexOf(@Independent @NotModified T element, int end) { return 0; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> leftPadTo(int length, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        int length() { return 0; }

        //override from io.vavr.Value, io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Array<U> map(@Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Array<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Array<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> Array<T> narrow(@Independent @NotModified Array<? extends T> array) { return null; }
        @Independent @NotModified static <T> Array<T> of(@Independent @NotModified T element) { return null; }
        @Independent @NotModified static <T> Array<T> of(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified
        static <T> Array<T> ofAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        @Independent @NotModified
        static Array<Boolean> ofAll(@Independent @NotModified boolean ... elements) { return null; }

        @Independent @NotModified
        static Array<Byte> ofAll(@Independent @NotModified byte ... elements) { return null; }

        @Independent @NotModified
        static Array<Character> ofAll(@Independent @NotModified char ... elements) { return null; }

        @Independent @NotModified
        static Array<Double> ofAll(@Independent @NotModified double ... elements) { return null; }

        @Independent @NotModified
        static Array<Float> ofAll(@Independent @NotModified float ... elements) { return null; }

        @Independent @NotModified
        static Array<Integer> ofAll(@Independent @NotModified int ... elements) { return null; }

        @Independent @NotModified
        static <T> Array<T> ofAll(@Independent @NotModified Stream<? extends T> javaStream) { return null; }

        @Independent @NotModified
        static Array<Long> ofAll(@Independent @NotModified long ... elements) { return null; }

        @Independent @NotModified
        static Array<Short> ofAll(@Independent @NotModified short ... elements) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> orElse(@Independent @NotModified Iterable<? extends T> other) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> orElse(@Independent @NotModified Supplier<? extends Iterable<? extends T>> supplier) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> padTo(int length, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<Array<T>, Array<T>> partition(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified

        Array<T> patch(int from, @Independent(hc = true) @NotModified Iterable<? extends T> that, int replaced) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> peek(@Independent @NotModified Consumer<? super T> action) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Array<Array<T>> permutations() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Array<T> prepend(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> prependAll(@Independent(hc = true) @NotModified Iterable<? extends T> elements) { return null; }
        @Independent @NotModified static Array<Character> range(char from, char toExclusive) { return null; }
        @Independent @NotModified static Array<Integer> range(int from, int toExclusive) { return null; }
        @Independent @NotModified static Array<Long> range(long from, long toExclusive) { return null; }
        @Independent @NotModified
        static Array<Character> rangeBy(char from, char toExclusive, int step) { return null; }

        @Independent @NotModified
        static Array<Double> rangeBy(double from, double toExclusive, double step) { return null; }
        @Independent @NotModified static Array<Integer> rangeBy(int from, int toExclusive, int step) { return null; }
        @Independent @NotModified static Array<Long> rangeBy(long from, long toExclusive, long step) { return null; }
        @Independent @NotModified static Array<Character> rangeClosed(char from, char toInclusive) { return null; }
        @Independent @NotModified static Array<Integer> rangeClosed(int from, int toInclusive) { return null; }
        @Independent @NotModified static Array<Long> rangeClosed(long from, long toInclusive) { return null; }
        @Independent @NotModified
        static Array<Character> rangeClosedBy(char from, char toInclusive, int step) { return null; }

        @Independent @NotModified
        static Array<Double> rangeClosedBy(double from, double toInclusive, double step) { return null; }

        @Independent @NotModified
        static Array<Integer> rangeClosedBy(int from, int toInclusive, int step) { return null; }

        @Independent @NotModified
        static Array<Long> rangeClosedBy(long from, long toInclusive, long step) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> reject(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> remove(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> removeAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> removeAll(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> removeAll(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Array<T> removeAt(int index) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> removeFirst(@Independent @NotModified Predicate<T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> removeLast(@Independent @NotModified Predicate<T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        Array<T> replace(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        Array<T> replaceAll(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> retainAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> reverse() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> rotateLeft(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> rotateRight(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Array<T> scan(
            @Independent @NotModified T zero,
            @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> operation) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Array<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> operation) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Array<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> shuffle() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Array<T> slice(int beginIndex, int endIndex) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Array<T>> slideBy(@Independent @NotModified Function<? super T, ?> classifier) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Array<T>> sliding(int size) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Array<T>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U> Array<T> sortBy(
            @Independent @NotModified Comparator<? super U> comparator,
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U extends Comparable<? super U>> Array<T> sortBy(
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        Array<T> sorted() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        Array<T> sorted(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Tuple2<Array<T>, Array<T>> span(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Tuple2<Array<T>, Array<T>> splitAt(int n) { return null; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Tuple2<Array<T>, Array<T>> splitAt(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified

        Tuple2<Array<T>, Array<T>> splitAtInclusive(@Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> subSequence(int beginIndex) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> subSequence(int beginIndex, int endIndex) { return null; }

        @Independent @NotModified
        static <T> Array<T> tabulate(int n, @Independent @NotModified Function<? super Integer, ? extends T> f) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Array<T> tail() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<Array<T>> tailOption() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> take(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> takeRight(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> takeRightUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Array<T> takeRightWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> takeUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Array<T> takeWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent @NotModified
        <U> U transform(@Independent @NotModified Function<? super Array<T>, ? extends U> f) { return null; }

        @Independent @NotModified
        static <T> Array<T> unfold(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends T>>> f) { return null; }

        @Independent @NotModified
        static <T, U> Array<U> unfoldLeft(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends U>>> f) { return null; }

        @Independent @NotModified
        static <T, U> Array<U> unfoldRight(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends U, ? extends T>>> f) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<Array<T1>, Array<T2>> unzip(
            @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2, T3> Tuple3<Array<T1>, Array<T2>, Array<T3>> unzip3(
            @Independent @NotModified Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        Array<T> update(int index, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        Array<T> update(int index, @Independent @NotModified Function<? super T, ? extends T> updater) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Array<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Array<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified T thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> Array<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Array<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Array<U> zipWithIndex(@Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> mapper) {
            return null;
        }
    }

    //public interface BitSet implements SortedSet<T>
    //annotated as EXPECTED; computed @FinalFields @Independent(hc = true) -- G1: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class BitSet$<T> {
        static final long serialVersionUID = 0L;
        //static class Builder
        @FinalFields
        @Independent
        class Builder<T> {
            @Independent @NotModified Collector<T, ArrayList<T>, BitSet<T>> collector() { return null; }
            @Independent @NotModified BitSet<T> empty() { return null; }
            @Independent @NotModified
            BitSet<T> fill(int n, @Independent @Modified Supplier<? extends T> s) { return null; }

            @Independent @Modified @NotModified(after = "fromInt,toInt")
            BitSet<T> of(@Independent @Modified T t) { return null; }
            @Independent @NotModified BitSet<T> of(@Independent @Modified T ... values) { return null; }
            @Independent @NotModified
            BitSet<T> ofAll(@Independent @Modified Iterable<? extends T> values) { return null; }

            @Independent @NotModified
            BitSet<T> ofAll(@Independent @Modified Stream<? extends T> javaStream) { return null; }

            @Independent @NotModified
            BitSet<T> tabulate(int n, @Independent @Modified Function<? super Integer, ? extends T> f) { return null; }
        }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Independent @NotModified
        BitSet<T> add(@Independent @NotModified T arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Independent @NotModified
        BitSet<T> addAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <R> io.vavr.collection.SortedSet<R> collect(
            @Independent @NotModified PartialFunction<? super T, ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static Collector<Integer, ArrayList<Integer>, BitSet<Integer>> collector() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Independent @NotModified
        BitSet<T> diff(@Independent @NotModified io.vavr.collection.Set<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        BitSet<T> distinct() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> distinctBy(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> BitSet<T> distinctBy(@IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> drop(int arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> dropRight(int arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> dropUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> dropWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }
        @Independent @NotModified static BitSet<Integer> empty() { return null; }
        @Independent @NotModified
        static BitSet<Integer> fill(int n, @Independent @NotModified Supplier<Integer> s) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> filter(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.SortedSet
        @Independent @NotModified

        <U> io.vavr.collection.SortedSet<U> flatMap(
            @Independent @NotModified Comparator<? super U> comparator,
            @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.SortedSet<U> flatMap(
            @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) { return null; }

        //override from io.vavr.collection.Foldable, io.vavr.collection.Traversable
        @Identity @Independent @NotModified

        <U> U foldRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> f) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, BitSet<T>> groupBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends C> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<BitSet<T>> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> init() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<BitSet<T>> initOption() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Independent @NotModified
        BitSet<T> intersect(@Independent @NotModified io.vavr.collection.Set<? extends T> arg0) { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T last() { return null; }

        //override from io.vavr.collection.SortedSet
        @Independent @NotModified

        <U> io.vavr.collection.SortedSet<U> map(
            @Independent @NotModified Comparator<? super U> comparator,
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.SortedSet<U> map(@Independent @NotModified Function<? super T, ? extends U> mapper) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> io.vavr.collection.SortedSet<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<Void> mapToVoid() { return null; }
        @Independent @NotModified static BitSet<Integer> of(@Independent @NotModified Integer value) { return null; }
        @Independent @NotModified
        static BitSet<Integer> of(@Independent @NotModified Integer ... values) { return null; }

        @Independent @NotModified
        static BitSet<Integer> ofAll(@Independent @NotModified Iterable<Integer> values) { return null; }

        @Independent @NotModified
        static BitSet<Boolean> ofAll(@Independent @NotModified boolean ... elements) { return null; }

        @Independent @NotModified
        static BitSet<Byte> ofAll(@Independent @NotModified byte ... elements) { return null; }

        @Independent @NotModified
        static BitSet<Character> ofAll(@Independent @NotModified char ... elements) { return null; }

        @Independent @NotModified
        static BitSet<Integer> ofAll(@Independent @NotModified int ... elements) { return null; }

        @Independent @NotModified
        static BitSet<Integer> ofAll(@Independent @NotModified Stream<Integer> javaStream) { return null; }

        @Independent @NotModified
        static BitSet<Long> ofAll(@Independent @NotModified long ... elements) { return null; }

        @Independent @NotModified
        static BitSet<Short> ofAll(@Independent @NotModified short ... elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<BitSet<T>, BitSet<T>> partition(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        BitSet<T> peek(@Independent @NotModified Consumer<? super T> action) { return null; }
        @Independent @NotModified static BitSet<Character> range(char from, char toExclusive) { return null; }
        @Independent @NotModified static BitSet<Integer> range(int from, int toExclusive) { return null; }
        @Independent @NotModified static BitSet<Long> range(long from, long toExclusive) { return null; }
        @Independent @NotModified
        static BitSet<Character> rangeBy(char from, char toExclusive, int step) { return null; }
        @Independent @NotModified static BitSet<Integer> rangeBy(int from, int toExclusive, int step) { return null; }
        @Independent @NotModified static BitSet<Long> rangeBy(long from, long toExclusive, long step) { return null; }
        @Independent @NotModified static BitSet<Character> rangeClosed(char from, char toInclusive) { return null; }
        @Independent @NotModified static BitSet<Integer> rangeClosed(int from, int toInclusive) { return null; }
        @Independent @NotModified static BitSet<Long> rangeClosed(long from, long toInclusive) { return null; }
        @Independent @NotModified
        static BitSet<Character> rangeClosedBy(char from, char toInclusive, int step) { return null; }

        @Independent @NotModified
        static BitSet<Integer> rangeClosedBy(int from, int toInclusive, int step) { return null; }

        @Independent @NotModified
        static BitSet<Long> rangeClosedBy(long from, long toInclusive, long step) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> reject(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Independent @NotModified
        BitSet<T> remove(@Independent @NotModified T arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Independent @NotModified
        BitSet<T> removeAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        BitSet<T> replace(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        BitSet<T> replaceAll(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        BitSet<T> retainAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        BitSet<T> scan(
            @Independent @NotModified T arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> arg1) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<BitSet<T>> slideBy(@IgnoreModifications @Independent @NotModified Function<? super T, ?> arg0) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<BitSet<T>> sliding(int size) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<BitSet<T>> sliding(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        Tuple2<BitSet<T>, BitSet<T>> span(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        @Independent @NotModified
        static BitSet<Integer> tabulate(int n, @Independent @NotModified Function<Integer, Integer> f) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> tail() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<BitSet<T>> tailOption() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> take(int arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> takeRight(int arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> takeUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        BitSet<T> takeWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Independent @NotModified
        SortedSet<T> toJavaSet() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Fluent @Independent @NotModified
        BitSet<T> union(@Independent @NotModified io.vavr.collection.Set<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <T1, T2> Tuple2<TreeSet<T1>, TreeSet<T2>> unzip(
            @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <T1, T2, T3> Tuple3<TreeSet<T1>, TreeSet<T2>, TreeSet<T3>> unzip3(
            @Independent @NotModified Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return null;
        }
        @Independent @NotModified static BitSet.Builder<Byte> withBytes() { return null; }
        @Independent @NotModified static BitSet.Builder<Character> withCharacters() { return null; }
        @Independent @NotModified
        static <T extends Enum<T>> BitSet.Builder<T> withEnum(Class<T> enumClass) { return null; }
        @Independent @NotModified static BitSet.Builder<Long> withLongs() { return null; }
        @Independent @NotModified
        static <T> BitSet.Builder<T> withRelations(
            @Independent @NotModified Function1<Integer, T> fromInt,
            @Independent @NotModified Function1<T, Integer> toInt) { return null; }
        @Independent @NotModified static BitSet.Builder<Short> withShorts() { return null; }
        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> TreeSet<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> TreeSet<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified T thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> TreeSet<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeSet<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> TreeSet<U> zipWithIndex(
            @Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> mapper) { return null; }
    }

    //public final class CharSeq implements CharSequence, IndexedSeq<Character>, Serializable, Comparable<CharSeq>
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: a persistent sequence of chars: no hidden content (VAVR.md)
    @ImmutableContainer
    class CharSeq$ {
        //public interface CharFunction
        @FinalFields
        @Container
        @Independent
        class CharFunction<R> {@Independent(absent = true) @Modified R apply(char arg0) { return null; } }

        //public interface CharUnaryOperator
        @FinalFields
        @Container
        @Independent
        class CharUnaryOperator {@Modified char apply(char arg0) { return '\0'; } }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq append(@Independent @NotModified Character element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq appendAll(@Independent @NotModified Iterable<? extends Character> elements) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        java.util.List<Character> asJava() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq asJava(@Independent @NotModified Consumer<? super java.util.List<Character>> action) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        java.util.List<Character> asJavaMutable() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified

        CharSeq asJavaMutable(@Independent @NotModified Consumer<? super java.util.List<Character>> action) {
            return null;
        }
        @Independent @NotModified CharSeq capitalize() { return null; }
        @Independent @NotModified CharSeq capitalize(@Independent @NotModified Locale locale) { return null; }
        //override from java.lang.CharSequence
        @NotModified
        char charAt(int index) { return '\0'; }
        @NotModified int codePointAt(int index) { return 0; }
        @NotModified int codePointBefore(int index) { return 0; }
        @NotModified int codePointCount(int beginIndex, int endIndex) { return 0; }
        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <R> IndexedSeq<R> collect(
            @Independent @NotModified PartialFunction<? super Character, ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static Collector<Character, ArrayList<Character>, CharSeq> collector() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<CharSeq> combinations() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<CharSeq> combinations(int k) { return null; }

        //override from java.lang.Comparable
        @NotModified
        int compareTo(@Independent @NotModified CharSeq anotherString) { return 0; }
        @NotModified int compareToIgnoreCase(@Independent @NotModified CharSeq str) { return 0; }
        @Independent @NotModified CharSeq concat(@Independent @NotModified CharSeq str) { return null; }
        @NotModified boolean contains(@Independent @NotModified CharSequence s) { return false; }
        @NotModified boolean contentEquals(@Independent @NotModified CharSequence cs) { return false; }
        @NotModified boolean contentEquals(@Independent @NotModified StringBuffer sb) { return false; }
        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Iterator<CharSeq> crossProduct(int power) { return null; }
        @Independent @NotModified Byte decodeByte() { return null; }
        @Independent @NotModified Integer decodeInteger() { return null; }
        @Independent @NotModified Long decodeLong() { return null; }
        @Independent @NotModified Short decodeShort() { return null; }
        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq distinct() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq distinctBy(@Independent @NotModified Comparator<? super Character> comparator) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        <U> CharSeq distinctBy(@Independent @NotModified Function<? super Character, ? extends U> keyExtractor) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq distinctByKeepLast(@Independent @NotModified Comparator<? super Character> comparator) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U> CharSeq distinctByKeepLast(@Independent @NotModified Function<? super Character, ? extends U> keyExtractor) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq drop(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq dropRight(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq dropRightUntil(@Independent @NotModified Predicate<? super Character> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq dropRightWhile(@Independent @NotModified Predicate<? super Character> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        CharSeq dropUntil(@Independent @NotModified Predicate<? super Character> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        CharSeq dropWhile(@Independent @NotModified Predicate<? super Character> predicate) { return null; }
        @Independent @NotModified static CharSeq empty() { return null; }
        @NotModified boolean endsWith(@Independent @NotModified CharSeq suffix) { return false; }
        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }
        @NotModified boolean equalsIgnoreCase(@Independent @NotModified CharSeq anotherString) { return false; }
        @Independent @NotModified
        static CharSeq fill(int n, @Independent @NotModified Supplier<? extends Character> s) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq filter(@Independent @NotModified Predicate<? super Character> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> IndexedSeq<U> flatMap(
            @Independent @NotModified Function<? super Character, ? extends Iterable<? extends U>> mapper) {
            return null;
        }

        @Fluent @Independent @NotModified
        CharSeq flatMapChars(@Independent @NotModified CharSeq.CharFunction<? extends CharSequence> mapper) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        Character get(int index) { return null; }
        @Independent @NotModified byte [] getBytes() { return null; }
        @Independent @NotModified byte [] getBytes(String charsetName) { return null; }
        @Independent @NotModified byte [] getBytes(@Independent @NotModified Charset charset) { return null; }
        //override from java.lang.CharSequence
        @NotModified
        void getChars(int srcBegin, int srcEnd, @Independent @NotModified char [] dst, int dstBegin) { }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, CharSeq> groupBy(
            @Independent @NotModified Function<? super Character, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<CharSeq> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Character head() { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int indexOf(@Independent @NotModified Character element, int from) { return 0; }
        @NotModified int indexOf(int ch) { return 0; }
        @NotModified int indexOf(int ch, int fromIndex) { return 0; }
        @NotModified int indexOf(@Independent @NotModified CharSeq str) { return 0; }
        @NotModified int indexOf(@Independent @NotModified CharSeq str, int fromIndex) { return 0; }
        @Independent @NotModified Option<Integer> indexOfOption(@Independent @NotModified CharSeq str) { return null; }
        @Independent @NotModified
        Option<Integer> indexOfOption(@Independent @NotModified CharSeq str, int fromIndex) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        CharSeq init() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<CharSeq> initOption() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq insert(int index, @Independent @NotModified Character element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq insertAll(int index, @Independent @NotModified Iterable<? extends Character> elements) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq intersperse(@Independent @NotModified Character element) { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.CharSequence
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent @NotModified
        Iterator<Character> iterator() { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int lastIndexOf(@Independent @NotModified Character element, int end) { return 0; }
        @NotModified int lastIndexOf(int ch) { return 0; }
        @NotModified int lastIndexOf(int ch, int fromIndex) { return 0; }
        @NotModified int lastIndexOf(@Independent @NotModified CharSeq str) { return 0; }
        @NotModified int lastIndexOf(@Independent @NotModified CharSeq str, int fromIndex) { return 0; }
        @Independent @NotModified Option<Integer> lastIndexOfOption(int ch, int fromIndex) { return null; }
        @Independent @NotModified
        Option<Integer> lastIndexOfOption(@Independent @NotModified CharSeq str) { return null; }

        @Independent @NotModified
        Option<Integer> lastIndexOfOption(@Independent @NotModified CharSeq str, int fromIndex) { return null; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq leftPadTo(int length, @Independent @NotModified Character element) { return null; }

        //override from io.vavr.collection.Traversable, java.lang.CharSequence
        @NotModified
        int length() { return 0; }

        //override from io.vavr.Value, io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> IndexedSeq<U> map(@Independent @NotModified Function<? super Character, ? extends U> mapper) { return null; }

        @Fluent @Independent @NotModified
        CharSeq mapChars(@Independent @NotModified CharSeq.CharUnaryOperator mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> IndexedSeq<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<Void> mapToVoid() { return null; }
        @NotModified boolean matches(String regex) { return false; }
        //override from io.vavr.collection.Traversable
        @NotModified @GetSet("back")
        String mkString() { return null; }
        @Independent @NotModified static CharSeq of(@Independent @NotModified CharSequence sequence) { return null; }
        @Independent @NotModified static CharSeq of(char character) { return null; }
        @Independent @NotModified static CharSeq of(@Independent @NotModified char ... characters) { return null; }
        @Independent @NotModified
        static CharSeq ofAll(@Independent @NotModified Iterable<? extends Character> elements) { return null; }
        @NotModified int offsetByCodePoints(int index, int codePointOffset) { return 0; }
        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq orElse(@Independent @NotModified Iterable<? extends Character> other) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        CharSeq orElse(@Independent @NotModified Supplier<? extends Iterable<? extends Character>> supplier) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq padTo(int length, @Independent @NotModified Character element) { return null; }
        @NotModified boolean parseBoolean() { return false; }
        @NotModified byte parseByte() { return 0; }
        @NotModified byte parseByte(int radix) { return 0; }
        @NotModified double parseDouble() { return 0.0; }
        @NotModified float parseFloat() { return 0.0F; }
        @NotModified int parseInt() { return 0; }
        @NotModified int parseInt(int radix) { return 0; }
        @NotModified long parseLong() { return 0L; }
        @NotModified long parseLong(int radix) { return 0L; }
        @NotModified short parseShort() { return 0; }
        @NotModified short parseShort(int radix) { return 0; }
        @NotModified int parseUnsignedInt() { return 0; }
        @NotModified int parseUnsignedInt(int radix) { return 0; }
        @NotModified long parseUnsignedLong() { return 0L; }
        @NotModified long parseUnsignedLong(int radix) { return 0L; }
        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<CharSeq, CharSeq> partition(@Independent @NotModified Predicate<? super Character> predicate) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq patch(int from, @Independent @NotModified Iterable<? extends Character> that, int replaced) { return null; }

        //override from io.vavr.Value, io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq peek(@Independent @NotModified Consumer<? super Character> action) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<CharSeq> permutations() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq prepend(@Independent @NotModified Character element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq prependAll(@Independent @NotModified Iterable<? extends Character> elements) { return null; }
        @Independent @NotModified static CharSeq range(char from, char toExclusive) { return null; }
        @Independent @NotModified static CharSeq rangeBy(char from, char toExclusive, int step) { return null; }
        @Independent @NotModified static CharSeq rangeClosed(char from, char toInclusive) { return null; }
        @Independent @NotModified static CharSeq rangeClosedBy(char from, char toInclusive, int step) { return null; }
        @NotModified
        boolean regionMatches(
            boolean ignoreCase,
            int toffset,
            @Independent @NotModified CharSeq other,
            int ooffset,
            int len) { return false; }

        @NotModified
        boolean regionMatches(int toffset, @Independent @NotModified CharSeq other, int ooffset, int len) { return false; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq reject(@Independent @NotModified Predicate<? super Character> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq remove(@Independent @NotModified Character element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq removeAll(@Independent @NotModified Character element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq removeAll(@Independent @NotModified Iterable<? extends Character> elements) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq removeAll(@Independent @NotModified Predicate<? super Character> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq removeAt(int index) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq removeFirst(@Independent @NotModified Predicate<Character> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq removeLast(@Independent @NotModified Predicate<Character> predicate) { return null; }
        @Independent @NotModified static CharSeq repeat(char character, int times) { return null; }
        @Fluent @Independent @NotModified CharSeq repeat(int times) { return null; }
        @Independent @NotModified
        CharSeq replace(
            @Independent @NotModified CharSequence target,
            @Independent @NotModified CharSequence replacement) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        CharSeq replace(
            @Independent @NotModified Character currentElement,
            @Independent @NotModified Character newElement) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        CharSeq replaceAll(
            @Independent @NotModified Character currentElement,
            @Independent @NotModified Character newElement) { return null; }
        @Independent @NotModified CharSeq replaceAll(String regex, String replacement) { return null; }
        @Independent @NotModified CharSeq replaceFirst(String regex, String replacement) { return null; }
        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq retainAll(@Independent @NotModified Iterable<? extends Character> elements) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq reverse() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq rotateLeft(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq rotateRight(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        CharSeq scan(
            @Independent @NotModified Character zero,
            @Independent @NotModified BiFunction<? super Character, ? super Character, ? extends Character> operation) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> IndexedSeq<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super Character, ? extends U> operation) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> IndexedSeq<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super Character, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq shuffle() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq slice(int beginIndex, int endIndex) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<CharSeq> slideBy(@Independent @NotModified Function<? super Character, ?> classifier) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<CharSeq> sliding(int size) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<CharSeq> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U> CharSeq sortBy(
            @Independent @NotModified Comparator<? super U> comparator,
            @Independent @NotModified Function<? super Character, ? extends U> mapper) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U extends Comparable<? super U>> CharSeq sortBy(
            @Independent @NotModified Function<? super Character, ? extends U> mapper) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq sorted() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq sorted(@Independent @NotModified Comparator<? super Character> comparator) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Tuple2<CharSeq, CharSeq> span(@Independent @NotModified Predicate<? super Character> predicate) { return null; }
        @Independent @NotModified Seq<CharSeq> split(String regex) { return null; }
        @Independent @NotModified Seq<CharSeq> split(String regex, int limit) { return null; }
        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Tuple2<CharSeq, CharSeq> splitAt(int n) { return null; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified

        Tuple2<CharSeq, CharSeq> splitAt(@Independent @NotModified Predicate<? super Character> predicate) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified

        Tuple2<CharSeq, CharSeq> splitAtInclusive(@Independent @NotModified Predicate<? super Character> predicate) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @NotModified
        boolean startsWith(@Independent @NotModified Iterable<? extends Character> that, int offset) { return false; }
        @NotModified boolean startsWith(@Independent @NotModified CharSeq prefix) { return false; }
        @NotModified boolean startsWith(@Independent @NotModified CharSeq prefix, int toffset) { return false; }
        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq subSequence(int beginIndex) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, java.lang.CharSequence
        @Fluent @Independent @NotModified
        CharSeq subSequence(int beginIndex, int endIndex) { return null; }
        @Independent @NotModified CharSeq substring(int beginIndex) { return null; }
        @Independent @NotModified CharSeq substring(int beginIndex, int endIndex) { return null; }
        @Independent @NotModified
        static CharSeq tabulate(int n, @Independent @NotModified Function<? super Integer, ? extends Character> f) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        CharSeq tail() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<CharSeq> tailOption() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq take(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq takeRight(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq takeRightUntil(@Independent @NotModified Predicate<? super Character> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        CharSeq takeRightWhile(@Independent @NotModified Predicate<? super Character> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq takeUntil(@Independent @NotModified Predicate<? super Character> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        CharSeq takeWhile(@Independent @NotModified Predicate<? super Character> predicate) { return null; }
        @Independent @NotModified Boolean toBoolean() { return null; }
        @Independent @NotModified Byte toByte() { return null; }
        @Independent @NotModified Byte toByte(int radix) { return null; }
        @Independent @NotModified char [] toCharArray() { return null; }
        @Independent @NotModified Double toDouble() { return null; }
        @Independent @NotModified Float toFloat() { return null; }
        @Independent @NotModified Integer toInteger() { return null; }
        @Independent @NotModified Integer toInteger(int radix) { return null; }
        //override from io.vavr.Value
        @Independent @NotModified
        Character [] toJavaArray() { return null; }
        @Independent @NotModified Long toLong() { return null; }
        @Independent @NotModified Long toLong(int radix) { return null; }
        @Independent @NotModified CharSeq toLowerCase() { return null; }
        @Independent @NotModified CharSeq toLowerCase(@Independent @NotModified Locale locale) { return null; }
        @Independent @NotModified Short toShort() { return null; }
        @Independent @NotModified Short toShort(int radix) { return null; }
        //override from io.vavr.Value, java.lang.CharSequence, java.lang.Object
        @NotModified @GetSet("back")
        public String toString() { return null; }
        @Independent @NotModified CharSeq toUpperCase() { return null; }
        @Independent @NotModified CharSeq toUpperCase(@Independent @NotModified Locale locale) { return null; }
        @Independent @NotModified
        <U> U transform(@Independent @NotModified Function<? super CharSeq, ? extends U> f) { return null; }
        @Independent @NotModified CharSeq trim() { return null; }
        @Independent @NotModified
        static CharSeq unfold(
            @Independent @NotModified Character seed,
            @Independent @NotModified Function<
                ? super Character,
                Option<Tuple2<? extends Character, ? extends Character>>> f) { return null; }

        @Independent @NotModified
        static <T> CharSeq unfoldLeft(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends Character>>> f) {
            return null;
        }

        @Independent @NotModified
        static <T> CharSeq unfoldRight(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends Character, ? extends T>>> f) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<IndexedSeq<T1>, IndexedSeq<T2>> unzip(
            @Independent @NotModified Function<? super Character, Tuple2<? extends T1, ? extends T2>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2, T3> Tuple3<IndexedSeq<T1>, IndexedSeq<T2>, IndexedSeq<T3>> unzip3(
            @Independent @NotModified Function<? super Character, Tuple3<? extends T1, ? extends T2, ? extends T3>>
                unzipper) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        CharSeq update(int index, @Independent @NotModified Character element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified

        CharSeq update(int index, @Independent @NotModified Function<? super Character, ? extends Character> updater) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> IndexedSeq<Tuple2<Character, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> IndexedSeq<Tuple2<Character, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified Character thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> IndexedSeq<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super Character, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<Tuple2<Character, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> IndexedSeq<U> zipWithIndex(
            @Independent @NotModified BiFunction<? super Character, ? super Integer, ? extends U> mapper) { return null; }
    }

    //public interface Foldable
    //EXPECTED no immutability claim -- computed @FinalFields @Independent(hc = true), annotated -- root of G1 (computed is right): also implemented by Iterator, Future, Lazy: stateful (VAVR.md)
    @FinalFields
    @Independent(hc = true)
    class Foldable$<T> {
        @Independent @NotModified
        T fold(
            @Independent @Modified T zero,
            @Independent @Modified BiFunction<? super T, ? super T, ? extends T> combine) { return null; }

        @Independent @NotModified
        <U> U foldLeft(
            @Independent @Modified U arg0,
            @IgnoreModifications @Independent @Modified BiFunction<? super U, ? super T, ? extends U> arg1) {
            return null;
        }

        @Independent(hc = true) @Modified
        <U> U foldRight(
            @Independent(hc = true) @Modified U arg0,
            @IgnoreModifications @Independent @Modified BiFunction<? super T, ? super U, ? extends U> arg1) {
            return null;
        }

        @Independent @Modified
        T reduce(@Independent @Modified BiFunction<? super T, ? super T, ? extends T> op) { return null; }

        @Independent @Modified
        T reduceLeft(@IgnoreModifications @Independent @Modified BiFunction<? super T, ? super T, ? extends T> arg0) {
            return null;
        }

        @Independent @NotModified
        Option<T> reduceLeftOption(
            @IgnoreModifications @Independent @Modified BiFunction<? super T, ? super T, ? extends T> arg0) {
            return null;
        }

        @Independent @NotModified
        Option<T> reduceOption(@Independent @Modified BiFunction<? super T, ? super T, ? extends T> op) { return null; }

        @Independent @Modified
        T reduceRight(@IgnoreModifications @Independent @Modified BiFunction<? super T, ? super T, ? extends T> arg0) {
            return null;
        }

        @Independent @NotModified
        Option<T> reduceRightOption(
            @IgnoreModifications @Independent @Modified BiFunction<? super T, ? super T, ? extends T> arg0) {
            return null;
        }
    }

    //public final class HashMap implements Map<K,V>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class HashMap$<K, V> {
        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2, V2> io.vavr.collection.HashMap<K2, V2> bimap(
            @Independent @NotModified Function<? super K, ? extends K2> keyMapper,
            @Independent @NotModified Function<? super V, ? extends V2> valueMapper) { return null; }

        @Independent @NotModified
        static <K, V> Collector<Tuple2<K, V>, ArrayList<Tuple2<K, V>>, io.vavr.collection.HashMap<K, V>> collector() {
            return null;
        }

        @Independent @NotModified
        static <K, V, T extends V> Collector<T, ArrayList<T>, io.vavr.collection.HashMap<K, V>> collector(
            @Independent @NotModified Function<? super T, ? extends K> keyMapper) { return null; }

        @Independent @NotModified
        static <K, V, T> Collector<T, ArrayList<T>, io.vavr.collection.HashMap<K, V>> collector(
            @Independent @NotModified Function<? super T, ? extends K> keyMapper,
            @Independent @NotModified Function<? super T, ? extends V> valueMapper) { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        Tuple2<V, io.vavr.collection.HashMap<K, V>> computeIfAbsent(
            @Independent @NotModified K key,
            @Independent @NotModified Function<? super K, ? extends V> mappingFunction) { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        Tuple2<Option<V>, io.vavr.collection.HashMap<K, V>> computeIfPresent(
            @Independent @NotModified K key,
            @Independent @NotModified BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from io.vavr.collection.Map
        @NotModified
        boolean containsKey(@Independent(hc = true) @NotModified K key) { return false; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashMap<K, V> distinct() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> distinctBy(
            @Independent @NotModified Comparator<? super Tuple2<K, V>> comparator) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.HashMap<K, V> distinctBy(
            @Independent @NotModified Function<? super Tuple2<K, V>, ? extends U> keyExtractor) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashMap<K, V> drop(int n) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashMap<K, V> dropRight(int n) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> dropUntil(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> dropWhile(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) {
            return null;
        }
        @Independent @NotModified static <K, V> io.vavr.collection.HashMap<K, V> empty() { return null; }
        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.HashMap<K, V> fill(
            int n,
            @Independent @NotModified Supplier<? extends Tuple2<? extends K, ? extends V>> s) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> filter(@Independent @NotModified BiPredicate<? super K, ? super V> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> filter(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> filterKeys(@Independent @NotModified Predicate<? super K> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> filterValues(@Independent @NotModified Predicate<? super V> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        <K2, V2> io.vavr.collection.HashMap<K2, V2> flatMap(
            @Independent @NotModified BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent(hc = true) @NotModified
        Option<V> get(@Independent(hc = true) @NotModified K key) { return null; }

        //override from io.vavr.collection.Map
        @Independent(hc = true) @NotModified

        V getOrElse(@Independent(hc = true) @NotModified K key, @Independent(hc = true) @NotModified V defaultValue) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, io.vavr.collection.HashMap<K, V>> groupBy(
            @Independent @NotModified Function<? super Tuple2<K, V>, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<io.vavr.collection.HashMap<K, V>> grouped(int size) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<K, V> head() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashMap<K, V> init() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<io.vavr.collection.HashMap<K, V>> initOption() { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Map, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent(hc = true) @NotModified
        Iterator<Tuple2<K, V>> iterator() { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        io.vavr.collection.Set<K> keySet() { return null; }

        //override from io.vavr.collection.Map
        @Independent(hc = true) @NotModified
        Iterator<K> keysIterator() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<K, V> last() { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2, V2> io.vavr.collection.HashMap<K2, V2> map(
            @Independent @NotModified BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2> io.vavr.collection.HashMap<K2, V> mapKeys(
            @Independent @NotModified Function<? super K, ? extends K2> keyMapper) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2> io.vavr.collection.HashMap<K2, V> mapKeys(
            @Independent @NotModified Function<? super K, ? extends K2> keyMapper,
            @Independent @NotModified BiFunction<? super V, ? super V, ? extends V> valueMerge) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <V2> io.vavr.collection.HashMap<K, V2> mapValues(
            @Independent @NotModified Function<? super V, ? extends V2> valueMapper) { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        io.vavr.collection.HashMap<K, V> merge(
            @Independent @NotModified io.vavr.collection.Map<? extends K, ? extends V> that) { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        <U extends V> io.vavr.collection.HashMap<K, V> merge(
            @Independent @NotModified io.vavr.collection.Map<? extends K, U> that,
            @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> collisionResolution) { return null; }

        @Identity @Independent @NotModified
        static <K, V> io.vavr.collection.HashMap<K, V> narrow(
            @Independent @NotModified io.vavr.collection.HashMap<? extends K, ? extends V> hashMap) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.HashMap<K, V> of(
            @Independent @NotModified K key,
            @Independent @NotModified V value) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.HashMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.HashMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.HashMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.HashMap<K, V> of(
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
        static <K, V> io.vavr.collection.HashMap<K, V> of(
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
        static <K, V> io.vavr.collection.HashMap<K, V> of(
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
        static <K, V> io.vavr.collection.HashMap<K, V> of(
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
        static <K, V> io.vavr.collection.HashMap<K, V> of(
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
        static <K, V> io.vavr.collection.HashMap<K, V> of(
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
        static <K, V> io.vavr.collection.HashMap<K, V> of(
            @Independent @NotModified Tuple2<? extends K, ? extends V> entry) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.HashMap<K, V> ofAll(
            @Independent @NotModified Map<? extends K, ? extends V> map) { return null; }

        @Independent @NotModified
        static <T, K, V> io.vavr.collection.HashMap<K, V> ofAll(
            @Independent @NotModified Stream<? extends T> stream,
            @Independent @NotModified Function<? super T, Tuple2<? extends K, ? extends V>> entryMapper) { return null; }

        @Independent @NotModified
        static <T, K, V> io.vavr.collection.HashMap<K, V> ofAll(
            @Independent @NotModified Stream<? extends T> stream,
            @Independent @NotModified Function<? super T, ? extends K> keyMapper,
            @Independent @NotModified Function<? super T, ? extends V> valueMapper) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.HashMap<K, V> ofEntries(
            @Independent @NotModified Iterable<? extends Tuple2<? extends K, ? extends V>> entries) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.HashMap<K, V> ofEntries(
            @Independent @NotModified Tuple2<? extends K, ? extends V> ... entries) { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.HashMap<K, V> ofEntries(
            @Independent @NotModified Map.Entry<? extends K, ? extends V> ... entries) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.HashMap<K, V> orElse(@Independent @NotModified Iterable<? extends Tuple2<K, V>> other) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.HashMap<K, V> orElse(
            @Independent @NotModified Supplier<? extends Iterable<? extends Tuple2<K, V>>> supplier) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<io.vavr.collection.HashMap<K, V>, io.vavr.collection.HashMap<K, V>> partition(
            @Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.HashMap<K, V> peek(@Independent @NotModified Consumer<? super Tuple2<K, V>> action) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> put(
            @Independent(hc = true) @NotModified K key,
            @Independent(hc = true) @NotModified V value) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <U extends V> io.vavr.collection.HashMap<K, V> put(
            @Independent @NotModified K key,
            @Independent @NotModified U value,
            @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> merge) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> put(@Independent @NotModified Tuple2<? extends K, ? extends V> entry) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <U extends V> io.vavr.collection.HashMap<K, V> put(
            @Independent @NotModified Tuple2<? extends K, U> entry,
            @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> merge) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> reject(@Independent @NotModified BiPredicate<? super K, ? super V> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> reject(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> rejectKeys(@Independent @NotModified Predicate<? super K> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> rejectValues(@Independent @NotModified Predicate<? super V> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified
        io.vavr.collection.HashMap<K, V> remove(@Independent(hc = true) @NotModified K key) { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        io.vavr.collection.HashMap<K, V> removeAll(@Independent(hc = true) @NotModified Iterable<? extends K> keys) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> removeAll(
            @Independent @NotModified BiPredicate<? super K, ? super V> predicate) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> removeKeys(@Independent @NotModified Predicate<? super K> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> removeValues(@Independent @NotModified Predicate<? super V> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        io.vavr.collection.HashMap<K, V> replace(
            @Independent @NotModified K key,
            @Independent @NotModified V oldValue,
            @Independent @NotModified V newValue) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.HashMap<K, V> replace(
            @Independent @NotModified Tuple2<K, V> currentElement,
            @Independent @NotModified Tuple2<K, V> newElement) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.HashMap<K, V> replaceAll(
            @Independent @NotModified Tuple2<K, V> currentElement,
            @Independent @NotModified Tuple2<K, V> newElement) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> replaceAll(
            @Independent @NotModified BiFunction<? super K, ? super V, ? extends V> function) { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        io.vavr.collection.HashMap<K, V> replaceValue(
            @Independent @NotModified K key,
            @Independent @NotModified V value) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> retainAll(@Independent @NotModified Iterable<? extends Tuple2<K, V>> elements) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.HashMap<K, V> scan(
            @Independent @NotModified Tuple2<K, V> zero,
            @Independent @NotModified BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>>
                operation) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @NotModified
        int size() { return 0; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<io.vavr.collection.HashMap<K, V>> slideBy(
            @Independent @NotModified Function<? super Tuple2<K, V>, ?> classifier) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<io.vavr.collection.HashMap<K, V>> sliding(int size) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<io.vavr.collection.HashMap<K, V>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<io.vavr.collection.HashMap<K, V>, io.vavr.collection.HashMap<K, V>> span(
            @Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        @Independent @NotModified
        static <K, V> io.vavr.collection.HashMap<K, V> tabulate(
            int n,
            @Independent @NotModified Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashMap<K, V> tail() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<io.vavr.collection.HashMap<K, V>> tailOption() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashMap<K, V> take(int n) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashMap<K, V> takeRight(int n) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.HashMap<K, V> takeUntil(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.HashMap<K, V> takeWhile(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified
        HashMap<K, V> toJavaMap() { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public String toString() { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        io.vavr.collection.Stream<V> values() { return null; }

        //override from io.vavr.collection.Map
        @Independent(hc = true) @NotModified
        Iterator<V> valuesIterator() { return null; }
    }

    //public final class HashMultimap extends AbstractMultimap<K,V,HashMultimap<K,V>> implements Serializable
    //annotated as EXPECTED; computed @FinalFields @Container @Independent -- G1: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class HashMultimap$<K, V> {
        //public static class Builder
        @Immutable(hc = true)
        @Independent
        class Builder<V> {
            @Independent @NotModified
            <K, V2 extends V> Collector<Tuple2<K, V2>, ArrayList<Tuple2<K, V2>>, Multimap<K, V2>> collector() {
                return null;
            }
            @Independent @NotModified <K, V2 extends V> HashMultimap<K, V2> empty() { return null; }
            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> fill(
                int n,
                @Independent @NotModified Tuple2<? extends K, ? extends V2> element) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> fill(
                int n,
                @Independent @Modified Supplier<? extends Tuple2<? extends K, ? extends V2>> s) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> of(@Independent @NotModified K key, @Independent @Modified V2 value) {
                return null;
            }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @NotModified K k2,
                @Independent @Modified V2 v2) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @NotModified K k3,
                @Independent @Modified V2 v3) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @NotModified K k4,
                @Independent @Modified V2 v4) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @NotModified K k5,
                @Independent @Modified V2 v5) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @NotModified K k6,
                @Independent @Modified V2 v6) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @NotModified K k7,
                @Independent @Modified V2 v7) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @Modified K k7,
                @Independent @Modified V2 v7,
                @Independent @NotModified K k8,
                @Independent @Modified V2 v8) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @Modified K k7,
                @Independent @Modified V2 v7,
                @Independent @Modified K k8,
                @Independent @Modified V2 v8,
                @Independent @NotModified K k9,
                @Independent @Modified V2 v9) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @Modified K k7,
                @Independent @Modified V2 v7,
                @Independent @Modified K k8,
                @Independent @Modified V2 v8,
                @Independent @Modified K k9,
                @Independent @Modified V2 v9,
                @Independent @NotModified K k10,
                @Independent @Modified V2 v10) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> of(@Independent @Modified Tuple2<? extends K, ? extends V2> entry) {
                return null;
            }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> ofAll(@Independent @NotModified Map<? extends K, ? extends V2> map) {
                return null;
            }

            @Independent @NotModified
            <T, K, V2 extends V> HashMultimap<K, V2> ofAll(
                @Independent @Modified Stream<? extends T> stream,
                @Independent @Modified Function<? super T, Tuple2<? extends K, ? extends V2>> entryMapper) {
                return null;
            }

            @Independent @NotModified
            <T, K, V2 extends V> HashMultimap<K, V2> ofAll(
                @Independent @Modified Stream<? extends T> stream,
                @Independent @Modified Function<? super T, ? extends K> keyMapper,
                @Independent @Modified Function<? super T, ? extends V2> valueMapper) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> ofEntries(
                @Independent @Modified Iterable<? extends Tuple2<? extends K, ? extends V2>> entries) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> ofEntries(
                @Independent @NotModified Tuple2<? extends K, ? extends V2> ... entries) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> ofEntries(
                @Independent @NotModified Map.Entry<? extends K, ? extends V2> ... entries) { return null; }

            @Independent @NotModified
            <K, V2 extends V> HashMultimap<K, V2> tabulate(
                int n,
                @Independent @Modified Function<? super Integer, ? extends Tuple2<? extends K, ? extends V2>> f) {
                return null;
            }
        }

        @Identity @Independent @NotModified
        static <K, V> HashMultimap<K, V> narrow(@Independent @NotModified HashMultimap<? extends K, ? extends V> map) {
            return null;
        }
        @Independent @NotModified static <V> HashMultimap.Builder<V> withSeq() { return null; }
        @Independent @NotModified static <V> HashMultimap.Builder<V> withSet() { return null; }
        @Independent @NotModified
        static <V extends Comparable<?>> HashMultimap.Builder<V> withSortedSet() { return null; }

        @Independent @NotModified
        static <V> HashMultimap.Builder<V> withSortedSet(@Independent @NotModified Comparator<? super V> comparator) {
            return null;
        }
    }

    //public final class HashSet implements Set<T>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class HashSet$<T> {
        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> add(@Independent(hc = true) @NotModified T element) { return null; }

        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified

        io.vavr.collection.HashSet<T> addAll(@Independent(hc = true) @NotModified Iterable<? extends T> elements) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <R> io.vavr.collection.HashSet<R> collect(
            @Independent @NotModified PartialFunction<? super T, ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T> Collector<T, ArrayList<T>, io.vavr.collection.HashSet<T>> collector() { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set
        @NotModified
        boolean contains(@Independent(hc = true) @NotModified T element) { return false; }

        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified

        io.vavr.collection.HashSet<T> diff(@Independent @NotModified io.vavr.collection.Set<? extends T> elements) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> distinct() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.HashSet<T> distinctBy(@Independent @NotModified Comparator<? super T> comparator) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.HashSet<T> distinctBy(
            @Independent @NotModified Function<? super T, ? extends U> keyExtractor) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> drop(int n) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> dropRight(int n) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> dropUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> dropWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }
        @Independent @NotModified static <T> io.vavr.collection.HashSet<T> empty() { return null; }
        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        @Independent @NotModified
        static <T> io.vavr.collection.HashSet<T> fill(int n, @Independent @NotModified Supplier<? extends T> s) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> filter(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.HashSet<U> flatMap(
            @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) { return null; }

        //override from io.vavr.collection.Foldable, io.vavr.collection.Traversable
        @Identity @Independent @NotModified

        <U> U foldRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> f) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, io.vavr.collection.HashSet<T>> groupBy(
            @Independent @NotModified Function<? super T, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<io.vavr.collection.HashSet<T>> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T head() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Option<T> headOption() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> init() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Option<io.vavr.collection.HashSet<T>> initOption() { return null; }

        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified

        io.vavr.collection.HashSet<T> intersect(@Independent @NotModified io.vavr.collection.Set<? extends T> elements) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent(hc = true) @NotModified
        Iterator<T> iterator() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T last() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @NotModified
        int length() { return 0; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.HashSet<U> map(@Independent @NotModified Function<? super T, ? extends U> mapper) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> io.vavr.collection.HashSet<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.HashSet<Void> mapToVoid() { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified

        String mkString(
            @Independent @NotModified CharSequence prefix,
            @Independent @NotModified CharSequence delimiter,
            @Independent @NotModified CharSequence suffix) { return null; }

        @Identity @Independent @NotModified
        static <T> io.vavr.collection.HashSet<T> narrow(
            @Independent @NotModified io.vavr.collection.HashSet<? extends T> hashSet) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.HashSet<T> of(@Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.HashSet<T> of(@Independent @NotModified T ... elements) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.HashSet<T> ofAll(@Independent @NotModified Iterable<? extends T> elements) {
            return null;
        }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Boolean> ofAll(@Independent @NotModified boolean ... elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Byte> ofAll(@Independent @NotModified byte ... elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Character> ofAll(@Independent @NotModified char ... elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Double> ofAll(@Independent @NotModified double ... elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Float> ofAll(@Independent @NotModified float ... elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Integer> ofAll(@Independent @NotModified int ... elements) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.HashSet<T> ofAll(@Independent @NotModified Stream<? extends T> javaStream) {
            return null;
        }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Long> ofAll(@Independent @NotModified long ... elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Short> ofAll(@Independent @NotModified short ... elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> orElse(@Independent @NotModified Iterable<? extends T> other) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.HashSet<T> orElse(
            @Independent @NotModified Supplier<? extends Iterable<? extends T>> supplier) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<io.vavr.collection.HashSet<T>, io.vavr.collection.HashSet<T>> partition(
            @Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> peek(@Independent @NotModified Consumer<? super T> action) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Character> range(char from, char toExclusive) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Integer> range(int from, int toExclusive) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Long> range(long from, long toExclusive) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Character> rangeBy(char from, char toExclusive, int step) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Double> rangeBy(double from, double toExclusive, double step) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Integer> rangeBy(int from, int toExclusive, int step) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Long> rangeBy(long from, long toExclusive, long step) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Character> rangeClosed(char from, char toInclusive) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Integer> rangeClosed(int from, int toInclusive) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Long> rangeClosed(long from, long toInclusive) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Character> rangeClosedBy(char from, char toInclusive, int step) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Double> rangeClosedBy(double from, double toInclusive, double step) {
            return null;
        }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Integer> rangeClosedBy(int from, int toInclusive, int step) { return null; }

        @Independent @NotModified
        static io.vavr.collection.HashSet<Long> rangeClosedBy(long from, long toInclusive, long step) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> reject(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> remove(@Independent(hc = true) @NotModified T element) { return null; }

        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> removeAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.HashSet<T> replace(
            @Independent(hc = true) @NotModified T currentElement,
            @Independent(hc = true) @NotModified T newElement) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.HashSet<T> replaceAll(
            @Independent(hc = true) @NotModified T currentElement,
            @Independent(hc = true) @NotModified T newElement) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> retainAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.HashSet<T> scan(
            @Independent @NotModified T zero,
            @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> operation) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.HashSet<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.HashSet<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<io.vavr.collection.HashSet<T>> slideBy(@Independent @NotModified Function<? super T, ?> classifier) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<io.vavr.collection.HashSet<T>> sliding(int size) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<io.vavr.collection.HashSet<T>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        Tuple2<io.vavr.collection.HashSet<T>, io.vavr.collection.HashSet<T>> span(
            @Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.HashSet<T> tabulate(
            int n,
            @Independent @NotModified Function<? super Integer, ? extends T> f) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> tail() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Option<io.vavr.collection.HashSet<T>> tailOption() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> take(int n) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> takeRight(int n) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> takeUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.HashSet<T> takeWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set
        @Independent @NotModified
        HashSet<T> toJavaSet() { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent @NotModified
        <U> U transform(@Independent @NotModified Function<? super io.vavr.collection.HashSet<T>, ? extends U> f) {
            return null;
        }

        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified

        io.vavr.collection.HashSet<T> union(
            @Independent(hc = true) @NotModified io.vavr.collection.Set<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<io.vavr.collection.HashSet<T1>, io.vavr.collection.HashSet<T2>> unzip(
            @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2, T3> Tuple3<
            io.vavr.collection.HashSet<T1>,
            io.vavr.collection.HashSet<T2>,
            io.vavr.collection.HashSet<T3>> unzip3(
            @Independent @NotModified Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.HashSet<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> that) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.HashSet<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified T thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> io.vavr.collection.HashSet<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.HashSet<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.HashSet<U> zipWithIndex(
            @Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> mapper) { return null; }
    }

    //public interface IndexedSeq implements Seq<T>
    //annotated as EXPECTED; computed @FinalFields @Independent(hc = true) -- G1: implemented by persistent collections only (VAVR.md)
    @ImmutableContainer(hc = true)
    class IndexedSeq$<T> {
        static final long serialVersionUID = 0L;
        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        IndexedSeq<T> append(@Independent @NotModified T arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> appendAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        IndexedSeq<T> asJava(@IgnoreModifications @Independent @NotModified Consumer<? super java.util.List<T>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        IndexedSeq<T> asJavaMutable(
            @IgnoreModifications @Independent @NotModified Consumer<? super java.util.List<T>> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        PartialFunction<Integer, T> asPartialFunction() { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <R> IndexedSeq<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<? extends IndexedSeq<T>> combinations() { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<? extends IndexedSeq<T>> combinations(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        Iterator<? extends IndexedSeq<T>> crossProduct(int arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> distinct() { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> distinctBy(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> IndexedSeq<T> distinctBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> distinctByKeepLast(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        <U> IndexedSeq<T> distinctByKeepLast(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> drop(int arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> dropRight(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        IndexedSeq<T> dropRightUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        IndexedSeq<T> dropRightWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> dropUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> dropWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        boolean endsWith(@Independent @NotModified Seq<? extends T> that) { return false; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> filter(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> IndexedSeq<U> flatMap(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, ? extends IndexedSeq<T>> groupBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends C> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends IndexedSeq<T>> grouped(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int indexOfSlice(@Independent @NotModified Iterable<? extends T> that, int from) { return 0; }

        //override from io.vavr.collection.Seq
        @NotModified
        int indexWhere(@Independent @NotModified Predicate<? super T> predicate, int from) { return 0; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> init() { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends IndexedSeq<T>> initOption() { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> insert(int arg0, @Independent @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> insertAll(int arg0, @Independent @NotModified Iterable<? extends T> arg1) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> intersperse(@Independent @NotModified T arg0) { return null; }

        //override from io.vavr.PartialFunction
        @Identity @NotModified
        boolean isDefinedAt(@Independent @NotModified Integer index) { return false; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T last() { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int lastIndexOfSlice(@Independent @NotModified Iterable<? extends T> that, int end) { return 0; }

        //override from io.vavr.collection.Seq
        @NotModified
        int lastIndexWhere(@Independent @NotModified Predicate<? super T> predicate, int end) { return 0; }

        //override from io.vavr.Value, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> IndexedSeq<U> map(@IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> IndexedSeq<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> IndexedSeq<T> narrow(@Independent @NotModified IndexedSeq<? extends T> indexedSeq) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> orElse(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        IndexedSeq<T> orElse(
            @IgnoreModifications @Independent @NotModified Supplier<? extends Iterable<? extends T>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> padTo(int arg0, @Independent @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends IndexedSeq<T>, ? extends IndexedSeq<T>> partition(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        IndexedSeq<T> patch(int arg0, @Independent(hc = true) @NotModified Iterable<? extends T> arg1, int arg2) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> peek(@IgnoreModifications @Independent @NotModified Consumer<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<? extends IndexedSeq<T>> permutations() { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> prepend(@Independent @NotModified T arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> prependAll(@Independent(hc = true) @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> reject(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> remove(@Independent @NotModified T arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> removeAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> removeAll(@Independent @NotModified T arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> removeAll(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> removeAt(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> removeFirst(@IgnoreModifications @Independent @NotModified Predicate<T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> removeLast(@IgnoreModifications @Independent @NotModified Predicate<T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> replace(@Independent @NotModified T arg0, @Independent @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> replaceAll(@Independent @NotModified T arg0, @Independent @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> retainAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> reverse() { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        Iterator<T> reverseIterator() { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> rotateLeft(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> rotateRight(int arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        IndexedSeq<T> scan(
            @Independent @NotModified T arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> arg1) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> IndexedSeq<U> scanLeft(
            @Independent @NotModified U arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> arg1) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> IndexedSeq<U> scanRight(
            @Independent @NotModified U arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> arg1) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @NotModified
        int search(@Independent @NotModified T element) { return 0; }

        //override from io.vavr.collection.Seq
        @NotModified

        int search(@Independent @NotModified T element, @Independent @NotModified Comparator<? super T> comparator) {
            return 0;
        }

        //override from io.vavr.collection.Seq
        @NotModified
        int segmentLength(@Independent @NotModified Predicate<? super T> predicate, int from) { return 0; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> shuffle() { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> slice(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<? extends IndexedSeq<T>> slideBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ?> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends IndexedSeq<T>> sliding(int arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends IndexedSeq<T>> sliding(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        <U> IndexedSeq<T> sortBy(
            @Independent @NotModified Comparator<? super U> arg0,
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg1) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        <U extends Comparable<? super U>> IndexedSeq<T> sortBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        IndexedSeq<T> sorted() { return null; }

        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        IndexedSeq<T> sorted(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends IndexedSeq<T>, ? extends IndexedSeq<T>> span(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        boolean startsWith(@Independent @NotModified Iterable<? extends T> that, int offset) { return false; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> subSequence(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        IndexedSeq<T> subSequence(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> tail() { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends IndexedSeq<T>> tailOption() { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> take(int arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> takeRight(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        IndexedSeq<T> takeRightUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        IndexedSeq<T> takeRightWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> takeUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<T> takeWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<? extends IndexedSeq<T1>, ? extends IndexedSeq<T2>> unzip(
            @IgnoreModifications @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2, T3> Tuple3<? extends IndexedSeq<T1>, ? extends IndexedSeq<T2>, ? extends IndexedSeq<T3>> unzip3(
            @IgnoreModifications @Independent @NotModified Function<
                ? super T,
                Tuple3<? extends T1, ? extends T2, ? extends T3>> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        IndexedSeq<T> update(int arg0, @Independent @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified

        IndexedSeq<T> update(
            int arg0,
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends T> arg1) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> IndexedSeq<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> IndexedSeq<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified U arg2) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> IndexedSeq<R> zipWith(
            @Independent @NotModified Iterable<? extends U> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> arg1) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        IndexedSeq<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> IndexedSeq<U> zipWithIndex(
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> arg0) {
            return null;
        }
    }

    //public interface Iterator implements Iterator<T>, Traversable<T>
    @FinalFields
    @Independent(hc = true)
    class Iterator$<T> {
        //override from io.vavr.collection.Traversable
        @Independent @Modified

        <R> Iterator<R> collect(@Independent @Modified PartialFunction<? super T, ? extends R> partialFunction) {
            return null;
        }

        @Independent @NotModified
        static <T> Iterator<T> concat(@Independent @Modified Iterable<? extends Iterable<? extends T>> iterables) {
            return null;
        }

        @Independent @NotModified
        static <T> Iterator<T> concat(@Independent @NotModified Iterable<? extends T> ... iterables) { return null; }

        @Fluent @Independent @Modified
        Iterator<T> concat(@Independent @Modified java.util.Iterator<? extends T> that) { return null; }
        @Independent @NotModified static <T> Iterator<T> continually(@Independent @NotModified T t) { return null; }
        @Independent @NotModified
        static <T> Iterator<T> continually(@Independent @Modified Supplier<? extends T> supplier) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<T> distinct() { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<T> distinctBy(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        <U> Iterator<T> distinctBy(@Independent @Modified Function<? super T, ? extends U> keyExtractor) { return null; }

        @Independent @Modified
        Iterator<T> distinctByKeepLast(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        @Independent @Modified
        <U> Iterator<T> distinctByKeepLast(@Independent @Modified Function<? super T, ? extends U> keyExtractor) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<T> drop(int n) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<T> dropRight(int n) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<T> dropUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<T> dropWhile(@Independent @Modified Predicate<? super T> predicate) { return null; }
        @Independent @NotModified static <T> Iterator<T> empty() { return null; }
        @Independent @NotModified
        static <T> Iterator<T> fill(int n, @Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> Iterator<T> fill(int n, @Independent @Modified Supplier<? extends T> s) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        Iterator<T> filter(@Independent @Modified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        Option<T> findLast(@Independent @Modified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified

        <U> Iterator<U> flatMap(@Independent @Modified Function<? super T, ? extends Iterable<? extends U>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Foldable, io.vavr.collection.Traversable
        @Identity @Independent(hc = true) @Modified

        <U> U foldRight(
            @Independent(hc = true) @Modified U zero,
            @Independent @Modified BiFunction<? super T, ? super U, ? extends U> f) { return null; }
        @Independent @NotModified static Iterator<Integer> from(int value) { return null; }
        @Independent @NotModified static Iterator<Integer> from(int value, int step) { return null; }
        @Independent @NotModified static Iterator<Long> from(long value) { return null; }
        @Independent @NotModified static Iterator<Long> from(long value, long step) { return null; }
        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @Modified
        T get() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified

        <C> io.vavr.collection.Map<C, Iterator<T>> groupBy(
            @Independent @Modified Function<? super T, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<Seq<T>> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        T head() { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<T> init() { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Option<Iterator<T>> initOption() { return null; }
        @Independent @Modified Iterator<T> intersperse(@Independent @NotModified T element) { return null; }
        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isSequential() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        @Independent @NotModified
        static <T> Iterator<T> iterate(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, ? extends T> f) { return null; }

        @Independent @NotModified
        static <T> Iterator<T> iterate(@Independent @Modified Supplier<? extends Option<? extends T>> supplier) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Iterable
        @Fluent @Independent @NotModified
        Iterator<T> iterator() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        T last() { return null; }

        //override from io.vavr.collection.Traversable
        @Modified
        int length() { return 0; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @Modified
        <U> Iterator<U> map(@Independent @Modified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @Modified
        <U> Iterator<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @Modified
        Iterator<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> Iterator<T> narrow(@Independent @NotModified Iterator<? extends T> iterator) { return null; }
        @Independent @NotModified static <T> Iterator<T> of(@Independent @NotModified T element) { return null; }
        @Independent @NotModified static <T> Iterator<T> of(@Independent @NotModified T ... elements) { return null; }
        @Identity @Independent @NotModified
        static <T> Iterator<T> ofAll(@Independent @NotModified Iterable<? extends T> iterable) { return null; }

        @Independent @NotModified
        static Iterator<Boolean> ofAll(@Independent @NotModified boolean ... elements) { return null; }

        @Independent @NotModified
        static Iterator<Byte> ofAll(@Independent @NotModified byte ... elements) { return null; }

        @Independent @NotModified
        static Iterator<Character> ofAll(@Independent @NotModified char ... elements) { return null; }

        @Independent @NotModified
        static Iterator<Double> ofAll(@Independent @NotModified double ... elements) { return null; }

        @Independent @NotModified
        static Iterator<Float> ofAll(@Independent @NotModified float ... elements) { return null; }

        @Independent @NotModified
        static Iterator<Integer> ofAll(@Independent @NotModified int ... elements) { return null; }

        @Independent @NotModified
        static <T> Iterator<T> ofAll(@Independent @Modified java.util.Iterator<? extends T> iterator) { return null; }

        @Independent @NotModified
        static Iterator<Long> ofAll(@Independent @NotModified long ... elements) { return null; }

        @Independent @NotModified
        static Iterator<Short> ofAll(@Independent @NotModified short ... elements) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<T> orElse(@Independent @NotModified Iterable<? extends T> other) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<T> orElse(@Independent @Modified Supplier<? extends Iterable<? extends T>> supplier) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        Tuple2<Iterator<T>, Iterator<T>> partition(@Independent @Modified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @Modified
        Iterator<T> peek(@Independent @Modified Consumer<? super T> action) { return null; }
        @Independent @NotModified static Iterator<Character> range(char from, char toExclusive) { return null; }
        @Independent @NotModified static Iterator<Integer> range(int from, int toExclusive) { return null; }
        @Independent @NotModified static Iterator<Long> range(long from, long toExclusive) { return null; }
        @Independent @NotModified
        static Iterator<Character> rangeBy(char from, char toExclusive, int step) { return null; }

        @Independent @NotModified
        static Iterator<Double> rangeBy(double from, double toExclusive, double step) { return null; }

        @Independent @NotModified
        static Iterator<Integer> rangeBy(int from, int toExclusive, int step) { return null; }

        @Independent @NotModified
        static Iterator<BigDecimal> rangeBy(
            @Independent @NotModified BigDecimal from,
            @Independent @NotModified BigDecimal toExclusive,
            @Independent @NotModified BigDecimal step) { return null; }

        @Independent @NotModified
        static Iterator<Long> rangeBy(long from, long toExclusive, long step) { return null; }
        @Independent @NotModified static Iterator<Character> rangeClosed(char from, char toInclusive) { return null; }
        @Independent @NotModified static Iterator<Integer> rangeClosed(int from, int toInclusive) { return null; }
        @Independent @NotModified static Iterator<Long> rangeClosed(long from, long toInclusive) { return null; }
        @Independent @NotModified
        static Iterator<Character> rangeClosedBy(char from, char toInclusive, int step) { return null; }

        @Independent @NotModified
        static Iterator<Double> rangeClosedBy(double from, double toInclusive, double step) { return null; }

        @Independent @NotModified
        static Iterator<Integer> rangeClosedBy(int from, int toInclusive, int step) { return null; }

        @Independent @NotModified
        static Iterator<Long> rangeClosedBy(long from, long toInclusive, long step) { return null; }

        //override from io.vavr.collection.Foldable, io.vavr.collection.Traversable
        @Independent @Modified
        T reduceLeft(@Independent @Modified BiFunction<? super T, ? super T, ? extends T> op) { return null; }

        //override from io.vavr.collection.Foldable, io.vavr.collection.Traversable
        @Independent @Modified
        T reduceRight(@Independent @Modified BiFunction<? super T, ? super T, ? extends T> op) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        Iterator<T> reject(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified

        Iterator<T> replace(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @Modified

        Iterator<T> replaceAll(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<T> retainAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified

        Traversable<T> scan(
            @Independent @NotModified T zero,
            @Independent @Modified BiFunction<? super T, ? super T, ? extends T> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified

        <U> Iterator<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @Modified BiFunction<? super U, ? super T, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified

        <U> Iterator<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @Modified BiFunction<? super T, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        Iterator<Seq<T>> slideBy(@Independent @Modified Function<? super T, ?> classifier) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<Seq<T>> sliding(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<Seq<T>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Tuple2<Iterator<T>, Iterator<T>> span(@Independent @Modified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        @Independent @NotModified
        static <T> Iterator<T> tabulate(int n, @Independent @Modified Function<? super Integer, ? extends T> f) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Iterator<T> tail() { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @Modified
        Option<Iterator<T>> tailOption() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        Iterator<T> take(int n) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        Iterator<T> takeRight(int n) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        Iterator<T> takeUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        Iterator<T> takeWhile(@Independent @Modified Predicate<? super T> predicate) { return null; }

        @Independent @Modified
        <U> U transform(@Independent @Modified Function<? super Iterator<T>, ? extends U> f) { return null; }

        @Independent @NotModified
        static <T> Iterator<T> unfold(
            @Independent @Modified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends T>>> f) { return null; }

        @Independent @NotModified
        static <T, U> Iterator<U> unfoldLeft(
            @Independent @Modified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends U>>> f) { return null; }

        @Independent @NotModified
        static <T, U> Iterator<U> unfoldRight(
            @Independent @Modified T seed,
            @Independent @Modified Function<? super T, Option<Tuple2<? extends U, ? extends T>>> f) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified

        <T1, T2> Tuple2<Iterator<T1>, Iterator<T2>> unzip(
            @Independent @Modified Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified

        <T1, T2, T3> Tuple3<Iterator<T1>, Iterator<T2>, Iterator<T3>> unzip3(
            @Independent @Modified Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        <U> Iterator<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified

        <U> Iterator<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified T thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified

        <U, R> Iterator<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @Modified BiFunction<? super T, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified
        Iterator<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @Modified

        <U> Iterator<U> zipWithIndex(@Independent @Modified BiFunction<? super T, ? super Integer, ? extends U> mapper) {
            return null;
        }
    }

    //public interface LinearSeq implements Seq<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: implemented by persistent collections only (VAVR.md)
    @ImmutableContainer(hc = true)
    class LinearSeq$<T> {
        static final long serialVersionUID = 0L;
        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        LinearSeq<T> append(@Independent @NotModified T arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        LinearSeq<T> appendAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        LinearSeq<T> asJava(@IgnoreModifications @Independent @NotModified Consumer<? super java.util.List<T>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        LinearSeq<T> asJavaMutable(
            @IgnoreModifications @Independent @NotModified Consumer<? super java.util.List<T>> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        PartialFunction<Integer, T> asPartialFunction() { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <R> LinearSeq<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<? extends LinearSeq<T>> combinations() { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<? extends LinearSeq<T>> combinations(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        Iterator<? extends LinearSeq<T>> crossProduct(int arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> distinct() { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> distinctBy(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinearSeq<T> distinctBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> distinctByKeepLast(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        <U> LinearSeq<T> distinctByKeepLast(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        LinearSeq<T> drop(int arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        LinearSeq<T> dropRight(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified

        LinearSeq<T> dropRightUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        LinearSeq<T> dropRightWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> dropUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> dropWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> filter(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinearSeq<U> flatMap(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, ? extends LinearSeq<T>> groupBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends C> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends LinearSeq<T>> grouped(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int indexOfSlice(@Independent @NotModified Iterable<? extends T> that, int from) { return 0; }

        //override from io.vavr.collection.Seq
        @NotModified
        int indexWhere(@Independent @NotModified Predicate<? super T> predicate, int from) { return 0; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        LinearSeq<T> init() { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends LinearSeq<T>> initOption() { return null; }

        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        LinearSeq<T> insert(int arg0, @Independent(hc = true) @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> insertAll(int arg0, @Independent(hc = true) @NotModified Iterable<? extends T> arg1) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> intersperse(@Independent(hc = true) @NotModified T arg0) { return null; }

        //override from io.vavr.PartialFunction
        @NotModified
        boolean isDefinedAt(@Independent @NotModified Integer index) { return false; }

        //override from io.vavr.collection.Seq
        @NotModified
        int lastIndexOfSlice(@Independent @NotModified Iterable<? extends T> that, int end) { return 0; }

        //override from io.vavr.collection.Seq
        @NotModified
        int lastIndexWhere(@Independent @NotModified Predicate<? super T> predicate, int end) { return 0; }

        //override from io.vavr.Value, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinearSeq<U> map(@IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> LinearSeq<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> LinearSeq<T> narrow(@Independent @NotModified LinearSeq<? extends T> linearSeq) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> orElse(@Independent(hc = true) @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        LinearSeq<T> orElse(
            @IgnoreModifications @Independent @NotModified Supplier<? extends Iterable<? extends T>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> padTo(int arg0, @Independent @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends LinearSeq<T>, ? extends LinearSeq<T>> partition(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> patch(int arg0, @Independent @NotModified Iterable<? extends T> arg1, int arg2) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> peek(@IgnoreModifications @Independent @NotModified Consumer<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<? extends LinearSeq<T>> permutations() { return null; }

        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        LinearSeq<T> prepend(@Independent @NotModified T arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        LinearSeq<T> prependAll(@Independent(hc = true) @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> reject(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> remove(@Independent @NotModified T arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> removeAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> removeAll(@Independent @NotModified T arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> removeAll(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> removeAt(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> removeFirst(@IgnoreModifications @Independent @NotModified Predicate<T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> removeLast(@IgnoreModifications @Independent @NotModified Predicate<T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        LinearSeq<T> replace(@Independent @NotModified T arg0, @Independent(hc = true) @NotModified T arg1) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        LinearSeq<T> replaceAll(@Independent @NotModified T arg0, @Independent(hc = true) @NotModified T arg1) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> retainAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> reverse() { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        Iterator<T> reverseIterator() { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> rotateLeft(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> rotateRight(int arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        LinearSeq<T> scan(
            @Independent @NotModified T arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> arg1) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinearSeq<U> scanLeft(
            @Independent @NotModified U arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> arg1) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinearSeq<U> scanRight(
            @Independent @NotModified U arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> arg1) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @NotModified
        int search(@Independent @NotModified T element) { return 0; }

        //override from io.vavr.collection.Seq
        @NotModified

        int search(@Independent @NotModified T element, @Independent @NotModified Comparator<? super T> comparator) {
            return 0;
        }

        //override from io.vavr.collection.Seq
        @NotModified
        int segmentLength(@Independent @NotModified Predicate<? super T> predicate, int from) { return 0; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> shuffle() { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> slice(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<? extends LinearSeq<T>> slideBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ?> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends LinearSeq<T>> sliding(int arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends LinearSeq<T>> sliding(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        <U> LinearSeq<T> sortBy(
            @Independent @NotModified Comparator<? super U> arg0,
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg1) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        <U extends Comparable<? super U>> LinearSeq<T> sortBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> sorted() { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> sorted(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends LinearSeq<T>, ? extends LinearSeq<T>> span(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        LinearSeq<T> subSequence(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> subSequence(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        LinearSeq<T> tail() { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends LinearSeq<T>> tailOption() { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> take(int arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> takeRight(int arg0) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        LinearSeq<T> takeRightUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        LinearSeq<T> takeRightWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> takeUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<T> takeWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<? extends LinearSeq<T1>, ? extends LinearSeq<T2>> unzip(
            @IgnoreModifications @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        LinearSeq<T> update(int arg0, @Independent @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        LinearSeq<T> update(
            int arg0,
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends T> arg1) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> LinearSeq<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> arg0) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinearSeq<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified U arg2) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> LinearSeq<R> zipWith(
            @Independent @NotModified Iterable<? extends U> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> arg1) {
            return null;
        }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        LinearSeq<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinearSeq<U> zipWithIndex(
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> arg0) {
            return null;
        }
    }

    //public final class LinkedHashMap implements Map<K,V>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class LinkedHashMap$<K, V> {
        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2, V2> LinkedHashMap<K2, V2> bimap(
            @Independent @NotModified Function<? super K, ? extends K2> keyMapper,
            @Independent @NotModified Function<? super V, ? extends V2> valueMapper) { return null; }

        @Independent @NotModified
        static <K, V> Collector<Tuple2<K, V>, ArrayList<Tuple2<K, V>>, LinkedHashMap<K, V>> collector() { return null; }

        @Independent @NotModified
        static <K, V, T extends V> Collector<T, ArrayList<T>, LinkedHashMap<K, V>> collector(
            @Independent @NotModified Function<? super T, ? extends K> keyMapper) { return null; }

        @Independent @NotModified
        static <K, V, T> Collector<T, ArrayList<T>, LinkedHashMap<K, V>> collector(
            @Independent @NotModified Function<? super T, ? extends K> keyMapper,
            @Independent @NotModified Function<? super T, ? extends V> valueMapper) { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        Tuple2<V, LinkedHashMap<K, V>> computeIfAbsent(
            @Independent @NotModified K key,
            @Independent @NotModified Function<? super K, ? extends V> mappingFunction) { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        Tuple2<Option<V>, LinkedHashMap<K, V>> computeIfPresent(
            @Independent @NotModified K key,
            @Independent @NotModified BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from io.vavr.collection.Map
        @NotModified
        boolean containsKey(@Independent(hc = true) @NotModified K key) { return false; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashMap<K, V> distinct() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        LinkedHashMap<K, V> distinctBy(@Independent @NotModified Comparator<? super Tuple2<K, V>> comparator) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinkedHashMap<K, V> distinctBy(
            @Independent @NotModified Function<? super Tuple2<K, V>, ? extends U> keyExtractor) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashMap<K, V> drop(int n) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashMap<K, V> dropRight(int n) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        LinkedHashMap<K, V> dropUntil(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        LinkedHashMap<K, V> dropWhile(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) {
            return null;
        }
        @Independent @NotModified static <K, V> LinkedHashMap<K, V> empty() { return null; }
        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> fill(
            int n,
            @Independent @NotModified Supplier<? extends Tuple2<? extends K, ? extends V>> s) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        LinkedHashMap<K, V> filter(@Independent @NotModified BiPredicate<? super K, ? super V> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        LinkedHashMap<K, V> filter(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        LinkedHashMap<K, V> filterKeys(@Independent @NotModified Predicate<? super K> predicate) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        LinkedHashMap<K, V> filterValues(@Independent @NotModified Predicate<? super V> predicate) { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        <K2, V2> LinkedHashMap<K2, V2> flatMap(
            @Independent @NotModified BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent(hc = true) @NotModified
        Option<V> get(@Independent(hc = true) @NotModified K key) { return null; }

        //override from io.vavr.collection.Map
        @Independent(hc = true) @NotModified

        V getOrElse(@Independent(hc = true) @NotModified K key, @Independent(hc = true) @NotModified V defaultValue) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, LinkedHashMap<K, V>> groupBy(
            @Independent @NotModified Function<? super Tuple2<K, V>, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<LinkedHashMap<K, V>> grouped(int size) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Tuple2<K, V> head() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        LinkedHashMap<K, V> init() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<LinkedHashMap<K, V>> initOption() { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isSequential() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Map, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent @NotModified
        Iterator<Tuple2<K, V>> iterator() { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified
        io.vavr.collection.Set<K> keySet() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Tuple2<K, V> last() { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2, V2> LinkedHashMap<K2, V2> map(
            @Independent @NotModified BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2> LinkedHashMap<K2, V> mapKeys(@Independent @NotModified Function<? super K, ? extends K2> keyMapper) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2> LinkedHashMap<K2, V> mapKeys(
            @Independent @NotModified Function<? super K, ? extends K2> keyMapper,
            @Independent @NotModified BiFunction<? super V, ? super V, ? extends V> valueMerge) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <W> LinkedHashMap<K, W> mapValues(@Independent @NotModified Function<? super V, ? extends W> mapper) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        LinkedHashMap<K, V> merge(@Independent @NotModified io.vavr.collection.Map<? extends K, ? extends V> that) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        <U extends V> LinkedHashMap<K, V> merge(
            @Independent @NotModified io.vavr.collection.Map<? extends K, U> that,
            @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> collisionResolution) { return null; }

        @Identity @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> narrow(
            @Independent @NotModified LinkedHashMap<? extends K, ? extends V> linkedHashMap) { return null; }

        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> of(@Independent @NotModified K key, @Independent @NotModified V value) {
            return null;
        }

        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> of(
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
        static <K, V> LinkedHashMap<K, V> of(
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
        static <K, V> LinkedHashMap<K, V> of(
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
        static <K, V> LinkedHashMap<K, V> of(
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
        static <K, V> LinkedHashMap<K, V> of(
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
        static <K, V> LinkedHashMap<K, V> of(
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
        static <K, V> LinkedHashMap<K, V> of(@Independent @NotModified Tuple2<? extends K, ? extends V> entry) {
            return null;
        }

        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> ofAll(@Independent @NotModified Map<? extends K, ? extends V> map) {
            return null;
        }

        @Independent @NotModified
        static <T, K, V> LinkedHashMap<K, V> ofAll(
            @Independent @NotModified Stream<? extends T> stream,
            @Independent @NotModified Function<? super T, Tuple2<? extends K, ? extends V>> entryMapper) { return null; }

        @Independent @NotModified
        static <T, K, V> LinkedHashMap<K, V> ofAll(
            @Independent @NotModified Stream<? extends T> stream,
            @Independent @NotModified Function<? super T, ? extends K> keyMapper,
            @Independent @NotModified Function<? super T, ? extends V> valueMapper) { return null; }

        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> ofEntries(
            @Independent @NotModified Iterable<? extends Tuple2<? extends K, ? extends V>> entries) { return null; }

        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> ofEntries(
            @Independent @NotModified Tuple2<? extends K, ? extends V> ... entries) { return null; }

        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> ofEntries(
            @Independent @NotModified Map.Entry<? extends K, ? extends V> ... entries) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashMap<K, V> orElse(@Independent @NotModified Iterable<? extends Tuple2<K, V>> other) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        LinkedHashMap<K, V> orElse(
            @Independent @NotModified Supplier<? extends Iterable<? extends Tuple2<K, V>>> supplier) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<LinkedHashMap<K, V>, LinkedHashMap<K, V>> partition(
            @Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashMap<K, V> peek(@Independent @NotModified Consumer<? super Tuple2<K, V>> action) { return null; }

        //override from io.vavr.collection.Map
        @Independent(hc = true) @NotModified

        LinkedHashMap<K, V> put(
            @Independent(hc = true) @NotModified K key,
            @Independent(hc = true) @NotModified V value) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <U extends V> LinkedHashMap<K, V> put(
            @Independent @NotModified K key,
            @Independent @NotModified U value,
            @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> merge) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        LinkedHashMap<K, V> put(@Independent @NotModified Tuple2<? extends K, ? extends V> entry) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <U extends V> LinkedHashMap<K, V> put(
            @Independent @NotModified Tuple2<? extends K, U> entry,
            @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> merge) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        LinkedHashMap<K, V> reject(@Independent @NotModified BiPredicate<? super K, ? super V> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        LinkedHashMap<K, V> reject(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        LinkedHashMap<K, V> rejectKeys(@Independent @NotModified Predicate<? super K> predicate) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        LinkedHashMap<K, V> rejectValues(@Independent @NotModified Predicate<? super V> predicate) { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified
        LinkedHashMap<K, V> remove(@Independent(hc = true) @NotModified K key) { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified
        LinkedHashMap<K, V> removeAll(@Independent @NotModified Iterable<? extends K> keys) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        LinkedHashMap<K, V> removeAll(@Independent @NotModified BiPredicate<? super K, ? super V> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        LinkedHashMap<K, V> removeKeys(@Independent @NotModified Predicate<? super K> predicate) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        LinkedHashMap<K, V> removeValues(@Independent @NotModified Predicate<? super V> predicate) { return null; }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        LinkedHashMap<K, V> replace(
            @Independent @NotModified K key,
            @Independent @NotModified V oldValue,
            @Independent @NotModified V newValue) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        LinkedHashMap<K, V> replace(
            @Independent(hc = true) @NotModified Tuple2<K, V> currentElement,
            @Independent(hc = true) @NotModified Tuple2<K, V> newElement) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        LinkedHashMap<K, V> replaceAll(
            @Independent @NotModified Tuple2<K, V> currentElement,
            @Independent @NotModified Tuple2<K, V> newElement) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        LinkedHashMap<K, V> replaceAll(@Independent @NotModified BiFunction<? super K, ? super V, ? extends V> function) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified

        LinkedHashMap<K, V> replaceValue(@Independent @NotModified K key, @Independent @NotModified V value) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        LinkedHashMap<K, V> retainAll(@Independent @NotModified Iterable<? extends Tuple2<K, V>> elements) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        LinkedHashMap<K, V> scan(
            @Independent @NotModified Tuple2<K, V> zero,
            @Independent @NotModified BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>>
                operation) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @NotModified
        int size() { return 0; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<LinkedHashMap<K, V>> slideBy(@Independent @NotModified Function<? super Tuple2<K, V>, ?> classifier) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<LinkedHashMap<K, V>> sliding(int size) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<LinkedHashMap<K, V>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<LinkedHashMap<K, V>, LinkedHashMap<K, V>> span(
            @Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        @Independent @NotModified
        static <K, V> LinkedHashMap<K, V> tabulate(
            int n,
            @Independent @NotModified Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        LinkedHashMap<K, V> tail() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<LinkedHashMap<K, V>> tailOption() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashMap<K, V> take(int n) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashMap<K, V> takeRight(int n) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        LinkedHashMap<K, V> takeUntil(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        LinkedHashMap<K, V> takeWhile(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Fluent @Independent @NotModified
        java.util.LinkedHashMap<K, V> toJavaMap() { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public String toString() { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        Seq<V> values() { return null; }
    }

    //public final class LinkedHashMultimap extends AbstractMultimap<K,V,LinkedHashMultimap<K,V>> implements Serializable
    //annotated as EXPECTED; computed @FinalFields @Container @Independent -- G1: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class LinkedHashMultimap$<K, V> {
        //public static class Builder
        @Immutable(hc = true)
        @Independent
        class Builder<V> {
            @Independent @NotModified
            <K, V2 extends V> Collector<Tuple2<K, V2>, ArrayList<Tuple2<K, V2>>, Multimap<K, V2>> collector() {
                return null;
            }
            @Independent @NotModified <K, V2 extends V> LinkedHashMultimap<K, V2> empty() { return null; }
            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> fill(
                int n,
                @Independent @NotModified Tuple2<? extends K, ? extends V2> element) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> fill(
                int n,
                @Independent @Modified Supplier<? extends Tuple2<? extends K, ? extends V2>> s) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> of(
                @Independent @NotModified K key,
                @Independent @Modified V2 value) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @NotModified K k2,
                @Independent @Modified V2 v2) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @NotModified K k3,
                @Independent @Modified V2 v3) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @NotModified K k4,
                @Independent @Modified V2 v4) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @NotModified K k5,
                @Independent @Modified V2 v5) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @NotModified K k6,
                @Independent @Modified V2 v6) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @NotModified K k7,
                @Independent @Modified V2 v7) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @Modified K k7,
                @Independent @Modified V2 v7,
                @Independent @NotModified K k8,
                @Independent @Modified V2 v8) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @Modified K k7,
                @Independent @Modified V2 v7,
                @Independent @Modified K k8,
                @Independent @Modified V2 v8,
                @Independent @NotModified K k9,
                @Independent @Modified V2 v9) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @Modified K k7,
                @Independent @Modified V2 v7,
                @Independent @Modified K k8,
                @Independent @Modified V2 v8,
                @Independent @Modified K k9,
                @Independent @Modified V2 v9,
                @Independent @NotModified K k10,
                @Independent @Modified V2 v10) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> of(
                @Independent @Modified Tuple2<? extends K, ? extends V2> entry) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> ofAll(
                @Independent @NotModified Map<? extends K, ? extends V2> map) { return null; }

            @Independent @NotModified
            <T, K, V2 extends V> LinkedHashMultimap<K, V2> ofAll(
                @Independent @Modified Stream<? extends T> stream,
                @Independent @Modified Function<? super T, Tuple2<? extends K, ? extends V2>> entryMapper) {
                return null;
            }

            @Independent @NotModified
            <T, K, V2 extends V> LinkedHashMultimap<K, V2> ofAll(
                @Independent @Modified Stream<? extends T> stream,
                @Independent @Modified Function<? super T, ? extends K> keyMapper,
                @Independent @Modified Function<? super T, ? extends V2> valueMapper) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> ofEntries(
                @Independent @Modified Iterable<? extends Tuple2<? extends K, ? extends V2>> entries) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> ofEntries(
                @Independent @NotModified Tuple2<? extends K, ? extends V2> ... entries) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> ofEntries(
                @Independent @NotModified Map.Entry<? extends K, ? extends V2> ... entries) { return null; }

            @Independent @NotModified
            <K, V2 extends V> LinkedHashMultimap<K, V2> tabulate(
                int n,
                @Independent @Modified Function<? super Integer, ? extends Tuple2<? extends K, ? extends V2>> f) {
                return null;
            }
        }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isSequential() { return false; }

        @Identity @Independent @NotModified
        static <K, V> LinkedHashMultimap<K, V> narrow(
            @Independent @NotModified LinkedHashMultimap<? extends K, ? extends V> map) { return null; }
        @Independent @NotModified static <V> LinkedHashMultimap.Builder<V> withSeq() { return null; }
        @Independent @NotModified static <V> LinkedHashMultimap.Builder<V> withSet() { return null; }
        @Independent @NotModified
        static <V extends Comparable<?>> LinkedHashMultimap.Builder<V> withSortedSet() { return null; }

        @Independent @NotModified
        static <V> LinkedHashMultimap.Builder<V> withSortedSet(
            @Independent @NotModified Comparator<? super V> comparator) { return null; }
    }

    //public final class LinkedHashSet implements Set<T>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class LinkedHashSet$<T> {
        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified
        LinkedHashSet<T> add(@Independent(hc = true) @NotModified T element) { return null; }

        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified
        LinkedHashSet<T> addAll(@Independent(hc = true) @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <R> LinkedHashSet<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> partialFunction) {
            return null;
        }
        @Independent @NotModified static <T> Collector<T, ArrayList<T>, LinkedHashSet<T>> collector() { return null; }
        //override from io.vavr.Value, io.vavr.collection.Set
        @NotModified
        boolean contains(@Independent(hc = true) @NotModified T element) { return false; }

        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified
        LinkedHashSet<T> diff(@Independent @NotModified io.vavr.collection.Set<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<T> distinct() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        LinkedHashSet<T> distinctBy(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinkedHashSet<T> distinctBy(@Independent @NotModified Function<? super T, ? extends U> keyExtractor) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<T> drop(int n) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<T> dropRight(int n) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<T> dropUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<T> dropWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }
        @Independent @NotModified static <T> LinkedHashSet<T> empty() { return null; }
        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        @Independent @NotModified
        static <T> LinkedHashSet<T> fill(int n, @Independent @NotModified Supplier<? extends T> s) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<T> filter(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        <U> LinkedHashSet<U> flatMap(
            @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) { return null; }

        //override from io.vavr.collection.Foldable, io.vavr.collection.Traversable
        @Identity @Independent @NotModified

        <U> U foldRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> f) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, LinkedHashSet<T>> groupBy(
            @Independent @NotModified Function<? super T, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<LinkedHashSet<T>> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T head() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Option<T> headOption() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        LinkedHashSet<T> init() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<LinkedHashSet<T>> initOption() { return null; }

        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified

        LinkedHashSet<T> intersect(@Independent @NotModified io.vavr.collection.Set<? extends T> elements) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isSequential() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent @NotModified
        Iterator<T> iterator() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T last() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @NotModified
        int length() { return 0; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        <U> LinkedHashSet<U> map(@Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        <U> LinkedHashSet<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<Void> mapToVoid() { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified

        String mkString(
            @Independent @NotModified CharSequence prefix,
            @Independent @NotModified CharSequence delimiter,
            @Independent @NotModified CharSequence suffix) { return null; }

        @Identity @Independent @NotModified
        static <T> LinkedHashSet<T> narrow(@Independent @NotModified LinkedHashSet<? extends T> linkedHashSet) {
            return null;
        }
        @Independent @NotModified static <T> LinkedHashSet<T> of(@Independent @NotModified T element) { return null; }
        @Independent @NotModified
        static <T> LinkedHashSet<T> of(@Independent @NotModified T ... elements) { return null; }

        @Independent @NotModified
        static <T> LinkedHashSet<T> ofAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Boolean> ofAll(@Independent @NotModified boolean ... elements) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Byte> ofAll(@Independent @NotModified byte ... elements) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Character> ofAll(@Independent @NotModified char ... elements) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Double> ofAll(@Independent @NotModified double ... elements) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Float> ofAll(@Independent @NotModified float ... elements) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Integer> ofAll(@Independent @NotModified int ... elements) { return null; }

        @Independent @NotModified
        static <T> LinkedHashSet<T> ofAll(@Independent @NotModified Stream<? extends T> javaStream) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Long> ofAll(@Independent @NotModified long ... elements) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Short> ofAll(@Independent @NotModified short ... elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<T> orElse(@Independent(hc = true) @NotModified Iterable<? extends T> other) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        LinkedHashSet<T> orElse(@Independent @NotModified Supplier<? extends Iterable<? extends T>> supplier) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<LinkedHashSet<T>, LinkedHashSet<T>> partition(@Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<T> peek(@Independent @NotModified Consumer<? super T> action) { return null; }
        @Independent @NotModified static LinkedHashSet<Character> range(char from, char toExclusive) { return null; }
        @Independent @NotModified static LinkedHashSet<Integer> range(int from, int toExclusive) { return null; }
        @Independent @NotModified static LinkedHashSet<Long> range(long from, long toExclusive) { return null; }
        @Independent @NotModified
        static LinkedHashSet<Character> rangeBy(char from, char toExclusive, int step) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Double> rangeBy(double from, double toExclusive, double step) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Integer> rangeBy(int from, int toExclusive, int step) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Long> rangeBy(long from, long toExclusive, long step) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Character> rangeClosed(char from, char toInclusive) { return null; }
        @Independent @NotModified static LinkedHashSet<Integer> rangeClosed(int from, int toInclusive) { return null; }
        @Independent @NotModified static LinkedHashSet<Long> rangeClosed(long from, long toInclusive) { return null; }
        @Independent @NotModified
        static LinkedHashSet<Character> rangeClosedBy(char from, char toInclusive, int step) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Double> rangeClosedBy(double from, double toInclusive, double step) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Integer> rangeClosedBy(int from, int toInclusive, int step) { return null; }

        @Independent @NotModified
        static LinkedHashSet<Long> rangeClosedBy(long from, long toInclusive, long step) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<T> reject(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified
        LinkedHashSet<T> remove(@Independent(hc = true) @NotModified T element) { return null; }

        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified
        LinkedHashSet<T> removeAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        LinkedHashSet<T> replace(
            @Independent(hc = true) @NotModified T currentElement,
            @Independent @NotModified T newElement) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        LinkedHashSet<T> replaceAll(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<T> retainAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        LinkedHashSet<T> scan(
            @Independent @NotModified T zero,
            @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> operation) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinkedHashSet<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinkedHashSet<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<LinkedHashSet<T>> slideBy(@Independent @NotModified Function<? super T, ?> classifier) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<LinkedHashSet<T>> sliding(int size) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<LinkedHashSet<T>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<LinkedHashSet<T>, LinkedHashSet<T>> span(@Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        @Independent @NotModified
        static <T> LinkedHashSet<T> tabulate(int n, @Independent @NotModified Function<? super Integer, ? extends T> f) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        LinkedHashSet<T> tail() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Option<LinkedHashSet<T>> tailOption() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        LinkedHashSet<T> take(int n) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        LinkedHashSet<T> takeRight(int n) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<T> takeUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        LinkedHashSet<T> takeWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set
        @Independent @NotModified
        java.util.LinkedHashSet<T> toJavaSet() { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent @NotModified
        <U> U transform(@Independent @NotModified Function<? super LinkedHashSet<T>, ? extends U> f) { return null; }

        //override from io.vavr.collection.Set
        @Fluent @Independent @NotModified

        LinkedHashSet<T> union(@Independent(hc = true) @NotModified io.vavr.collection.Set<? extends T> elements) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<LinkedHashSet<T1>, LinkedHashSet<T2>> unzip(
            @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2, T3> Tuple3<LinkedHashSet<T1>, LinkedHashSet<T2>, LinkedHashSet<T3>> unzip3(
            @Independent @NotModified Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> LinkedHashSet<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinkedHashSet<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified T thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> LinkedHashSet<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        LinkedHashSet<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> LinkedHashSet<U> zipWithIndex(
            @Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> mapper) { return null; }
    }

    //public interface List implements LinearSeq<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class List$<T> {
        static final long serialVersionUID = 0L;
        //static final class Cons implements List<T>, Serializable
        //annotated as EXPECTED; computed @FinalFields @Container @Independent(hc = true) -- G1: persistent collection (VAVR.md)
        @ImmutableContainer(hc = true)
        class Cons<T> {
            //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object o) { return false; }

            //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.collection.Traversable
            @Independent(hc = true) @NotModified @GetSet("head")
            T head() { return null; }

            //override from io.vavr.Value, io.vavr.collection.List, io.vavr.collection.Traversable
            @NotModified
            boolean isEmpty() { return false; }

            //override from io.vavr.collection.List, io.vavr.collection.Traversable
            @NotModified @GetSet("length")
            int length() { return 0; }

            //override from io.vavr.collection.LinearSeq, io.vavr.collection.List, io.vavr.collection.Seq, io.vavr.collection.Traversable
            @Independent(hc = true) @NotModified @GetSet("tail")
            List<T> tail() { return null; }

            //override from io.vavr.Value, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        //static final class Nil implements List<T>, Serializable
        //annotated as EXPECTED; computed @FinalFields @Container @Dependent -- G1, G2: persistent collection (VAVR.md)
        @ImmutableContainer(hc = true)
        class Nil<T> {
            //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object o) { return false; }

            //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.collection.Traversable
            @Independent @NotModified
            T head() { return null; }
            @Independent @NotModified static <T> List.Nil<T> instance() { return null; }
            //override from io.vavr.Value, io.vavr.collection.List, io.vavr.collection.Traversable
            @NotModified
            boolean isEmpty() { return false; }

            //override from io.vavr.collection.List, io.vavr.collection.Traversable
            @NotModified
            int length() { return 0; }

            //override from io.vavr.collection.LinearSeq, io.vavr.collection.List, io.vavr.collection.Seq, io.vavr.collection.Traversable
            @Independent @NotModified
            List<T> tail() { return null; }

            //override from io.vavr.Value, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> append(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> appendAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        java.util.List<T> asJava() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> asJava(@Independent @NotModified Consumer<? super java.util.List<T>> action) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        java.util.List<T> asJavaMutable() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> asJavaMutable(@Independent @NotModified Consumer<? super java.util.List<T>> action) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <R> List<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> partialFunction) {
            return null;
        }
        @Independent @NotModified static <T> Collector<T, ArrayList<T>, List<T>> collector() { return null; }
        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<List<T>> combinations() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<List<T>> combinations(int k) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Iterator<List<T>> crossProduct(int power) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        List<T> distinct() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        List<T> distinctBy(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        <U> List<T> distinctBy(@Independent @NotModified Function<? super T, ? extends U> keyExtractor) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> distinctByKeepLast(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U> List<T> distinctByKeepLast(@Independent @NotModified Function<? super T, ? extends U> keyExtractor) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        List<T> drop(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        List<T> dropRight(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> dropRightUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> dropRightWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        List<T> dropUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        List<T> dropWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }
        @Independent @NotModified static <T> List<T> empty() { return null; }
        @Independent @NotModified static <T> List<T> fill(int n, @Independent @NotModified T element) { return null; }
        @Independent @NotModified
        static <T> List<T> fill(int n, @Independent @NotModified Supplier<? extends T> s) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        List<T> filter(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> List<U> flatMap(@Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        T get(int index) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, List<T>> groupBy(
            @Independent @NotModified Function<? super T, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<List<T>> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.collection.Seq
        @NotModified
        int indexOf(@Independent @NotModified T element, int from) { return 0; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        List<T> init() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<List<T>> initOption() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> insert(int index, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> insertAll(int index, @Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> intersperse(@Independent @NotModified T element) { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T last() { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int lastIndexOf(@Independent @NotModified T element, int end) { return 0; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> leftPadTo(int length, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        int length() { return 0; }

        //override from io.vavr.Value, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> List<U> map(@Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> List<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        List<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> List<T> narrow(@Independent @NotModified List<? extends T> list) { return null; }
        @Independent @NotModified static <T> List<T> of(@Independent @NotModified T element) { return null; }
        @Independent @NotModified static <T> List<T> of(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified
        static <T> List<T> ofAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        @Independent @NotModified
        static List<Boolean> ofAll(@Independent @NotModified boolean ... elements) { return null; }
        @Independent @NotModified static List<Byte> ofAll(@Independent @NotModified byte ... elements) { return null; }
        @Independent @NotModified
        static List<Character> ofAll(@Independent @NotModified char ... elements) { return null; }

        @Independent @NotModified
        static List<Double> ofAll(@Independent @NotModified double ... elements) { return null; }

        @Independent @NotModified
        static List<Float> ofAll(@Independent @NotModified float ... elements) { return null; }

        @Independent @NotModified
        static List<Integer> ofAll(@Independent @NotModified int ... elements) { return null; }

        @Independent @NotModified
        static <T> List<T> ofAll(@Independent @NotModified Stream<? extends T> javaStream) { return null; }
        @Independent @NotModified static List<Long> ofAll(@Independent @NotModified long ... elements) { return null; }
        @Independent @NotModified
        static List<Short> ofAll(@Independent @NotModified short ... elements) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        List<T> orElse(@Independent @NotModified Iterable<? extends T> other) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        List<T> orElse(@Independent @NotModified Supplier<? extends Iterable<? extends T>> supplier) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> padTo(int length, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<List<T>, List<T>> partition(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> patch(int from, @Independent @NotModified Iterable<? extends T> that, int replaced) { return null; }
        @Independent @NotModified T peek() { return null; }
        //override from io.vavr.Value, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        List<T> peek(@Independent @NotModified Consumer<? super T> action) { return null; }
        @Independent @NotModified Option<T> peekOption() { return null; }
        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<List<T>> permutations() { return null; }
        @Independent @NotModified List<T> pop() { return null; }
        @Independent @NotModified Tuple2<T, List<T>> pop2() { return null; }
        @Independent @NotModified Option<Tuple2<T, List<T>>> pop2Option() { return null; }
        @Independent @NotModified Option<List<T>> popOption() { return null; }
        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> prepend(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> prependAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }
        @Fluent @Independent @NotModified List<T> push(@Independent @NotModified T element) { return null; }
        @Independent @NotModified List<T> push(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified List<T> pushAll(@Independent @NotModified Iterable<T> elements) { return null; }
        @Independent @NotModified static List<Character> range(char from, char toExclusive) { return null; }
        @Independent @NotModified static List<Integer> range(int from, int toExclusive) { return null; }
        @Independent @NotModified static List<Long> range(long from, long toExclusive) { return null; }
        @Independent @NotModified
        static List<Character> rangeBy(char from, char toExclusive, int step) { return null; }

        @Independent @NotModified
        static List<Double> rangeBy(double from, double toExclusive, double step) { return null; }
        @Independent @NotModified static List<Integer> rangeBy(int from, int toExclusive, int step) { return null; }
        @Independent @NotModified static List<Long> rangeBy(long from, long toExclusive, long step) { return null; }
        @Independent @NotModified static List<Character> rangeClosed(char from, char toInclusive) { return null; }
        @Independent @NotModified static List<Integer> rangeClosed(int from, int toInclusive) { return null; }
        @Independent @NotModified static List<Long> rangeClosed(long from, long toInclusive) { return null; }
        @Independent @NotModified
        static List<Character> rangeClosedBy(char from, char toInclusive, int step) { return null; }

        @Independent @NotModified
        static List<Double> rangeClosedBy(double from, double toInclusive, double step) { return null; }

        @Independent @NotModified
        static List<Integer> rangeClosedBy(int from, int toInclusive, int step) { return null; }

        @Independent @NotModified
        static List<Long> rangeClosedBy(long from, long toInclusive, long step) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        List<T> reject(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> remove(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> removeAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> removeAll(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> removeAll(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> removeAt(int index) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> removeFirst(@Independent @NotModified Predicate<T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> removeLast(@Independent @NotModified Predicate<T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        List<T> replace(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        List<T> replaceAll(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        List<T> retainAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> reverse() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> rotateLeft(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> rotateRight(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        List<T> scan(
            @Independent @NotModified T zero,
            @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> operation) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> List<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> operation) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> List<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> shuffle() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> slice(int beginIndex, int endIndex) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<List<T>> slideBy(@Independent @NotModified Function<? super T, ?> classifier) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<List<T>> sliding(int size) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<List<T>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U> List<T> sortBy(
            @Independent @NotModified Comparator<? super U> comparator,
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U extends Comparable<? super U>> List<T> sortBy(
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> sorted() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> sorted(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<List<T>, List<T>> span(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        Tuple2<List<T>, List<T>> splitAt(int n) { return null; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Tuple2<List<T>, List<T>> splitAt(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified

        Tuple2<List<T>, List<T>> splitAtInclusive(@Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> subSequence(int beginIndex) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        List<T> subSequence(int beginIndex, int endIndex) { return null; }

        @Independent @NotModified
        static <T> List<T> tabulate(int n, @Independent @NotModified Function<? super Integer, ? extends T> f) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        List<T> tail() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<List<T>> tailOption() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        List<T> take(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        List<T> takeRight(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> takeRightUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> takeRightWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        List<T> takeUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        List<T> takeWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        <U> U transform(@Independent @NotModified Function<? super List<T>, ? extends U> f) { return null; }

        @Identity @Independent @NotModified
        static <T> List<List<T>> transpose(@Independent @NotModified List<List<T>> matrix) { return null; }

        @Independent @NotModified
        static <T> List<T> unfold(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends T>>> f) { return null; }

        @Independent @NotModified
        static <T, U> List<U> unfoldLeft(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends U>>> f) { return null; }

        @Independent @NotModified
        static <T, U> List<U> unfoldRight(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends U, ? extends T>>> f) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<List<T1>, List<T2>> unzip(
            @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2, T3> Tuple3<List<T1>, List<T2>, List<T3>> unzip3(
            @Independent @NotModified Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> update(int index, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        List<T> update(int index, @Independent @NotModified Function<? super T, ? extends T> updater) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> List<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> List<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified T thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> List<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        List<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> List<U> zipWithIndex(@Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> mapper) {
            return null;
        }
    }

    //public interface Map implements Traversable<Tuple2<K,V>>, PartialFunction<K,V>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: implemented by persistent collections only (VAVR.md)
    @ImmutableContainer(hc = true)
    class Map$<K, V> {
        static final long serialVersionUID = 0L;
        //override from io.vavr.Function1, io.vavr.PartialFunction, java.util.function.Function
        @Independent @NotModified
        V apply(@Independent @NotModified K key) { return null; }
        @Independent @NotModified PartialFunction<K, V> asPartialFunction() { return null; }
        @Independent @NotModified
        <K2, V2> io.vavr.collection.Map<K2, V2> bimap(
            @IgnoreModifications @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @IgnoreModifications @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <R> Seq<R> collect(@Independent @NotModified PartialFunction<? super Tuple2<K, V>, ? extends R> partialFunction) {
            return null;
        }

        @Independent @NotModified
        Tuple2<V, ? extends io.vavr.collection.Map<K, V>> computeIfAbsent(
            @Independent @NotModified K arg0,
            @IgnoreModifications @Independent @NotModified Function<? super K, ? extends V> arg1) { return null; }

        @Independent @NotModified
        Tuple2<Option<V>, ? extends io.vavr.collection.Map<K, V>> computeIfPresent(
            @Independent @NotModified K arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super K, ? super V, ? extends V> arg1) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        boolean contains(@Independent @NotModified Tuple2<K, V> element) { return false; }
        @NotModified boolean containsKey(@Independent(hc = true) @NotModified K arg0) { return false; }
        @NotModified boolean containsValue(@Independent @NotModified V value) { return false; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Map<K, V> distinct() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> distinctBy(@Independent @NotModified Comparator<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Map<K, V> distinctBy(
            @IgnoreModifications @Independent @NotModified Function<? super Tuple2<K, V>, ? extends U> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Map<K, V> drop(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Map<K, V> dropRight(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> dropUntil(
            @IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> dropWhile(
            @IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) { return null; }

        @Independent @NotModified
        static <K, V> Tuple2<K, V> entry(@Independent @NotModified K key, @Independent @NotModified V value) {
            return null;
        }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> filter(
            @IgnoreModifications @Independent @NotModified BiPredicate<? super K, ? super V> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> filter(
            @IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) { return null; }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> filterKeys(
            @IgnoreModifications @Independent @NotModified Predicate<? super K> arg0) { return null; }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> filterValues(
            @IgnoreModifications @Independent @NotModified Predicate<? super V> arg0) { return null; }

        @Independent @NotModified
        <K2, V2> io.vavr.collection.Map<K2, V2> flatMap(
            @IgnoreModifications @Independent @NotModified BiFunction<
                ? super K,
                ? super V,
                ? extends Iterable<Tuple2<K2, V2>>> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> flatMap(
            @Independent @NotModified Function<? super Tuple2<K, V>, ? extends Iterable<? extends U>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Foldable, io.vavr.collection.Traversable
        @Identity @Independent @NotModified

        <U> U foldRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super Tuple2<K, V>, ? super U, ? extends U> f) { return null; }
        @NotModified void forEach(@Independent @NotModified BiConsumer<K, V> action) { }
        @Independent(hc = true) @NotModified
        Option<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        @Independent(hc = true) @NotModified
        V getOrElse(@Independent(hc = true) @NotModified K arg0, @Independent(hc = true) @NotModified V arg1) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, ? extends io.vavr.collection.Map<K, V>> groupBy(
            @IgnoreModifications @Independent @NotModified Function<? super Tuple2<K, V>, ? extends C> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends io.vavr.collection.Map<K, V>> grouped(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Map<K, V> init() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends io.vavr.collection.Map<K, V>> initOption() { return null; }

        //override from io.vavr.PartialFunction
        @NotModified
        boolean isDefinedAt(@Independent @NotModified K key) { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isDistinct() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent(hc = true) @NotModified @NotNull
        Iterator<Tuple2<K, V>> iterator() { return null; }

        @Independent @NotModified
        <U> Iterator<U> iterator(@Independent @NotModified BiFunction<K, V, ? extends U> mapper) { return null; }
        @Independent @NotModified io.vavr.collection.Set<K> keySet() { return null; }
        @Independent @NotModified Iterator<K> keysIterator() { return null; }
        //override from io.vavr.collection.Traversable
        @NotModified
        int length() { return 0; }

        //override from io.vavr.PartialFunction
        @Independent @NotModified
        Function1<K, Option<V>> lift() { return null; }

        @Independent @NotModified
        <K2, V2> io.vavr.collection.Map<K2, V2> map(
            @IgnoreModifications @Independent @NotModified BiFunction<? super K, ? super V, Tuple2<K2, V2>> arg0) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Seq<U> map(@Independent @NotModified Function<? super Tuple2<K, V>, ? extends U> mapper) { return null; }

        @Independent @NotModified
        <K2> io.vavr.collection.Map<K2, V> mapKeys(
            @IgnoreModifications @Independent @NotModified Function<? super K, ? extends K2> arg0) { return null; }

        @Independent @NotModified
        <K2> io.vavr.collection.Map<K2, V> mapKeys(
            @IgnoreModifications @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super V, ? super V, ? extends V> arg1) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Seq<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<Void> mapToVoid() { return null; }

        @Independent @NotModified
        <V2> io.vavr.collection.Map<K, V2> mapValues(
            @IgnoreModifications @Independent @NotModified Function<? super V, ? extends V2> arg0) { return null; }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> merge(
            @Independent @NotModified io.vavr.collection.Map<? extends K, ? extends V> arg0) { return null; }

        @Independent @NotModified
        <U extends V> io.vavr.collection.Map<K, V> merge(
            @Independent @NotModified io.vavr.collection.Map<? extends K, U> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> arg1) {
            return null;
        }

        @Identity @Independent @NotModified
        static <K, V> io.vavr.collection.Map<K, V> narrow(
            @Independent @NotModified io.vavr.collection.Map<? extends K, ? extends V> map) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> orElse(@Independent @NotModified Iterable<? extends Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> orElse(
            @IgnoreModifications @Independent @NotModified Supplier<? extends Iterable<? extends Tuple2<K, V>>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends io.vavr.collection.Map<K, V>, ? extends io.vavr.collection.Map<K, V>> partition(
            @IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> peek(
            @IgnoreModifications @Independent @NotModified Consumer<? super Tuple2<K, V>> arg0) { return null; }

        @Independent(hc = true) @NotModified
        io.vavr.collection.Map<K, V> put(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified V arg1) { return null; }

        @Independent @NotModified
        <U extends V> io.vavr.collection.Map<K, V> put(
            @Independent @NotModified K arg0,
            @Independent @NotModified U arg1,
            @IgnoreModifications @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> arg2) {
            return null;
        }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> put(@Independent @NotModified Tuple2<? extends K, ? extends V> arg0) { return null; }

        @Independent @NotModified
        <U extends V> io.vavr.collection.Map<K, V> put(
            @Independent @NotModified Tuple2<? extends K, U> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> arg1) {
            return null;
        }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> reject(
            @IgnoreModifications @Independent @NotModified BiPredicate<? super K, ? super V> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> reject(
            @IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) { return null; }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> rejectKeys(
            @IgnoreModifications @Independent @NotModified Predicate<? super K> arg0) { return null; }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> rejectValues(
            @IgnoreModifications @Independent @NotModified Predicate<? super V> arg0) { return null; }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> remove(@Independent(hc = true) @NotModified K arg0) { return null; }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> removeAll(@Independent(hc = true) @NotModified Iterable<? extends K> arg0) {
            return null;
        }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> removeAll(
            @IgnoreModifications @Independent @NotModified BiPredicate<? super K, ? super V> arg0) { return null; }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> removeKeys(
            @IgnoreModifications @Independent @NotModified Predicate<? super K> arg0) { return null; }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> removeValues(
            @IgnoreModifications @Independent @NotModified Predicate<? super V> arg0) { return null; }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> replace(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified V arg2) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> replace(
            @Independent(hc = true) @NotModified Tuple2<K, V> arg0,
            @Independent(hc = true) @NotModified Tuple2<K, V> arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> replaceAll(
            @Independent @NotModified Tuple2<K, V> arg0,
            @Independent @NotModified Tuple2<K, V> arg1) { return null; }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> replaceAll(
            @IgnoreModifications @Independent @NotModified BiFunction<? super K, ? super V, ? extends V> arg0) {
            return null;
        }

        @Independent @NotModified
        io.vavr.collection.Map<K, V> replaceValue(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> retainAll(@Independent @NotModified Iterable<? extends Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> scan(
            @Independent @NotModified Tuple2<K, V> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<
                ? super Tuple2<K, V>,
                ? super Tuple2<K, V>,
                ? extends Tuple2<K, V>> arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super Tuple2<K, V>, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super Tuple2<K, V>, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        int size() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<? extends io.vavr.collection.Map<K, V>> slideBy(
            @IgnoreModifications @Independent @NotModified Function<? super Tuple2<K, V>, ?> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends io.vavr.collection.Map<K, V>> sliding(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends io.vavr.collection.Map<K, V>> sliding(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends io.vavr.collection.Map<K, V>, ? extends io.vavr.collection.Map<K, V>> span(
            @IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        io.vavr.collection.Map<K, V> tail() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends io.vavr.collection.Map<K, V>> tailOption() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Map<K, V> take(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Map<K, V> takeRight(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> takeUntil(
            @IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Map<K, V> takeWhile(
            @IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) { return null; }
        @Independent @NotModified Map<K, V> toJavaMap() { return null; }
        @Independent @NotModified
        <U> U transform(@Independent @NotModified Function<? super io.vavr.collection.Map<K, V>, ? extends U> f) {
            return null;
        }
        @Independent(hc = true) @NotModified Tuple2<Seq<K>, Seq<V>> unzip() { return null; }
        @Independent(hc = true) @NotModified
        <T1, T2> Tuple2<Seq<T1>, Seq<T2>> unzip(
            @Independent @NotModified BiFunction<? super K, ? super V, Tuple2<? extends T1, ? extends T2>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <T1, T2> Tuple2<Seq<T1>, Seq<T2>> unzip(
            @Independent @NotModified Function<? super Tuple2<K, V>, Tuple2<? extends T1, ? extends T2>> unzipper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <T1, T2, T3> Tuple3<Seq<T1>, Seq<T2>, Seq<T3>> unzip3(
            @Independent @NotModified BiFunction<? super K, ? super V, Tuple3<? extends T1, ? extends T2, ? extends T3>>
                unzipper) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <T1, T2, T3> Tuple3<Seq<T1>, Seq<T2>, Seq<T3>> unzip3(
            @Independent @NotModified Function<? super Tuple2<K, V>, Tuple3<? extends T1, ? extends T2, ? extends T3>>
                unzipper) { return null; }
        @Independent @NotModified Seq<V> values() { return null; }
        @Independent @NotModified Iterator<V> valuesIterator() { return null; }
        @Independent @NotModified
        Function1<K, V> withDefault(@Independent @NotModified Function<? super K, ? extends V> defaultFunction) {
            return null;
        }

        @Independent @NotModified
        Function1<K, V> withDefaultValue(@Independent @NotModified V defaultValue) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Seq<Tuple2<Tuple2<K, V>, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<Tuple2<Tuple2<K, V>, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified Tuple2<K, V> thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> Seq<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super Tuple2<K, V>, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<Tuple2<Tuple2<K, V>, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> zipWithIndex(
            @Independent @NotModified BiFunction<? super Tuple2<K, V>, ? super Integer, ? extends U> mapper) {
            return null;
        }
    }

    //public interface Multimap implements Traversable<Tuple2<K,V>>, PartialFunction<K,Traversable<V>>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: implemented by persistent collections only (VAVR.md)
    @ImmutableContainer(hc = true)
    class Multimap$<K, V> {
        static final long serialVersionUID = 0L;
        //enum ContainerType extends Enum<ContainerType>
        @FinalFields
        @Independent
        class ContainerType {
            @Independent @NotModified static final Multimap.ContainerType SEQ = null;
            @Independent @NotModified static final Multimap.ContainerType SET = null;
            @Independent @NotModified static final Multimap.ContainerType SORTED_SET = null;
        }

        //override from io.vavr.Function1, io.vavr.PartialFunction, java.util.function.Function
        @Independent @NotModified
        Traversable<V> apply(@Independent @NotModified K key) { return null; }
        @Independent(hc = true) @NotModified io.vavr.collection.Map<K, Traversable<V>> asMap() { return null; }
        @Independent @NotModified PartialFunction<K, Traversable<V>> asPartialFunction() { return null; }
        @Independent @NotModified
        <K2, V2> Multimap<K2, V2> bimap(
            @IgnoreModifications @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @IgnoreModifications @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <R> Seq<R> collect(@Independent @NotModified PartialFunction<? super Tuple2<K, V>, ? extends R> partialFunction) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        boolean contains(@Independent @NotModified Tuple2<K, V> element) { return false; }
        @NotModified boolean containsKey(@Independent(hc = true) @NotModified K arg0) { return false; }
        @NotModified boolean containsValue(@Independent @NotModified V value) { return false; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Multimap<K, V> distinct() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Multimap<K, V> distinctBy(@Independent @NotModified Comparator<? super Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Multimap<K, V> distinctBy(
            @IgnoreModifications @Independent @NotModified Function<? super Tuple2<K, V>, ? extends U> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Multimap<K, V> drop(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Multimap<K, V> dropRight(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Multimap<K, V> dropUntil(@IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Multimap<K, V> dropWhile(@IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        @Independent @NotModified
        Multimap<K, V> filter(@IgnoreModifications @Independent @NotModified BiPredicate<? super K, ? super V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Multimap<K, V> filter(@IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        @Independent @NotModified
        Multimap<K, V> filterKeys(@IgnoreModifications @Independent @NotModified Predicate<? super K> arg0) {
            return null;
        }

        @Independent @NotModified
        Multimap<K, V> filterValues(@IgnoreModifications @Independent @NotModified Predicate<? super V> arg0) {
            return null;
        }

        @Independent @NotModified
        <K2, V2> Multimap<K2, V2> flatMap(
            @IgnoreModifications @Independent @NotModified BiFunction<
                ? super K,
                ? super V,
                ? extends Iterable<Tuple2<K2, V2>>> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> flatMap(
            @Independent @NotModified Function<? super Tuple2<K, V>, ? extends Iterable<? extends U>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Foldable, io.vavr.collection.Traversable
        @Identity @Independent @NotModified

        <U> U foldRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super Tuple2<K, V>, ? super U, ? extends U> f) { return null; }
        @NotModified void forEach(@Independent @NotModified BiConsumer<K, V> action) { }
        @Independent @NotModified Option<Traversable<V>> get(@Independent @NotModified K arg0) { return null; }
        @Independent(hc = true) @NotModified Multimap.ContainerType getContainerType() { return null; }
        @Independent(hc = true) @NotModified
        Traversable<V> getOrElse(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Traversable<? extends V> arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, ? extends Multimap<K, V>> groupBy(
            @IgnoreModifications @Independent @NotModified Function<? super Tuple2<K, V>, ? extends C> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends Multimap<K, V>> grouped(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Multimap<K, V> init() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends Multimap<K, V>> initOption() { return null; }

        //override from io.vavr.PartialFunction
        @NotModified
        boolean isDefinedAt(@Independent @NotModified K key) { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isDistinct() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent @NotModified @NotNull
        Iterator<Tuple2<K, V>> iterator() { return null; }

        @Independent @NotModified
        <U> Iterator<U> iterator(@Independent @NotModified BiFunction<K, V, ? extends U> mapper) { return null; }
        @Independent(hc = true) @NotModified io.vavr.collection.Set<K> keySet() { return null; }
        //override from io.vavr.collection.Traversable
        @NotModified
        int length() { return 0; }

        @Independent @NotModified
        <K2, V2> Multimap<K2, V2> map(
            @IgnoreModifications @Independent @NotModified BiFunction<? super K, ? super V, Tuple2<K2, V2>> arg0) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Seq<U> map(@Independent @NotModified Function<? super Tuple2<K, V>, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Seq<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<Void> mapToVoid() { return null; }

        @Independent @NotModified
        <V2> Multimap<K, V2> mapValues(
            @IgnoreModifications @Independent @NotModified Function<? super V, ? extends V2> arg0) { return null; }

        @Independent @NotModified
        Multimap<K, V> merge(@Independent @NotModified Multimap<? extends K, ? extends V> arg0) { return null; }

        @Independent @NotModified
        <K2 extends K, V2 extends V> Multimap<K, V> merge(
            @Independent @NotModified Multimap<K2, V2> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<Traversable<V>, Traversable<V2>, Traversable<V>>
                arg1) { return null; }

        @Identity @Independent @NotModified
        static <K, V> Multimap<K, V> narrow(@Independent @NotModified Multimap<? extends K, ? extends V> map) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Multimap<K, V> orElse(@Independent @NotModified Iterable<? extends Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Multimap<K, V> orElse(
            @IgnoreModifications @Independent @NotModified Supplier<? extends Iterable<? extends Tuple2<K, V>>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends Multimap<K, V>, ? extends Multimap<K, V>> partition(
            @IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified

        Multimap<K, V> peek(@IgnoreModifications @Independent @NotModified Consumer<? super Tuple2<K, V>> arg0) {
            return null;
        }

        @Independent @NotModified
        Multimap<K, V> put(@Independent(hc = true) @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        @Independent @NotModified
        Multimap<K, V> put(@Independent(hc = true) @NotModified Tuple2<? extends K, ? extends V> arg0) { return null; }

        @Independent @NotModified
        Multimap<K, V> reject(@IgnoreModifications @Independent @NotModified BiPredicate<? super K, ? super V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Multimap<K, V> reject(@IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        @Independent @NotModified
        Multimap<K, V> rejectKeys(@IgnoreModifications @Independent @NotModified Predicate<? super K> arg0) {
            return null;
        }

        @Independent @NotModified
        Multimap<K, V> rejectValues(@IgnoreModifications @Independent @NotModified Predicate<? super V> arg0) {
            return null;
        }
        @Independent @NotModified Multimap<K, V> remove(@Independent(hc = true) @NotModified K arg0) { return null; }
        @Independent @NotModified
        Multimap<K, V> remove(@Independent(hc = true) @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        @Independent @NotModified
        Multimap<K, V> removeAll(@Independent @NotModified Iterable<? extends K> arg0) { return null; }

        @Independent @NotModified
        Multimap<K, V> removeAll(@IgnoreModifications @Independent @NotModified BiPredicate<? super K, ? super V> arg0) {
            return null;
        }

        @Independent @NotModified
        Multimap<K, V> removeKeys(@IgnoreModifications @Independent @NotModified Predicate<? super K> arg0) {
            return null;
        }

        @Independent @NotModified
        Multimap<K, V> removeValues(@IgnoreModifications @Independent @NotModified Predicate<? super V> arg0) {
            return null;
        }

        @Independent @NotModified
        Multimap<K, V> replace(
            @Independent(hc = true) @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified V arg2) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Multimap<K, V> replace(
            @Independent(hc = true) @NotModified Tuple2<K, V> arg0,
            @Independent @NotModified Tuple2<K, V> arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Multimap<K, V> replaceAll(
            @Independent(hc = true) @NotModified Tuple2<K, V> arg0,
            @Independent @NotModified Tuple2<K, V> arg1) { return null; }

        @Independent @NotModified
        Multimap<K, V> replaceAll(
            @IgnoreModifications @Independent @NotModified BiFunction<? super K, ? super V, ? extends V> arg0) {
            return null;
        }

        @Independent @NotModified
        Multimap<K, V> replaceValue(@Independent(hc = true) @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Multimap<K, V> retainAll(@Independent @NotModified Iterable<? extends Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Multimap<K, V> scan(
            @Independent @NotModified Tuple2<K, V> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<
                ? super Tuple2<K, V>,
                ? super Tuple2<K, V>,
                ? extends Tuple2<K, V>> arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super Tuple2<K, V>, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super Tuple2<K, V>, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        int size() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<? extends Multimap<K, V>> slideBy(
            @IgnoreModifications @Independent @NotModified Function<? super Tuple2<K, V>, ?> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends Multimap<K, V>> sliding(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends Multimap<K, V>> sliding(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends Multimap<K, V>, ? extends Multimap<K, V>> span(
            @IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Multimap<K, V> tail() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends Multimap<K, V>> tailOption() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Multimap<K, V> take(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Multimap<K, V> takeRight(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Multimap<K, V> takeUntil(@IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Multimap<K, V> takeWhile(@IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }
        @Independent @NotModified Map<K, Collection<V>> toJavaMap() { return null; }
        @Independent @NotModified
        <U> U transform(@Independent @NotModified Function<? super Multimap<K, V>, ? extends U> f) { return null; }

        @Independent(hc = true) @NotModified
        <T1, T2> Tuple2<Seq<T1>, Seq<T2>> unzip(
            @Independent @NotModified BiFunction<? super K, ? super V, Tuple2<? extends T1, ? extends T2>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <T1, T2> Tuple2<Seq<T1>, Seq<T2>> unzip(
            @Independent @NotModified Function<? super Tuple2<K, V>, Tuple2<? extends T1, ? extends T2>> unzipper) {
            return null;
        }

        @Independent(hc = true) @NotModified
        <T1, T2, T3> Tuple3<Seq<T1>, Seq<T2>, Seq<T3>> unzip3(
            @Independent @NotModified BiFunction<? super K, ? super V, Tuple3<? extends T1, ? extends T2, ? extends T3>>
                unzipper) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <T1, T2, T3> Tuple3<Seq<T1>, Seq<T2>, Seq<T3>> unzip3(
            @Independent @NotModified Function<? super Tuple2<K, V>, Tuple3<? extends T1, ? extends T2, ? extends T3>>
                unzipper) { return null; }
        @Independent @NotModified Traversable<V> values() { return null; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Seq<Tuple2<Tuple2<K, V>, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<Tuple2<Tuple2<K, V>, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified Tuple2<K, V> thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> Seq<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super Tuple2<K, V>, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<Tuple2<Tuple2<K, V>, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> zipWithIndex(
            @Independent @NotModified BiFunction<? super Tuple2<K, V>, ? super Integer, ? extends U> mapper) {
            return null;
        }
    }

    //public interface Ordered
    @ImmutableContainer(hc = true)
    class Ordered$<T> {@Independent(hc = true) @NotModified Comparator<T> comparator() { return null; } }

    //public final class PriorityQueue extends AbstractQueue<T,PriorityQueue<T>> implements Serializable, Ordered<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class PriorityQueue$<T> {
        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <R> PriorityQueue<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> partialFunction) {
            return null;
        }
        @Independent @NotModified static <T> Collector<T, ArrayList<T>, PriorityQueue<T>> collector() { return null; }
        //override from io.vavr.collection.Ordered
        @Independent(hc = true) @NotModified
        Comparator<T> comparator() { return null; }

        //override from io.vavr.collection.AbstractQueue
        @Independent(hc = true) @NotModified
        Tuple2<T, PriorityQueue<T>> dequeue() { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        PriorityQueue<T> distinct() { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        PriorityQueue<T> distinctBy(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        <U> PriorityQueue<T> distinctBy(@Independent @NotModified Function<? super T, ? extends U> keyExtractor) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        PriorityQueue<T> drop(int n) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        PriorityQueue<T> dropRight(int n) { return null; }

        //override from io.vavr.collection.AbstractQueue, io.vavr.collection.Traversable
        @Independent @NotModified
        PriorityQueue<T> dropWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }
        @Independent @NotModified static <T extends Comparable<? super T>> PriorityQueue<T> empty() { return null; }
        @Independent @NotModified
        static <T> PriorityQueue<T> empty(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.AbstractQueue
        @Independent(hc = true) @NotModified
        PriorityQueue<T> enqueue(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.AbstractQueue
        @Independent @NotModified
        PriorityQueue<T> enqueueAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        @Independent @NotModified
        static <T> PriorityQueue<T> fill(int size, @Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> PriorityQueue<T> fill(int size, @Independent @NotModified Supplier<? extends T> supplier) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        PriorityQueue<T> filter(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        <U> PriorityQueue<U> flatMap(
            @Independent @NotModified Comparator<U> comparator,
            @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> PriorityQueue<U> flatMap(
            @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) { return null; }

        //override from io.vavr.collection.Foldable, io.vavr.collection.Traversable
        @Identity @Independent @NotModified

        <U> U foldRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> accumulator) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, ? extends PriorityQueue<T>> groupBy(
            @Independent @NotModified Function<? super T, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends PriorityQueue<T>> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T head() { return null; }

        //override from io.vavr.collection.AbstractQueue, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        PriorityQueue<T> init() { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isOrdered() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T last() { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified @GetSet("size")
        int length() { return 0; }

        @Independent @NotModified
        <U> PriorityQueue<U> map(
            @Independent @NotModified Comparator<U> comparator,
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> PriorityQueue<U> map(@Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> PriorityQueue<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        PriorityQueue<Void> mapToVoid() { return null; }

        @Independent @NotModified
        PriorityQueue<T> merge(@Independent @NotModified PriorityQueue<T> target) { return null; }

        @Identity @Independent @NotModified
        static <T> PriorityQueue<T> narrow(@Independent @NotModified PriorityQueue<? extends T> queue) { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> PriorityQueue<T> of(@Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> PriorityQueue<T> of(@Independent @NotModified T ... elements) {
            return null;
        }

        @Independent @NotModified
        static <T> PriorityQueue<T> of(
            @Independent @NotModified Comparator<? super T> comparator,
            @Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> PriorityQueue<T> of(
            @Independent @NotModified Comparator<? super T> comparator,
            @Independent @NotModified T ... elements) { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> PriorityQueue<T> ofAll(
            @Independent @NotModified Iterable<? extends T> elements) { return null; }

        @Independent @NotModified
        static <T> PriorityQueue<T> ofAll(
            @Independent @NotModified Comparator<? super T> comparator,
            @Independent @NotModified Iterable<? extends T> elements) { return null; }

        @Independent @NotModified
        static <T> PriorityQueue<T> ofAll(
            @Independent @NotModified Comparator<? super T> comparator,
            @Independent @NotModified Stream<? extends T> javaStream) { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> PriorityQueue<T> ofAll(
            @Independent @NotModified Stream<? extends T> javaStream) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        PriorityQueue<T> orElse(@Independent @NotModified Iterable<? extends T> other) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        PriorityQueue<T> orElse(@Independent @NotModified Supplier<? extends Iterable<? extends T>> supplier) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        Tuple2<? extends PriorityQueue<T>, ? extends PriorityQueue<T>> partition(
            @Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        PriorityQueue<T> replace(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        PriorityQueue<T> replaceAll(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        PriorityQueue<T> scan(
            @Independent @NotModified T zero,
            @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> PriorityQueue<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> PriorityQueue<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<? extends PriorityQueue<T>> slideBy(@Independent @NotModified Function<? super T, ?> classifier) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends PriorityQueue<T>> sliding(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends PriorityQueue<T>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends PriorityQueue<T>, ? extends PriorityQueue<T>> span(
            @Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        @Independent @NotModified
        static <T> PriorityQueue<T> tabulate(
            int size,
            @Independent @NotModified Function<? super Integer, ? extends T> function) { return null; }

        //override from io.vavr.collection.AbstractQueue, io.vavr.collection.Traversable
        @Independent @NotModified
        PriorityQueue<T> tail() { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        PriorityQueue<T> take(int n) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        PriorityQueue<T> takeRight(int n) { return null; }

        //override from io.vavr.collection.AbstractQueue, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        PriorityQueue<T> takeUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value
        @Independent(hc = true) @NotModified
        List<T> toList() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<? extends PriorityQueue<T1>, ? extends PriorityQueue<T2>> unzip(
            @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2, T3> Tuple3<? extends PriorityQueue<T1>, ? extends PriorityQueue<T2>, ? extends PriorityQueue<T3>>
            unzip3(
            @Independent @NotModified Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        <U> PriorityQueue<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> PriorityQueue<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified T thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> PriorityQueue<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        PriorityQueue<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> PriorityQueue<U> zipWithIndex(
            @Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> mapper) { return null; }
    }

    //public final class Queue extends AbstractQueue<T,Queue<T>> implements LinearSeq<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class Queue$<T> {
        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        Queue<T> append(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> appendAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        java.util.List<T> asJava() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<T> asJava(@Independent @NotModified Consumer<? super java.util.List<T>> action) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        java.util.List<T> asJavaMutable() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<T> asJavaMutable(@Independent @NotModified Consumer<? super java.util.List<T>> action) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <R> Queue<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> partialFunction) {
            return null;
        }
        @Independent @NotModified static <T> Collector<T, ArrayList<T>, Queue<T>> collector() { return null; }
        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<Queue<T>> combinations() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<Queue<T>> combinations(int k) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Iterator<Queue<T>> crossProduct(int power) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Queue<T> distinct() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Queue<T> distinctBy(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Queue<T> distinctBy(@Independent @NotModified Function<? super T, ? extends U> keyExtractor) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<T> distinctByKeepLast(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U> Queue<T> distinctByKeepLast(@Independent @NotModified Function<? super T, ? extends U> keyExtractor) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Queue<T> drop(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Queue<T> dropRight(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        Queue<T> dropRightUntil(@Independent(hc = true) @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        Queue<T> dropRightWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }

        //override from io.vavr.collection.AbstractQueue, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Queue<T> dropWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }
        @Independent @NotModified static <T> Queue<T> empty() { return null; }
        //override from io.vavr.collection.AbstractQueue
        @Independent(hc = true) @NotModified
        Queue<T> enqueue(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.AbstractQueue
        @Fluent @Independent @NotModified
        Queue<T> enqueueAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }
        @Independent @NotModified static <T> Queue<T> fill(int n, @Independent @NotModified T element) { return null; }
        @Independent @NotModified
        static <T> Queue<T> fill(int n, @Independent @NotModified Supplier<? extends T> s) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Queue<T> filter(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Queue<U> flatMap(@Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        T get(int index) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, Queue<T>> groupBy(
            @Independent @NotModified Function<? super T, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Queue<T>> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        T head() { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int indexOf(@Independent @NotModified T element, int from) { return 0; }

        //override from io.vavr.collection.AbstractQueue, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Queue<T> init() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        Queue<T> insert(int index, @Independent(hc = true) @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> insertAll(int index, @Independent(hc = true) @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> intersperse(@Independent(hc = true) @NotModified T element) { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        T last() { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int lastIndexOf(@Independent @NotModified T element, int end) { return 0; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> leftPadTo(int length, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        int length() { return 0; }

        //override from io.vavr.Value, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Queue<U> map(@Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Queue<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Queue<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> Queue<T> narrow(@Independent @NotModified Queue<? extends T> queue) { return null; }
        @Independent @NotModified static <T> Queue<T> of(@Independent @NotModified T element) { return null; }
        @Independent @NotModified static <T> Queue<T> of(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified
        static <T> Queue<T> ofAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        @Independent @NotModified
        static Queue<Boolean> ofAll(@Independent @NotModified boolean ... elements) { return null; }

        @Independent @NotModified
        static Queue<Byte> ofAll(@Independent @NotModified byte ... elements) { return null; }

        @Independent @NotModified
        static Queue<Character> ofAll(@Independent @NotModified char ... elements) { return null; }

        @Independent @NotModified
        static Queue<Double> ofAll(@Independent @NotModified double ... elements) { return null; }

        @Independent @NotModified
        static Queue<Float> ofAll(@Independent @NotModified float ... elements) { return null; }

        @Independent @NotModified
        static Queue<Integer> ofAll(@Independent @NotModified int ... elements) { return null; }

        @Independent @NotModified
        static <T> Queue<T> ofAll(@Independent @NotModified Stream<? extends T> javaStream) { return null; }

        @Independent @NotModified
        static Queue<Long> ofAll(@Independent @NotModified long ... elements) { return null; }

        @Independent @NotModified
        static Queue<Short> ofAll(@Independent @NotModified short ... elements) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Queue<T> orElse(@Independent(hc = true) @NotModified Iterable<? extends T> other) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Queue<T> orElse(@Independent @NotModified Supplier<? extends Iterable<? extends T>> supplier) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> padTo(int length, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<Queue<T>, Queue<T>> partition(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<T> patch(int from, @Independent @NotModified Iterable<? extends T> that, int replaced) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<Queue<T>> permutations() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        Queue<T> prepend(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> prependAll(@Independent(hc = true) @NotModified Iterable<? extends T> elements) { return null; }
        @Independent @NotModified static Queue<Character> range(char from, char toExclusive) { return null; }
        @Independent @NotModified static Queue<Integer> range(int from, int toExclusive) { return null; }
        @Independent @NotModified static Queue<Long> range(long from, long toExclusive) { return null; }
        @Independent @NotModified
        static Queue<Character> rangeBy(char from, char toExclusive, int step) { return null; }

        @Independent @NotModified
        static Queue<Double> rangeBy(double from, double toExclusive, double step) { return null; }
        @Independent @NotModified static Queue<Integer> rangeBy(int from, int toExclusive, int step) { return null; }
        @Independent @NotModified static Queue<Long> rangeBy(long from, long toExclusive, long step) { return null; }
        @Independent @NotModified static Queue<Character> rangeClosed(char from, char toInclusive) { return null; }
        @Independent @NotModified static Queue<Integer> rangeClosed(int from, int toInclusive) { return null; }
        @Independent @NotModified static Queue<Long> rangeClosed(long from, long toInclusive) { return null; }
        @Independent @NotModified
        static Queue<Character> rangeClosedBy(char from, char toInclusive, int step) { return null; }

        @Independent @NotModified
        static Queue<Double> rangeClosedBy(double from, double toInclusive, double step) { return null; }

        @Independent @NotModified
        static Queue<Integer> rangeClosedBy(int from, int toInclusive, int step) { return null; }

        @Independent @NotModified
        static Queue<Long> rangeClosedBy(long from, long toInclusive, long step) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> remove(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> removeAll(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<T> removeAt(int index) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> removeFirst(@Independent @NotModified Predicate<T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> removeLast(@Independent @NotModified Predicate<T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        Queue<T> replace(@Independent @NotModified T currentElement, @Independent(hc = true) @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        Queue<T> replaceAll(
            @Independent @NotModified T currentElement,
            @Independent(hc = true) @NotModified T newElement) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> reverse() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> rotateLeft(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> rotateRight(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Queue<T> scan(
            @Independent @NotModified T zero,
            @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> operation) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Queue<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> operation) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Queue<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> shuffle() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<T> slice(int beginIndex, int endIndex) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Queue<T>> slideBy(@Independent @NotModified Function<? super T, ?> classifier) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Queue<T>> sliding(int size) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Queue<T>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U> Queue<T> sortBy(
            @Independent @NotModified Comparator<? super U> comparator,
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U extends Comparable<? super U>> Queue<T> sortBy(
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<T> sorted() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<T> sorted(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<Queue<T>, Queue<T>> span(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        Tuple2<Queue<T>, Queue<T>> splitAt(int n) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        Tuple2<Queue<T>, Queue<T>> splitAt(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        Tuple2<Queue<T>, Queue<T>> splitAtInclusive(@Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @NotModified
        boolean startsWith(@Independent @NotModified Iterable<? extends T> that, int offset) { return false; }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        Queue<T> subSequence(int beginIndex) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> subSequence(int beginIndex, int endIndex) { return null; }

        @Independent @NotModified
        static <T> Queue<T> tabulate(int n, @Independent @NotModified Function<? super Integer, ? extends T> f) {
            return null;
        }

        //override from io.vavr.collection.AbstractQueue, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Queue<T> tail() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Queue<T> take(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Queue<T> takeRight(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> takeRightUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Queue<T> takeRightWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.AbstractQueue, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Queue<T> takeUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        <U> U transform(@Independent @NotModified Function<? super Queue<T>, ? extends U> f) { return null; }

        @Identity @Independent @NotModified
        static <T> Queue<Queue<T>> transpose(@Independent @NotModified Queue<Queue<T>> matrix) { return null; }

        @Independent @NotModified
        static <T> Queue<T> unfold(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends T>>> f) { return null; }

        @Independent @NotModified
        static <T, U> Queue<U> unfoldLeft(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends U>>> f) { return null; }

        @Independent @NotModified
        static <T, U> Queue<U> unfoldRight(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends U, ? extends T>>> f) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<Queue<T1>, Queue<T2>> unzip(
            @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2, T3> Tuple3<Queue<T1>, Queue<T2>, Queue<T3>> unzip3(
            @Independent @NotModified Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<T> update(int index, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Queue<T> update(int index, @Independent @NotModified Function<? super T, ? extends T> updater) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Queue<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Queue<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified T thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> Queue<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Queue<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Queue<U> zipWithIndex(@Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> mapper) {
            return null;
        }
    }

    //public interface Seq implements Traversable<T>, PartialFunction<Integer,T>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: implemented by persistent collections only (VAVR.md)
    @ImmutableContainer(hc = true)
    class Seq$<T> {
        static final long serialVersionUID = 0L;
        @Independent(hc = true) @NotModified Seq<T> append(@Independent @NotModified T arg0) { return null; }
        @Independent(hc = true) @NotModified
        Seq<T> appendAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.Function1, io.vavr.PartialFunction, java.util.function.Function
        @Independent @NotModified
        T apply(@Independent @NotModified Integer index) { return null; }
        @Independent @NotModified java.util.List<T> asJava() { return null; }
        @Independent @NotModified
        Seq<T> asJava(@IgnoreModifications @Independent @NotModified Consumer<? super java.util.List<T>> arg0) {
            return null;
        }
        @Independent @NotModified java.util.List<T> asJavaMutable() { return null; }
        @Independent @NotModified
        Seq<T> asJavaMutable(@IgnoreModifications @Independent @NotModified Consumer<? super java.util.List<T>> arg0) {
            return null;
        }
        @Independent @NotModified PartialFunction<Integer, T> asPartialFunction() { return null; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        <R> Seq<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> arg0) { return null; }
        @Independent @NotModified Seq<? extends Seq<T>> combinations() { return null; }
        @Independent @NotModified Seq<? extends Seq<T>> combinations(int arg0) { return null; }
        @NotModified boolean containsSlice(@Independent @NotModified Iterable<? extends T> that) { return false; }
        @Independent @NotModified Iterator<Tuple2<T, T>> crossProduct() { return null; }
        @Independent @NotModified
        <U> Iterator<Tuple2<T, U>> crossProduct(@Independent @NotModified Iterable<? extends U> that) { return null; }
        @Independent @NotModified Iterator<? extends Seq<T>> crossProduct(int arg0) { return null; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> distinct() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> distinctBy(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<T> distinctBy(@IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) {
            return null;
        }

        @Independent @NotModified
        Seq<T> distinctByKeepLast(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent @NotModified
        <U> Seq<T> distinctByKeepLast(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Seq<T> drop(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Seq<T> dropRight(int arg0) { return null; }

        @Independent(hc = true) @NotModified
        Seq<T> dropRightUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        @Independent @NotModified
        Seq<T> dropRightWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> dropUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> dropWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }
        @NotModified boolean endsWith(@Independent @NotModified Seq<? extends T> that) { return false; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> filter(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> flatMap(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Foldable, io.vavr.collection.Traversable
        @Identity @Independent @NotModified

        <U> U foldRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> f) { return null; }
        @Independent(hc = true) @NotModified T get(int arg0) { return null; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, ? extends Seq<T>> groupBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends C> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends Seq<T>> grouped(int arg0) { return null; }
        @NotModified int indexOf(@Independent @NotModified T element) { return 0; }
        @NotModified int indexOf(@Independent @NotModified T arg0, int arg1) { return 0; }
        @Independent @NotModified Option<Integer> indexOfOption(@Independent @NotModified T element) { return null; }
        @Independent @NotModified
        Option<Integer> indexOfOption(@Independent @NotModified T element, int from) { return null; }
        @NotModified int indexOfSlice(@Independent @NotModified Iterable<? extends T> that) { return 0; }
        @NotModified int indexOfSlice(@Independent @NotModified Iterable<? extends T> arg0, int arg1) { return 0; }
        @Independent @NotModified
        Option<Integer> indexOfSliceOption(@Independent @NotModified Iterable<? extends T> that) { return null; }

        @Independent @NotModified
        Option<Integer> indexOfSliceOption(@Independent @NotModified Iterable<? extends T> that, int from) { return null; }
        @NotModified int indexWhere(@Independent @NotModified Predicate<? super T> predicate) { return 0; }
        @NotModified
        int indexWhere(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0, int arg1) { return 0; }

        @Independent @NotModified
        Option<Integer> indexWhereOption(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        Option<Integer> indexWhereOption(@Independent @NotModified Predicate<? super T> predicate, int from) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Seq<T> init() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends Seq<T>> initOption() { return null; }

        @Independent(hc = true) @NotModified
        Seq<T> insert(int arg0, @Independent(hc = true) @NotModified T arg1) { return null; }

        @Independent @NotModified
        Seq<T> insertAll(int arg0, @Independent(hc = true) @NotModified Iterable<? extends T> arg1) { return null; }
        @Independent @NotModified Seq<T> intersperse(@Independent(hc = true) @NotModified T arg0) { return null; }
        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isSequential() { return false; }
        @Independent @NotModified Iterator<T> iterator(int index) { return null; }
        @NotModified int lastIndexOf(@Independent @NotModified T element) { return 0; }
        @NotModified int lastIndexOf(@Independent @NotModified T arg0, int arg1) { return 0; }
        @Independent @NotModified
        Option<Integer> lastIndexOfOption(@Independent @NotModified T element) { return null; }

        @Independent @NotModified
        Option<Integer> lastIndexOfOption(@Independent @NotModified T element, int end) { return null; }
        @NotModified int lastIndexOfSlice(@Independent @NotModified Iterable<? extends T> that) { return 0; }
        @NotModified int lastIndexOfSlice(@Independent @NotModified Iterable<? extends T> arg0, int arg1) { return 0; }
        @Independent @NotModified
        Option<Integer> lastIndexOfSliceOption(@Independent @NotModified Iterable<? extends T> that) { return null; }

        @Independent @NotModified
        Option<Integer> lastIndexOfSliceOption(@Independent @NotModified Iterable<? extends T> that, int end) {
            return null;
        }
        @NotModified int lastIndexWhere(@Independent @NotModified Predicate<? super T> predicate) { return 0; }
        @NotModified
        int lastIndexWhere(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0, int arg1) {
            return 0;
        }

        @Independent @NotModified
        Option<Integer> lastIndexWhereOption(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        Option<Integer> lastIndexWhereOption(@Independent @NotModified Predicate<? super T> predicate, int end) {
            return null;
        }
        @Independent @NotModified Seq<T> leftPadTo(int arg0, @Independent @NotModified T arg1) { return null; }
        //override from io.vavr.PartialFunction
        @Independent @NotModified
        Function1<Integer, Option<T>> lift() { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> map(@IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Seq<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> Seq<T> narrow(@Independent @NotModified Seq<? extends T> seq) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> orElse(@Independent(hc = true) @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Seq<T> orElse(@IgnoreModifications @Independent @NotModified Supplier<? extends Iterable<? extends T>> arg0) {
            return null;
        }
        @Independent @NotModified Seq<T> padTo(int arg0, @Independent @NotModified T arg1) { return null; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends Seq<T>, ? extends Seq<T>> partition(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        @Independent @NotModified
        Seq<T> patch(int arg0, @Independent(hc = true) @NotModified Iterable<? extends T> arg1, int arg2) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> peek(@IgnoreModifications @Independent @NotModified Consumer<? super T> arg0) { return null; }
        @Independent @NotModified Seq<? extends Seq<T>> permutations() { return null; }
        @NotModified int prefixLength(@Independent @NotModified Predicate<? super T> predicate) { return 0; }
        @Independent(hc = true) @NotModified Seq<T> prepend(@Independent @NotModified T arg0) { return null; }
        @Independent(hc = true) @NotModified
        Seq<T> prependAll(@Independent(hc = true) @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> reject(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }
        @Independent @NotModified Seq<T> remove(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified
        Seq<T> removeAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified Seq<T> removeAll(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified
        Seq<T> removeAll(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }
        @Independent @NotModified Seq<T> removeAt(int arg0) { return null; }
        @Independent @NotModified
        Seq<T> removeFirst(@IgnoreModifications @Independent @NotModified Predicate<T> arg0) { return null; }

        @Independent @NotModified
        Seq<T> removeLast(@IgnoreModifications @Independent @NotModified Predicate<T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> replace(@Independent @NotModified T arg0, @Independent(hc = true) @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> replaceAll(@Independent @NotModified T arg0, @Independent(hc = true) @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> retainAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified Seq<T> reverse() { return null; }
        @Independent @NotModified Iterator<T> reverseIterator() { return null; }
        @Independent @NotModified Seq<T> rotateLeft(int arg0) { return null; }
        @Independent @NotModified Seq<T> rotateRight(int arg0) { return null; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Seq<T> scan(
            @Independent @NotModified T arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> arg1) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> scanLeft(
            @Independent @NotModified U arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> arg1) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> scanRight(
            @Independent @NotModified U arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> arg1) {
            return null;
        }
        @NotModified int search(@Independent @NotModified T arg0) { return 0; }
        @NotModified
        int search(@Independent @NotModified T arg0, @Independent @NotModified Comparator<? super T> arg1) { return 0; }

        @NotModified
        int segmentLength(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0, int arg1) { return 0; }
        @Independent @NotModified Seq<T> shuffle() { return null; }
        @Independent @NotModified Seq<T> slice(int arg0, int arg1) { return null; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<? extends Seq<T>> slideBy(@IgnoreModifications @Independent @NotModified Function<? super T, ?> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends Seq<T>> sliding(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends Seq<T>> sliding(int arg0, int arg1) { return null; }

        @Independent @NotModified
        <U> Seq<T> sortBy(
            @Independent @NotModified Comparator<? super U> arg0,
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg1) { return null; }

        @Independent @NotModified
        <U extends Comparable<? super U>> Seq<T> sortBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) { return null; }
        @Independent(hc = true) @NotModified Seq<T> sorted() { return null; }
        @Independent(hc = true) @NotModified
        Seq<T> sorted(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends Seq<T>, ? extends Seq<T>> span(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }
        @Independent @NotModified Tuple2<? extends Seq<T>, ? extends Seq<T>> splitAt(int arg0) { return null; }
        @Independent @NotModified
        Tuple2<? extends Seq<T>, ? extends Seq<T>> splitAt(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        @Independent @NotModified
        Tuple2<? extends Seq<T>, ? extends Seq<T>> splitAtInclusive(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }
        @NotModified boolean startsWith(@Independent @NotModified Iterable<? extends T> that) { return false; }
        @NotModified
        boolean startsWith(@Independent @NotModified Iterable<? extends T> that, int offset) { return false; }
        @Independent(hc = true) @NotModified Seq<T> subSequence(int arg0) { return null; }
        @Independent @NotModified Seq<T> subSequence(int arg0, int arg1) { return null; }
        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Seq<T> tail() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends Seq<T>> tailOption() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> take(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> takeRight(int arg0) { return null; }

        @Independent @NotModified
        Seq<T> takeRightUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        @Independent @NotModified
        Seq<T> takeRightWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> takeUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> takeWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<? extends Seq<T1>, ? extends Seq<T2>> unzip(
            @IgnoreModifications @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2, T3> Tuple3<? extends Seq<T1>, ? extends Seq<T2>, ? extends Seq<T3>> unzip3(
            @IgnoreModifications @Independent @NotModified Function<
                ? super T,
                Tuple3<? extends T1, ? extends T2, ? extends T3>> arg0) { return null; }
        @Independent(hc = true) @NotModified Seq<T> update(int arg0, @Independent @NotModified T arg1) { return null; }
        @Independent(hc = true) @NotModified
        Seq<T> update(int arg0, @IgnoreModifications @Independent @NotModified Function<? super T, ? extends T> arg1) {
            return null;
        }

        @Independent @NotModified
        Function1<Integer, T> withDefault(
            @Independent @NotModified Function<? super Integer, ? extends T> defaultFunction) { return null; }

        @Independent @NotModified
        Function1<Integer, T> withDefaultValue(@Independent @NotModified T defaultValue) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Seq<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified U arg2) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> Seq<R> zipWith(
            @Independent @NotModified Iterable<? extends U> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> arg1) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> zipWithIndex(
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> arg0) {
            return null;
        }
    }

    //public interface Set implements Traversable<T>, Function1<T,Boolean>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: implemented by persistent collections only (VAVR.md)
    @ImmutableContainer(hc = true)
    class Set$<T> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        io.vavr.collection.Set<T> add(@Independent(hc = true) @NotModified T arg0) { return null; }

        @Independent @NotModified
        io.vavr.collection.Set<T> addAll(@Independent(hc = true) @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.Function1, java.util.function.Function
        @Independent @NotModified
        Boolean apply(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <R> io.vavr.collection.Set<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> arg0) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        boolean contains(@Independent(hc = true) @NotModified T arg0) { return false; }

        @Independent @NotModified
        io.vavr.collection.Set<T> diff(@Independent @NotModified io.vavr.collection.Set<? extends T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Set<T> distinct() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Set<T> distinctBy(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<T> distinctBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Set<T> drop(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Set<T> dropRight(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Set<T> dropUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Set<T> dropWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Set<T> filter(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<U> flatMap(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, ? extends io.vavr.collection.Set<T>> groupBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends C> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends io.vavr.collection.Set<T>> grouped(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Set<T> init() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Option<? extends io.vavr.collection.Set<T>> initOption() { return null; }

        @Independent @NotModified
        io.vavr.collection.Set<T> intersect(
            @Independent(hc = true) @NotModified io.vavr.collection.Set<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isDistinct() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent(hc = true) @NotModified @NotNull
        Iterator<T> iterator() { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        int length() { return 0; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<U> map(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> io.vavr.collection.Set<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Set<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> io.vavr.collection.Set<T> narrow(@Independent @NotModified io.vavr.collection.Set<? extends T> set) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Set<T> orElse(@Independent(hc = true) @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Set<T> orElse(
            @IgnoreModifications @Independent @NotModified Supplier<? extends Iterable<? extends T>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends io.vavr.collection.Set<T>, ? extends io.vavr.collection.Set<T>> partition(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Set<T> peek(@IgnoreModifications @Independent @NotModified Consumer<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Set<T> reject(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        @Independent @NotModified
        io.vavr.collection.Set<T> remove(@Independent(hc = true) @NotModified T arg0) { return null; }

        @Independent @NotModified
        io.vavr.collection.Set<T> removeAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Set<T> replace(
            @Independent(hc = true) @NotModified T arg0,
            @Independent(hc = true) @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Set<T> replaceAll(
            @Independent(hc = true) @NotModified T arg0,
            @Independent(hc = true) @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Set<T> retainAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Set<T> scan(
            @Independent @NotModified T arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> arg1) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<U> scanLeft(
            @Independent @NotModified U arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> arg1) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<U> scanRight(
            @Independent @NotModified U arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> arg1) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<? extends io.vavr.collection.Set<T>> slideBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ?> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends io.vavr.collection.Set<T>> sliding(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends io.vavr.collection.Set<T>> sliding(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        Tuple2<? extends io.vavr.collection.Set<T>, ? extends io.vavr.collection.Set<T>> span(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        io.vavr.collection.Set<T> tail() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Option<? extends io.vavr.collection.Set<T>> tailOption() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Set<T> take(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Set<T> takeRight(int arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Set<T> takeUntil(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Set<T> takeWhile(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.Value
        @Independent @NotModified
        Set<T> toJavaSet() { return null; }

        @Independent @NotModified
        io.vavr.collection.Set<T> union(@Independent(hc = true) @NotModified io.vavr.collection.Set<? extends T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <T1, T2> Tuple2<? extends io.vavr.collection.Set<T1>, ? extends io.vavr.collection.Set<T2>> unzip(
            @IgnoreModifications @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <T1, T2, T3> Tuple3<
            ? extends io.vavr.collection.Set<T1>,
            ? extends io.vavr.collection.Set<T2>,
            ? extends io.vavr.collection.Set<T3>> unzip3(
            @IgnoreModifications @Independent @NotModified Function<
                ? super T,
                Tuple3<? extends T1, ? extends T2, ? extends T3>> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> arg0) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified U arg2) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> io.vavr.collection.Set<R> zipWith(
            @Independent @NotModified Iterable<? extends U> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> arg1) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Set<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<U> zipWithIndex(
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> arg0) {
            return null;
        }
    }

    //public interface SortedMap implements Map<K,V>, Ordered<K>
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: implemented by persistent collections only (VAVR.md)
    @ImmutableContainer(hc = true)
    class SortedMap$<K, V> {
        static final long serialVersionUID = 0L;
        @Independent @NotModified
        <K2, V2> SortedMap<K2, V2> bimap(
            @Independent @NotModified Comparator<? super K2> arg0,
            @IgnoreModifications @Independent @NotModified Function<? super K, ? extends K2> arg1,
            @IgnoreModifications @Independent @NotModified Function<? super V, ? extends V2> arg2) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2, V2> SortedMap<K2, V2> bimap(
            @IgnoreModifications @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @IgnoreModifications @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        Tuple2<V, ? extends SortedMap<K, V>> computeIfAbsent(
            @Independent @NotModified K arg0,
            @IgnoreModifications @Independent @NotModified Function<? super K, ? extends V> arg1) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        Tuple2<Option<V>, ? extends SortedMap<K, V>> computeIfPresent(
            @Independent @NotModified K arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super K, ? super V, ? extends V> arg1) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        SortedMap<K, V> distinct() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        SortedMap<K, V> distinctBy(@Independent @NotModified Comparator<? super Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> SortedMap<K, V> distinctBy(
            @IgnoreModifications @Independent @NotModified Function<? super Tuple2<K, V>, ? extends U> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        SortedMap<K, V> drop(int arg0) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        SortedMap<K, V> dropRight(int arg0) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        SortedMap<K, V> dropUntil(@IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        SortedMap<K, V> dropWhile(@IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        SortedMap<K, V> filter(@IgnoreModifications @Independent @NotModified BiPredicate<? super K, ? super V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        SortedMap<K, V> filter(@IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        SortedMap<K, V> filterKeys(@IgnoreModifications @Independent @NotModified Predicate<? super K> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        SortedMap<K, V> filterValues(@IgnoreModifications @Independent @NotModified Predicate<? super V> arg0) {
            return null;
        }

        @Independent @NotModified
        <K2, V2> SortedMap<K2, V2> flatMap(
            @Independent @NotModified Comparator<? super K2> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<
                ? super K,
                ? super V,
                ? extends Iterable<Tuple2<K2, V2>>> arg1) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2, V2> SortedMap<K2, V2> flatMap(
            @IgnoreModifications @Independent @NotModified BiFunction<
                ? super K,
                ? super V,
                ? extends Iterable<Tuple2<K2, V2>>> arg0) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, ? extends SortedMap<K, V>> groupBy(
            @IgnoreModifications @Independent @NotModified Function<? super Tuple2<K, V>, ? extends C> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends SortedMap<K, V>> grouped(int arg0) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        SortedMap<K, V> init() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends SortedMap<K, V>> initOption() { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isOrdered() { return false; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        io.vavr.collection.SortedSet<K> keySet() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<K, V> last() { return null; }

        @Independent @NotModified
        <K2, V2> SortedMap<K2, V2> map(
            @Independent @NotModified Comparator<? super K2> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super K, ? super V, Tuple2<K2, V2>> arg1) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2, V2> SortedMap<K2, V2> map(
            @IgnoreModifications @Independent @NotModified BiFunction<? super K, ? super V, Tuple2<K2, V2>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2> SortedMap<K2, V> mapKeys(
            @IgnoreModifications @Independent @NotModified Function<? super K, ? extends K2> arg0) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <K2> SortedMap<K2, V> mapKeys(
            @IgnoreModifications @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super V, ? super V, ? extends V> arg1) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <V2> SortedMap<K, V2> mapValues(
            @IgnoreModifications @Independent @NotModified Function<? super V, ? extends V2> arg0) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        SortedMap<K, V> merge(@Independent @NotModified io.vavr.collection.Map<? extends K, ? extends V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <U extends V> SortedMap<K, V> merge(
            @Independent @NotModified io.vavr.collection.Map<? extends K, U> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> arg1) {
            return null;
        }

        @Identity @Independent @NotModified
        static <K, V> SortedMap<K, V> narrow(@Independent @NotModified SortedMap<? extends K, ? extends V> sortedMap) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        SortedMap<K, V> orElse(@Independent @NotModified Iterable<? extends Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        SortedMap<K, V> orElse(
            @IgnoreModifications @Independent @NotModified Supplier<? extends Iterable<? extends Tuple2<K, V>>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends SortedMap<K, V>, ? extends SortedMap<K, V>> partition(
            @IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        SortedMap<K, V> peek(@IgnoreModifications @Independent @NotModified Consumer<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent(hc = true) @NotModified
        SortedMap<K, V> put(@Independent @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <U extends V> SortedMap<K, V> put(
            @Independent @NotModified K arg0,
            @Independent @NotModified U arg1,
            @IgnoreModifications @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> arg2) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        SortedMap<K, V> put(@Independent @NotModified Tuple2<? extends K, ? extends V> arg0) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        <U extends V> SortedMap<K, V> put(
            @Independent @NotModified Tuple2<? extends K, U> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> arg1) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        SortedMap<K, V> reject(@IgnoreModifications @Independent @NotModified BiPredicate<? super K, ? super V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        SortedMap<K, V> reject(@IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        SortedMap<K, V> rejectKeys(@IgnoreModifications @Independent @NotModified Predicate<? super K> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        SortedMap<K, V> rejectValues(@IgnoreModifications @Independent @NotModified Predicate<? super V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        SortedMap<K, V> remove(@Independent @NotModified K arg0) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        SortedMap<K, V> removeAll(@Independent @NotModified Iterable<? extends K> arg0) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        SortedMap<K, V> removeAll(@IgnoreModifications @Independent @NotModified BiPredicate<? super K, ? super V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        SortedMap<K, V> removeKeys(@IgnoreModifications @Independent @NotModified Predicate<? super K> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        SortedMap<K, V> removeValues(@IgnoreModifications @Independent @NotModified Predicate<? super V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        SortedMap<K, V> replace(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified V arg2) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        SortedMap<K, V> replace(
            @Independent @NotModified Tuple2<K, V> arg0,
            @Independent @NotModified Tuple2<K, V> arg1) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        SortedMap<K, V> replaceAll(
            @Independent @NotModified Tuple2<K, V> arg0,
            @Independent @NotModified Tuple2<K, V> arg1) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified

        SortedMap<K, V> replaceAll(
            @IgnoreModifications @Independent @NotModified BiFunction<? super K, ? super V, ? extends V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        SortedMap<K, V> replaceValue(@Independent @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        SortedMap<K, V> retainAll(@Independent @NotModified Iterable<? extends Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        SortedMap<K, V> scan(
            @Independent @NotModified Tuple2<K, V> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<
                ? super Tuple2<K, V>,
                ? super Tuple2<K, V>,
                ? extends Tuple2<K, V>> arg1) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<? extends SortedMap<K, V>> slideBy(
            @IgnoreModifications @Independent @NotModified Function<? super Tuple2<K, V>, ?> arg0) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends SortedMap<K, V>> sliding(int arg0) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends SortedMap<K, V>> sliding(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends SortedMap<K, V>, ? extends SortedMap<K, V>> span(
            @IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        SortedMap<K, V> tail() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends SortedMap<K, V>> tailOption() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        SortedMap<K, V> take(int arg0) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified
        SortedMap<K, V> takeRight(int arg0) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        SortedMap<K, V> takeUntil(@IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @Independent @NotModified

        SortedMap<K, V> takeWhile(@IgnoreModifications @Independent @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        java.util.SortedMap<K, V> toJavaMap() { return null; }
    }

    //public interface SortedMultimap implements Multimap<K,V>, Ordered<K>
    //annotated as EXPECTED; computed @FinalFields @Container @Dependent -- G1, G2: implemented by persistent collections only (VAVR.md)
    @ImmutableContainer(hc = true)
    class SortedMultimap$<K, V> {
        static final long serialVersionUID = 0L;
        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        SortedMultimap<K, V> distinct() { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> distinctBy(@Independent(hc = true) @NotModified Comparator<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <U> SortedMultimap<K, V> distinctBy(
            @IgnoreModifications @Independent(hc = true) @NotModified Function<? super Tuple2<K, V>, ? extends U> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        SortedMultimap<K, V> drop(int arg0) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        SortedMultimap<K, V> dropRight(int arg0) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> dropUntil(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> dropWhile(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> filter(
            @IgnoreModifications @Independent(hc = true) @NotModified BiPredicate<? super K, ? super V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> filter(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> filterKeys(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super K> arg0) { return null; }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> filterValues(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super V> arg0) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <C> io.vavr.collection.Map<C, ? extends SortedMultimap<K, V>> groupBy(
            @IgnoreModifications @Independent(hc = true) @NotModified Function<? super Tuple2<K, V>, ? extends C> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Iterator<? extends SortedMultimap<K, V>> grouped(int arg0) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        SortedMultimap<K, V> init() { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Option<? extends SortedMultimap<K, V>> initOption() { return null; }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified
        io.vavr.collection.SortedSet<K> keySet() { return null; }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> merge(@Independent(hc = true) @NotModified Multimap<? extends K, ? extends V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        <K2 extends K, V2 extends V> SortedMultimap<K, V> merge(
            @Independent(hc = true) @NotModified Multimap<K2, V2> arg0,
            @IgnoreModifications @Independent(hc = true) @NotModified BiFunction<
                Traversable<V>,
                Traversable<V2>,
                Traversable<V>> arg1) { return null; }

        @Identity @Independent @NotModified
        static <K, V> SortedMultimap<K, V> narrow(
            @Independent @NotModified SortedMultimap<? extends K, ? extends V> map) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> orElse(@Independent(hc = true) @NotModified Iterable<? extends Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> orElse(
            @IgnoreModifications @Independent(hc = true) @NotModified Supplier<? extends Iterable<? extends Tuple2<K, V>>>
                arg0) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        Tuple2<? extends SortedMultimap<K, V>, ? extends SortedMultimap<K, V>> partition(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> peek(
            @IgnoreModifications @Independent(hc = true) @NotModified Consumer<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> put(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified V arg1) { return null; }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> put(@Independent(hc = true) @NotModified Tuple2<? extends K, ? extends V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> reject(
            @IgnoreModifications @Independent(hc = true) @NotModified BiPredicate<? super K, ? super V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> reject(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> rejectKeys(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super K> arg0) { return null; }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> rejectValues(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super V> arg0) { return null; }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified
        SortedMultimap<K, V> remove(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> remove(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified V arg1) { return null; }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified
        SortedMultimap<K, V> removeAll(@Independent(hc = true) @NotModified Iterable<? extends K> arg0) { return null; }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> removeAll(
            @IgnoreModifications @Independent(hc = true) @NotModified BiPredicate<? super K, ? super V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> removeKeys(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super K> arg0) { return null; }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> removeValues(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super V> arg0) { return null; }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> replace(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified V arg1,
            @Independent(hc = true) @NotModified V arg2) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> replace(
            @Independent(hc = true) @NotModified Tuple2<K, V> arg0,
            @Independent(hc = true) @NotModified Tuple2<K, V> arg1) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> replaceAll(
            @Independent(hc = true) @NotModified Tuple2<K, V> arg0,
            @Independent(hc = true) @NotModified Tuple2<K, V> arg1) { return null; }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> replaceAll(
            @IgnoreModifications @Independent(hc = true) @NotModified BiFunction<? super K, ? super V, ? extends V> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> replaceValue(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified V arg1) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> retainAll(@Independent(hc = true) @NotModified Iterable<? extends Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> scan(
            @Independent(hc = true) @NotModified Tuple2<K, V> arg0,
            @IgnoreModifications @Independent(hc = true) @NotModified BiFunction<
                ? super Tuple2<K, V>,
                ? super Tuple2<K, V>,
                ? extends Tuple2<K, V>> arg1) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        Iterator<? extends SortedMultimap<K, V>> slideBy(
            @IgnoreModifications @Independent(hc = true) @NotModified Function<? super Tuple2<K, V>, ?> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Iterator<? extends SortedMultimap<K, V>> sliding(int arg0) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Iterator<? extends SortedMultimap<K, V>> sliding(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        Tuple2<? extends SortedMultimap<K, V>, ? extends SortedMultimap<K, V>> span(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        SortedMultimap<K, V> tail() { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Option<? extends SortedMultimap<K, V>> tailOption() { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        SortedMultimap<K, V> take(int arg0) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        SortedMultimap<K, V> takeRight(int arg0) { return null; }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> takeUntil(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        SortedMultimap<K, V> takeWhile(
            @IgnoreModifications @Independent(hc = true) @NotModified Predicate<? super Tuple2<K, V>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Multimap
        @Independent @NotModified
        java.util.SortedMap<K, Collection<V>> toJavaMap() { return null; }
    }

    //public interface SortedSet implements Set<T>, Ordered<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: implemented by persistent collections only (VAVR.md)
    @ImmutableContainer(hc = true)
    class SortedSet$<T> {
        static final long serialVersionUID = 0L;
        //override from io.vavr.collection.Set
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> add(@Independent(hc = true) @NotModified T arg0) { return null; }

        //override from io.vavr.collection.Set
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> addAll(@Independent(hc = true) @NotModified Iterable<? extends T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <R> io.vavr.collection.SortedSet<R> collect(
            @Independent @NotModified PartialFunction<? super T, ? extends R> arg0) { return null; }

        //override from io.vavr.collection.Set
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> diff(@Independent @NotModified io.vavr.collection.Set<? extends T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> distinct() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> distinctBy(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.SortedSet<T> distinctBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> drop(int arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> dropRight(int arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> dropUntil(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> dropWhile(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> filter(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        @Independent @NotModified
        <U> io.vavr.collection.SortedSet<U> flatMap(
            @Independent @NotModified Comparator<? super U> arg0,
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> arg1) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.SortedSet<U> flatMap(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, ? extends io.vavr.collection.SortedSet<T>> groupBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends C> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends io.vavr.collection.SortedSet<T>> grouped(int arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> init() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends io.vavr.collection.SortedSet<T>> initOption() { return null; }

        //override from io.vavr.collection.Set
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> intersect(
            @Independent(hc = true) @NotModified io.vavr.collection.Set<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isOrdered() { return false; }

        @Independent @NotModified
        <U> io.vavr.collection.SortedSet<U> map(
            @Independent @NotModified Comparator<? super U> arg0,
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg1) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.SortedSet<U> map(
            @IgnoreModifications @Independent @NotModified Function<? super T, ? extends U> arg0) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> io.vavr.collection.SortedSet<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> io.vavr.collection.SortedSet<T> narrow(
            @Independent @NotModified io.vavr.collection.SortedSet<? extends T> sortedSet) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> orElse(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> orElse(
            @IgnoreModifications @Independent @NotModified Supplier<? extends Iterable<? extends T>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<? extends io.vavr.collection.SortedSet<T>, ? extends io.vavr.collection.SortedSet<T>> partition(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> peek(@IgnoreModifications @Independent @NotModified Consumer<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> reject(@IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from io.vavr.collection.Set
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> remove(@Independent @NotModified T arg0) { return null; }

        //override from io.vavr.collection.Set
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> removeAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> replace(
            @Independent(hc = true) @NotModified T arg0,
            @Independent @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> replaceAll(
            @Independent(hc = true) @NotModified T arg0,
            @Independent @NotModified T arg1) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> retainAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> scan(
            @Independent @NotModified T arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> arg1) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<U> scanLeft(
            @Independent @NotModified U arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> arg1) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<U> scanRight(
            @Independent @NotModified U arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> arg1) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<? extends io.vavr.collection.SortedSet<T>> slideBy(
            @IgnoreModifications @Independent @NotModified Function<? super T, ?> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends io.vavr.collection.SortedSet<T>> sliding(int arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<? extends io.vavr.collection.SortedSet<T>> sliding(int arg0, int arg1) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        Tuple2<? extends io.vavr.collection.SortedSet<T>, ? extends io.vavr.collection.SortedSet<T>> span(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> tail() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<? extends io.vavr.collection.SortedSet<T>> tailOption() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> take(int arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<T> takeRight(int arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> takeUntil(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> takeWhile(
            @IgnoreModifications @Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set
        @Independent @NotModified
        SortedSet<T> toJavaSet() { return null; }

        //override from io.vavr.collection.Set
        @Independent @NotModified

        io.vavr.collection.SortedSet<T> union(
            @Independent(hc = true) @NotModified io.vavr.collection.Set<? extends T> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <T1, T2> Tuple2<? extends io.vavr.collection.SortedSet<T1>, ? extends io.vavr.collection.SortedSet<T2>> unzip(
            @IgnoreModifications @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <T1, T2, T3> Tuple3<
            ? extends io.vavr.collection.SortedSet<T1>,
            ? extends io.vavr.collection.SortedSet<T2>,
            ? extends io.vavr.collection.SortedSet<T3>> unzip3(
            @IgnoreModifications @Independent @NotModified Function<
                ? super T,
                Tuple3<? extends T1, ? extends T2, ? extends T3>> arg0) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.SortedSet<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> arg0) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.SortedSet<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified U arg2) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> io.vavr.collection.SortedSet<R> zipWith(
            @Independent @NotModified Iterable<? extends U> arg0,
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> arg1) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.SortedSet<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.SortedSet<U> zipWithIndex(
            @IgnoreModifications @Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> arg0) {
            return null;
        }
    }

    //public interface Stream implements LinearSeq<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G3, G2: persistent, lazily evaluated (memoizing tail) (VAVR.md)
    @ImmutableContainer(hc = true)
    class Stream$<T> {
        static final long serialVersionUID = 0L;
        //abstract static class Cons implements Stream<T>
        //annotated as EXPECTED; computed @FinalFields @Independent -- G1, G3: persistent, lazily evaluated (memoizing tail) (VAVR.md)
        @ImmutableContainer(hc = true)
        class Cons<T> {
            //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object o) { return false; }

            //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.collection.Traversable
            @Independent(hc = true) @NotModified @GetSet("head")
            T head() { return null; }

            //override from io.vavr.Value, io.vavr.collection.Traversable
            @NotModified
            boolean isEmpty() { return false; }

            //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Iterable
            @Independent @NotModified
            Iterator<T> iterator() { return null; }

            //override from io.vavr.Value, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        //static final class Empty implements Stream<T>, Serializable
        //annotated as EXPECTED; computed @FinalFields @Container @Dependent -- G1, G3, G2: persistent, lazily evaluated (memoizing tail) (VAVR.md)
        @ImmutableContainer(hc = true)
        class Empty<T> {
            //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object o) { return false; }

            //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.collection.Traversable
            @Independent @NotModified
            T head() { return null; }
            @Independent @NotModified static <T> io.vavr.collection.Stream.Empty<T> instance() { return null; }
            //override from io.vavr.Value, io.vavr.collection.Traversable
            @NotModified
            boolean isEmpty() { return false; }

            //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Iterable
            @Independent @NotModified
            Iterator<T> iterator() { return null; }

            //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Stream, io.vavr.collection.Traversable
            @Independent @NotModified
            io.vavr.collection.Stream<T> tail() { return null; }

            //override from io.vavr.Value, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        io.vavr.collection.Stream<T> append(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> appendAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> appendSelf(
            @Independent @NotModified Function<
                ? super io.vavr.collection.Stream<T>,
                ? extends io.vavr.collection.Stream<T>> mapper) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        java.util.List<T> asJava() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        io.vavr.collection.Stream<T> asJava(@Independent @NotModified Consumer<? super java.util.List<T>> action) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        java.util.List<T> asJavaMutable() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        io.vavr.collection.Stream<T> asJavaMutable(@Independent @NotModified Consumer<? super java.util.List<T>> action) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <R> io.vavr.collection.Stream<R> collect(
            @Independent @NotModified PartialFunction<? super T, ? extends R> partialFunction) { return null; }

        @Independent @NotModified
        static <T> Collector<T, ArrayList<T>, io.vavr.collection.Stream<T>> collector() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        io.vavr.collection.Stream<io.vavr.collection.Stream<T>> combinations() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        io.vavr.collection.Stream<io.vavr.collection.Stream<T>> combinations(int k) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> concat(
            @Independent @NotModified Iterable<? extends Iterable<? extends T>> iterables) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> concat(@Independent @NotModified Iterable<? extends T> ... iterables) {
            return null;
        }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> cons(
            @Independent @NotModified T head,
            @Independent @NotModified Supplier<? extends io.vavr.collection.Stream<? extends T>> tailSupplier) {
            return null;
        }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> continually(@Independent @NotModified T t) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> continually(@Independent @NotModified Supplier<? extends T> supplier) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Iterator<io.vavr.collection.Stream<T>> crossProduct(int power) { return null; }
        @Fluent @Independent @NotModified io.vavr.collection.Stream<T> cycle() { return null; }
        @Independent @NotModified io.vavr.collection.Stream<T> cycle(int count) { return null; }
        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> distinct() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.Stream<T> distinctBy(@Independent @NotModified Comparator<? super T> comparator) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        <U> io.vavr.collection.Stream<T> distinctBy(
            @Independent @NotModified Function<? super T, ? extends U> keyExtractor) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        io.vavr.collection.Stream<T> distinctByKeepLast(@Independent @NotModified Comparator<? super T> comparator) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U> io.vavr.collection.Stream<T> distinctByKeepLast(
            @Independent @NotModified Function<? super T, ? extends U> keyExtractor) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Stream<T> drop(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> dropRight(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        io.vavr.collection.Stream<T> dropRightUntil(@Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        io.vavr.collection.Stream<T> dropRightWhile(@Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Stream<T> dropUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Stream<T> dropWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }
        @Independent @NotModified static <T> io.vavr.collection.Stream<T> empty() { return null; }
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> extend(@Independent @NotModified T next) { return null; }

        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> extend(@Independent @NotModified Function<? super T, ? extends T> nextFunction) {
            return null;
        }

        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> extend(@Independent @NotModified Supplier<? extends T> nextSupplier) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> fill(int n, @Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> fill(int n, @Independent @NotModified Supplier<? extends T> s) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> filter(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Stream<U> flatMap(
            @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) { return null; }
        @Independent @NotModified static io.vavr.collection.Stream<Integer> from(int value) { return null; }
        @Independent @NotModified static io.vavr.collection.Stream<Integer> from(int value, int step) { return null; }
        @Independent @NotModified static io.vavr.collection.Stream<Long> from(long value) { return null; }
        @Independent @NotModified static io.vavr.collection.Stream<Long> from(long value, long step) { return null; }
        //override from io.vavr.collection.Seq
        @Independent @NotModified
        T get(int index) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, io.vavr.collection.Stream<T>> groupBy(
            @Independent @NotModified Function<? super T, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<io.vavr.collection.Stream<T>> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.collection.Seq
        @NotModified
        int indexOf(@Independent @NotModified T element, int from) { return 0; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Stream<T> init() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<io.vavr.collection.Stream<T>> initOption() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        io.vavr.collection.Stream<T> insert(int index, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        io.vavr.collection.Stream<T> insertAll(int index, @Independent @NotModified Iterable<? extends T> elements) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> intersperse(@Independent @NotModified T element) { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> iterate(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, ? extends T> f) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> iterate(
            @Independent @NotModified Supplier<? extends Option<? extends T>> supplier) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T last() { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int lastIndexOf(@Independent @NotModified T element, int end) { return 0; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> leftPadTo(int length, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        int length() { return 0; }

        //override from io.vavr.Value, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Stream<U> map(@Independent @NotModified Function<? super T, ? extends U> mapper) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> io.vavr.collection.Stream<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Stream<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> narrow(
            @Independent @NotModified io.vavr.collection.Stream<? extends T> stream) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> of(@Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> of(@Independent @NotModified T ... elements) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> ofAll(@Independent @NotModified Iterable<? extends T> elements) {
            return null;
        }

        @Independent @NotModified
        static io.vavr.collection.Stream<Boolean> ofAll(@Independent @NotModified boolean ... elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Byte> ofAll(@Independent @NotModified byte ... elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Character> ofAll(@Independent @NotModified char ... elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Double> ofAll(@Independent @NotModified double ... elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Float> ofAll(@Independent @NotModified float ... elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Integer> ofAll(@Independent @NotModified int ... elements) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> ofAll(@Independent @NotModified Stream<? extends T> javaStream) {
            return null;
        }

        @Independent @NotModified
        static io.vavr.collection.Stream<Long> ofAll(@Independent @NotModified long ... elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Short> ofAll(@Independent @NotModified short ... elements) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> orElse(@Independent @NotModified Iterable<? extends T> other) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.Stream<T> orElse(
            @Independent @NotModified Supplier<? extends Iterable<? extends T>> supplier) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> padTo(int length, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        Tuple2<io.vavr.collection.Stream<T>, io.vavr.collection.Stream<T>> partition(
            @Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        io.vavr.collection.Stream<T> patch(int from, @Independent @NotModified Iterable<? extends T> that, int replaced) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> peek(@Independent @NotModified Consumer<? super T> action) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        io.vavr.collection.Stream<io.vavr.collection.Stream<T>> permutations() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        io.vavr.collection.Stream<T> prepend(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent(hc = true) @NotModified
        io.vavr.collection.Stream<T> prependAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Character> range(char from, char toExclusive) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Integer> range(int from, int toExclusive) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Long> range(long from, long toExclusive) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Character> rangeBy(char from, char toExclusive, int step) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Double> rangeBy(double from, double toExclusive, double step) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Integer> rangeBy(int from, int toExclusive, int step) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Long> rangeBy(long from, long toExclusive, long step) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Character> rangeClosed(char from, char toInclusive) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Integer> rangeClosed(int from, int toInclusive) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Long> rangeClosed(long from, long toInclusive) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Character> rangeClosedBy(char from, char toInclusive, int step) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Double> rangeClosedBy(double from, double toInclusive, double step) {
            return null;
        }

        @Independent @NotModified
        static io.vavr.collection.Stream<Integer> rangeClosedBy(int from, int toInclusive, int step) { return null; }

        @Independent @NotModified
        static io.vavr.collection.Stream<Long> rangeClosedBy(long from, long toInclusive, long step) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> reject(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> remove(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> removeAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> removeAll(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> removeAll(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        io.vavr.collection.Stream<T> removeAt(int index) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> removeFirst(@Independent @NotModified Predicate<T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> removeLast(@Independent @NotModified Predicate<T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.Stream<T> replace(
            @Independent @NotModified T currentElement,
            @Independent @NotModified T newElement) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        io.vavr.collection.Stream<T> replaceAll(
            @Independent @NotModified T currentElement,
            @Independent @NotModified T newElement) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> retainAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> reverse() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> rotateLeft(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> rotateRight(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        io.vavr.collection.Stream<T> scan(
            @Independent @NotModified T zero,
            @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> operation) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Stream<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> operation) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Stream<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> shuffle() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        io.vavr.collection.Stream<T> slice(int beginIndex, int endIndex) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<io.vavr.collection.Stream<T>> slideBy(@Independent @NotModified Function<? super T, ?> classifier) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<io.vavr.collection.Stream<T>> sliding(int size) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<io.vavr.collection.Stream<T>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U> io.vavr.collection.Stream<T> sortBy(
            @Independent @NotModified Comparator<? super U> comparator,
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U extends Comparable<? super U>> io.vavr.collection.Stream<T> sortBy(
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> sorted() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        io.vavr.collection.Stream<T> sorted(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<io.vavr.collection.Stream<T>, io.vavr.collection.Stream<T>> span(
            @Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        Tuple2<io.vavr.collection.Stream<T>, io.vavr.collection.Stream<T>> splitAt(int n) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        Tuple2<io.vavr.collection.Stream<T>, io.vavr.collection.Stream<T>> splitAt(
            @Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified

        Tuple2<io.vavr.collection.Stream<T>, io.vavr.collection.Stream<T>> splitAtInclusive(
            @Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        io.vavr.collection.Stream<T> subSequence(int beginIndex) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        io.vavr.collection.Stream<T> subSequence(int beginIndex, int endIndex) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> tabulate(
            int n,
            @Independent @NotModified Function<? super Integer, ? extends T> f) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        io.vavr.collection.Stream<T> tail() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<io.vavr.collection.Stream<T>> tailOption() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Stream<T> take(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Stream<T> takeRight(int n) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        io.vavr.collection.Stream<T> takeRightUntil(@Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        io.vavr.collection.Stream<T> takeRightWhile(@Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Stream<T> takeUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Stream<T> takeWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        <U> U transform(@Independent @NotModified Function<? super io.vavr.collection.Stream<T>, ? extends U> f) {
            return null;
        }

        @Identity @Independent @NotModified
        static <T> io.vavr.collection.Stream<io.vavr.collection.Stream<T>> transpose(
            @Independent @NotModified io.vavr.collection.Stream<io.vavr.collection.Stream<T>> matrix) { return null; }

        @Independent @NotModified
        static <T> io.vavr.collection.Stream<T> unfold(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends T>>> f) { return null; }

        @Independent @NotModified
        static <T, U> io.vavr.collection.Stream<U> unfoldLeft(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends U>>> f) { return null; }

        @Independent @NotModified
        static <T, U> io.vavr.collection.Stream<U> unfoldRight(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends U, ? extends T>>> f) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<io.vavr.collection.Stream<T1>, io.vavr.collection.Stream<T2>> unzip(
            @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) { return null; }

        //override from io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2, T3> Tuple3<io.vavr.collection.Stream<T1>, io.vavr.collection.Stream<T2>, io.vavr.collection.Stream<T3>>
            unzip3(
            @Independent @NotModified Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified
        io.vavr.collection.Stream<T> update(int index, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq
        @Independent @NotModified

        io.vavr.collection.Stream<T> update(
            int index,
            @Independent @NotModified Function<? super T, ? extends T> updater) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Stream<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> that) {
            return null;
        }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Stream<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> iterable,
            @Independent @NotModified T thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> io.vavr.collection.Stream<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        io.vavr.collection.Stream<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.LinearSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Stream<U> zipWithIndex(
            @Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> mapper) { return null; }
    }

    //public interface Traversable implements Foldable<T>, Value<T>
    //EXPECTED no immutability claim -- computed @FinalFields @Dependent, annotated -- root of G1 (computed is right): also implemented by Iterator, Future, Lazy: stateful (VAVR.md)
    @FinalFields
    @Independent(absent = true)
    class Traversable$<T> {
        @Independent @Modified
        <K> Option<io.vavr.collection.Map<K, T>> arrangeBy(
            @Independent @Modified Function<? super T, ? extends K> getKey) { return null; }
        @Independent @NotModified Option<Double> average() { return null; }
        @Independent @Modified
        <R> Traversable<R> collect(@Independent @Modified PartialFunction<? super T, ? extends R> arg0) { return null; }
        @NotModified boolean containsAll(@Independent @NotModified Iterable<? extends T> elements) { return false; }
        @NotModified int count(@Independent @Modified Predicate<? super T> predicate) { return 0; }
        @Independent @Modified Traversable<T> distinct() { return null; }
        @Independent @Modified
        Traversable<T> distinctBy(@Independent @Modified Comparator<? super T> arg0) { return null; }

        @Independent @Modified
        <U> Traversable<T> distinctBy(@IgnoreModifications @Independent @Modified Function<? super T, ? extends U> arg0) {
            return null;
        }
        @Independent(absent = true) @Modified Traversable<T> drop(int arg0) { return null; }
        @Independent(absent = true) @Modified Traversable<T> dropRight(int arg0) { return null; }
        @Independent @Modified
        Traversable<T> dropUntil(@IgnoreModifications @Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent @Modified
        Traversable<T> dropWhile(@IgnoreModifications @Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object arg0) { return false; }
        @NotModified boolean existsUnique(@Independent @Modified Predicate<? super T> predicate) { return false; }
        @Independent @Modified
        Traversable<T> filter(@IgnoreModifications @Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent @NotModified
        Option<T> find(@Independent @Modified Predicate<? super T> predicate) { return null; }

        @Independent @NotModified
        Option<T> findLast(@Independent @Modified Predicate<? super T> predicate) { return null; }

        @Independent @Modified
        <U> Traversable<U> flatMap(
            @IgnoreModifications @Independent @Modified Function<? super T, ? extends Iterable<? extends U>> arg0) {
            return null;
        }

        //override from io.vavr.collection.Foldable
        @Independent @NotModified

        <U> U foldLeft(
            @Independent @Modified U zero,
            @Independent @Modified BiFunction<? super U, ? super T, ? extends U> f) { return null; }

        //override from io.vavr.collection.Foldable
        @Independent(hc = true) @Modified

        <U> U foldRight(
            @Independent(hc = true) @Modified U arg0,
            @IgnoreModifications @Independent @Modified BiFunction<? super T, ? super U, ? extends U> arg1) {
            return null;
        }
        @NotModified void forEachWithIndex(@Independent @Modified ObjIntConsumer<? super T> action) { }
        //override from io.vavr.Value
        @Independent @Modified
        T get() { return null; }

        @Independent @Modified
        <C> io.vavr.collection.Map<C, ? extends Traversable<T>> groupBy(
            @IgnoreModifications @Independent @Modified Function<? super T, ? extends C> arg0) { return null; }
        @Independent @Modified Iterator<? extends Traversable<T>> grouped(int arg0) { return null; }
        @NotModified boolean hasDefiniteSize() { return false; }
        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }
        @Independent(hc = true) @Modified T head() { return null; }
        @Independent @Modified Option<T> headOption() { return null; }
        @Independent(absent = true) @Modified Traversable<T> init() { return null; }
        @Independent @Modified Option<? extends Traversable<T>> initOption() { return null; }
        @NotModified boolean isDistinct() { return false; }
        //override from io.vavr.Value
        @NotModified
        boolean isEmpty() { return false; }
        @NotModified boolean isOrdered() { return false; }
        @NotModified boolean isSequential() { return false; }
        //override from io.vavr.Value
        @NotModified
        boolean isSingleValued() { return false; }
        @NotModified boolean isTraversableAgain() { return false; }
        //override from io.vavr.Value, java.lang.Iterable
        @Independent @NotModified
        Iterator<T> iterator() { return null; }
        @Independent(hc = true) @Modified T last() { return null; }
        @Independent @Modified Option<T> lastOption() { return null; }
        @NotModified int length() { return 0; }
        //override from io.vavr.Value
        @Independent @Modified

        <U> Traversable<U> map(@IgnoreModifications @Independent @Modified Function<? super T, ? extends U> arg0) {
            return null;
        }

        //override from io.vavr.Value
        @Independent @Modified
        <U> Traversable<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value
        @Independent @Modified
        Traversable<Void> mapToVoid() { return null; }
        @Independent @Modified Option<T> max() { return null; }
        @Independent @Modified
        Option<T> maxBy(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        @Independent @NotModified
        <U extends Comparable<? super U>> Option<T> maxBy(@Independent @Modified Function<? super T, ? extends U> f) {
            return null;
        }
        @Independent @Modified Option<T> min() { return null; }
        @Independent @Modified
        Option<T> minBy(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        @Independent @NotModified
        <U extends Comparable<? super U>> Option<T> minBy(@Independent @Modified Function<? super T, ? extends U> f) {
            return null;
        }
        @Independent @NotModified CharSeq mkCharSeq() { return null; }
        @Independent @NotModified CharSeq mkCharSeq(@Independent @NotModified CharSequence delimiter) { return null; }
        @Independent @NotModified
        CharSeq mkCharSeq(
            @Independent @NotModified CharSequence prefix,
            @Independent @NotModified CharSequence delimiter,
            @Independent @NotModified CharSequence suffix) { return null; }
        @NotModified String mkString() { return null; }
        @NotModified String mkString(@Independent @NotModified CharSequence delimiter) { return null; }
        @NotModified
        String mkString(
            @Independent @NotModified CharSequence prefix,
            @Independent @NotModified CharSequence delimiter,
            @Independent @NotModified CharSequence suffix) { return null; }

        @Identity @Independent @NotModified
        static <T> Traversable<T> narrow(@Independent @NotModified Traversable<? extends T> traversable) { return null; }
        @NotModified boolean nonEmpty() { return false; }
        @Independent @Modified
        Traversable<T> orElse(@Independent(absent = true) @Modified Iterable<? extends T> arg0) { return null; }

        @Independent @Modified
        Traversable<T> orElse(
            @IgnoreModifications @Independent @Modified Supplier<? extends Iterable<? extends T>> arg0) { return null; }

        @Independent(hc = true) @Modified
        Tuple2<? extends Traversable<T>, ? extends Traversable<T>> partition(
            @IgnoreModifications @Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.Value
        @Independent @Modified
        Traversable<T> peek(@IgnoreModifications @Independent @Modified Consumer<? super T> arg0) { return null; }
        @Independent @NotModified Number product() { return null; }
        //override from io.vavr.collection.Foldable
        @Independent @NotModified
        T reduceLeft(@Independent @Modified BiFunction<? super T, ? super T, ? extends T> op) { return null; }

        //override from io.vavr.collection.Foldable
        @Independent @NotModified

        Option<T> reduceLeftOption(@Independent @Modified BiFunction<? super T, ? super T, ? extends T> op) {
            return null;
        }

        //override from io.vavr.collection.Foldable
        @Independent @NotModified
        T reduceRight(@Independent @Modified BiFunction<? super T, ? super T, ? extends T> op) { return null; }

        //override from io.vavr.collection.Foldable
        @Independent @NotModified

        Option<T> reduceRightOption(@Independent @Modified BiFunction<? super T, ? super T, ? extends T> op) {
            return null;
        }

        @Independent @Modified
        Traversable<T> reject(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        @Independent(hc = true) @Modified
        Traversable<T> replace(@Independent(hc = true) @Modified T arg0, @Independent(hc = true) @Modified T arg1) {
            return null;
        }

        @Independent(hc = true) @Modified
        Traversable<T> replaceAll(@Independent(hc = true) @Modified T arg0, @Independent(hc = true) @Modified T arg1) {
            return null;
        }

        @Independent @Modified
        Traversable<T> retainAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        @Independent @Modified
        Traversable<T> scan(
            @Independent @NotModified T arg0,
            @IgnoreModifications @Independent @Modified BiFunction<? super T, ? super T, ? extends T> arg1) {
            return null;
        }

        @Independent @Modified
        <U> Traversable<U> scanLeft(
            @Independent @NotModified U arg0,
            @IgnoreModifications @Independent @Modified BiFunction<? super U, ? super T, ? extends U> arg1) {
            return null;
        }

        @Independent @Modified
        <U> Traversable<U> scanRight(
            @Independent @NotModified U arg0,
            @IgnoreModifications @Independent @Modified BiFunction<? super T, ? super U, ? extends U> arg1) {
            return null;
        }
        @Independent @NotModified T single() { return null; }
        @Independent @NotModified Option<T> singleOption() { return null; }
        @NotModified int size() { return 0; }
        @Independent @Modified
        Iterator<? extends Traversable<T>> slideBy(
            @IgnoreModifications @Independent @Modified Function<? super T, ?> arg0) { return null; }
        @Independent @Modified Iterator<? extends Traversable<T>> sliding(int arg0) { return null; }
        @Independent @Modified Iterator<? extends Traversable<T>> sliding(int arg0, int arg1) { return null; }
        @Independent(hc = true) @Modified
        Tuple2<? extends Traversable<T>, ? extends Traversable<T>> span(
            @IgnoreModifications @Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from io.vavr.Value, java.lang.Iterable
        @Independent @NotModified
        Spliterator<T> spliterator() { return null; }
        @Independent @NotModified Number sum() { return null; }
        @Independent(absent = true) @Modified Traversable<T> tail() { return null; }
        @Independent @Modified Option<? extends Traversable<T>> tailOption() { return null; }
        @Independent @Modified Traversable<T> take(int arg0) { return null; }
        @Independent @Modified Traversable<T> takeRight(int arg0) { return null; }
        @Independent @Modified
        Traversable<T> takeUntil(@IgnoreModifications @Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent @Modified
        Traversable<T> takeWhile(@IgnoreModifications @Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent(absent = true) @Modified
        <T1, T2> Tuple2<? extends Traversable<T1>, ? extends Traversable<T2>> unzip(
            @IgnoreModifications @Independent @Modified Function<? super T, Tuple2<? extends T1, ? extends T2>> arg0) {
            return null;
        }

        @Independent(absent = true) @Modified
        <T1, T2, T3> Tuple3<? extends Traversable<T1>, ? extends Traversable<T2>, ? extends Traversable<T3>> unzip3(
            @IgnoreModifications @Independent @Modified Function<
                ? super T,
                Tuple3<? extends T1, ? extends T2, ? extends T3>> arg0) { return null; }

        @Independent @Modified
        <U> Traversable<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> arg0) { return null; }

        @Independent @Modified
        <U> Traversable<Tuple2<T, U>> zipAll(
            @Independent @Modified Iterable<? extends U> arg0,
            @Independent @NotModified T arg1,
            @Independent @Modified U arg2) { return null; }

        @Independent @Modified
        <U, R> Traversable<R> zipWith(
            @Independent @NotModified Iterable<? extends U> arg0,
            @IgnoreModifications @Independent @Modified BiFunction<? super T, ? super U, ? extends R> arg1) {
            return null;
        }
        @Independent @Modified Traversable<Tuple2<T, Integer>> zipWithIndex() { return null; }
        @Independent @Modified
        <U> Traversable<U> zipWithIndex(
            @IgnoreModifications @Independent @Modified BiFunction<? super T, ? super Integer, ? extends U> arg0) {
            return null;
        }
    }

    //public interface Tree implements Traversable<T>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class Tree$<T> {
        static final long serialVersionUID = 0L;
        //static final class Empty implements Tree<T>, Serializable
        @ImmutableContainer
        class Empty<T> {
            //override from io.vavr.collection.Tree
            @NotModified
            String draw() { return null; }

            //override from io.vavr.Value, io.vavr.collection.Traversable, io.vavr.collection.Tree, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object o) { return false; }

            //override from io.vavr.collection.Tree
            @Independent @NotModified
            List<Tree.Node<T>> getChildren() { return null; }

            //override from io.vavr.collection.Tree
            @Independent @NotModified
            T getValue() { return null; }

            //override from io.vavr.Value, io.vavr.collection.Traversable, io.vavr.collection.Tree, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }
            @Independent @NotModified static <T> Tree.Empty<T> instance() { return null; }
            //override from io.vavr.Value, io.vavr.collection.Traversable
            @NotModified
            boolean isEmpty() { return false; }

            //override from io.vavr.collection.Tree
            @NotModified
            boolean isLeaf() { return false; }

            //override from io.vavr.collection.Traversable
            @Independent @NotModified
            T last() { return null; }

            //override from io.vavr.collection.Traversable
            @NotModified
            int length() { return 0; }

            //override from io.vavr.collection.Tree
            @NotModified
            String toLispString() { return null; }

            //override from io.vavr.Value, io.vavr.collection.Tree, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        //static final class Node implements Tree<T>, Serializable
        //annotated as EXPECTED; computed @FinalFields @Independent -- G1: persistent collection (VAVR.md)
        @ImmutableContainer(hc = true)
        class Node<T> {
            Node(
                @Independent(hc = true) @NotModified T value,
                @Independent(hc = true) @NotModified List<Tree.Node<T>> children) { }

            //override from io.vavr.collection.Tree
            @NotModified
            String draw() { return null; }

            //override from io.vavr.Value, io.vavr.collection.Traversable, io.vavr.collection.Tree, java.lang.Object
            @NotModified
            public boolean equals(@Independent @NotModified Object o) { return false; }

            //override from io.vavr.collection.Tree
            @Independent(hc = true) @NotModified @GetSet("children")
            List<Tree.Node<T>> getChildren() { return null; }

            //override from io.vavr.collection.Tree
            @Independent(hc = true) @NotModified @GetSet("value")
            T getValue() { return null; }

            //override from io.vavr.Value, io.vavr.collection.Traversable, io.vavr.collection.Tree, java.lang.Object
            @NotModified
            public int hashCode() { return 0; }

            //override from io.vavr.Value, io.vavr.collection.Traversable
            @NotModified
            boolean isEmpty() { return false; }

            //override from io.vavr.collection.Tree
            @NotModified
            boolean isLeaf() { return false; }

            //override from io.vavr.collection.Traversable
            @Independent(hc = true) @NotModified
            T last() { return null; }

            //override from io.vavr.collection.Traversable
            @NotModified @GetSet("size")
            int length() { return 0; }

            //override from io.vavr.collection.Tree
            @NotModified
            String toLispString() { return null; }

            //override from io.vavr.Value, io.vavr.collection.Tree, java.lang.Object
            @NotModified
            public String toString() { return null; }
        }

        //enum Order extends Enum<Order>
        @ImmutableContainer
        class Order {
            @Independent @NotModified static final Tree.Order IN_ORDER = null;
            @Independent @NotModified static final Tree.Order LEVEL_ORDER = null;
            @Independent @NotModified static final Tree.Order POST_ORDER = null;
            @Independent @NotModified static final Tree.Order PRE_ORDER = null;
        }
        @NotModified int branchCount() { return 0; }
        @Independent @NotModified
        static <T, ID> List<Tree.Node<T>> build(
            @Independent @NotModified Iterable<? extends T> source,
            @Independent @NotModified Function<? super T, ? extends ID> idMapper,
            @Independent @NotModified Function<? super T, ? extends ID> parentMapper) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <R> Tree<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> partialFunction) {
            return null;
        }
        @Independent @NotModified static <T> Collector<T, ArrayList<T>, Tree<T>> collector() { return null; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> distinct() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> distinctBy(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Seq<T> distinctBy(@Independent @NotModified Function<? super T, ? extends U> keyExtractor) { return null; }
        @NotModified String draw() { return null; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> drop(int n) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> dropRight(int n) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> dropUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> dropWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }
        @Independent @NotModified static <T> Tree.Empty<T> empty() { return null; }
        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object arg0) { return false; }
        @Independent @NotModified static <T> Tree<T> fill(int n, @Independent @NotModified T element) { return null; }
        @Independent @NotModified
        static <T> Tree<T> fill(int n, @Independent @NotModified Supplier<? extends T> s) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> filter(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Tree<U> flatMap(@Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Foldable, io.vavr.collection.Traversable
        @Identity @Independent(hc = true) @NotModified

        <U> U foldRight(
            @Independent(hc = true) @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> f) { return null; }
        @Independent(hc = true) @NotModified List<Tree.Node<T>> getChildren() { return null; }
        @Independent(hc = true) @NotModified T getValue() { return null; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, Seq<T>> groupBy(
            @Independent @NotModified Function<? super T, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Seq<T>> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T head() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> init() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Option<Seq<T>> initOption() { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }
        @NotModified boolean isBranch() { return false; }
        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isDistinct() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }
        @NotModified boolean isLeaf() { return false; }
        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isSequential() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent @NotModified
        Iterator<T> iterator() { return null; }
        @Independent @NotModified Iterator<T> iterator(@Independent @NotModified Tree.Order order) { return null; }
        @NotModified int leafCount() { return 0; }
        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Tree<U> map(@Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Tree<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Independent @NotModified
        Tree<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> Tree<T> narrow(@Independent @NotModified Tree<? extends T> tree) { return null; }
        @NotModified int nodeCount() { return 0; }
        @Independent @NotModified static <T> Tree.Node<T> of(@Independent @NotModified T value) { return null; }
        @Independent @NotModified
        static <T> Tree.Node<T> of(
            @Independent @NotModified T value,
            @Independent @NotModified Iterable<Tree.Node<T>> children) { return null; }

        @Independent @NotModified
        static <T> Tree.Node<T> of(
            @Independent @NotModified T value,
            @Independent @NotModified Tree.Node<T> ... children) { return null; }
        @Independent @NotModified static <T> Tree<T> of(@Independent @NotModified T ... values) { return null; }
        @Independent @NotModified
        static <T> Tree<T> ofAll(@Independent @NotModified Iterable<? extends T> iterable) { return null; }

        @Independent @NotModified
        static <T> Tree<T> ofAll(@Independent @NotModified Stream<? extends T> javaStream) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Tree<T> orElse(@Independent @NotModified Iterable<? extends T> other) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Tree<T> orElse(@Independent @NotModified Supplier<? extends Iterable<? extends T>> supplier) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<Seq<T>, Seq<T>> partition(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Tree<T> peek(@Independent @NotModified Consumer<? super T> action) { return null; }

        @Independent @NotModified
        static <T> Tree.Node<T> recurse(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, ? extends Iterable<? extends T>> descend) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> reject(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        Tree<T> replace(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Tree<T> replaceAll(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> retainAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        Seq<T> scan(
            @Independent @NotModified T zero,
            @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Seq<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Seq<T>> slideBy(@Independent @NotModified Function<? super T, ?> classifier) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Seq<T>> sliding(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Seq<T>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<Seq<T>, Seq<T>> span(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        @Independent @NotModified
        static <T> Tree<T> tabulate(int n, @Independent @NotModified Function<? super Integer, ? extends T> f) {
            return null;
        }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> tail() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Option<Seq<T>> tailOption() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> take(int n) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> takeRight(int n) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> takeUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Seq<T> takeWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }
        @NotModified String toLispString() { return null; }
        //override from io.vavr.Value, java.lang.Object
        @NotModified @NotNull
        public String toString() { return null; }

        @Independent @NotModified
        <U> U transform(@Independent @NotModified Function<? super Tree<T>, ? extends U> f) { return null; }
        @Independent @NotModified Seq<Tree.Node<T>> traverse() { return null; }
        @Independent @NotModified
        Seq<Tree.Node<T>> traverse(@Independent @NotModified Tree.Order order) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<Tree<T1>, Tree<T2>> unzip(
            @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2, T3> Tuple3<Tree<T1>, Tree<T2>, Tree<T3>> unzip3(
            @Independent @NotModified Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return null;
        }
        @Independent @NotModified Seq<T> values() { return null; }
        @Independent @NotModified Seq<T> values(@Independent @NotModified Tree.Order order) { return null; }
        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Tree<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Tree<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified T thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> Tree<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Tree<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Tree<U> zipWithIndex(@Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> mapper) {
            return null;
        }
    }

    //public final class TreeMap implements SortedMap<K,V>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Independent -- G1: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class TreeMap$<K, V> {
        //override from io.vavr.collection.SortedMap
        @Independent @NotModified

        <K2, V2> TreeMap<K2, V2> bimap(
            @Independent @NotModified Comparator<? super K2> keyComparator,
            @Independent @NotModified Function<? super K, ? extends K2> keyMapper,
            @Independent @NotModified Function<? super V, ? extends V2> valueMapper) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified

        <K2, V2> TreeMap<K2, V2> bimap(
            @Independent @NotModified Function<? super K, ? extends K2> keyMapper,
            @Independent @NotModified Function<? super V, ? extends V2> valueMapper) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> Collector<Tuple2<K, V>, ArrayList<Tuple2<K, V>>, TreeMap<K, V>>
            collector() { return null; }

        @Independent @NotModified
        static <K, V> Collector<Tuple2<K, V>, ArrayList<Tuple2<K, V>>, TreeMap<K, V>> collector(
            @Independent @NotModified Comparator<? super K> keyComparator) { return null; }

        @Independent @NotModified
        static <K, V, T extends V> Collector<T, ArrayList<T>, TreeMap<K, V>> collector(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified Function<? super T, ? extends K> keyMapper) { return null; }

        @Independent @NotModified
        static <K, V, T> Collector<T, ArrayList<T>, TreeMap<K, V>> collector(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified Function<? super T, ? extends K> keyMapper,
            @Independent @NotModified Function<? super T, ? extends V> valueMapper) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V, T extends V> Collector<T, ArrayList<T>, TreeMap<K, V>> collector(
            @Independent @NotModified Function<? super T, ? extends K> keyMapper) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V, T> Collector<T, ArrayList<T>, TreeMap<K, V>> collector(
            @Independent @NotModified Function<? super T, ? extends K> keyMapper,
            @Independent @NotModified Function<? super T, ? extends V> valueMapper) { return null; }

        //override from io.vavr.collection.Ordered
        @Independent @NotModified
        Comparator<K> comparator() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Fluent @Independent @NotModified

        Tuple2<V, TreeMap<K, V>> computeIfAbsent(
            @Independent @NotModified K key,
            @Independent @NotModified Function<? super K, ? extends V> mappingFunction) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Fluent @Independent @NotModified

        Tuple2<Option<V>, TreeMap<K, V>> computeIfPresent(
            @Independent @NotModified K key,
            @Independent @NotModified BiFunction<? super K, ? super V, ? extends V> remappingFunction) { return null; }

        //override from io.vavr.collection.Map
        @NotModified
        boolean containsKey(@Independent @NotModified K key) { return false; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeMap<K, V> distinct() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeMap<K, V> distinctBy(@Independent @NotModified Comparator<? super Tuple2<K, V>> comparator) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> TreeMap<K, V> distinctBy(@Independent @NotModified Function<? super Tuple2<K, V>, ? extends U> keyExtractor) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeMap<K, V> drop(int n) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeMap<K, V> dropRight(int n) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeMap<K, V> dropUntil(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeMap<K, V> dropWhile(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }
        @Independent @NotModified static <K extends Comparable<? super K>, V> TreeMap<K, V> empty() { return null; }
        @Independent @NotModified
        static <K, V> TreeMap<K, V> empty(@Independent @NotModified Comparator<? super K> keyComparator) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> TreeMap<K, V> fill(
            int n,
            @Independent @NotModified Supplier<? extends Tuple2<? extends K, ? extends V>> s) { return null; }

        @Independent @NotModified
        static <K, V> TreeMap<K, V> fill(
            @Independent @NotModified Comparator<? super K> keyComparator,
            int n,
            @Independent @NotModified Supplier<? extends Tuple2<? extends K, ? extends V>> s) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified
        TreeMap<K, V> filter(@Independent @NotModified BiPredicate<? super K, ? super V> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeMap<K, V> filter(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified
        TreeMap<K, V> filterKeys(@Independent @NotModified Predicate<? super K> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified
        TreeMap<K, V> filterValues(@Independent @NotModified Predicate<? super V> predicate) { return null; }

        //override from io.vavr.collection.SortedMap
        @Independent @NotModified

        <K2, V2> TreeMap<K2, V2> flatMap(
            @Independent @NotModified Comparator<? super K2> keyComparator,
            @Independent @NotModified BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified

        <K2, V2> TreeMap<K2, V2> flatMap(
            @Independent @NotModified BiFunction<? super K, ? super V, ? extends Iterable<Tuple2<K2, V2>>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        Option<V> get(@Independent @NotModified K key) { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        V getOrElse(@Independent @NotModified K key, @Independent @NotModified V defaultValue) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, TreeMap<K, V>> groupBy(
            @Independent @NotModified Function<? super Tuple2<K, V>, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<TreeMap<K, V>> grouped(int size) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<K, V> head() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeMap<K, V> init() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<TreeMap<K, V>> initOption() { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Map, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent @NotModified
        Iterator<Tuple2<K, V>> iterator() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified
        io.vavr.collection.SortedSet<K> keySet() { return null; }

        //override from io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<K, V> last() { return null; }

        //override from io.vavr.collection.SortedMap
        @Independent @NotModified

        <K2, V2> TreeMap<K2, V2> map(
            @Independent @NotModified Comparator<? super K2> keyComparator,
            @Independent @NotModified BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified

        <K2, V2> TreeMap<K2, V2> map(@Independent @NotModified BiFunction<? super K, ? super V, Tuple2<K2, V2>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified

        <K2> TreeMap<K2, V> mapKeys(@Independent @NotModified Function<? super K, ? extends K2> keyMapper) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified

        <K2> TreeMap<K2, V> mapKeys(
            @Independent @NotModified Function<? super K, ? extends K2> keyMapper,
            @Independent @NotModified BiFunction<? super V, ? super V, ? extends V> valueMerge) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified

        <W> TreeMap<K, W> mapValues(@Independent @NotModified Function<? super V, ? extends W> valueMapper) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Fluent @Independent @NotModified

        TreeMap<K, V> merge(@Independent @NotModified io.vavr.collection.Map<? extends K, ? extends V> that) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Fluent @Independent @NotModified

        <U extends V> TreeMap<K, V> merge(
            @Independent @NotModified io.vavr.collection.Map<? extends K, U> that,
            @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> collisionResolution) { return null; }

        @Identity @Independent @NotModified
        static <K, V> TreeMap<K, V> narrow(@Independent @NotModified TreeMap<? extends K, ? extends V> treeMap) {
            return null;
        }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> TreeMap<K, V> of(
            @Independent @NotModified K key,
            @Independent @NotModified V value) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> TreeMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> TreeMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> TreeMap<K, V> of(
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> TreeMap<K, V> of(
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
        static <K extends Comparable<? super K>, V> TreeMap<K, V> of(
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
        static <K extends Comparable<? super K>, V> TreeMap<K, V> of(
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
        static <K extends Comparable<? super K>, V> TreeMap<K, V> of(
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
        static <K extends Comparable<? super K>, V> TreeMap<K, V> of(
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
        static <K extends Comparable<? super K>, V> TreeMap<K, V> of(
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
        static <K extends Comparable<? super K>, V> TreeMap<K, V> of(
            @Independent @NotModified Tuple2<? extends K, ? extends V> entry) { return null; }

        @Independent @NotModified
        static <K, V> TreeMap<K, V> of(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified K key,
            @Independent @NotModified V value) { return null; }

        @Independent @NotModified
        static <K, V> TreeMap<K, V> of(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2) { return null; }

        @Independent @NotModified
        static <K, V> TreeMap<K, V> of(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3) { return null; }

        @Independent @NotModified
        static <K, V> TreeMap<K, V> of(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified K k1,
            @Independent @NotModified V v1,
            @Independent @NotModified K k2,
            @Independent @NotModified V v2,
            @Independent @NotModified K k3,
            @Independent @NotModified V v3,
            @Independent @NotModified K k4,
            @Independent @NotModified V v4) { return null; }

        @Independent @NotModified
        static <K, V> TreeMap<K, V> of(
            @Independent @NotModified Comparator<? super K> keyComparator,
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
        static <K, V> TreeMap<K, V> of(
            @Independent @NotModified Comparator<? super K> keyComparator,
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
        static <K, V> TreeMap<K, V> of(
            @Independent @NotModified Comparator<? super K> keyComparator,
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
        static <K, V> TreeMap<K, V> of(
            @Independent @NotModified Comparator<? super K> keyComparator,
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
        static <K, V> TreeMap<K, V> of(
            @Independent @NotModified Comparator<? super K> keyComparator,
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
        static <K, V> TreeMap<K, V> of(
            @Independent @NotModified Comparator<? super K> keyComparator,
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
        static <K, V> TreeMap<K, V> of(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified Tuple2<? extends K, ? extends V> entry) { return null; }

        @Independent @NotModified
        static <K, V> TreeMap<K, V> ofAll(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified Map<? extends K, ? extends V> map) { return null; }

        @Independent @NotModified
        static <T, K, V> TreeMap<K, V> ofAll(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified Stream<? extends T> stream,
            @Independent @NotModified Function<? super T, Tuple2<? extends K, ? extends V>> entryMapper) { return null; }

        @Independent @NotModified
        static <T, K, V> TreeMap<K, V> ofAll(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified Stream<? extends T> stream,
            @Independent @NotModified Function<? super T, ? extends K> keyMapper,
            @Independent @NotModified Function<? super T, ? extends V> valueMapper) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> TreeMap<K, V> ofAll(
            @Independent @NotModified Map<? extends K, ? extends V> map) { return null; }

        @Independent @NotModified
        static <T, K extends Comparable<? super K>, V> TreeMap<K, V> ofAll(
            @Independent @NotModified Stream<? extends T> stream,
            @Independent @NotModified Function<? super T, Tuple2<? extends K, ? extends V>> entryMapper) { return null; }

        @Independent @NotModified
        static <T, K extends Comparable<? super K>, V> TreeMap<K, V> ofAll(
            @Independent @NotModified Stream<? extends T> stream,
            @Independent @NotModified Function<? super T, ? extends K> keyMapper,
            @Independent @NotModified Function<? super T, ? extends V> valueMapper) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> TreeMap<K, V> ofEntries(
            @Independent @NotModified Iterable<? extends Tuple2<? extends K, ? extends V>> entries) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> TreeMap<K, V> ofEntries(
            @Independent @NotModified Tuple2<? extends K, ? extends V> ... entries) { return null; }

        @Independent @NotModified
        static <K, V> TreeMap<K, V> ofEntries(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified Iterable<? extends Tuple2<? extends K, ? extends V>> entries) { return null; }

        @Independent @NotModified
        static <K, V> TreeMap<K, V> ofEntries(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified Tuple2<? extends K, ? extends V> ... entries) { return null; }

        @Independent @NotModified
        static <K, V> TreeMap<K, V> ofEntries(
            @Independent @NotModified Comparator<? super K> keyComparator,
            @Independent @NotModified Map.Entry<? extends K, ? extends V> ... entries) { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> TreeMap<K, V> ofEntries(
            @Independent @NotModified Map.Entry<? extends K, ? extends V> ... entries) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeMap<K, V> orElse(@Independent @NotModified Iterable<? extends Tuple2<K, V>> other) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        TreeMap<K, V> orElse(@Independent @NotModified Supplier<? extends Iterable<? extends Tuple2<K, V>>> supplier) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<TreeMap<K, V>, TreeMap<K, V>> partition(
            @Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeMap<K, V> peek(@Independent @NotModified Consumer<? super Tuple2<K, V>> action) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent(hc = true) @NotModified
        TreeMap<K, V> put(@Independent @NotModified K key, @Independent @NotModified V value) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified

        <U extends V> TreeMap<K, V> put(
            @Independent @NotModified K key,
            @Independent @NotModified U value,
            @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> merge) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified
        TreeMap<K, V> put(@Independent @NotModified Tuple2<? extends K, ? extends V> entry) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified

        <U extends V> TreeMap<K, V> put(
            @Independent @NotModified Tuple2<? extends K, U> entry,
            @Independent @NotModified BiFunction<? super V, ? super U, ? extends V> merge) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified
        TreeMap<K, V> reject(@Independent @NotModified BiPredicate<? super K, ? super V> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeMap<K, V> reject(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified
        TreeMap<K, V> rejectKeys(@Independent @NotModified Predicate<? super K> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified
        TreeMap<K, V> rejectValues(@Independent @NotModified Predicate<? super V> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Fluent @Independent @NotModified
        TreeMap<K, V> remove(@Independent @NotModified K key) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Fluent @Independent @NotModified
        TreeMap<K, V> removeAll(@Independent @NotModified Iterable<? extends K> keys) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified
        TreeMap<K, V> removeAll(@Independent @NotModified BiPredicate<? super K, ? super V> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified
        TreeMap<K, V> removeKeys(@Independent @NotModified Predicate<? super K> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified
        TreeMap<K, V> removeValues(@Independent @NotModified Predicate<? super V> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Fluent @Independent @NotModified

        TreeMap<K, V> replace(
            @Independent @NotModified K key,
            @Independent @NotModified V oldValue,
            @Independent @NotModified V newValue) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        TreeMap<K, V> replace(
            @Independent @NotModified Tuple2<K, V> currentElement,
            @Independent @NotModified Tuple2<K, V> newElement) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        TreeMap<K, V> replaceAll(
            @Independent @NotModified Tuple2<K, V> currentElement,
            @Independent @NotModified Tuple2<K, V> newElement) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Independent @NotModified

        TreeMap<K, V> replaceAll(@Independent @NotModified BiFunction<? super K, ? super V, ? extends V> function) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Fluent @Independent @NotModified
        TreeMap<K, V> replaceValue(@Independent @NotModified K key, @Independent @NotModified V value) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeMap<K, V> retainAll(@Independent @NotModified Iterable<? extends Tuple2<K, V>> elements) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified

        TreeMap<K, V> scan(
            @Independent @NotModified Tuple2<K, V> zero,
            @Independent @NotModified BiFunction<? super Tuple2<K, V>, ? super Tuple2<K, V>, ? extends Tuple2<K, V>>
                operation) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.Traversable
        @NotModified
        int size() { return 0; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified

        Iterator<TreeMap<K, V>> slideBy(@Independent @NotModified Function<? super Tuple2<K, V>, ?> classifier) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<TreeMap<K, V>> sliding(int size) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<TreeMap<K, V>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<TreeMap<K, V>, TreeMap<K, V>> span(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        @Independent @NotModified
        static <K extends Comparable<? super K>, V> TreeMap<K, V> tabulate(
            int n,
            @Independent @NotModified Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f) {
            return null;
        }

        @Independent @NotModified
        static <K, V> TreeMap<K, V> tabulate(
            @Independent @NotModified Comparator<? super K> keyComparator,
            int n,
            @Independent @NotModified Function<? super Integer, ? extends Tuple2<? extends K, ? extends V>> f) {
            return null;
        }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeMap<K, V> tail() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<TreeMap<K, V>> tailOption() { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeMap<K, V> take(int n) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeMap<K, V> takeRight(int n) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeMap<K, V> takeUntil(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeMap<K, V> takeWhile(@Independent @NotModified Predicate<? super Tuple2<K, V>> predicate) { return null; }

        //override from io.vavr.collection.Map, io.vavr.collection.SortedMap
        @Fluent @Independent @NotModified
        java.util.TreeMap<K, V> toJavaMap() { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public String toString() { return null; }

        //override from io.vavr.collection.Map
        @Independent @NotModified
        Seq<V> values() { return null; }
    }

    //public final class TreeMultimap extends AbstractMultimap<K,V,TreeMultimap<K,V>> implements Serializable, SortedMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Container @Independent -- G1: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class TreeMultimap$<K, V> {
        //public static class Builder
        @Immutable(hc = true)
        @Independent
        class Builder<V> {
            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> Collector<
                Tuple2<K, V2>,
                ArrayList<Tuple2<K, V2>>,
                TreeMultimap<K, V2>> collector() { return null; }

            @Independent @NotModified
            <K, V2 extends V> Collector<Tuple2<K, V2>, ArrayList<Tuple2<K, V2>>, TreeMultimap<K, V2>> collector(
                @Independent @NotModified Comparator<? super K> keyComparator) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> empty() { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> empty(@Independent @NotModified Comparator<? super K> keyComparator) {
                return null;
            }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> fill(
                int n,
                @Independent @NotModified Tuple2<? extends K, ? extends V2> element) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> fill(
                int n,
                @Independent @Modified Supplier<? extends Tuple2<? extends K, ? extends V2>> s) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> fill(
                @Independent @NotModified Comparator<? super K> keyComparator,
                int n,
                @Independent @NotModified Tuple2<? extends K, ? extends V2> element) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> fill(
                @Independent @NotModified Comparator<? super K> keyComparator,
                int n,
                @Independent @Modified Supplier<? extends Tuple2<? extends K, ? extends V2>> s) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @NotModified K key,
                @Independent @Modified V2 value) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @NotModified K k2,
                @Independent @Modified V2 v2) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @NotModified K k3,
                @Independent @Modified V2 v3) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @NotModified K k4,
                @Independent @Modified V2 v4) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @NotModified K k5,
                @Independent @Modified V2 v5) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @NotModified K k6,
                @Independent @Modified V2 v6) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @NotModified K k7,
                @Independent @Modified V2 v7) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @Modified K k7,
                @Independent @Modified V2 v7,
                @Independent @NotModified K k8,
                @Independent @Modified V2 v8) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @Modified K k7,
                @Independent @Modified V2 v7,
                @Independent @Modified K k8,
                @Independent @Modified V2 v8,
                @Independent @NotModified K k9,
                @Independent @Modified V2 v9) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @Modified K k7,
                @Independent @Modified V2 v7,
                @Independent @Modified K k8,
                @Independent @Modified V2 v8,
                @Independent @Modified K k9,
                @Independent @Modified V2 v9,
                @Independent @NotModified K k10,
                @Independent @Modified V2 v10) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @Modified Tuple2<? extends K, ? extends V2> entry) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @NotModified K key,
                @Independent @Modified V2 value) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @NotModified K k2,
                @Independent @Modified V2 v2) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @NotModified K k3,
                @Independent @Modified V2 v3) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @NotModified K k4,
                @Independent @Modified V2 v4) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @NotModified K k5,
                @Independent @Modified V2 v5) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @NotModified K k6,
                @Independent @Modified V2 v6) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @NotModified K k7,
                @Independent @Modified V2 v7) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @Modified K k7,
                @Independent @Modified V2 v7,
                @Independent @NotModified K k8,
                @Independent @Modified V2 v8) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @Modified K k7,
                @Independent @Modified V2 v7,
                @Independent @Modified K k8,
                @Independent @Modified V2 v8,
                @Independent @NotModified K k9,
                @Independent @Modified V2 v9) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified K k1,
                @Independent @Modified V2 v1,
                @Independent @Modified K k2,
                @Independent @Modified V2 v2,
                @Independent @Modified K k3,
                @Independent @Modified V2 v3,
                @Independent @Modified K k4,
                @Independent @Modified V2 v4,
                @Independent @Modified K k5,
                @Independent @Modified V2 v5,
                @Independent @Modified K k6,
                @Independent @Modified V2 v6,
                @Independent @Modified K k7,
                @Independent @Modified V2 v7,
                @Independent @Modified K k8,
                @Independent @Modified V2 v8,
                @Independent @Modified K k9,
                @Independent @Modified V2 v9,
                @Independent @NotModified K k10,
                @Independent @Modified V2 v10) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> of(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified Tuple2<? extends K, ? extends V2> entry) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> ofAll(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @NotModified Map<? extends K, ? extends V2> map) { return null; }

            @Independent @NotModified
            <T, K, V2 extends V> TreeMultimap<K, V2> ofAll(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified Stream<? extends T> stream,
                @Independent @Modified Function<? super T, Tuple2<? extends K, ? extends V2>> entryMapper) {
                return null;
            }

            @Independent @NotModified
            <T, K, V2 extends V> TreeMultimap<K, V2> ofAll(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified Stream<? extends T> stream,
                @Independent @Modified Function<? super T, ? extends K> keyMapper,
                @Independent @Modified Function<? super T, ? extends V2> valueMapper) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> ofAll(
                @Independent @NotModified Map<? extends K, ? extends V2> map) { return null; }

            @Independent @NotModified
            <T, K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> ofAll(
                @Independent @Modified Stream<? extends T> stream,
                @Independent @Modified Function<? super T, Tuple2<? extends K, ? extends V2>> entryMapper) {
                return null;
            }

            @Independent @NotModified
            <T, K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> ofAll(
                @Independent @Modified Stream<? extends T> stream,
                @Independent @Modified Function<? super T, ? extends K> keyMapper,
                @Independent @Modified Function<? super T, ? extends V2> valueMapper) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> ofEntries(
                @Independent @Modified Iterable<? extends Tuple2<? extends K, ? extends V2>> entries) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> ofEntries(
                @Independent @NotModified Tuple2<? extends K, ? extends V2> ... entries) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> ofEntries(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @Modified Iterable<? extends Tuple2<? extends K, ? extends V2>> entries) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> ofEntries(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @NotModified Tuple2<? extends K, ? extends V2> ... entries) { return null; }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> ofEntries(
                @Independent @NotModified Comparator<? super K> keyComparator,
                @Independent @NotModified Map.Entry<? extends K, ? extends V2> ... entries) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> ofEntries(
                @Independent @NotModified Map.Entry<? extends K, ? extends V2> ... entries) { return null; }

            @Independent @NotModified
            <K extends Comparable<? super K>, V2 extends V> TreeMultimap<K, V2> tabulate(
                int n,
                @Independent @Modified Function<? super Integer, ? extends Tuple2<? extends K, ? extends V2>> f) {
                return null;
            }

            @Independent @NotModified
            <K, V2 extends V> TreeMultimap<K, V2> tabulate(
                @Independent @NotModified Comparator<? super K> keyComparator,
                int n,
                @Independent @Modified Function<? super Integer, ? extends Tuple2<? extends K, ? extends V2>> f) {
                return null;
            }
        }

        //override from io.vavr.collection.Ordered
        @Independent @NotModified
        Comparator<K> comparator() { return null; }

        //override from io.vavr.collection.AbstractMultimap, io.vavr.collection.Multimap, io.vavr.collection.SortedMultimap
        @Independent(hc = true) @NotModified
        io.vavr.collection.SortedSet<K> keySet() { return null; }

        @Identity @Independent @NotModified
        static <K, V> TreeMultimap<K, V> narrow(@Independent @NotModified TreeMultimap<? extends K, ? extends V> map) {
            return null;
        }

        //override from io.vavr.collection.AbstractMultimap, io.vavr.collection.Multimap, io.vavr.collection.SortedMultimap
        @Independent @NotModified
        java.util.SortedMap<K, Collection<V>> toJavaMap() { return null; }
        @Independent @NotModified static <V> TreeMultimap.Builder<V> withSeq() { return null; }
        @Independent @NotModified static <V> TreeMultimap.Builder<V> withSet() { return null; }
        @Independent @NotModified
        static <V extends Comparable<?>> TreeMultimap.Builder<V> withSortedSet() { return null; }

        @Independent @NotModified
        static <V> TreeMultimap.Builder<V> withSortedSet(@Independent @NotModified Comparator<? super V> comparator) {
            return null;
        }
    }

    //public final class TreeSet implements SortedSet<T>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Independent -- G1: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class TreeSet$<T> {
        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Fluent @Independent @NotModified
        TreeSet<T> add(@Independent(hc = true) @NotModified T element) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Fluent @Independent @NotModified
        TreeSet<T> addAll(@Independent(hc = true) @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <R> TreeSet<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> partialFunction) {
            return null;
        }

        @Independent @NotModified
        static <T extends Comparable<? super T>> Collector<T, ArrayList<T>, TreeSet<T>> collector() { return null; }

        @Independent @NotModified
        static <T> Collector<T, ArrayList<T>, TreeSet<T>> collector(
            @Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.Ordered
        @Independent(hc = true) @NotModified
        Comparator<T> comparator() { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set
        @NotModified
        boolean contains(@Independent(hc = true) @NotModified T element) { return false; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Fluent @Independent @NotModified
        TreeSet<T> diff(@Independent @NotModified io.vavr.collection.Set<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> distinct() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> distinctBy(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        <U> TreeSet<T> distinctBy(@Independent @NotModified Function<? super T, ? extends U> keyExtractor) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> drop(int n) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> dropRight(int n) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> dropUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> dropWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }
        @Independent @NotModified static <T extends Comparable<? super T>> TreeSet<T> empty() { return null; }
        @Independent @NotModified
        static <T> TreeSet<T> empty(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> TreeSet<T> fill(
            int n,
            @Independent @NotModified Supplier<? extends T> s) { return null; }

        @Independent @NotModified
        static <T> TreeSet<T> fill(
            @Independent @NotModified Comparator<? super T> comparator,
            int n,
            @Independent @NotModified Supplier<? extends T> s) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> filter(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.SortedSet
        @Independent @NotModified

        <U> TreeSet<U> flatMap(
            @Independent @NotModified Comparator<? super U> comparator,
            @Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> TreeSet<U> flatMap(@Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Foldable, io.vavr.collection.Traversable
        @Identity @Independent @NotModified

        <U> U foldRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> f) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, TreeSet<T>> groupBy(
            @Independent @NotModified Function<? super T, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<TreeSet<T>> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T head() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        Option<T> headOption() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeSet<T> init() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<TreeSet<T>> initOption() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Fluent @Independent @NotModified

        TreeSet<T> intersect(@Independent(hc = true) @NotModified io.vavr.collection.Set<? extends T> elements) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent @NotModified
        Iterator<T> iterator() { return null; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T last() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.Traversable
        @NotModified
        int length() { return 0; }

        //override from io.vavr.collection.SortedSet
        @Independent @NotModified

        <U> TreeSet<U> map(
            @Independent @NotModified Comparator<? super U> comparator,
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> TreeSet<U> map(@Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> TreeSet<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeSet<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> TreeSet<T> narrow(@Independent @NotModified TreeSet<? extends T> treeSet) { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> TreeSet<T> of(@Independent @NotModified T value) { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> TreeSet<T> of(@Independent @NotModified T ... values) { return null; }

        @Independent @NotModified
        static <T> TreeSet<T> of(
            @Independent @NotModified Comparator<? super T> comparator,
            @Independent @NotModified T value) { return null; }

        @Independent @NotModified
        static <T> TreeSet<T> of(
            @Independent @NotModified Comparator<? super T> comparator,
            @Independent @NotModified T ... values) { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> TreeSet<T> ofAll(
            @Independent @NotModified Iterable<? extends T> values) { return null; }

        @Independent @NotModified
        static TreeSet<Boolean> ofAll(@Independent @NotModified boolean ... elements) { return null; }

        @Independent @NotModified
        static TreeSet<Byte> ofAll(@Independent @NotModified byte ... elements) { return null; }

        @Independent @NotModified
        static TreeSet<Character> ofAll(@Independent @NotModified char ... elements) { return null; }

        @Independent @NotModified
        static TreeSet<Double> ofAll(@Independent @NotModified double ... elements) { return null; }

        @Independent @NotModified
        static TreeSet<Float> ofAll(@Independent @NotModified float ... elements) { return null; }

        @Independent @NotModified
        static TreeSet<Integer> ofAll(@Independent @NotModified int ... elements) { return null; }

        @Independent @NotModified
        static <T> TreeSet<T> ofAll(
            @Independent @NotModified Comparator<? super T> comparator,
            @Independent @NotModified Iterable<? extends T> values) { return null; }

        @Independent @NotModified
        static <T> TreeSet<T> ofAll(
            @Independent @NotModified Comparator<? super T> comparator,
            @Independent @NotModified Stream<? extends T> javaStream) { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> TreeSet<T> ofAll(
            @Independent @NotModified Stream<? extends T> javaStream) { return null; }

        @Independent @NotModified
        static TreeSet<Long> ofAll(@Independent @NotModified long ... elements) { return null; }

        @Independent @NotModified
        static TreeSet<Short> ofAll(@Independent @NotModified short ... elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> orElse(@Independent @NotModified Iterable<? extends T> other) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> orElse(@Independent @NotModified Supplier<? extends Iterable<? extends T>> supplier) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        Tuple2<TreeSet<T>, TreeSet<T>> partition(@Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> peek(@Independent @NotModified Consumer<? super T> action) { return null; }
        @Independent @NotModified static TreeSet<Character> range(char from, char toExclusive) { return null; }
        @Independent @NotModified static TreeSet<Integer> range(int from, int toExclusive) { return null; }
        @Independent @NotModified static TreeSet<Long> range(long from, long toExclusive) { return null; }
        @Independent @NotModified
        static TreeSet<Character> rangeBy(char from, char toExclusive, int step) { return null; }

        @Independent @NotModified
        static TreeSet<Double> rangeBy(double from, double toExclusive, double step) { return null; }
        @Independent @NotModified static TreeSet<Integer> rangeBy(int from, int toExclusive, int step) { return null; }
        @Independent @NotModified static TreeSet<Long> rangeBy(long from, long toExclusive, long step) { return null; }
        @Independent @NotModified static TreeSet<Character> rangeClosed(char from, char toInclusive) { return null; }
        @Independent @NotModified static TreeSet<Integer> rangeClosed(int from, int toInclusive) { return null; }
        @Independent @NotModified static TreeSet<Long> rangeClosed(long from, long toInclusive) { return null; }
        @Independent @NotModified
        static TreeSet<Character> rangeClosedBy(char from, char toInclusive, int step) { return null; }

        @Independent @NotModified
        static TreeSet<Double> rangeClosedBy(double from, double toInclusive, double step) { return null; }

        @Independent @NotModified
        static TreeSet<Integer> rangeClosedBy(int from, int toInclusive, int step) { return null; }

        @Independent @NotModified
        static TreeSet<Long> rangeClosedBy(long from, long toInclusive, long step) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> reject(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Independent @NotModified
        TreeSet<T> remove(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Fluent @Independent @NotModified
        TreeSet<T> removeAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        TreeSet<T> replace(
            @Independent(hc = true) @NotModified T currentElement,
            @Independent @NotModified T newElement) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        TreeSet<T> replaceAll(
            @Independent(hc = true) @NotModified T currentElement,
            @Independent @NotModified T newElement) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> retainAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        TreeSet<T> scan(
            @Independent @NotModified T zero,
            @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> operation) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.Set<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<TreeSet<T>> slideBy(@Independent @NotModified Function<? super T, ?> classifier) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<TreeSet<T>> sliding(int size) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<TreeSet<T>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified
        Tuple2<TreeSet<T>, TreeSet<T>> span(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        @Independent @NotModified
        static <T extends Comparable<? super T>> TreeSet<T> tabulate(
            int n,
            @Independent @NotModified Function<? super Integer, ? extends T> f) { return null; }

        @Independent @NotModified
        static <T> TreeSet<T> tabulate(
            @Independent @NotModified Comparator<? super T> comparator,
            int n,
            @Independent @NotModified Function<? super Integer, ? extends T> f) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeSet<T> tail() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        Option<TreeSet<T>> tailOption() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> take(int n) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> takeRight(int n) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> takeUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        TreeSet<T> takeWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value, io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Independent @NotModified
        java.util.TreeSet<T> toJavaSet() { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent @NotModified
        <U> U transform(@Independent @NotModified Function<? super TreeSet<T>, ? extends U> f) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet
        @Fluent @Independent @NotModified

        TreeSet<T> union(@Independent(hc = true) @NotModified io.vavr.collection.Set<? extends T> elements) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <T1, T2> Tuple2<TreeSet<T1>, TreeSet<T2>> unzip(
            @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent(hc = true) @NotModified

        <T1, T2, T3> Tuple3<TreeSet<T1>, TreeSet<T2>, TreeSet<T3>> unzip3(
            @Independent @NotModified Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> TreeSet<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> TreeSet<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified T thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> TreeSet<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified
        TreeSet<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.Set, io.vavr.collection.SortedSet, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> io.vavr.collection.SortedSet<U> zipWithIndex(
            @Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> mapper) { return null; }
    }

    //public final class Vector implements IndexedSeq<T>, Serializable
    //annotated as EXPECTED; computed @FinalFields @Dependent -- G1, G2: persistent collection (VAVR.md)
    @ImmutableContainer(hc = true)
    class Vector$<T> {
        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> append(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> appendAll(@Independent @NotModified Iterable<? extends T> iterable) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        java.util.List<T> asJava() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Vector<T> asJava(@Independent @NotModified Consumer<? super java.util.List<T>> action) { return null; }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        java.util.List<T> asJavaMutable() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Vector<T> asJavaMutable(@Independent @NotModified Consumer<? super java.util.List<T>> action) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <R> Vector<R> collect(@Independent @NotModified PartialFunction<? super T, ? extends R> partialFunction) {
            return null;
        }
        @Independent @NotModified static <T> Collector<T, ArrayList<T>, Vector<T>> collector() { return null; }
        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Vector<Vector<T>> combinations() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Vector<Vector<T>> combinations(int k) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Iterator<Vector<T>> crossProduct(int power) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> distinct() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> distinctBy(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        <U> Vector<T> distinctBy(@Independent @NotModified Function<? super T, ? extends U> keyExtractor) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Vector<T> distinctByKeepLast(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U> Vector<T> distinctByKeepLast(@Independent @NotModified Function<? super T, ? extends U> keyExtractor) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> drop(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> dropRight(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Vector<T> dropRightUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Vector<T> dropRightWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Vector<T> dropUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Vector<T> dropWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }
        @Independent @NotModified static <T> Vector<T> empty() { return null; }
        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object o) { return false; }

        @Independent @NotModified
        static <T> Vector<T> fill(int n, @Independent @NotModified T element) { return null; }

        @Independent @NotModified
        static <T> Vector<T> fill(int n, @Independent @NotModified Supplier<? extends T> s) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> filter(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Vector<U> flatMap(@Independent @NotModified Function<? super T, ? extends Iterable<? extends U>> mapper) {
            return null;
        }

        //override from io.vavr.collection.Seq
        @Independent @NotModified
        T get(int index) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <C> io.vavr.collection.Map<C, Vector<T>> groupBy(
            @Independent @NotModified Function<? super T, ? extends C> classifier) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Vector<T>> grouped(int size) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean hasDefiniteSize() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from io.vavr.collection.Traversable
        @Independent @NotModified
        T head() { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int indexOf(@Independent @NotModified T element, int from) { return 0; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> init() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Option<Vector<T>> initOption() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> insert(int index, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> insertAll(int index, @Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Vector<T> intersperse(@Independent @NotModified T element) { return null; }

        //override from io.vavr.Value
        @NotModified
        boolean isAsync() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable
        @NotModified
        boolean isEmpty() { return false; }

        //override from io.vavr.Value
        @NotModified
        boolean isLazy() { return false; }

        //override from io.vavr.collection.Traversable
        @NotModified
        boolean isTraversableAgain() { return false; }

        //override from io.vavr.Value, io.vavr.collection.Traversable, java.lang.Iterable
        @Independent @NotModified
        Iterator<T> iterator() { return null; }

        //override from io.vavr.collection.Seq
        @NotModified
        int lastIndexOf(@Independent @NotModified T element, int end) { return 0; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> leftPadTo(int length, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.Traversable
        @NotModified
        int length() { return 0; }

        //override from io.vavr.Value, io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Vector<U> map(@Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.Value, io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Vector<U> mapTo(@Independent @NotModified U value) { return null; }

        //override from io.vavr.Value, io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Vector<Void> mapToVoid() { return null; }

        @Identity @Independent @NotModified
        static <T> Vector<T> narrow(@Independent @NotModified Vector<? extends T> vector) { return null; }
        @Independent @NotModified static <T> Vector<T> of(@Independent @NotModified T element) { return null; }
        @Independent @NotModified static <T> Vector<T> of(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified
        static <T> Vector<T> ofAll(@Independent @NotModified Iterable<? extends T> iterable) { return null; }

        @Independent @NotModified
        static Vector<Boolean> ofAll(@Independent @NotModified boolean ... elements) { return null; }

        @Independent @NotModified
        static Vector<Byte> ofAll(@Independent @NotModified byte ... elements) { return null; }

        @Independent @NotModified
        static Vector<Character> ofAll(@Independent @NotModified char ... elements) { return null; }

        @Independent @NotModified
        static Vector<Double> ofAll(@Independent @NotModified double ... elements) { return null; }

        @Independent @NotModified
        static Vector<Float> ofAll(@Independent @NotModified float ... elements) { return null; }

        @Independent @NotModified
        static Vector<Integer> ofAll(@Independent @NotModified int ... elements) { return null; }

        @Independent @NotModified
        static <T> Vector<T> ofAll(@Independent @NotModified Stream<? extends T> javaStream) { return null; }

        @Independent @NotModified
        static Vector<Long> ofAll(@Independent @NotModified long ... elements) { return null; }

        @Independent @NotModified
        static Vector<Short> ofAll(@Independent @NotModified short ... elements) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> orElse(@Independent @NotModified Iterable<? extends T> other) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> orElse(@Independent @NotModified Supplier<? extends Iterable<? extends T>> supplier) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> padTo(int length, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Tuple2<Vector<T>, Vector<T>> partition(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> patch(int from, @Independent @NotModified Iterable<? extends T> that, int replaced) { return null; }

        //override from io.vavr.Value, io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> peek(@Independent @NotModified Consumer<? super T> action) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified
        Vector<Vector<T>> permutations() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> prepend(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> prependAll(@Independent @NotModified Iterable<? extends T> iterable) { return null; }
        @Independent @NotModified static Vector<Character> range(char from, char toExclusive) { return null; }
        @Independent @NotModified static Vector<Integer> range(int from, int toExclusive) { return null; }
        @Independent @NotModified static Vector<Long> range(long from, long toExclusive) { return null; }
        @Independent @NotModified
        static Vector<Character> rangeBy(char from, char toExclusive, int step) { return null; }

        @Independent @NotModified
        static Vector<Double> rangeBy(double from, double toExclusive, double step) { return null; }
        @Independent @NotModified static Vector<Integer> rangeBy(int from, int toExclusive, int step) { return null; }
        @Independent @NotModified static Vector<Long> rangeBy(long from, long toExclusive, long step) { return null; }
        @Independent @NotModified static Vector<Character> rangeClosed(char from, char toInclusive) { return null; }
        @Independent @NotModified static Vector<Integer> rangeClosed(int from, int toInclusive) { return null; }
        @Independent @NotModified static Vector<Long> rangeClosed(long from, long toInclusive) { return null; }
        @Independent @NotModified
        static Vector<Character> rangeClosedBy(char from, char toInclusive, int step) { return null; }

        @Independent @NotModified
        static Vector<Double> rangeClosedBy(double from, double toInclusive, double step) { return null; }

        @Independent @NotModified
        static Vector<Integer> rangeClosedBy(int from, int toInclusive, int step) { return null; }

        @Independent @NotModified
        static Vector<Long> rangeClosedBy(long from, long toInclusive, long step) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> reject(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> remove(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> removeAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> removeAll(@Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> removeAll(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> removeAt(int index) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> removeFirst(@Independent @NotModified Predicate<T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> removeLast(@Independent @NotModified Predicate<T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified

        Vector<T> replace(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Vector<T> replaceAll(@Independent @NotModified T currentElement, @Independent @NotModified T newElement) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> retainAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> reverse() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> rotateLeft(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> rotateRight(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        Vector<T> scan(
            @Independent @NotModified T zero,
            @Independent @NotModified BiFunction<? super T, ? super T, ? extends T> operation) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Vector<U> scanLeft(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super U, ? super T, ? extends U> operation) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Vector<U> scanRight(
            @Independent @NotModified U zero,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends U> operation) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> shuffle() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> slice(int beginIndex, int endIndex) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Vector<T>> slideBy(@Independent @NotModified Function<? super T, ?> classifier) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Vector<T>> sliding(int size) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Iterator<Vector<T>> sliding(int size, int step) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U> Vector<T> sortBy(
            @Independent @NotModified Comparator<? super U> comparator,
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Independent @NotModified

        <U extends Comparable<? super U>> Vector<T> sortBy(
            @Independent @NotModified Function<? super T, ? extends U> mapper) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> sorted() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> sorted(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Tuple2<Vector<T>, Vector<T>> span(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Tuple2<Vector<T>, Vector<T>> splitAt(int n) { return null; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Tuple2<Vector<T>, Vector<T>> splitAt(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.Seq
        @Fluent @Independent @NotModified

        Tuple2<Vector<T>, Vector<T>> splitAtInclusive(@Independent @NotModified Predicate<? super T> predicate) {
            return null;
        }

        //override from io.vavr.Value
        @NotModified
        String stringPrefix() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> subSequence(int beginIndex) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> subSequence(int beginIndex, int endIndex) { return null; }

        @Independent @NotModified
        static <T> Vector<T> tabulate(int n, @Independent @NotModified Function<? super Integer, ? extends T> f) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> tail() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Option<Vector<T>> tailOption() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> take(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> takeRight(int n) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> takeRightUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> takeRightWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> takeUntil(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Fluent @Independent @NotModified
        Vector<T> takeWhile(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from io.vavr.Value, java.lang.Object
        @NotModified
        public String toString() { return null; }

        @Independent @NotModified
        <U> U transform(@Independent @NotModified Function<? super Vector<T>, ? extends U> f) { return null; }

        @Identity @Independent @NotModified
        static <T> Vector<Vector<T>> transpose(@Independent @NotModified Vector<Vector<T>> matrix) { return null; }

        @Independent @NotModified
        static <T> Vector<T> unfold(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends T>>> f) { return null; }

        @Independent @NotModified
        static <T, U> Vector<U> unfoldLeft(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends T, ? extends U>>> f) { return null; }

        @Independent @NotModified
        static <T, U> Vector<U> unfoldRight(
            @Independent @NotModified T seed,
            @Independent @NotModified Function<? super T, Option<Tuple2<? extends U, ? extends T>>> f) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2> Tuple2<Vector<T1>, Vector<T2>> unzip(
            @Independent @NotModified Function<? super T, Tuple2<? extends T1, ? extends T2>> unzipper) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <T1, T2, T3> Tuple3<Vector<T1>, Vector<T2>, Vector<T3>> unzip3(
            @Independent @NotModified Function<? super T, Tuple3<? extends T1, ? extends T2, ? extends T3>> unzipper) {
            return null;
        }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> update(int index, @Independent @NotModified T element) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq
        @Fluent @Independent @NotModified
        Vector<T> update(int index, @Independent @NotModified Function<? super T, ? extends T> updater) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        <U> Vector<Tuple2<T, U>> zip(@Independent @NotModified Iterable<? extends U> that) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Vector<Tuple2<T, U>> zipAll(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified T thisElem,
            @Independent @NotModified U thatElem) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U, R> Vector<R> zipWith(
            @Independent @NotModified Iterable<? extends U> that,
            @Independent @NotModified BiFunction<? super T, ? super U, ? extends R> mapper) { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified
        Vector<Tuple2<T, Integer>> zipWithIndex() { return null; }

        //override from io.vavr.collection.IndexedSeq, io.vavr.collection.Seq, io.vavr.collection.Traversable
        @Independent @NotModified

        <U> Vector<U> zipWithIndex(@Independent @NotModified BiFunction<? super T, ? super Integer, ? extends U> mapper) {
            return null;
        }
    }
}
