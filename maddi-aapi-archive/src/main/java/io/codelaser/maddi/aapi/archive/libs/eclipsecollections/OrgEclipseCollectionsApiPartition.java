package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.partition.PartitionImmutableCollection;
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
public class OrgEclipseCollectionsApiPartition {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.partition";
    //public interface PartitionImmutableCollection implements PartitionIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- G1: two immutable collections; computed through the implementor union (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class PartitionImmutableCollection$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable
        @Independent(hc = true) @NotModified
        ImmutableCollection<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable
        @Independent(hc = true) @NotModified
        ImmutableCollection<T> getSelected() { return null; }
    }

    //public interface PartitionIterable
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (2) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionIterable$<T> {
        @Independent(absent = true) @NotModified RichIterable<T> getRejected() { return null; }
        @Independent(absent = true) @NotModified RichIterable<T> getSelected() { return null; }
    }

    //public interface PartitionMutableCollection implements PartitionIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (2) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionMutableCollection$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable
        @Independent(absent = true) @NotModified
        MutableCollection<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable
        @Independent(absent = true) @NotModified
        MutableCollection<T> getSelected() { return null; }
        @Independent @Modified PartitionImmutableCollection<T> toImmutable() { return null; }
    }
}
