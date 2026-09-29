package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.*;
import org.eclipse.collections.api.bag.primitive.*;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.MutableMapIterable;
import org.eclipse.collections.api.multimap.bag.*;
import org.eclipse.collections.api.partition.bag.*;
import org.eclipse.collections.api.set.*;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
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
public class OrgEclipseCollectionsApiBag {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.bag";
    //public interface Bag implements RichIterable<T>
    @Independent(absent = true)
    class Bag$<T> {
        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <K, V, R extends MutableMapIterable<K, V>> R aggregateBy(
            @Independent @Modified Function<? super T, ? extends K> groupBy,
            @Independent @Modified Function0<? extends V> zeroValueFactory,
            @Independent @Modified Function2<? super V, ? super T, ? extends V> nonMutatingAggregator,
            @Independent @Modified R target) { return null; }

        @Modified
        boolean allSatisfyWithOccurrences(@Independent @Modified ObjectIntPredicate<? super T> arg0) { return false; }

        @Modified
        boolean anySatisfyWithOccurrences(@Independent @Modified ObjectIntPredicate<? super T> arg0) { return false; }
        @Independent @Modified ListIterable<ObjectIntPair<T>> bottomOccurrences(int arg0) { return null; }
        @Independent @Modified
        <V> RichIterable<V> collectWithOccurrences(
            @Independent @Modified ObjectIntToObjectFunction<? super T, ? extends V> arg0) { return null; }

        @Independent @Modified
        <V, R extends Collection<V>> R collectWithOccurrences(
            @Independent @Modified ObjectIntToObjectFunction<? super T, ? extends V> function,
            @Independent @Modified R target) { return null; }

