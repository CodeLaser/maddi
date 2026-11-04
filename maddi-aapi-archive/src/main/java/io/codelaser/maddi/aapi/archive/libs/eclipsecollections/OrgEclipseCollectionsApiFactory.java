package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.lang.reflect.Method;
import org.eclipse.collections.api.factory.bag.ImmutableBagFactory;
import org.eclipse.collections.api.factory.bag.MultiReaderBagFactory;
import org.eclipse.collections.api.factory.bag.MutableBagFactory;
import org.eclipse.collections.api.factory.bag.sorted.ImmutableSortedBagFactory;
import org.eclipse.collections.api.factory.bag.sorted.MutableSortedBagFactory;
import org.eclipse.collections.api.factory.bimap.ImmutableBiMapFactory;
import org.eclipse.collections.api.factory.bimap.MutableBiMapFactory;
import org.eclipse.collections.api.factory.list.*;
import org.eclipse.collections.api.factory.map.FixedSizeMapFactory;
import org.eclipse.collections.api.factory.map.ImmutableMapFactory;
import org.eclipse.collections.api.factory.map.MutableMapFactory;
import org.eclipse.collections.api.factory.map.sorted.ImmutableSortedMapFactory;
import org.eclipse.collections.api.factory.map.sorted.MutableSortedMapFactory;
import org.eclipse.collections.api.factory.set.*;
import org.eclipse.collections.api.factory.set.sorted.ImmutableSortedSetFactory;
import org.eclipse.collections.api.factory.set.sorted.MutableSortedSetFactory;
import org.eclipse.collections.api.factory.stack.ImmutableStackFactory;
import org.eclipse.collections.api.factory.stack.MutableStackFactory;
import io.codelaser.maddi.annotation.Container;
import io.codelaser.maddi.annotation.FinalFields;
import io.codelaser.maddi.annotation.Fluent;
import io.codelaser.maddi.annotation.Immutable;
import io.codelaser.maddi.annotation.Independent;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
@Independent(absent = true)
public class OrgEclipseCollectionsApiFactory {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.factory";
    //public final class Bags
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class Bags$ {
        @Independent @NotModified static final ImmutableBagFactory immutable = null;
        @Independent @NotModified static final MultiReaderBagFactory multiReader = null;
        @Independent @NotModified static final MutableBagFactory mutable = null;
    }

    //public final class BiMaps
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class BiMaps$ {
        @Independent @NotModified static final ImmutableBiMapFactory immutable = null;
        @Independent @NotModified static final MutableBiMapFactory mutable = null;
    }

    //public final class Lists
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class Lists$ {
        @Independent @NotModified static final FixedSizeListFactory fixedSize = null;
        @Independent @NotModified static final ImmutableListFactory immutable = null;
        @Independent @NotModified static final MultiReaderListFactory multiReader = null;
        @Independent @NotModified static final MutableListFactory mutable = null;
    }

    //public final class Maps
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class Maps$ {
        @Independent @NotModified static final FixedSizeMapFactory fixedSize = null;
        @Independent @NotModified static final ImmutableMapFactory immutable = null;
        @Independent @NotModified static final MutableMapFactory mutable = null;
    }

    //public final class ServiceLoaderUtils
    //EXPECTED no claim: a utility class -- computed @FinalFields @Independent @Container (1), annotated --  (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent
    class ServiceLoaderUtils$ {
        @Independent @NotModified static <T> T loadServiceClass(Class<T> serviceClass) { return null; }
    }

    //public final class Sets
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class Sets$ {
        @Independent @NotModified static final FixedSizeSetFactory fixedSize = null;
        @Independent @NotModified static final ImmutableSetFactory immutable = null;
        @Independent @NotModified static final MultiReaderSetFactory multiReader = null;
        @Independent @NotModified static final MutableSetFactory mutable = null;
    }

    //public final class SortedBags
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class SortedBags$ {
        @Independent @NotModified static final ImmutableSortedBagFactory immutable = null;
        @Independent @NotModified static final MutableSortedBagFactory mutable = null;
    }

    //public final class SortedMaps
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class SortedMaps$ {
        @Independent @NotModified static final ImmutableSortedMapFactory immutable = null;
        @Independent @NotModified static final MutableSortedMapFactory mutable = null;
    }

    //public final class SortedSets
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class SortedSets$ {
        @Independent @NotModified static final ImmutableSortedSetFactory immutable = null;
        @Independent @NotModified static final MutableSortedSetFactory mutable = null;
    }

    //public final class Stacks
    //annotated as EXPECTED; computed @Immutable(hc = true) (38) -- F-factory: a factory is stateless; a Mutable*Factory creates mutable collections, it is not one (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class Stacks$ {
        @Independent @NotModified static final ImmutableStackFactory immutable = null;
        @Independent @NotModified static final MutableStackFactory mutable = null;
    }

    //public class ThrowingInvocationHandler implements InvocationHandler
    //EXPECTED no claim -- computed mutable @Dependent @Container (1), annotated --  (ECLIPSECOLLECTIONS.md)
    @Container
    @Independent(absent = true)
    class ThrowingInvocationHandler$ {
        ThrowingInvocationHandler$(String error) { }
        //override from java.lang.reflect.InvocationHandler
        @Independent @NotModified

        Object invoke(
            @Independent @NotModified Object proxy,
            @Independent @NotModified Method method,
            @Independent @NotModified Object [] args) { return null; }
    }
}
