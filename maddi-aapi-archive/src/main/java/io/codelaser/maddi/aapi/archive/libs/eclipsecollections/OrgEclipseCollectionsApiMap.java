package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.Bag;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.*;
import org.eclipse.collections.api.block.function.*;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.*;
import org.eclipse.collections.api.map.*;
import org.eclipse.collections.api.map.primitive.*;
import org.eclipse.collections.api.multimap.ImmutableMultimap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.multimap.bag.BagMultimap;
import org.eclipse.collections.api.multimap.bag.ImmutableBagMultimap;
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.multimap.set.ImmutableSetMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.multimap.set.UnsortedSetMultimap;
import org.eclipse.collections.api.partition.PartitionImmutableCollection;
import org.eclipse.collections.api.partition.PartitionMutableCollection;
import org.eclipse.collections.api.partition.bag.PartitionBag;
import org.eclipse.collections.api.partition.bag.PartitionImmutableBag;
import org.eclipse.collections.api.partition.bag.PartitionMutableBag;
import org.eclipse.collections.api.partition.list.PartitionImmutableList;
import org.eclipse.collections.api.partition.list.PartitionList;
import org.eclipse.collections.api.partition.list.PartitionMutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.UnsortedSetIterable;
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
public class OrgEclipseCollectionsApiMap {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.map";
    //public interface ConcurrentMutableMap implements MutableMap<K,V>, ConcurrentMap<K,V>
    @Independent(absent = true)
    class ConcurrentMutableMap$<K, V> {
        //override from java.util.Map, java.util.concurrent.ConcurrentMap, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @NotModified
        void forEach(@Independent @Modified BiConsumer<? super K, ? super V> action) { }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        V getOrDefault(@Independent @Modified Object key, @Independent @NotModified V defaultValue) { return null; }

        //override from java.util.Map, java.util.concurrent.ConcurrentMap
        @Independent @Modified

        V merge(
            @Independent @NotModified K arg0,
            @Independent @Modified V arg1,
            @IgnoreModifications @Independent(absent = true) @Modified BiFunction<? super V, ? super V, ? extends V>
                arg2) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMap, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        ConcurrentMutableMap<K, V> tap(@Independent @Modified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MutableMap, org.eclipse.collections.api.map.MutableMapIterable
        @Fluent @Independent @Modified
        ConcurrentMutableMap<K, V> withMap(@Independent @NotModified Map<? extends K, ? extends V> map) { return null; }

        //override from org.eclipse.collections.api.map.MutableMap, org.eclipse.collections.api.map.MutableMapIterable
        @Fluent @Independent @Modified

        ConcurrentMutableMap<K, V> withMapIterable(
            @Independent @Modified MapIterable<? extends K, ? extends V> mapIterable) { return null; }
    }

    //public interface FixedSizeMap implements MutableMap<K,V>
    @Independent(absent = true)
    class FixedSizeMap$<K, V> {
        //override from java.util.Map
        @NotModified
        void clear() { }

