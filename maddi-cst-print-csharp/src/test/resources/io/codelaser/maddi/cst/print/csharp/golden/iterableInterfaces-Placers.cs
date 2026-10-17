using Maddi.JavaCompat;
using System.Collections.Generic;
namespace Org.Example.Placerses;
public sealed class Placers {
    internal abstract class AbstractPlacer<P> : IPlacer<P> {
        public bool Empty => !this.Iterator().HasNext();
        public abstract IJavaIterator<P> Iterator();
    }

    internal sealed class ListPlacer : AbstractPlacer<string> {
        private readonly List<string> list = new List<string> { "a" };
        public override IJavaIterator<string> Iterator() => list.Iterator();
    }

    internal interface IPlacer<P> : IEnumerable<P> {
        bool Empty { get; }
        IJavaIterator<P> Iterator();
        IEnumerator<P> IEnumerable<P>.GetEnumerator() => Iterator().AsEnumerator();
        System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator() => Iterator().AsEnumerator();
    }
}
