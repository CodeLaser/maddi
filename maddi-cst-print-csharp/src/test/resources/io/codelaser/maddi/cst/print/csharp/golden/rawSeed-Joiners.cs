using System.Collections.Generic;
using System.Linq;
namespace A;
internal sealed class Joiners<A, B> {
    private readonly int size;
    internal Joiners() : this(0) { }
    internal Joiners(int size) { this.size = size; }
    internal Joiners<A, B> And(Joiners<A, B> other) => new Joiners<A, B>(size + other.size);
}
internal static class Joiners {
    private static readonly Joiners<object, object> NONE = new Joiners<object, object>();
    internal static Joiners<A, B> Merge<A, B>(List<Joiners<A, B>> joiners) => joiners.Aggregate((Joiners<A, B>) (object)
        NONE, (Joiners<A, B> p0, Joiners<A, B> p1) => p0.And(p1));
}
