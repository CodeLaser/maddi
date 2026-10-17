using System;
namespace Org.Example.Companion;
public sealed class Response<T> {
    public T Content { get; }
    internal Response(T content) { this.Content = content; }
    public string Describe() => Response.EMPTY + Content + Response.Count();
}
public static class Response {
    internal const string EMPTY = "";
    internal static int Count() => 1;
    public static Response<T> From<T>(T content) => new Response<T>(content);
}
internal interface IStore<E> {
    E Get();
    public sealed class Lambda(Func<E> f) : IStore<E> { public E Get() => f(); }
}
internal static class IStore {
    public const int LIMIT = 10;
    public static IStore<E> Empty<E>() => null;
}
internal sealed class User { internal Response<string> Hello() => Response.From("hello" + IStore.LIMIT); }
