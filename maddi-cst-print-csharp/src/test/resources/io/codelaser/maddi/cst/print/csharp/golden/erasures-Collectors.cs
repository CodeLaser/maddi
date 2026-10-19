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
    internal interface IBiCollector<A, B, ResultContainer_, Result_> {
        Func<ResultContainer_> Supplier();
        ITriFunction<ResultContainer_, A, B, Action> Accumulator();
        static IBiCollector<A, B, object, Result_> Erased_(IBiCollector<A, B, ResultContainer_, Result_> erased) => erased as IBiCollector<A, B, object, Result_> ?? new Erasure_(erased);
        private sealed class Erasure_ : IBiCollector<A, B, object, Result_> {
            private readonly IBiCollector<A, B, ResultContainer_, Result_> erased;
            internal Erasure_(IBiCollector<A, B, ResultContainer_, Result_> erased) { this.erased = erased; }
            public Func<object> Supplier() { var f = erased.Supplier(); return () => f(); }
            public ITriFunction<object, A, B, Action> Accumulator() { var f = erased.Accumulator(); return new ITriFunction<object, A, B, Action>.Lambda((x0_, x1_, x2_) => f.Apply((ResultContainer_) x0_, x1_, x2_)); }
        }
    }

    internal interface ICollector<A, ResultContainer_, Result_> {
        Func<ResultContainer_> Supplier();
        Func<ResultContainer_, A, Action> Accumulator();
        Func<ResultContainer_, Result_> Finisher();
        static ICollector<A, object, Result_> Erased_(ICollector<A, ResultContainer_, Result_> erased) => erased as ICollector<A, object, Result_> ?? new Erasure_(erased);
        private sealed class Erasure_ : ICollector<A, object, Result_> {
            private readonly ICollector<A, ResultContainer_, Result_> erased;
            internal Erasure_(ICollector<A, ResultContainer_, Result_> erased) { this.erased = erased; }
            public Func<object> Supplier() { var f = erased.Supplier(); return () => f(); }
            public Func<object, A, Action> Accumulator() { var f = erased.Accumulator(); return (x0_, x1_) => f((ResultContainer_) x0_, x1_); }
            public Func<object, Result_> Finisher() { var f = erased.Finisher(); return x0_ => f((ResultContainer_) x0_); }
        }
    }

    internal sealed class Concat : ITriFunction<string, string, string, string> {
        public string Apply(string a, string b, string c) => a + b + c;
    }

    internal sealed class Count<A, B> : IBiCollector<A, B, int[], int> {
        public Func<int[]> Supplier() => () => new int[1];
        public ITriFunction<int[], A, B, Action> Accumulator() => new ITriFunction<int[], A, B, Action>.Lambda((c, a, b) => {
            c[0]++;
            return () => c[0]--;
        });
    }

    internal sealed class Distinct<A, M> : ICollector<A, ISet<M>, int> {
        internal Distinct(Func<A, M> mapper) { }
        public Func<ISet<M>> Supplier() => () => new HashSet<M>();
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

    internal interface ITriFunction<A, B, C, R> {
        R Apply(A a, B b, C c);
        public sealed class Lambda(Func<A, B, C, R> f) : ITriFunction<A, B, C, R> { public R Apply(A a, B b, C c) => f(a, b, c); }
    }
}
