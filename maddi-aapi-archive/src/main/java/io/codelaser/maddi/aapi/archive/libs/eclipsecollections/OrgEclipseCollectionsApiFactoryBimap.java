package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Map;
import org.eclipse.collections.api.bimap.ImmutableBiMap;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.api.map.ImmutableMap;
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
@Independent(absent = true)
public class OrgEclipseCollectionsApiFactoryBimap {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory.bimap";
    //public interface ImmutableBiMapFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class ImmutableBiMapFactory$ {
        @Independent @NotModified <K, V> ImmutableBiMap<K, V> empty() { return null; }
        @Independent @NotModified <K, V> ImmutableBiMap<K, V> of() { return null; }
        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> of(@Independent @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }

        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5,
            @Independent @NotModified K arg6,
            @Independent @NotModified V arg7) { return null; }

        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> ofAll(@Independent @Modified Map<K, V> arg0) { return null; }

        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> ofAll(@Independent @Modified MutableBiMap<K, V> arg0) { return null; }

        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> ofAll(@Independent @Modified ImmutableMap<K, V> arg0) { return null; }
        @Independent @NotModified <K, V> ImmutableBiMap<K, V> with() { return null; }
        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> with(@Independent @NotModified K arg0, @Independent @NotModified V arg1) {
            return null;
        }

        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }

        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5,
            @Independent @NotModified K arg6,
            @Independent @NotModified V arg7) { return null; }

        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> withAll(@Independent @Modified Map<K, V> arg0) { return null; }

        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> withAll(@Independent @Modified MutableBiMap<K, V> arg0) { return null; }

        @Independent @NotModified
        <K, V> ImmutableBiMap<K, V> withAll(@Independent @Modified ImmutableMap<K, V> arg0) { return null; }
    }

    //public interface MutableBiMapFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MutableBiMapFactory$ {
        @Independent @NotModified <K, V> MutableBiMap<K, V> empty() { return null; }
        @Independent @NotModified <K, V> MutableBiMap<K, V> of() { return null; }
        @Independent @NotModified
        <K, V> MutableBiMap<K, V> of(@Independent @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        @Independent @NotModified
        <K, V> MutableBiMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> MutableBiMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }

        @Independent @NotModified
        <K, V> MutableBiMap<K, V> of(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5,
            @Independent @NotModified K arg6,
            @Independent @NotModified V arg7) { return null; }
        @Independent @NotModified <K, V> MutableBiMap<K, V> with() { return null; }
        @Independent @NotModified
        <K, V> MutableBiMap<K, V> with(@Independent @NotModified K arg0, @Independent @NotModified V arg1) { return null; }

        @Independent @NotModified
        <K, V> MutableBiMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3) { return null; }

        @Independent @NotModified
        <K, V> MutableBiMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5) { return null; }

        @Independent @NotModified
        <K, V> MutableBiMap<K, V> with(
            @Independent @NotModified K arg0,
            @Independent @NotModified V arg1,
            @Independent @NotModified K arg2,
            @Independent @NotModified V arg3,
            @Independent @NotModified K arg4,
            @Independent @NotModified V arg5,
            @Independent @NotModified K arg6,
            @Independent @NotModified V arg7) { return null; }
    }
}
