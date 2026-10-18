using System;
namespace Org.Example.Ctors;
internal sealed class Joiners {
    internal static Joiner<A, B> LessThan<A, B, P>(Func<A, P> left, Func<B, P> right)
        where P : IComparable<P> => new Joiner<A, B>(x0_ => left(x0_), x0_ => right(x0_));
    internal sealed class Joiner<A, B> { internal Joiner(Func<A, object> left, Func<B, object> right) { } }
}
