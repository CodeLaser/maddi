using Maddi.JavaCompat;
using System;
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
