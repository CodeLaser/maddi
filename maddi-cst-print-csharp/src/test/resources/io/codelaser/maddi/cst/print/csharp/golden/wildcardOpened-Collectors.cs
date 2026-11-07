using System;
namespace A;
internal sealed class Collectors {
    internal static IAndThen last;
    internal static ICollector<A, X> andThen<A, I, X>(ICollector<A, I> @delegate, Func<I, X> f) => @delegate.Open_(
        new CollectorsAndThenOpener_<A, I, X>(f));

    internal sealed class AndThen<A, R, I, X> : ICollector<A, R, X>, IAndThen {
        private readonly ICollector<A, R, I> @delegate;
        private readonly Func<I, X> f;
        internal AndThen(ICollector<A, R, I> @delegate, Func<I, X> f) {
            this.@delegate = @delegate;
            this.f = f;
        }
        public R Supply() => @delegate.Supply();
        public void Accumulate(R r, A a) { @delegate.Accumulate(r, a); }
        public X Finish(R r) => f(@delegate.Finish(r));
    }
    internal interface IAndThen { }

    internal sealed class Change<S, E, V> : IProvider<S> {
        private readonly IVariable<S, E, V> variable;
        internal Change(IVariable<S, E, V> variable) { this.variable = variable; }
        public int Size => variable.Name().Length;
    }

    internal interface ICollector<in A, R, out I> : ICollector<A, I> {
        R Supply();
        void Accumulate(R r, A a);
        I Finish(R r);
        object ICollector<A, I>.Supply() => Supply();
        TOpen_ ICollector<A, I>.Open_<TOpen_>(ICollectorOpener_<A, I, TOpen_> o) => o.With<R>(this);
    }
    internal interface ICollector<in A, out I> {
        object Supply();
        TOpen_ Open_<TOpen_>(ICollectorOpener_<A, I, TOpen_> o);
    }
    internal interface ICollectorOpener_<out A, in I, out TOpen_> { TOpen_ With<R>(ICollector<A, R, I> x); }

    internal sealed class Holder<S> {
        private readonly IVariable<S> variable;
        internal Holder(object o) { this.variable = (IVariable<S>) o; }
        internal IProvider<S> Change() => variable.Open_(new HolderChangeOpener_<S>());
    }
    internal sealed class HolderChangeOpener_<S> : IVariableOpener_<S, IProvider<S>> {
        IProvider<S> IVariableOpener_<S, IProvider<S>>.With<E, V>(IVariable<S, E, V> variable) => new Change<S, E, V>(variable);
    }

    internal interface IProvider<S> { int Size { get; } }
    internal sealed class ProviderLambda<S>(Func<int> f) : IProvider<S> { public int Size => f(); }

    internal interface IVariable<S, out E, V> : IVariable<S> {
        string Name();
        E Entity();
        string IVariable<S>.Name() => Name();
        object IVariable<S>.Entity() => Entity();
        TOpen_ IVariable<S>.Open_<TOpen_>(IVariableOpener_<S, TOpen_> o) => o.With<E, V>(this);
    }
    internal interface IVariable<S> {
        string Name();
        object Entity();
        TOpen_ Open_<TOpen_>(IVariableOpener_<S, TOpen_> o);
    }
    internal interface IVariableOpener_<S, out TOpen_> { TOpen_ With<E, V>(IVariable<S, E, V> x); }
}
internal sealed class CollectorsAndThenOpener_<A, I, X>(Func<I, X> f) : Collectors.ICollectorOpener_<A, I, Collectors.ICollector<A, X>> {
    Collectors.ICollector<A, X> Collectors.ICollectorOpener_<A, I, Collectors.ICollector<A, X>>.With<R>(Collectors.ICollector<A, R, I> @delegate) => new Collectors.AndThen<A, R, I, X>(@delegate, f);
}
