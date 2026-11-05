using System;
namespace A;
internal sealed class Collectors {
    internal static IColl<S> andThen<S, RC>(IColl<S, RC> @delegate, Func<RC, RC> f) => new AndThen<S, RC>(@delegate, f);
    internal static IColl<string> Empty() => new AndThen<string, object>(null, null);
    internal static int Arity<TS, TRC>(AndThen<TS, TRC> a) => ((IColl<TS, TRC>) a).N();
    internal sealed class AndThen<S, RC> : IColl<S, RC> {
        internal AndThen(IColl<S, RC> @delegate, Func<RC, RC> f) { }
        public RC Start(S s) => default;
    }

    internal interface IColl<in S, out RC> : IColl<S> {
        RC Start(S s);
        int N() => 0;
        object IColl<S>.Start(S s) => Start(s);
        int IColl<S>.N() => N();
    }
    internal interface IColl<in S> {
        object Start(S s);
        int N();
    }
    internal sealed class CollLambda<S, RC>(Func<S, RC> f) : IColl<S, RC> { public RC Start(S s) => f(s); }

    internal sealed class Filtered<A, B> : IFiltered {
        internal Func<A, B, bool> Filter { get; }
        internal Filtered(Func<A, B, bool> filter) { this.Filter = filter; }
        Func<object, object, bool> IFiltered.Filter { get { var f = Filter; return (x0_, x1_) => f((A) x0_, (B) x1_); } }
    }
    internal interface IFiltered { internal Func<object, object, bool> Filter { get; } }

    internal sealed class Holder {
        internal readonly IFiltered f;
        internal Holder(IFiltered f) { this.f = f; }
        internal bool Any() => f.Filter != null;
    }
    internal sealed class Leaf<S> : Mid<S> { }
    internal class Mid<S> : Top<S> { }
    internal sealed class Pair {
        internal readonly ITop top;
        internal readonly Leaf<object> leaf;
        internal Pair(ITop top, Leaf<object> leaf) {
            this.top = top;
            this.leaf = leaf;
        }
        internal string Names() => top.Name() + leaf.Name();
    }

    internal class Top<S> : ITop {
        internal string Name() => "t";
        string ITop.Name() => Name();
    }
    internal interface ITop { internal string Name(); }
}
