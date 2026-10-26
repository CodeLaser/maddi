using Maddi.JavaCompat;
using System;
using System.Collections.Generic;
namespace Org.Example.Inner;
public sealed class Range : IEnumerable<long> {
    private readonly long from;
    private readonly long to;
    public Range(long from, long to) {
        this.from = from;
        this.to = to;
    }
    public IJavaIterator<long> Iterator() => new Upcoming(this);
    internal long Width() => to - from;
    internal Upcoming Twice(Range other) => new Upcoming(other);
    internal Func<long, Stepped> Stepper() => p0 => new Stepped(this, p0);
    internal sealed class Plain { internal int One() => 1; }
    internal sealed class Stepped : Upcoming {
        private readonly Range outer;
        internal long Step { get; }
        internal Stepped(Range outer, long step) : base(outer) {
            this.outer = outer;
            this.Step = step;
        }
    }

    internal class Upcoming : IJavaIterator<long> {
        private readonly Range outer;
        private long next;
        internal Upcoming(Range outer) {
            this.outer = outer;
            this.next = outer.from;
        }
        public bool HasNext() => next < outer.to;
        public long? Next() => next++;
        internal long Left() => outer.Width() - (next - outer.from);
        internal Upcoming Copy() => new Upcoming(outer);
    }
    public IEnumerator<long> GetEnumerator() => Iterator().AsEnumerator();
    System.Collections.IEnumerator System.Collections.IEnumerable.GetEnumerator() => GetEnumerator();
}
