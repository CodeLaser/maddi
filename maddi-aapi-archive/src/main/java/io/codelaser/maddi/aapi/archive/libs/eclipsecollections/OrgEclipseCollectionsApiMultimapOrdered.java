package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Comparator;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.multimap.bag.BagMultimap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap;
import org.eclipse.collections.api.multimap.ordered.ReversibleIterableMultimap;
import org.eclipse.collections.api.multimap.ordered.SortedIterableMultimap;
import org.eclipse.collections.api.ordered.OrderedIterable;
import org.eclipse.collections.api.ordered.ReversibleIterable;
import org.eclipse.collections.api.ordered.SortedIterable;
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
public class OrgEclipseCollectionsApiMultimapOrdered {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.multimap.ordered";
    //public interface OrderedIterableMultimap implements Multimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class OrderedIterableMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified

        <K2, V2> BagMultimap<K2, V2> collectKeyMultiValues(
            @Independent @Modified Function<? super K, ? extends K2> arg0,
            @Independent @Modified Function<? super V, ? extends V2> arg1) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified

        <K2, V2> BagMultimap<K2, V2> collectKeysValues(
            @Independent @Modified Function2<? super K, ? super V, Pair<K2, V2>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent @Modified

        <V2> OrderedIterableMultimap<K, V2> collectValues(@Independent @Modified Function<? super V, ? extends V2> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @NotModified
        OrderedIterable<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @Modified @NotModified(after = "comparator,delegate")
        OrderedIterableMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @Modified

        OrderedIterableMultimap<K, V> rejectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @Modified

        OrderedIterableMultimap<K, V> rejectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @Modified

        OrderedIterableMultimap<K, V> selectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap
        @Independent(hc = true) @Modified

        OrderedIterableMultimap<K, V> selectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }
    }

    //public interface ReversibleIterableMultimap implements OrderedIterableMultimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class ReversibleIterableMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent @Modified

        <V2> ReversibleIterableMultimap<K, V2> collectValues(
            @Independent @Modified Function<? super V, ? extends V2> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent(hc = true) @NotModified
        ReversibleIterable<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent(hc = true) @Modified @NotModified(after = "comparator,delegate")
        ReversibleIterableMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent(hc = true) @Modified

        ReversibleIterableMultimap<K, V> rejectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent(hc = true) @Modified

        ReversibleIterableMultimap<K, V> rejectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent(hc = true) @Modified

        ReversibleIterableMultimap<K, V> selectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent(hc = true) @Modified

        ReversibleIterableMultimap<K, V> selectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }
    }

    //public interface SortedIterableMultimap implements OrderedIterableMultimap<K,V>
    @FinalFields
    @Independent(absent = true)
    class SortedIterableMultimap$<K, V> {
        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent @Modified

        <V2> ListMultimap<K, V2> collectValues(@Independent @Modified Function<? super V, ? extends V2> arg0) {
            return null;
        }
        @Independent(hc = true) @NotModified Comparator<? super V> comparator() { return null; }
        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent(hc = true) @NotModified
        SortedIterable<V> get(@Independent(hc = true) @NotModified K arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent(hc = true) @Modified @NotModified(after = "comparator,delegate")
        SortedIterableMultimap<K, V> newEmpty() { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent(hc = true) @Modified

        SortedIterableMultimap<K, V> rejectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent(hc = true) @Modified

        SortedIterableMultimap<K, V> rejectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent(hc = true) @Modified

        SortedIterableMultimap<K, V> selectKeysMultiValues(
            @Independent @Modified Predicate2<? super K, ? super RichIterable<V>> arg0) { return null; }

        //override from org.eclipse.collections.api.multimap.Multimap, org.eclipse.collections.api.multimap.ordered.OrderedIterableMultimap
        @Independent(hc = true) @Modified

        SortedIterableMultimap<K, V> selectKeysValues(@Independent @Modified Predicate2<? super K, ? super V> arg0) {
            return null;
        }
    }
}
