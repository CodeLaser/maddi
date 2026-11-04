package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.*;
import org.eclipse.collections.api.map.*;
import org.eclipse.collections.api.map.sorted.ImmutableSortedMap;
import org.eclipse.collections.api.map.sorted.MutableSortedMap;
import org.eclipse.collections.api.map.sorted.SortedMapIterable;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.multimap.sortedset.ImmutableSortedSetMultimap;
import org.eclipse.collections.api.multimap.sortedset.MutableSortedSetMultimap;
import org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap;
import org.eclipse.collections.api.partition.list.PartitionImmutableList;
import org.eclipse.collections.api.partition.list.PartitionList;
import org.eclipse.collections.api.partition.list.PartitionMutableList;
import org.eclipse.collections.api.set.MutableSet;
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
@Independent(absent = true)
public class OrgEclipseCollectionsApiMapSorted {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.map.sorted";
    //public interface ImmutableSortedMap implements SortedMapIterable<K,V>, ImmutableMapIterable<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableSortedMap$<K, V> {
        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <K1, V1, V2> ImmutableMap<K1, V2> aggregateBy(
            @Independent @NotModified Function<? super K, ? extends K1> keyFunction,
            @Independent @NotModified Function<? super V, ? extends V1> valueFunction,
            @Independent @NotModified Function0<? extends V2> zeroValueFactory,
            @Independent @NotModified Function2<? super V2, ? super V1, ? extends V2> nonMutatingAggregator) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <KK, VV> ImmutableMap<KK, VV> aggregateBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function0<? extends VV> zeroValueFactory,
            @Independent @NotModified Function2<? super VV, ? super V, ? extends VV> nonMutatingAggregator) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <KK, VV> ImmutableMap<KK, VV> aggregateInPlaceBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function0<? extends VV> zeroValueFactory,
            @Independent @NotModified Procedure2<? super VV, ? super V> mutatingAggregator) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        SortedMap<K, V> castToMap() { return null; }
        @Independent @NotModified SortedMap<K, V> castToSortedMap() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        <R> ImmutableList<R> collect(@Independent @NotModified Function<? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable
        @Independent @NotModified

        <K2, V2> ImmutableMap<K2, V2> collect(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableBooleanList collectBoolean(@Independent @NotModified BooleanFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableByteList collectByte(@Independent @NotModified ByteFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableCharList collectChar(@Independent @NotModified CharFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableDoubleList collectDouble(@Independent @NotModified DoubleFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableFloatList collectFloat(@Independent @NotModified FloatFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <R> ImmutableList<R> collectIf(
            @Independent @NotModified Predicate<? super V> arg0,
            @Independent @NotModified Function<? super V, ? extends R> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableIntList collectInt(@Independent @NotModified IntFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <R> ImmutableOrderedMap<R, V> collectKeysUnique(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableLongList collectLong(@Independent @NotModified LongFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableShortList collectShort(@Independent @NotModified ShortFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable
        @Independent @NotModified

        <R> ImmutableSortedMap<K, R> collectValues(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P, VV> ImmutableList<VV> collectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends VV> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <R> ImmutableList<R> collectWithIndex(
            @Independent @NotModified ObjectIntToObjectFunction<? super V, ? extends R> function) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <R> ImmutableList<R> flatCollect(@Independent @NotModified Function<? super V, ? extends Iterable<R>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P, R> ImmutableList<R> flatCollectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends Iterable<R>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable
        @Independent @NotModified
        ImmutableSortedSetMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <VV> ImmutableListMultimap<VV, V> groupBy(@Independent @NotModified Function<? super V, ? extends VV> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <VV> ImmutableListMultimap<VV, V> groupByEach(
            @Independent @NotModified Function<? super V, ? extends Iterable<VV>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <VV> ImmutableMap<VV, V> groupByUniqueKey(@Independent @NotModified Function<? super V, ? extends VV> function) {
            return null;
        }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableSortedMap<K, V> newWithAllKeyValueArguments(
            @Independent @NotModified Pair<? extends K, ? extends V> ... arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableSortedMap<K, V> newWithAllKeyValues(
            @Independent @NotModified Iterable<? extends Pair<? extends K, ? extends V>> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableSortedMap<K, V> newWithKeyValue(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableSortedMap<K, V> newWithMap(@Independent @NotModified Map<? extends K, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableSortedMap<K, V> newWithMapIterable(
            @Independent @NotModified MapIterable<? extends K, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableSortedMap<K, V> newWithoutAllKeys(@Independent @NotModified Iterable<? extends K> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableSortedMap<K, V> newWithoutKey(@Independent @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        PartitionImmutableList<V> partition(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P> PartitionImmutableList<V> partitionWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <KK> ImmutableMap<KK, V> reduceBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function2<? super V, ? super V, ? extends V> reduceFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<V> reject(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable
        @Independent @NotModified
        ImmutableSortedMap<K, V> reject(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P> ImmutableList<V> rejectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<V> select(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable
        @Independent @NotModified
        ImmutableSortedMap<K, V> select(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        <S> ImmutableList<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P> ImmutableList<V> selectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableSortedMap<K, V> tap(@Independent @NotModified Procedure<? super V> arg0) { return null; }
        @Independent @NotModified MutableSortedMap<K, V> toSortedMap() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        <S> ImmutableList<Pair<V, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<Pair<V, Integer>> zipWithIndex() { return null; }
    }

    //public interface MutableSortedMap implements MutableMapIterable<K,V>, SortedMapIterable<K,V>, SortedMap<K,V>, Cloneable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableSortedMap$<K, V> {
        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableSortedMap<K, V> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableSortedMap<K, V> asUnmodifiable() { return null; }
        @Independent(absent = true) @NotModified protected MutableSortedMap<K, V> clone() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        <R> MutableList<R> collect(@Independent @NotModified Function<? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable
        @Independent(hc = true) @NotModified

        <K2, V2> MutableMap<K2, V2> collect(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableBooleanList collectBoolean(@Independent @NotModified BooleanFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableByteList collectByte(@Independent @NotModified ByteFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableCharList collectChar(@Independent @NotModified CharFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableDoubleList collectDouble(@Independent @NotModified DoubleFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableFloatList collectFloat(@Independent @NotModified FloatFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <R> MutableList<R> collectIf(
            @Independent @NotModified Predicate<? super V> arg0,
            @Independent @NotModified Function<? super V, ? extends R> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableIntList collectInt(@Independent @NotModified IntFunction<? super V> arg0) { return null; }

        @Independent @Modified
        <E> MutableSortedMap<K, V> collectKeysAndValues(
            @Independent @NotModified Iterable<E> arg0,
            @Independent @NotModified Function<? super E, ? extends K> arg1,
            @Independent @NotModified Function<? super E, ? extends V> arg2) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @NotModified

        <R> MutableOrderedMap<R, V> collectKeysUnique(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableLongList collectLong(@Independent @NotModified LongFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableShortList collectShort(@Independent @NotModified ShortFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable
        @Independent @NotModified

        <R> MutableSortedMap<K, R> collectValues(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, VV> MutableList<VV> collectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends VV> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <R> MutableList<R> collectWithIndex(
            @Independent @NotModified ObjectIntToObjectFunction<? super V, ? extends R> function) { return null; }

        //override from org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        MutableList<V> distinct() { return null; }

        //override from org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(absent = true) @NotModified
        MutableSortedMap<K, V> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        MutableSortedMap<K, V> dropWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from java.util.Map, java.util.SortedMap
        @Independent(absent = true) @Modified
        MutableSet<Map.Entry<K, V>> entrySet() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <R> MutableList<R> flatCollect(@Independent @NotModified Function<? super V, ? extends Iterable<R>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, R> MutableList<R> flatCollectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends Iterable<R>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable
        @Independent @Modified
        MutableSortedSetMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent(hc = true) @Modified

        <P> V getIfAbsentPutWith(
            @Independent @NotModified K arg0,
            @Independent @NotModified Function<? super P, ? extends V> arg1,
            @Independent @NotModified P arg2) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <VV> MutableListMultimap<VV, V> groupBy(@Independent @NotModified Function<? super V, ? extends VV> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <VV> MutableListMultimap<VV, V> groupByEach(
            @Independent @NotModified Function<? super V, ? extends Iterable<VV>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <VV> MutableMap<VV, V> groupByUniqueKey(@Independent @NotModified Function<? super V, ? extends VV> function) {
            return null;
        }

        //override from java.util.SortedMap
        @Independent(absent = true) @NotModified
        MutableSortedMap<K, V> headMap(@Independent @NotModified K arg0) { return null; }

        //override from java.util.Map, java.util.SortedMap
        @Independent(hc = true) @NotModified
        MutableSet<K> keySet() { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @NotModified
        MutableSortedMap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        PartitionMutableList<V> partition(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        PartitionMutableList<V> partitionWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <P> PartitionMutableList<V> partitionWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        MutableList<V> reject(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable
        @Independent @NotModified
        MutableSortedMap<K, V> reject(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <P> MutableList<V> rejectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        MutableList<V> select(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable
        @Independent @NotModified
        MutableSortedMap<K, V> select(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        <S> MutableList<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <P> MutableList<V> selectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from java.util.SortedMap
        @Independent(absent = true) @NotModified
        MutableSortedMap<K, V> subMap(@Independent @NotModified K arg0, @Independent @NotModified K arg1) { return null; }

        //override from java.util.SortedMap
        @Independent(absent = true) @NotModified
        MutableSortedMap<K, V> tailMap(@Independent @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(absent = true) @NotModified
        MutableSortedMap<K, V> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        MutableSortedMap<K, V> takeWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableSortedMap<K, V> tap(@Independent @NotModified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        MutableSortedMap<K, V> toReversed() { return null; }

        //override from java.util.Map, java.util.SortedMap
        @Independent(hc = true) @NotModified
        MutableCollection<V> values() { return null; }

        @Independent @Modified
        MutableSortedMap<K, V> with(@Independent @NotModified Pair<K, V> ... pairs) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableSortedMap<K, V> withAllKeyValueArguments(
            @Independent @NotModified Pair<? extends K, ? extends V> ... arg0) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableSortedMap<K, V> withAllKeyValues(
            @Independent(absent = true) @NotModified Iterable<? extends Pair<? extends K, ? extends V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableSortedMap<K, V> withKeyValue(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified V arg1) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Fluent @Independent @Modified
        MutableSortedMap<K, V> withMap(@Independent @NotModified Map<? extends K, ? extends V> map) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Fluent @Independent @Modified

        MutableSortedMap<K, V> withMapIterable(
            @Independent @NotModified MapIterable<? extends K, ? extends V> mapIterable) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableSortedMap<K, V> withoutAllKeys(@Independent(hc = true) @NotModified Iterable<? extends K> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableSortedMap<K, V> withoutKey(@Independent @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        <S> MutableList<Pair<V, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.sorted.SortedMapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        MutableList<Pair<V, Integer>> zipWithIndex() { return null; }
    }

    //public interface SortedMapIterable implements MapIterable<K,V>, ReversibleIterable<V>
    @Independent(absent = true)
    class SortedMapIterable$<K, V> {
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        <V1> ListIterable<V1> collect(@Independent @Modified Function<? super V, ? extends V1> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified

        <K2, V2> UnsortedMapIterable<K2, V2> collect(
            @Independent @Modified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        BooleanList collectBoolean(@Independent @Modified BooleanFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        ByteList collectByte(@Independent @Modified ByteFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        CharList collectChar(@Independent @Modified CharFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        DoubleList collectDouble(@Independent @Modified DoubleFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        FloatList collectFloat(@Independent @Modified FloatFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V1> ListIterable<V1> collectIf(
            @Independent @Modified Predicate<? super V> arg0,
            @Independent @Modified Function<? super V, ? extends V1> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        IntList collectInt(@Independent @Modified IntFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        LongList collectLong(@Independent @Modified LongFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        ShortList collectShort(@Independent @Modified ShortFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified

        <R> SortedMapIterable<K, R> collectValues(
            @Independent @Modified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V1> ListIterable<V1> collectWith(
            @Independent @Modified Function2<? super V, ? super P, ? extends V1> arg0,
            @Independent @Modified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V1> ListIterable<V1> collectWithIndex(
            @Independent @Modified ObjectIntToObjectFunction<? super V, ? extends V1> function) { return null; }
        @Independent(hc = true) @NotModified Comparator<? super K> comparator() { return null; }
        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ListIterable<V> distinct() { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(absent = true) @NotModified
        SortedMapIterable<K, V> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        SortedMapIterable<K, V> dropWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V1> ListIterable<V1> flatCollect(@Independent @Modified Function<? super V, ? extends Iterable<V1>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V1> ListIterable<V1> flatCollectWith(
            @Independent @Modified Function2<? super V, ? super P, ? extends Iterable<V1>> function,
            @Independent @Modified P parameter) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        SortedSetMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        <VV> ListMultimap<VV, V> groupBy(@Independent @Modified Function<? super V, ? extends VV> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <VV> ListMultimap<VV, V> groupByEach(@Independent @Modified Function<? super V, ? extends Iterable<VV>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        PartitionList<V> partition(@Independent @Modified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        PartitionList<V> partitionWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <P> PartitionList<V> partitionWith(
            @Independent @Modified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        ListIterable<V> reject(@Independent @Modified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        SortedMapIterable<K, V> reject(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <P> ListIterable<V> rejectWith(
            @Independent @Modified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        ListIterable<V> select(@Independent @Modified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        SortedMapIterable<K, V> select(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        <S> ListIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified

        <P> ListIterable<V> selectWith(
            @Independent @Modified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(absent = true) @NotModified
        SortedMapIterable<K, V> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        SortedMapIterable<K, V> takeWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        SortedMapIterable<K, V> tap(@Independent @Modified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified
        ImmutableSortedMap<K, V> toImmutable() { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        SortedMapIterable<K, V> toReversed() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        <S> ListIterable<Pair<V, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(hc = true) @Modified
        ListIterable<Pair<V, Integer>> zipWithIndex() { return null; }
    }
}
