namespace Org.Example.Mutual;
public sealed class Tree {
    internal int size;
    internal sealed class Branch {
        private readonly Tree outer;
        internal Branch(Tree outer) { this.outer = outer; }
        internal Leaf Leaf() => new Leaf(outer);
        internal int Size() => outer.size;
    }

    internal sealed class Leaf {
        private readonly Tree outer;
        internal Leaf(Tree outer) { this.outer = outer; }
        internal Branch Branch() => new Branch(outer);
    }
}
