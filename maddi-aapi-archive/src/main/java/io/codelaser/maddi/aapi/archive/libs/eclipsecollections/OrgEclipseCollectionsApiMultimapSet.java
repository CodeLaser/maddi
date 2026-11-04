package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.multimap.ImmutableMultimap;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.api.multimap.bag.*;
import org.eclipse.collections.api.multimap.set.*;
import org.eclipse.collections.api.set.*;
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
public class OrgEclipseCollectionsApiMultimapSet {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.multimap.set";
    //public interface ImmutableSetIterableMultimap implements SetMultimap<K,V>, ImmutableMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (2) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableSetIterableMultimap$<K, V> {
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

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent @NotModified
        ImmutableSetIterableMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @NotModified
        ImmutableSetIterable<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @NotModified
        ImmutableSetIterableMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap
        @Independent @NotModified

        ImmutableSetIterableMultimap<K, V> newWith(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap
        @Independent @NotModified

        ImmutableSetIterableMultimap<K, V> newWithAll(
            @Independent @NotModified K arg0,
            @Independent @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap
        @Independent @NotModified

        ImmutableSetIterableMultimap<K, V> newWithout(
            @Independent @NotModified Object arg0,
            @Independent @NotModified Object arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap
        @Independent @NotModified
        ImmutableSetIterableMultimap<K, V> newWithoutAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @NotModified

        ImmutableSetIterableMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @NotModified

        ImmutableSetIterableMultimap<K, V> rejectKeysValues(
            @Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @NotModified

        ImmutableSetIterableMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @NotModified

        ImmutableSetIterableMultimap<K, V> selectKeysValues(
            @Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }
    }

    //public interface ImmutableSetMultimap implements UnsortedSetMultimap<K,V>, ImmutableSetIterableMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (2) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableSetMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent @NotModified

        <K2, V2> ImmutableBagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent @NotModified

        <K2, V2> ImmutableBagMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent @NotModified

        <V2> ImmutableBagMultimap<K, V2> collectValues(@Independent @NotModified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent @NotModified
        ImmutableSetMultimap<V, K> flip() { return null; }

        @NotModified
        void forEachKeyImmutableSet(@Independent @NotModified Procedure2<? super K, ? super ImmutableSet<V>> arg0) { }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent(hc = true) @NotModified
        ImmutableSet<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent @NotModified
        ImmutableSetMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap
        @Independent @NotModified

        ImmutableSetMultimap<K, V> newWith(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap
        @Independent @NotModified

        ImmutableSetMultimap<K, V> newWithAll(
            @Independent @NotModified K arg0,
            @Independent @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap
        @Independent @NotModified

        ImmutableSetMultimap<K, V> newWithout(
            @Independent @NotModified Object arg0,
            @Independent @NotModified Object arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap
        @Independent @NotModified
        ImmutableSetMultimap<K, V> newWithoutAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent @NotModified

        ImmutableSetMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent @NotModified

        ImmutableSetMultimap<K, V> rejectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent @NotModified

        ImmutableSetMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.ImmutableMultimap, org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.ImmutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent @NotModified

        ImmutableSetMultimap<K, V> selectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }
    }

    //public interface MutableSetIterableMultimap implements SetMultimap<K,V>, MutableMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (2) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableSetIterableMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent @NotModified
        MutableSetIterableMultimap<K, V> asSynchronized() { return null; }

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
        @Independent @Modified

        <V2> MutableMultimap<K, V2> collectValues(@Independent @NotModified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent @Modified
        MutableSetIterableMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @NotModified
        MutableSetIterable<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent(hc = true) @NotModified

        MutableSetIterable<V> getIfAbsentPutAll(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(absent = true) @Modified @NotModified(after = "comparator,delegate,hashingStrategy")
        MutableSetIterableMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(absent = true) @Modified

        MutableSetIterableMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(absent = true) @Modified

        MutableSetIterableMultimap<K, V> rejectKeysValues(
            @Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent(hc = true) @Modified
        MutableSetIterable<V> removeAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent(hc = true) @NotModified

        MutableSetIterable<V> replaceValues(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(absent = true) @Modified

        MutableSetIterableMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(absent = true) @Modified

        MutableSetIterableMultimap<K, V> selectKeysValues(
            @Independent @NotModified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Independent @NotModified

        MutableSetIterableMultimap<K, V> withKeyMultiValues(
            @Independent @NotModified K key,
            @Independent @NotModified V ... values) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap
        @Fluent @Independent @Modified

        MutableSetIterableMultimap<K, V> withKeyValue(
            @Independent @NotModified K key,
            @Independent @NotModified V value) { return null; }
    }

    //public interface MutableSetMultimap implements UnsortedSetMultimap<K,V>, MutableSetIterableMultimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (2) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableSetMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap
        @Independent @NotModified
        MutableSetMultimap<K, V> asSynchronized() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent @Modified

        <K2, V2> MutableBagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent @Modified

        <K2, V2> MutableBagMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent @Modified

        <V2> MutableBagMultimap<K, V2> collectValues(@Independent @NotModified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent @Modified
        MutableSetMultimap<V, K> flip() { return null; }

        @Modified
        void forEachKeyMutableSet(@Independent @NotModified Procedure2<? super K, ? super MutableSet<V>> arg0) { }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent(hc = true) @NotModified
        MutableSet<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap
        @Independent(hc = true) @NotModified

        MutableSet<V> getIfAbsentPutAll(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent(absent = true) @Modified @NotModified(after = "delegate,hashingStrategy")
        MutableSetMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent(absent = true) @Modified

        MutableSetMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent(absent = true) @Modified

        MutableSetMultimap<K, V> rejectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap
        @Independent(hc = true) @Modified
        MutableSet<V> removeAll(@Independent @NotModified Object arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap
        @Independent(hc = true) @NotModified

        MutableSet<V> replaceValues(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent(absent = true) @Modified

        MutableSetMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap, org.eclipse.collections.api.multimap.set.SetMultimap, org.eclipse.collections.api.multimap.set.UnsortedSetMultimap
        @Independent(absent = true) @Modified

        MutableSetMultimap<K, V> selectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap
        @Independent @NotModified

        MutableSetMultimap<K, V> withKeyMultiValues(
            @Independent @NotModified K key,
            @Independent @NotModified V ... values) { return null; }

        //override from org.eclipse.collections.api.multimap.MutableMultimap, org.eclipse.collections.api.multimap.set.MutableSetIterableMultimap
        @Fluent @Independent @Modified

        MutableSetMultimap<K, V> withKeyValue(@Independent @NotModified K key, @Independent @NotModified V value) {
            return null;
        }
    }

    //public interface SetMultimap implements Multimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class SetMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified
        SetMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @NotModified
        SetIterable<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified @NotModified(after = "comparator,delegate,hashingStrategy")
        SetMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified

        SetMultimap<K, V> rejectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified
        SetMultimap<K, V> rejectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified

        SetMultimap<K, V> selectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified
        SetMultimap<K, V> selectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }
    }

    //public interface UnsortedSetMultimap implements SetMultimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class UnsortedSetMultimap$<K, V> {
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
        @Independent @Modified

        <V2> UnsortedBagMultimap<K, V2> collectValues(@Independent @Modified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(hc = true) @NotModified
        UnsortedSetIterable<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(absent = true) @Modified @NotModified(after = "delegate,hashingStrategy")
        UnsortedSetMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(absent = true) @Modified

        UnsortedSetMultimap<K, V> rejectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(absent = true) @Modified

        UnsortedSetMultimap<K, V> rejectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(absent = true) @Modified

        UnsortedSetMultimap<K, V> selectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.set.SetMultimap
        @Independent(absent = true) @Modified

        UnsortedSetMultimap<K, V> selectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified
        ImmutableSetMultimap<K, V> toImmutable() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified
        MutableSetMultimap<K, V> toMutable() { return null; }
    }
}
