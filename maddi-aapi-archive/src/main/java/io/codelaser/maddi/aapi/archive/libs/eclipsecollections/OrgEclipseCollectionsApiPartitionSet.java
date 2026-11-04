package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.partition.set.PartitionImmutableSet;
import org.eclipse.collections.api.set.*;
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
public class OrgEclipseCollectionsApiPartitionSet {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.partition.set";
    //public interface PartitionImmutableSet implements PartitionUnsortedSet<T>, PartitionImmutableSetIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (2) -- G1: two immutable collections; computed through the implementor union (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class PartitionImmutableSet$<T> {
        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.set.PartitionImmutableSetIterable, org.eclipse.collections.api.partition.set.PartitionSet, org.eclipse.collections.api.partition.set.PartitionUnsortedSet
        @Independent(hc = true) @NotModified
        ImmutableSet<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.set.PartitionImmutableSetIterable, org.eclipse.collections.api.partition.set.PartitionSet, org.eclipse.collections.api.partition.set.PartitionUnsortedSet
        @Independent(hc = true) @NotModified
        ImmutableSet<T> getSelected() { return null; }
    }

    //public interface PartitionImmutableSetIterable implements PartitionSet<T>, PartitionImmutableCollection<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (2) -- G1: two immutable collections; computed through the implementor union (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class PartitionImmutableSetIterable$<T> {
        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.set.PartitionSet
        @Independent(hc = true) @NotModified
        ImmutableSetIterable<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionImmutableCollection, org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.set.PartitionSet
        @Independent(hc = true) @NotModified
        ImmutableSetIterable<T> getSelected() { return null; }
    }

    //public interface PartitionMutableSet implements PartitionUnsortedSet<T>, PartitionMutableSetIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (4) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionMutableSet$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.set.PartitionMutableSetIterable, org.eclipse.collections.api.partition.set.PartitionSet, org.eclipse.collections.api.partition.set.PartitionUnsortedSet
        @Independent(absent = true) @NotModified
        MutableSet<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.set.PartitionMutableSetIterable, org.eclipse.collections.api.partition.set.PartitionSet, org.eclipse.collections.api.partition.set.PartitionUnsortedSet
        @Independent(absent = true) @NotModified
        MutableSet<T> getSelected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionMutableCollection
        @Independent @Modified
        PartitionImmutableSet<T> toImmutable() { return null; }
    }

    //public interface PartitionMutableSetIterable implements PartitionSet<T>, PartitionMutableCollection<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (4) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionMutableSetIterable$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.set.PartitionSet
        @Independent(absent = true) @NotModified
        MutableSetIterable<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.PartitionMutableCollection, org.eclipse.collections.api.partition.set.PartitionSet
        @Independent(absent = true) @NotModified
        MutableSetIterable<T> getSelected() { return null; }
    }

    //public interface PartitionSet implements PartitionIterable<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (4) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionSet$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable
        @Independent(absent = true) @NotModified
        SetIterable<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable
        @Independent(absent = true) @NotModified
        SetIterable<T> getSelected() { return null; }
    }

    //public interface PartitionUnsortedSet implements PartitionSet<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (4) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionUnsortedSet$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.set.PartitionSet
        @Independent(absent = true) @NotModified
        UnsortedSetIterable<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.set.PartitionSet
        @Independent(absent = true) @NotModified
        UnsortedSetIterable<T> getSelected() { return null; }
    }
}
