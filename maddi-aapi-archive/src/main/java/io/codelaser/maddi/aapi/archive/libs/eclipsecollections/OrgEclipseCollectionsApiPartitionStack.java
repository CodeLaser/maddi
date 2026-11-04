package io.codelaser.maddi.aapi.archive.libs.eclipsecollections;
import org.eclipse.collections.api.partition.stack.PartitionImmutableStack;
import org.eclipse.collections.api.stack.ImmutableStack;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.stack.StackIterable;
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
public class OrgEclipseCollectionsApiPartitionStack {
    public static final String PACKAGE_NAME = "org.eclipse.collections.api.partition.stack";
    //public interface PartitionImmutableStack implements PartitionStack<T>
    //annotated as EXPECTED; computed @FinalFields @Dependent @Container (1) -- G1: two immutable collections; computed through the implementor union (ECLIPSECOLLECTIONS.md)
    @ImmutableContainer(hc = true)
    class PartitionImmutableStack$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.stack.PartitionStack
        @Independent(hc = true) @NotModified
        ImmutableStack<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.stack.PartitionStack
        @Independent(hc = true) @NotModified
        ImmutableStack<T> getSelected() { return null; }
    }

    //public interface PartitionMutableStack implements PartitionStack<T>
    //annotated as EXPECTED; computed @Immutable(hc = true) (1), @FinalFields @Dependent @Container (1) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent
    class PartitionMutableStack$<T> {
        @NotModified void add(@Independent @NotModified T arg0) { }
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.stack.PartitionStack
        @Independent @NotModified
        MutableStack<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable, org.eclipse.collections.api.partition.stack.PartitionStack
        @Independent @NotModified
        MutableStack<T> getSelected() { return null; }
        @Independent @NotModified PartitionImmutableStack<T> toImmutable() { return null; }
    }

    //public interface PartitionStack implements PartitionOrderedIterable<T>
    //annotated as EXPECTED; computed @Immutable(hc = true) (1), @FinalFields @Dependent @Container (1) -- F3: a partition holds two collections in final fields; the mutable and shared ones are not immutable (ECLIPSECOLLECTIONS.md)
    @FinalFields
    @Container
    @Independent(absent = true)
    class PartitionStack$<T> {
        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable
        @Independent(absent = true) @NotModified
        StackIterable<T> getRejected() { return null; }

        //override from org.eclipse.collections.api.partition.PartitionIterable, org.eclipse.collections.api.partition.ordered.PartitionOrderedIterable
        @Independent(absent = true) @NotModified
        StackIterable<T> getSelected() { return null; }
    }
}
