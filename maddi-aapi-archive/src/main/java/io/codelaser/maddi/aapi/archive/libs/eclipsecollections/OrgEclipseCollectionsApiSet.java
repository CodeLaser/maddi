package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.multimap.set.*;
import org.eclipse.collections.api.partition.set.*;
import org.eclipse.collections.api.set.*;
import org.eclipse.collections.api.set.primitive.*;
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
public class OrgEclipseCollectionsApiSet {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.set";
    //public interface FixedSizeSet implements MutableSet<T>, FixedSizeCollection<T>
    @Independent(absent = true)
    class FixedSizeSet$<T> {
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.FixedSizeCollection, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSet, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified
        FixedSizeSet<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.FixedSizeCollection, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSet, org.eclipse.collections.api.set.MutableSetIterable
        @Independent @Modified
        MutableSet<T> with(@Independent(hc = true) @Modified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.FixedSizeCollection, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSet, org.eclipse.collections.api.set.MutableSetIterable
        @Independent @Modified
        MutableSet<T> withAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.FixedSizeCollection, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSet, org.eclipse.collections.api.set.MutableSetIterable
        @Independent @NotModified
        MutableSet<T> without(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.FixedSizeCollection, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSet, org.eclipse.collections.api.set.MutableSetIterable
        @Independent @Modified
        MutableSet<T> withoutAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }
    }

