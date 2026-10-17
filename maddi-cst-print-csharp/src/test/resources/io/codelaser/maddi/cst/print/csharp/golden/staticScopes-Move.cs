namespace Org.Example.Scopes;
public abstract class Move<T> { internal abstract T Value(); }
public static class Move { internal static int Size(object o) => 1; }
public sealed record Id(long Key, Id.Kind? kind) {
    internal Id(long key) : this(key, Id.Kind.Node) { }
    public enum Kind { Node, Input }
}
