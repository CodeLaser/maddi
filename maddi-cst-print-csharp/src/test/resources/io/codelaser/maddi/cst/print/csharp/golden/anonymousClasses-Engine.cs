using System.Collections.Generic;
namespace Org.Example.Anon;
internal sealed class Engine {
    private readonly string prefix = "n";
    internal int Size(IGraph g) => g.Nodes().Count;
    internal int Run(int count) {
        List<string> all = new List<string>();
        IGraph g = new GraphImpl(this, all, count);
        Counter c = new CounterImpl(count);
        return Size(g) + c.Next();
    }

    internal abstract class Counter {
        internal readonly int start;
        internal Counter(int start) { this.start = start; }
        internal abstract int Next();
    }

    internal interface IGraph {
        List<string> Nodes();
        string First();
    }

    private sealed class GraphImpl : IGraph {
        private readonly Engine outer;
        private readonly List<string> all;
        private readonly int count;
        internal GraphImpl(Engine outer, List<string> all, int count) {
            this.outer = outer;
            this.all = all;
            this.count = count;
        }
        public List<string> Nodes() => all;
        public string First() => outer.prefix + count;
    }

    private sealed class CounterImpl : Counter {
        internal CounterImpl(int p0) : base(p0) { this.i = start; }
        private int i;
        internal override int Next() => i++;
    }
}
