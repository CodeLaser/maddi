using Maddi.JavaCompat;
using System;
using System.Collections.Generic;
namespace Org.Example.Streamses;
public sealed class Streams<A> : IStreams {
    internal S Share<S>(S stream) where S : IStream => stream;
    internal IStream<ResultA_> Map<ResultA_>(Func<A, ResultA_> mapping) => Share(new MapStream<A, ResultA_>(
        this,
        mapping));

    internal S Share<S>(S stream, Action<S> consumer) where S : IStream {
        consumer(stream);
        return stream;
    }
    internal IStream<NewA> group<NewA>(Group<A, NewA> group) => Share(new Bridge<NewA>(this), group.SetBridge);
    internal IStream<B> Join<B>(Joined<A, B> joined) => Share(new Bridge2<A, B>(this), joined.SetBridge);
    S IStreams.Share<S>(S stream) => Share<S>(stream);
    S IStreams.Share<S>(S stream, Action<S> consumer) => Share<S>(stream, consumer);
}
public interface IStreams {
    internal S Share<S>(S stream) where S : IStream;
    internal S Share<S>(S stream, Action<S> consumer) where S : IStream;
}
public static class Streams {
    internal static string Key<T>(Func<T, object> mapping, T t) => JavaString.ValueOf(mapping(t));
    internal static string Keyed<T, P>(Func<T, P> mapping, T t) => Key(x0_ => mapping(x0_), t);
    internal static string Raw(Func<object, object> mapping) => JavaString.ValueOf(mapping("x"));
    internal static string RawKeyed<T, P>(Func<T, P> mapping) => Raw(x0_ => mapping((T) x0_));
    internal static string Supplied(Func<object> supplier) => JavaString.ValueOf(supplier());
    internal static string Supply<P>(Func<P> supplier) => Supplied(() => supplier());
    internal static ISet<E> FromMap<E>(IDictionary<E, bool> map) => map.KeySet();
    internal static List<M> Fill<V, M>(V value, Func<M> supplier)
        where M : IDictionary<string, V> => new List<M> { supplier() };

    internal static List<IDictionary<string, V>> Filled<V>(V value) => Fill<V, IDictionary<string, V>>(
        value,
        () => (IDictionary<string, V>) new Dictionary<string, V>());
    internal static ISet<T> IdentitySet<T>() => FromMap(new Dictionary<T, bool>());
    internal static Slot<R, R> MinSlot<R>() where R : IComparable<R> => new Slot<R, R>(
        true,
        new SortedDictionary<R, R>());
    internal static Func<L, R, Pair<L, R>> Pairing<L, R>() => (p0, p1) => new Pair<L, R>(p0, p1);
    internal static Func<int, ISet<V>> Sets<V>() => p0 => new HashSet<V>(p0);
    internal static List<R> Empty<R>() => new List<R>();
    internal static Func<List<R>> EmptySupplier<R>() => Streams.Empty<R>;
    internal static R Max<T, R>(Func<T, R> mapping, T t) => mapping(t);
    internal static P Maxed<T, P>(Func<T, P> mapping, T t) => Max(mapping, t);
}
internal sealed class Bridge<A> : IStream<A> { internal Bridge(IStreams parent) { } }
internal sealed class Bridge2<A, B> : IStream<B> { internal Bridge2(IStreams parent) { } }
internal abstract class Builder { internal abstract S Build<S>(S s) where S : IStream; }
internal sealed class Group<A, NewA> : IStream<NewA> { internal void SetBridge(Bridge<NewA> bridge) { } }
internal sealed class Joined<A, B> : IStream<B> { internal void SetBridge(Bridge2<A, B> bridge) { } }
internal sealed class MapStream<A, NewA> : IStream<NewA> {
    internal MapStream(Streams<A> parent, Func<A, NewA> mapping) { }
}
internal sealed record Pair<L, R>(L Left, R Right);
internal sealed class SimpleBuilder : Builder { internal override S Build<S>(S s) => s; }
internal sealed class Slot<R, P> { internal Slot(bool min, SortedDictionary<P, R> map) { } }
internal interface IStream<T> : IStream { }
internal interface IStream { }
