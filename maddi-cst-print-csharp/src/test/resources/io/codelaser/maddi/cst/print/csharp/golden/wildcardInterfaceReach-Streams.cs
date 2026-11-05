using System;
namespace A;
internal sealed class Streams {
    internal abstract class Base<S> : IBase {
        protected internal readonly Base<S> parent;
        internal Base(Base<S> parent) { this.parent = parent; }
        internal int Depth() => parent == null ? 0 : 1 + parent.Depth();
        internal abstract int Arity { get; }
        int IBase.Depth() => Depth();
        int IBase.Arity => Arity;
    }
    internal interface IBase {
        internal int Depth();
        internal int Arity { get; }
    }

    internal sealed class Holder {
        internal readonly IMap any;
        internal Holder(IMap any) { this.any = any; }
        internal int Depth() => any.Depth();
        internal IBase Up() => any.parent;
    }

    internal sealed class Map<S, A> : Base<S>, IMap {
        internal override int Arity { get; }
        internal Map(Base<S> parent, int n) : base(parent) { this.Arity = n; }
        internal int Same(IMap other) => other.Arity;
        public override bool Equals(object o) {
            if (o is not Map<S, A> that) { return false; }
            return object.Equals(parent, that.parent) && Arity == that.Arity;
        }
        public override int GetHashCode() => HashCode.Combine(parent, Arity);
        int IMap.Arity => Arity;
        IBase IMap.parent => parent;
    }
    internal interface IMap : IBase {
        internal int Arity { get; }
        internal IBase parent { get; }
    }
}
