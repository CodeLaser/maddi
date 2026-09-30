package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Comparator;
import java.util.SortedSet;
import org.eclipse.collections.api.set.sorted.ImmutableSortedSet;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
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
public class OrgEclipseCollectionsApiFactorySetSorted {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory.set.sorted";
    //public interface ImmutableSortedSetFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class ImmutableSortedSetFactory$ {
        @Independent @NotModified <T> ImmutableSortedSet<T> empty() { return null; }
        @Independent @NotModified
        <T> ImmutableSortedSet<T> empty(@Independent @NotModified Comparator<? super T> arg0) { return null; }
        @Independent @NotModified <T> ImmutableSortedSet<T> of() { return null; }
        @Independent @NotModified <T> ImmutableSortedSet<T> of(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableSortedSet<T> of(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent(hc = true) @NotModified
        <T> ImmutableSortedSet<T> of(
            @Independent(hc = true) @NotModified Comparator<? super T> arg0,
            @Independent @NotModified T ... arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSortedSet<T> ofAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }

        @Independent(hc = true) @NotModified
        <T> ImmutableSortedSet<T> ofAll(
            @Independent(hc = true) @NotModified Comparator<? super T> arg0,
            @Independent @Modified Iterable<? extends T> arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSortedSet<T> ofSortedSet(@Independent @NotModified SortedSet<T> arg0) { return null; }
        @Independent @NotModified <T> ImmutableSortedSet<T> with() { return null; }
        @Independent @NotModified <T> ImmutableSortedSet<T> with(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableSortedSet<T> with(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent(hc = true) @NotModified
        <T> ImmutableSortedSet<T> with(
            @Independent(hc = true) @NotModified Comparator<? super T> arg0,
            @Independent @NotModified T ... arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSortedSet<T> withAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }

        @Independent(hc = true) @NotModified
        <T> ImmutableSortedSet<T> withAll(
            @Independent(hc = true) @NotModified Comparator<? super T> arg0,
            @Independent @Modified Iterable<? extends T> arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSortedSet<T> withSortedSet(@Independent @NotModified SortedSet<T> arg0) { return null; }
    }

    //public interface MutableSortedSetFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MutableSortedSetFactory$ {
        @Independent @NotModified <T> MutableSortedSet<T> empty() { return null; }
        @Independent @NotModified
        <T> MutableSortedSet<T> empty(@Independent @NotModified Comparator<? super T> arg0) { return null; }
        @Independent @NotModified <T> MutableSortedSet<T> of() { return null; }
        @Independent @NotModified <T> MutableSortedSet<T> of(@Independent @Modified T ... arg0) { return null; }
        @Independent @NotModified
        <T> MutableSortedSet<T> of(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableSortedSet<T> of(
            @Independent @NotModified Comparator<? super T> arg0,
            @Independent @Modified T ... arg1) { return null; }

        @Independent @NotModified
        <T> MutableSortedSet<T> ofAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableSortedSet<T> ofAll(
            @Independent @NotModified Comparator<? super T> arg0,
            @Independent @NotModified Iterable<? extends T> arg1) { return null; }
        @Independent @NotModified <T> MutableSortedSet<T> with() { return null; }
        @Independent @NotModified <T> MutableSortedSet<T> with(@Independent @Modified T ... arg0) { return null; }
        @Independent @NotModified
        <T> MutableSortedSet<T> with(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableSortedSet<T> with(
            @Independent @NotModified Comparator<? super T> arg0,
            @Independent @Modified T ... arg1) { return null; }

        @Independent @NotModified
        <T> MutableSortedSet<T> withAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableSortedSet<T> withAll(
            @Independent @NotModified Comparator<? super T> arg0,
            @Independent @NotModified Iterable<? extends T> arg1) { return null; }
    }
}
