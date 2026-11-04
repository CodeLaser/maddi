package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.partition.list.PartitionImmutableList;
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
public class OrgEclipseCollectionsApiPartitionList {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.partition.list";
    //public interface PartitionImmutableList implements PartitionImmutableCollection<T>, PartitionList<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- G1: two immutable collections; computed through the implementor union (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class PartitionImmutableList$<T> {
        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.list.PartitionList, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable
        @Independent(hc = true) @NotModified
        ImmutableList<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.list.PartitionList, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable
        @Independent(hc = true) @NotModified
        ImmutableList<T> getSelected() { return null; }
    }

    //public interface PartitionList implements PartitionReversibleIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (2) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionList$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable
        @Independent(absent = true) @NotModified
        ListIterable<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable
        @Independent(absent = true) @NotModified
        ListIterable<T> getSelected() { return null; }
    }

    //public interface PartitionMutableList implements PartitionMutableCollection<T>, PartitionList<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (2) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionMutableList$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.list.PartitionList, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable
        @Independent(absent = true) @NotModified
        MutableList<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.list.PartitionList, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable
        @Independent(absent = true) @NotModified
        MutableList<T> getSelected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionMutableCollection
        @Independent @Modified
        PartitionImmutableList<T> toImmutable() { return null; }
    }
}
