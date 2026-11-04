package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Map;
import org.eclipse.collections.api.bag.ImmutableBagIterable;
import org.eclipse.collections.api.bimap.BiMap;
import org.eclipse.collections.api.bimap.ImmutableBiMap;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.map.*;
import org.eclipse.collections.api.multimap.set.ImmutableSetMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.multimap.set.SetMultimap;
import org.eclipse.collections.api.partition.set.PartitionImmutableSet;
import org.eclipse.collections.api.partition.set.PartitionMutableSet;
import org.eclipse.collections.api.partition.set.PartitionUnsortedSet;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
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
public class OrgEclipseCollectionsApiBimap {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.bimap";
    //public interface BiMap implements MapIterable<K,V>
    @Independent(absent = true)
    class BiMap$<K, V> {
        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified

        <K2, V2> BiMap<K2, V2> collect(@Independent @Modified Function2<? super K, ? super V, Pair<K2, V2>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <R> BiMap<R, V> collectKeysUnique(@Independent @Modified Function2<? super K, ? super V, ? extends R> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified

        <R> BiMap<K, R> collectValues(@Independent @Modified Function2<? super K, ? super V, ? extends R> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        SetMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent(absent = true) @NotModified
        BiMap<V, K> flipUniqueValues() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        <V1> SetMultimap<V1, V> groupBy(@Independent @Modified Function<? super V, ? extends V1> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V1> SetMultimap<V1, V> groupByEach(@Independent @Modified Function<? super V, ? extends Iterable<V1>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        <VV> BiMap<VV, V> groupByUniqueKey(@Independent @Modified Function<? super V, ? extends VV> arg0) { return null; }

        @Independent(absent = true) @Modified @NotModified(after = "delegate,lock")
        BiMap<V, K> inverse() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        PartitionUnsortedSet<V> partition(@Independent @Modified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> PartitionUnsortedSet<V> partitionWith(
            @Independent @Modified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        SetIterable<V> reject(@Independent @Modified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        BiMap<K, V> reject(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> SetIterable<V> rejectWith(
            @Independent @Modified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        SetIterable<V> select(@Independent @Modified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        BiMap<K, V> select(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        <S> SetIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> SetIterable<V> selectWith(
            @Independent @Modified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        BiMap<K, V> tap(@Independent @Modified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MapIterable
        @Independent @Modified
        ImmutableBiMap<K, V> toImmutable() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        <S> SetIterable<Pair<V, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        SetIterable<Pair<V, Integer>> zipWithIndex() { return null; }
    }

    //public interface ImmutableBiMap implements BiMap<K,V>, ImmutableMapIterable<K,V>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableBiMap$<K, V> {
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

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @NotModified

        <V1> ImmutableBagIterable<V1> collect(@Independent @NotModified Function<? super V, ? extends V1> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <K2, V2> ImmutableBiMap<K2, V2> collect(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @NotModified

        <V1> ImmutableBagIterable<V1> collectIf(
            @Independent @NotModified Predicate<? super V> arg0,
            @Independent @NotModified Function<? super V, ? extends V1> arg1) { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <R> ImmutableBiMap<R, V> collectKeysUnique(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified

        <R> ImmutableBiMap<K, R> collectValues(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @NotModified

        <P, V1> ImmutableBagIterable<V1> collectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends V1> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @NotModified

        <V1> ImmutableBagIterable<V1> flatCollect(
            @Independent @NotModified Function<? super V, ? extends Iterable<V1>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <P, V1> ImmutableBagIterable<V1> flatCollectWith(
            @Independent @NotModified Function2<? super V, ? super P, ? extends Iterable<V1>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified
        ImmutableSetMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent(hc = true) @NotModified
        ImmutableBiMap<V, K> flipUniqueValues() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <V1> ImmutableSetMultimap<V1, V> groupBy(@Independent @NotModified Function<? super V, ? extends V1> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <V1> ImmutableSetMultimap<V1, V> groupByEach(
            @Independent @NotModified Function<? super V, ? extends Iterable<V1>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <VV> ImmutableBiMap<VV, V> groupByUniqueKey(
            @Independent @NotModified Function<? super V, ? extends VV> function) { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap
        @Independent(hc = true) @NotModified
        ImmutableBiMap<V, K> inverse() { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableBiMap<K, V> newWithAllKeyValueArguments(
            @Independent @NotModified Pair<? extends K, ? extends V> ... arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableBiMap<K, V> newWithAllKeyValues(
            @Independent @NotModified Iterable<? extends Pair<? extends K, ? extends V>> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableBiMap<K, V> newWithKeyValue(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableBiMap<K, V> newWithMap(@Independent @NotModified Map<? extends K, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        ImmutableBiMap<K, V> newWithMapIterable(@Independent @NotModified MapIterable<? extends K, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableBiMap<K, V> newWithoutAllKeys(@Independent @NotModified Iterable<? extends K> arg0) { return null; }

        //override from org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableBiMap<K, V> newWithoutKey(@Independent @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        PartitionImmutableSet<V> partition(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap
        @Independent @NotModified

        <P> PartitionImmutableSet<V> partitionWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <KK> ImmutableMapIterable<KK, V> reduceBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function2<? super V, ? super V, ? extends V> reduceFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableSet<V> reject(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified
        ImmutableBiMap<K, V> reject(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <P> ImmutableSet<V> rejectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableSet<V> select(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified
        ImmutableBiMap<K, V> select(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        <S> ImmutableSet<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified

        <P> ImmutableSet<V> selectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable, org.eclipse.collections.api.map.MapIterable
        @Independent @NotModified
        ImmutableBiMap<K, V> tap(@Independent @NotModified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        <S> ImmutableSet<Pair<V, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.ImmutableMapIterable
        @Independent @NotModified
        ImmutableSet<Pair<V, Integer>> zipWithIndex() { return null; }
    }

    //public interface MutableBiMap implements BiMap<K,V>, MutableMapIterable<K,V>, Cloneable
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableBiMap$<K, V> {
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

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @NotModified
        MutableBiMap<K, V> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableBiMap<K, V> asUnmodifiable() { return null; }
        @Independent @NotModified protected MutableBiMap<K, V> clone() { return null; }
        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <K2, V2> MutableBiMap<K2, V2> collect(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @NotModified

        <R> MutableBiMap<R, V> collectKeysUnique(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <R> MutableBiMap<K, R> collectValues(
            @Independent @NotModified Function2<? super K, ? super V, ? extends R> arg0) { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableSetMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @NotModified
        MutableBiMap<V, K> flipUniqueValues() { return null; }

        @Independent(hc = true) @Modified
        V forcePut(@Independent(hc = true) @NotModified K arg0, @Independent(hc = true) @NotModified V arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <V1> MutableSetMultimap<V1, V> groupBy(@Independent @NotModified Function<? super V, ? extends V1> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <V1> MutableSetMultimap<V1, V> groupByEach(
            @Independent @NotModified Function<? super V, ? extends Iterable<V1>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <VV> MutableBiMap<VV, V> groupByUniqueKey(@Independent @NotModified Function<? super V, ? extends VV> function) {
            return null;
        }

        //override from org.eclipse.collections.api.bimap.BiMap
        @Independent(absent = true) @Modified @NotModified(after = "delegate,lock")
        MutableBiMap<V, K> inverse() { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @NotModified
        MutableBiMap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent(hc = true) @Modified
        PartitionMutableSet<V> partition(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap
        @Independent(hc = true) @Modified

        <P> PartitionMutableSet<V> partitionWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from java.util.Map
        @Independent(hc = true) @Modified
        V put(@Independent(hc = true) @NotModified K arg0, @Independent(hc = true) @NotModified V arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        <KK> MutableMap<KK, V> reduceBy(
            @Independent @NotModified Function<? super V, ? extends KK> groupBy,
            @Independent @NotModified Function2<? super V, ? super V, ? extends V> reduceFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent(hc = true) @Modified
        MutableSet<V> reject(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableBiMap<K, V> reject(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent(hc = true) @Modified

        <P> MutableSet<V> rejectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent(hc = true) @Modified
        MutableSet<V> select(@Independent @NotModified Predicate<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableBiMap<K, V> select(@Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        <S> MutableSet<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent(hc = true) @Modified

        <P> MutableSet<V> selectWith(
            @Independent @NotModified Predicate2<? super V, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MapIterable, org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableBiMap<K, V> tap(@Independent @NotModified Procedure<? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableBiMap<K, V> withAllKeyValueArguments(@Independent @NotModified Pair<? extends K, ? extends V> ... arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableBiMap<K, V> withAllKeyValues(
            @Independent(absent = true) @NotModified Iterable<? extends Pair<? extends K, ? extends V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableBiMap<K, V> withKeyValue(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified V arg1) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Fluent @Independent @Modified
        MutableBiMap<K, V> withMap(@Independent @NotModified Map<? extends K, ? extends V> map) { return null; }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Fluent @Independent @Modified

        MutableBiMap<K, V> withMapIterable(@Independent @NotModified MapIterable<? extends K, ? extends V> mapIterable) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified

        MutableBiMap<K, V> withoutAllKeys(@Independent(hc = true) @NotModified Iterable<? extends K> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.map.MutableMapIterable
        @Independent @Modified
        MutableBiMap<K, V> withoutKey(@Independent @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent(hc = true) @Modified
        <S> MutableSet<Pair<V, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.bimap.BiMap, org.eclipse.collections.api.map.MutableMapIterable
        @Independent(hc = true) @Modified
        MutableSet<Pair<V, Integer>> zipWithIndex() { return null; }
    }
}
