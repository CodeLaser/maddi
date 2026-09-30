package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
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
public class OrgEclipseCollectionsApiFactorySetStrategy {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory.set.strategy";
    //public interface ImmutableHashingStrategySetFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class ImmutableHashingStrategySetFactory$ {
        @Independent @NotModified
        <T> ImmutableSet<T> of(@Independent @NotModified HashingStrategy<? super T> arg0) { return null; }

        @Independent @NotModified
        <T> ImmutableSet<T> of(
            @Independent @NotModified HashingStrategy<? super T> arg0,
            @Independent @NotModified T ... arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSet<T> ofAll(
            @Independent @NotModified HashingStrategy<? super T> arg0,
            @Independent @NotModified Iterable<? extends T> arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSet<T> ofInitialCapacity(@Independent @NotModified HashingStrategy<? super T> arg0, int arg1) {
            return null;
        }

        @Independent @NotModified
        <T> ImmutableSet<T> with(@Independent @NotModified HashingStrategy<? super T> arg0) { return null; }

        @Independent @NotModified
        <T> ImmutableSet<T> with(
            @Independent @NotModified HashingStrategy<? super T> arg0,
            @Independent @NotModified T ... arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSet<T> withAll(
            @Independent @NotModified HashingStrategy<? super T> arg0,
            @Independent @NotModified Iterable<? extends T> arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableSet<T> withInitialCapacity(@Independent @NotModified HashingStrategy<? super T> arg0, int arg1) {
            return null;
        }
    }

    //public interface MutableHashingStrategySetFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MutableHashingStrategySetFactory$ {
        @Independent @NotModified
        <T, V> MutableSet<T> fromFunction(@Independent @Modified Function<? super T, ? extends V> function) {
            return null;
        }

        @Independent @NotModified
        <T> MutableSet<T> of(@Independent @NotModified HashingStrategy<? super T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableSet<T> of(
            @Independent @NotModified HashingStrategy<? super T> arg0,
            @Independent @NotModified T ... arg1) { return null; }

        @Independent @NotModified
        <T> MutableSet<T> ofAll(
            @Independent @Modified HashingStrategy<? super T> arg0,
            @Independent @Modified Iterable<? extends T> arg1) { return null; }

        @Independent @NotModified
        <T> MutableSet<T> ofInitialCapacity(@Independent @NotModified HashingStrategy<? super T> arg0, int arg1) {
            return null;
        }

        @Independent @NotModified
        <T> MutableSet<T> with(@Independent @NotModified HashingStrategy<? super T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableSet<T> with(
            @Independent @NotModified HashingStrategy<? super T> arg0,
            @Independent @NotModified T ... arg1) { return null; }

        @Independent @NotModified
        <T> MutableSet<T> withAll(
            @Independent @Modified HashingStrategy<? super T> arg0,
            @Independent @Modified Iterable<? extends T> arg1) { return null; }

        @Independent @NotModified
        <T> MutableSet<T> withInitialCapacity(@Independent @NotModified HashingStrategy<? super T> arg0, int arg1) {
            return null;
        }
    }
}
