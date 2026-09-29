package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.*;
import java.util.concurrent.ExecutorService;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.*;
import org.eclipse.collections.api.list.primitive.*;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.partition.list.PartitionImmutableList;
import org.eclipse.collections.api.partition.list.PartitionList;
import org.eclipse.collections.api.partition.list.PartitionMutableList;
import org.eclipse.collections.api.set.ParallelUnsortedSetIterable;
import org.eclipse.collections.api.tuple.Pair;
import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.FinalFields;
import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.rare.IgnoreModifications;
@Independent(absent = true)
public class OrgEclipseCollectionsApiList {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.list";
    //public interface FixedSizeList implements MutableList<T>, FixedSizeCollection<T>
    @Independent(absent = true)
    class FixedSizeList$<T> {
        //override from org.eclipse.collections.api.list.MutableList
        @Fluent @Independent @Modified
        FixedSizeList<T> sortThis() { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Fluent @Independent @Modified
        FixedSizeList<T> sortThis(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.FixedSizeCollection, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.list.MutableList, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Fluent @Independent @Modified @NotNull
        FixedSizeList<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.list.MutableList, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        FixedSizeList<T> toReversed() { return null; }

        //override from org.eclipse.collections.api.collection.FixedSizeCollection, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.MutableList
        @Independent(hc = true) @Modified
        MutableList<T> with(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.FixedSizeCollection, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MutableList<T> withAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.FixedSizeCollection, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MutableList<T> without(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.FixedSizeCollection, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MutableList<T> withoutAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }
    }

    //public interface ImmutableList implements ImmutableCollection<T>, ListIterable<T>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableList$<T> {
        @Independent @NotModified List<T> castToList() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V> ImmutableList<V> collect(@Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableBooleanList collectBoolean(@Independent @NotModified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableByteList collectByte(@Independent @NotModified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableCharList collectChar(@Independent @NotModified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableDoubleList collectDouble(@Independent @NotModified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableFloatList collectFloat(@Independent @NotModified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V> ImmutableList<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableIntList collectInt(@Independent @NotModified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableLongList collectLong(@Independent @NotModified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableShortList collectShort(@Independent @NotModified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P, V> ImmutableList<V> collectWith(
            @Independent(hc = true) @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V> ImmutableList<V> collectWithIndex(
            @Independent @NotModified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<T> distinct() { return null; }

        //override from org.eclipse.collections.api.list.ListIterable
        @Independent @NotModified
        ImmutableList<T> distinct(@Independent @NotModified HashingStrategy<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable
        @Independent @NotModified
        <V> ImmutableList<T> distinctBy(@Independent @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<T> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<T> dropWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V> ImmutableList<V> flatCollect(
            @Independent(hc = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P, V> ImmutableList<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V> ImmutableListMultimap<V, T> groupBy(@Independent @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V> ImmutableListMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableList<T> newWith(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableList<T> newWithAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableList<T> newWithout(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableList<T> newWithoutAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        PartitionImmutableList<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        PartitionImmutableList<T> partitionWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P> PartitionImmutableList<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P> ImmutableList<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable
        @Independent @NotModified

        ImmutableList<T> rejectWithIndex(@Independent @NotModified ObjectIntPredicate<? super T> predicate) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        <S> ImmutableList<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P> ImmutableList<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable
        @Independent @NotModified

        ImmutableList<T> selectWithIndex(@Independent @NotModified ObjectIntPredicate<? super T> predicate) {
            return null;
        }

        //override from org.eclipse.collections.api.list.ListIterable
        @Independent(hc = true) @NotModified
        ImmutableList<T> subList(int arg0, int arg1) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<T> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<T> takeWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Fluent @Independent @NotModified
        ImmutableList<T> toImmutableList() { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<T> toReversed() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        <S> ImmutableList<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface ListIterable implements ReversibleIterable<T>
    @Independent(absent = true)
    class ListIterable$<T> {
        @Independent @Modified
        ParallelListIterable<T> asParallel(@Independent @NotModified ExecutorService arg0, int arg1) { return null; }
        @Modified int binarySearch(@Independent @NotModified T key) { return 0; }
        @Modified
        int binarySearch(@Independent @NotModified T key, @Independent @NotModified Comparator<? super T> comparator) {
            return 0;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> ListIterable<V> collect(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        BooleanList collectBoolean(@Independent @Modified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        ByteList collectByte(@Independent @Modified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        CharList collectChar(@Independent @Modified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        DoubleList collectDouble(@Independent @Modified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        FloatList collectFloat(@Independent @Modified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> ListIterable<V> collectIf(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        IntList collectInt(@Independent @Modified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        LongList collectLong(@Independent @Modified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        ShortList collectShort(@Independent @Modified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V> ListIterable<V> collectWith(
            @Independent(hc = true) @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @Modified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> ListIterable<V> collectWithIndex(
            @Independent @Modified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        ListIterable<T> distinct() { return null; }

        @Independent(hc = true) @Modified
        ListIterable<T> distinct(@Independent @Modified HashingStrategy<? super T> arg0) { return null; }

        @Independent(hc = true) @Modified
        <V> ListIterable<T> distinctBy(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        ListIterable<T> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        ListIterable<T> dropWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object arg0) { return false; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> ListIterable<V> flatCollect(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V> ListIterable<V> flatCollectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @Modified P parameter) { return null; }

        @Modified
        <T2> void forEachInBoth(
            @Independent @Modified ListIterable<T2> other,
            @Independent @Modified Procedure2<? super T, ? super T2> procedure) { }
        @Independent(hc = true) @NotModified T get(int arg0) { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @NotModified
        T getFirst() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @NotModified
        T getLast() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <V> ListMultimap<V, T> groupBy(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <V> ListMultimap<V, T> groupByEach(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }
        @NotModified int lastIndexOf(@Independent @NotModified Object arg0) { return 0; }
        @Independent(absent = true) @Modified ListIterator<T> listIterator() { return null; }
        @Independent(absent = true) @Modified ListIterator<T> listIterator(int arg0) { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        PartitionList<T> partition(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        PartitionList<T> partitionWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <P> PartitionList<T> partitionWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        ListIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <P> ListIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        @Independent @Modified
        ListIterable<T> rejectWithIndex(@Independent @Modified ObjectIntPredicate<? super T> predicate) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        ListIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        <S> ListIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <P> ListIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        @Independent @Modified
        ListIterable<T> selectWithIndex(@Independent @Modified ObjectIntPredicate<? super T> predicate) { return null; }
        @Independent(absent = true) @Modified ListIterable<T> subList(int arg0, int arg1) { return null; }
        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        ListIterable<T> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        ListIterable<T> takeWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        ListIterable<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }
        @Independent @Modified ImmutableList<T> toImmutable() { return null; }
        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        ListIterable<T> toReversed() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        <S> ListIterable<Pair<T, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        ListIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface MultiReaderList implements MutableList<T>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection behind a lock (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MultiReaderList$<T> {
        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        protected MultiReaderList<T> clone() { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.MutableList
        @Independent @NotModified
        MultiReaderList<T> newEmpty() { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MultiReaderList<T> reverseThis() { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MultiReaderList<T> shuffleThis() { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MultiReaderList<T> shuffleThis(@Independent @NotModified Random arg0) { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Fluent @Independent @Modified
        MultiReaderList<T> sortThis() { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Fluent @Independent @Modified
        MultiReaderList<T> sortThis(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified

        <V extends Comparable<? super V>> MultiReaderList<T> sortThisBy(
            @Independent @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MultiReaderList<T> sortThisByBoolean(@Independent @NotModified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MultiReaderList<T> sortThisByByte(@Independent @NotModified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MultiReaderList<T> sortThisByChar(@Independent @NotModified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MultiReaderList<T> sortThisByDouble(@Independent @NotModified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MultiReaderList<T> sortThisByFloat(@Independent @NotModified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MultiReaderList<T> sortThisByInt(@Independent @NotModified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MultiReaderList<T> sortThisByLong(@Independent @NotModified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.list.MutableList
        @Independent @Modified
        MultiReaderList<T> sortThisByShort(@Independent @NotModified ShortFunction<? super T> arg0) { return null; }

        //override from java.util.List, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.list.MutableList
        @Independent(absent = true) @Modified
        MultiReaderList<T> subList(int arg0, int arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.list.MutableList, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Fluent @Independent @Modified
        MultiReaderList<T> tap(@Independent @NotModified Procedure<? super T> procedure) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.MutableList
        @Fluent @Independent @Modified
        MultiReaderList<T> with(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.MutableList
        @Fluent @Independent @Modified
        MultiReaderList<T> withAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }
        @Modified void withReadLockAndDelegate(@Independent @NotModified Procedure<? super MutableList<T>> arg0) { }
        @Modified void withWriteLockAndDelegate(@Independent @NotModified Procedure<? super MutableList<T>> arg0) { }
        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.MutableList
        @Fluent @Independent @Modified
        MultiReaderList<T> without(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.MutableList
        @Fluent @Independent @Modified
        MultiReaderList<T> withoutAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }
    }

    //public interface MutableList implements MutableCollection<T>, List<T>, Cloneable, ListIterable<T>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableList$<T> {
        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableList<T> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableList<T> asUnmodifiable() { return null; }
        @Independent(hc = true) @Modified protected MutableList<T> clone() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        <V> MutableList<V> collect(@Independent @NotModified Function<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        MutableBooleanList collectBoolean(@Independent @NotModified BooleanFunction<? super T> booleanFunction) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableByteList collectByte(@Independent @NotModified ByteFunction<? super T> byteFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableCharList collectChar(@Independent @NotModified CharFunction<? super T> charFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        MutableDoubleList collectDouble(@Independent @NotModified DoubleFunction<? super T> doubleFunction) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableFloatList collectFloat(@Independent @NotModified FloatFunction<? super T> floatFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> MutableList<V> collectIf(
            @Independent @NotModified Predicate<? super T> predicate,
            @Independent @NotModified Function<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableIntList collectInt(@Independent @NotModified IntFunction<? super T> intFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableLongList collectLong(@Independent @NotModified LongFunction<? super T> longFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableShortList collectShort(@Independent @NotModified ShortFunction<? super T> shortFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V> MutableList<V> collectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> MutableList<V> collectWithIndex(
            @Independent @NotModified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        MutableList<T> distinct() { return null; }

        //override from org.eclipse.collections.api.list.ListIterable
        @Independent(hc = true) @Modified
        MutableList<T> distinct(@Independent @NotModified HashingStrategy<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable
        @Independent(hc = true) @Modified
        <V> MutableList<T> distinctBy(@Independent @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableList<T> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        MutableList<T> dropWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> MutableList<V> flatCollect(@Independent @NotModified Function<? super T, ? extends Iterable<V>> function) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V> MutableList<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from java.util.List, java.util.SequencedCollection, org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @NotModified
        T getFirst() { return null; }

        //override from java.util.List, java.util.SequencedCollection, org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @NotModified
        T getLast() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <V> MutableListMultimap<V, T> groupBy(
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <V> MutableListMultimap<V, T> groupByEach(
            @Independent(hc = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from java.util.List, org.eclipse.collections.api.ordered.OrderedIterable
        @NotModified
        int indexOf(@Independent @NotModified Object o) { return 0; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @NotModified
        MutableList<T> newEmpty() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        PartitionMutableList<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        PartitionMutableList<T> partitionWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <P> PartitionMutableList<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableList<T> reject(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P> MutableList<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> predicate,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable
        @Independent @Modified
        MutableList<T> rejectWithIndex(@Independent @NotModified ObjectIntPredicate<? super T> predicate) { return null; }
        @Fluent @Independent @Modified MutableList<T> reverseThis() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableList<T> select(@Independent @NotModified Predicate<? super T> predicate) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        <S> MutableList<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P> MutableList<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> predicate,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable
        @Independent @Modified
        MutableList<T> selectWithIndex(@Independent @NotModified ObjectIntPredicate<? super T> predicate) { return null; }
        @Fluent @Independent @Modified MutableList<T> shuffleThis() { return null; }
        @Fluent @Independent @Modified
        MutableList<T> shuffleThis(@Independent @NotModified Random random) { return null; }
        @Fluent @Independent @Modified MutableList<T> sortThis() { return null; }
        @Fluent @Independent @Modified
        MutableList<T> sortThis(@Independent @NotModified Comparator<? super T> comparator) { return null; }

        @Fluent @Independent @Modified
        <V extends Comparable<? super V>> MutableList<T> sortThisBy(
            @Independent @NotModified Function<? super T, ? extends V> function) { return null; }

        @Independent @Modified
        MutableList<T> sortThisByBoolean(@Independent @NotModified BooleanFunction<? super T> arg0) { return null; }

        @Independent @Modified
        MutableList<T> sortThisByByte(@Independent @NotModified ByteFunction<? super T> arg0) { return null; }

        @Independent @Modified
        MutableList<T> sortThisByChar(@Independent @NotModified CharFunction<? super T> arg0) { return null; }

        @Independent @Modified
        MutableList<T> sortThisByDouble(@Independent @NotModified DoubleFunction<? super T> arg0) { return null; }

        @Independent @Modified
        MutableList<T> sortThisByFloat(@Independent @NotModified FloatFunction<? super T> arg0) { return null; }

        @Independent @Modified
        MutableList<T> sortThisByInt(@Independent @NotModified IntFunction<? super T> arg0) { return null; }

        @Independent @Modified
        MutableList<T> sortThisByLong(@Independent @NotModified LongFunction<? super T> arg0) { return null; }

        @Independent @Modified
        MutableList<T> sortThisByShort(@Independent @NotModified ShortFunction<? super T> arg0) { return null; }

        //override from java.util.List, org.eclipse.collections.api.list.ListIterable
        @Independent(absent = true) @Modified
        MutableList<T> subList(int arg0, int arg1) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        MutableList<T> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        MutableList<T> takeWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Fluent @Independent @Modified
        MutableList<T> tap(@Independent @NotModified Procedure<? super T> procedure) { return null; }

        //override from java.util.Collection, java.util.List, org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection
        @Independent @NotModified
        Object [] toArray() { return null; }

        //override from java.util.Collection, java.util.List, org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection
        @Independent @NotModified
        <T1> T1 [] toArray(@Independent @NotModified T1 [] a) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable
        @Independent @Modified
        ImmutableList<T> toImmutable() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ImmutableList<T> toImmutableList() { return null; }

        //override from org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Fluent @Independent @Modified
        MutableList<T> toReversed() { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableList<T> with(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableList<T> withAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableList<T> without(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableList<T> withoutAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        <S> MutableList<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.list.ListIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        MutableList<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface ParallelListIterable implements ParallelIterable<T>
    @FinalFields
    @Independent(hc = true)
    class ParallelListIterable$<T> {
        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        ParallelUnsortedSetIterable<T> asUnique() { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        <V> ParallelListIterable<V> collect(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <V> ParallelListIterable<V> collectIf(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent @Modified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <P, V> ParallelListIterable<V> collectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @Modified P arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <V> ParallelListIterable<V> flatCollect(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent(hc = true) @Modified
        <V> ListMultimap<V, T> groupBy(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent(hc = true) @Modified

        <V> ListMultimap<V, T> groupByEach(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        ParallelListIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <P> ParallelListIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        ParallelListIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        <S> ParallelListIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <P> ParallelListIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
    }
}
