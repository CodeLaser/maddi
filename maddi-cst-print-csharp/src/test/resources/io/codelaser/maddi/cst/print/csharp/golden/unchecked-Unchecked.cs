using System;
using System.Collections.Generic;
namespace Org.Example.Uncheckeds;
internal sealed class Unchecked<Node_> {
    private readonly IDictionary<string, Node_> nodes = new Dictionary<string, Node_>(16);
    internal bool SeenNode(Node_[] values, Node_ v) => Unchecked.Seen((object[]) (object) values, v);
}
internal static class Unchecked {
    private static readonly Func<object, object, object> PICK_FIRST = (a, b) => a;
    private static readonly Func<object, object> IDENTITY = x_ => x_;
    internal static int Weigh(IDictionary<string, int> weights) => weights.Count;
    internal static int WeighAny<W>(IDictionary<string, W> weights) => Weigh((IDictionary<string, int>) weights);
    internal static bool Seen(object[] values, object v) {
        foreach (object o in values) { if (o == v) { return true; } }
        return false;
    }
    internal static Func<int, ISet<V>> Sets<V>() => (Func<int, ISet<V>>) (p0 => new HashSet<V>(p0));
    internal static Func<A, B, A> PickFirst<A, B>() => (a, b) => a;
    internal static Func<A, A> Identity<A>() => x_ => x_;
    internal static IMv<S> Build<S, M>(params M[] moves) where M : IMv<S> => moves[0];
    internal static IMv<S> Same<S>(IMv<S> a, IMv<S> b) => Build<S, IMv<S>>(a, b);
    internal static IMv<S> Mixed<S>(Unassign<S> a, Assign<S> b) => Build<S, IMv<S>>(a, b);
    internal static Cell<T> Pick<T>(Cell<T>[] cells) => cells[0];
    internal static Cell<T> First<T>(Cell<T> c) {
        var all = new Cell<T>[1];
        all[0] = c;
        return Pick(all);
    }
}
internal sealed class Assign<S> : IMv<S> { }
internal sealed class Cell<T> { internal T t; }
internal interface IMv<S> { }
internal sealed class Unassign<S> : IMv<S> { }
