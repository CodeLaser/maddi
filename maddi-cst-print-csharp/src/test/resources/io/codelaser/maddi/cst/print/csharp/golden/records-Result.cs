using Maddi.JavaCompat;
using System;
namespace Org.Example.Rec;
internal interface IResult<out T> { T Response(); }
internal sealed class ResultLambda<T>(Func<T> f) : IResult<T> { public T Response() => f(); }
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
    public override string ToString() => "Success[response=" + JavaString.ValueOf(Response) + ", note=" + JavaString.ValueOf(Note) + "]";
}
