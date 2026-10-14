using System;
namespace Org.Example.Rec;
internal interface IResult<T> {
    T Response();
    public sealed class Lambda(Func<T> f) : IResult<T> { public T Response() => f(); }
}
internal sealed record Success<T> : IResult<T> {
    public T Response { get; }
    public string Note { get; }
    public Success(T response, string note) {
        if (response == null) { throw new ArgumentException("response"); }
        note = note == null ? "" : note.Trim();
        Response = response;
        Note = note;
    }
    T IResult<T>.Response() => Response;
}
