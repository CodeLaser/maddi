using System;
using System.Collections.Generic;
namespace Org.Example.Fun;
internal sealed class Graph {
    internal static int Walk(Visitor v) => v("a");
    internal static int Lengths() => Walk(s => s.Length) + Walk(Graph.One);
    internal static int Counted() => Walk((string node) => 2);
    internal static Visitor Bound(Visitor v) => v.Invoke;
    internal static int One(string s) => 1;
    internal static void Tell(IListener l) { l.Changed("b"); }
    internal static void Quiet() { Tell(new IListener.Lambda(n => { })); }
    internal interface IListener {
        void Changed(string node);
        public sealed class Lambda(Action<string> f) : IListener { public void Changed(string node) => f(node); }
    }

    internal sealed class Recorder : IListener {
        internal readonly List<string> seen = new List<string>();
        public void Changed(string node) { seen.Add(node); }
    }
    internal delegate int Visitor(string node);
}
