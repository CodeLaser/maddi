package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Collection;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.primitive.*;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.*;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.partition.stack.PartitionImmutableStack;
import org.eclipse.collections.api.partition.stack.PartitionMutableStack;
import org.eclipse.collections.api.partition.stack.PartitionStack;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.StackIterable;
import org.eclipse.collections.api.stack.primitive.*;
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
public class OrgEclipseCollectionsApiStack {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.stack";
    //public interface ImmutableStack implements StackIterable<T>
    //annotated as EXPECTED; computed mutable @Dependent (1) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableStack$<T> {
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

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        <V> ImmutableStack<V> collect(@Independent @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableBooleanStack collectBoolean(@Independent @NotModified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableByteStack collectByte(@Independent @NotModified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableCharStack collectChar(@Independent @NotModified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableDoubleStack collectDouble(@Independent @NotModified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableFloatStack collectFloat(@Independent @NotModified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified

        <V> ImmutableStack<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableIntStack collectInt(@Independent @NotModified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableLongStack collectLong(@Independent @NotModified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableShortStack collectShort(@Independent @NotModified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified

        <P, V> ImmutableStack<V> collectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified

        <V> ImmutableStack<V> collectWithIndex(
            @Independent @NotModified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }

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

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableStack<T> distinct() { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableStack<T> dropWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified

        <V> ImmutableStack<V> flatCollect(@Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified

        <P, V> ImmutableStack<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified

        <V> ImmutableListMultimap<V, T> groupBy(@Independent @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified

        <V> ImmutableListMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <V> ImmutableMap<V, T> groupByUniqueKey(@Independent @NotModified Function<? super T, ? extends V> function) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        PartitionImmutableStack<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        PartitionImmutableStack<T> partitionWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified

        <P> PartitionImmutableStack<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @Independent @NotModified Pair<T, ImmutableStack<T>> peekAndPop() { return null; }
        @Independent @NotModified Pair<ListIterable<T>, ImmutableStack<T>> peekAndPop(int count) { return null; }
        @Independent(hc = true) @NotModified ImmutableStack<T> pop() { return null; }
        @Independent(hc = true) @NotModified ImmutableStack<T> pop(int arg0) { return null; }
        @Independent @NotModified ImmutableStack<T> push(@Independent @NotModified T arg0) { return null; }
        //override from org.eclipse.collections.api.RichIterable
        @Independent @NotModified

        <K> ImmutableMap<K, T> reduceBy(
            @Independent @NotModified Function<? super T, ? extends K> groupBy,
            @Independent @NotModified Function2<? super T, ? super T, ? extends T> reduceFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableStack<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified

        <P> ImmutableStack<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableStack<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        <S> ImmutableStack<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified

        <P> ImmutableStack<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @NotModified
        int size() { return 0; }

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

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableStack<T> takeWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableStack<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        <S> ImmutableStack<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        ImmutableStack<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface MutableStack implements StackIterable<T>
    //annotated as EXPECTED; computed mutable @Independent(hc = true) (conditional) (1) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(hc = true)
    class MutableStack$<T> {
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
        @Independent @NotModified MutableStack<T> asSynchronized() { return null; }
        @Independent @Modified MutableStack<T> asUnmodifiable() { return null; }
        @Modified void clear() { }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        <V> MutableStack<V> collect(@Independent @NotModified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableBooleanStack collectBoolean(@Independent @NotModified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableByteStack collectByte(@Independent @NotModified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableCharStack collectChar(@Independent @NotModified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableDoubleStack collectDouble(@Independent @NotModified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableFloatStack collectFloat(@Independent @NotModified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified

        <V> MutableStack<V> collectIf(
            @Independent @NotModified Predicate<? super T> arg0,
            @Independent @NotModified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableIntStack collectInt(@Independent @NotModified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableLongStack collectLong(@Independent @NotModified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableShortStack collectShort(@Independent @NotModified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified

        <P, V> MutableStack<V> collectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified

        <V> MutableStack<V> collectWithIndex(
            @Independent @NotModified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }

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

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableStack<T> distinct() { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        MutableStack<T> dropWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified

        <V> MutableStack<V> flatCollect(@Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified

        <P, V> MutableStack<V> flatCollectWith(
            @Independent @NotModified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @NotModified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified

        <V> MutableListMultimap<V, T> groupBy(@Independent @NotModified Function<? super T, ? extends V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified

        <V> MutableListMultimap<V, T> groupByEach(
            @Independent @NotModified Function<? super T, ? extends Iterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> MutableMap<V, T> groupByUniqueKey(@Independent @NotModified Function<? super T, ? extends V> function) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        PartitionMutableStack<T> partition(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        PartitionMutableStack<T> partitionWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified

        <P> PartitionMutableStack<T> partitionWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @Independent(hc = true) @Modified T pop() { return null; }
        @Independent(hc = true) @Modified ListIterable<T> pop(int arg0) { return null; }
        @Independent(hc = true) @Modified
        <R extends Collection<T>> R pop(int arg0, @Independent(hc = true) @NotModified R arg1) { return null; }

        @Independent(hc = true) @Modified
        <R extends MutableStack<T>> R pop(int arg0, @Independent(hc = true) @NotModified R arg1) { return null; }
        @Modified void push(@Independent(hc = true) @NotModified T arg0) { }
        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <K> MutableMap<K, T> reduceBy(
            @Independent @NotModified Function<? super T, ? extends K> groupBy,
            @Independent @NotModified Function2<? super T, ? super T, ? extends T> reduceFunction) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableStack<T> reject(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified

        <P> MutableStack<T> rejectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableStack<T> select(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        <S> MutableStack<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified

        <P> MutableStack<T> selectWith(
            @Independent @NotModified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V> MutableObjectDoubleMap<V> sumByDouble(
            @Independent @NotModified Function<? super T, ? extends V> arg0,
            @Independent @NotModified DoubleFunction<? super T> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent(hc = true) @Modified

        <V> MutableObjectDoubleMap<V> sumByFloat(
            @Independent @NotModified Function<? super T, ? extends V> arg0,
            @Independent @NotModified FloatFunction<? super T> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> MutableObjectLongMap<V> sumByInt(
            @Independent @NotModified Function<? super T, ? extends V> arg0,
            @Independent @NotModified IntFunction<? super T> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable
        @Independent @Modified

        <V> MutableObjectLongMap<V> sumByLong(
            @Independent @NotModified Function<? super T, ? extends V> arg0,
            @Independent @NotModified LongFunction<? super T> arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @NotModified
        MutableStack<T> takeWhile(@Independent @NotModified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableStack<T> tap(@Independent @NotModified Procedure<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        <S> MutableStack<Pair<T, S>> zip(@Independent @NotModified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable, org.eclipse.collections.api.stack.StackIterable
        @Independent @Modified
        MutableStack<Pair<T, Integer>> zipWithIndex() { return null; }
    }

    //public interface StackIterable implements OrderedIterable<T>
    @Independent(hc = true)
    class StackIterable$<T> {
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        <V> StackIterable<V> collect(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        BooleanStack collectBoolean(@Independent @Modified BooleanFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        ByteStack collectByte(@Independent @Modified ByteFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        CharStack collectChar(@Independent @Modified CharFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        DoubleStack collectDouble(@Independent @Modified DoubleFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        FloatStack collectFloat(@Independent @Modified FloatFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <V> StackIterable<V> collectIf(
            @Independent @Modified Predicate<? super T> arg0,
            @Independent @Modified Function<? super T, ? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        IntStack collectInt(@Independent @Modified IntFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        LongStack collectLong(@Independent @Modified LongFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        ShortStack collectShort(@Independent @Modified ShortFunction<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <P, V> StackIterable<V> collectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends V> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <V> StackIterable<V> collectWithIndex(
            @Independent @Modified ObjectIntToObjectFunction<? super T, ? extends V> function) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        StackIterable<T> distinct() { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @NotModified
        StackIterable<T> dropWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @Modified Object arg0) { return false; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <V> StackIterable<V> flatCollect(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <P, V> StackIterable<V> flatCollectWith(
            @Independent @Modified Function2<? super T, ? super P, ? extends Iterable<V>> function,
            @Independent @Modified P parameter) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @NotModified
        T getFirst() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent(hc = true) @NotModified
        T getLast() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        <V> ListMultimap<V, T> groupBy(@Independent @Modified Function<? super T, ? extends V> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <V> ListMultimap<V, T> groupByEach(@Independent @Modified Function<? super T, ? extends Iterable<V>> arg0) {
            return null;
        }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        PartitionStack<T> partition(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @NotModified
        PartitionStack<T> partitionWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <P> PartitionStack<T> partitionWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }
        @Independent(hc = true) @NotModified T peek() { return null; }
        @Independent(hc = true) @NotModified ListIterable<T> peek(int arg0) { return null; }
        @Independent(hc = true) @Modified T peekAt(int arg0) { return null; }
        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        StackIterable<T> reject(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <P> StackIterable<T> rejectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        StackIterable<T> select(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        <S> StackIterable<S> selectInstancesOf(Class<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified

        <P> StackIterable<T> selectWith(
            @Independent @Modified Predicate2<? super T, ? super P> arg0,
            @Independent @NotModified P arg1) { return null; }

        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @NotModified
        StackIterable<T> takeWhile(@Independent @Modified Predicate<? super T> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        StackIterable<T> tap(@Independent @Modified Procedure<? super T> arg0) { return null; }
        @Independent @NotModified ImmutableStack<T> toImmutable() { return null; }
        //override from org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @NotModified
        MutableStack<T> toStack() { return null; }

        //override from java.lang.Object, org.eclipse.collections.api.RichIterable
        @NotModified @NotNull
        public String toString() { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        <S> StackIterable<Pair<T, S>> zip(@Independent @Modified Iterable<S> arg0) { return null; }

        //override from org.eclipse.collections.api.RichIterable, org.eclipse.collections.api.ordered.OrderedIterable
        @Independent @Modified
        StackIterable<Pair<T, Integer>> zipWithIndex() { return null; }
    }
}
