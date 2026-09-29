package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Map;
import org.eclipse.collections.api.map.*;
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
public class OrgEclipseCollectionsApiFactoryMap {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory.map";
    //public interface FixedSizeMapFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class FixedSizeMapFactory$ {
        @Independent @NotModified <K, V> FixedSizeMap<K, V> empty() { return null; }
        @Independent @NotModified <K, V> FixedSizeMap<K, V> of() { return null; }
        @Independent @NotModified
        <K, V> FixedSizeMap<K, V> of(@Independent @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        @Independent @NotModified
        <K, V> FixedSizeMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> FixedSizeMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }
        @Independent @NotModified <K, V> FixedSizeMap<K, V> with() { return null; }
        @Independent @NotModified
        <K, V> FixedSizeMap<K, V> with(@Independent @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        @Independent @NotModified
        <K, V> FixedSizeMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> FixedSizeMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }
    }

    //public interface ImmutableMapFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class ImmutableMapFactory$ {
        @Independent @NotModified <K, V> ImmutableMap<K, V> empty() { return null; }
        @Independent @NotModified <K, V> ImmutableMap<K, V> of() { return null; }
        @Independent @NotModified
        <K, V> ImmutableMap<K, V> of(@Independent @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5,
            @Independent @NotModified K arg6,
            @Independent @NotModified V arg7) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> ofAll(@Independent @Modified Map<? extends K, ? extends V> arg0) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> ofMap(@Independent @Modified Map<? extends K, ? extends V> arg0) { return null; }
        @Independent @NotModified <K, V> ImmutableMap<K, V> with() { return null; }
        @Independent @NotModified
        <K, V> ImmutableMap<K, V> with(@Independent @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5,
            @Independent @NotModified K arg6,
            @Independent @NotModified V arg7) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> withAll(@Independent @Modified Map<? extends K, ? extends V> arg0) { return null; }
    }

    //public interface MutableMapFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MutableMapFactory$ {
        @Independent @NotModified <K, V> MutableMap<K, V> empty() { return null; }
        @Independent @NotModified <K, V> MutableMap<K, V> of() { return null; }
        @Independent @NotModified
        <K, V> MutableMap<K, V> of(@Independent @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5,
            @Independent @NotModified K arg6,
            @Independent @NotModified V arg7) { return null; }
        @Independent @NotModified <K, V> MutableMap<K, V> ofInitialCapacity(int arg0) { return null; }
        @Independent @NotModified
        <K, V> MutableMap<K, V> ofMap(@Independent @Modified Map<? extends K, ? extends V> arg0) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> ofMapIterable(@Independent @Modified MapIterable<? extends K, ? extends V> arg0) {
            return null;
        }
        @Independent @NotModified <K, V> MutableMap<K, V> with() { return null; }
        @Independent @NotModified
        <K, V> MutableMap<K, V> with(@Independent @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5,
            @Independent @NotModified K arg6,
            @Independent @NotModified V arg7) { return null; }
        @Independent @NotModified <K, V> MutableMap<K, V> withInitialCapacity(int arg0) { return null; }
        @Independent @NotModified
        <K, V> MutableMap<K, V> withMap(@Independent @Modified Map<? extends K, ? extends V> arg0) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> withMapIterable(@Independent @Modified MapIterable<? extends K, ? extends V> arg0) {
            return null;
        }
    }
}
