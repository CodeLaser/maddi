package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.bag.*;
import org.eclipse.collections.api.multimap.list.ImmutableListMultimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
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
public class OrgEclipseCollectionsApiMultimapList {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.multimap.list";
    //public interface ImmutableListMultimap implements ListMultimap<K,V>, ImmutableMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableListMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent @NotModified

        <K2, V2> ImmutableBagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent @NotModified

        <K2, V2> ImmutableBagMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @NotModified

        <V2> ImmutableListMultimap<K, V2> collectValues(
            @Independent @NotModified Function<? super V, ? extends V2> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.list.ListMultimap
        @Independent @NotModified
        ImmutableBagMultimap<V, K> flip() { return null; }

        @NotModified
        void forEachKeyImmutableList(@Independent @NotModified Procedure2<? super K, ? super ImmutableList<V>> arg0) { }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent(hc = true) @NotModified
        ImmutableList<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @NotModified
        ImmutableListMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap
        @Independent @NotModified

        ImmutableListMultimap<K, V> newWith(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap
        @Independent @NotModified

        ImmutableListMultimap<K, V> newWithAll(
            @Independent @NotModified K arg0,
            @Independent @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap
        @Independent @NotModified

        ImmutableListMultimap<K, V> newWithout(
            @Independent @NotModified Object arg0,
            @Independent @NotModified Object arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap
        @Independent @NotModified
        ImmutableListMultimap<K, V> newWithoutAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @NotModified

        ImmutableListMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @NotModified

        ImmutableListMultimap<K, V> rejectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @NotModified

        ImmutableListMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @NotModified

        ImmutableListMultimap<K, V> selectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }
    }

    //public interface ListMultimap implements ReversibleIterableMultimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class ListMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent @Modified

        <K2, V2> BagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @Modified Function<? super K, ? extends K2> arg0,
            @Independent @Modified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent @Modified

        <K2, V2> BagMultimap<K2, V2> collectKeysValues(
            @Independent @Modified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @Modified

        <V2> ListMultimap<K, V2> collectValues(@Independent @Modified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified
        UnsortedBagMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent(hc = true) @NotModified
        ListIterable<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @NotModified
        ListMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @Modified

        ListMultimap<K, V> rejectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @Modified
        ListMultimap<K, V> rejectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @Modified

        ListMultimap<K, V> selectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @Modified
        ListMultimap<K, V> selectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified
        ImmutableListMultimap<K, V> toImmutable() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified
        MutableListMultimap<K, V> toMutable() { return null; }
    }

    //public interface MutableListMultimap implements ListMultimap<K,V>, MutableMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableListMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent @NotModified
        MutableListMultimap<K, V> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent @Modified

        <K2, V2> MutableBagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent @Modified

        <K2, V2> MutableBagMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @Modified

        <V2> MutableListMultimap<K, V2> collectValues(@Independent @NotModified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.list.ListMultimap
        @Independent @Modified
        MutableBagMultimap<V, K> flip() { return null; }

        @Modified
        void forEachKeyMutableList(@Independent @NotModified Procedure2<? super K, ? super MutableList<V>> arg0) { }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent(hc = true) @NotModified
        MutableList<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent(hc = true) @NotModified

        MutableList<V> getIfAbsentPutAll(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @NotModified
        MutableListMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @Modified

        MutableListMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @Modified

        MutableListMultimap<K, V> rejectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent(hc = true) @Modified
        MutableList<V> removeAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent(hc = true) @NotModified

        MutableList<V> replaceValues(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @Modified

        MutableListMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.list.ListMultimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap, org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap
        @Independent @Modified

        MutableListMultimap<K, V> selectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent @NotModified

        MutableListMultimap<K, V> withKeyMultiValues(
            @Independent @NotModified K key,
            @Independent @NotModified V ... values) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Fluent @Independent @Modified

        MutableListMultimap<K, V> withKeyValue(@Independent @NotModified K key, @Independent @NotModified V value) {
            return null;
        }
    }
}
