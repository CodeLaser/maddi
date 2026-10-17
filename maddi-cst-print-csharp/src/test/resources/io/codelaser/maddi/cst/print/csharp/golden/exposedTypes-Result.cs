using System.Collections.Generic;
namespace Org.Example.Exposed;
public sealed class Result {
    public List<Pair> Pairs { get; } = new List<Pair>();
    internal Hidden hidden() => new Hidden();
    internal sealed class Hidden { }
    public sealed class Pair {
        internal readonly int a;
        internal Pair(int a) { this.a = a; }
    }
}
