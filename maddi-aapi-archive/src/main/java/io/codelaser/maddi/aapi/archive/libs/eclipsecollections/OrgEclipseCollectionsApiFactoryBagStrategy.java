package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.Function;
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
public class OrgEclipseCollectionsApiFactoryBagStrategy {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory.bag.strategy";
    //public interface MutableHashingStrategyBagFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MutableHashingStrategyBagFactory$ {
        @Independent @NotModified
        <T> MutableBag<T> empty(@Independent @Modified HashingStrategy<? super T> arg0) { return null; }

        @Independent @NotModified
        <T, V> MutableBag<T> fromFunction(@Independent @Modified Function<? super T, ? extends V> function) {
            return null;
        }

        @Independent @NotModified
        <T> MutableBag<T> of(@Independent @Modified HashingStrategy<? super T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableBag<T> of(@Independent @Modified HashingStrategy<? super T> arg0, @Independent @Modified T ... arg1) {
            return null;
        }

        @Independent @NotModified
        <T> MutableBag<T> ofAll(
            @Independent @Modified HashingStrategy<? super T> arg0,
            @Independent @Modified Iterable<? extends T> arg1) { return null; }

        @Independent @NotModified
        <T> MutableBag<T> with(@Independent @Modified HashingStrategy<? super T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableBag<T> with(
            @Independent @Modified HashingStrategy<? super T> arg0,
            @Independent @Modified T ... arg1) { return null; }

        @Independent @NotModified
        <T> MutableBag<T> withAll(
            @Independent @Modified HashingStrategy<? super T> arg0,
            @Independent @Modified Iterable<? extends T> arg1) { return null; }
    }
}
