package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.*;
import org.eclipse.collections.api.list.primitive.*;
import org.eclipse.collections.api.multimap.sortedset.ImmutableSortedSetMultimap;
import org.eclipse.collections.api.multimap.sortedset.MutableSortedSetMultimap;
import org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap;
import org.eclipse.collections.api.partition.set.sorted.PartitionImmutableSortedSet;
import org.eclipse.collections.api.partition.set.sorted.PartitionMutableSortedSet;
import org.eclipse.collections.api.partition.set.sorted.PartitionSortedSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.set.sorted.*;
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
public class OrgEclipseCollectionsApiSetSorted {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.set.sorted";
    //public interface ImmutableSortedSet implements SortedSetIterable<T>, ImmutableSetIterable<T>
    //annotated as EXPECTED; computed mutable @Independent(hc = true) (conditional) (1) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableSortedSet$<T> {
        @Independent @NotModified SortedSet<T> castToSortedSet() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        <V> ImmutableList<V> collect(@Independent @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableBooleanList collectBoolean(@Independent @NotModified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableByteList collectByte(@Independent @NotModified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableCharList collectChar(@Independent @NotModified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableDoubleList collectDouble(@Independent @NotModified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableFloatList collectFloat(@Independent @NotModified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified

        <V> ImmutableList<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableIntList collectInt(@Independent @NotModified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableLongList collectLong(@Independent @NotModified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableShortList collectShort(@Independent @NotModified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified

        <P, V> ImmutableList<V> collectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified

        <V> ImmutableList<V> collectWithIndex(
            @Independent @NotModified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> difference(@Independent @NotModified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> distinct() { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> dropWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified

        <V> ImmutableList<V> flatCollect(@Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified

        <P, V> ImmutableList<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified

        <V> ImmutableSortedSetMultimap<V, T> groupBy(@Independent @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified

        <V> ImmutableSortedSetMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> intersect(@Independent @NotModified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSortedSet<T> newWith(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSortedSet<T> newWithAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSortedSet<T> newWithout(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSortedSet<T> newWithoutAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        PartitionImmutableSortedSet<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified

        PartitionImmutableSortedSet<T> partitionWhile(@Independent @NotModified Predicate<? super T> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified

        <P> PartitionImmutableSortedSet<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<SortedSetIterable<T>> powerSet() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified

        <P> ImmutableSortedSet<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        <S> ImmutableSortedSet<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified

        <P> ImmutableSortedSet<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified

        ImmutableSortedSet<T> symmetricDifference(@Independent @NotModified SetIterable<? extends T> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> takeWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Fluent @Independent @NotModified
        ImmutableSortedSet<T> toImmutableSortedSet() { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> toReversed() { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> union(@Independent @NotModified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        <S> ImmutableList<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface MutableSortedSet implements MutableSetIterable<T>, SortedSetIterable<T>, SortedSet<T>, Cloneable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableSortedSet$<T> {
        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableSortedSet<T> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableSortedSet<T> asUnmodifiable() { return null; }
        @Independent @Modified protected MutableSortedSet<T> clone() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified

        <V> MutableList<V> collect(@Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified
        MutableBooleanList collectBoolean(@Independent @NotModified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified
        MutableByteList collectByte(@Independent @NotModified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified
        MutableCharList collectChar(@Independent @NotModified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified
        MutableDoubleList collectDouble(@Independent @NotModified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified
        MutableFloatList collectFloat(@Independent @NotModified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified

        <V> MutableList<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified
        MutableIntList collectInt(@Independent @NotModified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified
        MutableLongList collectLong(@Independent @NotModified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified
        MutableShortList collectShort(@Independent @NotModified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified

        <P, V> MutableList<V> collectWith(
            @Independent(hc = true) @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified

        <V> MutableList<V> collectWithIndex(
            @Independent @NotModified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified

        MutableSortedSet<T> difference(@Independent(hc = true) @NotModified SetIterable<? extends T> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified
        MutableSortedSet<T> distinct() { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent(hc = true) @NotModified
        MutableSortedSet<T> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        MutableSortedSet<T> dropWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified

        <V> MutableList<V> flatCollect(
            @Independent(hc = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified

        <P, V> MutableList<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from java.util.SequencedCollection, java.util.SortedSet, org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @NotModified
        T getFirst() { return null; }

        //override from java.util.SequencedCollection, java.util.SortedSet, org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @NotModified
        T getLast() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent(hc = true) @Modified

        <V> MutableSortedSetMultimap<V, T> groupBy(
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent(hc = true) @Modified

        <V> MutableSortedSetMultimap<V, T> groupByEach(
            @Independent(hc = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from java.util.SortedSet
        @Independent(hc = true) @Modified
        MutableSortedSet<T> headSet(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified
        MutableSortedSet<T> intersect(@Independent(hc = true) @NotModified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @NotModified
        MutableSortedSet<T> newEmpty() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent(hc = true) @Modified
        PartitionMutableSortedSet<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        PartitionMutableSortedSet<T> partitionWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent(hc = true) @Modified

        <P> PartitionMutableSortedSet<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        MutableSortedSet<SortedSetIterable<T>> powerSet() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent(hc = true) @Modified
        MutableSortedSet<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent(hc = true) @Modified

        <P> MutableSortedSet<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent(hc = true) @Modified
        MutableSortedSet<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified
        <S> MutableSortedSet<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent(hc = true) @Modified

        <P> MutableSortedSet<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from java.util.SortedSet
        @Independent(hc = true) @Modified
        MutableSortedSet<T> subSet(@Independent @NotModified T arg0, @Independent @NotModified T arg1) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified

        MutableSortedSet<T> symmetricDifference(@Independent(hc = true) @NotModified SetIterable<? extends T> arg0) {
            return null;
        }

        //override from java.util.SortedSet
        @Independent(hc = true) @Modified
        MutableSortedSet<T> tailSet(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent(hc = true) @NotModified
        MutableSortedSet<T> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        MutableSortedSet<T> takeWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @Modified
        MutableSortedSet<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> toImmutable() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableSortedSet<T> toImmutableSortedSet() { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        MutableSortedSet<T> toReversed() { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent @NotModified
        MutableSortedSet<T> union(@Independent(hc = true) @NotModified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable
        @Fluent @Independent @Modified
        MutableSortedSet<T> with(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable
        @Fluent @Independent @Modified
        MutableSortedSet<T> withAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable
        @Fluent @Independent @Modified
        MutableSortedSet<T> without(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable
        @Fluent @Independent @Modified
        MutableSortedSet<T> withoutAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.MutableSetIterable
        @Independent(hc = true) @Modified
        <S> MutableList<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.sorted.SortedSetIterable
        @Independent(hc = true) @Modified
        MutableSortedSet<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface ParallelSortedSetIterable implements ParallelSetIterable<T>
    @FinalFields
    @Independent(hc = true)
    class ParallelSortedSetIterable$<T> {
        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent @NotModified
        ParallelSortedSetIterable<T> asUnique() { return null; }

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
        @Independent(hc = true) @NotModified Comparator<? super T> comparator() { return null; }
        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <V> ParallelListIterable<V> flatCollect(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent(hc = true) @Modified
        <V> SortedSetMultimap<V, T> groupBy(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent(hc = true) @Modified

        <V> SortedSetMultimap<V, T> groupByEach(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent @Modified
        ParallelSortedSetIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent @Modified

        <P> ParallelSortedSetIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent @Modified
        ParallelSortedSetIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent @Modified
        <S> ParallelSortedSetIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent @Modified

        <P> ParallelSortedSetIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
    }

    //public interface SortedSetIterable implements SetIterable<T>, Comparable<SortedSetIterable<T>>, SortedIterable<T>, ReversibleIterable<T>
    @Independent(hc = true)
    class SortedSetIterable$<T> {
        //override from org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified
        ParallelSortedSetIterable<T> asParallel(@Independent @Modified ExecutorService arg0, int arg1) { return null; }

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

        //override from org.eclipse.collections.api.ordered.SortedIterable
        @Independent(hc = true) @NotModified
        Comparator<? super T> comparator() { return null; }

        //override from org.eclipse.collections.api.set.SetIterable
        @Independent @Modified
        SortedSetIterable<T> difference(@Independent(hc = true) @Modified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @Modified
        SortedSetIterable<T> distinct() { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @NotModified
        SortedSetIterable<T> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        SortedSetIterable<T> dropWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V> ListIterable<V> flatCollect(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V> ListIterable<V> flatCollectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @Modified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent(hc = true) @Modified

        <V> SortedSetMultimap<V, T> groupBy(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent(hc = true) @Modified

        <V> SortedSetMultimap<V, T> groupByEach(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable
        @Independent @Modified
        SortedSetIterable<T> intersect(@Independent(hc = true) @Modified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(hc = true) @Modified
        PartitionSortedSet<T> partition(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        PartitionSortedSet<T> partitionWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(hc = true) @Modified

        <P> PartitionSortedSet<T> partitionWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @Independent @Modified SortedSetIterable<SortedSetIterable<T>> powerSet() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(hc = true) @Modified
        SortedSetIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(hc = true) @Modified

        <P> SortedSetIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(hc = true) @Modified
        SortedSetIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.SetIterable
        @Independent @Modified
        <S> SortedSetIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(hc = true) @Modified

        <P> SortedSetIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable
        @Independent @Modified

        SortedSetIterable<T> symmetricDifference(@Independent(hc = true) @Modified SetIterable<? extends T> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @NotModified
        SortedSetIterable<T> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable
        @Independent @NotModified
        SortedSetIterable<T> takeWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.SetIterable
        @Independent @Modified
        SortedSetIterable<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified
        ImmutableSortedSet<T> toImmutable() { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        SortedSetIterable<T> toReversed() { return null; }

        //override from org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified
        SortedSetIterable<T> union(@Independent(hc = true) @Modified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable, org.eclipse.collections.api.ordered.SortedIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(hc = true) @Modified
        SortedSetIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }
}
