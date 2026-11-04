package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import java.util.Map;
import org.eclipse.collections.api.tuple.*;
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
public class OrgEclipseCollectionsApiTuple {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.tuple";
    //public interface Pair implements Serializable, Comparable<Pair<T1,T2>>
    //annotated as EXPECTED; computed @Immutable(hc = true) (4) -- a tuple is a value; Pair.put(Map) writes its argument, so no container (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class Pair$<T1, T2> {
        @Independent(hc = true) @NotModified T1 getOne() { return null; }
        @Independent(hc = true) @NotModified T2 getTwo() { return null; }
        @NotModified boolean isEqual() { return false; }
        @NotModified boolean isSame() { return false; }
        @NotModified void put(@Independent(hc = true) @Modified Map<? super T1, ? super T2> arg0) { }
        @Independent(hc = true) @NotModified Pair<T2, T1> swap() { return null; }
        @Independent @NotModified Map.Entry<T1, T2> toEntry() { return null; }
    }

    //public interface Triple implements Serializable, Comparable<Triple<T1,T2,T3>>
    //annotated as EXPECTED; computed @Immutable(hc = true) (4) -- a tuple is a value; Pair.put(Map) writes its argument, so no container (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class Triple$<T1, T2, T3> {
        @Independent(hc = true) @NotModified T1 getOne() { return null; }
        @Independent(hc = true) @NotModified T3 getThree() { return null; }
        @Independent(hc = true) @NotModified T2 getTwo() { return null; }
        @NotModified boolean isEqual() { return false; }
        @NotModified boolean isSame() { return false; }
        @Independent(hc = true) @NotModified Triple<T3, T2, T1> reverse() { return null; }
    }

    //public interface Triplet implements Triple<T,T,T>
    //annotated as EXPECTED; computed @Immutable(hc = true) (4) -- a tuple is a value; Pair.put(Map) writes its argument, so no container (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class Triplet$<T> {
        //override from org.eclipse.collections.api.tuple.Triple
        @Independent @NotModified
        Triplet<T> reverse() { return null; }
    }

    //public interface Twin implements Pair<T,T>
    //annotated as EXPECTED; computed @Immutable(hc = true) (4) -- a tuple is a value; Pair.put(Map) writes its argument, so no container (ECLIPSECOLLECTIONS.md)
    @Immutable(hc = true)
    class Twin$<T> {
        //override from org.eclipse.collections.api.tuple.Pair
        @Independent @NotModified
        Twin<T> swap() { return null; }
    }
}
