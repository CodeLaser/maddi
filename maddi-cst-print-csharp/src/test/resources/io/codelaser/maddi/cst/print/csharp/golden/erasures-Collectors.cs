using Maddi.JavaCompat;
using System;
using System.Collections.Generic;
namespace Org.Example.Erasures;
internal sealed class Collectors {
    internal static ICollector<A, C, R> Logged<A, C, R>(ICollector<A, C, R> collector) => collector;
    internal static ICollector<A, object, List<A>> toList<A>() => ICollector<A, List<A>, List<A>>.Erased_(new ToList<A>());
    internal static ICollector<A, object, List<A>> LoggedList<A>() => Logged(toList<A>());
    internal static ICollector<A, object, R> Plain<A, C, R>(ICollector<A, C, R> collector)
        => ICollector<A, C, R>.Erased_(collector);

    internal static ICollector<A, object, int> distinct<A, M>(Func<A, M> mapper) => ICollector<A, ISet<M>, int>.Erased_(
        new Distinct<A, M>(mapper));
    internal static IBiCollector<A, B, C, R> LoggedBi<A, B, C, R>(IBiCollector<A, B, C, R> collector) => collector;
    internal static IBiCollector<A, B, object, int> count<A, B>() => IBiCollector<A, B, int[], int>.Erased_(
        new Count<A, B>());
    internal static IBiCollector<A, B, object, int> LoggedCount<A, B>() => LoggedBi(count<A, B>());
    internal interface IBiCollector<in A, in B, ResultContainer_, Result_> {
        Func<ResultContainer_> Supplier();
        ITriFunction<ResultContainer_, A, B, Action> Accumulator();
        static IBiCollector<A, B, object, Result_> Erased_(IBiCollector<A, B, ResultContainer_, Result_> erased) => erased as IBiCollector<A, B, object, Result_> ?? new BiCollectorErasure_<A, B, ResultContainer_, Result_>(erased);
    }
    internal sealed class BiCollectorErasure_<A, B, ResultContainer_, Result_> : IBiCollector<A, B, object, Result_> {
        private readonly IBiCollector<A, B, ResultContainer_, Result_> erased;
        internal BiCollectorErasure_(IBiCollector<A, B, ResultContainer_, Result_> erased) { this.erased = erased; }
        public Func<object> Supplier() { var f = erased.Supplier(); return () => f(); }
        public ITriFunction<object, A, B, Action> Accumulator() { var f = erased.Accumulator(); return new TriFunctionLambda<object, A, B, Action>((x0_, x1_, x2_) => f.Apply((ResultContainer_) x0_, x1_, x2_)); }
    }

    internal interface ICollector<in A, ResultContainer_, out Result_> {
        Func<ResultContainer_> Supplier();
        Func<ResultContainer_, A, Action> Accumulator();
        Func<ResultContainer_, Result_> Finisher();
        static ICollector<A, object, Result_> Erased_(ICollector<A, ResultContainer_, Result_> erased) => erased as ICollector<A, object, Result_> ?? new CollectorErasure_<A, ResultContainer_, Result_>(erased);
    }
    internal sealed class CollectorErasure_<A, ResultContainer_, Result_> : ICollector<A, object, Result_> {
        private readonly ICollector<A, ResultContainer_, Result_> erased;
        internal CollectorErasure_(ICollector<A, ResultContainer_, Result_> erased) { this.erased = erased; }
        public Func<object> Supplier() { var f = erased.Supplier(); return () => f(); }
        public Func<object, A, Action> Accumulator() { var f = erased.Accumulator(); return (x0_, x1_) => f((ResultContainer_) x0_, x1_); }
        public Func<object, Result_> Finisher() { var f = erased.Finisher(); return x0_ => f((ResultContainer_) x0_); }
    }

    internal sealed class Concat : ITriFunction<string, string, string, string> {
        public string Apply(string a, string b, string c) => a + b + c;
    }

    internal sealed class Count<A, B> : IBiCollector<A, B, int[], int> {
        public Func<int[]> Supplier() => () => new int[1];
        public ITriFunction<int[], A, B, Action> Accumulator() => new TriFunctionLambda<int[], A, B, Action>((c, a, b) => {
            c[0]++;
            return () => c[0]--;
        });
    }

    internal sealed class Distinct<A, M> : ICollector<A, ISet<M>, int> {
        internal Distinct(Func<A, M> mapper) { }
        public Func<ISet<M>> Supplier() => () => new JavaLinkedHashSet<M>();
        public Func<ISet<M>, A, Action> Accumulator() => (set, a) => () => { };
        public Func<ISet<M>, int> Finisher() => (ISet<M> p0) => p0.Count;
    }

    internal sealed class ToList<A> : ICollector<A, List<A>, List<A>> {
        public Func<List<A>> Supplier() => () => new List<A>();
        public Func<List<A>, A, Action> Accumulator() => (list, a) => {
            list.Add(a);
            return () => list.Remove(a);
        };
        public Func<List<A>, List<A>> Finisher() => list => list;
    }

    internal interface ITriFunction<in A, in B, in C, out R> { R Apply(A a, B b, C c); }
    internal sealed class TriFunctionLambda<A, B, C, R>(Func<A, B, C, R> f) : ITriFunction<A, B, C, R> { public R Apply(A a, B b, C c) => f(a, b, c); }
}
