using System;
using System.Collections.Generic;
namespace Org.Example.Collectorses;
internal sealed class Collectors {
    internal static ICollector<A, List<A>> toList<A>() => new ToList<A>();
    internal static List<string> Names(List<string> items) => new Group<string, List<string>>(Collectors.toList<string>())
        .Collect(items);

    internal interface ICollector<A, ResultContainer_, Result_> : ICollector<A, Result_> {
        Func<ResultContainer_> Supplier();
        Func<ResultContainer_, A, Action> Accumulator();
        Func<ResultContainer_, Result_> Finisher();
        Func<object> ICollector<A, Result_>.Supplier() { var f = Supplier(); return () => f(); }
        Func<object, A, Action> ICollector<A, Result_>.Accumulator() { var f = Accumulator(); return (x0_, x1_) => f((ResultContainer_) x0_, x1_); }
        Func<object, Result_> ICollector<A, Result_>.Finisher() { var f = Finisher(); return x0_ => f((ResultContainer_) x0_); }
    }
    internal interface ICollector<A, Result_> {
        Func<object> Supplier();
        Func<object, A, Action> Accumulator();
        Func<object, Result_> Finisher();
    }

    internal sealed class Group<A, R> {
        private readonly ICollector<A, R> collector;
        internal Group(ICollector<A, R> collector) { this.collector = collector; }
        internal R Collect(List<A> items) {
            object container = collector.Supplier()();
            var accumulator = (Func<object, A, Action>) (Func<object, A, Action>) collector.Accumulator();
            foreach (A a in items) { accumulator(container, a); }
            return ((Func<object, R>) (Func<object, R>) collector.Finisher())(container);
        }
    }

    internal sealed class ToList<A> : ICollector<A, List<A>, List<A>> {
        public Func<List<A>> Supplier() => () => new List<A>();
        public Func<List<A>, A, Action> Accumulator() => (list, a) => {
            list.Add(a);
            return () => list.Remove(a);
        };
        public Func<List<A>, List<A>> Finisher() => list => list;
    }
}
