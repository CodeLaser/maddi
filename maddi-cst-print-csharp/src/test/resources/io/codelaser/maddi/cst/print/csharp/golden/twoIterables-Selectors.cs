using Maddi.JavaCompat;
using System.Collections.Generic;
namespace Org.Example.Iter;
public sealed class Selectors {
    internal sealed class Cached<T> : FromList<T> { internal Cached(List<T> items) : base(items) { } }
    internal abstract class Caching<T> : IEntitySelector<T> {
        public long Size => 0;
        public abstract IJavaIterator<T> Iterator();
        public IEnumerator<T> GetEnumerator() => Iterator().AsEnumerator();
        System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator() => GetEnumerator();
        public abstract List<T> List();
    }
    internal interface IEntitySelector<T> : ISelector<T>, IListIterable<T> { }
    internal class FromList<T> : ISelector<T>, IListIterable<T> {
        private readonly List<T> items;
        internal FromList(List<T> items) { this.items = items; }
        public long Size => items.Count;
        public List<T> List() => items;
        public IJavaIterator<T> Iterator() => items.Iterator();
        public IEnumerator<T> GetEnumerator() => Iterator().AsEnumerator();
        System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator() => GetEnumerator();
    }

    internal interface IListIterable<T> : IEnumerable<T> {
        List<T> List();
        IJavaIterator<T> Iterator();
        IEnumerator<T> IEnumerable<T>.GetEnumerator() => Iterator().AsEnumerator();
        System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator() => Iterator().AsEnumerator();
    }

    internal interface ISelector<T> : IEnumerable<T> {
        long Size { get; }
        IJavaIterator<T> Iterator();
        IEnumerator<T> IEnumerable<T>.GetEnumerator() => Iterator().AsEnumerator();
        System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator() => Iterator().AsEnumerator();
    }
}
