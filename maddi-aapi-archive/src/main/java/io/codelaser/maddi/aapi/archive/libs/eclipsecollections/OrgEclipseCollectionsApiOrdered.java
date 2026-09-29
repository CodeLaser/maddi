package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap;
import org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap;
import org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap;
import org.eclipse.collections.api.ordered.OrderedIterable;
import org.eclipse.collections.api.ordered.ReversibleIterable;
import org.eclipse.collections.api.ordered.SortedIterable;
import org.eclipse.collections.api.ordered.primitive.*;
import org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable;
import org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable;
import org.eclipse.collections.api.partition.ordered.PartitionSortedIterable;
import org.eclipse.collections.api.stack.MutableStack;
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
public class OrgEclipseCollectionsApiOrdered {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.ordered";
    //public interface OrderedIterable implements RichIterable<T>
    @Independent(hc = true)
    class OrderedIterable$<T> {
        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> OrderedIterable<V> collect(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        OrderedBooleanIterable collectBoolean(@Independent @Modified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        OrderedByteIterable collectByte(@Independent @Modified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        OrderedCharIterable collectChar(@Independent @Modified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        OrderedDoubleIterable collectDouble(@Independent @Modified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        OrderedFloatIterable collectFloat(@Independent @Modified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> OrderedIterable<V> collectIf(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        OrderedIntIterable collectInt(@Independent @Modified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        OrderedLongIterable collectLong(@Independent @Modified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        OrderedShortIterable collectShort(@Independent @Modified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <P, V> OrderedIterable<V> collectWith(
            @Independent(hc = true) @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @Modified P arg1) { return null; }

        @Independent @Modified
        <V> OrderedIterable<V> collectWithIndex(
            @Independent @Modified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }

        @Independent @Modified
        <V, R extends Collection<V>> R collectWithIndex(
            @Independent @Modified ObjectIntToObjectFunction<? super T, ? extends V> function,
            @Independent @Modified R target) { return null; }

        @Modified
        <S> boolean corresponds(
            @Independent @Modified OrderedIterable<S> arg0,
            @Independent @Modified Predicate2<? super T, ? super S> arg1) { return false; }
        @Modified int detectIndex(@Independent @Modified Predicate<? super T> arg0) { return 0; }
        @Independent(hc = true) @Modified OrderedIterable<T> distinct() { return null; }
        @Independent(hc = true) @Modified
        OrderedIterable<T> dropWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> OrderedIterable<V> flatCollect(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <P, V> OrderedIterable<V> flatCollectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @Modified P parameter) { return null; }
        @Modified void forEach(int arg0, int arg1, @Independent @Modified Procedure<? super T> arg2) { }
        @Modified
        void forEachWithIndex(int arg0, int arg1, @Independent @Modified ObjectIntProcedure<? super T> arg2) { }

        //override from org.eclipse.collections.api.InternalIterable
        @Modified
        void forEachWithIndex(@Independent @Modified ObjectIntProcedure<? super T> arg0) { }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @NotModified
        T getFirst() { return null; }
        @Independent @Modified Optional<T> getFirstOptional() { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @NotModified
        T getLast() { return null; }
        @Independent @Modified Optional<T> getLastOptional() { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V> OrderedIterableMultimap<V, T> groupBy(
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V> OrderedIterableMultimap<V, T> groupByEach(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }
        @NotModified int indexOf(@Independent @NotModified Object object) { return 0; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @NotModified
        T max() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @NotModified
        T min() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        PartitionOrderedIterable<T> partition(@Independent @Modified Predicate<? super T> arg0) { return null; }

        @Independent(hc = true) @Modified
        PartitionOrderedIterable<T> partitionWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> PartitionOrderedIterable<T> partitionWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        OrderedIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> OrderedIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        @Independent @Modified
        <R extends Collection<T>> R rejectWithIndex(
            @Independent @Modified ObjectIntPredicate<? super T> predicate,
            @Independent @Modified R target) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        OrderedIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        <S> OrderedIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <P> OrderedIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        @Independent @Modified
        <R extends Collection<T>> R selectWithIndex(
            @Independent @Modified ObjectIntPredicate<? super T> predicate,
            @Independent @Modified R target) { return null; }

        @Independent(hc = true) @Modified
        OrderedIterable<T> takeWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        OrderedIterable<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }
        @Independent @NotModified MutableStack<T> toStack() { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        <S> OrderedIterable<Pair<T, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <S, R extends Collection<Pair<T, S>>> R zip(
            @Independent @NotModified Iterable<S> arg0,
            @Independent @Modified R arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified
        OrderedIterable<Pair<T, Integer>> zipWithIndex() { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified
        <R extends Collection<Pair<T, Integer>>> R zipWithIndex(@Independent @Modified R arg0) { return null; }
    }

    //public interface ReversibleIterable implements OrderedIterable<T>
    @Independent(absent = true)
    class ReversibleIterable$<T> {
        @Independent @Modified LazyIterable<T> asReversed() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <V> ReversibleIterable<V> collect(@Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        ReversibleBooleanIterable collectBoolean(@Independent @Modified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        ReversibleByteIterable collectByte(@Independent @Modified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        ReversibleCharIterable collectChar(@Independent @Modified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        ReversibleDoubleIterable collectDouble(@Independent @Modified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        ReversibleFloatIterable collectFloat(@Independent @Modified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <V> ReversibleIterable<V> collectIf(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        ReversibleIntIterable collectInt(@Independent @Modified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        ReversibleLongIterable collectLong(@Independent @Modified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        ReversibleShortIterable collectShort(@Independent @Modified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <P, V> ReversibleIterable<V> collectWith(
            @Independent(hc = true) @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @Modified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <V> ReversibleIterable<V> collectWithIndex(
            @Independent @Modified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }
        @Modified int detectLastIndex(@Independent @Modified Predicate<? super T> arg0) { return 0; }
        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        ReversibleIterable<T> distinct() { return null; }
        @Independent(absent = true) @Modified ReversibleIterable<T> drop(int arg0) { return null; }
        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        ReversibleIterable<T> dropWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <V> ReversibleIterable<V> flatCollect(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <P, V> ReversibleIterable<V> flatCollectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @Modified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified

        <V> ReversibleIterableMultimap<V, T> groupBy(
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified

        <V> ReversibleIterableMultimap<V, T> groupByEach(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        PartitionReversibleIterable<T> partition(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        PartitionReversibleIterable<T> partitionWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified

        <P> PartitionReversibleIterable<T> partitionWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        ReversibleIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified

        <P> ReversibleIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @Modified void reverseForEach(@Independent @Modified Procedure<? super T> procedure) { }
        @Modified void reverseForEachWithIndex(@Independent @Modified ObjectIntProcedure<? super T> procedure) { }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        ReversibleIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        <S> ReversibleIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified

        <P> ReversibleIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @Independent(absent = true) @Modified ReversibleIterable<T> take(int arg0) { return null; }
        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        ReversibleIterable<T> takeWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        ReversibleIterable<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }
        @Independent @Modified ReversibleIterable<T> toReversed() { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        <S> ReversibleIterable<Pair<T, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        ReversibleIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface SortedIterable implements OrderedIterable<T>
    @Independent(hc = true)
    class SortedIterable$<T> {
        @Independent(hc = true) @NotModified Comparator<? super T> comparator() { return null; }
        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        SortedIterable<T> distinct() { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        SortedIterable<T> dropWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified

        <V> SortedIterableMultimap<V, T> groupBy(
            @Independent(hc = true) @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified

        <V> SortedIterableMultimap<V, T> groupByEach(
            @Independent(hc = true) @Modified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @NotModified
        T max() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @NotModified
        T min() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        PartitionSortedIterable<T> partition(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        PartitionSortedIterable<T> partitionWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        SortedIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified

        <P> SortedIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        SortedIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        <S> SortedIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified

        <P> SortedIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        SortedIterable<T> takeWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        SortedIterable<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        <S> ListIterable<Pair<T, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @Modified
        SortedIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }
}
