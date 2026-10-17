using System;
using System.Collections.Generic;
namespace Org.Example.Defaults;
internal sealed class Streams {
    internal sealed class Concrete : IInnerStream<string> { }
    internal interface IInnerStream<A> : IStream<A> {
        List<C> IStream<A>.Join<C>(params string[] joiners) => new List<C>();
        S IStream<A>.Penalize<S>(S weight, long match) => weight;
    }

    internal interface IStream<A> {
        List<C> Join<C>(params string[] joiners);
        S Penalize<S>(S weight, long match) where S : IComparable<S>;
    }
}
