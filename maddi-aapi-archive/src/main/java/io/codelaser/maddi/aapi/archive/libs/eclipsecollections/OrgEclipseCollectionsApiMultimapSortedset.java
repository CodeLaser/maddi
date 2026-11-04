package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.multimap.bag.BagMultimap;
import org.eclipse.collections.api.multimap.bag.ImmutableBagMultimap;
import org.eclipse.collections.api.multimap.bag.MutableBagMultimap;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.multimap.set.ImmutableSetMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.multimap.sortedset.ImmutableSortedSetMultimap;
import org.eclipse.collections.api.multimap.sortedset.MutableSortedSetMultimap;
import org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap;
import org.eclipse.collections.api.set.sorted.ImmutableSortedSet;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.api.set.sorted.SortedSetIterable;
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
public class OrgEclipseCollectionsApiMultimapSortedset {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.multimap.sortedset";
    //public interface ImmutableSortedSetMultimap implements SortedSetMultimap<K,V>, ImmutableSetIterableMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableSortedSetMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent @NotModified

        <K2, V2> ImmutableBagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent @NotModified

        <K2, V2> ImmutableBagMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent @NotModified

        <V2> ImmutableListMultimap<K, V2> collectValues(
            @Independent @NotModified Function<? super V, ? extends V2> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent @NotModified
        ImmutableSetMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent(hc = true) @NotModified
        ImmutableSortedSet<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent(hc = true) @NotModified
        ImmutableSortedSetMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap
        @Independent @NotModified

        ImmutableSortedSetMultimap<K, V> newWith(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap
        @Independent @NotModified

        ImmutableSortedSetMultimap<K, V> newWithAll(
            @Independent @NotModified K arg0,
            @Independent @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap
        @Independent @NotModified

        ImmutableSortedSetMultimap<K, V> newWithout(
            @Independent @NotModified Object arg0,
            @Independent @NotModified Object arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap
        @Independent @NotModified
        ImmutableSortedSetMultimap<K, V> newWithoutAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent(hc = true) @NotModified

        ImmutableSortedSetMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent(hc = true) @NotModified

        ImmutableSortedSetMultimap<K, V> rejectKeysValues(
            @Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent(hc = true) @NotModified

        ImmutableSortedSetMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent(hc = true) @NotModified

        ImmutableSortedSetMultimap<K, V> selectKeysValues(
            @Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }
    }

    //public interface MutableSortedSetMultimap implements MutableSetIterableMultimap<K,V>, SortedSetMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableSortedSetMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap
        @Independent @NotModified
        MutableSortedSetMultimap<K, V> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent @Modified

        <K2, V2> MutableBagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent @Modified

        <K2, V2> MutableBagMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent @Modified

        <V2> MutableListMultimap<K, V2> collectValues(@Independent @NotModified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent @Modified
        MutableSetMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent(hc = true) @NotModified
        MutableSortedSet<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap
        @Independent(hc = true) @NotModified

        MutableSortedSet<V> getIfAbsentPutAll(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent(hc = true) @Modified @NotModified(after = "comparator,delegate")
        MutableSortedSetMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent(hc = true) @Modified

        MutableSortedSetMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent(hc = true) @Modified

        MutableSortedSetMultimap<K, V> rejectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap
        @Independent(hc = true) @Modified
        MutableSortedSet<V> removeAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap
        @Independent(hc = true) @NotModified

        MutableSortedSet<V> replaceValues(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent(hc = true) @Modified

        MutableSortedSetMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.sortedset.SortedSetMultimap
        @Independent(hc = true) @Modified

        MutableSortedSetMultimap<K, V> selectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap
        @Independent @NotModified

        MutableSortedSetMultimap<K, V> withKeyMultiValues(
            @Independent @NotModified K key,
            @Independent @NotModified V ... values) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap
        @Fluent @Independent @Modified

        MutableSortedSetMultimap<K, V> withKeyValue(@Independent @NotModified K key, @Independent @NotModified V value) {
            return null;
        }
    }

    //public interface SortedSetMultimap implements SetMultimap<K,V>, SortedIterableMultimap<K,V>, ReversibleIterableMultimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class SortedSetMultimap$<K, V> {
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

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @NotModified
        SortedSetIterable<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @Modified @NotModified(after = "comparator,delegate")
        SortedSetMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @Modified

        SortedSetMultimap<K, V> rejectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @Modified

        SortedSetMultimap<K, V> rejectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @Modified

        SortedSetMultimap<K, V> selectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap, org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @Modified

        SortedSetMultimap<K, V> selectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @Modified
        ImmutableSortedSetMultimap<K, V> toImmutable() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified
        MutableSortedSetMultimap<K, V> toMutable() { return null; }
    }
}
