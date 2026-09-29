package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.bag.*;
import org.eclipse.collections.api.partition.bag.PartitionImmutableBag;
import org.eclipse.collections.api.partition.bag.PartitionImmutableBagIterable;
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
public class OrgEclipseCollectionsApiPartitionBag {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.partition.bag";
    //public interface PartitionBag implements PartitionIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (4) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionBag$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable
        @Independent(absent = true) @NotModified
        Bag<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable
        @Independent(absent = true) @NotModified
        Bag<T> getSelected() { return null; }
    }

    //public interface PartitionImmutableBag implements PartitionImmutableBagIterable<T>, PartitionUnsortedBag<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (2) -- G1: two immutable collections; computed through the implementor union (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class PartitionImmutableBag$<T> {
        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.bag.PartitionBag, org.eclipse.collections.api.partition.bag.PartitionImmutableBagIterable, org.eclipse.collections.api.partition.bag.PartitionUnsortedBag
        @Independent(hc = true) @NotModified
        ImmutableBag<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.bag.PartitionBag, org.eclipse.collections.api.partition.bag.PartitionImmutableBagIterable, org.eclipse.collections.api.partition.bag.PartitionUnsortedBag
        @Independent(hc = true) @NotModified
        ImmutableBag<T> getSelected() { return null; }
    }

    //public interface PartitionImmutableBagIterable implements PartitionImmutableCollection<T>, PartitionBag<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (2) -- G1: two immutable collections; computed through the implementor union (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class PartitionImmutableBagIterable$<T> {
        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.bag.PartitionBag
        @Independent(hc = true) @NotModified
        ImmutableBagIterable<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.bag.PartitionBag
        @Independent(hc = true) @NotModified
        ImmutableBagIterable<T> getSelected() { return null; }
    }

    //public interface PartitionMutableBag implements PartitionMutableBagIterable<T>, PartitionUnsortedBag<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (4) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionMutableBag$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.bag.PartitionBag, org.eclipse.collections.api.partition.bag.PartitionMutableBagIterable, org.eclipse.collections.api.partition.bag.PartitionUnsortedBag
        @Independent(absent = true) @NotModified
        MutableBag<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.bag.PartitionBag, org.eclipse.collections.api.partition.bag.PartitionMutableBagIterable, org.eclipse.collections.api.partition.bag.PartitionUnsortedBag
        @Independent(absent = true) @NotModified
        MutableBag<T> getSelected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.bag.PartitionMutableBagIterable
        @Independent @Modified
        PartitionImmutableBag<T> toImmutable() { return null; }
    }

    //public interface PartitionMutableBagIterable implements PartitionMutableCollection<T>, PartitionBag<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (4) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionMutableBagIterable$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.bag.PartitionBag
        @Independent(absent = true) @NotModified
        MutableBagIterable<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.bag.PartitionBag
        @Independent(absent = true) @NotModified
        MutableBagIterable<T> getSelected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionMutableCollection
        @Independent @Modified
        PartitionImmutableBagIterable<T> toImmutable() { return null; }
    }

    //public interface PartitionUnsortedBag implements PartitionBag<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (4) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionUnsortedBag$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.bag.PartitionBag
        @Independent(absent = true) @NotModified
        UnsortedBag<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.bag.PartitionBag
        @Independent(absent = true) @NotModified
        UnsortedBag<T> getSelected() { return null; }
    }
}
