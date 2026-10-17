namespace Org.Example.Joinerses;
public sealed class Joiners<A> {
    internal int Join<B>(Joiners<B> other, params IBiJoiner<A, B>[] joiners) => joiners.Length;
    internal int Join<B>(Joiners<B> other, IBiJoiner<A, B> joiner) => Join(other, new IBiJoiner<A, B>[] { joiner });
    internal int Join<B>(Joiners<B> other) => Join(other, new IBiJoiner<A, B>[0]);
}
internal interface IBiJoiner<A, B> { }
