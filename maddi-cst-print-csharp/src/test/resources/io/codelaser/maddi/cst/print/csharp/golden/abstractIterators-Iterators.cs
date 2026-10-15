using Maddi.JavaCompat;
using System;
namespace Org.Example.Iteratorses;
public sealed class Iterators {
    internal sealed class Countdown : SelectionIterator<int> {
        private int left = 3;
        public override bool HasNext() => left > 0;
        public override int Next() => left--;
    }

    internal abstract class SelectionIterator<S> : IJavaIterator<S> {
        public void Remove() { throw new NotSupportedException(); }
        public abstract bool HasNext();
        public abstract S Next();
    }
}
