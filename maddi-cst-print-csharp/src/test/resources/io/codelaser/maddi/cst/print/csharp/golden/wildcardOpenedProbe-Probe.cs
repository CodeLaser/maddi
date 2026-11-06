using System;
namespace A;
internal sealed class Probe {
    internal static ICollector<A, X> andThen<A, I, X>(ICollector<A, I> @delegate, Func<I, X> mapping) => @delegate.Open_(
        new ProbeAndThenOpener_<A, I, X>(mapping));

    internal sealed class AndThen<A, R, I, X> : ICollector<A, R, X>, IAndThen {
        private readonly ICollector<A, R, I> @delegate;
        private readonly Func<I, X> mapping;
        internal AndThen(ICollector<A, R, I> @delegate, Func<I, X> mapping) {
            this.@delegate = (@delegate ?? throw new NullReferenceException());
            this.mapping = (mapping ?? throw new NullReferenceException());
        }
        public Func<R> Supplier() => @delegate.Supplier();
        public Func<R, A, Action> Accumulator() => @delegate.Accumulator();
        public Func<R, X> Finisher() {
            var finisher = @delegate.Finisher();
            return container => mapping(finisher(container));
        }

        public override bool Equals(object o) {
            if (o is AndThen<A, R, I, X> other) {
                return object.Equals(@delegate, other.@delegate) && object.Equals(mapping, other.mapping);
            }
            return false;
        }
        public override int GetHashCode() => HashCode.Combine(@delegate, mapping);
    }
    internal interface IAndThen { }

    internal interface ICollector<in A, R, out X> : ICollector<A, X> {
        Func<R> Supplier();
        Func<R, A, Action> Accumulator();
        Func<R, X> Finisher();
        Func<object> ICollector<A, X>.Supplier() { var f = Supplier(); return () => f(); }
        Func<object, X> ICollector<A, X>.Finisher() { var f = Finisher(); return x0_ => f((R) x0_); }
        TOpen_ ICollector<A, X>.Open_<TOpen_>(ICollectorOpener_<A, X, TOpen_> o) => o.With<R>(this);
    }
    internal interface ICollector<in A, out X> {
        Func<object> Supplier();
        Func<object, X> Finisher();
        TOpen_ Open_<TOpen_>(ICollectorOpener_<A, X, TOpen_> o);
    }
    internal interface ICollectorOpener_<out A, in X, out TOpen_> { TOpen_ With<R>(ICollector<A, R, X> x); }
}
internal sealed class ProbeAndThenOpener_<A, I, X>(Func<I, X> mapping) : Probe.ICollectorOpener_<A, I, Probe.ICollector<A, X>> {
    Probe.ICollector<A, X> Probe.ICollectorOpener_<A, I, Probe.ICollector<A, X>>.With<R>(Probe.ICollector<A, R, I> @delegate) => new Probe.AndThen<A, R, I, X>(@delegate, mapping);
}
