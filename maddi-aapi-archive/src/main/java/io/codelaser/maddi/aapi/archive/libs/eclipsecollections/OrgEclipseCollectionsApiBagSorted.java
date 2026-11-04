package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Comparator;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.sorted.*;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.*;
import org.eclipse.collections.api.list.primitive.*;
import org.eclipse.collections.api.map.sorted.MutableSortedMap;
import org.eclipse.collections.api.map.sorted.SortedMapIterable;
import org.eclipse.collections.api.multimap.sortedbag.ImmutableSortedBagMultimap;
import org.eclipse.collections.api.multimap.sortedbag.MutableSortedBagMultimap;
import org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap;
import org.eclipse.collections.api.partition.bag.sorted.PartitionImmutableSortedBag;
import org.eclipse.collections.api.partition.bag.sorted.PartitionMutableSortedBag;
import org.eclipse.collections.api.partition.bag.sorted.PartitionSortedBag;
import org.eclipse.collections.api.set.sorted.ImmutableSortedSet;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.api.set.sorted.SortedSetIterable;
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
public class OrgEclipseCollectionsApiBagSorted {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.bag.sorted";
    //public interface ImmutableSortedBag implements ImmutableBagIterable<T>, SortedBag<T>
    //annotated as EXPECTED; computed mutable @Independent(hc = true) (conditional) (1) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableSortedBag$<T> {
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        <V> ImmutableList<V> collect(@Independent @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableBooleanList collectBoolean(@Independent @NotModified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableByteList collectByte(@Independent @NotModified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableCharList collectChar(@Independent @NotModified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableDoubleList collectDouble(@Independent @NotModified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableFloatList collectFloat(@Independent @NotModified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V> ImmutableList<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableIntList collectInt(@Independent @NotModified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableLongList collectLong(@Independent @NotModified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableShortList collectShort(@Independent @NotModified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P, V> ImmutableList<V> collectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V> ImmutableList<V> collectWithIndex(
            @Independent @NotModified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag
        @Independent @NotModified

        <V> ImmutableList<V> collectWithOccurrences(
            @Independent @NotModified ObjectIntToObjectFunction<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        <V> ImmutableBag<V> countBy(@Independent @NotModified Function<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <V> ImmutableBag<V> countByEach(@Independent @NotModified Function<? super T, ? extends Iterable<V>> function) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <V, P> ImmutableBag<V> countByWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        ImmutableSortedSet<T> distinct() { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableSortedBag<T> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        ImmutableSortedBag<T> dropWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V> ImmutableList<V> flatCollect(@Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P, V> ImmutableList<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent(hc = true) @NotModified

        <V> ImmutableSortedBagMultimap<V, T> groupBy(@Independent @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent(hc = true) @NotModified

        <V> ImmutableSortedBagMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent(hc = true) @NotModified
        ImmutableSortedBag<T> newWith(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSortedBag<T> newWithAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSortedBag<T> newWithout(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSortedBag<T> newWithoutAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        PartitionImmutableSortedBag<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P> PartitionImmutableSortedBag<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        ImmutableSortedBag<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified

        <P> ImmutableSortedBag<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        ImmutableSortedBag<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag
        @Independent @NotModified
        ImmutableSortedBag<T> selectByOccurrences(@Independent @NotModified IntPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag
        @Independent @NotModified
        ImmutableSortedBag<T> selectDuplicates() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        <S> ImmutableSortedBag<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag
        @Independent @NotModified
        ImmutableSortedSet<T> selectUnique() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified

        <P> ImmutableSortedBag<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableSortedBag<T> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        ImmutableSortedBag<T> takeWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        ImmutableSortedBag<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Fluent @Independent @NotModified
        ImmutableSortedBag<T> toImmutableSortedBag() { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag
        @Independent @NotModified
        MutableSortedMap<T, Integer> toMapOfItemToCount() { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableSortedBag<T> toReversed() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        <S> ImmutableList<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        ImmutableSortedSet<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface MutableSortedBag implements SortedBag<T>, MutableBagIterable<T>, Cloneable
    //annotated as EXPECTED; computed mutable @Independent(hc = true) (conditional) (1) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(hc = true)
    class MutableSortedBag$<T> {
        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @NotModified
        MutableSortedBag<T> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @NotModified
        MutableSortedBag<T> asUnmodifiable() { return null; }
        @Independent @NotModified protected MutableSortedBag<T> clone() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        <V> MutableList<V> collect(@Independent @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableBooleanList collectBoolean(@Independent @NotModified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableByteList collectByte(@Independent @NotModified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableCharList collectChar(@Independent @NotModified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableDoubleList collectDouble(@Independent @NotModified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableFloatList collectFloat(@Independent @NotModified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> MutableList<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableIntList collectInt(@Independent @NotModified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableLongList collectLong(@Independent @NotModified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableShortList collectShort(@Independent @NotModified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V> MutableList<V> collectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> MutableList<V> collectWithIndex(
            @Independent @NotModified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag
        @Independent @Modified

        <V> MutableList<V> collectWithOccurrences(
            @Independent @NotModified ObjectIntToObjectFunction<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        <V> MutableBag<V> countBy(@Independent @NotModified Function<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified

        <V> MutableBag<V> countByEach(@Independent @NotModified Function<? super T, ? extends Iterable<V>> function) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified

        <V, P> MutableBag<V> countByWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        MutableSortedSet<T> distinct() { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        MutableSortedBag<T> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        MutableSortedBag<T> dropWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> MutableList<V> flatCollect(@Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V> MutableList<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified

        <V> MutableSortedBagMultimap<V, T> groupBy(@Independent @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified

        <V> MutableSortedBagMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @NotModified
        MutableSortedBag<T> newEmpty() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        PartitionMutableSortedBag<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        PartitionMutableSortedBag<T> partitionWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P> PartitionMutableSortedBag<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        MutableSortedBag<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified

        <P> MutableSortedBag<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        MutableSortedBag<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag
        @Independent @NotModified
        MutableSortedBag<T> selectByOccurrences(@Independent @NotModified IntPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag
        @Independent @NotModified
        MutableSortedBag<T> selectDuplicates() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        <S> MutableSortedBag<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag
        @Independent @Modified
        MutableSortedSet<T> selectUnique() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified

        <P> MutableSortedBag<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        MutableSortedBag<T> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        MutableSortedBag<T> takeWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        MutableSortedBag<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ImmutableSortedBag<T> toImmutableSortedBag() { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag
        @Independent @Modified
        MutableSortedMap<T, Integer> toMapOfItemToCount() { return null; }

        //override from org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        MutableSortedBag<T> toReversed() { return null; }

        //override from org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableSortedBag<T> with(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableSortedBag<T> withAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBagIterable
        @Fluent @Independent @Modified
        MutableSortedBag<T> withOccurrences(@Independent @NotModified T element, int occurrences) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableSortedBag<T> without(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableSortedBag<T> withoutAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBagIterable
        @Fluent @Independent @Modified
        MutableSortedBag<T> withoutOccurrences(@Independent @NotModified T element, int occurrences) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        <S> MutableList<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.sorted.SortedBag, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        MutableSortedSet<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface ParallelSortedBag implements ParallelBag<T>
    @FinalFields
    @Container
    @Independent(absent = true)
    class ParallelSortedBag$<T> {
        //override from org.eclipse.collections.api.ParallelIterable
        @Independent(absent = true) @NotModified

        <V> ParallelListIterable<V> collect(
            @Independent(absent = true) @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent(absent = true) @NotModified

        <V> ParallelListIterable<V> collectIf(
            @Independent(absent = true) @NotModified Predicate<? super T> arg0,
            @Independent(absent = true) @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent(absent = true) @NotModified

        <P, V> ParallelListIterable<V> collectWith(
            @Independent(absent = true) @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent(absent = true) @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent(absent = true) @NotModified

        <V> ParallelListIterable<V> flatCollect(
            @Independent(absent = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent(absent = true) @NotModified

        <V> SortedBagMultimap<V, T> groupBy(
            @Independent(absent = true) @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent(absent = true) @NotModified

        <V> SortedBagMultimap<V, T> groupByEach(
            @Independent(absent = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent(absent = true) @NotModified
        ParallelSortedBag<T> reject(@Independent(absent = true) @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent(absent = true) @NotModified

        <P> ParallelSortedBag<T> rejectWith(
            @Independent(absent = true) @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent(absent = true) @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent(absent = true) @NotModified
        ParallelSortedBag<T> select(@Independent(absent = true) @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent(absent = true) @NotModified
        <S> ParallelSortedBag<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent(absent = true) @NotModified

        <P> ParallelSortedBag<T> selectWith(
            @Independent(absent = true) @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent(absent = true) @NotModified P arg1) { return null; }
    }

    //public interface SortedBag implements Bag<T>, Comparable<SortedBag<T>>, SortedIterable<T>, ReversibleIterable<T>
    @Independent(hc = true)
    class SortedBag$<T> {
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        <V> ListIterable<V> collect(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

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
            @Independent @Modified Function<? super T, ? extends V> arg1) { return null; }

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
            @Independent @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @Modified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> ListIterable<V> collectWithIndex(
            @Independent @Modified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified

        <V> ListIterable<V> collectWithOccurrences(
            @Independent @Modified ObjectIntToObjectFunction<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.SortedIterable
        @Independent(hc = true) @NotModified
        Comparator<? super T> comparator() { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        SortedSetIterable<T> distinct() { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        SortedBag<T> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        SortedBag<T> dropWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> ListIterable<V> flatCollect(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V> ListIterable<V> flatCollectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @Modified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent(hc = true) @Modified
        <V> SortedBagMultimap<V, T> groupBy(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent(hc = true) @Modified

        <V> SortedBagMultimap<V, T> groupByEach(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent(hc = true) @NotModified
        T max() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent(hc = true) @NotModified
        T min() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        PartitionSortedBag<T> partition(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        PartitionSortedBag<T> partitionWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P> PartitionSortedBag<T> partitionWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        SortedBag<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified

        <P> SortedBag<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        SortedBag<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        SortedBag<T> selectByOccurrences(@Independent @NotModified IntPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        SortedBag<T> selectDuplicates() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        <S> SortedBag<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        SortedSetIterable<T> selectUnique() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified

        <P> SortedBag<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        SortedBag<T> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        SortedBag<T> takeWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        SortedBag<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        ImmutableSortedBag<T> toImmutable() { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        SortedMapIterable<T, Integer> toMapOfItemToCount() { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        SortedBag<T> toReversed() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        SortedSetIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }
}
