package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Collection;
import java.util.Spliterator;
import java.util.stream.Stream;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.function.*;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.collection.FixedSizeCollection;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.collection.primitive.*;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.ImmutableMapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.*;
import org.eclipse.collections.api.multimap.ImmutableMultimap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.partition.PartitionImmutableCollection;
import org.eclipse.collections.api.partition.PartitionMutableCollection;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.Twin;
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
public class OrgEclipseCollectionsApiCollection {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.collection";
    //public interface FixedSizeCollection implements MutableCollection<T>
    @Independent(absent = true)
    class FixedSizeCollection$<T> {
        //override from java.util.Collection
        @NotModified
        boolean add(@Independent @NotModified T arg0) { return false; }

        //override from java.util.Collection
        @NotModified
        boolean addAll(@Independent @NotModified Collection<? extends T> arg0) { return false; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @NotModified
        boolean addAllIterable(@Independent @NotModified Iterable<? extends T> arg0) { return false; }

        //override from java.util.Collection
        @NotModified
        void clear() { }

        //override from java.util.Collection
        @NotModified
        boolean remove(@Independent @NotModified Object arg0) { return false; }

        //override from java.util.Collection
        @NotModified
        boolean removeAll(@Independent @NotModified Collection<?> arg0) { return false; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @NotModified
        boolean removeAllIterable(@Independent @NotModified Iterable<?> arg0) { return false; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @NotModified
        boolean removeIf(@Independent @NotModified Predicate<? super T> arg0) { return false; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @NotModified

        <P> boolean removeIfWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return false; }

        //override from java.util.Collection
        @NotModified
        boolean retainAll(@Independent @NotModified Collection<?> arg0) { return false; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @NotModified
        boolean retainAllIterable(@Independent @NotModified Iterable<?> arg0) { return false; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        FixedSizeCollection<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified
        MutableCollection<T> with(@Independent(hc = true) @Modified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableCollection<T> withAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableCollection<T> without(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableCollection<T> withoutAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }
    }

    //public interface ImmutableCollection implements RichIterable<T>
    //annotated as EXPECTED; computed mutable @Independent(hc = true) (conditional) (1) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableCollection$<T> {
        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <K, V> ImmutableMap<K, V> aggregateBy(
            @Independent @NotModified Function<? super T, ? extends K> groupBy,
            @Independent @NotModified Function0<? extends V> zeroValueFactory,
            @Independent @NotModified Function2<? super V, ? super T, ? extends V> nonMutatingAggregator) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <K, V> ImmutableMap<K, V> aggregateInPlaceBy(
            @Independent @NotModified Function<? super T, ? extends K> groupBy,
            @Independent @NotModified Function0<? extends V> zeroValueFactory,
            @Independent @NotModified Procedure2<? super V, ? super T> mutatingAggregator) { return null; }
        @Independent @NotModified Collection<T> castToCollection() { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V> ImmutableCollection<V> collect(@Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        ImmutableBooleanCollection collectBoolean(@Independent @NotModified BooleanFunction<? super T> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableByteCollection collectByte(@Independent @NotModified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableCharCollection collectChar(@Independent @NotModified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableDoubleCollection collectDouble(@Independent @NotModified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableFloatCollection collectFloat(@Independent @NotModified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V> ImmutableCollection<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableIntCollection collectInt(@Independent @NotModified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableLongCollection collectLong(@Independent @NotModified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableShortCollection collectShort(@Independent @NotModified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <P, V> ImmutableCollection<V> collectWith(
            @Independent(hc = true) @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        <V> ImmutableBag<V> countBy(@Independent @NotModified Function<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V> ImmutableBag<V> countByEach(@Independent @NotModified Function<? super T, ? extends Iterable<V>> function) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V, P> ImmutableBag<V> countByWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V> ImmutableCollection<V> flatCollect(
            @Independent(hc = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <P, V> ImmutableCollection<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @NotModified

        <V> ImmutableMultimap<V, T> groupBy(@Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @NotModified

        <V> ImmutableMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V> ImmutableMap<V, T> groupByUniqueKey(@Independent @NotModified Function<? super T, ? extends V> function) {
            return null;
        }

        @Independent(hc = true) @NotModified
        ImmutableCollection<T> newWith(@Independent @NotModified T arg0) { return null; }

        @Independent @NotModified
        ImmutableCollection<T> newWithAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified ImmutableCollection<T> newWithout(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified
        ImmutableCollection<T> newWithoutAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified Stream<T> parallelStream() { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @NotModified
        PartitionImmutableCollection<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <P> PartitionImmutableCollection<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <K> ImmutableMapIterable<K, T> reduceBy(
            @Independent @NotModified Function<? super T, ? extends K> groupBy,
            @Independent @NotModified Function2<? super T, ? super T, ? extends T> reduceFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableCollection<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <P> ImmutableCollection<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableCollection<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        <S> ImmutableCollection<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <P> ImmutableCollection<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from java.lang.Iterable
        @Independent @NotModified
        Spliterator<T> spliterator() { return null; }
        @Independent @NotModified Stream<T> stream() { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V> ImmutableObjectDoubleMap<V> sumByDouble(
            @Independent @NotModified Function<? super T, ? extends V> arg0,
            @Independent @NotModified DoubleFunction<? super T> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V> ImmutableObjectDoubleMap<V> sumByFloat(
            @Independent @NotModified Function<? super T, ? extends V> arg0,
            @Independent @NotModified FloatFunction<? super T> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V> ImmutableObjectLongMap<V> sumByInt(
            @Independent @NotModified Function<? super T, ? extends V> arg0,
            @Independent @NotModified IntFunction<? super T> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V> ImmutableObjectLongMap<V> sumByLong(
            @Independent @NotModified Function<? super T, ? extends V> arg0,
            @Independent @NotModified LongFunction<? super T> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableCollection<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        <S> ImmutableCollection<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        ImmutableCollection<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface MutableCollection implements Collection<T>, RichIterable<T>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableCollection$<T> {
        @Modified
        boolean addAllIterable(@Independent(hc = true) @NotModified Iterable<? extends T> arg0) { return false; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <K, V> MutableMap<K, V> aggregateBy(
            @Independent @NotModified Function<? super T, ? extends K> groupBy,
            @Independent @NotModified Function0<? extends V> zeroValueFactory,
            @Independent @NotModified Function2<? super V, ? super T, ? extends V> nonMutatingAggregator) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <K, V> MutableMap<K, V> aggregateInPlaceBy(
            @Independent @NotModified Function<? super T, ? extends K> groupBy,
            @Independent @NotModified Function0<? extends V> zeroValueFactory,
            @Independent @NotModified Procedure2<? super V, ? super T> mutatingAggregator) { return null; }
        @Independent @Modified MutableCollection<T> asSynchronized() { return null; }
        @Independent @Modified MutableCollection<T> asUnmodifiable() { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> MutableCollection<V> collect(@Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        MutableBooleanCollection collectBoolean(@Independent @NotModified BooleanFunction<? super T> booleanFunction) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        MutableByteCollection collectByte(@Independent @NotModified ByteFunction<? super T> byteFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        MutableCharCollection collectChar(@Independent @NotModified CharFunction<? super T> charFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        MutableDoubleCollection collectDouble(@Independent @NotModified DoubleFunction<? super T> doubleFunction) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        MutableFloatCollection collectFloat(@Independent @NotModified FloatFunction<? super T> floatFunction) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> MutableCollection<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        MutableIntCollection collectInt(@Independent @NotModified IntFunction<? super T> intFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        MutableLongCollection collectLong(@Independent @NotModified LongFunction<? super T> longFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        MutableShortCollection collectShort(@Independent @NotModified ShortFunction<? super T> shortFunction) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <P, V> MutableCollection<V> collectWith(
            @Independent(hc = true) @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        <V> MutableBag<V> countBy(@Independent @NotModified Function<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> MutableBag<V> countByEach(@Independent @NotModified Function<? super T, ? extends Iterable<V>> function) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V, P> MutableBag<V> countByWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> MutableCollection<V> flatCollect(
            @Independent(hc = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <P, V> MutableCollection<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified

        <V> MutableMultimap<V, T> groupBy(@Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified

        <V> MutableMultimap<V, T> groupByEach(
            @Independent(hc = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> MutableMap<V, T> groupByUniqueKey(@Independent @NotModified Function<? super T, ? extends V> function) {
            return null;
        }

        @Independent @Modified
        <IV, P> IV injectIntoWith(
            @Independent @NotModified IV arg0,
            @Independent(hc = true) @NotModified Function3<? super IV, ? super T, ? super P, ? extends IV> arg1,
            @Independent @NotModified P arg2) { return null; }
        @Independent(absent = true) @Modified MutableCollection<T> newEmpty() { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified
        PartitionMutableCollection<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified

        <P> PartitionMutableCollection<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified
        MutableCollection<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified

        <P> MutableCollection<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @Modified boolean removeAllIterable(@Independent(hc = true) @NotModified Iterable<?> arg0) { return false; }
        @Modified boolean removeIf(@Independent @NotModified Predicate<? super T> arg0) { return false; }
        @Modified
        <P> boolean removeIfWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return false; }
        @Modified boolean retainAllIterable(@Independent @NotModified Iterable<?> arg0) { return false; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified
        MutableCollection<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        @Independent @Modified
        <P> Twin<MutableList<T>> selectAndRejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified
        <S> MutableCollection<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified

        <P> MutableCollection<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V> MutableObjectDoubleMap<V> sumByDouble(
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0,
            @Independent @NotModified DoubleFunction<? super T> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V> MutableObjectDoubleMap<V> sumByFloat(
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0,
            @Independent @NotModified FloatFunction<? super T> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> MutableObjectLongMap<V> sumByInt(
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0,
            @Independent @NotModified IntFunction<? super T> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> MutableObjectLongMap<V> sumByLong(
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0,
            @Independent @NotModified LongFunction<? super T> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        MutableCollection<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from java.util.Collection, org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        Object [] toArray() { return null; }

        //override from java.util.Collection, org.eclipse.collections.api.RichIterable
        @Independent @NotModified
        <T1> T1 [] toArray(@Independent @NotModified T1 [] a) { return null; }
        @Independent @Modified ImmutableCollection<T> toImmutable() { return null; }
        @Independent(hc = true) @Modified
        MutableCollection<T> with(@Independent(hc = true) @NotModified T arg0) { return null; }

        @Independent @Modified
        MutableCollection<T> withAll(@Independent(hc = true) @NotModified Iterable<? extends T> arg0) { return null; }

        @Independent @Modified
        MutableCollection<T> without(@Independent(hc = true) @NotModified T arg0) { return null; }

        @Independent @Modified
        MutableCollection<T> withoutAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        <S> MutableCollection<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        MutableCollection<Pair<T, Integer>> zipWithIndex() { return null; }
    }
}
