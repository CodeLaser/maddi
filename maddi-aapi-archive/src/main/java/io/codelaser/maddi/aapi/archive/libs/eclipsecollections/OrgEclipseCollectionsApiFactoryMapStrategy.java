package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Map;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
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
public class OrgEclipseCollectionsApiFactoryMapStrategy {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory.map.strategy";
    //public interface ImmutableHashingStrategyMapFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class ImmutableHashingStrategyMapFactory$ {
        @Independent @NotModified
        <K, V> ImmutableMap<K, V> of(@Independent @NotModified HashingStrategy<? super K> arg0) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @Modified V arg2) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @Modified V arg2,
            @Independent @Modified K arg3,
            @Independent @Modified V arg4) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @Modified V arg2,
            @Independent @Modified K arg3,
            @Independent @Modified V arg4,
            @Independent @Modified K arg5,
            @Independent @Modified V arg6) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> of(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @Modified V arg2,
            @Independent @Modified K arg3,
            @Independent @Modified V arg4,
            @Independent @Modified K arg5,
            @Independent @Modified V arg6,
            @Independent @Modified K arg7,
            @Independent @Modified V arg8) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> ofAll(@Independent @Modified Map<K, V> arg0) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> ofMap(@Independent @Modified Map<K, V> arg0) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> with(@Independent @NotModified HashingStrategy<? super K> arg0) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> with(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @Modified V arg2) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> with(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @Modified V arg2,
            @Independent @Modified K arg3,
            @Independent @Modified V arg4) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> with(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @Modified V arg2,
            @Independent @Modified K arg3,
            @Independent @Modified V arg4,
            @Independent @Modified K arg5,
            @Independent @Modified V arg6) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> with(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @Modified V arg2,
            @Independent @Modified K arg3,
            @Independent @Modified V arg4,
            @Independent @Modified K arg5,
            @Independent @Modified V arg6,
            @Independent @Modified K arg7,
            @Independent @Modified V arg8) { return null; }

        @Independent @NotModified
        <K, V> ImmutableMap<K, V> withAll(@Independent @Modified Map<K, V> arg0) { return null; }
    }

    //public interface MutableHashingStrategyMapFactory
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class MutableHashingStrategyMapFactory$ {
        @Independent @NotModified
        <K, V, T> MutableMap<K, V> fromFunction(@Independent @Modified Function<? super K, ? extends T> function) {
            return null;
        }

        @Independent @NotModified
        <K, V> MutableMap<K, V> of(@Independent @Modified HashingStrategy<? super K> arg0) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> of(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @NotModified V arg2) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> of(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @NotModified V arg2,
            @Independent @Modified K arg3,
            @Independent @NotModified V arg4) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> of(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @NotModified V arg2,
            @Independent @Modified K arg3,
            @Independent @NotModified V arg4,
            @Independent @Modified K arg5,
            @Independent @NotModified V arg6) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> of(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @NotModified V arg2,
            @Independent @Modified K arg3,
            @Independent @NotModified V arg4,
            @Independent @Modified K arg5,
            @Independent @NotModified V arg6,
            @Independent @Modified K arg7,
            @Independent @NotModified V arg8) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> with(@Independent @Modified HashingStrategy<? super K> arg0) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> with(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @NotModified V arg2) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> with(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @NotModified V arg2,
            @Independent @Modified K arg3,
            @Independent @NotModified V arg4) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> with(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @NotModified V arg2,
            @Independent @Modified K arg3,
            @Independent @NotModified V arg4,
            @Independent @Modified K arg5,
            @Independent @NotModified V arg6) { return null; }

        @Independent @NotModified
        <K, V> MutableMap<K, V> with(
            @Independent @NotModified HashingStrategy<? super K> arg0,
            @Independent @Modified K arg1,
            @Independent @NotModified V arg2,
            @Independent @Modified K arg3,
            @Independent @NotModified V arg4,
            @Independent @Modified K arg5,
            @Independent @NotModified V arg6,
            @Independent @Modified K arg7,
            @Independent @NotModified V arg8) { return null; }
    }
}