        //override from java.util.Map
        @Independent @NotModified
        V put(@Independent @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        //override from java.util.Map
        @NotModified
        void putAll(@Independent @NotModified Map<? extends K, ? extends V> arg0) { }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @NotModified
        void putAllMapIterable(@Independent @NotModified MapIterable<? extends K, ? extends V> mapIterable) { }

        //override from java.util.Map
        @Independent @NotModified
        V remove(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @NotModified
        boolean removeAllKeys(@Independent @NotModified Set<? extends K> arg0) { return false; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @NotModified
        boolean removeIf(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return false; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @NotModified
        V removeKey(@Independent @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMap, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        FixedSizeMap<K, V> tap(@Independent @Modified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MutableMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @NotModified
        FixedSizeMap<K, V> withMap(@Independent @NotModified Map<? extends K, ? extends V> map) { return null; }

        //override from org.eclipse.collections.api.map.MutableMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @NotModified

        FixedSizeMap<K, V> withMapIterable(@Independent @NotModified MapIterable<? extends K, ? extends V> mapIterable) {
            return null;
        }
    }

    //public interface ImmutableMap implements UnsortedMapIterable<K,V>, ImmutableMapIterable<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (3) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableMap$<K, V> {
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

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        <VV> ImmutableBag<VV> collect(@Independent @NotModified Function<? super V, ? extends VV> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified

        <K2, V2> ImmutableMap<K2, V2> collect(
            @Independent(hc = true) @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableBooleanBag collectBoolean(@Independent @NotModified BooleanFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableByteBag collectByte(@Independent @NotModified ByteFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableCharBag collectChar(@Independent @NotModified CharFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableDoubleBag collectDouble(@Independent @NotModified DoubleFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableFloatBag collectFloat(@Independent @NotModified FloatFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified

        <R> ImmutableBag<R> collectIf(
            @Independent @NotModified Predicate<? super V> arg0,
            @Independent @NotModified Function<? super V, ? extends R> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableIntBag collectInt(@Independent @NotModified IntFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <R> ImmutableMap<R, V> collectKeysUnique(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableLongBag collectLong(@Independent @NotModified LongFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableShortBag collectShort(@Independent @NotModified ShortFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @NotModified

        <R> ImmutableMap<K, R> collectValues(
            @Independent(hc = true) @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified

        <P, VV> ImmutableBag<VV> collectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends VV> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified

        <R> ImmutableBag<R> flatCollect(@Independent @NotModified Function<? super V, ? extends Iterable<R>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified

        <P, R> ImmutableBag<R> flatCollectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends Iterable<R>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableSetMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableMap<V, K> flipUniqueValues() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified

        <VV> ImmutableBagMultimap<VV, V> groupBy(@Independent @NotModified Function<? super V, ? extends VV> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified

        <VV> ImmutableBagMultimap<VV, V> groupByEach(
            @Independent @NotModified Function<? super V, ? extends Iterable<VV>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified

        <V1> ImmutableMap<V1, V> groupByUniqueKey(@Independent @NotModified Function<? super V, ? extends V1> function) {
            return null;
        }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableMap<K, V> newWithAllKeyValueArguments(
            @Independent @NotModified Pair<? extends K, ? extends V> ... arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableMap<K, V> newWithAllKeyValues(
            @Independent @NotModified Iterable<? extends Pair<? extends K, ? extends V>> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableMap<K, V> newWithKeyValue(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableMap<K, V> newWithMap(@Independent(hc = true) @NotModified Map<? extends K, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableMap<K, V> newWithMapIterable(@Independent @NotModified MapIterable<? extends K, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableMap<K, V> newWithoutAllKeys(@Independent @NotModified Iterable<? extends K> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableMap<K, V> newWithoutKey(@Independent @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        PartitionImmutableBag<V> partition(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified

        <P> PartitionImmutableBag<V> partitionWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <KK> ImmutableMap<KK, V> reduceBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function2<? super V, ? super V, ? extends V> reduceFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableBag<V> reject(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableMap<K, V> reject(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified

        <P> ImmutableBag<V> rejectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableBag<V> select(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableMap<K, V> select(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        <S> ImmutableBag<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified

        <P> ImmutableBag<V> selectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableMap<K, V> tap(@Independent @NotModified Procedure<? super V> arg0) { return null; }
        @Independent @NotModified MutableMap<K, V> toMap() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        <S> ImmutableBag<Pair<V, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified
        ImmutableSet<Pair<V, Integer>> zipWithIndex() { return null; }
    }

    //public interface ImmutableMapIterable implements MapIterable<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (3) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableMapIterable$<K, V> {
        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <K1, V1, V2> ImmutableMapIterable<K1, V2> aggregateBy(
            @Independent @NotModified Function<? super K, ? extends K1> keyFunction,
            @Independent @NotModified Function<? super V, ? extends V1> valueFunction,
            @Independent @NotModified Function0<? extends V2> zeroValueFactory,
            @Independent @NotModified Function2<? super V2, ? super V1, ? extends V2> nonMutatingAggregator) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <KK, VV> ImmutableMapIterable<KK, VV> aggregateBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function0<? extends VV> zeroValueFactory,
            @Independent @NotModified Function2<? super VV, ? super V, ? extends VV> nonMutatingAggregator) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <KK, VV> ImmutableMapIterable<KK, VV> aggregateInPlaceBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function0<? extends VV> zeroValueFactory,
            @Independent @NotModified Procedure2<? super VV, ? super V> mutatingAggregator) { return null; }
        @Independent @NotModified Map<K, V> castToMap() { return null; }
        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <K2, V2> ImmutableMapIterable<K2, V2> collect(
            @Independent(hc = true) @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <R> ImmutableMapIterable<R, V> collectKeysUnique(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent(hc = true) @NotModified

        <R> ImmutableMapIterable<K, R> collectValues(
            @Independent(hc = true) @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V1> ImmutableBag<V1> countBy(@Independent @NotModified Function<? super V, ? extends V1> function) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V1> ImmutableBag<V1> countByEach(
            @Independent @NotModified Function<? super V, ? extends Iterable<V1>> function) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V1, P> ImmutableBag<V1> countByWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends V1> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified
        ImmutableMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent(hc = true) @NotModified
        ImmutableMapIterable<V, K> flipUniqueValues() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V1> ImmutableMultimap<V1, V> groupBy(@Independent @NotModified Function<? super V, ? extends V1> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V1> ImmutableMultimap<V1, V> groupByEach(
            @Independent @NotModified Function<? super V, ? extends Iterable<V1>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V1> ImmutableMapIterable<V1, V> groupByUniqueKey(
            @Independent @NotModified Function<? super V, ? extends V1> arg0) { return null; }

        @Independent @NotModified
        ImmutableMapIterable<K, V> newWithAllKeyValueArguments(
            @Independent @NotModified Pair<? extends K, ? extends V> ... arg0) { return null; }

        @Independent @NotModified
        ImmutableMapIterable<K, V> newWithAllKeyValues(
            @Independent(hc = true) @NotModified Iterable<? extends Pair<? extends K, ? extends V>> arg0) { return null; }

        @Independent @NotModified
        ImmutableMapIterable<K, V> newWithKeyValue(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        @Independent @NotModified
        ImmutableMapIterable<K, V> newWithMap(@Independent(hc = true) @NotModified Map<? extends K, ? extends V> arg0) {
            return null;
        }

        @Independent @NotModified
        ImmutableMapIterable<K, V> newWithMapIterable(
            @Independent @NotModified MapIterable<? extends K, ? extends V> arg0) { return null; }

        @Independent @NotModified
        ImmutableMapIterable<K, V> newWithoutAllKeys(@Independent @NotModified Iterable<? extends K> arg0) {
            return null;
        }

        @Independent @NotModified
        ImmutableMapIterable<K, V> newWithoutKey(@Independent @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        PartitionImmutableCollection<V> partition(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <KK> ImmutableMapIterable<KK, V> reduceBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function2<? super V, ? super V, ? extends V> reduceFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableCollection<V> reject(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        ImmutableMapIterable<K, V> reject(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <P> ImmutableCollection<V> rejectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableCollection<V> select(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        ImmutableMapIterable<K, V> select(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        <S> ImmutableCollection<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <P> ImmutableCollection<V> selectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified
        ImmutableMapIterable<K, V> tap(@Independent @NotModified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        <S> ImmutableCollection<Pair<V, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableCollection<Pair<V, Integer>> zipWithIndex() { return null; }
    }

    //public interface ImmutableOrderedMap implements OrderedMap<K,V>, ImmutableMapIterable<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (3) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableOrderedMap$<K, V> {
        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <K1, V1, V2> ImmutableOrderedMap<K1, V2> aggregateBy(
            @Independent @NotModified Function<? super K, ? extends K1> keyFunction,
            @Independent @NotModified Function<? super V, ? extends V1> valueFunction,
            @Independent @NotModified Function0<? extends V2> zeroValueFactory,
            @Independent @NotModified Function2<? super V2, ? super V1, ? extends V2> nonMutatingAggregator) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <KK, VV> ImmutableOrderedMap<KK, VV> aggregateBy(
            @Independent @NotModified Function<? super V, ? extends KK> arg0,
            @Independent @NotModified Function0<? extends VV> arg1,
            @Independent @NotModified Function2<? super VV, ? super V, ? extends VV> arg2) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <KK, VV> ImmutableOrderedMap<KK, VV> aggregateInPlaceBy(
            @Independent @NotModified Function<? super V, ? extends KK> arg0,
            @Independent @NotModified Function0<? extends VV> arg1,
            @Independent @NotModified Procedure2<? super VV, ? super V> arg2) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        <VV> ImmutableList<VV> collect(@Independent @NotModified Function<? super V, ? extends VV> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @NotModified

        <K2, V2> ImmutableOrderedMap<K2, V2> collect(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableBooleanList collectBoolean(@Independent @NotModified BooleanFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableByteList collectByte(@Independent @NotModified ByteFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableCharList collectChar(@Independent @NotModified CharFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableDoubleList collectDouble(@Independent @NotModified DoubleFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableFloatList collectFloat(@Independent @NotModified FloatFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V1> ImmutableList<V1> collectIf(
            @Independent @NotModified Predicate<? super V> arg0,
            @Independent @NotModified Function<? super V, ? extends V1> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableIntList collectInt(@Independent @NotModified IntFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <R> ImmutableOrderedMap<R, V> collectKeysUnique(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableLongList collectLong(@Independent @NotModified LongFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableShortList collectShort(@Independent @NotModified ShortFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @NotModified

        <R> ImmutableOrderedMap<K, R> collectValues(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P, V1> ImmutableList<V1> collectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends V1> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<V> distinct() { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableOrderedMap<K, V> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableOrderedMap<K, V> dropWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V1> ImmutableList<V1> flatCollect(@Independent @NotModified Function<? super V, ? extends Iterable<V1>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P, V1> ImmutableList<V1> flatCollectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends Iterable<V1>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @NotModified
        ImmutableListMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @NotModified
        ImmutableOrderedMap<V, K> flipUniqueValues() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V1> ImmutableListMultimap<V1, V> groupBy(@Independent @NotModified Function<? super V, ? extends V1> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <V1> ImmutableListMultimap<V1, V> groupByEach(
            @Independent @NotModified Function<? super V, ? extends Iterable<V1>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @NotModified

        <V1> ImmutableOrderedMap<V1, V> groupByUniqueKey(
            @Independent @NotModified Function<? super V, ? extends V1> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableOrderedMap<K, V> newWithAllKeyValueArguments(
            @Independent @NotModified Pair<? extends K, ? extends V> ... arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableOrderedMap<K, V> newWithAllKeyValues(
            @Independent(hc = true) @NotModified Iterable<? extends Pair<? extends K, ? extends V>> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableOrderedMap<K, V> newWithKeyValue(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableOrderedMap<K, V> newWithMap(@Independent(hc = true) @NotModified Map<? extends K, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableOrderedMap<K, V> newWithMapIterable(
            @Independent @NotModified MapIterable<? extends K, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableOrderedMap<K, V> newWithoutAllKeys(@Independent @NotModified Iterable<? extends K> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableOrderedMap<K, V> newWithoutKey(@Independent @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        PartitionImmutableList<V> partition(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        PartitionImmutableList<V> partitionWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P> PartitionImmutableList<V> partitionWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <KK> ImmutableOrderedMap<KK, V> reduceBy(
            @Independent @NotModified Function<? super V, ? extends KK> arg0,
            @Independent @NotModified Function2<? super V, ? super V, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<V> reject(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @NotModified
        ImmutableOrderedMap<K, V> reject(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P> ImmutableList<V> rejectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<V> select(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @NotModified
        ImmutableOrderedMap<K, V> select(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        <S> ImmutableList<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified

        <P> ImmutableList<V> selectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <VV> ImmutableObjectDoubleMap<VV> sumByDouble(
            @Independent @NotModified Function<? super V, ? extends VV> arg0,
            @Independent @NotModified DoubleFunction<? super V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <VV> ImmutableObjectDoubleMap<VV> sumByFloat(
            @Independent @NotModified Function<? super V, ? extends VV> arg0,
            @Independent @NotModified FloatFunction<? super V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <VV> ImmutableObjectLongMap<VV> sumByInt(
            @Independent @NotModified Function<? super V, ? extends VV> arg0,
            @Independent @NotModified IntFunction<? super V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <VV> ImmutableObjectLongMap<VV> sumByLong(
            @Independent @NotModified Function<? super V, ? extends VV> arg0,
            @Independent @NotModified LongFunction<? super V> arg1) { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableOrderedMap<K, V> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableOrderedMap<K, V> takeWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableOrderedMap<K, V> tap(@Independent @NotModified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableOrderedMap<K, V> toReversed() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        <S> ImmutableList<Pair<V, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ImmutableList<Pair<V, Integer>> zipWithIndex() { return null; }
    }

    //public interface MapIterable implements RichIterable<V>
    @Independent(absent = true)
    class MapIterable$<K, V> {
        @Independent @Modified
        <K1, V1, V2> MapIterable<K1, V2> aggregateBy(
            @Independent @Modified Function<? super K, ? extends K1> keyFunction,
            @Independent @Modified Function<? super V, ? extends V1> valueFunction,
            @Independent @Modified Function0<? extends V2> zeroValueFactory,
            @Independent @Modified Function2<? super V2, ? super V1, ? extends V2> nonMutatingAggregator) { return null; }

        @Independent @Modified
        <K2, V2> MapIterable<K2, V2> collect(
            @Independent(hc = true) @Modified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        @Independent @Modified
        <R> MapIterable<R, V> collectKeysUnique(
            @Independent(hc = true) @Modified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        @Independent(absent = true) @Modified
        <R> MapIterable<K, R> collectValues(
            @Independent(hc = true) @Modified Function2<? super K, ? super V, ? extends R> arg0) { return null; }
        @NotModified boolean containsKey(@Independent @Modified Object arg0) { return false; }
        @NotModified boolean containsValue(@Independent @Modified Object arg0) { return false; }
        @Independent(hc = true) @NotModified
        Pair<K, V> detect(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        @Independent @Modified
        Optional<Pair<K, V>> detectOptional(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object arg0) { return false; }
        @Independent @Modified Multimap<V, K> flip() { return null; }
        @Independent(absent = true) @Modified MapIterable<V, K> flipUniqueValues() { return null; }
        @Modified void forEach(@Independent @Modified BiConsumer<? super K, ? super V> action) { }
        @NotModified void forEachKey(@Independent @Modified Procedure<? super K> arg0) { }
        @Modified void forEachKeyValue(@Independent @Modified Procedure2<? super K, ? super V> arg0) { }
        @Modified void forEachValue(@Independent @Modified Procedure<? super V> arg0) { }
        @Independent(hc = true) @NotModified V get(@Independent @Modified Object arg0) { return null; }
        @Independent(hc = true) @NotModified
        V getIfAbsent(@Independent(hc = true) @Modified K arg0, @Independent @Modified Function0<? extends V> arg1) {
            return null;
        }

        @Independent(hc = true) @NotModified
        V getIfAbsentValue(@Independent(hc = true) @Modified K arg0, @Independent(hc = true) @NotModified V arg1) {
            return null;
        }

        @Independent(hc = true) @Modified
        <P> V getIfAbsentWith(
            @Independent(hc = true) @Modified K arg0,
            @Independent(absent = true) @Modified Function<? super P, ? extends V> arg1,
            @Independent @Modified P arg2) { return null; }

        @Independent @Modified
        V getOrDefault(@Independent @Modified Object key, @Independent @NotModified V defaultValue) { return null; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Independent @Modified
        <A> A ifPresentApply(
            @Independent(hc = true) @Modified K arg0,
            @Independent(hc = true) @Modified Function<? super V, ? extends A> arg1) { return null; }

        @Independent @Modified
        <IV> IV injectIntoKeyValue(
            @Independent @NotModified IV injectedValue,
            @Independent @Modified Function3<? super IV, ? super K, ? super V, ? extends IV> function) { return null; }
        @Independent @Modified RichIterable<Pair<K, V>> keyValuesView() { return null; }
        @Independent @NotModified RichIterable<K> keysView() { return null; }
        @Independent @Modified Stream<V> parallelStream() { return null; }
        @Independent(hc = true) @Modified
        MapIterable<K, V> reject(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        @Independent(hc = true) @Modified
        MapIterable<K, V> select(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from java.lang.Iterable
        @Independent @Modified
        Spliterator<V> spliterator() { return null; }
        @Independent @Modified Stream<V> stream() { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        MapIterable<K, V> tap(@Independent @Modified Procedure<? super V> arg0) { return null; }
        @Independent @Modified ImmutableMapIterable<K, V> toImmutable() { return null; }
        //override from java.lang.Object, org.eclipse.collections.api.RichIterable
        @NotModified @NotNull
        public String toString() { return null; }
        @Independent(hc = true) @NotModified RichIterable<V> valuesView() { return null; }
    }

    //public interface MutableMap implements MutableMapIterable<K,V>, UnsortedMapIterable<K,V>, Cloneable
    //annotated as EXPECTED; computed mutable @Dependent (3) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableMap$<K, V> {
        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <K1, V1, V2> MutableMap<K1, V2> aggregateBy(
            @Independent @NotModified Function<? super K, ? extends K1> keyFunction,
            @Independent @NotModified Function<? super V, ? extends V1> valueFunction,
            @Independent @NotModified Function0<? extends V2> zeroValueFactory,
            @Independent @NotModified Function2<? super V2, ? super V1, ? extends V2> nonMutatingAggregator) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <KK, VV> MutableMap<KK, VV> aggregateBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function0<? extends VV> zeroValueFactory,
            @Independent @NotModified Function2<? super VV, ? super V, ? extends VV> nonMutatingAggregator) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <KK, VV> MutableMap<KK, VV> aggregateInPlaceBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function0<? extends VV> zeroValueFactory,
            @Independent @NotModified Procedure2<? super VV, ? super V> mutatingAggregator) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableMap<K, V> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableMap<K, V> asUnmodifiable() { return null; }
        @Independent(absent = true) @Modified protected MutableMap<K, V> clone() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        <R> MutableBag<R> collect(@Independent @NotModified Function<? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @NotModified

        <K2, V2> MutableMap<K2, V2> collect(
            @Independent(hc = true) @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        MutableBooleanBag collectBoolean(@Independent @NotModified BooleanFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        MutableByteBag collectByte(@Independent @NotModified ByteFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        MutableCharBag collectChar(@Independent @NotModified CharFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        MutableDoubleBag collectDouble(@Independent @NotModified DoubleFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        MutableFloatBag collectFloat(@Independent @NotModified FloatFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified

        <R> MutableBag<R> collectIf(
            @Independent @NotModified Predicate<? super V> arg0,
            @Independent @NotModified Function<? super V, ? extends R> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        MutableIntBag collectInt(@Independent @NotModified IntFunction<? super V> arg0) { return null; }

        @Independent @Modified
        <E> MutableMap<K, V> collectKeysAndValues(
            @Independent @NotModified Iterable<E> arg0,
            @Independent @NotModified Function<? super E, ? extends K> arg1,
            @Independent @NotModified Function<? super E, ? extends V> arg2) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <R> MutableMap<R, V> collectKeysUnique(
            @Independent(hc = true) @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        MutableLongBag collectLong(@Independent @NotModified LongFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        MutableShortBag collectShort(@Independent @NotModified ShortFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(absent = true) @Modified

        <R> MutableMap<K, R> collectValues(
            @Independent(hc = true) @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified

        <P, V1> MutableBag<V1> collectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends V1> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified

        <R> MutableBag<R> flatCollect(@Independent @NotModified Function<? super V, ? extends Iterable<R>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified

        <P, R> MutableBag<R> flatCollectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends Iterable<R>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        MutableSetMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @NotModified
        MutableMap<V, K> flipUniqueValues() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @Modified

        <VV> MutableBagMultimap<VV, V> groupBy(@Independent @NotModified Function<? super V, ? extends VV> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @Modified

        <VV> MutableBagMultimap<VV, V> groupByEach(
            @Independent @NotModified Function<? super V, ? extends Iterable<VV>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified

        <V1> MutableMap<V1, V> groupByUniqueKey(@Independent @NotModified Function<? super V, ? extends V1> function) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent(absent = true) @Modified
        MutableMap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @Modified
        PartitionMutableBag<V> partition(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @Modified

        <P> PartitionMutableBag<V> partitionWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <KK> MutableMap<KK, V> reduceBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function2<? super V, ? super V, ? extends V> reduceFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @Modified
        MutableBag<V> reject(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @Modified
        MutableMap<K, V> reject(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @Modified

        <P> MutableBag<V> rejectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @Modified
        MutableBag<V> select(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @Modified
        MutableMap<K, V> select(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        <S> MutableBag<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @Modified

        <P> MutableBag<V> selectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent @Modified
        MutableMap<K, V> tap(@Independent @NotModified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableMap<K, V> withAllKeyValueArguments(@Independent @NotModified Pair<? extends K, ? extends V> ... arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableMap<K, V> withAllKeyValues(
            @Independent(absent = true) @NotModified Iterable<? extends Pair<? extends K, ? extends V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent(hc = true) @Modified

        MutableMap<K, V> withKeyValue(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified V arg1) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Fluent @Independent @Modified
        MutableMap<K, V> withMap(@Independent @NotModified Map<? extends K, ? extends V> map) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Fluent @Independent @Modified

        MutableMap<K, V> withMapIterable(@Independent @NotModified MapIterable<? extends K, ? extends V> mapIterable) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableMap<K, V> withoutAllKeys(@Independent(hc = true) @NotModified Iterable<? extends K> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableMap<K, V> withoutKey(@Independent @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @Modified
        <S> MutableBag<Pair<V, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.UnsortedMapIterable
        @Independent(hc = true) @Modified
        MutableSet<Pair<V, Integer>> zipWithIndex() { return null; }
    }

    //public interface MutableMapIterable implements MapIterable<K,V>, Map<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (3) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableMapIterable$<K, V> {
        @Independent @Modified
        V add(@Independent @NotModified Pair<? extends K, ? extends V> keyValuePair) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified

        <K1, V1, V2> MutableMapIterable<K1, V2> aggregateBy(
            @Independent @NotModified Function<? super K, ? extends K1> keyFunction,
            @Independent @NotModified Function<? super V, ? extends V1> valueFunction,
            @Independent @NotModified Function0<? extends V2> zeroValueFactory,
            @Independent @NotModified Function2<? super V2, ? super V1, ? extends V2> nonMutatingAggregator) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <KK, VV> MutableMapIterable<KK, VV> aggregateBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function0<? extends VV> zeroValueFactory,
            @Independent @NotModified Function2<? super VV, ? super V, ? extends VV> nonMutatingAggregator) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <KK, VV> MutableMapIterable<KK, VV> aggregateInPlaceBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function0<? extends VV> zeroValueFactory,
            @Independent @NotModified Procedure2<? super VV, ? super V> mutatingAggregator) { return null; }
        @Independent @Modified MutableMapIterable<K, V> asSynchronized() { return null; }
        @Independent @Modified MutableMapIterable<K, V> asUnmodifiable() { return null; }
        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified

        <K2, V2> MutableMapIterable<K2, V2> collect(
            @Independent(hc = true) @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified

        <R> MutableMapIterable<R, V> collectKeysUnique(
            @Independent(hc = true) @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent(absent = true) @Modified

        <R> MutableMapIterable<K, R> collectValues(
            @Independent(hc = true) @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        <V1> MutableBag<V1> countBy(@Independent @NotModified Function<? super V, ? extends V1> function) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V1> MutableBag<V1> countByEach(@Independent @NotModified Function<? super V, ? extends Iterable<V1>> function) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V1, P> MutableBag<V1> countByWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends V1> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        MutableMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent(absent = true) @Modified
        MutableMapIterable<V, K> flipUniqueValues() { return null; }

        //override from java.util.Map, org.eclipse.collections.api.map.MapIterable
        @NotModified
        void forEach(@Independent @NotModified BiConsumer<? super K, ? super V> action) { }

        @Independent(hc = true) @Modified
        V getIfAbsentPut(@Independent(hc = true) @NotModified K arg0, @Independent(hc = true) @NotModified V arg1) {
            return null;
        }

        @Independent(hc = true) @Modified
        V getIfAbsentPut(
            @Independent(hc = true) @NotModified K arg0,
            @Independent @NotModified Function0<? extends V> arg1) { return null; }

        @Independent(hc = true) @Modified
        <P> V getIfAbsentPutWith(
            @Independent(hc = true) @NotModified K arg0,
            @Independent @NotModified Function<? super P, ? extends V> arg1,
            @Independent @NotModified P arg2) { return null; }

        @Independent(hc = true) @Modified
        V getIfAbsentPutWithKey(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Function<? super K, ? extends V> arg1) { return null; }

        //override from java.util.Map, org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        V getOrDefault(@Independent @NotModified Object key, @Independent @NotModified V defaultValue) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V1> MutableMultimap<V1, V> groupBy(@Independent @NotModified Function<? super V, ? extends V1> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V1> MutableMultimap<V1, V> groupByEach(
            @Independent @NotModified Function<? super V, ? extends Iterable<V1>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V1> MutableMapIterable<V1, V> groupByUniqueKey(
            @Independent @NotModified Function<? super V, ? extends V1> arg0) { return null; }
        @Independent(absent = true) @Modified MutableMapIterable<K, V> newEmpty() { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        PartitionMutableCollection<V> partition(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        @Modified
        void putAllMapIterable(@Independent @NotModified MapIterable<? extends K, ? extends V> mapIterable) { }

        @Independent @Modified
        V putPair(@Independent @NotModified Pair<? extends K, ? extends V> keyValuePair) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <KK> MutableMapIterable<KK, V> reduceBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function2<? super V, ? super V, ? extends V> reduceFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        MutableCollection<V> reject(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent(hc = true) @Modified
        MutableMapIterable<K, V> reject(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> MutableCollection<V> rejectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @Modified boolean removeAllKeys(@Independent @NotModified Set<? extends K> keys) { return false; }
        @Modified
        boolean removeIf(@Independent @NotModified Predicate2<? super K, ? super V> predicate) { return false; }
        @Independent(hc = true) @Modified V removeKey(@Independent(hc = true) @NotModified K arg0) { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        MutableCollection<V> select(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent(hc = true) @Modified
        MutableMapIterable<K, V> select(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        <S> MutableCollection<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> MutableCollection<V> selectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V1> MutableObjectDoubleMap<V1> sumByDouble(
            @Independent @NotModified Function<? super V, ? extends V1> arg0,
            @Independent @NotModified DoubleFunction<? super V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V1> MutableObjectDoubleMap<V1> sumByFloat(
            @Independent @NotModified Function<? super V, ? extends V1> arg0,
            @Independent @NotModified FloatFunction<? super V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V1> MutableObjectLongMap<V1> sumByInt(
            @Independent @NotModified Function<? super V, ? extends V1> arg0,
            @Independent @NotModified IntFunction<? super V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V1> MutableObjectLongMap<V1> sumByLong(
            @Independent @NotModified Function<? super V, ? extends V1> arg0,
            @Independent @NotModified LongFunction<? super V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        MutableMapIterable<K, V> tap(@Independent @NotModified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        ImmutableMapIterable<K, V> toImmutable() { return null; }

        @Independent(hc = true) @Modified
        V updateValue(
            @Independent(hc = true) @NotModified K arg0,
            @Independent @NotModified Function0<? extends V> arg1,
            @Independent(hc = true) @NotModified Function<? super V, ? extends V> arg2) { return null; }

        @Independent(hc = true) @Modified
        <P> V updateValueWith(
            @Independent(hc = true) @NotModified K arg0,
            @Independent @NotModified Function0<? extends V> arg1,
            @Independent(hc = true) @NotModified Function2<? super V, ? super P, ? extends V> arg2,
            @Independent @NotModified P arg3) { return null; }

        @Independent @Modified
        MutableMapIterable<K, V> withAllKeyValueArguments(
            @Independent @NotModified Pair<? extends K, ? extends V> ... arg0) { return null; }

        @Independent @Modified
        MutableMapIterable<K, V> withAllKeyValues(
            @Independent(absent = true) @NotModified Iterable<? extends Pair<? extends K, ? extends V>> arg0) {
            return null;
        }

        @Independent(hc = true) @Modified
        MutableMapIterable<K, V> withKeyValue(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified V arg1) { return null; }

        @Fluent @Independent @Modified
        MutableMapIterable<K, V> withMap(@Independent @NotModified Map<? extends K, ? extends V> map) { return null; }

        @Fluent @Independent @Modified
        MutableMapIterable<K, V> withMapIterable(
            @Independent @NotModified MapIterable<? extends K, ? extends V> mapIterable) { return null; }

        @Independent @Modified
        MutableMapIterable<K, V> withoutAllKeys(@Independent(hc = true) @NotModified Iterable<? extends K> arg0) {
            return null;
        }
        @Independent @Modified MutableMapIterable<K, V> withoutKey(@Independent @NotModified K arg0) { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        <S> MutableCollection<Pair<V, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        MutableCollection<Pair<V, Integer>> zipWithIndex() { return null; }
    }

    //public interface MutableOrderedMap implements OrderedMap<K,V>, MutableMapIterable<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (3) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableOrderedMap$<K, V> {
        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @NotModified

        <K1, V1, V2> MutableOrderedMap<K1, V2> aggregateBy(
            @Independent @NotModified Function<? super K, ? extends K1> arg0,
            @Independent @NotModified Function<? super V, ? extends V1> arg1,
            @Independent @NotModified Function0<? extends V2> arg2,
            @Independent @NotModified Function2<? super V2, ? super V1, ? extends V2> arg3) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <KK, VV> MutableOrderedMap<KK, VV> aggregateBy(
            @Independent @NotModified Function<? super V, ? extends KK> arg0,
            @Independent @NotModified Function0<? extends VV> arg1,
            @Independent @NotModified Function2<? super VV, ? super V, ? extends VV> arg2) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <KK, VV> MutableOrderedMap<KK, VV> aggregateInPlaceBy(
            @Independent @NotModified Function<? super V, ? extends KK> arg0,
            @Independent @NotModified Function0<? extends VV> arg1,
            @Independent @NotModified Procedure2<? super VV, ? super V> arg2) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableOrderedMap<K, V> asUnmodifiable() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        <VV> MutableList<VV> collect(@Independent @NotModified Function<? super V, ? extends VV> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @NotModified

        <K2, V2> MutableOrderedMap<K2, V2> collect(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableBooleanList collectBoolean(@Independent @NotModified BooleanFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableByteList collectByte(@Independent @NotModified ByteFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableCharList collectChar(@Independent @NotModified CharFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableDoubleList collectDouble(@Independent @NotModified DoubleFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableFloatList collectFloat(@Independent @NotModified FloatFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V1> MutableList<V1> collectIf(
            @Independent @NotModified Predicate<? super V> arg0,
            @Independent @NotModified Function<? super V, ? extends V1> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableIntList collectInt(@Independent @NotModified IntFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @NotModified

        <R> MutableOrderedMap<R, V> collectKeysUnique(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableLongList collectLong(@Independent @NotModified LongFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableShortList collectShort(@Independent @NotModified ShortFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @NotModified

        <R> MutableOrderedMap<K, R> collectValues(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V1> MutableList<V1> collectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends V1> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        MutableList<V> distinct() { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(absent = true) @NotModified
        MutableOrderedMap<K, V> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        MutableOrderedMap<K, V> dropWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V1> MutableList<V1> flatCollect(@Independent @NotModified Function<? super V, ? extends Iterable<V1>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V1> MutableList<V1> flatCollectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends Iterable<V1>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @NotModified
        MutableListMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent(absent = true) @NotModified
        MutableOrderedMap<V, K> flipUniqueValues() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V1> MutableListMultimap<V1, V> groupBy(@Independent @NotModified Function<? super V, ? extends V1> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V1> MutableListMultimap<V1, V> groupByEach(
            @Independent @NotModified Function<? super V, ? extends Iterable<V1>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @Modified

        <V1> MutableOrderedMap<V1, V> groupByUniqueKey(@Independent @NotModified Function<? super V, ? extends V1> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @NotModified
        MutableOrderedMap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        PartitionMutableList<V> partition(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        PartitionMutableList<V> partitionWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P> PartitionMutableList<V> partitionWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <KK> MutableOrderedMap<KK, V> reduceBy(
            @Independent @NotModified Function<? super V, ? extends KK> arg0,
            @Independent @NotModified Function2<? super V, ? super V, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableList<V> reject(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @NotModified
        MutableOrderedMap<K, V> reject(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P> MutableList<V> rejectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableList<V> select(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap
        @Independent @NotModified
        MutableOrderedMap<K, V> select(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        <S> MutableList<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P> MutableList<V> selectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(absent = true) @NotModified
        MutableOrderedMap<K, V> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        MutableOrderedMap<K, V> takeWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableOrderedMap<K, V> tap(@Independent @NotModified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        MutableOrderedMap<K, V> toReversed() { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableOrderedMap<K, V> withAllKeyValueArguments(
            @Independent @NotModified Pair<? extends K, ? extends V> ... arg0) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableOrderedMap<K, V> withAllKeyValues(
            @Independent(absent = true) @NotModified Iterable<? extends Pair<? extends K, ? extends V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableOrderedMap<K, V> withKeyValue(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified V arg1) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Fluent @Independent @Modified
        MutableOrderedMap<K, V> withMap(@Independent @NotModified Map<? extends K, ? extends V> map) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Fluent @Independent @Modified

        MutableOrderedMap<K, V> withMapIterable(
            @Independent @NotModified MapIterable<? extends K, ? extends V> mapIterable) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableOrderedMap<K, V> withoutAllKeys(@Independent @NotModified Iterable<? extends K> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableOrderedMap<K, V> withoutKey(@Independent @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        <S> MutableList<Pair<V, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable, org.eclipse.collections.api.map.OrderedMap, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        MutableList<Pair<V, Integer>> zipWithIndex() { return null; }
    }

    //public interface OrderedMap implements MapIterable<K,V>, ReversibleIterable<V>
    @Independent(absent = true)
    class OrderedMap$<K, V> {
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        <VV> ListIterable<VV> collect(@Independent @Modified Function<? super V, ? extends VV> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <K2, V2> OrderedMap<K2, V2> collect(@Independent @Modified Function2<? super K, ? super V, Pair<K2, V2>> arg0) {
            return null;
        }

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
        @Independent @NotModified

        <R> OrderedMap<K, R> collectValues(@Independent @Modified Function2<? super K, ? super V, ? extends R> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P, V1> ListIterable<V1> collectWith(
            @Independent @Modified Function2<? super V, ? super P, ? extends V1> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        ListIterable<V> distinct() { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(absent = true) @NotModified
        OrderedMap<K, V> drop(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        OrderedMap<K, V> dropWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

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
        @Independent @NotModified
        ListMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent(absent = true) @NotModified
        OrderedMap<V, K> flipUniqueValues() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        <V1> ListMultimap<V1, V> groupBy(@Independent @Modified Function<? super V, ? extends V1> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <V1> ListMultimap<V1, V> groupByEach(@Independent @Modified Function<? super V, ? extends Iterable<V1>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V1> OrderedMap<V1, V> groupByUniqueKey(@Independent @Modified Function<? super V, ? extends V1> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        PartitionList<V> partition(@Independent @Modified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        PartitionList<V> partitionWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P> PartitionList<V> partitionWith(
            @Independent @Modified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        ListIterable<V> reject(@Independent @Modified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified
        OrderedMap<K, V> reject(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P> ListIterable<V> rejectWith(
            @Independent @Modified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        ListIterable<V> select(@Independent @Modified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified
        OrderedMap<K, V> select(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        <S> ListIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified

        <P> ListIterable<V> selectWith(
            @Independent @Modified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent(absent = true) @NotModified
        OrderedMap<K, V> take(int arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        OrderedMap<K, V> takeWhile(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        OrderedMap<K, V> tap(@Independent @Modified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified
        ImmutableOrderedMap<K, V> toImmutable() { return null; }

        //override from org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        OrderedMap<K, V> toReversed() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @NotModified
        <S> ListIterable<Pair<V, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.ordered.ReversibleIterable
        @Independent @Modified
        ListIterable<Pair<V, Integer>> zipWithIndex() { return null; }
    }

    //public interface UnsortedMapIterable implements MapIterable<K,V>
    @Independent(absent = true)
    class UnsortedMapIterable$<K, V> {
        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        <V1> Bag<V1> collect(@Independent @Modified Function<? super V, ? extends V1> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <K2, V2> UnsortedMapIterable<K2, V2> collect(
            @Independent(hc = true) @Modified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        BooleanBag collectBoolean(@Independent @Modified BooleanFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ByteBag collectByte(@Independent @Modified ByteFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        CharBag collectChar(@Independent @Modified CharFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        DoubleBag collectDouble(@Independent @Modified DoubleFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        FloatBag collectFloat(@Independent @Modified FloatFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V1> Bag<V1> collectIf(
            @Independent @Modified Predicate<? super V> arg0,
            @Independent @Modified Function<? super V, ? extends V1> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        IntBag collectInt(@Independent @Modified IntFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LongBag collectLong(@Independent @Modified LongFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ShortBag collectShort(@Independent @Modified ShortFunction<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent(absent = true) @Modified

        <R> UnsortedMapIterable<K, R> collectValues(
            @Independent(hc = true) @Modified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <P, V1> Bag<V1> collectWith(
            @Independent @Modified Function2<? super V, ? super P, ? extends V1> arg0,
            @Independent @Modified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        <V1> Bag<V1> flatCollect(@Independent @Modified Function<? super V, ? extends Iterable<V1>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <P, V1> Bag<V1> flatCollectWith(
            @Independent @Modified Function2<? super V, ? super P, ? extends Iterable<V1>> function,
            @Independent @Modified P parameter) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        UnsortedSetMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent(hc = true) @Modified
        UnsortedMapIterable<V, K> flipUniqueValues() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        <V1> BagMultimap<V1, V> groupBy(@Independent @Modified Function<? super V, ? extends V1> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V1> BagMultimap<V1, V> groupByEach(@Independent @Modified Function<? super V, ? extends Iterable<V1>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V1> UnsortedMapIterable<V1, V> groupByUniqueKey(@Independent @Modified Function<? super V, ? extends V1> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        PartitionBag<V> partition(@Independent @Modified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> PartitionBag<V> partitionWith(
            @Independent @Modified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        Bag<V> reject(@Independent @Modified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent(hc = true) @Modified
        UnsortedMapIterable<K, V> reject(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> Bag<V> rejectWith(
            @Independent @Modified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        Bag<V> select(@Independent @Modified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent(hc = true) @Modified
        UnsortedMapIterable<K, V> select(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        <S> Bag<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> Bag<V> selectWith(
            @Independent @Modified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        UnsortedMapIterable<K, V> tap(@Independent @Modified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        ImmutableMap<K, V> toImmutable() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        <S> Bag<Pair<V, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        UnsortedSetIterable<Pair<V, Integer>> zipWithIndex() { return null; }
    }
}
