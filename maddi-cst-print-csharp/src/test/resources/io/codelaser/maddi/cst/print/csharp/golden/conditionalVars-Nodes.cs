using Maddi.JavaCompat;
using System;
namespace Org.Example.Nodeses;
public sealed class Nodes {
    internal static int Build<A>(bool joiners) {
        Node<A> node = joiners ? new Indexed<A>() : new Unindexed<A>();
        return node.Size;
    }

    internal static string Describe(object bean) {
        object beanClass = bean == null ? "null" : bean.GetType();
        return "" + JavaString.ValueOf(beanClass);
    }

    internal static int Same(bool b) {
        var n = b ? new Indexed<string>() : new Indexed<string>();
        return n.Size;
    }

    internal static int exists<A, B>(bool joiners) {
        IExists<A> node = joiners ? new IndexedExists<A, B>() : new UnindexedExists<A, string>();
        return node.Size;
    }

    internal static string MetaModel(bool genuine) {
        INamed model = genuine ? new Genuine() : new Shadow();
        return model.Name;
    }

    internal abstract class Exists<T, R> : IExists<T> {
        internal abstract int Size { get; }
        int IExists<T>.Size => Size;
    }
    internal interface IExists<T> { internal int Size { get; } }

    internal sealed class Genuine : INamed, ISized {
        public string Name => "g";
        public int Size => 1;
    }
    internal sealed class Indexed<A> : Node<A> { internal override int Size => 1; }
    internal sealed class IndexedExists<T, R> : Exists<T, R> { internal override int Size => 1; }
    internal interface INamed { string Name { get; } }
    internal sealed class NamedLambda(Func<string> f) : INamed { public string Name => f(); }
    internal abstract class Node<A> { internal abstract int Size { get; } }
    internal sealed class Shadow : INamed, ISized {
        public string Name => "s";
        public int Size => 0;
    }

    internal interface ISized { int Size { get; } }
    internal sealed class SizedLambda(Func<int> f) : ISized { public int Size => f(); }
    internal sealed class Unindexed<A> : Node<A> { internal override int Size => 0; }
    internal sealed class UnindexedExists<T, R> : Exists<T, R> { internal override int Size => 0; }
}
