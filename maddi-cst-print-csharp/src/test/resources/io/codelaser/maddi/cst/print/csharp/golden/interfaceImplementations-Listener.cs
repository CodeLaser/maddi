using System;
namespace Org.Example.Interfaces;
internal interface IListener<E> {
    void OnEvent(E @event);
    Type EventClass();
}
internal interface IStringListener : IListener<string> {
    Type IListener<string>.EventClass() => typeof(string);
    public sealed class Lambda(Action<string> f) : IStringListener { public void OnEvent(string @event) => f(@event); }
}
internal interface IFailure {
    IFailure WithCode(int code);
    public sealed class Lambda(Func<int, IFailure> f) : IFailure { public IFailure WithCode(int code) => f(code); }
}
internal interface IModel {
    int Dimension { get; }
    string Embed(string text);
}
internal abstract class AbstractModel : IModel {
    public int Dimension => Embed("test").Length;
    public abstract string Embed(string text);
}
internal sealed class UpperModel : AbstractModel { public override string Embed(string text) => text.ToUpperInvariant(); }
internal interface IStream {
    string GetSemantics();
    bool Distinct();
    int Size { get; }
}
internal abstract class BaseStream {
    private readonly string semantics;
    internal BaseStream(string semantics) { this.semantics = semantics; }
    public string GetSemantics() => semantics;
    public bool Distinct() => false;
}
internal abstract class MiddleStream : BaseStream, IStream {
    internal MiddleStream() : base("all") { }
    public abstract int Size { get; }
}
internal sealed class LeafStream : MiddleStream { public override int Size => 0; }
internal sealed class Holder {
    internal sealed class SimpleFailure : IFailure {
        public SimpleFailure WithCode(int code) => this;
        IFailure IFailure.WithCode(int code) => WithCode(code);
    }
}
