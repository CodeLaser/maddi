using Maddi.JavaCompat;
using System;
using System.Collections.Generic;
namespace Org.Example.Scopeds;
internal sealed class Scoped {
    internal static Notifiable<T> Build<T>(bool unique) => new Notifiable<T>(unique ? new OrderedSet<List<T>>()
        : new List<List<T>>());

    internal sealed class Joiner<A> { internal Joiner(Func<A, object>[] mappings) { } }
    internal static class Joiner {
        private static readonly Joiner<object> NONE = new Joiner<object>(new Func<object, object>[0]);
    }
    internal sealed class Notifiable<T> { internal Notifiable(ICollection<List<T>> queue) { } }
    internal sealed class OrderedSet<E> : JavaSet<E> {
        public override IJavaIterator<E> Iterator() => new JavaIterator<E>(new List<E>());
        public override int Size() => 0;
    }
}
