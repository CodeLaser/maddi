using System;
using System.Collections.Generic;
namespace Org.Example.Capture;
internal interface INamed {
    string Name();
    string Title();
}
internal sealed class Context<Embedded> { internal List<Embedded> Items() => new List<Embedded>(); }
internal interface IListener {
    void OnRequest<TEmbedded>(Context<TEmbedded> context) { }
    int Size<TEmbedded>(Context<TEmbedded> context) where TEmbedded : INamed => context.Items().Count;
    int Length(Context<string> context) => context.Items().Count;
}
internal sealed class Counting : IListener {
    public void OnRequest<TEmbedded>(Context<TEmbedded> context) { Console.Out.WriteLine(context.Items().Count); }
}
