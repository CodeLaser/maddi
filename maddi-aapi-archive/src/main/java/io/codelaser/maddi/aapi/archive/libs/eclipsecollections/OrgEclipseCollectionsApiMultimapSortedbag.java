package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.sorted.ImmutableSortedBag;
import org.eclipse.collections.api.bag.sorted.MutableSortedBag;
import org.eclipse.collections.api.bag.sorted.SortedBag;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.multimap.bag.BagMultimap;
import org.eclipse.collections.api.multimap.bag.ImmutableBagMultimap;
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.multimap.sortedbag.ImmutableSortedBagMultimap;
import org.eclipse.collections.api.multimap.sortedbag.MutableSortedBagMultimap;
import org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap;
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
public class OrgEclipseCollectionsApiMultimapSortedbag {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.multimap.sortedbag";
    //public interface ImmutableSortedBagMultimap implements ImmutableBagIterableMultimap<K,V>, SortedBagMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableSortedBagMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent @NotModified

        <K2, V2> ImmutableBagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent @NotModified

        <K2, V2> ImmutableBagMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent @NotModified

        <V2> ImmutableListMultimap<K, V2> collectValues(
            @Independent @NotModified Function<? super V, ? extends V2> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap
        @Independent @NotModified
        ImmutableBagMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent(hc = true) @NotModified
        ImmutableSortedBag<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent(hc = true) @NotModified
        ImmutableSortedBagMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap
        @Independent @NotModified

        ImmutableSortedBagMultimap<K, V> newWith(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap
        @Independent @NotModified

        ImmutableSortedBagMultimap<K, V> newWithAll(
            @Independent @NotModified K arg0,
            @Independent @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap
        @Independent @NotModified

        ImmutableSortedBagMultimap<K, V> newWithout(
            @Independent @NotModified Object arg0,
            @Independent @NotModified Object arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap
        @Independent @NotModified
        ImmutableSortedBagMultimap<K, V> newWithoutAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent(hc = true) @NotModified

        ImmutableSortedBagMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent(hc = true) @NotModified

        ImmutableSortedBagMultimap<K, V> rejectKeysValues(
            @Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent(hc = true) @NotModified

        ImmutableSortedBagMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent(hc = true) @NotModified

        ImmutableSortedBagMultimap<K, V> selectKeysValues(
            @Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }
    }

    //public interface MutableSortedBagMultimap implements MutableBagIterableMultimap<K,V>, SortedBagMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableSortedBagMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Independent @NotModified
        MutableSortedBagMultimap<K, V> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent @Modified

        <K2, V2> MutableBagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent @Modified

        <K2, V2> MutableBagMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent @Modified

        <V2> MutableListMultimap<K, V2> collectValues(@Independent @NotModified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Independent @Modified
        MutableBagMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent(hc = true) @NotModified
        MutableSortedBag<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Independent(hc = true) @NotModified

        MutableSortedBag<V> getIfAbsentPutAll(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent(hc = true) @Modified @NotModified(after = "comparator,delegate")
        MutableSortedBagMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent(hc = true) @Modified

        MutableSortedBagMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent(hc = true) @Modified

        MutableSortedBagMultimap<K, V> rejectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Independent(hc = true) @Modified
        MutableSortedBag<V> removeAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Independent(hc = true) @NotModified

        MutableSortedBag<V> replaceValues(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent(hc = true) @Modified

        MutableSortedBagMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.sortedbag.SortedBagMultimap
        @Independent(hc = true) @Modified

        MutableSortedBagMultimap<K, V> selectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Independent @NotModified

        MutableSortedBagMultimap<K, V> withKeyMultiValues(
            @Independent @NotModified K key,
            @Independent @NotModified V ... values) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Fluent @Independent @Modified

        MutableSortedBagMultimap<K, V> withKeyValue(@Independent @NotModified K key, @Independent @NotModified V value) {
            return null;
        }
    }

    //public interface SortedBagMultimap implements BagMultimap<K,V>, SortedIterableMultimap<K,V>, ReversibleIterableMultimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class SortedBagMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent @Modified

        <K2, V2> BagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @Modified Function<? super K, ? extends K2> arg0,
            @Independent @Modified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent @Modified

        <K2, V2> BagMultimap<K2, V2> collectKeysValues(
            @Independent @Modified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap
        @Independent @Modified

        <V2> ListMultimap<K, V2> collectValues(@Independent @Modified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap
        @Independent(hc = true) @NotModified
        SortedBag<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap
        @Independent(hc = true) @Modified @NotModified(after = "comparator,delegate")
        SortedBagMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap
        @Independent(hc = true) @Modified

        SortedBagMultimap<K, V> rejectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap
        @Independent(hc = true) @Modified

        SortedBagMultimap<K, V> rejectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap
        @Independent(hc = true) @Modified

        SortedBagMultimap<K, V> selectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap
        @Independent(hc = true) @Modified

        SortedBagMultimap<K, V> selectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @Modified
        ImmutableSortedBagMultimap<K, V> toImmutable() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified
        MutableSortedBagMultimap<K, V> toMutable() { return null; }
    }
}