    //public interface ImmutableSet implements UnsortedSetIterable<T>, ImmutableSetIterable<T>
    //annotated as EXPECTED; computed mutable @Dependent (2) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableSet$<T> {
        @Independent @NotModified Set<T> castToSet() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        <V> ImmutableSet<V> collect(@Independent @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableBooleanSet collectBoolean(@Independent @NotModified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableByteSet collectByte(@Independent @NotModified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableCharSet collectChar(@Independent @NotModified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableDoubleSet collectDouble(@Independent @NotModified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableFloatSet collectFloat(@Independent @NotModified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified

        <V> ImmutableSet<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableIntSet collectInt(@Independent @NotModified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableLongSet collectLong(@Independent @NotModified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableShortSet collectShort(@Independent @NotModified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified

        <P, V> ImmutableSet<V> collectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableSet<T> difference(@Independent @NotModified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified

        <V> ImmutableSet<V> flatCollect(@Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified

        <P, V> ImmutableSet<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified

        <V> ImmutableSetMultimap<V, T> groupBy(@Independent @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified

        <V> ImmutableSetMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableSet<T> intersect(@Independent @NotModified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSet<T> newWith(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSet<T> newWithAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSet<T> newWithout(@Independent @NotModified T arg0) { return null; }

        //override from org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified
        ImmutableSet<T> newWithoutAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified
        PartitionImmutableSet<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified

        <P> PartitionImmutableSet<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableSet<UnsortedSetIterable<T>> powerSet() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableSet<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified

        <P> ImmutableSet<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableSet<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        <S> ImmutableSet<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified

        <P> ImmutableSet<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableSet<T> symmetricDifference(@Independent @NotModified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableSet<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Fluent @Independent @NotModified
        ImmutableSet<T> toImmutableSet() { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableSet<T> union(@Independent(hc = true) @NotModified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        <S> ImmutableSet<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.ImmutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @NotModified
        ImmutableSet<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface ImmutableSetIterable implements SetIterable<T>, ImmutableCollection<T>
    //annotated as EXPECTED; computed mutable @Dependent (2) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableSetIterable$<T> {
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <V> ImmutableSetIterableMultimap<V, T> groupBy(@Independent @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection
        @Independent @NotModified

        <V> ImmutableSetIterableMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified
        PartitionImmutableSetIterable<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified

        <P> PartitionImmutableSetIterable<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified
        ImmutableSetIterable<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified

        <P> ImmutableSetIterable<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified
        ImmutableSetIterable<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified
        <S> ImmutableSetIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified

        <P> ImmutableSetIterable<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified
        ImmutableSetIterable<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.ImmutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent @NotModified
        ImmutableSetIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface MultiReaderSet implements MutableSet<T>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- a mutable collection behind a lock (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MultiReaderSet$<T> {
        //override from org.eclipse.collections.api.set.MutableSet
        @Independent @Modified
        protected MultiReaderSet<T> clone() { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSet
        @Independent @NotModified
        MultiReaderSet<T> newEmpty() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSet, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Fluent @Independent @Modified
        MultiReaderSet<T> tap(@Independent @NotModified Procedure<? super T> procedure) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSet, org.eclipse.collections.api.set.MutableSetIterable
        @Fluent @Independent @Modified
        MultiReaderSet<T> with(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSet, org.eclipse.collections.api.set.MutableSetIterable
        @Fluent @Independent @Modified
        MultiReaderSet<T> withAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }
        @Modified void withReadLockAndDelegate(@Independent @NotModified Procedure<? super MutableSet<T>> arg0) { }
        @Modified void withWriteLockAndDelegate(@Independent @NotModified Procedure<? super MutableSet<T>> arg0) { }
        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSet, org.eclipse.collections.api.set.MutableSetIterable
        @Fluent @Independent @Modified
        MultiReaderSet<T> without(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSet, org.eclipse.collections.api.set.MutableSetIterable
        @Fluent @Independent @Modified
        MultiReaderSet<T> withoutAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }
    }

    //public interface MutableSet implements UnsortedSetIterable<T>, MutableSetIterable<T>, Cloneable
    //annotated as EXPECTED; computed mutable @Dependent (2) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableSet$<T> {
        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableSet<T> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent @Modified
        MutableSet<T> asUnmodifiable() { return null; }
        @Independent(absent = true) @Modified protected MutableSet<T> clone() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified

        <V> MutableSet<V> collect(@Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified

        MutableBooleanSet collectBoolean(@Independent @NotModified BooleanFunction<? super T> booleanFunction) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified
        MutableByteSet collectByte(@Independent @NotModified ByteFunction<? super T> byteFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified
        MutableCharSet collectChar(@Independent @NotModified CharFunction<? super T> charFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified

        MutableDoubleSet collectDouble(@Independent @NotModified DoubleFunction<? super T> doubleFunction) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified
        MutableFloatSet collectFloat(@Independent @NotModified FloatFunction<? super T> floatFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified

        <V> MutableSet<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified
        MutableIntSet collectInt(@Independent @NotModified IntFunction<? super T> intFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified
        MutableLongSet collectLong(@Independent @NotModified LongFunction<? super T> longFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified
        MutableShortSet collectShort(@Independent @NotModified ShortFunction<? super T> shortFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified

        <P, V> MutableSet<V> collectWith(
            @Independent(hc = true) @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified
        MutableSet<T> difference(@Independent(hc = true) @NotModified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified

        <V> MutableSet<V> flatCollect(
            @Independent(hc = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified

        <P, V> MutableSet<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent(absent = true) @Modified

        <V> MutableSetMultimap<V, T> groupBy(@Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent(absent = true) @Modified

        <V> MutableSetMultimap<V, T> groupByEach(
            @Independent(hc = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified
        MutableSet<T> intersect(@Independent(hc = true) @NotModified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Independent(absent = true) @Modified
        MutableSet<T> newEmpty() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified
        PartitionMutableSet<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified

        <P> PartitionMutableSet<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified
        MutableSet<UnsortedSetIterable<T>> powerSet() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent(absent = true) @Modified
        MutableSet<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent(absent = true) @Modified

        <P> MutableSet<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent(absent = true) @Modified
        MutableSet<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent(absent = true) @Modified
        <S> MutableSet<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent(absent = true) @Modified

        <P> MutableSet<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified

        MutableSet<T> symmetricDifference(@Independent(hc = true) @NotModified SetIterable<? extends T> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified
        MutableSet<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent @Modified
        ImmutableSet<T> toImmutable() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ImmutableSet<T> toImmutableSet() { return null; }

        //override from org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent(hc = true) @Modified
        MutableSet<T> union(@Independent(absent = true) @NotModified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable
        @Fluent @Independent @Modified
        MutableSet<T> with(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable
        @Fluent @Independent @Modified
        MutableSet<T> withAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable
        @Fluent @Independent @Modified
        MutableSet<T> without(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable
        @Fluent @Independent @Modified
        MutableSet<T> withoutAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent(hc = true) @Modified
        <S> MutableSet<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.MutableSetIterable, org.eclipse.collections.api.set.SetIterable, org.eclipse.collections.api.set.UnsortedSetIterable
        @Independent(hc = true) @Modified
        MutableSet<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface MutableSetIterable implements SetIterable<T>, MutableCollection<T>, Set<T>
    //annotated as EXPECTED; computed mutable @Dependent (2) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableSetIterable$<T> {
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection
        @Independent(absent = true) @Modified

        <V> MutableSetIterableMultimap<V, T> groupBy(
            @Independent(hc = true) @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection
        @Independent(absent = true) @Modified

        <V> MutableSetIterableMultimap<V, T> groupByEach(
            @Independent(hc = true) @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified
        PartitionMutableSetIterable<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified

        <P> PartitionMutableSetIterable<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified
        MutableSetIterable<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified

        <P> MutableSetIterable<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified
        MutableSetIterable<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified
        <S> MutableSetIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified

        <P> MutableSetIterable<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent @Modified
        MutableSetIterable<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from java.util.Collection, java.util.Set, org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection
        @Independent @NotModified
        Object [] toArray() { return null; }

        //override from java.util.Collection, java.util.Set, org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection
        @Independent @NotModified
        <T1> T1 [] toArray(@Independent @NotModified T1 [] a) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableSetIterable<T> with(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableSetIterable<T> withAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableSetIterable<T> without(@Independent @NotModified T element) { return null; }

        //override from org.eclipse.collections.api.collection.MutableCollection
        @Fluent @Independent @Modified
        MutableSetIterable<T> withoutAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection
        @Independent(hc = true) @Modified
        <S> MutableCollection<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.collection.MutableCollection, org.eclipse.collections.api.set.SetIterable
        @Independent(hc = true) @Modified
        MutableSetIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface ParallelSetIterable implements ParallelIterable<T>
    @FinalFields
    @Independent(hc = true)
    class ParallelSetIterable$<T> {
        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @NotModified
        ParallelSetIterable<T> asUnique() { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent(hc = true) @Modified
        <V> SetMultimap<V, T> groupBy(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent(hc = true) @Modified

        <V> SetMultimap<V, T> groupByEach(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        ParallelSetIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <P> ParallelSetIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        ParallelSetIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified
        <S> ParallelSetIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable
        @Independent @Modified

        <P> ParallelSetIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
    }

    //public interface ParallelUnsortedSetIterable implements ParallelSetIterable<T>
    @FinalFields
    @Independent(hc = true)
    class ParallelUnsortedSetIterable$<T> {
        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent @NotModified
        ParallelUnsortedSetIterable<T> asUnique() { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent(hc = true) @Modified

        <V> UnsortedSetMultimap<V, T> groupBy(@Independent @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent(hc = true) @Modified

        <V> UnsortedSetMultimap<V, T> groupByEach(
            @Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent @Modified
        ParallelUnsortedSetIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent @Modified

        <P> ParallelUnsortedSetIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent @Modified
        ParallelUnsortedSetIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent @Modified
        <S> ParallelUnsortedSetIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.ParallelIterable, org.eclipse.collections.api.set.ParallelSetIterable
        @Independent @Modified

        <P> ParallelUnsortedSetIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
    }

    //public interface Pool
    @FinalFields
    @Independent(hc = true)
    class Pool$<V> {
        @Modified void clear() { }
        @Independent(hc = true) @NotModified V get(@Independent @Modified V arg0) { return null; }
        @Independent(hc = true) @Modified V put(@Independent(hc = true) @Modified V arg0) { return null; }
        @Independent(hc = true) @Modified V removeFromPool(@Independent @Modified V arg0) { return null; }
        @NotModified int size() { return 0; }
    }

    //public interface SetIterable implements RichIterable<T>
    @Independent(absent = true)
    class SetIterable$<T> {
        @Independent @Modified
        ParallelSetIterable<T> asParallel(@Independent @Modified ExecutorService arg0, int arg1) { return null; }

        @Independent @Modified
        <B> LazyIterable<Pair<T, B>> cartesianProduct(@Independent @Modified SetIterable<B> arg0) { return null; }

        @Independent @Modified
        SetIterable<T> difference(@Independent(hc = true) @Modified SetIterable<? extends T> arg0) { return null; }

        @Independent @Modified
        <R extends Set<T>> R differenceInto(
            @Independent(hc = true) @Modified SetIterable<? extends T> arg0,
            @Independent @Modified R arg1) { return null; }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @NotModified Object arg0) { return false; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        @Independent @Modified
        SetIterable<T> intersect(@Independent(hc = true) @Modified SetIterable<? extends T> arg0) { return null; }

        @Independent @Modified
        <R extends Set<T>> R intersectInto(
            @Independent(hc = true) @Modified SetIterable<? extends T> arg0,
            @Independent @Modified R arg1) { return null; }

        @Modified
        boolean isProperSubsetOf(@Independent(hc = true) @Modified SetIterable<? extends T> arg0) { return false; }
        @Modified boolean isSubsetOf(@Independent(hc = true) @Modified SetIterable<? extends T> arg0) { return false; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified
        PartitionSet<T> partition(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified

        <P> PartitionSet<T> partitionWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified
        SetIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified

        <P> SetIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified
        SetIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified
        <S> SetIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified

        <P> SetIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        @Independent @Modified
        SetIterable<T> symmetricDifference(@Independent(hc = true) @Modified SetIterable<? extends T> arg0) {
            return null;
        }

        @Independent @Modified
        <R extends Set<T>> R symmetricDifferenceInto(
            @Independent(hc = true) @Modified SetIterable<? extends T> arg0,
            @Independent @Modified R arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        SetIterable<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }
        @Independent @Modified ImmutableSetIterable<T> toImmutable() { return null; }
        @Independent(hc = true) @Modified
        SetIterable<T> union(@Independent(absent = true) @Modified SetIterable<? extends T> arg0) { return null; }

        @Independent @Modified
        <R extends Set<T>> R unionInto(
            @Independent(hc = true) @Modified SetIterable<? extends T> arg0,
            @Independent @Modified R arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        SetIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface UnsortedSetIterable implements SetIterable<T>
    @Independent(absent = true)
    class UnsortedSetIterable$<T> {
        //override from org.eclipse.collections.api.set.SetIterable
        @Independent @Modified

        ParallelUnsortedSetIterable<T> asParallel(@Independent @NotModified ExecutorService arg0, int arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> UnsortedSetIterable<V> collect(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        BooleanSet collectBoolean(@Independent @Modified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ByteSet collectByte(@Independent @Modified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        CharSet collectChar(@Independent @Modified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        DoubleSet collectDouble(@Independent @Modified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        FloatSet collectFloat(@Independent @Modified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> UnsortedSetIterable<V> collectIf(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        IntSet collectInt(@Independent @Modified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        LongSet collectLong(@Independent @Modified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        ShortSet collectShort(@Independent @Modified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <P, V> UnsortedSetIterable<V> collectWith(
            @Independent(hc = true) @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @Modified P arg1) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable
        @Independent @Modified

        UnsortedSetIterable<T> difference(@Independent(hc = true) @Modified SetIterable<? extends T> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> UnsortedSetIterable<V> flatCollect(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <P, V> UnsortedSetIterable<V> flatCollectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @Modified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified

        <V> UnsortedSetMultimap<V, T> groupBy(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(absent = true) @Modified

        <V> UnsortedSetMultimap<V, T> groupByEach(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable
        @Independent @Modified
        UnsortedSetIterable<T> intersect(@Independent(hc = true) @Modified SetIterable<? extends T> arg0) { return null; }
        @Independent @Modified UnsortedSetIterable<UnsortedSetIterable<T>> powerSet() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified
        UnsortedSetIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified

        <P> UnsortedSetIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified
        UnsortedSetIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified
        <S> UnsortedSetIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(absent = true) @Modified

        <P> UnsortedSetIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable
        @Independent @Modified

        UnsortedSetIterable<T> symmetricDifference(@Independent(hc = true) @Modified SetIterable<? extends T> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.set.SetIterable
        @Independent @Modified
        UnsortedSetIterable<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.set.SetIterable
        @Independent @Modified
        ImmutableSet<T> toImmutable() { return null; }

        //override from org.eclipse.collections.api.set.SetIterable
        @Independent(hc = true) @Modified
        UnsortedSetIterable<T> union(@Independent(absent = true) @Modified SetIterable<? extends T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        <S> UnsortedSetIterable<Pair<T, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.set.SetIterable
        @Independent(hc = true) @Modified
        UnsortedSetIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }
}
