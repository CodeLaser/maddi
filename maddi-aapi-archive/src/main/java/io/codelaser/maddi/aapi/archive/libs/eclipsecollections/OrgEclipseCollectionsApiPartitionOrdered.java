package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.ordered.OrderedIterable;
import org.eclipse.collections.api.ordered.ReversibleIterable;
import org.eclipse.collections.api.ordered.SortedIterable;
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
public class OrgEclipseCollectionsApiPartitionOrdered {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.partition.ordered";
    //public interface PartitionOrderedIterable implements PartitionIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (3) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionOrderedIterable$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable
        @Independent(absent = true) @NotModified
        OrderedIterable<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable
        @Independent(absent = true) @NotModified
        OrderedIterable<T> getSelected() { return null; }
    }

    //public interface PartitionReversibleIterable implements PartitionOrderedIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (3) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionReversibleIterable$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable
        @Independent(absent = true) @NotModified
        ReversibleIterable<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable
        @Independent(absent = true) @NotModified
        ReversibleIterable<T> getSelected() { return null; }
    }

    //public interface PartitionSortedIterable implements PartitionOrderedIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (3) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionSortedIterable$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable
        @Independent(absent = true) @NotModified
        SortedIterable<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable
        @Independent(absent = true) @NotModified
        SortedIterable<T> getSelected() { return null; }
    }
}
