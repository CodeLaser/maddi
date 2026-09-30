package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Comparator;
import org.eclipse.collections.api.bag.sorted.ImmutableSortedBag;
import org.eclipse.collections.api.bag.sorted.MutableSortedBag;
import org.eclipse.collections.api.bag.sorted.SortedBag;
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
public class OrgEclipseCollectionsApiFactoryBagSorted {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory.bag.sorted";
    //public interface ImmutableSortedBagFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class ImmutableSortedBagFactory$ {
        @Independent @NotModified <T> ImmutableSortedBag<T> empty() { return null; }
        @Independent @NotModified
        <T> ImmutableSortedBag<T> empty(@Independent @Modified Comparator<? super T> arg0) { return null; }
        @Independent @NotModified <T> ImmutableSortedBag<T> of() { return null; }
        @Independent @NotModified <T> ImmutableSortedBag<T> of(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableSortedBag<T> of(@Independent @Modified Comparator<? super T> arg0) { return null; }

        @Independent(hc = true) @NotModified
        <T> ImmutableSortedBag<T> of(
            @Independent(hc = true) @Modified Comparator<? super T> arg0,
            @Independent @NotModified T ... arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSortedBag<T> ofAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }

        @Independent(hc = true) @NotModified
        <T> ImmutableSortedBag<T> ofAll(
            @Independent(hc = true) @Modified Comparator<? super T> arg0,
            @Independent @Modified Iterable<? extends T> arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSortedBag<T> ofSortedBag(@Independent @Modified SortedBag<T> arg0) { return null; }
        @Independent @NotModified <T> ImmutableSortedBag<T> with() { return null; }
        @Independent @NotModified <T> ImmutableSortedBag<T> with(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableSortedBag<T> with(@Independent @Modified Comparator<? super T> arg0) { return null; }

        @Independent @NotModified
        <T> ImmutableSortedBag<T> with(
            @Independent(hc = true) @Modified Comparator<? super T> arg0,
            @Independent @NotModified T ... arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSortedBag<T> withAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> ImmutableSortedBag<T> withAll(
            @Independent(hc = true) @Modified Comparator<? super T> arg0,
            @Independent @Modified Iterable<? extends T> arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSortedBag<T> withSortedBag(@Independent @Modified SortedBag<T> arg0) { return null; }
    }

    //public interface MutableSortedBagFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MutableSortedBagFactory$ {
        @Independent @NotModified <T> MutableSortedBag<T> empty() { return null; }
        @Independent @NotModified
        <T> MutableSortedBag<T> empty(@Independent @NotModified Comparator<? super T> arg0) { return null; }
        @Independent @NotModified <T> MutableSortedBag<T> of() { return null; }
        @Independent @NotModified <T> MutableSortedBag<T> of(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> MutableSortedBag<T> of(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableSortedBag<T> of(
            @Independent @NotModified Comparator<? super T> arg0,
            @Independent @NotModified T ... arg1) { return null; }

        @Independent @NotModified
        <T> MutableSortedBag<T> ofAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableSortedBag<T> ofAll(
            @Independent @NotModified Comparator<? super T> arg0,
            @Independent @Modified Iterable<? extends T> arg1) { return null; }
        @Independent @NotModified <T> MutableSortedBag<T> with() { return null; }
        @Independent @NotModified <T> MutableSortedBag<T> with(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> MutableSortedBag<T> with(@Independent @NotModified Comparator<? super T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableSortedBag<T> with(
            @Independent @NotModified Comparator<? super T> arg0,
            @Independent @NotModified T ... arg1) { return null; }

        @Independent @NotModified
        <T> MutableSortedBag<T> withAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableSortedBag<T> withAll(
            @Independent @NotModified Comparator<? super T> arg0,
            @Independent @Modified Iterable<? extends T> arg1) { return null; }
    }
}
