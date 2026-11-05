using System;
namespace Org.Example.Accumulatorses;
internal sealed class Accumulators {
    internal interface IAccumulator<in C, in A> {
        Action Into(C container);
        Action Apply(C container, A a) => Into(container);
    }
    internal sealed class AccumulatorLambda<C, A>(Func<C, Action> f) : IAccumulator<C, A> { public Action Into(C container) => f(container); }

    internal interface ICollector<in A, in C> { Func<C, A, Action> Accumulator(); }
    internal sealed class CollectorLambda<A, C>(Func<Func<C, A, Action>> f) : ICollector<A, C> { public Func<C, A, Action> Accumulator() => f(); }

    internal sealed class Counting<A> : ICollector<A, int[]> {
        public IAccumulator<int[], A> Accumulator() => new AccumulatorLambda<int[], A>(c => () => c[0]++);
        Func<int[], A, Action> ICollector<A, int[]>.Accumulator() => Accumulator().Apply;
    }
}
