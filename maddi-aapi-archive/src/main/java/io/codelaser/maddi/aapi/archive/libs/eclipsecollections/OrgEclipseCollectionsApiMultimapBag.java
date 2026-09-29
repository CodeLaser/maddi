package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.*;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.multimap.ImmutableMultimap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.multimap.bag.*;
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
public class OrgEclipseCollectionsApiMultimapBag {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.multimap.bag";
    //public interface BagMultimap implements Multimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class BagMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified
        BagMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @NotModified
        Bag<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified @NotModified(after = "comparator,delegate,hashingStrategy")
        BagMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified

        BagMultimap<K, V> rejectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified
        BagMultimap<K, V> rejectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified

        BagMultimap<K, V> selectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified
        BagMultimap<K, V> selectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }
    }

    //public interface ImmutableBagIterableMultimap implements ImmutableMultimap<K,V>, BagMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (2) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableBagIterableMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap
        @Independent @NotModified

        <K2, V2> ImmutableBagIterableMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap
        @Independent @NotModified

        <K2, V2> ImmutableBagIterableMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap
        @Independent @NotModified

        <V2> ImmutableMultimap<K, V2> collectValues(@Independent @NotModified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent @NotModified
        ImmutableBagIterableMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(hc = true) @NotModified
        ImmutableBagIterable<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(hc = true) @NotModified
        ImmutableBagIterableMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap
        @Independent @NotModified

        ImmutableBagIterableMultimap<K, V> newWith(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap
        @Independent @NotModified

        ImmutableBagIterableMultimap<K, V> newWithAll(
            @Independent @NotModified K arg0,
            @Independent @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap
        @Independent @NotModified

        ImmutableBagIterableMultimap<K, V> newWithout(
            @Independent @NotModified Object arg0,
            @Independent @NotModified Object arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap
        @Independent @NotModified
        ImmutableBagIterableMultimap<K, V> newWithoutAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(hc = true) @NotModified

        ImmutableBagIterableMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(hc = true) @NotModified

        ImmutableBagIterableMultimap<K, V> rejectKeysValues(
            @Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(hc = true) @NotModified

        ImmutableBagIterableMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(hc = true) @NotModified

        ImmutableBagIterableMultimap<K, V> selectKeysValues(
            @Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }
    }

    //public interface ImmutableBagMultimap implements UnsortedBagMultimap<K,V>, ImmutableBagIterableMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (2) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableBagMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent @NotModified

        <K2, V2> ImmutableBagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent @NotModified

        <K2, V2> ImmutableBagMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent @NotModified

        <V2> ImmutableBagMultimap<K, V2> collectValues(@Independent @NotModified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap
        @Independent @NotModified
        ImmutableBagMultimap<V, K> flip() { return null; }

        @NotModified
        void forEachKeyImmutableBag(@Independent @NotModified Procedure2<? super K, ? super ImmutableBag<V>> arg0) { }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent(hc = true) @NotModified
        ImmutableBag<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent @NotModified
        ImmutableBagMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap
        @Independent @NotModified

        ImmutableBagMultimap<K, V> newWith(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap
        @Independent @NotModified

        ImmutableBagMultimap<K, V> newWithAll(
            @Independent @NotModified K arg0,
            @Independent @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap
        @Independent @NotModified

        ImmutableBagMultimap<K, V> newWithout(
            @Independent @NotModified Object arg0,
            @Independent @NotModified Object arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap
        @Independent @NotModified
        ImmutableBagMultimap<K, V> newWithoutAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent @NotModified

        ImmutableBagMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent @NotModified

        ImmutableBagMultimap<K, V> rejectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent @NotModified

        ImmutableBagMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.ImmutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent @NotModified

        ImmutableBagMultimap<K, V> selectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }
    }

    //public interface MutableBagIterableMultimap implements MutableMultimap<K,V>, BagMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (2) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableBagIterableMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent @NotModified
        MutableBagIterableMultimap<K, V> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap
        @Independent @Modified

        <K2, V2> MutableBagIterableMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap
        @Independent @Modified

        <K2, V2> MutableBagIterableMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap
        @Independent(absent = true) @Modified

        <V2> MutableMultimap<K, V2> collectValues(@Independent @NotModified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent @Modified
        MutableBagIterableMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(hc = true) @NotModified
        MutableBagIterable<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent(hc = true) @NotModified

        MutableBagIterable<V> getIfAbsentPutAll(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(absent = true) @Modified @NotModified(after = "comparator,delegate,hashingStrategy")
        MutableBagIterableMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(absent = true) @Modified

        MutableBagIterableMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(absent = true) @Modified

        MutableBagIterableMultimap<K, V> rejectKeysValues(
            @Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent(hc = true) @Modified
        MutableBagIterable<V> removeAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent(hc = true) @NotModified

        MutableBagIterable<V> replaceValues(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(absent = true) @Modified

        MutableBagIterableMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(absent = true) @Modified

        MutableBagIterableMultimap<K, V> selectKeysValues(
            @Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent @NotModified

        MutableBagIterableMultimap<K, V> withKeyMultiValues(
            @Independent @NotModified K key,
            @Independent @NotModified V ... values) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Fluent @Independent @Modified

        MutableBagIterableMultimap<K, V> withKeyValue(
            @Independent @NotModified K key,
            @Independent @NotModified V value) { return null; }
    }

    //public interface MutableBagMultimap implements MutableBagIterableMultimap<K,V>, UnsortedBagMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (2) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableBagMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Independent @NotModified
        MutableBagMultimap<K, V> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent @Modified

        <K2, V2> MutableBagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent @Modified

        <K2, V2> MutableBagMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent(absent = true) @Modified

        <V2> MutableBagMultimap<K, V2> collectValues(@Independent @NotModified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Independent @Modified
        MutableBagMultimap<V, K> flip() { return null; }

        @Modified
        void forEachKeyMutableBag(@Independent @NotModified Procedure2<? super K, ? super MutableBag<V>> arg0) { }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent(hc = true) @NotModified
        MutableBag<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Independent(hc = true) @NotModified

        MutableBag<V> getIfAbsentPutAll(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent(absent = true) @Modified @NotModified(after = "delegate,hashingStrategy")
        MutableBagMultimap<K, V> newEmpty() { return null; }

        @Modified
        void putOccurrences(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified V arg1,
            int arg2) { }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent(absent = true) @Modified

        MutableBagMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent(absent = true) @Modified

        MutableBagMultimap<K, V> rejectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Independent(hc = true) @Modified
        MutableBag<V> removeAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Independent(hc = true) @NotModified

        MutableBag<V> replaceValues(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent(absent = true) @Modified

        MutableBagMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.BagMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap, org.eclipse.collections.api.multimap.bag.UnsortedBagMultimap
        @Independent(absent = true) @Modified

        MutableBagMultimap<K, V> selectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Independent @NotModified

        MutableBagMultimap<K, V> withKeyMultiValues(
            @Independent @NotModified K key,
            @Independent @NotModified V ... values) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.bag.MutableBagIterableMultimap
        @Fluent @Independent @Modified

        MutableBagMultimap<K, V> withKeyValue(@Independent @NotModified K key, @Independent @NotModified V value) {
            return null;
        }
    }

    //public interface UnsortedBagMultimap implements BagMultimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class UnsortedBagMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified

        <K2, V2> UnsortedBagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @Modified Function<? super K, ? extends K2> arg0,
            @Independent @Modified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified

        <K2, V2> UnsortedBagMultimap<K2, V2> collectKeysValues(
            @Independent @Modified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified

        <V2> UnsortedBagMultimap<K, V2> collectValues(@Independent @Modified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(hc = true) @NotModified
        UnsortedBag<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(absent = true) @Modified @NotModified(after = "delegate,hashingStrategy")
        UnsortedBagMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(absent = true) @Modified

        UnsortedBagMultimap<K, V> rejectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(absent = true) @Modified

        UnsortedBagMultimap<K, V> rejectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(absent = true) @Modified

        UnsortedBagMultimap<K, V> selectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.bag.BagMultimap
        @Independent(absent = true) @Modified

        UnsortedBagMultimap<K, V> selectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified
        ImmutableBagMultimap<K, V> toImmutable() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified
        MutableBagMultimap<K, V> toMutable() { return null; }
    }
}
