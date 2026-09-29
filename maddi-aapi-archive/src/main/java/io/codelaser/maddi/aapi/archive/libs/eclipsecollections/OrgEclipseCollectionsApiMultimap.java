package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Collection;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.bag.Bag;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.ImmutableMultimap;
import org.eclipse.collections.api.multimap.Multimap;
import org.eclipse.collections.api.multimap.MutableMultimap;
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
@Independent(absent = true)
public class OrgEclipseCollectionsApiMultimap {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.multimap";
    //public interface ImmutableMultimap implements Multimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- G1: the interface is shared with lazy views and implemented through mutable delegates (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class ImmutableMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @NotModified

        <K2, V2> ImmutableMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @NotModified

        <K2, V2> ImmutableMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @NotModified

        <V2> ImmutableMultimap<K, V2> collectValues(@Independent @NotModified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @NotModified
        ImmutableMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @NotModified
        ImmutableCollection<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @NotModified
        ImmutableMultimap<K, V> newEmpty() { return null; }

        @Independent @NotModified
        ImmutableMultimap<K, V> newWith(@Independent @NotModified K key, @Independent @NotModified V value) {
            return null;
        }

        @Independent @NotModified
        ImmutableMultimap<K, V> newWithAll(
            @Independent @NotModified K key,
            @Independent @NotModified Iterable<? extends V> values) { return null; }

        @Independent @NotModified
        ImmutableMultimap<K, V> newWithout(@Independent @NotModified Object key, @Independent @NotModified Object value) {
            return null;
        }

        @Independent @NotModified
        ImmutableMultimap<K, V> newWithoutAll(@Independent @NotModified Object key) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @NotModified

        ImmutableMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @NotModified

        ImmutableMultimap<K, V> rejectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @NotModified

        ImmutableMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @NotModified

        ImmutableMultimap<K, V> selectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }
    }

    //public interface Multimap
    @FinalFields
    @Independent(absent = true)
    class Multimap$<K, V> {
        @Independent @Modified
        <K2, V2> Multimap<K2, V2> collectKeyMultiValues(
            @Independent @Modified Function<? super K, ? extends K2> arg0,
            @Independent @Modified Function<? super V, ? extends V2> arg1) { return null; }

        @Independent @Modified
        <K2, V2, R extends MutableMultimap<K2, V2>> R collectKeyMultiValues(
            @Independent @Modified Function<? super K, ? extends K2> arg0,
            @Independent @Modified Function<? super V, ? extends V2> arg1,
            @Independent @Modified R arg2) { return null; }

        @Independent @Modified
        <K2, V2> Multimap<K2, V2> collectKeysValues(
            @Independent @Modified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        @Independent @Modified
        <K2, V2, R extends MutableMultimap<K2, V2>> R collectKeysValues(
            @Independent @Modified Function2<? super K, ? super V, Pair<K2, V2>> arg0,
            @Independent @Modified R arg1) { return null; }

        @Independent(absent = true) @Modified
        <V2> Multimap<K, V2> collectValues(@Independent @Modified Function<? super V, ? extends V2> arg0) { return null; }

        @Independent @Modified
        <V2, R extends MutableMultimap<K, V2>> R collectValues(
            @Independent @Modified Function<? super V, ? extends V2> arg0,
            @Independent @Modified R arg1) { return null; }
        @NotModified boolean containsKey(@Independent @Modified Object arg0) { return false; }
        @Modified
        boolean containsKeyAndValue(@Independent @Modified Object arg0, @Independent @Modified Object arg1) {
            return false;
        }
        @NotModified boolean containsValue(@Independent @Modified Object arg0) { return false; }
        //override from java.lang.Object
        @NotModified
        public boolean equals(@Independent @Modified Object arg0) { return false; }
        @Independent @Modified Multimap<V, K> flip() { return null; }
        @NotModified void forEachKey(@Independent @Modified Procedure<? super K> arg0) { }
        @Modified
        void forEachKeyMultiValues(@Independent @Modified Procedure2<? super K, ? super RichIterable<V>> arg0) { }
        @Modified void forEachKeyValue(@Independent @Modified Procedure2<? super K, ? super V> arg0) { }
        @Modified void forEachValue(@Independent @Modified Procedure<? super V> arg0) { }
        @Independent(absent = true) @NotModified
        RichIterable<V> get(@Independent(hc = true) @Modified K arg0) { return null; }

        //override from java.lang.Object
        @NotModified
        public int hashCode() { return 0; }
        @NotModified boolean isEmpty() { return false; }
        @Independent @Modified Bag<K> keyBag() { return null; }
        @Independent(absent = true) @Modified
        RichIterable<Pair<K, RichIterable<V>>> keyMultiValuePairsView() { return null; }
        @Independent @NotModified SetIterable<K> keySet() { return null; }
        @Independent @Modified RichIterable<Pair<K, V>> keyValuePairsView() { return null; }
        @Independent @NotModified RichIterable<K> keysView() { return null; }
        @Independent @Modified RichIterable<RichIterable<V>> multiValuesView() { return null; }
        @Independent(absent = true) @Modified @NotModified(after = "comparator,delegate,hashingStrategy")
        Multimap<K, V> newEmpty() { return null; }
        @NotModified boolean notEmpty() { return false; }
        @Independent(absent = true) @Modified
        Multimap<K, V> rejectKeysMultiValues(@Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) {
            return null;
        }

        @Independent @Modified
        <R extends MutableMultimap<K, V>> R rejectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0,
            @Independent @Modified R arg1) { return null; }

        @Independent(absent = true) @Modified
        Multimap<K, V> rejectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        @Independent @Modified
        <R extends MutableMultimap<K, V>> R rejectKeysValues(
            @Independent @Modified Predicate2<? super K, ? super V> arg0,
            @Independent @Modified R arg1) { return null; }

        @Independent(absent = true) @Modified
        Multimap<K, V> selectKeysMultiValues(@Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) {
            return null;
        }

        @Independent @Modified
        <R extends MutableMultimap<K, V>> R selectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0,
            @Independent @Modified R arg1) { return null; }

        @Independent(absent = true) @Modified
        Multimap<K, V> selectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) { return null; }

        @Independent @Modified
        <R extends MutableMultimap<K, V>> R selectKeysValues(
            @Independent @Modified Predicate2<? super K, ? super V> arg0,
            @Independent @Modified R arg1) { return null; }
        @NotModified int size() { return 0; }
        @NotModified int sizeDistinct() { return 0; }
        @Independent(hc = true) @Modified ImmutableMultimap<K, V> toImmutable() { return null; }
        @Independent(absent = true) @NotModified MutableMap<K, RichIterable<V>> toMap() { return null; }
        @Independent @NotModified
        <R extends Collection<V>> MutableMap<K, R> toMap(@Independent @NotModified Function0<R> arg0) { return null; }
        @Independent @Modified MutableMultimap<K, V> toMutable() { return null; }
        @Independent @NotModified RichIterable<V> valuesView() { return null; }
    }

    //public interface MutableMultimap implements Multimap<K,V>
    //annotated as EXPECTED; computed @FinalFields @Dependent (1) -- a mutable collection stores its arguments, never modifies them (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class MutableMultimap$<K, V> {
        @Modified boolean add(@Independent @NotModified Pair<? extends K, ? extends V> keyValuePair) { return false; }
        @Independent @NotModified MutableMultimap<K, V> asSynchronized() { return null; }
        @Modified void clear() { }
        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified

        <K2, V2> MutableMultimap<K2, V2> collectKeyMultiValues(
            @Independent @NotModified Function<? super K, ? extends K2> arg0,
            @Independent @NotModified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified

        <K2, V2> MutableMultimap<K2, V2> collectKeysValues(
            @Independent @NotModified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified

        <V2> MutableMultimap<K, V2> collectValues(@Independent @NotModified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified
        MutableMultimap<V, K> flip() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @NotModified
        MutableCollection<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        @Independent @Modified
        MutableCollection<V> getIfAbsentPutAll(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified @NotModified(after = "comparator,delegate,hashingStrategy")
        MutableMultimap<K, V> newEmpty() { return null; }

        @Modified
        boolean put(@Independent(hc = true) @NotModified K arg0, @Independent(hc = true) @NotModified V arg1) {
            return false;
        }

        @Modified
        boolean putAll(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return false; }

        @Modified
        <KK extends K, VV extends V> boolean putAll(@Independent @NotModified Multimap<KK, VV> arg0) { return false; }

        @Modified
        boolean putAllPairs(@Independent @NotModified Iterable<? extends Pair<? extends K, ? extends V>> pairs) {
            return false;
        }

        @Modified
        boolean putAllPairs(@Independent @NotModified Pair<? extends K, ? extends V> ... pairs) { return false; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified

        MutableMultimap<K, V> rejectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified

        MutableMultimap<K, V> rejectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        @Modified
        boolean remove(@Independent @NotModified Object arg0, @Independent @NotModified Object arg1) { return false; }
        @Independent @Modified RichIterable<V> removeAll(@Independent @NotModified Object arg0) { return null; }
        @Independent @Modified
        RichIterable<V> replaceValues(
            @Independent(hc = true) @NotModified K arg0,
            @Independent(hc = true) @NotModified Iterable<? extends V> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified

        MutableMultimap<K, V> selectKeysMultiValues(
            @Independent @NotModified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(absent = true) @Modified

        MutableMultimap<K, V> selectKeysValues(@Independent @NotModified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        @Fluent @Independent @Modified
        MutableMultimap<K, V> withKeyMultiValues(
            @Independent @NotModified K key,
            @Independent @NotModified V ... values) { return null; }

        @Fluent @Independent @Modified
        MutableMultimap<K, V> withKeyValue(@Independent @NotModified K key, @Independent @NotModified V value) {
            return null;
        }
    }
}
