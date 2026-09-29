package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import org.eclipse.collections.api.map.sorted.ImmutableSortedMap;
import org.eclipse.collections.api.map.sorted.MutableSortedMap;
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
public class OrgEclipseCollectionsApiFactoryMapSorted {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory.map.sorted";
    //public interface ImmutableSortedMapFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class ImmutableSortedMapFactory$ {
        @Independent @NotModified <K, V> ImmutableSortedMap<K, V> empty() { return null; }
        @Independent @NotModified <K, V> ImmutableSortedMap<K, V> of() { return null; }
        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> of(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5,
            @Independent @NotModified K arg6,
            @Independent @NotModified V arg7) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> of(@Independent @NotModified Comparator<? super K> arg0) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2,
            @Independent @NotModified K arg3,
            @Independent @NotModified V arg4) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2,
            @Independent @NotModified K arg3,
            @Independent @NotModified V arg4,
            @Independent @NotModified K arg5,
            @Independent @NotModified V arg6) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> of(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2,
            @Independent @NotModified K arg3,
            @Independent @NotModified V arg4,
            @Independent @NotModified K arg5,
            @Independent @NotModified V arg6,
            @Independent @NotModified K arg7,
            @Independent @NotModified V arg8) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> ofSortedMap(@Independent @NotModified SortedMap<K, V> arg0) { return null; }
        @Independent @NotModified <K, V> ImmutableSortedMap<K, V> with() { return null; }
        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> with(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5,
            @Independent @NotModified K arg6,
            @Independent @NotModified V arg7) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> with(@Independent @NotModified Comparator<? super K> arg0) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> with(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> with(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2,
            @Independent @NotModified K arg3,
            @Independent @NotModified V arg4) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> with(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2,
            @Independent @NotModified K arg3,
            @Independent @NotModified V arg4,
            @Independent @NotModified K arg5,
            @Independent @NotModified V arg6) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> with(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2,
            @Independent @NotModified K arg3,
            @Independent @NotModified V arg4,
            @Independent @NotModified K arg5,
            @Independent @NotModified V arg6,
            @Independent @NotModified K arg7,
            @Independent @NotModified V arg8) { return null; }

        @Independent @NotModified
        <K, V> ImmutableSortedMap<K, V> withSortedMap(@Independent @NotModified SortedMap<K, V> arg0) { return null; }
    }

    //public interface MutableSortedMapFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MutableSortedMapFactory$ {
        @Independent @NotModified <K, V> MutableSortedMap<K, V> empty() { return null; }
        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> empty(@Independent @NotModified Comparator<? super K> arg0) { return null; }
        @Independent @NotModified <K, V> MutableSortedMap<K, V> of() { return null; }
        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> of(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5,
            @Independent @NotModified K arg6,
            @Independent @NotModified V arg7) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> of(@Independent @NotModified Comparator<? super K> arg0) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> of(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> of(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2,
            @Independent @NotModified K arg3,
            @Independent @NotModified V arg4) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> of(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2,
            @Independent @NotModified K arg3,
            @Independent @NotModified V arg4,
            @Independent @NotModified K arg5,
            @Independent @NotModified V arg6) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> of(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2,
            @Independent @NotModified K arg3,
            @Independent @NotModified V arg4,
            @Independent @NotModified K arg5,
            @Independent @NotModified V arg6,
            @Independent @NotModified K arg7,
            @Independent @NotModified V arg8) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> ofSortedMap(@Independent @NotModified Map<? extends K, ? extends V> arg0) {
            return null;
        }
        @Independent @NotModified <K, V> MutableSortedMap<K, V> with() { return null; }
        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> with(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5,
            @Independent @NotModified K arg6,
            @Independent @NotModified V arg7) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> with(@Independent @NotModified Comparator<? super K> arg0) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> with(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> with(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2,
            @Independent @NotModified K arg3,
            @Independent @NotModified V arg4) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> with(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2,
            @Independent @NotModified K arg3,
            @Independent @NotModified V arg4,
            @Independent @NotModified K arg5,
            @Independent @NotModified V arg6) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> with(
            @Independent @NotModified Comparator<? super K> arg0,
            @Independent @NotModified K arg1,
            @Independent @NotModified V arg2,
            @Independent @NotModified K arg3,
            @Independent @NotModified V arg4,
            @Independent @NotModified K arg5,
            @Independent @NotModified V arg6,
            @Independent @NotModified K arg7,
            @Independent @NotModified V arg8) { return null; }

        @Independent @NotModified
        <K, V> MutableSortedMap<K, V> withSortedMap(@Independent @NotModified Map<? extends K, ? extends V> arg0) {
            return null;
        }
    }
}
