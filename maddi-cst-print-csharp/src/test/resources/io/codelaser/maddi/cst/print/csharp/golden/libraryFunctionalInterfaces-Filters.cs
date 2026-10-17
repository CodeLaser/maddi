using System;
namespace Org.Example.Filterses;
public sealed class Filters {
    internal static bool Movable(IMovable<string> m, object e) => m.Test("s", e);
    internal static int Pin(IPinReader r, object e) => r == null ? 0 : r.ApplyAsInt(e);
    internal sealed class Always : IMovable<string> { public bool Test(string s, object e) => true; }
    internal interface IMovable<S> {
        IMovable<S> And(IMovable<S> other) => new IMovable<S>.Lambda((s, e) => Test(s, e) && other.Test(s, e));
        bool Test(S arg0, object arg1);
        public sealed class Lambda(Func<S, object, bool> f) : IMovable<S> { public bool Test(S arg0, object arg1) => f(arg0, arg1); }
    }

    internal interface IPinReader {
        int ApplyAsInt(object arg0);
        public sealed class Lambda(Func<object, int> f) : IPinReader { public int ApplyAsInt(object arg0) => f(arg0); }
    }
}
