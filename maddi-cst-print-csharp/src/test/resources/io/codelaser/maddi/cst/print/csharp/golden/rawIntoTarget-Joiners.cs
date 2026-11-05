using Maddi.JavaCompat;
using System;
using System.Collections.Generic;
namespace A;
internal sealed class Joiners<A, B> {
    private readonly IMapper<A, object>[] leftMappings;
    private readonly Func<B, object>[] rightMappings;
    private readonly IMapper<A, object>[] single;
    internal Joiners(IMapper<A>[] leftMappings, Func<B, object>[] rightMappings, IMapper<A> first) {
        this.leftMappings = (IMapper<A, object>[]) leftMappings;
        this.rightMappings = (Func<B, object>[]) rightMappings;
        this.single = new IMapper<A, object>[] { (IMapper<A, object>) first };
    }

    internal Joiners<A, B> And(Joiners<A, B> other) {
        IMapper<A, object>[] newLeftMappings = JavaArrays.CopyOf(leftMappings, leftMappings.Length + 1);
        Func<B, object>[] newRightMappings = JavaArrays.CopyOf(rightMappings, rightMappings.Length + 1);
        newLeftMappings[leftMappings.Length] = other.leftMappings[0];
        newRightMappings[rightMappings.Length] = other.rightMappings[0];
        return new Joiners<A, B>(newLeftMappings, newRightMappings, leftMappings[0]);
    }
}
internal interface IMapper<in X, out R> : IMapper<X> {
    R Map(X x);
    R Twice(X x) => Map(x);
    object IMapper<X>.Map(X x) => Map(x);
    object IMapper<X>.Twice(X x) => Twice(x);
}
internal interface IMapper<in X> {
    object Map(X x);
    object Twice(X x);
}
internal sealed class MapperLambda<X, R>(Func<X, R> f) : IMapper<X, R> { public R Map(X x) => f(x); }
internal sealed class Sentinels<B> {
    internal ITuple<B> next = (ITuple<B>) Sentinels.EMPTY;
    internal Pair<B> None() => (Pair<B>) (object) Sentinels.NONE;
}
internal static class Sentinels {
    internal static readonly ITuple<object> EMPTY = new TupleLambda<object>(() => null);
    internal static readonly Pair<object> NONE = new Pair<object>();
}
internal sealed class Pair<T> : ITuple<T> { public T Value() => default; }
internal interface ITuple<out T> {
    T Value();
    int Size() => 1;
}
internal sealed class TupleLambda<T>(Func<T> f) : ITuple<T> { public T Value() => f(); }
internal sealed class Sorters {
    internal static ISorter<string> Sorter(Comparison<object> raw) {
        ISorter<string> s;
        s = new BySorter<string>(raw);
        return s;
    }

    internal sealed class BySorter<X> : ISorter<X> {
        internal BySorter(Comparison<X> c) { }
        public void Sort(List<X> xs) { }
    }

    internal interface ISorter<X> {
        void Sort(List<X> xs);
        int N() => 0;
    }
    internal sealed class SorterLambda<X>(Action<List<X>> f) : ISorter<X> { public void Sort(List<X> xs) => f(xs); }
}
internal sealed class Descriptors<Solution_> {
    internal readonly Genuine<Solution_> only = null;
    internal int Build<TS>(ListVariable<TS> v) => 1;
    internal int Size() => only.List() ? Build((ListVariable<Solution_>) only) : 0;
}
internal static class Descriptors {
    internal static int Take<T>(ListVariable<T> v) => 0;
    internal static int Pass<TS>(ListVariable<TS> w) => Take(w);
}
internal class Genuine<S> { internal bool List() => false; }
internal sealed class ListVariable<S> : Genuine<S> { }
internal sealed class Plugins {
    internal static T NewInstance<T>(Type type) => default;
    internal static IFilter<string, int> Filter(Type filterClass) {
        IFilter<string, int> filter = NewInstance<IFilter<string, int>>(filterClass);
        return filter;
    }
    internal static IFilter<string, int> Direct(Type filterClass) => NewInstance<IFilter<string, int>>(filterClass);
    internal interface IFilter<in S, in T> {
        bool Accept(S s, T t);
        bool Reject(S s, T t) => !Accept(s, t);
    }
    internal sealed class FilterLambda<S, T>(Func<S, T, bool> f) : IFilter<S, T> { public bool Accept(S s, T t) => f(s, t); }
}
