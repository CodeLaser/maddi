using System;
namespace Org.Example.Eraseds;
internal sealed class Erased {
    internal sealed class ComparingSorter<S, T> : ISorter<S, T> { internal ComparingSorter(Comparison<T> comparator) { } }
    internal sealed class Holder<S> {
        private ISorter<S, object> sorter;
        internal void SortBy(object instance) {
            var comparator = (Comparison<object>) instance;
            sorter = new ComparingSorter<S, object>(comparator);
        }
    }

    internal sealed class Joiner<A, B> {
        private readonly Func<A, object>[] mappings;
        internal Joiner(Func<A, object>[] mappings) { this.mappings = (Func<A, object>[]) mappings; }
        internal Joiner<A, B> Reordered() {
            var copy = new Func<object, object>[mappings.Length];
            for (int i = 0; i < copy.Length; i++) { copy[i] = mappings[mappings.Length - 1 - i]; }
            return new Joiner<object, object>(copy);
        }
    }
    internal interface ISorter<S, T> { }
}
