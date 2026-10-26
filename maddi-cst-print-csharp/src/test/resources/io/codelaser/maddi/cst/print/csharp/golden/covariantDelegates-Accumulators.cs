using System;
namespace Org.Example.Accumulatorses;
internal sealed class Accumulators {
    internal interface IAccumulator<C, A> {
        Action Into(C container);
        Action Apply(C container, A a) => Into(container);
        public sealed class Lambda(Func<C, Action> f) : IAccumulator<C, A> { public Action Into(C container) => f(container); }
    }

    internal interface ICollector<A, C> {
        Func<C, A, Action> Accumulator();
        public sealed class Lambda(Func<Func<C, A, Action>> f) : ICollector<A, C> { public Func<C, A, Action> Accumulator() => f(); }
    }

    internal sealed class Counting<A> : ICollector<A, int[]> {
        public IAccumulator<int[], A> Accumulator() => new IAccumulator<int[], A>.Lambda(c => () => c[0]++);
        Func<int[], A, Action> ICollector<A, int[]>.Accumulator() => Accumulator().Apply;
    }
}
