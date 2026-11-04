package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.stream.Stream;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.MutableStack;
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
public class OrgEclipseCollectionsApiFactoryStack {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory.stack";
    //public interface ImmutableStackFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class ImmutableStackFactory$ {
        @Independent @NotModified <T> ImmutableStack<T> empty() { return null; }
        @Independent @NotModified
        <T> ImmutableStack<T> fromStream(@Independent @NotModified Stream<? extends T> stream) { return null; }
        @Independent @NotModified <T> ImmutableStack<T> of() { return null; }
        @Independent @NotModified <T> ImmutableStack<T> of(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified <T> ImmutableStack<T> of(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableStack<T> ofAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> ImmutableStack<T> ofAllReversed(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> ImmutableStack<T> ofReversed(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified <T> ImmutableStack<T> with() { return null; }
        @Independent @NotModified <T> ImmutableStack<T> with(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified <T> ImmutableStack<T> with(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableStack<T> withAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> ImmutableStack<T> withAllReversed(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> ImmutableStack<T> withReversed(@Independent @NotModified T ... arg0) { return null; }
    }

    //public interface MutableStackFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MutableStackFactory$ {
        @Independent @NotModified <T> MutableStack<T> empty() { return null; }
        @Independent @NotModified
        <T> MutableStack<T> fromStream(@Independent @NotModified Stream<? extends T> arg0) { return null; }
        @Independent @NotModified <T> MutableStack<T> of() { return null; }
        @Independent @NotModified <T> MutableStack<T> of(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified
        <T> MutableStack<T> ofAll(@Independent @NotModified Iterable<? extends T> elements) { return null; }

        @Independent @NotModified
        <T> MutableStack<T> ofAllReversed(@Independent @NotModified Iterable<? extends T> items) { return null; }

        @Independent @NotModified
        <T> MutableStack<T> ofReversed(@Independent @NotModified T ... elements) { return null; }
        @Independent @NotModified <T> MutableStack<T> with() { return null; }
        @Independent @NotModified <T> MutableStack<T> with(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> MutableStack<T> withAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableStack<T> withAllReversed(@Independent @NotModified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> MutableStack<T> withReversed(@Independent @NotModified T ... arg0) { return null; }
    }
}
