using System;
using System.Collections.Generic;
namespace A;
internal sealed class Indexes {
    internal IIndex<T> Build<T, TK>(Unpacker<TK> u, Func<IIndex<T>> s, bool b) {
        if (b) { return new Equal<T, TK>(u, s); }
        return new Containing<T, TK, ICollection<TK>>(u, s);
    }

    internal sealed class Containing<T, K, C> : IIndex<T>, IContaining<T, K> where C : ICollection<K> {
        internal Containing(Unpacker<K> u, Func<IIndex<T>> s) { }
        public int Size => 1;
    }
    internal interface IContaining<T, K> : IIndex<T> { }

    internal sealed class Equal<T, K> : IIndex<T> {
        internal Equal(Unpacker<K> u, Func<IIndex<T>> s) { }
        public int Size => 0;
    }

    internal interface IIndex<T> { int Size { get; } }
    internal sealed class IndexLambda<T>(Func<int> f) : IIndex<T> { public int Size => f(); }
    internal delegate K Unpacker<out K>(object o);
}
