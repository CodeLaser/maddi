using System;
namespace Org.Example.Boxedrefs;
public sealed class Counts {
    internal static Func<State, long> Finisher() => (State p0) => (long) p0.Result();
    internal static Func<long> First() => () => (long) State.Initial();
    internal sealed class State {
        internal long? Result() => 1L;
        internal static long? Initial() => 0L;
        internal long? Own() => 2L;
        internal Func<long> Mine() => () => (long) this.Own();
    }
}
