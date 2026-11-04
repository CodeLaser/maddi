package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.partition.set.sorted.PartitionImmutableSortedSet;
import org.eclipse.collections.api.set.sorted.ImmutableSortedSet;
import org.eclipse.collections.api.set.sorted.MutableSortedSet;
import org.eclipse.collections.api.set.sorted.SortedSetIterable;
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
public class OrgEclipseCollectionsApiPartitionSetSorted {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.partition.set.sorted";
    //public interface PartitionImmutableSortedSet implements PartitionSortedSet<T>, PartitionImmutableSetIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- G1: two immutable collections; computed through the implementor union (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class PartitionImmutableSortedSet$<T> {
        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable, org.eclipse.collections.api.partition.ordered.PartitionSortedIterable, org.eclipse.collections.api.partition.set.PartitionImmutableSetIterable, org.eclipse.collections.api.partition.set.PartitionSet, org.eclipse.collections.api.partition.set.sorted.PartitionSortedSet
        @Independent(hc = true) @NotModified
        ImmutableSortedSet<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable, org.eclipse.collections.api.partition.ordered.PartitionSortedIterable, org.eclipse.collections.api.partition.set.PartitionImmutableSetIterable, org.eclipse.collections.api.partition.set.PartitionSet, org.eclipse.collections.api.partition.set.sorted.PartitionSortedSet
        @Independent(hc = true) @NotModified
        ImmutableSortedSet<T> getSelected() { return null; }
    }

    //public interface PartitionMutableSortedSet implements PartitionSortedSet<T>, PartitionMutableSetIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (2) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionMutableSortedSet$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable, org.eclipse.collections.api.partition.ordered.PartitionSortedIterable, org.eclipse.collections.api.partition.set.PartitionMutableSetIterable, org.eclipse.collections.api.partition.set.PartitionSet, org.eclipse.collections.api.partition.set.sorted.PartitionSortedSet
        @Independent(absent = true) @NotModified
        MutableSortedSet<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable, org.eclipse.collections.api.partition.ordered.PartitionSortedIterable, org.eclipse.collections.api.partition.set.PartitionMutableSetIterable, org.eclipse.collections.api.partition.set.PartitionSet, org.eclipse.collections.api.partition.set.sorted.PartitionSortedSet
        @Independent(absent = true) @NotModified
        MutableSortedSet<T> getSelected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionMutableCollection
        @Independent @NotModified
        PartitionImmutableSortedSet<T> toImmutable() { return null; }
    }

    //public interface PartitionSortedSet implements PartitionSet<T>, PartitionSortedIterable<T>, PartitionReversibleIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (2) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionSortedSet$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable, org.eclipse.collections.api.partition.ordered.PartitionSortedIterable, org.eclipse.collections.api.partition.set.PartitionSet
        @Independent(absent = true) @NotModified
        SortedSetIterable<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable, org.eclipse.collections.api.partition.ordered.PartitionSortedIterable, org.eclipse.collections.api.partition.set.PartitionSet
        @Independent(absent = true) @NotModified
        SortedSetIterable<T> getSelected() { return null; }
    }
}
