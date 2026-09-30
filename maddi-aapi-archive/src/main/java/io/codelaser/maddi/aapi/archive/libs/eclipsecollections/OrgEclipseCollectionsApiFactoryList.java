package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Comparator;
import java.util.stream.Stream;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.*;
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
public class OrgEclipseCollectionsApiFactoryList {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory.list";
    //public interface FixedSizeListFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class FixedSizeListFactory$ {
        @Independent @NotModified <T> FixedSizeList<T> empty() { return null; }
        @Independent @NotModified
        <T> FixedSizeList<T> fromStream(@Independent @NotModified Stream<? extends T> arg0) { return null; }
        @Independent @NotModified <T> FixedSizeList<T> of() { return null; }
        @Independent @NotModified <T> FixedSizeList<T> of(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified
        <T> FixedSizeList<T> of(@Independent @NotModified T arg0, @Independent @NotModified T arg1) { return null; }

        @Independent @NotModified
        <T> FixedSizeList<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2) { return null; }

        @Independent @NotModified
        <T> FixedSizeList<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3) { return null; }

        @Independent @NotModified
        <T> FixedSizeList<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4) { return null; }

        @Independent @NotModified
        <T> FixedSizeList<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4,
            @Independent @NotModified T arg5) { return null; }
        @Independent @NotModified <T> FixedSizeList<T> of(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> FixedSizeList<T> ofAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified <T> FixedSizeList<T> with() { return null; }
        @Independent @NotModified <T> FixedSizeList<T> with(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified
        <T> FixedSizeList<T> with(@Independent @NotModified T arg0, @Independent @NotModified T arg1) { return null; }

        @Independent @NotModified
        <T> FixedSizeList<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2) { return null; }

        @Independent @NotModified
        <T> FixedSizeList<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3) { return null; }

        @Independent @NotModified
        <T> FixedSizeList<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4) { return null; }

        @Independent @NotModified
        <T> FixedSizeList<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4,
            @Independent @NotModified T arg5) { return null; }
        @Independent @NotModified <T> FixedSizeList<T> with(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> FixedSizeList<T> withAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
    }

    //public interface ImmutableListFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class ImmutableListFactory$ {
        @Independent @NotModified <T> ImmutableList<T> empty() { return null; }
        @Independent @NotModified
        <T> ImmutableList<T> fromStream(@Independent @NotModified Stream<? extends T> stream) { return null; }
        @Independent @NotModified <T> ImmutableList<T> of() { return null; }
        @Independent @NotModified <T> ImmutableList<T> of(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableList<T> of(@Independent @NotModified T arg0, @Independent @NotModified T arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4,
            @Independent @NotModified T arg5) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4,
            @Independent @NotModified T arg5,
            @Independent @NotModified T arg6) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4,
            @Independent @NotModified T arg5,
            @Independent @NotModified T arg6,
            @Independent @NotModified T arg7) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4,
            @Independent @NotModified T arg5,
            @Independent @NotModified T arg6,
            @Independent @NotModified T arg7,
            @Independent @NotModified T arg8) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> of(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4,
            @Independent @NotModified T arg5,
            @Independent @NotModified T arg6,
            @Independent @NotModified T arg7,
            @Independent @NotModified T arg8,
            @Independent @NotModified T arg9) { return null; }
        @Independent @NotModified <T> ImmutableList<T> of(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableList<T> ofAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified <T> ImmutableList<T> with() { return null; }
        @Independent @NotModified <T> ImmutableList<T> with(@Independent @NotModified T arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableList<T> with(@Independent @NotModified T arg0, @Independent @NotModified T arg1) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4,
            @Independent @NotModified T arg5) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4,
            @Independent @NotModified T arg5,
            @Independent @NotModified T arg6) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4,
            @Independent @NotModified T arg5,
            @Independent @NotModified T arg6,
            @Independent @NotModified T arg7) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4,
            @Independent @NotModified T arg5,
            @Independent @NotModified T arg6,
            @Independent @NotModified T arg7,
            @Independent @NotModified T arg8) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> with(
            @Independent @NotModified T arg0,
            @Independent @NotModified T arg1,
            @Independent @NotModified T arg2,
            @Independent @NotModified T arg3,
            @Independent @NotModified T arg4,
            @Independent @NotModified T arg5,
            @Independent @NotModified T arg6,
            @Independent @NotModified T arg7,
            @Independent @NotModified T arg8,
            @Independent @NotModified T arg9) { return null; }
        @Independent @NotModified <T> ImmutableList<T> with(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> ImmutableList<T> withAll(@Independent @Modified Iterable<? extends T> arg0) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> withAllSorted(
            @Independent @NotModified Comparator<? super T> comparator,
            @Independent @Modified RichIterable<? extends T> items) { return null; }

        @Independent @NotModified
        <T> ImmutableList<T> withAllSorted(@Independent @Modified RichIterable<? extends T> items) { return null; }
    }

    //public interface MultiReaderListFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MultiReaderListFactory$ {
        @Independent @NotModified <T> MultiReaderList<T> empty() { return null; }
        @Independent @NotModified
        <T> MultiReaderList<T> fromStream(@Independent @NotModified Stream<? extends T> arg0) { return null; }
        @Independent @NotModified <T> MultiReaderList<T> of() { return null; }
        @Independent @NotModified <T> MultiReaderList<T> of(@Independent @NotModified T ... items) { return null; }
        @Independent @NotModified
        <T> MultiReaderList<T> ofAll(@Independent @NotModified Iterable<? extends T> iterable) { return null; }
        @Independent @NotModified <T> MultiReaderList<T> ofInitialCapacity(int capacity) { return null; }
        @Independent @NotModified <T> MultiReaderList<T> with() { return null; }
        @Independent @NotModified <T> MultiReaderList<T> with(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> MultiReaderList<T> withAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified <T> MultiReaderList<T> withInitialCapacity(int arg0) { return null; }
        @Independent @NotModified
        <T> MultiReaderList<T> withNValues(int arg0, @Independent @NotModified Function0<? extends T> arg1) {
            return null;
        }
    }

    //public interface MutableListFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MutableListFactory$ {
        @Independent @NotModified <T> MutableList<T> empty() { return null; }
        @Independent @NotModified
        <T> MutableList<T> fromStream(@Independent @NotModified Stream<? extends T> arg0) { return null; }
        @Independent @NotModified <T> MutableList<T> of() { return null; }
        @Independent @NotModified <T> MutableList<T> of(@Independent @NotModified T ... items) { return null; }
        @Independent @NotModified
        <T> MutableList<T> ofAll(@Independent @NotModified Iterable<? extends T> iterable) { return null; }
        @Independent @NotModified <T> MutableList<T> ofInitialCapacity(int capacity) { return null; }
        @Independent @NotModified <T> MutableList<T> with() { return null; }
        @Independent @NotModified <T> MutableList<T> with(@Independent @NotModified T ... arg0) { return null; }
        @Independent @NotModified
        <T> MutableList<T> withAll(@Independent @NotModified Iterable<? extends T> arg0) { return null; }
        @Independent @NotModified <T> MutableList<T> withInitialCapacity(int arg0) { return null; }
        @Independent @NotModified
        <T> MutableList<T> withNValues(int arg0, @Independent @NotModified Function0<? extends T> arg1) { return null; }
        @Independent @NotModified <T> MutableList<T> wrapCopy(@Independent @NotModified T ... array) { return null; }
    }
}
