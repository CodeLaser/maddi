package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.stream.Stream;
import org.eclipse.collections.api.set.*;
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
public class OrgEclipseCollectionsApiFactorySet {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory.set";
    //public interface FixedSizeSetFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class FixedSizeSetFactory$ {
        @Independent @NotModified <T> FixedSizeSet<T> empty() { return null; }
        @Independent @NotModified
        <T> MutableSet<T> fromStream(@Independent @NotModified Stream<? extends T> arg0) { return null; }
        @Independent @NotModified <T> FixedSizeSet<T> of() { return null; }
        @Independent @NotModified <T> FixedSizeSet<T> of(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified
        <T> FixedSizeSet<T> of(@Independent @NotModified T arg0, @Independent @NotModified T arg1) { return null; }

        @Independent @NotModified
        <T> FixedSizeSet<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2) { return null; }

        @Independent @NotModified
        <T> FixedSizeSet<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3) { return null; }

        @Independent @NotModified
        <T> MutableSet<T> ofAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified <T> FixedSizeSet<T> with() { return null; }
        @Independent @NotModified <T> FixedSizeSet<T> with(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified
        <T> FixedSizeSet<T> with(@Independent @NotModified T arg0, @Independent @NotModified T arg1) { return null; }

        @Independent @NotModified
        <T> FixedSizeSet<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2) { return null; }

        @Independent @NotModified
        <T> FixedSizeSet<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3) { return null; }

        @Independent @NotModified
        <T> MutableSet<T> withAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }
    }

    //public interface ImmutableSetFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class ImmutableSetFactory$ {
        @Independent @NotModified <T> ImmutableSet<T> empty() { return null; }
        @Independent @NotModified
        <T> ImmutableSet<T> fromStream(@Independent @NotModified Stream<? extends T> stream) { return null; }
        @Independent @NotModified <T> ImmutableSet<T> of() { return null; }
        @Independent @NotModified <T> ImmutableSet<T> of(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableSet<T> of(@Independent @NotModified T arg0, @Independent @NotModified T arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSet<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2) { return null; }

        @Independent @NotModified
        <T> ImmutableSet<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3) { return null; }
        @Independent @NotModified <T> ImmutableSet<T> of(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableSet<T> ofAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified <T> ImmutableSet<T> with() { return null; }
        @Independent @NotModified <T> ImmutableSet<T> with(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableSet<T> with(@Independent @NotModified T arg0, @Independent @NotModified T arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSet<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2) { return null; }

        @Independent @NotModified
        <T> ImmutableSet<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3) { return null; }
        @Independent @NotModified <T> ImmutableSet<T> with(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableSet<T> withAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }
    }

    //public interface MultiReaderSetFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MultiReaderSetFactory$ {
        @Independent @NotModified <T> MultiReaderSet<T> empty() { return null; }
        @Independent @NotModified
        <T> MultiReaderSet<T> fromStream(@Independent @NotModified Stream<? extends T> arg0) { return null; }
        @Independent @NotModified <T> MultiReaderSet<T> of() { return null; }
        @Independent @NotModified <T> MultiReaderSet<T> of(@Independent @NotModified T ... items) { return null; }
        @Independent @NotModified
        <T> MultiReaderSet<T> ofAll(@Independent @NotModified Iterable<? extends T> items) { return null; }
        @Independent @NotModified <T> MultiReaderSet<T> ofInitialCapacity(int capacity) { return null; }
        @Independent @NotModified <T> MultiReaderSet<T> with() { return null; }
        @Independent @NotModified <T> MultiReaderSet<T> with(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> MultiReaderSet<T> withAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified <T> MultiReaderSet<T> withInitialCapacity(int arg0) { return null; }
    }

    //public interface MutableSetFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MutableSetFactory$ {
        @Independent @NotModified <T> MutableSet<T> empty() { return null; }
        @Independent @NotModified
        <T> MutableSet<T> fromStream(@Independent @NotModified Stream<? extends T> arg0) { return null; }
        @Independent @NotModified <T> MutableSet<T> of() { return null; }
        @Independent @NotModified <T> MutableSet<T> of(@Independent @NotModified T ... items) { return null; }
        @Independent @NotModified
        <T> MutableSet<T> ofAll(@Independent @NotModified Iterable<? extends T> items) { return null; }
        @Independent @NotModified <T> MutableSet<T> ofInitialCapacity(int capacity) { return null; }
        @Independent @NotModified <T> MutableSet<T> with() { return null; }
        @Independent @NotModified <T> MutableSet<T> with(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> MutableSet<T> withAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified <T> MutableSet<T> withInitialCapacity(int arg0) { return null; }
    }
}