        @Independent(hc = true) @Modified
        T detectWithOccurrences(@Independent @Modified ObjectIntPredicate<? super T> arg0) { return null; }
        @Independent @Modified RichIterable<T> distinctView() { return null; }
        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @Modified Object arg0) { return false; }
        @Modified void forEachWithOccurrences(@Independent @Modified ObjectIntProcedure<? super T> arg0) { }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V> BagMultimap<V, T> groupBy(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V> BagMultimap<V, T> groupByEach(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Modified
        boolean noneSatisfyWithOccurrences(@Independent @Modified ObjectIntPredicate<? super T> arg0) { return false; }
        @NotModified int occurrencesOf(@Independent @Modified Object arg0) { return 0; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        PartitionBag<T> partition(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> PartitionBag<T> partitionWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <K, R extends MutableMapIterable<K, T>> R reduceBy(
            @Independent @Modified Function<? super T, ? extends K> groupBy,
            @Independent @NotModified Function2<? super T, ? super T, ? extends T> reduceFunction,
            @Independent @Modified R target) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <R> R reduceInPlace(
            @Independent @Modified Supplier<R> supplier,
            @Independent @Modified BiConsumer<R, ? super T> accumulator) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        <R, A> R reduceInPlace(@Independent @Modified Collector<? super T, A, R> collector) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        Bag<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> Bag<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        Bag<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent(absent = true) @Modified
        Bag<T> selectByOccurrences(@Independent @Modified IntPredicate arg0) { return null; }
        @Independent @Modified Bag<T> selectDuplicates() { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        <S> Bag<S> selectInstancesOf(Class<S> arg0) { return null; }
        @Independent @Modified SetIterable<T> selectUnique() { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> Bag<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @NotModified int sizeDistinct() { return 0; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        DoubleSummaryStatistics summarizeDouble(@Independent @Modified DoubleFunction<? super T> function) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        DoubleSummaryStatistics summarizeFloat(@Independent @Modified FloatFunction<? super T> function) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        IntSummaryStatistics summarizeInt(@Independent @Modified IntFunction<? super T> function) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LongSummaryStatistics summarizeLong(@Independent @Modified LongFunction<? super T> function) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        Bag<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }
        @Independent @Modified ImmutableBagIterable<T> toImmutable() { return null; }
        @Independent(hc = true) @Modified MapIterable<T, Integer> toMapOfItemToCount() { return null; }
        @Modified String toStringOfItemToCount() { return null; }
        @Independent @Modified ListIterable<ObjectIntPair<T>> topOccurrences(int arg0) { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        SetIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface ImmutableBag implements UnsortedBag<T>, ImmutableBagIterable<T>
    //annotated as EXPECTED; computed mutable @Independent(hc = true) (conditional) (2) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableBag$<T> {
        //override from org.eclipse.collections.api.bag.Bag
        @Independent @NotModified
        ImmutableList<ObjectIntPair<T>> bottomOccurrences(int arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <V> ImmutableBag<V> collect(@Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableBooleanBag collectBoolean(@Independent @NotModified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableByteBag collectByte(@Independent @NotModified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableCharBag collectChar(@Independent @NotModified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableDoubleBag collectDouble(@Independent @NotModified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableFloatBag collectFloat(@Independent @NotModified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <V> ImmutableBag<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableIntBag collectInt(@Independent @NotModified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableLongBag collectLong(@Independent @NotModified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableShortBag collectShort(@Independent @NotModified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <P, V> ImmutableBag<V> collectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag
        @Independent @NotModified

        <V> ImmutableBag<V> collectWithOccurrences(
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

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <V> ImmutableBag<V> flatCollect(
            @Independent(hc = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <P, V> ImmutableBag<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <V> ImmutableBagMultimap<V, T> groupBy(
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <V> ImmutableBagMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableBag<T> newWith(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableBag<T> newWithAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableBag<T> newWithout(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableBag<T> newWithoutAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent(hc = true) @NotModified
        PartitionImmutableBag<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <P> PartitionImmutableBag<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableBag<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <P> ImmutableBag<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableBag<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag
        @Independent @NotModified
        ImmutableBag<T> selectByOccurrences(@Independent @NotModified IntPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag
        @Independent @NotModified
        ImmutableBag<T> selectDuplicates() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        <S> ImmutableBag<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag
        @Independent @NotModified
        ImmutableSet<T> selectUnique() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <P> ImmutableBag<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableBag<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Fluent @Independent @NotModified
        ImmutableBag<T> toImmutableBag() { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @NotModified
        ImmutableList<ObjectIntPair<T>> topOccurrences(int arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        <S> ImmutableBag<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.ImmutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSet<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface ImmutableBagIterable implements Bag<T>, ImmutableCollection<T>
    //annotated as EXPECTED; computed mutable @Independent(hc = true) (conditional) (2) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableBagIterable$<T> {
        //override from org.eclipse.collections.api.bag.Bag
        @Independent @NotModified

        <V> ImmutableCollection<V> collectWithOccurrences(
            @Independent @NotModified ObjectIntToObjectFunction<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent(hc = true) @NotModified

        <V> ImmutableBagIterableMultimap<V, T> groupBy(
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent(hc = true) @NotModified

        <V> ImmutableBagIterableMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent(hc = true) @NotModified
        PartitionImmutableBagIterable<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <P> PartitionImmutableBagIterable<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableBagIterable<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <P> ImmutableBagIterable<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableBagIterable<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @NotModified
        ImmutableBagIterable<T> selectByOccurrences(@Independent @NotModified IntPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @NotModified
        ImmutableBagIterable<T> selectDuplicates() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        <S> ImmutableBagIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @NotModified
        ImmutableSetIterable<T> selectUnique() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <P> ImmutableBagIterable<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableBagIterable<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent(hc = true) @NotModified
        MutableMapIterable<T, Integer> toMapOfItemToCount() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSetIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface MultiReaderBag implements MutableBag<T>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection behind a lock (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MultiReaderBag$<T> {
        //override from org.eclipse.collections.api.bag.MutableBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @NotModified
        MultiReaderBag<T> newEmpty() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MultiReaderBag<T> tap(@Independent @NotModified Procedure<? super T> procedure) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MultiReaderBag<T> with(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MultiReaderBag<T> withAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBag, org.eclipse.collections.api.bag.MutableBagIterable
        @Fluent @Independent @Modified
        MultiReaderBag<T> withOccurrences(@Independent @NotModified T element, int occurrences) { return null; }
        @Modified void withReadLockAndDelegate(@Independent @NotModified Procedure<? super MutableBag<T>> arg0) { }
        @Modified void withWriteLockAndDelegate(@Independent @NotModified Procedure<? super MutableBag<T>> arg0) { }
        //override from org.eclipse.collections.api.bag.MutableBag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MultiReaderBag<T> without(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MultiReaderBag<T> withoutAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBag, org.eclipse.collections.api.bag.MutableBagIterable
        @Fluent @Independent @Modified
        MultiReaderBag<T> withoutOccurrences(@Independent @NotModified T element, int occurrences) { return null; }
    }

    //public interface MutableBag implements UnsortedBag<T>, MutableBagIterable<T>
    //annotated as EXPECTED; computed mutable @Dependent (2) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableBag$<T> {
        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableBag<T> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableBag<T> asUnmodifiable() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        <V> MutableBag<V> collect(@Independent @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified

        MutableBooleanBag collectBoolean(@Independent @NotModified BooleanFunction<? super T> booleanFunction) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableByteBag collectByte(@Independent @NotModified ByteFunction<? super T> byteFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableCharBag collectChar(@Independent @NotModified CharFunction<? super T> charFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified

        MutableDoubleBag collectDouble(@Independent @NotModified DoubleFunction<? super T> doubleFunction) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableFloatBag collectFloat(@Independent @NotModified FloatFunction<? super T> floatFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified

        <V> MutableBag<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableIntBag collectInt(@Independent @NotModified IntFunction<? super T> intFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableLongBag collectLong(@Independent @NotModified LongFunction<? super T> longFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableShortBag collectShort(@Independent @NotModified ShortFunction<? super T> shortFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified

        <P, V> MutableBag<V> collectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag
        @Independent @Modified

        <V> MutableBag<V> collectWithOccurrences(
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

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified

        <V> MutableBag<V> flatCollect(@Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified

        <P, V> MutableBag<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified

        <V> MutableBagMultimap<V, T> groupBy(@Independent @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified

        <V> MutableBagMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent(absent = true) @Modified
        MutableBag<T> newEmpty() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified
        PartitionMutableBag<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified

        <P> PartitionMutableBag<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified
        MutableBag<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified

        <P> MutableBag<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified
        MutableBag<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag
        @Independent(absent = true) @Modified
        MutableBag<T> selectByOccurrences(@Independent @NotModified IntPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag
        @Independent @Modified
        MutableBag<T> selectDuplicates() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        <S> MutableBag<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag
        @Independent @Modified
        MutableSet<T> selectUnique() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified

        <P> MutableBag<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableBag<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        ImmutableBag<T> toImmutable() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ImmutableBag<T> toImmutableBag() { return null; }

        //override from org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable
        @Independent @Modified
        MutableMap<T, Integer> toMapOfItemToCount() { return null; }

        //override from org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableBag<T> with(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableBag<T> withAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBagIterable
        @Fluent @Independent @Modified
        MutableBag<T> withOccurrences(@Independent @NotModified T element, int occurrences) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableBag<T> without(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableBag<T> withoutAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.bag.MutableBagIterable
        @Fluent @Independent @Modified
        MutableBag<T> withoutOccurrences(@Independent @NotModified T element, int occurrences) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified
        <S> MutableBag<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.bag.MutableBagIterable, org.eclipse.collections.api.bag.UnsortedBag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified
        MutableSet<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface MutableBagIterable implements Bag<T>, MutableCollection<T>
    //annotated as EXPECTED; computed mutable @Dependent (2) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableBagIterable$<T> {
        @Modified int addOccurrences(@Independent(hc = true) @NotModified T arg0, int arg1) { return 0; }
        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        MutableList<ObjectIntPair<T>> bottomOccurrences(int arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified

        <V> RichIterable<V> collectWithOccurrences(
            @Independent @NotModified ObjectIntToObjectFunction<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified

        <V> MutableBagIterableMultimap<V, T> groupBy(@Independent @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified

        <V> MutableBagIterableMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified
        PartitionMutableBagIterable<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified

        <P> PartitionMutableBagIterable<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified
        MutableBagIterable<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified

        <P> MutableBagIterable<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        @Modified
        boolean removeOccurrences(@Independent(hc = true) @NotModified Object arg0, int arg1) { return false; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified
        MutableBagIterable<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent(absent = true) @Modified
        MutableBagIterable<T> selectByOccurrences(@Independent @NotModified IntPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        MutableBagIterable<T> selectDuplicates() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        <S> MutableBagIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        MutableSetIterable<T> selectUnique() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified

        <P> MutableBagIterable<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @Modified boolean setOccurrences(@Independent(hc = true) @NotModified T arg0, int arg1) { return false; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableBagIterable<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        MutableMapIterable<T, Integer> toMapOfItemToCount() { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        MutableList<ObjectIntPair<T>> topOccurrences(int arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableBagIterable<T> with(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableBagIterable<T> withAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        @Fluent @Independent @Modified
        MutableBagIterable<T> withOccurrences(@Independent @NotModified T element, int occurrences) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableBagIterable<T> without(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableBagIterable<T> withoutAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        @Fluent @Independent @Modified
        MutableBagIterable<T> withoutOccurrences(@Independent @NotModified T element, int occurrences) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified
        MutableSetIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface ParallelBag implements ParallelIterable<T>
    @FinalFields
    @Independent(hc = true)
    class ParallelBag$<T> {
        @Modified void forEachWithOccurrences(@Independent @Modified ObjectIntProcedure<? super T> arg0) { }
        //override from org.eclipse.collections.api.ParallelIterable
        @Independent(hc = true) @Modified
        <V> BagMultimap<V, T> groupBy(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent(hc = true) @Modified

        <V> BagMultimap<V, T> groupByEach(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        ParallelBag<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <P> ParallelBag<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        ParallelBag<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        <S> ParallelBag<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <P> ParallelBag<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
    }

    //public interface ParallelUnsortedBag implements ParallelBag<T>
    @FinalFields
    @Independent(hc = true)
    class ParallelUnsortedBag$<T> {
        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        ParallelUnsortedSetIterable<T> asUnique() { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        <V> ParallelUnsortedBag<V> collect(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <V> ParallelUnsortedBag<V> collectIf(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent @Modified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <P, V> ParallelUnsortedBag<V> collectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @Modified P arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <V> ParallelUnsortedBag<V> flatCollect(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent(hc = true) @Modified

        <V> UnsortedBagMultimap<V, T> groupBy(@Independent @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent(hc = true) @Modified

        <V> UnsortedBagMultimap<V, T> groupByEach(
            @Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent @Modified
        ParallelUnsortedBag<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent @Modified

        <P> ParallelUnsortedBag<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent @Modified
        ParallelUnsortedBag<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent @Modified
        <S> ParallelUnsortedBag<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.bag.ParallelBag
        @Independent @Modified

        <P> ParallelUnsortedBag<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
    }

    //public interface UnsortedBag implements Bag<T>
    @Independent(absent = true)
    class UnsortedBag$<T> {
        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> UnsortedBag<V> collect(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        BooleanBag collectBoolean(@Independent @Modified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ByteBag collectByte(@Independent @Modified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        CharBag collectChar(@Independent @Modified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        DoubleBag collectDouble(@Independent @Modified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        FloatBag collectFloat(@Independent @Modified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> UnsortedBag<V> collectIf(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        IntBag collectInt(@Independent @Modified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LongBag collectLong(@Independent @Modified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ShortBag collectShort(@Independent @Modified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <P, V> UnsortedBag<V> collectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @Modified P arg1) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified

        <V> UnsortedBag<V> collectWithOccurrences(
            @Independent @Modified ObjectIntToObjectFunction<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> UnsortedBag<V> flatCollect(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <P, V> UnsortedBag<V> flatCollectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @Modified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag
        @Independent(hc = true) @Modified

        <V> UnsortedBagMultimap<V, T> groupBy(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag
        @Independent(hc = true) @Modified

        <V> UnsortedBagMultimap<V, T> groupByEach(
            @Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag
        @Independent(hc = true) @Modified
        PartitionUnsortedBag<T> partition(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag
        @Independent(hc = true) @Modified
        UnsortedBag<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag
        @Independent(hc = true) @Modified

        <P> UnsortedBag<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag
        @Independent(hc = true) @Modified
        UnsortedBag<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent(absent = true) @Modified
        UnsortedBag<T> selectByOccurrences(@Independent @Modified IntPredicate arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        UnsortedBag<T> selectDuplicates() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        <S> UnsortedBag<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        UnsortedSetIterable<T> selectUnique() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag
        @Independent(hc = true) @Modified

        <P> UnsortedBag<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag
        @Independent @Modified
        UnsortedBag<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        <S> UnsortedBag<Pair<T, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bag.Bag
        @Independent(hc = true) @Modified
        UnsortedSetIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }
}
