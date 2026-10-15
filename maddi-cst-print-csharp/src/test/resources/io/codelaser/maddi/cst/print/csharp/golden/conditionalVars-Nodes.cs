namespace Org.Example.Nodeses;
public sealed class Nodes {
    internal static int Build<A>(bool joiners) {
        Node<A> node = joiners ? new Indexed<A>() : new Unindexed<A>();
        return node.Size;
    }

    internal static string Describe(object bean) {
        object beanClass = bean == null ? "null" : bean.GetType();
        return "" + beanClass;
    }

    internal static int Same(bool b) {
        var n = b ? new Indexed<string>() : new Indexed<string>();
        return n.Size;
    }
    internal sealed class Indexed<A> : Node<A> { internal override int Size => 1; }
    internal abstract class Node<A> { internal abstract int Size { get; } }
    internal sealed class Unindexed<A> : Node<A> { internal override int Size => 0; }
}
