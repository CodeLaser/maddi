package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.bag.sorted.ImmutableSortedBag;
import org.eclipse.collections.api.bag.sorted.MutableSortedBag;
import org.eclipse.collections.api.bag.sorted.SortedBag;
import org.eclipse.collections.api.partition.bag.sorted.PartitionImmutableSortedBag;
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
public class OrgEclipseCollectionsApiPartitionBagSorted {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.partition.bag.sorted";
    //public interface PartitionImmutableSortedBag implements PartitionSortedBag<T>, PartitionImmutableBagIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- G1: two immutable collections; computed through the implementor union (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class PartitionImmutableSortedBag$<T> {
        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.bag.PartitionBag, org.eclipse.collections.api.partition.bag.PartitionImmutableBagIterable, org.eclipse.collections.api.partition.bag.sorted.PartitionSortedBag, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable, org.eclipse.collections.api.partition.ordered.PartitionSortedIterable
        @Independent(hc = true) @NotModified
        ImmutableSortedBag<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.bag.PartitionBag, org.eclipse.collections.api.partition.bag.PartitionImmutableBagIterable, org.eclipse.collections.api.partition.bag.sorted.PartitionSortedBag, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable, org.eclipse.collections.api.partition.ordered.PartitionSortedIterable
        @Independent(hc = true) @NotModified
        ImmutableSortedBag<T> getSelected() { return null; }
    }

    //public interface PartitionMutableSortedBag implements PartitionSortedBag<T>, PartitionMutableBagIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (2) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionMutableSortedBag$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.bag.PartitionBag, org.eclipse.collections.api.partition.bag.PartitionMutableBagIterable, org.eclipse.collections.api.partition.bag.sorted.PartitionSortedBag, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable, org.eclipse.collections.api.partition.ordered.PartitionSortedIterable
        @Independent(absent = true) @NotModified
        MutableSortedBag<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.bag.PartitionBag, org.eclipse.collections.api.partition.bag.PartitionMutableBagIterable, org.eclipse.collections.api.partition.bag.sorted.PartitionSortedBag, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable, org.eclipse.collections.api.partition.ordered.PartitionSortedIterable
        @Independent(absent = true) @NotModified
        MutableSortedBag<T> getSelected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.bag.PartitionMutableBagIterable
        @Independent @Modified
        PartitionImmutableSortedBag<T> toImmutable() { return null; }
    }

    //public interface PartitionSortedBag implements PartitionBag<T>, PartitionSortedIterable<T>, PartitionReversibleIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (2) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionSortedBag$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.bag.PartitionBag, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable, org.eclipse.collections.api.partition.ordered.PartitionSortedIterable
        @Independent(absent = true) @NotModified
        SortedBag<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.bag.PartitionBag, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.ordered.PartitionReversibleIterable, org.eclipse.collections.api.partition.ordered.PartitionSortedIterable
        @Independent(absent = true) @NotModified
        SortedBag<T> getSelected() { return null; }
    }
}
