package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.stream.Stream;
import org.eclipse.collections.api.bag.ImmutableBag;
import org.eclipse.collections.api.bag.MultiReaderBag;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
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
public class OrgEclipseCollectionsApiFactoryBag {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory.bag";
    //public interface ImmutableBagFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class ImmutableBagFactory$ {
        @Independent @NotModified <T> ImmutableBag<T> empty() { return null; }
        @Independent @NotModified
        <T> ImmutableBag<T> fromStream(@Independent @NotModified Stream<? extends T> stream) { return null; }
        @Independent @NotModified <T> ImmutableBag<T> of() { return null; }
        @Independent @NotModified <T> ImmutableBag<T> of(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified <T> ImmutableBag<T> of(@Independent @Modified T ... arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableBag<T> ofAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> ImmutableBag<T> ofOccurrences(@Independent @Modified T element, int occurrence) { return null; }

        @Independent @NotModified
        <T> ImmutableBag<T> ofOccurrences(
            @Independent @Modified T element1,
            int occurrence1,
            @Independent @Modified T element2,
            int occurrence2) { return null; }

        @Independent @NotModified
        <T> ImmutableBag<T> ofOccurrences(
            @Independent @Modified T element1,
            int occurrence1,
            @Independent @Modified T element2,
            int occurrence2,
            @Independent @Modified T element3,
            int occurrence3) { return null; }

        @Independent @NotModified
        <T> ImmutableBag<T> ofOccurrences(
            @Independent @Modified T element1,
            int occurrence1,
            @Independent @Modified T element2,
            int occurrence2,
            @Independent @Modified T element3,
            int occurrence3,
            @Independent @Modified T element4,
            int occurrence4) { return null; }

        @Independent @NotModified
        <T> ImmutableBag<T> ofOccurrences(@Independent @Modified ObjectIntPair<T> ... elementsWithOccurrences) {
            return null;
        }
        @Independent @NotModified <T> ImmutableBag<T> with() { return null; }
        @Independent @NotModified <T> ImmutableBag<T> with(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified <T> ImmutableBag<T> with(@Independent @Modified T ... arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableBag<T> withAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> ImmutableBag<T> withOccurrences(@Independent @Modified T element, int occurrence) { return null; }

        @Independent @NotModified
        <T> ImmutableBag<T> withOccurrences(
            @Independent @Modified T element1,
            int occurrence1,
            @Independent @Modified T element2,
            int occurrence2) { return null; }

        @Independent @NotModified
        <T> ImmutableBag<T> withOccurrences(
            @Independent @Modified T element1,
            int occurrence1,
            @Independent @Modified T element2,
            int occurrence2,
            @Independent @Modified T element3,
            int occurrence3) { return null; }

        @Independent @NotModified
        <T> ImmutableBag<T> withOccurrences(
            @Independent @Modified T element1,
            int occurrence1,
            @Independent @Modified T element2,
            int occurrence2,
            @Independent @Modified T element3,
            int occurrence3,
            @Independent @Modified T element4,
            int occurrence4) { return null; }

        @Independent @NotModified
        <T> ImmutableBag<T> withOccurrences(@Independent @Modified ObjectIntPair<T> ... elementsWithOccurrences) {
            return null;
        }
    }

    //public interface MultiReaderBagFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MultiReaderBagFactory$ {
        @Independent @NotModified <T> MultiReaderBag<T> empty() { return null; }
        @Independent @NotModified
        <T> MultiReaderBag<T> fromStream(@Independent @NotModified Stream<? extends T> arg0) { return null; }
        @Independent @NotModified <T> MultiReaderBag<T> of() { return null; }
        @Independent @NotModified <T> MultiReaderBag<T> of(@Independent @Modified T ... elements) { return null; }
        @Independent @NotModified
        <T> MultiReaderBag<T> ofAll(@Independent @Modified Iterable<? extends T> items) { return null; }
        @Independent @NotModified <T> MultiReaderBag<T> with() { return null; }
        @Independent @NotModified <T> MultiReaderBag<T> with(@Independent @Modified T ... arg0) { return null; }
        @Independent @NotModified
        <T> MultiReaderBag<T> withAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }
    }

    //public interface MutableBagFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MutableBagFactory$ {
        @Independent @NotModified <T> MutableBag<T> empty() { return null; }
        @Independent @NotModified
        <T> MutableBag<T> fromStream(@Independent @NotModified Stream<? extends T> arg0) { return null; }
        @Independent @NotModified <T> MutableBag<T> of() { return null; }
        @Independent @NotModified <T> MutableBag<T> of(@Independent @Modified T ... elements) { return null; }
        @Independent @NotModified
        <T> MutableBag<T> ofAll(@Independent @Modified Iterable<? extends T> items) { return null; }
        @Independent @NotModified <T> MutableBag<T> ofInitialCapacity(int capacity) { return null; }
        @Independent @NotModified
        <T> MutableBag<T> ofOccurrences(@Independent @Modified T element, int occurrence) { return null; }

        @Independent @NotModified
        <T> MutableBag<T> ofOccurrences(
            @Independent @Modified T element1,
            int occurrence1,
            @Independent @Modified T element2,
            int occurrence2) { return null; }

        @Independent @NotModified
        <T> MutableBag<T> ofOccurrences(
            @Independent @Modified T element1,
            int occurrence1,
            @Independent @Modified T element2,
            int occurrence2,
            @Independent @Modified T element3,
            int occurrence3) { return null; }

        @Independent @NotModified
        <T> MutableBag<T> ofOccurrences(
            @Independent @Modified T element1,
            int occurrence1,
            @Independent @Modified T element2,
            int occurrence2,
            @Independent @Modified T element3,
            int occurrence3,
            @Independent @Modified T element4,
            int occurrence4) { return null; }

        @Independent @NotModified
        <T> MutableBag<T> ofOccurrences(@Independent @NotModified ObjectIntPair<T> ... elementsWithOccurrences) {
            return null;
        }
        @Independent @NotModified <T> MutableBag<T> with() { return null; }
        @Independent @NotModified <T> MutableBag<T> with(@Independent @Modified T ... arg0) { return null; }
        @Independent @NotModified
        <T> MutableBag<T> withAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified <T> MutableBag<T> withInitialCapacity(int capacity) { return null; }
        @Independent @NotModified
        <T> MutableBag<T> withOccurrences(@Independent @Modified T element, int occurrence) { return null; }

        @Independent @NotModified
        <T> MutableBag<T> withOccurrences(
            @Independent @Modified T element1,
            int occurrence1,
            @Independent @Modified T element2,
            int occurrence2) { return null; }

        @Independent @NotModified
        <T> MutableBag<T> withOccurrences(
            @Independent @Modified T element1,
            int occurrence1,
            @Independent @Modified T element2,
            int occurrence2,
            @Independent @Modified T element3,
            int occurrence3) { return null; }

        @Independent @NotModified
        <T> MutableBag<T> withOccurrences(
            @Independent @Modified T element1,
            int occurrence1,
            @Independent @Modified T element2,
            int occurrence2,
            @Independent @Modified T element3,
            int occurrence3,
            @Independent @Modified T element4,
            int occurrence4) { return null; }

        @Independent @NotModified
        <T> MutableBag<T> withOccurrences(@Independent @NotModified ObjectIntPair<T> ... elementsWithOccurrences) {
            return null;
        }
    }
}
